plugins {
    alias(libs.plugins.apkmanager.android.feature)
    alias(libs.plugins.apkmanager.android.screenshot)
}

android {
    namespace = "me.geckotv.apkmanager.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(projects.core.datastore)
}
