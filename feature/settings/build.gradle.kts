plugins {
    alias(libs.plugins.apkmanager.android.feature)
    alias(libs.plugins.apkmanager.android.screenshot)
}

android {
    namespace = "me.geckotv.apkmanager.feature.settings"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.datastore)
    implementation(libs.coil.compose)
}
