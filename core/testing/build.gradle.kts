plugins {
    alias(libs.plugins.apkmanager.android.library)
}

android {
    namespace = "me.geckotv.apkmanager.core.testing"
}

dependencies {
    api(projects.core.datastore)
    api(projects.core.model)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
