package me.geckotv.apkmanager.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A repository as returned by `GET /repos/{owner}/{repo}`. */
@Serializable
data class NetworkRepository(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: NetworkOwner,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("stargazers_count") val stars: Int = 0,
    val archived: Boolean = false,
    val topics: List<String> = emptyList(),
)

@Serializable
data class NetworkOwner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/** The signed-in user as returned by `GET /user`. */
@Serializable
data class NetworkUser(
    val login: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * The user a token belongs to.
 *
 * @property scopes OAuth scopes of the token from the `X-OAuth-Scopes` header, or `null` when GitHub
 *   does not report scopes (fine-grained personal access tokens).
 */
data class NetworkAuthenticatedUser(
    val user: NetworkUser,
    val scopes: Set<String>?,
)

/** A release as returned by the REST releases endpoints. */
@Serializable
data class NetworkRelease(
    val id: Long,
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<NetworkReleaseAsset> = emptyList(),
)

/** A file attached to a release. */
@Serializable
data class NetworkReleaseAsset(
    val id: Long,
    val name: String,
    @SerialName("content_type") val contentType: String? = null,
    val size: Long = 0,
    /** Checksum in the form `sha256:<hex>`, provided by GitHub for newer assets. */
    val digest: String? = null,
    @SerialName("download_count") val downloadCount: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    /** Public download URL; does not work for private repositories. */
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    /** API URL of the asset; used with `Accept: application/octet-stream` to download private assets. */
    val url: String,
)

/** The latest release of a repository as returned by the batched GraphQL query. */
data class NetworkReleaseSummary(
    val id: Long,
    val tagName: String,
    val name: String?,
    val isPrerelease: Boolean,
    val publishedAt: String?,
    val htmlUrl: String,
)

/** Owner and name of a repository, e.g. `SilberGecko6917/APK-Manager`. */
data class RepositoryRef(
    val owner: String,
    val name: String,
) {
    override fun toString(): String = "$owner/$name"
}
