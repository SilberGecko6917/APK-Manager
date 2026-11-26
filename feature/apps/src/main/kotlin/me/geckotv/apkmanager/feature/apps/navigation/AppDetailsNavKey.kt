package me.geckotv.apkmanager.feature.apps.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation key of the details of the tracked app [appId]. */
@Serializable
data class AppDetailsNavKey(
    val appId: Long,
) : NavKey
