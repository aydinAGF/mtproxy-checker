package com.example.network

import android.content.Context
import android.net.Uri
import com.example.data.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder

class ProxyFetcher(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun fetchProxies(url: String): List<Proxy> = withContext(Dispatchers.IO) {
        val proxies = mutableListOf<Proxy>()
        try {
            if (url.startsWith("content://")) {
                val uri = Uri.parse(url)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.forEachLine { line ->
                            val proxy = parseProxyLine(line.trim(), url)
                            if (proxy != null) {
                                proxies.add(proxy)
                            }
                        }
                    }
                }
            } else {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.lines()?.forEach { line ->
                        val proxy = parseProxyLine(line.trim(), url)
                        if (proxy != null) {
                            proxies.add(proxy)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        proxies
    }

    suspend fun parseProxiesFromString(content: String, sourceUrl: String): List<Proxy> = withContext(Dispatchers.Default) {
        val proxies = mutableListOf<Proxy>()
        content.lines().forEach { line ->
            val proxy = parseProxyLine(line.trim(), sourceUrl)
            if (proxy != null) {
                proxies.add(proxy)
            }
        }
        proxies
    }

    private fun parseProxyLine(line: String, sourceUrl: String): Proxy? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        try {
            if (trimmed.startsWith("tg://proxy") || trimmed.startsWith("https://t.me/proxy")) {
                val queryParamsStart = trimmed.indexOf('?')
                if (queryParamsStart == -1) return null

                val query = trimmed.substring(queryParamsStart + 1)
                val params = query.split("&")
                
                var server: String? = null
                var port: Int? = null
                var secret: String? = null

                for (param in params) {
                    val pair = param.split("=")
                    if (pair.size == 2) {
                        val key = URLDecoder.decode(pair[0], "UTF-8")
                        val value = URLDecoder.decode(pair[1], "UTF-8")
                        when (key) {
                            "server" -> server = value
                            "port" -> port = value.toIntOrNull()
                            "secret" -> secret = value
                        }
                    }
                }

                if (server != null && port != null && secret != null) {
                    return Proxy(server, port, com.example.data.ProxyType.MTPROTO, secret = secret, sourceUrl = sourceUrl)
                }
            } else if (trimmed.startsWith("tg://socks") || trimmed.startsWith("https://t.me/socks")) {
                val queryParamsStart = trimmed.indexOf('?')
                if (queryParamsStart == -1) return null

                val query = trimmed.substring(queryParamsStart + 1)
                val params = query.split("&")
                
                var server: String? = null
                var port: Int? = null
                var user: String? = null
                var pass: String? = null

                for (param in params) {
                    val pair = param.split("=")
                    if (pair.size == 2) {
                        val key = URLDecoder.decode(pair[0], "UTF-8")
                        val value = URLDecoder.decode(pair[1], "UTF-8")
                        when (key) {
                            "server" -> server = value
                            "port" -> port = value.toIntOrNull()
                            "user" -> user = value
                            "pass" -> pass = value
                        }
                    }
                }

                if (server != null && port != null) {
                    return Proxy(server, port, com.example.data.ProxyType.SOCKS5, username = user, password = pass, sourceUrl = sourceUrl)
                }
            } else if (trimmed.startsWith("socks5://")) {
                val withoutScheme = trimmed.substring("socks5://".length)
                return parseStandardUrlFormat(withoutScheme, com.example.data.ProxyType.SOCKS5, sourceUrl)
            } else if (trimmed.startsWith("http://")) {
                val withoutScheme = trimmed.substring("http://".length)
                return parseStandardUrlFormat(withoutScheme, com.example.data.ProxyType.HTTP, sourceUrl)
            } else if (trimmed.startsWith("https://")) {
                val withoutScheme = trimmed.substring("https://".length)
                return parseStandardUrlFormat(withoutScheme, com.example.data.ProxyType.HTTP, sourceUrl)
            } else {
                val parts = trimmed.split(":")
                if (parts.size == 2) {
                    val server = parts[0]
                    val port = parts[1].toIntOrNull()
                    if (port != null) {
                        return Proxy(server, port, com.example.data.ProxyType.SOCKS5, sourceUrl = sourceUrl)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseStandardUrlFormat(withoutScheme: String, type: com.example.data.ProxyType, sourceUrl: String): Proxy? {
        var server: String? = null
        var port: Int? = null
        var user: String? = null
        var pass: String? = null

        val atIndex = withoutScheme.indexOf('@')
        var hostPortPart = withoutScheme
        if (atIndex != -1) {
            val credentialsPart = withoutScheme.substring(0, atIndex)
            hostPortPart = withoutScheme.substring(atIndex + 1)

            val credParts = credentialsPart.split(":")
            if (credParts.size == 2) {
                user = URLDecoder.decode(credParts[0], "UTF-8")
                pass = URLDecoder.decode(credParts[1], "UTF-8")
            } else if (credParts.size == 1) {
                user = URLDecoder.decode(credParts[0], "UTF-8")
            }
        }

        val hpParts = hostPortPart.split(":")
        if (hpParts.size == 2) {
            server = hpParts[0]
            port = hpParts[1].substringBefore('/').toIntOrNull()
        }

        if (server != null && port != null) {
            return Proxy(server, port, type, username = user, password = pass, sourceUrl = sourceUrl)
        }
        return null
    }
}
