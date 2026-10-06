package com.beouttv.app.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutTextStyles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesState(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList()
) {
    val isEmpty get() = channels.isEmpty() && movies.isEmpty() && series.isEmpty()
}

class FavoritesViewModel(
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {
    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            com.beouttv.app.data.favorites.FavoritesManager.favoritesFlow.collect {
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = FavoritesState(
                channels = repository.getFavoriteChannels(),
                movies = repository.getFavoriteMovies(),
                series = repository.getFavoriteSeries()
            )
        }
    }
}

@Composable
fun FavoritesScreen(
    onExploreClick: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    onChannelClick: (Channel) -> Unit,
    viewModel: FavoritesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .statusBarsPadding()
    ) {
        Text(
            text = S.favorites(),
            style = BeoutTextStyles.ScreenTitle,
            color = BeoutColors.TextPrimary,
            modifier = Modifier.padding(BeoutDimensions.ScreenPadding)
        )

        if (state.isEmpty) {
            BeoutEmptyState(
                title = S.emptyFavorites(),
                description = tr(
                    ar = "أضف القنوات أو الأفلام المفضلة لديك للوصول إليها بنقرة واحدة بدون إنترنت.",
                    en = "Add your favorite channels or movies to access them quickly offline.",
                    tr = "Favori kanallarınızı veya filmlerinizi çevrimdışı hızlıca erişmek için ekleyin.",
                    fa = "شبکه‌ها یا فیلم‌های مورد علاقه خود را برای دسترسی سریع اضافه کنید.",
                    fr = "Ajoutez vos chaînes ou films préférés pour y accéder rapidement hors ligne."
                ),
                actionLabel = S.explore(),
                onActionClick = onExploreClick
            )
        } else {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val favGridCols = when {
                screenWidth >= 900.dp -> 6
                screenWidth >= 720.dp -> 5
                screenWidth >= 540.dp -> 4
                screenWidth >= 350.dp -> 3
                else -> 2
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(favGridCols),
                contentPadding = PaddingValues(BeoutDimensions.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS),
                verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceM)
            ) {
                items(state.movies, key = { "m_" + it.id }) { movie ->
                    BeoutMovieCard(movie = movie, onClick = { onMovieClick(movie) }, modifier = Modifier.fillMaxWidth())
                }
                items(state.series, key = { "s_" + it.id }) { series ->
                    BeoutSeriesCard(series = series, onClick = { onSeriesClick(series) }, modifier = Modifier.fillMaxWidth())
                }
                items(state.channels, key = { "c_" + it.id }) { channel ->
                    BeoutChannelCard(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                        onFavoriteClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
