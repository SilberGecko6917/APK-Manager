package me.geckotv.apkmanager.core.installer

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.installer.internal.ApkDownloader
import me.geckotv.apkmanager.core.installer.internal.DownloadResult
import me.geckotv.apkmanager.core.installer.internal.sha256
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkAuthenticatedUser
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import me.geckotv.apkmanager.core.testing.TestData
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkDownloaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val content = ByteArray(1_000_000) { (it % 251).toByte() }
    private val network = FakeNetwork()
    private val repository = GitHubRepository("octo", "app")
    private val asset = TestData.asset(id = 7, name = "app.apk", size = content.size.toLong())

    @Test
    fun download_writesFileAndComputesChecksum() =
        runTest {
            network.result = GitHubResult.Success(content.toResponseBody())
            val progress = mutableListOf<Pair<Long, Long?>>()

            val result =
                downloader().download(repository, asset) { read, total -> progress += read to total }

            result as DownloadResult.Success
            assertEquals(content.size.toLong(), result.size)
            assertEquals(content.sha256(), result.sha256)
            assertArrayEquals(content, result.file.readBytes())
            assertEquals(RepositoryRef("octo", "app") to 7L, network.requested)
            assertEquals(content.size.toLong() to content.size.toLong(), progress.last())
            assertEquals(progress.map { it.first }.sorted(), progress.map { it.first })
        }

    @Test
    fun download_apiError_isReported() =
        runTest {
            network.result = GitHubResult.Failure(GitHubError.NotFound)

            val result = downloader().download(repository, asset) { _, _ -> }

            assertEquals(DownloadResult.Failure(InstallFailure.DownloadFailed(SourceError.NotFound)), result)
            assertTrue(
                folder.root
                    .resolve("downloads")
                    .listFiles()
                    .orEmpty()
                    .isEmpty(),
            )
        }

    private fun TestScope.downloader() =
        ApkDownloader(network, folder.root.resolve("downloads"), StandardTestDispatcher(testScheduler))

    private class FakeNetwork : GitHubNetworkDataSource {
        var result: GitHubResult<ResponseBody> = GitHubResult.Failure(GitHubError.NotFound)
        var requested: Pair<RepositoryRef, Long>? = null

        override suspend fun downloadAsset(
            repository: RepositoryRef,
            assetId: Long,
        ): GitHubResult<ResponseBody> {
            requested = repository to assetId
            return result
        }

        override suspend fun authenticatedUser(token: String): GitHubResult<NetworkAuthenticatedUser> =
            throw UnsupportedOperationException()

        override suspend fun repository(repository: RepositoryRef): GitHubResult<NetworkRepository> =
            throw UnsupportedOperationException()

        override suspend fun latestRelease(
            repository: RepositoryRef,
            etag: String?,
        ): GitHubResult<NetworkRelease> = throw UnsupportedOperationException()

        override suspend fun releases(
            repository: RepositoryRef,
            perPage: Int,
            page: Int,
            etag: String?,
        ): GitHubResult<List<NetworkRelease>> = throw UnsupportedOperationException()

        override suspend fun latestReleases(
            repositories: List<RepositoryRef>,
        ): GitHubResult<Map<RepositoryRef, NetworkReleaseSummary?>> = throw UnsupportedOperationException()
    }
}
