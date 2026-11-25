plugins {
    alias(libs.plugins.apkmanager.android.library)
    alias(libs.plugins.apkmanager.hilt)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "me.geckotv.apkmanager.core.database"
    testOptions.unitTests.isIncludeAndroidResources = true
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
