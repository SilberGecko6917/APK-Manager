package me.geckotv.apkmanager.core.testing

import me.geckotv.apkmanager.core.data.repository.DeviceInfo

/** A device with fixed ABIs, by default a typical 64-bit ARM phone. */
class FakeDeviceInfo(
    override val supportedAbis: List<String> = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
) : DeviceInfo
