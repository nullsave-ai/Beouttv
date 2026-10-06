@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.beouttv.app.ui.screens.shortdrama

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import coil.compose.AsyncImage
import com.beouttv.app.R
import com.beouttv.app.data.model.ShortDramaFeedItem
import com.beouttv.app.data.repository.ShortDramaDownloadManager
import com.beouttv.app.data.repository.ShortDramaMediaCache
import com.beouttv.app.data.repository.ShortDramaRepository
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.data.ads.AdsterraManager
import com.beouttv.app.ui.components.InAppAdOverlay
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShortDramaScreen(
    viewModel: ShortDramaViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val episodeNotice by viewModel.episodeNoticeEvent.collectAsState()
    val activeAdUrl by AdsterraManager.activeAdUrl.collectAsState()
    val isAdActive = !activeAdUrl.isNullOrBlank()

    LaunchedEffect(Unit) {
        viewModel.initFavorites(context)
    }

    LaunchedEffect(episodeNotice) {
        episodeNotice?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearEpisodeNotice()
        }
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguages("ar", "ara", "arabic")
                .setPreferredTextLanguages("ar", "ara", "arabic")
                .setSelectUndeterminedTextLanguage(true)
                .setViewportSizeToPhysicalDisplaySize(context, true)
                .build()
        }
    }

    val sharedExoPlayer = remember {
        val ua = "okhttp/4.12.0"

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(15_000)

        val cacheDataSourceFactory = ShortDramaMediaCache.buildCacheDataSourceFactory(context, httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1_500,   // Min buffer: 1.5s
                3_500,   // Max buffer: 3.5s
                150,     // Start buffer: 150ms (instant start!)
                350      // Rebuffer: 350ms
            )
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    DisposableEffect(sharedExoPlayer) {
        onDispose {
            sharedExoPlayer.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sharedExoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    sharedExoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isAdActive) {
                        sharedExoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.feedItems.isNotEmpty()) {
            key(uiState.selectedCategory?.id) {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { uiState.feedItems.size }
                )

                LaunchedEffect(pagerState.currentPage, uiState.feedItems) {
                    viewModel.onFeedPageChanged(pagerState.currentPage)
                    val cur = pagerState.currentPage
                    val items = uiState.feedItems
                    for (offset in 1..2) {
                        val nextIdx = cur + offset
                        if (nextIdx in items.indices) {
                            val nextItem = items[nextIdx]
                            val nextUrl = nextItem.sources.getOrNull(nextItem.currentSourceIndex)?.streamUrl
                                ?: nextItem.sources.firstOrNull()?.streamUrl
                            if (!nextUrl.isNullOrBlank()) {
                                ShortDramaMediaCache.preload(context, nextUrl)
                            }
                        }
                    }
                }

                VerticalPager(
                    state = pagerState,
                    key = { page -> uiState.feedItems.getOrNull(page)?.let { "${it.series.id}_${it.episode.id}" } ?: page.toString() },
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1
                ) { page ->
                    val feedItem = uiState.feedItems[page]
                    val isCurrentPage = pagerState.currentPage == page

                    ReelVideoItem(
                        feedItem = feedItem,
                        isActive = isCurrentPage,
                        isAdActive = isAdActive,
                        isMuted = uiState.isMuted,
                        isLiked = uiState.likedSeriesIds.contains(feedItem.series.id),
                        isCleanScreen = uiState.isCleanScreen,
                        exoPlayer = sharedExoPlayer,
                        trackSelector = trackSelector,
                        onToggleLike = { viewModel.toggleLike(feedItem.series.id, context) },
                        onToggleMute = { viewModel.toggleMute() },
                        onToggleCleanScreen = { viewModel.toggleCleanScreen() },
                        onExitCleanScreen = { viewModel.setCleanScreen(false) },
                        onOpenEpisodes = { viewModel.showEpisodesSheet() },
                        onOpenSources = { viewModel.showSourcesSheet() },
                        onAdvanceNextEpisode = {
                            val items = uiState.feedItems
                            if (page in items.indices) {
                                val item = items[page]
                                val nextEpNum = item.episode.episodeNumber + 1
                                val hasNextEpStream = ShortDramaRepository.buildSourcesForEpisode(
                                    item.series.id,
                                    nextEpNum,
                                    item.series.platform
                                ).isNotEmpty()

                                if (hasNextEpStream && nextEpNum <= item.series.episodeCount) {
                                    viewModel.advanceToNextEpisode()
                                } else if (page + 1 < items.size) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page + 1)
                                    }
                                }
                            }
                        },
                        onAdvancePreviousEpisode = { viewModel.advanceToPreviousEpisode() }
                    )
                }
            }
        } else if (uiState.selectedCategory?.id == "favorites_ar") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "قائمة المعجبات فارغة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "انقر على زر الإعجاب في أي مسلسل لإضافته إلى قائمة المعجبات هنا والوصول إليه في أي وقت",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BeoutColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        } else if (uiState.selectedCategory?.id == "downloads_ar") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "لا توجد أعمال محملة حالياً",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "يمكنك تنزيل الحلقات من قائمة الحلقات في أي مسلسل لمشاهدتها بدون إنترنت",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BeoutColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = BeoutColors.BrightBlue,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = "جاري تحضير تلفزيون قصير...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = BeoutColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // ── Top Header: Fades out in Clean Screen Mode ──
        AnimatedVisibility(
            visible = !uiState.isCleanScreen,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BeoutTV Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_drama_reels),
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            text = "تلفزيون قصير",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BeoutColors.TextPrimary,
                                fontSize = 15.sp
                            )
                        )
                    }
                }

                // Category & Platform Tabs
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(uiState.categories) { cat ->
                        val isSelected = uiState.selectedCategory?.id == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.selectCategory(cat) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 20.dp else 0.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (isSelected) BeoutColors.BrightBlue else Color.Transparent)
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .padding(top = 4.dp),
                        color = BeoutColors.BrightBlue,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // Bottom Sheets for Episodes & Sources
        if (uiState.isEpisodesSheetVisible || uiState.isSourcesSheetVisible) {
            val currentItem = uiState.feedItems.getOrNull(uiState.currentFeedIndex)
            if (currentItem != null) {
                ShortDramaEpisodesBottomSheet(
                    feedItem = currentItem,
                    initialTab = if (uiState.isSourcesSheetVisible) 1 else 0,
                    onDismiss = {
                        viewModel.hideEpisodesSheet()
                        viewModel.hideSourcesSheet()
                    },
                    onEpisodeSelected = { episode ->
                        viewModel.selectEpisodeForCurrentSeries(episode)
                    },
                    onSourceSelected = { sourceIndex ->
                        viewModel.selectSourceForCurrentItem(sourceIndex)
                    }
                )
            }
        }

        // SmartLink In-App Ad Overlay (5-second countdown with Skip button)
        if (!activeAdUrl.isNullOrBlank()) {
            InAppAdOverlay(
                adUrl = activeAdUrl!!,
                onDismiss = {
                    AdsterraManager.dismissAd()
                }
            )
        }
    }
}

@Composable
private fun ReelVideoItem(
    feedItem: ShortDramaFeedItem,
    isActive: Boolean,
    isAdActive: Boolean = false,
    isMuted: Boolean,
    isLiked: Boolean,
    isCleanScreen: Boolean,
    exoPlayer: ExoPlayer,
    trackSelector: DefaultTrackSelector,
    onToggleLike: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCleanScreen: () -> Unit,
    onExitCleanScreen: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onOpenSources: () -> Unit,
    onAdvanceNextEpisode: () -> Unit,
    onAdvancePreviousEpisode: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var showHeartAnimation by remember { mutableStateOf(false) }
    var isFastForwarding by remember { mutableStateOf(false) }
    var skipFeedbackText by remember { mutableStateOf<String?>(null) }
    var skipFeedbackSide by remember { mutableIntStateOf(0) } // -1 left, 1 right

    // Playback time tracking
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    // Subtitle cues state tracking for elevated Compose rendering
    var currentCues by remember(feedItem.series.id, feedItem.episode.episodeNumber) {
        mutableStateOf<List<Cue>>(emptyList())
    }

    // Smart 25% Chunk Preload Tracking for Active & Next Episodes
    var triggeredCurrentChunks by remember(feedItem.series.id, feedItem.episode.episodeNumber) { mutableStateOf(setOf<Int>()) }
    var triggeredNextChunks by remember(feedItem.series.id, feedItem.episode.episodeNumber) { mutableStateOf(setOf<Int>()) }

    val activeSource = feedItem.sources.getOrNull(feedItem.currentSourceIndex)
        ?: feedItem.sources.firstOrNull()

    // Dynamic resolution constraint adjustment: unconstrained 1080p, 720p, or 540p
    LaunchedEffect(activeSource?.quality, isActive) {
        if (!isActive) return@LaunchedEffect
        val q = activeSource?.quality ?: ""
        val builder = trackSelector.buildUponParameters()
        when {
            q.contains("1080") -> {
                builder.clearVideoSizeConstraints()
                    .setMaxVideoSize(1080, 1920)
            }
            q.contains("720") -> {
                builder.clearVideoSizeConstraints()
                    .setMaxVideoSize(720, 1280)
            }
            q.contains("540") -> {
                builder.clearVideoSizeConstraints()
                    .setMaxVideoSize(540, 960)
            }
            else -> {
                builder.clearVideoSizeConstraints()
            }
        }
        trackSelector.parameters = builder.build()
    }

    // Check if current episode is available locally in downloaded cache
    val downloadedEp = remember(feedItem.series.id, feedItem.episode.episodeNumber) {
        ShortDramaDownloadManager.getDownloadedEpisode(feedItem.series.id, feedItem.episode.episodeNumber)
    }

    var dynamicSubtitleUrl by remember(feedItem.series.id, feedItem.episode.episodeNumber) { mutableStateOf("") }

    // Asynchronously resolve subtitle if activeSource does not have one yet
    LaunchedEffect(feedItem.series.id, feedItem.episode.episodeNumber, activeSource?.subtitleUrl) {
        if (activeSource?.subtitleUrl.isNullOrBlank() && downloadedEp?.localSubtitlePath.isNullOrBlank()) {
            val cleanVid = ShortDramaRepository.resolveVideoId(feedItem.series.id)
            if (cleanVid.all { it.isDigit() }) {
                val resolved = withContext(Dispatchers.IO) {
                    ShortDramaRepository.resolveSubtitleForEpisode(cleanVid, feedItem.episode.episodeNumber)
                }
                if (resolved.isNotBlank()) {
                    dynamicSubtitleUrl = resolved
                }
            }
        }
    }

    var lastLoadedSeriesId by remember { mutableStateOf("") }
    var lastLoadedEpisodeNumber by remember { mutableIntStateOf(-1) }

    // Set or update media source whenever activeSource, subtitles, or offline status change
    LaunchedEffect(isActive, activeSource?.streamUrl, activeSource?.subtitleUrl, downloadedEp, dynamicSubtitleUrl) {
        if (!isActive) return@LaunchedEffect
        triggeredCurrentChunks = emptySet()
        triggeredNextChunks = emptySet()

        val isSameEpisode = (lastLoadedSeriesId == feedItem.series.id && lastLoadedEpisodeNumber == feedItem.episode.episodeNumber)
        lastLoadedSeriesId = feedItem.series.id
        lastLoadedEpisodeNumber = feedItem.episode.episodeNumber

        val mediaItemBuilder = MediaItem.Builder()
        val localVideoFile = downloadedEp?.localVideoPath?.let { File(it) }
        if (localVideoFile != null && localVideoFile.exists() && localVideoFile.length() > 50_000L) {
            // Play 100% offline from local app storage with zero network latency
            mediaItemBuilder.setUri(Uri.fromFile(localVideoFile))
                .setMimeType(MimeTypes.VIDEO_MP4)
            val localSub = downloadedEp.localSubtitlePath
            if (!localSub.isNullOrBlank() && File(localSub).exists()) {
                val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(File(localSub)))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("ar")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            } else if (!dynamicSubtitleUrl.isNullOrBlank()) {
                val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(dynamicSubtitleUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("ar")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }
        } else {
            val url = activeSource?.streamUrl
            if (url.isNullOrBlank()) return@LaunchedEffect
            mediaItemBuilder.setUri(Uri.parse(url))
            val subUrl = activeSource.subtitleUrl.ifBlank { dynamicSubtitleUrl }
            if (!subUrl.isNullOrBlank()) {
                val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("ar")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }
        }

        val mediaItem = mediaItemBuilder.build()
        // Seamlessly maintain current seek position ONLY when switching quality/server on the SAME episode!
        val resumePos = if (isSameEpisode && currentPositionMs > 1_000L) currentPositionMs else 0L
        if (!isSameEpisode) {
            currentPositionMs = 0L
            totalDurationMs = 0L
        }
        exoPlayer.setMediaItem(mediaItem, resumePos)
        exoPlayer.prepare()
        if (!isAdActive) {
            exoPlayer.play()
        }
        if (!isSameEpisode && feedItem.episode.episodeNumber >= 3) {
            AdsterraManager.onEpisodeLaunch(feedItem.episode.episodeNumber)
        }
    }

    LaunchedEffect(isAdActive, isActive) {
        if (isActive) {
            if (isAdActive) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }
    }

    LaunchedEffect(isMuted, exoPlayer, isActive) {
        if (isActive) {
            exoPlayer.volume = if (isMuted) 0f else 1f
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            isFastForwarding = false
        }
    }

    // Real-time timeline ticker & MovieBox-style lean preloader
    LaunchedEffect(isPlaying, exoPlayer, isActive) {
        while (isActive) {
            if (!isScrubbing) {
                currentPositionMs = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                totalDurationMs = if (dur > 0) dur else 0L

                if (totalDurationMs > 4_000L) {
                    val fraction = currentPositionMs.toFloat() / totalDurationMs
                    val videoId = ShortDramaRepository.resolveVideoId(feedItem.series.id)
                    val nextEp = feedItem.episode.episodeNumber + 1

                    // Preload first 256KB of next episode when user is 65% through current reel
                    if (fraction >= 0.65f && 0 !in triggeredNextChunks && nextEp <= feedItem.series.episodeCount) {
                        triggeredNextChunks = triggeredNextChunks + 0
                        ShortDramaMediaCache.preloadNextEpisode(context, videoId, nextEp, feedItem.series.platform)
                    }
                }
            }
            delay(250)
        }
    }

    DisposableEffect(exoPlayer, isActive) {
        if (!isActive) return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isBuffering = false
                isPlaying = true
                ShortDramaMediaCache.resumePreload()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isBuffering = false
                    ShortDramaMediaCache.resumePreload()
                } else if (state == Player.STATE_BUFFERING) {
                    if (!exoPlayer.isPlaying) {
                        isBuffering = true
                    }
                    ShortDramaMediaCache.pausePreload()
                } else if (state == Player.STATE_ENDED) {
                    isBuffering = false
                    // Auto-advance to next episode seamlessly
                    onAdvanceNextEpisode()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) isBuffering = false
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ShortDramaPlayer", "Playback error: ${error.message}", error)
                isBuffering = false
                val sources = feedItem.sources
                val currentIdx = feedItem.currentSourceIndex
                if (sources.isNotEmpty() && currentIdx < sources.size - 1) {
                    val nextIdx = currentIdx + 1
                    val nextSrc = sources[nextIdx]
                    val nextUrl = nextSrc.streamUrl
                    if (!nextUrl.isNullOrBlank()) {
                        val mediaItem = MediaItem.Builder()
                            .setUri(Uri.parse(nextUrl))
                            .build()
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
            }
            override fun onCues(cueGroup: CueGroup) {
                currentCues = cueGroup.cues
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            currentCues = emptyList()
            exoPlayer.removeListener(listener)
        }
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomActionPadding = navBarPadding + 98.dp

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Infinite animation for orbital buffer rings & cyber laser line
    val infiniteTransition = rememberInfiniteTransition(label = "ambientBuffer")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )
    val laserOffsetFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(isCleanScreen) {
                detectTapGestures(
                    onTap = {
                        if (isCleanScreen) {
                            // Any tap on clean screen exits clean mode!
                            onExitCleanScreen()
                        } else if (isFastForwarding) {
                            exoPlayer.setPlaybackSpeed(1.0f)
                            isFastForwarding = false
                        } else {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isCleanScreen) {
                            onExitCleanScreen()
                        } else {
                            val width = containerSize.width
                            if (width > 0) {
                                val ratio = offset.x / width
                                when {
                                    ratio < 0.35f -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val maxDur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                                        val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(maxDur)
                                        exoPlayer.seekTo(target)
                                        currentPositionMs = target
                                        skipFeedbackText = "+10 ثواني"
                                        skipFeedbackSide = -1
                                    }
                                    ratio > 0.65f -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(target)
                                        currentPositionMs = target
                                        skipFeedbackText = "-10 ثواني"
                                        skipFeedbackSide = 1
                                    }
                                    else -> {
                                        onToggleLike()
                                        showHeartAnimation = true
                                    }
                                }
                            }
                        }
                    },
                    onLongPress = {
                        if (!isCleanScreen) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            exoPlayer.setPlaybackSpeed(2.0f)
                            isFastForwarding = true
                        }
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (isFastForwarding) {
                            exoPlayer.setPlaybackSpeed(1.0f)
                            isFastForwarding = false
                        }
                    }
                )
            }
    ) {
        // Series Cover Poster (shown as immediate background / placeholder)
        AsyncImage(
            model = feedItem.series.coverUrl,
            contentDescription = feedItem.series.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Player Surface: 100% Fullscreen Immersive Canvas (Adaptive Zoom in Portrait, Fit in Landscape)
        if (isActive) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = if (isLandscape) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        // Subtitles are rendered via high-precision Compose overlay to avoid being hidden under system nav bar
                        subtitleView?.visibility = android.view.View.GONE
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    if (playerView.player != exoPlayer) {
                        playerView.player = exoPlayer
                    }
                    playerView.resizeMode = if (isLandscape) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    playerView.subtitleView?.visibility = android.view.View.GONE
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── 2X Fast Forward Active Floating Indicator ──
        AnimatedVisibility(
            visible = isFastForwarding && !isCleanScreen,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .clickable {
                        exoPlayer.setPlaybackSpeed(1.0f)
                        isFastForwarding = false
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = null,
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "2X تسريع (انقر للإيقاف)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إيقاف التسريع",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // ── Skip Feedback Indicator (-10s / +10s) ──
        if (skipFeedbackText != null) {
            LaunchedEffect(skipFeedbackText) {
                delay(650)
                skipFeedbackText = null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = if (skipFeedbackSide == -1) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (skipFeedbackSide == -1) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
                        contentDescription = null,
                        tint = BeoutColors.BrightBlue,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = skipFeedbackText ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // ── Big Heart Like Animation ──
        if (showHeartAnimation) {
            LaunchedEffect(Unit) {
                delay(650)
                showHeartAnimation = false
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(96.dp)
                )
            }
        }

        // ── Stream Loading / Resolving state ──
        if (activeSource?.streamUrl.isNullOrBlank() && isActive) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BeoutColors.BrightBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
            }
        } else if (!isPlaying && !isBuffering && !isScrubbing && !isCleanScreen) {
            // ── Pause state indicator (Only in normal mode) ──
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // ── Reel Subtitle Overlay (Elevated ~25% below video center, comfortably above bottom controls & system navigation buttons) ──
        if (currentCues.isNotEmpty()) {
            val subtitleText = remember(currentCues) {
                currentCues.mapNotNull { it.text?.toString() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .trim()
            }
            if (subtitleText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = BiasAlignment(0f, 0.50f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(1.5f, 1.5f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
            }
        }

        // ── All Overlays (Smoothly hidden in Clean Screen Mode) ──
        AnimatedVisibility(
            visible = !isCleanScreen,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Subtle Bottom Shadow Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                // Floating Time Preview Bubble during Seek / Scrubbing
                if (isScrubbing) {
                    val remain = (totalDurationMs - scrubPositionMs).coerceAtLeast(0L)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomActionPadding + 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE610141B))
                            .border(1.dp, BeoutColors.BrightBlue, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${formatDuration(scrubPositionMs)}  /  -${formatDuration(remain)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Bottom-left: Series Metadata, Server, and Auto-Buffering Status Badge
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 88.dp, bottom = bottomActionPadding),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BeoutColors.PrimaryBlue)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "بي اوت TV",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            )
                        }


                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BeoutColors.Card.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "حلقة ${feedItem.episode.episodeNumber} من ${feedItem.series.episodeCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = BeoutColors.TextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Ambient Buffering Badge (Replaces central spinner)
                        AnimatedVisibility(visible = isBuffering) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F223D))
                                    .border(0.8.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(BeoutColors.BrightBlue)
                                    )
                                    Text(
                                        text = "جاري التحميل...",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BeoutColors.BrightBlue,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = feedItem.series.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Navigation Shortcuts (Previous & Next Episode)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick previous episode shortcut if episodeNumber > 1
                        if (feedItem.episode.episodeNumber > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BeoutColors.Card.copy(alpha = 0.70f))
                                    .clickable { onAdvancePreviousEpisode() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "الحلقة السابقة",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BeoutColors.BrightBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Quick next episode shortcut
                        if (feedItem.episode.episodeNumber < feedItem.series.episodeCount) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BeoutColors.Card.copy(alpha = 0.70f))
                                    .clickable { onAdvanceNextEpisode() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الحلقة التالية",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BeoutColors.BrightBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = null,
                                    tint = BeoutColors.BrightBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // ── Bottom Scrubbing Timeline & Current/Remaining Time (Unified Single Row) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = navBarPadding + 74.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayCurrent = if (isScrubbing) scrubPositionMs else currentPositionMs
                    val displayRemaining = (totalDurationMs - displayCurrent).coerceAtLeast(0L)

                    // 1. In RTL layout: Child 1 is on the physical RIGHT: Elapsed Time (00:00)
                    Text(
                        text = formatDuration(displayCurrent),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        modifier = Modifier.width(42.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )

                    // 2. Center: RTL Scrubber with 1:1 Thumb Dragging (Starts from Right, Fills to Left)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .pointerInput(totalDurationMs) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (totalDurationMs > 0 && size.width > 0) {
                                            isScrubbing = true
                                            val fraction = (1f - (offset.x / size.width)).coerceIn(0f, 1f)
                                            scrubPositionMs = (fraction * totalDurationMs).toLong()
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (totalDurationMs > 0 && size.width > 0) {
                                            val fraction = (1f - (change.position.x / size.width)).coerceIn(0f, 1f)
                                            scrubPositionMs = (fraction * totalDurationMs).toLong()
                                        }
                                    },
                                    onDragEnd = {
                                        if (isScrubbing) {
                                            exoPlayer.seekTo(scrubPositionMs)
                                            currentPositionMs = scrubPositionMs
                                            isScrubbing = false
                                        }
                                    },
                                    onDragCancel = {
                                        isScrubbing = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val activeFraction = if (totalDurationMs > 0) {
                            if (isScrubbing) (scrubPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                            else (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                        } else 0f

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        ) {
                            val w = size.width
                            val midY = size.height / 2f
                            val trackHeight = 3.5.dp.toPx()

                            // 1. Background Track
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.20f),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, midY - trackHeight / 2f),
                                size = androidx.compose.ui.geometry.Size(w, trackHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                            )

                            // 2. Active Played Progress (Starts from Right, extends towards Left)
                            val activeWidth = w * activeFraction
                            val startX = (w - activeWidth).coerceIn(0f, w)

                            // 3. Serrated equalizer ticks at the beginning (at physical RIGHT)
                            val serratedCount = 5
                            val serratedStep = 4.5.dp.toPx()
                            val serratedStartX = 2.dp.toPx()
                            for (i in 0 until serratedCount) {
                                val tx = w - serratedStartX - i * serratedStep
                                val tickHeight = (5 + (i % 3) * 2.2).dp.toPx()
                                val tickColor = if (tx >= startX) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.35f)
                                drawLine(
                                    color = tickColor,
                                    start = androidx.compose.ui.geometry.Offset(tx, midY - tickHeight / 2f),
                                    end = androidx.compose.ui.geometry.Offset(tx, midY + tickHeight / 2f),
                                    strokeWidth = 1.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            if (activeWidth > 0f) {
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            BeoutColors.BrightBlue,
                                            BeoutColors.BrightBlue.copy(alpha = 0.85f)
                                        ),
                                        startX = startX,
                                        endX = w
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(startX, midY - trackHeight / 2f),
                                    size = androidx.compose.ui.geometry.Size(activeWidth, trackHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                                )

                                // Undulating wave shimmer along the active progress line
                                val waveAmplitude = 1.6.dp.toPx()
                                val waveFreq = 0.06f
                                val wavePath = androidx.compose.ui.graphics.Path()
                                var isFirst = true
                                val stepPx = 4f
                                var wx = startX
                                while (wx <= w) {
                                    val wy = midY + kotlin.math.sin((wx * waveFreq) - (laserOffsetFraction * 2 * kotlin.math.PI.toFloat())) * waveAmplitude
                                    if (isFirst) {
                                        wavePath.moveTo(wx, wy)
                                        isFirst = false
                                    } else {
                                        wavePath.lineTo(wx, wy)
                                    }
                                    wx += stepPx
                                }
                                drawPath(
                                    path = wavePath,
                                    color = Color.White.copy(alpha = 0.45f),
                                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // 4. Glowing Thumb Handle directly in Canvas: Exactly at startX (1:1 with touch!)
                            val thumbRadius = if (isScrubbing) 8.dp.toPx() else 6.dp.toPx()
                            val innerDotRadius = if (isScrubbing) 3.5.dp.toPx() else 2.3.dp.toPx()
                            val thumbCenter = androidx.compose.ui.geometry.Offset(startX, midY)

                            // Outer dark glowing circle
                            drawCircle(
                                color = Color(0xFF0F223D),
                                radius = thumbRadius,
                                center = thumbCenter
                            )
                            // Bright blue ring
                            drawCircle(
                                color = BeoutColors.BrightBlue,
                                radius = thumbRadius,
                                center = thumbCenter,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            // Inner white core
                            drawCircle(
                                color = Color.White,
                                radius = innerDotRadius,
                                center = thumbCenter
                            )
                        }
                    }

                    // 3. In RTL layout: Child 3 is on the physical LEFT: Remaining Time (-00:45)
                    Text(
                        text = "-${formatDuration(displayRemaining)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BeoutColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        modifier = Modifier.width(46.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }

                // ── Bottom-right: Vertical Action Buttons ──
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = bottomActionPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Series Cover Thumbnail with Ambient Orbital Halo Ring
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                rotate(orbitAngle) {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color.Transparent,
                                                BeoutColors.BrightBlue,
                                                Color(0xFF4285F4),
                                                Color.Transparent
                                            )
                                        ),
                                        startAngle = 0f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, BeoutColors.BrightBlue, CircleShape)
                                .clickable { onOpenEpisodes() }
                        ) {
                            AsyncImage(
                                model = feedItem.series.coverUrl,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Clean Screen Mode Button (Like TikTok Clear Display)
                    ReelActionButton(
                        icon = Icons.Rounded.CropFree,
                        label = "شاشة نظيفة",
                        iconTint = BeoutColors.BrightBlue,
                        onClick = onToggleCleanScreen
                    )

                    // Like Button
                    ReelActionButton(
                        icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        label = if (isLiked) "معجب" else "إعجاب",
                        iconTint = if (isLiked) Color(0xFFFF2D55) else Color.White,
                        onClick = onToggleLike
                    )

                    // Episodes Sheet Button
                    ReelActionButton(
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        label = "الحلقات",
                        iconTint = Color.White,
                        onClick = onOpenEpisodes
                    )

                    // Quality / Sources Sheet Button
                    ReelActionButton(
                        icon = Icons.Rounded.HighQuality,
                        label = "الجودة",
                        iconTint = BeoutColors.BrightBlue,
                        onClick = onOpenSources
                    )

                    // Mute / Unmute Button
                    ReelActionButton(
                        icon = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        label = if (isMuted) "كتم" else "صوت",
                        iconTint = Color.White,
                        onClick = onToggleMute
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BeoutColors.Card.copy(alpha = 0.70f))
                .border(1.dp, BeoutColors.Divider.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = BeoutColors.TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%02d:%02d".format(mins, secs)
}

/**
 * Ensures subtitle lines never exceed 5 words per line for optimal vertical screen readability
 */
private fun formatSubtitleMaxWordsPerLine(text: CharSequence, maxWords: Int = 5): String {
    val plain = text.toString().replace(Regex("<[^>]*>"), "")
    val words = plain.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (words.size <= maxWords) return words.joinToString(" ")
    return words.chunked(maxWords).joinToString("\n") { it.joinToString(" ") }
}
