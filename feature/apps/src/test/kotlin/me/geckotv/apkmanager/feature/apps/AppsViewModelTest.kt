package me.geckotv.apkmanager.feature.apps

import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.testing.FakeAppInstaller
import me.geckotv.apkmanager.core.testing.FakeDeviceInfo
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

// Robolectric provides the android.content.Intent that install confirmations carry.
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val trackedApps = FakeTrackedAppsRepository()
    private val installer = FakeAppInstaller()
    private val deviceInfo = FakeDeviceInfo()
    private lateinit var viewModel: AppsViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = AppsViewModel(trackedApps, installer, deviceInfo)
    }

    @Test
    fun noAppsAreTrackedInitially() =
        runTest {
            collectUiState()

            assertEquals(emptyList<AppGroup>(), ready().groups)
            assertEquals(0, trackedApps.updateCheckCount)
        }

    @Test
    fun groupsAppsByStatusAndSortsThemByName() =
        runTest {
            collectUiState()

            trackedApps.setApps(sampleApps())

            val groups = ready().groups
            assertEquals(
                listOf(AppGroupKind.UpdatesAvailable, AppGroupKind.UpToDate, AppGroupKind.NotInstalled),
                groups.map { it.kind },
            )
            assertEquals(listOf("alpha", "Zeta"), groups[0].items.map { it.app.displayName })
            assertEquals(listOf("Beta", "gamma", "Nightly", "Quiet"), groups[1].items.map { it.app.displayName })
            assertEquals(listOf("Delta"), groups[2].items.map { it.app.displayName })
        }

    @Test
    fun unknownAndNoReleaseCountAsUpToDate() =
        runTest {
            collectUiState()

            trackedApps.setApps(sampleApps())

            val upToDate = ready().groups.first { it.kind == AppGroupKind.UpToDate }.items
            assertTrue(upToDate.any { it.app.displayName == "Nightly" })
            assertTrue(upToDate.any { it.app.displayName == "Quiet" })
        }

    @Test
    fun emptyGroupsAreOmitted() =
        runTest {
            collectUiState()

            trackedApps.setApps(listOf(app(1, "Delta", installedVersion = null)))

            assertEquals(listOf(AppGroupKind.NotInstalled), ready().groups.map { it.kind })
        }

    @Test
    fun checksForUpdatesWhenCreatedWithTrackedApps() =
        runTest {
            trackedApps.setApps(sampleApps())

            AppsViewModel(trackedApps, installer, deviceInfo)

            assertEquals(1, trackedApps.updateCheckCount)
        }

    @Test
    fun refreshChecksForUpdatesOnce() =
        runTest {
            collectUiState()

            viewModel.refresh()

            assertEquals(1, trackedApps.updateCheckCount)
            assertFalse(ready().isRefreshing)
        }

    @Test
    fun successfulRefreshShowsNoMessage() =
        runTest {
            val messages = collectMessages()
            trackedApps.updateCheckResult = UpdateCheckResult(checkedCount = 3, failures = emptyMap())

            viewModel.refresh()

            assertEquals(emptyList<AppsMessage>(), messages)
        }

    @Test
    fun failedChecksAreCounted() =
        runTest {
            val messages = collectMessages()
            trackedApps.updateCheckResult =
                UpdateCheckResult(
                    checkedCount = 1,
                    failures = mapOf(1L to SourceError.Unavailable, 2L to SourceError.NotFound),
                )

            viewModel.refresh()

            assertEquals(listOf<AppsMessage>(AppsMessage.CheckFailed(2)), messages)
        }

    @Test
    fun rateLimitMessageHasTheRetryTime() =
        runTest {
            val messages = collectMessages()
            val retryAt = Instant.parse("2026-11-25T14:05:00Z")
            trackedApps.updateCheckResult =
                UpdateCheckResult(checkedCount = 0, failures = mapOf(1L to SourceError.RateLimited(retryAt)))

            viewModel.refresh()

            assertEquals(listOf<AppsMessage>(AppsMessage.RateLimited(retryAt)), messages)
        }

    @Test
    fun offlineAndUnauthorizedHaveTheirOwnMessages() =
        runTest {
            val messages = collectMessages()

            trackedApps.updateCheckResult =
                UpdateCheckResult(checkedCount = 0, failures = mapOf(1L to SourceError.Offline))
            viewModel.refresh()
            trackedApps.updateCheckResult =
                UpdateCheckResult(checkedCount = 0, failures = mapOf(1L to SourceError.Unauthorized))
            viewModel.refresh()

            assertEquals(listOf(AppsMessage.Offline, AppsMessage.Unauthorized), messages)
        }

    @Test
    fun installsTheLatestReleaseOfTheApp() =
        runTest {
            trackedApps.setApps(sampleApps())

            viewModel.install(7)

            assertEquals(listOf(7L), installer.installedApps.map { it.id })
            assertEquals(
                "v1.0.0",
                installer.installedApps
                    .single()
                    .latestRelease
                    ?.tagName,
            )
        }

    @Test
    fun installingAnUnknownAppDoesNothing() =
        runTest {
            trackedApps.setApps(sampleApps())

            viewModel.install(99)

            assertEquals(emptyList<TrackedApp>(), installer.installedApps)
        }

    @Test
    fun installStateAppearsOnTheItem() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())

            viewModel.install(1)

            assertEquals(InstallState.Downloading(bytesRead = 0, totalBytes = null), item(1).install)
            assertNull(item(2).install)
        }

    @Test
    fun itemsOfferTheMatchingInstallAction() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())

            assertEquals(InstallAction.Update, item(1).installAction)
            assertEquals(InstallAction.Install, item(7).installAction)
            assertNull(item(3).installAction)

            viewModel.install(1)
            assertNull(item(1).installAction)
        }

    @Test
    fun notInstalledAppsWithoutApkForThisDeviceCannotBeInstalled() =
        runTest {
            collectUiState()
            val release = TestData.release(7, "v1.0.0", assets = listOf(TestData.asset(70, "app-x86_64.apk")))
            trackedApps.setApps(
                listOf(
                    TestData.trackedApp(
                        id = 7,
                        repository = GitHubRepository("octo", "Delta"),
                        latestRelease = release,
                    ),
                ),
            )

            assertFalse(item(7).hasCompatibleApk)
            assertNull(item(7).installAction)
        }

    @Test
    fun updatesFinishWithAMessageAndAreDismissed() =
        runTest {
            val messages = collectMessages()
            trackedApps.setApps(sampleApps())
            viewModel.install(1)

            installer.setState(1, InstallState.Succeeded(versionName = "1.1.0"))

            assertEquals(listOf<AppsMessage>(AppsMessage.Updated("alpha", "1.1.0")), messages)
            assertEquals(emptyMap<Long, InstallState>(), installer.installs.value)
        }

    @Test
    fun firstInstallsFinishWithAnInstalledMessage() =
        runTest {
            val messages = collectMessages()
            trackedApps.setApps(sampleApps())
            viewModel.install(7)

            installer.setState(7, InstallState.Succeeded(versionName = "1.0.0"))

            assertEquals(listOf<AppsMessage>(AppsMessage.Installed("Delta")), messages)
            assertEquals(emptyMap<Long, InstallState>(), installer.installs.value)
        }

    @Test
    fun failedInstallsShowTheFailureAndTheReleaseAndAreDismissed() =
        runTest {
            val messages = collectMessages()
            trackedApps.setApps(sampleApps())
            viewModel.install(1)

            installer.setState(1, InstallState.Failed(InstallFailure.NoCompatibleApk))

            assertEquals(
                listOf<AppsMessage>(AppsMessage.InstallFailed(InstallFailure.NoCompatibleApk, "v1.1.0")),
                messages,
            )
            assertEquals(emptyMap<Long, InstallState>(), installer.installs.value)
        }

    @Test
    fun cancelledInstallsAreDismissedSilently() =
        runTest {
            val messages = collectMessages()
            trackedApps.setApps(sampleApps())
            viewModel.install(1)

            installer.setState(1, InstallState.Failed(InstallFailure.Cancelled))

            assertEquals(emptyList<AppsMessage>(), messages)
            assertEquals(emptyMap<Long, InstallState>(), installer.installs.value)
        }

    @Test
    fun updateAllInstallsOnlyUpdatesThatAreNotRunning() =
        runTest {
            trackedApps.setApps(sampleApps())
            viewModel.install(1)

            viewModel.updateAll()

            // alpha is already installing; Zeta is the only other app with an update.
            assertEquals(listOf(1L, 2L), installer.installedApps.map { it.id })
        }

    @Test
    fun updateAllLeavesOtherAppsAlone() =
        runTest {
            trackedApps.setApps(sampleApps())

            viewModel.updateAll()

            assertEquals(listOf(1L, 2L), installer.installedApps.map { it.id }.sorted())
        }

    @Test
    fun updateAllIsOfferedForSeveralUpdatesThatAreNotAllRunning() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())
            val updates = { ready().groups.first { it.kind == AppGroupKind.UpdatesAvailable } }
            assertTrue(updates().canUpdateAll)
            assertFalse(ready().groups.first { it.kind == AppGroupKind.UpToDate }.canUpdateAll)

            viewModel.install(1)
            assertTrue(updates().canUpdateAll)

            viewModel.install(2)
            assertFalse(updates().canUpdateAll)
        }

    @Test
    fun installingWhileWaitingForConfirmationShowsItAgain() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())
            installer.stateAfterInstall = InstallState.WaitingForUser(Intent("confirm"))
            viewModel.install(1)
            installer.onConfirmationShown(1)
            assertEquals(InstallAction.Confirm, item(1).installAction)

            viewModel.install(1)

            assertEquals(false, (installer.installs.value[1] as InstallState.WaitingForUser).isShown)
            assertEquals(1, installer.installedApps.size)
        }

    @Test
    fun installsStartedElsewhereFinishWithTheRightMessage() =
        runTest {
            val messages = collectMessages()
            trackedApps.setApps(sampleApps())

            // e.g. from the details screen of the app
            installer.install(trackedApps.trackedApps.first { it.id == 1L })
            installer.setState(1, InstallState.Succeeded(versionName = "1.1.0"))

            assertEquals(listOf<AppsMessage>(AppsMessage.Updated("alpha", "1.1.0")), messages)
        }

    private fun TestScope.collectUiState() {
        // uiState only collects its upstream while it has a subscriber.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun TestScope.collectMessages(): List<AppsMessage> {
        val messages = mutableListOf<AppsMessage>()
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler),
        ) { viewModel.messages.collect { messages += it } }
        return messages
    }

    private fun ready(): AppsUiState.Ready = viewModel.uiState.value as AppsUiState.Ready

    private fun item(id: Long): AppItem = ready().groups.flatMap { it.items }.first { it.id == id }

    /** Two apps with updates, four up to date (one unknown, one without release) and one not installed. */
    private fun sampleApps(): List<TrackedApp> =
        listOf(
            app(1, "alpha", installedVersion = "1.0.0", tag = "v1.1.0"),
            app(2, "Zeta", installedVersion = "2.0.0", tag = "v2.1.0"),
            app(3, "gamma", installedVersion = "1.0.0", tag = "v1.0.0"),
            app(4, "Beta", installedVersion = "3.0.0", tag = "v3.0.0"),
            app(5, "Nightly", installedVersion = "20260101", tag = "nightly"),
            app(6, "Quiet", installedVersion = "1.0.0", tag = null),
            app(7, "Delta", installedVersion = null, tag = "v1.0.0"),
        )

    private fun app(
        id: Long,
        name: String,
        installedVersion: String?,
        tag: String? = "v1.0.0",
    ): TrackedApp =
        TestData.trackedApp(
            id = id,
            repository = GitHubRepository("octo", name),
            latestRelease = tag?.let { TestData.release(id, it) },
            installedApp = installedVersion?.let { TestData.installedApp("com.example.app$id", name, it) },
        )
}
