package me.geckotv.apkmanager.feature.apps.details

import android.content.Intent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UpdateStatus
import me.geckotv.apkmanager.core.testing.FakeAppInstaller
import me.geckotv.apkmanager.core.testing.FakeDeviceInfo
import me.geckotv.apkmanager.core.testing.FakeInstalledAppsRepository
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import me.geckotv.apkmanager.core.testing.TestData
import me.geckotv.apkmanager.feature.apps.AppsMessage
import me.geckotv.apkmanager.feature.apps.InstallAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric provides the android.content.Intent that install confirmations carry.
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val trackedApps =
        FakeTrackedAppsRepository(listOf(app(1, "alpha", "1.0.0", "v1.1.0"), app(2, "Beta", null)))
    private val installedApps = FakeInstalledAppsRepository()
    private val installer = FakeAppInstaller()
    private val deviceInfo = FakeDeviceInfo()
    private val userSettings = FakeUserSettingsRepository()
    private val releases = listOf(TestData.release(11, "v1.1.0"), TestData.release(10, "v1.0.0"))

    @Test
    fun showsTheAppWithItsInstallAction() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            val ready = viewModel.ready()
            assertEquals("alpha", ready.item.app.displayName)
            assertEquals(InstallAction.Update, ready.item.installAction)
            assertTrue(ready.item.hasCompatibleApk)
            assertNull(ready.installedApps)
            assertFalse(ready.isConfirmingRemove)
        }

    @Test
    fun anAppWithoutApkForThisDeviceCannotBeInstalled() =
        runTest {
            val release = TestData.release(7, "v1.0.0", assets = listOf(TestData.asset(70, "app-x86_64.apk")))
            trackedApps.setApps(listOf(app(3, "Gamma", null).copy(latestRelease = release)))
            val viewModel = createViewModel(appId = 3)
            collectUiState(viewModel)

            assertFalse(viewModel.ready().item.hasCompatibleApk)
            assertNull(viewModel.ready().item.installAction)
        }

    @Test
    fun loadsTheReleaseHistoryOnStart() =
        runTest {
            trackedApps.releaseHistories[1] = SourceResult.Success(releases)
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            assertEquals(ReleaseHistoryState.Loaded(releases), viewModel.ready().history)
        }

    @Test
    fun failedHistoryCanBeLoadedAgain() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            assertEquals(ReleaseHistoryState.Failed(SourceError.NotFound), viewModel.ready().history)

            trackedApps.releaseHistories[1] = SourceResult.Success(releases)
            viewModel.retryHistory()

            assertEquals(ReleaseHistoryState.Loaded(releases), viewModel.ready().history)
        }

    @Test
    fun historyIsLoadingWhileItIsFetched() =
        runTest {
            val history = CompletableDeferred<SourceResult<List<Release>>>()
            val repository =
                object : TrackedAppsRepository by trackedApps {
                    override suspend fun releaseHistory(id: Long) = history.await()
                }
            val viewModel = AppDetailsViewModel(1, repository, installedApps, installer, deviceInfo, userSettings)
            collectUiState(viewModel)
            assertEquals(ReleaseHistoryState.Loading, viewModel.ready().history)

            history.complete(SourceResult.Success(releases))

            assertEquals(ReleaseHistoryState.Loaded(releases), viewModel.ready().history)
        }

    @Test
    fun releasesExpandOneAtATime() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            viewModel.toggleRelease(11)
            assertEquals(11L, viewModel.ready().expandedReleaseId)

            viewModel.toggleRelease(10)
            assertEquals(10L, viewModel.ready().expandedReleaseId)

            viewModel.toggleRelease(10)
            assertNull(viewModel.ready().expandedReleaseId)
        }

    @Test
    fun installsTheLatestRelease() =
        runTest {
            val viewModel = createViewModel(appId = 2)
            collectUiState(viewModel)
            assertEquals(InstallAction.Install, viewModel.ready().item.installAction)

            viewModel.install()

            assertEquals(listOf(2L), installer.installedApps.map { it.id })
            assertTrue(viewModel.ready().item.install is InstallState.Downloading)
            assertNull(viewModel.ready().item.installAction)
        }

    @Test
    fun updatesTheApp() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            viewModel.install()

            assertEquals(listOf(1L), installer.installedApps.map { it.id })
        }

    @Test
    fun confirmShowsTheConfirmationAgain() =
        runTest {
            installer.stateAfterInstall = InstallState.WaitingForUser(Intent("confirm"))
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.install()
            installer.onConfirmationShown(1)
            assertEquals(InstallAction.Confirm, viewModel.ready().item.installAction)

            viewModel.install()

            assertEquals(false, (installer.installs.value[1] as InstallState.WaitingForUser).isShown)
            assertEquals(1, installer.installedApps.size)
        }

    @Test
    fun cancelStopsTheInstall() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.install()

            viewModel.cancelInstall()

            assertNull(viewModel.ready().item.install)
            assertEquals(InstallAction.Update, viewModel.ready().item.installAction)
        }

    @Test
    fun includingPrereleasesChangesTheApp() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            viewModel.setIncludePrereleases(true)

            assertTrue(
                viewModel
                    .ready()
                    .item.app.includePrereleases,
            )
        }

    @Test
    fun failedUpdateCheckAfterChangingTheSettingIsReported() =
        runTest {
            trackedApps.updateCheckResult =
                UpdateCheckResult(checkedCount = 0, failures = mapOf(1L to SourceError.Offline))
            val viewModel = createViewModel(appId = 1)
            val events = collectEvents(viewModel)

            viewModel.setIncludePrereleases(true)

            assertEquals(listOf<AppDetailsEvent>(AppDetailsEvent.ShowMessage(AppsMessage.Offline)), events)
        }

    @Test
    fun packagePickerListsInstalledApps() =
        runTest {
            installedApps.install(TestData.installedApp("com.example.one", "One", "1.0"))
            val viewModel = createViewModel(appId = 2)
            collectUiState(viewModel)

            viewModel.showPackagePicker()
            assertEquals(listOf("com.example.one"), viewModel.ready().installedApps?.map { it.packageName })

            viewModel.closePackagePicker()
            assertNull(viewModel.ready().installedApps)
        }

    @Test
    fun linkingAnInstalledAppClosesThePicker() =
        runTest {
            val viewModel = createViewModel(appId = 2)
            collectUiState(viewModel)
            viewModel.showPackagePicker()

            viewModel.setPackageName("com.example.one")

            assertEquals(
                "com.example.one",
                viewModel
                    .ready()
                    .item.app.packageName,
            )
            assertNull(viewModel.ready().installedApps)
        }

    @Test
    fun unlinkingAnAppClearsItsPackageName() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            viewModel.setPackageName(null)

            assertNull(
                viewModel
                    .ready()
                    .item.app.packageName,
            )
        }

    @Test
    fun skippingTheLatestReleaseHidesTheUpdate() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            assertEquals(InstallAction.Update, viewModel.ready().item.installAction)

            viewModel.skipLatestRelease()

            val app = viewModel.ready().item.app
            assertEquals(1L, app.skippedReleaseId)
            assertEquals(UpdateStatus.UpToDate, app.updateStatus)
            assertNull(viewModel.ready().item.installAction)
        }

    @Test
    fun undoingTheSkipBringsTheUpdateBack() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.skipLatestRelease()

            viewModel.undoSkip()

            assertNull(
                viewModel
                    .ready()
                    .item.app.skippedReleaseId,
            )
            assertEquals(InstallAction.Update, viewModel.ready().item.installAction)
        }

    @Test
    fun theAssetFilterSheetOpensWithTheDeviceAbis() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            assertNull(viewModel.ready().optionsSheet)

            viewModel.showAssetFilter()
            assertEquals(OptionsSheet.AssetFilterSheet(deviceInfo.supportedAbis), viewModel.ready().optionsSheet)

            viewModel.dismissOptionsSheet()
            assertNull(viewModel.ready().optionsSheet)
        }

    @Test
    fun savingTheAssetFilterChangesWhichApkCounts() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.showAssetFilter()

            viewModel.saveAssetFilter(include = " foss ", exclude = "")

            assertEquals(
                AssetFilter(include = "foss", exclude = null),
                viewModel
                    .ready()
                    .item.app.assetFilter,
            )
            assertFalse(viewModel.ready().item.hasCompatibleApk)
            assertNull(viewModel.ready().optionsSheet)
        }

    @Test
    fun anInvalidAssetFilterIsNotSaved() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.showAssetFilter()

            viewModel.saveAssetFilter(include = "foss", exclude = "(debug")

            assertTrue(
                viewModel
                    .ready()
                    .item.app.assetFilter.isEmpty,
            )
            assertTrue(viewModel.ready().optionsSheet is OptionsSheet.AssetFilterSheet)
        }

    @Test
    fun savingBlankPatternsResetsTheAssetFilter() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.saveAssetFilter(include = "foss", exclude = "debug")
            assertFalse(
                viewModel
                    .ready()
                    .item.app.assetFilter.isEmpty,
            )

            viewModel.saveAssetFilter(include = "", exclude = "  ")

            assertEquals(
                AssetFilter(),
                viewModel
                    .ready()
                    .item.app.assetFilter,
            )
            assertTrue(viewModel.ready().item.hasCompatibleApk)
        }

    @Test
    fun savingTheVersionPatternChangesHowTagsAreRead() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.showVersionPattern()
            assertEquals(OptionsSheet.VersionPatternSheet, viewModel.ready().optionsSheet)

            viewModel.saveVersionPattern(" v(.*) ")

            assertEquals(
                "v(.*)",
                viewModel
                    .ready()
                    .item.app.versionPattern,
            )
            assertNull(viewModel.ready().optionsSheet)
        }

    @Test
    fun anInvalidVersionPatternIsNotSaved() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.showVersionPattern()

            viewModel.saveVersionPattern("v(")

            assertNull(
                viewModel
                    .ready()
                    .item.app.versionPattern,
            )
            assertEquals(OptionsSheet.VersionPatternSheet, viewModel.ready().optionsSheet)
        }

    @Test
    fun savingABlankVersionPatternResetsIt() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.saveVersionPattern("v(.*)")

            viewModel.saveVersionPattern("")

            assertNull(
                viewModel
                    .ready()
                    .item.app.versionPattern,
            )
        }

    @Test
    fun theUpdatePolicySheetShowsTheDefaultFromTheSettings() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            assertEquals(UpdatePolicy.Notify, viewModel.ready().defaultUpdatePolicy)

            userSettings.setDefaultUpdatePolicy(UpdatePolicy.Install)
            viewModel.showUpdatePolicy()

            assertEquals(UpdatePolicy.Install, viewModel.ready().defaultUpdatePolicy)
            assertEquals(OptionsSheet.UpdatePolicySheet, viewModel.ready().optionsSheet)
        }

    @Test
    fun choosingAnUpdatePolicySavesItAndClosesTheSheet() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.showUpdatePolicy()

            viewModel.setUpdatePolicy(UpdatePolicy.Install)

            assertNull(viewModel.ready().optionsSheet)
            assertEquals(
                UpdatePolicy.Install,
                viewModel
                    .ready()
                    .item.app.updatePolicy,
            )

            viewModel.setUpdatePolicy(null)

            assertNull(
                viewModel
                    .ready()
                    .item.app.updatePolicy,
            )
        }

    @Test
    fun dismissingTheUpdatePolicySheetKeepsThePolicy() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)
            viewModel.showUpdatePolicy()

            viewModel.dismissOptionsSheet()

            assertNull(viewModel.ready().optionsSheet)
            assertNull(
                viewModel
                    .ready()
                    .item.app.updatePolicy,
            )
        }

    @Test
    fun automaticUpdatesOfAnAppFromAnotherInstallerNeedConfirmation() =
        runTest {
            setInstalledApp(installer = "com.android.vending")

            assertEquals(UpdateConfirmHint.AnotherInstaller, confirmHint())
        }

    @Test
    fun automaticUpdatesOfAnAppInstalledByApkManagerNeedNoConfirmation() =
        runTest {
            setInstalledApp(installer = deviceInfo.ownPackageName)

            assertNull(confirmHint())
        }

    @Test
    fun automaticUpdatesOfAnAppWithAnOldTargetNeedConfirmation() =
        runTest {
            setInstalledApp(installer = deviceInfo.ownPackageName, targetSdk = 21)

            assertEquals(UpdateConfirmHint.OldTargetSdk, confirmHint())
        }

    @Test
    fun automaticUpdatesBeforeAndroid12NeedConfirmation() =
        runTest {
            setInstalledApp(installer = deviceInfo.ownPackageName)

            assertEquals(UpdateConfirmHint.OldAndroid, confirmHint(FakeDeviceInfo(sdkInt = 30)))
        }

    @Test
    fun anAppThatIsNotInstalledHasNoConfirmHint() =
        runTest {
            val viewModel = createViewModel(appId = 2)
            collectUiState(viewModel)

            assertNull(viewModel.ready().confirmHint)
        }

    @Test
    fun removingAsksForConfirmationFirst() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            collectUiState(viewModel)

            viewModel.showRemoveConfirmation()
            assertTrue(viewModel.ready().isConfirmingRemove)

            viewModel.dismissRemoveConfirmation()
            assertFalse(viewModel.ready().isConfirmingRemove)
            assertTrue(trackedApps.trackedApps.any { it.id == 1L })
        }

    @Test
    fun removingTheAppClosesTheScreen() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            val events = collectEvents(viewModel)
            collectUiState(viewModel)

            viewModel.showRemoveConfirmation()
            viewModel.remove()

            assertFalse(trackedApps.trackedApps.any { it.id == 1L })
            assertEquals(listOf<AppDetailsEvent>(AppDetailsEvent.Closed), events)
            assertTrue(viewModel.uiState.value is AppDetailsUiState.Loading)
        }

    @Test
    fun theScreenClosesWhenTheAppIsRemovedElsewhere() =
        runTest {
            val viewModel = createViewModel(appId = 1)
            val events = collectEvents(viewModel)
            assertEquals(emptyList<AppDetailsEvent>(), events)

            trackedApps.setApps(trackedApps.trackedApps.filterNot { it.id == 1L })

            assertEquals(listOf<AppDetailsEvent>(AppDetailsEvent.Closed), events)
        }

    @Test
    fun theScreenClosesIfTheAppDoesNotExist() =
        runTest {
            val viewModel = createViewModel(appId = 99)
            val events = collectEvents(viewModel)

            assertEquals(listOf<AppDetailsEvent>(AppDetailsEvent.Closed), events)
        }

    /** Created after [MainDispatcherRule] has replaced the main dispatcher, so `viewModelScope` uses the test one. */
    private fun createViewModel(appId: Long) =
        AppDetailsViewModel(appId, trackedApps, installedApps, installer, deviceInfo, userSettings)

    private fun setInstalledApp(
        installer: String?,
        targetSdk: Int = 35,
    ) {
        val installed =
            TestData.installedApp("com.example.app1", "alpha", "1.0.0").copy(
                targetSdkVersion = targetSdk,
                installerPackageName = installer,
            )
        trackedApps.setApps(listOf(app(1, "alpha", null).copy(installedApp = installed)))
    }

    private fun TestScope.confirmHint(device: FakeDeviceInfo = deviceInfo): UpdateConfirmHint? {
        val viewModel = AppDetailsViewModel(1, trackedApps, installedApps, installer, device, userSettings)
        collectUiState(viewModel)
        return viewModel.ready().confirmHint
    }

    private fun TestScope.collectUiState(viewModel: AppDetailsViewModel) {
        // uiState only collects its upstream while it has a subscriber.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun TestScope.collectEvents(viewModel: AppDetailsViewModel): List<AppDetailsEvent> {
        val events = mutableListOf<AppDetailsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        return events
    }

    private fun AppDetailsViewModel.ready(): AppDetailsUiState.Ready = uiState.value as AppDetailsUiState.Ready

    private fun app(
        id: Long,
        name: String,
        installedVersion: String?,
        tag: String = "v1.0.0",
    ): TrackedApp =
        TestData.trackedApp(
            id = id,
            repository = GitHubRepository("octo", name),
            latestRelease = TestData.release(id, tag),
            installedApp = installedVersion?.let { TestData.installedApp("com.example.app$id", name, it) },
        )
}
