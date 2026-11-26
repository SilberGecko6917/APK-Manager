package me.geckotv.apkmanager.core.work

import me.geckotv.apkmanager.core.model.TrackedApp

/** Tells the user about available updates. */
interface UpdateNotifier {
    /**
     * Shows a notification for every app with an update that was not notified yet, and removes the
     * notifications of apps that are up to date now.
     */
    suspend fun notifyUpdates(apps: List<TrackedApp>)
}
