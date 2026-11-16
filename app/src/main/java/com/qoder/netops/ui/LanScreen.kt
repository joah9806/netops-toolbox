package com.qoder.netops.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qoder.netops.core.LanDevice
import com.qoder.netops.core.LanUiState
import com.qoder.netops.core.LanViewModel
import com.qoder.netops.ui.theme.MonoFamily
import com.qoder.netops.ui.theme.netOpsPalette
import java.util.Locale

@Composable
fun LanScreen(vm: LanViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        ScreenHeader(
            eyebrow = "TOOL 03 / LAN",
            title = "局域网扫描",
            description = "发现当前网段内的在线设备，读取延迟、主机名与 MAC 地址。"
        )
        Spacer(Modifier.height(12.dp))

        LocalInfoCard(state, onRefresh = vm::refreshLocal)
        Spacer(Modifier.height(10.dp))

        ScanCard(state, vm)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("在线设备", style = MaterialTheme.typography.titleMedium)
            if (state.devices.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                StatusPill(
                    text = "${state.devices.size}",
                    color = MaterialTheme.colorScheme.onSurface,
                    container = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            val trailing = when {
                state.running -> "${state.scanned} / ${state.total}"
                state.elapsedSeconds != null -> String.format(Locale.US, "%.1f s", state.elapsedSeconds)
                else -> null
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    fontFamily = MonoFamily,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.devices.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EmptyPanel(
                    title = if (state.running) "正在扫描" else "暂无设备",
                    hint = if (state.running) {
                        "正在逐个探测地址，发现设备后会实时出现在这里。"
                    } else {
                        "输入网段并开始扫描。部分设备屏蔽 ICMP 时可能无法被发现。"
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(state.devices, key = { it.ip }) { device ->
                    DeviceRow(device)
                }
            }
        }
    }
}

@Composable
private fun LocalInfoCard(state: LanUiState, onRefresh: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val local = state.local
    CardSurface {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "本机地址",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = local?.ip ?: "—",
                        fontFamily = MonoFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }
                if (state.detecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = scheme.onSurfaceVariant,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    SmallAction(
                        label = "刷新",
                        onClick = onRefresh,
                        enabled = !state.running
                    )
                }
            }
            if (local != null) {
                Spacer(Modifier.height(10.dp))
                Row {
                    InfoMini("网段", local.cidr, Modifier.weight(1.4f))
                    InfoMini("网关", local.gateway ?: "—", Modifier.weight(1f))
                    InfoMini("接口", local.interfaceName, Modifier.weight(0.8f))
                }
            } else if (!state.detecting && state.error == null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "未检测到可用的 IPv4 网络，请连接 Wi-Fi 或有线网络后刷新。",
                    fontSize = 10.5.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            state.error?.let { message ->
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "! $message",
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                    color = scheme.error
                )
            }
        }
    }
}

@Composable
private fun InfoMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = MonoFamily,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScanCard(state: LanUiState, vm: LanViewModel) {
    val scheme = MaterialTheme.colorScheme
    val palette = netOpsPalette()
    val cidr = state.local?.cidr
    CardSurface {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonoField(
                    value = state.target,
                    onValueChange = vm::onTargetChange,
                    label = "扫描范围",
                    placeholder = "192.168.1.0/24",
                    modifier = Modifier.weight(1f),
                    enabled = !state.running,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )
                SquareActionButton(
                    running = state.running,
                    enabled = state.running || state.target.isNotBlank(),
                    onClick = { if (state.running) vm.stopScan() else vm.startScan() }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniChip(
                    label = "本网段",
                    selected = cidr != null && state.target == cidr,
                    enabled = !state.running && cidr != null,
                    onClick = vm::useDetectedNetwork
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "最多 1024 个地址",
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    color = palette.terminalDim
                )
            }
            if (state.running || state.message != null || (state.error != null && !state.running)) {
                Spacer(Modifier.height(10.dp))
                if (state.running) {
                    ThinProgressBar(
                        progress = if (state.total > 0) state.scanned.toFloat() / state.total else 0f
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            state.error != null && !state.running -> "! ${state.error}"
                            else -> state.message.orEmpty()
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = 10.5.sp,
                        color = if (state.error != null && !state.running) scheme.error else scheme.onSurfaceVariant
                    )
                    if (state.running) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${state.scanned} / ${state.total}",
                            fontFamily = MonoFamily,
                            fontSize = 9.5.sp,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: LanDevice) {
    val scheme = MaterialTheme.colorScheme
    val palette = netOpsPalette()
    val isGateway = device.isGateway
    val bubbleContainer = when {
        isGateway -> palette.successContainer
        else -> palette.accentSoft
    }
    val bubbleColor = when {
        isGateway -> palette.success
        device.isSelf -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val detail = buildString {
        device.mac?.let { append(it) }
        device.vendor?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(
            1.dp,
            if (isGateway) palette.success.copy(alpha = 0.38f) else scheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bubbleContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = device.ip.substringAfterLast("."),
                    fontFamily = MonoFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = bubbleColor
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.ip,
                        fontFamily = MonoFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface
                    )
                    if (isGateway) {
                        Spacer(Modifier.width(5.dp))
                        TinyBadge("网关", palette.success, palette.successContainer)
                    }
                    if (device.isSelf) {
                        Spacer(Modifier.width(5.dp))
                        TinyBadge("本机", scheme.onSurface, scheme.surfaceVariant)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = device.hostname ?: "主机名未解析",
                    fontSize = 10.5.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotEmpty()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = detail,
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        color = palette.terminalDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = device.latencyMs?.let {
                    String.format(Locale.US, "%.1f ms", it)
                } ?: "—",
                fontFamily = MonoFamily,
                fontSize = 9.5.sp,
                color = if (device.latencyMs != null) palette.success else palette.terminalDim
            )
        }
    }
}