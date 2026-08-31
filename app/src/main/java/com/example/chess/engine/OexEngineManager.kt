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
import kotlinx.coroutines.withContext
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
    val isCustom: Boolean = false,
    val isBundled: Boolean = false
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
     * PRIMARY METHOD: Find pre-bundled Stockfish in APK's native library directory.
     * This is checked first before any download or external scan.
     */
    fun findBundledStockfish(): OexEngineInfo? {
        try {
            val nativeLibraryDir = context.applicationInfo.nativeLibraryDir ?: return null
            val bundledStockfish = File(nativeLibraryDir, "libstockfish.so")
            if (bundledStockfish.exists()) {
                Log.i("OexEngineManager", "Found bundled Stockfish: ${bundledStockfish.absolutePath}")
                return OexEngineInfo(
                    id = "bundled-stockfish-18",
                    name = "Stockfish 18 (Built-in)",
                    packageName = context.packageName,
                    executablePath = bundledStockfish.absolutePath,
                    version = "Stockfish 18 Official Release",
                    isStockfish = true,
                    isOex = false,
                    isCustom = true,
                    isBundled = true
                )
            }
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error finding bundled Stockfish: ${e.message}")
        }
        return null
    }

    /**
     * Get the best available engine — prefers bundled Stockfish.
     */
    suspend fun getBestAvailableEngine(): OexEngineInfo? {
        val bundled = findBundledStockfish()
        if (bundled != null) return bundled
        val discovered = discoverEngines()
        return discovered.firstOrNull { it.isBundled }
            ?: discovered.firstOrNull { it.isStockfish }
            ?: discovered.firstOrNull()
    }

    suspend fun discoverEngines(): List<OexEngineInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OexEngineInfo>()
        val pm = context.packageManager
        val seenPackages = mutableSetOf<String>()

        // 0. Check Pre-Bundled / Native Stockfish Engine
        findBundledStockfish()?.let {
            list.add(it)
            seenPackages.add(it.id)
        }

        // 1. Check custom imported engines
        val savedCustomEngines = loadSavedCustomEngines()
        for (custom in savedCustomEngines) {
            if (custom.executablePath != null && File(custom.executablePath).exists()) {
                list.add(custom)
                seenPackages.add(custom.id)
            }
        }

        // 2. Query Intent actions for Open Exchange (OEX)
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

        list.sortedWith(compareByDescending<OexEngineInfo> { it.isStockfish }.thenByDescending { it.isCustom })
    }

    suspend fun importCustomEngineFromUri(uri: Uri): Result<OexEngineInfo> = withContext(Dispatchers.IO) {
        try {
            val fileName = queryFileName(uri) ?: "stockfish_archive_${System.currentTimeMillis()}.tar"
            val customId = "stockfish_${System.currentTimeMillis()}"
            val targetDir = File(context.filesDir, "custom_engines/$customId")
            if (!targetDir.exists()) targetDir.mkdirs()

            val extractedFiles = context.contentResolver.openInputStream(uri)?.use { input ->
                ArchiveExtractor.extract(input, fileName, targetDir)
            } ?: return@withContext Result.failure(Exception("Could not open selected file stream."))

            val bestBinary = ArchiveExtractor.findBestExecutableBinary(extractedFiles)
                ?: return@withContext Result.failure(Exception("No executable binary found in archive. Ensure the archive contains a Stockfish/UCI binary."))

            setExecutablePermission(bestBinary)

            val codeCacheDir = File(context.codeCacheDir, "engines/$customId")
            if (!codeCacheDir.exists()) codeCacheDir.mkdirs()
            val codeCacheBinary = File(codeCacheDir, bestBinary.name)
            try {
                FileInputStream(bestBinary).use { ins ->
                    FileOutputStream(codeCacheBinary).use { outs ->
                        ins.copyTo(outs)
                    }
                }
                setExecutablePermission(codeCacheBinary)
            } catch (_: Exception) {}

            var validPath: String? = null
            var engineNameResult: String? = null

            val verify1 = verifyUciEngine(bestBinary.absolutePath)
            if (verify1.isValid) {
                validPath = bestBinary.absolutePath
                engineNameResult = verify1.engineName
            } else if (codeCacheBinary.exists()) {
                val verify2 = verifyUciEngine(codeCacheBinary.absolutePath)
                if (verify2.isValid) {
                    validPath = codeCacheBinary.absolutePath
                    engineNameResult = verify2.engineName
                }
            }

            val finalExecPath = validPath ?: bestBinary.absolutePath
            val finalEngineName = engineNameResult
                ?: if (fileName.contains("stockfish", ignoreCase = true) || bestBinary.name.contains("stockfish", ignoreCase = true)) {
                    "Stockfish 18 (ARMv8)"
                } else {
                    bestBinary.name.removeSuffix(".apk").removeSuffix(".bin")
                }

            val isSf = finalEngineName.contains("Stockfish", ignoreCase = true) || fileName.contains("stockfish", ignoreCase = true)
            val info = OexEngineInfo(
                id = customId,
                name = finalEngineName,
                packageName = "custom.engine.$customId",
                executablePath = finalExecPath,
                version = "Stockfish 18 / UCI Engine",
                isStockfish = isSf,
                isOex = false,
                isCustom = true
            )

            saveCustomEngine(info)
            Result.success(info)
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error importing custom engine: ${e.message}", e)
            Result.failure(Exception("Error extracting and starting engine: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    suspend fun autoScanAndImportFromDownloads(): Result<OexEngineInfo> = withContext(Dispatchers.IO) {
        val candidateDirs = listOfNotNull(
            File("/storage/emulated/0/Download"),
            File("/sdcard/Download"),
            context.getExternalFilesDir(null),
            try { android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS) } catch (_: Exception) { null }
        )

        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: continue
                val targetFile = files.filter { it.isFile }
                    .sortedWith(
                        compareByDescending<File> { it.name.contains("dotprod", ignoreCase = true) }
                            .thenByDescending { it.name.contains("armv8", ignoreCase = true) }
                            .thenByDescending { it.name.contains("stockfish", ignoreCase = true) }
                            .thenByDescending { it.name.endsWith(".tar", ignoreCase = true) }
                    )
                    .firstOrNull { file ->
                        val n = file.name.lowercase()
                        n.contains("stockfish") && (n.endsWith(".tar") || n.endsWith(".tar.gz") || n.endsWith(".tgz") || n.endsWith(".zip") || n.endsWith(".apk"))
                    }

                if (targetFile != null) {
                    try {
                        val customId = "stockfish_auto_${System.currentTimeMillis()}"
                        val targetDir = File(context.filesDir, "custom_engines/$customId")
                        if (!targetDir.exists()) targetDir.mkdirs()

                        val extractedFiles = FileInputStream(targetFile).use { input ->
                            ArchiveExtractor.extract(input, targetFile.name, targetDir)
                        }
                        val bestBinary = ArchiveExtractor.findBestExecutableBinary(extractedFiles)
                        if (bestBinary != null) {
                            setExecutablePermission(bestBinary)
                            val info = OexEngineInfo(
                                id = customId,
                                name = "Stockfish 18 (${targetFile.name.removeSuffix(".tar")})",
                                packageName = "custom.engine.$customId",
                                executablePath = bestBinary.absolutePath,
                                version = "Stockfish 18 Auto-Loaded",
                                isStockfish = true,
                                isOex = false,
                                isCustom = true
                            )
                            saveCustomEngine(info)
                            return@withContext Result.success(info)
                        }
                    } catch (e: Exception) {
                        Log.e("OexEngineManager", "Failed auto-extracting ${targetFile.name}: ${e.message}")
                    }
                }
            }
        }
        Result.failure(Exception("No Stockfish archive found in Downloads."))
    }

    private data class UciVerification(val isValid: Boolean, val engineName: String?)

    private fun verifyUciEngine(execPath: String): UciVerification {
        var proc: Process? = null
        try {
            val process = ProcessBuilder(execPath).redirectErrorStream(true).start()
            proc = process
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))
            val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))

            writer.write("uci\n")
            writer.flush()

            var engineName: String? = null
            var responded = false
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 3000) {
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
                    Thread.sleep(30)
                }
            }

            try {
                writer.write("quit\n")
                writer.flush()
                writer.close()
                reader.close()
            } catch (_: Exception) {}

            process.destroy()
            return UciVerification(isValid = responded, engineName = engineName)
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Engine verification failed: ${e.message}")
            proc?.destroy()
            return UciVerification(isValid = false, engineName = null)
        }
    }

    fun saveCustomEngine(info: OexEngineInfo) {
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

    private fun prepareEngineExecutable(packageName: String, appInfo: ApplicationInfo): String? {
        try {
            val enginesDir = File(context.filesDir, "oex_engines/$packageName")
            if (!enginesDir.exists()) enginesDir.mkdirs()

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
                            return if (targetExec.canExecute()) targetExec.absolutePath else f.absolutePath
                        }
                    }
                }
            }

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
            Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
        } catch (_: Exception) {}
    }

    /**
     * Start engine with MAXIMUM STRENGTH settings
     */
    suspend fun startEngine(engine: OexEngineInfo): Boolean = withContext(Dispatchers.IO) {
        stopEngine()
        val execPath = engine.executablePath ?: return@withContext false

        try {
            val file = File(execPath)
            if (file.exists()) setExecutablePermission(file)

            val pb = ProcessBuilder(execPath)
            if (file.parentFile?.exists() == true) pb.directory(file.parentFile)
            pb.redirectErrorStream(true)

            val process = pb.start()
            activeProcess = process
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))
            val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
            processWriter = writer
            processReader = reader
            activeEngineInfo = engine

            // === PHASE 1: UCI Handshake ===
            writer.write("uci\n")
            writer.flush()

            var uciOkReceived = false
            var idName = ""
            val uciStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - uciStart < 5000) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    Log.d("UCI_INIT", line)
                    if (line.startsWith("id name ")) {
                        idName = line.removePrefix("id name ").trim()
                    }
                    if (line.trim() == "uciok" || line.contains("uciok")) {
                        uciOkReceived = true
                        break
                    }
                } else {
                    Thread.sleep(20)
                }
            }

            // Verify Stockfish if bundled
            if (engine.isBundled && !idName.contains("Stockfish", ignoreCase = true)) {
                Log.e("OexEngineManager", "Bundled engine id name '$idName' does not contain Stockfish!")
                // We won't fail hard, but log it
            }

            // === PHASE 2: Configure Settings ===
            writer.write("setoption name Skill Level value 20\n")
            writer.write("setoption name Hash value 256\n")
            writer.write("setoption name Threads value 4\n")
            writer.write("setoption name MultiPV value 1\n")
            writer.write("setoption name UCI_LimitStrength value false\n")
            writer.write("setoption name UCI_Elo value 3200\n")
            writer.write("setoption name Contempt value 24\n")
            writer.write("setoption name Analysis Contempt value Both\n")
            writer.write("setoption name Slow Mover value 100\n")
            writer.write("setoption name nodestime value 0\n")
            writer.write("setoption name Clear Hash\n")
            writer.flush()

            Thread.sleep(150)

            // === PHASE 3: isready ===
            writer.write("isready\n")
            writer.flush()

            var readyOkReceived = false
            val readyStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - readyStart < 5000) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    Log.d("UCI_READY", line)
                    if (line.trim() == "readyok" || line.contains("readyok")) {
                        readyOkReceived = true
                        break
                    }
                } else {
                    Thread.sleep(20)
                }
            }

            // === PHASE 4: New Game ===
            writer.write("ucinewgame\n")
            writer.write("isready\n")
            writer.flush()

            val newGameStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - newGameStart < 3000) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    if (line.trim() == "readyok" || line.contains("readyok")) break
                } else {
                    Thread.sleep(20)
                }
            }

            Log.i("OexEngineManager", "Engine ${engine.name} started at MAX POWER (uciok=$uciOkReceived, readyok=$readyOkReceived)")
            true
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Failed to start engine: ${e.message}")
            stopEngine()
            false
        }
    }

    /**
     * Resets the active engine state for a new game.
     */
    fun sendNewGame() {
        try {
            val writer = processWriter ?: return
            writer.write("ucinewgame\n")
            writer.write("isready\n")
            writer.flush()
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Error sending ucinewgame: ${e.message}")
        }
    }

    /**
     * Find BEST move with MAXIMUM analysis
     */
    suspend fun findBestMove(
        fen: String,
        movesUci: List<String> = emptyList(),
        depth: Int = 30,              // MAX depth (30 = very deep)
        moveTimeMs: Int = 5000,       // 5 seconds think time default
        onProgress: (scoreCp: Int, mateIn: Int?, depth: Int, pv: List<String>) -> Unit = { _, _, _, _ -> }
    ): EngineEvaluationResult? = withContext(Dispatchers.IO) {
        val process = activeProcess ?: return@withContext null
        val writer = processWriter ?: return@withContext null
        val reader = processReader ?: return@withContext null

        try {
            // Drain residual output
            while (reader.ready()) {
                reader.readLine()
            }

            // Sync with isready
            writer.write("isready\n")
            writer.flush()
            val syncStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - syncStart < 2000) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    if (line.trim() == "readyok" || line.contains("readyok")) break
                } else {
                    Thread.sleep(10)
                }
            }

            // Send position
            val cleanFen = fen.trim()
            if (movesUci.isNotEmpty()) {
                val movesStr = movesUci.joinToString(" ")
                writer.write("position fen $cleanFen moves $movesStr\n")
            } else {
                writer.write("position fen $cleanFen\n")
            }
            writer.flush()

            // === GO COMMAND FOR MAX POWER ===
            // Use BOTH depth AND movetime for strongest analysis
            writer.write("go movetime $moveTimeMs depth $depth\n")
            writer.flush()

            var bestMove: String? = null
            var currentScoreCp = 0
            var currentMateIn: Int? = null
            var currentDepth = 0
            var currentPv = emptyList<String>()
            var bestDepthReached = 0

            val searchTimeoutMs = moveTimeMs.toLong() + 2500L
            val searchStart = System.currentTimeMillis()

            while (System.currentTimeMillis() - searchStart < searchTimeoutMs) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    Log.d("UCI_SEARCH", trimmed)

                    if (trimmed.startsWith("bestmove")) {
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size >= 2) {
                            val candidate = parts[1].trim()
                            if (candidate != "(none)" && candidate.isNotEmpty()) {
                                bestMove = candidate
                            }
                        }
                        break
                    } else if (trimmed.startsWith("info")) {
                        // Parse depth
                        val depthMatch = Regex("depth\\s+(\\d+)").find(trimmed)
                        depthMatch?.groupValues?.get(1)?.toIntOrNull()?.let {
                            currentDepth = it
                            if (it > bestDepthReached) bestDepthReached = it
                        }

                        // Parse score cp
                        val scoreCpMatch = Regex("score\\s+cp\\s+([\\-\\d]+)").find(trimmed)
                        scoreCpMatch?.groupValues?.get(1)?.toIntOrNull()?.let {
                            currentScoreCp = it
                            currentMateIn = null
                        }

                        // Parse score mate
                        val scoreMateMatch = Regex("score\\s+mate\\s+([\\-\\d]+)").find(trimmed)
                        scoreMateMatch?.groupValues?.get(1)?.toIntOrNull()?.let {
                            currentMateIn = it
                            currentScoreCp = if (it > 0) 30000 else -30000
                        }

                        // Parse PV
                        val pvMatch = Regex("\\bpv\\s+(.+)").find(trimmed)
                        pvMatch?.groupValues?.get(1)?.let { pvStr ->
                            currentPv = pvStr.split(Regex("\\s+")).filter { it.length in 4..5 }
                        }

                        onProgress(currentScoreCp, currentMateIn, currentDepth, currentPv)
                    }
                } else {
                    Thread.sleep(10)
                }
            }

            // If no bestmove received, send stop
            if (bestMove == null) {
                writer.write("stop\n")
                writer.flush()
                val stopStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - stopStart < 2000) {
                    if (reader.ready()) {
                        val line = reader.readLine() ?: break
                        val trimmed = line.trim()
                        if (trimmed.startsWith("bestmove")) {
                            val parts = trimmed.split(Regex("\\s+"))
                            if (parts.size >= 2 && parts[1] != "(none)") {
                                bestMove = parts[1].trim()
                            }
                            break
                        }
                    } else {
                        Thread.sleep(10)
                    }
                }
            }

            Log.i("OexEngineManager", "Search complete: bestMove=$bestMove, depth=$bestDepthReached, score=$currentScoreCp")

            EngineEvaluationResult(
                bestMoveUci = bestMove,
                scoreCp = currentScoreCp,
                mateIn = currentMateIn,
                depth = bestDepthReached,
                pvLine = currentPv
            )
        } catch (e: Exception) {
            Log.e("OexEngineManager", "Search error: ${e.message}")
            null
        }
    }

    fun stopEngine() {
        try {
            processWriter?.let {
                it.write("quit\n")
                it.flush()
                it.close()
            }
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
