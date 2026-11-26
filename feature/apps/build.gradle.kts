plugins {
    alias(libs.plugins.apkmanager.android.feature)
    alias(libs.plugins.apkmanager.android.screenshot)
}

android {
    namespace = "me.geckotv.apkmanager.feature.apps"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.installer)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.markdown.renderer.m3)
}
