package com.qoder.netops.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs the platform's ping binary and streams its output. Using the binary
 * instead of InetAddress.isReachable gives real ICMP behaviour and timing.
 */
class PingTool {

    @Volatile
    private var runningProcess: Process? = null

    suspend fun execute(
        host: String,
        count: Int?,
        timeoutSeconds: Int,
        onLine: suspend (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val binary = if (File("/system/bin/ping").canExecute()) "/system/bin/ping" else "ping"
        val command = mutableListOf(binary, "-W", timeoutSeconds.toString())
        if (count != null) {
            command += "-c"
            command += count.toString()
        }
        command += host

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        runningProcess = process
        try {
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    onLine(line)
                }
            }
            process.waitFor()
            runCatching { process.exitValue() }.getOrDefault(-1)
        } finally {
            if (runningProcess === process) runningProcess = null
            process.destroy()
        }
    }

    fun stop() {
        runningProcess?.destroy()
    }
}