plugins {
    alias(libs.plugins.apkmanager.android.library)
    alias(libs.plugins.apkmanager.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "me.geckotv.apkmanager.core.network"
}

dependencies {
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)

    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
