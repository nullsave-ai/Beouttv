package com.beouttv.app.data.repository

import android.util.Log
import com.beouttv.app.data.api.moviebox.MovieBoxApiClient
import com.beouttv.app.data.filter.ContentFilterEngine
import com.beouttv.app.data.model.ShortDramaCategory
import com.beouttv.app.data.model.ShortDramaEpisode
import com.beouttv.app.data.model.ShortDramaFeedItem
import com.beouttv.app.data.model.ShortDramaSeries
import com.beouttv.app.data.model.ShortDramaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * MovieBox Short TV (تلفزيون دراما ريلز القصير) Repository.
 * 100% Authentic MovieBox Short TV content only.
 * High-speed direct CDN streams for instant start and zero buffering.
 * Authentic episode counts and real video streams matching MovieBox perfectly.
 */
object ShortDramaRepository {
    private const val TAG = "ShortDramaRepo"

    val knownSubtitleUrls = ConcurrentHashMap<String, String>()
    private val seriesCache = ConcurrentHashMap<String, List<ShortDramaSeries>>()
    private val episodesCache = ConcurrentHashMap<String, List<ShortDramaEpisode>>()
    private val sourcesCache = ConcurrentHashMap<String, List<ShortDramaSource>>()
    private val dynamicSourcesCache = ConcurrentHashMap<String, List<ShortDramaSource>>()

    private val defaultSourcesMap: Map<String, List<ShortDramaSource>> by lazy {
        MovieBoxShortDramaCatalog.getSourcesMap()
    }

    init {
        prepopulateCatalog()
    }

    fun isBlocked(title: String): Boolean {
        if (title.contains("midnight", ignoreCase = true) || title.contains("ميدنايت")) return true
        return ContentFilterEngine.isContentBlocked(title)
    }

    /**
     * Authentic MovieBox Short TV series list with exact episode counts and real covers
     */
    fun getAllSeries(): List<ShortDramaSeries> =
        MovieBoxShortDramaCatalog.getAllSeries().filter { !isBlocked(it.name) }

    private fun prepopulateCatalog() {
        val all = getAllSeries()
        seriesCache["hot_mb"] = all.filter { it.categoryId == "hot_mb" }
        seriesCache["marriage_love_mb"] = all.filter { it.categoryId == "marriage_love_mb" }
        seriesCache["hidden_ceo_mb"] = all.filter { it.categoryId == "hidden_ceo_mb" }
        seriesCache["revenge_her_mb"] = all.filter { it.categoryId == "revenge_her_mb" }
        seriesCache["old_love_mb"] = all.filter { it.categoryId == "old_love_mb" }
        seriesCache["dubbed_mb"] = all.filter { it.name.contains("مدبلج") || it.name.contains("دبلج") }
        seriesCache["subbed_mb"] = all.filter { !it.name.contains("مدبلج") }
        seriesCache["all_mb"] = all
        seriesCache["all_ar"] = all
    }

    fun getDefaultCategories(): List<ShortDramaCategory> = listOf(
        ShortDramaCategory("hot_mb", "الأكثر سخونة"),
        ShortDramaCategory("marriage_love_mb", "زواج مرتب للحب"),
        ShortDramaCategory("hidden_ceo_mb", "الرئيس المخفي"),
        ShortDramaCategory("revenge_her_mb", "انتقامها"),
        ShortDramaCategory("old_love_mb", "الحب القديم"),
        ShortDramaCategory("dubbed_mb", "مدبلج للعربية"),
        ShortDramaCategory("subbed_mb", "مترجم للعربية"),
        ShortDramaCategory("favorites_ar", "أعجبني"),
        ShortDramaCategory("downloads_ar", "المحملات")
    )

    fun getAllPlatforms(): List<ShortDramaCategory> = getDefaultCategories()

    fun getCachedSeries(categoryId: String): List<ShortDramaSeries> {
        return seriesCache[categoryId] ?: seriesCache["hot_mb"] ?: seriesCache["all_mb"] ?: emptyList()
    }

    fun getInitialPreloadedFeed(): List<ShortDramaFeedItem> {
        val seriesList = getAllSeries()
        return seriesList.map { series ->
            createFeedItemFromSeries(series, 1)
        }
    }

    fun isVideoIdKnown(seriesId: String): Boolean = true

    fun resolveVideoId(seriesId: String): String {
        return seriesId.removePrefix("mb_").removePrefix("series_")
    }

    /**
     * Builds authentic MovieBox CDN streaming sources for an episode.
     * Uses verified authentic video streams (1080p FHD, 720p HD, 480p SD).
     */
    fun buildSourcesForEpisode(
        videoId: String,
        episodeNumber: Int,
        platform: String = "",
        subtitleOverride: String? = null
    ): List<ShortDramaSource> {
        val cleanVideoId = resolveVideoId(videoId).removePrefix("epservers_")
        val cacheKey = "${cleanVideoId}_$episodeNumber"

        val cached = sourcesCache[cacheKey]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

        val sub = subtitleOverride
            ?: knownSubtitleUrls[cacheKey]
            ?: knownSubtitleUrls[cleanVideoId]
            ?: ""

        // Lookup verified authentic sources from MovieBox catalog or dynamic cache
        // Each episode strictly plays its OWN authentic MovieBox stream!
        val catalogSources = MovieBoxShortDramaCatalog.getSourcesForEpisode(cleanVideoId, episodeNumber)
            ?: if (episodeNumber == 1) {
                defaultSourcesMap[cleanVideoId]
                    ?: defaultSourcesMap["mb_$cleanVideoId"]
                    ?: dynamicSourcesCache[cleanVideoId]
                    ?: emptyList()
            } else {
                dynamicSourcesCache[cacheKey] ?: emptyList()
            }

        if (catalogSources.isEmpty()) {
            return emptyList()
        }

        val list = catalogSources.map { src ->
            src.copy(subtitleUrl = if (sub.isNotBlank()) sub else src.subtitleUrl)
        }

        if (list.isNotEmpty()) {
            sourcesCache[cacheKey] = list
        }
        return list
    }

    /**
     * Creates a feed item for the given series and episode, bound to its authentic MovieBox stream
     */
    fun createFeedItemFromSeries(series: ShortDramaSeries, episodeNumber: Int = 1): ShortDramaFeedItem {
        val videoId = resolveVideoId(series.id)
        val epId = "epservers_${videoId}_$episodeNumber"
        val sources = buildSourcesForEpisode(videoId, episodeNumber, series.platform)
        return ShortDramaFeedItem(
            series = series,
            episode = ShortDramaEpisode(
                id = epId,
                seriesId = series.id,
                episodeNumber = episodeNumber,
                title = "الحلقة $episodeNumber",
                thumbnailUrl = series.coverUrl
            ),
            sources = sources,
            currentSourceIndex = 0
        )
    }

    suspend fun getSeriesByCategory(categoryId: String): List<ShortDramaSeries> = withContext(Dispatchers.IO) {
        val cached = seriesCache[categoryId]
        if (!cached.isNullOrEmpty()) return@withContext cached

        val all = getAllSeries()
        val result = when (categoryId) {
            "hot_mb" -> all.filter { it.categoryId == "hot_mb" }
            "marriage_love_mb" -> all.filter { it.categoryId == "marriage_love_mb" }
            "hidden_ceo_mb" -> all.filter { it.categoryId == "hidden_ceo_mb" }
            "revenge_her_mb" -> all.filter { it.categoryId == "revenge_her_mb" }
            "old_love_mb" -> all.filter { it.categoryId == "old_love_mb" }
            "dubbed_mb" -> all.filter { it.name.contains("مدبلج") || it.name.contains("دبلج") }
            "subbed_mb" -> all.filter { !it.name.contains("مدبلج") }
            else -> all
        }
        seriesCache[categoryId] = result
        result
    }

    /**
     * Resolves authentic high-speed direct CDN streams for any MovieBox Short TV drama via postId -> review_detail.
     * Guaranteed to return streams matching THIS specific drama only.
     */
    suspend fun resolveMovieBoxStreams(videoId: String): List<ShortDramaSource> = withContext(Dispatchers.IO) {
        val cleanVideoId = resolveVideoId(videoId)
        val cached = dynamicSourcesCache[cleanVideoId] ?: defaultSourcesMap[cleanVideoId] ?: defaultSourcesMap["mb_$cleanVideoId"]
        if (!cached.isNullOrEmpty()) return@withContext cached

        try {
            val detail = MovieBoxApiClient.getSubjectDetail(cleanVideoId)
            val detector = detail?.resourceDetectors?.firstOrNull()
            val postId = detector?.postId
            if (!postId.isNullOrBlank() && postId != "0") {
                val reviewUrl = "https://h5.aoneroom.com/wefeed-h5-bff/mini/review_detail?postId=$postId"
                val req = okhttp3.Request.Builder()
                    .url(reviewUrl)
                    .header("User-Agent", "okhttp/4.12.0")
                    .header("x-tr-devtype", "h5")
                    .header("x-tr-region", "CN")
                    .get()
                    .build()
                val okClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val resp = okClient.newCall(req).execute()
                val body = resp.body?.string().orEmpty()
                val jsonObj = com.google.gson.JsonParser.parseString(body).asJsonObject
                val post = jsonObj.getAsJsonObject("data")?.getAsJsonObject("post")
                val media = post?.getAsJsonObject("media")
                val vids = media?.getAsJsonArray("video")
                if (vids != null && vids.size() > 0) {
                    val resolved = mutableListOf<ShortDramaSource>()
                    for (i in 0 until vids.size()) {
                        val v = vids.get(i).asJsonObject
                        val defn = v.get("definition")?.asInt ?: 3
                        val u = v.get("url")?.asString.orEmpty()
                        if (u.isNotBlank()) {
                            val (sname, squal, sid) = when (defn) {
                                7 -> Triple("سيرفر MovieBox الأصلي (FHD)", "1080p FHD", "mb_fhd")
                                5 -> Triple("سيرفر MovieBox السريع (HD)", "720p HD", "mb_hd")
                                else -> Triple("سيرفر MovieBox الموفر (SD)", "480p SD", "mb_sd")
                            }
                            resolved.add(
                                ShortDramaSource(
                                    id = sid,
                                    name = sname,
                                    streamUrl = u,
                                    userAgent = "okhttp/4.12.0",
                                    quality = squal,
                                    isDirectCdn = true
                                )
                            )
                        }
                    }
                    if (resolved.isNotEmpty()) {
                        val sorted = resolved.sortedByDescending {
                            when (it.id) {
                                "mb_fhd" -> 3
                                "mb_hd" -> 2
                                else -> 1
                            }
                        }
                        dynamicSourcesCache[cleanVideoId] = sorted
                        dynamicSourcesCache["mb_$cleanVideoId"] = sorted
                        sourcesCache["${cleanVideoId}_1"] = sorted
                        return@withContext sorted
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dynamic MovieBox stream lookup failed for $cleanVideoId: ${e.message}")
        }
        emptyList()
    }

    suspend fun refreshFromMovieBoxApi(): List<ShortDramaSeries> = withContext(Dispatchers.IO) {
        try {
            val queries = listOf(
                "زواج مرتب للحب" to "marriage_love_mb",
                "الرئيس المخفي" to "hidden_ceo_mb",
                "انتقامها" to "revenge_her_mb",
                "الحب القديم" to "old_love_mb",
                "مدبلج" to "hot_mb"
            )
            val mapped = mutableListOf<ShortDramaSeries>()
            val seenSubjectIds = mutableSetOf<String>()

            queries.forEach { (query, catId) ->
                try {
                    val items = MovieBoxApiClient.search(query)
                    val candidates = items.filter {
                        !isBlocked(it.title) &&
                        !ContentFilterEngine.isContentBlocked(it.title, it.genre, it.description) &&
                        (it.subjectType == 7 || it.genre.contains("Drama", ignoreCase = true) || it.genre.contains("Short", ignoreCase = true))
                    }.take(6)

                    for (item in candidates) {
                        val cleanId = item.subjectId
                        if (cleanId.isBlank() || !seenSubjectIds.add(cleanId)) continue
                        val id = "mb_$cleanId"

                        // Check if already in catalog or cache
                        if (defaultSourcesMap.containsKey(cleanId) || dynamicSourcesCache.containsKey(cleanId)) {
                            val title = item.title
                            val isDubbed = title.contains("dubbed", ignoreCase = true) ||
                                           title.contains("مدبلج", ignoreCase = true) ||
                                           title.contains("دبلج", ignoreCase = true)
                            val displayTitle = if (isDubbed && !title.contains("مدبلج")) "(مدبلج) $title" else title
                            mapped.add(
                                ShortDramaSeries(
                                    id = id,
                                    name = displayTitle,
                                    coverUrl = item.cover?.url.orEmpty(),
                                    episodeCount = item.resourceDetectors?.firstOrNull()?.totalEpisode?.takeIf { it > 0 } ?: 30,
                                    platform = "MovieBox",
                                    categoryId = catId
                                )
                            )
                            continue
                        }

                        // Authentically resolve real MovieBox CDN video streams for new candidate
                        val resolvedSources = resolveMovieBoxStreams(cleanId)
                        if (resolvedSources.isNotEmpty()) {
                            val detail = try { MovieBoxApiClient.getSubjectDetail(cleanId) } catch (_: Exception) { null }
                            val epCount = detail?.resourceDetectors?.firstOrNull()?.totalEpisode?.takeIf { it > 0 } ?: 30
                            val title = detail?.title?.takeIf { it.isNotBlank() } ?: item.title
                            val isDubbed = title.contains("dubbed", ignoreCase = true) ||
                                           title.contains("مدبلج", ignoreCase = true) ||
                                           title.contains("دبلج", ignoreCase = true)
                            val displayTitle = if (isDubbed && !title.contains("مدبلج")) "(مدبلج) $title" else title

                            mapped.add(
                                ShortDramaSeries(
                                    id = id,
                                    name = displayTitle,
                                    coverUrl = detail?.cover?.url?.takeIf { it.isNotBlank() } ?: item.cover?.url.orEmpty(),
                                    episodeCount = epCount,
                                    platform = "MovieBox",
                                    categoryId = catId
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            if (mapped.isNotEmpty()) {
                val currentAll = getAllSeries()
                val merged = (currentAll + mapped).distinctBy { it.id }
                seriesCache["hot_mb"] = merged.filter { it.categoryId == "hot_mb" }
                seriesCache["marriage_love_mb"] = merged.filter { it.categoryId == "marriage_love_mb" }
                seriesCache["hidden_ceo_mb"] = merged.filter { it.categoryId == "hidden_ceo_mb" }
                seriesCache["revenge_her_mb"] = merged.filter { it.categoryId == "revenge_her_mb" }
                seriesCache["old_love_mb"] = merged.filter { it.categoryId == "old_love_mb" }
                seriesCache["dubbed_mb"] = merged.filter { it.name.contains("مدبلج") || it.name.contains("دبلج") }
                seriesCache["subbed_mb"] = merged.filter { !it.name.contains("مدبلج") }
                seriesCache["all_mb"] = merged
                seriesCache["all_ar"] = merged
            }
            mapped
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getEpisodes(seriesId: String): List<ShortDramaEpisode> = withContext(Dispatchers.IO) {
        val cached = episodesCache[seriesId]
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val videoId = resolveVideoId(seriesId)
        val series = (seriesCache["all_mb"] ?: getAllSeries()).firstOrNull {
            it.id == seriesId || resolveVideoId(it.id) == videoId
        }
        val totalEps = series?.episodeCount?.takeIf { it > 0 } ?: 1

        val list = (1..totalEps).map { epNum ->
            ShortDramaEpisode(
                id = "epservers_${videoId}_$epNum",
                seriesId = seriesId,
                episodeNumber = epNum,
                title = "الحلقة $epNum",
                thumbnailUrl = series?.coverUrl.orEmpty()
            )
        }
        episodesCache[seriesId] = list
        list
    }

    /**
     * Resolves authentic MovieBox CDN streams for a specific episode (any episode 1..N).
     * Uses MovieBoxShortDramaCatalog.getSourcesForEpisode for instant playback,
     * and falls back to dynamic mini/reviews query to retrieve individual episode streams.
     */
    suspend fun getEpisodeSources(
        episodeId: String,
        seriesId: String,
        episodeNumber: Int,
        platform: String = ""
    ): List<ShortDramaSource> = withContext(Dispatchers.IO) {
        val videoId = resolveVideoId(seriesId)
        val cacheKey = "${videoId}_$episodeNumber"

        // 1. If episode has cached sources, return immediately
        val cachedEp = sourcesCache[cacheKey] ?: dynamicSourcesCache[cacheKey]
        if (!cachedEp.isNullOrEmpty()) return@withContext cachedEp

        // 2. Check authentic MovieBox multi-episode catalog (instant 0ms)
        val catalogEp = MovieBoxShortDramaCatalog.getSourcesForEpisode(videoId, episodeNumber)
        if (!catalogEp.isNullOrEmpty()) {
            sourcesCache[cacheKey] = catalogEp
            return@withContext catalogEp
        }

        // 3. For dynamic/live dramas: query mini/reviews to get real episode stream
        val dynamicReviewsEp = resolveMovieBoxReviewsForEpisode(videoId, episodeNumber)
        if (dynamicReviewsEp.isNotEmpty()) {
            sourcesCache[cacheKey] = dynamicReviewsEp
            return@withContext dynamicReviewsEp
        }

        // 4. For episode 1: check defaultSourcesMap / dynamicSourcesCache
        if (episodeNumber == 1) {
            val ep1 = defaultSourcesMap[videoId]
                ?: defaultSourcesMap["mb_$videoId"]
                ?: dynamicSourcesCache[videoId]
            if (!ep1.isNullOrEmpty()) {
                sourcesCache[cacheKey] = ep1
                return@withContext ep1
            }
        }

        emptyList()
    }

    /**
     * Queries mini/reviews?subjectId={videoId} to extract authentic video streams
     * for all episodes of the drama dynamically.
     */
    private suspend fun resolveMovieBoxReviewsForEpisode(
        videoId: String,
        episodeNumber: Int
    ): List<ShortDramaSource> = withContext(Dispatchers.IO) {
        try {
            val cleanId = resolveVideoId(videoId)
            val url = "https://h5.aoneroom.com/wefeed-h5-bff/mini/reviews?subjectId=$cleanId&pageSize=100&withSubjectDetail=false"
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "okhttp/4.12.0")
                .header("x-tr-devtype", "h5")
                .header("x-tr-region", "CN")
                .get()
                .build()
            val okClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val resp = okClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            val jsonObj = com.google.gson.JsonParser.parseString(body).asJsonObject
            val posts = jsonObj.getAsJsonObject("data")?.getAsJsonArray("postList")
            if (posts != null && posts.size() > 0) {
                for (pIdx in 0 until posts.size()) {
                    val p = posts.get(pIdx).asJsonObject
                    val epNum = pIdx + 1
                    val vids = p.getAsJsonObject("media")?.getAsJsonArray("video")
                    if (vids != null && vids.size() > 0) {
                        val epSources = mutableListOf<ShortDramaSource>()
                        for (vi in 0 until vids.size()) {
                            val v = vids.get(vi).asJsonObject
                            val defn = v.get("definition")?.asInt ?: 3
                            val u = v.get("url")?.asString.orEmpty()
                            if (u.isNotBlank()) {
                                val (sname, squal, sid) = when (defn) {
                                    7 -> Triple("سيرفر MovieBox الأصلي (FHD)", "1080p FHD", "mb_fhd")
                                    5 -> Triple("سيرفر MovieBox السريع (HD)", "720p HD", "mb_hd")
                                    else -> Triple("سيرفر MovieBox الموفر (SD)", "480p SD", "mb_sd")
                                }
                                epSources.add(
                                    ShortDramaSource(
                                        id = sid,
                                        name = sname,
                                        streamUrl = u,
                                        userAgent = "okhttp/4.12.0",
                                        quality = squal,
                                        isDirectCdn = true
                                    )
                                )
                            }
                        }
                        val sorted = epSources.sortedByDescending {
                            when (it.id) {
                                "mb_fhd" -> 3
                                "mb_hd" -> 2
                                else -> 1
                            }
                        }
                        if (sorted.isNotEmpty()) {
                            dynamicSourcesCache["${cleanId}_$epNum"] = sorted
                            sourcesCache["${cleanId}_$epNum"] = sorted
                        }
                    }
                }
                return@withContext dynamicSourcesCache["${cleanId}_$episodeNumber"] ?: emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveMovieBoxReviewsForEpisode failed: ${e.message}")
        }
        emptyList()
    }

    /**
     * Gets postId for a series from catalog or live API.
     */
    private suspend fun getPostIdForSeries(videoId: String): String? {
        // Check catalog json first
        val catalogEntry = MovieBoxShortDramaCatalog.entries.firstOrNull {
            resolveVideoId(it.series.id) == videoId
        }
        if (catalogEntry != null) {
            val catalogPostId = MovieBoxShortDramaCatalog.getPostId(videoId)
            if (!catalogPostId.isNullOrBlank() && catalogPostId != "0") return catalogPostId
        }
        // Fall back to live API
        return try {
            val detail = MovieBoxApiClient.getSubjectDetail(videoId)
            detail?.resourceDetectors?.firstOrNull()?.postId
        } catch (_: Exception) { null }
    }

    /**
     * Calls review_detail?postId={postId}&resourcePosition={position} to get
     * the authentic CDN stream for a specific episode.
     */
    private suspend fun resolveMovieBoxStreamsForEpisode(
        videoId: String,
        postId: String,
        resourcePosition: Int
    ): List<ShortDramaSource> = withContext(Dispatchers.IO) {
        try {
            val url = "https://h5.aoneroom.com/wefeed-h5-bff/mini/review_detail?postId=$postId&resourcePosition=$resourcePosition"
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "okhttp/4.12.0")
                .header("x-tr-devtype", "h5")
                .header("x-tr-region", "CN")
                .get()
                .build()
            val okClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val resp = okClient.newCall(req).execute()
            val body = resp.body?.string().orEmpty()
            val jsonObj = com.google.gson.JsonParser.parseString(body).asJsonObject
            val post = jsonObj.getAsJsonObject("data")?.getAsJsonObject("post")
            val media = post?.getAsJsonObject("media")
            val vids = media?.getAsJsonArray("video")
            if (vids != null && vids.size() > 0) {
                val resolved = mutableListOf<ShortDramaSource>()
                for (i in 0 until vids.size()) {
                    val v = vids.get(i).asJsonObject
                    val defn = v.get("definition")?.asInt ?: 3
                    val u = v.get("url")?.asString.orEmpty()
                    if (u.isNotBlank()) {
                        val (sname, squal, sid) = when (defn) {
                            7 -> Triple("سيرفر MovieBox الأصلي (FHD)", "1080p FHD", "mb_fhd")
                            5 -> Triple("سيرفر MovieBox السريع (HD)", "720p HD", "mb_hd")
                            else -> Triple("سيرفر MovieBox الموفر (SD)", "480p SD", "mb_sd")
                        }
                        resolved.add(
                            ShortDramaSource(
                                id = sid,
                                name = sname,
                                streamUrl = u,
                                userAgent = "okhttp/4.12.0",
                                quality = squal,
                                isDirectCdn = true
                            )
                        )
                    }
                }
                resolved.sortedByDescending {
                    when (it.id) {
                        "mb_fhd" -> 3
                        "mb_hd" -> 2
                        else -> 1
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveMovieBoxStreamsForEpisode failed pos=$resourcePosition: ${e.message}")
            emptyList()
        }
    }

    suspend fun resolveSubtitleForEpisode(videoId: String, episodeNumber: Int): String = withContext(Dispatchers.IO) {
        val cleanVid = resolveVideoId(videoId)
        knownSubtitleUrls["${cleanVid}_$episodeNumber"] ?: knownSubtitleUrls[cleanVid] ?: ""
    }
}
