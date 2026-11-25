package me.geckotv.apkmanager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.geckotv.apkmanager.core.designsystem.component.NavigationToolbar
import me.geckotv.apkmanager.core.designsystem.component.NavigationToolbarItem
import me.geckotv.apkmanager.feature.account.AccountRoute
import me.geckotv.apkmanager.feature.account.navigation.AccountNavKey
import me.geckotv.apkmanager.feature.apps.AppsScreen
import me.geckotv.apkmanager.feature.apps.navigation.AppsNavKey
import me.geckotv.apkmanager.feature.onboarding.OnboardingRoute
import me.geckotv.apkmanager.feature.settings.SettingsRoute
import me.geckotv.apkmanager.feature.settings.navigation.SettingsNavKey
import me.geckotv.apkmanager.navigation.TopLevelDestination
import me.geckotv.apkmanager.navigation.currentTopLevelDestination
import me.geckotv.apkmanager.navigation.navigateToTopLevel

/** Space screens keep free at their bottom so the floating navigation toolbar does not cover content. */
private val NavigationToolbarClearance = 96.dp

/**
 * Root composable: shows the onboarding on first launch, then the main navigation.
 */
@Composable
fun ApkManagerApp(
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = onboardingCompleted,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding",
        ) { completed ->
            if (completed) {
                MainNavigation()
            } else {
                OnboardingRoute()
            }
        }
    }
}

@Composable
private fun MainNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TopLevelDestination.entries.first().navKey)
    val currentDestination = backStack.currentTopLevelDestination()
    // The toolbar is only shown on top-level destinations, not on detail screens such as the account.
    val showNavigationToolbar = TopLevelDestination.entries.any { it.navKey == backStack.lastOrNull() }

    Box(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith fadeOut() },
            entryProvider =
                entryProvider {
                    entry<AppsNavKey> { AppsScreen(bottomContentPadding = NavigationToolbarClearance) }
                    entry<SettingsNavKey> {
                        SettingsRoute(
                            onOpenAccount = { backStack.add(AccountNavKey) },
                            bottomContentPadding = NavigationToolbarClearance,
                        )
                    }
                    entry<AccountNavKey> { AccountRoute(onBack = { backStack.removeLastOrNull() }) }
                },
        )

        AnimatedVisibility(
            visible = showNavigationToolbar,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            MainNavigationToolbar(
                currentDestination = currentDestination,
                onDestinationSelect = { destination -> backStack.navigateToTopLevel(destination) },
            )
        }
    }
}

@Composable
private fun MainNavigationToolbar(
    currentDestination: TopLevelDestination,
    onDestinationSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationToolbar(
        items =
            TopLevelDestination.entries.map { destination ->
                NavigationToolbarItem(
                    label = stringResource(destination.label),
                    icon = destination.icon,
                    selectedIcon = destination.selectedIcon,
                )
            },
        selectedIndex = currentDestination.ordinal,
        onItemSelect = { index -> onDestinationSelect(TopLevelDestination.entries[index]) },
        modifier = modifier,
    )
}
