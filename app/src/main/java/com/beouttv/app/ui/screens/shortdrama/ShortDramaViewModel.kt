package com.beouttv.app.ui.screens.shortdrama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.data.model.ShortDramaCategory
import com.beouttv.app.data.model.ShortDramaEpisode
import com.beouttv.app.data.model.ShortDramaFeedItem
import com.beouttv.app.data.repository.ShortDramaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShortDramaUiState(
    val categories: List<ShortDramaCategory> = emptyList(),
    val selectedCategory: ShortDramaCategory? = null,
    val feedItems: List<ShortDramaFeedItem> = emptyList(),
    val currentFeedIndex: Int = 0,
    val isEpisodesSheetVisible: Boolean = false,
    val isSourcesSheetVisible: Boolean = false,
    val isPlatformsSheetVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isMuted: Boolean = false,
    val likedSeriesIds: Set<String> = emptySet(),
    val isCleanScreen: Boolean = false
)

class ShortDramaViewModel : ViewModel() {

    companion object {
        val isCleanScreenGlobal = MutableStateFlow(false)
        val pendingTabSwitch = MutableStateFlow<String?>(null)
        var pendingOfflineSeries: Pair<com.beouttv.app.data.model.ShortDramaSeries, Int>? = null
    }

    val episodeNoticeEvent = MutableStateFlow<String?>(null)

    fun clearEpisodeNotice() {
        episodeNoticeEvent.value = null
    }

    private val _uiState = MutableStateFlow(ShortDramaUiState())
    val uiState: StateFlow<ShortDramaUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val pending = pendingOfflineSeries
        if (pending != null) {
            pendingOfflineSeries = null
            playOfflineSeries(pending.first, pending.second)
            return
        }

        val cats = ShortDramaRepository.getDefaultCategories()
        val defaultCat = cats.firstOrNull { it.id == "hot_mb" } ?: cats.firstOrNull()
        val preloadedFeed = ShortDramaRepository.getInitialPreloadedFeed().shuffled()

        _uiState.value = _uiState.value.copy(
            categories = cats,
            selectedCategory = defaultCat,
            feedItems = preloadedFeed,
            isLoading = false
        )

        if (defaultCat != null) {
            viewModelScope.launch {
                loadCategorySeries(defaultCat, isInitial = true)
                try {
                    ShortDramaRepository.refreshFromMovieBoxApi()
                    val freshSeries = ShortDramaRepository.getCachedSeries(defaultCat.id)
                    if (freshSeries.isNotEmpty() && _uiState.value.selectedCategory?.id == defaultCat.id) {
                        val freshFeed = freshSeries.map { ShortDramaRepository.createFeedItemFromSeries(it, 1) }
                        _uiState.value = _uiState.value.copy(feedItems = freshFeed)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun playOfflineSeries(series: com.beouttv.app.data.model.ShortDramaSeries, episodeNumber: Int = 1) {
        val cats = ShortDramaRepository.getDefaultCategories()
        val dlCat = cats.firstOrNull { it.id == "downloads_ar" } ?: cats.firstOrNull()
        val feedItem = ShortDramaRepository.createFeedItemFromSeries(series, episodeNumber)
        _uiState.value = _uiState.value.copy(
            categories = cats,
            selectedCategory = dlCat,
            feedItems = listOf(feedItem),
            currentFeedIndex = 0,
            isLoading = false
        )
    }

    fun initFavorites(context: android.content.Context) {
        try {
            val prefs = context.getSharedPreferences("beout_drama_likes", android.content.Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("liked_ids", emptySet()) ?: emptySet()
            if (saved.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(likedSeriesIds = saved)
            }
        } catch (_: Exception) {}
    }

    fun selectCategory(category: ShortDramaCategory) {
        val isAlreadySelected = (_uiState.value.selectedCategory?.id == category.id)
        if (isAlreadySelected && category.id != "downloads_ar") {
            // Re-shuffle current feed for fresh discovery when user re-taps active tab!
            val currentFeed = _uiState.value.feedItems
            if (currentFeed.size > 1) {
                _uiState.value = _uiState.value.copy(
                    feedItems = currentFeed.shuffled(),
                    currentFeedIndex = 0
                )
            }
            return
        }

        // Instant Favorites / Liked tab handling (randomized)
        if (category.id == "favorites_ar") {
            val likedIds = _uiState.value.likedSeriesIds
            val all = ShortDramaRepository.getAllSeries()
            val likedSeries = all.filter { it.id in likedIds }
            val feed = likedSeries.shuffled().map { ShortDramaRepository.createFeedItemFromSeries(it, 1) }
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                feedItems = feed,
                currentFeedIndex = 0,
                isPlatformsSheetVisible = false,
                isLoading = false
            )
            return
        }

        // Instant Downloads tab handling (without "مسلسلات" in title)
        if (category.id == "downloads_ar") {
            val downloadedList = com.beouttv.app.data.repository.ShortDramaDownloadManager.downloadedSeriesList.value
            val allSeries = ShortDramaRepository.getAllSeries()
            val seriesWithDownloads = downloadedList.map { dl ->
                allSeries.firstOrNull { it.id == dl.seriesId } ?: com.beouttv.app.data.model.ShortDramaSeries(
                    id = dl.seriesId,
                    name = dl.seriesName,
                    coverUrl = dl.coverUrl,
                    episodeCount = dl.totalEpisodes,
                    platform = "بي اوت TV",
                    categoryId = "downloads_ar"
                )
            }
            val feed = seriesWithDownloads.map { ShortDramaRepository.createFeedItemFromSeries(it, 1) }
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                feedItems = feed,
                currentFeedIndex = 0,
                isPlatformsSheetVisible = false,
                isLoading = false
            )
            return
        }

        // Instant 0ms memory cache lookup - Freshly randomized per tab for dynamic discovery!
        val cached = ShortDramaRepository.getCachedSeries(category.id)
        if (cached.isNotEmpty()) {
            val randomizedSeries = cached.shuffled()
            val instantFeed = randomizedSeries.map { ShortDramaRepository.createFeedItemFromSeries(it, 1) }
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                feedItems = instantFeed,
                currentFeedIndex = 0,
                isPlatformsSheetVisible = false,
                isLoading = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isPlatformsSheetVisible = false,
            isLoading = true
        )
        viewModelScope.launch {
            loadCategorySeries(category, isInitial = false)
        }
    }

    private suspend fun loadCategorySeries(
        category: ShortDramaCategory,
        isInitial: Boolean = false
    ) {
        try {
            val seriesList = withContext(Dispatchers.IO) {
                ShortDramaRepository.getSeriesByCategory(category.id)
            }
            if (seriesList.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return
            }

            // Build feed items with fresh randomized discovery for each tab
            val randomized = if (category.id != "downloads_ar") seriesList.shuffled() else seriesList
            val instantFeed = randomized.map { series ->
                ShortDramaRepository.createFeedItemFromSeries(series, 1)
            }

            if (!isInitial || _uiState.value.selectedCategory?.id == category.id) {
                _uiState.value = _uiState.value.copy(
                    feedItems = instantFeed,
                    currentFeedIndex = 0,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onFeedPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentFeedIndex = index)
        val feed = _uiState.value.feedItems
        if (index in feed.indices) {
            val currentItem = feed[index]
            if (currentItem.sources.isEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val sources = ShortDramaRepository.getEpisodeSources(
                        currentItem.episode.id,
                        currentItem.series.id,
                        currentItem.episode.episodeNumber,
                        currentItem.series.platform
                    )
                    if (sources.isNotEmpty()) {
                        val currentList = _uiState.value.feedItems.toMutableList()
                        if (index in currentList.indices && currentList[index].series.id == currentItem.series.id) {
                            currentList[index] = currentList[index].copy(sources = sources)
                            _uiState.value = _uiState.value.copy(feedItems = currentList.toList())
                        }
                    }
                }
            }
        }

        // TikTok-style prefetching for upcoming reel
        val nextIndex = index + 1
        if (nextIndex in feed.indices) {
            val nextItem = feed[nextIndex]
            if (nextItem.sources.isEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        ShortDramaRepository.getEpisodeSources(
                            nextItem.episode.id,
                            nextItem.series.id,
                            nextItem.episode.episodeNumber,
                            nextItem.series.platform
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun toggleMute() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun toggleLike(seriesId: String, context: android.content.Context? = null) {
        val likes = _uiState.value.likedSeriesIds.toMutableSet()
        if (seriesId in likes) likes.remove(seriesId) else likes.add(seriesId)
        _uiState.value = _uiState.value.copy(likedSeriesIds = likes)

        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences("beout_drama_likes", android.content.Context.MODE_PRIVATE)
                prefs.edit().putStringSet("liked_ids", likes).apply()
            } catch (_: Exception) {}
        }

        // If currently viewing favorites tab, update the feed live
        if (_uiState.value.selectedCategory?.id == "favorites_ar") {
            val all = ShortDramaRepository.getAllSeries()
            val likedSeries = all.filter { it.id in likes }
            val feed = likedSeries.map { ShortDramaRepository.createFeedItemFromSeries(it, 1) }
            _uiState.value = _uiState.value.copy(feedItems = feed)
        }
    }

    fun showEpisodesSheet() {
        _uiState.value = _uiState.value.copy(isEpisodesSheetVisible = true)
    }

    fun hideEpisodesSheet() {
        _uiState.value = _uiState.value.copy(isEpisodesSheetVisible = false)
    }

    fun showSourcesSheet() {
        _uiState.value = _uiState.value.copy(isSourcesSheetVisible = true)
    }

    fun hideSourcesSheet() {
        _uiState.value = _uiState.value.copy(isSourcesSheetVisible = false)
    }

    fun showPlatformsSheet() {
        _uiState.value = _uiState.value.copy(isPlatformsSheetVisible = true)
    }

    fun hidePlatformsSheet() {
        _uiState.value = _uiState.value.copy(isPlatformsSheetVisible = false)
    }

    fun toggleCleanScreen() {
        val next = !_uiState.value.isCleanScreen
        _uiState.value = _uiState.value.copy(isCleanScreen = next)
        isCleanScreenGlobal.value = next
    }

    fun setCleanScreen(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isCleanScreen = enabled)
        isCleanScreenGlobal.value = enabled
    }

    fun selectSourceForCurrentItem(sourceIndex: Int) {
        val currentIndex = _uiState.value.currentFeedIndex
        val items = _uiState.value.feedItems.toMutableList()
        if (currentIndex in items.indices) {
            val currentItem = items[currentIndex]
            if (sourceIndex in currentItem.sources.indices) {
                items[currentIndex] = currentItem.copy(currentSourceIndex = sourceIndex)
                _uiState.value = _uiState.value.copy(
                    feedItems = items.toList(),
                    isSourcesSheetVisible = false
                )
            }
        }
    }

    /**
     * Switch episode for the active reel. Sources are derived truthfully matching MovieBox.
     */
    fun selectEpisodeForCurrentSeries(episode: ShortDramaEpisode) {
        val currentIndex = _uiState.value.currentFeedIndex
        val items = _uiState.value.feedItems.toMutableList()
        if (currentIndex !in items.indices) return

        val currentItem = items[currentIndex]
        val epNum = episode.episodeNumber

        val immediateSources = ShortDramaRepository.buildSourcesForEpisode(
            currentItem.series.id,
            epNum,
            currentItem.series.platform
        )

        if (immediateSources.isNotEmpty()) {
            val updated = currentItem.copy(
                episode = episode,
                sources = immediateSources,
                currentSourceIndex = 0
            )
            items[currentIndex] = updated
            _uiState.value = _uiState.value.copy(
                feedItems = items.toList(),
                isEpisodesSheetVisible = false
            )
        } else {
            // Dismiss sheet and fetch via repository dynamically
            _uiState.value = _uiState.value.copy(isEpisodesSheetVisible = false)
            val placeholder = currentItem.copy(
                episode = episode,
                currentSourceIndex = 0
            )
            items[currentIndex] = placeholder
            _uiState.value = _uiState.value.copy(feedItems = items.toList())

            viewModelScope.launch {
                val epSources = withContext(Dispatchers.IO) {
                    ShortDramaRepository.getEpisodeSources(
                        episode.id,
                        currentItem.series.id,
                        epNum,
                        currentItem.series.platform
                    )
                }
                val latestItems = _uiState.value.feedItems.toMutableList()
                if (currentIndex in latestItems.indices &&
                    latestItems[currentIndex].series.id == currentItem.series.id) {
                    if (epSources.isNotEmpty()) {
                        latestItems[currentIndex] = latestItems[currentIndex].copy(
                            episode = episode,
                            sources = epSources,
                            currentSourceIndex = 0
                        )
                    }
                    _uiState.value = _uiState.value.copy(feedItems = latestItems.toList())
                }
            }
        }
    }

    /**
     * Advances to the next episode seamlessly within the current active series
     */
    fun advanceToNextEpisode() {
        val currentIndex = _uiState.value.currentFeedIndex
        val items = _uiState.value.feedItems
        if (currentIndex !in items.indices) return
        val currentItem = items[currentIndex]
        val nextEpNum = currentItem.episode.episodeNumber + 1
        if (nextEpNum <= currentItem.series.episodeCount) {
            val videoId = ShortDramaRepository.resolveVideoId(currentItem.series.id)
            val nextEpisode = ShortDramaEpisode(
                id = "epservers_${videoId}_$nextEpNum",
                seriesId = currentItem.series.id,
                episodeNumber = nextEpNum,
                title = "الحلقة $nextEpNum",
                thumbnailUrl = currentItem.series.coverUrl
            )
            selectEpisodeForCurrentSeries(nextEpisode)
        }
    }

    /**
     * Advances to the previous episode
     */
    fun advanceToPreviousEpisode() {
        val currentIndex = _uiState.value.currentFeedIndex
        val items = _uiState.value.feedItems
        if (currentIndex !in items.indices) return
        val currentItem = items[currentIndex]
        val prevEpNum = currentItem.episode.episodeNumber - 1
        if (prevEpNum >= 1) {
            val videoId = ShortDramaRepository.resolveVideoId(currentItem.series.id)
            val prevEpisode = ShortDramaEpisode(
                id = "epservers_${videoId}_$prevEpNum",
                seriesId = currentItem.series.id,
                episodeNumber = prevEpNum,
                title = "الحلقة $prevEpNum",
                thumbnailUrl = currentItem.series.coverUrl
            )
            selectEpisodeForCurrentSeries(prevEpisode)
        }
    }
}
