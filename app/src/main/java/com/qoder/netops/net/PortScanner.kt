package com.qoder.netops.net

import com.qoder.netops.core.PortResult
import com.qoder.netops.core.PortStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object PortScanner {

    suspend fun scan(
        host: String,
        ports: List<Int>,
        timeoutMs: Int,
        concurrency: Int = 32,
        onResult: suspend (PortResult) -> Unit
    ) = coroutineScope {
        val address = withContext(Dispatchers.IO) { InetAddress.getByName(host) }
        val semaphore = Semaphore(concurrency)
        ports.map { port ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    onResult(probe(address, port, timeoutMs))
                }
            }
        }.awaitAll()
        Unit
    }

    private fun probe(address: InetAddress, port: Int, timeoutMs: Int): PortResult {
        val started = System.nanoTime()
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(address, port), timeoutMs)
            val latency = (System.nanoTime() - started) / 1_000_000
            val banner = readBanner(socket)
            PortResult(
                port = port,
                status = PortStatus.OPEN,
                latencyMs = latency,
                banner = banner,
                service = NetParsers.serviceName(port)
            )
        } catch (_: SocketTimeoutException) {
            PortResult(port, PortStatus.TIMEOUT, service = NetParsers.serviceName(port))
        } catch (_: ConnectException) {
            PortResult(port, PortStatus.CLOSED, service = NetParsers.serviceName(port))
        } catch (_: IOException) {
            PortResult(port, PortStatus.ERROR, service = NetParsers.serviceName(port))
        } finally {
            runCatching { socket?.close() }
        }
    }

    private fun readBanner(socket: Socket): String? {
        return try {
            socket.soTimeout = 350
            val buffer = ByteArray(160)
            val read = socket.getInputStream().read(buffer)
            if (read <= 0) {
                null
            } else {
                var text = String(buffer, 0, read, Charsets.ISO_8859_1)
                    .replace(Regex("""[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]"""), ".")
                    .lineSequence()
                    .firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.length > 72) text = text.take(72) + "…"
                text.ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }
}