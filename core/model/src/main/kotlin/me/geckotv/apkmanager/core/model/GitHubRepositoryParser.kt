package me.geckotv.apkmanager.core.model

/** Parses user input into a [GitHubRepository]. */
object GitHubRepositoryParser {
    /**
     * Accepts `owner/repo`, repository URLs with or without scheme (`github.com/owner/repo`,
     * `https://github.com/owner/repo/releases/tag/v1`, `…/repo.git`) and SSH remotes
     * (`git@github.com:owner/repo.git`). Returns `null` for other hosts and invalid names.
     */
    fun parse(input: String): GitHubRepository? = TODO()
}
