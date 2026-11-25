import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "me.geckotv.apkmanager.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id =
                libs.plugins.apkmanager.android.application
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id =
                libs.plugins.apkmanager.android.application.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.apkmanager.android.library
                    .asProvider()
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id =
                libs.plugins.apkmanager.android.library.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.apkmanager.android.feature
                    .get()
                    .pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidScreenshot") {
            id =
                libs.plugins.apkmanager.android.screenshot
                    .get()
                    .pluginId
            implementationClass = "AndroidScreenshotConventionPlugin"
        }
        register("hilt") {
            id =
                libs.plugins.apkmanager.hilt
                    .get()
                    .pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.apkmanager.jvm.library
                    .get()
                    .pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
