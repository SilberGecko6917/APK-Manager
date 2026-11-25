package me.geckotv.apkmanager.core.installer.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.inject.Inject

/** Result of downloading an APK. */
internal sealed interface DownloadResult {
    /** The APK is in [file]; the caller deletes it when it is no longer needed. */
    data class Success(
        val file: File,
        val size: Long,
        val sha256: String,
    ) : DownloadResult

    data class Failure(
        val failure: InstallFailure,
    ) : DownloadResult
}

/** Downloads release assets. */
internal interface AssetDownloader {
    /**
     * Downloads [asset] of [repository] into a new file, reporting `(bytesRead, totalBytes)` to
     * [onProgress]. Nothing is left behind if the download fails.
     */
    suspend fun download(
        repository: GitHubRepository,
        asset: ReleaseAsset,
        onProgress: (Long, Long?) -> Unit,
    ): DownloadResult
}

/**
 * Downloads release assets through the GitHub API, which works for private repositories too, and
 * computes the SHA-256 digest while writing the file.
 */
internal class ApkDownloader
    @Inject
    constructor(
        private val network: GitHubNetworkDataSource,
        @DownloadDirectory private val directory: File,
        @Dispatcher(ApkManagerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : AssetDownloader {
        override suspend fun download(
            repository: GitHubRepository,
            asset: ReleaseAsset,
            onProgress: (Long, Long?) -> Unit,
        ): DownloadResult =
            withContext(ioDispatcher) {
                val target = File(directory, "${repository.owner}-${repository.name}-${asset.id}.apk")
                when (val result = network.downloadAsset(RepositoryRef(repository.owner, repository.name), asset.id)) {
                    is GitHubResult.Success -> {
                        result.data.use { body -> write(body, asset, target, onProgress) }
                    }

                    is GitHubResult.Failure -> {
                        DownloadResult.Failure(
                            InstallFailure.DownloadFailed(result.error.toSourceError()),
                        )
                    }

                    GitHubResult.NotModified -> {
                        DownloadResult.Failure(
                            InstallFailure.DownloadFailed(SourceError.Unexpected(null)),
                        )
                    }
                }
            }

        private suspend fun write(
            body: ResponseBody,
            asset: ReleaseAsset,
            target: File,
            onProgress: (Long, Long?) -> Unit,
        ): DownloadResult {
            val total = body.contentLength().takeIf { it > 0 } ?: asset.size.takeIf { it > 0 }
            return try {
                target.parentFile?.mkdirs()
                val (size, sha256) =
                    body.byteStream().use { input ->
                        target.outputStream().use { output -> copy(input, output) { read -> onProgress(read, total) } }
                    }
                onProgress(size, total)
                DownloadResult.Success(file = target, size = size, sha256 = sha256)
            } catch (exception: IOException) {
                target.delete()
                DownloadResult.Failure(exception.toInstallFailure())
            }
        }

        /** Copies [input] to [output] and returns the byte count and SHA-256 digest. */
        private suspend fun copy(
            input: InputStream,
            output: OutputStream,
            onProgress: (Long) -> Unit,
        ): Pair<Long, String> {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead = 0L
            var reported = 0L
            var count = input.read(buffer)
            while (count >= 0) {
                currentCoroutineContext().ensureActive()
                output.write(buffer, 0, count)
                digest.update(buffer, 0, count)
                bytesRead += count
                if (bytesRead - reported >= PROGRESS_STEP) {
                    reported = bytesRead
                    onProgress(bytesRead)
                }
                count = input.read(buffer)
            }
            return bytesRead to digest.digest().toHexString()
        }

        private fun IOException.toInstallFailure(): InstallFailure =
            if (message?.contains("ENOSPC") == true || message?.contains("No space left") == true) {
                InstallFailure.Storage
            } else {
                InstallFailure.DownloadFailed(SourceError.Offline)
            }

        private companion object {
            const val BUFFER_SIZE = 64 * 1024
            const val PROGRESS_STEP = 256 * 1024L
        }
    }

internal fun GitHubError.toSourceError(): SourceError =
    when (this) {
        GitHubError.NotFound -> SourceError.NotFound
        GitHubError.Unauthorized -> SourceError.Unauthorized
        is GitHubError.Forbidden -> SourceError.Forbidden(message)
        is GitHubError.RateLimited -> SourceError.RateLimited(retryAt)
        is GitHubError.Server -> SourceError.Unavailable
        is GitHubError.Network -> SourceError.Offline
        is GitHubError.Unexpected -> SourceError.Unexpected(message)
    }
