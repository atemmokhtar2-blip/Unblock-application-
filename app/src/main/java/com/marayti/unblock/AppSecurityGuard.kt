package com.marayti.unblock

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug

/**
 * Lightweight runtime checks. These raise the cost of casual tampering;
 * they are not a substitute for server-side Google Play Integrity verification.
 */
internal object AppSecurityGuard {
    fun isEnvironmentTrusted(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true

        // A release package must not be marked debuggable or attached to a debugger.
        if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) return false
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) return false

        // Detect a process being traced on Linux/Android.
        val tracerPid = runCatching {
            java.io.File("/proc/self/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("TracerPid:") }
                    ?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0
            }
        }.getOrDefault(0)
        if (tracerPid != 0) return false

        // Confirm the installed package has a platform-recognized signing certificate.
        return runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= 28) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            if (Build.VERSION.SDK_INT >= 28) {
                info.signingInfo?.hasMultipleSigners() == true ||
                    info.signingInfo?.apkContentsSigners?.isNotEmpty() == true
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.isNotEmpty() == true
            }
        }.getOrDefault(false)
    }
}
