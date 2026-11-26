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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.geckotv.apkmanager.core.designsystem.component.NavigationToolbar
import me.geckotv.apkmanager.core.designsystem.component.NavigationToolbarItem
import me.geckotv.apkmanager.feature.account.AccountRoute
import me.geckotv.apkmanager.feature.account.navigation.AccountNavKey
import me.geckotv.apkmanager.feature.apps.AddAppFloatingActionButton
import me.geckotv.apkmanager.feature.apps.AppsRoute
import me.geckotv.apkmanager.feature.apps.InstallConfirmations
import me.geckotv.apkmanager.feature.apps.details.AppDetailsPlaceholder
import me.geckotv.apkmanager.feature.apps.details.AppDetailsRoute
import me.geckotv.apkmanager.feature.apps.navigation.AppDetailsNavKey
import me.geckotv.apkmanager.feature.apps.navigation.AppsNavKey
import me.geckotv.apkmanager.feature.onboarding.OnboardingRoute
import me.geckotv.apkmanager.feature.settings.SettingsRoute
import me.geckotv.apkmanager.feature.settings.navigation.SettingsNavKey
import me.geckotv.apkmanager.navigation.TopLevelDestination
import me.geckotv.apkmanager.navigation.currentTopLevelDestination
import me.geckotv.apkmanager.navigation.navigateToTopLevel
import me.geckotv.apkmanager.navigation.openAppDetails

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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun MainNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TopLevelDestination.entries.first().navKey)
    val currentDestination = backStack.currentTopLevelDestination()
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    // On wide windows, the details of an app are shown next to the apps list.
    val isTwoPane = listDetailStrategy.directive.maxHorizontalPartitions > 1
    val showsAppDetailsNextToList = isTwoPane && backStack.lastOrNull() is AppDetailsNavKey
    // The toolbar is only shown on top-level destinations, not on detail screens such as the account. It stays
    // while the apps list is shown with the details of an app next to it.
    val showNavigationToolbar =
        TopLevelDestination.entries.any { it.navKey == backStack.lastOrNull() } ||
            (showsAppDetailsNextToList && currentDestination == TopLevelDestination.Apps)
    var showAddAppSheet by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        MainNavDisplay(
            backStack = backStack,
            sceneStrategy = listDetailStrategy,
            isTwoPane = isTwoPane,
            showAddAppSheet = showAddAppSheet,
            onShowAddAppSheetChange = { showAddAppSheet = it },
        )

        // Install confirmations come from the system dialog, whatever screen is shown.
        InstallConfirmations()

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
                onAddAppClick = { showAddAppSheet = true },
            )
        }
    }
}

/** The screens of the app. Wide windows show the details of an app next to the apps list. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun MainNavDisplay(
    backStack: NavBackStack<NavKey>,
    sceneStrategy: ListDetailSceneStrategy<NavKey>,
    isTwoPane: Boolean,
    showAddAppSheet: Boolean,
    onShowAddAppSheetChange: (Boolean) -> Unit,
) {
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
        sceneStrategies = listOf(sceneStrategy),
        entryProvider =
            entryProvider {
                entry<AppsNavKey>(
                    metadata =
                        ListDetailSceneStrategy.listPane(
                            detailPlaceholder = { AppDetailsPlaceholder() },
                        ),
                ) {
                    AppsRoute(
                        showAddAppSheet = showAddAppSheet,
                        onAddAppSheetDismiss = { onShowAddAppSheetChange(false) },
                        onAddAppClick = { onShowAddAppSheetChange(true) },
                        onAppClick = { appId -> backStack.openAppDetails(appId) },
                        bottomContentPadding = NavigationToolbarClearance,
                    )
                }
                entry<AppDetailsNavKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    AppDetailsRoute(
                        appId = key.appId,
                        onBack = { backStack.removeLastOrNull() },
                        bottomContentPadding = if (isTwoPane) NavigationToolbarClearance else 0.dp,
                    )
                }
                entry<SettingsNavKey> {
                    SettingsRoute(
                        onOpenAccount = { backStack.add(AccountNavKey) },
                        bottomContentPadding = NavigationToolbarClearance,
                    )
                }
                entry<AccountNavKey> { AccountRoute(onBack = { backStack.removeLastOrNull() }) }
            },
    )
}

@Composable
private fun MainNavigationToolbar(
    currentDestination: TopLevelDestination,
    onDestinationSelect: (TopLevelDestination) -> Unit,
    onAddAppClick: () -> Unit,
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
        // Apps are added from the apps list only.
        floatingActionButton =
            if (currentDestination == TopLevelDestination.Apps) {
                { AddAppFloatingActionButton(onClick = onAddAppClick) }
            } else {
                null
            },
    )
}
