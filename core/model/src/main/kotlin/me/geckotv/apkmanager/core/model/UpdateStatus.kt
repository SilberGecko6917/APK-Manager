package me.geckotv.apkmanager.core.model

/** Whether a tracked app can be updated. */
enum class UpdateStatus {
    /** The latest release is newer than the installed version. */
    UpdateAvailable,

    /** The installed version is the latest release or newer. */
    UpToDate,

    /** The app is not installed, or its package name is not known yet. */
    NotInstalled,

    /** The app is installed, but no release was found (yet). */
    NoRelease,

    /** The installed version and the release tag cannot be compared, e.g. tag `nightly`. */
    Unknown,
    ;

    companion object {
        /**
         * Compares the installed version name with the latest release tag using [Version].
         *
         * - Not installed → [NotInstalled]; no release → [NoRelease].
         * - Both versions parse → [UpdateAvailable] if the release is newer, otherwise [UpToDate].
         * - Otherwise, [UpToDate] if the normalized strings are equal (e.g. tag `v-nightly-1`, version
         *   name `nightly-1`), else [Unknown].
         */
        fun of(
            installedApp: InstalledApp?,
            latestRelease: Release?,
        ): UpdateStatus {
            val installedName = installedApp?.versionName
            val installed = installedName?.let(Version::parse)
            val latest = latestRelease?.let { Version.parse(it.tagName) }
            return when {
                installedApp == null -> NotInstalled
                latestRelease == null -> NoRelease
                installed != null && latest != null -> if (latest > installed) UpdateAvailable else UpToDate
                installedName != null && normalize(installedName) == normalize(latestRelease.tagName) -> UpToDate
                else -> Unknown
            }
        }

        private val LEADING_V = Regex("^v(?=[^a-z])")
        private val SEPARATORS = Regex("[-_. ]")

        private fun normalize(text: String): String =
            text
                .trim()
                .lowercase()
                .replace(LEADING_V, "")
                .replace(SEPARATORS, "")
    }
}
