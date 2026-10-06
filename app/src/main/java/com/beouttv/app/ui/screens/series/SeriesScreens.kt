package com.beouttv.app.ui.screens.series

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.Episode
import com.beouttv.app.data.model.Season
import com.beouttv.app.data.model.Series
import com.beouttv.app.data.model.StreamServer
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SeriesListUiState {
    data object Loading : SeriesListUiState
    data class Error(val message: String) : SeriesListUiState
    data class Success(
        val series: List<Series>,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : SeriesListUiState
}

class SeriesListViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SeriesListUiState>(SeriesListUiState.Loading)
    val uiState: StateFlow<SeriesListUiState> = _uiState.asStateFlow()

    private val _categoriesFlow = MutableStateFlow<List<com.beouttv.app.data.model.MediaCategory>>(emptyList())
    val categoriesFlow: StateFlow<List<com.beouttv.app.data.model.MediaCategory>> = _categoriesFlow.asStateFlow()

    private val _selectedCategorySlug = MutableStateFlow("all")
    val selectedCategorySlug: StateFlow<String> = _selectedCategorySlug.asStateFlow()

    val activeSource = com.beouttv.app.data.source.MediaSourceManager.activeSourceFlow

    private var currentPage = 1
    private var isFetchingMore = false
    private var hasMorePages = true
    private val currentSeriesList = mutableListOf<Series>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    init {
        loadCategories()
        load("all")
        viewModelScope.launch {
            repository.seriesFlow.collect { list ->
                if (list.isNotEmpty() && _selectedCategorySlug.value == "all" && currentPage == 1 && currentSeriesList.isEmpty()) {
                    currentSeriesList.addAll(list)
                    _uiState.value = SeriesListUiState.Success(
                        series = currentSeriesList.toList(),
                        isLoadingMore = false,
                        hasMore = true
                    )
                }
            }
        }

        // When source switches, reload
        viewModelScope.launch {
            com.beouttv.app.data.source.MediaSourceManager.sourceSwitchEvent.collect {
                loadCategories()
                load(_selectedCategorySlug.value, isReload = true)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val cats = repository.getCategories().filter { it.isSeries }
            _categoriesFlow.value = cats
        }
    }

    fun load(categorySlug: String = _selectedCategorySlug.value, isReload: Boolean = false) {
        if (_selectedCategorySlug.value != categorySlug || isReload) {
            currentPage = 1
            hasMorePages = true
            isFetchingMore = false
            currentSeriesList.clear()
        }
        _selectedCategorySlug.value = categorySlug

        viewModelScope.launch {
            _uiState.value = SeriesListUiState.Loading
            try {
                val list = repository.getSeriesPaged(categorySlug, page = 1, perPage = 30)
                currentSeriesList.clear()
                currentSeriesList.addAll(list)
                hasMorePages = list.size >= 15
                _uiState.value = SeriesListUiState.Success(
                    series = currentSeriesList.toList(),
                    isLoadingMore = false,
                    hasMore = hasMorePages
                )
            } catch (e: Exception) {
                _uiState.value = SeriesListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMore() {
        if (isFetchingMore || !hasMorePages) return
        val currentState = _uiState.value
        if (currentState !is SeriesListUiState.Success) return

        isFetchingMore = true
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val nextItems = repository.getSeriesPaged(_selectedCategorySlug.value, page = nextPage, perPage = 30)
                if (nextItems.isNotEmpty()) {
                    val existingIds = currentSeriesList.map { it.id }.toSet()
                    val newUniqueItems = nextItems.filter { !existingIds.contains(it.id) }
                    currentSeriesList.addAll(newUniqueItems)
                    currentPage = nextPage
                    hasMorePages = nextItems.size >= 15
                } else {
                    hasMorePages = false
                }
                _uiState.value = SeriesListUiState.Success(
                    series = currentSeriesList.toList(),
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
fun SeriesListScreen(
    onBack: () -> Unit,
    onSeriesClick: (Series) -> Unit,
    viewModel: SeriesListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val apiCategories by viewModel.categoriesFlow.collectAsState()
    val selectedSlug by viewModel.selectedCategorySlug.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val xtreamSeries by com.beouttv.app.data.xtream.XtreamAccountManager.xtreamSeries.collectAsState()
    val xtreamAccounts by com.beouttv.app.data.xtream.XtreamAccountManager.accounts.collectAsState()
    val activeSeriesAccounts = remember(xtreamAccounts) { xtreamAccounts.filter { it.isActive && it.showSeries } }
    var selectedSourceFilter by rememberSaveable { mutableIntStateOf(com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter) } // 0: All, 1: App Library, 2: Personal Xtream
    var selectedXtreamCategory by rememberSaveable { mutableStateOf(com.beouttv.app.data.xtream.XtreamAccountManager.initialXtreamSeriesCategory) }

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    // Detect reaching end of grid for infinite scroll pagination
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = (state as? SeriesListUiState.Success)?.series?.size ?: 0
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            val s = state
            if (s is SeriesListUiState.Success && s.hasMore && !s.isLoadingMore) {
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
                Text(tr("المسلسلات", "Series", "Diziler", "سریال‌ها", "Séries"), style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp), color = BeoutColors.TextPrimary)
                BeoutSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = tr("ابحث في مكتبة المسلسلات...", "Search series library...", "Dizilerde ara...", "جستجو در سریال‌ها...", "Rechercher dans les séries..."),
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
                Text(tr("المسلسلات", "Series", "Diziler", "سریال‌ها", "Séries"), style = BeoutTextStyles.ScreenTitle, color = BeoutColors.TextPrimary)
            }

            // In-page Search Bar
            BeoutSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = tr("ابحث في مكتبة المسلسلات...", "Search series library...", "Dizilerde ara...", "جستجو در سریال‌ها...", "Rechercher dans les séries..."),
                modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS)
            )
            Spacer(Modifier.height(BeoutDimensions.SpaceS))
        }

        // Switcher Bar: All vs BeoutTV vs Personal Xtream
        if (xtreamSeries.isNotEmpty()) {
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
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 0
                        }
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                            selectedSourceFilter = 0
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 0
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
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 1
                        }
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                            selectedSourceFilter = 1
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 1
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
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 2
                        }
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                            selectedSourceFilter = 2
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 2
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
                            Text("${xtreamSeries.size}", style = BeoutTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = if (selectedSourceFilter == 2) Color.White else BeoutColors.BrightBlue)
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
            if (selectedSourceFilter != 1 && activeSeriesAccounts.isNotEmpty()) {
                activeSeriesAccounts.forEach { acc ->
                    val accCategories = remember(xtreamSeries, acc.id) {
                        xtreamSeries
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
                            if (activeSeriesAccounts.size > 1) {
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
                                contentPadding = PaddingValues(horizontal = if (activeSeriesAccounts.size > 1) 4.dp else BeoutDimensions.ScreenPadding),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    val isSelected = selectedXtreamCategory == "all"
                                    val selectAll = {
                                        selectedXtreamCategory = "all"
                                        com.beouttv.app.data.xtream.XtreamAccountManager.initialXtreamSeriesCategory = "all"
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
                                        com.beouttv.app.data.xtream.XtreamAccountManager.initialXtreamSeriesCategory = newCat
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
            is SeriesListUiState.Loading -> {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    BeoutPosterSkeletonRow()
                    Spacer(Modifier.height(16.dp))
                    BeoutPosterSkeletonRow()
                }
            }
            is SeriesListUiState.Error -> BeoutErrorState(onRetry = { viewModel.load() })
            is SeriesListUiState.Success -> {
                val displayedSeries = remember(s.series, xtreamSeries, selectedSourceFilter, searchQuery, selectedXtreamCategory) {
                    val filteredXtream = if (selectedXtreamCategory == "all") {
                        xtreamSeries
                    } else {
                        xtreamSeries.filter { it.genre.trim().equals(selectedXtreamCategory.trim(), ignoreCase = true) }
                    }
                    val raw = when (selectedSourceFilter) {
                        1 -> s.series
                        2 -> filteredXtream
                        else -> s.series + filteredXtream
                    }
                    if (searchQuery.isBlank()) raw
                    else raw.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.originalTitle?.contains(searchQuery, ignoreCase = true) == true) ||
                        it.genre.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (displayedSeries.isNotEmpty()) {
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
                        items(displayedSeries, key = { it.id }) { series ->
                            Box {
                                BeoutSeriesCard(series = series, onClick = { onSeriesClick(series) }, modifier = Modifier.fillMaxWidth())
                                if (series.id.startsWith("xtream_")) {
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
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(gridCols) }) {
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
                            text = "لا توجد مسلسلات متوفرة في هذا القسم حالياً.",
                            style = BeoutTextStyles.SecondaryInfo,
                            color = BeoutColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Series Details ----------------

sealed interface SeriesDetailsUiState {
    data object Loading : SeriesDetailsUiState
    data class Error(val message: String) : SeriesDetailsUiState
    data class Success(
        val series: Series,
        val similarSeries: List<Series> = emptyList()
    ) : SeriesDetailsUiState
}

class SeriesDetailsViewModel(
    private val seriesId: String,
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SeriesDetailsUiState>(SeriesDetailsUiState.Loading)
    val uiState: StateFlow<SeriesDetailsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load(id: String = seriesId) {
        viewModelScope.launch {
            _uiState.value = SeriesDetailsUiState.Loading
            try {
                val series = repository.getSeriesById(id)
                if (series != null) {
                    val allSeries = if (id.startsWith("xtream_")) {
                        com.beouttv.app.data.xtream.XtreamAccountManager.xtreamSeries.value
                    } else {
                        repository.getSeries()
                    }
                    val similar = allSeries.filter { it.id != series.id && (it.genre == series.genre || it.rating >= 7.5f) }.take(8)
                    _uiState.value = SeriesDetailsUiState.Success(series, similar)
                } else {
                    _uiState.value = SeriesDetailsUiState.Error(tr("المسلسل غير متوفر", "Series not found", "Dizi bulunamadı", "سریال یافت نشد", "Série introuvable"))
                }
            } catch (e: Exception) {
                _uiState.value = SeriesDetailsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleFavorite() {
        val current = (_uiState.value as? SeriesDetailsUiState.Success)?.series ?: return
        val newFav = !current.isFavorite
        val updated = current.copy(isFavorite = newFav)
        val similar = (_uiState.value as? SeriesDetailsUiState.Success)?.similarSeries.orEmpty()
        _uiState.value = SeriesDetailsUiState.Success(updated, similar)

        viewModelScope.launch {
            repository.toggleFavoriteSeries(current.id)
        }
    }
}

@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    onBack: () -> Unit,
    onEpisodeClick: (Episode) -> Unit
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SeriesDetailsViewModel>(
        key = seriesId,
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                SeriesDetailsViewModel(seriesId) as T
        }
    )
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.load(seriesId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
    ) {
        when (val s = state) {
            is SeriesDetailsUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
                    BeoutHeroSkeleton(modifier = Modifier.padding(BeoutDimensions.ScreenPadding))
                    Spacer(Modifier.height(BeoutDimensions.SpaceM))
                    BeoutPosterSkeletonRow()
                }
            }
            is SeriesDetailsUiState.Error -> {
                BeoutErrorState(
                    description = s.message,
                    onBack = onBack,
                    onRetry = viewModel::load
                )
            }
            is SeriesDetailsUiState.Success -> {
                SeriesDetailsContent(
                    series = s.series,
                    similarSeries = s.similarSeries,
                    isTvMode = isTvMode,
                    onBack = onBack,
                    onEpisodeClick = onEpisodeClick,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onTrailerClick = { ytId ->
                        val cleanTitle = s.series.originalTitle?.ifBlank { null } ?: s.series.title
                        val url = if (!ytId.isNullOrBlank()) {
                            "https://www.youtube.com/watch?v=$ytId"
                        } else {
                            val query = java.net.URLEncoder.encode("$cleanTitle official trailer", "UTF-8")
                            "https://www.youtube.com/results?search_query=$query"
                        }
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                setPackage("com.google.android.youtube")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            context.startActivity(fallbackIntent)
                        }
                    },
                    onShareClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, s.series.title)
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "شاهد مسلسل ${s.series.title} (${s.series.year}) بكافة المواسم والحلقات على تطبيق BEOUT TV"
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة المسلسل"))
                    },
                    onNavigateToSeries = { selected ->
                        viewModel.load(selected.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun SeriesDetailsContent(
    series: Series,
    similarSeries: List<Series>,
    isTvMode: Boolean = false,
    onBack: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onToggleFavorite: () -> Unit,
    onTrailerClick: (String?) -> Unit,
    onShareClick: () -> Unit,
    onNavigateToSeries: (Series) -> Unit
) {
    val context = LocalContext.current
    var selectedSeasonNumber by remember { mutableStateOf(series.seasons.firstOrNull()?.seasonNumber ?: 1) }
    var selectedServerId by remember { mutableStateOf<String?>(null) }
    var isStoryExpanded by remember { mutableStateOf(false) }
    var isSeasonDropdownOpen by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    val allDownloads by com.beouttv.app.data.repository.MovieBoxDownloadManager.downloads.collectAsState()
    val hasDownloadedEpisodes = remember(allDownloads, series) {
        allDownloads.any { dl ->
            dl.status == com.beouttv.app.data.repository.DownloadStatus.COMPLETED &&
            series.seasons.any { s -> s.episodes.any { ep -> ep.id == dl.mediaId } }
        }
    }

    val onDownloadSeriesClick = {
        showDownloadDialog = true
    }

    val currentSeason = series.seasons.find { it.seasonNumber == selectedSeasonNumber }
        ?: series.seasons.firstOrNull()
        ?: Season(1, emptyList())

    val firstEpisode = remember(currentSeason) {
        currentSeason.episodes.firstOrNull() ?: series.seasons.firstOrNull()?.episodes?.firstOrNull()
    }
    val isXtream = remember(series) {
        series.id.startsWith("xtream_") ||
        (series.seasons.firstOrNull()?.episodes?.firstOrNull()?.streamUrl?.contains("/series/") == true)
    }

    if (isTvMode) {
        // 10-Foot Smart TV Leanback Experience for Series
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Navigation & Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingCircleButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = S.back(),
                        onClick = onBack,
                        modifier = Modifier.tvFocusable(shape = CircleShape)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)) {
                        FloatingCircleButton(
                            icon = if (hasDownloadedEpisodes) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = tr("تحميل الحلقات", "Download Episodes", "Bölümleri İndir", "دانلود قسمت‌ها", "Télécharger les épisodes"),
                            tint = if (hasDownloadedEpisodes) Color(0xFF4CAF50) else Color.White,
                            onClick = onDownloadSeriesClick,
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        )

                        FloatingCircleButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = tr("مشاركة", "Share", "Paylaş", "اشتراک‌گذاری", "Partager"),
                            onClick = onShareClick,
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        )

                        val favTint by androidx.compose.animation.animateColorAsState(
                            targetValue = if (series.isFavorite) BeoutColors.LiveRed else BeoutColors.TextPrimary,
                            label = "favColor"
                        )
                        FloatingCircleButton(
                            icon = if (series.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = S.favorites(),
                            tint = favTint,
                            onClick = onToggleFavorite,
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        )
                    }
                }
            }

            // Cinematic 2-Column Showcase
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Left Column: Poster + Primary Actions
                    Column(modifier = Modifier.width(220.dp)) {
                        AsyncImage(
                            model = series.posterUrl,
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(BeoutShapes.Medium)
                                .border(1.5.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Medium)
                                .background(BeoutColors.Card)
                        )

                        Spacer(Modifier.height(14.dp))

                        firstEpisode?.let { ep ->
                            val playEp1 = {
                                val targetUrl = (if (!selectedServerId.isNullOrBlank()) {
                                    ep.servers.find { it.id == selectedServerId }?.url
                                } else null) ?: ep.streamUrl.takeIf { it.isNotBlank() } ?: ep.servers.firstOrNull()?.url.orEmpty()
                                onEpisodeClick(ep.copy(streamUrl = targetUrl))
                            }
                            Button(
                                onClick = playEp1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .tvFocusable(shape = BeoutShapes.Medium, onEnter = playEp1),
                                shape = BeoutShapes.Medium,
                                colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.PrimaryBlue)
                            ) {
                                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tr("مشاهدة الحلقة الأولى", "Watch Episode 1", "1. Bölümü İzle", "تماشای قسمت اول", "Regarder l'épisode 1"),
                                    style = BeoutTextStyles.Button.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (!isXtream) {
                            val openTrailer = { onTrailerClick(series.trailerYoutubeId) }
                            OutlinedButton(
                                onClick = openTrailer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .tvFocusable(shape = BeoutShapes.Medium, onEnter = openTrailer),
                                shape = BeoutShapes.Medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = BeoutColors.ElevatedSurface,
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BeoutColors.Divider)
                            ) {
                                Icon(imageVector = Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = BeoutColors.BrightBlue)
                                Spacer(Modifier.width(BeoutDimensions.SpaceXS))
                                Text(
                                    text = tr("الإعلان الرسمي", "Trailer", "Fragman", "تریلر رسمی", "Bande-annonce"),
                                    style = BeoutTextStyles.SecondaryInfo.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                                )
                            }
                        }
                    }

                    // Right Column: Title + Badges + Story + Season Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = series.title,
                            style = BeoutTextStyles.ScreenTitle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )

                        if (!series.originalTitle.isNullOrBlank() && series.originalTitle != series.title) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = series.originalTitle!!,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp, color = BeoutColors.BrightBlue)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (series.rating > 0f) {
                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.ElevatedSurface)
                                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.35f), BeoutShapes.Small)
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = BeoutColors.BrightBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "${series.rating}/10",
                                            style = BeoutTextStyles.Caption.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BeoutColors.BrightBlue
                                            )
                                        )
                                    }
                                }
                            }

                            MetaBadge(text = "${series.year}")
                            val seasonsCount = series.seasons.size.coerceAtLeast(1)
                            MetaBadge(text = "$seasonsCount ${tr("مواسم", "Seasons", "Sezon", "فصل", "Saisons")}")
                            MetaBadge(text = "1080p FHD", isHighlight = true)
                            MetaBadge(text = series.genre)
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = tr("قصة المسلسل", "Synopsis", "Dizi Konusu", "داستان سریال", "Synopsis"),
                            style = BeoutTextStyles.SectionTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = series.description.ifBlank { tr("لا يوجد وصف متاح لهذا المسلسل حالياً.", "No description available.", "Bu dizi için açıklama bulunmuyor.", "توضیحاتی برای این سریال در دسترس نیست.", "Aucune description disponible.") },
                            style = BeoutTextStyles.BodyRegular.copy(fontSize = 13.sp, lineHeight = 20.sp),
                            color = BeoutColors.TextSecondary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(16.dp))

                        // Season Selection Pills for TV
                        Text(
                            text = tr("اختر الموسم", "Select Season", "Sezon Seç", "انتخاب فصل", "Sélectionner la saison"),
                            style = BeoutTextStyles.SectionTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(series.seasons) { season ->
                                val isSelected = season.seasonNumber == selectedSeasonNumber
                                val bg = if (isSelected) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface
                                val border = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider
                                val textCol = if (isSelected) Color.White else BeoutColors.TextSecondary

                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Medium)
                                        .background(bg)
                                        .border(1.dp, border, BeoutShapes.Medium)
                                        .tvFocusable(
                                            shape = BeoutShapes.Medium,
                                            onEnter = { selectedSeasonNumber = season.seasonNumber }
                                        )
                                        .clickable { selectedSeasonNumber = season.seasonNumber }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "${tr("الموسم", "Season", "Sezon", "فصل", "Saison")} ${season.seasonNumber} (${season.episodes.size})",
                                        style = BeoutTextStyles.SecondaryInfo.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = textCol
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Episodes Section for TV (Horizontal shelf with TV focusable cards)
            item {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    BeoutSectionHeader(
                        title = "${tr("حلقات الموسم", "Season Episodes", "Sezon Bölümleri", "قسمت‌های فصل", "Épisodes de la saison")} $selectedSeasonNumber (${currentSeason.episodes.size} ${tr("حلقة", "Episodes", "Bölüm", "قسمت", "Épisodes")})"
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(currentSeason.episodes, key = { it.id }) { ep ->
                            TvEpisodeItemCard(
                                episode = ep,
                                seriesPoster = series.backdropUrl.ifBlank { series.posterUrl },
                                onClick = {
                                    val targetUrl = if (!selectedServerId.isNullOrBlank()) {
                                        ep.servers.find { it.id == selectedServerId }?.url ?: ep.streamUrl
                                    } else ep.streamUrl
                                    onEpisodeClick(ep.copy(streamUrl = targetUrl))
                                }
                            )
                        }
                    }
                }
            }

            // Similar Series Section for TV
            if (similarSeries.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        BeoutSectionHeader(
                            title = tr("مسلسلات مشابهة ومقترحة", "Recommended Series", "Önerilen Diziler", "سریال‌های مشابه", "Séries recommandées")
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                        ) {
                            items(similarSeries, key = { it.id }) { item ->
                                BeoutSeriesCard(
                                    series = item,
                                    onClick = { onNavigateToSeries(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                AsyncImage(
                    model = series.backdropUrl.ifBlank { series.posterUrl },
                    contentDescription = series.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BeoutColors.ElevatedSurface)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    BeoutColors.Background.copy(alpha = 0.85f),
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    BeoutColors.Background.copy(alpha = 0.7f),
                                    BeoutColors.Background
                                )
                            )
                        )
                )

                firstEpisode?.let { ep ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(BeoutColors.BrightBlue.copy(alpha = 0.85f))
                            .clickable {
                                val targetUrl = (if (!selectedServerId.isNullOrBlank()) {
                                    ep.servers.find { it.id == selectedServerId }?.url
                                } else null) ?: ep.streamUrl.takeIf { it.isNotBlank() } ?: ep.servers.firstOrNull()?.url.orEmpty()
                                onEpisodeClick(ep.copy(streamUrl = targetUrl))
                            }
                            .padding(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "مشاهدة المسلسل",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingCircleButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = S.back(),
                        onClick = onBack
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)) {
                        FloatingCircleButton(
                            icon = if (hasDownloadedEpisodes) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = tr("تحميل الحلقات", "Download Episodes", "Bölümleri İndir", "دانلود قسمت‌ها", "Télécharger les épisodes"),
                            tint = if (hasDownloadedEpisodes) Color(0xFF4CAF50) else Color.White,
                            onClick = onDownloadSeriesClick
                        )

                        FloatingCircleButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = tr("مشاركة", "Share", "Paylaş", "اشتراک‌گذاری", "Partager"),
                            onClick = onShareClick
                        )

                        val favTint by androidx.compose.animation.animateColorAsState(
                            targetValue = if (series.isFavorite) BeoutColors.LiveRed else BeoutColors.TextPrimary,
                            label = "favColor"
                        )
                        FloatingCircleButton(
                            icon = if (series.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = S.favorites(),
                            tint = favTint,
                            onClick = onToggleFavorite
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding)
                    .offset(y = (-32).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = series.posterUrl,
                        contentDescription = series.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(118.dp)
                            .aspectRatio(2f / 3f)
                            .clip(BeoutShapes.Medium)
                            .border(1.5.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), BeoutShapes.Medium)
                            .background(BeoutColors.Card)
                    )

                    Spacer(Modifier.width(BeoutDimensions.SpaceM))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = series.title,
                            style = BeoutTextStyles.ScreenTitle.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = BeoutColors.TextPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        if (!series.originalTitle.isNullOrBlank() && series.originalTitle != series.title) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = series.originalTitle!!,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, color = BeoutColors.BrightBlue),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (series.rating > 0f) {
                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.ElevatedSurface)
                                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.35f), BeoutShapes.Small)
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = BeoutColors.BrightBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "${series.rating}/10",
                                            style = BeoutTextStyles.Caption.copy(
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = BeoutColors.BrightBlue
                                            )
                                        )
                                    }
                                }
                            }

                            MetaBadge(text = "${series.year}")
                            val seasonsCount = series.seasons.size.coerceAtLeast(1)
                            MetaBadge(text = "$seasonsCount ${tr("مواسم", "Seasons", "Sezon", "فصل", "Saisons")}")
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetaBadge(text = "1080p FHD", isHighlight = true)
                            MetaBadge(text = series.genre)
                        }
                    }
                }

                Spacer(Modifier.height(BeoutDimensions.SpaceM))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                ) {
                    firstEpisode?.let { ep ->
                        BeoutPrimaryButton(
                            text = tr("مشاهدة الحلقة الأولى", "Watch Episode 1", "1. Bölümü İzle", "تماشای قسمت اول", "Regarder l'épisode 1"),
                            leadingIcon = Icons.Rounded.PlayArrow,
                            onClick = {
                                val targetUrl = if (!selectedServerId.isNullOrBlank()) {
                                    ep.servers.find { it.id == selectedServerId }?.url ?: ep.streamUrl
                                } else ep.streamUrl
                                onEpisodeClick(ep.copy(streamUrl = targetUrl))
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    if (!isXtream) {
                        OutlinedButton(
                            onClick = { onTrailerClick(series.trailerYoutubeId) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = BeoutShapes.Medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BeoutColors.ElevatedSurface,
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(BeoutColors.BrightBlue, BeoutColors.PrimaryBlue))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = BeoutColors.BrightBlue
                            )
                            Spacer(Modifier.width(BeoutDimensions.SpaceXS))
                            Text(
                                text = tr("الإعلان الرسمي", "Trailer", "Fragman", "تریلر رسمی", "Bande-annonce"),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(BeoutDimensions.SpaceL))

                if (series.description.isNotBlank()) {
                    Column {
                        BeoutSectionHeader(
                            title = tr("قصة المسلسل", "Synopsis", "Dizi Konusu", "داستان سریال", "Synopsis")
                        )
                        Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                        Text(
                            text = series.description,
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp, lineHeight = 20.sp),
                            color = BeoutColors.TextSecondary,
                            maxLines = if (isStoryExpanded) 20 else 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (series.description.length > 120) {
                            Text(
                                text = if (isStoryExpanded) tr("عرض أقل", "Show less", "Daha az", "کمتر", "Moins") else tr("قراءة المزيد...", "Read more...", "Daha fazla...", "بیشتر...", "Lire la suite..."),
                                style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = BeoutColors.BrightBlue,
                                modifier = Modifier
                                    .clickable { isStoryExpanded = !isStoryExpanded }
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(BeoutDimensions.SpaceL))
                }

                BeoutSectionHeader(
                    title = tr("المواسم والحلقات", "Seasons & Episodes", "Sezonlar ve Bölümler", "فصل‌ها و قسمت‌ها", "Saisons et Épisodes")
                )
                Spacer(Modifier.height(BeoutDimensions.SpaceS))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.ElevatedSurface)
                        .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                        .clickable { isSeasonDropdownOpen = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${tr("الموسم", "Season", "Sezon", "فصل", "Saison")} $selectedSeasonNumber (${currentSeason.episodes.size} ${tr("حلقة", "Episodes", "Bölüm", "قسمت", "Épisodes")})",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = BeoutColors.TextPrimary
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = BeoutColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = isSeasonDropdownOpen,
                        onDismissRequest = { isSeasonDropdownOpen = false },
                        modifier = Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFF161F2C))
                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                    ) {
                        series.seasons.forEach { season ->
                            val isSelected = season.seasonNumber == selectedSeasonNumber
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${tr("الموسم", "Season", "Sezon", "فصل", "Saison")} ${season.seasonNumber} (${season.episodes.size} ${tr("حلقة", "Episodes", "Bölüm", "قسمت", "Épisodes")})",
                                            style = BeoutTextStyles.CardTitle.copy(
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            ),
                                            color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextPrimary
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = BeoutColors.BrightBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedSeasonNumber = season.seasonNumber
                                    isSeasonDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(BeoutDimensions.SpaceM))
            }
        }

        items(currentSeason.episodes, key = { it.id }) { ep ->
            Box(modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding, vertical = 5.dp)) {
                EpisodeItemCard(
                    episode = ep,
                    seriesPoster = series.backdropUrl.ifBlank { series.posterUrl },
                    onClick = {
                        val targetUrl = (if (!selectedServerId.isNullOrBlank()) {
                            ep.servers.find { it.id == selectedServerId }?.url
                        } else null) ?: ep.streamUrl.takeIf { it.isNotBlank() } ?: ep.servers.firstOrNull()?.url.orEmpty()
                        onEpisodeClick(ep.copy(streamUrl = targetUrl))
                    }
                )
            }
        }

        if (similarSeries.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = BeoutDimensions.SpaceL)) {
                    BeoutSectionHeader(
                        title = tr("مسلسلات مشابهة ومقترحة", "Recommended Series", "Önerilen Diziler", "سریال‌های مشابه", "Séries recommandées"),
                        modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
                    )
                    Spacer(Modifier.height(BeoutDimensions.SpaceS))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                    ) {
                        items(similarSeries, key = { it.id }) { item ->
                            BeoutSeriesCard(
                                series = item,
                                onClick = { onNavigateToSeries(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (showDownloadDialog) {
            AlertDialog(
                onDismissRequest = { showDownloadDialog = false },
                containerColor = BeoutColors.Card,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = tr("تحميل حلقات المسلسل", "Download Episodes", "Bölümleri İndir", "دانلود قسمت‌ها", "Télécharger les épisodes"),
                            style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (series.seasons.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(series.seasons) { season ->
                                    val isSelected = season.seasonNumber == selectedSeasonNumber
                                    val bg = if (isSelected) BeoutColors.BrightBlue else BeoutColors.ElevatedSurface
                                    val border = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider
                                    val textCol = if (isSelected) Color.White else BeoutColors.TextSecondary

                                    Box(
                                        modifier = Modifier
                                            .clip(BeoutShapes.Medium)
                                            .background(bg)
                                            .border(1.dp, border, BeoutShapes.Medium)
                                            .clickable { selectedSeasonNumber = season.seasonNumber }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${tr("الموسم", "Season", "Sezon", "فصل", "Saison")} ${season.seasonNumber}",
                                            style = BeoutTextStyles.SecondaryInfo.copy(
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = textCol
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${tr("الموسم", "Season", "Sezon", "فصل", "Saison")} $selectedSeasonNumber • ${currentSeason.episodes.size} ${tr("حلقة", "episodes", "bölüm", "قسمت", "épisodes")}",
                            style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp),
                            color = BeoutColors.TextSecondary
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentSeason.episodes, key = { it.id }) { ep ->
                                val isEpDownloaded = allDownloads.any { it.mediaId == ep.id && it.status == com.beouttv.app.data.repository.DownloadStatus.COMPLETED }
                                val isEpDownloading = allDownloads.any { it.mediaId == ep.id && (it.status == com.beouttv.app.data.repository.DownloadStatus.DOWNLOADING || it.status == com.beouttv.app.data.repository.DownloadStatus.QUEUED) }

                                val downloadEpAction = {
                                    if (isEpDownloaded) {
                                        onEpisodeClick(ep)
                                        showDownloadDialog = false
                                    } else {
                                        val stream = ep.servers.firstOrNull()?.url?.takeIf { it.isNotBlank() } ?: ep.streamUrl
                                        if (stream.isNotBlank()) {
                                            com.beouttv.app.data.repository.MovieBoxDownloadManager.startDownload(
                                                context = context,
                                                mediaId = ep.id,
                                                title = ep.title,
                                                videoUrl = stream,
                                                posterUrl = ep.thumbnailUrl.ifBlank { series.posterUrl },
                                                quality = "1080p",
                                                subtitleUrl = ep.subtitles.firstOrNull()?.url,
                                                mediaType = "series"
                                            )
                                            android.widget.Toast.makeText(context, "بدأ تحميل الحلقة في الخلفية", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "رابط التحميل غير متوفر حالياً", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.ElevatedSurface)
                                        .border(1.dp, if (isEpDownloaded) Color(0xFF4CAF50).copy(alpha = 0.5f) else BeoutColors.Divider, BeoutShapes.Small)
                                        .clickable(onClick = downloadEpAction)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tr("الحلقة", "Episode", "Bölüm", "قسمت", "Épisode")} ${ep.episodeNumber}: ${ep.title.removePrefix("Episode").trim()}",
                                            style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (ep.durationMinutes > 0) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${ep.durationMinutes} ${tr("دقيقة", "min", "dk", "دقیقه", "min")}",
                                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                                color = BeoutColors.TextSecondary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = downloadEpAction,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isEpDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                            contentDescription = null,
                                            tint = when {
                                                isEpDownloaded -> Color(0xFF4CAF50)
                                                isEpDownloading -> BeoutColors.BrightBlue
                                                else -> Color.White
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDownloadDialog = false }) {
                        Text(
                            text = tr("إغلاق", "Close", "Kapat", "بستن", "Fermer"),
                            color = BeoutColors.BrightBlue,
                            style = BeoutTextStyles.ButtonLabel
                        )
                    }
                }
            )
        }
    }

@Composable
private fun EpisodeItemCard(
    episode: Episode,
    seriesPoster: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
            .tvFocusable(shape = BeoutShapes.Medium)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .aspectRatio(16f / 9f)
                .clip(BeoutShapes.Small)
                .background(BeoutColors.ElevatedSurface)
        ) {
            AsyncImage(
                model = episode.thumbnailUrl.ifBlank { seriesPoster },
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
            )

            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(BeoutColors.BrightBlue.copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "E${episode.episodeNumber}",
                    style = BeoutTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${tr("الحلقة", "Episode", "Bölüm", "قسمت", "Épisode")} ${episode.episodeNumber}: ${episode.title.removePrefix("Episode").trim()}",
                style = BeoutTextStyles.CardTitle.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = BeoutColors.TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${episode.durationMinutes} ${tr("دقيقة", "min", "dk", "دقیقه", "min")} • 1080p FHD",
                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                color = BeoutColors.TextSecondary
            )

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = null,
                    tint = BeoutColors.BrightBlue,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = tr("مترجم بالعربية", "Arabic Subtitles", "Türkçe Altyazılı", "زیرنویس فارسی", "Sous-titré"),
                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                    color = BeoutColors.BrightBlue
                )
            }
        }

        val context = LocalContext.current
        val isEpDownloaded = remember(episode.id) {
            com.beouttv.app.data.repository.MovieBoxDownloadManager.isDownloaded(episode.id)
        }

        IconButton(
            onClick = {
                if (isEpDownloaded) {
                    onClick()
                } else {
                    val stream = episode.servers.firstOrNull()?.url?.takeIf { it.isNotBlank() } ?: episode.streamUrl
                    if (stream.isNotBlank()) {
                        com.beouttv.app.data.repository.MovieBoxDownloadManager.startDownload(
                            context = context,
                            mediaId = episode.id,
                            title = "${episode.title}",
                            videoUrl = stream,
                            posterUrl = episode.thumbnailUrl.ifBlank { seriesPoster },
                            quality = "1080p",
                            subtitleUrl = episode.subtitles.firstOrNull()?.url,
                            mediaType = "series"
                        )
                        android.widget.Toast.makeText(context, "بدأ تحميل الحلقة في الخلفية", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Icon(
                imageVector = if (isEpDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                contentDescription = "تحميل الحلقة",
                tint = if (isEpDownloaded) Color(0xFF4CAF50) else BeoutColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = BeoutColors.BrightBlue,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 4.dp)
        )
    }
}

@Composable
private fun TvEpisodeItemCard(
    episode: Episode,
    seriesPoster: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
            .tvFocusable(shape = BeoutShapes.Medium, onEnter = onClick)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(BeoutColors.ElevatedSurface)
        ) {
            AsyncImage(
                model = episode.thumbnailUrl.ifBlank { seriesPoster },
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(BeoutColors.BrightBlue.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "E${episode.episodeNumber}",
                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "${tr("الحلقة", "Episode", "Bölüm", "قسمت", "Épisode")} ${episode.episodeNumber}: ${episode.title.removePrefix("Episode").trim()}",
                style = BeoutTextStyles.CardTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = BeoutColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${episode.durationMinutes} ${tr("دقيقة", "min", "dk", "دقیقه", "min")} • 1080p FHD",
                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 11.sp),
                color = BeoutColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MetaBadge(
    text: String,
    isHighlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(BeoutShapes.Small)
            .background(if (isHighlight) BeoutColors.BrightBlue.copy(alpha = 0.15f) else BeoutColors.ElevatedSurface)
            .border(1.dp, if (isHighlight) BeoutColors.BrightBlue.copy(alpha = 0.4f) else BeoutColors.Divider, BeoutShapes.Small)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = BeoutTextStyles.Caption.copy(
                fontSize = 11.sp,
                fontWeight = if (isHighlight) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                color = if (isHighlight) BeoutColors.BrightBlue else BeoutColors.TextSecondary
            )
        )
    }
}
