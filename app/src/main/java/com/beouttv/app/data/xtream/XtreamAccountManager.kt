package com.beouttv.app.data.xtream

import android.content.Context
import android.content.SharedPreferences
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Singleton manager for personal Xtream Codes and M3U accounts.
 * Enforces a maximum limit of 3 accounts.
 * Leverages high-speed binary caching for instantaneous load times.
 * Handles subscription expiration detection and playback blocking.
 */
object XtreamAccountManager {

    const val MAX_ACCOUNTS = 3
    private const val PREFS_NAME = "beout_xtream_accounts_prefs"
    private const val KEY_ACCOUNTS = "saved_accounts_json"

    // Preserved browsing filter states across screens & back navigation
    var initialMoviesSourceFilter: Int = 0
    var initialSeriesSourceFilter: Int = 0
    var initialXtreamMoviesCategory: String = "all"
    var initialXtreamSeriesCategory: String = "all"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = XtreamClient()
    private var prefs: SharedPreferences? = null

    private val _accounts = MutableStateFlow<List<XtreamAccount>>(emptyList())
    val accounts: StateFlow<List<XtreamAccount>> = _accounts.asStateFlow()

    private val _xtreamChannels = MutableStateFlow<List<Channel>>(emptyList())
    val xtreamChannels: StateFlow<List<Channel>> = _xtreamChannels.asStateFlow()

    private val _xtreamMovies = MutableStateFlow<List<Movie>>(emptyList())
    val xtreamMovies: StateFlow<List<Movie>> = _xtreamMovies.asStateFlow()

    private val _xtreamSeries = MutableStateFlow<List<Series>>(emptyList())
    val xtreamSeries: StateFlow<List<Series>> = _xtreamSeries.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        XtreamProtobufCache.init(context)

        scope.launch {
            loadAccountsFromPrefs()
            loadAllCachedData()
        }
    }

    private fun loadAccountsFromPrefs() {
        val jsonStr = prefs?.getString(KEY_ACCOUNTS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<XtreamAccount>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    XtreamAccount(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        type = if (obj.optString("type") == "M3U") XtreamAccountType.M3U else XtreamAccountType.XTREAM,
                        serverUrl = obj.optString("serverUrl", ""),
                        username = obj.optString("username", ""),
                        password = obj.optString("password", ""),
                        m3uUrl = obj.optString("m3uUrl", ""),
                        expiryTimestamp = obj.optLong("expiryTimestamp", 0L),
                        isActive = obj.optBoolean("isActive", true),
                        channelsCount = obj.optInt("channelsCount", 0),
                        moviesCount = obj.optInt("moviesCount", 0),
                        seriesCount = obj.optInt("seriesCount", 0),
                        showChannels = obj.optBoolean("showChannels", true),
                        showMovies = obj.optBoolean("showMovies", true),
                        showSeries = obj.optBoolean("showSeries", true),
                        lastSyncTime = obj.optLong("lastSyncTime", 0L)
                    )
                )
            }
            _accounts.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAccountsToPrefs() {
        val array = JSONArray()
        for (acc in _accounts.value) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type.name)
                put("serverUrl", acc.serverUrl)
                put("username", acc.username)
                put("password", acc.password)
                put("m3uUrl", acc.m3uUrl)
                put("expiryTimestamp", acc.expiryTimestamp)
                put("isActive", acc.isActive)
                put("channelsCount", acc.channelsCount)
                put("moviesCount", acc.moviesCount)
                put("seriesCount", acc.seriesCount)
                put("showChannels", acc.showChannels)
                put("showMovies", acc.showMovies)
                put("showSeries", acc.showSeries)
                put("lastSyncTime", acc.lastSyncTime)
            }
            array.put(obj)
        }
        prefs?.edit()?.putString(KEY_ACCOUNTS, array.toString())?.apply()
    }

    suspend fun loadAllCachedData() {
        val currentAccounts = _accounts.value
        val allChannels = mutableListOf<Channel>()
        val allMovies = mutableListOf<Movie>()
        val allSeries = mutableListOf<Series>()

        for (acc in currentAccounts) {
            if (!acc.isActive) continue
            // Instant pure Protobuf binary load (<5ms per account)
            if (acc.showChannels) {
                val channels = XtreamProtobufCache.loadChannels(acc.id)
                allChannels.addAll(channels)
            }
            if (acc.showMovies) {
                val movies = XtreamProtobufCache.loadMovies(acc.id)
                allMovies.addAll(movies)
            }
            if (acc.showSeries) {
                val series = XtreamProtobufCache.loadSeries(acc.id)
                allSeries.addAll(series)
            }
        }

        _xtreamChannels.value = allChannels
        _xtreamMovies.value = allMovies
        _xtreamSeries.value = allSeries
    }

    /**
     * Checks if a stream or media belongs to an expired Xtream account.
     * Returns Pair(isExpired: Boolean, accountName: String?)
     */
    fun isMediaExpired(mediaIdOrUrl: String): Pair<Boolean, String?> {
        for (acc in _accounts.value) {
            // Check matching criteria: account id in mediaId or username/server in streamUrl
            val isMatch = mediaIdOrUrl.contains("xtream_${acc.id}") ||
                    (acc.username.isNotBlank() && mediaIdOrUrl.contains("/${acc.username}/")) ||
                    (acc.m3uUrl.isNotBlank() && mediaIdOrUrl.contains(acc.m3uUrl))

            if (isMatch) {
                if (acc.isExpired()) {
                    return Pair(true, acc.name)
                }
            }
        }
        return Pair(false, null)
    }

    /**
     * Finds the matching Xtream account for a given media URL or ID.
     */
    fun getAccountForMedia(mediaIdOrUrl: String): XtreamAccount? {
        return _accounts.value.firstOrNull { acc ->
            mediaIdOrUrl.contains("xtream_${acc.id}") ||
                    (acc.username.isNotBlank() && mediaIdOrUrl.contains("/${acc.username}/")) ||
                    (acc.m3uUrl.isNotBlank() && mediaIdOrUrl.contains(acc.m3uUrl))
        }
    }

    /**
     * Adds an Xtream Codes account (Server, Username, Password).
     * Enforces the MAX_ACCOUNTS = 3 limit.
     */
    suspend fun addXtreamAccount(
        name: String,
        serverUrl: String,
        user: String,
        pass: String,
        showChannels: Boolean = true,
        showMovies: Boolean = true,
        showSeries: Boolean = true,
        onProgress: (String) -> Unit = {}
    ): Result<XtreamAccount> = withContext(Dispatchers.IO) {
        if (_accounts.value.size >= MAX_ACCOUNTS) {
            return@withContext Result.failure(
                IllegalStateException("الحد الأقصى هو $MAX_ACCOUNTS حسابات شخصية فقط. يرجى حذف حساب قديم أولاً.")
            )
        }

        _isSyncing.value = true
        _syncMessage.value = "جارٍ التحقق من بيانات الاتصال بالخادم..."
        onProgress("جارٍ التحقق من بيانات الاشتراك...")

        val auth = client.authenticate(serverUrl, user, pass)
        if (!auth.success) {
            _isSyncing.value = false
            return@withContext Result.failure(Exception(auth.message))
        }

        val nowSecs = System.currentTimeMillis() / 1000L
        if (auth.expDate > 0L && nowSecs > auth.expDate) {
            _isSyncing.value = false
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val dateFormatted = sdf.format(Date(auth.expDate * 1000L))
            return@withContext Result.failure(
                Exception("عذراً، هذا الاشتراك منتهي الصلاحية بتاريخ $dateFormatted ولا يمكن إضافته.")
            )
        }

        val accountId = UUID.randomUUID().toString()
        val accountName = if (name.isNotBlank()) name else auth.serverName

        _syncMessage.value = "جارٍ تحميل القنوات التلفزيونية المباشرة..."
        onProgress("جارٍ جلب القنوات المباشرة...")
        val channels = client.fetchLiveChannels(serverUrl, user, pass, accountId, accountName)

        _syncMessage.value = "جارٍ تحميل مكتبة الأفلام (VOD)..."
        onProgress("جارٍ جلب الأفلام...")
        val movies = client.fetchVodMovies(serverUrl, user, pass, accountId, accountName)

        _syncMessage.value = "جارٍ تحميل المسلسلات..."
        onProgress("جارٍ جلب المسلسلات...")
        val series = client.fetchSeries(serverUrl, user, pass, accountId, accountName)

        _syncMessage.value = "جارٍ الحفظ والتخزين الفائق السرعة..."
        onProgress("جارٍ الحفظ والتخزين الفائق السرعة...")
        XtreamProtobufCache.saveChannels(accountId, channels)
        XtreamProtobufCache.saveMovies(accountId, movies)
        XtreamProtobufCache.saveSeries(accountId, series)

        val newAccount = XtreamAccount(
            id = accountId,
            name = accountName,
            type = XtreamAccountType.XTREAM,
            serverUrl = client.cleanBaseUrl(serverUrl),
            username = user,
            password = pass,
            expiryTimestamp = auth.expDate,
            isActive = true,
            channelsCount = channels.size,
            moviesCount = movies.size,
            seriesCount = series.size,
            showChannels = showChannels,
            showMovies = showMovies,
            showSeries = showSeries,
            lastSyncTime = System.currentTimeMillis()
        )

        _accounts.value = _accounts.value + newAccount
        saveAccountsToPrefs()
        loadAllCachedData()

        _isSyncing.value = false
        _syncMessage.value = ""
        Result.success(newAccount)
    }

    /**
     * Adds an M3U / M3U8 Playlist URL account.
     * Enforces the MAX_ACCOUNTS = 3 limit.
     */
    suspend fun addM3uAccount(
        name: String,
        m3uUrl: String,
        showChannels: Boolean = true,
        showMovies: Boolean = true,
        showSeries: Boolean = true,
        onProgress: (String) -> Unit = {}
    ): Result<XtreamAccount> = withContext(Dispatchers.IO) {
        if (_accounts.value.size >= MAX_ACCOUNTS) {
            return@withContext Result.failure(
                IllegalStateException("الحد الأقصى هو $MAX_ACCOUNTS حسابات شخصية فقط. يرجى حذف حساب قديم أولاً.")
            )
        }

        _isSyncing.value = true
        _syncMessage.value = "جارٍ قراءة وفحص ملف قائمة التشغيل M3U..."
        onProgress("جارٍ فحص الرابط...")

        val accountId = UUID.randomUUID().toString()
        val accountName = if (name.isNotBlank()) name else "قائمة تشغيل M3U"

        val (channels, movies, series) = client.fetchM3U(m3uUrl, accountId, accountName)
        if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
            _isSyncing.value = false
            return@withContext Result.failure(Exception("لم يتم العثور على أي قنوات أو وسائط صالحة في الرابط."))
        }

        _syncMessage.value = "جارٍ الحفظ والتخزين الفائق السرعة..."
        onProgress("جارٍ الحفظ والتخزين الفائق السرعة...")
        XtreamProtobufCache.saveChannels(accountId, channels)
        XtreamProtobufCache.saveMovies(accountId, movies)
        XtreamProtobufCache.saveSeries(accountId, series)

        val newAccount = XtreamAccount(
            id = accountId,
            name = accountName,
            type = XtreamAccountType.M3U,
            m3uUrl = m3uUrl.trim(),
            expiryTimestamp = 0L,
            isActive = true,
            channelsCount = channels.size,
            moviesCount = movies.size,
            seriesCount = series.size,
            showChannels = showChannels,
            showMovies = showMovies,
            showSeries = showSeries,
            lastSyncTime = System.currentTimeMillis()
        )

        _accounts.value = _accounts.value + newAccount
        saveAccountsToPrefs()
        loadAllCachedData()

        _isSyncing.value = false
        _syncMessage.value = ""
        Result.success(newAccount)
    }

    /**
     * Refreshes and updates content for an existing account.
     */
    suspend fun syncAccount(accountId: String, onProgress: (String) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        val account = _accounts.value.firstOrNull { it.id == accountId }
            ?: return@withContext Result.failure(Exception("الحساب غير موجود"))

        _isSyncing.value = true
        try {
            if (account.type == XtreamAccountType.XTREAM) {
                val auth = client.authenticate(account.serverUrl, account.username, account.password)
                val nowSecs = System.currentTimeMillis() / 1000L
                if (auth.success && auth.expDate > 0L && nowSecs > auth.expDate) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    val dateFormatted = sdf.format(Date(auth.expDate * 1000L))
                    val updated = account.copy(expiryTimestamp = auth.expDate, lastSyncTime = System.currentTimeMillis())
                    _accounts.value = _accounts.value.map { if (it.id == accountId) updated else it }
                    saveAccountsToPrefs()
                    return@withContext Result.failure(
                        Exception("عذراً، هذا الاشتراك منتهي الصلاحية بتاريخ $dateFormatted.")
                    )
                }
                val channels = client.fetchLiveChannels(account.serverUrl, account.username, account.password, account.id, account.name)
                val movies = client.fetchVodMovies(account.serverUrl, account.username, account.password, account.id, account.name)
                val series = client.fetchSeries(account.serverUrl, account.username, account.password, account.id, account.name)

                XtreamProtobufCache.saveChannels(account.id, channels)
                XtreamProtobufCache.saveMovies(account.id, movies)
                XtreamProtobufCache.saveSeries(account.id, series)

                val updated = account.copy(
                    expiryTimestamp = if (auth.success) auth.expDate else account.expiryTimestamp,
                    channelsCount = channels.size,
                    moviesCount = movies.size,
                    seriesCount = series.size,
                    lastSyncTime = System.currentTimeMillis()
                )
                _accounts.value = _accounts.value.map { if (it.id == accountId) updated else it }
            } else {
                val (channels, movies, series) = client.fetchM3U(account.m3uUrl, account.id, account.name)
                XtreamProtobufCache.saveChannels(account.id, channels)
                XtreamProtobufCache.saveMovies(account.id, movies)
                XtreamProtobufCache.saveSeries(account.id, series)

                val updated = account.copy(
                    channelsCount = channels.size,
                    moviesCount = movies.size,
                    seriesCount = series.size,
                    lastSyncTime = System.currentTimeMillis()
                )
                _accounts.value = _accounts.value.map { if (it.id == accountId) updated else it }
            }

            saveAccountsToPrefs()
            loadAllCachedData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
            _syncMessage.value = ""
        }
    }

    /**
     * Toggles an account's enabled state.
     */
    fun toggleAccount(accountId: String) {
        val current = _accounts.value
        _accounts.value = current.map {
            if (it.id == accountId) it.copy(isActive = !it.isActive) else it
        }
        saveAccountsToPrefs()
        scope.launch {
            loadAllCachedData()
        }
    }

    /**
     * Deletes an account and its Protobuf cache files.
     */
    fun deleteAccount(accountId: String) {
        _accounts.value = _accounts.value.filter { it.id != accountId }
        saveAccountsToPrefs()
        scope.launch {
            XtreamProtobufCache.deleteAccountCache(accountId)
            loadAllCachedData()
        }
    }

    /**
     * Updates an existing account's configuration (name, credentials, or category filters).
     */
    suspend fun updateAccount(updated: XtreamAccount, credentialsChanged: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val currentList = _accounts.value
        val exists = currentList.any { it.id == updated.id }
        if (!exists) return@withContext Result.failure(Exception("الحساب غير موجود"))

        _accounts.value = currentList.map { if (it.id == updated.id) updated else it }
        saveAccountsToPrefs()

        if (credentialsChanged) {
            syncAccount(updated.id)
        } else {
            loadAllCachedData()
            Result.success(Unit)
        }
    }

    /**
     * Looks up a movie by its Xtream ID from the active in-memory list.
     */
    fun getMovieById(movieId: String): Movie? {
        return _xtreamMovies.value.firstOrNull { it.id == movieId }
    }

    /**
     * Enriches movie details (plot, backdrop, cast, rating) on demand from Xtream API.
     */
    suspend fun getMovieWithDetails(movie: Movie): Movie = withContext(Dispatchers.IO) {
        val acc = getAccountForMedia(movie.id) ?: return@withContext movie
        if (acc.type == XtreamAccountType.XTREAM) {
            val enriched = client.fetchVodDetails(acc.serverUrl, acc.username, acc.password, movie, acc.id)
            _xtreamMovies.value = _xtreamMovies.value.map { if (it.id == movie.id) enriched else it }
            enriched
        } else {
            movie
        }
    }

    /**
     * Looks up a series by its Xtream ID from the active in-memory list.
     */
    fun getSeriesById(seriesId: String): Series? {
        return _xtreamSeries.value.firstOrNull { it.id == seriesId }
    }

    /**
     * Fetches details/episodes for a series when opened.
     */
    suspend fun getSeriesWithEpisodes(series: Series): Series = withContext(Dispatchers.IO) {
        if (series.seasons.isNotEmpty()) return@withContext series
        val acc = getAccountForMedia(series.id) ?: return@withContext series
        if (acc.type == XtreamAccountType.XTREAM) {
            val enriched = client.fetchSeriesEpisodes(acc.serverUrl, acc.username, acc.password, series, acc.id)
            _xtreamSeries.value = _xtreamSeries.value.map { if (it.id == series.id) enriched else it }
            enriched
        } else {
            series
        }
    }
}
