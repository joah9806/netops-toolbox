package com.qoder.netops.core

enum class TerminalLineKind {
    COMMAND, OUTPUT, SUCCESS, ERROR, INFO
}

data class TerminalLine(
    val text: String,
    val kind: TerminalLineKind = TerminalLineKind.OUTPUT
)

data class PingSummary(
    val sent: Int,
    val received: Int,
    val lossPercent: Double,
    val minMs: Double? = null,
    val avgMs: Double? = null,
    val maxMs: Double? = null
)

enum class PingMode(val label: String, val count: Int?) {
    FOUR("4 次", 4),
    SIXTEEN("16 次", 16),
    CONTINUOUS("持续", null)
}

data class PingUiState(
    val host: String = "",
    val mode: PingMode = PingMode.FOUR,
    val running: Boolean = false,
    val started: Boolean = false,
    val lines: List<TerminalLine> = emptyList(),
    val summary: PingSummary? = null
)

enum class PortStatus(val label: String) {
    OPEN("开放"),
    CLOSED("关闭"),
    TIMEOUT("超时"),
    ERROR("错误")
}

data class PortResult(
    val port: Int,
    val status: PortStatus,
    val latencyMs: Long? = null,
    val banner: String? = null,
    val service: String? = null
)

data class PortUiState(
    val host: String = "",
    val portsInput: String = "22, 80, 443, 3389, 8080",
    val running: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val results: List<PortResult> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

data class LocalNetworkInfo(
    val ip: String,
    val prefixLength: Int,
    val cidr: String,
    val gateway: String?,
    val interfaceName: String
)

data class LanDevice(
    val ip: String,
    val hostname: String? = null,
    val mac: String? = null,
    val vendor: String? = null,
    val latencyMs: Double? = null,
    val isGateway: Boolean = false,
    val isSelf: Boolean = false
)

data class LanUiState(
    val local: LocalNetworkInfo? = null,
    val detecting: Boolean = true,
    val target: String = "",
    val running: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val devices: List<LanDevice> = emptyList(),
    val message: String? = null,
    val error: String? = null,
    val elapsedSeconds: Double? = null
)