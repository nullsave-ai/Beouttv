package com.beouttv.app.data.source.stream

import android.util.Base64
import com.beouttv.app.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ResolvedGatewayStream(
    val streamUrl: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val drmKeyId: String? = null,
    val drmKey: String? = null
)

/**
 * معالج بوابة البث الحي المشفرة والمموهة (Live Gateway Edge Stream Resolver):
 * 1. التعرف التلقائي على روابط وقنوات البوابة الحية وعناوين الـ API.
 * 2. الاتصال المشفر بالترويسات الرسمية (okhttp/4.12.0) ودعم حل النطاقات عبر DoH لتجاوز حجب مزودي الخدمة بالعراق.
 * 3. فك تشفير الحمولات وحساب مفاتيح الوقت T واستخراج تيار الفيديو والـ DRM تلقائياً.
 */
object LiveGatewayStreamResolver {

    private const val GATEWAY_STATIC_KEY = "c!xZj+N9&G@Ev@vw"
    private const val PRIMARY_GATEWAY_HOST = "https://deft.yacinelive.com/api/channel/"
    private const val FALLBACK_GATEWAY_HOST = "https://def.yacinelive.com/api/channel/"
    private const val DEFAULT_GATEWAY_HOST = PRIMARY_GATEWAY_HOST

    private const val PRIMARY_CATEGORIES_URL = "https://deft.yacinelive.com/api/categories/%d/channels"
    private const val FALLBACK_CATEGORIES_URL = "https://def.yacinelive.com/api/categories/%d/channels"

    private val dohDnsCache = ConcurrentHashMap<String, List<InetAddress>>()

    /**
     * نظام حل أسماء النطاقات المعزز (Multi-Tier DoH & Resilient DNS):
     * 1. فحص الكاش الداخلي ونظام الـ DNS المحلي بالجهاز.
     * 2. الاستعلام عبر Google DoH (https://dns.google/resolve) المفتوح بالكامل في العراق ومطابق لـ RFC 8484.
     * 3. التراجع الاحتياطي إلى Cloudflare DoH (cloudflare-dns.com).
     * 4. شبكة الأمان النهائية: عناوين IP الثابتة لسيرفرات ياسين (181.215.178.128 / 185.135.157.115) لمنع أي فشل اتصالي.
     */
    private val secureDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            dohDnsCache[hostname]?.let { return it }

            try {
                val sysAddresses = Dns.SYSTEM.lookup(hostname)
                if (sysAddresses.isNotEmpty()) {
                    dohDnsCache[hostname] = sysAddresses
                    return sysAddresses
                }
            } catch (_: UnknownHostException) {
            }

            val dohEndpoints = listOf(
                "https://dns.google/resolve?name=$hostname&type=A",
                "https://cloudflare-dns.com/dns-query?name=$hostname&type=A"
            )

            val rawClient = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()

            for (dohUrl in dohEndpoints) {
                try {
                    val dohReq = Request.Builder()
                        .url(dohUrl)
                        .header("Accept", "application/dns-json")
                        .build()

                    rawClient.newCall(dohReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrBlank()) {
                                val json = JSONObject(body)
                                val answers = json.optJSONArray("Answer")
                                if (answers != null && answers.length() > 0) {
                                    val addresses = mutableListOf<InetAddress>()
                                    for (i in 0 until answers.length()) {
                                        val item = answers.getJSONObject(i)
                                        if (item.optInt("type") == 1) {
                                            val ip = item.optString("data").trim()
                                            if (ip.isNotEmpty()) {
                                                addresses.add(InetAddress.getByName(ip))
                                            }
                                        }
                                    }
                                    if (addresses.isNotEmpty()) {
                                        dohDnsCache[hostname] = addresses
                                        return addresses
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Fallback for Yacine Live API server hosts if DNS is completely blocked
            if (hostname.contains("yacinelive.com") || hostname.contains("yacine")) {
                try {
                    val fallbackIps = listOf(
                        InetAddress.getByName("181.215.178.128"),
                        InetAddress.getByName("185.135.157.115")
                    )
                    dohDnsCache[hostname] = fallbackIps
                    return fallbackIps
                } catch (_: Exception) {}
            }

            throw UnknownHostException("Unable to resolve host: $hostname")
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(secureDns)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * كشف ما إذا كان الرابط يتبع للبوابة الحية المشفرة (Yacine TV)
     */
    fun isTargetApiUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase().trim()
        return lower.contains("/api/channel/") ||
                lower.contains("yacinelive.com") ||
                lower.startsWith("yacine://") ||
                lower.startsWith("gw://") ||
                lower.contains("deft.yacine") ||
                lower.contains("def.yacine") ||
                (lower.startsWith("channel://"))
    }

    /**
     * فك تشفير واستخراج رابط البث والترويسات ومفاتيح DRM من البوابة الحية (Yacine TV)
     */
    suspend fun resolve(apiUrl: String): ResolvedGatewayStream? = withContext(Dispatchers.IO) {
        val cleanUrl = apiUrl.trim()
        val channelId = when {
            cleanUrl.startsWith("yacine://") -> cleanUrl.removePrefix("yacine://").trim()
            cleanUrl.startsWith("channel://") -> cleanUrl.removePrefix("channel://").trim()
            cleanUrl.startsWith("gw://") -> cleanUrl.removePrefix("gw://").trim()
            cleanUrl.all { it.isDigit() } -> cleanUrl
            cleanUrl.contains("/api/channel/") -> cleanUrl.substringAfter("/api/channel/").substringBefore("?").trim()
            else -> ""
        }

        val candidateUrls = if (channelId.isNotBlank()) {
            listOf(
                "$PRIMARY_GATEWAY_HOST$channelId",
                "$FALLBACK_GATEWAY_HOST$channelId"
            )
        } else {
            listOf(
                cleanUrl.replace("http://def.yacinelive.com", "https://deft.yacinelive.com")
                    .replace("http://deft.yacinelive.com", "https://deft.yacinelive.com"),
                cleanUrl.replace("https://deft.yacinelive.com", "https://def.yacinelive.com")
            )
        }

        for (targetUrl in candidateUrls) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "okhttp/4.12.0")
                    .header("Accept", "application/json")
                    .header("Accept-Encoding", "identity")
                    .header("Connection", "Keep-Alive")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    continue
                }

                val bodyStr = response.body?.string()?.trim() ?: continue
                val timestampHeader = response.header("T").orEmpty()
                val rawBytes = Base64.decode(bodyStr, Base64.DEFAULT)

                val fullKey = (GATEWAY_STATIC_KEY + timestampHeader).toByteArray(Charsets.UTF_8)
                val decryptedBytes = ByteArray(rawBytes.size) { i ->
                    (rawBytes[i].toInt() xor fullKey[i % fullKey.size].toInt()).toByte()
                }

                val decryptedJson = String(decryptedBytes, Charsets.UTF_8)
                val jsonRoot = JSONObject(decryptedJson)
                val dataArr = jsonRoot.optJSONArray("data") ?: continue
                if (dataArr.length() == 0) continue

                // Find the best stream: prefer direct M3U8/HLS or DASH streams over web embeds
                var chosenObj: JSONObject? = null
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.optJSONObject(i) ?: continue
                    val url = obj.optString("url", "").trim()
                    val urlType = obj.optInt("url_type", 0)
                    if (url.isBlank()) continue

                    val isWebEmbed = url.contains(".html", ignoreCase = true) ||
                            url.contains("embed.php", ignoreCase = true) ||
                            url.contains("arab-stream.live/p/", ignoreCase = true)

                    if (!isWebEmbed && (urlType == 1 || urlType == 3 || url.contains(".m3u8", ignoreCase = true) || url.contains(".mpd", ignoreCase = true))) {
                        chosenObj = obj
                        break
                    }
                }
                if (chosenObj == null) {
                    for (i in 0 until dataArr.length()) {
                        val obj = dataArr.optJSONObject(i) ?: continue
                        if (obj.optString("url", "").trim().isNotBlank()) {
                            chosenObj = obj
                            break
                        }
                    }
                }

                if (chosenObj == null) continue

                val streamUrl = chosenObj.optString("url", "").trim()
                if (streamUrl.isBlank()) continue

                val headersObj = chosenObj.optJSONObject("headers")
                val apiUa = headersObj?.optString("User-Agent")?.ifBlank { null }
                    ?: chosenObj.optString("user_agent").ifBlank { null }
                val apiRef = headersObj?.optString("Referer")?.ifBlank { null }
                    ?: chosenObj.optString("referer").ifBlank { null }

                val streamLower = streamUrl.lowercase()
                val effectiveReferer = when {
                    streamLower.contains("edgenextcdn.net") || streamLower.contains("shahid") || streamLower.contains("mbc") -> "https://shahid.mbc.net/"
                    streamLower.contains("catiacast.video") || streamLower.contains("imn.iq") -> "https://imn.iq/"
                    streamLower.contains("alrabiaa") -> "https://www.alrabiaa.tv/"
                    streamLower.contains("ycncdn.online") -> "https://studio.x.com/"
                    !apiRef.isNullOrBlank() && apiRef != "https://x.com/" -> apiRef
                    else -> "https://studio.x.com/"
                }

                val effectiveUserAgent = when {
                    !apiUa.isNullOrBlank() && !apiUa.contains("ExoPlayer", ignoreCase = true) -> apiUa
                    else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
                }

                var drmKeyId: String? = null
                var drmKey: String? = null
                val drmObj = chosenObj.optJSONObject("drm")
                if (drmObj != null) {
                    drmKeyId = drmObj.optString("key_id").ifBlank { null }
                    drmKey = drmObj.optString("key").ifBlank { null }
                }

                return@withContext ResolvedGatewayStream(
                    streamUrl = streamUrl,
                    userAgent = effectiveUserAgent,
                    referer = effectiveReferer,
                    drmKeyId = drmKeyId,
                    drmKey = drmKey
                )
            } catch (_: Exception) {
                // Try next candidate URL
            }
        }
        null
    }

    /**
     * جلب قنوات البوابة الحية (Yacine Live Catalog) لباقات الترفيه والأطفال
     * وإدراجها في قائمة قنوات التطبيق بضمان 100%.
     */
    suspend fun fetchGatewayChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Channel>()
        val categories = listOf(
            8 to "باقة بي ان الترفيهية",
            13 to "قنوات الأطفال والوثائقية"
        )

        try {
            coroutineScope {
                val deferreds = categories.map { (catId, catName) ->
                    async {
                        val groupList = mutableListOf<Channel>()
                        val candidateCatUrls = listOf(
                            String.format(PRIMARY_CATEGORIES_URL, catId),
                            String.format(FALLBACK_CATEGORIES_URL, catId)
                        )

                        for (url in candidateCatUrls) {
                            try {
                                val request = Request.Builder()
                                    .url(url)
                                    .header("User-Agent", "okhttp/4.12.0")
                                    .header("Accept", "application/json")
                                    .header("Accept-Encoding", "identity")
                                    .build()

                                val resp = httpClient.newCall(request).execute()
                                if (!resp.isSuccessful) {
                                    resp.close()
                                    continue
                                }

                                val body = resp.body?.string() ?: continue
                                val tHeader = resp.header("T").orEmpty()
                                val raw = Base64.decode(body, Base64.DEFAULT)
                                val key = (GATEWAY_STATIC_KEY + tHeader).toByteArray(Charsets.UTF_8)
                                val dec = ByteArray(raw.size) { i ->
                                    (raw[i].toInt() xor key[i % key.size].toInt()).toByte()
                                }
                                val json = JSONObject(String(dec, Charsets.UTF_8))
                                val dataArr = json.optJSONArray("data") ?: continue
                                for (i in 0 until dataArr.length()) {
                                    val item = dataArr.optJSONObject(i) ?: continue
                                    val id = item.optString("id", "").trim()
                                    val name = item.optString("name", "").trim()
                                    val logo = item.optString("logo", "").trim()
                                    if (id.isEmpty() || name.isEmpty()) continue
                                    if (catId == 8 && name.contains("BOX OFFICE", ignoreCase = true)) continue

                                    groupList.add(
                                        Channel(
                                            id = "gw_${catId}_$id",
                                            name = name,
                                            logoUrl = logo.ifBlank { "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572" },
                                            category = catName,
                                            isLive = true,
                                            currentProgram = "بث حي مباشر",
                                            streamUrl = "$PRIMARY_GATEWAY_HOST$id",
                                            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                                            referer = "https://studio.x.com/"
                                        )
                                    )
                                }
                                if (groupList.isNotEmpty()) break
                            } catch (_: Exception) {}
                        }
                        groupList
                    }
                }
                val seenNames = mutableSetOf<String>()
                deferreds.awaitAll().forEach { groupChannels ->
                    for (ch in groupChannels) {
                        val key = "${ch.category}|${ch.name.lowercase().replace(" ", "")}"
                        if (!seenNames.contains(key)) {
                            seenNames.add(key)
                            list.add(ch)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }
}
