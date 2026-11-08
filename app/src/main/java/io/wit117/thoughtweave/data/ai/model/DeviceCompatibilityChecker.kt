package io.wit117.thoughtweave.data.ai.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Rejects 32-bit and low-RAM devices. A measured memory threshold can follow in Phase 6. */
class DeviceCompatibilityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isSupported(): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        return Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() && !activityManager.isLowRamDevice
    }
}
