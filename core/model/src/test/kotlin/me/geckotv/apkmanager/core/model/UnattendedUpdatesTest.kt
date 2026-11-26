package me.geckotv.apkmanager.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnattendedUpdatesTest {
    private fun app(
        installer: String? = OWN_PACKAGE,
        targetSdk: Int = 35,
        packageName: String = "com.example.app",
    ) = InstalledApp(
        packageName = packageName,
        label = "App",
        versionName = "1.0",
        versionCode = 1,
        isSystemApp = false,
        targetSdkVersion = targetSdk,
        installerPackageName = installer,
    )

    @Test
    fun minimumTargetSdk_followsAndroidVersion() {
        assertEquals(29, UnattendedUpdates.minimumTargetSdk(31))
        assertEquals(29, UnattendedUpdates.minimumTargetSdk(32))
        assertEquals(30, UnattendedUpdates.minimumTargetSdk(33))
        assertEquals(31, UnattendedUpdates.minimumTargetSdk(34))
        assertEquals(33, UnattendedUpdates.minimumTargetSdk(35))
        assertEquals(34, UnattendedUpdates.minimumTargetSdk(36))
        assertEquals(35, UnattendedUpdates.minimumTargetSdk(37))
    }

    @Test
    fun installedByApkManager_withRecentTarget_isPossible() {
        assertTrue(UnattendedUpdates.isPossible(app(), OWN_PACKAGE, sdkInt = 36))
    }

    @Test
    fun apkManagerItself_isPossible() {
        assertTrue(
            UnattendedUpdates.isPossible(
                app(installer = "com.android.vending", packageName = OWN_PACKAGE),
                OWN_PACKAGE,
                36,
            ),
        )
    }

    @Test
    fun installedByAnotherApp_isNotPossible() {
        assertFalse(UnattendedUpdates.isPossible(app(installer = "com.android.vending"), OWN_PACKAGE, sdkInt = 36))
        assertFalse(UnattendedUpdates.isPossible(app(installer = null), OWN_PACKAGE, sdkInt = 36))
    }

    @Test
    fun oldTargetSdk_isNotPossible() {
        assertFalse(UnattendedUpdates.isPossible(app(targetSdk = 33), OWN_PACKAGE, sdkInt = 36))
        assertTrue(UnattendedUpdates.isPossible(app(targetSdk = 33), OWN_PACKAGE, sdkInt = 35))
    }

    @Test
    fun beforeAndroid12_isNotPossible() {
        assertFalse(UnattendedUpdates.isPossible(app(), OWN_PACKAGE, sdkInt = 30))
    }

    private companion object {
        const val OWN_PACKAGE = "me.geckotv.apkmanager"
    }
}
