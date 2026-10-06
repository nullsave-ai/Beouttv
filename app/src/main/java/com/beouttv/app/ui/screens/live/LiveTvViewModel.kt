package com.beouttv.app.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.ContentCategory
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.data.source.stream.LiveGatewayStreamResolver
import com.beouttv.app.data.source.stream.OkRuStreamResolver
import com.beouttv.app.data.source.stream.OstoraApiResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LiveBouquet(
    val key: String,
    val count: Int,
    val firstChannelLogoUrl: String = ""
)

sealed interface LiveTvUiState {
    data object Loading : LiveTvUiState
    data class Error(val message: String) : LiveTvUiState
    data class Success(val channels: List<Channel>) : LiveTvUiState
}

class LiveTvViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveTvUiState>(LiveTvUiState.Loading)
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _bouquets = MutableStateFlow<List<LiveBouquet>>(emptyList())
    val bouquets: StateFlow<List<LiveBouquet>> = _bouquets.asStateFlow()

    private val _favoritesCount = MutableStateFlow(0)
    val favoritesCount: StateFlow<Int> = _favoritesCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    // In-memory channels cache for 0ms instantaneous filtering
    private var baseRepoChannels: List<Channel> = emptyList()
    private var xtreamLiveChannels: List<Channel> = emptyList()
    private var allChannels: List<Channel> = emptyList()

    // Dynamic stream URL resolution: tracks which channelId is currently being resolved
    private val _resolvingChannelId = MutableStateFlow<String?>(null)
    val resolvingChannelId: StateFlow<String?> = _resolvingChannelId.asStateFlow()

    // Emits a ready-to-play Channel (with fresh streamUrl) after dynamic resolution
    private val _resolvedChannel = MutableSharedFlow<Channel>(extraBufferCapacity = 1)
    val resolvedChannel: SharedFlow<Channel> = _resolvedChannel.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            repository.channelsFlow.collect { channels ->
                if (channels.isNotEmpty()) {
                    baseRepoChannels = channels
                    refreshCombinedChannels()
                }
            }
        }

        viewModelScope.launch {
            com.beouttv.app.data.xtream.XtreamAccountManager.xtreamChannels.collect { xCh ->
                xtreamLiveChannels = xCh
                refreshCombinedChannels()
            }
        }
    }

    private fun refreshCombinedChannels() {
        allChannels = baseRepoChannels + xtreamLiveChannels
        updateBouquetsAndCounts()
        applyFilter()
    }

    fun load(showSkeleton: Boolean = false) {
        viewModelScope.launch {
            if (showSkeleton || allChannels.isEmpty()) {
                _uiState.value = LiveTvUiState.Loading
            }
            try {
                val channels = repository.getChannels("All")
                baseRepoChannels = channels
                xtreamLiveChannels = com.beouttv.app.data.xtream.XtreamAccountManager.xtreamChannels.value
                refreshCombinedChannels()
            } catch (e: Exception) {
                if (allChannels.isEmpty()) {
                    _uiState.value = LiveTvUiState.Error(e.message ?: "حدث خطأ أثناء تحميل القنوات")
                }
            }
        }
    }

    /**
     * Resolves a fresh stream URL for Ostora and Edge Gateway channels dynamically before playing.
     */
    fun resolveAndPlay(channel: Channel) {
        viewModelScope.launch {
            if (channel.id.startsWith("gw_") || LiveGatewayStreamResolver.isTargetApiUrl(channel.streamUrl)) {
                _resolvingChannelId.value = channel.id
                try {
                    val resolved = withContext(Dispatchers.IO) {
                        LiveGatewayStreamResolver.resolve(channel.streamUrl)
                    }
                    if (resolved != null && resolved.streamUrl.isNotBlank()) {
                        _resolvedChannel.emit(
                            channel.copy(
                                streamUrl = resolved.streamUrl,
                                userAgent = resolved.userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                                referer = resolved.referer ?: "https://studio.x.com/",
                                drmKeyId = resolved.drmKeyId ?: channel.drmKeyId,
                                drmKey = resolved.drmKey ?: channel.drmKey
                            )
                        )
                    } else {
                        _resolvedChannel.emit(channel)
                    }
                } catch (_: Exception) {
                    _resolvedChannel.emit(channel)
                } finally {
                    _resolvingChannelId.value = null
                }
                return@launch
            }

            if (channel.id.startsWith("ok_") || channel.id.startsWith("okru_") || OkRuStreamResolver.isOkRuUrl(channel.streamUrl)) {
                _resolvingChannelId.value = channel.id
                try {
                    val resolved = withContext(Dispatchers.IO) {
                        OkRuStreamResolver.resolve(channel.streamUrl)
                    }
                    if (resolved != null && resolved.streamUrl.isNotBlank()) {
                        _resolvedChannel.emit(
                            channel.copy(
                                streamUrl = resolved.streamUrl,
                                userAgent = resolved.userAgent ?: OkRuStreamResolver.DEFAULT_USER_AGENT,
                                referer = resolved.referer ?: OkRuStreamResolver.DEFAULT_REFERER,
                                drmKeyId = resolved.drmKeyId ?: channel.drmKeyId,
                                drmKey = resolved.drmKey ?: channel.drmKey
                            )
                        )
                    } else {
                        _resolvedChannel.emit(channel)
                    }
                } catch (_: Exception) {
                    _resolvedChannel.emit(channel)
                } finally {
                    _resolvingChannelId.value = null
                }
                return@launch
            }

            // =========================================================================
            // [OSTORA TV ENGINE] مخصص لقنوات وسيرفرات شبكة الأسطورة المعتمدة
            // =========================================================================
            if (!OstoraApiResolver.isOstoraChannel(channel)) {
                _resolvedChannel.emit(channel)
                return@launch
            }

            _resolvingChannelId.value = channel.id
            try {
                val resolved = withContext(Dispatchers.IO) {
                    OstoraApiResolver.resolveChannelStream(channel)
                }
                _resolvedChannel.emit(
                    channel.copy(
                        streamUrl = resolved.streamUrl,
                        userAgent = resolved.userAgent,
                        referer = resolved.referer,
                        drmKeyId = resolved.drmKeyId ?: channel.drmKeyId,
                        drmKey = resolved.drmKey ?: channel.drmKey
                    )
                )
            } catch (_: Exception) {
                _resolvedChannel.emit(
                    channel.copy(
                        streamUrl = OstoraApiResolver.cleanStreamUrl(channel.streamUrl),
                        userAgent = channel.userAgent ?: OstoraApiResolver.DEFAULT_USER_AGENT,
                        referer = OstoraApiResolver.DEFAULT_REFERER
                    )
                )
            } finally {
                _resolvingChannelId.value = null
            }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _showOnlyFavorites.value = false
        applyFilter()
    }

    fun toggleOnlyFavorites() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
        applyFilter()
    }

    fun onQueryChange(value: String) {
        _query.value = value
        applyFilter()
    }

    fun toggleFavorite(channelId: String) {
        // Optimistic UI update: instantly toggle favorite state in memory with 0ms lag
        allChannels = allChannels.map {
            if (it.id == channelId) it.copy(isFavorite = !it.isFavorite) else it
        }
        updateBouquetsAndCounts()
        applyFilter()

        viewModelScope.launch {
            repository.toggleFavoriteChannel(channelId)
        }
    }

    private fun updateBouquetsAndCounts() {
        _totalCount.value = allChannels.size
        _favoritesCount.value = allChannels.count { it.isFavorite }

        val standardKeys = mutableListOf("All")
        if (allChannels.any { it.category == "اشتراكي الشخصي" || it.id.startsWith("xtream_") }) {
            standardKeys.add("اشتراكي الشخصي")
        }
        standardKeys.addAll(
            listOf(
                "قنوات بي ان سبورت",
                "قنوات بي ان سبورت SD",
                "قنوات ألوان الرياضية",
                "قنوات رياضية عربية",
                "قنوات ثمانية",
                "باقة قنوات MBC",
                "قنوات الدوري الألماني وشاهد",
                "قنوات STARZPLAY الرياضية",
                "قنوات المباريات والبث المباشر",
                "قنوات KSA SPORTS",
                "قنوات ON SPORTS والرياضة المصرية"
            )
        )

        val foundCategories = allChannels
            .map { it.category.trim() }
            .filter { c ->
                c.isNotBlank() &&
                !c.contains("osn", ignoreCase = true) &&
                !c.contains("beout", ignoreCase = true) &&
                !c.contains("nm", ignoreCase = true) &&
                !c.contains("أبوظبي") &&
                !c.contains("ابوظبي") &&
                !c.contains("abu dhabi", ignoreCase = true) &&
                !c.contains("أطفال") &&
                !c.contains("اطفال") &&
                !c.contains("وثائقي") &&
                !c.contains("الترفيهية") &&
                !c.contains("الكأس") &&
                !c.contains("الكاس") &&
                !c.contains("alkass", ignoreCase = true) &&
                !c.contains("دبي") &&
                !c.contains("dubai", ignoreCase = true) &&
                !c.contains("stc", ignoreCase = true) &&
                !c.contains("نايل سات") &&
                !c.contains("nilesat", ignoreCase = true) &&
                !c.contains("أخبار") &&
                !c.contains("اخبار") &&
                !c.contains("news", ignoreCase = true)
            }
            .distinct()

        val combinedKeys = mutableListOf<String>()
        combinedKeys.addAll(standardKeys)
        foundCategories.forEach { c ->
            if (!combinedKeys.contains(c)) combinedKeys.add(c)
        }

        val list = mutableListOf<LiveBouquet>()
        combinedKeys.forEach { key ->
            val matching = if (key == "All") allChannels else allChannels.filter { matchesCategory(it, key) }
            val count = matching.size
            val firstLogo = matching.firstOrNull { it.logoUrl.isNotBlank() }?.logoUrl ?: ""
            // Only add categories that have channels (or All)
            if (count > 0 || key == "All") {
                list.add(LiveBouquet(key = key, count = count, firstChannelLogoUrl = firstLogo))
            }
        }
        _bouquets.value = list
    }

    private fun applyFilter() {
        val q = _query.value.trim()
        val cat = _selectedCategory.value
        val onlyFavs = _showOnlyFavorites.value

        var filtered = allChannels

        if (onlyFavs) {
            filtered = filtered.filter { it.isFavorite }
        } else if (cat != "All" && cat != ContentCategory.ALL.label) {
            filtered = filtered.filter { matchesCategory(it, cat) }
        }

        if (q.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true)
            }
        }

        _uiState.value = LiveTvUiState.Success(filtered)
    }

    private fun matchesCategory(channel: Channel, category: String): Boolean {
        if (category == "All" || category.isBlank()) return true
        val cat = channel.category.trim()
        val name = channel.name.trim()

        // Direct canonical match
        if (cat.equals(category, ignoreCase = true)) return true

        return when (category) {
            "All" -> true
            "اشتراكي الشخصي", "Personal IPTV" ->
                cat == "اشتراكي الشخصي" || channel.id.startsWith("xtream_")
            "قنوات بي ان سبورت SD", "beIN Sports SD" ->
                cat.contains("SD", ignoreCase = true) && (cat.contains("بي ان") || cat.contains("bein", true))

            "قنوات بي ان سبورت", "beIN Sports" ->
                (cat.contains("beIN", ignoreCase = true) || cat.contains("بي ان") || cat.contains("بى ان")) &&
                !cat.contains("Movies", ignoreCase = true) && !cat.contains("الترفيهية") && !cat.contains("SD", ignoreCase = true) &&
                !cat.contains("NM", ignoreCase = true) && !name.contains("NM", ignoreCase = true)

            "قنوات رياضية عربية", "Arab Sports" ->
                cat.contains("رياضية عربية") || cat.contains("ARABSPORT", ignoreCase = true)

            "باقة قنوات MBC", "MBC" ->
                cat.contains("MBC", ignoreCase = true) || name.startsWith("MBC", ignoreCase = true)

            "قنوات الدوري الألماني وشاهد", "Bundesliga", "Shahid" ->
                cat.contains("الدوري الألماني") || cat.contains("شاهد") || name.contains("شاهد") || name.contains("Shahid", ignoreCase = true)

            "قنوات STARZPLAY الرياضية", "STARZPLAY" ->
                cat.contains("STARZPLAY", ignoreCase = true) || name.contains("STARZPLAY", ignoreCase = true)

            "قنوات المباريات والبث المباشر", "Matches" ->
                cat.contains("المباريات") || cat.contains("TOD", ignoreCase = true) || name.contains("TOD", ignoreCase = true)

            "قنوات ثمانية", "ثمانية" ->
                cat.contains("ثمانية") || name.contains("ثمانية")

            "قنوات ألوان الرياضية", "ألوان الرياضية", "Alwan Sports" ->
                cat.contains("ألوان") || name.contains("ألوان") || cat.contains("alwan", true) || name.contains("alwan", true)

            "قنوات KSA SPORTS", "KSA SPORTS", "السعودية الرياضية" ->
                cat.contains("KSA SPORT", ignoreCase = true) || name.contains("KSA SPORT", ignoreCase = true) ||
                (cat.contains("السعودية") && name.contains("رياض"))

            "قنوات ON SPORTS والرياضة المصرية", "ON SPORTS", "الرياضة المصرية" ->
                cat.contains("ON SPORT", ignoreCase = true) || cat.contains("الرياضة المصرية") ||
                name.contains("أون تايم") || name.contains("اون تايم") || name.contains("الأهلي") ||
                name.contains("الزمالك") || name.contains("نايل سبورت")

            else -> cat.contains(category, ignoreCase = true)
        }
    }
}
