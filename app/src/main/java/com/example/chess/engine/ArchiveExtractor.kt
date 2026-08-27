package com.example.chess.engine

import android.util.Log
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

object ArchiveExtractor {

    private const val TAG = "ArchiveExtractor"

    /**
     * Extracts an archive (.tar, .tar.gz, .tgz, .zip, .apk) or saves a direct binary to targetDir.
     * Returns the list of extracted files.
     */
    fun extract(sourceStream: InputStream, fileName: String, targetDir: File): List<File> {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") -> {
                try {
                    GZIPInputStream(BufferedInputStream(sourceStream)).use { gzipIn ->
                        extractTarStream(gzipIn, targetDir)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gzip tar extract failed: ${e.message}")
                    emptyList()
                }
            }
            lowerName.endsWith(".tar") -> {
                try {
                    BufferedInputStream(sourceStream).use { bufIn ->
                        extractTarStream(bufIn, targetDir)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Tar extract failed: ${e.message}")
                    emptyList()
                }
            }
            lowerName.endsWith(".zip") || lowerName.endsWith(".apk") -> {
                try {
                    val tempZip = File(targetDir, "temp_${System.currentTimeMillis()}.zip")
                    FileOutputStream(tempZip).use { out ->
                        sourceStream.copyTo(out)
                    }
                    val extracted = extractZip(tempZip, targetDir)
                    tempZip.delete()
                    extracted
                } catch (e: Exception) {
                    Log.e(TAG, "Zip/APK extract failed: ${e.message}")
                    emptyList()
                }
            }
            else -> {
                // Check magic bytes: GZIP (0x1F, 0x8B), ZIP (0x50, 0x4B), ELF (0x7F, 'E', 'L', 'F')
                val pushback = PushbackInputStream(BufferedInputStream(sourceStream), 512)
                val header = ByteArray(512)
                val read = pushback.read(header)
                if (read > 0) {
                    pushback.unread(header, 0, read)
                }

                if (read >= 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
                    // GZIP stream
                    GZIPInputStream(pushback).use { gzipIn ->
                        extractTarStream(gzipIn, targetDir)
                    }
                } else if (read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                    // ZIP stream
                    val tempZip = File(targetDir, "temp_${System.currentTimeMillis()}.zip")
                    FileOutputStream(tempZip).use { out ->
                        pushback.copyTo(out)
                    }
                    val extracted = extractZip(tempZip, targetDir)
                    tempZip.delete()
                    extracted
                } else if (read >= 262 && isTarHeader(header)) {
                    extractTarStream(pushback, targetDir)
                } else {
                    // Direct binary (ELF or UCI executable)
                    val rawFile = File(targetDir, if (fileName.isNotEmpty()) fileName else "stockfish_binary")
                    FileOutputStream(rawFile).use { out ->
                        pushback.copyTo(out)
                    }
                    listOf(rawFile)
                }
            }
        }
    }

    private fun isTarHeader(header: ByteArray): Boolean {
        if (header.size < 262) return false
        val magic = String(header, 257, 5, Charsets.US_ASCII)
        return magic.startsWith("ustar")
    }

    /**
     * Standard POSIX ustar / GNU tar archive stream extractor.
     */
    private fun extractTarStream(inputStream: InputStream, targetDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        val headerBuf = ByteArray(512)

        while (true) {
            var read = 0
            while (read < 512) {
                val r = inputStream.read(headerBuf, read, 512 - read)
                if (r == -1) break
                read += r
            }
            if (read < 512) break

            // Check for EOF (all zero block)
            var allZero = true
            for (i in 0 until 512) {
                if (headerBuf[i] != 0.toByte()) {
                    allZero = false
                    break
                }
            }
            if (allZero) break

            // 1. File name: bytes 0..99
            var nameLen = 0
            while (nameLen < 100 && headerBuf[nameLen] != 0.toByte()) {
                nameLen++
            }
            val entryName = String(headerBuf, 0, nameLen, Charsets.UTF_8).trim()
            if (entryName.isEmpty()) continue

            // 2. File size in octal: bytes 124..135
            var sizeStr = String(headerBuf, 124, 12, Charsets.US_ASCII).trim().replace("\u0000", "")
            val size = try {
                sizeStr.toLong(8)
            } catch (_: Exception) {
                0L
            }

            // 3. Type flag at 156 ('0' / 0 = file, '5' = dir)
            val typeFlag = headerBuf[156].toInt().toChar()
            val cleanName = entryName.replace("..", "").trimStart('/')
            val destFile = File(targetDir, cleanName)

            if (typeFlag == '5' || cleanName.endsWith("/")) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { out ->
                    var remaining = size
                    val buf = ByteArray(8192)
                    while (remaining > 0) {
                        val toRead = Math.min(buf.size.toLong(), remaining).toInt()
                        val r = inputStream.read(buf, 0, toRead)
                        if (r == -1) break
                        out.write(buf, 0, r)
                        remaining -= r
                    }
                }
                extractedFiles.add(destFile)

                // TAR pads every record to a multiple of 512 bytes
                val padding = (512 - (size % 512)) % 512
                if (padding > 0) {
                    var skipped = 0L
                    while (skipped < padding) {
                        val s = inputStream.skip(padding - skipped)
                        if (s <= 0) {
                            val dummy = ByteArray((padding - skipped).toInt())
                            val r = inputStream.read(dummy)
                            if (r == -1) break
                            skipped += r
                        } else {
                            skipped += s
                        }
                    }
                }
            }
        }
        return extractedFiles
    }

    private fun extractZip(zipFile: File, targetDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val cleanName = entry.name.replace("..", "").trimStart('/')
                val destFile = File(targetDir, cleanName)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(destFile).use { out ->
                            input.copyTo(out)
                        }
                    }
                    extractedFiles.add(destFile)
                }
            }
        }
        return extractedFiles
    }

    /**
     * Inspects extracted files to locate the primary Stockfish / UCI ELF binary.
     */
    fun findBestExecutableBinary(files: List<File>): File? {
        if (files.isEmpty()) return null

        // 1. Check for ELF magic bytes (0x7F 'E' 'L' 'F')
        val elfFiles = files.filter { file ->
            if (!file.isFile || file.length() < 100) return@filter false
            try {
                FileInputStream(file).use { stream ->
                    val magic = ByteArray(4)
                    val read = stream.read(magic)
                    read == 4 && magic[0] == 0x7F.toByte() && magic[1] == 'E'.code.toByte() &&
                            magic[2] == 'L'.code.toByte() && magic[3] == 'F'.code.toByte()
                }
            } catch (_: Exception) {
                false
            }
        }

        if (elfFiles.isNotEmpty()) {
            // Prioritize ARMv8 / 64-bit dotprod if multiple
            val prioritized = elfFiles.sortedWith(
                compareByDescending<File> { it.name.contains("dotprod", ignoreCase = true) }
                    .thenByDescending { it.name.contains("armv8", ignoreCase = true) || it.name.contains("arm64", ignoreCase = true) }
                    .thenByDescending { it.name.contains("stockfish", ignoreCase = true) }
                    .thenByDescending { it.length() } // Largest contains NNUE weights
            )
            return prioritized.first()
        }

        // 2. Fallback by name heuristics
        return files.filter { it.isFile }
            .sortedWith(
                compareByDescending<File> { it.name.contains("stockfish", ignoreCase = true) }
                    .thenByDescending { it.name.contains("arm", ignoreCase = true) }
                    .thenByDescending { it.length() }
            ).firstOrNull()
    }
}
