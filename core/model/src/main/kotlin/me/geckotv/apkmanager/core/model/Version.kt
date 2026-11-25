package me.geckotv.apkmanager.core.model

/**
 * A version number parsed tolerantly from a release tag or an Android `versionName`, e.g. `v1.2.3`,
 * `1.2.3-beta.2`, `release-2024.05.01` or `1.2.3 (45)`.
 */
class Version private constructor() : Comparable<Version> {
    override fun compareTo(other: Version): Int = TODO()

    companion object {
        /** Parses [text], or returns `null` if it contains no version number. */
        fun parse(text: String): Version? = TODO()
    }
}
