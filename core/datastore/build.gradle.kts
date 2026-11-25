plugins {
    alias(libs.plugins.apkmanager.android.library)
    alias(libs.plugins.apkmanager.hilt)
}

android {
    namespace = "me.geckotv.apkmanager.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.datastore.preferences)

    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
