package me.geckotv.apkmanager.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubRepositoryParserTest {
    private val expected = GitHubRepository("owner", "repo")

    @Test
    fun `accepts owner and repo`() {
        assertEquals(expected, GitHubRepositoryParser.parse("owner/repo"))
        assertEquals(expected, GitHubRepositoryParser.parse("  owner/repo \n"))
    }

    @Test
    fun `accepts github urls with or without scheme and www`() {
        val inputs =
            listOf(
                "github.com/owner/repo",
                "www.github.com/owner/repo",
                "http://github.com/owner/repo",
                "https://github.com/owner/repo",
                "https://www.github.com/owner/repo",
                "HTTPS://GitHub.com/owner/repo",
            )
        for (input in inputs) {
            assertEquals(input, expected, GitHubRepositoryParser.parse(input))
        }
    }

    @Test
    fun `ignores extra path query and fragment`() {
        val inputs =
            listOf(
                "https://github.com/owner/repo/",
                "https://github.com/owner/repo/releases",
                "https://github.com/owner/repo/releases/tag/v1.0",
                "https://github.com/owner/repo/tree/main",
                "https://github.com/owner/repo?tab=readme-ov-file",
                "https://github.com/owner/repo#readme",
                "https://github.com/owner/repo/issues?q=is%3Aopen#top",
                "owner/repo/",
            )
        for (input in inputs) {
            assertEquals(input, expected, GitHubRepositoryParser.parse(input))
        }
    }

    @Test
    fun `strips the git suffix`() {
        assertEquals(expected, GitHubRepositoryParser.parse("https://github.com/owner/repo.git"))
        assertEquals(expected, GitHubRepositoryParser.parse("owner/repo.git"))
        assertEquals(GitHubRepository("owner", "my.git"), GitHubRepositoryParser.parse("owner/my.git.git"))
    }

    @Test
    fun `accepts ssh remotes`() {
        assertEquals(expected, GitHubRepositoryParser.parse("git@github.com:owner/repo.git"))
        assertEquals(expected, GitHubRepositoryParser.parse("git@github.com:owner/repo"))
    }

    @Test
    fun `preserves case`() {
        assertEquals(
            GitHubRepository("SilberGecko6917", "APK-Manager"),
            GitHubRepositoryParser.parse("https://github.com/SilberGecko6917/APK-Manager"),
        )
    }

    @Test
    fun `accepts valid names`() {
        assertEquals(GitHubRepository("a", "b"), GitHubRepositoryParser.parse("a/b"))
        assertEquals(GitHubRepository("a-b-c", "x.y_z-1"), GitHubRepositoryParser.parse("a-b-c/x.y_z-1"))
        assertEquals(GitHubRepository("a".repeat(39), "r"), GitHubRepositoryParser.parse("${"a".repeat(39)}/r"))
        assertEquals(GitHubRepository("o", "r".repeat(100)), GitHubRepositoryParser.parse("o/${"r".repeat(100)}"))
        assertEquals(GitHubRepository("o", ".github"), GitHubRepositoryParser.parse("o/.github"))
    }

    @Test
    fun `rejects other hosts`() {
        val inputs =
            listOf(
                "gitlab.com/a/b",
                "https://gitlab.com/a/b",
                "https://example.com/a/b",
                "https://notgithub.com/a/b",
                "https://github.com.evil.com/a/b",
                "git@gitlab.com:a/b.git",
                "ftp://github.com/a/b",
            )
        for (input in inputs) {
            assertNull(input, GitHubRepositoryParser.parse(input))
        }
    }

    @Test
    fun `rejects missing repo and empty input`() {
        val inputs =
            listOf(
                "",
                "   ",
                "owner",
                "owner/",
                "/repo",
                "github.com/owner",
                "https://github.com/owner/",
                "https://github.com",
                "/",
            )
        for (input in inputs) {
            assertNull(input, GitHubRepositoryParser.parse(input))
        }
    }

    @Test
    fun `rejects invalid owners`() {
        val inputs =
            listOf(
                "-owner/repo",
                "owner-/repo",
                "ow--ner/repo",
                "ow_ner/repo",
                "ow ner/repo",
                "${"a".repeat(40)}/repo",
                "own.er/repo",
            )
        for (input in inputs) {
            assertNull(input, GitHubRepositoryParser.parse(input))
        }
    }

    @Test
    fun `rejects invalid repo names`() {
        val inputs =
            listOf("owner/.", "owner/..", "owner/re po", "owner/re@po", "owner/${"r".repeat(101)}", "owner/.git")
        for (input in inputs) {
            assertNull(input, GitHubRepositoryParser.parse(input))
        }
    }

    @Test
    fun `rejects reserved first segments`() {
        val reserved =
            listOf("orgs", "settings", "marketplace", "topics", "apps", "sponsors", "features", "about", "login")
        for (segment in reserved) {
            assertNull(segment, GitHubRepositoryParser.parse("github.com/$segment/foo"))
            assertNull(segment, GitHubRepositoryParser.parse("https://github.com/${segment.uppercase()}/foo"))
        }
    }
}
