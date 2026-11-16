package com.qoder.netops

import com.qoder.netops.net.NetParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NetParsersTest {

    @Test
    fun cidr24ReturnsUsableHostsOnly() {
        val targets = NetParsers.parseScanTargets("192.168.1.0/24")
        assertEquals(254, targets.size)
        assertEquals("192.168.1.1", targets.first())
        assertEquals("192.168.1.254", targets.last())
    }

    @Test
    fun cidrAcceptsHostAddressAndNormalizesToNetwork() {
        val targets = NetParsers.parseScanTargets("192.168.1.77/29")
        assertEquals(6, targets.size)
        assertEquals("192.168.1.73", targets.first())
        assertEquals("192.168.1.78", targets.last())
    }

    @Test
    fun cidr30And31And32() {
        assertEquals(
            listOf("10.0.0.1", "10.0.0.2"),
            NetParsers.parseScanTargets("10.0.0.0/30")
        )
        assertEquals(
            listOf("10.0.0.0", "10.0.0.1"),
            NetParsers.parseScanTargets("10.0.0.0/31")
        )
        assertEquals(
            listOf("10.0.0.7"),
            NetParsers.parseScanTargets("10.0.0.7/32")
        )
    }

    @Test
    fun largeCidrIsRejected() {
        try {
            NetParsers.parseScanTargets("10.0.0.0/8")
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("范围过大"))
        }
    }

    @Test
    fun lastOctetRange() {
        val targets = NetParsers.parseScanTargets("192.168.31.1-10")
        assertEquals(10, targets.size)
        assertEquals("192.168.31.1", targets.first())
        assertEquals("192.168.31.10", targets.last())
    }

    @Test
    fun fullRangeInSameSubnet() {
        val targets = NetParsers.parseScanTargets("192.168.1.1 - 192.168.1.5")
        assertEquals(listOf("192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4", "192.168.1.5"), targets)
    }

    @Test
    fun invalidAddressesAreRejected() {
        listOf("192.168.1.256", "192.168.1", "not-an-ip", "192.168.1.5-1", "192.168.1.1-192.168.2.3").forEach { value ->
            try {
                NetParsers.parseScanTargets(value)
                fail("Expected IllegalArgumentException for $value")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun portsAreDeduplicatedAndSorted() {
        val ports = NetParsers.parsePorts("443, 22 80, 22 8080")
        assertEquals(listOf(22, 80, 443, 8080), ports)
    }

    @Test
    fun portRangeExpands() {
        val ports = NetParsers.parsePorts("1-1024")
        assertEquals(1024, ports.size)
        assertEquals(1, ports.first())
        assertEquals(1024, ports.last())
    }

    @Test
    fun invalidPortsAreRejected() {
        listOf("0", "65536", "10-1", "1-3000", "abc").forEach { value ->
            try {
                NetParsers.parsePorts(value)
                fail("Expected IllegalArgumentException for $value")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun parsesIputilsPingSummary() {
        val output = """
            PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.
            64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=24.5 ms

            --- 8.8.8.8 ping statistics ---
            4 packets transmitted, 4 received, 0% packet loss, time 3004ms
            rtt min/avg/max/mdev = 24.5/25.1/26.0/0.6 ms
        """.trimIndent()

        val summary = NetParsers.parsePingSummary(output)!!
        assertEquals(4, summary.sent)
        assertEquals(4, summary.received)
        assertEquals(0.0, summary.lossPercent, 0.001)
        assertEquals(24.5, summary.minMs!!, 0.001)
        assertEquals(25.1, summary.avgMs!!, 0.001)
        assertEquals(26.0, summary.maxMs!!, 0.001)
    }

    @Test
    fun parsesToyboxPingSummaryWithLoss() {
        val output = """
            --- 192.168.1.1 ping statistics ---
            3 packets transmitted, 2 packets received, 33% packet loss
            round-trip min/avg/max/stddev = 1.2/2.4/3.6/1.2 ms
        """.trimIndent()

        val summary = NetParsers.parsePingSummary(output)!!
        assertEquals(3, summary.sent)
        assertEquals(2, summary.received)
        assertEquals(33.0, summary.lossPercent, 0.001)
        assertEquals(2.4, summary.avgMs!!, 0.001)
    }

    @Test
    fun noSummaryWhenPingFailsToResolve() {
        assertNull(NetParsers.parsePingSummary("ping: unknown host nope.invalid"))
    }

    @Test
    fun parsesPingTime() {
        assertEquals(12.34, NetParsers.parsePingTimeMs("64 bytes from 1.1.1.1: time=12.34 ms")!!, 0.001)
        assertEquals(1.0, NetParsers.parsePingTimeMs("64 bytes from 1.1.1.1: time<1 ms")!!, 0.001)
        assertNull(NetParsers.parsePingTimeMs("Destination Host Unreachable"))
    }

    @Test
    fun parsesGatewayFromProcRoute() {
        val text = """
            Iface	Destination	Gateway 	Flags	RefCnt	Use	Metric	Mask	MTU	Window	IRTT
            wlan0	00000000	0101A8C0	0003	0	0	0	00000000	0	0	0
            wlan0	0001A8C0	00000000	0001	0	0	0	00FFFFFF	0	0	0
        """.trimIndent()
        assertEquals("192.168.1.1", NetParsers.parseDefaultGatewayFromProcRoute(text))
    }

    @Test
    fun parsesGatewayFromIpRoute() {
        val text = """
            default via 192.168.31.1 dev wlan0 proto dhcp src 192.168.31.23 metric 303
            192.168.31.0/24 dev wlan0 proto kernel scope link src 192.168.31.23
        """.trimIndent()
        assertEquals("192.168.31.1", NetParsers.parseDefaultGatewayFromIpRoute(text))
    }

    @Test
    fun parsesArpTableAndSkipsIncompleteEntries() {
        val text = """
            IP address       HW type     Flags       HW address            Mask     Device
            192.168.1.1      0x1         0x2         aa:bb:cc:dd:ee:ff     *        wlan0
            192.168.1.2      0x1         0x0         00:00:00:00:00:00     *        wlan0
            192.168.1.3      0x1         0x2         f4:f5:d8:11:22:33     *        wlan0
        """.trimIndent()
        val arp = NetParsers.parseArpTable(text)
        assertEquals(2, arp.size)
        assertEquals("AA:BB:CC:DD:EE:FF", arp["192.168.1.1"])
        assertEquals("F4:F5:D8:11:22:33", arp["192.168.1.3"])
        assertNull(arp["192.168.1.2"])
    }

    @Test
    fun parsesIpNeighAndIgnoresIpv6() {
        val text = """
            192.168.1.1 dev wlan0 lladdr AA:BB:CC:DD:EE:FF REACHABLE
            fe80::1 dev wlan0 lladdr 11:22:33:44:55:66 router
            192.168.1.9 dev wlan0  FAILED
        """.trimIndent()
        val arp = NetParsers.parseIpNeigh(text)
        assertEquals(mapOf("192.168.1.1" to "AA:BB:CC:DD:EE:FF"), arp)
    }

    @Test
    fun normalizesUserEnteredHosts() {
        assertEquals("example.com", NetParsers.normalizeTargetHost(" https://example.com:8080/path?q=1 "))
        assertEquals("192.168.1.1", NetParsers.normalizeTargetHost("192.168.1.1:80"))
        assertEquals("2001:db8::1", NetParsers.normalizeTargetHost("[2001:db8::1]"))
        assertEquals("wlan-host", NetParsers.normalizeTargetHost("wlan-host"))
    }

    @Test
    fun serviceNamesCoverCommonPorts() {
        assertEquals("SSH", NetParsers.serviceName(22))
        assertEquals("HTTPS", NetParsers.serviceName(443))
        assertEquals("MySQL", NetParsers.serviceName(3306))
        assertNull(NetParsers.serviceName(65001))
    }
}