import com.android.build.api.dsl.ApplicationExtension
import me.geckotv.apkmanager.buildlogic.configureKotlinAndroid
import me.geckotv.apkmanager.buildlogic.intVersion
import me.geckotv.apkmanager.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.intVersion("targetSdk")
                lint {
                    abortOnError = true
                    checkDependencies = true
                    xmlReport = false
                    sarifReport = true
                }
            }
        }
    }
}
