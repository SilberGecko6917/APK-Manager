package me.geckotv.apkmanager.feature.apps

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.repository.AddAppOptions
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.testing.FakeDeviceInfo
import me.geckotv.apkmanager.core.testing.FakeInstalledAppsRepository
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = GitHubRepository("octo", "app")
    private val trackedApps = FakeTrackedAppsRepository()
    private val installedApps = FakeInstalledAppsRepository()
    private lateinit var viewModel: AddAppViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = AddAppViewModel(trackedApps, installedApps, FakeDeviceInfo())
    }

    @Test
    fun startsWithAnEmptyInput() {
        val state = viewModel.uiState.value

        assertEquals("", state.input)
        assertNull(state.inputError)
        assertEquals(AddAppStep.Input, state.step)
    }

    @Test
    fun invalidInputShowsAnError() {
        viewModel.setInput("not a repository")

        viewModel.lookUp()

        assertEquals(AddAppInputError.Invalid, viewModel.uiState.value.inputError)
        assertEquals(AddAppStep.Input, viewModel.uiState.value.step)
    }

    @Test
    fun typingClearsTheError() {
        viewModel.setInput("nope")
        viewModel.lookUp()

        viewModel.setInput("octo/app")

        assertNull(viewModel.uiState.value.inputError)
    }

    @Test
    fun alreadyTrackedRepositoryIsNotLookedUp() =
        runTest {
            trackedApps.setApps(listOf(TestData.trackedApp(1, repository)))
            viewModel.setInput("https://github.com/OCTO/App")

            viewModel.lookUp()

            assertEquals(AddAppInputError.AlreadyTracked, viewModel.uiState.value.inputError)
            assertEquals(AddAppStep.Input, viewModel.uiState.value.step)
        }

    @Test
    fun lookUpShowsThePreviewOfAUrl() =
        runTest {
            trackedApps.previews[repository] = SourceResult.Success(TestData.preview(repository))
            viewModel.setInput("https://github.com/octo/app/releases/tag/v1")

            viewModel.lookUp()

            val step = preview()
            assertEquals(repository, step.preview.repository)
            assertEquals("v1.0.0", step.release?.tagName)
            assertNull(step.packageName)
            assertFalse(step.isPickingPackage)
        }

    @Test
    fun lookUpErrorsAreShownWithTheirReason() =
        runTest {
            val errors =
                listOf(SourceError.NotFound, SourceError.Offline, SourceError.Unauthorized, SourceError.Unavailable)
            errors.forEach { error ->
                trackedApps.previews[repository] = SourceResult.Failure(error)
                viewModel.setInput("octo/app")

                viewModel.lookUp()

                assertEquals(AddAppStep.Failed(error), viewModel.uiState.value.step)
                viewModel.back()
            }
        }

    @Test
    fun backFromAnErrorKeepsTheInputAndRetryLooksUpAgain() =
        runTest {
            viewModel.setInput("octo/app")
            viewModel.lookUp()
            assertEquals(AddAppStep.Failed(SourceError.NotFound), viewModel.uiState.value.step)

            viewModel.back()
            assertEquals(AddAppStep.Input, viewModel.uiState.value.step)
            assertEquals("octo/app", viewModel.uiState.value.input)

            trackedApps.previews[repository] = SourceResult.Success(TestData.preview(repository))
            viewModel.lookUp()
            assertTrue(viewModel.uiState.value.step is AddAppStep.Preview)
        }

    @Test
    fun singleCompatibleApkNeedsNoChoice() =
        runTest {
            lookUp(release("v1", "app-arm64-v8a.apk", "app-x86_64.apk"))

            val step = preview()
            assertFalse(step.hasAssetChoice)
            assertEquals("app-arm64-v8a.apk", step.selectedAsset?.name)
            assertEquals(listOf(true, false), step.candidates.map { it.isCompatible })
        }

    @Test
    fun severalCompatibleApksPreselectTheBestOne() =
        runTest {
            lookUp(release("v1", "app-universal.apk", "app-arm64-v8a.apk", "app-x86_64.apk"))

            val step = preview()
            assertTrue(step.hasAssetChoice)
            assertEquals("app-arm64-v8a.apk", step.selectedAsset?.name)
            assertNull(step.chosenAssetName)
        }

    @Test
    fun addingWithoutChoosingDoesNotRememberAnAsset() =
        runTest {
            lookUp(release("v1", "app-universal.apk", "app-arm64-v8a.apk"))

            viewModel.add()

            assertEquals(
                listOf(AddAppOptions(null, includePrereleases = false, preferredAssetName = null)),
                trackedApps.addedOptions,
            )
        }

    @Test
    fun addingRemembersTheAssetTheUserChose() =
        runTest {
            lookUp(release("v1", "app-universal.apk", "app-arm64-v8a.apk"))

            viewModel.selectAsset("app-universal.apk")
            assertEquals("app-universal.apk", preview().selectedAsset?.name)
            viewModel.add()

            assertEquals("app-universal.apk", trackedApps.addedOptions.single().preferredAssetName)
        }

    @Test
    fun releaseWithoutApkCanStillBeAdded() =
        runTest {
            lookUp(release("v1", "checksums.txt", "app.aab"))

            val step = preview()
            assertTrue(step.candidates.isEmpty())
            assertNull(step.selectedAsset)

            viewModel.add()
            assertEquals(1, trackedApps.trackedApps.size)
        }

    @Test
    fun repositoryWithoutReleaseCanStillBeAdded() =
        runTest {
            trackedApps.previews[repository] =
                SourceResult.Success(TestData.preview(repository, releases = emptyList()))
            viewModel.setInput("octo/app")
            viewModel.lookUp()

            assertNull(preview().release)

            viewModel.add()
            assertNull(trackedApps.trackedApps.single().latestRelease)
        }

    @Test
    fun includingPreReleasesSwitchesTheLatestRelease() =
        runTest {
            val stable = TestData.release(1, "v1.0.0")
            val beta = TestData.release(2, "v2.0.0-beta.1", isPrerelease = true)
            lookUp(beta, stable)
            assertEquals("v1.0.0", preview().release?.tagName)

            viewModel.setIncludePrereleases(true)
            assertEquals("v2.0.0-beta.1", preview().release?.tagName)

            viewModel.add()
            assertTrue(trackedApps.addedOptions.single().includePrereleases)
            assertEquals(
                "v2.0.0-beta.1",
                trackedApps.trackedApps
                    .single()
                    .latestRelease
                    ?.tagName,
            )
        }

    @Test
    fun suggestsTheOnlyMatchingInstalledApp() =
        runTest {
            installedApps.install(TestData.installedApp("org.example.octoapp", "Octo App", "1.0"))
            installedApps.install(TestData.installedApp("org.example.other", "Other", "1.0"))

            lookUp(release("v1", "app.apk"))

            assertEquals("org.example.octoapp", preview().packageName)
        }

    @Test
    fun suggestionIgnoresCaseSpacesAndHyphens() {
        val apps = listOf(TestData.installedApp("org.example.x", "My Cool App", "1.0"))

        assertEquals("org.example.x", suggestInstalledApp("my-cool-app", apps)?.packageName)
        assertEquals("org.example.x", suggestInstalledApp("MyCoolApp", apps)?.packageName)
        assertNull(suggestInstalledApp("something-else", apps))
    }

    @Test
    fun noSuggestionIfSeveralAppsMatchOrOnlySystemAppsDo() {
        val several =
            listOf(
                TestData.installedApp("org.example.app", "App", "1.0"),
                TestData.installedApp("org.example.app.beta", "App Beta", "1.0"),
            )
        val system = listOf(TestData.installedApp("com.android.app", "App", "1.0", isSystemApp = true))

        assertNull(suggestInstalledApp("app", several))
        assertNull(suggestInstalledApp("app", system))
    }

    @Test
    fun packagePickerSelectsOrUnlinksAnApp() =
        runTest {
            installedApps.install(TestData.installedApp("org.example.octoapp", "Octo App", "1.0"))
            installedApps.install(TestData.installedApp("org.example.mine", "Mine", "2.0"))
            lookUp(release("v1", "app.apk"))

            viewModel.showPackagePicker()
            assertTrue(preview().isPickingPackage)
            viewModel.selectPackage("org.example.mine")
            assertEquals("Mine", preview().installedApp?.label)
            assertFalse(preview().isPickingPackage)

            viewModel.showPackagePicker()
            viewModel.back()
            assertFalse(preview().isPickingPackage)
            assertEquals("org.example.mine", preview().packageName)

            viewModel.selectPackage(null)
            assertNull(preview().installedApp)
        }

    @Test
    fun addingTracksTheAppAndResetsTheSheet() =
        runTest {
            val events = mutableListOf<AddAppEvent>()
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { viewModel.events.collect { events += it } }
            installedApps.install(TestData.installedApp("org.example.octoapp", "Octo App", "1.0"))
            lookUp(release("v1", "app.apk"))

            viewModel.add()

            val options = trackedApps.addedOptions.single()
            assertEquals("org.example.octoapp", options.packageName)
            assertEquals(1, trackedApps.trackedApps.size)
            assertEquals(listOf<AddAppEvent>(AddAppEvent.Added("Octo App")), events)
            assertEquals(AddAppUiState(), viewModel.uiState.value)
        }

    @Test
    fun resetForgetsEverything() =
        runTest {
            lookUp(release("v1", "app.apk"))

            viewModel.reset()

            assertEquals(AddAppUiState(), viewModel.uiState.value)
        }

    /** Looks up the repository, whose [releases] are listed newest first. */
    private fun lookUp(vararg releases: Release) {
        trackedApps.previews[repository] =
            SourceResult.Success(TestData.preview(repository, releases = releases.toList()))
        viewModel.setInput("octo/app")
        viewModel.lookUp()
    }

    private fun release(
        tag: String,
        vararg assetNames: String,
    ): Release =
        TestData.release(
            id = 1,
            tag = tag,
            assets = assetNames.mapIndexed { index, name -> TestData.asset(index + 1L, name) },
        )

    private fun preview(): AddAppStep.Preview = viewModel.uiState.value.step as AddAppStep.Preview
}
