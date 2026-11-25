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
    implementation(projects.core.common)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)

    testImplementation(libs.junit4)
}
