package com.beouttv.app.ui.screens.movies

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.imdb.ImdbMetadataEngine
import com.beouttv.app.data.model.CastMember
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.repository.BeoutRepository
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.ui.components.*
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MovieDetailsUiState {
    data object Loading : MovieDetailsUiState
    data class Error(val message: String) : MovieDetailsUiState
    data class Success(
        val movie: Movie,
        val similarMovies: List<Movie> = emptyList()
    ) : MovieDetailsUiState
}

class MovieDetailsViewModel(
    private val movieId: String,
    private val repository: BeoutRepository = ServiceLocator.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieDetailsUiState>(MovieDetailsUiState.Loading)
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()

    init {
        load(movieId)
        viewModelScope.launch {
            com.beouttv.app.data.favorites.FavoritesManager.favoritesFlow.collect {
                val current = _uiState.value
                if (current is MovieDetailsUiState.Success) {
                    val m = current.movie
                    val ctx = ServiceLocator.getAppContext()
                    if (ctx != null) {
                        val isFav = com.beouttv.app.data.favorites.FavoritesManager.isMovieFavorite(ctx, m.id)
                        if (m.isFavorite != isFav) {
                            _uiState.value = current.copy(movie = m.copy(isFavorite = isFav))
                        }
                    }
                }
            }
        }
    }

    fun load(targetId: String = movieId) {
        viewModelScope.launch {
            _uiState.value = MovieDetailsUiState.Loading
            try {
                val movie = repository.getMovieById(targetId)
                if (movie != null) {
                    val lang = com.beouttv.app.data.i18n.LanguageManager.currentLanguage.value
                    val initialLocalized = ImdbMetadataEngine.localizeMovie(movie, lang)
                    val allMovies = if (targetId.startsWith("xtream_")) {
                        com.beouttv.app.data.xtream.XtreamAccountManager.xtreamMovies.value
                    } else {
                        repository.getMovies()
                    }
                    val primaryGenre = movie.genre.split("•", "،", ",").firstOrNull()?.trim() ?: ""
                    val rawSimilar = allMovies
                        .filter { it.id != movie.id }
                        .sortedWith(
                            compareByDescending<Movie> { it.genre.contains(primaryGenre) }
                                .thenByDescending { it.rating }
                        )
                        .take(12)

                    _uiState.value = MovieDetailsUiState.Success(initialLocalized, rawSimilar)

                    // Asynchronously enrich both main movie and all similar movies in parallel with persistent disk cache
                    launch(Dispatchers.IO) {
                        val enrichedMovieDeferred = async { ImdbMetadataEngine.enrichMovie(movie, lang) }
                        val enrichedSimilarDeferred = rawSimilar.map { sim ->
                            async { ImdbMetadataEngine.enrichMovie(sim, lang) }
                        }

                        val enrichedMovie = enrichedMovieDeferred.await()
                        val enrichedSimilar = enrichedSimilarDeferred.awaitAll()

                        val current = _uiState.value
                        if (current is MovieDetailsUiState.Success && current.movie.id == movie.id) {
                            val curFav = current.movie.isFavorite
                            val preservedMovie = enrichedMovie.copy(
                                isFavorite = curFav,
                                streamUrl = if (movie.streamUrl.isNotBlank()) movie.streamUrl else enrichedMovie.streamUrl,
                                servers = if (movie.servers.isNotEmpty()) movie.servers else enrichedMovie.servers,
                                subtitles = if (movie.subtitles.isNotEmpty()) movie.subtitles else enrichedMovie.subtitles
                            )
                            _uiState.value = MovieDetailsUiState.Success(
                                preservedMovie,
                                enrichedSimilar
                            )
                        }
                    }
                } else {
                    _uiState.value = MovieDetailsUiState.Error("لم يتم العثور على بيانات الفيلم")
                }
            } catch (e: Exception) {
                _uiState.value = MovieDetailsUiState.Error(e.message ?: "حدث خطأ أثناء تحميل البيانات")
            }
        }
    }

    fun toggleFavorite() {
        val current = (_uiState.value as? MovieDetailsUiState.Success)?.movie ?: return
        val newFavState = !current.isFavorite
        val optimistic = current.copy(isFavorite = newFavState)
        val similar = (_uiState.value as? MovieDetailsUiState.Success)?.similarMovies.orEmpty()
        _uiState.value = MovieDetailsUiState.Success(optimistic, similar)

        viewModelScope.launch {
            repository.toggleFavoriteMovie(current.id)
        }
    }
}

@Composable
fun MovieDetailsScreen(
    movieId: String,
    onBack: () -> Unit,
    onPlayClick: (Movie) -> Unit,
    onNavigateToMovie: ((Movie) -> Unit)? = null
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<MovieDetailsViewModel>(
        key = movieId,
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                MovieDetailsViewModel(movieId) as T
        }
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()

    LaunchedEffect(movieId) {
        viewModel.load(movieId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeoutColors.Background)
    ) {
        when (val s = state) {
            is MovieDetailsUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
                    BeoutHeroSkeleton(modifier = Modifier.padding(BeoutDimensions.ScreenPadding))
                    Spacer(Modifier.height(BeoutDimensions.SpaceM))
                    BeoutPosterSkeletonRow()
                }
            }
            is MovieDetailsUiState.Error -> {
                BeoutErrorState(
                    description = s.message,
                    onBack = onBack,
                    onRetry = viewModel::load
                )
            }
            is MovieDetailsUiState.Success -> {
                MovieDetailsContent(
                    movie = s.movie,
                    similarMovies = s.similarMovies,
                    isTvMode = isTvMode,
                    onBack = onBack,
                    onPlayClick = { onPlayClick(s.movie) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onTrailerClick = { ytId ->
                        val url = if (!ytId.isNullOrBlank()) {
                            "https://www.youtube.com/watch?v=$ytId"
                        } else {
                            val cleanTitle = s.movie.originalTitle?.ifBlank { null } ?: s.movie.title
                            val query = java.net.URLEncoder.encode("$cleanTitle official trailer", "UTF-8")
                            "https://www.youtube.com/results?search_query=$query"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                setPackage("com.google.android.youtube")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(fallbackIntent)
                        }
                    },
                    onShareClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, s.movie.title)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "شاهد فيلم ${s.movie.title} (${s.movie.year}) بتقييم IMDb ${s.movie.rating} على تطبيق BEOUT TV Cinema"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيلم"))
                    },
                    onSimilarMovieClick = { selected ->
                        if (onNavigateToMovie != null) {
                            onNavigateToMovie(selected)
                        } else {
                            viewModel.load(selected.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MovieDetailsContent(
    movie: Movie,
    similarMovies: List<Movie>,
    isTvMode: Boolean = false,
    onBack: () -> Unit,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTrailerClick: (String?) -> Unit,
    onShareClick: () -> Unit,
    onSimilarMovieClick: (Movie) -> Unit
) {
    val context = LocalContext.current
    val currentLang by com.beouttv.app.data.i18n.LanguageManager.currentLanguage.collectAsState()
    val localizedMovie = remember(movie, currentLang) {
        com.beouttv.app.data.imdb.ImdbMetadataEngine.localizeMovie(movie, currentLang)
    }
    val localizedSimilar = remember(similarMovies, currentLang) {
        similarMovies.map { com.beouttv.app.data.imdb.ImdbMetadataEngine.localizeMovie(it, currentLang) }
    }
    val isXtream = remember(movie) { movie.id.startsWith("xtream_") || movie.streamUrl.contains("/movie/") }

    var isStoryExpanded by remember { mutableStateOf(false) }

    val allDownloads by com.beouttv.app.data.repository.MovieBoxDownloadManager.downloads.collectAsState()
    val isDownloaded = remember(allDownloads, localizedMovie.id) {
        allDownloads.any { it.mediaId == localizedMovie.id && it.status == com.beouttv.app.data.repository.DownloadStatus.COMPLETED }
    }
    val isDownloading = remember(allDownloads, localizedMovie.id) {
        allDownloads.any { it.mediaId == localizedMovie.id && (it.status == com.beouttv.app.data.repository.DownloadStatus.DOWNLOADING || it.status == com.beouttv.app.data.repository.DownloadStatus.QUEUED) }
    }

    val onDownloadClick = {
        if (isDownloaded) {
            onPlayClick()
        } else {
            val bestStream = localizedMovie.servers.firstOrNull()?.url?.takeIf { it.isNotBlank() } ?: localizedMovie.streamUrl
            if (bestStream.isNotBlank()) {
                com.beouttv.app.data.repository.MovieBoxDownloadManager.startDownload(
                    context = context,
                    mediaId = localizedMovie.id,
                    title = localizedMovie.title,
                    videoUrl = bestStream,
                    posterUrl = localizedMovie.posterUrl,
                    quality = localizedMovie.quality,
                    subtitleUrl = localizedMovie.subtitles.firstOrNull()?.url,
                    mediaType = "movie"
                )
                android.widget.Toast.makeText(context, "بدأ تحميل الفيلم في الخلفية", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "رابط التحميل غير متوفر حالياً", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (isTvMode) {
        // 10-Foot Smart TV Leanback Experience
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Navigation & Action Row
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
                            icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = if (isDownloaded) tr("محمّل", "Downloaded", "İndirildi", "دانلود شد", "Téléchargé") else tr("تحميل", "Download", "İndir", "دانلود", "Télécharger"),
                            tint = if (isDownloaded) Color(0xFF4CAF50) else if (isDownloading) BeoutColors.BrightBlue else Color.White,
                            onClick = onDownloadClick,
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        )

                        FloatingCircleButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = tr("مشاركة", "Share", "Paylaş", "اشتراک‌گذاری", "Partager"),
                            onClick = onShareClick,
                            modifier = Modifier.tvFocusable(shape = CircleShape)
                        )

                        val favTint by animateColorAsState(
                            targetValue = if (localizedMovie.isFavorite) BeoutColors.LiveRed else BeoutColors.TextPrimary,
                            label = "favColor"
                        )
                        FloatingCircleButton(
                            icon = if (localizedMovie.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
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
                    // Left Column: Poster + Primary Action Buttons
                    Column(modifier = Modifier.width(220.dp)) {
                        AsyncImage(
                            model = localizedMovie.posterUrl,
                            contentDescription = localizedMovie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(BeoutShapes.Medium)
                                .border(1.5.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), BeoutShapes.Medium)
                                .background(BeoutColors.Card)
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .tvFocusable(shape = BeoutShapes.Medium, onEnter = onPlayClick),
                            shape = BeoutShapes.Medium,
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.PrimaryBlue)
                        ) {
                            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = S.play(), style = BeoutTextStyles.Button.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.weight(1f).height(40.dp).tvFocusable(shape = BeoutShapes.Medium, onEnter = onToggleFavorite),
                                shape = BeoutShapes.Medium,
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = BeoutColors.ElevatedSurface, contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BeoutColors.Divider)
                            ) {
                                Icon(
                                    imageVector = if (localizedMovie.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (localizedMovie.isFavorite) BeoutColors.LiveRed else Color.White
                                )
                            }

                            if (!isXtream) {
                                val trailerAction = { onTrailerClick(localizedMovie.trailerYoutubeId) }
                                OutlinedButton(
                                    onClick = trailerAction,
                                    modifier = Modifier.weight(1f).height(40.dp).tvFocusable(shape = BeoutShapes.Medium, onEnter = trailerAction),
                                    shape = BeoutShapes.Medium,
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = BeoutColors.ElevatedSurface, contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BeoutColors.Divider)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = BeoutColors.BrightBlue)
                                }
                            }
                        }
                    }

                    // Right Column: Title, Metadata Badges, Synopsis, Specs
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localizedMovie.title,
                            style = BeoutTextStyles.ScreenTitle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )

                        if (!localizedMovie.originalTitle.isNullOrBlank() && localizedMovie.originalTitle != localizedMovie.title) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = localizedMovie.originalTitle!!,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp, color = BeoutColors.BrightBlue)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingBadge(rating = localizedMovie.rating)
                            MetaBadge(text = "${localizedMovie.year}")
                            MetaBadge(text = if (localizedMovie.quality.isNotBlank()) localizedMovie.quality.take(8) else "1080p FHD", isPrimary = true)
                            MetaBadge(text = formatFullDuration(localizedMovie.duration, currentLang))
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = tr("قصة الفيلم", "Story Synopsis", "Film Özeti", "داستان فیلم", "Synopsis"),
                            style = BeoutTextStyles.SectionTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = localizedMovie.description.ifBlank { tr("لا يوجد وصف متاح لهذا الفيلم حالياً.", "No description available.", "Bu film için açıklama bulunmuyor.", "توضیحاتی برای این فیلم در دسترس نیست.", "Aucune description disponible.") },
                            style = BeoutTextStyles.BodyRegular.copy(fontSize = 13.sp, lineHeight = 20.sp),
                            color = BeoutColors.TextSecondary
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoCard(
                                title = tr("التصنيف", "Genre", "Tür", "ژانر", "Genre"),
                                value = localizedMovie.genre.ifBlank { "عام" },
                                icon = Icons.Rounded.Category,
                                modifier = Modifier.weight(1f)
                            )
                            InfoCard(
                                title = tr("الجودة", "Quality", "Kalite", "کیفیت", "Qualité"),
                                value = if (localizedMovie.quality.isNotBlank()) localizedMovie.quality else "1080p FHD",
                                icon = Icons.Rounded.HighQuality,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Similar Movies Section for TV
            if (localizedSimilar.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = tr("أفلام مقترحة ومشابهة", "Similar & Recommended Movies", "Benzer Filmler", "فیلم‌های مشابه و پیشنهادی", "Films similaires"),
                            style = BeoutTextStyles.SectionTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(localizedSimilar, key = { it.id }) { sim ->
                                BeoutMovieCard(
                                    movie = sim,
                                    onClick = { onSimilarMovieClick(sim) },
                                    modifier = Modifier.width(160.dp).tvFocusable(shape = BeoutShapes.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Cinematic Hero Header with Backdrops & Action Overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Backdrop Image
                AsyncImage(
                    model = localizedMovie.backdropUrl.ifBlank { localizedMovie.posterUrl },
                    contentDescription = localizedMovie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BeoutColors.ElevatedSurface)
                )

                // Top & Bottom Cinematic Fade Gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BeoutColors.Background.copy(alpha = 0.85f),
                                    Color.Transparent,
                                    BeoutColors.Background.copy(alpha = 0.7f),
                                    BeoutColors.Background
                                )
                            )
                        )
                )

                // Floating Center Play Pulse
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(BeoutColors.PrimaryBlue.copy(alpha = 0.85f))
                        .clickable(onClick = onPlayClick)
                        .padding(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "مشاهدة الفيلم",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Top Floating Bar (Back, Share, Favorite)
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
                            icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = if (isDownloaded) tr("محمّل", "Downloaded", "İndirildi", "دانلود شد", "Téléchargé") else tr("تحميل", "Download", "İndir", "دانلود", "Télécharger"),
                            tint = if (isDownloaded) Color(0xFF4CAF50) else if (isDownloading) BeoutColors.BrightBlue else Color.White,
                            onClick = onDownloadClick
                        )

                        FloatingCircleButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = tr("مشاركة", "Share", "Paylaş", "اشتراک‌گذاری", "Partager"),
                            onClick = onShareClick
                        )

                        val favTint by animateColorAsState(
                            targetValue = if (localizedMovie.isFavorite) BeoutColors.LiveRed else BeoutColors.TextPrimary,
                            label = "favColor"
                        )
                        FloatingCircleButton(
                            icon = if (localizedMovie.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = S.favorites(),
                            tint = favTint,
                            onClick = onToggleFavorite
                        )
                    }
                }
            }
        }

        // 2. Movie Poster & Primary Title + Spec Badges
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
                    // High-Res Poster Card
                    AsyncImage(
                        model = localizedMovie.posterUrl,
                        contentDescription = localizedMovie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(118.dp)
                            .aspectRatio(2f / 3f)
                            .clip(BeoutShapes.Medium)
                            .border(1.5.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), BeoutShapes.Medium)
                            .background(BeoutColors.Card)
                    )

                    Spacer(Modifier.width(BeoutDimensions.SpaceM))

                    // Title & Authentic Meta Badges
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localizedMovie.title,
                            style = BeoutTextStyles.ScreenTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = BeoutColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!localizedMovie.originalTitle.isNullOrBlank() && localizedMovie.originalTitle != localizedMovie.title) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = localizedMovie.originalTitle!!,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, color = BeoutColors.BrightBlue),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(BeoutDimensions.SpaceS))

                        // Real IMDb Rating & Year & Duration Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceXS)
                        ) {
                            // IMDb Badge
                            RatingBadge(rating = localizedMovie.rating)

                            // Real Year
                            MetaBadge(text = "${localizedMovie.year}")

                            // Quality Badge
                            MetaBadge(
                                text = if (localizedMovie.quality.isNotBlank()) localizedMovie.quality.take(8) else "1080p FHD",
                                isPrimary = true
                            )
                        }

                        Spacer(Modifier.height(BeoutDimensions.SpaceXS))

                        // Duration Badge: e.g. "1 س 40 د (100 دقيقة)"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = BeoutColors.TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatFullDuration(localizedMovie.duration, currentLang),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, color = BeoutColors.TextSecondary)
                            )
                        }
                    }
                }
            }
        }

        // 3. Primary Action Buttons (Play Now, Trailer, Favorites)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding)
                    .offset(y = (-16).dp)
            ) {
                // Big Play Button
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = BeoutShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(BeoutDimensions.SpaceS))
                    Text(
                        text = S.play(),
                        style = BeoutTextStyles.Button.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(BeoutDimensions.SpaceS))

                // Secondary Action Row: "في قائمتي" + "الإعلان الترويجي"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                ) {
                    // 1. Favorites / My List Button
                    OutlinedButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = BeoutShapes.Medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BeoutColors.ElevatedSurface,
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(BeoutColors.Divider, BeoutColors.Divider))
                        )
                    ) {
                        Icon(
                            imageVector = if (localizedMovie.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (localizedMovie.isFavorite) BeoutColors.LiveRed else Color.White
                        )
                        Spacer(Modifier.width(BeoutDimensions.SpaceXS))
                        Text(
                            text = tr("في قائمتي", "In My List", "Listemde", "در لیست من", "Dans ma liste"),
                            style = BeoutTextStyles.SecondaryInfo.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                        )
                    }

                    // 2. Trailer Button (hidden for personal Xtream)
                    if (!isXtream) {
                        OutlinedButton(
                            onClick = { onTrailerClick(localizedMovie.trailerYoutubeId) },
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
                                text = tr("الإعلان", "Trailer", "Fragman", "پیش‌نمایش", "Bande-annonce"),
                                style = BeoutTextStyles.SecondaryInfo.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                            )
                        }
                    }
                }
            }
        }

        // 4. Genres & Categories Chips
        item {
            val genres = if (localizedMovie.genresList.isNotEmpty()) localizedMovie.genresList
            else localizedMovie.genre.split("•", ",", "/").map { it.trim() }.filter { it.isNotBlank() }

            if (genres.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceXS),
                    modifier = Modifier.padding(bottom = BeoutDimensions.SpaceM)
                ) {
                    items(genres) { genre ->
                        Box(
                            modifier = Modifier
                                .clip(BeoutShapes.Large)
                                .background(BeoutColors.Card)
                                .border(1.dp, BeoutColors.Divider, BeoutShapes.Large)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = BeoutColors.BrightBlue
                            )
                        }
                    }
                }
            }
        }

        // 5. Synopsis & Story Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceXS)
            ) {
                SectionHeading(title = S.story(), icon = Icons.Rounded.Movie)
                Spacer(Modifier.height(BeoutDimensions.SpaceXS))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BeoutShapes.Medium)
                        .background(BeoutColors.Card)
                        .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
                        .clickable { isStoryExpanded = !isStoryExpanded }
                        .padding(BeoutDimensions.SpaceM)
                ) {
                    Column {
                        Text(
                            text = localizedMovie.description.ifBlank { "Watch ${localizedMovie.title} in HD quality." },
                            style = BeoutTextStyles.Body.copy(
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = BeoutColors.TextPrimary
                            ),
                            maxLines = if (isStoryExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (localizedMovie.description.length > 120) {
                            Spacer(Modifier.height(BeoutDimensions.SpaceXS))
                            Text(
                                text = if (isStoryExpanded) tr("عرض أقل", "Show Less", "Daha Az Göster", "نمایش کمتر", "Moins") else tr("قراءة المزيد...", "Read More...", "Devamını Oku...", "ادامه مطلب...", "Lire la suite..."),
                                style = BeoutTextStyles.Caption.copy(
                                    color = BeoutColors.BrightBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Cast & Stars Section (طاقم التمثيل والنجوم)
        item {
            val castList = localizedMovie.castMembers.ifEmpty {
                localizedMovie.cast.map { CastMember(name = it, character = "طاقم العمل") }
            }

            if (castList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = BeoutDimensions.SpaceS)
                ) {
                    SectionHeading(
                        title = S.cast(),
                        icon = Icons.Rounded.Person,
                        modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
                    )
                    Spacer(Modifier.height(BeoutDimensions.SpaceS))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceM)
                    ) {
                        items(castList) { member ->
                            CastMemberCard(member = member)
                        }
                    }
                }
            }
        }

        // 7. Production & Technical Specifications Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BeoutDimensions.ScreenPadding, vertical = BeoutDimensions.SpaceS)
            ) {
                SectionHeading(title = S.details(), icon = Icons.Rounded.Info)
                Spacer(Modifier.height(BeoutDimensions.SpaceS))

                Column(
                    verticalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                    ) {
                        val displayCountry = localizedMovie.country.ifBlank {
                            val cat = localizedMovie.category
                            when {
                                cat.contains("عربي") -> "مصر • الوطن العربي"
                                cat.contains("تركي") -> "تركيا"
                                cat.contains("هندي") -> "الهند"
                                cat.contains("كوري") -> "كوريا الجنوبية"
                                cat.contains("انمي") || cat.contains("ياباني") -> "اليابان"
                                cat.contains("اسيوي") -> "آسيا"
                                else -> "الولايات المتحدة • بريطانيا"
                            }
                        }

                        val displayDirector = localizedMovie.director.ifBlank {
                            localizedMovie.genre.takeIf { it.isNotBlank() } ?: "السينما العالمية"
                        }

                        InfoCard(
                            title = S.director(),
                            value = displayDirector,
                            icon = Icons.Rounded.Videocam,
                            modifier = Modifier.weight(1f)
                        )
                        InfoCard(
                            title = tr("دولة الإنتاج", "Country", "Ülke", "کشور سازنده", "Pays"),
                            value = displayCountry,
                            icon = Icons.Rounded.Public,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                    ) {
                        InfoCard(
                            title = tr("اللغة والترجمة", "Language & Subtitles", "Dil ve Altyazı", "زبان و زیرنویس", "Langue et sous-titres"),
                            value = localizedMovie.language.ifBlank { "HD Audio" },
                            icon = Icons.Rounded.Language,
                            modifier = Modifier.weight(1f)
                        )
                        InfoCard(
                            title = tr("الجودة والبث", "Quality & Stream", "Kalite ve Yayın", "کیفیت پخش", "Qualité & Stream"),
                            value = "${localizedMovie.quality} • 4K HDR",
                            icon = Icons.Rounded.HighQuality,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 8. Recommended & Similar Movies
        item {
            if (localizedSimilar.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BeoutDimensions.SpaceM)
                ) {
                    SectionHeading(
                        title = S.similarMovies(),
                        icon = Icons.Rounded.Movie,
                        modifier = Modifier.padding(horizontal = BeoutDimensions.ScreenPadding)
                    )
                    Spacer(Modifier.height(BeoutDimensions.SpaceS))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = BeoutDimensions.ScreenPadding),
                        horizontalArrangement = Arrangement.spacedBy(BeoutDimensions.SpaceS)
                    ) {
                        items(localizedSimilar, key = { it.id }) { similar ->
                            BeoutMovieCard(
                                movie = similar,
                                onClick = { onSimilarMovieClick(similar) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Helper Components ----------------

@Composable
private fun SectionHeading(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = BeoutColors.BrightBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(BeoutDimensions.SpaceXS))
        }
        Text(
            text = title,
            style = BeoutTextStyles.SectionTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            color = BeoutColors.TextPrimary
        )
    }
}

@Composable
private fun RatingBadge(rating: Float) {
    if (rating <= 0f) return
    Box(
        modifier = Modifier
            .clip(BeoutShapes.Small)
            .background(BeoutColors.ElevatedSurface)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Small)
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
                text = "IMDb ${String.format(java.util.Locale.US, "%.1f", rating)}",
                style = BeoutTextStyles.Caption.copy(
                    fontWeight = FontWeight.Bold,
                    color = BeoutColors.BrightBlue
                )
            )
        }
    }
}

@Composable
private fun MetaBadge(text: String, isPrimary: Boolean = false) {
    val bg = if (isPrimary) BeoutColors.PrimaryBlue.copy(alpha = 0.2f) else BeoutColors.ElevatedSurface
    val border = if (isPrimary) BeoutColors.BrightBlue.copy(alpha = 0.5f) else BeoutColors.Divider
    val color = if (isPrimary) BeoutColors.BrightBlue else BeoutColors.TextSecondary

    Box(
        modifier = Modifier
            .clip(BeoutShapes.Small)
            .background(bg)
            .border(1.dp, border, BeoutShapes.Small)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = BeoutTextStyles.Caption.copy(
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        )
    }
}

@Composable
private fun CastMemberCard(member: CastMember) {
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!member.profileUrl.isNullOrBlank()) {
            AsyncImage(
                model = member.profileUrl,
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), CircleShape)
                    .background(BeoutColors.ElevatedSurface)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, BeoutColors.Divider, CircleShape)
                    .background(BeoutColors.ElevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(1).uppercase(),
                    style = BeoutTextStyles.SectionTitle.copy(color = BeoutColors.BrightBlue),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(BeoutDimensions.SpaceXXS))

        Text(
            text = member.name,
            style = BeoutTextStyles.Caption.copy(
                fontWeight = FontWeight.SemiBold,
                color = BeoutColors.TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (member.character.isNotBlank()) {
            Text(
                text = member.character,
                style = BeoutTextStyles.Caption.copy(
                    fontSize = 10.sp,
                    color = BeoutColors.TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(BeoutShapes.Medium)
            .background(BeoutColors.Card)
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Medium)
            .padding(BeoutDimensions.SpaceS)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BeoutColors.BrightBlue,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(BeoutDimensions.SpaceXXS))
            Text(
                text = title,
                style = BeoutTextStyles.Caption.copy(color = BeoutColors.TextSecondary)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = BeoutTextStyles.SecondaryInfo.copy(
                fontWeight = FontWeight.Medium,
                color = BeoutColors.TextPrimary
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatFullDuration(rawDuration: String, language: com.beouttv.app.data.i18n.AppLanguage): String {
    val digits = rawDuration.filter { it.isDigit() }
    val totalMinutes = digits.toIntOrNull() ?: 100
    if (totalMinutes <= 0) return rawDuration

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when (language) {
        com.beouttv.app.data.i18n.AppLanguage.ARABIC -> {
            if (hours > 0 && minutes > 0) "$hours س $minutes د ($totalMinutes دقيقة)"
            else if (hours > 0) "$hours س ($totalMinutes دقيقة)"
            else "$totalMinutes دقيقة"
        }
        com.beouttv.app.data.i18n.AppLanguage.ENGLISH -> {
            if (hours > 0 && minutes > 0) "${hours}h ${minutes}m ($totalMinutes min)"
            else if (hours > 0) "${hours}h ($totalMinutes min)"
            else "$totalMinutes min"
        }
        com.beouttv.app.data.i18n.AppLanguage.TURKISH -> {
            if (hours > 0 && minutes > 0) "$hours sa $minutes dk ($totalMinutes dk)"
            else if (hours > 0) "$hours sa ($totalMinutes dk)"
            else "$totalMinutes dk"
        }
        com.beouttv.app.data.i18n.AppLanguage.PERSIAN -> {
            if (hours > 0 && minutes > 0) "$hours س $minutes د ($totalMinutes دقیقه)"
            else if (hours > 0) "$hours س ($totalMinutes دقیقه)"
            else "$totalMinutes دقیقه"
        }
        com.beouttv.app.data.i18n.AppLanguage.FRENCH -> {
            if (hours > 0 && minutes > 0) "${hours}h ${minutes}m ($totalMinutes min)"
            else if (hours > 0) "${hours}h ($totalMinutes min)"
            else "$totalMinutes min"
        }
    }
}
