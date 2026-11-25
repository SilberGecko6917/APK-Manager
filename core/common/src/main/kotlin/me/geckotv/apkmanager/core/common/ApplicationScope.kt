package me.geckotv.apkmanager.core.common

import javax.inject.Qualifier

/** Qualifier for the [kotlinx.coroutines.CoroutineScope] that lives as long as the application. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
