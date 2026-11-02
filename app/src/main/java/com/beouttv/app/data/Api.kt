package com.beouttv.app.data

import com.beouttv.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HttpException(val code: Int) : IOException("HTTP $code")

/** عميل شبكة خفيف: HttpURLConnection + ETag + دمج الطلبات المتطابقة + إلغاء فعلي للاتصال. */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object Api {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    @Volatile var deviceId: String = ""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inflight = HashMap<String, Deferred<String>>()
    private val etags = HashMap<String, Pair<String, String>>()

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    suspend fun get(path: String, dedupe: Boolean = true): String {
        if (!dedupe) return fetch(path)
        val d = synchronized(inflight) {
            inflight.getOrPut(path) {
                scope.async {
                    try { fetch(path) } finally { synchronized(inflight) { inflight.remove(path) } }
                }
            }
        }
        return d.await()
    }

    suspend inline fun <reified T> getJson(path: String, dedupe: Boolean = true): T =
        json.decodeFromString<T>(get(path, dedupe))

    private suspend fun fetch(path: String): String = withContext(Dispatchers.IO) {
        val targetUrl = if (path.startsWith("http://") || path.startsWith("https://")) path
        else BuildConfig.API_BASE.trimEnd('/') + "/" + path.trimStart('/')
        val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) BeoutTV/2.0")
            setRequestProperty("X-Device", deviceId)
            synchronized(etags) { etags[path] }?.let { setRequestProperty("If-None-Match", it.first) }
        }
        val h = currentCoroutineContext()[Job]?.invokeOnCompletion { conn.disconnect() }
        try {
            when (val code = conn.responseCode) {
                304 -> synchronized(etags) { etags[path]?.second } ?: throw HttpException(code)
                in 200..299 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.getHeaderField("ETag")?.let { synchronized(etags) { etags[path] = it to body } }
                    body
                }
                else -> throw HttpException(code)
            }
        } finally {
            h?.dispose()
            conn.disconnect()
        }
    }

    /** إرسال بدون انتظار نتيجة (مزامنة السجل والمفضلة). */
    fun post(path: String, body: String) {
        scope.async {
            runCatching {
                val targetUrl = if (path.startsWith("http://") || path.startsWith("https://")) path
                else BuildConfig.API_BASE.trimEnd('/') + "/" + path.trimStart('/')
                val c = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true
                    connectTimeout = 8_000; readTimeout = 8_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Device", deviceId)
                }
                c.outputStream.use { it.write(body.toByteArray()) }
                c.responseCode
                c.disconnect()
            }
        }
    }

    fun clearCache() = synchronized(etags) { etags.clear() }
}
