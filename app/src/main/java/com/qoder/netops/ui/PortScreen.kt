package com.qoder.netops.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qoder.netops.core.PortResult
import com.qoder.netops.core.PortStatus
import com.qoder.netops.core.PortUiState
import com.qoder.netops.core.PortViewModel
import com.qoder.netops.ui.theme.MonoFamily
import com.qoder.netops.ui.theme.netOpsPalette

private data class PortPreset(val label: String, val value: String)

private val presets = listOf(
    PortPreset(
        "常用",
        "21,22,23,25,53,80,110,143,443,445,1433,3306,3389,5432,5900,6379,8080,8443"
    ),
    PortPreset("Web", "80,443,8080,8443"),
    PortPreset("远程", "22,23,3389,5900"),
    PortPreset("数据库", "3306,5432,1433,6379,27017"),
    PortPreset("1-1024", "1-1024")
)

@Composable
fun PortScreen(vm: PortViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val openCount = state.results.count { it.status == PortStatus.OPEN }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        ScreenHeader(
            eyebrow = "TOOL 02 / TCP",
            title = "端口连通性",
            description = "通过 TCP 握手检测端口开放状态，支持多端口、范围与 Banner 识别。"
        )
        Spacer(Modifier.height(14.dp))

        CardSurface {
            Column(Modifier.padding(14.dp)) {
                MonoField(
                    value = state.host,
                    onValueChange = vm::onHostChange,
                    label = "主机 / 域名",
                    placeholder = "192.168.1.1 或 example.com",
                    enabled = !state.running,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
                Spacer(Modifier.height(10.dp))
                MonoField(
                    value = state.portsInput,
                    onValueChange = vm::onPortsChange,
                    label = "端口列表",
                    placeholder = "22,80,443 或 1-1024",
                    enabled = !state.running,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )
                Spacer(Modifier.height(10.dp))
                ChipsRow {
                    presets.forEach { preset ->
                        MiniChip(
                            label = preset.label,
                            enabled = !state.running,
                            onClick = { vm.applyPreset(preset.value) }
                        )
                    }
                }
                state.error?.let { message ->
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "! $message",
                        fontFamily = MonoFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrimaryActionButton(
                    label = "开始探测",
                    running = state.running,
                    enabled = state.running || state.host.isNotBlank(),
                    onClick = { if (state.running) vm.stop() else vm.start() }
                )
            }
        }

        if (state.running || state.message != null) {
            Spacer(Modifier.height(10.dp))
            PortProgressCard(state, openCount)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("探测结果", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            if (openCount > 0) {
                val palette = netOpsPalette()
                StatusPill(
                    text = "$openCount OPEN",
                    color = palette.success,
                    container = palette.successContainer
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.total > 0) {
                Text(
                    text = "${state.completed} / ${state.total}",
                    fontFamily = MonoFamily,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.results.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EmptyPanel(
                    title = if (state.running) "正在探测" else "暂无结果",
                    hint = if (state.running) {
                        "端口结果会随着握手完成实时出现。"
                    } else {
                        "输入目标主机与端口后开始探测，开放端口会标记为绿色。"
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
                items(state.results, key = { it.port }) { result ->
                    PortResultRow(result)
                }
            }
        }
    }
}

@Composable
private fun PortProgressCard(state: PortUiState, openCount: Int) {
    val palette = netOpsPalette()
    val scheme = MaterialTheme.colorScheme
    CardSurface {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.running) "SCANNING" else "COMPLETED",
                    fontFamily = MonoFamily,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp,
                    color = palette.terminalDim
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "开放 $openCount",
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                    color = if (openCount > 0) palette.success else scheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${state.completed}/${state.total}",
                    fontFamily = MonoFamily,
                    fontSize = 10.5.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            ThinProgressBar(
                progress = if (state.total > 0) state.completed.toFloat() / state.total else 0f,
                color = if (openCount > 0) palette.success else scheme.onBackground
            )
            state.message?.let { message ->
                Spacer(Modifier.height(7.dp))
                Text(
                    text = message,
                    fontSize = 10.5.sp,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PortResultRow(result: PortResult) {
    val palette = netOpsPalette()
    val scheme = MaterialTheme.colorScheme
    val isOpen = result.status == PortStatus.OPEN
    val statusColor: androidx.compose.ui.graphics.Color
    val statusContainer: androidx.compose.ui.graphics.Color
    when (result.status) {
        PortStatus.OPEN -> {
            statusColor = palette.success
            statusContainer = palette.successContainer
        }

        PortStatus.CLOSED -> {
            statusColor = scheme.onSurfaceVariant
            statusContainer = scheme.surfaceVariant
        }

        PortStatus.TIMEOUT -> {
            statusColor = palette.warning
            statusContainer = palette.warningContainer
        }

        PortStatus.ERROR -> {
            statusColor = scheme.error
            statusContainer = scheme.errorContainer
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(
            1.dp,
            if (isOpen) palette.success.copy(alpha = 0.38f) else scheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${result.port}",
                modifier = Modifier.width(52.dp),
                fontFamily = MonoFamily,
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = if (isOpen) palette.success else scheme.onSurface
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = result.service ?: "未知服务",
                    fontSize = 11.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = scheme.onSurface
                )
                result.banner?.let { banner ->
                    Text(
                        text = banner,
                        fontFamily = MonoFamily,
                        fontSize = 9.5.sp,
                        color = palette.terminalDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(result.status.label, statusColor, statusContainer)
                result.latencyMs?.let { latency ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$latency ms",
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        color = palette.terminalDim
                    )
                }
            }
        }
    }
}