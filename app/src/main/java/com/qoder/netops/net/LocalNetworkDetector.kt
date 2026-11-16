package com.qoder.netops.net

import com.qoder.netops.core.LocalNetworkInfo
import java.io.File
import java.net.Inet4Address
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

object LocalNetworkDetector {

    fun detect(): LocalNetworkInfo? {
        val candidates = mutableListOf<Pair<String, InterfaceAddress>>()
        try {
            for (ni in NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()) {
                if (!ni.isUp || ni.isLoopback) continue
                val name = ni.name.orEmpty()
                for (ia in ni.interfaceAddresses) {
                    val address = ia.address
                    if (address is Inet4Address &&
                        !address.isLoopbackAddress &&
                        !address.isLinkLocalAddress &&
                        address.isSiteLocalAddress
                    ) {
                        candidates += name to ia
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }

        val preferred = candidates.minByOrNull { (name, _) -> interfaceRank(name) } ?: return null
        val name = preferred.first
        val ia = preferred.second
        val ip = ia.address.hostAddress ?: return null
        val ipLong = NetParsers.ipv4ToLong(ip) ?: return null
        val prefix = ia.networkPrefixLength.toInt().takeIf { it in 1..32 } ?: 24
        val network = ipLong and NetParsers.prefixMask(prefix)
        val cidr = "${NetParsers.longToIpv4(network)}/$prefix"
        return LocalNetworkInfo(
            ip = ip,
            prefixLength = prefix,
            cidr = cidr,
            gateway = detectGateway(),
            interfaceName = name
        )
    }

    private fun interfaceRank(name: String): Int = when {
        name.startsWith("wlan") -> 0
        name.startsWith("eth") -> 1
        name.startsWith("ap") -> 2
        name.startsWith("swlan") -> 2
        name.startsWith("p2p") -> 3
        else -> 4
    }

    private fun detectGateway(): String? {
        runCatching {
            val text = File("/proc/net/route").readText()
            NetParsers.parseDefaultGatewayFromProcRoute(text)
        }.getOrNull()?.let { return it }

        return runCatching {
            val process = ProcessBuilder("/system/bin/ip", "route")
                .redirectErrorStream(true)
                .start()
            try {
                val text = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor(1200, TimeUnit.MILLISECONDS)
                NetParsers.parseDefaultGatewayFromIpRoute(text)
            } finally {
                process.destroy()
            }
        }.getOrNull()
    }

    fun readArpTable(): Map<String, String> {
        val merged = mutableMapOf<String, String>()
        runCatching {
            NetParsers.parseArpTable(File("/proc/net/arp").readText())
        }.getOrNull()?.let { merged.putAll(it) }

        runCatching {
            val process = ProcessBuilder("/system/bin/ip", "neigh", "show")
                .redirectErrorStream(true)
                .start()
            try {
                val text = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor(1200, TimeUnit.MILLISECONDS)
                NetParsers.parseIpNeigh(text)
            } finally {
                process.destroy()
            }
        }.getOrNull()?.let { merged.putAll(it) }
        return merged
    }
}