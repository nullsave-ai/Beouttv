package com.beouttv.app.data.source.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class OkRuVideoDetails(
    val stream: ResolvedGatewayStream,
    val title: String? = null,
    val posterUrl: String? = null,
    val isLive: Boolean = false
)

/**
 * معالج وسائط وبث منصة OK.ru المدمج في التطبيق:
 * 1. استخراج معرّف الفيديو (Video ID) من جميع أشكال روابط ok.ru و odnoklassniki و okru://.
 * 2. جلب صفحة التضمين الخفيفة (videoembed) بأقل من 35 كيلوبايت وبسرعة فائقة.
 * 3. فك ترميز معلمات المشغل واستخراج تيار HLS الحي الموقّع بـ IP جهاز العميل محلياً لتجاوز خطأ 403 Forbidden.
 * 4. تزويد ExoPlayer بالترويسات الأصلية (Referer: https://ok.ru/ و User-Agent الحديث) لضمان التشغيل المستمر.
 */
object OkRuStreamResolver {

    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    const val DEFAULT_REFERER = "https://ok.ru/"

    private val dohDnsCache = ConcurrentHashMap<String, List<InetAddress>>()

    /**
     * نظام DoH المقاوم للحجب لضمان وصول المشغل لخوادم ok.ru و okcdn.ru حتى لو حجبها مزود الخدمة المحلي.
     */
    private val resilientDns = object : Dns {
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
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
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

            throw UnknownHostException("Unable to resolve host: $hostname")
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(resilientDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val VIDEO_ID_REGEX = Pattern.compile(
        """(?:ok\.ru|odnoklassniki\.ru)/(?:video|videoembed|live)/(\d+)""",
        Pattern.CASE_INSENSITIVE
    )
    private val QUERY_ID_REGEX = Pattern.compile(
        """(?:mid|videoId|id)=(\d+)|/mid/(\d+)""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * فحص ما إذا كان الرابط يتبع لشبكة OK.ru
     */
    fun isOkRuUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase().trim()
        return lower.contains("ok.ru/") ||
                lower.contains("odnoklassniki.ru/") ||
                lower.contains("okcdn.ru/") ||
                lower.startsWith("okru://") ||
                lower.startsWith("ok://")
    }

    /**
     * استخراج رقم الفيديو Video ID
     */
    fun extractVideoId(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()

        if (trimmed.all { it.isDigit() }) {
            return trimmed
        }

        if (trimmed.startsWith("okru://", ignoreCase = true)) {
            val part = trimmed.substring("okru://".length).trim()
            val id = part.substringBefore("?").substringBefore("/").substringBefore("#").trim()
            if (id.all { it.isDigit() } && id.isNotEmpty()) return id
        }

        if (trimmed.startsWith("ok://", ignoreCase = true)) {
            val part = trimmed.substring("ok://".length).trim()
            val id = part.substringBefore("?").substringBefore("/").substringBefore("#").trim()
            if (id.all { it.isDigit() } && id.isNotEmpty()) return id
        }

        val matcher = VIDEO_ID_REGEX.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1)
        }

        val queryMatcher = QUERY_ID_REGEX.matcher(trimmed)
        if (queryMatcher.find()) {
            return queryMatcher.group(1) ?: queryMatcher.group(2)
        }

        if (trimmed.startsWith("ok_", ignoreCase = true) || trimmed.startsWith("okru_", ignoreCase = true)) {
            val id = trimmed.substringAfter("_").trim()
            if (id.all { it.isDigit() } && id.isNotEmpty()) return id
        }

        return null
    }

    /**
     * حل رابط OK.ru وتحويله إلى تدفق HLS مباشر متوافق مع مشغل ExoPlayer
     */
    suspend fun resolve(rawUrl: String): ResolvedGatewayStream? = withContext(Dispatchers.IO) {
        resolveDetails(rawUrl)?.stream
    }

    /**
     * حل تفاصيل الفيديو الكاملة (تدفق HLS + العنوان + الغلاف)
     */
    suspend fun resolveDetails(rawUrl: String): OkRuVideoDetails? = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        val videoId = extractVideoId(cleanUrl)

        val candidateUrls = if (!videoId.isNullOrBlank()) {
            listOf(
                "https://ok.ru/videoembed/$videoId",
                "https://ok.ru/video/$videoId"
            )
        } else if (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true)) {
            listOf(cleanUrl)
        } else {
            emptyList()
        }

        for (targetUrl in candidateUrls) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    continue
                }

                val bodyStr = response.body?.string() ?: continue
                val marker = "data-options=\""
                val markerIdx = bodyStr.indexOf(marker)

                if (markerIdx != -1) {
                    val start = markerIdx + marker.length
                    val end = bodyStr.indexOf("\"", start)
                    if (end != -1) {
                        val rawEscapedJson = bodyStr.substring(start, end)
                        val unescapedJson = unescapeHtml(rawEscapedJson)
                        val rootJson = JSONObject(unescapedJson)
                        val flashvars = rootJson.optJSONObject("flashvars")

                        val metadataObj = flashvars?.opt("metadata")
                        val metaJson: JSONObject? = when (metadataObj) {
                            is JSONObject -> metadataObj
                            is String -> try { JSONObject(metadataObj) } catch (_: Exception) { null }
                            else -> null
                        }

                        if (metaJson != null) {
                            val hlsMaster = metaJson.optString("hlsMasterPlaylistUrl").ifBlank { null }
                            val hlsPlayback = metaJson.optString("hlsPlaybackMasterPlaylistUrl").ifBlank { null }
                            val hlsLive = metaJson.optString("hlsLiveUrl").ifBlank { null }

                            var chosenStreamUrl = hlsMaster ?: hlsPlayback ?: hlsLive

                            if (chosenStreamUrl.isNullOrBlank()) {
                                val videosArr = metaJson.optJSONArray("videos")
                                if (videosArr != null && videosArr.length() > 0) {
                                    val qualityPreference = listOf("ultra", "quad", "full", "hd", "sd", "low", "lowest", "mobile")
                                    val videoList = mutableListOf<Pair<String, String>>()
                                    for (i in 0 until videosArr.length()) {
                                        val vObj = videosArr.optJSONObject(i) ?: continue
                                        val vName = vObj.optString("name", "").lowercase()
                                        val vUrl = vObj.optString("url", "").trim()
                                        if (vUrl.isNotBlank()) {
                                            videoList.add(vName to vUrl)
                                        }
                                    }
                                    for (q in qualityPreference) {
                                        val match = videoList.firstOrNull { it.first == q }
                                        if (match != null) {
                                            chosenStreamUrl = match.second
                                            break
                                        }
                                    }
                                    if (chosenStreamUrl.isNullOrBlank() && videoList.isNotEmpty()) {
                                        chosenStreamUrl = videoList.first().second
                                    }
                                }
                            }

                            if (!chosenStreamUrl.isNullOrBlank()) {
                                val movieObj = metaJson.optJSONObject("movie")
                                val videoTitle = movieObj?.optString("title")?.ifBlank { null }
                                val posterUrl = movieObj?.optString("poster")?.ifBlank { null }
                                val isLive = metaJson.optBoolean("isLive", false) ||
                                        chosenStreamUrl.contains(".m3u8") ||
                                        bodyStr.contains("vid-card_live")

                                return@withContext OkRuVideoDetails(
                                    stream = ResolvedGatewayStream(
                                        streamUrl = chosenStreamUrl,
                                        userAgent = DEFAULT_USER_AGENT,
                                        referer = DEFAULT_REFERER
                                    ),
                                    title = videoTitle,
                                    posterUrl = posterUrl,
                                    isLive = isLive
                                )
                            }
                        }
                    }
                }

                // Fallback: direct regex search for .m3u8 URL in the page
                val hlsRegex = Pattern.compile("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
                val hlsMatcher = hlsRegex.matcher(bodyStr)
                if (hlsMatcher.find()) {
                    val rawFound = hlsMatcher.group(0)
                    val unescapedUrl = unescapeHtml(rawFound)
                    return@withContext OkRuVideoDetails(
                        stream = ResolvedGatewayStream(
                            streamUrl = unescapedUrl,
                            userAgent = DEFAULT_USER_AGENT,
                            referer = DEFAULT_REFERER
                        ),
                        isLive = true
                    )
                }
            } catch (_: Exception) {
                // Try next candidate URL
            }
        }

        null
    }

    private fun unescapeHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&#34;", "\"")
            .replace("&amp;", "&")
            .replace("&#38;", "&")
            .replace("&lt;", "<")
            .replace("&#60;", "<")
            .replace("&gt;", ">")
            .replace("&#62;", ">")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }
}
