package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AppsScreenScreenshotTest {
    @Test
    fun appsEmptyLight() = capture("apps_empty_light", darkTheme = false) { AppsScreenFor(emptyList()) }

    @Test
    fun appsEmptyDark() = capture("apps_empty_dark", darkTheme = true) { AppsScreenFor(emptyList()) }

    @Test
    fun appsListLight() = capture("apps_list_light", darkTheme = false) { AppsScreenFor(sampleApps().map { item(it) }) }

    @Test
    fun appsListDark() = capture("apps_list_dark", darkTheme = true) { AppsScreenFor(sampleApps().map { item(it) }) }

    @Test
    fun addAppInputLight() =
        capture("add_app_input_light", darkTheme = false) { Sheet { AddAppContent(AddAppUiState()) } }

    @Test
    fun addAppInvalidInputDark() =
        capture("add_app_input_invalid_dark", darkTheme = true) {
            Sheet {
                AddAppContent(AddAppUiState(input = "not a repo", inputError = AddAppInputError.Invalid))
            }
        }

    @Test
    fun addAppLoadingLight() =
        capture("add_app_loading_light", darkTheme = false) {
            Sheet { AddAppContent(AddAppUiState(input = "octo/app", step = AddAppStep.Loading)) }
        }

    @Test
    fun addAppPreviewLight() =
        capture("add_app_preview_light", darkTheme = false) {
            Sheet { AddAppContent(previewState()) }
        }

    @Test
    fun addAppPreviewDark() =
        capture("add_app_preview_dark", darkTheme = true) { Sheet { AddAppContent(previewState()) } }

    @Test
    fun addAppErrorLight() =
        capture("add_app_error_light", darkTheme = false) {
            Sheet { AddAppContent(errorState(SourceError.NotFound)) }
        }

    @Test
    fun addAppErrorDark() =
        capture("add_app_error_dark", darkTheme = true) {
            Sheet {
                AddAppContent(errorState(SourceError.RateLimited(Instant.parse("2026-11-25T14:05:00Z"))))
            }
        }

    @Test
    fun packagePickerLight() =
        capture("package_picker_light", darkTheme = false) {
            Sheet {
                PackagePicker(
                    installedApps = installedApps(),
                    selectedPackageName = "org.example.octoapp",
                    onSelect = {},
                    onBackClick = {},
                )
            }
        }

    @Test
    fun appsListInstallingLight() =
        capture("apps_list_installing_light", darkTheme = false) { AppsScreenFor(installingApps()) }

    @Test
    fun appsListInstallingDark() =
        capture("apps_list_installing_dark", darkTheme = true) { AppsScreenFor(installingApps()) }

    @Test
    fun appsListVerifyingLight() =
        capture("apps_list_verifying_light", darkTheme = false) {
            AppsScreenFor(listOf(item(app(1, "Octo App", "1.1.0", "v1.2.0"), InstallState.Verifying)))
        }

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
    private fun AppsScreenFor(items: List<AppItem>) {
        AppsScreen(
            uiState = AppsUiState.Ready(groups = groupApps(items), isRefreshing = false),
            snackbarHostState = SnackbarHostState(),
            onRefresh = {},
            onAppClick = {},
            onInstallClick = {},
            onUpdateAllClick = {},
            onAddAppClick = {},
            showProgressIndicator = false,
        )
    }

    /** Draws sheet content on the sheet's background. */
    @Composable
    private fun Sheet(content: @Composable () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)) { content() }
        }
    }

    @Composable
    private fun AddAppContent(state: AddAppUiState) {
        AddAppSheetContent(
            state = state,
            onInputChange = {},
            onPasteClick = {},
            onLookUpClick = {},
            onBackClick = {},
            onIncludePrereleasesChange = {},
            onAssetSelect = {},
            onInstalledAppClick = {},
            onPackageSelect = {},
            onAddClick = {},
            showProgressIndicator = false,
        )
    }

    private fun errorState(error: SourceError) = AddAppUiState(input = "octo/app", step = AddAppStep.Failed(error))

    private fun previewState(): AddAppUiState {
        val release =
            TestData.release(
                id = 1,
                tag = "v1.2.0",
                assets =
                    listOf(
                        TestData.asset(1, "octo-app-v1.2.0-universal.apk", size = 24_000_000),
                        TestData.asset(2, "octo-app-v1.2.0-arm64-v8a.apk", size = 12_500_000),
                        TestData.asset(3, "octo-app-v1.2.0-x86_64.apk", size = 13_000_000),
                    ),
            )
        val preview = TestData.preview(GitHubRepository("octo", "app"), releases = listOf(release), isPrivate = true)
        return AddAppUiState(
            input = "octo/app",
            step =
                AddAppStep.Preview(
                    preview = preview,
                    installedApps = installedApps(),
                    supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
                    packageName = "org.example.octoapp",
                ),
        )
    }

    private fun installedApps(): List<InstalledApp> =
        listOf(
            TestData.installedApp("org.example.octoapp", "Octo App", "1.1.0"),
            TestData.installedApp("org.example.notes", "Notes", "4.2"),
            TestData.installedApp("org.example.player", "Player", "0.9.1"),
            TestData.installedApp("com.android.settings", "Settings", "16", isSystemApp = true),
        )

    /** An update that is downloading (40 %), one that can be started, and one app to install. */
    private fun installingApps(): List<AppItem> =
        listOf(
            item(app(1, "Octo App", "1.1.0", "v1.2.0")),
            item(
                app(2, "Notes", "4.2", "v4.3.0"),
                InstallState.Downloading(bytesRead = 4_000_000, totalBytes = 10_000_000),
            ),
            item(app(3, "Player", "0.9.1", "v0.9.1")),
            item(app(5, "Camera", null, "v2.0.0")),
        )

    private fun item(
        app: TrackedApp,
        install: InstallState? = null,
    ) = AppItem(app = app, install = install, hasCompatibleApk = true)

    /** Updates for two apps, three up to date (one unknown), and one not installed, the latter two with labels. */
    private fun sampleApps(): List<TrackedApp> =
        listOf(
            app(1, "Octo App", "1.1.0", "v1.2.0"),
            app(2, "Notes", "4.2", "v4.3.0", isPrerelease = true),
            app(3, "Player", "0.9.1", "v0.9.1").copy(isPrivate = true),
            app(4, "Nightly Tool", "20260101", "nightly"),
            app(5, "Camera", null, "v2.0.0"),
        )

    private fun app(
        id: Long,
        name: String,
        installedVersion: String?,
        tag: String,
        isPrerelease: Boolean = false,
    ): TrackedApp =
        TestData
            .trackedApp(
                id = id,
                repository = GitHubRepository("octo", name.lowercase().replace(' ', '-')),
                latestRelease = TestData.release(id, tag, isPrerelease = isPrerelease),
                installedApp = installedVersion?.let { TestData.installedApp("org.example.app$id", name, it) },
            ).let { app -> if (installedVersion == null) app.copy(displayName = name) else app }
}
