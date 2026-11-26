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
         * Decides whether [latestRelease] is an update of [installedApp].
         *
         * - Not installed → [NotInstalled]; no release → [NoRelease]; the release was skipped
         *   ([skippedReleaseId]) → [UpToDate].
         * - APK Manager installed the app ([installedRelease]) and it was not changed since (same
         *   version code): [UpToDate] for the same release; for another release, the version
         *   comparison below decides, and [UpdateAvailable] if it cannot. This makes tags without a
         *   version, such as `nightly`, work.
         * - Otherwise the installed version name is compared with the version in the tag ([Version],
         *   extracted with [versionPattern] if set): [UpdateAvailable] if the release is newer,
         *   [UpToDate] if not, [UpToDate] for equal normalized strings (tag `v-nightly-1`, version
         *   name `nightly-1`), else [Unknown].
         */
        fun of(
            installedApp: InstalledApp?,
            latestRelease: Release?,
            installedRelease: InstalledRelease? = null,
            versionPattern: String? = null,
            skippedReleaseId: Long? = null,
        ): UpdateStatus {
            val installedByUs = installedRelease != null && installedRelease.versionCode == installedApp?.versionCode
            return when {
                installedApp == null -> NotInstalled
                latestRelease == null -> NoRelease
                latestRelease.id == skippedReleaseId -> UpToDate
                installedByUs && installedRelease?.releaseId == latestRelease.id -> UpToDate
                else -> compareVersions(installedApp, latestRelease, versionPattern, unknownIsUpdate = installedByUs)
            }
        }

        private fun compareVersions(
            installedApp: InstalledApp,
            latestRelease: Release,
            versionPattern: String?,
            unknownIsUpdate: Boolean,
        ): UpdateStatus {
            val installedName = installedApp.versionName
            val tagVersion = versionFromTag(latestRelease.tagName, versionPattern)
            val installed = installedName?.let(Version::parse)
            val latest = Version.parse(tagVersion)
            return when {
                installed != null && latest != null -> if (latest > installed) UpdateAvailable else UpToDate
                installedName != null && normalize(installedName) == normalize(tagVersion) -> UpToDate
                unknownIsUpdate -> UpdateAvailable
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
