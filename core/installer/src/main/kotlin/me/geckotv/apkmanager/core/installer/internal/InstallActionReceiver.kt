package me.geckotv.apkmanager.core.installer.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import me.geckotv.apkmanager.core.installer.AppInstaller
import javax.inject.Inject

/** Handles the "Cancel" action of install notifications. */
@AndroidEntryPoint
internal class InstallActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var appInstaller: AppInstaller

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_CANCEL) return
        val appId = intent.getLongExtra(EXTRA_APP_ID, -1)
        if (appId != -1L) appInstaller.cancel(appId)
    }

    companion object {
        const val ACTION_CANCEL = "me.geckotv.apkmanager.action.CANCEL_INSTALL"
        const val EXTRA_APP_ID = "app_id"
    }
}
