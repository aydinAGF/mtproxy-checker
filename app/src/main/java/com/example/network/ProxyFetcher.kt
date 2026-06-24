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

    private fun parseProxyLine(line: String, sourceUrl: String): Proxy? {
        if (line.isEmpty() || (!line.startsWith("tg://") && !line.startsWith("https://t.me/proxy"))) return null

        try {
            val queryParamsStart = line.indexOf('?')
            if (queryParamsStart == -1) return null

            val query = line.substring(queryParamsStart + 1)
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
                return Proxy(server, port, secret, sourceUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
