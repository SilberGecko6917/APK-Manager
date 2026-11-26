package me.geckotv.apkmanager.core.model

/**
 * Regular expressions that narrow down which release assets count for an app, e.g. include `foss`
 * or exclude `debug|beta`. Matched case-insensitively against the file name; an invalid pattern is
 * ignored.
 */
data class AssetFilter(
    val include: String? = null,
    val exclude: String? = null,
) {
    private val includeRegex: Regex? = include?.toRegexOrNull()
    private val excludeRegex: Regex? = exclude?.toRegexOrNull()

    /** Whether no pattern is set. */
    val isEmpty: Boolean get() = include.isNullOrBlank() && exclude.isNullOrBlank()

    /** Whether the asset [name] passes the filter. */
    fun matches(name: String): Boolean =
        (includeRegex == null || includeRegex.containsMatchIn(name)) &&
            (excludeRegex == null || !excludeRegex.containsMatchIn(name))
}

/** The release that APK Manager installed for an app, and the version code that APK had. */
data class InstalledRelease(
    val releaseId: Long,
    val versionCode: Long,
)

/**
 * Extracts the version from a release tag with [pattern]: the first capturing group if there is one,
 * otherwise the whole match. Returns [tag] unchanged if the pattern is missing, invalid or does not
 * match.
 */
fun versionFromTag(
    tag: String,
    pattern: String?,
): String {
    val match = pattern?.toRegexOrNull()?.find(tag) ?: return tag
    return match.groupValues.getOrNull(1)?.takeIf(String::isNotEmpty) ?: match.value
}

/** Compiles a user-entered pattern case-insensitively, or `null` if it is blank or invalid. */
fun String.toRegexOrNull(): Regex? =
    takeIf(String::isNotBlank)?.let { pattern ->
        try {
            Regex(pattern, RegexOption.IGNORE_CASE)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
