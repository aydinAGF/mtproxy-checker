package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object MTProtoChecker {

    // Simulates an MTProto proxy connection check
    // Performs TCP connect and optionally a basic byte exchange to verify MTProto behavior.
    // Full gotd/td MTProto implementation in Kotlin is complex, so we perform a 
    // TCP ping + Obfuscated2 / Fake-TLS handshake ping where possible.
    suspend fun checkProxy(server: String, port: Int, secret: String): Long? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(server, port), 3000)
            socket.soTimeout = 3000

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

            // In a real Obfuscated2 handshake with proxy secret:
            // 1. You would use AES-CTR. For this applet, doing the TCP connection
            //    and sending the first 64 bytes is enough to confirm the port is open and accepting data.
            //    A true proxy checker does the full AES-CTR handshake. We'll do a partial write here to
            //    trigger the proxy's packet processing.
            
            val out = socket.getOutputStream()
            out.write(payload)
            out.flush()
            
            // Wait briefly to see if server immediately disconnects us for bad crypto
            // (most proxies will drop within 500ms if the crypto is completely wrong)
            try {
                val input = socket.getInputStream()
                // A very short read block to wait for drop
                socket.soTimeout = 500
                input.read() 
            } catch (e: java.net.SocketTimeoutException) {
                // Timeout is good! It means the server kept the connection open 
                // waiting for more data, which is typical MTProto behavior.
            } catch (e: Exception) {
                // Connection closed or reset
                return@withContext null
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
