package me.geckotv.apkmanager.core.network.internal

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/** [GitHubNetworkDataSource] backed by Retrofit. */
internal class RetrofitGitHubNetwork
    @Inject
    constructor(
        private val api: GitHubApi,
        private val credentialsProvider: GitHubCredentialsProvider,
    ) : GitHubNetworkDataSource {
        override suspend fun repository(repository: RepositoryRef): GitHubResult<NetworkRepository> =
            call { api.repository(repository.owner, repository.name) }

        override suspend fun latestRelease(
            repository: RepositoryRef,
            etag: String?,
        ): GitHubResult<NetworkRelease> = call { api.latestRelease(repository.owner, repository.name, etag) }

        override suspend fun releases(
            repository: RepositoryRef,
            perPage: Int,
            page: Int,
            etag: String?,
        ): GitHubResult<List<NetworkRelease>> =
            call { api.releases(repository.owner, repository.name, perPage.coerceIn(1, MAX_PAGE_SIZE), page, etag) }

        override suspend fun latestReleases(
            repositories: List<RepositoryRef>,
        ): GitHubResult<Map<RepositoryRef, NetworkReleaseSummary?>> {
            if (credentialsProvider.token().isNullOrBlank()) return GitHubResult.Failure(GitHubError.Unauthorized)

            val releases = mutableMapOf<RepositoryRef, NetworkReleaseSummary?>()
            val failure =
                repositories.distinct().chunked(GRAPHQL_BATCH_SIZE).firstNotNullOfOrNull { batch ->
                    when (val result = latestReleasesBatch(batch)) {
                        is GitHubResult.Success -> {
                            releases += result.data
                            null
                        }

                        is GitHubResult.Failure -> {
                            result
                        }

                        GitHubResult.NotModified -> {
                            GitHubResult.Failure(GitHubError.Unexpected(NOT_MODIFIED, null))
                        }
                    }
                }
            return failure ?: GitHubResult.Success(releases)
        }

        private suspend fun latestReleasesBatch(
            batch: List<RepositoryRef>,
        ): GitHubResult<Map<RepositoryRef, NetworkReleaseSummary?>> =
            when (val result = call { api.graphql(LatestReleasesQuery.build(batch)) }) {
                is GitHubResult.Success -> {
                    val data = result.data["data"]
                    if (data == null || data is JsonNull) {
                        result.data.graphQlFailure()
                    } else {
                        GitHubResult.Success(LatestReleasesQuery.parse(batch, data.jsonObject))
                    }
                }

                is GitHubResult.Failure -> {
                    result
                }

                GitHubResult.NotModified -> {
                    GitHubResult.Failure(GitHubError.Unexpected(NOT_MODIFIED, null))
                }
            }

        override suspend fun downloadAsset(
            repository: RepositoryRef,
            assetId: Long,
        ): GitHubResult<ResponseBody> = call { api.asset(repository.owner, repository.name, assetId) }

        private suspend fun <T : Any> call(request: suspend () -> Response<T>): GitHubResult<T> =
            try {
                request().toResult()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                GitHubResult.Failure(GitHubError.Network(exception))
            } catch (exception: SerializationException) {
                GitHubResult.Failure(GitHubError.Unexpected(code = null, message = exception.message))
            }

        /** A GraphQL response without `data` failed as a whole, e.g. because the query was rejected. */
        private fun JsonObject.graphQlFailure(): GitHubResult.Failure {
            val message =
                (this["errors"] as? JsonArray)
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.toString()
            return GitHubResult.Failure(GitHubError.Unexpected(code = null, message = message))
        }

        private companion object {
            const val MAX_PAGE_SIZE = 100
            const val GRAPHQL_BATCH_SIZE = 50
            const val NOT_MODIFIED = 304
        }
    }

/** Maps an HTTP response to a [GitHubResult], classifying errors the way GitHub reports them. */
internal fun <T : Any> Response<T>.toResult(): GitHubResult<T> {
    val body = body()
    return when {
        isSuccessful && body != null -> GitHubResult.Success(body, etag = headers()["ETag"])
        code() == HTTP_NOT_MODIFIED -> GitHubResult.NotModified
        else -> GitHubResult.Failure(toError())
    }
}

private fun Response<*>.toError(): GitHubError {
    val headers = headers()
    val code = code()
    val remaining = headers["x-ratelimit-remaining"]?.toIntOrNull()
    val retryAfter = headers["retry-after"]?.toLongOrNull()
    val resetAt = headers["x-ratelimit-reset"]?.toLongOrNull()?.let(Instant::ofEpochSecond)
    val errorMessage = errorBody()?.use { it.string() }
    return when {
        code == HTTP_UNAUTHORIZED -> {
            GitHubError.Unauthorized
        }

        code == HTTP_NOT_FOUND -> {
            GitHubError.NotFound
        }

        // Secondary rate limits send retry-after; primary ones send x-ratelimit-remaining: 0.
        code == HTTP_TOO_MANY_REQUESTS || (code == HTTP_FORBIDDEN && (remaining == 0 || retryAfter != null)) -> {
            GitHubError.RateLimited(
                retryAt = retryAfter?.let { Instant.now().plusSeconds(it) } ?: resetAt,
            )
        }

        code == HTTP_FORBIDDEN -> {
            GitHubError.Forbidden(errorMessage)
        }

        code >= HTTP_SERVER_ERROR -> {
            GitHubError.Server(code)
        }

        else -> {
            GitHubError.Unexpected(code, errorMessage)
        }
    }
}

private const val HTTP_NOT_MODIFIED = 304
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500
