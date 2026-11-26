package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.InstalledRelease
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.testing.TestData
import me.geckotv.apkmanager.feature.apps.AppItem
import me.geckotv.apkmanager.feature.apps.AppsScreen
import me.geckotv.apkmanager.feature.apps.AppsUiState
import me.geckotv.apkmanager.feature.apps.groupApps
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1700dp-xxhdpi")
class AppDetailsScreenshotTest {
    @Test
    fun detailsUpdateAvailableLight() =
        capture("app_details_update_light", darkTheme = false) { Details(updateAvailable()) }

    @Test
    fun detailsUpdateAvailableDark() =
        capture("app_details_update_dark", darkTheme = true) { Details(updateAvailable()) }

    @Test
    fun detailsSkippedLight() = capture("app_details_skipped_light", darkTheme = false) { Details(skipped()) }

    @Test
    fun detailsSkippedDark() = capture("app_details_skipped_dark", darkTheme = true) { Details(skipped()) }

    @Test
    @Config(qualifiers = "w411dp-h700dp-xxhdpi")
    fun assetFilterSheetLight() =
        capture(
            "app_details_asset_filter_light",
            darkTheme = false,
        ) { AssetFilterSheet(include = "foss", exclude = "debug") }

    @Test
    @Config(qualifiers = "w411dp-h700dp-xxhdpi")
    fun assetFilterSheetDark() =
        capture(
            "app_details_asset_filter_dark",
            darkTheme = true,
        ) { AssetFilterSheet(include = "foss", exclude = "debug") }

    @Test
    @Config(qualifiers = "w411dp-h700dp-xxhdpi")
    fun assetFilterSheetInvalidLight() =
        capture("app_details_asset_filter_invalid_light", darkTheme = false) {
            AssetFilterSheet(include = "foss", exclude = "(debug")
        }

    @Test
    @Config(qualifiers = "w411dp-h580dp-xxhdpi")
    fun versionPatternSheetLight() =
        capture(
            "app_details_version_pattern_light",
            darkTheme = false,
        ) { VersionPatternSheet(pattern = "release-(.*)") }

    @Test
    @Config(qualifiers = "w411dp-h580dp-xxhdpi")
    fun versionPatternSheetDark() =
        capture("app_details_version_pattern_dark", darkTheme = true) { VersionPatternSheet(pattern = "release-(.*)") }

    @Test
    fun detailsDownloadingLight() =
        capture("app_details_downloading_light", darkTheme = false) {
            Details(updateAvailable(install = InstallState.Downloading(4_800_000, 12_000_000)))
        }

    @Test
    fun detailsDownloadingDark() =
        capture("app_details_downloading_dark", darkTheme = true) {
            Details(updateAvailable(install = InstallState.Downloading(4_800_000, 12_000_000)))
        }

    @Test
    fun detailsHistoryFailedLight() =
        capture("app_details_history_failed_light", darkTheme = false) {
            Details(updateAvailable(history = ReleaseHistoryState.Failed(SourceError.Offline)))
        }

    @Test
    fun detailsHistoryFailedDark() =
        capture("app_details_history_failed_dark", darkTheme = true) {
            Details(updateAvailable(history = ReleaseHistoryState.Failed(SourceError.Offline)))
        }

    @Test
    fun detailsHistoryLoadingLight() =
        capture("app_details_history_loading_light", darkTheme = false) {
            Details(updateAvailable(history = ReleaseHistoryState.Loading))
        }

    @Test
    fun detailsNotInstalledLight() =
        capture("app_details_not_installed_light", darkTheme = false) { Details(notInstalled()) }

    @Test
    fun detailsNotInstalledDark() =
        capture("app_details_not_installed_dark", darkTheme = true) { Details(notInstalled()) }

    @Test
    @Config(qualifiers = "w1000dp-h700dp-xxhdpi")
    fun twoPanePlaceholderLight() =
        capture("app_details_two_pane_placeholder_light", darkTheme = false) { TwoPane(null) }

    @Test
    @Config(qualifiers = "w1000dp-h700dp-xxhdpi")
    fun twoPanePlaceholderDark() = capture("app_details_two_pane_placeholder_dark", darkTheme = true) { TwoPane(null) }

    @Test
    @Config(qualifiers = "w1000dp-h1100dp-xxhdpi")
    fun twoPaneDetailsLight() = capture("app_details_two_pane_light", darkTheme = false) { TwoPane(updateAvailable()) }

    private fun capture(
        name: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
        }
    }

    @Composable
    private fun Details(state: AppDetailsUiState.Ready) {
        AppDetailsScreen(
            uiState = state,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onInstallClick = {},
            onCancelClick = {},
            onIncludePrereleasesChange = {},
            onInstalledAppClick = {},
            onPackageSelect = {},
            onPickerBack = {},
            onRetryHistoryClick = {},
            onReleaseClick = {},
            onRemoveClick = {},
            onRemoveConfirm = {},
            onRemoveDismiss = {},
            options = AppDetailsOptionsActions(),
            showProgressIndicator = false,
        )
    }

    @Composable
    private fun AssetFilterSheet(
        include: String,
        exclude: String,
    ) {
        SheetSurface {
            AssetFilterSheetContent(
                filter = AssetFilter(include, exclude),
                release = apkRelease(),
                supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
                preferredAssetName = null,
                onSaveClick = { _, _ -> },
                onCancelClick = {},
            )
        }
    }

    @Composable
    private fun VersionPatternSheet(pattern: String) {
        val releases =
            listOf("release-1.2.0", "release-1.1.0", "nightly", "release-1.0.1", "v1.0.0")
                .mapIndexed { index, tag -> release(index + 1L, tag, tag, notes = null) }
        SheetSurface {
            VersionPatternSheetContent(
                pattern = pattern,
                tags = previewTags(releases.first(), releases),
                onSaveClick = {},
                onCancelClick = {},
            )
        }
    }

    /** What a bottom sheet shows around its content. */
    @Composable
    private fun SheetSurface(content: @Composable () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)) { content() }
        }
    }

    /** A release with one APK per ABI and flavor. */
    private fun apkRelease(): Release =
        release(HISTORY_LATEST_ID, "v1.2.0", "Spring update", notes = null).copy(
            assets =
                listOf(
                    "octo-app-play-arm64-v8a.apk",
                    "octo-app-foss-x86_64.apk",
                    "octo-app-foss-armeabi-v7a.apk",
                    "octo-app-foss-arm64-v8a.apk",
                    "octo-app-foss-arm64-v8a-debug.apk",
                    "octo-app-foss-arm64-v8a.apk.sha256",
                ).mapIndexed { index, name -> TestData.asset(index + 1L, name) },
        )

    /** The apps list with, next to it, the details of [selected] or the placeholder. */
    @Composable
    private fun TwoPane(selected: AppDetailsUiState.Ready?) {
        val items = listOf(AppItem(app(), hasCompatibleApk = true), AppItem(notInstalledApp(), hasCompatibleApk = true))
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Row {
                TwoPaneContent(items, selected)
            }
        }
    }

    @Composable
    private fun TwoPaneContent(
        items: List<AppItem>,
        selected: AppDetailsUiState.Ready?,
    ) {
        AppsScreen(
            uiState = AppsUiState.Ready(groups = groupApps(items), isRefreshing = false),
            snackbarHostState = SnackbarHostState(),
            onRefresh = {},
            onAppClick = {},
            onInstallClick = {},
            onUpdateAllClick = {},
            onAddAppClick = {},
            modifier = Modifier.width(400.dp).fillMaxHeight(),
            showProgressIndicator = false,
        )
        if (selected == null) {
            AppDetailsPlaceholder()
        } else {
            Details(selected)
        }
    }

    private fun updateAvailable(
        install: InstallState? = null,
        history: ReleaseHistoryState = ReleaseHistoryState.Loaded(history()),
    ) = AppDetailsUiState.Ready(
        item = AppItem(app = app(), install = install, hasCompatibleApk = true),
        history = history,
        installedApps = null,
        isConfirmingRemove = false,
        expandedReleaseId = HISTORY_EXPANDED_ID,
    )

    /** The update is skipped; the app is installed through APK Manager and has options set. */
    private fun skipped() =
        updateAvailable().let { state ->
            state.copy(
                item =
                    state.item.copy(
                        app =
                            state.item.app.copy(
                                skippedReleaseId = HISTORY_LATEST_ID,
                                assetFilter = AssetFilter(include = "foss", exclude = "debug"),
                                versionPattern = "v(.*)",
                            ),
                    ),
            )
        }

    private fun notInstalled() =
        AppDetailsUiState.Ready(
            item = AppItem(app = notInstalledApp(), hasCompatibleApk = true),
            history = ReleaseHistoryState.Loaded(listOf(release(5, "v2.0.0", "", notes = null))),
            installedApps = null,
            isConfirmingRemove = false,
            expandedReleaseId = null,
        )

    private fun app(): TrackedApp =
        TestData
            .trackedApp(
                id = 1,
                repository = GitHubRepository("octo", "octo-app"),
                latestRelease = history().first(),
                installedApp = TestData.installedApp("org.example.octoapp", "Octo App", "1.1.0"),
                includePrereleases = true,
            ).copy(installedRelease = InstalledRelease(releaseId = HISTORY_INSTALLED_ID, versionCode = 1))

    private fun notInstalledApp(): TrackedApp =
        TestData
            .trackedApp(
                id = 2,
                repository = GitHubRepository("octo", "camera"),
                latestRelease = release(5, "v2.0.0", "Camera 2", notes = null),
                isPrivate = true,
            ).copy(displayName = "Camera")

    private fun history(): List<Release> =
        listOf(
            release(HISTORY_LATEST_ID, "v1.2.0", "Spring update", notes = LONG_NOTES),
            release(HISTORY_EXPANDED_ID, "v1.2.0-rc.1", "Release candidate", notes = SHORT_NOTES, isPrerelease = true),
            release(3, "v1.1.0", "Octo App 1.1", notes = SHORT_NOTES),
            release(2, "v1.0.1", "v1.0.1", notes = null),
            release(1, "v1.0.0", "First release", notes = SHORT_NOTES),
        )

    private fun release(
        id: Long,
        tag: String,
        name: String,
        notes: String?,
        isPrerelease: Boolean = false,
    ): Release =
        TestData.release(id, tag, isPrerelease = isPrerelease).copy(
            name = name,
            body = notes,
            publishedAt = Instant.parse("2026-11-20T12:00:00Z").minusSeconds(id * SECONDS_PER_WEEK),
        )

    private companion object {
        const val HISTORY_LATEST_ID = 5L
        const val HISTORY_EXPANDED_ID = 4L
        const val HISTORY_INSTALLED_ID = 3L
        const val SECONDS_PER_WEEK = 7 * 24 * 3600L

        val SHORT_NOTES =
            """
            ## Fixes

            - Fixed a crash when opening the **settings**
            - Smaller download size
            """.trimIndent()

        val LONG_NOTES =
            """
            ## What's new

            Octo App now looks better and starts faster. See the [full changelog](https://example.org/changelog)
            for every detail.

            - Redesigned **settings** screen with a new search
            - Faster startup: the cache is read in the background
            - New `--verbose` flag for the command line tool
            - Support for *dark mode* on every screen

            ### Fixes

            1. Fixed a crash when opening `Settings > About`
            2. Fixed the sync that stopped after an update
            3. The notification channel is created again after a reinstall

            ```kotlin
            val octo = OctoApp(verbose = true)
            octo.sync()
            ```

            ### Known issues

            - The widget needs a restart after the update
            - Backups of version 0.9 cannot be restored
            """.trimIndent()
    }
}
