package com.beouttv.app.data.api.moviebox

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

object MovieBoxApiClient {
    private const val BASE_URL = "https://h5.aoneroom.com"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("x-tr-devtype", "h5")
                    .header("x-tr-region", "CN")
                    .header("x-md-global-color", "lane4")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val gson: Gson by lazy { Gson() }

    suspend fun getTrending(page: Int = 1, perPage: Int = 20): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val safePerPage = perPage.coerceIn(1, 20)
        val formBody = FormBody.Builder()
            .add("page", page.toString())
            .add("perPage", safePerPage.toString())
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/mini/trending")
            .post(formBody)
            .build()

        executeListRequest(request)
    }

    suspend fun getMovies(page: Int = 1, perPage: Int = 20, genre: String = ""): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val safePerPage = perPage.coerceIn(1, 20)
        val builder = FormBody.Builder()
            .add("channelId", "1")
            .add("subjectType", "1")
            .add("page", page.toString())
            .add("perPage", safePerPage.toString())

        if (genre.isNotBlank() && genre != "all") {
            builder.add("genre", genre)
        }

        val request = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/mini/subject-list")
            .post(builder.build())
            .build()

        executeListRequest(request)
    }

    suspend fun getSeries(page: Int = 1, perPage: Int = 20, genre: String = ""): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val safePerPage = perPage.coerceIn(1, 20)
        val builder = FormBody.Builder()
            .add("channelId", "2")
            .add("subjectType", "2")
            .add("page", page.toString())
            .add("perPage", safePerPage.toString())

        if (genre.isNotBlank() && genre != "all") {
            builder.add("genre", genre)
        }

        val request = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/mini/subject-list")
            .post(builder.build())
            .build()

        executeListRequest(request)
    }

    suspend fun getAnime(page: Int = 1, perPage: Int = 20, genre: String = ""): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val safePerPage = perPage.coerceIn(1, 20)
        when {
            genre.equals("movies", true) -> {
                val formBody = FormBody.Builder()
                    .add("channelId", "1")
                    .add("subjectType", "1")
                    .add("genre", "Anime")
                    .add("page", page.toString())
                    .add("perPage", safePerPage.toString())
                    .build()
                val request = Request.Builder().url("$BASE_URL/wefeed-h5-bff/mini/subject-list").post(formBody).build()
                executeListRequest(request)
            }
            genre.equals("series", true) -> {
                val formBody = FormBody.Builder()
                    .add("channelId", "2")
                    .add("subjectType", "2")
                    .add("genre", "Anime")
                    .add("page", page.toString())
                    .add("perPage", safePerPage.toString())
                    .build()
                val request = Request.Builder().url("$BASE_URL/wefeed-h5-bff/mini/subject-list").post(formBody).build()
                executeListRequest(request)
            }
            else -> {
                val moviesDeferred = async {
                    val formBody = FormBody.Builder()
                        .add("channelId", "1")
                        .add("subjectType", "1")
                        .add("genre", "Anime")
                        .add("page", page.toString())
                        .add("perPage", safePerPage.toString())
                        .build()
                    val req = Request.Builder().url("$BASE_URL/wefeed-h5-bff/mini/subject-list").post(formBody).build()
                    executeListRequest(req)
                }
                val seriesDeferred = async {
                    val formBody = FormBody.Builder()
                        .add("channelId", "2")
                        .add("subjectType", "2")
                        .add("genre", "Anime")
                        .add("page", page.toString())
                        .add("perPage", safePerPage.toString())
                        .build()
                    val req = Request.Builder().url("$BASE_URL/wefeed-h5-bff/mini/subject-list").post(formBody).build()
                    executeListRequest(req)
                }
                val mList = moviesDeferred.await()
                val sList = seriesDeferred.await()
                val combined = mutableListOf<MovieBoxItem>()
                val maxLen = maxOf(mList.size, sList.size)
                for (i in 0 until maxLen) {
                    if (i < mList.size) combined.add(mList[i])
                    if (i < sList.size) combined.add(sList[i])
                }
                combined
            }
        }
    }

    suspend fun getAsianMovies(page: Int = 1, perPage: Int = 20): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val safePerPage = perPage.coerceIn(1, 20)
        val asianCuratedQueries = listOf(
            "Parasite", "Train to Busan", "Ip Man", "The Raid", "Oldboy",
            "Godzilla", "Spirited Away", "Vincenzo", "Crash Landing",
            "Kingdom", "Peninsula", "Demon Slayer", "Ghibli", "Rurouni Kenshin"
        )
        val targetQueries = if (page == 1) {
            listOf("Parasite", "Train to Busan", "Ip Man", "The Raid", "Oldboy", "Godzilla")
        } else {
            val startIdx = ((page - 1) * 3) % asianCuratedQueries.size
            listOf(
                asianCuratedQueries[startIdx],
                asianCuratedQueries[(startIdx + 1) % asianCuratedQueries.size],
                asianCuratedQueries[(startIdx + 2) % asianCuratedQueries.size]
            )
        }

        val deferredList: List<Deferred<List<MovieBoxItem>>> = targetQueries.map { q ->
            async {
                try { search(q) } catch (_: Exception) { emptyList<MovieBoxItem>() }
            }
        }
        val results: List<MovieBoxItem> = deferredList.awaitAll().flatten()
        val filtered = results.filter { item: MovieBoxItem ->
            !com.beouttv.app.data.filter.ContentFilterEngine.isContentBlocked(item.title, item.genre, item.description) &&
            !item.title.contains("midnight", ignoreCase = true) &&
            !item.title.contains("ميدنايت", ignoreCase = true) &&
            (item.cover?.url?.isNotBlank() == true)
        }.distinctBy { it.subjectId }

        if (filtered.isNotEmpty()) {
            filtered.take(safePerPage)
        } else {
            getMovies(page = page, perPage = safePerPage, genre = "Action")
        }
    }

    suspend fun search(query: String): List<MovieBoxItem> = withContext(Dispatchers.IO) {
        val encoded = try {
            java.net.URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) { query }

        val request = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/mini/search?keyword=$encoded")
            .get()
            .build()

        executeListRequest(request)
    }

    suspend fun getSubjectDetail(subjectId: String): MovieBoxSubjectDetail? = withContext(Dispatchers.IO) {
        val cleanId = subjectId.removePrefix("mb_")
        val request = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/mini/subject_detail?subjectId=$cleanId")
            .get()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val bodyString = response.body?.string() ?: return@withContext null
            val type = object : TypeToken<MovieBoxResponse<MovieBoxSubjectDetail>>() {}.type
            val result: MovieBoxResponse<MovieBoxSubjectDetail> = gson.fromJson(bodyString, type)
            result.data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun executeListRequest(request: Request): List<MovieBoxItem> {
        return try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val bodyString = response.body?.string() ?: return emptyList()
            val type = object : TypeToken<MovieBoxResponse<MovieBoxListData>>() {}.type
            val result: MovieBoxResponse<MovieBoxListData> = gson.fromJson(bodyString, type)
            result.data?.items ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
