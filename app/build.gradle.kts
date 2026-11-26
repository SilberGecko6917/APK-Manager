plugins {
    alias(libs.plugins.apkmanager.android.application)
    alias(libs.plugins.apkmanager.android.application.compose)
    alias(libs.plugins.apkmanager.hilt)
}

android {
    namespace = "me.geckotv.apkmanager"

    defaultConfig {
        applicationId = "me.geckotv.apkmanager"
        // The release workflow derives both from the Git tag (see docs/RELEASING.md).
        versionCode = providers.gradleProperty("apkmanager.versionCode").map(String::toInt).getOrElse(1)
        versionName = providers.gradleProperty("apkmanager.versionName").getOrElse("0.1.0-dev")

        val githubClientId = providers.gradleProperty("apkmanager.github.clientId").get()
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$githubClientId\"")
        val selfRepository = providers.gradleProperty("apkmanager.github.repository").get()
        buildConfigField("String", "SELF_REPOSITORY", "\"$selfRepository\"")
    }

    // Release signing is configured through environment variables, so the keystore and its
    // passwords never live in the repository. Without them, release builds stay unsigned.
    val releaseKeystore = providers.environmentVariable("APKMANAGER_KEYSTORE_FILE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("APKMANAGER_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("APKMANAGER_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("APKMANAGER_KEY_PASSWORD").get()
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
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

    // ApkManagerApplicationTest starts the real application, which needs the app's resources.
    testOptions.unitTests.isIncludeAndroidResources = true

    // F-Droid and IzzyOnDroid reject APKs that contain the encrypted dependency info block.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.installer)
    implementation(projects.core.model)
    implementation(projects.core.work)
    implementation(projects.feature.account)
    implementation(projects.feature.apps)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.settings)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
