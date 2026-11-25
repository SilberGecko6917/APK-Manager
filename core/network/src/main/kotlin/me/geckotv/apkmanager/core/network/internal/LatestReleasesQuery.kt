package me.geckotv.apkmanager.core.network.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.RepositoryRef

@Serializable
internal data class GraphQlRequest(
    val query: String,
    val variables: Map<String, String>,
)

/**
 * Builds one GraphQL query that fetches the latest release of many repositories at once. Each
 * repository gets an alias (`r0`, `r1`, …); owners and names are passed as variables, never
 * interpolated into the query.
 */
internal object LatestReleasesQuery {
    fun build(repositories: List<RepositoryRef>): GraphQlRequest {
        val variableDefinitions =
            repositories.indices.joinToString(", ") { index -> "\$o$index: String!, \$n$index: String!" }
        val fields =
            repositories.indices.joinToString("\n") { index ->
                "  r$index: repository(owner: \$o$index, name: \$n$index) { ...LatestRelease }"
            }
        val query =
            """
            |query($variableDefinitions) {
            |$fields
            |}
            |fragment LatestRelease on Repository {
            |  latestRelease { databaseId tagName name isPrerelease publishedAt url }
            |}
            """.trimMargin()
        val variables =
            buildMap {
                repositories.forEachIndexed { index, repository ->
                    put("o$index", repository.owner)
                    put("n$index", repository.name)
                }
            }
        return GraphQlRequest(query = query, variables = variables)
    }

    /**
     * Maps the response `data` to the repositories in request order. A repository that does not
     * exist (or is not visible) and one without releases both map to `null`.
     */
    fun parse(
        repositories: List<RepositoryRef>,
        data: JsonObject,
    ): Map<RepositoryRef, NetworkReleaseSummary?> =
        repositories
            .mapIndexed { index, repository ->
                val release = data["r$index"].objectOrNull()?.get("latestRelease").objectOrNull()
                repository to release?.toSummary()
            }.toMap()

    private fun JsonObject.toSummary(): NetworkReleaseSummary? {
        val id = this["databaseId"].primitiveOrNull()?.longOrNull
        val tagName = this["tagName"].primitiveOrNull()?.contentOrNull
        if (id == null || tagName == null) return null
        return NetworkReleaseSummary(
            id = id,
            tagName = tagName,
            name = this["name"].primitiveOrNull()?.contentOrNull,
            isPrerelease = this["isPrerelease"].primitiveOrNull()?.booleanOrNull ?: false,
            publishedAt = this["publishedAt"].primitiveOrNull()?.contentOrNull,
            htmlUrl = this["url"].primitiveOrNull()?.contentOrNull.orEmpty(),
        )
    }

    private fun JsonElement?.objectOrNull(): JsonObject? = if (this == null || this is JsonNull) null else jsonObject

    private fun JsonElement?.primitiveOrNull(): JsonPrimitive? =
        if (this == null || this is JsonNull) null else jsonPrimitive
}
