package com.example.chess.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.*
import java.util.zip.ZipFile

data class OexEngineInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val executablePath: String?,
    val version: String = "",
    val isStockfish: Boolean = false,
    val isOex: Boolean = true,
    val isCustom: Boolean = false
)

data class EngineEvaluationResult(
    val bestMoveUci: String?,
    val scoreCp: Int = 0,
    val mateIn: Int? = null,
    val depth: Int = 0,
    val pvLine: List<String> = emptyList()
)

class OexEngineManager(private val context: Context) {

    private var activeProcess: Process? = null
    private var processWriter: BufferedWriter? = null
    private var processReader: BufferedReader? = null
    private var activeEngineInfo: OexEngineInfo? = null

    private val customEnginesPrefs = context.getSharedPreferences("custom_engines_prefs", Context.MODE_PRIVATE)

    /**
     * Discovers all installed UCI / OEX engines on the user's device,
     * including Stockfish (Stockfish 18, Stockfish 17, Stockfish OEX, DroidFish, etc.)
     * and any previously imported custom engines.
     */
    suspend fun discoverEngines(): List<OexEngineInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OexEngineInfo>()
        val pm = context.packageManager
        val seenPackages = mutableSetOf<String>()

        // 1. Check custom imported engines from Downloads/Files
        val savedCustomEngines = loadSavedCustomEngines()
        for (custom in savedCustomEngines) {
            if (custom.executablePath != null && File(custom.executablePath).exists()) {
                list.add(custom)
                seenPackages.add(custom.id)
            }
        }

        // 2. Query Intent actions for Open Exchange (OEX) standard
        try {
            val intent = Intent("chess.engine.engineAction")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            val resolveInfos = pm.queryIntentActivities(intent, flags)
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (seenPackages.add(pkgName)) {
                    val appInfo = info.activityInfo.applicationInfo
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val execPath = prepareEngineExecutable(pkgName, appInfo)
                    val isSf = label.contains("Stockfish", ignoreCase = true) || pkgName.contains("stockfish", ignoreCase = true)

                    list.add(
                        OexEngineInfo(
                            id = pkgName,
                            name = label,
                            packageName = pkgName,
                            executablePath = execPath,
                            version = if (isSf) "Stockfish Engine" else "OEX Engine",
                            isStockfish = isSf,
                            isOex = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error querying OEX intent: ${e.message}")
        }

        // 3. Scan installed packages for Stockfish / Chess engines
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val pkgName = app.packageName
                val label = pm.getApplicationLabel(app).toString()

                val isStockfishMatch = label.contains("Stockfish", ignoreCase = true) ||
                        pkgName.contains("stockfish", ignoreCase = true) ||
                        label.contains("Komodo", ignoreCase = true) ||
                        pkgName.contains("droidfish", ignoreCase = true) ||
                        pkgName.contains("chess.engine", ignoreCase = true)

                if (isStockfishMatch && seenPackages.add(pkgName)) {
                    val execPath = prepareEngineExecutable(pkgName, app)
                    val isSf = label.contains("Stockfish", ignoreCase = true) || pkgName.contains("stockfish", ignoreCase = true)

                    list.add(
                        OexEngineInfo(
                            id = pkgName,
                            name = label,
                            packageName = pkgName,
                            executablePath = execPath,
                            version = if (isSf) "Stockfish External Engine" else "UCI Engine",
                            isStockfish = isSf,
                            isOex = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error scanning installed packages: ${e.message}")
        }

        // Sort: Stockfish first, then custom engines, then others
        list.sortedWith(compareByDescending<OexEngineInfo> { it.isStockfish }.thenByDescending { it.isCustom })
    }

    /**
     * Imports a custom chess engine binary or APK chosen by the user from Downloads.
     * Verifies the engine with a UCI handshake.
     * Returns Result.success(info) on success or Result.failure with error message.
     */
    suspend fun importCustomEngineFromUri(uri: Uri): Result<OexEngineInfo> = withContext(Dispatchers.IO) {
        try {
            val fileName = queryFileName(uri) ?: "custom_engine_${System.currentTimeMillis()}"
            val customId = "custom_${System.currentTimeMillis()}"
            val targetDir = File(context.filesDir, "custom_engines/$customId")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val tempFile = File(targetDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Sorry, engine not detected."))

            var candidateExecPath: String? = null

            // If APK or ZIP file, try extracting native ELF binaries
            if (fileName.endsWith(".apk", ignoreCase = true) || fileName.endsWith(".zip", ignoreCase = true)) {
                try {
                    ZipFile(tempFile).use { zip ->
                        val entries = zip.entries()
                        val candidateEntries = mutableListOf<java.util.zip.ZipEntry>()

                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val name = entry.name
                            if ((name.startsWith("lib/arm64-v8a/") || name.startsWith("lib/armeabi-v7a/") || name.startsWith("assets/"))
                                && (name.contains("stockfish") || name.contains("engine") || name.endsWith(".so"))
                            ) {
                                candidateEntries.add(entry)
                            }
                        }

                        val bestEntry = candidateEntries.firstOrNull { it.name.contains("arm64-v8a") }
                            ?: candidateEntries.firstOrNull()

                        if (bestEntry != null) {
                            val extractedName = File(bestEntry.name).name
                            val extractedFile = File(targetDir, extractedName)
                            zip.getInputStream(bestEntry).use { input ->
                                FileOutputStream(extractedFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            setExecutablePermission(extractedFile)
                            candidateExecPath = extractedFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OexEngineManager", "Failed to unzip APK: ${e.message}")
                }
            } else {
                // Direct binary / executable
                setExecutablePermission(tempFile)
                candidateExecPath = tempFile.absolutePath
            }

            val execPath = candidateExecPath ?: return@withContext Result.failure(Exception("Sorry, engine not detected."))

            // Verify if the engine responds to UCI commands
            val verificationResult = verifyUciEngine(execPath)
            if (!verificationResult.isValid) {
                tempFile.delete()
                targetDir.deleteRecursively()
                return@withContext Result.failure(Exception("Sorry, engine not detected."))
            }

            val engineName = verificationResult.engineName ?: fileName.removeSuffix(".apk").removeSuffix(".bin")
            val isSf = engineName.contains("Stockfish", ignoreCase = true)
            val info = OexEngineInfo(
                id = customId,
                name = engineName,
                packageName = "custom.engine.$customId",
                executablePath = execPath,
                version = "Custom Imported Engine",
                isStockfish = isSf,
                isOex = false,
                isCustom = true
            )

            saveCustomEngine(info)
            Result.success(info)
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error importing custom engine: ${e.message}")
            Result.failure(Exception("Sorry, engine not detected."))
        }
    }

    private data class UciVerification(val isValid: Boolean, val engineName: String?)

    private fun verifyUciEngine(execPath: String): UciVerification {
        var proc: Process? = null
        try {
            val process = ProcessBuilder(execPath).redirectErrorStream(true).start()
            proc = process
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            writer.write("uci\n")
            writer.flush()

            var engineName: String? = null
            var responded = false
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 2500) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("id name ")) {
                        engineName = line.removePrefix("id name ").trim()
                        responded = true
                    }
                    if (line.contains("uciok") || line.contains("id author")) {
                        responded = true
                        break
                    }
                } else {
                    Thread.sleep(50)
                }
            }

            writer.write("quit\n")
            writer.flush()
            writer.close()
            reader.close()
            process.destroy()

            return UciVerification(isValid = responded, engineName = engineName)
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Engine verification failed: ${e.message}")
            proc?.destroy()
            return UciVerification(isValid = false, engineName = null)
        }
    }

    private fun saveCustomEngine(info: OexEngineInfo) {
        val current = customEnginesPrefs.getStringSet("custom_engine_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(info.id)
        customEnginesPrefs.edit()
            .putStringSet("custom_engine_ids", current)
            .putString("engine_name_${info.id}", info.name)
            .putString("engine_path_${info.id}", info.executablePath)
            .putBoolean("engine_sf_${info.id}", info.isStockfish)
            .apply()
    }

    private fun loadSavedCustomEngines(): List<OexEngineInfo> {
        val ids = customEnginesPrefs.getStringSet("custom_engine_ids", emptySet()) ?: emptySet()
        val list = mutableListOf<OexEngineInfo>()
        for (id in ids) {
            val name = customEnginesPrefs.getString("engine_name_$id", "Custom Engine") ?: "Custom Engine"
            val path = customEnginesPrefs.getString("engine_path_$id", null)
            val isSf = customEnginesPrefs.getBoolean("engine_sf_$id", false)
            if (path != null && File(path).exists()) {
                list.add(
                    OexEngineInfo(
                        id = id,
                        name = name,
                        packageName = "custom.engine.$id",
                        executablePath = path,
                        version = "Custom Imported Engine",
                        isStockfish = isSf,
                        isOex = false,
                        isCustom = true
                    )
                )
            }
        }
        return list
    }

    private fun queryFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        result = it.getString(idx)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { File(it).name }
        }
        return result
    }

    /**
     * Extracts or prepares the native engine binary so it can be executed.
     */
    private fun prepareEngineExecutable(packageName: String, appInfo: ApplicationInfo): String? {
        try {
            val enginesDir = File(context.filesDir, "oex_engines/$packageName")
            if (!enginesDir.exists()) {
                enginesDir.mkdirs()
            }

            // 1. Check native library directory of the target app
            val nativeDir = File(appInfo.nativeLibraryDir)
            if (nativeDir.exists() && nativeDir.isDirectory) {
                val nativeFiles = nativeDir.listFiles()
                if (nativeFiles != null && nativeFiles.isNotEmpty()) {
                    for (f in nativeFiles) {
                        if (f.canExecute() || f.name.endsWith(".so")) {
                            val targetExec = File(enginesDir, f.name)
                            if (!targetExec.exists() || targetExec.length() != f.length()) {
                                f.copyTo(targetExec, overwrite = true)
                                setExecutablePermission(targetExec)
                            }
                            return targetExec.absolutePath
                        }
                    }
                }
            }

            // 2. Extract directly from APK (sourceDir) if native files are bundled inside
            val apkFile = File(appInfo.sourceDir)
            if (apkFile.exists()) {
                ZipFile(apkFile).use { zip ->
                    val entries = zip.entries()
                    val candidateEntries = mutableListOf<java.util.zip.ZipEntry>()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if ((name.startsWith("lib/arm64-v8a/") || name.startsWith("lib/armeabi-v7a/") || name.startsWith("assets/"))
                            && (name.contains("stockfish") || name.contains("engine") || name.endsWith(".so"))
                        ) {
                            candidateEntries.add(entry)
                        }
                    }

                    val bestEntry = candidateEntries.firstOrNull { it.name.contains("arm64-v8a") }
                        ?: candidateEntries.firstOrNull()

                    if (bestEntry != null) {
                        val fileName = File(bestEntry.name).name
                        val targetExec = File(enginesDir, fileName)
                        zip.getInputStream(bestEntry).use { input ->
                            FileOutputStream(targetExec).use { output ->
                                input.copyTo(output)
                            }
                        }
                        setExecutablePermission(targetExec)
                        return targetExec.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Failed to prepare executable for $packageName: ${e.message}")
        }
        return null
    }

    private fun setExecutablePermission(file: File) {
        try {
            file.setExecutable(true, false)
            file.setReadable(true, false)
            Runtime.getRuntime().exec("chmod 755 ${file.absolutePath}").waitFor()
        } catch (_: Exception) {}
    }

    suspend fun startEngine(engine: OexEngineInfo): Boolean = withContext(Dispatchers.IO) {
        stopEngine()
        val execPath = engine.executablePath ?: return@withContext false

        try {
            val process = ProcessBuilder(execPath).redirectErrorStream(true).start()
            activeProcess = process
            processWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
            processReader = BufferedReader(InputStreamReader(process.inputStream))
            activeEngineInfo = engine

            // Initial handshake
            sendRawCommand("uci")
            delay(100)
            sendRawCommand("isready")
            true
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Failed to launch process for ${engine.name}: ${e.message}")
            false
        }
    }

    private fun sendRawCommand(cmd: String) {
        try {
            val writer = processWriter ?: return
            writer.write(cmd)
            writer.newLine()
            writer.flush()
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error sending UCI: ${e.message}")
        }
    }

    /**
     * Queries the external engine (Stockfish 18 / UCI) for the best move in a position.
     */
    suspend fun findBestMove(
        fen: String,
        depth: Int = 10,
        moveTimeMs: Int = 1500,
        onProgress: (scoreCp: Int, mateIn: Int?) -> Unit = { _, _ -> }
    ): EngineEvaluationResult? = withContext(Dispatchers.IO) {
        val process = activeProcess ?: return@withContext null
        val reader = processReader ?: return@withContext null

        try {
            sendRawCommand("position fen $fen")
            sendRawCommand("go depth $depth movetime $moveTimeMs")

            var bestMove: String? = null
            var currentScoreCp = 0
            var currentMateIn: Int? = null
            var currentDepth = 0

            withTimeoutOrNull(moveTimeMs.toLong() + 3000L) {
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("bestmove")) {
                        val parts = line.split(" ")
                        if (parts.size >= 2) {
                            bestMove = parts[1]
                        }
                        break
                    } else if (line.startsWith("info")) {
                        if (line.contains("score cp")) {
                            val idx = line.indexOf("score cp")
                            val scoreStr = line.substring(idx + 9).trim().split(" ").firstOrNull()
                            scoreStr?.toIntOrNull()?.let {
                                currentScoreCp = it
                                currentMateIn = null
                                onProgress(it, null)
                            }
                        } else if (line.contains("score mate")) {
                            val idx = line.indexOf("score mate")
                            val mateStr = line.substring(idx + 11).trim().split(" ").firstOrNull()
                            mateStr?.toIntOrNull()?.let {
                                currentMateIn = it
                                onProgress(if (it > 0) 10000 else -10000, it)
                            }
                        }
                    }
                }
            }

            EngineEvaluationResult(
                bestMoveUci = bestMove,
                scoreCp = currentScoreCp,
                mateIn = currentMateIn,
                depth = currentDepth
            )
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error during UCI search: ${e.message}")
            null
        }
    }

    fun stopEngine() {
        try {
            sendRawCommand("quit")
            processWriter?.close()
            processReader?.close()
            activeProcess?.destroy()
        } catch (_: Exception) {} finally {
            processWriter = null
            processReader = null
            activeProcess = null
            activeEngineInfo = null
        }
    }

    val isRunning: Boolean
        get() = activeProcess != null
}
