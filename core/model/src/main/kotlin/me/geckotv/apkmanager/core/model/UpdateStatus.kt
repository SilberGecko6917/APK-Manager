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
        ): UpdateStatus = TODO("Implemented by the version comparison change")
    }
}
