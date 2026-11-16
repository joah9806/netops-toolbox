package com.qoder.netops.net

import com.qoder.netops.core.LanDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object LanScanner {

    /**
     * Sweeps the targets with ICMP, then resolves reverse DNS and ARP entries for
     * the devices that answered. Results are emitted as soon as they are known.
     */
    suspend fun scan(
        targets: List<String>,
        selfIp: String?,
        gateway: String?,
        onProgress: suspend (scanned: Int, total: Int) -> Unit,
        onFound: suspend (LanDevice) -> Unit,
        onPhase: suspend (String) -> Unit
    ): Int = coroutineScope {
        val found = ConcurrentHashMap<String, LanDevice>()
        val completed = AtomicInteger(0)
        val selfInTargets = selfIp != null && targets.contains(selfIp)
        val semaphore = Semaphore(48)

        if (selfInTargets) {
            val self = LanDevice(
                ip = selfIp,
                hostname = "本机",
                latencyMs = null,
                isSelf = true,
                isGateway = selfIp == gateway
            )
            found[selfIp] = self
            onFound(self)
            completed.set(1)
        }
        onProgress(completed.get(), targets.size)

        targets.filter { it != selfIp }.map { ip ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val latency = pingOnce(ip, timeoutSeconds = 1)
                    if (latency != null) {
                        val device = LanDevice(
                            ip = ip,
                            latencyMs = latency,
                            isGateway = ip == gateway
                        )
                        found[ip] = device
                        onFound(device)
                    }
                    val done = completed.incrementAndGet()
                    onProgress(done, targets.size)
                }
            }
        }.awaitAll()

        onPhase("读取 ARP 缓存与主机名…")
        delay(300)
        val arp = LocalNetworkDetector.readArpTable()

        val resolveSemaphore = Semaphore(12)
        val enriched = found.values.map { device ->
            async(Dispatchers.IO) {
                resolveSemaphore.withPermit {
                    if (device.isSelf) {
                        device.copy(mac = null, vendor = null, hostname = "本机")
                    } else {
                        val hostname = resolveHostname(device.ip)
                        val mac = arp[device.ip]
                        device.copy(
                            hostname = hostname,
                            mac = mac,
                            vendor = OuiLookup.vendorFor(mac)
                        )
                    }
                }
            }
        }.awaitAll()

        val sorted = enriched.sortedBy { NetParsers.ipv4ToLong(it.ip) ?: 0L }
        sorted.forEach { onFound(it) }
        sorted.size
    }

    private suspend fun pingOnce(ip: String, timeoutSeconds: Int): Double? =
        withContext(Dispatchers.IO) {
            var process: Process? = null
            try {
                val binary = if (File("/system/bin/ping").canExecute()) "/system/bin/ping" else "ping"
                process = ProcessBuilder(
                    binary,
                    "-c", "1",
                    "-W", timeoutSeconds.toString(),
                    ip
                ).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exit = process.waitFor()
                if (exit == 0) {
                    NetParsers.parsePingTimeMs(output) ?: 1.0
                } else {
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } finally {
                runCatching { process?.destroy() }
            }
        }

    private suspend fun resolveHostname(ip: String): String? = withTimeoutOrNull(800) {
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = ip.split(".").map { it.toInt().toByte() }.toByteArray()
                val name = InetAddress.getByAddress(bytes).canonicalHostName
                name.takeIf { it.isNotBlank() && it != ip }
            }.getOrNull()
        }
    }
}