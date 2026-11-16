package com.qoder.netops.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qoder.netops.core.PingMode
import com.qoder.netops.core.PingSummary
import com.qoder.netops.core.PingViewModel
import com.qoder.netops.ui.theme.netOpsPalette
import java.util.Locale

@Composable
fun PingScreen(vm: PingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        ScreenHeader(
            eyebrow = "TOOL 01 / ICMP",
            title = "Ping 连通性",
            description = "向目标发送 ICMP 探测包，实时查看响应时间与丢包统计。"
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
                    imeAction = ImeAction.Done
                )
                Spacer(Modifier.height(12.dp))
                ChipsRow {
                    PingMode.entries.forEach { mode ->
                        MiniChip(
                            label = mode.label,
                            selected = state.mode == mode,
                            enabled = !state.running,
                            onClick = { vm.onModeChange(mode) }
                        )
                    }
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

        Spacer(Modifier.height(12.dp))
        TerminalPanel(
            lines = state.lines,
            running = state.running,
            emptyHint = "输入主机地址并开始探测，命令与实时结果会显示在这里。",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onCopy = if (state.lines.isNotEmpty()) {
                {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "ping output",
                            state.lines.joinToString("\n") { it.text }
                        )
                    )
                    Toast.makeText(context, "已复制终端内容", Toast.LENGTH_SHORT).show()
                }
            } else {
                null
            }
        )

        AnimatedVisibility(visible = state.summary != null) {
            Column {
                state.summary?.let { summary ->
                    Spacer(Modifier.height(10.dp))
                    PingSummaryCard(summary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PingSummaryCard(summary: PingSummary) {
    val palette = netOpsPalette()
    val scheme = MaterialTheme.colorScheme
    CardSurface {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                label = "丢包率",
                value = String.format(Locale.US, "%.0f%%", summary.lossPercent),
                modifier = Modifier.weight(1f),
                valueColor = if (summary.lossPercent == 0.0) palette.success else scheme.error
            )
            VerticalRule()
            StatCell(
                label = "平均延迟",
                value = summary.avgMs?.let { String.format(Locale.US, "%.1f ms", it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            VerticalRule()
            StatCell(
                label = "接收 / 发送",
                value = "${summary.received} / ${summary.sent}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .padding(horizontal = 12.dp)
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}