plugins {
    alias(libs.plugins.apkmanager.android.feature)
    alias(libs.plugins.apkmanager.android.screenshot)
}

android {
    namespace = "me.geckotv.apkmanager.feature.account"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.network)
}
