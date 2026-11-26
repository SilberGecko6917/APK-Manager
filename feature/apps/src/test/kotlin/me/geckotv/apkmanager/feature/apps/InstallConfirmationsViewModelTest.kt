package me.geckotv.apkmanager.feature.apps

import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.testing.FakeAppInstaller
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric provides the android.content.Intent that install confirmations carry.
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class InstallConfirmationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val installer = FakeAppInstaller(stateAfterInstall = InstallState.WaitingForUser(Intent("confirm")))
    private lateinit var viewModel: InstallConfirmationsViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = InstallConfirmationsViewModel(installer)
    }

    @Test
    fun nothingIsPendingWithoutInstalls() =
        runTest {
            collectPending()

            assertNull(viewModel.pendingConfirmation.value)
        }

    @Test
    fun anInstallThatWaitsForTheUserIsPending() =
        runTest {
            collectPending()

            installer.install(app(1))

            val pending = viewModel.pendingConfirmation.value
            assertEquals(1L, pending?.appId)
            assertEquals("confirm", pending?.intent?.action)
        }

    @Test
    fun otherInstallStatesAreNotPending() =
        runTest {
            collectPending()
            installer.stateAfterInstall = InstallState.Verifying

            installer.install(app(1))

            assertNull(viewModel.pendingConfirmation.value)
        }

    @Test
    fun confirmationsThatAreShownAreNotPendingAnyMore() =
        runTest {
            collectPending()
            installer.install(app(1))

            viewModel.onConfirmationShown(1)

            assertEquals(listOf(1L), installer.confirmationsShown)
            assertNull(viewModel.pendingConfirmation.value)
        }

    @Test
    fun showingTheConfirmationAgainMakesItPendingAgain() =
        runTest {
            collectPending()
            installer.install(app(1))
            viewModel.onConfirmationShown(1)

            installer.showConfirmationAgain(1)

            assertEquals(1L, viewModel.pendingConfirmation.value?.appId)
        }

    @Test
    fun theNextInstallFollowsOnceTheFirstIsShown() =
        runTest {
            collectPending()
            installer.install(app(1))
            installer.install(app(2))
            assertEquals(1L, viewModel.pendingConfirmation.value?.appId)

            viewModel.onConfirmationShown(1)

            assertEquals(2L, viewModel.pendingConfirmation.value?.appId)
        }

    @Test
    fun aConfirmationThatCannotBeShownCancelsTheInstall() =
        runTest {
            collectPending()
            installer.install(app(1))

            viewModel.onConfirmationFailed(1)

            assertNull(viewModel.pendingConfirmation.value)
            assertEquals(emptyMap<Long, InstallState>(), installer.installs.value)
        }

    private fun TestScope.collectPending() {
        // The state only collects its upstream while it has a subscriber.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.pendingConfirmation.collect() }
    }

    private fun app(id: Long) = TestData.trackedApp(id = id, repository = GitHubRepository("octo", "app$id"))
}
