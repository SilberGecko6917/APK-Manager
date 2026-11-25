package me.geckotv.apkmanager.core.network.internal

import kotlinx.serialization.json.JsonObject
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Retrofit definition of the GitHub REST and GraphQL endpoints the app uses. */
internal interface GitHubApi {
    @GET("repos/{owner}/{repo}")
    suspend fun repository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Response<NetworkRepository>

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("If-None-Match") etag: String?,
    ): Response<NetworkRelease>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun releases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
        @Header("If-None-Match") etag: String?,
    ): Response<List<NetworkRelease>>

    /** Downloads an asset. GitHub answers with a redirect to the storage host, which OkHttp follows. */
    @Streaming
    @GET("repos/{owner}/{repo}/releases/assets/{assetId}")
    suspend fun asset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("assetId") assetId: Long,
        @Header("Accept") accept: String = "application/octet-stream",
    ): Response<ResponseBody>

    @POST("graphql")
    suspend fun graphql(
        @Body request: GraphQlRequest,
    ): Response<JsonObject>
}
