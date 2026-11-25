package me.geckotv.apkmanager.feature.apps

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.testing.FakeInstalledAppsRepository
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val trackedApps = FakeTrackedAppsRepository()
    private val installedApps = FakeInstalledAppsRepository()
    private lateinit var viewModel: AppsViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = AppsViewModel(trackedApps, installedApps)
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
            assertEquals(listOf("alpha", "Zeta"), groups[0].apps.map { it.displayName })
            assertEquals(listOf("Beta", "gamma", "Nightly", "Quiet"), groups[1].apps.map { it.displayName })
            assertEquals(listOf("Delta"), groups[2].apps.map { it.displayName })
        }

    @Test
    fun unknownAndNoReleaseCountAsUpToDate() =
        runTest {
            collectUiState()

            trackedApps.setApps(sampleApps())

            val upToDate = ready().groups.first { it.kind == AppGroupKind.UpToDate }.apps
            assertTrue(upToDate.any { it.displayName == "Nightly" })
            assertTrue(upToDate.any { it.displayName == "Quiet" })
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

            AppsViewModel(trackedApps, installedApps)

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
    fun selectingAnAppOpensItsActions() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())

            viewModel.selectApp(1)

            val actions = ready().actions
            assertNotNull(actions)
            assertEquals("alpha", actions?.app?.displayName)
            assertNull(actions?.installedApps)

            viewModel.dismissApp()
            assertNull(ready().actions)
        }

    @Test
    fun actionsFollowChangesOfTheApp() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())
            viewModel.selectApp(1)

            viewModel.setIncludePrereleases(true)

            assertEquals(true, ready().actions?.app?.includePrereleases)
        }

    @Test
    fun packagePickerListsInstalledApps() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())
            installedApps.install(TestData.installedApp("com.example.one", "One", "1.0"))
            viewModel.selectApp(1)

            viewModel.showPackagePicker()
            assertEquals(listOf("com.example.one"), ready().actions?.installedApps?.map { it.packageName })

            viewModel.closePackagePicker()
            assertNull(ready().actions?.installedApps)
        }

    @Test
    fun linkingAnInstalledAppClosesThePicker() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())
            viewModel.selectApp(1)
            viewModel.showPackagePicker()

            viewModel.setPackageName("com.example.one")

            assertEquals("com.example.one", trackedApps.trackedApps.first { it.id == 1L }.packageName)
            assertNull(ready().actions?.installedApps)
        }

    @Test
    fun removingAnAppClosesItsActions() =
        runTest {
            collectUiState()
            trackedApps.setApps(sampleApps())
            viewModel.selectApp(1)

            viewModel.removeApp()

            assertNull(ready().actions)
            assertFalse(trackedApps.trackedApps.any { it.id == 1L })
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
