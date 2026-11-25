plugins {
    alias(libs.plugins.apkmanager.android.application)
    alias(libs.plugins.apkmanager.android.application.compose)
    alias(libs.plugins.apkmanager.hilt)
}

android {
    namespace = "me.geckotv.apkmanager"

    defaultConfig {
        applicationId = "me.geckotv.apkmanager"
        versionCode = 1
        versionName = "0.1.0-dev"

        val githubClientId = providers.gradleProperty("apkmanager.github.clientId").get()
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$githubClientId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // F-Droid and IzzyOnDroid reject APKs that contain the encrypted dependency info block.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.account)
    implementation(projects.feature.apps)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
