package com.example.data

data class Proxy(
    val server: String,
    val port: Int,
    val secret: String,
    val sourceUrl: String? = null,
    var isChecking: Boolean = false,
    var status: ProxyStatus = ProxyStatus.UNTESTED,
    var latencyMs: Long? = null
)

enum class ProxyStatus {
    UNTESTED,
    VALID,
    INVALID,
    TIMEOUT
}
