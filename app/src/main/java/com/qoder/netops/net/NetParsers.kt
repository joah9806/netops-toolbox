package com.qoder.netops.net

import com.qoder.netops.core.PingSummary

/**
 * Pure parsing helpers. Keeping these free of Android APIs makes the tricky
 * input handling (CIDR math, port specs, ping output, routing tables) testable.
 */
object NetParsers {

    private val IPV4_REGEX = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")
    private val MAC_REGEX = Regex("""^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$""")

    fun ipv4ToLong(ip: String): Long? {
        if (!IPV4_REGEX.matches(ip)) return null
        var result = 0L
        val parts = ip.split(".")
        for (part in parts) {
            val value = part.toIntOrNull() ?: return null
            if (value !in 0..255) return null
            result = (result shl 8) or value.toLong()
        }
        return result and 0xFFFFFFFFL
    }

    fun longToIpv4(value: Long): String {
        val v = value and 0xFFFFFFFFL
        return listOf(
            (v shr 24) and 0xFF,
            (v shr 16) and 0xFF,
            (v shr 8) and 0xFF,
            v and 0xFF
        ).joinToString(".")
    }

    fun prefixMask(prefix: Int): Long {
        if (prefix <= 0) return 0L
        if (prefix >= 32) return 0xFFFFFFFFL
        return (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
    }

    /**
     * Accepts:
     *  - a single IPv4 address             192.168.1.10
     *  - CIDR notation                     192.168.1.0/24
     *  - last-octet range                  192.168.1.1-254
     *  - full range within the same /24    192.168.1.1-192.168.1.99
     */
    fun parseScanTargets(raw: String, limit: Int = 1024): List<String> {
        val value = raw.trim()
        require(value.isNotEmpty()) { "请输入扫描范围" }

        if (value.contains("/")) {
            val base = value.substringBefore("/").trim()
            val prefixText = value.substringAfter("/").trim()
            val baseLong = ipv4ToLong(base)
                ?: throw IllegalArgumentException("网段格式不正确，例如 192.168.1.0/24")
            val prefix = prefixText.toIntOrNull()
                ?: throw IllegalArgumentException("子网掩码位数不正确")
            require(prefix in 0..32) { "子网掩码位数需在 0–32 之间" }
            val mask = prefixMask(prefix)
            val network = baseLong and mask
            val broadcast = (network or (mask.inv() and 0xFFFFFFFFL)) and 0xFFFFFFFFL
            var start = network
            var end = broadcast
            if (prefix <= 30) {
                start += 1
                end -= 1
            }
            val count = end - start + 1
            require(count in 1..limit.toLong()) {
                "范围过大：最多支持 $limit 个地址，请使用更小的网段（如 /24）"
            }
            return (start..end).map { longToIpv4(it) }
        }

        val hyphen = value.indexOf('-')
        if (hyphen > 0) {
            val left = value.substring(0, hyphen).trim()
            val right = value.substring(hyphen + 1).trim()
            val leftLong = ipv4ToLong(left)
                ?: throw IllegalArgumentException("起始地址格式不正确")
            if (right.contains(".")) {
                val rightLong = ipv4ToLong(right)
                    ?: throw IllegalArgumentException("结束地址格式不正确")
                require((leftLong shr 8) == (rightLong shr 8)) {
                    "暂只支持同一 /24 网段内的地址范围"
                }
                require(leftLong <= rightLong) { "起始地址不能大于结束地址" }
                val count = rightLong - leftLong + 1
                require(count <= limit) { "范围过大：最多支持 $limit 个地址" }
                return (leftLong..rightLong).map { longToIpv4(it) }
            }
            val endOctet = right.toIntOrNull()
                ?: throw IllegalArgumentException("结束地址格式不正确")
            require(endOctet in 0..255) { "末位地址需在 0–255 之间" }
            val startOctet = (leftLong and 0xFF).toInt()
            require(startOctet <= endOctet) { "起始地址不能大于结束地址" }
            val count = endOctet - startOctet + 1
            require(count <= limit) { "范围过大：最多支持 $limit 个地址" }
            val base = leftLong and 0xFFFFFF00L
            return (startOctet..endOctet).map { longToIpv4(base or it.toLong()) }
        }

        ipv4ToLong(value) ?: throw IllegalArgumentException("IP 地址格式不正确")
        return listOf(value)
    }

    /**
     * Accepts "22,80,443", "22 80 443", "1-1024" or a mix. Deduplicated and sorted.
     */
    fun parsePorts(raw: String, max: Int = 2048): List<Int> {
        val tokens = raw.trim().split(Regex("""[,\s;，、]+""")).filter { it.isNotEmpty() }
        require(tokens.isNotEmpty()) { "请输入端口，例如 22,80,443 或 1-1024" }
        val ports = sortedSetOf<Int>()
        for (token in tokens) {
            if (token.count { it == '-' } == 1) {
                val startText = token.substringBefore('-').trim()
                val endText = token.substringAfter('-').trim()
                val start = startText.toIntOrNull()
                    ?: throw IllegalArgumentException("端口范围格式不正确：$token")
                val end = endText.toIntOrNull()
                    ?: throw IllegalArgumentException("端口范围格式不正确：$token")
                require(start in 1..65535 && end in 1..65535) { "端口需在 1–65535 之间" }
                require(start <= end) { "端口范围起始值不能大于结束值" }
                require(end - start + 1 <= max) { "端口范围过大：最多支持 $max 个端口" }
                for (port in start..end) {
                    ports.add(port)
                    if (ports.size > max) {
                        throw IllegalArgumentException("端口数量过多：最多支持 $max 个端口")
                    }
                }
            } else {
                val port = token.toIntOrNull()
                    ?: throw IllegalArgumentException("端口格式不正确：$token")
                require(port in 1..65535) { "端口需在 1–65535 之间" }
                ports.add(port)
                if (ports.size > max) {
                    throw IllegalArgumentException("端口数量过多：最多支持 $max 个端口")
                }
            }
        }
        return ports.toList()
    }

    fun parsePingSummary(raw: String): PingSummary? {
        val packets = Regex("""(\d+)\s+packets transmitted,\s*(\d+)\s+(?:packets\s+)?received""")
            .find(raw) ?: return null
        val sent = packets.groupValues[1].toIntOrNull() ?: return null
        val received = packets.groupValues[2].toIntOrNull() ?: return null
        val loss = Regex("""([\d.]+)%\s+packet loss""")
            .find(raw)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: if (sent > 0) (sent - received) * 100.0 / sent else 0.0
        val rtt = Regex("""(?:rtt|round-trip)\s+min/avg/max/(?:mdev|stddev)\s*=\s*([\d.]+)/([\d.]+)/([\d.]+)""")
            .find(raw)
        return PingSummary(
            sent = sent,
            received = received,
            lossPercent = loss,
            minMs = rtt?.groupValues?.get(1)?.toDoubleOrNull(),
            avgMs = rtt?.groupValues?.get(2)?.toDoubleOrNull(),
            maxMs = rtt?.groupValues?.get(3)?.toDoubleOrNull()
        )
    }

    fun parsePingTimeMs(line: String): Double? =
        Regex("""time[=<]\s*([\d.]+)\s*ms""").find(line)?.groupValues?.get(1)?.toDoubleOrNull()

    /** /proc/net/route uses little-endian hex for the gateway column. */
    fun parseDefaultGatewayFromProcRoute(text: String): String? {
        text.lineSequence().drop(1).forEach { line ->
            val fields = line.trim().split(Regex("""\s+"""))
            if (fields.size >= 4 && fields[1] == "00000000") {
                val flags = fields[3].toIntOrNull(16) ?: return@forEach
                if (flags and 0x2 == 0) return@forEach
                val hex = fields[2]
                if (hex.length == 8) {
                    val b0 = hex.substring(0, 2).toIntOrNull(16) ?: return@forEach
                    val b1 = hex.substring(2, 4).toIntOrNull(16) ?: return@forEach
                    val b2 = hex.substring(4, 6).toIntOrNull(16) ?: return@forEach
                    val b3 = hex.substring(6, 8).toIntOrNull(16) ?: return@forEach
                    return "$b3.$b2.$b1.$b0"
                }
            }
        }
        return null
    }

    fun parseDefaultGatewayFromIpRoute(text: String): String? =
        Regex("""default\s+via\s+(\d{1,3}(?:\.\d{1,3}){3})""")
            .find(text)?.groupValues?.get(1)

    private fun parseHex(value: String): Int? {
        val cleaned = value.removePrefix("0x").removePrefix("0X")
        return cleaned.toIntOrNull(16)
    }

    fun parseArpTable(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        text.lineSequence().forEach { line ->
            if (line.startsWith("IP address")) return@forEach
            val fields = line.trim().split(Regex("""\s+"""))
            if (fields.size >= 6) {
                val ip = fields[0]
                val flags = parseHex(fields[2]) ?: 0
                val mac = fields[3]
                if (ipv4ToLong(ip) != null && flags and 0x2 != 0 &&
                    MAC_REGEX.matches(mac) && mac != "00:00:00:00:00:00"
                ) {
                    result[ip] = mac.uppercase()
                }
            }
        }
        return result
    }

    fun parseIpNeigh(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        text.lineSequence().forEach { line ->
            val fields = line.trim().split(Regex("""\s+"""))
            if (fields.isEmpty()) return@forEach
            val ip = fields[0]
            if (ipv4ToLong(ip) == null) return@forEach
            val index = fields.indexOf("lladdr")
            if (index in 1 until fields.size - 1) {
                val mac = fields[index + 1]
                if (MAC_REGEX.matches(mac) && mac != "00:00:00:00:00:00") {
                    result[ip] = mac.uppercase()
                }
            }
        }
        return result
    }

    fun normalizeTargetHost(raw: String): String {
        var value = raw.trim()
        if (value.contains("://")) value = value.substringAfter("://")
        value = value.substringBefore("/").substringBefore("?")
        if (!value.startsWith("[") && value.count { it == ':' } == 1) {
            value = value.substringBefore(":")
        }
        return value.removeSurrounding("[", "]").trim()
    }

    fun serviceName(port: Int): String? = when (port) {
        20, 21 -> "FTP"
        22 -> "SSH"
        23 -> "Telnet"
        25 -> "SMTP"
        53 -> "DNS"
        67, 68 -> "DHCP"
        80 -> "HTTP"
        110 -> "POP3"
        123 -> "NTP"
        135 -> "MS RPC"
        139 -> "NetBIOS"
        143 -> "IMAP"
        161 -> "SNMP"
        389 -> "LDAP"
        443 -> "HTTPS"
        445 -> "SMB"
        465 -> "SMTPS"
        514 -> "Syslog"
        587 -> "SMTP"
        631 -> "IPP"
        993 -> "IMAPS"
        995 -> "POP3S"
        1080 -> "SOCKS"
        1433 -> "MSSQL"
        1521 -> "Oracle"
        1701 -> "L2TP"
        1723 -> "PPTP"
        1883 -> "MQTT"
        2049 -> "NFS"
        2375, 2376 -> "Docker"
        3000 -> "Dev Server"
        3306 -> "MySQL"
        3389 -> "RDP"
        5000, 5001 -> "Synology"
        5432 -> "PostgreSQL"
        5555 -> "ADB"
        5900 -> "VNC"
        5985, 5986 -> "WinRM"
        6379 -> "Redis"
        7000 -> "AirPlay"
        8080, 8081 -> "HTTP-Alt"
        8443 -> "HTTPS-Alt"
        8888 -> "HTTP-Alt"
        9000 -> "Portainer"
        9090 -> "Web Console"
        9200 -> "Elasticsearch"
        9100 -> "Printer"
        27017 -> "MongoDB"
        32400 -> "Plex"
        else -> null
    }

    fun formatLatency(ms: Double?): String =
        if (ms == null) "—" else String.format(java.util.Locale.US, "%.1f ms", ms)
}