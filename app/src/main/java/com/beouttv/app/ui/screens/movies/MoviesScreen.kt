package com.beouttv.app.ui.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.MediaCategory
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.data.source.MediaSourceManager
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface MoviesUiState {
    data object Loading : MoviesUiState
    data class Error(val message: String) : MoviesUiState
    data class Success(
        val movies: List<Movie>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : MoviesUiState
}

class MoviesViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {
    companion object {
        var initialCategorySlug: String? = null
    }

    private val _uiState = MutableStateFlow<MoviesUiState>(MoviesUiState.Loading)
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private val _categoriesFlow = MutableStateFlow<List<MediaCategory>>(emptyList())
    val categoriesFlow: StateFlow<List<MediaCategory>> = _categoriesFlow.asStateFlow()

    private val _selectedCategorySlug = MutableStateFlow("all")
    val selectedCategorySlug: StateFlow<String> = _selectedCategorySlug.asStateFlow()

    val activeSource = MediaSourceManager.activeSourceFlow

    private var currentPage = 1
    private var isFetchingMore = false
    private var hasMorePages = true
    private val currentMoviesList = mutableListOf<Movie>()

    init {
        val startCat = initialCategorySlug ?: "all"
        initialCategorySlug = null
        _selectedCategorySlug.value = startCat
        loadCategories()
        load(startCat)
        observeLiveUpdates()

        // When content source switches, reload categories and movies from new source
        viewModelScope.launch {
            MediaSourceManager.sourceSwitchEvent.collect {
                loadCategories()
                load(_selectedCategorySlug.value, isReload = true)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val cats = repository.getCategories().filter { !it.isSeries }
            _categoriesFlow.value = cats
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            repository.moviesFlow.collectLatest { updatedMovies ->
                if (updatedMovies.isNotEmpty() && _selectedCategorySlug.value == "all" && currentPage == 1 && currentMoviesList.isEmpty()) {
                    currentMoviesList.addAll(updatedMovies)
                    _uiState.value = MoviesUiState.Success(
                        movies = currentMoviesList.toList(),
                        isLoadingMore = false,
                        hasMore = true
                    )
                }
            }
        }
    }

    fun load(categorySlug: String = _selectedCategorySlug.value, isReload: Boolean = false) {
        if (_selectedCategorySlug.value != categorySlug || isReload) {
            currentPage = 1
            hasMorePages = true
            isFetchingMore = false
            currentMoviesList.clear()
        }
        _selectedCategorySlug.value = categorySlug

        viewModelScope.launch {
            _uiState.value = MoviesUiState.Loading
            try {
                val list = repository.getMoviesPaged(categorySlug, page = 1, perPage = 30)
                currentMoviesList.clear()
                currentMoviesList.addAll(list)
                hasMorePages = list.size >= 15
                _uiState.value = MoviesUiState.Success(
                    movies = currentMoviesList.toList(),
                    isLoadingMore = false,
                    hasMore = hasMorePages
                )
            } catch (e: Exception) {
                _uiState.value = MoviesUiState.Error(e.message ?: "حدث خطأ أثناء تحميل الأفلام")
            }
        }
    }

    fun loadMore() {
        if (isFetchingMore || !hasMorePages) return
        val currentState = _uiState.value
        if (currentState !is MoviesUiState.Success) return

        isFetchingMore = true
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val nextItems = repository.getMoviesPaged(_selectedCategorySlug.value, page = nextPage, perPage = 30)
                if (nextItems.isNotEmpty()) {
                    val existingIds = currentMoviesList.map { it.id }.toSet()
                    val newUniqueItems = nextItems.filter { !existingIds.contains(it.id) }
                    currentMoviesList.addAll(newUniqueItems)
                    currentPage = nextPage
                    hasMorePages = nextItems.size >= 15
                } else {
                    hasMorePages = false
                }
                _uiState.value = MoviesUiState.Success(
                    movies = currentMoviesList.toList(),
                    isLoadingMore = false,
                    hasMore = hasMorePages
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isLoadingMore = false)
            } finally {
                isFetchingMore = false
            }
        }
    }
}

@Composable
fun MoviesScreen(
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: MoviesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val apiCategories by viewModel.categoriesFlow.collectAsState()
    val selectedSlug by viewModel.selectedCategorySlug.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val xtreamMovies by com.beouttv.app.data.xtream.XtreamAccountManager.xtreamMovies.collectAsState()
    val xtreamAccounts by com.beouttv.app.data.xtream.XtreamAccountManager.accounts.collectAsState()
    val activeMovieAccounts = remember(xtreamAccounts) { xtreamAccounts.filter { it.isActive && it.showMovies } }
    var selectedSourceFilter by rememberSaveable { mutableIntStateOf(com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter) } // 0: All, 1: App Library, 2: Personal Xtream
    var selectedXtreamCategory by rememberSaveable { mutableStateOf(com.beouttv.app.data.xtream.XtreamAccountManager.initialXtreamMoviesCategory) }

    val gridState = rememberLazyGridState()
    val categoryRowState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val target = MoviesViewModel.initialCategorySlug
        if (target != null) {
            MoviesViewModel.initialCategorySlug = null
            if (selectedSourceFilter == 2) {
                selectedSourceFilter = 0
                com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 0
            }
            if (target != selectedSlug) {
                viewModel.load(target)
            }
        }
    }

    LaunchedEffect(selectedSlug, apiCategories) {
        if (selectedSlug != "all" && apiCategories.isNotEmpty()) {
            val idx = apiCategories.indexOfFirst { it.slug == selectedSlug }
            if (idx >= 0) {
                try {
                    categoryRowState.animateScrollToItem((idx + 1).coerceAtLeast(0))
                } catch (_: Exception) {}
            }
        }
    }

    // Detect reaching end of grid for infinite scroll pagination
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = (state as? MoviesUiState.Success)?.movies?.size ?: 0
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            val s = state
            if (s is MoviesUiState.Success && s.hasMore && !s.isLoadingMore) {
                viewModel.loadMore()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .then(if (isTvMode) Modifier.padding(top = 10.dp) else Modifier.statusBarsPadding())
    ) {
        if (isTvMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BeoutIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = S.back(),
                    onClick = onBack,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                )
                Text(S.movies(), style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp), color = BeoutColors.TextPrimary)
                BeoutSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = tr("ابحث في مكتبة الأفلام...", "Search movies library...", "Filmlerde ara...", "جستجو در فیلم‌ها...", "Rechercher dans les films..."),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Clean Top Header with Back Button and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BeoutIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = S.back(),
                    onClick = onBack,
                    modifier = Modifier.tvFocusable(shape = CircleShape)
                )
                Spacer(Modifier.width(BeoutDimensions.SpaceS))
                Text(S.movies(), style = BeoutTextStyles.ScreenTitle, color = BeoutColors.TextPrimary)
            }

            // In-page Search Bar
            BeoutSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = tr("ابحث في مكتبة الأفلام...", "Search movies library...", "Filmlerde ara...", "جستجو در فیلم‌ها...", "Rechercher dans les films..."),
                modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS)
            )
            Spacer(Modifier.height(BeoutDimensions.SpaceS))
        }

        // Switcher Bar: All vs BeoutTV vs Personal Xtream
        if (xtreamMovies.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 0: All
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(BeoutShapes.Small)
                        .background(if (selectedSourceFilter == 0) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface)
                        .border(1.dp, if (selectedSourceFilter == 0) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Small)
                        .clickable {
                            selectedSourceFilter = 0
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 0
                        }
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                            selectedSourceFilter = 0
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 0
                        })
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("الكل", style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp), color = if (selectedSourceFilter == 0) Color.White else BeoutColors.TextSecondary)
                }

                // Option 1: App Library
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(BeoutShapes.Small)
                        .background(if (selectedSourceFilter == 1) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface)
                        .border(1.dp, if (selectedSourceFilter == 1) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Small)
                        .clickable {
                            selectedSourceFilter = 1
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 1
                        }
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                            selectedSourceFilter = 1
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 1
                        })
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("مكتبة بي اوت", style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp), color = if (selectedSourceFilter == 1) Color.White else BeoutColors.TextSecondary)
                }

                // Option 2: Personal Xtream
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(BeoutShapes.Small)
                        .background(if (selectedSourceFilter == 2) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface)
                        .border(1.dp, if (selectedSourceFilter == 2) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Small)
                        .clickable {
                            selectedSourceFilter = 2
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 2
                        }
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                            selectedSourceFilter = 2
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 2
                        })
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("اشتراكي الشخصي", style = BeoutTextStyles.ButtonLabel.copy(fontSize = 11.sp), color = if (selectedSourceFilter == 2) Color.White else BeoutColors.TextSecondary)
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(BeoutShapes.Small)
                                .background(if (selectedSourceFilter == 2) Color.White.copy(alpha = 0.25f) else BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("${xtreamMovies.size}", style = BeoutTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = if (selectedSourceFilter == 2) Color.White else BeoutColors.BrightBlue)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Dynamic Category Filter Rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = BeoutDimensions.SpaceS),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Beout Library Categories (shown for All or App Library)
            if (selectedSourceFilter != 2) {
                LazyRow(
                    state = categoryRowState,
                    contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                ) {
                    item {
                        val isSelected = selectedSlug == "all"
                        Box(
                            modifier = Modifier
                                .clip(BeoutShapes.Medium)
                                .background(if (isSelected) BeoutColors.BrightBlue else BeoutColors.Card)
                                .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Medium)
                                .tvFocusable(shape = BeoutShapes.Medium, onEnter = { viewModel.load("all") })
                                .clickable { viewModel.load("all") }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = S.all(),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) Color.White else BeoutColors.TextSecondary
                            )
                        }
                    }

                    items(apiCategories) { cat ->
                        val isSelected = selectedSlug == cat.slug
                        Box(
                            modifier = Modifier
                                .clip(BeoutShapes.Medium)
                                .background(if (isSelected) BeoutColors.BrightBlue else BeoutColors.Card)
                                .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Medium)
                                .tvFocusable(shape = BeoutShapes.Medium, onEnter = { viewModel.load(cat.slug) })
                                .clickable { viewModel.load(cat.slug) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) Color.White else BeoutColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Personal Xtream Categories (shown for Personal or All if personal accounts exist)
            if (selectedSourceFilter != 1 && activeMovieAccounts.isNotEmpty()) {
                activeMovieAccounts.forEach { acc ->
                    val accCategories = remember(xtreamMovies, acc.id) {
                        xtreamMovies
                            .filter { it.id.contains("xtream_${acc.id}") }
                            .map { it.genre.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                    }
                    if (accCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeMovieAccounts.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = BeoutDimensions.ScreenPadding, end = 4.dp)
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.ElevatedSurface)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = acc.name,
                                        style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = BeoutColors.BrightBlue,
                                        maxLines = 1
                                    )
                                }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = if (activeMovieAccounts.size > 1) 4.dp else BeoutDimensions.ScreenPadding),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    val isSelected = selectedXtreamCategory == "all"
                                    val selectAll = {
                                        selectedXtreamCategory = "all"
                                        com.beouttv.app.data.xtream.XtreamAccountManager.initialXtreamMoviesCategory = "all"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Medium)
                                            .background(if (isSelected) BeoutColors.BrightBlue else BeoutColors.Card)
                                            .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Medium)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = selectAll)
                                            .clickable(onClick = selectAll)
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "كل ${acc.name}",
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            color = if (isSelected) Color.White else BeoutColors.TextSecondary
                                        )
                                    }
                                }
                                items(accCategories) { catName ->
                                    val isSelected = selectedXtreamCategory == catName
                                    val selectCat = {
                                        val newCat = if (isSelected) "all" else catName
                                        selectedXtreamCategory = newCat
                                        com.beouttv.app.data.xtream.XtreamAccountManager.initialXtreamMoviesCategory = newCat
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Medium)
                                            .background(if (isSelected) BeoutColors.BrightBlue else BeoutColors.Card)
                                            .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, BeoutShapes.Medium)
                                            .tvFocusable(shape = BeoutShapes.Medium, onEnter = selectCat)
                                            .clickable(onClick = selectCat)
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = catName,
                                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            color = if (isSelected) Color.White else BeoutColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        when (val s = state) {
            is MoviesUiState.Loading -> {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    BeoutPosterSkeletonRow()
                    Spacer(Modifier.height(16.dp))
                    BeoutPosterSkeletonRow()
                }
            }
            is MoviesUiState.Error -> BeoutErrorState(description = s.message, onRetry = { viewModel.load() })
            is MoviesUiState.Success -> {
                val displayedMovies = remember(s.movies, xtreamMovies, selectedSourceFilter, searchQuery, selectedXtreamCategory) {
                    val filteredXtream = if (selectedXtreamCategory == "all") {
                        xtreamMovies
                    } else {
                        xtreamMovies.filter { it.genre.trim().equals(selectedXtreamCategory.trim(), ignoreCase = true) }
                    }
                    val raw = when (selectedSourceFilter) {
                        1 -> s.movies
                        2 -> filteredXtream
                        else -> s.movies + filteredXtream
                    }
                    if (searchQuery.isBlank()) raw
                    else raw.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.originalTitle?.contains(searchQuery, ignoreCase = true) == true) ||
                        it.genre.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (displayedMovies.isNotEmpty()) {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val gridCols = when {
                        isTvMode -> 5
                        screenWidth >= 900.dp -> 6
                        screenWidth >= 720.dp -> 5
                        screenWidth >= 540.dp -> 4
                        screenWidth >= 350.dp -> 3
                        else -> 2
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridCols),
                        contentPadding = PaddingValues(
                            start = BeoutDimensions.ScreenPadding,
                            end = BeoutDimensions.ScreenPadding,
                            bottom = if (isTvMode) 32.dp else 80.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                        verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceM)
                    ) {
                        items(displayedMovies, key = { it.id }) { movie ->
                            Box {
                                BeoutMovieCard(movie = movie, onClick = { onMovieClick(movie) }, modifier = Modifier.fillMaxWidth())
                                if (movie.id.startsWith("xtream_")) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(BeoutShapes.Small)
                                            .background(BeoutColors.BrightBlue)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "خاص بك",
                                            style = BeoutTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Loading Indicator for Infinite Scrolling
                        if (s.isLoadingMore) {
                            item(span = { GridItemSpan(gridCols) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = BeoutColors.BrightBlue,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد أفلام متوفرة في هذا القسم حالياً.",
                            style = BeoutTextStyles.SecondaryInfo,
                            color = BeoutColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
