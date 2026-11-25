package me.geckotv.apkmanager.core.model

import java.math.BigInteger

/**
 * A version number parsed tolerantly from a release tag or an Android `versionName`, e.g. `v1.2.3`,
 * `1.2.3-beta.2`, `release-2024.05.01` or `1.2.3 (45)`.
 *
 * Numbers are compared numerically and missing trailing components count as zero, so `1.2` equals
 * `1.2.0`. A pre-release is older than the same version without a label. Unknown suffixes and build
 * metadata (`+build.7`) are ignored.
 */
class Version private constructor(
    private val numbers: List<BigInteger>,
    private val preRelease: PreRelease?,
) : Comparable<Version> {
    /** [numbers] without trailing zeros; the basis for equality and hashing. */
    private val significant: List<BigInteger> = numbers.dropLastWhile { it == BigInteger.ZERO }

    override fun compareTo(other: Version): Int {
        val size = maxOf(numbers.size, other.numbers.size)
        for (index in 0 until size) {
            val result =
                numbers
                    .getOrElse(index) { BigInteger.ZERO }
                    .compareTo(other.numbers.getOrElse(index) { BigInteger.ZERO })
            if (result != 0) return result
        }
        return comparePreReleases(preRelease, other.preRelease)
    }

    override fun equals(other: Any?): Boolean = other is Version && compareTo(other) == 0

    override fun hashCode(): Int = 31 * significant.hashCode() + (preRelease?.hashCode() ?: 0)

    override fun toString(): String = numbers.joinToString(".") + preRelease?.let { "-$it" }.orEmpty()

    /** Declared in ascending order of precedence. */
    private enum class Label(
        val text: String,
    ) {
        Dev("dev"),
        Alpha("alpha"),
        Beta("beta"),
        Preview("pre"),
        ReleaseCandidate("rc"),
    }

    private data class PreRelease(
        val label: Label,
        val number: BigInteger,
    ) {
        override fun toString(): String = if (number == BigInteger.ZERO) label.text else "${label.text}.$number"
    }

    companion object {
        private val NUMBER = Regex("""\d+(?:\.\d+)*""")
        private val PRE_RELEASE =
            Regex(
                """[-_.]?(?<word>alpha|beta|rc|preview|pre|dev|snapshot)(?![a-z])(?:[.-]?(?<number>\d+))?""" +
                    """|[-_.]?(?<letter>[ab])(?<letterNumber>\d+)""",
                RegexOption.IGNORE_CASE,
            )

        /** Parses [text], or returns `null` if it contains no version number. */
        fun parse(text: String): Version? {
            val match = NUMBER.find(text) ?: return null
            val numbers = match.value.split('.').map { BigInteger(it) }
            val rest = text.substring(match.range.last + 1)
            val preRelease = PRE_RELEASE.matchAt(rest, 0)?.let(::toPreRelease)
            return Version(numbers, preRelease)
        }

        private fun toPreRelease(match: MatchResult): PreRelease {
            val word = (match.groups["word"] ?: match.groups["letter"])?.value.orEmpty().lowercase()
            val number = (match.groups["number"] ?: match.groups["letterNumber"])?.value ?: "0"
            return PreRelease(labelOf(word), BigInteger(number))
        }

        private fun labelOf(word: String): Label =
            when (word) {
                "alpha", "a" -> Label.Alpha
                "beta", "b" -> Label.Beta
                "pre", "preview" -> Label.Preview
                "rc" -> Label.ReleaseCandidate
                else -> Label.Dev
            }

        private fun comparePreReleases(
            first: PreRelease?,
            second: PreRelease?,
        ): Int =
            when {
                first == null && second == null -> 0
                first == null -> 1
                second == null -> -1
                first.label != second.label -> first.label.compareTo(second.label)
                else -> first.number.compareTo(second.number)
            }
    }
}
