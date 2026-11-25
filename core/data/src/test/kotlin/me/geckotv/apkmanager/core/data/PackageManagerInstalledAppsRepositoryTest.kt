package me.geckotv.apkmanager.core.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.internal.PackageManagerInstalledAppsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PackageManagerInstalledAppsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun installedApp_readsPackageInfo() =
        runTest {
            install(packageName = "com.example.app", label = "Example", versionName = "1.2.3", versionCode = 42)

            val app = repository().installedApp("com.example.app")!!

            assertEquals("Example", app.label)
            assertEquals("1.2.3", app.versionName)
            assertEquals(42L, app.versionCode)
            assertEquals(false, app.isSystemApp)
        }

    @Test
    fun installedApp_unknownPackage_isNull() =
        runTest {
            assertNull(repository().installedApp("com.example.missing"))
        }

    @Test
    fun installedApps_containsInstalledPackages() =
        runTest {
            install(packageName = "com.example.app", label = "Example", versionName = "1.0", versionCode = 1)

            assertTrue(repository().installedApps().any { it.packageName == "com.example.app" })
        }

    private fun kotlinx.coroutines.test.TestScope.repository() =
        PackageManagerInstalledAppsRepository(
            context = context,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = backgroundScope,
        )

    private fun install(
        packageName: String,
        label: String,
        versionName: String,
        versionCode: Int,
    ) {
        val packageInfo =
            PackageInfo().apply {
                this.packageName = packageName
                this.versionName = versionName
                @Suppress("DEPRECATION")
                this.versionCode = versionCode
                applicationInfo =
                    ApplicationInfo().apply {
                        this.packageName = packageName
                        nonLocalizedLabel = label
                    }
            }
        shadowOf(context.packageManager).installPackage(packageInfo)
    }
}
