package com.example.chess.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

data class DeviceSpecs(
    val cpuArch: String,
    val is64Bit: Boolean,
    val hasDotProd: Boolean,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val osVersion: String,
    val recommendedBuildName: String,
    val downloadFileName: String,
    val primaryDownloadUrl: String,
    val fallbackDownloadUrl: String
)

object DeviceSpecsDetector {

    private const val TAG = "DeviceSpecsDetector"

    // Official Stockfish 18 / Release binaries
    private const val SF18_BASE = "https://github.com/official-stockfish/Stockfish/releases/download/sf_18"
    private const val SF_LATEST_BASE = "https://github.com/official-stockfish/Stockfish/releases/latest/download"

    fun detect(context: Context): DeviceSpecs {
        val supportedAbis = Build.SUPPORTED_ABIS ?: arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
        val primaryAbi = supportedAbis.firstOrNull() ?: "arm64-v8a"
        val is64Bit = supportedAbis.any { it.contains("64") }

        val hasDotProd = checkCpuDotProd()

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val (downloadFile, recommendedName) = when {
            primaryAbi.contains("x86_64") || primaryAbi.contains("x86") -> {
                "stockfish-android-x86-64.tar" to "Stockfish (x86_64 Native)"
            }
            is64Bit && hasDotProd -> {
                "stockfish-android-armv8-dotprod.tar" to "Stockfish 18 (ARMv8.2+ DotProd NNUE)"
            }
            is64Bit -> {
                "stockfish-android-armv8.tar" to "Stockfish 18 (ARMv8 64-bit NNUE)"
            }
            else -> {
                "stockfish-android-armv7.tar" to "Stockfish (ARMv7 32-bit)"
            }
        }

        val primaryUrl = "$SF18_BASE/$downloadFile"
        val fallbackUrl = "$SF_LATEST_BASE/$downloadFile"

        return DeviceSpecs(
            cpuArch = primaryAbi,
            is64Bit = is64Bit,
            hasDotProd = hasDotProd,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            osVersion = osVersion,
            recommendedBuildName = recommendedName,
            downloadFileName = downloadFile,
            primaryDownloadUrl = primaryUrl,
            fallbackDownloadUrl = fallbackUrl
        )
    }

    private fun checkCpuDotProd(): Boolean {
        try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists() && cpuInfo.canRead()) {
                val content = cpuInfo.readText()
                if (content.contains("asimddp", ignoreCase = true) ||
                    content.contains("dotprod", ignoreCase = true)
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not check /proc/cpuinfo: ${e.message}")
        }
        // Modern 64-bit ARM devices with Android 10+ (API 29+) almost all support dotprod
        return Build.VERSION.SDK_INT >= 29 && Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    }
}
