package com.example.data

data class Proxy(
    val server: String,
    val port: Int,
    val type: ProxyType,
    val secret: String? = null,
    val username: String? = null,
    val password: String? = null,
    val sourceUrl: String? = null,
    var isChecking: Boolean = false,
    var status: ProxyStatus = ProxyStatus.UNTESTED,
    var latencyMs: Long? = null
)

enum class ProxyType {
    MTPROTO,
    SOCKS5,
    HTTP
}

enum class ProxyStatus {
    UNTESTED,
    VALID,
    INVALID,
    TIMEOUT
}
