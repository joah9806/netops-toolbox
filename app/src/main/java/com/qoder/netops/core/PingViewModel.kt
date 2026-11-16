package com.qoder.netops.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoder.netops.net.NetParsers
import com.qoder.netops.net.PingTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PingViewModel : ViewModel() {

    private val tool = PingTool()
    private val _state = MutableStateFlow(PingUiState())
    val state: StateFlow<PingUiState> = _state.asStateFlow()

    private var job: Job? = null
    private val rawOutput = StringBuilder()

    fun onHostChange(value: String) {
        if (!_state.value.running) {
            _state.update { it.copy(host = value) }
        }
    }

    fun onModeChange(mode: PingMode) {
        if (!_state.value.running) {
            _state.update { it.copy(mode = mode) }
        }
    }

    fun start() {
        if (_state.value.running) return
        val host = NetParsers.normalizeTargetHost(_state.value.host)
        if (host.isBlank()) {
            appendError("请输入主机地址或域名")
            return
        }
        val mode = _state.value.mode
        rawOutput.clear()
        val command = buildString {
            append("$ ping ")
            if (mode.count != null) append("-c ${mode.count} ")
            append("-W 2 ")
            append(host)
        }
        _state.update {
            it.copy(
                host = host,
                running = true,
                started = true,
                summary = null,
                lines = listOf(TerminalLine(command, TerminalLineKind.COMMAND))
            )
        }

        job = viewModelScope.launch {
            try {
                val exit = tool.execute(host, mode.count, 2) { line -> handleLine(line) }
                val summary = NetParsers.parsePingSummary(rawOutput.toString())
                if (summary != null) {
                    _state.update { it.copy(summary = summary) }
                }
                if (exit != 0 && summary == null) {
                    appendError("Ping 进程退出码 $exit，未能获取统计信息")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendError("执行失败：${e.message ?: e.javaClass.simpleName}")
            } finally {
                _state.update { it.copy(running = false) }
            }
        }
    }

    fun stop() {
        if (!_state.value.running) return
        tool.stop()
        job?.cancel()
        append(TerminalLine("■ 已停止", TerminalLineKind.INFO))
        val summary = NetParsers.parsePingSummary(rawOutput.toString())
        _state.update { it.copy(running = false, summary = summary ?: it.summary) }
    }

    private fun handleLine(line: String) {
        val trimmed = line.trimEnd()
        if (trimmed.isBlank()) return
        rawOutput.append(trimmed).append('\n')
        val kind = when {
            trimmed.startsWith("PING ") -> TerminalLineKind.INFO
            trimmed.contains("Destination Host Unreachable", ignoreCase = true) ||
                trimmed.contains("100% packet loss") -> TerminalLineKind.ERROR
            trimmed.contains("unknown host", ignoreCase = true) ||
                trimmed.contains("cannot resolve", ignoreCase = true) ||
                trimmed.contains("Operation not permitted", ignoreCase = true) ->
                TerminalLineKind.ERROR
            trimmed.contains("packet loss") ||
                trimmed.contains("packets transmitted") -> TerminalLineKind.INFO
            trimmed.startsWith("rtt ") || trimmed.startsWith("round-trip") ->
                TerminalLineKind.SUCCESS
            trimmed.contains("time=") || trimmed.contains("time<") ->
                TerminalLineKind.SUCCESS
            trimmed.startsWith("---") -> TerminalLineKind.INFO
            else -> TerminalLineKind.OUTPUT
        }
        append(TerminalLine(trimmed, kind))
    }

    private fun appendError(text: String) {
        append(TerminalLine("! $text", TerminalLineKind.ERROR))
    }

    private fun append(line: TerminalLine) {
        _state.update { current ->
            val lines = if (current.lines.size >= 600) {
                current.lines.drop(current.lines.size - 550) + line
            } else {
                current.lines + line
            }
            current.copy(lines = lines)
        }
    }
}