package me.geckotv.apkmanager

import androidx.navigation3.runtime.NavKey
import me.geckotv.apkmanager.feature.apps.navigation.AppsNavKey
import me.geckotv.apkmanager.feature.settings.navigation.SettingsNavKey
import me.geckotv.apkmanager.navigation.TopLevelDestination
import me.geckotv.apkmanager.navigation.currentTopLevelDestination
import me.geckotv.apkmanager.navigation.navigateToTopLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelNavigationTest {
    @Test
    fun navigatingToAnotherDestinationKeepsTheStartDestinationBelow() {
        val backStack = mutableListOf<NavKey>(AppsNavKey)

        backStack.navigateToTopLevel(TopLevelDestination.Settings)

        assertEquals(listOf(AppsNavKey, SettingsNavKey), backStack)
        assertEquals(TopLevelDestination.Settings, backStack.currentTopLevelDestination())
    }

    @Test
    fun navigatingToTheStartDestinationClearsTheBackStack() {
        val backStack = mutableListOf<NavKey>(AppsNavKey, SettingsNavKey)

        backStack.navigateToTopLevel(TopLevelDestination.Apps)

        assertEquals(listOf<NavKey>(AppsNavKey), backStack)
    }

    @Test
    fun navigatingToTheCurrentDestinationDoesNothing() {
        val backStack = mutableListOf<NavKey>(AppsNavKey, SettingsNavKey)

        backStack.navigateToTopLevel(TopLevelDestination.Settings)

        assertEquals(listOf(AppsNavKey, SettingsNavKey), backStack)
    }
}
