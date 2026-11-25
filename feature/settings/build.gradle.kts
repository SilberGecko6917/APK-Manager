plugins {
    alias(libs.plugins.apkmanager.android.feature)
    alias(libs.plugins.apkmanager.android.screenshot)
}

android {
    namespace = "me.geckotv.apkmanager.feature.settings"
}

dependencies {
    implementation(projects.core.datastore)
}
