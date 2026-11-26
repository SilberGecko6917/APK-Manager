package me.geckotv.apkmanager.core.data.internal

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import javax.inject.Inject

internal class AndroidDeviceInfo
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : DeviceInfo {
        override val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
        override val sdkInt: Int = Build.VERSION.SDK_INT
        override val ownPackageName: String = context.packageName
    }
