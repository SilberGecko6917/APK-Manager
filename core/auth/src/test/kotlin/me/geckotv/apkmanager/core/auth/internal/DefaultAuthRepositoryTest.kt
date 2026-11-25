package me.geckotv.apkmanager.core.auth.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.auth.GitHubOAuthConfig
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.auth.SignInResult
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkAuthenticatedUser
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import me.geckotv.apkmanager.core.network.model.NetworkUser
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthRepositoryTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val network = FakeNetwork()

    // Lazy: the temporary folder only exists once the rule has been applied.
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { tmpFolder.newFile("credentials.preferences_pb") },
        )
    }

    private val repository by lazy {
        DefaultAuthRepository(
            deviceFlowClient = DeviceFlowClient(unusedOAuthApi(), GitHubOAuthConfig("client"), Clock.systemUTC()),
            credentialsStore = CredentialsStore(dataStore, ReversingCipher),
            network = network,
            applicationScope = testScope.backgroundScope,
        )
    }

    @Test
    fun signInWithTokenStoresTheEncryptedTokenAndTheAccount() =
        testScope.runTest {
            network.result = success(scopes = setOf("repo"))

            val result = repository.signInWithToken("  ghp_token  ")

            val account = (result as SignInResult.Success).account
            assertEquals("octocat", account.login)
            assertEquals(AuthMethod.PersonalAccessToken, account.authMethod)
            assertEquals(GitHubAccessLevel.Private, account.accessLevel)
            assertEquals(AuthState.SignedIn(account), repository.authState.first())
            assertEquals("ghp_token", repository.token())
            assertEquals("ghp_token", network.lastToken)
            // Only the encrypted form is written to disk.
            assertNotEquals("ghp_token", dataStore.data.first()[stringPreferencesKey("token")])
        }

    @Test
    fun fineGrainedTokensHaveAnUnknownAccessLevel() =
        testScope.runTest {
            network.result = success(scopes = null)

            val account = (repository.signInWithToken("github_pat_x") as SignInResult.Success).account

            assertNull(account.accessLevel)
        }

    @Test
    fun rejectedTokensAreNotStored() =
        testScope.runTest {
            network.result = GitHubResult.Failure(GitHubError.Unauthorized)

            assertEquals(SignInResult.Failed(SignInFailure.InvalidToken), repository.signInWithToken("bad"))
            assertEquals(AuthState.SignedOut, repository.authState.first())
            assertNull(repository.token())
        }

    @Test
    fun refreshMarksARevokedTokenInvalid() =
        testScope.runTest {
            network.result = success(scopes = emptySet())
            repository.signInWithToken("ghp_token")
            network.result = GitHubResult.Failure(GitHubError.Unauthorized)

            repository.refreshAccount()

            assertEquals(true, (repository.authState.first() as AuthState.SignedIn).tokenInvalid)
        }

    @Test
    fun signOutForgetsTheToken() =
        testScope.runTest {
            network.result = success(scopes = emptySet())
            repository.signInWithToken("ghp_token")

            repository.signOut()

            assertEquals(AuthState.SignedOut, repository.authState.first())
            assertNull(repository.token())
        }

    @Test
    fun tokensThatCannotBeDecryptedCountAsSignedOut() =
        testScope.runTest {
            val store = CredentialsStore(dataStore, ReversingCipher)
            store.save("ghp_token", fakeAccount)
            val unreadable = CredentialsStore(dataStore, BrokenCipher)

            assertNull(unreadable.credentials.first())
        }

    private fun success(scopes: Set<String>?): GitHubResult<NetworkAuthenticatedUser> =
        GitHubResult.Success(NetworkAuthenticatedUser(NetworkUser("octocat", "The Octocat"), scopes))

    private fun unusedOAuthApi(): GitHubOAuthApi =
        Retrofit
            .Builder()
            .baseUrl("https://github.invalid/")
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubOAuthApi::class.java)

    private object ReversingCipher : TokenCipher {
        override fun encrypt(plaintext: String) = plaintext.reversed()

        override fun decrypt(ciphertext: String) = ciphertext.reversed()
    }

    private object BrokenCipher : TokenCipher {
        override fun encrypt(plaintext: String) = plaintext

        override fun decrypt(ciphertext: String): String? = null
    }

    private val fakeAccount =
        me.geckotv.apkmanager.core.model.GitHubAccount(
            login = "octocat",
            name = null,
            avatarUrl = null,
            authMethod = AuthMethod.OAuth,
            accessLevel = GitHubAccessLevel.Public,
        )

    private class FakeNetwork : GitHubNetworkDataSource {
        var result: GitHubResult<NetworkAuthenticatedUser> = GitHubResult.Failure(GitHubError.NotFound)
        var lastToken: String? = null

        override suspend fun authenticatedUser(token: String): GitHubResult<NetworkAuthenticatedUser> {
            lastToken = token
            return result
        }

        override suspend fun repository(repository: RepositoryRef): GitHubResult<NetworkRepository> = error("unused")

        override suspend fun latestRelease(
            repository: RepositoryRef,
            etag: String?,
        ): GitHubResult<NetworkRelease> = error("unused")

        override suspend fun releases(
            repository: RepositoryRef,
            perPage: Int,
            page: Int,
            etag: String?,
        ): GitHubResult<List<NetworkRelease>> = error("unused")

        override suspend fun latestReleases(
            repositories: List<RepositoryRef>,
        ): GitHubResult<Map<RepositoryRef, NetworkReleaseSummary?>> = error("unused")

        override suspend fun downloadAsset(
            repository: RepositoryRef,
            assetId: Long,
        ): GitHubResult<ResponseBody> = error("unused")
    }
}
