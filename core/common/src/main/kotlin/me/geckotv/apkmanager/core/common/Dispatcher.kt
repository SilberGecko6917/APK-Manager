package me.geckotv.apkmanager.core.common

import javax.inject.Qualifier

/** Qualifier for injecting a specific [kotlinx.coroutines.CoroutineDispatcher]. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(
    val dispatcher: ApkManagerDispatchers,
)

enum class ApkManagerDispatchers {
    Default,
    IO,
}
