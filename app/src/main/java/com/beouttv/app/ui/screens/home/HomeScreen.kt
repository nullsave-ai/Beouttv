@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.beouttv.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.*
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable

@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onPlayMovieDirect: (Movie) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onSeriesClick: (Series) -> Unit,
    onSeeAllChannels: () -> Unit,
    onSeeAllMovies: () -> Unit,
    onSeeAllSeries: () -> Unit,
    onSeeAllAsianMovies: (String) -> Unit = {},
    onSeeAllTrending: () -> Unit = {},
    onSeeAllAnime: () -> Unit = {},
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
            .then(if (!isTvMode) Modifier.statusBarsPadding() else Modifier.padding(top = 10.dp))
    ) {
        if (!isTvMode) {
            BeoutTopBar(
                title = "BEOUT TV",
                onMenuClick = onMenuClick,
                onNotificationsClick = onNotificationsClick,
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick
            )
        }

        when (val s = state) {
            is HomeUiState.Loading -> HomeLoading()
            is HomeUiState.Error -> BeoutErrorState(
                description = s.message,
                onRetry = { viewModel.load() }
            )
            is HomeUiState.Success -> HomeContent(
                state = s,
                isTvMode = isTvMode,
                onChannelClick = onChannelClick,
                onMovieClick = onMovieClick,
                onPlayMovieDirect = onPlayMovieDirect,
                onContinueWatchingClick = onContinueWatchingClick,
                onSeriesClick = onSeriesClick,
                onSeeAllChannels = onSeeAllChannels,
                onSeeAllMovies = onSeeAllMovies,
                onSeeAllSeries = onSeeAllSeries,
                onSeeAllAsianMovies = onSeeAllAsianMovies,
                onSeeAllTrending = onSeeAllTrending,
                onSeeAllAnime = onSeeAllAnime,
                onToggleChannelFavorite = viewModel::toggleFavoriteChannel
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceL),
        contentPadding = PaddingValues(vertical = BeoutDimensions.SpaceM)
    ) {
        item { BeoutHeroSkeleton(modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)) }
        item { BeoutChannelSkeletonRow() }
        item { BeoutPosterSkeletonRow() }
        item { BeoutPosterSkeletonRow() }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    isTvMode: Boolean,
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onPlayMovieDirect: (Movie) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onSeriesClick: (Series) -> Unit,
    onSeeAllChannels: () -> Unit,
    onSeeAllMovies: () -> Unit,
    onSeeAllSeries: () -> Unit,
    onSeeAllAsianMovies: (String) -> Unit,
    onSeeAllTrending: () -> Unit,
    onSeeAllAnime: () -> Unit,
    onToggleChannelFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLang by com.beouttv.app.data.i18n.LanguageManager.currentLanguage.collectAsState()
    val shelfStartPadding = if (isTvMode) 84.dp else BeoutDimensions.ScreenPadding

    // High-performance memoized sublists for fast 60/120fps scrolling without garbage collection churn
    val displayedMovies = remember(state.movies) { state.movies.take(24) }
    val asianMovies = remember(state.asianMovies) { state.asianMovies.take(24) }
    val displayedSeries = remember(state.series) { state.series.take(24) }
    val displayedChannels = remember(state.liveChannels) { state.liveChannels.take(28) }
    val displayedXtreamMovies = remember(state.xtreamMovies) { state.xtreamMovies.take(24) }
    val displayedXtreamSeries = remember(state.xtreamSeries) { state.xtreamSeries.take(24) }
    val mbTrending = remember(state.movieBoxTrending) { state.movieBoxTrending.take(24) }
    val mbAnime = remember(state.movieBoxAnime) { state.movieBoxAnime.take(24) }

    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(if (isTvMode) 20.dp else BeoutDimensions.SpaceL),
        contentPadding = PaddingValues(bottom = if (isTvMode) 32.dp else 90.dp)
    ) {
        // 0. Stories / Trailers Shelf (iOS Mobile Experience)
        if (!isTvMode && state.stories.isNotEmpty()) {
            item(key = "stories_section", contentType = "stories_shelf") {
                BeoutStoriesRow(
                    stories = state.stories,
                    onStoryClick = { activeStoryIndex = it }
                )
            }
        }

        // 1. Featured Showcase Banner (Manual swipeable, no auto jump)
        if (state.featuredMovies.isNotEmpty()) {
            item(key = "hero_carousel_section", contentType = "hero_carousel") {
                HeroCarousel(
                    featuredMovies = state.featuredMovies,
                    isTvMode = isTvMode,
                    onPlayClick = onPlayMovieDirect,
                    onDetailsClick = onMovieClick
                )
            }
        }

        // 2. Real Continue Watching Section
        if (state.continueWatching.isNotEmpty()) {
            item(key = "continue_watching_section", contentType = "continue_watching_shelf") {
                Column {
                    BeoutSectionHeader(title = tr("متابعة المشاهدة", "Continue Watching", "İzlemeye Devam Et", "ادامه تماشا", "Reprendre la lecture"))
                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                    ) {
                        items(state.continueWatching, key = { "cw_" + it.id }, contentType = { "continue_watching_card" }) { item ->
                            BeoutContinueWatchingCard(
                                title = item.title,
                                thumbnailUrl = item.thumbnailUrl,
                                progressPercent = item.progressPercent,
                                remainingLabel = item.remainingLabel,
                                onClick = { onContinueWatchingClick(item) }
                            )
                        }
                    }
                }
            }
        }

        // Adsterra 320x50 Banner (Zone ID 31138665)
        item(key = "home_adsterra_banner_section", contentType = "ad_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AdsterraBanner()
            }
        }

        // 3. Live TV Channels Section
        if (state.liveChannels.isNotEmpty()) {
            item(key = "live_channels_section", contentType = "live_channels_shelf") {
                Column {
                    BeoutSectionHeader(title = S.liveTv(), onSeeAllClick = onSeeAllChannels)
                    if (isTvMode) {
                        // Synchronized double-row shelf for Smart TV (صفين متوازيين يتحركان معاً أفقياً)
                        val (row1Channels, row2Channels) = remember(state.liveChannels) {
                            val entChannels = state.liveChannels.filter {
                                it.category.contains("ألوان", ignoreCase = true) ||
                                it.category.contains("ترفيه", ignoreCase = true) ||
                                it.category.contains("شاهد", ignoreCase = true) ||
                                it.category.contains("alwan", ignoreCase = true) ||
                                it.category.contains("drama", ignoreCase = true) ||
                                it.category.contains("cinema", ignoreCase = true) ||
                                it.category.contains("movies", ignoreCase = true) ||
                                it.category.contains("mbc", ignoreCase = true)
                            }.ifEmpty {
                                state.liveChannels.drop(state.liveChannels.size / 2)
                            }
                            val entIds = entChannels.map { it.id }.toSet()
                            val mainChannels = state.liveChannels.filter { !entIds.contains(it.id) }.ifEmpty { state.liveChannels }
                            Pair(mainChannels, entChannels)
                        }
                        val shelfCount = maxOf(row1Channels.size, row2Channels.size)

                        LazyRow(
                            contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(shelfCount, key = { "tv_shelf_$it" }, contentType = { "tv_double_channel" }) { idx ->
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val topChannel = row1Channels.getOrNull(idx % row1Channels.size)
                                    if (topChannel != null) {
                                        BeoutChannelCard(
                                            channel = topChannel,
                                            onClick = { onChannelClick(topChannel) },
                                            onFavoriteClick = { onToggleChannelFavorite(topChannel.id) },
                                            modifier = Modifier
                                                .width(BeoutDimensions.TvChannelCardWidth)
                                                .tvFocusable(shape = BeoutShapes.Medium),
                                            isTvMode = isTvMode
                                        )
                                    }
                                    val bottomChannel = row2Channels.getOrNull(idx % row2Channels.size)
                                    if (bottomChannel != null) {
                                        BeoutChannelCard(
                                            channel = bottomChannel,
                                            onClick = { onChannelClick(bottomChannel) },
                                            onFavoriteClick = { onToggleChannelFavorite(bottomChannel.id) },
                                            modifier = Modifier
                                                .width(BeoutDimensions.TvChannelCardWidth)
                                                .tvFocusable(shape = BeoutShapes.Medium),
                                            isTvMode = isTvMode
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                        ) {
                            items(displayedChannels, key = { "ch_" + it.id }, contentType = { "channel_card" }) { channel ->
                                BeoutChannelCard(
                                    channel = channel,
                                    onClick = { onChannelClick(channel) },
                                    onFavoriteClick = { onToggleChannelFavorite(channel.id) },
                                    isTvMode = isTvMode
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Personal Xtream Movies Row (Placed first above Latest Cinema as requested)
        if (displayedXtreamMovies.isNotEmpty()) {
            item(key = "xtream_movies_section", contentType = "xtream_movies_shelf") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "أفلام من اشتراكك الشخصي",
                                style = BeoutTextStyles.SectionTitle,
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                    .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), BeoutShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "خاص بك",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }

                        val openXtreamMovies = {
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 2
                            onSeeAllMovies()
                        }
                        Text(
                            text = S.seeAll(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            modifier = Modifier
                                .clickable(onClick = openXtreamMovies)
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = openXtreamMovies)
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                    ) {
                        items(displayedXtreamMovies, key = { "home_xtream_mov_" + it.id }, contentType = { "movie_card" }) { movie ->
                            BeoutMovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                isTvMode = isTvMode,
                                language = currentLang
                            )
                        }
                    }
                }
            }
        }

        // 5. Movies Library Section (MovieBox Latest Cinema & Movies)
        if (displayedMovies.isNotEmpty()) {
            item(key = "movies_section", contentType = "movies_shelf") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = S.latestCinema(),
                                style = BeoutTextStyles.SectionTitle,
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.ElevatedSurface)
                                    .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ONE+",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }

                        Text(
                            text = S.seeAll(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            modifier = Modifier
                                .clickable(onClick = {
                                    com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 0
                                    onSeeAllMovies()
                                })
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = {
                                    com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 0
                                    onSeeAllMovies()
                                })
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                    ) {
                        items(displayedMovies, key = { "home_mov_" + it.id }, contentType = { "movie_card" }) { movie ->
                            BeoutMovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                isTvMode = isTvMode,
                                language = currentLang
                            )
                        }
                    }
                }
            }

            // Shelf 2: Asian & Korean Cinema (أفلام آسيوية وكورية • MovieBox)
            if (asianMovies.isNotEmpty()) {
                item(key = "asian_movies_section", contentType = "asian_movies_shelf") {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tr("أفلام آسيوية وكورية", "Asian & Korean Movies", "Asya ve Kore Filmleri", "فیلم‌های آسیایی و کره‌ای", "Films asiatiques et coréens"),
                                    style = BeoutTextStyles.SectionTitle,
                                    color = BeoutColors.TextPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(BeoutShapes.Small)
                                        .background(BeoutColors.ElevatedSurface)
                                        .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ONE+",
                                        style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = BeoutColors.BrightBlue
                                    )
                                }
                            }

                            Text(
                                text = S.seeAll(),
                                style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = BeoutColors.BrightBlue,
                                modifier = Modifier
                                    .clickable(onClick = { onSeeAllAsianMovies(state.asianCategorySlug) })
                                    .tvFocusable(shape = BeoutShapes.Small, onEnter = { onSeeAllAsianMovies(state.asianCategorySlug) })
                                    .padding(4.dp)
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                        ) {
                            items(asianMovies, key = { "asian_" + it.id }, contentType = { "movie_card" }) { movie ->
                                BeoutMovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie) },
                                    modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                    isTvMode = isTvMode,
                                    language = currentLang
                                )
                            }
                        }
                    }
                }
            }
        }

        // MovieBox Global Trending Section
        if (mbTrending.isNotEmpty()) {
            item(key = "moviebox_trending_section", contentType = "moviebox_trending_shelf") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "الأكثر رواجاً عالمياً",
                                style = BeoutTextStyles.SectionTitle,
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.ElevatedSurface)
                                    .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ONE+",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }

                        Text(
                            text = S.seeAll(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            modifier = Modifier
                                .clickable(onClick = onSeeAllTrending)
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = onSeeAllTrending)
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                    ) {
                        items(mbTrending, key = { "mb_trend_" + it.id }, contentType = { "movie_card" }) { movie ->
                            BeoutMovieCard(
                                movie = movie,
                                onClick = {
                                    if (movie.isSeries) {
                                        val s = Series(
                                            id = movie.id,
                                            title = movie.title,
                                            originalTitle = movie.originalTitle,
                                            posterUrl = movie.posterUrl,
                                            backdropUrl = movie.backdropUrl,
                                            year = movie.year,
                                            rating = movie.rating,
                                            genre = movie.genre,
                                            description = movie.description
                                        )
                                        onSeriesClick(s)
                                    } else {
                                        onMovieClick(movie)
                                    }
                                },
                                modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                isTvMode = isTvMode,
                                language = currentLang
                            )
                        }
                    }
                }
            }
        }

        // MovieBox Anime World Section
        if (mbAnime.isNotEmpty()) {
            item(key = "moviebox_anime_section", contentType = "moviebox_anime_shelf") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "عالم الأنمي والمانجا",
                                style = BeoutTextStyles.SectionTitle,
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.ElevatedSurface)
                                    .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ONE+ Anime",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }

                        Text(
                            text = S.seeAll(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            modifier = Modifier
                                .clickable(onClick = onSeeAllAnime)
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = onSeeAllAnime)
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                    ) {
                        items(mbAnime, key = { "mb_anime_" + it.id }, contentType = { "movie_card" }) { movie ->
                            BeoutMovieCard(
                                movie = movie,
                                onClick = {
                                    if (movie.isSeries) {
                                        val s = Series(
                                            id = movie.id,
                                            title = movie.title,
                                            originalTitle = movie.originalTitle,
                                            posterUrl = movie.posterUrl,
                                            backdropUrl = movie.backdropUrl,
                                            year = movie.year,
                                            rating = movie.rating,
                                            genre = movie.genre,
                                            description = movie.description
                                        )
                                        onSeriesClick(s)
                                    } else {
                                        onMovieClick(movie)
                                    }
                                },
                                modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                isTvMode = isTvMode,
                                language = currentLang
                            )
                        }
                    }
                }
            }
        }


        // 6. Personal Xtream Series Row (Placed above Series Section as requested)
        if (displayedXtreamSeries.isNotEmpty()) {
            item(key = "xtream_series_section", contentType = "xtream_series_shelf") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مسلسلات من اشتراكك الشخصي",
                                style = BeoutTextStyles.SectionTitle,
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                                    .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), BeoutShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "خاص بك",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }

                        val openXtreamSeries = {
                            com.beouttv.app.data.xtream.XtreamAccountManager.initialSeriesSourceFilter = 2
                            onSeeAllSeries()
                        }
                        Text(
                            text = S.seeAll(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            modifier = Modifier
                                .clickable(onClick = openXtreamSeries)
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = openXtreamSeries)
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                    ) {
                        items(displayedXtreamSeries, key = { "home_xtream_ser_" + it.id }, contentType = { "series_card" }) { series ->
                            BeoutSeriesCard(
                                series = series,
                                onClick = { onSeriesClick(series) },
                                modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                isTvMode = isTvMode
                            )
                        }
                    }
                }
            }
        }

        // 7. Series Section (MovieBox Global Series)
        if (displayedSeries.isNotEmpty()) {
            item(key = "series_section", contentType = "series_shelf") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding, top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tr("مسلسلات عالمية", "Global Series", "Dünya Dizileri", "سریال‌های جهانی", "Séries mondiales"),
                                style = BeoutTextStyles.SectionTitle,
                                color = BeoutColors.TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(BeoutShapes.Small)
                                    .background(BeoutColors.ElevatedSurface)
                                    .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ONE+",
                                    style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = BeoutColors.BrightBlue
                                )
                            }
                        }

                        Text(
                            text = S.seeAll(),
                            style = BeoutTextStyles.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.BrightBlue,
                            modifier = Modifier
                                .clickable(onClick = onSeeAllSeries)
                                .tvFocusable(shape = BeoutShapes.Small, onEnter = onSeeAllSeries)
                                .padding(4.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(start = shelfStartPadding, end = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else BeoutDimensions.SpaceS)
                    ) {
                        items(displayedSeries, key = { "home_ser_" + it.id }, contentType = { "series_card" }) { series ->
                            BeoutSeriesCard(
                                series = series,
                                onClick = { onSeriesClick(series) },
                                modifier = if (isTvMode) Modifier.tvFocusable(shape = BeoutShapes.Medium) else Modifier,
                                isTvMode = isTvMode
                            )
                        }
                    }
                }
            }
        }
    }

    activeStoryIndex?.let { storyIdx ->
        BeoutStoryViewerModal(
            stories = state.stories,
            initialIndex = storyIdx,
            onDismiss = { activeStoryIndex = null },
            onWatchDetails = { story ->
                activeStoryIndex = null
                val cleanId = story.subjectId
                if (story.subjectType == 2) {
                    val seriesObj = Series(
                        id = "mb_$cleanId",
                        title = story.title,
                        originalTitle = story.title,
                        posterUrl = story.coverUrl,
                        backdropUrl = story.coverUrl,
                        year = 2024,
                        rating = story.score.toFloatOrNull() ?: 7.5f,
                        genre = story.genre.ifBlank { "مسلسلات" },
                        description = "مشاهدة وتحميل ${story.title} بجودة عالية على BeoutTV.",
                        seasons = emptyList()
                    )
                    com.beouttv.app.data.repository.ServiceLocator.repository.cacheDiscoveredSeries(seriesObj)
                    onSeriesClick(seriesObj)
                } else {
                    val movieObj = Movie(
                        id = "mb_$cleanId",
                        title = story.title,
                        originalTitle = story.title,
                        posterUrl = story.coverUrl,
                        backdropUrl = story.coverUrl,
                        year = 2024,
                        rating = story.score.toFloatOrNull() ?: 7.5f,
                        quality = "1080p FHD",
                        duration = "115 دقيقة",
                        genre = story.genre.ifBlank { "سينما عالمية" },
                        category = "أفلام وان+",
                        description = "مشاهدة وتحميل ${story.title} بجودة عالية على BeoutTV.",
                        streamUrl = story.videoUrl,
                        isSeries = false
                    )
                    com.beouttv.app.data.repository.ServiceLocator.repository.cacheDiscoveredMovie(movieObj)
                    onMovieClick(movieObj)
                }
            }
        )
    }
}

/**
 * Featured Showcase Banner for top movies.
 * Swipeable by user without auto-jump, responsive layout.
 */
@Composable
private fun HeroCarousel(
    featuredMovies: List<Movie>,
    isTvMode: Boolean,
    onPlayClick: (Movie) -> Unit,
    onDetailsClick: (Movie) -> Unit
) {
    if (featuredMovies.isEmpty()) return

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val bannerHeight = when {
        isLandscape && screenHeight < 500.dp -> 150.dp
        isTvMode && screenHeight >= 500.dp -> 210.dp
        isTvMode -> 155.dp
        isLandscape -> 150.dp
        else -> 180.dp
    }

    val bannerStartPadding = if (isTvMode) 84.dp else BeoutDimensions.ScreenPadding
    val pagerState = rememberPagerState(pageCount = { featuredMovies.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = bannerStartPadding, end = BeoutDimensions.ScreenPadding)
            .height(bannerHeight)
            .clip(BeoutShapes.Large)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Large)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> featuredMovies.getOrNull(page)?.id ?: page },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movie = featuredMovies.getOrNull(page) ?: return@HorizontalPager
            HeroSlide(
                movie = movie,
                isTvMode = isTvMode,
                onPlayClick = { onPlayClick(movie) },
                onDetailsClick = { onDetailsClick(movie) }
            )
        }

        // Top End: TV Remote hints (if in TV mode) & Pager Dots
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTvMode) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ريموت: الأسهم للتنقل • OK للاختيار",
                        color = BeoutColors.BrightBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(featuredMovies.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(if (isSelected) 14.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(
    movie: Movie,
    isTvMode: Boolean = false,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop Image
        AsyncImage(
            model = movie.backdropUrl.ifBlank { movie.posterUrl },
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(BeoutColors.ElevatedSurface)
        )

        // Dark Cinematic Fade Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BeoutColors.Background.copy(alpha = 0.5f),
                            BeoutColors.Background.copy(alpha = 0.95f)
                        ),
                        startY = 40f
                    )
                )
        )

        // Bottom Info & Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(if (isTvMode) 16.dp else 12.dp)
        ) {
            // Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // IMDb Rating Pill (Only shown if real rating exists)
                if (movie.rating > 0f) {
                    Box(
                        modifier = Modifier
                            .clip(BeoutShapes.Small)
                            .background(BeoutColors.ElevatedSurface)
                            .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "IMDb ${String.format(java.util.Locale.US, "%.1f", movie.rating)}",
                                style = BeoutTextStyles.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = BeoutColors.BrightBlue)
                            )
                        }
                    }
                }

                // Year & Genre Badges
                Text(
                    text = "${movie.year} • ${movie.genre.take(20)}",
                    style = BeoutTextStyles.Caption.copy(fontSize = 10.5.sp),
                    color = BeoutColors.BrightBlue
                )
            }

            Spacer(Modifier.height(3.dp))

            // Title
            Text(
                text = movie.title,
                style = BeoutTextStyles.ScreenTitle.copy(fontSize = if (isTvMode) 17.sp else 14.5.sp, fontWeight = FontWeight.Bold),
                color = BeoutColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Story
            if (movie.description.isNotBlank()) {
                Text(
                    text = movie.description,
                    style = BeoutTextStyles.SecondaryInfo.copy(fontSize = if (isTvMode) 11.5.sp else 10.5.sp),
                    color = BeoutColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(if (isTvMode) 8.dp else 6.dp))

            // Buttons Row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayClick,
                    shape = BeoutShapes.Small,
                    colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = if (isTvMode) 16.dp else 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(if (isTvMode) 36.dp else 30.dp)
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = onPlayClick)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTvMode) 18.dp else 15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(S.play(), style = BeoutTextStyles.Button.copy(fontSize = if (isTvMode) 12.5.sp else 11.sp, fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onDetailsClick,
                    shape = BeoutShapes.Small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BeoutColors.TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BeoutColors.Divider),
                    contentPadding = PaddingValues(horizontal = if (isTvMode) 16.dp else 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(if (isTvMode) 36.dp else 30.dp)
                        .tvFocusable(shape = BeoutShapes.Small, onEnter = onDetailsClick)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(if (isTvMode) 16.dp else 13.dp),
                        tint = BeoutColors.TextSecondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(S.details(), style = BeoutTextStyles.Button.copy(fontSize = if (isTvMode) 12.5.sp else 11.sp))
                }
            }
        }
    }
}
