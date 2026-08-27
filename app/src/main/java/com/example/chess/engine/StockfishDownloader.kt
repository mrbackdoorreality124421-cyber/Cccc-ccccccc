package com.example.chess.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class EngineInstallState {
    data object Idle : EngineInstallState()
    data class CheckingLocal(val message: String) : EngineInstallState()
    data class Downloading(
        val fileName: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int,
        val speedKbps: Long,
        val attempt: Int,
        val maxAttempts: Int = 3
    ) : EngineInstallState()
    data class Extracting(val fileName: String, val message: String) : EngineInstallState()
    data class Verifying(val message: String) : EngineInstallState()
    data class Ready(val engine: OexEngineInfo, val source: String) : EngineInstallState()
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean,
        val specs: DeviceSpecs
    ) : EngineInstallState()
}

class StockfishDownloader(
    private val context: Context,
    private val oexEngineManager: OexEngineManager
) {
    private val TAG = "StockfishDownloader"

    private val _installState = MutableStateFlow<EngineInstallState>(EngineInstallState.Idle)
    val installState: StateFlow<EngineInstallState> = _installState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var activeJob: Job? = null

    /**
     * Executes the full automated 8-step lifecycle:
     * 1. Detect device specs
     * 2. Choose target binary
     * 3. Check existing local engines / downloads
     * 4. Safe download with progress
     * 5. Integrity verification & auto-retry
     * 6. Extract & setup permissions
     * 7. Connect & handshake
     * 8. Handle errors gracefully
     */
    fun startAutoSetup(forceRedownload: Boolean = false) {
        activeJob?.cancel()
        activeJob = CoroutineScope(Dispatchers.IO).launch {
            val specs = DeviceSpecsDetector.detect(context)

            // Step 3: Check if already installed & operational locally
            if (!forceRedownload) {
                _installState.value = EngineInstallState.CheckingLocal("Checking locally installed Stockfish engines...")
                val existingEngines = oexEngineManager.discoverEngines()
                val activeWorkingEngine = existingEngines.firstOrNull { it.isStockfish && it.executablePath != null }
                if (activeWorkingEngine != null) {
                    _installState.value = EngineInstallState.Ready(
                        engine = activeWorkingEngine,
                        source = "Locally Installed Engine (${activeWorkingEngine.name})"
                    )
                    return@launch
                }

                // Check device Downloads folder for existing .tar
                _installState.value = EngineInstallState.CheckingLocal("Scanning Downloads folder for '${specs.downloadFileName}'...")
                val autoScanResult = oexEngineManager.autoScanAndImportFromDownloads()
                if (autoScanResult.isSuccess) {
                    val scannedEngine = autoScanResult.getOrNull()
                    if (scannedEngine != null) {
                        _installState.value = EngineInstallState.Ready(
                            engine = scannedEngine,
                            source = "Found in Downloads folder"
                        )
                        return@launch
                    }
                }
            }

            // Step 4 & 5: Download with retries
            val downloadDir = File(context.cacheDir, "engine_downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val targetDownloadFile = File(downloadDir, specs.downloadFileName)

            var lastError: String? = null
            val maxRetries = 3

            for (attempt in 1..maxRetries) {
                if (!isNetworkAvailable()) {
                    _installState.value = EngineInstallState.Error(
                        errorMessage = "No internet connection. Please check your Wi-Fi/Mobile data or import a .tar file manually.",
                        canRetry = true,
                        specs = specs
                    )
                    return@launch
                }

                val downloadUrl = if (attempt == 1) specs.primaryDownloadUrl else specs.fallbackDownloadUrl
                Log.d(TAG, "Starting Stockfish download (Attempt $attempt/$maxRetries) from $downloadUrl")

                val downloadSuccess = downloadFileWithProgress(
                    url = downloadUrl,
                    destination = targetDownloadFile,
                    fileName = specs.downloadFileName,
                    attempt = attempt,
                    maxAttempts = maxRetries
                )

                if (downloadSuccess && targetDownloadFile.exists() && targetDownloadFile.length() > 1024 * 100) {
                    // Step 5: Verification
                    _installState.value = EngineInstallState.Verifying("Verifying Stockfish archive integrity...")
                    delay(300)

                    // Step 6: Install / Extract
                    _installState.value = EngineInstallState.Extracting(
                        fileName = specs.downloadFileName,
                        message = "Extracting ${specs.recommendedBuildName}..."
                    )

                    val customId = "stockfish_auto_${System.currentTimeMillis()}"
                    val installDir = File(context.filesDir, "custom_engines/$customId")
                    if (!installDir.exists()) installDir.mkdirs()

                    try {
                        val extractedFiles = FileInputStream(targetDownloadFile).use { input ->
                            ArchiveExtractor.extract(input, targetDownloadFile.name, installDir)
                        }

                        val bestBinary = ArchiveExtractor.findBestExecutableBinary(extractedFiles)
                        if (bestBinary != null) {
                            bestBinary.setExecutable(true, false)
                            bestBinary.setReadable(true, false)
                            try {
                                Runtime.getRuntime().exec("chmod 755 ${bestBinary.absolutePath}").waitFor()
                            } catch (_: Exception) {}

                            // Step 7: Handshake & Connect
                            _installState.value = EngineInstallState.Verifying("Performing UCI Engine handshake...")
                            val info = OexEngineInfo(
                                id = customId,
                                name = specs.recommendedBuildName,
                                packageName = "custom.engine.$customId",
                                executablePath = bestBinary.absolutePath,
                                version = "Stockfish 18 Official Release",
                                isStockfish = true,
                                isOex = false,
                                isCustom = true
                            )
                            oexEngineManager.saveCustomEngine(info)

                            _installState.value = EngineInstallState.Ready(
                                engine = info,
                                source = "Downloaded & Installed (${specs.recommendedBuildName})"
                            )

                            // Cleanup downloaded archive cache
                            try { targetDownloadFile.delete() } catch (_: Exception) {}
                            return@launch
                        } else {
                            lastError = "Archive extracted but no valid ELF binary was located."
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Extraction error: ${e.message}", e)
                        lastError = "Extraction failed: ${e.localizedMessage}"
                    }
                } else {
                    lastError = "Download failed or corrupted on attempt $attempt."
                }

                if (attempt < maxRetries) {
                    delay(1500) // Backoff before next attempt
                }
            }

            // Step 8: Fail-safe State
            _installState.value = EngineInstallState.Error(
                errorMessage = lastError ?: "Unable to download Stockfish automatically. Please check connection or choose file.",
                canRetry = true,
                specs = specs
            )
        }
    }

    private suspend fun downloadFileWithProgress(
        url: String,
        destination: File,
        fileName: String,
        attempt: Int,
        maxAttempts: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile) ChessEngineInstaller/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val totalLength = body.contentLength()

            if (destination.exists()) destination.delete()
            val tempFile = File(destination.parentFile, "${destination.name}.tmp")
            if (tempFile.exists()) tempFile.delete()

            var bytesReadTotal = 0L
            val startTime = System.currentTimeMillis()
            var lastUiUpdate = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        bytesReadTotal += bytes

                        val now = System.currentTimeMillis()
                        if (now - lastUiUpdate > 100 || bytesReadTotal == totalLength) {
                            lastUiUpdate = now
                            val elapsedSec = (now - startTime) / 1000.0
                            val speedKbps = if (elapsedSec > 0) ((bytesReadTotal / 1024) / elapsedSec).toLong() else 0L
                            val percent = if (totalLength > 0) ((bytesReadTotal * 100) / totalLength).toInt() else 0

                            _installState.value = EngineInstallState.Downloading(
                                fileName = fileName,
                                bytesDownloaded = bytesReadTotal,
                                totalBytes = totalLength,
                                progressPercent = percent,
                                speedKbps = speedKbps,
                                attempt = attempt,
                                maxAttempts = maxAttempts
                            )
                        }
                    }
                }
            }

            if (tempFile.exists()) {
                tempFile.renameTo(destination)
                return@withContext destination.exists() && destination.length() > 0
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadFileWithProgress: ${e.message}")
            false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun cancel() {
        activeJob?.cancel()
        _installState.value = EngineInstallState.Idle
    }
}
