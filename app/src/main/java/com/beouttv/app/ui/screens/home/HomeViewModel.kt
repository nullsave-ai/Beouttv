package com.beouttv.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.data.model.*
import com.beouttv.app.data.api.moviebox.*
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val featuredMovies: List<Movie>,
        val continueWatching: List<ContinueWatchingItem>,
        val liveChannels: List<Channel>,
        val movies: List<Movie>,
        val series: List<Series>,
        val xtreamMovies: List<Movie> = emptyList(),
        val xtreamSeries: List<Series> = emptyList(),
        val asianMovies: List<Movie> = emptyList(),
        val asianCategorySlug: String = "%D8%A7%D9%81%D9%84%D8%A7%D9%85+%D8%A7%D8%B3%D9%8A%D9%88%D9%8A%D8%A9",
        val movieBoxTrending: List<Movie> = emptyList(),
        val movieBoxAnime: List<Movie> = emptyList(),
        val stories: List<com.beouttv.app.data.api.moviebox.MovieBoxStoryItem> = emptyList()
    ) : HomeUiState
}

class HomeViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
        observeLiveUpdates()
        viewModelScope.launch {
            com.beouttv.app.data.source.MediaSourceManager.sourceSwitchEvent.collect {
                load()
            }
        }
    }

    private fun selectBannerMovies(movieList: List<Movie>): List<Movie> {
        val nonSplash = movieList.filter { !it.title.contains("splash", ignoreCase = true) }
        val withSecondaryImage = nonSplash.filter { 
            it.backdropUrl.isNotBlank() && it.backdropUrl != it.posterUrl 
        }
        return if (withSecondaryImage.size >= 3) {
            withSecondaryImage.take(6)
        } else {
            nonSplash.take(6)
        }
    }

    private fun sortChannelsForHome(channels: List<Channel>): List<Channel> {
        return channels.sortedWith(
            compareByDescending<Channel> { 
                it.category == "قنوات بي ان سبورت" || (it.category.contains("بي ان سبورت") && !it.category.contains("SD") && !it.category.contains("الترفيهية"))
            }.thenByDescending {
                it.category.contains("بي ان سبورت SD")
            }
        )
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            repository.channelsFlow.collectLatest { updatedChannels ->
                if (updatedChannels.isNotEmpty()) {
                    val current = _uiState.value
                    if (current is HomeUiState.Success && (current.liveChannels.size != updatedChannels.size || current.liveChannels.firstOrNull()?.id != updatedChannels.firstOrNull()?.id)) {
                        _uiState.value = current.copy(
                            liveChannels = sortChannelsForHome(updatedChannels)
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            com.beouttv.app.data.xtream.XtreamAccountManager.xtreamMovies.collectLatest { xm ->
                val current = _uiState.value
                if (current is HomeUiState.Success && (current.xtreamMovies.size != xm.size || current.xtreamMovies.firstOrNull()?.id != xm.firstOrNull()?.id)) {
                    _uiState.value = current.copy(xtreamMovies = xm)
                }
            }
        }

        viewModelScope.launch {
            com.beouttv.app.data.xtream.XtreamAccountManager.xtreamSeries.collectLatest { xs ->
                val current = _uiState.value
                if (current is HomeUiState.Success && (current.xtreamSeries.size != xs.size || current.xtreamSeries.firstOrNull()?.id != xs.firstOrNull()?.id)) {
                    _uiState.value = current.copy(xtreamSeries = xs)
                }
            }
        }
    }

    private var bannerEnrichJob: Job? = null

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // High-performance concurrent fetching across MovieBox and local endpoints
                val continueWatchingDeferred = async(Dispatchers.IO) { try { repository.getContinueWatching() } catch (_: Exception) { emptyList() } }
                val channelsDeferred = async(Dispatchers.IO) { try { repository.getChannels() } catch (_: Exception) { emptyList() } }

                val mbTrendingDeferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getTrending(page = 1, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbTrendingPage2Deferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getTrending(page = 2, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbMoviesDeferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getMovies(page = 1, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbMoviesPage2Deferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getMovies(page = 2, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbSeriesDeferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getSeries(page = 1, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbSeriesPage2Deferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getSeries(page = 2, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbAsianDeferred = async(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getAsianMovies(page = 1, perPage = 20)
                    } catch (_: Exception) { emptyList() }
                }
                val mbAnimeDeferred = async(Dispatchers.IO) {
                    try {
                        val items = com.beouttv.app.data.api.moviebox.MovieBoxApiClient.getAnime(page = 1, perPage = 18)
                        items.map { com.beouttv.app.data.api.moviebox.MovieBoxMapper.toMovie(it) }
                    } catch (_: Exception) { emptyList<Movie>() }
                }

                val continueWatching = continueWatchingDeferred.await()
                val rawChannels = channelsDeferred.await()
                val channels = sortChannelsForHome(rawChannels)

                val rawTrending = mbTrendingDeferred.await()
                val rawTrendingPage2 = mbTrendingPage2Deferred.await()
                val allTrending = (rawTrending + rawTrendingPage2).distinctBy { it.subjectId }
                val mbTrending = allTrending.map { com.beouttv.app.data.api.moviebox.MovieBoxMapper.toMovie(it) }

                val rawMovies = mbMoviesDeferred.await()
                val rawMoviesPage2 = mbMoviesPage2Deferred.await()
                val allMovies = (rawMovies + rawMoviesPage2).distinctBy { it.subjectId }
                val mbMovies = allMovies.map { com.beouttv.app.data.api.moviebox.MovieBoxMapper.toMovie(it) }

                val rawSeries = mbSeriesDeferred.await()
                val rawSeriesPage2 = mbSeriesPage2Deferred.await()
                val allSeries = (rawSeries + rawSeriesPage2).distinctBy { it.subjectId }
                val mbSeries = allSeries.map { com.beouttv.app.data.api.moviebox.MovieBoxMapper.toSeries(it) }

                val rawAsian = mbAsianDeferred.await()
                val mbAsianMovies = rawAsian.map { com.beouttv.app.data.api.moviebox.MovieBoxMapper.toMovie(it) }

                // Top Banner Showcase: Curate directly from MovieBox Trending & Latest Cinema & Movies!
                val bannerCandidates = mutableListOf<Movie>()
                val maxBannerPool = maxOf(mbTrending.size, mbMovies.size)
                for (i in 0 until maxBannerPool) {
                    if (i < mbTrending.size && !mbTrending[i].isSeries) bannerCandidates.add(mbTrending[i])
                    if (i < mbMovies.size) bannerCandidates.add(mbMovies[i])
                }
                val featured = selectBannerMovies(bannerCandidates)

                // Seamless cache registration in repository
                mbMovies.forEach { repository.cacheDiscoveredMovie(it) }
                mbTrending.forEach {
                    if (it.isSeries) {
                        repository.cacheDiscoveredSeries(
                            Series(
                                id = it.id,
                                title = it.title,
                                posterUrl = it.posterUrl,
                                backdropUrl = it.backdropUrl,
                                year = it.year,
                                rating = it.rating,
                                genre = it.genre,
                                description = it.description,
                                originalTitle = it.originalTitle
                            )
                        )
                    } else {
                        repository.cacheDiscoveredMovie(it)
                    }
                }
                mbAsianMovies.forEach { repository.cacheDiscoveredMovie(it) }
                mbSeries.forEach { repository.cacheDiscoveredSeries(it) }

                fun detectStudio(title: String, genre: String, desc: String): Pair<String, Long> {
                    val t = "$title $genre $desc".lowercase()
                    return when {
                        t.contains("netflix") || t.contains("witcher") || t.contains("stranger") || t.contains("spenser") || t.contains("rebel moon") || t.contains("the killer") || t.contains("sierra burgess") || t.contains("alex strangelove") || t.contains("ricky gervais") ->
                            Pair("NETFLIX", 0xFFE50914L)
                        t.contains("marvel") || t.contains("avengers") || t.contains("spider-man") || t.contains("spiderman") || t.contains("thor") || t.contains("iron man") || t.contains("loki") || t.contains("deadpool") ->
                            Pair("MARVEL", 0xFFEC1D24L)
                        t.contains("disney") || t.contains("pixar") || t.contains("star wars") || t.contains("mandalorian") || t.contains("lion king") || t.contains("aladdin") || t.contains("moana") || t.contains("frozen") ->
                            Pair("DISNEY+", 0xFF0063E5L)
                        t.contains("hbo") || t.contains("game of thrones") || t.contains("house of the dragon") || t.contains("last of us") || t.contains("dmz") || t.contains("succession") || t.contains("euphoria") ->
                            Pair("HBO MAX", 0xFF6B21A8L)
                        t.contains("apple") || t.contains("ted lasso") || t.contains("severance") || t.contains("morning show") || t.contains("silo") || t.contains("foundation") ->
                            Pair("APPLE TV+", 0xFF374151L)
                        t.contains("prime") || t.contains("amazon") || t.contains("boys") || t.contains("reacher") || t.contains("rings of power") || t.contains("fallout") || t.contains("invincible") ->
                            Pair("PRIME VIDEO", 0xFF00A8E1L)
                        t.contains("paramount") || t.contains("yellowstone") || t.contains("star trek") || t.contains("halo") || t.contains("mission: impossible") || t.contains("mission impossible") || t.contains("top gun") || t.contains("quiet place") || t.contains("hell or high water") ->
                            Pair("PARAMOUNT+", 0xFF0064FFL)
                        t.contains("warner") || t.contains("dune") || t.contains("batman") || t.contains("harry potter") || t.contains("godzilla") || t.contains("rampage") || t.contains("oppenheimer") ->
                            Pair("WARNER BROS", 0xFF1E3A8AL)
                        else -> {
                            val fallbackStudios = listOf(
                                Pair("NETFLIX", 0xFFE50914L),
                                Pair("HBO MAX", 0xFF6B21A8L),
                                Pair("DISNEY+", 0xFF0063E5L),
                                Pair("PRIME VIDEO", 0xFF00A8E1L),
                                Pair("APPLE TV+", 0xFF374151L),
                                Pair("MARVEL", 0xFFEC1D24L),
                                Pair("PARAMOUNT+", 0xFF0064FFL),
                                Pair("WARNER BROS", 0xFF1E3A8AL)
                            )
                            val idx = (title.hashCode().let { if (it < 0) -it else it }) % fallbackStudios.size
                            fallbackStudios[idx]
                        }
                    }
                }

                val stories = allTrending.filter { item ->
                    val url = item.trailer?.videoAddress?.url
                    !url.isNullOrBlank() && !com.beouttv.app.data.filter.ContentFilterEngine.isContentBlocked(item.title, item.genre)
                }.map { item ->
                    // Find Arabic caption in extCaptions (both direct and inside resolutionList)
                    var arSubUrl: String? = null
                    for (rd in item.resourceDetectors ?: emptyList()) {
                        val directCaptions = rd.extCaptions ?: emptyList()
                        val resCaptions = rd.resolutionList?.flatMap { it.extCaptions ?: emptyList<MovieBoxExtCaption>() } ?: emptyList()
                        val allCaps: List<MovieBoxExtCaption> = directCaptions + resCaptions
                        val cap = allCaps.firstOrNull { c ->
                            c.lan.equals("ar", ignoreCase = true) ||
                            c.lan.equals("ara", ignoreCase = true) ||
                            c.lanName.contains("عرب")
                        }
                        if (cap != null && !cap.url.isNullOrBlank()) {
                            arSubUrl = cap.url
                            break
                        }
                    }

                    val isDubbed = item.title.contains("مدبلج", ignoreCase = true) ||
                                   item.description.contains("مدبلج", ignoreCase = true) ||
                                   item.genre.contains("مدبلج", ignoreCase = true)

                    val (studioBadge, studioColor) = detectStudio(item.title, item.genre, item.description)

                    com.beouttv.app.data.api.moviebox.MovieBoxStoryItem(
                        id = "story_${item.subjectId}",
                        title = item.title,
                        coverUrl = item.cover?.url.orEmpty(),
                        videoUrl = item.trailer?.videoAddress?.url.orEmpty(),
                        durationSeconds = item.trailer?.videoAddress?.duration ?: 15L,
                        subjectId = item.subjectId,
                        subjectType = item.subjectType,
                        score = item.score ?: item.imdbRatingValue ?: "7.8",
                        genre = item.genre,
                        subtitleUrl = arSubUrl,
                        isDubbed = isDubbed,
                        isArabic = true,
                        studioBadge = studioBadge,
                        studioColorHex = studioColor
                    )
                }
                val mbAnime = mbAnimeDeferred.await()

                val xtreamMovies = com.beouttv.app.data.xtream.XtreamAccountManager.xtreamMovies.value
                val xtreamSeries = com.beouttv.app.data.xtream.XtreamAccountManager.xtreamSeries.value

                // Instantly emit success with zero-lag UI display!
                _uiState.value = HomeUiState.Success(
                    featuredMovies = featured,
                    continueWatching = continueWatching,
                    liveChannels = channels,
                    movies = mbMovies,
                    series = mbSeries,
                    xtreamMovies = xtreamMovies,
                    xtreamSeries = xtreamSeries,
                    asianMovies = mbAsianMovies,
                    asianCategorySlug = "mb_asian",
                    movieBoxTrending = mbTrending,
                    movieBoxAnime = mbAnime,
                    stories = stories
                )

                // Background lazy enrichment of banner movies (runs silently after initial render)
                enrichBannerMovies(featured)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Success(
                    featuredMovies = emptyList(),
                    continueWatching = emptyList(),
                    liveChannels = emptyList(),
                    movies = emptyList(),
                    series = emptyList()
                )
            }
        }
    }

    private fun enrichBannerMovies(featured: List<Movie>) {
        bannerEnrichJob?.cancel()
        bannerEnrichJob = viewModelScope.launch(Dispatchers.IO) {
            // Wait 2.5 seconds so initial UI render & user interaction have zero contention
            delay(2500)
            val lang = com.beouttv.app.data.i18n.LanguageManager.currentLanguage.value
            val needsEnrichment = featured.filter { it.backdropUrl == it.posterUrl }.take(4)
            if (needsEnrichment.isEmpty()) return@launch

            var anyUpdated = false
            val updatedFeaturedList = featured.toMutableList()
            for (m in needsEnrichment) {
                try {
                    val enriched = com.beouttv.app.data.imdb.ImdbMetadataEngine.enrichMovie(m, lang)
                    if (enriched.backdropUrl != m.backdropUrl) {
                        val idx = updatedFeaturedList.indexOfFirst { it.id == m.id }
                        if (idx != -1) {
                            updatedFeaturedList[idx] = enriched
                            anyUpdated = true
                        }
                    }
                } catch (_: Exception) {}
            }
            if (anyUpdated) {
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    _uiState.value = current.copy(featuredMovies = updatedFeaturedList)
                }
            }
        }
    }

    fun toggleFavoriteChannel(channelId: String) {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            val updatedChannels = current.liveChannels.map {
                if (it.id == channelId) it.copy(isFavorite = !it.isFavorite) else it
            }
            _uiState.value = current.copy(liveChannels = updatedChannels)
        }
        viewModelScope.launch {
            repository.toggleFavoriteChannel(channelId)
        }
    }
}
