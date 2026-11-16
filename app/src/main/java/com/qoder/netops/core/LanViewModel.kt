package com.qoder.netops.core

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoder.netops.net.LanScanner
import com.qoder.netops.net.LocalNetworkDetector
import com.qoder.netops.net.NetParsers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class LanViewModel : ViewModel() {

    private val _state = MutableStateFlow(LanUiState())
    val state: StateFlow<LanUiState> = _state.asStateFlow()

    private val deviceMap = ConcurrentHashMap<String, LanDevice>()
    private var scanJob: Job? = null

    init {
        refreshLocal()
    }

    fun refreshLocal() {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.update { it.copy(detecting = true, error = null) }
            val info = withContext(Dispatchers.IO) { LocalNetworkDetector.detect() }
            _state.update { current ->
                val target = if (current.target.isBlank() && info != null) info.cidr else current.target
                current.copy(
                    local = info,
                    detecting = false,
                    target = target,
                    error = if (info == null) "未检测到局域网 IPv4 地址，请先连接 Wi-Fi" else null
                )
            }
        }
    }

    fun onTargetChange(value: String) {
        if (!_state.value.running) {
            _state.update { it.copy(target = value, error = null) }
        }
    }

    fun useDetectedNetwork() {
        if (_state.value.running) return
        val cidr = _state.value.local?.cidr ?: return
        _state.update { it.copy(target = cidr, error = null) }
    }

    fun startScan() {
        if (_state.value.running) return
        val targets = try {
            NetParsers.parseScanTargets(_state.value.target)
        } catch (e: IllegalArgumentException) {
            _state.update { it.copy(error = e.message ?: "扫描范围格式不正确") }
            return
        }
        val local = _state.value.local
        deviceMap.clear()
        _state.update {
            it.copy(
                running = true,
                scanned = 0,
                total = targets.size,
                devices = emptyList(),
                message = "正在探测 ${targets.size} 个地址…",
                error = null,
                elapsedSeconds = null
            )
        }

        scanJob = viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            try {
                LanScanner.scan(
                    targets = targets,
                    selfIp = local?.ip,
                    gateway = local?.gateway,
                    onProgress = { scanned, total ->
                        _state.update { it.copy(scanned = scanned, total = total) }
                    },
                    onFound = { device ->
                        deviceMap[device.ip] = device
                        publishDevices()
                    },
                    onPhase = { phase ->
                        _state.update { it.copy(message = phase) }
                    }
                )
                val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
                val count = _state.value.devices.size
                _state.update {
                    it.copy(
                        running = false,
                        elapsedSeconds = elapsed,
                        message = "扫描完成 · 发现 $count 台设备 · 用时 " +
                            String.format(Locale.US, "%.1f", elapsed) + " 秒"
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(running = false, message = "已停止") }
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        running = false,
                        message = null,
                        error = "扫描失败：${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _state.update { it.copy(running = false, message = "已停止") }
    }

    private fun publishDevices() {
        val sorted = deviceMap.values.sortedBy { NetParsers.ipv4ToLong(it.ip) ?: 0L }
        _state.update { it.copy(devices = sorted) }
    }
}