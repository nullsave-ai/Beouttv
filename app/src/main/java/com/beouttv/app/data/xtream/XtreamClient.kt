package com.beouttv.app.data.xtream

import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Episode
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Season
import com.beouttv.app.data.model.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Network client for fetching data from Xtream Codes API (player_api.php)
 * and universal M3U/M3U8 playlists.
 */
class XtreamClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    fun cleanBaseUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        while (u.endsWith("/")) {
            u = u.substring(0, u.length - 1)
        }
        return u
    }

    /**
     * Authenticates with Xtream Codes API and extracts user profile and expiration date.
     */
    suspend fun authenticate(
        serverUrl: String,
        user: String,
        pass: String
    ): XtreamAuthResponse = withContext(Dispatchers.IO) {
        val base = cleanBaseUrl(serverUrl)
        val authUrl = "$base/player_api.php?username=$user&password=$pass"
        try {
            val req = Request.Builder()
                .url(authUrl)
                .header("User-Agent", "IPTVSmartersPro/3.1.5 (Android)")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext XtreamAuthResponse(
                    success = false,
                    message = "فشل الاتصال بالخادم (رمز: ${resp.code})"
                )
            }

            val body = resp.body?.string() ?: ""
            if (body.isBlank() || !body.trim().startsWith("{")) {
                return@withContext XtreamAuthResponse(
                    success = false,
                    message = "استجابة غير صحيحة من الخادم"
                )
            }

            val json = JSONObject(body)
            val userInfo = json.optJSONObject("user_info")
            val serverInfo = json.optJSONObject("server_info")

            if (userInfo == null) {
                return@withContext XtreamAuthResponse(
                    success = false,
                    message = "اسم المستخدم أو كلمة المرور غير صحيحة"
                )
            }

            val auth = userInfo.optInt("auth", 0)
            val status = userInfo.optString("status", "")
            if (auth != 1 || status.equals("Disabled", ignoreCase = true) || status.equals("Banned", ignoreCase = true)) {
                return@withContext XtreamAuthResponse(
                    success = false,
                    message = if (status.isNotBlank()) "حالة الحساب: $status" else "بيانات الدخول غير صحيحة"
                )
            }

            // Parse exp_date (can be null, numeric timestamp string, or 0)
            val expRaw = userInfo.optString("exp_date", "").trim()
            val expTimestamp = expRaw.toLongOrNull() ?: 0L

            val serverName = serverInfo?.optString("server_name", "") ?: ""
            val maxConn = userInfo.optString("max_connections", "1")

            XtreamAuthResponse(
                success = true,
                serverName = if (serverName.isNotBlank()) serverName else "Xtream Server",
                username = userInfo.optString("username", user),
                status = status,
                expDate = expTimestamp,
                maxConnections = maxConn
            )
        } catch (e: Exception) {
            e.printStackTrace()
            XtreamAuthResponse(
                success = false,
                message = "خطأ أثناء الاتصال: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Fetches live channels from Xtream Codes API.
     */
    suspend fun fetchLiveChannels(
        serverUrl: String,
        user: String,
        pass: String,
        accountId: String,
        accountName: String
    ): List<Channel> = withContext(Dispatchers.IO) {
        val base = cleanBaseUrl(serverUrl)
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_live_streams"
        val channels = mutableListOf<Channel>()
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/3.1.5 (Android)")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()

            val body = resp.body?.string() ?: ""
            if (!body.trim().startsWith("[")) return@withContext emptyList()

            val array = JSONArray(body)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val streamId = item.optString("stream_id", "")
                if (streamId.isBlank()) continue

                val name = item.optString("name", "قناة ${i + 1}")
                val icon = item.optString("stream_icon", "")
                    .ifBlank { item.optString("logo", "") }
                    .ifBlank { item.optString("tvg_logo", "") }
                    .ifBlank { item.optString("icon", "") }
                val category = item.optString("category_name", "اشتراكي الشخصي").ifBlank { "اشتراكي الشخصي" }
                val streamUrl = "$base/live/$user/$pass/$streamId.ts"

                channels.add(
                    Channel(
                        id = "xtream_${accountId}_ch_${streamId}",
                        name = name,
                        logoUrl = icon,
                        category = "اشتراكي الشخصي",
                        isLive = true,
                        streamUrl = streamUrl,
                        groupId = accountName,
                        serverId = accountId
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        channels
    }

    /**
     * Fetches VOD movies from Xtream Codes API.
     */
    suspend fun fetchVodMovies(
        serverUrl: String,
        user: String,
        pass: String,
        accountId: String,
        accountName: String
    ): List<Movie> = withContext(Dispatchers.IO) {
        val base = cleanBaseUrl(serverUrl)
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_vod_streams"
        val movies = mutableListOf<Movie>()
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/3.1.5 (Android)")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()

            val body = resp.body?.string() ?: ""
            if (!body.trim().startsWith("[")) return@withContext emptyList()

            val array = JSONArray(body)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val streamId = item.optString("stream_id", "")
                if (streamId.isBlank()) continue

                val name = item.optString("name", "فيلم")
                val icon = item.optString("stream_icon", "")
                val ext = item.optString("container_extension", "mp4").ifBlank { "mp4" }
                val rating = item.optDouble("rating", 0.0).toFloat().coerceIn(0f, 10f)
                val category = item.optString("category_name", "أفلام").ifBlank { "أفلام" }
                val streamUrl = "$base/movie/$user/$pass/$streamId.$ext"

                movies.add(
                    Movie(
                        id = "xtream_${accountId}_mov_${streamId}",
                        title = name,
                        posterUrl = icon,
                        backdropUrl = icon,
                        year = item.optInt("year", 2025),
                        rating = rating,
                        genre = category,
                        category = "اشتراكي الشخصي",
                        streamUrl = streamUrl,
                        description = item.optString("plot", ""),
                        director = accountName,
                        quality = "HD"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        movies
    }

    /**
     * Fetches series from Xtream Codes API.
     */
    suspend fun fetchSeries(
        serverUrl: String,
        user: String,
        pass: String,
        accountId: String,
        accountName: String
    ): List<Series> = withContext(Dispatchers.IO) {
        val base = cleanBaseUrl(serverUrl)
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_series"
        val seriesList = mutableListOf<Series>()
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/3.1.5 (Android)")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()

            val body = resp.body?.string() ?: ""
            if (!body.trim().startsWith("[")) return@withContext emptyList()

            val array = JSONArray(body)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val seriesId = item.optString("series_id", "")
                if (seriesId.isBlank()) continue

                val name = item.optString("name", "مسلسل")
                val cover = item.optString("cover", "")
                val rating = item.optDouble("rating", 0.0).toFloat().coerceIn(0f, 10f)
                val category = item.optString("category_name", "مسلسلات").ifBlank { "مسلسلات" }
                val plot = item.optString("plot", "")
                val year = item.optInt("year", 2025)

                seriesList.add(
                    Series(
                        id = "xtream_${accountId}_ser_${seriesId}",
                        title = name,
                        posterUrl = cover,
                        backdropUrl = cover,
                        year = year,
                        rating = rating,
                        genre = "$category • اشتراك شخصي",
                        description = plot,
                        originalTitle = seriesId // Keep seriesId in originalTitle for episodes lookup
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        seriesList
    }

    /**
     * Fetches full metadata and backdrop for a VOD movie from Xtream Codes API.
     */
    suspend fun fetchVodDetails(
        serverUrl: String,
        user: String,
        pass: String,
        movie: Movie,
        accountId: String
    ): Movie = withContext(Dispatchers.IO) {
        val base = cleanBaseUrl(serverUrl)
        val realStreamId = movie.id.substringAfterLast("_")
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_vod_info&vod_id=$realStreamId"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/3.1.5 (Android)")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext movie

            val body = resp.body?.string() ?: ""
            if (!body.trim().startsWith("{")) return@withContext movie

            val json = JSONObject(body)
            val info = json.optJSONObject("info") ?: return@withContext movie

            val plot = info.optString("plot", "").ifBlank { movie.description }
            val coverBig = info.optString("cover_big", "")
                .ifBlank { info.optString("movie_image", "") }
                .ifBlank { movie.posterUrl }
            val backdrop = if (coverBig.isNotBlank()) coverBig else movie.backdropUrl
            val poster = if (coverBig.isNotBlank()) coverBig else movie.posterUrl
            val rating = info.optDouble("rating", movie.rating.toDouble()).toFloat().coerceIn(0f, 10f)
            val genre = info.optString("genre", "").ifBlank { movie.genre }
            val director = info.optString("director", "").ifBlank { movie.director }
            val castStr = info.optString("cast", "")
            val castList: List<String> = if (castStr.isNotBlank()) {
                castStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                movie.cast
            }
            val releaseDate = info.optString("releasedate", "").ifBlank { info.optString("release_date", "") }
            val year = releaseDate.take(4).toIntOrNull() ?: movie.year

            movie.copy(
                description = plot,
                posterUrl = poster,
                backdropUrl = backdrop,
                rating = rating,
                genre = genre,
                director = director,
                cast = castList,
                year = year
            )
        } catch (e: Exception) {
            e.printStackTrace()
            movie
        }
    }

    /**
     * Fetches seasons, episodes and full metadata for a specific series.
     */
    suspend fun fetchSeriesEpisodes(
        serverUrl: String,
        user: String,
        pass: String,
        series: Series,
        accountId: String
    ): Series = withContext(Dispatchers.IO) {
        val base = cleanBaseUrl(serverUrl)
        val realSeriesId = series.originalTitle ?: series.id.substringAfterLast("_")
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$realSeriesId"

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/3.1.5 (Android)")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext series

            val body = resp.body?.string() ?: ""
            if (!body.trim().startsWith("{")) return@withContext series

            val json = JSONObject(body)
            val infoObj = json.optJSONObject("info")
            val plot = infoObj?.optString("plot", "")?.ifBlank { series.description } ?: series.description
            val cover = infoObj?.optString("cover", "")?.ifBlank { series.posterUrl } ?: series.posterUrl
            val backdropArray = infoObj?.optJSONArray("backdrop_path")
            val backdrop = if (backdropArray != null && backdropArray.length() > 0) {
                backdropArray.optString(0, cover)
            } else cover
            val rating = infoObj?.optDouble("rating", series.rating.toDouble())?.toFloat()?.coerceIn(0f, 10f) ?: series.rating
            val genre = infoObj?.optString("genre", "")?.ifBlank { series.genre } ?: series.genre
            val releaseDate = infoObj?.optString("releaseDate", "")?.ifBlank { infoObj.optString("release_date", "") }.orEmpty()
            val year = releaseDate.take(4).toIntOrNull() ?: series.year

            val episodesObj = json.optJSONObject("episodes") ?: return@withContext series.copy(
                description = plot,
                posterUrl = cover,
                backdropUrl = backdrop,
                rating = rating,
                genre = genre,
                year = year
            )

            val seasonsList = mutableListOf<Season>()
            val keys = episodesObj.keys()
            while (keys.hasNext()) {
                val seasonNumStr = keys.next()
                val seasonNum = seasonNumStr.toIntOrNull() ?: 1
                val epArray = episodesObj.optJSONArray(seasonNumStr) ?: continue

                val epList = mutableListOf<Episode>()
                for (j in 0 until epArray.length()) {
                    val epJson = epArray.optJSONObject(j) ?: continue
                    val epId = epJson.optString("id", "")
                    val epNum = epJson.optInt("episode_num", j + 1)
                    val title = epJson.optString("title", "الحلقة $epNum")
                    val ext = epJson.optString("container_extension", "mp4").ifBlank { "mp4" }
                    val streamUrl = "$base/series/$user/$pass/$epId.$ext"

                    epList.add(
                        Episode(
                            id = "xtream_${accountId}_ep_${epId}",
                            episodeNumber = epNum,
                            title = title,
                            streamUrl = streamUrl,
                            seriesId = series.id,
                            seasonNumber = seasonNum
                        )
                    )
                }

                seasonsList.add(
                    Season(
                        seasonNumber = seasonNum,
                        episodes = epList.sortedBy { it.episodeNumber }
                    )
                )
            }

            series.copy(
                description = plot,
                posterUrl = cover,
                backdropUrl = backdrop,
                rating = rating,
                genre = genre,
                year = year,
                seasons = seasonsList.sortedBy { it.seasonNumber }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            series
        }
    }

    /**
     * Parses an M3U/M3U8 playlist URL or content.
     */
    suspend fun fetchM3U(
        m3uUrl: String,
        accountId: String,
        accountName: String
    ): Triple<List<Channel>, List<Movie>, List<Series>> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        val movies = mutableListOf<Movie>()
        val series = mutableListOf<Series>()

        try {
            val req = Request.Builder()
                .url(m3uUrl)
                .header("User-Agent", "VLC/3.0.18 LibVLC/3.0.18")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext Triple(emptyList(), emptyList(), emptyList())

            val body = resp.body?.string() ?: ""
            val reader = BufferedReader(StringReader(body))

            var line: String?
            var currentTvgName = ""
            var currentLogo = ""
            var currentGroup = ""

            val titlePattern = Pattern.compile("tvg-name=\"([^\"]*)\"|group-title=\"([^\"]*)\"|tvg-logo=\"([^\"]*)\"")

            var index = 0
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.startsWith("#EXTINF:")) {
                    index++
                    // Parse metadata
                    val commaIdx = l.lastIndexOf(',')
                    val titleFromEnd = if (commaIdx != -1 && commaIdx < l.length - 1) {
                        l.substring(commaIdx + 1).trim()
                    } else ""

                    val matcher = titlePattern.matcher(l)
                    currentTvgName = titleFromEnd
                    currentGroup = "اشتراكي الشخصي"
                    currentLogo = ""

                    while (matcher.find()) {
                        if (matcher.group(1) != null && currentTvgName.isBlank()) currentTvgName = matcher.group(1)!!
                        if (matcher.group(2) != null) currentGroup = matcher.group(2)!!
                        if (matcher.group(3) != null) currentLogo = matcher.group(3)!!
                    }
                    if (currentTvgName.isBlank()) currentTvgName = "عنصر $index"
                } else if (l.isNotBlank() && !l.startsWith("#")) {
                    val streamUrl = l
                    val isMovie = streamUrl.contains("/movie/") || streamUrl.endsWith(".mp4", ignoreCase = true) || streamUrl.endsWith(".mkv", ignoreCase = true) || currentGroup.contains("Movie", ignoreCase = true) || currentGroup.contains("أفلام", ignoreCase = true)
                    val isSeries = streamUrl.contains("/series/") || currentGroup.contains("Series", ignoreCase = true) || currentGroup.contains("مسلسلات", ignoreCase = true)

                    if (isMovie) {
                        movies.add(
                            Movie(
                                id = "xtream_${accountId}_m3u_mov_$index",
                                title = currentTvgName,
                                posterUrl = currentLogo,
                                backdropUrl = currentLogo,
                                year = 2025,
                                rating = 8.0f,
                                genre = currentGroup,
                                category = "اشتراكي الشخصي",
                                streamUrl = streamUrl,
                                director = accountName,
                                quality = "HD"
                            )
                        )
                    } else if (isSeries) {
                        series.add(
                            Series(
                                id = "xtream_${accountId}_m3u_ser_$index",
                                title = currentTvgName,
                                posterUrl = currentLogo,
                                backdropUrl = currentLogo,
                                year = 2025,
                                rating = 8.0f,
                                genre = currentGroup,
                                seasons = listOf(
                                    Season(
                                        seasonNumber = 1,
                                        episodes = listOf(
                                            Episode(
                                                id = "xtream_${accountId}_m3u_ep_$index",
                                                episodeNumber = 1,
                                                title = currentTvgName,
                                                streamUrl = streamUrl,
                                                seriesId = "xtream_${accountId}_m3u_ser_$index",
                                                seasonNumber = 1
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    } else {
                        channels.add(
                            Channel(
                                id = "xtream_${accountId}_m3u_ch_$index",
                                name = currentTvgName,
                                logoUrl = currentLogo,
                                category = "اشتراكي الشخصي",
                                isLive = true,
                                streamUrl = streamUrl,
                                groupId = accountName,
                                serverId = accountId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Triple(channels, movies, series)
    }
}
