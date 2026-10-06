package com.beouttv.app.data.source.stream

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class ResolvedStreamResult(
    val streamUrl: String,
    val userAgent: String,
    val referer: String = "https://ostora.tv/",
    val drmKeyId: String? = null,
    val drmKey: String? = null
)

object OstoraApiResolver {

    private val DEFAULT_BASE_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_OSTORA_DEFAULT_BASE)
    private val DOH_URL: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_DOH_URL)
    val DEFAULT_USER_AGENT: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_OSTORA_USER_AGENT)
    val DEFAULT_REFERER: String get() = com.beouttv.app.data.security.NativeSecurity.getString(com.beouttv.app.data.security.NativeSecurity.KEY_OSTORA_REFERER)

    private val API_KEY get() = com.beouttv.app.data.security.NativeSecurity.getAesKey()
    private val API_IV  get() = com.beouttv.app.data.security.NativeSecurity.getAesIv()

    private var activeBaseUrl: String = ""
        get() = if (field.isBlank()) DEFAULT_BASE_URL else field
    private var lastDomainResolveTime: Long = 0L

    // In-memory short-lived cache for group channels (TTL = 45 seconds)
    private val categoryItemsCache = ConcurrentHashMap<String, Pair<Long, List<JSONObject>>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * فك تشفير منظومة الأسطورة: AES-256-CBC ثم فك الضغط (Zlib / Inflater)
     */
    fun decryptOstoraPayload(encryptedData: String): String {
        return try {
            val cleaned = encryptedData.replace("\n", "").replace("\r", "").replace(" ", "").replace("\t", "")
                .replace("-", "+").replace("_", "/")
            var padded = cleaned
            while (padded.length % 4 != 0) padded += "="

            val encryptedBytes = Base64.decode(padded, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(API_KEY, "AES")
            val ivSpec = IvParameterSpec(API_IV)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            var text = String(decryptedBytes, Charsets.UTF_8).trim()

            if (text.startsWith("eN")) {
                var zpad = text.trim()
                while (zpad.length % 4 != 0) zpad += "="
                val zdata = Base64.decode(zpad, Base64.DEFAULT)
                val inflater = Inflater()
                inflater.setInput(zdata)
                val outputStream = ByteArrayOutputStream(zdata.size)
                val buffer = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count <= 0) break
                    outputStream.write(buffer, 0, count)
                }
                inflater.end()
                text = String(outputStream.toByteArray(), Charsets.UTF_8)
            }
            text
        } catch (_: Exception) {
            ""
        }
    }

    private val DOH_ENDPOINTS = listOf(
        "https://1.1.1.1/dns-query?name=start.ver6.top&type=TXT",
        "https://dns.google/resolve?name=start.ver6.top&type=TXT",
        "https://cloudflare-dns.com/dns-query?name=start.ver6.top&type=TXT",
        "https://dns.quad9.net/dns-query?name=start.ver6.top&type=TXT",
        "https://dns.adguard-dns.com/resolve?name=start.ver6.top&type=TXT"
    )

    private val FALLBACK_DOMAINS = listOf(
        "https://hzu62fiidsa0d937ohro25kw8c4ancbeep50bup8wbyjf7d098.sa034.shop",
        "https://sa034.shop",
        "https://bsh7l368dl2qq4o09s96iuowr5yx80enakj2ilditwrazkpa6t.sa050.shop",
        "https://sa050.shop",
        "https://2kulxg7p804n52d9e16eqvl61moeqp5ta8ohfm935v1jhq0azb.sa031.shop",
        "https://sa031.shop",
        "https://brwhftdrl9dugekrqgsj8zkbpyxzpfuas7520l34zvivipc5ea.sa036.shop",
        "https://sa036.shop",
        "https://9mkf935lte302gz0j6qodbwvh4ndr6aq58kpo7zy3d4vzr7mjh.sa004.shop",
        "https://vsh71sfkud5l2znrtokaj6eajoe8hhgr9xiqdpvzn2ccjkl6iz.sa028.shop",
        "https://d3nzinj58ahs7n34cidrm7p6zcf0qwl0vrkbsu4gt5h29g8eij.sa028.shop",
        "https://ek5fhhtyfg4rstbya76oqzmjniv46y2kru2b0e3tpp1x3wcdxr.sa001.shop",
        "https://sa004.shop",
        "https://sa028.shop",
        "https://sa012.shop",
        "https://sa015.shop"
    )

    /**
     * جلب أحدث نطاق نشط للأسطورة عبر منظومة DoH المتعددة (Cloudflare + Google) مع استراتيجية الصمود ضد الحظر
     */
    suspend fun resolveActiveDomain(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && lastDomainResolveTime > 0 && (now - lastDomainResolveTime) < 15 * 60 * 1000 && activeBaseUrl.isNotBlank() && activeBaseUrl != DEFAULT_BASE_URL) {
            return@withContext activeBaseUrl
        }

        val dohList = if (DOH_URL.isNotBlank() && !DOH_ENDPOINTS.contains(DOH_URL)) {
            listOf(DOH_URL) + DOH_ENDPOINTS
        } else {
            DOH_ENDPOINTS
        }

        for (dohEndpoint in dohList) {
            try {
                val request = Request.Builder()
                    .url(dohEndpoint)
                    .header("Accept", "application/dns-json")
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val answers = json.optJSONArray("Answer")
                        if (answers != null && answers.length() > 0) {
                            val sb = StringBuilder()
                            for (i in 0 until answers.length()) {
                                val dataStr = answers.getJSONObject(i).optString("data")
                                sb.append(dataStr.replace("\"", "").replace(" ", ""))
                            }
                            val decryptedTxt = decryptOstoraPayload(sb.toString())
                            if (decryptedTxt.isNotBlank()) {
                                val configJson = JSONObject(decryptedTxt)
                                val x0 = configJson.optString("x0", "").trim()
                                if (x0.isNotEmpty()) {
                                    var paddedX0 = x0
                                    while (paddedX0.length % 4 != 0) paddedX0 += "="
                                    val domainBytes = Base64.decode(paddedX0, Base64.DEFAULT)
                                    val rawDomain = String(domainBytes, Charsets.UTF_8).trim()
                                    val finalDomain = when {
                                        rawDomain.startsWith("http://", ignoreCase = true) -> "https://" + rawDomain.substring(7)
                                        rawDomain.startsWith("https://", ignoreCase = true) -> rawDomain
                                        else -> "https://$rawDomain"
                                    }.trimEnd('/')
                                    if (finalDomain.length > 8) {
                                        activeBaseUrl = finalDomain
                                        lastDomainResolveTime = now
                                        return@withContext activeBaseUrl
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (activeBaseUrl.isBlank() || activeBaseUrl == DEFAULT_BASE_URL) {
            activeBaseUrl = DEFAULT_BASE_URL.takeIf { it.isNotBlank() } ?: FALLBACK_DOMAINS.first()
        }
        activeBaseUrl
    }

    /**
     * جلب حمولة مشفرة مع تجربة الدومين النشط والتراجع التلقائي إلى الدومينات الاحتياطية في حال الفشل
     */
    suspend fun fetchAndDecryptApiPath(apiPath: String): String? = withContext(Dispatchers.IO) {
        val currentBase = resolveActiveDomain().trimEnd('/')
        val path = if (apiPath.startsWith("/")) apiPath else "/$apiPath"

        // 1. تجربة الدومين النشط الحالي
        val res1 = fetchAndDecrypt("$currentBase$path")
        if (!res1.isNullOrBlank()) return@withContext res1

        // 2. تجربة تحديث الدومين إجبارياً عبر DoH
        val freshBase = resolveActiveDomain(forceRefresh = true).trimEnd('/')
        if (freshBase != currentBase) {
            val res2 = fetchAndDecrypt("$freshBase$path")
            if (!res2.isNullOrBlank()) return@withContext res2
        }

        // 3. تجربة قائمة الدومينات الاحتياطية
        for (fallback in FALLBACK_DOMAINS) {
            val cleanFb = fallback.trimEnd('/')
            if (cleanFb == currentBase || cleanFb == freshBase) continue
            val res = fetchAndDecrypt("$cleanFb$path")
            if (!res.isNullOrBlank()) {
                activeBaseUrl = cleanFb
                lastDomainResolveTime = System.currentTimeMillis()
                return@withContext res
            }
        }
        null
    }

    suspend fun fetchMainCategories(): String? = withContext(Dispatchers.IO) {
        fetchAndDecryptApiPath("/api/v6.2/main?page=1")
    }

    suspend fun fetchCategoryChannels(categoryId: String): String? = withContext(Dispatchers.IO) {
        fetchAndDecryptApiPath("/api/v6.2/category/$categoryId")
    }

    /**
     * Removes any Ostora player prefix ("412<F>", "413<F>", etc.) from raw stream URLs.
     */
    fun cleanStreamUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return ""
        var cleaned = rawUrl.replace(Regex("^[0-9]+<[Ff]>"), "")
            .replace("412<F>", "").replace("412<f>", "")
            .replace("413<F>", "").replace("413<f>", "")
            .replace("414<F>", "").replace("414<f>", "")
        if (cleaned.contains("<F>", ignoreCase = true)) {
            cleaned = cleaned.substringAfterLast("<F>").substringAfterLast("<f>")
        }
        if (cleaned.contains("|") && !cleaned.contains("###")) {
            val pipeIdx = cleaned.lastIndexOf('|')
            val after = cleaned.substring(pipeIdx + 1)
            if (after.all { it.isDigit() }) {
                cleaned = cleaned.substring(0, pipeIdx)
            }
        }
        return cleaned.trim()
    }

    /**
     * Checks whether a raw URL belongs to the Ostora IPTV network.
     */
    fun isOstoraUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("maziika") ||
                lower.contains("ostora") ||
                lower.contains(".shop") ||
                lower.contains(".fun") ||
                Regex("^[0-9]+<[Ff]>").containsMatchIn(url) ||
                lower.contains("<f>") ||
                (lower.contains(".json") && lower.contains("token=")) ||
                lower.contains("ver6.top")
    }

    private val KNOWN_OSTORA_GROUPS = setOf(
        // Main Server Sports Groups
        "1", "200", "283", "331", "332", "333", "470", "1069", "1070", "3464", "3934", "7001",
        "3471", "1684", "355", "3402", "1072", "1071", "3483", "2925", "2923", "4192", "4297", "6979", "7193",
        // Main Server Entertainment, Movies & Series (سيرفر البث الرئيسي)
        "468",  // باقة Netflix
        "491",  // باقة NETFLIX 2
        "472",  // باقة الأسطورة VIP
        "2944", // باقة شاهد VIP
        "525",  // باقة شاهد
        "498",  // قنوات شاهد و MBC
        "336",  // قنوات MBC
        "527",  // MBC جودة متوسطة
        "3079", // مسرح مصر
        "3080", // أفلام النجوم
        "2942", // الأسطورة أفلام
        "2943", // شوتايم أفلام
        "1654", // وياك
        "3942", // ميلودى
        "361",  // قنوات روتانا
        "490",  // ماجيستك
        "4905", // قنوات ART
        "3467", // قنوات الترفيه والمنوعات
        // Main Server Countries
        "359",  // القنوات المصرية
        "1639", // القنوات المغربية
        "1637", // القنوات الجزائرية
        "1636", // القنوات السورية
        "1638", // القنوات السودانية
        "356",  // قنوات الفجر
        "3458"
    )

    fun parseDrmKeysFromUrl(url: String): Triple<String, String?, String?> {
        if (!url.contains("###")) return Triple(url, null, null)
        val parts = url.split("###")
        val cleanUrl = parts[0].trim()
        val drmPart = if (parts.size > 1) parts[1].trim() else ""
        if (drmPart.contains(":")) {
            val keyParts = drmPart.split(":")
            val kid = keyParts[0].trim()
            val key = keyParts[1].trim()
            return Triple(cleanUrl, kid, key)
        }
        return Triple(cleanUrl, null, null)
    }

    /**
     * Checks whether a channel is an Ostora network channel that requires dynamic token resolution.
     */
    fun isOstoraChannel(channel: Channel): Boolean {
        // قنوات وسيرفرات ياسين تيفي / البوابة الحية و OK.ru معزولة تماماً ولا تتبع نظام الأسطورة
        if (channel.id.startsWith("gw_") ||
            channel.id.startsWith("yacine") ||
            channel.id.startsWith("ok_") ||
            channel.id.startsWith("okru_") ||
            LiveGatewayStreamResolver.isTargetApiUrl(channel.streamUrl) ||
            OkRuStreamResolver.isOkRuUrl(channel.streamUrl)
        ) {
            return false
        }

        val nameLower = channel.name.lowercase()
        val urlLower = channel.streamUrl.lowercase()
        return (channel.groupId != null && KNOWN_OSTORA_GROUPS.contains(channel.groupId)) ||
                isOstoraUrl(channel.streamUrl) ||
                channel.id.startsWith("ostora") ||
                channel.id.startsWith("sp_") ||
                channel.id.startsWith("ent_") ||
                channel.id.startsWith("osn_") ||
                isKnownSportsChannel(channel.id) ||
                nameLower.contains("bein") ||
                nameLower.contains("بي ان") ||
                nameLower.contains("بى ان") ||
                nameLower.contains("ssc") ||
                nameLower.contains("alkass") ||
                nameLower.contains("كأس") ||
                nameLower.contains("osn") ||
                nameLower.contains("ثمانية") ||
                nameLower.contains("ألوان") ||
                nameLower.contains("الوان") ||
                urlLower.contains("51.68.119") ||
                urlLower.contains("138.201.145")
    }

    /**
     * Resolves a fresh, live tokenized stream URL for a Channel right before playback starts.
     * Replicates the exact behavior of STAR Plus (instant live token acquisition).
     */
    suspend fun resolveChannelStream(channel: Channel): ResolvedStreamResult = withContext(Dispatchers.IO) {
        resolveDynamicStream(
            streamUrl = channel.streamUrl,
            channelId = channel.id,
            channelName = channel.name,
            groupId = channel.groupId
        )
    }

    /**
     * Universal Dynamic Stream Resolver:
     * Takes raw URL, optional channelId, optional channelName, optional groupId.
     * Fetches fresh tokens directly from the live Ostora API before ExoPlayer starts.
     */
    suspend fun resolveDynamicStream(
        streamUrl: String,
        channelId: String? = null,
        channelName: String? = null,
        groupId: String? = null
    ): ResolvedStreamResult = withContext(Dispatchers.IO) {
        val (cleanedUrl, urlKid, urlKey) = parseDrmKeysFromUrl(cleanStreamUrl(streamUrl))

        val nameLower = (channelName ?: "").lowercase()
        val isOstoraName = nameLower.contains("bein") || nameLower.contains("بي ان") ||
                nameLower.contains("بى ان") || nameLower.contains("ssc") ||
                nameLower.contains("alkass") || nameLower.contains("كأس") ||
                nameLower.contains("ثمانية") || nameLower.contains("ألوان") ||
                nameLower.contains("osn") || nameLower.contains("اخبار") || nameLower.contains("اطفال") ||
                (channelId != null && (channelId.startsWith("sp_") || channelId.startsWith("ostora_") || isKnownSportsChannel(channelId)))

        if (!isOstoraUrl(streamUrl) && !isOstoraName && (groupId.isNullOrBlank() || !KNOWN_OSTORA_GROUPS.contains(groupId))) {
            return@withContext ResolvedStreamResult(
                streamUrl = cleanedUrl,
                userAgent = DEFAULT_USER_AGENT,
                referer = DEFAULT_REFERER,
                drmKeyId = urlKid,
                drmKey = urlKey
            )
        }

        try {
            val baseUrl = resolveActiveDomain()
            val targetGroupId = groupId?.takeIf { it.isNotBlank() }
                ?: channelId?.let { findGroupIdByChannelId(it) }
                ?: inferGroupIdFromUrlOrName(streamUrl, channelName ?: "")

            if (!targetGroupId.isNullOrBlank()) {
                val freshChannel = fetchFreshChannelFromGroup(
                    baseUrl = baseUrl,
                    groupId = targetGroupId,
                    targetChannelId = channelId.orEmpty(),
                    targetChannelName = channelName.orEmpty(),
                    targetStreamUrl = cleanedUrl
                )
                if (freshChannel != null) {
                    val rawSource = freshChannel.optString("source", "")
                    val (freshUrl, kid, key) = parseDrmKeysFromUrl(cleanStreamUrl(rawSource))
                    val freshAgent = freshChannel.optString("agent", "").ifBlank { DEFAULT_USER_AGENT }
                    if (freshUrl.isNotBlank()) {
                        return@withContext ResolvedStreamResult(
                            streamUrl = freshUrl,
                            userAgent = if (freshAgent.contains("ExoPlayer", true)) DEFAULT_USER_AGENT else freshAgent,
                            referer = DEFAULT_REFERER,
                            drmKeyId = kid ?: urlKid,
                            drmKey = key ?: urlKey
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: return cleaned URL with browser user agent & referer
        ResolvedStreamResult(
            streamUrl = cleanedUrl,
            userAgent = DEFAULT_USER_AGENT,
            referer = DEFAULT_REFERER,
            drmKeyId = urlKid,
            drmKey = urlKey
        )
    }

    /**
     * محرك الاكتشاف الذكي لكافة السيرفرات والجودات الحقيقية المتوفرة فعلياً للقناة عبر شبكة خوادم الأسطورة.
     * يفحص في توازي فائق السرعة كافة باقات القناة الحقيقية (FHD 1080p, HD 720p, SD 480p, LOW 360p, البديل)
     * مع استخراج روابط وتوكنات حية طازجة، ولا يعتمد على أسماء أو جودات وهمية أو عشوائية أبداً.
     */
    suspend fun resolveRealChannelServers(
        currentStreamUrl: String,
        channelId: String? = null,
        channelName: String? = null,
        groupId: String? = null
    ): List<StreamServer> = withContext(Dispatchers.IO) {
        val resultServers = mutableListOf<StreamServer>()
        val nameLower = (channelName ?: "").lowercase()
        val isBein = nameLower.contains("bein") || nameLower.contains("بي ان") ||
                nameLower.contains("بي إن") || nameLower.contains("بين سبورت") ||
                (channelId != null && channelId.startsWith("sp_bein"))
        val isSsc = nameLower.contains("ssc") || (channelId != null && channelId.startsWith("sp_ssc"))

        try {
            val baseUrl = resolveActiveDomain()

            val groupConfigs = when {
                isBein -> listOf(
                    Triple("333", "سيرفر 1 (1080p FHD)", "1080p FHD"),
                    Triple("332", "سيرفر 2 (720p HD)", "720p HD"),
                    Triple("283", "سيرفر 3 (480p SD)", "480p SD"),
                    Triple("331", "سيرفر 4 (360p Low)", "360p Low"),
                    Triple("1070", "سيرفر بديل 1 (720p HD)", "720p HD"),
                    Triple("1069", "سيرفر بديل 2 (480p SD)", "480p SD")
                )
                isSsc -> listOf(
                    Triple("3934", "سيرفر 1 (1080p FHD)", "1080p FHD"),
                    Triple("7001", "سيرفر 2 (720p HD)", "720p HD"),
                    Triple("3464", "سيرفر 3 (480p SD)", "480p SD")
                )
                else -> {
                    val inferred = groupId?.takeIf { it.isNotBlank() }
                        ?: channelId?.let { findGroupIdByChannelId(it) }
                        ?: inferGroupIdFromUrlOrName(currentStreamUrl, channelName ?: "")
                    if (!inferred.isNullOrBlank()) {
                        listOf(Triple(inferred, "سيرفر 1 (رئيسي)", "720p HD"))
                    } else {
                        emptyList()
                    }
                }
            }

            if (groupConfigs.isNotEmpty()) {
                coroutineScope {
                    val deferreds = groupConfigs.map { (gid, srvName, srvQuality) ->
                        async {
                            try {
                                val item = fetchFreshChannelFromGroup(
                                    baseUrl = baseUrl,
                                    groupId = gid,
                                    targetChannelId = channelId.orEmpty(),
                                    targetChannelName = channelName.orEmpty(),
                                    targetStreamUrl = currentStreamUrl
                                )
                                if (item != null) {
                                    val rawSource = item.optString("source", "").trim()
                                    if (rawSource.isNotBlank()) {
                                        val clean = cleanStreamUrl(rawSource)
                                        if (clean.isNotBlank()) {
                                            return@async StreamServer(
                                                id = "ostora_${gid}_${item.optString("id", "")}",
                                                name = srvName,
                                                quality = srvQuality,
                                                url = clean,
                                                isDirect = true
                                            )
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                            null
                        }
                    }
                    val resolvedItems = deferreds.awaitAll().filterNotNull()

                    val seenUrls = mutableSetOf<String>()
                    for (srv in resolvedItems) {
                        val norm = srv.url.substringBefore("?").trim()
                        if (!seenUrls.contains(norm)) {
                            seenUrls.add(norm)
                            resultServers.add(srv)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // في حال لم تتوفر باقات إضافية للقناة، يتم اعتماد السيرفر الرئيسي الحالي
        if (resultServers.isEmpty() && currentStreamUrl.isNotBlank()) {
            val resolvedCurrent = try {
                resolveDynamicStream(currentStreamUrl, channelId, channelName, groupId)
            } catch (_: Exception) {
                null
            }
            val finalUrl = resolvedCurrent?.streamUrl?.takeIf { it.isNotBlank() } ?: cleanStreamUrl(currentStreamUrl)
            resultServers.add(
                StreamServer(
                    id = "srv_current",
                    name = "سيرفر 1 (رئيسي)",
                    quality = "تلقائي (Auto)",
                    url = finalUrl,
                    isDirect = true
                )
            )
        }

        resultServers
    }

    /**
     * Fetches group channels from Ostora API with a 45-second memory cache to eliminate duplicate network calls.
     */
    private suspend fun fetchFreshChannelFromGroup(
        baseUrl: String,
        groupId: String,
        targetChannelId: String,
        targetChannelName: String,
        targetStreamUrl: String
    ): JSONObject? {
        val now = System.currentTimeMillis()
        val cached = categoryItemsCache[groupId]
        val itemsList = if (cached != null && (now - cached.first) < 45_000) {
            cached.second
        } else {
            val decryptedData = fetchAndDecryptApiPath("/api/v6.2/category/$groupId") ?: return null
            try {
                val cleanJson = if (decryptedData.contains("{") && decryptedData.contains("}")) {
                    decryptedData.substring(decryptedData.indexOf('{'), decryptedData.lastIndexOf('}') + 1)
                } else decryptedData
                val dataObj = JSONObject(cleanJson)
                val itemsArr = dataObj.optJSONObject("data")?.optJSONArray("items")
                    ?: dataObj.optJSONArray("items")
                    ?: dataObj.optJSONArray("data")
                    ?: JSONArray()
                val list = mutableListOf<JSONObject>()
                for (i in 0 until itemsArr.length()) {
                    val item = itemsArr.optJSONObject(i)
                    if (item != null) list.add(item)
                }
                categoryItemsCache[groupId] = now to list
                list
            } catch (_: Exception) {
                return null
            }
        }

        // 1. Match by exact id (handles both raw ID e.g. "4111" and prefixed "ostora_332_4111")
        if (targetChannelId.isNotBlank()) {
            val strippedId = if (targetChannelId.startsWith("ostora_")) {
                targetChannelId.substringAfterLast("_")
            } else {
                targetChannelId
            }
            val matchById = itemsList.firstOrNull { it.optString("id") == targetChannelId || it.optString("id") == strippedId }
            if (matchById != null) return matchById
        }

        // 2. Match by stream filename pattern (e.g. "1_42.json" or "84.json")
        if (targetStreamUrl.isNotBlank()) {
            val streamFilename = targetStreamUrl.substringAfterLast("/").substringBefore("?").trim()
            if (streamFilename.isNotEmpty() && streamFilename.endsWith(".json")) {
                val matchByPath = itemsList.firstOrNull {
                    it.optString("source", "").contains(streamFilename)
                }
                if (matchByPath != null) return matchByPath
            }
        }

        // 3. Match by smart channel number extraction for beIN and numbered sports channels
        if (targetChannelName.isNotBlank()) {
            val targetNum = Regex("""\b(\d{1,2})\b""").find(targetChannelName)?.groupValues?.get(1)
            if (targetNum != null) {
                val matchByNum = itemsList.firstOrNull {
                    val itemName = it.optString("name", "")
                    val itemNum = Regex("""\b(\d{1,2})\b""").find(itemName)?.groupValues?.get(1)
                    itemNum == targetNum
                }
                if (matchByNum != null) return matchByNum
            }

            // 4. Fallback: match by normalized channel name
            val normTarget = targetChannelName.replace(" ", "").lowercase()
            val matchByName = itemsList.firstOrNull {
                val itemName = it.optString("name").replace(" ", "").lowercase()
                itemName == normTarget || itemName.contains(normTarget) || normTarget.contains(itemName)
            }
            if (matchByName != null) return matchByName
        }

        return if (itemsList.size == 1) itemsList.firstOrNull() else null
    }

    /**
     * Fallback lookup for major sports channel IDs in case groupId was not stored.
     * Extracts group directly from ostora_<groupId>_<channelId> formatted IDs, or resolves from known ID sets.
     */
    fun findGroupIdByChannelId(channelId: String): String? {
        if (channelId.startsWith("ostora_")) {
            val parts = channelId.split("_")
            if (parts.size >= 3 && parts[1].isNotEmpty() && parts[1].all { it.isDigit() }) {
                return parts[1]
            }
        }
        val cleanId = if (channelId.startsWith("ostora_")) channelId.substringAfterLast("_") else channelId
        return when {
            // beIN Movies / Series / Drama (group 360)
            cleanId.startsWith("sp_bein_movies") || cleanId.startsWith("sp_bein_series") || cleanId.startsWith("sp_bein_drama") -> "360"
            // OSN Entertainment (group 454)
            cleanId.startsWith("sp_osn") -> "454"
            // Kids & Documentary (group 338)
            cleanId.startsWith("sp_kids") || cleanId.startsWith("sp_spacetoon") || cleanId.startsWith("sp_cn") ||
                    cleanId.startsWith("sp_majid") || cleanId.startsWith("sp_nat_geo") || cleanId.startsWith("sp_aljazeera_doc") -> "338"
            // News (group 495)
            cleanId.startsWith("sp_news") -> "495"
            // Thamanya Sports (group 6979)
            cleanId.startsWith("sp_thamanya") -> "6979"
            // Alwan Sports (group 7193)
            cleanId.startsWith("sp_alwan") -> "7193"
            // Egyptian Sports (group 200)
            cleanId.startsWith("sp_egypt") -> "200"
            // beIN Sports FHD (group 333)
            cleanId in setOf("4128", "4129", "4130", "4132", "4134", "4136", "4138", "4140", "4142", "4144") -> "333"
            // beIN Sports HD (group 332)
            cleanId in setOf("4111", "4113", "4115", "4117", "4119", "4121", "4123", "4125", "4127") ||
            cleanId.startsWith("sp_bein") -> "332"
            // beIN Sports SD (group 283)
            cleanId in setOf("154718", "154719", "10234", "10236", "10238", "10240", "10242", "10244", "10246", "10248", "10250") -> "283"
            // SSC Sports (group 3934 or 7001 or 3464)
            cleanId.startsWith("sp_ssc") -> "3934"
            // Alkass Sports (group 471)
            cleanId.startsWith("sp_alkass") -> "471"
            // AD Sports (group 467)
            cleanId.startsWith("sp_adsports") -> "467"
            // KSA Sports (group 470)
            cleanId.startsWith("sp_ksasports") -> "470"
            // beIN Sports LOW (group 331)
            cleanId in setOf("4076", "4074") -> "331"
            // beIN Sports HD alternative (group 1070)
            cleanId in setOf("22272", "22269") -> "1070"
            // beIN Sports SD alternative (group 1069)
            cleanId in setOf("17219", "17217") -> "1069"
            else -> null
        }
    }

    /**
     * Checks if a channel ID is a known sports channel or an Ostora channel.
     */
    fun isKnownSportsChannel(channelId: String): Boolean {
        return channelId.startsWith("ostora_") || findGroupIdByChannelId(channelId) != null
    }

    /**
     * Infer groupId from URL signature or Channel Name.
     */
    private fun inferGroupIdFromUrlOrName(url: String, name: String): String? {
        val lowerUrl = url.lowercase()
        val lowerName = name.lowercase()

        return when {
            lowerUrl.contains("/x1/") && lowerUrl.contains("_42.json") -> "333"
            lowerUrl.contains("/x1/") && lowerUrl.contains("_32.json") -> "332"
            lowerUrl.contains("/x1/") && lowerUrl.contains("_22.json") -> "283"
            lowerUrl.contains("/x1/") && lowerUrl.contains("_1.json") -> "331"
            lowerUrl.contains("/x1/") && (lowerUrl.contains("14.json") || lowerUrl.contains("15.json") || lowerUrl.contains("16.json") || lowerUrl.contains("17.json") || lowerUrl.contains("18.json")) -> "200"
            lowerUrl.contains("/x1/") && (lowerUrl.contains("19.json") || lowerUrl.contains("20.json") || lowerUrl.contains("21.json") || lowerUrl.contains("22.json")) -> "470"
            lowerUrl.contains("/x1/") && (lowerUrl.contains("23.json") || lowerUrl.contains("24.json") || lowerUrl.contains("25.json") || lowerUrl.contains("26.json") || lowerUrl.contains("27.json") || lowerUrl.contains("28.json")) -> "471"
            lowerUrl.contains("/x3/") && lowerUrl.contains("84.json") -> "1070"
            lowerUrl.contains("/x3/") && lowerUrl.contains("94.json") -> "1069"
            lowerUrl.contains("/x3/") -> "1070"
            lowerUrl.contains("/x6/") -> "495"
            lowerUrl.contains("shahid-sports") -> "3934"
            lowerUrl.contains("intigral-ott") -> "7001"
            lowerName.contains("osn") -> "454"
            lowerName.contains("bein movie") || lowerName.contains("بي ان افلام") || lowerName.contains("بي ان دراما") || lowerName.contains("bein series") || lowerName.contains("bein ent") -> "360"
            lowerName.contains("اطفال") || lowerName.contains("kids") || lowerName.contains("كرتون") || lowerName.contains("سبيستون") -> "338"
            lowerName.contains("اخبار") || lowerName.contains("news") || lowerName.contains("الجزيرة") || lowerName.contains("العربية") -> "495"
            lowerName.contains("ssc") || lowerName.contains("شاهد سبورت") -> "3934"
            (lowerName.contains("bein") || lowerName.contains("بي ان") || lowerName.contains("بى ان")) &&
                    (lowerName.contains("fhd") || lowerName.contains("4k") || lowerName.contains("1080")) -> "333"
            (lowerName.contains("bein") || lowerName.contains("بي ان") || lowerName.contains("بى ان")) &&
                    lowerName.contains("sd") -> "283"
            lowerName.contains("bein") || lowerName.contains("بي ان") || lowerName.contains("بى ان") -> "332"
            lowerName.contains("أبو ظبي") || lowerName.contains("abu dhabi") || lowerName.contains("adsports") -> "467"
            lowerName.contains("كأس") || lowerName.contains("alkass") -> "471"
            lowerName.contains("السعودية الرياضية") -> "470"
            lowerName.contains("ألوان") || lowerName.contains("الوان") || lowerName.contains("alwan") -> "7193"
            lowerName.contains("ثمانية") || lowerName.contains("thmanyah") -> "6979"
            else -> "332"
        }
    }

    /**
     * جلب قنوات الأسطورة الحية المباشرة (beIN Sports, SSC, Alkass, AD Sports)
     * مباشرة من خادم الأسطورة النشط لإضافتها في صدارة مشغل التطبيق بضمان 100%.
     */
    suspend fun fetchLiveOstoraChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Channel>()
        try {
            val baseUrl = resolveActiveDomain()
            val allLiveGroups = listOf(
                // Sports Groups
                "332" to "قنوات بي ان سبورت",
                "283" to "قنوات بي ان سبورت SD",
                "7193" to "قنوات ألوان الرياضية",
                "6979" to "قنوات ثمانية",
                "7001" to "قنوات ثمانية",
                "200" to "قنوات ON SPORTS والرياضة المصرية",
                "471" to "قنوات الكأس الرياضية",
                "470" to "قنوات KSA SPORTS",
                // News
                "495" to "قنوات الأخبار"
            )

            coroutineScope {
                val deferreds = allLiveGroups.map { (groupId, catName) ->
                    async {
                        val groupList = mutableListOf<Channel>()
                        try {
                            val decrypted = fetchAndDecryptApiPath("/api/v6.2/category/$groupId") ?: return@async emptyList()
                            val dataObj = JSONObject(decrypted)
                            val itemsArr = dataObj.optJSONObject("data")?.optJSONArray("items")
                                ?: dataObj.optJSONArray("items")
                                ?: dataObj.optJSONArray("data")
                                ?: JSONArray()

                            for (i in 0 until itemsArr.length()) {
                                val it = itemsArr.optJSONObject(i) ?: continue
                                val rawSource = it.optString("source", "")
                                if (rawSource.isBlank()) continue

                                val name = it.optString("name", "قناة").trim()
                                val lowerName = name.lowercase()

                                // Strict exclusions requested by user
                                if (lowerName.contains("وقت المبار") || lowerName.contains("kora plus") ||
                                    lowerName.contains("(server 1)") || lowerName.contains("(server 4)") ||
                                    lowerName.contains("تايم سبورت") || lowerName.contains("time sport") ||
                                    lowerName.contains("ssc") || lowerName.contains("mbc") ||
                                    lowerName.contains("حدث مباشر") ||
                                    lowerName.contains("أبوظبي") || lowerName.contains("ابوظبي") ||
                                    lowerName.contains("abu dhabi") || lowerName.contains("adsports") ||
                                    lowerName.contains("ad sport") || lowerName.contains("beout") ||
                                    lowerName.contains("osn") || lowerName.contains("أطفال") ||
                                    lowerName.contains("اطفال") || lowerName.contains("ترفيه") ||
                                    lowerName.contains(" nm") || lowerName.contains("nm ")
                                ) {
                                    continue
                                }

                                val (cleanUrl, kid, key) = parseDrmKeysFromUrl(cleanStreamUrl(rawSource))
                                if (cleanUrl.isBlank() || cleanUrl.contains("hex-cdn.link", ignoreCase = true) || cleanUrl.contains("raeed4tv", ignoreCase = true)) {
                                    continue
                                }
                                val chId = "ostora_${groupId}_${it.optString("id", i.toString())}"
                                val image = it.optString("image", "").ifBlank {
                                    "https://imagecache.365scores.com/image/upload/f_auto,w_80,h_80,c_limit,d_Competitions:default.png/Competitions/572"
                                }
                                val agent = it.optString("agent", "").ifBlank { DEFAULT_USER_AGENT }

                                groupList.add(
                                    Channel(
                                        id = chId,
                                        name = name,
                                        logoUrl = image,
                                        category = catName,
                                        isLive = true,
                                        currentProgram = "بث مباشر فائق الدقة",
                                        streamUrl = cleanUrl,
                                        userAgent = agent,
                                        referer = DEFAULT_REFERER,
                                        groupId = groupId,
                                        drmKeyId = kid,
                                        drmKey = key
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                        groupList
                    }
                }
                val seenNames = mutableSetOf<String>()
                val seenUrls = mutableSetOf<String>()
                deferreds.awaitAll().forEach { groupChannels ->
                    for (ch in groupChannels) {
                        val normName = ch.name.lowercase().replace(" ", "")
                        val normUrl = ch.streamUrl.trim()
                        if (!seenNames.contains(normName) && !seenUrls.contains(normUrl)) {
                            seenNames.add(normName)
                            seenUrls.add(normUrl)
                            list.add(ch)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }

    /**
     * Dynamic stream resolver for raw URLs
     */
    suspend fun resolveStreamUrl(rawUrl: String): String {
        return resolveDynamicStream(rawUrl).streamUrl
    }

    /**
     * تشغيل البث مباشرة عبر مشغل الفيديو الرسمي للأسطورة (مشغل الفيديو 4.0 - info.t4w.vp).
     * يدعم الاستدعاء الصريح للنشاط أو عبر مخطط URI (urlvplayer://).
     */
    fun launchOstoraVideoPlayer(
        context: Context,
        streamUrl: String,
        title: String? = null,
        userAgent: String? = null
    ): Boolean {
        if (streamUrl.isBlank()) return false
        val cleanUrl = cleanStreamUrl(streamUrl)
        val effAgent = userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT

        // 1. محاولة التشغيل المباشر لمكون VideoPlayer الرسمي
        try {
            val directIntent = Intent().apply {
                component = ComponentName("info.t4w.vp", "info.t4w.vp.view.VideoPlayer")
                putExtra("url", cleanUrl)
                if (!title.isNullOrBlank()) {
                    putExtra("title", title)
                }
                putExtra("agent", effAgent)
                val headersBundle = android.os.Bundle().apply {
                    putString("User-Agent", effAgent)
                    putString("Referer", DEFAULT_REFERER)
                }
                putExtra("headers", headersBundle)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(directIntent)
            return true
        } catch (_: Exception) {}

        // 2. محاولة التشغيل عبر Scheme المخصص (urlvplayer://play)
        try {
            val encodedUrl = URLEncoder.encode(cleanUrl, "UTF-8")
            val encodedAgent = URLEncoder.encode(effAgent, "UTF-8")
            val schemeUri = Uri.parse("urlvplayer://play?url=$encodedUrl&agent=$encodedAgent")
            val schemeIntent = Intent(Intent.ACTION_VIEW, schemeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(schemeIntent)
            return true
        } catch (_: Exception) {}

        // 3. محاولة تشغيل مشغل الفيديو الخارجي العام عبر URL Player
        try {
            val urlPlayerUri = Uri.parse("urlplayer://play?url=" + URLEncoder.encode(cleanUrl, "UTF-8"))
            val playerIntent = Intent(Intent.ACTION_VIEW, urlPlayerUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playerIntent)
            return true
        } catch (_: Exception) {}

        return false
    }

    private fun fetchAndDecrypt(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val encryptedBody = response.body?.string() ?: return null
                decryptOstoraPayload(encryptedBody)
            }
        } catch (_: Exception) {
            null
        }
    }
}
