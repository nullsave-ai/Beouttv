package com.beouttv.app.ui.screens.movies

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.beouttv.app.data.api.moviebox.MovieBoxApiClient
import com.beouttv.app.data.api.moviebox.MovieBoxItem
import com.beouttv.app.data.api.moviebox.MovieBoxMapper
import com.beouttv.app.data.filter.ContentFilterEngine
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CatalogType {
    ANIME, TRENDING, ALL_MOVIES, ALL_SERIES
}

class MovieBoxCatalogViewModel(
    val catalogType: CatalogType
) : ViewModel() {
    private val _items = MutableStateFlow<List<MovieBoxItem>>(emptyList())
    val items: StateFlow<List<MovieBoxItem>> = _items.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MovieBoxItem>>(emptyList())
    val searchResults: StateFlow<List<MovieBoxItem>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedGenre = MutableStateFlow("all")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private var currentPage = 1
    private var hasMore = true
    private var searchJob: Job? = null

    init {
        loadData(reset = true)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        // Live debounce search
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400)
            executeSearch(query)
        }
    }

    fun executeSearch(query: String) {
        if (query.isBlank()) return
        _isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = MovieBoxApiClient.search(query)
                val filtered = results.filter {
                    !ContentFilterEngine.isContentBlocked(it.title, it.genre, it.description)
                }
                _searchResults.value = filtered
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun selectGenre(genre: String) {
        if (_selectedGenre.value != genre) {
            _selectedGenre.value = genre
            if (catalogType != CatalogType.TRENDING && catalogType != CatalogType.ANIME) {
                loadData(reset = true)
            }
        }
    }

    fun loadData(reset: Boolean = false) {
        if (reset) {
            currentPage = 1
            hasMore = true
            _items.value = emptyList()
            _isLoading.value = true
        } else {
            if (_isLoadingMore.value || !hasMore) return
            _isLoadingMore.value = true
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val genreParam = if (_selectedGenre.value == "all") "" else _selectedGenre.value
                val fetched = when (catalogType) {
                    CatalogType.ANIME -> MovieBoxApiClient.getAnime(page = currentPage, perPage = 20, genre = genreParam)
                    CatalogType.TRENDING -> MovieBoxApiClient.getTrending(page = currentPage, perPage = 20)
                    CatalogType.ALL_MOVIES -> MovieBoxApiClient.getMovies(page = currentPage, perPage = 20, genre = genreParam)
                    CatalogType.ALL_SERIES -> MovieBoxApiClient.getSeries(page = currentPage, perPage = 20, genre = genreParam)
                }.filter { !ContentFilterEngine.isContentBlocked(it.title, it.genre, it.description) }

                if (fetched.isEmpty()) {
                    hasMore = false
                } else {
                    currentPage++
                    val current = _items.value.toMutableList()
                    fetched.forEach { item ->
                        if (current.none { it.subjectId == item.subjectId }) {
                            current.add(item)
                        }
                    }
                    _items.value = current
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
            }
        }
    }
}

@Composable
fun MovieBoxCatalogScreen(
    catalogType: CatalogType,
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<MovieBoxCatalogViewModel>(
        key = catalogType.name,
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MovieBoxCatalogViewModel(catalogType) as T
        }
    )

    val items by viewModel.items.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val focusManager = LocalFocusManager.current

    val title = when (catalogType) {
        CatalogType.ANIME -> tr("عالم الأنمي الياباني", "Anime Universe", "Anime Dünyası", "دنیای انیمه", "Univers Anime")
        CatalogType.TRENDING -> tr("الأكثر رواجاً في وان+", "ONE+ Top Trending", "En Çok İzlenenler", "پربازدیدترین‌ها", "Tendances")
        CatalogType.ALL_MOVIES -> tr("أفلام وان+ الشاملة", "ONE+ Movies", "Filmler", "فیلم‌ها", "Films")
        CatalogType.ALL_SERIES -> tr("مسلسلات وان+ الشاملة", "ONE+ Series", "Diziler", "سریال‌ها", "Séries")
    }

    val genres = remember(catalogType) {
        when (catalogType) {
            CatalogType.ALL_MOVIES -> listOf(
                "all" to tr("الكل", "All", "Tümü", "همه", "Tous"),
                "Action" to tr("أكشن", "Action", "Aksiyon", "اکشن", "Action"),
                "Comedy" to tr("كوميديا", "Comedy", "Komedi", "کمدی", "Comédie"),
                "Anime" to tr("أنمي", "Anime", "Anime", "انیمه", "Anime"),
                "Romance" to tr("رومانسي", "Romance", "Romantik", "عاشقانه", "Romance"),
                "Adventure" to tr("مغامرات", "Adventure", "Macera", "ماجراجویی", "Aventure"),
                "Drama" to tr("دراما", "Drama", "Dram", "درام", "Drame"),
                "Horror" to tr("رعب", "Horror", "Korku", "ترسناک", "Horreur"),
                "Sci-Fi" to tr("خيال علمي", "Sci-Fi", "Bilim Kurgu", "علمی تخیلی", "Science-fiction"),
                "Thriller" to tr("إثارة وغموض", "Thriller", "Gerilim", "مهیج", "Thriller")
            )
            CatalogType.ALL_SERIES -> listOf(
                "all" to tr("الكل", "All", "Tümü", "همه", "Tous"),
                "Action" to tr("أكشن", "Action", "Aksiyon", "اکشن", "Action"),
                "Comedy" to tr("كوميديا", "Comedy", "Komedi", "کمدی", "Comédie"),
                "Romance" to tr("رومانسي", "Romance", "Romantik", "عاشقانه", "Romance"),
                "Adventure" to tr("مغامرات", "Adventure", "Macera", "ماجراجویی", "Aventure"),
                "Drama" to tr("دراما", "Drama", "Dram", "درام", "Drame"),
                "Crime" to tr("جريمة", "Crime", "Suç", "جنایی", "Crime"),
                "Mystery" to tr("غموض", "Mystery", "Gizem", "معمایی", "Mystère")
            )
            CatalogType.ANIME -> listOf(
                "all" to tr("الكل", "All", "Tümü", "همه", "Tous"),
                "series" to tr("مسلسلات الأنمي", "Anime Series", "Anime Dizileri", "سریال‌های انیمه", "Séries Anime"),
                "movies" to tr("أفلام الأنمي", "Anime Movies", "Anime Filmleri", "فیلم‌های انیمه", "Films Anime")
            )
            CatalogType.TRENDING -> listOf(
                "all" to tr("الكل", "All", "Tümü", "همه", "Tous"),
                "movies" to tr("الأفلام", "Movies", "Filmler", "فیلم‌ها", "Films"),
                "series" to tr("المسلسلات", "Series", "Diziler", "سریال‌ها", "Séries")
            )
        }
    }

    val displayedItems = remember(items, searchResults, searchQuery, selectedGenre, catalogType) {
        if (searchQuery.isNotBlank()) {
            if (searchResults.isNotEmpty()) {
                searchResults
            } else {
                items.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.genre.contains(searchQuery, ignoreCase = true)
                }
            }
        } else {
            when (catalogType) {
                CatalogType.TRENDING -> {
                    when (selectedGenre) {
                        "movies" -> items.filter { it.subjectType == 1 }
                        "series" -> items.filter { it.subjectType == 2 }
                        else -> items
                    }
                }
                CatalogType.ANIME -> {
                    when (selectedGenre) {
                        "movies" -> items.filter { it.subjectType == 1 }
                        "series" -> items.filter { it.subjectType == 2 }
                        else -> items
                    }
                }
                else -> items
            }
        }
    }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (searchQuery.isBlank() && lastVisibleIndex != null && lastVisibleIndex >= items.size - 6) {
                    viewModel.loadData(reset = false)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .statusBarsPadding()
    ) {
        // Modern iOS Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BeoutDimensions.TopBarHeight)
                .padding(horizontal = BeoutDimensions.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeoutIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = S.back(),
                onClick = onBack
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = BeoutTextStyles.SectionTitle.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BeoutColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tr("مكتبة سينمائية متطورة فائقة السرعة", "High-speed Cinema Catalog", "Sinema Kataloğu", "کاتالوگ سینما", "Catalogue Cinéma"),
                    style = BeoutTextStyles.Caption.copy(
                        fontSize = 10.sp,
                        color = BeoutColors.TextSecondary
                    ),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BeoutColors.ElevatedSurface)
                    .border(0.8.dp, BeoutColors.Divider, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${displayedItems.size}+",
                    style = BeoutTextStyles.Caption.copy(
                        fontSize = 11.sp,
                        color = BeoutColors.BrightBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Modern iOS Glassmorphic Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BeoutColors.ElevatedSurface)
                    .border(1.dp, BeoutColors.Divider.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (searchQuery.isNotBlank()) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = tr("ابحث في الأفلام والعناوين...", "Search in movies & titles...", "Filmlerde ara...", "جستجو در فیلم‌ها...", "Rechercher des films..."),
                            style = BeoutTextStyles.Body.copy(
                                fontSize = 13.sp,
                                color = BeoutColors.TextSecondary.copy(alpha = 0.6f)
                            )
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(BeoutColors.BrightBlue),
                        textStyle = BeoutTextStyles.Body.copy(
                            fontSize = 13.5.sp,
                            color = BeoutColors.TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.executeSearch(searchQuery)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BeoutColors.BrightBlue
                    )
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearSearch() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "مسح",
                            tint = BeoutColors.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // iOS Capsule Genre Segmented Row (when not actively searching)
        if (searchQuery.isEmpty() && genres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(genres) { (key, label) ->
                    val isSelected = selectedGenre == key
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface,
                        animationSpec = tween(150),
                        label = "genrePillBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else BeoutColors.TextSecondary,
                        animationSpec = tween(150),
                        label = "genrePillText"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(0.8.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, RoundedCornerShape(10.dp))
                            .clickable { viewModel.selectGenre(key) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = BeoutTextStyles.Caption.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = BeoutColors.Divider.copy(alpha = 0.6f))

        // Content Area
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = BeoutColors.BrightBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (displayedItems.isEmpty()) {
            // Modern iOS Empty Search State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(BeoutColors.ElevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = tr("لا توجد نتائج مطابقة", "No Results Found", "Sonuç Bulunamadı", "نتیجه‌ای یافت نشد", "Aucun résultat trouvé"),
                        style = BeoutTextStyles.SectionTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Text(
                        text = tr("جرّب البحث بكلمات أخرى أو اختر تصنيفاً مختلفاً", "Try searching with different terms or select a category", "Farklı terimler deneyin", "با کلمات دیگر جستجو کنید", "Essayez d'autres mots"),
                        style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, color = BeoutColors.TextSecondary),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (searchQuery.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearSearch() },
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = tr("مسح البحث", "Clear Search", "Aramayı Temizle", "پاک کردن جستجو", "Effacer"),
                                style = BeoutTextStyles.ButtonLabel.copy(fontSize = 12.sp, color = Color.White)
                            )
                        }
                    }
                }
            }
        } else {
            // Modern iOS Poster Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                state = gridState,
                contentPadding = PaddingValues(
                    horizontal = BeoutDimensions.ScreenPadding,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedItems, key = { it.subjectId }) { item ->
                    val isSeries = item.subjectType == 2
                    val movieObj = remember(item) { MovieBoxMapper.toMovie(item) }
                    val seriesObj = remember(item) { MovieBoxMapper.toSeries(item) }

                    IosMediaPosterCard(
                        item = item,
                        onClick = {
                            if (isSeries) {
                                ServiceLocator.repository.cacheDiscoveredSeries(seriesObj)
                                onSeriesClick(seriesObj)
                            } else {
                                ServiceLocator.repository.cacheDiscoveredMovie(movieObj)
                                onMovieClick(movieObj)
                            }
                        }
                    )
                }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = BeoutColors.BrightBlue,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sleek iOS Media Poster Card with Glassmorphic Badges and Fluid Scrim
 */
@Composable
private fun IosMediaPosterCard(
    item: MovieBoxItem,
    onClick: () -> Unit
) {
    val ratingValue = item.imdbRatingValue ?: item.score ?: "7.8"
    val isSeries = item.subjectType == 2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(14.dp))
            .background(BeoutColors.ElevatedSurface)
            .border(0.6.dp, BeoutColors.Divider.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        // Poster Image
        AsyncImage(
            model = item.cover?.url.orEmpty(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Glassmorphic Rating Badge (Top Start)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = ratingValue,
                    style = BeoutTextStyles.Caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        // Quality or Series Tag (Top End)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (isSeries) Color(0xFF5856D6).copy(alpha = 0.85f) else BeoutColors.BrightBlue.copy(alpha = 0.85f))
                .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = if (isSeries) "مسلسل" else "FHD",
                style = BeoutTextStyles.Caption.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
        }

        // Bottom Gradient Scrim & Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 7.dp, vertical = 6.dp)
        ) {
            Column {
                Text(
                    text = item.title,
                    style = BeoutTextStyles.Body.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.genre.ifBlank { if (isSeries) "مسلسلات" else "أفلام" },
                    style = BeoutTextStyles.Caption.copy(
                        fontSize = 10.sp,
                        color = BeoutColors.TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
