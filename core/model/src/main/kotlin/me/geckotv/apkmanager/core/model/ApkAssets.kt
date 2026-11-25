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
    private const val APK_EXTENSION = ".apk"
    private const val SEPARATOR = "[^a-z0-9]"

    private const val GROUP_RELEASE = 0
    private const val GROUP_DEBUG = 1
    private const val GROUP_INCOMPATIBLE = 2

    /**
     * An ABI and the file name tokens that denote it. A token is matched as a whole, delimited by
     * non-alphanumeric characters; `-` inside a token stands for any separator. [placeholder] replaces
     * the token in [matchesPreferred] so that its digits are not mistaken for a version.
     */
    private class AbiRule(
        val abi: String,
        val placeholder: String,
        tokens: List<String>,
    ) {
        val regex =
            Regex(
                tokens.joinToString(separator = "|", prefix = "(?<![a-z0-9])(?:", postfix = ")(?![a-z0-9])") {
                    it.replace("-", SEPARATOR)
                },
            )
    }

    /** Ordered so that longer tokens win: `x86_64` before `x86`, `arm64` before `arm`. */
    private val ABI_RULES =
        listOf(
            AbiRule("x86_64", "~abi-x~", listOf("x86-64", "x64", "amd64")),
            AbiRule("arm64-v8a", "~abi-a~", listOf("arm64-v8a", "arm64", "aarch64", "armv8")),
            AbiRule("armeabi-v7a", "~abi-b~", listOf("armeabi-v7a", "armeabi", "armv7a", "armv7", "arm32", "arm")),
            AbiRule("x86", "~abi-c~", listOf("x86", "i686", "i386")),
        )

    private val DEBUG = Regex("(?<![a-z0-9])debug(?![a-z0-9])")
    private val VERSION_RUN = Regex("""v?\d+(?:[._]\d+)*""")

    /**
     * The `.apk` assets of a release, best first: compatible before incompatible, then by the
     * device's ABI preference ([supportedAbis] order, i.e. `Build.SUPPORTED_ABIS`), then universal,
     * release before debug builds. Other files (checksums, `.aab`, `.apks`, archives) are left out.
     *
     * More precisely, all compatible release builds come first (ranked by ABI preference, universal
     * last), then compatible debug builds, then incompatible ones. Ties keep the original order.
     */
    fun candidates(
        assets: List<ReleaseAsset>,
        supportedAbis: List<String>,
    ): List<ApkAssetCandidate> =
        assets
            .filter { it.name.endsWith(APK_EXTENSION, ignoreCase = true) }
            .map { rate(it, supportedAbis) }
            .sortedWith(
                compareBy<ApkAssetCandidate> { group(it) }.thenBy { rank(it, supportedAbis) },
            )

    /**
     * The asset to install: the one matching [preferredAssetName] (see [matchesPreferred]) if it
     * exists and is compatible, otherwise the best compatible candidate, or `null` if there is none.
     */
    fun select(
        assets: List<ReleaseAsset>,
        supportedAbis: List<String>,
        preferredAssetName: String?,
    ): ReleaseAsset? {
        val compatible = candidates(assets, supportedAbis).filter { it.isCompatible }
        val preferred =
            preferredAssetName?.let { name ->
                compatible.firstOrNull { matchesPreferred(it.asset.name, name) }
            }
        return (preferred ?: compatible.firstOrNull())?.asset
    }

    /**
     * Whether [assetName] is the "same" file as [preferredAssetName] from an earlier release, i.e. the
     * names are equal once versions and numbers are ignored (`app-v1.2.0-arm64.apk` matches
     * `app-v1.3.1-arm64.apk`). ABI tokens keep their identity, so `arm64-v8a` and `armeabi-v7a` differ.
     */
    fun matchesPreferred(
        assetName: String,
        preferredAssetName: String,
    ): Boolean = normalizeName(assetName) == normalizeName(preferredAssetName)

    private fun rate(
        asset: ReleaseAsset,
        supportedAbis: List<String>,
    ): ApkAssetCandidate {
        val name = asset.name.lowercase()
        val abi = ABI_RULES.firstOrNull { it.regex.containsMatchIn(name) }?.abi
        return ApkAssetCandidate(
            asset = asset,
            abi = abi,
            isCompatible = abi == null || abi in supportedAbis,
            isDebug = DEBUG.containsMatchIn(name),
        )
    }

    private fun group(candidate: ApkAssetCandidate): Int =
        when {
            !candidate.isCompatible -> GROUP_INCOMPATIBLE
            candidate.isDebug -> GROUP_DEBUG
            else -> GROUP_RELEASE
        }

    /** Index in [supportedAbis]; universal APKs rank after every device ABI. */
    private fun rank(
        candidate: ApkAssetCandidate,
        supportedAbis: List<String>,
    ): Int = candidate.abi?.let { supportedAbis.indexOf(it) } ?: supportedAbis.size

    private fun normalizeName(name: String): String {
        val withoutAbi =
            ABI_RULES.fold(name.lowercase()) { text, rule ->
                text.replace(rule.regex, rule.placeholder)
            }
        return withoutAbi.replace(VERSION_RUN, "#")
    }
}
