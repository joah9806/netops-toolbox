package com.qoder.netops.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoder.netops.net.NetParsers
import com.qoder.netops.net.PortScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class PortViewModel : ViewModel() {

    private val _state = MutableStateFlow(PortUiState())
    val state: StateFlow<PortUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onHostChange(value: String) {
        if (!_state.value.running) {
            _state.update { it.copy(host = value, error = null) }
        }
    }

    fun onPortsChange(value: String) {
        if (!_state.value.running) {
            _state.update { it.copy(portsInput = value, error = null) }
        }
    }

    fun applyPreset(value: String) {
        if (_state.value.running) return
        _state.update { it.copy(portsInput = value, error = null) }
    }

    fun start() {
        if (_state.value.running) return
        val host = NetParsers.normalizeTargetHost(_state.value.host)
        if (host.isBlank()) {
            _state.update { it.copy(error = "请输入主机地址或域名") }
            return
        }
        val ports = try {
            NetParsers.parsePorts(_state.value.portsInput)
        } catch (e: IllegalArgumentException) {
            _state.update { it.copy(error = e.message ?: "端口格式不正确") }
            return
        }

        _state.update {
            it.copy(
                host = host,
                running = true,
                results = emptyList(),
                completed = 0,
                total = ports.size,
                message = "正在探测 ${ports.size} 个端口…",
                error = null
            )
        }

        job = viewModelScope.launch {
            try {
                PortScanner.scan(host, ports, timeoutMs = 1200) { result ->
                    _state.update { current ->
                        val merged = (current.results + result).sortedWith(
                            compareBy({ it.status != PortStatus.OPEN }, { it.port })
                        )
                        current.copy(
                            completed = (current.completed + 1).coerceAtMost(ports.size),
                            results = merged
                        )
                    }
                }
                val open = _state.value.results.count { it.status == PortStatus.OPEN }
                _state.update {
                    it.copy(
                        running = false,
                        message = "完成 · 开放 $open 个 / 共 ${ports.size} 个端口"
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(running = false, message = "已停止") }
                throw e
            } catch (e: UnknownHostException) {
                _state.update {
                    it.copy(
                        running = false,
                        message = null,
                        error = "无法解析主机：$host"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        running = false,
                        message = null,
                        error = "探测失败：${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        _state.update { it.copy(running = false, message = "已停止") }
    }
}