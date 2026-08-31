import java.io.File

fun main() {
    val execPath = "app/src/main/jniLibs/arm64-v8a/libstockfish.so"
    val pb = ProcessBuilder(execPath)
    pb.redirectErrorStream(true)
    try {
        val process = pb.start()
        val reader = process.inputStream.bufferedReader()
        println("Output: " + reader.readLine())
    } catch (e: Exception) {
        println("Error: " + e.message)
    }
}
