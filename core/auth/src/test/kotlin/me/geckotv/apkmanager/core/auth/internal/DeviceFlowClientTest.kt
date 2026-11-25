package me.geckotv.apkmanager.core.auth.internal

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.geckotv.apkmanager.core.auth.GitHubOAuthConfig
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DeviceFlowClientTest {
    private val server = MockWebServer()
    private lateinit var api: GitHubOAuthApi

    @Before
    fun setUp() {
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(
                    Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()),
                ).build()
                .create(GitHubOAuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun startRequestsTheRepoScopeForPrivateAccess() =
        runTest {
            server.enqueue(json(DEVICE_CODE))

            val result = client().start(GitHubAccessLevel.Private)

            val code = (result as DeviceFlowClient.StartResult.Started).deviceCode
            assertEquals("ABCD-1234", code.userCode)
            assertEquals("https://github.com/login/device", code.verificationUri)
            val body = server.takeRequest().body!!.utf8()
            assertTrue(body.contains("client_id=test-client"))
            assertTrue(body.contains("scope=repo"))
        }

    @Test
    fun startFailsWhenDeviceFlowIsDisabled() =
        runTest {
            server.enqueue(json("""{"error":"device_flow_disabled"}"""))

            val result = client().start(GitHubAccessLevel.Public)

            assertEquals(DeviceFlowClient.StartResult.Failed(SignInFailure.Unknown), result)
        }

    @Test
    fun pollsUntilAuthorizedAndHonorsSlowDown() =
        runTest {
            server.enqueue(json(DEVICE_CODE))
            server.enqueue(json("""{"error":"authorization_pending"}"""))
            server.enqueue(json("""{"error":"slow_down","interval":10}"""))
            server.enqueue(json("""{"access_token":"gho_token","token_type":"bearer","scope":"repo"}"""))
            val client = client()
            val code = (client.start(GitHubAccessLevel.Private) as DeviceFlowClient.StartResult.Started).deviceCode

            val result = client.awaitToken(code)

            assertEquals(DeviceFlowClient.TokenResult.Success("gho_token", setOf("repo")), result)
            // 5 s, then 5 s, then the 10 s interval GitHub asked for.
            assertEquals(20_000L, testScheduler.currentTime)
            server.takeRequest()
            val poll = server.takeRequest().body!!.utf8()
            assertTrue(poll.contains("device_code=dev-code"))
            assertTrue(poll.contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code"))
        }

    @Test
    fun reportsDeniedAuthorization() =
        runTest {
            server.enqueue(json(DEVICE_CODE))
            server.enqueue(json("""{"error":"access_denied"}"""))
            val client = client()
            val code = (client.start(GitHubAccessLevel.Public) as DeviceFlowClient.StartResult.Started).deviceCode

            assertEquals(DeviceFlowClient.TokenResult.Failed(SignInFailure.Denied), client.awaitToken(code))
        }

    @Test
    fun stopsPollingWhenTheCodeExpires() =
        runTest {
            server.enqueue(json(DEVICE_CODE.replace("\"expires_in\": 900", "\"expires_in\": 12")))
            server.enqueue(json("""{"error":"authorization_pending"}"""))
            server.enqueue(json("""{"error":"authorization_pending"}"""))
            val client = client()
            val code = (client.start(GitHubAccessLevel.Public) as DeviceFlowClient.StartResult.Started).deviceCode

            assertEquals(DeviceFlowClient.TokenResult.Failed(SignInFailure.Expired), client.awaitToken(code))
            assertEquals(3, server.requestCount)
        }

    private fun TestScope.client() = DeviceFlowClient(api, GitHubOAuthConfig("test-client"), virtualClock())

    /** A clock that follows the test scheduler, so expiry works with virtual time. */
    private fun TestScope.virtualClock(): Clock =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId?): Clock = this

            override fun instant(): Instant = Instant.ofEpochMilli(testScheduler.currentTime)
        }

    private fun json(body: String): MockResponse =
        MockResponse
            .Builder()
            .setHeader("Content-Type", "application/json")
            .body(body)
            .build()

    private companion object {
        val DEVICE_CODE =
            """
            {
              "device_code": "dev-code",
              "user_code": "ABCD-1234",
              "verification_uri": "https://github.com/login/device",
              "expires_in": 900,
              "interval": 5
            }
            """.trimIndent()
    }
}
