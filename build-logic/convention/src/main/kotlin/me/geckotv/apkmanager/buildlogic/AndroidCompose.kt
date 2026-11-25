package me.geckotv.apkmanager.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Enables Jetpack Compose and adds the Compose BOM plus tooling dependencies. */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.buildFeatures.compose = true

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
    }

    configure<KotlinAndroidProjectExtension> {
        compilerOptions.optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        val enableMetrics = providers.gradleProperty("enableComposeCompilerMetrics").map(String::toBoolean)
        val enableReports = providers.gradleProperty("enableComposeCompilerReports").map(String::toBoolean)
        val composeReportsDir = layout.buildDirectory.dir("compose")
        metricsDestination.set(composeReportsDir.zip(enableMetrics) { dir, enabled -> dir.takeIf { enabled } })
        reportsDestination.set(composeReportsDir.zip(enableReports) { dir, enabled -> dir.takeIf { enabled } })
    }
}
