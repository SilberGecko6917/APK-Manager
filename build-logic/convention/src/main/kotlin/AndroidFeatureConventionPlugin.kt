import me.geckotv.apkmanager.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Feature modules: an Android library with Compose, Hilt, Navigation 3 keys (kotlinx.serialization)
 * and the shared UI and data dependencies.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("apkmanager.android.library")
            pluginManager.apply("apkmanager.android.library.compose")
            pluginManager.apply("apkmanager.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:model"))

                add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-core").get())

                add("testImplementation", project(":core:testing"))
                add("testImplementation", libs.findLibrary("junit4").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
            }
        }
    }
}
