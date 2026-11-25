import com.android.build.api.dsl.LibraryExtension
import me.geckotv.apkmanager.buildlogic.configureKotlinAndroid
import me.geckotv.apkmanager.buildlogic.intVersion
import me.geckotv.apkmanager.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = libs.intVersion("targetSdk")
                lint.targetSdk = libs.intVersion("targetSdk")
                // Derive a unique resource prefix from the module path, e.g. ":core:common" -> "core_common_".
                resourcePrefix = path
                    .split("""\W""".toRegex())
                    .drop(1)
                    .distinct()
                    .joinToString("_")
                    .lowercase() + "_"
            }
        }
    }
}
