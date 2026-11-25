package me.geckotv.apkmanager.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import me.geckotv.apkmanager.R
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.feature.apps.navigation.AppsNavKey
import me.geckotv.apkmanager.feature.settings.navigation.SettingsNavKey

/** Destinations shown in the floating navigation toolbar. The first one is the start destination. */
enum class TopLevelDestination(
    val navKey: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
) {
    Apps(
        navKey = AppsNavKey,
        label = R.string.destination_apps,
        icon = ApkManagerIcons.Apps,
        selectedIcon = ApkManagerIcons.AppsFilled,
    ),
    Settings(
        navKey = SettingsNavKey,
        label = R.string.destination_settings,
        icon = ApkManagerIcons.Settings,
        selectedIcon = ApkManagerIcons.SettingsFilled,
    ),
}
