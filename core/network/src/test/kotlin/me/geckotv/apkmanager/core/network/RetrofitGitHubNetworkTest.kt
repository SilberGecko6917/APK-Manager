package me.geckotv.apkmanager.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.geckotv.apkmanager.core.network.internal.RetrofitGitHubNetwork
import me.geckotv.apkmanager.core.network.internal.createGitHubApi
import me.geckotv.apkmanager.core.network.internal.createGitHubOkHttpClient
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class RetrofitGitHubNetworkTest {
    private val server = MockWebServer()
    private val rateLimitTracker = RateLimitTracker()
    private val json = Json { ignoreUnknownKeys = true }
    private var token: String? = null
    private lateinit var network: RetrofitGitHubNetwork

    private val repository = RepositoryRef("octo", "app")

    @Before
    fun setUp() {
        server.start()
        val credentials = GitHubCredentialsProvider { token }
        val client = createGitHubOkHttpClient(server.hostName, credentials, rateLimitTracker)
        network = RetrofitGitHubNetwork(createGitHubApi(server.url("/").toString(), client, json), credentials)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun latestReleaseParsesTheReleaseAndSendsGitHubHeaders() =
        runTest {
            server.enqueue(json(RELEASE_JSON, "ETag" to "\"abc\""))

            val result = network.latestRelease(repository)

            val success = result as GitHubResult.Success
            assertEquals("v1.2.0", success.data.tagName)
            assertEquals("\"abc\"", success.etag)
            val asset = success.data.assets.single()
            assertEquals("app-arm64-v8a-release.apk", asset.name)
            assertEquals("sha256:0123", asset.digest)
            assertEquals(5_000_000L, asset.size)

            val request = server.takeRequest()
            assertEquals("/repos/octo/app/releases/latest", request.url.encodedPath)
            assertEquals("application/vnd.github+json", request.headers["Accept"])
            assertEquals("2022-11-28", request.headers["X-GitHub-Api-Version"])
            assertTrue(request.headers["User-Agent"].orEmpty().startsWith("APK-Manager"))
            assertNull(request.headers["Authorization"])
            assertNull(request.headers["If-None-Match"])
        }

    @Test
    fun sendsTheTokenWhenSignedIn() =
        runTest {
            token = "secret-token"
            server.enqueue(json(RELEASE_JSON))

            network.latestRelease(repository)

            assertEquals("Bearer secret-token", server.takeRequest().headers["Authorization"])
        }

    @Test
    fun authenticatedUserSendsTheGivenTokenAndReadsTheScopes() =
        runTest {
            token = "stored-token"
            server.enqueue(json("""{"login":"octocat","name":"The Octocat"}""", "X-OAuth-Scopes" to "repo, read:org"))
            server.enqueue(json("""{"login":"octocat"}"""))

            val classic = (network.authenticatedUser("new-token") as GitHubResult.Success).data
            val fineGrained = (network.authenticatedUser("fine-grained") as GitHubResult.Success).data

            assertEquals("octocat", classic.user.login)
            assertEquals(setOf("repo", "read:org"), classic.scopes)
            assertNull(fineGrained.scopes)
            // The explicit token wins over the stored one.
            assertEquals("Bearer new-token", server.takeRequest().headers["Authorization"])
        }

    @Test
    fun returnsNotModifiedForAMatchingEtag() =
        runTest {
            server.enqueue(MockResponse.Builder().code(304).build())

            val result = network.latestRelease(repository, etag = "\"abc\"")

            assertEquals(GitHubResult.NotModified, result)
            assertEquals("\"abc\"", server.takeRequest().headers["If-None-Match"])
        }

    @Test
    fun classifiesErrors() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(404)
                    .body("""{"message":"Not Found"}""")
                    .build(),
            )
            server.enqueue(MockResponse.Builder().code(401).build())
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(403)
                    .setHeader("x-ratelimit-remaining", 0)
                    .setHeader("x-ratelimit-reset", 1_900_000_000)
                    .build(),
            )
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(429)
                    .setHeader("retry-after", 60)
                    .build(),
            )
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(403)
                    .body("""{"message":"Resource protected"}""")
                    .build(),
            )
            server.enqueue(MockResponse.Builder().code(502).build())

            assertEquals(GitHubResult.Failure(GitHubError.NotFound), network.latestRelease(repository))
            assertEquals(GitHubResult.Failure(GitHubError.Unauthorized), network.latestRelease(repository))
            assertEquals(
                GitHubResult.Failure(GitHubError.RateLimited(Instant.ofEpochSecond(1_900_000_000))),
                network.latestRelease(repository),
            )
            val secondary = (network.latestRelease(repository) as GitHubResult.Failure).error
            assertTrue(secondary is GitHubError.RateLimited && secondary.retryAt != null)
            val forbidden = (network.latestRelease(repository) as GitHubResult.Failure).error
            assertTrue(forbidden is GitHubError.Forbidden && forbidden.message.orEmpty().contains("Resource protected"))
            assertEquals(GitHubResult.Failure(GitHubError.Server(502)), network.latestRelease(repository))
        }

    @Test
    fun tracksRateLimitHeaders() =
        runTest {
            server.enqueue(
                json(
                    RELEASE_JSON,
                    "x-ratelimit-limit" to "5000",
                    "x-ratelimit-remaining" to "4999",
                    "x-ratelimit-reset" to "1900000000",
                    "x-ratelimit-resource" to "core",
                ),
            )

            network.latestRelease(repository)

            val core = rateLimitTracker.rateLimits.value.getValue("core")
            assertEquals(5000, core.limit)
            assertEquals(4999, core.remaining)
            assertEquals(Instant.ofEpochSecond(1_900_000_000), core.resetAt)
        }

    @Test
    fun latestReleasesRequiresAToken() =
        runTest {
            val result = network.latestReleases(listOf(repository))

            assertEquals(GitHubResult.Failure(GitHubError.Unauthorized), result)
            assertEquals(0, server.requestCount)
        }

    @Test
    fun latestReleasesBatchesRepositoriesInOneGraphQlQuery() =
        runTest {
            token = "secret-token"
            val missing = RepositoryRef("octo", "gone")
            server.enqueue(
                json(
                    """
                    {
                      "data": {
                        "r0": { "latestRelease": { "databaseId": 42, "tagName": "v2.0.0", "name": "2.0",
                                "isPrerelease": false, "publishedAt": "2026-11-01T10:00:00Z",
                                "url": "https://github.com/octo/app/releases/tag/v2.0.0" } },
                        "r1": null
                      },
                      "errors": [ { "type": "NOT_FOUND", "path": ["r1"], "message": "Could not resolve" } ]
                    }
                    """.trimIndent(),
                ),
            )

            val result = network.latestReleases(listOf(repository, missing))

            val releases = (result as GitHubResult.Success).data
            assertEquals("v2.0.0", releases.getValue(repository)?.tagName)
            assertEquals(42L, releases.getValue(repository)?.id)
            assertNull(releases.getValue(missing))

            val request = server.takeRequest()
            assertEquals("/graphql", request.url.encodedPath)
            val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
            val variables = body.getValue("variables").jsonObject
            assertEquals("octo", variables.getValue("o0").jsonPrimitive.content)
            assertEquals("app", variables.getValue("n0").jsonPrimitive.content)
            assertEquals("gone", variables.getValue("n1").jsonPrimitive.content)
            // Owners and names are passed as variables, never interpolated into the query.
            assertTrue("octo" !in body.getValue("query").jsonPrimitive.content)
        }

    @Test
    fun assetDownloadDoesNotForwardTheTokenToTheStorageHost() =
        runTest {
            token = "secret-token"
            MockWebServer().use { storage ->
                storage.start()
                storage.enqueue(MockResponse.Builder().body("apk-bytes").build())
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(302)
                        .setHeader("Location", storage.url("/signed/app.apk?token=x"))
                        .build(),
                )

                val result = network.downloadAsset(repository, assetId = 7)

                val body = (result as GitHubResult.Success).data
                assertEquals("apk-bytes", body.use { it.string() })

                val apiRequest = server.takeRequest()
                assertEquals("/repos/octo/app/releases/assets/7", apiRequest.url.encodedPath)
                assertEquals("application/octet-stream", apiRequest.headers["Accept"])
                assertEquals("Bearer secret-token", apiRequest.headers["Authorization"])

                val storageRequest = storage.takeRequest()
                assertEquals("/signed/app.apk", storageRequest.url.encodedPath)
                assertNull(storageRequest.headers["Authorization"])
            }
        }

    private fun json(
        body: String,
        vararg headers: Pair<String, String>,
    ): MockResponse =
        MockResponse
            .Builder()
            .setHeader("Content-Type", "application/json")
            .apply { headers.forEach { (name, value) -> setHeader(name, value) } }
            .body(body)
            .build()

    private companion object {
        val RELEASE_JSON =
            """
            {
              "id": 1,
              "tag_name": "v1.2.0",
              "name": "1.2.0",
              "body": "Changelog",
              "draft": false,
              "prerelease": false,
              "published_at": "2026-11-20T12:00:00Z",
              "html_url": "https://github.com/octo/app/releases/tag/v1.2.0",
              "unknown_field": true,
              "assets": [
                {
                  "id": 7,
                  "name": "app-arm64-v8a-release.apk",
                  "content_type": "application/vnd.android.package-archive",
                  "size": 5000000,
                  "digest": "sha256:0123",
                  "browser_download_url": "https://github.com/octo/app/releases/download/v1.2.0/app.apk",
                  "url": "https://api.github.com/repos/octo/app/releases/assets/7"
                }
              ]
            }
            """.trimIndent()
    }
}
