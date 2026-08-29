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
     *
     * Imported archives are treated as untrusted input. Every extracted path is canonicalized
     * and required to remain inside targetDir to prevent path traversal writes.
     */
    fun extract(sourceStream: InputStream, fileName: String, targetDir: File): List<File> {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        require(targetDir.isDirectory) { "Extraction target is not a directory" }

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
                    GZIPInputStream(pushback).use { gzipIn ->
                        extractTarStream(gzipIn, targetDir)
                    }
                } else if (read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
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
                    // Direct binary: never allow the source name to escape targetDir.
                    val safeName = File(fileName.ifEmpty { "stockfish_binary" }).name
                    val rawFile = safeDestination(targetDir, safeName) ?: return emptyList()
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

    /** Returns a canonical destination only when it is strictly within targetDir. */
    private fun safeDestination(targetDir: File, relativeName: String): File? {
        val canonicalRoot = targetDir.canonicalFile
        val destination = File(canonicalRoot, relativeName.replace('\\', '/')).canonicalFile
        val rootPath = canonicalRoot.path
        val destinationPath = destination.path
        return if (destinationPath == rootPath || destinationPath.startsWith(rootPath + File.separator)) {
            destination
        } else {
            Log.w(TAG, "Blocked archive path traversal: $relativeName")
            null
        }
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

            var allZero = true
            for (i in 0 until 512) {
                if (headerBuf[i] != 0.toByte()) {
                    allZero = false
                    break
                }
            }
            if (allZero) break

            var nameLen = 0
            while (nameLen < 100 && headerBuf[nameLen] != 0.toByte()) {
                nameLen++
            }
            val entryName = String(headerBuf, 0, nameLen, Charsets.UTF_8).trim()
            if (entryName.isEmpty()) continue

            val sizeStr = String(headerBuf, 124, 12, Charsets.US_ASCII)
                .trim()
                .replace("\u0000", "")
            val size = try {
                sizeStr.toLong(8)
            } catch (_: Exception) {
                0L
            }
            if (size < 0) {
                Log.w(TAG, "Blocked invalid negative TAR size: $entryName")
                return extractedFiles
            }

            val typeFlag = headerBuf[156].toInt().toChar()
            val destFile = safeDestination(targetDir, entryName)

            if (typeFlag == '5' || entryName.endsWith("/")) {
                if (destFile != null) destFile.mkdirs()
            } else {
                if (destFile == null) {
                    skipFully(inputStream, size)
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
                }

                val padding = (512 - (size % 512)) % 512
                if (padding > 0) skipFully(inputStream, padding)
            }
        }
        return extractedFiles
    }

    private fun skipFully(inputStream: InputStream, byteCount: Long) {
        var remaining = byteCount
        val discard = ByteArray(8192)
        while (remaining > 0) {
            val skipped = inputStream.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                val toRead = minOf(discard.size.toLong(), remaining).toInt()
                val read = inputStream.read(discard, 0, toRead)
                if (read == -1) break
                remaining -= read
            }
        }
    }

    private fun extractZip(zipFile: File, targetDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val destFile = safeDestination(targetDir, entry.name) ?: run {
                    continue
                }
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
            val prioritized = elfFiles.sortedWith(
                compareByDescending<File> { it.name.contains("dotprod", ignoreCase = true) }
                    .thenByDescending { it.name.contains("armv8", ignoreCase = true) || it.name.contains("arm64", ignoreCase = true) }
                    .thenByDescending { it.name.contains("stockfish", ignoreCase = true) }
                    .thenByDescending { it.length() }
            )
            return prioritized.first()
        }

        return files.filter { it.isFile }
            .sortedWith(
                compareByDescending<File> { it.name.contains("stockfish", ignoreCase = true) }
                    .thenByDescending { it.name.contains("arm", ignoreCase = true) }
                    .thenByDescending { it.length() }
            ).firstOrNull()
    }
}
