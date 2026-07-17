package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object ProxyChecker {

    // Simulates a proxy connection check
    suspend fun checkProxy(proxy: com.example.data.Proxy): Long? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(proxy.server, proxy.port), 3000)
            socket.soTimeout = 3000

            when (proxy.type) {
                com.example.data.ProxyType.MTPROTO -> {
                    // MTProto Obfuscated2 Handshake Simulation
                    val random = java.security.SecureRandom()
                    val payload = ByteArray(64)
                    while (true) {
                        random.nextBytes(payload)
                        val val0 = payload[0].toInt()
                        if (val0 == 0xef) continue
                        
                        val str = String(payload.sliceArray(0..3), Charsets.US_ASCII)
                        if (str == "POST" || str == "GET " || str == "HEAD" || str == "HTTP") continue
                        if (payload.sliceArray(4..7).all { it == 0.toByte() }) continue
                        break
                    }

                    val out = socket.getOutputStream()
                    out.write(payload)
                    out.flush()
                    
                    try {
                        val input = socket.getInputStream()
                        socket.soTimeout = 500
                        input.read() 
                    } catch (e: java.net.SocketTimeoutException) {
                        // Timeout is good!
                    } catch (e: Exception) {
                        return@withContext null
                    }
                }
                com.example.data.ProxyType.SOCKS5 -> {
                    val out = socket.getOutputStream()
                    // SOCKS5 greeting (version 5, 1 auth method, no auth)
                    // If auth is required, we still send this to check if server responds with 0x05
                    out.write(byteArrayOf(0x05, 0x01, 0x00))
                    out.flush()
                    
                    try {
                        val input = socket.getInputStream()
                        val response = ByteArray(2)
                        var read = 0
                        while (read < 2) {
                            val count = input.read(response, read, 2 - read)
                            if (count == -1) break
                            read += count
                        }
                        if (read < 2 || response[0] != 0x05.toByte()) {
                            return@withContext null
                        }
                    } catch (e: Exception) {
                        return@withContext null
                    }
                }
                com.example.data.ProxyType.HTTP -> {
                    // Basic TCP connect is usually sufficient, but let's try a CONNECT request or basic ping
                    // For now, TCP connect success is considered good enough for a fast check.
                }
            }

            val latency = System.currentTimeMillis() - startTime
            return@withContext latency
        } catch (e: Exception) {
            return@withContext null
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {}
        }
    }
}
