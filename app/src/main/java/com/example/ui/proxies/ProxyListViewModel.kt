package com.example.ui.proxies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Proxy
import com.example.network.ProxyFetcher
import com.example.network.MTProtoChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProxyListViewModel(application: Application) : AndroidViewModel(application) {
    private val fetcher = ProxyFetcher(application)

    private val _proxies = MutableStateFlow<List<Proxy>>(emptyList())
    val proxies: StateFlow<List<Proxy>> = _proxies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSource = MutableStateFlow<String?>(null)
    val currentSource: StateFlow<String?> = _currentSource.asStateFlow()

    fun fetchProxies(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentSource.value = url
            val result = fetcher.fetchProxies(url)
            _proxies.value = result
            _isLoading.value = false
        }
    }

    fun checkAllProxies() {
        viewModelScope.launch {
            val currentProxies = _proxies.value.map { it.copy(isChecking = true, status = com.example.data.ProxyStatus.UNTESTED) }
            _proxies.value = currentProxies

            currentProxies.forEachIndexed { index, proxy ->
                launch {
                    val latency = MTProtoChecker.checkProxy(proxy.server, proxy.port, proxy.secret)
                    val updatedStatus = if (latency != null) com.example.data.ProxyStatus.VALID else com.example.data.ProxyStatus.INVALID
                    
                    // Update only this proxy
                    _proxies.value = _proxies.value.toMutableList().apply {
                        this[index] = this[index].copy(
                            isChecking = false,
                            status = updatedStatus,
                            latencyMs = latency
                        )
                    }
                }
            }
        }
    }

    fun checkProxy(proxy: Proxy) {
        val index = _proxies.value.indexOf(proxy)
        if (index == -1) return

        viewModelScope.launch {
            _proxies.value = _proxies.value.toMutableList().apply {
                this[index] = this[index].copy(isChecking = true)
            }

            val latency = MTProtoChecker.checkProxy(proxy.server, proxy.port, proxy.secret)
            val updatedStatus = if (latency != null) com.example.data.ProxyStatus.VALID else com.example.data.ProxyStatus.INVALID

            _proxies.value = _proxies.value.toMutableList().apply {
                this[index] = this[index].copy(
                    isChecking = false,
                    status = updatedStatus,
                    latencyMs = latency
                )
            }
        }
    }
}
