package me.geckotv.apkmanager.core.model

/** Parses user input into a [GitHubRepository]. */
object GitHubRepositoryParser {
    private const val MAX_OWNER_LENGTH = 39
    private const val MAX_NAME_LENGTH = 100
    private const val SSH_PREFIX = "git@github.com:"
    private const val GIT_SUFFIX = ".git"

    private val WEB_PREFIX = Regex("^(?:https?://)?(?:www\\.)?github\\.com/", RegexOption.IGNORE_CASE)
    private val OWNER = Regex("^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9]))*$")
    private val NAME = Regex("^[A-Za-z0-9._-]+$")

    /** First path segments on github.com that are site pages, not users or organizations. */
    private val RESERVED_OWNERS =
        setOf(
            "about",
            "apps",
            "codespaces",
            "collections",
            "enterprise",
            "events",
            "explore",
            "features",
            "issues",
            "join",
            "login",
            "marketplace",
            "new",
            "notifications",
            "orgs",
            "organizations",
            "pricing",
            "pulls",
            "search",
            "security",
            "sessions",
            "settings",
            "sponsors",
            "topics",
            "trending",
            "users",
        )

    /**
     * Accepts `owner/repo`, repository URLs with or without scheme (`github.com/owner/repo`,
     * `https://github.com/owner/repo/releases/tag/v1`, `…/repo.git`) and SSH remotes
     * (`git@github.com:owner/repo.git`). Returns `null` for other hosts and invalid names.
     */
    fun parse(input: String): GitHubRepository? {
        val segments = extractPath(input.trim())?.split('/').orEmpty()
        val owner = segments.getOrNull(0).orEmpty()
        val name = segments.getOrNull(1).orEmpty().removeSuffix(GIT_SUFFIX)
        return if (isValidOwner(owner) && isValidName(name)) GitHubRepository(owner, name) else null
    }

    /** The part after the host (or the whole input for `owner/repo`), without query and fragment. */
    private fun extractPath(input: String): String? {
        val text = input.substringBefore('#').substringBefore('?')
        val webPrefix = WEB_PREFIX.find(text)
        return when {
            text.startsWith(SSH_PREFIX, ignoreCase = true) -> text.substring(SSH_PREFIX.length)
            webPrefix != null -> text.substring(webPrefix.range.last + 1)
            "://" in text -> null
            else -> text
        }
    }

    private fun isValidOwner(owner: String): Boolean =
        owner.length <= MAX_OWNER_LENGTH && OWNER.matches(owner) && owner.lowercase() !in RESERVED_OWNERS

    private fun isValidName(name: String): Boolean =
        name.length <= MAX_NAME_LENGTH && NAME.matches(name) && name != "." && name != ".."
}
