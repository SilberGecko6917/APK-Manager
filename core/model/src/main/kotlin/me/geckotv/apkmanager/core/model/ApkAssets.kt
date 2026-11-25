package me.geckotv.apkmanager.core.model

/** An APK asset of a release, rated for the current device. */
data class ApkAssetCandidate(
    val asset: ReleaseAsset,
    /** The ABI in the file name, normalized to an Android ABI name; `null` for universal APKs. */
    val abi: String?,
    /** Whether the device supports [abi] (always `true` for universal APKs). */
    val isCompatible: Boolean,
    /** Whether the file name marks a debug build. */
    val isDebug: Boolean,
)

/** Picks the APK to install from a release's assets. */
object ApkAssets {
    /**
     * The `.apk` assets of a release, best first: compatible before incompatible, then by the
     * device's ABI preference ([supportedAbis] order, i.e. `Build.SUPPORTED_ABIS`), then universal,
     * release before debug builds. Other files (checksums, `.aab`, `.apks`, archives) are left out.
     */
    fun candidates(
        assets: List<ReleaseAsset>,
        supportedAbis: List<String>,
    ): List<ApkAssetCandidate> = TODO()

    /**
     * The asset to install: the one matching [preferredAssetName] (see [matchesPreferred]) if it
     * exists, otherwise the best compatible candidate, or `null` if there is none.
     */
    fun select(
        assets: List<ReleaseAsset>,
        supportedAbis: List<String>,
        preferredAssetName: String?,
    ): ReleaseAsset? = TODO()

    /**
     * Whether [assetName] is the "same" file as [preferredAssetName] from an earlier release, i.e. the
     * names are equal once versions and numbers are ignored (`app-v1.2.0-arm64.apk` matches
     * `app-v1.3.1-arm64.apk`).
     */
    fun matchesPreferred(
        assetName: String,
        preferredAssetName: String,
    ): Boolean = TODO()
}
