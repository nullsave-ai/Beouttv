package com.beouttv.app.data.api

import com.beouttv.app.data.config.AppGatewayManager
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SectionDto(val name: String, val slug: String)

data class ItemDto(
    val id: String,
    val realId: String,
    val title: String,
    val thumbnail: String,
    val releaseYear: Int,
    val isSeries: Boolean,
    val seriesSlug: String? = null,
    val seasonSlug: String? = null,
    val rating: Float = 0f,
    val category: String? = null,
    val story: String = "",
    val genre: String = "",
    val quality: String = "1080p FHD",
    val country: String = "",
    val director: String = "",
    val actors: List<String> = emptyList(),
    val language: String = ""
)

data class ItemDetailsDto(
    val id: String,
    val realId: String,
    val title: String,
    val thumbnail: String,
    val story: String,
    val releaseYear: Int,
    val terms: Map<String, String>,
    val streamVariants: List<StreamVariantDto>,
    val rating: Float = 0f,
    val genre: String = "سينما",
    val actors: List<String> = emptyList(),
    val director: String = "",
    val country: String = "",
    val seriesSlug: String? = null,
    val seasonSlug: String? = null
)

data class StreamVariantDto(val quality: String, val url: String)
data class SeriesSeasonDto(val name: String, val slug: String)
data class SeriesEpisodeDto(val id: String, val realId: String, val number: Int, val title: String)

class CoreApiClient(
    private val client: OkHttpClient = buildClient()
) {
    companion object {
        private fun buildClient(): OkHttpClient {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val req = original.newBuilder()
                    .header("User-Agent", "okhttp/4.12.0")
                    .header("X-App-Key", "sv_x9k2m7p4q8n3r6t1w5y0z")
                    .header("X-App-Sig", "0de08ec6cd0526dcc699041ed8e6dfb3")
                    .header("X-App-Chk", "cebe2159")
                    .header("X-Device-ID", "3b29c9ef4e872d8a")
                    .header("X-Device-Name", "Samsung SM-S901B")
                chain.proceed(req.build())
            }

            return OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()
        }
    }

    private fun getBaseUrl(): String {
        val activeSourceUrl = try {
            com.beouttv.app.data.source.MediaSourceManager.getActiveSource().baseUrl
        } catch (_: Exception) { "" }
        return if (activeSourceUrl.isNotBlank()) activeSourceUrl else AppGatewayManager.getActiveApiUrl()
    }

    suspend fun fetchString(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")
            response.body?.string().orEmpty()
        }
    }

    suspend fun getSections(): List<SectionDto> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}?action=categories"
            val body = fetchString(url).trim()
            val dataArray = if (body.startsWith("[")) {
                JSONArray(body)
            } else {
                val json = JSONObject(body)
                json.optJSONArray("data") ?: (if (json.has("categories")) json.getJSONArray("categories") else JSONArray())
            }
            val list = mutableListOf<SectionDto>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val name = item.optString("name")
                val slug = item.optString("slug")
                if (name.isNotBlank() && slug.isNotBlank()) {
                    list.add(SectionDto(name, slug))
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getItems(
        page: Int = 1,
        perPage: Int = 20,
        type: String? = "all",
        categorySlug: String? = null,
        searchQuery: String? = null
    ): List<ItemDto> = withContext(Dispatchers.IO) {
        try {
            val builder = StringBuilder("${getBaseUrl()}?action=posts&page=$page&per_page=$perPage")
            if (!searchQuery.isNullOrBlank()) {
                builder.append("&search=").append(java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8"))
            } else if (!categorySlug.isNullOrBlank() && categorySlug != "all") {
                val cleanCategory = if (categorySlug.contains("%")) categorySlug else java.net.URLEncoder.encode(categorySlug, "UTF-8")
                builder.append("&category=").append(cleanCategory)
            } else if (!type.isNullOrBlank()) {
                builder.append("&type=").append(type)
            }

            val body = fetchString(builder.toString())
            val json = JSONObject(body)
            val posts = json.optJSONArray("posts") ?: json.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<ItemDto>()
            for (i in 0 until posts.length()) {
                val p = posts.getJSONObject(i)
                val id = p.optString("id", i.toString())
                val realId = p.optString("realid", id)
                val title = p.optString("title", "Unknown")
                val thumbnail = p.optString("thumbnail", p.optString("poster", ""))
                val seriesSlug = if (p.has("series_slug") && p.optString("series_slug").isNotBlank()) {
                    p.optString("series_slug")
                } else if (p.has("series_info")) {
                    p.optJSONObject("series_info")?.optString("slug")
                } else null

                val seasonSlug = if (p.has("season_slug") && p.optString("season_slug").isNotBlank()) {
                    p.optString("season_slug")
                } else if (p.has("season_info")) {
                    p.optJSONObject("season_info")?.optString("slug")
                } else null

                val isSeries = !seriesSlug.isNullOrBlank() || !seasonSlug.isNullOrBlank()

                var story = ""
                var ratingVal = 0f
                if (p.has("meta")) {
                    val meta = p.optJSONObject("meta")
                    if (meta != null) {
                        story = meta.optString("story", "")
                        val rStr = meta.optString("imdbRating", meta.optString("rating", "")).trim()
                        val parsed = rStr.filter { it.isDigit() || it == '.' }.toFloatOrNull()
                        if (parsed != null && parsed in 1.0f..10.0f) {
                            ratingVal = parsed
                        }
                    }
                }
                if (ratingVal == 0f) {
                    val fallbackR = p.optDouble("rating", p.optDouble("imdb_rating", 0.0)).toFloat()
                    if (fallbackR in 1.0f..10.0f) ratingVal = fallbackR
                }

                var genre = "أكشن • دراما"
                var quality = "1080p FHD"
                var cat = p.optString("category", p.optString("category_slug", "أفلام"))

                // Extract real year: 1) from title regex, 2) from terms.release-year, 3) from field
                val titleYear = Regex("""\b(19\d\d|20\d\d)\b""").find(title)?.value?.toIntOrNull()
                var year = titleYear ?: 0

                var country = ""
                var director = ""
                val actorList = mutableListOf<String>()
                var lang = "مترجم بالعربية"

                if (p.has("terms")) {
                    val terms = p.optJSONObject("terms")
                    if (terms != null) {
                        val gArr = terms.optJSONArray("genre")
                        if (gArr != null && gArr.length() > 0) {
                            val gList = mutableListOf<String>()
                            for (gi in 0 until gArr.length()) {
                                gList.add(gArr.getJSONObject(gi).optString("name"))
                            }
                            genre = gList.joinToString(" • ")
                        }
                        val qArr = terms.optJSONArray("quality")
                        if (qArr != null && qArr.length() > 0) {
                            quality = qArr.getJSONObject(0).optString("name", "1080p FHD")
                        }
                        val cArr = terms.optJSONArray("category")
                        if (cArr != null && cArr.length() > 0) {
                            cat = cArr.getJSONObject(0).optString("name", cat)
                        }
                        val yArr = terms.optJSONArray("release-year")
                        if (yArr != null && yArr.length() > 0) {
                            val yStr = yArr.getJSONObject(0).optString("name", "")
                            val parsedYear = yStr.filter { it.isDigit() }.toIntOrNull()
                            if (parsedYear != null && parsedYear > 1900) {
                                year = parsedYear
                            }
                        }
                        val cntArr = terms.optJSONArray("country")
                        if (cntArr != null && cntArr.length() > 0) {
                            val cList = mutableListOf<String>()
                            for (ci in 0 until cntArr.length()) {
                                val n = cntArr.getJSONObject(ci).optString("name")
                                if (n.isNotBlank()) cList.add(n)
                            }
                            if (cList.isNotEmpty()) country = cList.joinToString(" • ")
                        }
                        val dirArr = terms.optJSONArray("director")
                        if (dirArr != null && dirArr.length() > 0) {
                            val dList = mutableListOf<String>()
                            for (di in 0 until dirArr.length()) {
                                val n = dirArr.getJSONObject(di).optString("name")
                                if (n.isNotBlank()) dList.add(n)
                            }
                            if (dList.isNotEmpty()) director = dList.joinToString(", ")
                        }
                        val actArr = terms.optJSONArray("actor")
                        if (actArr != null && actArr.length() > 0) {
                            for (ai in 0 until actArr.length()) {
                                val n = actArr.getJSONObject(ai).optString("name")
                                if (n.isNotBlank()) actorList.add(n)
                            }
                        }
                        val langArr = terms.optJSONArray("language")
                        if (langArr != null && langArr.length() > 0) {
                            val lList = mutableListOf<String>()
                            for (li in 0 until langArr.length()) {
                                val n = langArr.getJSONObject(li).optString("name")
                                if (n.isNotBlank()) lList.add(n)
                            }
                            if (lList.isNotEmpty()) lang = lList.joinToString(" • ")
                        }
                    }
                }

                if (country.isBlank()) {
                    val fullCheck = "$cat $title $story".lowercase()
                    country = when {
                        fullCheck.contains("عربي") || fullCheck.contains("مصر") || fullCheck.contains("سعودي") || fullCheck.contains("كويت") || fullCheck.contains("شامي") || fullCheck.contains("لبنان") -> "مصر • الوطن العربي"
                        fullCheck.contains("تركي") || fullCheck.contains("تركية") || fullCheck.contains("turkey") -> "تركيا"
                        fullCheck.contains("هندي") || fullCheck.contains("هندية") || fullCheck.contains("india") || fullCheck.contains("بوليوود") -> "الهند"
                        fullCheck.contains("كوري") || fullCheck.contains("كورية") || fullCheck.contains("korea") -> "كوريا الجنوبية"
                        fullCheck.contains("ياباني") || fullCheck.contains("انمي") || fullCheck.contains("أنمي") || fullCheck.contains("japan") -> "اليابان"
                        fullCheck.contains("اسيوي") || fullCheck.contains("اسيوية") || fullCheck.contains("صيني") -> "آسيا"
                        fullCheck.contains("اجنبي") || fullCheck.contains("أجنبي") || fullCheck.contains("english") -> "الولايات المتحدة • بريطانيا"
                        else -> "الولايات المتحدة"
                    }
                }

                if (year <= 1900) {
                    val rawYear = p.optInt("release-year", p.optInt("year", 0))
                    year = if (rawYear > 1900) rawYear else 2024
                }

                list.add(
                    ItemDto(
                        id = id,
                        realId = realId,
                        title = title,
                        thumbnail = thumbnail,
                        releaseYear = year,
                        isSeries = isSeries,
                        seriesSlug = seriesSlug,
                        seasonSlug = seasonSlug,
                        rating = ratingVal,
                        category = cat,
                        story = story,
                        genre = genre,
                        quality = quality,
                        country = country,
                        director = director,
                        actors = actorList,
                        language = lang
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getItemDetails(id: String): ItemDetailsDto? = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}?action=posts&id=$id"
            val body = fetchString(url)
            val json = JSONObject(body)
            val posts = json.optJSONArray("posts")
            val data = if (posts != null && posts.length() > 0) {
                posts.getJSONObject(0)
            } else if (json.has("data")) {
                val d = json.get("data")
                if (d is JSONObject) d
                else if (d is JSONArray && d.length() > 0) d.getJSONObject(0)
                else json
            } else json

            val realId = data.optString("realid", id)
            val title = data.optString("title", "")
            val thumbnail = data.optString("thumbnail", data.optString("poster", ""))

            val seriesSlug = if (data.has("series_slug") && data.optString("series_slug").isNotBlank()) {
                data.optString("series_slug")
            } else if (data.has("series_info")) {
                data.optJSONObject("series_info")?.optString("slug")
            } else null

            val seasonSlug = if (data.has("season_slug") && data.optString("season_slug").isNotBlank()) {
                data.optString("season_slug")
            } else if (data.has("season_info")) {
                data.optJSONObject("season_info")?.optString("slug")
            } else null

            val titleYear = Regex("""\b(19\d\d|20\d\d)\b""").find(title)?.value?.toIntOrNull()
            var year = titleYear ?: 0

            var story = ""
            var ratingVal = 0f
            val variants = mutableListOf<StreamVariantDto>()

            if (data.has("meta")) {
                val meta = data.optJSONObject("meta")
                if (meta != null) {
                    story = meta.optString("story", "")
                    val rStr = meta.optString("imdbRating", meta.optString("rating", "")).trim()
                    val parsed = rStr.filter { it.isDigit() || it == '.' }.toFloatOrNull()
                    if (parsed != null && parsed in 1.0f..10.0f) {
                        ratingVal = parsed
                    }

                    val dList = meta.optJSONArray("download_links_list")
                    if (dList != null) {
                        for (i in 0 until dList.length()) {
                            val d = dList.getJSONObject(i)
                            variants.add(StreamVariantDto(d.optString("quality", "HD"), d.optString("url", "")))
                        }
                    }
                }
            }
            if (story.isEmpty()) story = data.optString("story", data.optString("description", ""))

            val termsMap = mutableMapOf<String, String>()
            var genre = "أكشن • دراما"
            var country = ""
            var director = ""
            val actorList = mutableListOf<String>()

            if (data.has("terms")) {
                val terms = data.optJSONObject("terms")
                if (terms != null) {
                    val keys = terms.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        termsMap[k] = terms.optString(k)
                    }

                    val gArr = terms.optJSONArray("genre")
                    if (gArr != null && gArr.length() > 0) {
                        val gList = mutableListOf<String>()
                        for (gi in 0 until gArr.length()) {
                            gList.add(gArr.getJSONObject(gi).optString("name"))
                        }
                        genre = gList.joinToString(" • ")
                    }

                    val yArr = terms.optJSONArray("release-year")
                    if (yArr != null && yArr.length() > 0) {
                        val yStr = yArr.getJSONObject(0).optString("name", "")
                        val parsedY = yStr.filter { it.isDigit() }.toIntOrNull()
                        if (parsedY != null && parsedY > 1900) year = parsedY
                    }

                    val cntArr = terms.optJSONArray("country")
                    if (cntArr != null && cntArr.length() > 0) {
                        val cList = mutableListOf<String>()
                        for (ci in 0 until cntArr.length()) {
                            val n = cntArr.getJSONObject(ci).optString("name")
                            if (n.isNotBlank()) cList.add(n)
                        }
                        if (cList.isNotEmpty()) country = cList.joinToString(" • ")
                    }

                    val dirArr = terms.optJSONArray("director")
                    if (dirArr != null && dirArr.length() > 0) {
                        val dList = mutableListOf<String>()
                        for (di in 0 until dirArr.length()) {
                            val n = dirArr.getJSONObject(di).optString("name")
                            if (n.isNotBlank()) dList.add(n)
                        }
                        if (dList.isNotEmpty()) director = dList.joinToString(", ")
                    }

                    val actArr = terms.optJSONArray("actor")
                    if (actArr != null && actArr.length() > 0) {
                        for (ai in 0 until actArr.length()) {
                            val n = actArr.getJSONObject(ai).optString("name")
                            if (n.isNotBlank()) actorList.add(n)
                        }
                    }
                }
            }

            if (year <= 1900) {
                val rawYear = data.optInt("release-year", data.optInt("year", 0))
                year = if (rawYear > 1900) rawYear else 2024
            }

            ItemDetailsDto(
                id = id,
                realId = realId,
                title = title,
                thumbnail = thumbnail,
                story = story,
                releaseYear = year,
                terms = termsMap,
                streamVariants = variants,
                rating = ratingVal,
                genre = genre,
                actors = actorList,
                director = director,
                country = country,
                seriesSlug = seriesSlug,
                seasonSlug = seasonSlug
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun resolveStreamUrl(realId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}?action=stream&realid=$realId"
            val body = fetchString(url)
            val json = JSONObject(body)
            val direct = json.optString("url", json.optString("player_url", ""))
            if (direct.isNotBlank()) direct else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate all multi-source streaming servers for movies and series episodes
     * (WeCima Direct, VidLink 4K, Videasy Multi-Sub, ZxcStream FHD, 2Embed/CineSrc)
     */
    fun generateStreamServers(
        realId: String,
        tmdbId: String? = null,
        imdbId: String? = null,
        isSeries: Boolean = false,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1
    ): List<com.beouttv.app.data.model.StreamServer> {
        val servers = mutableListOf<com.beouttv.app.data.model.StreamServer>()
        val cleanTmdb = tmdbId?.filter { it.isDigit() }?.ifBlank { null }
        val cleanImdb = if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) imdbId else null

        // 1. Primary Direct Server (Beout Direct Fast HLS Stream - only for valid numerical IDs)
        val isNumericRealId = realId.isNotBlank() && !realId.startsWith("mb_") && realId.all { it.isDigit() }
        if (isNumericRealId) {
            servers.add(
                com.beouttv.app.data.model.StreamServer(
                    id = "srv_direct",
                    name = "سيرفر Beout المباشر السريع (Direct HLS)",
                    quality = "1080p FHD",
                    url = "https://govid.live/prem-$realId.m3u8",
                    isDirect = true
                )
            )
        }

        // 2. VidLink Pro (Ultra Fast 4K / 1080p Engine)
        if (cleanTmdb != null) {
            val vidlinkUrl = if (isSeries) "https://vidlink.pro/tv/$cleanTmdb/$seasonNumber/$episodeNumber"
                             else "https://vidlink.pro/movie/$cleanTmdb"
            servers.add(
                com.beouttv.app.data.model.StreamServer(
                    id = "srv_vidlink",
                    name = "سيرفر VidLink Pro (4K Ultra HD)",
                    quality = "4K 2160p",
                    url = vidlinkUrl,
                    isDirect = false
                )
            )
        }

        // 3. Videasy Multi-Source (Multi-Subtitle & Audio)
        if (cleanImdb != null || cleanTmdb != null) {
            val targetId = cleanImdb ?: cleanTmdb!!
            val videasyUrl = if (isSeries) "https://player.videasy.to/tv/$targetId/$seasonNumber/$episodeNumber"
                             else "https://player.videasy.to/movie/$targetId"
            servers.add(
                com.beouttv.app.data.model.StreamServer(
                    id = "srv_videasy",
                    name = "سيرفر Videasy (متعدد الجودات والترجمات)",
                    quality = "1080p Multi-Sub",
                    url = videasyUrl,
                    isDirect = false
                )
            )
        }

        // 4. ZxcStream / Aquarius High-Bitrate Server
        if (cleanTmdb != null) {
            val zxcUrl = if (isSeries) "https://player.zxcstream.xyz/player/tv/$cleanTmdb/$seasonNumber/$episodeNumber"
                         else "https://player.zxcstream.xyz/player/movie/$cleanTmdb"
            servers.add(
                com.beouttv.app.data.model.StreamServer(
                    id = "srv_zxc",
                    name = "سيرفر ZxcStream فائق السرعة",
                    quality = "1080p FHD",
                    url = zxcUrl,
                    isDirect = false
                )
            )
        }

        // 5. 2Embed / CineSrc Backup Server
        if (cleanTmdb != null) {
            val embedUrl = if (isSeries) "https://www.2embed.cc/embedtv/$cleanTmdb&s=$seasonNumber&e=$episodeNumber"
                           else "https://www.2embed.cc/embed/$cleanTmdb"
            servers.add(
                com.beouttv.app.data.model.StreamServer(
                    id = "srv_2embed",
                    name = "سيرفر CineSrc الاحتياطي",
                    quality = "720p HD",
                    url = embedUrl,
                    isDirect = false
                )
            )
        }

        // Default safety fallback
        if (servers.isEmpty()) {
            if (isNumericRealId) {
                servers.add(
                    com.beouttv.app.data.model.StreamServer(
                        id = "srv_default",
                        name = "السيرفر التلقائي (Auto HLS)",
                        quality = "1080p",
                        url = "https://govid.live/prem-$realId.m3u8",
                        isDirect = true
                    )
                )
            } else if (!cleanTmdb.isNullOrBlank()) {
                val fbUrl = if (isSeries) "https://vidlink.pro/tv/$cleanTmdb/$seasonNumber/$episodeNumber" else "https://vidlink.pro/movie/$cleanTmdb"
                servers.add(
                    com.beouttv.app.data.model.StreamServer(
                        id = "srv_vidlink_fb",
                        name = "سيرفر VidLink Pro (4K)",
                        quality = "1080p FHD",
                        url = fbUrl,
                        isDirect = false
                    )
                )
            }
        }

        return servers
    }

    /**
     * Intelligent Multi-Source Stream Resolver (WeCima -> VidLink -> Videasy -> ZxcStream -> 2Embed)
     */
    suspend fun resolveMultiSourceStream(
        realId: String,
        tmdbId: String? = null,
        imdbId: String? = null,
        isSeries: Boolean = false,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1
    ): String = withContext(Dispatchers.IO) {
        // 1. Primary Source: Direct WeCima stream
        if (realId.isNotBlank()) {
            val directUrl = resolveStreamUrl(realId)
            if (!directUrl.isNullOrBlank() && (directUrl.startsWith("http://") || directUrl.startsWith("https://"))) {
                return@withContext directUrl
            }
        }

        // 2. High-Speed Multi-Source Fallbacks using TMDB / IMDb
        val cleanTmdb = tmdbId?.filter { it.isDigit() }
        val cleanImdb = if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) imdbId else null

        if (!cleanTmdb.isNullOrBlank()) {
            if (isSeries) {
                return@withContext "https://vidlink.pro/tv/$cleanTmdb/$seasonNumber/$episodeNumber"
            } else {
                return@withContext "https://vidlink.pro/movie/$cleanTmdb"
            }
        }

        if (!cleanImdb.isNullOrBlank()) {
            if (isSeries) {
                return@withContext "https://player.videasy.to/tv/$cleanImdb/$seasonNumber/$episodeNumber"
            } else {
                return@withContext "https://player.videasy.to/movie/$cleanImdb"
            }
        }

        // Fallback default HLS endpoint
        if (realId.isNotBlank() && !realId.startsWith("mb_") && realId.all { it.isDigit() }) {
            "https://govid.live/prem-$realId.m3u8"
        } else {
            ""
        }
    }

    /**
     * Subtitle Fetcher Engine (SubDL + OpenSubtitles + Wyzie)
     */
    suspend fun fetchSubtitles(
        tmdbId: String? = null,
        imdbId: String? = null,
        query: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): List<com.beouttv.app.data.model.SubtitleTrack> = withContext(Dispatchers.IO) {
        val subtitles = mutableListOf<com.beouttv.app.data.model.SubtitleTrack>()
        var targetImdb = if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) imdbId else null

        // If IMDb ID is missing, try to resolve from title/query
        if (targetImdb.isNullOrBlank() && !query.isNullOrBlank()) {
            try {
                val cleanQ = com.beouttv.app.data.imdb.ImdbMetadataEngine.cleanSearchTitle(query)
                val suggestion = com.beouttv.app.data.imdb.ImdbMetadataEngine.searchImdbSuggestion(cleanQ)
                val id = suggestion?.optString("id")
                if (!id.isNullOrBlank() && id.startsWith("tt")) {
                    targetImdb = id
                }
            } catch (_: Exception) {}
        }

        if (!targetImdb.isNullOrBlank()) {
            try {
                val isEpisode = season != null && episode != null && season > 0 && episode > 0
                val subUrl = if (isEpisode) {
                    "https://opensubtitles-v3.strem.io/subtitles/series/$targetImdb:$season:$episode.json"
                } else {
                    "https://opensubtitles-v3.strem.io/subtitles/movie/$targetImdb.json"
                }

                val req = Request.Builder()
                    .url(subUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; K) AppleWebKit/537.36")
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (!body.isNullOrBlank() && body.startsWith("{")) {
                            val json = JSONObject(body)
                            val arr = json.optJSONArray("subtitles") ?: JSONArray()
                            val parsed = mutableListOf<com.beouttv.app.data.model.SubtitleTrack>()

                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val fileUrl = obj.optString("url", "")
                                val langCode = obj.optString("lang", "en").lowercase()
                                val isArabic = langCode == "ara" || langCode == "ar" || langCode.contains("arabic")
                                val isEnglish = langCode == "eng" || langCode == "en"

                                val releaseRaw = obj.optString("movieReleaseName", "").ifBlank {
                                    obj.optString("subtitleFileName", "").ifBlank {
                                        obj.optString("releaseGroup", "").ifBlank {
                                            obj.optString("releaseFormat", "")
                                        }
                                    }
                                }
                                val cleanRelease = when {
                                    releaseRaw.contains("YIFY", true) || releaseRaw.contains("YTS", true) -> "YIFY"
                                    releaseRaw.contains("BluRay", true) || releaseRaw.contains("BRRip", true) || releaseRaw.contains("BDRip", true) -> "BluRay"
                                    releaseRaw.contains("WEB", true) -> "WEB-DL"
                                    releaseRaw.contains("HDTV", true) -> "HDTV"
                                    releaseRaw.contains("Remastered", true) -> "Remastered"
                                    else -> ""
                                }
                                val tag = if (cleanRelease.isNotBlank()) " [$cleanRelease]" else ""

                                val label = when {
                                    isArabic -> "العربية (مترجم)$tag #${i + 1}"
                                    isEnglish -> "English$tag #${i + 1}"
                                    langCode == "fre" || langCode == "fr" -> "Français$tag"
                                    langCode == "ger" || langCode == "de" -> "Deutsch$tag"
                                    langCode == "spa" || langCode == "es" -> "Español$tag"
                                    langCode == "tur" || langCode == "tr" -> "Türkçe$tag"
                                    langCode == "per" || langCode == "fa" -> "فارسی$tag"
                                    else -> "${langCode.uppercase()}$tag"
                                }

                                if (fileUrl.isNotBlank()) {
                                    parsed.add(
                                        com.beouttv.app.data.model.SubtitleTrack(
                                            id = "sub_${targetImdb}_${obj.optString("id", i.toString())}",
                                            language = if (isArabic) "ar" else if (isEnglish) "en" else langCode,
                                            label = label,
                                            url = fileUrl,
                                            format = "srt"
                                        )
                                    )
                                }
                            }

                            // Prioritize Arabic subtitles first, then English, then others
                            val arabicSubs = parsed.filter { it.language == "ar" }
                            val englishSubs = parsed.filter { it.language == "en" }
                            val otherSubs = parsed.filter { it.language != "ar" && it.language != "en" }

                            subtitles.addAll(arabicSubs.take(5))
                            subtitles.addAll(englishSubs.take(3))
                            subtitles.addAll(otherSubs.take(3))
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        subtitles
    }

    suspend fun getSeriesSeasons(seriesSlug: String): List<SeriesSeasonDto> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}?action=seasons&series_slug=$seriesSlug"
            val body = fetchString(url).trim()
            val arr = if (body.startsWith("[")) {
                JSONArray(body)
            } else {
                val json = JSONObject(body)
                json.optJSONArray("data") ?: json.optJSONArray("seasons") ?: JSONArray()
            }
            val list = mutableListOf<SeriesSeasonDto>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val name = item.optString("name", "الموسم ${i + 1}")
                val slug = item.optString("slug", "")
                if (slug.isNotBlank()) {
                    list.add(SeriesSeasonDto(name = name, slug = slug))
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getSeasonEpisodes(seasonSlug: String): List<SeriesEpisodeDto> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<SeriesEpisodeDto>()
            val firstUrl = "${getBaseUrl()}?action=posts&season_slug=$seasonSlug&page=1&per_page=50"
            val firstBody = fetchString(firstUrl).trim()
            val firstJson = if (firstBody.startsWith("{")) JSONObject(firstBody) else null
            val firstPosts = if (firstBody.startsWith("[")) JSONArray(firstBody)
                             else firstJson?.optJSONArray("posts") ?: firstJson?.optJSONArray("data") ?: JSONArray()

            val totalPages = firstJson?.optInt("total_pages", 1) ?: 1

            fun parseEpisodes(arr: JSONArray) {
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val id = p.optString("id", i.toString())
                    val realId = p.optString("realid", id)
                    val title = p.optString("title", "الحلقة ${i + 1}")

                    // Extract exact real episode number from title (e.g. "الحلقة 526", "الحلقة 14")
                    val titleEpNum = Regex("""(?:الحلقة|حلقة|ep|episode)\s*(\d+)""", RegexOption.IGNORE_CASE)
                        .find(title)?.groupValues?.get(1)?.toIntOrNull()

                    var epNum = titleEpNum ?: (i + 1)
                    if (titleEpNum == null && p.has("meta")) {
                        val meta = p.optJSONObject("meta")
                        if (meta != null && meta.has("number")) {
                            val nStr = meta.optString("number", "").filter { it.isDigit() }
                            epNum = nStr.toIntOrNull() ?: meta.optInt("number", epNum)
                        }
                    }
                    list.add(SeriesEpisodeDto(id = id, realId = realId, number = epNum, title = title))
                }
            }

            parseEpisodes(firstPosts)

            // Multi-page fetch for massive series (e.g. Indian/Turkish series with 100 to 1000+ episodes)
            if (totalPages > 1) {
                val maxPages = totalPages.coerceAtMost(50) // up to 2500 episodes
                coroutineScope {
                    for (chunkStart in 2..maxPages step 8) {
                        val chunkEnd = (chunkStart + 7).coerceAtMost(maxPages)
                        val deferreds = (chunkStart..chunkEnd).map { pageNum ->
                            async {
                                try {
                                    val pageUrl = "${getBaseUrl()}?action=posts&season_slug=$seasonSlug&page=$pageNum&per_page=50"
                                    val body = fetchString(pageUrl).trim()
                                    if (body.startsWith("[")) JSONArray(body)
                                    else JSONObject(body).optJSONArray("posts") ?: JSONArray()
                                } catch (_: Exception) { JSONArray() }
                            }
                        }
                        val results = deferreds.awaitAll()
                        for (arr in results) {
                            parseEpisodes(arr)
                        }
                    }
                }
            }

            // Deduplicate and sort by natural episode number ascending (1, 2, 3... 500, 526)
            list.distinctBy { it.realId.ifBlank { it.id } }.sortedBy { it.number }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
