package com.beouttv.app.data.repository

import android.content.Context
import com.beouttv.app.data.api.CoreApiClient
import com.beouttv.app.data.config.AppGatewayManager
import java.io.File
import com.beouttv.app.data.favorites.FavoritesManager
import com.beouttv.app.data.filter.ContentFilterEngine
import com.beouttv.app.data.history.PlaybackHistoryManager
import com.beouttv.app.data.i18n.AppLanguage
import com.beouttv.app.data.i18n.LanguageManager
import com.beouttv.app.data.imdb.ImdbMetadataEngine
import com.beouttv.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class RemoteMediaRepository(
    private val context: Context? = null,
    private val client: CoreApiClient = CoreApiClient()
) : BeoutRepository {

    private val cachedChannels = CopyOnWriteArrayList<Channel>()
    private val cachedMovies = CopyOnWriteArrayList<Movie>()
    private val cachedSeries = CopyOnWriteArrayList<Series>()
    private val movieRealIds = ConcurrentHashMap<String, String>()
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isBatchEnriching = false

    private val _moviesFlow = MutableStateFlow<List<Movie>>(emptyList())
    override val moviesFlow: StateFlow<List<Movie>> = _moviesFlow.asStateFlow()

    private val _seriesFlow = MutableStateFlow<List<Series>>(emptyList())
    override val seriesFlow: StateFlow<List<Series>> = _seriesFlow.asStateFlow()

    private val _channelsFlow = MutableStateFlow<List<Channel>>(emptyList())
    override val channelsFlow: StateFlow<List<Channel>> = _channelsFlow.asStateFlow()

    private fun getValidContext(): Context? {
        return context ?: ServiceLocator.getAppContext()
    }

    private val isCatalogsLoaded = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        // Instant 15ms Protobuf preload: immediately populate cachedChannels from libbeout_streams.dat
        repoScope.launch(Dispatchers.IO) {
            val ctx = getValidContext()
            if (ctx != null && cachedChannels.isEmpty()) {
                try {
                    val datBytes = com.beouttv.app.data.storage.StreamsCatalogStorage.loadCatalogBytes(ctx)
                    if (datBytes != null && datBytes.isNotEmpty()) {
                        val instantChannels = com.beouttv.app.data.proto.ChannelProtoCodec.decodeChannelList(datBytes)
                        if (instantChannels.isNotEmpty() && cachedChannels.isEmpty()) {
                            cachedChannels.addAll(instantChannels)
                            _channelsFlow.value = cachedChannels.toList()
                            lastChannelSyncTimestamp = System.currentTimeMillis()
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Immediately synchronize live channels in background (Ostora + remote panel)
        repoScope.launch(Dispatchers.IO) {
            syncRemoteChannels(forceRefresh = true)
        }

        // Asynchronously sync remote config from control panel / dashboard
        repoScope.launch(Dispatchers.IO) {
            try {
                AppGatewayManager.syncRemoteConfig()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Reactively re-localize and update all movies & cards whenever app language changes
        repoScope.launch {
            LanguageManager.currentLanguage.collect { lang ->
                if (cachedMovies.isNotEmpty()) {
                    val currentLang = LanguageManager.currentLanguage.value
                    val localized = cachedMovies.map { ImdbMetadataEngine.localizeMovie(it, currentLang) }
                    _moviesFlow.value = localized
                }
            }
        }

        // Preload dynamic series catalog immediately
        repoScope.launch(Dispatchers.IO) {
            getSeries()
        }

        // Reactively listen to MediaSourceManager source switches
        repoScope.launch {
            com.beouttv.app.data.source.MediaSourceManager.sourceSwitchEvent.collect { _ ->
                reloadCurrentSourceContent()
            }
        }
    }

    private var lastChannelSyncTimestamp: Long = 0L

    private suspend fun syncRemoteChannels(forceRefresh: Boolean = false) = withContext(Dispatchers.IO) {
        val targetContext = getValidContext() ?: return@withContext

        // Delete any legacy plaintext disk cache to protect private stream URLs and DRM keys
        val legacyCache = File(targetContext.filesDir, "remote_channels_cache.json")
        if (legacyCache.exists()) {
            try { legacyCache.delete() } catch (_: Exception) {}
        }

        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedChannels.isNotEmpty()) {
            isCatalogsLoaded.set(true)
            return@withContext
        }

        // 1. Base Guaranteed Clean Catalog (Instant Protobuf Binary .dat - loads in < 2ms)
        val baseBundledChannels = try {
            val datBytes = com.beouttv.app.data.storage.StreamsCatalogStorage.loadCatalogBytes(targetContext)
            if (datBytes != null && datBytes.isNotEmpty()) {
                com.beouttv.app.data.proto.ChannelProtoCodec.decodeChannelList(datBytes)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        // 2. Admin channels added manually from Control Panel (/channels.json or /custom_channels.json)
        val customChannels = mutableListOf<Channel>()
        try {
            val panelUrls = listOf(
                "https://digite-bda04-default-rtdb.firebaseio.com/channels.json",
                "https://digite-bda04-default-rtdb.firebaseio.com/custom_channels.json"
            )
            for (pUrl in panelUrls) {
                try {
                    val responseStr = client.fetchString(pUrl).trim()
                    if (responseStr.isNotBlank() && (responseStr.startsWith("{") || responseStr.startsWith("["))) {
                        val parsed = parseChannelsJson(responseStr, targetContext)
                        for (c in parsed) {
                            if (customChannels.none { it.id == c.id || (it.name == c.name && it.category == c.category) }) {
                                customChannels.add(c)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // 3. Combine channels in fixed deterministic order (Zero fluctuation, instant startup)
        val combined = mutableListOf<Channel>()
        val seenKeys = mutableSetOf<String>()

        // Priority 1: Admin Custom Channels from Control Panel (Always displayed with top priority)
        for (ch in customChannels) {
            val normName = ch.name.lowercase().replace(" ", "")
            val normStream = ch.streamUrl.substringBefore("?").trim()
            val dedupeKey = "${ch.category.trim()}|$normName|$normStream"
            val categoryKey = "${ch.category.trim()}|$normName"
            seenKeys.add(dedupeKey)
            seenKeys.add(categoryKey)
            combined.add(ch)
        }

        fun addIfUnique(ch: Channel) {
            if (isChannelPurged(ch.name, ch.category, ch.id)) return
            val normName = ch.name.lowercase().replace(" ", "")
            val normStream = ch.streamUrl.substringBefore("?").trim()
            val dedupeKey = "${ch.category.trim()}|$normName|$normStream"
            val categoryKey = "${ch.category.trim()}|$normName"
            if (!seenKeys.contains(dedupeKey) && !seenKeys.contains(categoryKey)) {
                seenKeys.add(dedupeKey)
                seenKeys.add(categoryKey)
                combined.add(ch)
            }
        }

        // Priority 1.5: Special Bouquet & Dax Multi-Quality Sports Channels (Thamanya, Alwan, Alkass, beIN)
        try {
            val specialChannels = com.beouttv.app.data.source.stream.SpecialBouquetProvider.getChannels(targetContext)
            for (ch in specialChannels) {
                addIfUnique(ch)
            }
        } catch (_: Exception) {}

        // Priority 2: Base Bundled Clean Catalog (from libbeout_streams.dat)
        for (ch in baseBundledChannels) {
            addIfUnique(ch)
        }

        if (combined.isNotEmpty()) {
            cachedChannels.clear()
            cachedChannels.addAll(combined)
            _channelsFlow.value = cachedChannels.toList()
        }
        lastChannelSyncTimestamp = System.currentTimeMillis()
        isCatalogsLoaded.set(true)
    }

    private fun isChannelPurged(name: String, category: String, channelId: String = ""): Boolean {
        val lowerName = name.lowercase().trim()
        val lowerCat = category.lowercase().trim()
        val lowerId = channelId.lowercase().trim()

        // Match-time and temporary test channels
        if (lowerName.contains("وقت المبار") || lowerCat.contains("وقت المبار")) return true
        if (lowerName.contains("kora plus") || lowerCat.contains("kora plus")) return true
        if (lowerName.contains("حدث مباشر") || lowerCat.contains("حدث مباشر")) return true
        if (lowerName.contains("(server 1)") || lowerName.contains("(server 4)")) return true

        // Purge unwanted Yacine / Gateway channels completely
        if (lowerId.startsWith("gw_") || lowerId.startsWith("yacine") ||
            lowerCat.contains("yacine") || lowerName.contains("yacine") ||
            lowerCat.contains("ياسين") || lowerName.contains("ياسين")) return true

        // 1. Purge all Beout / BeoutQ channels completely
        if (lowerName.contains("beout") || lowerCat.contains("beout") || lowerId.contains("beout")) return true

        // 2. Purge all OSN channels completely
        if (lowerName.contains("osn") || lowerCat.contains("osn") || lowerId.contains("osn")) return true

        // 3. Purge all beIN Sports NM channels completely
        if (lowerName.contains(" nm") || lowerName.contains("nm ") || lowerCat.contains("nm") || lowerId.contains("_nm") || lowerId.contains("beinnm")) return true

        // 4. Purge all beIN Entertainment channels completely
        if (lowerCat.contains("ترفيه") || lowerCat.contains("entertainment") ||
            lowerName.contains("bein movie") || lowerName.contains("bein series") || lowerName.contains("bein drama") ||
            lowerCat.contains("بي ان الترفيهية")) return true

        // 5. Purge all Kids & Documentary channels completely
        if (lowerCat.contains("أطفال") || lowerCat.contains("اطفال") || lowerCat.contains("وثائقي") || lowerCat.contains("kids") ||
            lowerName.contains("سبيستون") || lowerName.contains("spacetoon") || lowerName.contains("كرتون") ||
            lowerName.contains("ماجد") || lowerName.contains("ناشونال جيوغرافيك") || lowerName.contains("الجزيرة الوثائقية")) return true

        // 6. Purge all Abu Dhabi Sports channels completely
        if (lowerName.contains("أبوظبي") || lowerName.contains("ابوظبي") || lowerName.contains("abu dhabi") ||
            lowerName.contains("adsports") || lowerName.contains("ad sport") ||
            lowerCat.contains("أبوظبي") || lowerCat.contains("ابوظبي") || lowerCat.contains("abu dhabi")) return true

        // 7. Purge all Alkass channels completely
        if (lowerName.contains("كأس") || lowerName.contains("كاس") || lowerName.contains("alkass") ||
            lowerCat.contains("كأس") || lowerCat.contains("كاس") || lowerCat.contains("alkass") ||
            lowerId.contains("alkass")) return true

        // 8. Purge all News channels completely (الجزيرة، العربية، الحدث، سكاي نيوز، الخ)
        if (lowerCat.contains("أخبار") || lowerCat.contains("اخبار") || lowerCat.contains("news") ||
            lowerName.contains("جزيرة") || lowerName.contains("عربية") || lowerName.contains("الحدث") ||
            lowerName.contains("سكاي") || lowerName.contains("sky news") || lowerName.contains("bbc") ||
            lowerName.contains("روسيا اليوم") || lowerName.contains("قاهرة") || lowerName.contains("ميادين") ||
            lowerName.contains("العالم") || lowerName.contains("الشرق") || lowerName.contains("الغد")) return true

        // 9. Purge all Dubai channels completely
        if (lowerName.contains("دبي") || lowerName.contains("dubai") ||
            lowerCat.contains("دبي") || lowerCat.contains("dubai") || lowerId.contains("dubai")) return true

        // 10. Purge all Nilesat channels completely
        if (lowerName.contains("نايل") || lowerName.contains("nile") ||
            lowerCat.contains("نايل") || lowerCat.contains("nile") ||
            lowerName.contains("nilesat") || lowerCat.contains("nilesat")) return true

        // 11. Purge all STC channels completely
        if (lowerName.contains("stc") || lowerCat.contains("stc") || lowerId.contains("stc")) return true

        // Old duplicate categories
        if (lowerCat.contains("bein sport hd") || lowerCat.contains("bein sport low")) return true
        if (lowerName.contains("تايم سبورت") || lowerCat.contains("تايم سبورت")) return true
        if (lowerName.contains("time sport") || lowerCat.contains("time sport")) return true
        if (lowerName.contains("ssc") || lowerCat.contains("ssc")) return true
        if (lowerCat.contains("mbc") || lowerCat.contains("شاهد") || lowerName.contains("mbc") ||
            lowerCat.contains("الشرق والإخبارية") || lowerCat.contains("الشرق والاخبارية")) return true

        // Foreign countries
        if (lowerCat.contains("قنوات دول") || lowerCat.contains("بريطاني") || lowerCat.contains("فرنسي") ||
            lowerCat.contains("british") || lowerCat.contains("french") || lowerCat.contains("german") ||
            lowerCat.contains("italian") || lowerCat.contains("turkish") || lowerCat.contains("world channels")) return true

        // Saudi general channels (remove Quran, Sunnah, Saudi 1, SBC, etc., KEEP ONLY KSA SPORTS)
        if (lowerCat.contains("سعودية") || lowerCat.contains("saudi")) {
            if (!lowerName.contains("ksa") && !lowerName.contains("sport") && !lowerName.contains("رياض")) {
                return true
            }
        }

        val purgedKeywords = listOf(
            "روتانا", "rotana", "الرسالة", "beoutq", "beout q", "caf 2019", "copa america",
            "عراق", "iraq",
            "الجزائر", "المغرب", "سوريا", "السودان", "فلسطين", "الأردن", "الاردن",
            "لبنان", "تونس", "ليبيا", "اذاع", "راديو", "radio", "max 1", "max 2", "max 3", "max 4"
        )
        for (kw in purgedKeywords) {
            if (lowerName.contains(kw) || lowerCat.contains(kw)) {
                return true
            }
        }
        return false
    }

    private fun parseChannelsJson(jsonStr: String, targetContext: Context): List<Channel> {
        val trimmed = jsonStr.trim()
        val list = mutableListOf<Channel>()
        try {
            val dataArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val json = JSONObject(trimmed)
                val directArr = json.optJSONArray("data")
                    ?: json.optJSONArray("channels")
                    ?: json.optJSONArray("items")
                if (directArr != null) {
                    directArr
                } else {
                    // Object dictionary from Firebase RTDB
                    val arr = JSONArray()
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val obj = json.optJSONObject(k)
                        if (obj != null) {
                            if (!obj.has("id")) obj.put("id", k)
                            arr.put(obj)
                        }
                    }
                    arr
                }
            }

            for (i in 0 until dataArray.length()) {
                val ch = dataArray.getJSONObject(i)
                val id = ch.optString("id", i.toString())
                val name = ch.optString("name", "Channel")
                val category = ch.optString("category", "عام")
                val url = ch.optString("streamUrl", ch.optString("url", ""))
                var logo = ch.optString("logoUrl", ch.optString("logo", ""))
                if (logo.startsWith("/")) {
                    logo = "https://admin.dramaramadan.net$logo"
                } else if (logo.isBlank()) {
                    logo = "https://picsum.photos/seed/ch$id/200/200"
                }
                val status = ch.optInt("status", 1)

                if (status == 1 && url.isNotBlank()) {
                    if (ContentFilterEngine.isContentBlocked(name, category)) {
                        continue
                    }

                    val drmKeyId = ch.optString("drmKeyId", ch.optString("drm_key_id", ch.optString("key_id", ""))).ifBlank { null }
                    val drmKey = ch.optString("drmKey", ch.optString("drm_key", ch.optString("key", ""))).ifBlank { null }
                    val drmLicense = ch.optString("drmLicenseUrl", ch.optString("drm_license_url", ch.optString("license_url", ""))).ifBlank { null }
                    val groupId = ch.optString("groupId", ch.optString("group_id", "")).ifBlank { null }
                    val serverId = ch.optString("serverId", ch.optString("server_id", "")).ifBlank { null }
                    val userAgent = ch.optString("userAgent", ch.optString("user_agent", "")).ifBlank { null }
                    val referer = ch.optString("referer", "").ifBlank { null }
                    val origin = ch.optString("origin", "").ifBlank { null }
                    val cookie = ch.optString("cookie", "").ifBlank { null }
                    val currentProgram = ch.optString("currentProgram", ch.optString("program", "بث مباشر عالي الدقة"))

                    val isFav = FavoritesManager.isChannelFavorite(targetContext, id)
                    list.add(
                        Channel(
                            id = id,
                            name = name,
                            logoUrl = logo,
                            category = category,
                            isLive = true,
                            currentProgram = currentProgram,
                            isFavorite = isFav,
                            streamUrl = url,
                            userAgent = userAgent,
                            referer = referer,
                            origin = origin,
                            cookie = cookie,
                            drmKeyId = drmKeyId,
                            drmKey = drmKey,
                            drmLicenseUrl = drmLicense,
                            groupId = groupId,
                            serverId = serverId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Prepend Special Bouquet channels at index 0 so sports channels are immediately visible
        val specialChannels = com.beouttv.app.data.source.stream.SpecialBouquetProvider.getChannels(targetContext)
        for (i in specialChannels.indices.reversed()) {
            val ch = specialChannels[i]
            if (list.none { it.id == ch.id }) {
                list.add(0, ch)
            }
        }

        return list
    }

    private val liveMatches = listOf(
        // TODAY - LIVE & UPCOMING
        Match(
            id = "m_cl_1",
            competition = "دوري أبطال أوروبا",
            homeTeam = "ريال مدريد",
            homeTeamLogoUrl = "https://picsum.photos/seed/rmlogo/80/80",
            awayTeam = "مانشستر سيتي",
            awayTeamLogoUrl = "https://picsum.photos/seed/mcfclogo/80/80",
            homeScore = 2,
            awayScore = 1,
            status = MatchStatus.LIVE,
            startTimeLabel = "مباشر • الدقيقة 78'",
            sport = "Football",
            channel = "beIN Sports 1 HD",
            commentator = "حفيظ دراجي",
            roundOrStadium = "ذهاب نصف النهائي • ملعب سانتياغو برنابيو",
            dateCategory = "today"
        ),
        Match(
            id = "m_pl_1",
            competition = "الدوري الإنجليزي الممتاز",
            homeTeam = "ليفربول",
            homeTeamLogoUrl = "https://picsum.photos/seed/lfclogo/80/80",
            awayTeam = "أرسنال",
            awayTeamLogoUrl = "https://picsum.photos/seed/afclogo/80/80",
            homeScore = 1,
            awayScore = 0,
            status = MatchStatus.LIVE,
            startTimeLabel = "مباشر • الدقيقة 34'",
            sport = "Football",
            channel = "beIN Sports 2 HD",
            commentator = "خليل البلوشي",
            roundOrStadium = "الجولة 28 • ملعب آنفيلد",
            dateCategory = "today"
        ),
        Match(
            id = "m_sa_1",
            competition = "دوري روشن السعودي للمحترفين",
            homeTeam = "الهلال",
            homeTeamLogoUrl = "https://picsum.photos/seed/hilallogo/80/80",
            awayTeam = "النصر",
            awayTeamLogoUrl = "https://picsum.photos/seed/nassrlogo/80/80",
            homeScore = null,
            awayScore = null,
            status = MatchStatus.UPCOMING,
            startTimeLabel = "اليوم 21:00",
            sport = "Football",
            channel = "SSC 1 HD",
            commentator = "فهد العتيبي",
            roundOrStadium = "الجولة 22 • المملكة أرينا (كلاسيكو)",
            dateCategory = "today"
        ),
        Match(
            id = "m_ll_1",
            competition = "الدوري الإسباني",
            homeTeam = "أتلتيكو مدريد",
            homeTeamLogoUrl = "https://picsum.photos/seed/atletilogo/80/80",
            awayTeam = "ريال بيتيس",
            awayTeamLogoUrl = "https://picsum.photos/seed/betislogo/80/80",
            homeScore = null,
            awayScore = null,
            status = MatchStatus.UPCOMING,
            startTimeLabel = "اليوم 20:30",
            sport = "Football",
            channel = "beIN Sports 3 HD",
            commentator = "علي محمد علي",
            roundOrStadium = "الجولة 26 • ملعب سيفيتاس ميتروبوليتانو",
            dateCategory = "today"
        ),
        Match(
            id = "m_sa_ita",
            competition = "الدوري الإيطالي",
            homeTeam = "إنتر ميلان",
            homeTeamLogoUrl = "https://picsum.photos/seed/interlogo/80/80",
            awayTeam = "يوفنتوس",
            awayTeamLogoUrl = "https://picsum.photos/seed/juvelogo/80/80",
            homeScore = null,
            awayScore = null,
            status = MatchStatus.UPCOMING,
            startTimeLabel = "اليوم 21:45",
            sport = "Football",
            channel = "AD Sports Premium 1",
            commentator = "عامر عبدالله",
            roundOrStadium = "الجولة 27 • ملعب سان سيرو (ديربي إيطاليا)",
            dateCategory = "today"
        ),

        // YESTERDAY - FINISHED
        Match(
            id = "m_yest_1",
            competition = "دوري أبطال أوروبا",
            homeTeam = "باريس سان جيرمان",
            homeTeamLogoUrl = "https://picsum.photos/seed/psglogo/80/80",
            awayTeam = "بايرن ميونخ",
            awayTeamLogoUrl = "https://picsum.photos/seed/bayernlogo/80/80",
            homeScore = 1,
            awayScore = 2,
            status = MatchStatus.FINISHED,
            startTimeLabel = "انتهت • FT",
            sport = "Football",
            channel = "beIN Sports 1 HD",
            commentator = "حسن العيدروس",
            roundOrStadium = "إياب ربع النهائي • حديقة الأمراء",
            dateCategory = "yesterday"
        ),
        Match(
            id = "m_yest_2",
            competition = "الدوري الإسباني",
            homeTeam = "برشلونة",
            homeTeamLogoUrl = "https://picsum.photos/seed/barcalogo/80/80",
            awayTeam = "إشبيلية",
            awayTeamLogoUrl = "https://picsum.photos/seed/sevillalogo/80/80",
            homeScore = 3,
            awayScore = 0,
            status = MatchStatus.FINISHED,
            startTimeLabel = "انتهت • FT",
            sport = "Football",
            channel = "beIN Sports 3 HD",
            commentator = "عصام الشوالي",
            roundOrStadium = "الجولة 26 • ملعب لويس كومبانيس",
            dateCategory = "yesterday"
        ),
        Match(
            id = "m_yest_3",
            competition = "دوري روشن السعودي للمحترفين",
            homeTeam = "الاتحاد",
            homeTeamLogoUrl = "https://picsum.photos/seed/ittihadlogo/80/80",
            awayTeam = "الأهلي",
            awayTeamLogoUrl = "https://picsum.photos/seed/ahlilogo/80/80",
            homeScore = 2,
            awayScore = 1,
            status = MatchStatus.FINISHED,
            startTimeLabel = "انتهت • FT",
            sport = "Football",
            channel = "SSC 1 HD",
            commentator = "فارس عوض",
            roundOrStadium = "الجولة 22 • مدينة الملك عبدالله الرياضية",
            dateCategory = "yesterday"
        ),

        // TOMORROW - UPCOMING
        Match(
            id = "m_tom_1",
            competition = "دوري أبطال أوروبا",
            homeTeam = "بايرن ميونخ",
            homeTeamLogoUrl = "https://picsum.photos/seed/bayernlogo/80/80",
            awayTeam = "تشيلسي",
            awayTeamLogoUrl = "https://picsum.photos/seed/chelsealogo/80/80",
            homeScore = null,
            awayScore = null,
            status = MatchStatus.UPCOMING,
            startTimeLabel = "غداً 22:00",
            sport = "Football",
            channel = "beIN Sports 1 HD",
            commentator = "عصام الشوالي",
            roundOrStadium = "نصف النهائي • أليانز أرينا",
            dateCategory = "tomorrow"
        ),
        Match(
            id = "m_tom_2",
            competition = "الدوري الإنجليزي الممتاز",
            homeTeam = "مانشستر يونايتد",
            homeTeamLogoUrl = "https://picsum.photos/seed/manutdlogo/80/80",
            awayTeam = "تشيلسي",
            awayTeamLogoUrl = "https://picsum.photos/seed/chelsealogo/80/80",
            homeScore = null,
            awayScore = null,
            status = MatchStatus.UPCOMING,
            startTimeLabel = "غداً 19:30",
            sport = "Football",
            channel = "beIN Sports 1 HD",
            commentator = "حفيظ دراجي",
            roundOrStadium = "الجولة 28 • أولد ترافورد",
            dateCategory = "tomorrow"
        ),
        Match(
            id = "m_tom_3",
            competition = "دوري روشن السعودي للمحترفين",
            homeTeam = "الشباب",
            homeTeamLogoUrl = "https://picsum.photos/seed/shabablogo/80/80",
            awayTeam = "التعاون",
            awayTeamLogoUrl = "https://picsum.photos/seed/taawonlogo/80/80",
            homeScore = null,
            awayScore = null,
            status = MatchStatus.UPCOMING,
            startTimeLabel = "غداً 18:00",
            sport = "Football",
            channel = "SSC 2 HD",
            commentator = "عيسى الحربين",
            roundOrStadium = "الجولة 22 • ملعب نادي الشباب",
            dateCategory = "tomorrow"
        )
    )

    private val leagueStandings = listOf(
        LeagueStanding(
            leagueId = "649",
            leagueName = "دوري روشن السعودي للمحترفين",
            season = "2025/2026",
            rows = listOf(
                StandingRow(1, "الهلال", "https://picsum.photos/seed/hilallogo/80/80", 22, 19, 3, 0, 48, 60, listOf("W", "W", "W", "W", "D")),
                StandingRow(2, "النصر", "https://picsum.photos/seed/nassrlogo/80/80", 22, 17, 2, 3, 36, 53, listOf("W", "W", "D", "W", "W")),
                StandingRow(3, "الأهلي", "https://picsum.photos/seed/ahlilogo/80/80", 22, 14, 5, 3, 24, 47, listOf("W", "L", "W", "W", "D")),
                StandingRow(4, "الاتحاد", "https://picsum.photos/seed/ittihadlogo/80/80", 22, 13, 4, 5, 19, 43, listOf("W", "W", "W", "L", "W")),
                StandingRow(5, "التعاون", "https://picsum.photos/seed/taawonlogo/80/80", 22, 11, 6, 5, 11, 39, listOf("D", "W", "L", "W", "D")),
                StandingRow(6, "الشباب", "https://picsum.photos/seed/shabablogo/80/80", 22, 9, 5, 8, 4, 32, listOf("L", "W", "D", "L", "W")),
                StandingRow(7, "الاتفاق", "https://picsum.photos/seed/ettifaqlogo/80/80", 22, 8, 7, 7, 2, 31, listOf("D", "D", "W", "L", "D")),
                StandingRow(8, "الفتح", "https://picsum.photos/seed/fatehlogo/80/80", 22, 8, 6, 8, -1, 30, listOf("L", "L", "W", "D", "W"))
            )
        ),
        LeagueStanding(
            leagueId = "7",
            leagueName = "الدوري الإنجليزي الممتاز",
            season = "2025/2026",
            rows = listOf(
                StandingRow(1, "ليفربول", "https://picsum.photos/seed/lfclogo/80/80", 27, 20, 4, 3, 38, 64, listOf("W", "W", "W", "D", "W")),
                StandingRow(2, "مانشستر سيتي", "https://picsum.photos/seed/mcfclogo/80/80", 27, 19, 5, 3, 35, 62, listOf("W", "W", "D", "W", "W")),
                StandingRow(3, "أرسنال", "https://picsum.photos/seed/afclogo/80/80", 27, 19, 4, 4, 39, 61, listOf("W", "W", "W", "W", "L")),
                StandingRow(4, "أستون فيلا", "https://picsum.photos/seed/avfclogo/80/80", 27, 16, 4, 7, 18, 52, listOf("L", "W", "W", "D", "W")),
                StandingRow(5, "توتنهام", "https://picsum.photos/seed/totlogo/80/80", 27, 15, 5, 7, 16, 50, listOf("W", "L", "W", "D", "W")),
                StandingRow(6, "مانشستر يونايتد", "https://picsum.photos/seed/manutdlogo/80/80", 27, 14, 2, 11, -2, 44, listOf("W", "W", "L", "W", "L")),
                StandingRow(7, "نيوكاسل", "https://picsum.photos/seed/newcastlelogo/80/80", 27, 12, 4, 11, 12, 40, listOf("L", "W", "D", "L", "W")),
                StandingRow(8, "تشيلسي", "https://picsum.photos/seed/chelsealogo/80/80", 27, 10, 6, 11, 4, 36, listOf("D", "L", "W", "D", "L"))
            )
        ),
        LeagueStanding(
            leagueId = "11",
            leagueName = "الدوري الإسباني",
            season = "2025/2026",
            rows = listOf(
                StandingRow(1, "ريال مدريد", "https://picsum.photos/seed/rmlogo/80/80", 26, 20, 5, 1, 38, 65, listOf("W", "W", "D", "W", "W")),
                StandingRow(2, "برشلونة", "https://picsum.photos/seed/barcalogo/80/80", 26, 18, 4, 4, 24, 58, listOf("W", "W", "W", "D", "W")),
                StandingRow(3, "جيرونا", "https://picsum.photos/seed/gironalogo/80/80", 26, 17, 5, 4, 22, 56, listOf("L", "W", "L", "W", "D")),
                StandingRow(4, "أتلتيكو مدريد", "https://picsum.photos/seed/atletilogo/80/80", 26, 16, 4, 6, 20, 52, listOf("W", "L", "W", "D", "W")),
                StandingRow(5, "أتلتيك بيلباو", "https://picsum.photos/seed/bilbaologo/80/80", 26, 14, 7, 5, 17, 49, listOf("D", "W", "W", "D", "W")),
                StandingRow(6, "ريال بيتيس", "https://picsum.photos/seed/betislogo/80/80", 26, 10, 12, 4, 5, 42, listOf("D", "D", "W", "D", "L"))
            )
        ),
        LeagueStanding(
            leagueId = "572",
            leagueName = "دوري أبطال أوروبا",
            season = "2025/2026",
            rows = listOf(
                StandingRow(1, "ريال مدريد", "https://picsum.photos/seed/rmlogo/80/80", 8, 7, 1, 0, 15, 22, listOf("W", "W", "W", "W", "W")),
                StandingRow(2, "مانشستر سيتي", "https://picsum.photos/seed/mcfclogo/80/80", 8, 6, 2, 0, 14, 20, listOf("W", "W", "D", "W", "W")),
                StandingRow(3, "بايرن ميونخ", "https://picsum.photos/seed/bayernlogo/80/80", 8, 6, 1, 1, 12, 19, listOf("W", "L", "W", "W", "W")),
                StandingRow(4, "ليفربول", "https://picsum.photos/seed/lfclogo/80/80", 8, 6, 0, 2, 10, 18, listOf("W", "W", "W", "L", "W")),
                StandingRow(5, "باريس سان جيرمان", "https://picsum.photos/seed/psglogo/80/80", 8, 5, 1, 2, 8, 16, listOf("L", "W", "D", "W", "W")),
                StandingRow(6, "برشلونة", "https://picsum.photos/seed/barcalogo/80/80", 8, 5, 1, 2, 7, 16, listOf("W", "W", "L", "W", "D"))
            )
        )
    )

    override suspend fun getHero(): HeroContent = withContext(Dispatchers.IO) {
        val movies = getMovies()
        if (movies.isNotEmpty()) {
            val top = movies.first()
            HeroContent(
                id = top.id,
                title = top.title,
                description = top.description.ifEmpty { "شاهد أحدث إنتاجات السينما العالمية بدقة 4K فائقة الوضوح." },
                category = "${top.genre} • ${top.year} • ${top.quality}",
                backdropUrl = top.backdropUrl.ifEmpty { top.posterUrl },
                streamUrl = top.streamUrl
            )
        } else {
            HeroContent(
                id = "hero_default",
                title = "مكتبة السينما والبث المباشر 2026",
                description = "آلاف الأفلام والمسلسلات الحصرية وقنوات البث الرياضية والترفيهية بجودة عالية.",
                category = "سينما • 2026 • 4K HDR",
                backdropUrl = "https://picsum.photos/seed/hero2026/1200/675",
                streamUrl = ""
            )
        }
    }

    override suspend fun getContinueWatching(): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        if (context != null) {
            val realHistory = PlaybackHistoryManager.getContinueWatchingList(context)
            if (realHistory.isNotEmpty()) {
                return@withContext realHistory
            }
        }
        emptyList()
    }

    override suspend fun getChannels(category: String): List<Channel> = withContext(Dispatchers.IO) {
        syncRemoteChannels(forceRefresh = false)

        val filtered = ContentFilterEngine.filterChannels(cachedChannels)
        val channelsWithFavs = filtered.map { ch ->
            val isFav = if (context != null) FavoritesManager.isChannelFavorite(context, ch.id) else ch.isFavorite
            ch.copy(isFavorite = isFav)
        }

        if (category == "All" || category.isBlank()) {
            channelsWithFavs
        } else {
            channelsWithFavs.filter {
                it.category.contains(category, ignoreCase = true) ||
                (category.contains("عراق", true) && (it.category.contains("عراق", true) || it.name.contains("عراق", true) || it.name.contains("دجلة", true) || it.name.contains("الشرقية", true) || it.name.contains("السومرية", true) || it.name.contains("سامراء", true) || it.name.contains("رووداو", true) || it.name.contains("زاكروس", true) || it.name.contains("عشتار", true) || it.name.contains("آي نيوز", true))) ||
                (category.contains("سعود", true) && (it.category.contains("سعود", true) || it.category.contains("KSA", true) || it.name.contains("السعودية", true) || it.name.contains("KSA", true) || it.name.contains("القرآن الكريم", true) || it.name.contains("السنة النبوية", true) || it.name.contains("الإخبارية", true) || it.name.contains("SBC", true))) ||
                (category.contains("مصر", true) && (it.category.contains("مصر", true) || it.name.contains("مصر", true) || it.name.contains("DMC", true) || it.name.contains("CBC", true) || it.name.contains("ON E", true) || it.name.contains("أون تايم", true))) ||
                (category.contains("روتانا", true) && (it.category.contains("روتانا", true) || it.name.contains("روتانا", true) || it.name.contains("الرسالة", true))) ||
                (category.contains("MBC", true) && (it.category.contains("MBC", true) || it.name.contains("MBC", true) || it.category.contains("ام بى سى", true) || it.category.contains("ام بي سي", true) || it.category.contains("شاهد", true))) ||
                (category.equals("Sports", true) && (it.category.contains("Sports", true) || it.category.contains("beIN", true) || it.category.contains("رياض", true) || it.category.contains("الكأس", true) || it.category.contains("ثمانية", true) || it.category.contains("SSC", true) || it.name.contains("bein", true) || it.name.contains("ssc", true) || it.name.contains("سبورت", true) || it.name.contains("كأس", true) || it.name.contains("أبوظبي", true) || it.name.contains("رياض", true))) ||
                (category.equals("News", true) && (it.category.contains("News", true) || it.category.contains("إخبار", true) || it.category.contains("الحدث", true) || it.name.contains("News", true) || it.name.contains("إخبار", true))) ||
                (category.equals("Kids", true) && (it.category.contains("Kids", true) || it.category.contains("أطفال", true) || it.category.contains("Spacetoon", true) || it.name.contains("كرتون", true) || it.name.contains("اطفال", true))) ||
                (category.equals("Movies", true) && (it.category.contains("Movies", true) || it.category.contains("أفلام", true) || it.category.contains("سينما", true) || it.name.contains("Movie", true) || it.name.contains("OSN", true))) ||
                (category.equals("Entertainment", true) && (it.category.contains("Entertainment", true) || it.category.contains("MBC", true) || it.category.contains("ألوان", true) || it.category.contains("شاهد", true) || it.category.contains("ترفيه", true) || it.category.contains("OSN", true))) ||
                (category.contains("OSN", true) && (it.category.contains("OSN", true) || it.name.contains("OSN", true))) ||
                ((category.contains("beIN Movies", true) || category.contains("باقة بي ان الترفيهية", true)) && ((it.category.contains("beIN", true) || it.name.contains("beIN", true)) && (it.category.contains("Movie", true) || it.name.contains("Movie", true) || it.category.contains("ترفيه", true) || it.name.contains("Series", true) || it.name.contains("Drama", true)))) ||
                (category.contains("Netflix", true) && (it.category.contains("Netflix", true) || it.name.contains("Netflix", true))) ||
                (category.contains("شاهد", true) && (it.category.contains("شاهد", true) || it.name.contains("شاهد", true)))
            }
        }
    }

    override suspend fun getMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val currentLang = LanguageManager.currentLanguage.value
        val contextRef = getValidContext()

        if (cachedMovies.isNotEmpty()) {
            val unEnrichedBanner = cachedMovies.take(8).filter { it.backdropUrl == it.posterUrl }
            if (unEnrichedBanner.isNotEmpty()) {
                enrichFeaturedMovies(unEnrichedBanner, currentLang)
            }
            val filtered = ContentFilterEngine.filterMovies(cachedMovies)
            return@withContext filtered.map { m ->
                val localized = ImdbMetadataEngine.localizeMovie(m, currentLang)
                val isFav = if (contextRef != null) {
                    FavoritesManager.isMovieFavorite(contextRef, m.id) || FavoritesManager.isMovieFavorite(contextRef, movieRealIds[m.id] ?: "")
                } else m.isFavorite
                localized.copy(isFavorite = isFav)
            }
        }

        try {
            val movieCategories = listOf("افلام اجنبي", "افلام عربي", "افلام تركية", "افلام اسيوية", "افلام انمي", "افلام هندي")
            val jobs = movieCategories.map { cat ->
                async {
                    try {
                        client.getItems(page = 1, perPage = 20, categorySlug = cat)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            val allResults = jobs.awaitAll().flatten()

            if (allResults.isNotEmpty()) {
                val list = mutableListOf<Movie>()
                for (item in allResults) {
                    if (list.none { it.id == item.id || movieRealIds[it.id] == item.realId }) {
                        movieRealIds[item.id] = item.realId
                        val displayTitle = ImdbMetadataEngine.cleanDisplayTitle(item.title)
                        if (ContentFilterEngine.isContentBlocked(displayTitle, item.title, item.category ?: "", item.story)) continue

                        val isFav = if (contextRef != null) FavoritesManager.isMovieFavorite(contextRef, item.id, item.realId) else false
                        val cachedEnriched = com.beouttv.app.data.proto.ProtobufDiskCache.loadMovie("${item.id}_${currentLang.code}")
                            ?: com.beouttv.app.data.proto.ProtobufDiskCache.loadMovie(item.id)
                        val m = cachedEnriched?.copy(
                            streamUrl = "https://govid.live/prem-${item.realId}.m3u8",
                            isFavorite = isFav
                        ) ?: Movie(
                            id = item.id,
                            title = displayTitle,
                            originalTitle = item.title,
                            posterUrl = item.thumbnail,
                            backdropUrl = item.thumbnail,
                            year = item.releaseYear,
                            rating = item.rating,
                            imdbRatingFormatted = "${item.rating}/10",
                            quality = item.quality,
                            duration = "115 دقيقة",
                            genre = if (item.genre.isNotBlank()) item.genre else (item.category ?: "أفلام"),
                            category = item.category ?: "أفلام",
                            genresList = if (item.genre.isNotBlank()) item.genre.split("•", ",", "/").map { it.trim() } else listOf("أفلام"),
                            description = if (item.story.isNotBlank()) item.story else "مشاهدة وتحميل $displayTitle مترجم بجودة عالية.",
                            isFavorite = isFav,
                            streamUrl = "https://govid.live/prem-${item.realId}.m3u8",
                            country = item.country,
                            director = item.director,
                            cast = item.actors,
                            language = item.language
                        )
                        list.add(ImdbMetadataEngine.localizeMovie(m, currentLang))
                    }
                }

                if (list.isNotEmpty()) {
                    cachedMovies.clear()
                    cachedMovies.addAll(list)
                    _moviesFlow.value = cachedMovies.toList()

                    // Proactively enrich top banner movies in background so high-res secondary backdrops appear immediately
                    enrichFeaturedMovies(list.take(8), currentLang)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedMovies
    }

    private fun enrichFeaturedMovies(moviesToEnrich: List<Movie>, lang: com.beouttv.app.data.i18n.AppLanguage) {
        repoScope.launch(Dispatchers.IO) {
            var anyUpdated = false
            moviesToEnrich.forEach { movie ->
                try {
                    val enriched = ImdbMetadataEngine.enrichMovie(movie, lang)
                    val index = cachedMovies.indexOfFirst { it.id == movie.id }
                    if (index != -1 && (cachedMovies[index].backdropUrl != enriched.backdropUrl || cachedMovies[index].posterUrl != enriched.posterUrl)) {
                        val currentStream = cachedMovies[index].streamUrl
                        val currentFav = cachedMovies[index].isFavorite
                        cachedMovies[index] = enriched.copy(
                            streamUrl = if (enriched.streamUrl.isNotBlank()) enriched.streamUrl else currentStream,
                            isFavorite = currentFav
                        )
                        anyUpdated = true
                    }
                } catch (_: Exception) {}
            }
            if (anyUpdated) {
                _moviesFlow.value = cachedMovies.toList()
            }
        }
    }

    override suspend fun getSeries(): List<Series> = withContext(Dispatchers.IO) {
        val contextRef = getValidContext()

        if (cachedSeries.isNotEmpty()) {
            val filtered = ContentFilterEngine.filterSeries(cachedSeries)
            return@withContext filtered.map { s ->
                val isFav = if (contextRef != null) FavoritesManager.isSeriesFavorite(contextRef, s.id) else s.isFavorite
                s.copy(isFavorite = isFav)
            }
        }

        try {
            val seriesCategories = listOf("مسلسلات اجنبي", "مسلسلات عربي", "مسلسلات تركية", "مسلسلات اسيوية", "مسلسلات انمي", "مسلسلات رمضان 2026")
            val jobs = seriesCategories.map { cat ->
                async {
                    try {
                        client.getItems(page = 1, perPage = 20, categorySlug = cat)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            val allResults = jobs.awaitAll().flatten()

            if (allResults.isNotEmpty()) {
                val list = mutableListOf<Series>()
                for (item in allResults) {
                    val cleanTitle = item.title
                        .replace(Regex("(?i)الموسم.*|الحلقة.*|حلقة.*|مترجم.*|مدبلج.*|اون لاين.*"), "")
                        .trim()
                        .ifBlank { item.title }

                    val seriesId = item.seriesSlug?.ifBlank { null } ?: cleanTitle.lowercase().replace(" ", "-")

                    if (list.none { it.id == seriesId || it.title.equals(cleanTitle, ignoreCase = true) }) {
                        val displayTitle = ImdbMetadataEngine.cleanDisplayTitle(cleanTitle)
                        if (ContentFilterEngine.isContentBlocked(displayTitle, item.title, item.category ?: "", item.story)) continue

                        val isFav = if (contextRef != null) FavoritesManager.isSeriesFavorite(contextRef, seriesId) else false
                        list.add(
                            Series(
                                id = seriesId,
                                title = displayTitle,
                                originalTitle = cleanTitle,
                                posterUrl = item.thumbnail,
                                backdropUrl = item.thumbnail,
                                year = item.releaseYear,
                                rating = item.rating,
                                genre = if (item.genre.isNotBlank()) item.genre else (item.category ?: "مسلسلات"),
                                description = if (item.story.isNotBlank()) item.story else "مشاهدة وتحميل مسلسل $displayTitle بجودة عالية وبكافة المواسم والحلقات على BeoutTV.",
                                isFavorite = isFav
                            )
                        )
                    }
                }

                if (list.isNotEmpty()) {
                    cachedSeries.clear()
                    cachedSeries.addAll(list)
                    _seriesFlow.value = cachedSeries.toList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedSeries
    }

    fun clearSourceCache() {
        cachedMovies.clear()
        cachedSeries.clear()
        cachedMediaCategories = emptyList()
        movieRealIds.clear()
        _moviesFlow.value = emptyList()
        _seriesFlow.value = emptyList()
    }

    override suspend fun reloadCurrentSourceContent() {
        withContext(Dispatchers.IO) {
            clearSourceCache()
            getCategories()
            getMovies()
            getSeries()
        }
    }

    private var cachedMediaCategories: List<MediaCategory> = emptyList()

    override suspend fun getCategories(): List<MediaCategory> = withContext(Dispatchers.IO) {
        if (cachedMediaCategories.isNotEmpty()) return@withContext cachedMediaCategories
        val movieBoxCategories = listOf(
            MediaCategory(name = "أفلام وان+", slug = "mb_movies", isSeries = false),
            MediaCategory(name = "أنمي وان+", slug = "mb_anime", isSeries = false),
            MediaCategory(name = "الأكثر رواجاً", slug = "mb_trending", isSeries = false),
            MediaCategory(name = "مسلسلات وان+", slug = "mb_series", isSeries = true)
        )
        try {
            val sections = client.getSections()
            if (sections.isNotEmpty()) {
                val list = sections.map { sec ->
                    val cleanName = sec.name.trim()
                    val isSeries = cleanName.contains("مسلسل", ignoreCase = true) ||
                                   cleanName.contains("رمضان", ignoreCase = true) ||
                                   cleanName.contains("برامج", ignoreCase = true)
                    MediaCategory(name = cleanName, slug = sec.slug, isSeries = isSeries)
                }
                cachedMediaCategories = movieBoxCategories + list
                return@withContext cachedMediaCategories
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        movieBoxCategories
    }

    override suspend fun getMoviesByCategory(categorySlug: String, page: Int): List<Movie> {
        val defaultCount = if (categorySlug.startsWith("mb_")) 20 else 30
        return getMoviesPaged(categorySlug, page, defaultCount)
    }

    override suspend fun getMoviesPaged(categorySlug: String, page: Int, perPage: Int): List<Movie> = withContext(Dispatchers.IO) {
        val currentLang = LanguageManager.currentLanguage.value
        val contextRef = getValidContext()

        // MovieBox category handling
        if (categorySlug.startsWith("mb_")) {
            val safePerPage = perPage.coerceIn(1, 20)
            val mbItems = try {
                when (categorySlug) {
                    "mb_anime" -> com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getAnime(page = page, perPage = safePerPage)
                    "mb_trending" -> com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getTrending(page = page, perPage = safePerPage)
                    else -> com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getMovies(page = page, perPage = safePerPage)
                }
            } catch (_: Exception) {
                emptyList()
            }
            val mapped = mbItems.map { item ->
                val m = com.beouttv.app.data.api.moviebox.MovieBoxMapper.toMovie(item)
                if (cachedMovies.none { it.id == m.id }) {
                    cachedMovies.add(m)
                }
                val isFav = if (contextRef != null) FavoritesManager.isMovieFavorite(contextRef, m.id) else m.isFavorite
                m.copy(isFavorite = isFav)
            }
            return@withContext mapped
        }

        // 1. If browsing "all", utilize cachedMovies for instant, comprehensive results without API type=movie filter dropouts
        if (categorySlug == "all" || categorySlug.isBlank()) {
            if (cachedMovies.isEmpty()) {
                try { getMovies() } catch (_: Exception) {}
            }
            if (cachedMovies.isNotEmpty()) {
                val filtered = ContentFilterEngine.filterMovies(cachedMovies)
                val fromIndex = (page - 1) * perPage
                if (fromIndex < filtered.size) {
                    val paged = filtered.drop(fromIndex).take(perPage)
                    return@withContext paged.map { m ->
                        val localized = ImdbMetadataEngine.localizeMovie(m, currentLang)
                        val isFav = if (contextRef != null) {
                            FavoritesManager.isMovieFavorite(contextRef, m.id) || FavoritesManager.isMovieFavorite(contextRef, movieRealIds[m.id] ?: "")
                        } else m.isFavorite
                        localized.copy(isFavorite = isFav)
                    }
                }
            }
        }

        try {
            val remoteItems = if (categorySlug == "all" || categorySlug.isBlank()) {
                client.getItems(page = page, perPage = perPage, type = "movies")
            } else {
                client.getItems(page = page, perPage = perPage, categorySlug = categorySlug)
            }

            val list = mutableListOf<Movie>()
            for (item in remoteItems) {
                // If browsing all movies, filter out series/episodes
                if ((categorySlug == "all" || categorySlug.isBlank()) && (item.isSeries || item.title.contains("مسلسل") || item.title.contains("الحلقة"))) {
                    continue
                }

                movieRealIds[item.id] = item.realId
                val displayTitle = ImdbMetadataEngine.cleanDisplayTitle(item.title)
                if (ContentFilterEngine.isContentBlocked(displayTitle, item.title, item.category ?: "", item.story)) continue

                val isFav = if (contextRef != null) FavoritesManager.isMovieFavorite(contextRef, item.id, item.realId) else false
                val m = Movie(
                    id = item.id,
                    title = displayTitle,
                    originalTitle = item.title,
                    posterUrl = item.thumbnail,
                    backdropUrl = item.thumbnail,
                    year = item.releaseYear,
                    rating = item.rating,
                    imdbRatingFormatted = "${item.rating}/10",
                    quality = item.quality,
                    duration = "115 دقيقة",
                    genre = if (item.genre.isNotBlank()) item.genre else (item.category ?: "أفلام"),
                    category = item.category ?: "أفلام",
                    genresList = if (item.genre.isNotBlank()) item.genre.split("•", ",", "/").map { it.trim() } else listOf("أفلام"),
                    description = if (item.story.isNotBlank()) item.story else "مشاهدة وتحميل $displayTitle مترجم بجودة عالية.",
                    isFavorite = isFav,
                    streamUrl = "https://govid.live/prem-${item.realId}.m3u8",
                    country = item.country,
                    director = item.director,
                    cast = item.actors,
                    language = item.language
                )
                list.add(ImdbMetadataEngine.localizeMovie(m, currentLang))
            }

            for (m in list) {
                if (cachedMovies.none { it.id == m.id }) {
                    cachedMovies.add(m)
                }
            }
            if (page == 1 && (categorySlug == "all" || categorySlug.isBlank()) && list.isNotEmpty()) {
                _moviesFlow.value = cachedMovies.toList()
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getSeriesByCategory(categorySlug: String, page: Int): List<Series> {
        val defaultCount = if (categorySlug.startsWith("mb_")) 20 else 30
        return getSeriesPaged(categorySlug, page, defaultCount)
    }

    override suspend fun getSeriesPaged(categorySlug: String, page: Int, perPage: Int): List<Series> = withContext(Dispatchers.IO) {
        val contextRef = getValidContext()

        // MovieBox Series category handling
        if (categorySlug == "mb_series" || categorySlug.startsWith("mb_")) {
            val safePerPage = perPage.coerceIn(1, 20)
            val mbItems = try {
                com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getSeries(page = page, perPage = safePerPage)
            } catch (_: Exception) {
                emptyList()
            }
            val mapped = mbItems.map { item ->
                val s = com.beouttv.app.data.api.moviebox.MovieBoxMapper.toSeries(item)
                if (cachedSeries.none { it.id == s.id }) {
                    cachedSeries.add(s)
                }
                val isFav = if (contextRef != null) FavoritesManager.isSeriesFavorite(contextRef, s.id) else s.isFavorite
                s.copy(isFavorite = isFav)
            }
            return@withContext mapped
        }

        // 1. If browsing "all", utilize cachedSeries for instant, comprehensive results
        if (categorySlug == "all" || categorySlug.isBlank()) {
            if (cachedSeries.isEmpty()) {
                try { getSeries() } catch (_: Exception) {}
            }
            if (cachedSeries.isNotEmpty()) {
                val filtered = ContentFilterEngine.filterSeries(cachedSeries)
                val fromIndex = (page - 1) * perPage
                if (fromIndex < filtered.size) {
                    val paged = filtered.drop(fromIndex).take(perPage)
                    return@withContext paged.map { s ->
                        val isFav = if (contextRef != null) FavoritesManager.isSeriesFavorite(contextRef, s.id) else s.isFavorite
                        s.copy(isFavorite = isFav)
                    }
                }
            }
        }

        try {
            val remoteItems = if (categorySlug == "all" || categorySlug.isBlank()) {
                client.getItems(page = page, perPage = perPage, type = "series")
            } else {
                client.getItems(page = page, perPage = perPage, categorySlug = categorySlug)
            }

            val list = mutableListOf<Series>()
            for (item in remoteItems) {
                val cleanTitle = item.title
                    .replace(Regex("(?i)الموسم.*|الحلقة.*|حلقة.*|مترجم.*|مدبلج.*|اون لاين.*"), "")
                    .trim()
                    .ifBlank { item.title }

                val seriesId = item.seriesSlug?.ifBlank { null } ?: cleanTitle.lowercase().replace(" ", "-")

                if (list.none { it.id == seriesId || it.title.equals(cleanTitle, ignoreCase = true) }) {
                    val displayTitle = ImdbMetadataEngine.cleanDisplayTitle(cleanTitle)
                    if (ContentFilterEngine.isContentBlocked(displayTitle, item.title, item.category ?: "", item.story)) continue

                    val isFav = if (contextRef != null) FavoritesManager.isSeriesFavorite(contextRef, seriesId) else false
                    list.add(
                        Series(
                            id = seriesId,
                            title = displayTitle,
                            originalTitle = cleanTitle,
                            posterUrl = item.thumbnail,
                            backdropUrl = item.thumbnail,
                            year = item.releaseYear,
                            rating = item.rating,
                            genre = if (item.genre.isNotBlank()) item.genre else (item.category ?: "مسلسلات"),
                            description = if (item.story.isNotBlank()) item.story else "مشاهدة وتحميل مسلسل $displayTitle بجودة عالية وبكافة المواسم والحلقات على BeoutTV.",
                            isFavorite = isFav
                        )
                    )
                }
            }

            for (s in list) {
                if (cachedSeries.none { it.id == s.id }) {
                    cachedSeries.add(s)
                }
            }
            if (page == 1 && (categorySlug == "all" || categorySlug.isBlank()) && list.isNotEmpty()) {
                _seriesFlow.value = cachedSeries.toList()
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val sportsClient = com.beouttv.app.data.api.SportsApiClient()

    override suspend fun getMatches(): List<Match> = withContext(Dispatchers.IO) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        getMatchesByDate(todayStr)
    }

    override suspend fun getMatchesByDate(date: String): List<Match> = withContext(Dispatchers.IO) {
        try {
            val remoteMatches = sportsClient.getMatchesByDate(date)
            if (remoteMatches.isNotEmpty()) {
                if (cachedChannels.isEmpty()) {
                    syncRemoteChannels(forceRefresh = false)
                }
                val enrichedMatches = remoteMatches.map { match ->
                    val matchedChannel = if (!match.channel.isNullOrBlank()) {
                        cachedChannels.find { ch -> ch.name.contains(match.channel!!, true) }
                    } else {
                        when {
                            match.competition.contains("روشن", true) || match.competition.contains("السعودي", true) ->
                                cachedChannels.find { ch -> ch.name.contains("SSC 1", true) || ch.name.contains("SSC", true) }
                            match.competition.contains("العراق", true) ->
                                cachedChannels.find { ch -> ch.name.contains("الرابعة", true) || ch.name.contains("Rabiaa", true) }
                            match.competition.contains("أبطال أوروبا", true) || match.competition.contains("الإسباني", true) || match.competition.contains("الإنجليزي", true) ->
                                cachedChannels.find { ch -> ch.name.contains("beIN Sports 1", true) }
                            else -> null
                        }
                    }
                    if (matchedChannel != null) {
                        match.copy(
                            channel = matchedChannel.name,
                            streamUrl = matchedChannel.streamUrl
                        )
                    } else {
                        match
                    }
                }
                return@withContext enrichedMatches
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val dateCat = when {
            date == todayStr -> "today"
            date < todayStr -> "yesterday"
            else -> "tomorrow"
        }
        val filteredFallback = liveMatches.filter { it.dateCategory == dateCat }
        if (filteredFallback.isNotEmpty()) filteredFallback else liveMatches
    }

    override suspend fun getStandings(): List<LeagueStanding> = withContext(Dispatchers.IO) {
        leagueStandings
    }

    override suspend fun getStandingsByLeague(leagueId: String, leagueName: String): LeagueStanding? = withContext(Dispatchers.IO) {
        try {
            val remoteStanding = sportsClient.getLeagueStandings(leagueId, leagueName)
            if (remoteStanding != null && remoteStanding.rows.isNotEmpty()) {
                return@withContext remoteStanding
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        leagueStandings.find { it.leagueId == leagueId }
    }

    override suspend fun getTeamsForLeague(leagueId: String): List<ClubInfo> = withContext(Dispatchers.IO) {
        try {
            val teams = sportsClient.getTeamsForLeague(leagueId)
            if (teams.isNotEmpty()) return@withContext teams
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    override suspend fun getTeamFixtures(teamId: String, teamName: String): List<Match> = withContext(Dispatchers.IO) {
        try {
            val fixtures = sportsClient.getTeamFixtures(teamId, teamName)
            if (fixtures.isNotEmpty()) return@withContext fixtures
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val todayMatches = getMatches()
            val teamMatches = todayMatches.filter {
                val compLower = it.competition.lowercase()
                val isWomensOrYouth = compLower.contains("سيدات") || compLower.contains("women") || compLower.contains("femenin") ||
                                      compLower.contains("تحت 21") || compLower.contains("تحت 19") || compLower.contains("تحت 23") ||
                                      compLower.contains("u21") || compLower.contains("u19") || compLower.contains("u23") || compLower.contains("شباب")
                !isWomensOrYouth && (it.homeTeam.contains(teamName, ignoreCase = true) || it.awayTeam.contains(teamName, ignoreCase = true))
            }
            if (teamMatches.isNotEmpty()) return@withContext teamMatches
        } catch (_: Exception) {}
        emptyList()
    }

    override suspend fun getTeamSquad(teamId: String, teamName: String): List<PlayerInfo> = withContext(Dispatchers.IO) {
        try {
            val squad = sportsClient.getTeamSquad(teamId, teamName)
            if (squad.isNotEmpty()) return@withContext squad
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    override suspend fun getMovieById(id: String): Movie? = withContext(Dispatchers.IO) {
        if (id.startsWith("xtream_")) {
            val xtreamMovie = com.beouttv.app.data.xtream.XtreamAccountManager.getMovieById(id)
            if (xtreamMovie != null) {
                return@withContext com.beouttv.app.data.xtream.XtreamAccountManager.getMovieWithDetails(xtreamMovie)
            }
        }
        if (id.startsWith("mb_")) {
            val fallback = cachedMovies.find { it.id == id }
            val detail = try {
                com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getSubjectDetail(id)
            } catch (_: Exception) { null }

            if (detail != null) {
                var movie = com.beouttv.app.data.api.moviebox.MovieBoxMapper.toDetailedMovie(detail, fallback)
                val currentLang = LanguageManager.currentLanguage.value
                val isFav = if (context != null) FavoritesManager.isMovieFavorite(context, movie.id) else movie.isFavorite

                val enriched = ImdbMetadataEngine.enrichMovie(movie, currentLang)
                var targetImdbId = enriched.imdbId ?: movie.imdbId
                var targetTmdbId = enriched.tmdbId ?: movie.tmdbId

                if (targetImdbId == null) {
                    val cleanQ = ImdbMetadataEngine.cleanSearchTitle(movie.originalTitle ?: movie.title)
                    val sug = ImdbMetadataEngine.searchImdbSuggestion(cleanQ, movie.year)
                    val foundId = sug?.optString("id")?.takeIf { it.startsWith("tt") }
                    if (foundId != null) targetImdbId = foundId
                }
                if (targetTmdbId == null && !targetImdbId.isNullOrBlank()) {
                    val cin = ImdbMetadataEngine.fetchCinemetaMeta(targetImdbId)
                    val cinTmdb = cin?.optString("moviedb_id", "")?.filter { it.isDigit() }?.ifBlank { null }
                    if (cinTmdb != null) targetTmdbId = cinTmdb
                }

                var finalServers = if (movie.servers.isNotEmpty()) movie.servers else enriched.servers
                var finalStream = if (movie.streamUrl.isNotBlank()) movie.streamUrl else enriched.streamUrl
                var finalSubs = (movie.subtitles + enriched.subtitles).distinctBy { it.url }

                val srvs = client.generateStreamServers(realId = id, tmdbId = targetTmdbId, imdbId = targetImdbId, isSeries = false)
                val subs = if (finalSubs.any { it.language == "ar" }) {
                    emptyList() // MovieBox already provides official, frame-perfect synchronized Arabic subtitles
                } else {
                    client.fetchSubtitles(tmdbId = targetTmdbId, imdbId = targetImdbId, query = movie.title)
                }
                finalServers = (finalServers + srvs).distinctBy { it.url }
                finalSubs = (finalSubs + subs).distinctBy { it.url }.sortedWith(
                    compareByDescending<SubtitleTrack> { it.language == "ar" && (it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox")) }
                        .thenByDescending { it.language == "ar" }
                        .thenByDescending { it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox") }
                )
                if (finalStream.isBlank()) {
                    finalStream = client.resolveMultiSourceStream(realId = id, tmdbId = targetTmdbId, imdbId = targetImdbId, isSeries = false).ifBlank {
                        finalServers.firstOrNull()?.url.orEmpty()
                    }
                }

                val finalMovie = enriched.copy(
                    imdbId = targetImdbId,
                    tmdbId = targetTmdbId,
                    streamUrl = finalStream,
                    servers = finalServers,
                    subtitles = finalSubs,
                    isFavorite = isFav,
                    isSeries = movie.isSeries
                )
                val idx = cachedMovies.indexOfFirst { it.id == id }
                if (idx != -1) cachedMovies[idx] = finalMovie else cachedMovies.add(finalMovie)
                _moviesFlow.value = cachedMovies.toList()
                return@withContext finalMovie
            } else if (fallback != null) {
                var fb = fallback
                val currentLang = LanguageManager.currentLanguage.value
                val enriched = ImdbMetadataEngine.enrichMovie(fb, currentLang)
                var targetImdbId = enriched.imdbId ?: fb.imdbId
                var targetTmdbId = enriched.tmdbId ?: fb.tmdbId
                if (targetImdbId == null) {
                    val cleanQ = ImdbMetadataEngine.cleanSearchTitle(fb.originalTitle ?: fb.title)
                    val sug = ImdbMetadataEngine.searchImdbSuggestion(cleanQ, fb.year)
                    targetImdbId = sug?.optString("id")?.takeIf { it.startsWith("tt") }
                }
                if (targetTmdbId == null && !targetImdbId.isNullOrBlank()) {
                    val cin = ImdbMetadataEngine.fetchCinemetaMeta(targetImdbId)
                    targetTmdbId = cin?.optString("moviedb_id", "")?.filter { it.isDigit() }?.ifBlank { null }
                }
                val srvs = client.generateStreamServers(realId = id, tmdbId = targetTmdbId, imdbId = targetImdbId, isSeries = false)
                val validStream = client.resolveMultiSourceStream(realId = id, tmdbId = targetTmdbId, imdbId = targetImdbId, isSeries = false).ifBlank {
                    srvs.firstOrNull()?.url.orEmpty()
                }
                fb = enriched.copy(
                    imdbId = targetImdbId,
                    tmdbId = targetTmdbId,
                    streamUrl = if (fb.streamUrl.isNotBlank()) fb.streamUrl else validStream,
                    servers = (fb.servers + srvs).distinctBy { it.url }
                )
                return@withContext fb
            }
            return@withContext null
        }
        if (cachedMovies.isEmpty()) {
            getMovies()
        }
        val found = cachedMovies.find { it.id == id || movieRealIds[it.id] == id } ?: run {
            // Attempt to fetch details directly from dynamic API
            val details = client.getItemDetails(id)
            if (details != null) {
                val ratingVal = details.rating
                val m = Movie(
                    id = details.id,
                    title = ImdbMetadataEngine.cleanDisplayTitle(details.title),
                    posterUrl = details.thumbnail,
                    backdropUrl = details.thumbnail,
                    year = details.releaseYear,
                    rating = ratingVal,
                    imdbRatingFormatted = if (ratingVal > 0f) "$ratingVal/10" else "",
                    genre = details.genre,
                    description = details.story.ifBlank { "مشاهدة وتحميل ${details.title} بجودة عالية." },
                    streamUrl = "https://govid.live/prem-${details.realId}.m3u8",
                    originalTitle = details.title,
                    country = details.country,
                    director = details.director,
                    cast = details.actors
                )
                cachedMovies.add(m)
                movieRealIds[details.id] = details.realId
                m
            } else null
        } ?: return@withContext null

        val targetRealId = movieRealIds[id] ?: movieRealIds[found.id] ?: found.id

        // Multi-Source Stream Resolution (WeCima -> VidLink -> Videasy -> ZxcStream -> 2Embed)
        val validStream = client.resolveMultiSourceStream(
            realId = targetRealId,
            tmdbId = found.tmdbId,
            imdbId = found.imdbId,
            isSeries = false
        )

        // Subtitles Fetching (SubDL + OpenSubtitles + Wyzie)
        val fetchedSubtitles = client.fetchSubtitles(
            tmdbId = found.tmdbId,
            imdbId = found.imdbId,
            query = found.title
        )

        // Real Multi-Source Servers List
        val serversList = client.generateStreamServers(
            realId = targetRealId,
            tmdbId = found.tmdbId,
            imdbId = found.imdbId,
            isSeries = false
        )

        val isFav = if (context != null) {
            FavoritesManager.isMovieFavorite(context, found.id, targetRealId)
        } else found.isFavorite

        val movieWithStream = found.copy(
            streamUrl = validStream,
            subtitles = fetchedSubtitles,
            servers = serversList,
            isFavorite = isFav
        )

        val currentLang = LanguageManager.currentLanguage.value
        val enriched = ImdbMetadataEngine.enrichMovie(movieWithStream, currentLang)
        val finalEnriched = enriched.copy(
            streamUrl = validStream,
            subtitles = fetchedSubtitles,
            servers = serversList,
            isFavorite = isFav
        )

        val index = cachedMovies.indexOfFirst { it.id == found.id }
        if (index != -1) {
            cachedMovies[index] = finalEnriched
            _moviesFlow.value = cachedMovies.toList()
        }

        context?.let { ctx ->
            repoScope.launch {
                com.beouttv.app.data.proto.ProtobufDiskCache.saveMovie(finalEnriched.id, finalEnriched)
            }
        }

        finalEnriched
    }

    override fun cacheDiscoveredSeries(series: Series) {
        val idx = cachedSeries.indexOfFirst { it.id == series.id || it.title.equals(series.title, ignoreCase = true) }
        if (idx != -1) {
            cachedSeries[idx] = series
        } else {
            cachedSeries.add(series)
        }
        _seriesFlow.value = cachedSeries.toList()
    }

    override fun cacheDiscoveredMovie(movie: Movie) {
        val idx = cachedMovies.indexOfFirst { it.id == movie.id || it.title.equals(movie.title, ignoreCase = true) }
        if (idx != -1) {
            cachedMovies[idx] = movie
        } else {
            cachedMovies.add(movie)
        }
        _moviesFlow.value = cachedMovies.toList()
    }

    override suspend fun getSeriesById(id: String): Series? = withContext(Dispatchers.IO) {
        if (id.startsWith("xtream_")) {
            val xtreamSeries = com.beouttv.app.data.xtream.XtreamAccountManager.getSeriesById(id)
            if (xtreamSeries != null) {
                return@withContext com.beouttv.app.data.xtream.XtreamAccountManager.getSeriesWithEpisodes(xtreamSeries)
            }
        }
        if (id.startsWith("mb_")) {
            val fallback = cachedSeries.find { it.id == id }
            val detail = try {
                com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getSubjectDetail(id)
            } catch (_: Exception) { null }

            if (detail != null) {
                var seriesObj = com.beouttv.app.data.api.moviebox.MovieBoxMapper.toDetailedSeries(detail, fallback)
                val isFav = if (context != null) FavoritesManager.isSeriesFavorite(context, seriesObj.id) else seriesObj.isFavorite

                // Search IMDb / Cinemeta for rich season and episode metadata and multi-source streams
                val cleanTitle = ImdbMetadataEngine.cleanSearchTitle(seriesObj.originalTitle ?: seriesObj.title)
                val imdbSug = ImdbMetadataEngine.searchImdbSuggestion(cleanTitle, seriesObj.year)
                val resolvedImdbId = imdbSug?.optString("id")?.takeIf { it.startsWith("tt") } ?: seriesObj.imdbId

                var resolvedTmdbId = seriesObj.tmdbId
                val cinemeta = if (!resolvedImdbId.isNullOrBlank()) {
                    ImdbMetadataEngine.fetchCinemetaSeriesMeta(resolvedImdbId)
                } else null

                if (cinemeta != null) {
                    val tmdbStr = cinemeta.optString("moviedb_id", "").filter { it.isDigit() }
                    if (tmdbStr.isNotBlank()) resolvedTmdbId = tmdbStr
                }

                val seriesSubtitles = client.fetchSubtitles(
                    tmdbId = resolvedTmdbId,
                    imdbId = resolvedImdbId,
                    query = seriesObj.title
                ).sortedWith(
                    compareByDescending<SubtitleTrack> { it.language == "ar" && (it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox")) }
                        .thenByDescending { it.language == "ar" }
                        .thenByDescending { it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox") }
                )

                // Cinemeta episodes map: key is "s_e"
                val cinemetaVideos = mutableMapOf<String, org.json.JSONObject>()
                if (cinemeta != null) {
                    val vArr = cinemeta.optJSONArray("videos")
                    if (vArr != null) {
                        for (vi in 0 until vArr.length()) {
                            val vObj = vArr.optJSONObject(vi) ?: continue
                            val sN = vObj.optInt("season", 1)
                            val eN = vObj.optInt("number", vObj.optInt("episode", 1))
                            cinemetaVideos["${sN}_${eN}"] = vObj
                        }
                    }
                }

                val enrichedSeasons = if (cinemetaVideos.isNotEmpty()) {
                    val validVideos = cinemetaVideos.values.filter { it.optInt("season", 1) > 0 }
                    val seasonMap = validVideos.groupBy { it.optInt("season", 1) }
                    seasonMap.toSortedMap().map { (sNum, vList) ->
                        val sortedEps = vList.sortedBy { it.optInt("number", it.optInt("episode", 1)) }
                        val episodes = sortedEps.map { vObj ->
                            val eNum = vObj.optInt("number", vObj.optInt("episode", 1))
                            val epTitle = vObj?.optString("name")?.takeIf { it.isNotBlank() } ?: "الحلقة $eNum"
                            val epThumb = vObj?.optString("thumbnail")?.takeIf { it.isNotBlank() } ?: seriesObj.backdropUrl

                            val srvs = client.generateStreamServers(
                                realId = "",
                                tmdbId = resolvedTmdbId,
                                imdbId = resolvedImdbId,
                                isSeries = true,
                                seasonNumber = sNum,
                                episodeNumber = eNum
                            )
                            val stream = client.resolveMultiSourceStream(
                                realId = "",
                                tmdbId = resolvedTmdbId,
                                imdbId = resolvedImdbId,
                                isSeries = true,
                                seasonNumber = sNum,
                                episodeNumber = eNum
                            )
                            Episode(
                                id = "${seriesObj.id}_s${sNum}_e$eNum",
                                episodeNumber = eNum,
                                title = epTitle,
                                durationMinutes = 45,
                                thumbnailUrl = epThumb,
                                streamUrl = stream,
                                seriesId = seriesObj.id,
                                seasonNumber = sNum,
                                servers = srvs,
                                subtitles = seriesSubtitles
                            )
                        }
                        Season(seasonNumber = sNum, episodes = episodes)
                    }
                } else {
                    seriesObj.seasons.map { season ->
                        val sNum = season.seasonNumber
                        val enrichedEpisodes = season.episodes.map { ep ->
                            val eNum = ep.episodeNumber
                            val vObj = cinemetaVideos["${sNum}_${eNum}"]
                            val epTitle = vObj?.optString("name")?.takeIf { it.isNotBlank() } ?: ep.title
                            val epThumb = vObj?.optString("thumbnail")?.takeIf { it.isNotBlank() } ?: ep.thumbnailUrl

                            var epServers = ep.servers
                            var epStream = ep.streamUrl

                            val srvs = client.generateStreamServers(
                                realId = "",
                                tmdbId = resolvedTmdbId,
                                imdbId = resolvedImdbId,
                                isSeries = true,
                                seasonNumber = sNum,
                                episodeNumber = eNum
                            )
                            epServers = (epServers + srvs).distinctBy { it.url }

                            if (epStream.isBlank()) {
                                val resolvedStream = client.resolveMultiSourceStream(
                                    realId = "",
                                    tmdbId = resolvedTmdbId,
                                    imdbId = resolvedImdbId,
                                    isSeries = true,
                                    seasonNumber = sNum,
                                    episodeNumber = eNum
                                )
                                epStream = resolvedStream.ifBlank { epServers.firstOrNull()?.url.orEmpty() }
                            }

                            val epSubs = (ep.subtitles + seriesSubtitles).distinctBy { it.url }.sortedWith(
                                compareByDescending<SubtitleTrack> { it.language == "ar" && (it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox")) }
                                    .thenByDescending { it.language == "ar" }
                                    .thenByDescending { it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox") }
                            )

                            ep.copy(
                                title = epTitle,
                                thumbnailUrl = epThumb,
                                streamUrl = epStream,
                                servers = epServers,
                                subtitles = epSubs
                            )
                        }
                        season.copy(episodes = enrichedEpisodes)
                    }
                }

                seriesObj = seriesObj.copy(
                    isFavorite = isFav,
                    imdbId = resolvedImdbId,
                    tmdbId = resolvedTmdbId,
                    seasons = enrichedSeasons
                )

                val idx = cachedSeries.indexOfFirst { it.id == id }
                if (idx != -1) cachedSeries[idx] = seriesObj else cachedSeries.add(seriesObj)
                _seriesFlow.value = cachedSeries.toList()
                return@withContext seriesObj
            } else if (fallback != null) {
                var fb = fallback
                if (fb.seasons.isEmpty() || fb.seasons.firstOrNull()?.episodes?.isEmpty() == true) {
                    val srvs = client.generateStreamServers(realId = "", tmdbId = fb.tmdbId, imdbId = fb.imdbId, isSeries = true, seasonNumber = 1, episodeNumber = 1)
                    val st = client.resolveMultiSourceStream(realId = "", tmdbId = fb.tmdbId, imdbId = fb.imdbId, isSeries = true, seasonNumber = 1, episodeNumber = 1)
                    val ep1 = Episode(
                        id = "${fb.id}_s1_e1",
                        episodeNumber = 1,
                        title = "الحلقة 1",
                        durationMinutes = 45,
                        thumbnailUrl = fb.backdropUrl,
                        streamUrl = st,
                        seriesId = fb.id,
                        seasonNumber = 1,
                        servers = srvs,
                        subtitles = emptyList()
                    )
                    fb = fb.copy(seasons = listOf(Season(seasonNumber = 1, episodes = listOf(ep1))))
                }
                return@withContext fb
            }
            return@withContext null
        }
        var seriesDetailsDto: com.beouttv.app.data.api.ItemDetailsDto? = null
        val series = cachedSeries.find {
            it.id == id ||
            it.originalTitle?.equals(id, ignoreCase = true) == true ||
            it.title.equals(id, ignoreCase = true)
        } ?: run {
            if (id.all { it.isDigit() }) {
                val details = client.getItemDetails(id)
                if (details != null) {
                    seriesDetailsDto = details
                    val ratingVal = details.rating
                    val s = Series(
                        id = details.id,
                        title = ImdbMetadataEngine.cleanDisplayTitle(details.title),
                        posterUrl = details.thumbnail,
                        backdropUrl = details.thumbnail,
                        year = details.releaseYear,
                        rating = ratingVal,
                        genre = details.genre,
                        description = details.story.ifBlank { "مشاهدة وتحميل ${details.title} بجودة عالية." },
                        originalTitle = details.title
                    )
                    cachedSeries.add(s)
                    s
                } else null
            } else {
                val foundItems = client.getItems(page = 1, perPage = 10, searchQuery = id)
                val match = foundItems.firstOrNull { it.isSeries || it.seriesSlug == id || it.title.contains(id, true) } ?: foundItems.firstOrNull()
                if (match != null) {
                    val s = Series(
                        id = match.id,
                        title = ImdbMetadataEngine.cleanDisplayTitle(match.title),
                        posterUrl = match.thumbnail,
                        backdropUrl = match.thumbnail,
                        year = match.releaseYear,
                        rating = match.rating,
                        genre = match.genre.ifBlank { match.category ?: "مسلسلات" },
                        description = match.story,
                        originalTitle = match.title
                    )
                    cachedSeries.add(s)
                    s
                } else null
            }
        } ?: return@withContext null

        try {
            if (seriesDetailsDto == null && (id.all { it.isDigit() } || series.id.all { it.isDigit() })) {
                seriesDetailsDto = client.getItemDetails(series.id)
            }

            val apiSeriesSlug = seriesDetailsDto?.seriesSlug?.ifBlank { null }
            val apiSeasonSlug = seriesDetailsDto?.seasonSlug?.ifBlank { null }

            val seriesSlug = apiSeriesSlug ?: if (id.isNotBlank() && !id.all { it.isDigit() }) id else {
                (series.originalTitle ?: series.title).lowercase().replace(" ", "-")
            }

            val seriesSubtitles = client.fetchSubtitles(
                tmdbId = series.tmdbId,
                imdbId = series.imdbId,
                query = series.title
            )

            // 1. Fetch exact real seasons from API via series_slug
            var seasonsDto = client.getSeriesSeasons(seriesSlug)
            if (seasonsDto.isEmpty() && apiSeasonSlug != null) {
                seasonsDto = listOf(com.beouttv.app.data.api.SeriesSeasonDto("الموسم الأول", apiSeasonSlug))
            }
            val seasonsList = mutableListOf<Season>()

            if (seasonsDto.isNotEmpty()) {
                for ((idx, sDto) in seasonsDto.withIndex()) {
                    val sNum = idx + 1
                    val epDtos = client.getSeasonEpisodes(sDto.slug)
                    val epList = epDtos.map { epDto ->
                        val epServers = client.generateStreamServers(
                            realId = epDto.realId,
                            tmdbId = series.tmdbId,
                            imdbId = series.imdbId,
                            isSeries = true,
                            seasonNumber = sNum,
                            episodeNumber = epDto.number
                        )
                        Episode(
                            id = epDto.id,
                            episodeNumber = epDto.number,
                            title = epDto.title,
                            durationMinutes = 48,
                            thumbnailUrl = series.backdropUrl.ifBlank { series.posterUrl },
                            streamUrl = epServers.firstOrNull()?.url ?: "https://govid.live/prem-${epDto.realId}.m3u8",
                            seriesId = series.id,
                            seasonNumber = sNum,
                            subtitles = seriesSubtitles,
                            servers = epServers
                        )
                    }
                    seasonsList.add(Season(seasonNumber = sNum, episodes = epList))
                }
            } else {
                // If single season without sub-seasons or fallback
                val targetSlug = apiSeasonSlug ?: seriesSlug
                val epDtos = client.getSeasonEpisodes(targetSlug)
                if (epDtos.isNotEmpty()) {
                    val epList = epDtos.map { epDto ->
                        val epServers = client.generateStreamServers(
                            realId = epDto.realId,
                            tmdbId = series.tmdbId,
                            imdbId = series.imdbId,
                            isSeries = true,
                            seasonNumber = 1,
                            episodeNumber = epDto.number
                        )
                        Episode(
                            id = epDto.id,
                            episodeNumber = epDto.number,
                            title = epDto.title,
                            durationMinutes = 48,
                            thumbnailUrl = series.backdropUrl.ifBlank { series.posterUrl },
                            streamUrl = epServers.firstOrNull()?.url ?: "https://govid.live/prem-${epDto.realId}.m3u8",
                            seriesId = series.id,
                            seasonNumber = 1,
                            subtitles = seriesSubtitles,
                            servers = epServers
                        )
                    }
                    seasonsList.add(Season(seasonNumber = 1, episodes = epList))
                } else {
                    val epServers = client.generateStreamServers(
                        realId = series.id,
                        tmdbId = series.tmdbId,
                        imdbId = series.imdbId,
                        isSeries = true,
                        seasonNumber = 1,
                        episodeNumber = 1
                    )
                    seasonsList.add(
                        Season(
                            seasonNumber = 1,
                            episodes = listOf(
                                Episode(
                                    id = "${series.id}_ep1",
                                    episodeNumber = 1,
                                    title = "الحلقة 1",
                                    durationMinutes = 48,
                                    thumbnailUrl = series.posterUrl,
                                    streamUrl = epServers.firstOrNull()?.url ?: "https://govid.live/prem-${series.id}.m3u8",
                                    seriesId = series.id,
                                    seasonNumber = 1,
                                    subtitles = seriesSubtitles,
                                    servers = epServers
                                )
                            )
                        )
                    )
                }
            }

            val finalSeries = series.copy(seasons = seasonsList)
            val idx = cachedSeries.indexOfFirst { it.id == id }
            if (idx != -1) {
                cachedSeries[idx] = finalSeries
                _seriesFlow.value = cachedSeries.toList()
            }
            return@withContext finalSeries
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext series
        }
    }

    override suspend fun toggleFavoriteChannel(id: String) {
        val newState = if (context != null) {
            FavoritesManager.toggleChannelFavorite(context, id)
        } else false
        val index = cachedChannels.indexOfFirst { it.id == id }
        if (index != -1) {
            cachedChannels[index] = cachedChannels[index].copy(isFavorite = newState)
        }
    }

    override suspend fun toggleFavoriteMovie(id: String) {
        val realId = movieRealIds[id] ?: id
        val newState = if (context != null) {
            FavoritesManager.toggleMovieFavorite(context, id, realId)
        } else false

        val index = cachedMovies.indexOfFirst { it.id == id || movieRealIds[it.id] == id }
        if (index != -1) {
            cachedMovies[index] = cachedMovies[index].copy(isFavorite = newState)
            val currentLang = LanguageManager.currentLanguage.value
            _moviesFlow.value = cachedMovies.map { ImdbMetadataEngine.localizeMovie(it, currentLang) }
        }
    }

    override suspend fun toggleFavoriteSeries(id: String) {
        val newState = if (context != null) {
            FavoritesManager.toggleSeriesFavorite(context, id)
        } else false
        val index = cachedSeries.indexOfFirst { it.id == id }
        if (index != -1) {
            cachedSeries[index] = cachedSeries[index].copy(isFavorite = newState)
        }
    }

    override suspend fun getFavoriteChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val favIds = if (context != null) FavoritesManager.getFavoriteChannelIds(context) else emptySet()
        getChannels("All").filter { favIds.contains(it.id) || it.isFavorite }
    }

    override suspend fun getFavoriteMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val favIds = if (context != null) FavoritesManager.getFavoriteMovieIds(context) else emptySet()
        getMovies().filter { favIds.contains(it.id) || favIds.contains(movieRealIds[it.id] ?: "") || it.isFavorite }
    }

    override suspend fun getFavoriteSeries(): List<Series> = withContext(Dispatchers.IO) {
        val favIds = if (context != null) FavoritesManager.getFavoriteSeriesIds(context) else emptySet()
        cachedSeries.filter { favIds.contains(it.id) || it.isFavorite }
    }
}
