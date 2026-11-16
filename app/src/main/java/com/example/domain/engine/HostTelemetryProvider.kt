package com.example.domain.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.domain.model.HostTelemetry

class HostTelemetryProvider(private val context: Context) {

    fun collect(): HostTelemetry {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024L * 1024L)
        val availableRamMb = memInfo.availMem / (1024L * 1024L)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalDiskBytes = statFs.totalBytes
        val freeDiskBytes = statFs.availableBytes
        val bytesPerGb = 1024.0 * 1024.0 * 1024.0

        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        return HostTelemetry(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            primaryAbi = abis.firstOrNull() ?: "unknown",
            supportedAbis = abis,
            cpuCores = cores,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            totalDiskGb = totalDiskBytes / bytesPerGb,
            freeDiskGb = freeDiskBytes / bytesPerGb,
            androidRelease = Build.VERSION.RELEASE ?: "15",
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
}
