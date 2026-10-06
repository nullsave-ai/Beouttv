package com.beouttv.app.ui.screens.search

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.data.filter.ContentFilterEngine
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Match
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS_SEARCH = "beout_search_prefs"
private const val KEY_RECENT = "recent_queries"

// Pre-indexed search item for sub-millisecond lookups
private data class IndexedMovie(
    val movie: Movie,
    val tokens: String
)

private data class IndexedChannel(
    val channel: Channel,
    val tokens: String
)

private data class IndexedSeries(
    val series: Series,
    val tokens: String
)

private data class IndexedMatch(
    val match: Match,
    val tokens: String
)

data class SearchResultState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isBlocked: Boolean = false,
    val matchedMatches: List<Match> = emptyList(),
    val matchedMovies: List<Movie> = emptyList(),
    val matchedSeries: List<Series> = emptyList(),
    val matchedChannels: List<Channel> = emptyList(),
    val totalCount: Int = 0
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {

    companion object {
        var initialQuery: String? = null
    }

    private val _rawMovies = MutableStateFlow<List<Movie>>(emptyList())
    private val _rawSeries = MutableStateFlow<List<Series>>(emptyList())
    private val _rawChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _rawMatches = MutableStateFlow<List<Match>>(emptyList())
    val movies: StateFlow<List<Movie>> = _rawMovies.asStateFlow()
    val series: StateFlow<List<Series>> = _rawSeries.asStateFlow()
    val channels: StateFlow<List<Channel>> = _rawChannels.asStateFlow()
    val matches: StateFlow<List<Match>> = _rawMatches.asStateFlow()

    private var indexedMovies: List<IndexedMovie> = emptyList()
    private var indexedSeries: List<IndexedSeries> = emptyList()
    private var indexedChannels: List<IndexedChannel> = emptyList()
    private var indexedMatches: List<IndexedMatch> = emptyList()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResult = MutableStateFlow(SearchResultState())
    val searchResult: StateFlow<SearchResultState> = _searchResult.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    init {
        loadData()
        observeLiveUpdates()
        setupDebouncedSearch()
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            repository.moviesFlow.collectLatest { updated ->
                if (updated.isNotEmpty()) {
                    _rawMovies.value = updated
                    rebuildMovieIndex(updated)
                }
            }
        }
        viewModelScope.launch {
            repository.seriesFlow.collectLatest { updated ->
                if (updated.isNotEmpty()) {
                    _rawSeries.value = updated
                    rebuildSeriesIndex(updated)
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mList = repository.getMovies()
                val sList = repository.getSeries()
                val cList = repository.getChannels("All")
                val todayMatches = try { repository.getMatchesByDate("today") } catch (_: Exception) { repository.getMatches() }
                val tomorrowMatches = try { repository.getMatchesByDate("tomorrow") } catch (_: Exception) { emptyList() }
                val allMatches = (todayMatches + tomorrowMatches).distinctBy { it.id }.ifEmpty { repository.getMatches() }

                _rawMovies.value = mList
                _rawSeries.value = sList
                _rawChannels.value = cList
                _rawMatches.value = allMatches

                rebuildMovieIndex(mList)
                rebuildSeriesIndex(sList)
                rebuildChannelIndex(cList)
                rebuildMatchIndex(allMatches)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun rebuildMovieIndex(list: List<Movie>) {
        viewModelScope.launch(Dispatchers.Default) {
            indexedMovies = list.map { m ->
                val allCategoryTranslations = com.beouttv.app.data.i18n.AppLanguage.entries.joinToString(" ") { lang ->
                    "${com.beouttv.app.data.imdb.ImdbMetadataEngine.localizeCategoryString(m.genre, lang)} ${com.beouttv.app.data.imdb.ImdbMetadataEngine.localizeCategoryString(m.category, lang)}"
                }
                val tokens = normalizeArabic("${m.title} ${m.originalTitle ?: ""} ${m.genre} ${m.category} ${m.year} $allCategoryTranslations ${m.director} ${m.cast.joinToString(" ")}")
                IndexedMovie(m, tokens)
            }
        }
    }

    private fun rebuildSeriesIndex(list: List<Series>) {
        viewModelScope.launch(Dispatchers.Default) {
            indexedSeries = list.map { s ->
                val tokens = normalizeArabic("${s.title} ${s.originalTitle ?: ""} ${s.genre} ${s.year} ${s.description}")
                IndexedSeries(s, tokens)
            }
        }
    }

    private fun rebuildChannelIndex(list: List<Channel>) {
        viewModelScope.launch(Dispatchers.Default) {
            indexedChannels = list.map { c ->
                val tokens = normalizeArabic("${c.name} ${c.category}")
                IndexedChannel(c, tokens)
            }
        }
    }

    private fun rebuildMatchIndex(list: List<Match>) {
        viewModelScope.launch(Dispatchers.Default) {
            indexedMatches = list.map { m ->
                val tokens = normalizeArabic("${m.homeTeam} ${m.awayTeam} ${m.competition} ${m.channel} ${m.commentator}")
                IndexedMatch(m, tokens)
            }
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    private fun setupDebouncedSearch() {
        viewModelScope.launch {
            _query
                .debounce(250)
                .distinctUntilChanged()
                .collectLatest { q ->
                    executeSearch(q.trim())
                }
        }
    }

    fun performSearch() {
        executeSearch(_query.value.trim())
    }

    private fun executeSearch(trimmed: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (trimmed.isEmpty()) {
                _searchResult.value = SearchResultState(query = "", isSearching = false)
                return@launch
            }

            if (ContentFilterEngine.isContentBlocked(trimmed)) {
                _searchResult.value = SearchResultState(query = trimmed, isBlocked = true, isSearching = false)
                return@launch
            }

            _searchResult.value = _searchResult.value.copy(isSearching = true)

            val normalizedQ = normalizeArabic(trimmed)
            val matchResults = indexedMatches
                .filter { it.tokens.contains(normalizedQ) }
                .map { it.match }

            val mResults = indexedMovies
                .filter { it.tokens.contains(normalizedQ) }
                .map { it.movie }
                .toMutableList()

            val sResults = indexedSeries
                .filter { it.tokens.contains(normalizedQ) }
                .map { it.series }
                .toMutableList()

            val cResults = indexedChannels
                .filter { it.tokens.contains(normalizedQ) }
                .map { it.channel }

            val initialTotal = matchResults.size + mResults.size + sResults.size + cResults.size
            _searchResult.value = SearchResultState(
                query = trimmed,
                isSearching = trimmed.length >= 2,
                isBlocked = false,
                matchedMatches = matchResults,
                matchedMovies = mResults.toList(),
                matchedSeries = sResults.toList(),
                matchedChannels = cResults,
                totalCount = initialTotal
            )

            // Asynchronously fetch live results from 220,000+ online database
            if (trimmed.length >= 2) {
                try {
                    val client = com.beouttv.app.data.api.CoreApiClient()
                    val onlineItems = client.getItems(page = 1, perPage = 25, searchQuery = trimmed)
                    if (onlineItems.isNotEmpty()) {
                        for (item in onlineItems) {
                            val isSeriesItem = item.isSeries ||
                                item.category?.contains("مسلسل") == true ||
                                item.category?.contains("انمي") == true ||
                                item.category?.contains("series", ignoreCase = true) == true ||
                                item.title.contains("مسلسل") ||
                                item.title.contains("الموسم") ||
                                item.title.contains("الحلقة") ||
                                item.title.contains("حلقة") ||
                                item.title.contains("Season", ignoreCase = true) ||
                                item.title.contains("Episode", ignoreCase = true)

                            val cleanTitle = if (isSeriesItem) {
                                item.title
                                    .replace(Regex("(?i)الموسم\\s*\\d+|حلقة\\s*\\d+|الحلقة\\s*\\d+|مترجم|مدبلج|اون لاين|كامل|برابط واحد"), "")
                                    .trim()
                                    .ifBlank { item.title }
                            } else item.title

                            val displayTitle = com.beouttv.app.data.imdb.ImdbMetadataEngine.cleanDisplayTitle(cleanTitle)
                            if (ContentFilterEngine.isContentBlocked(displayTitle, item.title, item.category ?: "", item.story)) continue

                            if (isSeriesItem) {
                                val seriesId = item.seriesSlug?.ifBlank { null } ?: item.id
                                if (sResults.none { it.id == seriesId || it.id == item.id || it.title.equals(displayTitle, ignoreCase = true) }) {
                                    sResults.add(
                                        Series(
                                            id = seriesId,
                                            title = displayTitle,
                                            originalTitle = cleanTitle,
                                            posterUrl = item.thumbnail,
                                            backdropUrl = item.thumbnail,
                                            year = item.releaseYear,
                                            rating = item.rating,
                                            genre = item.genre.ifBlank { item.category ?: "مسلسلات" },
                                            description = item.story
                                        )
                                    )
                                }
                            } else {
                                if (mResults.none { it.id == item.id || it.title.equals(displayTitle, ignoreCase = true) }) {
                                    mResults.add(
                                        Movie(
                                            id = item.id,
                                            title = displayTitle,
                                            originalTitle = item.title,
                                            posterUrl = item.thumbnail,
                                            backdropUrl = item.thumbnail,
                                            year = item.releaseYear,
                                            rating = item.rating,
                                            imdbRatingFormatted = if (item.rating > 0f) "${item.rating}/10" else "",
                                            quality = item.quality,
                                            duration = "115 دقيقة",
                                            genre = item.genre.ifBlank { item.category ?: "أفلام" },
                                            description = item.story,
                                            streamUrl = "https://govid.live/prem-${item.realId}.m3u8"
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Query MovieBox live catalog concurrently
                    try {
                        val mbItems = com.beouttv.app.data.api.moviebox.MovieBoxApiClient.search(trimmed)
                        for (item in mbItems) {
                            if (item.subjectType == 2) {
                                val s = com.beouttv.app.data.api.moviebox.MovieBoxMapper.toSeries(item)
                                if (sResults.none { it.id == s.id || it.title.equals(s.title, ignoreCase = true) }) {
                                    sResults.add(s)
                                    repository.cacheDiscoveredSeries(s)
                                }
                            } else {
                                val m = com.beouttv.app.data.api.moviebox.MovieBoxMapper.toMovie(item)
                                if (mResults.none { it.id == m.id || it.title.equals(m.title, ignoreCase = true) }) {
                                    mResults.add(m)
                                    repository.cacheDiscoveredMovie(m)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                } catch (_: Exception) {
                } finally {
                    _searchResult.value = SearchResultState(
                        query = trimmed,
                        isSearching = false,
                        isBlocked = false,
                        matchedMatches = matchResults,
                        matchedMovies = mResults.toList(),
                        matchedSeries = sResults.toList(),
                        matchedChannels = cResults,
                        totalCount = matchResults.size + mResults.size + sResults.size + cResults.size
                    )
                }
            } else {
                _searchResult.value = _searchResult.value.copy(isSearching = false)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun toggleFavoriteChannel(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteChannel(channelId)
            _rawChannels.value = repository.getChannels("All")
        }
    }

    fun setReminder(context: Context, match: Match) {
        com.beouttv.app.data.notification.NotificationsManager.scheduleMatchReminder(
            context = context,
            match = match
        )
    }

    fun loadRecentSearches(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT, null)
        if (raw != null) {
            val list = raw.split("|||").filter { it.isNotBlank() }
            _recentSearches.value = list
        } else {
            _recentSearches.value = listOf("beIN Sports", "Action", "4K HDR")
        }
    }

    fun addRecentSearch(context: Context, queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.length < 2) return
        val current = _recentSearches.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) || trimmed.startsWith(it, ignoreCase = true) || it.startsWith(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val limited = current.take(8)
        _recentSearches.value = limited
        val prefs = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENT, limited.joinToString("|||")).apply()
    }

    fun clearRecentSearches(context: Context) {
        _recentSearches.value = emptyList()
        val prefs = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENT).apply()
    }
}

@Composable
fun SearchScreen(
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: ((Series) -> Unit)? = null,
    onChannelClick: ((Channel) -> Unit)? = null,
    onMatchClick: ((Match) -> Unit)? = null,
    viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val query by viewModel.query.collectAsState()
    val searchState by viewModel.searchResult.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadRecentSearches(context)
        SearchViewModel.initialQuery?.let { q ->
            viewModel.onQueryChange(q)
            SearchViewModel.initialQuery = null
        }
    }

    val quickSuggestions = listOf(
        "أفلام أجنبي" to S.foreignMovies(),
        "أفلام عربي" to S.arabicMovies(),
        "أفلام أنمي وكرتون" to S.animeMovies(),
        "أفلام هندية" to S.indianMovies(),
        "أفلام آسيوية" to S.asianMovies(),
        "أفلام تركية" to S.turkishMovies(),
        "أكشن" to S.action(),
        "كوميدي" to S.comedy(),
        "رعب وغموض" to S.horror(),
        "قنوات رياضية" to tr("قنوات رياضية", "Sports Channels", "Spor Kanalları", "شبکه‌های ورزشی", "Chaînes de sport"),
        "قنوات إخبارية" to tr("قنوات إخبارية", "News Channels", "Haber Kanalları", "شبکه‌های خبری", "Chaînes d'information")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .then(if (!isTvMode) Modifier.statusBarsPadding() else Modifier.padding(top = 10.dp))
    ) {
        if (isTvMode) {
            // Sleek Inline Smart TV Search Header (One horizontal row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 76.dp,
                        end = BeoutDimensions.ScreenPadding,
                        top = 6.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = S.search(),
                    style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp),
                    color = BeoutColors.TextPrimary
                )
                Spacer(Modifier.width(16.dp))
                BeoutSearchBar(
                    query = query,
                    onQueryChange = { newQ -> viewModel.onQueryChange(newQ) },
                    onSearch = {
                        viewModel.performSearch()
                        if (query.trim().isNotEmpty()) {
                            viewModel.addRecentSearch(context, query.trim())
                        }
                    },
                    placeholder = S.searchPlaceholder(),
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = S.clear(),
                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = BeoutColors.BrightBlue,
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .clickable { viewModel.onQueryChange("") }
                            .tvFocusable(shape = BeoutShapes.Small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // Mobile Stacked Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = S.search(),
                    style = BeoutTextStyles.ScreenTitle,
                    color = BeoutColors.TextPrimary
                )
                Spacer(Modifier.weight(1f))
                if (query.isNotBlank()) {
                    Text(
                        text = S.clear(),
                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = BeoutColors.BrightBlue,
                        modifier = Modifier.clickable { viewModel.onQueryChange("") }
                    )
                }
            }

            BeoutSearchBar(
                query = query,
                onQueryChange = { newQ -> viewModel.onQueryChange(newQ) },
                onSearch = {
                    viewModel.performSearch()
                    if (query.trim().isNotEmpty()) {
                        viewModel.addRecentSearch(context, query.trim())
                    }
                },
                placeholder = S.searchPlaceholder(),
                modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
            )
        }

        AnimatedVisibility(visible = searchState.isSearching) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 4.dp)
                    .height(2.dp),
                color = BeoutColors.BrightBlue,
                trackColor = BeoutColors.ElevatedSurface
            )
        }

        Spacer(Modifier.height(if (isTvMode) 6.dp else BeoutDimensions.SpaceS))

        // Body Content
        if (query.isBlank()) {
            // Dynamic State BEFORE Search (Suggestions, Quick Categories, Trending Highlights)
            LazyColumn(
                contentPadding = PaddingValues(
                    start = if (isTvMode) 76.dp else 0.dp,
                    bottom = if (isTvMode) 32.dp else 80.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Quick Explore Tags
                item {
                    Column(modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS)) {
                        SectionHeader(title = tr("أقسام واقتراحات سريعة", "Quick Categories & Explore", "Hızlı Kategoriler", "دسته‌بندی‌های سریع", "Catégories rapides"), icon = Icons.Rounded.Explore)
                        Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceXS),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(quickSuggestions) { (key, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Large)
                                        .background(BeoutColors.ElevatedSurface)
                                        .border(1.dp, BeoutColors.Divider, BeoutShapes.Large)
                                        .clickable {
                                            viewModel.onQueryChange(label)
                                            viewModel.addRecentSearch(context, label)
                                        }
                                        .tvFocusable(shape = BeoutShapes.Large)
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                        color = BeoutColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Recent Searches (سجل البحث الأخير)
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(title = tr("عمليات البحث الأخيرة", "Recent Searches", "Son Aramalar", "جستجوهای اخیر", "Recherches récentes"), icon = Icons.Rounded.History)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = tr("مسح السجل", "Clear History", "Geçmişi Temizle", "پاک کردن تاریخچه", "Effacer"),
                                    style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                    color = BeoutColors.TextDisabled,
                                    modifier = Modifier.clickable { viewModel.clearRecentSearches(context) }
                                )
                            }
                            Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceXS),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(recentSearches) { recent ->
                                    Row(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Small)
                                            .background(BeoutColors.Card)
                                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                                            .clickable { viewModel.onQueryChange(recent) }
                                            .tvFocusable(shape = BeoutShapes.Small)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = BeoutColors.TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = recent,
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                                            color = BeoutColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Trending & Recommended Movies Highlights
                item {
                    val recommended = remember(movies) {
                        if (movies.isEmpty()) emptyList()
                        else {
                            val anime = movies.filter { it.genre.contains("أنمي", true) || it.genre.contains("انمي", true) || it.genre.contains("anime", true) || it.genre.contains("animation", true) }.drop(2).take(3)
                            val asian = movies.filter { it.genre.contains("آسيوية", true) || it.genre.contains("اسيوية", true) || it.genre.contains("asian", true) || it.genre.contains("asya", true) }.drop(2).take(3)
                            val indian = movies.filter { it.genre.contains("هندية", true) || it.genre.contains("هندي", true) || it.genre.contains("indian", true) || it.genre.contains("hint", true) }.drop(2).take(3)
                            val turkish = movies.filter { it.genre.contains("تركية", true) || it.genre.contains("تركي", true) || it.genre.contains("turkish", true) || it.genre.contains("türk", true) }.drop(1).take(3)
                            val arabic = movies.filter { it.genre.contains("عربي", true) || it.genre.contains("arabic", true) || it.genre.contains("arap", true) }.drop(5).take(3)
                            (anime + asian + indian + turkish + arabic).shuffled().take(12)
                        }
                    }
                    if (recommended.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceS)) {
                            SectionHeader(
                                title = tr("أفلام مقترحة ومختارة", "Featured Recommendations", "Önerilen Filmler", "فیلم‌های پیشنهادی", "Films suggérés"),
                                icon = Icons.Rounded.TrendingUp,
                                modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
                            )
                            Spacer(Modifier.height(BeoutDimensions.SpaceS))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else BeoutDimensions.SpaceS)
                            ) {
                                items(recommended, key = { it.id }) { movie ->
                                    BeoutMovieCard(
                                        movie = movie,
                                        onClick = { onMovieClick(movie) },
                                        modifier = if (isTvMode) Modifier.width(160.dp).tvFocusable(shape = BeoutShapes.Medium) else Modifier
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Suggested Channels
                item {
                    val suggestedChannels = remember(channels) {
                        channels.filter {
                            it.category.contains("شاهد", true) ||
                            it.category.contains("ألوان", true) ||
                            it.category.contains("وثائقية", true) ||
                            it.category.contains("MBC", true) ||
                            it.category.contains("TOD", true) ||
                            it.name.contains("Cinema", true) ||
                            it.name.contains("Movies", true)
                        }.take(12)
                    }
                    if (suggestedChannels.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceM)) {
                            SectionHeader(
                                title = tr("قنوات سينما وترفيه مختارة", "Cinema & Live Channels", "Seçkin TV Kanalları", "شبکه‌های منتخب", "Chaînes sélectionnées"),
                                icon = Icons.Rounded.LiveTv,
                                modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
                            )
                            Spacer(Modifier.height(BeoutDimensions.SpaceS))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else BeoutDimensions.SpaceS)
                            ) {
                                items(suggestedChannels, key = { it.id }) { ch ->
                                    BeoutChannelCard(
                                        channel = ch,
                                        onClick = { onChannelClick?.invoke(ch) },
                                        onFavoriteClick = { viewModel.toggleFavoriteChannel(ch.id) },
                                        modifier = if (isTvMode) Modifier.width(BeoutDimensions.TvChannelCardWidth).tvFocusable(shape = BeoutShapes.Medium) else Modifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Dynamic State WHILE Searching (Pre-Indexed High Speed Background Search)
            if (searchState.isBlocked) {
                BeoutEmptyState(
                    title = "محتوى غير متاح",
                    description = "تم حجب هذا البحث لتوافقه مع إرشادات الأمان الأسري والأخلاقي."
                )
            } else if (searchState.totalCount == 0 && !searchState.isSearching) {
                BeoutEmptyState(
                    title = "لا توجد نتائج لـ \"$query\"",
                    description = "تأكد من كتابة الاسم بشكل صحيح أو اختر أحد الاقتراحات السريعة."
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    var selectedResultTab by remember { mutableIntStateOf(0) }
                    val searchTabs = listOf(
                        0 to tr("الكل (${searchState.totalCount})", "All (${searchState.totalCount})", "Tümü (${searchState.totalCount})", "همه (${searchState.totalCount})", "Tous (${searchState.totalCount})"),
                        1 to tr("مباريات (${searchState.matchedMatches.size})", "Matches (${searchState.matchedMatches.size})", "Maçlar (${searchState.matchedMatches.size})", "مسابقات (${searchState.matchedMatches.size})", "Matchs (${searchState.matchedMatches.size})"),
                        2 to tr("قنوات (${searchState.matchedChannels.size})", "Channels (${searchState.matchedChannels.size})", "Kanallar (${searchState.matchedChannels.size})", "شبکه‌ها (${searchState.matchedChannels.size})", "Chaînes (${searchState.matchedChannels.size})"),
                        3 to tr("أفلام (${searchState.matchedMovies.size})", "Movies (${searchState.matchedMovies.size})", "Filmler (${searchState.matchedMovies.size})", "فیلم‌ها (${searchState.matchedMovies.size})", "Films (${searchState.matchedMovies.size})"),
                        4 to tr("مسلسلات (${searchState.matchedSeries.size})", "Series (${searchState.matchedSeries.size})", "Diziler (${searchState.matchedSeries.size})", "سریال‌ها (${searchState.matchedSeries.size})", "Séries (${searchState.matchedSeries.size})")
                    )

                    LazyRow(
                        contentPadding = PaddingValues(
                            start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding,
                            end = BeoutDimensions.ScreenPadding,
                            top = 6.dp,
                            bottom = 6.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchTabs) { (tabIndex, tabTitle) ->
                            val isSelected = selectedResultTab == tabIndex
                            val bg = if (isSelected) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface
                            val border = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider
                            val textCol = if (isSelected) Color.White else BeoutColors.TextSecondary

                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Medium)
                                    .background(bg)
                                    .border(1.dp, border, BeoutShapes.Medium)
                                    .clickable { selectedResultTab = tabIndex }
                                    .tvFocusable(shape = BeoutShapes.Medium)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tabTitle,
                                    style = BeoutTextStyles.Caption.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = textCol
                                )
                            }
                        }
                    }

                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp

                    when (selectedResultTab) {
                        0 -> {
                            // Comprehensive unified view with structured sections
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = if (isTvMode) 76.dp else 0.dp,
                                    bottom = if (isTvMode) 32.dp else 90.dp
                                )
                            ) {
                                // 1. Matches Section
                                if (searchState.matchedMatches.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceS)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SectionHeader(
                                                    title = tr("مباريات كرة القدم (${searchState.matchedMatches.size})", "Football Matches (${searchState.matchedMatches.size})", "Maçlar (${searchState.matchedMatches.size})", "مسابقات (${searchState.matchedMatches.size})", "Matchs (${searchState.matchedMatches.size})"),
                                                    icon = Icons.Rounded.SportsSoccer
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (searchState.matchedMatches.size > 3) {
                                                    Text(
                                                        text = tr("عرض الكل", "See All", "Tümünü Gör", "مشاهده همه", "Voir tout"),
                                                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                                                        color = BeoutColors.BrightBlue,
                                                        modifier = Modifier.clickable { selectedResultTab = 1 }
                                                    )
                                                }
                                            }
                                            searchState.matchedMatches.take(3).forEach { match ->
                                                BeoutMatchCard(
                                                    match = match,
                                                    onWatchClick = { onMatchClick?.invoke(match) },
                                                    onReminderClick = { viewModel.setReminder(context, match) },
                                                    modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 4.dp).tvFocusable(shape = BeoutShapes.Medium)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Channels Section
                                if (searchState.matchedChannels.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceM)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SectionHeader(
                                                    title = tr("قنوات البث المباشر (${searchState.matchedChannels.size})", "Live Channels (${searchState.matchedChannels.size})", "Canlı Kanallar (${searchState.matchedChannels.size})", "شبکه‌های زنده (${searchState.matchedChannels.size})", "Chaînes en direct (${searchState.matchedChannels.size})"),
                                                    icon = Icons.Rounded.LiveTv
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (searchState.matchedChannels.size > 4) {
                                                    Text(
                                                        text = tr("عرض الكل", "See All", "Tümünü Gör", "مشاهده همه", "Voir tout"),
                                                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                                                        color = BeoutColors.BrightBlue,
                                                        modifier = Modifier.clickable { selectedResultTab = 2 }
                                                    )
                                                }
                                            }
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                                                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else BeoutDimensions.SpaceS)
                                            ) {
                                                items(searchState.matchedChannels.take(if (isTvMode) 12 else 8), key = { "ch_" + it.id }) { ch ->
                                                    BeoutChannelCard(
                                                        channel = ch,
                                                        onClick = {
                                                            viewModel.addRecentSearch(context, query)
                                                            onChannelClick?.invoke(ch)
                                                        },
                                                        onFavoriteClick = { viewModel.toggleFavoriteChannel(ch.id) },
                                                        modifier = Modifier.width(if (isTvMode) BeoutDimensions.TvChannelCardWidth else BeoutDimensions.ChannelCardWidth).tvFocusable(shape = BeoutShapes.Medium)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Movies Section
                                if (searchState.matchedMovies.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceM)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SectionHeader(
                                                    title = tr("الأفلام والسينما (${searchState.matchedMovies.size})", "Movies (${searchState.matchedMovies.size})", "Filmler (${searchState.matchedMovies.size})", "فیلم‌ها (${searchState.matchedMovies.size})", "Films (${searchState.matchedMovies.size})"),
                                                    icon = Icons.Rounded.Movie
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (searchState.matchedMovies.size > 4) {
                                                    Text(
                                                        text = tr("عرض الكل", "See All", "Tümünü Gör", "مشاهده همه", "Voir tout"),
                                                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                                                        color = BeoutColors.BrightBlue,
                                                        modifier = Modifier.clickable { selectedResultTab = 3 }
                                                    )
                                                }
                                            }
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                                                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else BeoutDimensions.SpaceS)
                                            ) {
                                                items(searchState.matchedMovies.take(if (isTvMode) 16 else 10), key = { "m_" + it.id }) { movie ->
                                                    BeoutMovieCard(
                                                        movie = movie,
                                                        onClick = {
                                                            com.beouttv.app.data.repository.ServiceLocator.repository.cacheDiscoveredMovie(movie)
                                                            viewModel.addRecentSearch(context, query)
                                                            onMovieClick(movie)
                                                        },
                                                        modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Series Section
                                if (searchState.matchedSeries.isNotEmpty()) {
                                    item {
                                        Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceM)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SectionHeader(
                                                    title = tr("المسلسلات والإنتاج التلفزيوني (${searchState.matchedSeries.size})", "Series (${searchState.matchedSeries.size})", "Diziler (${searchState.matchedSeries.size})", "سریال‌ها (${searchState.matchedSeries.size})", "Séries (${searchState.matchedSeries.size})"),
                                                    icon = Icons.Rounded.Tv
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (searchState.matchedSeries.size > 4) {
                                                    Text(
                                                        text = tr("عرض الكل", "See All", "Tümünü Gör", "مشاهده همه", "Voir tout"),
                                                        style = BeoutTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                                                        color = BeoutColors.BrightBlue,
                                                        modifier = Modifier.clickable { selectedResultTab = 4 }
                                                    )
                                                }
                                            }
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                                                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else BeoutDimensions.SpaceS)
                                            ) {
                                                items(searchState.matchedSeries.take(if (isTvMode) 16 else 10), key = { "s_" + it.id }) { series ->
                                                    BeoutSeriesCard(
                                                        series = series,
                                                        onClick = {
                                                            com.beouttv.app.data.repository.ServiceLocator.repository.cacheDiscoveredSeries(series)
                                                            viewModel.addRecentSearch(context, query)
                                                            onSeriesClick?.invoke(series)
                                                        },
                                                        modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Football Matches Tab
                            LazyColumn(
                                contentPadding = PaddingValues(start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding, end = BeoutDimensions.ScreenPadding, top = 8.dp, bottom = if (isTvMode) 32.dp else 90.dp),
                                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(searchState.matchedMatches, key = { "match_" + it.id }) { match ->
                                    BeoutMatchCard(
                                        match = match,
                                        onWatchClick = { onMatchClick?.invoke(match) },
                                        onReminderClick = { viewModel.setReminder(context, match) },
                                        modifier = Modifier.tvFocusable(shape = BeoutShapes.Medium)
                                    )
                                }
                            }
                        }
                        2 -> {
                            // Live TV Channels Tab (Adaptive responsive grid from mobile to tablets/TV)
                            val searchChannelCols = when {
                                isTvMode -> 5
                                screenWidth >= 900.dp -> 6
                                screenWidth >= 720.dp -> 5
                                screenWidth >= 480.dp -> 4
                                screenWidth >= 340.dp -> 3
                                else -> 2
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(searchChannelCols),
                                contentPadding = PaddingValues(start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding, end = BeoutDimensions.ScreenPadding, top = 8.dp, bottom = if (isTvMode) 32.dp else 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(searchState.matchedChannels, key = { "ch_" + it.id }) { ch ->
                                    BeoutChannelCard(
                                        channel = ch,
                                        onClick = {
                                            viewModel.addRecentSearch(context, query)
                                            onChannelClick?.invoke(ch)
                                        },
                                        onFavoriteClick = { viewModel.toggleFavoriteChannel(ch.id) },
                                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Medium)
                                    )
                                }
                            }
                        }
                        3 -> {
                            // Movies Tab (Adaptive responsive grid from mobile to tablets/TV)
                            val searchMovieCols = when {
                                isTvMode -> 5
                                screenWidth >= 900.dp -> 6
                                screenWidth >= 720.dp -> 5
                                screenWidth >= 540.dp -> 4
                                screenWidth >= 350.dp -> 3
                                else -> 2
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(searchMovieCols),
                                contentPadding = PaddingValues(start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding, end = BeoutDimensions.ScreenPadding, top = 8.dp, bottom = if (isTvMode) 32.dp else 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceM),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(searchState.matchedMovies, key = { "m_" + it.id }) { movie ->
                                    BeoutMovieCard(
                                        movie = movie,
                                        onClick = {
                                            com.beouttv.app.data.repository.ServiceLocator.repository.cacheDiscoveredMovie(movie)
                                            viewModel.addRecentSearch(context, query)
                                            onMovieClick(movie)
                                        },
                                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Medium)
                                    )
                                }
                            }
                        }
                        4 -> {
                            // Series Tab (Adaptive responsive grid from mobile to tablets/TV)
                            val searchSeriesCols = when {
                                isTvMode -> 5
                                screenWidth >= 900.dp -> 6
                                screenWidth >= 720.dp -> 5
                                screenWidth >= 540.dp -> 4
                                screenWidth >= 350.dp -> 3
                                else -> 2
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(searchSeriesCols),
                                contentPadding = PaddingValues(start = if (isTvMode) 76.dp else BeoutDimensions.ScreenPadding, end = BeoutDimensions.ScreenPadding, top = 8.dp, bottom = if (isTvMode) 32.dp else 90.dp),
                                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceM),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(searchState.matchedSeries, key = { "s_" + it.id }) { series ->
                                    BeoutSeriesCard(
                                        series = series,
                                        onClick = {
                                            com.beouttv.app.data.repository.ServiceLocator.repository.cacheDiscoveredSeries(series)
                                            viewModel.addRecentSearch(context, query)
                                            onSeriesClick?.invoke(series)
                                        },
                                        modifier = Modifier.fillMaxWidth().tvFocusable(shape = BeoutShapes.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Helper Components & Normalizer ----------------

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = BeoutColors.BrightBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = BeoutTextStyles.SectionTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = BeoutColors.TextPrimary
        )
    }
}

private fun normalizeArabic(text: String): String {
    return text.lowercase()
        .replace(Regex("[\\u064B-\\u065F]"), "") // Remove diacritics / tashkeel
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')
        .replace(Regex("[^a-z0-9\\u0600-\\u06FF\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
