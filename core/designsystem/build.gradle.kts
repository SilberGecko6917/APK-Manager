plugins {
    alias(libs.plugins.apkmanager.android.library)
    alias(libs.plugins.apkmanager.android.library.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "me.geckotv.apkmanager.core.designsystem"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)

    implementation(libs.materialkolor)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
