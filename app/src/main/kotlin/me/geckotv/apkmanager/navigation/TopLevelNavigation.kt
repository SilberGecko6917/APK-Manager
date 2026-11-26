package me.geckotv.apkmanager.navigation

import androidx.navigation3.runtime.NavKey
import me.geckotv.apkmanager.feature.apps.navigation.AppDetailsNavKey

/**
 * Switches to a top-level [destination].
 *
 * The start destination always stays at the bottom of the back stack, so pressing back on another
 * top-level destination returns to it before leaving the app.
 */
fun MutableList<NavKey>.navigateToTopLevel(destination: TopLevelDestination) {
    if (lastOrNull() == destination.navKey) return
    val start = TopLevelDestination.entries.first().navKey
    clear()
    add(start)
    if (destination.navKey != start) add(destination.navKey)
}

/** The top-level destination the back stack currently shows, or the start destination. */
fun List<NavKey>.currentTopLevelDestination(): TopLevelDestination =
    asReversed().firstNotNullOfOrNull { key -> TopLevelDestination.entries.firstOrNull { it.navKey == key } }
        ?: TopLevelDestination.entries.first()

/**
 * Shows the details of app [appId]. Next to the apps list, the details of another app replace the
 * ones that are shown instead of stacking up.
 */
fun MutableList<NavKey>.openAppDetails(appId: Long) {
    if (lastOrNull() is AppDetailsNavKey) removeAt(lastIndex)
    add(AppDetailsNavKey(appId))
}
