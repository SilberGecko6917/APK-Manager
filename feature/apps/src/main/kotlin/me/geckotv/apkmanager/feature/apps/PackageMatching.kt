package me.geckotv.apkmanager.feature.apps

import me.geckotv.apkmanager.core.model.InstalledApp

private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]")

private fun normalize(text: String): String = text.replace(NON_ALPHANUMERIC, "").lowercase()

/**
 * The installed app that was most likely built from the repository [repositoryName]: the only
 * non-system app whose label or package name contains the name (ignoring case, spaces, hyphens and
 * other punctuation). `null` if there is no such app or if several match.
 */
internal fun suggestInstalledApp(
    repositoryName: String,
    installedApps: List<InstalledApp>,
): InstalledApp? {
    val needle = normalize(repositoryName)
    val matches =
        if (needle.isEmpty()) {
            emptyList()
        } else {
            installedApps.filter { app ->
                !app.isSystemApp && (needle in normalize(app.label) || needle in normalize(app.packageName))
            }
        }
    return matches.singleOrNull()
}

/**
 * The apps to choose from, sorted by label: non-system apps, or all apps with [includeSystemApps],
 * that contain [query] in their label or package name (ignoring case).
 */
internal fun filterInstalledApps(
    installedApps: List<InstalledApp>,
    query: String,
    includeSystemApps: Boolean,
): List<InstalledApp> {
    val trimmed = query.trim()
    return installedApps
        .filter { includeSystemApps || !it.isSystemApp }
        .filter {
            trimmed.isEmpty() ||
                it.label.contains(trimmed, ignoreCase = true) ||
                it.packageName.contains(trimmed, ignoreCase = true)
        }.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
}
