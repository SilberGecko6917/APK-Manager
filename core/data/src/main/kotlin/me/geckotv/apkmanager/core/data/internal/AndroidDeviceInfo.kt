package me.geckotv.apkmanager.core.data.internal

import android.os.Build
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import javax.inject.Inject

internal class AndroidDeviceInfo
    @Inject
    constructor() : DeviceInfo {
        override val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList()
    }
