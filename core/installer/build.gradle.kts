plugins {
    alias(libs.plugins.apkmanager.android.library)
    alias(libs.plugins.apkmanager.hilt)
}

android {
    namespace = "me.geckotv.apkmanager.core.installer"
    testOptions.unitTests.isIncludeAndroidResources = true
    buildFeatures.aidl = true
    defaultConfig.consumerProguardFiles("consumer-rules.pro")
}

dependencies {
    api(projects.core.model)

    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
