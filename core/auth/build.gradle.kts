plugins {
    alias(libs.plugins.apkmanager.android.library)
    alias(libs.plugins.apkmanager.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "me.geckotv.apkmanager.core.auth"
}

dependencies {
    api(projects.core.model)

    implementation(projects.core.common)
    implementation(projects.core.network)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.turbine)
}
