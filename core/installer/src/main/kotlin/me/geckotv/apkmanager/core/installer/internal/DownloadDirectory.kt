package me.geckotv.apkmanager.core.installer.internal

import javax.inject.Qualifier

/** The directory downloads are written to before they are installed. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DownloadDirectory
