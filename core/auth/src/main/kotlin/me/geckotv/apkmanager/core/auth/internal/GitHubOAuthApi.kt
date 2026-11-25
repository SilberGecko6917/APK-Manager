package me.geckotv.apkmanager.core.auth.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

/** GitHub's OAuth device flow endpoints on `github.com`. */
internal interface GitHubOAuthApi {
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String,
    ): DeviceCodeResponse

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun requestAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = DEVICE_CODE_GRANT_TYPE,
    ): AccessTokenResponse

    companion object {
        const val DEVICE_CODE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
    }
}

@Serializable
internal data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String? = null,
    @SerialName("user_code") val userCode: String? = null,
    @SerialName("verification_uri") val verificationUri: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    val interval: Int = 5,
    val error: String? = null,
)

@Serializable
internal data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val interval: Int? = null,
)
