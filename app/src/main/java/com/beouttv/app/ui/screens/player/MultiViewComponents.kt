package com.beouttv.app.ui.screens.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.beouttv.app.R
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.beouttv.app.data.api.CoreApiClient
import com.beouttv.app.data.source.stream.OstoraStreamDataSource
import com.beouttv.app.data.source.stream.SanitizedHlsPlaylistParserFactory
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.data.source.stream.LiveGatewayStreamResolver
import com.beouttv.app.data.source.stream.OkRuStreamResolver
import com.beouttv.app.data.source.stream.OstoraApiResolver
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutDimensions
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.tv.tvFocusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents an active channel stream in Multi-View mode (1 to 4 concurrent streams).
 */
data class MultiViewSlot(
    val slotId: Int, // 0..3
    val channelId: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val drmKeyId: String? = null,
    val drmKey: String? = null,
    val drmLicenseUrl: String? = null,
    val groupId: String? = null,
    val isLive: Boolean = true,
    var resizeModeIndex: Int = 0 // 0=Fit, 1=Fill, 2=Zoom
)

/**
 * Resolves stream URL dynamically for any channel (Ostora tokens, CoreApiClient, or Gateway endpoints).
 */
suspend fun resolveMultiViewChannelStream(channel: Channel): Channel {
    if (channel.id.startsWith("gw_") || channel.id.startsWith("yacine")) {
        return channel
    }

    val realId = when {
        channel.id.startsWith("ostora_") -> channel.id.substringAfter("ostora_")
        channel.id.startsWith("live_") -> channel.id.substringAfter("live_")
        channel.id.isNotBlank() && channel.id.length >= 4 && channel.id.all { it.isDigit() } -> channel.id
        else -> ""
    }

    var streamUrl = channel.streamUrl
    var userAgent = channel.userAgent ?: OstoraApiResolver.DEFAULT_USER_AGENT
    var referer = channel.referer ?: OstoraApiResolver.DEFAULT_REFERER
    var drmKeyId = channel.drmKeyId
    var drmKey = channel.drmKey

    // 1. Check CoreApiClient for direct dynamic token
    if (realId.isNotBlank()) {
        try {
            val tokenUrl = withContext(Dispatchers.IO) {
                CoreApiClient().resolveStreamUrl(realId)
            }
            if (!tokenUrl.isNullOrBlank()) {
                streamUrl = tokenUrl
            }
        } catch (_: Exception) {}
    }

    // 2. Check Ostora dynamic stream resolver for fresh tokens and DRM keys
    if (OstoraApiResolver.isOstoraChannel(channel) ||
        OstoraApiResolver.isOstoraUrl(streamUrl) ||
        OstoraApiResolver.isKnownSportsChannel(channel.name) ||
        OstoraApiResolver.isKnownSportsChannel(channel.id)
    ) {
        try {
            val resolved = withContext(Dispatchers.IO) {
                OstoraApiResolver.resolveDynamicStream(
                    streamUrl = streamUrl,
                    channelId = channel.id,
                    channelName = channel.name,
                    groupId = channel.groupId
                )
            }
            streamUrl = resolved.streamUrl
            userAgent = resolved.userAgent ?: userAgent
            referer = resolved.referer ?: referer
            drmKeyId = resolved.drmKeyId ?: drmKeyId
            drmKey = resolved.drmKey ?: drmKey
        } catch (_: Exception) {
            val cleaned = OstoraApiResolver.cleanStreamUrl(streamUrl)
            val (cUrl, kid, key) = OstoraApiResolver.parseDrmKeysFromUrl(cleaned)
            streamUrl = cUrl
            drmKeyId = kid ?: drmKeyId
            drmKey = key ?: drmKey
        }
    } else if (LiveGatewayStreamResolver.isTargetApiUrl(streamUrl)) {
        try {
            val resolved = withContext(Dispatchers.IO) {
                LiveGatewayStreamResolver.resolve(streamUrl)
            }
            if (resolved != null && resolved.streamUrl.isNotBlank()) {
                streamUrl = resolved.streamUrl
                userAgent = resolved.userAgent ?: userAgent
                referer = resolved.referer ?: referer
                drmKeyId = resolved.drmKeyId ?: drmKeyId
                drmKey = resolved.drmKey ?: drmKey
            }
        } catch (_: Exception) {}
    } else if (OkRuStreamResolver.isOkRuUrl(streamUrl) || channel.id.startsWith("ok_") || channel.id.startsWith("okru_")) {
        try {
            val resolved = withContext(Dispatchers.IO) {
                OkRuStreamResolver.resolve(streamUrl)
            }
            if (resolved != null && resolved.streamUrl.isNotBlank()) {
                streamUrl = resolved.streamUrl
                userAgent = resolved.userAgent ?: userAgent
                referer = resolved.referer ?: referer
                drmKeyId = resolved.drmKeyId ?: drmKeyId
                drmKey = resolved.drmKey ?: drmKey
            }
        } catch (_: Exception) {}
    }

    val (cleanUrl, parsedKid, parsedKey) = OstoraApiResolver.parseDrmKeysFromUrl(OstoraApiResolver.cleanStreamUrl(streamUrl))
    return channel.copy(
        streamUrl = cleanUrl,
        userAgent = userAgent,
        referer = referer,
        drmKeyId = parsedKid ?: drmKeyId,
        drmKey = parsedKey ?: drmKey
    )
}

/**
 * Determines whether a given slot should display the BeoutTV watermark overlay.
 * Returns true for all beIN Sports channels (Ostora streams) to permanently cover the underlying logo.
 */
fun shouldShowSlotWatermark(slot: MultiViewSlot): Boolean {
    val t = slot.name.lowercase(java.util.Locale.ROOT)
    val id = slot.channelId.lowercase(java.util.Locale.ROOT)
    val u = slot.streamUrl.lowercase(java.util.Locale.ROOT)
    val g = slot.groupId ?: ""

    val isOtherChannel = t.contains("ثمانية") || t.contains("thmanyah") ||
            t.contains("أون") || t.contains("اون") || t.contains("ontime") || t.contains("on sport") ||
            t.contains("ssc") || t.contains("alkass") || t.contains("كاس") || t.contains("كأس") ||
            t.contains("أبوظبي") || t.contains("ابوظبي") || t.contains("ad sport") ||
            t.contains("دبي") || t.contains("dubai") || t.contains("mbc") || t.contains("rotana") ||
            t.contains("قنوات عربية") || t.contains("الأخبار") || t.contains("اخبار") ||
            id.contains("thmanyah") || id.contains("ontime") || id.contains("onsport") || id.contains("ssc") ||
            id.contains("alkass") || id.contains("mbc") || id.contains("rotana")

    if (isOtherChannel) return false

    val hasBeinTitle = t.contains("bein") ||
            t.contains("بي ان") ||
            t.contains("بي إن") ||
            t.contains("بين سبورت") ||
            (t.contains("بين") && (t.contains("سبورت") || t.any { it.isDigit() }))

    val hasBeinIdOrUrl = id.contains("bein") || u.contains("bein")

    val isAlwanBein = (t.contains("ألوان") || t.contains("الوان") || t.contains("alwan") ||
            id.contains("alwan") || id.startsWith("sp_alwan") || g == "7193") &&
            (hasBeinTitle || hasBeinIdOrUrl || u.contains("maziika") || t.contains("سبورت") || t.any { it.isDigit() })

    return hasBeinTitle || hasBeinIdOrUrl || isAlwanBein
}

/**
 * Proportional BeoutTV Watermark overlay for each slot in Multi-View mode.
 * Adapts to each pane's dimensions and resize mode, covering the Ostora banner precisely.
 */
@Composable
fun MultiViewSlotWatermark(
    resizeModeIndex: Int,
    modifier: Modifier = Modifier,
    videoWidth: Int = 1920,
    videoHeight: Int = 1080
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val videoAspectRatio = remember(videoWidth, videoHeight) {
            if (videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else (16f / 9f)
        }
        val containerAspectRatio = if (maxHeight.value > 0) maxWidth.value / maxHeight.value else (16f / 9f)

        // Compute precise video frame geometry relative to the pane's local bounds
        val (videoLeft, videoTop, currentVideoWidth, currentVideoHeight) = remember(
            resizeModeIndex,
            containerAspectRatio,
            videoAspectRatio,
            maxWidth,
            maxHeight
        ) {
            when (resizeModeIndex) {
                1 -> {
                    // RESIZE_MODE_FILL (Stretched to pane)
                    listOf(0.dp, 0.dp, maxWidth, maxHeight)
                }
                2 -> {
                    // RESIZE_MODE_ZOOM (Cropped to fill pane)
                    if (containerAspectRatio > videoAspectRatio) {
                        val h = maxWidth / videoAspectRatio
                        val topOffset = (maxHeight - h) / 2
                        listOf(0.dp, topOffset, maxWidth, h)
                    } else {
                        val w = maxHeight * videoAspectRatio
                        val leftOffset = (maxWidth - w) / 2
                        listOf(leftOffset, 0.dp, w, maxHeight)
                    }
                }
                else -> {
                    // RESIZE_MODE_FIT (Letterbox / Pillarbox inside pane)
                    if (containerAspectRatio > videoAspectRatio) {
                        val w = maxHeight * videoAspectRatio
                        val leftOffset = (maxWidth - w) / 2
                        listOf(leftOffset, 0.dp, w, maxHeight)
                    } else {
                        val h = maxWidth / videoAspectRatio
                        val topOffset = (maxHeight - h) / 2
                        listOf(0.dp, topOffset, maxWidth, h)
                    }
                }
            }
        }

        // Ostora banner is horizontally centered at 9.87% from video frame's right edge
        // and vertically centered at 8.70% from video frame's top edge.
        val baseWatermarkWidth = (currentVideoWidth * 0.168f) * 0.90f
        val watermarkWidth = baseWatermarkWidth.coerceIn(16.dp, 103.dp)
        val watermarkHeight = (watermarkWidth / 3.0f) * 0.88f

        val effectiveVideoLeft = maxOf(0.dp, videoLeft)
        val effectiveVideoTop = maxOf(0.dp, videoTop)

        val ostoraCenterX = effectiveVideoLeft + currentVideoWidth - (currentVideoWidth * 0.0987f)
        val ostoraCenterY = effectiveVideoTop + (currentVideoHeight * 0.0870f)

        val watermarkX = (ostoraCenterX - (watermarkWidth / 2f))
            .coerceIn(0.dp, (maxWidth - watermarkWidth).coerceAtLeast(0.dp))
        val watermarkY = (ostoraCenterY - (watermarkHeight / 2f))
            .coerceIn(0.dp, (maxHeight - watermarkHeight).coerceAtLeast(0.dp))

        // Enforce LTR coordinates so x is strictly measured from the physical LEFT to RIGHT
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = watermarkX, y = watermarkY)
                        .width(watermarkWidth)
                        .height(watermarkHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .zIndex(5f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.beout_watermark),
                        contentDescription = "BeoutTV Watermark",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Secondary Player Pane for slots 1, 2, and 3.
 * Manages an isolated ExoPlayer instance, resource limits, and per-channel controls.
 */
@Composable
fun MultiViewSecondaryPlayerPane(
    slot: MultiViewSlot,
    isFocused: Boolean,
    isAudioActive: Boolean,
    onFocus: () -> Unit,
    onToggleAudio: () -> Unit,
    onClose: () -> Unit,
    onExpandFullscreen: () -> Unit,
    isExpandedFullscreen: Boolean = false,
    onOpenChannelPicker: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localResizeMode by remember { mutableIntStateOf(slot.resizeModeIndex) }
    var videoWidth by remember { mutableIntStateOf(1920) }
    var videoHeight by remember { mutableIntStateOf(1080) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isOverlayVisible by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var availableTracks by remember { mutableStateOf<androidx.media3.common.Tracks?>(null) }
    var currentQualityLabel by remember { mutableStateOf("تلقائي") }
    var showQualitySheet by remember { mutableStateOf(false) }
    var autoRetryCount by remember { mutableIntStateOf(0) }
    var isAutoRetrying by remember { mutableStateOf(false) }

    // Auto-hide HUD overlay after 5 seconds of inactivity
    LaunchedEffect(isOverlayVisible) {
        if (isOverlayVisible) {
            kotlinx.coroutines.delay(5000)
            isOverlayVisible = false
        }
    }

    val player = remember(context) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2_500,
                8_000,
                1_000,
                2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
        }

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
                // Secondary streams default to 720p/480p to prevent decoder exhaustion across multiple streams
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(1280, 720)
                    .build()
                volume = if (isAudioActive) 1f else 0f
                playWhenReady = true
            }
    }

    // Reactively update volume when audio focus shifts
    LaunchedEffect(isAudioActive) {
        player.volume = if (isAudioActive) 1f else 0f
    }

    // Media Source initialization with dynamic resolution & Ostora fallback
    LaunchedEffect(slot.streamUrl, slot.drmKeyId, slot.drmKey, reloadKey) {
        isLoading = true
        hasError = false
        try {
            player.stop()
            player.clearMediaItems()

            var targetStreamUrl = slot.streamUrl
            var targetKid = slot.drmKeyId
            var targetKey = slot.drmKey
            var targetAgent = slot.userAgent
            var targetRef = slot.referer

            if (targetStreamUrl.isBlank() || targetStreamUrl.contains("realid=") ||
                LiveGatewayStreamResolver.isTargetApiUrl(targetStreamUrl) ||
                OkRuStreamResolver.isOkRuUrl(targetStreamUrl) ||
                slot.channelId.startsWith("ok_") ||
                slot.channelId.startsWith("okru_") ||
                OstoraApiResolver.isOstoraUrl(targetStreamUrl) ||
                OstoraApiResolver.isKnownSportsChannel(slot.name) ||
                OstoraApiResolver.isKnownSportsChannel(slot.channelId) ||
                reloadKey > 0
            ) {
                try {
                    val resolvedCh = withContext(Dispatchers.IO) {
                        resolveMultiViewChannelStream(
                            Channel(
                                id = slot.channelId,
                                name = slot.name,
                                category = "",
                                logoUrl = slot.logoUrl,
                                streamUrl = slot.streamUrl,
                                userAgent = slot.userAgent,
                                referer = slot.referer,
                                drmKeyId = slot.drmKeyId,
                                drmKey = slot.drmKey,
                                groupId = slot.groupId,
                                isLive = slot.isLive
                            )
                        )
                    }
                    if (resolvedCh.streamUrl.isNotBlank()) {
                        targetStreamUrl = resolvedCh.streamUrl
                        targetKid = resolvedCh.drmKeyId ?: targetKid
                        targetKey = resolvedCh.drmKey ?: targetKey
                        targetAgent = resolvedCh.userAgent ?: targetAgent
                        targetRef = resolvedCh.referer ?: targetRef
                    }
                } catch (_: Exception) {}
            }

            val cleaned = OstoraApiResolver.cleanStreamUrl(targetStreamUrl.trim())
            val (cleanUrl, parsedKid, parsedKey) = OstoraApiResolver.parseDrmKeysFromUrl(cleaned)
            val effKid = parsedKid ?: targetKid
            val effKey = parsedKey ?: targetKey

            val streamLower = cleanUrl.lowercase()
            val effectiveUserAgent = when {
                !targetAgent.isNullOrBlank() && !targetAgent.contains("ExoPlayer", true) -> targetAgent
                else -> OstoraApiResolver.DEFAULT_USER_AGENT
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(effectiveUserAgent)
                .setKeepPostFor302Redirects(true)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(20000)
                .setReadTimeoutMs(20000)

            val effectiveRef = when {
                !targetRef.isNullOrBlank() -> targetRef
                streamLower.contains("edgenextcdn.net") || streamLower.contains("shahid") -> "https://shahid.mbc.net/"
                streamLower.contains("catiacast.video") || streamLower.contains("imn.iq") -> "https://imn.iq/"
                streamLower.contains("alrabiaa") -> "https://www.alrabiaa.tv/"
                else -> "https://ostora.tv/"
            }
            val defaultHeaders = mutableMapOf<String, String>()
            defaultHeaders["Referer"] = effectiveRef
            if (streamLower.contains("edgenextcdn.net") || streamLower.contains("shahid")) {
                defaultHeaders["Origin"] = "https://shahid.mbc.net"
            } else {
                defaultHeaders["Origin"] = "https://wecima.bar"
            }
            httpFactory.setDefaultRequestProperties(defaultHeaders)

            val ostoraFactory = OstoraStreamDataSource.Factory(upstreamFactory = httpFactory)
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, ostoraFactory)

            val uri = Uri.parse(cleanUrl)
            val isDash = cleanUrl.contains(".mpd", ignoreCase = true)
            val isTs = !isDash && (cleanUrl.endsWith(".ts", true) || cleanUrl.contains(".ts?") || cleanUrl.contains("format=ts"))
            val isHls = !isDash && !isTs

            var streamDrmManager: androidx.media3.exoplayer.drm.DrmSessionManager? = null
            if (!effKid.isNullOrBlank() && !effKey.isNullOrBlank()) {
                try {
                    val clearKeyBytes = buildClearKeyJson(effKid, effKey)
                    val callback = LocalMediaDrmCallback(clearKeyBytes)
                    streamDrmManager = DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                        .build(callback)
                } catch (_: Exception) {}
            }

            val mediaItemBuilder = when {
                isHls -> MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8)
                isDash -> MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_MPD)
                isTs -> MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.VIDEO_MP2T)
                else -> MediaItem.Builder().setUri(uri)
            }

            if (!effKid.isNullOrBlank() && !effKey.isNullOrBlank()) {
                mediaItemBuilder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).build()
                )
            }
            val mediaItem = mediaItemBuilder.build()

            val mediaSource = when {
                isHls -> {
                    val hlsParserFactory = SanitizedHlsPlaylistParserFactory()
                    val hlsFactory = HlsMediaSource.Factory(dataSourceFactory)
                        .setPlaylistParserFactory(hlsParserFactory)
                        .setAllowChunklessPreparation(false)
                    if (streamDrmManager != null) {
                        hlsFactory.setDrmSessionManagerProvider { streamDrmManager }
                    }
                    hlsFactory.createMediaSource(mediaItem)
                }
                isDash -> {
                    val dashFactory = DashMediaSource.Factory(dataSourceFactory)
                    if (streamDrmManager != null) {
                        dashFactory.setDrmSessionManagerProvider { streamDrmManager }
                    }
                    dashFactory.createMediaSource(mediaItem)
                }
                else -> {
                    androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }
            }

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
        } catch (_: Exception) {
            isLoading = false
            hasError = true
        }
    }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    isLoading = false
                    hasError = false
                    isAutoRetrying = false
                    autoRetryCount = 0
                } else if (state == androidx.media3.common.Player.STATE_BUFFERING) {
                    isLoading = true
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                availableTracks = tracks
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MultiViewSecondary", "Playback error in slot ${slot.slotId}: ${error.errorCodeName} (code=${error.errorCode})")
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }
                if (autoRetryCount < 2) {
                    autoRetryCount++
                    isAutoRetrying = true
                    isLoading = true
                    scope.launch {
                        kotlinx.coroutines.delay(1200)
                        reloadKey++
                    }
                    return
                }
                isLoading = false
                isAutoRetrying = false
                hasError = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    val aspectLabels = listOf("Fit", "Fill", "Zoom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) BeoutColors.BrightBlue else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                onFocus()
                isOverlayVisible = !isOverlayVisible
            }
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = when (localResizeMode) {
                        1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { view ->
                view.resizeMode = when (localResizeMode) {
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // BeoutTV Watermark overlay for secondary pane (covers Ostora logo on beIN Sports)
        if (shouldShowSlotWatermark(slot)) {
            MultiViewSlotWatermark(
                resizeModeIndex = localResizeMode,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading Spinner / Auto-retry Indicator
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = BeoutColors.BrightBlue,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    if (isAutoRetrying) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "جاري استعادة البث تلقائياً...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Error message & retry
        if (hasError && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF5252)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "تعذر تشغيل بث ${slot.name}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "قد يكون السيرفر مضغوطاً أو الرابط منتهي الصلاحية",
                        color = BeoutColors.TextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                autoRetryCount = 0
                                isAutoRetrying = false
                                reloadKey++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إعادة المحاولة", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        if (onOpenChannelPicker != null) {
                            OutlinedButton(
                                onClick = onOpenChannelPicker,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تبديل القناة", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Floating HUD Overlay
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.70f), Color.Transparent, Color.Black.copy(alpha = 0.70f))
                        )
                    )
                    .padding(8.dp)
            ) {
                // Top Header: Logo + Channel Name + Audio Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (slot.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = slot.logoUrl,
                                contentDescription = slot.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BeoutColors.ElevatedSurface)
                                    .padding(2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = slot.name,
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Audio Focus Badge / Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAudioActive) BeoutColors.BrightBlue.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, if (isAudioActive) BeoutColors.BrightBlue else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleAudio)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAudioActive) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isAudioActive) "صوت نشط" else "صامت",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom Floating Quick Toolbar
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality / Track Controller Button
                    SmallToolButton(
                        icon = Icons.Rounded.Tune,
                        label = currentQualityLabel,
                        onClick = { showQualitySheet = true }
                    )

                    // Aspect ratio quick cycle
                    SmallToolButton(
                        icon = Icons.Rounded.AspectRatio,
                        label = aspectLabels.getOrElse(localResizeMode) { "Fit" },
                        onClick = {
                            localResizeMode = (localResizeMode + 1) % 3
                            slot.resizeModeIndex = localResizeMode
                        }
                    )

                    // Reload stream
                    SmallToolButton(
                        icon = Icons.Rounded.Refresh,
                        label = null,
                        onClick = {
                            autoRetryCount = 0
                            isAutoRetrying = false
                            reloadKey++
                        }
                    )

                    // Add channel quick button (if slots < 4 and not expanded)
                    if (onOpenChannelPicker != null && !isExpandedFullscreen) {
                        SmallToolButton(
                            icon = Icons.Rounded.Add,
                            label = "قناة",
                            onClick = onOpenChannelPicker
                        )
                    }

                    // Fullscreen Maximize / Restore
                    SmallToolButton(
                        icon = if (isExpandedFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        label = if (isExpandedFullscreen) "عودة للشبكة" else null,
                        onClick = onExpandFullscreen
                    )

                    // Close (X)
                    SmallToolButton(
                        icon = Icons.Rounded.Close,
                        label = null,
                        tint = Color(0xFFFF5252),
                        onClick = onClose
                    )
                }
            }
        }

        // Slot Quality & Audio Track Controller Sheet
        if (showQualitySheet) {
            MultiViewSlotQualitySheet(
                slotIndex = slot.slotId,
                channelName = slot.name,
                logoUrl = slot.logoUrl,
                currentQualityLabel = currentQualityLabel,
                availableTracks = availableTracks,
                onDismiss = { showQualitySheet = false },
                onSelectQuality = { label, maxW, maxH ->
                    currentQualityLabel = label
                    if (maxW == Int.MAX_VALUE || maxH == Int.MAX_VALUE) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearVideoSizeConstraints()
                            .build()
                    } else {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setMaxVideoSize(maxW, maxH)
                            .build()
                    }
                },
                onSelectAudioTrack = { _, groupIndex, trackIndex ->
                    val audioGroups = availableTracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO }
                    if (audioGroups != null && groupIndex in audioGroups.indices) {
                        val group = audioGroups[groupIndex].mediaTrackGroup
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setOverrideForType(
                                androidx.media3.common.TrackSelectionOverride(group, listOf(trackIndex))
                            )
                            .build()
                    }
                }
            )
        }
    }
}

/**
 * Primary Player Wrapper for Slot 0 in Multi-View mode.
 * Gives Slot 0 the exact same visual frame, focus border, audio toggle, and controls as other slots.
 */
@Composable
fun MultiViewPrimaryPlayerWrapper(
    slot: MultiViewSlot,
    isFocused: Boolean,
    isAudioActive: Boolean,
    onFocus: () -> Unit,
    onToggleAudio: () -> Unit,
    onClose: () -> Unit,
    onExpandFullscreen: () -> Unit,
    onCycleAspect: () -> Unit,
    aspectLabel: String,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
    forceShowWatermark: Boolean = false,
    videoWidth: Int = 1920,
    videoHeight: Int = 1080,
    isExpandedFullscreen: Boolean = false,
    onOpenChannelPicker: (() -> Unit)? = null,
    qualityLabel: String? = null,
    onOpenQuality: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var isOverlayVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isOverlayVisible) {
        if (isOverlayVisible) {
            kotlinx.coroutines.delay(5000)
            isOverlayVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) BeoutColors.BrightBlue else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                onFocus()
                isOverlayVisible = !isOverlayVisible
            }
    ) {
        content()

        // BeoutTV Watermark overlay for primary pane (covers Ostora logo on beIN Sports)
        if (forceShowWatermark || shouldShowSlotWatermark(slot)) {
            MultiViewSlotWatermark(
                resizeModeIndex = slot.resizeModeIndex,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.70f), Color.Transparent, Color.Black.copy(alpha = 0.70f))
                        )
                    )
                    .padding(8.dp)
            ) {
                // Top Header: Logo + Channel Name + Audio Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (slot.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = slot.logoUrl,
                                contentDescription = slot.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BeoutColors.ElevatedSurface)
                                    .padding(2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = slot.name,
                            style = BeoutTextStyles.CardTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Audio Focus Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAudioActive) BeoutColors.BrightBlue.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, if (isAudioActive) BeoutColors.BrightBlue else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleAudio)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAudioActive) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isAudioActive) "صوت نشط" else "صامت",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Bottom Floating Quick Toolbar
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality selection button
                    if (onOpenQuality != null) {
                        SmallToolButton(
                            icon = Icons.Rounded.Tune,
                            label = qualityLabel ?: "الجودة",
                            onClick = onOpenQuality
                        )
                    }

                    SmallToolButton(
                        icon = Icons.Rounded.AspectRatio,
                        label = aspectLabel,
                        onClick = onCycleAspect
                    )

                    // Add channel quick button (if slots < 4 and not expanded)
                    if (onOpenChannelPicker != null && !isExpandedFullscreen) {
                        SmallToolButton(
                            icon = Icons.Rounded.Add,
                            label = "قناة",
                            onClick = onOpenChannelPicker
                        )
                    }

                    SmallToolButton(
                        icon = Icons.Rounded.Refresh,
                        label = null,
                        onClick = onReload
                    )

                    // Fullscreen Maximize / Restore
                    SmallToolButton(
                        icon = if (isExpandedFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        label = if (isExpandedFullscreen) "عودة للشبكة" else null,
                        onClick = onExpandFullscreen
                    )

                    SmallToolButton(
                        icon = Icons.Rounded.Close,
                        label = null,
                        tint = Color(0xFFFF5252),
                        onClick = onClose
                    )
                }
            }
        }
    }
}

/**
 * Layout Coordinator for Multi-View:
 * Handles Dual View (1x2), Triple View (2x2 with slot 4 invite card), and Quad View (2x2).
 */
@Composable
fun MultiViewGrid(
    slots: List<MultiViewSlot>,
    focusedSlotIndex: Int,
    audioSlotIndex: Int,
    expandedSlotIndex: Int? = null,
    onFocusSlot: (Int) -> Unit,
    onToggleAudioSlot: (Int) -> Unit,
    onCloseSlot: (Int) -> Unit,
    onExpandFullscreen: (Int) -> Unit,
    onExitFullscreen: () -> Unit = {},
    onCycleAspectSlot: (Int) -> Unit,
    aspectLabels: List<String>,
    onReloadSlot: (Int) -> Unit,
    onOpenChannelPicker: () -> Unit,
    primaryPlayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    forcePrimaryWatermark: Boolean = false,
    primaryVideoWidth: Int = 1920,
    primaryVideoHeight: Int = 1080,
    primaryQualityLabel: String? = null,
    onOpenQualitySlot: ((Int) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(4.dp)
    ) {
        if (expandedSlotIndex != null && expandedSlotIndex in slots.indices) {
            val expIdx = expandedSlotIndex
            // Render the expanded slot occupying the entire screen
            Box(modifier = Modifier.fillMaxSize()) {
                if (expIdx == 0) {
                    MultiViewPrimaryPlayerWrapper(
                        slot = slots[0],
                        isFocused = true,
                        isAudioActive = true,
                        isExpandedFullscreen = true,
                        onFocus = { onFocusSlot(0) },
                        onToggleAudio = { onToggleAudioSlot(0) },
                        onClose = { onCloseSlot(0) },
                        onExpandFullscreen = onExitFullscreen,
                        onCycleAspect = { onCycleAspectSlot(0) },
                        aspectLabel = aspectLabels.getOrElse(slots[0].resizeModeIndex) { "Fit" },
                        onReload = { onReloadSlot(0) },
                        onOpenChannelPicker = if (slots.size < 4) onOpenChannelPicker else null,
                        qualityLabel = primaryQualityLabel,
                        onOpenQuality = onOpenQualitySlot?.let { { it(0) } },
                        forceShowWatermark = forcePrimaryWatermark,
                        videoWidth = primaryVideoWidth,
                        videoHeight = primaryVideoHeight,
                        content = primaryPlayerContent
                    )
                } else {
                    MultiViewSecondaryPlayerPane(
                        slot = slots[expIdx],
                        isFocused = true,
                        isAudioActive = true,
                        isExpandedFullscreen = true,
                        onFocus = { onFocusSlot(expIdx) },
                        onToggleAudio = { onToggleAudioSlot(expIdx) },
                        onClose = { onCloseSlot(expIdx) },
                        onExpandFullscreen = onExitFullscreen,
                        onOpenChannelPicker = if (slots.size < 4) onOpenChannelPicker else null
                    )
                }

                // Floating Exit Fullscreen Chip at Top-Start (Returns to grid instantly)
                Surface(
                    onClick = onExitFullscreen,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0D111A),
                    border = BorderStroke(1.2.dp, BeoutColors.BrightBlue),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 14.dp)
                        .zIndex(1000f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = "Back to Multi-View",
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "عودة للبث المتعدد (${slots.size} قنوات)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Keep other secondary panes composed in a 0.dp box so they stay warm without resetting playback
            Box(modifier = Modifier.size(0.dp).zIndex(-1f)) {
                slots.forEachIndexed { i, s ->
                    if (i != expIdx && i > 0) {
                        MultiViewSecondaryPlayerPane(
                            slot = s,
                            isFocused = false,
                            isAudioActive = false,
                            onFocus = {},
                            onToggleAudio = {},
                            onClose = {},
                            onExpandFullscreen = {}
                        )
                    }
                }
            }
        } else {
            // Standard Multi-View Grid Layout
            when (slots.size) {
                2 -> {
                    // Dual View: Side-by-side 50% / 50%
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MultiViewPrimaryPlayerWrapper(
                                slot = slots[0],
                                isFocused = focusedSlotIndex == 0,
                                isAudioActive = audioSlotIndex == 0,
                                onFocus = { onFocusSlot(0) },
                                onToggleAudio = { onToggleAudioSlot(0) },
                                onClose = { onCloseSlot(0) },
                                onExpandFullscreen = { onExpandFullscreen(0) },
                                onCycleAspect = { onCycleAspectSlot(0) },
                                aspectLabel = aspectLabels.getOrElse(slots[0].resizeModeIndex) { "Fit" },
                                onReload = { onReloadSlot(0) },
                                onOpenChannelPicker = onOpenChannelPicker,
                                qualityLabel = primaryQualityLabel,
                                onOpenQuality = onOpenQualitySlot?.let { { it(0) } },
                                forceShowWatermark = forcePrimaryWatermark,
                                videoWidth = primaryVideoWidth,
                                videoHeight = primaryVideoHeight,
                                content = primaryPlayerContent
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MultiViewSecondaryPlayerPane(
                                slot = slots[1],
                                isFocused = focusedSlotIndex == 1,
                                isAudioActive = audioSlotIndex == 1,
                                onFocus = { onFocusSlot(1) },
                                onToggleAudio = { onToggleAudioSlot(1) },
                                onClose = { onCloseSlot(1) },
                                onExpandFullscreen = { onExpandFullscreen(1) },
                                onOpenChannelPicker = onOpenChannelPicker
                            )
                        }
                    }
                }

                3 -> {
                    // Triple View: 2x2 grid where the 4th cell is an "Add 4th Channel (+)" invitation card
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewPrimaryPlayerWrapper(
                                    slot = slots[0],
                                    isFocused = focusedSlotIndex == 0,
                                    isAudioActive = audioSlotIndex == 0,
                                    onFocus = { onFocusSlot(0) },
                                    onToggleAudio = { onToggleAudioSlot(0) },
                                    onClose = { onCloseSlot(0) },
                                    onExpandFullscreen = { onExpandFullscreen(0) },
                                    onCycleAspect = { onCycleAspectSlot(0) },
                                    aspectLabel = aspectLabels.getOrElse(slots[0].resizeModeIndex) { "Fit" },
                                    onReload = { onReloadSlot(0) },
                                    onOpenChannelPicker = onOpenChannelPicker,
                                    qualityLabel = primaryQualityLabel,
                                    onOpenQuality = onOpenQualitySlot?.let { { it(0) } },
                                    forceShowWatermark = forcePrimaryWatermark,
                                    videoWidth = primaryVideoWidth,
                                    videoHeight = primaryVideoHeight,
                                    content = primaryPlayerContent
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewSecondaryPlayerPane(
                                    slot = slots[1],
                                    isFocused = focusedSlotIndex == 1,
                                    isAudioActive = audioSlotIndex == 1,
                                    onFocus = { onFocusSlot(1) },
                                    onToggleAudio = { onToggleAudioSlot(1) },
                                    onClose = { onCloseSlot(1) },
                                    onExpandFullscreen = { onExpandFullscreen(1) },
                                    onOpenChannelPicker = onOpenChannelPicker
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewSecondaryPlayerPane(
                                    slot = slots[2],
                                    isFocused = focusedSlotIndex == 2,
                                    isAudioActive = audioSlotIndex == 2,
                                    onFocus = { onFocusSlot(2) },
                                    onToggleAudio = { onToggleAudioSlot(2) },
                                    onClose = { onCloseSlot(2) },
                                    onExpandFullscreen = { onExpandFullscreen(2) },
                                    onOpenChannelPicker = onOpenChannelPicker
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Cell 4: Add 4th Channel Card (+)
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewEmptySlotCard(onClick = onOpenChannelPicker)
                            }
                        }
                    }
                }

                4 -> {
                    // Quad View: 2x2 Grid with 4 concurrent streams
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewPrimaryPlayerWrapper(
                                    slot = slots[0],
                                    isFocused = focusedSlotIndex == 0,
                                    isAudioActive = audioSlotIndex == 0,
                                    onFocus = { onFocusSlot(0) },
                                    onToggleAudio = { onToggleAudioSlot(0) },
                                    onClose = { onCloseSlot(0) },
                                    onExpandFullscreen = { onExpandFullscreen(0) },
                                    onCycleAspect = { onCycleAspectSlot(0) },
                                    aspectLabel = aspectLabels.getOrElse(slots[0].resizeModeIndex) { "Fit" },
                                    onReload = { onReloadSlot(0) },
                                    onOpenChannelPicker = null,
                                    qualityLabel = primaryQualityLabel,
                                    onOpenQuality = onOpenQualitySlot?.let { { it(0) } },
                                    forceShowWatermark = forcePrimaryWatermark,
                                    videoWidth = primaryVideoWidth,
                                    videoHeight = primaryVideoHeight,
                                    content = primaryPlayerContent
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewSecondaryPlayerPane(
                                    slot = slots[1],
                                    isFocused = focusedSlotIndex == 1,
                                    isAudioActive = audioSlotIndex == 1,
                                    onFocus = { onFocusSlot(1) },
                                    onToggleAudio = { onToggleAudioSlot(1) },
                                    onClose = { onCloseSlot(1) },
                                    onExpandFullscreen = { onExpandFullscreen(1) },
                                    onOpenChannelPicker = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewSecondaryPlayerPane(
                                    slot = slots[2],
                                    isFocused = focusedSlotIndex == 2,
                                    isAudioActive = audioSlotIndex == 2,
                                    onFocus = { onFocusSlot(2) },
                                    onToggleAudio = { onToggleAudioSlot(2) },
                                    onClose = { onCloseSlot(2) },
                                    onExpandFullscreen = { onExpandFullscreen(2) },
                                    onOpenChannelPicker = null
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MultiViewSecondaryPlayerPane(
                                    slot = slots[3],
                                    isFocused = focusedSlotIndex == 3,
                                    isAudioActive = audioSlotIndex == 3,
                                    onFocus = { onFocusSlot(3) },
                                    onToggleAudio = { onToggleAudioSlot(3) },
                                    onClose = { onCloseSlot(3) },
                                    onExpandFullscreen = { onExpandFullscreen(3) },
                                    onOpenChannelPicker = null
                                )
                            }
                        }
                    }
                }
            }

            // Permanent Floating Pill: Add Channel (+), always available when slots < 4
            if (slots.size < 4) {
                Surface(
                    onClick = onOpenChannelPicker,
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xEE0D121D),
                    border = BorderStroke(1.2.dp, BeoutColors.BrightBlue),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .tvFocusable(shape = RoundedCornerShape(22.dp), onEnter = onOpenChannelPicker)
                        .zIndex(500f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = "Add Channel",
                            tint = BeoutColors.BrightBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "إضافة قناة (${slots.size}/4)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Placeholder card displayed in the 4th slot of a 3-channel grid, inviting the user to add a 4th stream.
 */
@Composable
fun MultiViewEmptySlotCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F141F))
            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                    .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Channel",
                    tint = BeoutColors.BrightBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "إضافة قناة رابعة للبث المتعدد",
                style = BeoutTextStyles.CardTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = "انقر لاختيار قناة ومشاهدة 4 مباريات معاً",
                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                color = BeoutColors.TextSecondary
            )
        }
    }
}

/**
 * Channel Selection Bottom Sheet for Multi-View.
 * Allows users to browse categories (beIN, Alkass, SSC, etc.), search, and add channels to the active grid.
 */
@Composable
fun MultiViewChannelPickerSheet(
    activeSlots: List<MultiViewSlot>,
    onChannelSelected: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var allChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isResolvingChannelId by remember { mutableStateOf<String?>(null) }

    // Preload channels list from repository
    LaunchedEffect(Unit) {
        try {
            val channels = withContext(Dispatchers.IO) {
                ServiceLocator.repository.getChannels("All")
            }
            allChannels = channels
        } catch (_: Exception) {}
    }

    val bouquets = remember(allChannels) {
        listOf(
            "All" to "جميع القنوات",
            "قنوات بي ان سبورت" to "بي ان سبورت",
            "قنوات SSC" to "قنوات SSC",
            "قنوات ثمانية" to "قنوات ثمانية",
            "قنوات ألوان الرياضية" to "ألوان الرياضية",
            "قنوات ON SPORTS والرياضة المصرية" to "أون سبورت",
            "المفضلة" to "المفضلة"
        )
    }

    val filteredChannels = remember(allChannels, selectedCategory, searchQuery) {
        allChannels.filter { channel ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "المفضلة" -> channel.isFavorite
                else -> channel.category.contains(selectedCategory, ignoreCase = true) ||
                        selectedCategory.contains(channel.category, ignoreCase = true) ||
                        (selectedCategory.contains("بي ان") && (channel.name.contains("bein", true) || channel.name.contains("بي ان") || channel.name.contains("بين"))) ||
                        (selectedCategory.contains("SSC") && (channel.name.contains("ssc", true))) ||
                        (selectedCategory.contains("ثمانية") && (channel.name.contains("ثمانية") || channel.name.contains("thmanyah", true)))
            }

            val matchesQuery = searchQuery.isBlank() ||
                    channel.name.contains(searchQuery, ignoreCase = true) ||
                    channel.category.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.70f))
            .clickable(onClick = onDismiss)
            .padding(top = 20.dp, start = 40.dp, end = 40.dp, bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BeoutColors.Card,
            border = BorderStroke(1.dp, BeoutColors.Divider),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = BeoutColors.BrightBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "إضافة قناة للبث المتعدد (Multi-View)",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "اختر قناة لمشاهدتها بالتزامن مع باقي القنوات (حد أقصى 4 قنوات)",
                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                color = BeoutColors.TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Slots Counter Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${activeSlots.size} / 4 قنوات نشطة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BeoutColors.BrightBlue
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search Box & Category Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن قناة...", fontSize = 12.sp, color = BeoutColors.TextDisabled) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = BeoutColors.TextSecondary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeoutColors.BrightBlue,
                            unfocusedBorderColor = BeoutColors.Divider,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = BeoutColors.BrightBlue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .height(44.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    // Categories Horizontal List
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bouquets.forEach { (key, label) ->
                            val isSelected = selectedCategory == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BeoutColors.BrightBlue else Color(0xFF161B26))
                                    .border(1.dp, if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider, RoundedCornerShape(8.dp))
                                    .tvFocusable(shape = RoundedCornerShape(8.dp), onEnter = { selectedCategory = key })
                                    .clickable { selectedCategory = key }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else BeoutColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Channels Grid
                if (filteredChannels.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد قنوات مطابقة للبحث أو الباقة المحددة",
                            style = BeoutTextStyles.Caption,
                            color = BeoutColors.TextDisabled
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(filteredChannels, key = { it.id }) { ch ->
                            val isAlreadyActive = activeSlots.any { it.channelId == ch.id || it.name == ch.name }
                            val isResolvingThis = isResolvingChannelId == ch.id

                            val selectChannelAction = {
                                if (!isAlreadyActive && !isResolvingThis) {
                                    isResolvingChannelId = ch.id
                                    coroutineScope.launch {
                                        val resolved = resolveMultiViewChannelStream(ch)
                                        isResolvingChannelId = null
                                        onChannelSelected(resolved)
                                        onDismiss()
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAlreadyActive) Color(0xFF0F141F) else Color(0xFF151924),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAlreadyActive) BeoutColors.BrightBlue.copy(alpha = 0.3f) else BeoutColors.Divider
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(shape = RoundedCornerShape(10.dp), onEnter = selectChannelAction)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isAlreadyActive && !isResolvingThis, onClick = selectChannelAction)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Logo Box
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0D0F14))
                                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (ch.logoUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = ch.logoUrl,
                                                contentDescription = ch.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.name,
                                            style = BeoutTextStyles.CardTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (isAlreadyActive) Color.White.copy(alpha = 0.5f) else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (isAlreadyActive) {
                                            Text(
                                                text = "معروضة حالياً",
                                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = BeoutColors.BrightBlue
                                            )
                                        } else if (isResolvingThis) {
                                            Text(
                                                text = "جاري التحضير...",
                                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                                                color = BeoutColors.BrightBlue
                                            )
                                        } else {
                                            Text(
                                                text = ch.category.take(18),
                                                style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
                                                color = BeoutColors.TextDisabled,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (isResolvingThis) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = BeoutColors.BrightBlue,
                                            strokeWidth = 2.dp
                                        )
                                    } else if (isAlreadyActive) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Active",
                                            tint = BeoutColors.BrightBlue,
                                            modifier = Modifier.size(16.dp)
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
}

/**
 * Top notification and status banner for Multi-View mode.
 */
@Composable
fun MultiViewControlBar(
    slots: List<MultiViewSlot>,
    focusedSlotIndex: Int,
    audioSlotIndex: Int,
    onFocusSlot: (Int) -> Unit,
    onToggleAudioSlot: (Int) -> Unit,
    onOpenAddChannel: () -> Unit,
    onExitMultiView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Multi-View status chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF00E676), CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "بث متعدد نشط (${slots.size}/4)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Channel Audio Switcher Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slots.forEachIndexed { index, slot ->
                val isAudioActive = audioSlotIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAudioActive) BeoutColors.BrightBlue else Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, if (isAudioActive) BeoutColors.BrightBlue else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        .tvFocusable(shape = RoundedCornerShape(6.dp), onEnter = { onToggleAudioSlot(index) })
                        .clickable { onToggleAudioSlot(index) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAudioActive) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "قناة ${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = if (isAudioActive) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }

            // Add Channel button (if < 4)
            if (slots.size < 4) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BeoutColors.BrightBlue.copy(alpha = 0.2f))
                        .border(1.dp, BeoutColors.BrightBlue, RoundedCornerShape(6.dp))
                        .tvFocusable(shape = RoundedCornerShape(6.dp), onEnter = onOpenAddChannel)
                        .clickable(onClick = onOpenAddChannel)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("إضافة قناة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BeoutColors.BrightBlue)
                    }
                }
            }

            // Exit Multi-View
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF261014))
                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .tvFocusable(shape = RoundedCornerShape(6.dp), onEnter = onExitMultiView)
                    .clickable(onClick = onExitMultiView)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FullscreenExit, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("إنهاء", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                }
            }
        }
    }
}

@Composable
private fun SmallToolButton(
    icon: ImageVector,
    label: String? = null,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .tvFocusable(shape = RoundedCornerShape(6.dp), onEnter = onClick)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            if (!label.isNullOrBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Dedicated, per-slot Quality and Audio Track Controller Sheet for Multi-View mode.
 * Gives each channel pane independent control over its resolution, adaptive engine, and commentary audio.
 */
@Composable
fun MultiViewSlotQualitySheet(
    slotIndex: Int,
    channelName: String,
    logoUrl: String,
    currentQualityLabel: String,
    availableTracks: androidx.media3.common.Tracks?,
    onSelectQuality: (label: String, maxW: Int, maxH: Int) -> Unit,
    onSelectAudioTrack: ((label: String, groupIdx: Int, trackIdx: Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E131F),
            border = BorderStroke(1.2.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 440.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = channelName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BeoutColors.ElevatedSurface)
                                    .padding(2.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = "إعدادات البث • نافذة ${slotIndex + 1}",
                                style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = channelName,
                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                color = BeoutColors.BrightBlue
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = BeoutColors.Divider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "دقة الفيديو ومحرك التكيف:",
                        style = BeoutTextStyles.Caption.copy(
                            color = BeoutColors.BrightBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // 1. Auto Quality
                    SlotQualityOptionRow(
                        title = "تلقائي (Auto)",
                        subtitle = "محرك التكيف التلقائي لأفضل سلاسة حسب سرعة اتصالك",
                        isSelected = currentQualityLabel.contains("Auto") || currentQualityLabel.contains("تلقائي"),
                        onClick = {
                            onSelectQuality("تلقائي", Int.MAX_VALUE, Int.MAX_VALUE)
                            onDismiss()
                        }
                    )

                    // 2. Dynamic Video tracks from stream
                    val dynamicQualities = mutableListOf<Triple<String, Int, Int>>()
                    availableTracks?.groups?.forEach { group ->
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val w = format.width
                                val h = format.height
                                if (w > 0 && h > 0) {
                                    val bitrateStr = if (format.bitrate > 0) {
                                        if (format.bitrate >= 1_000_000) String.format(" • %.1f Mbps", format.bitrate / 1_000_000f)
                                        else " • ${format.bitrate / 1000} kbps"
                                    } else ""
                                    val label = when {
                                        h >= 2160 -> "4K Ultra HD (${w}x${h})$bitrateStr"
                                        h >= 1080 -> "1080p FHD (${w}x${h})$bitrateStr"
                                        h >= 720 -> "720p HD (${w}x${h})$bitrateStr"
                                        h >= 480 -> "480p SD (${w}x${h})$bitrateStr"
                                        h >= 360 -> "360p (${w}x${h})$bitrateStr"
                                        else -> "${h}p (${w}x${h})$bitrateStr"
                                    }
                                    if (dynamicQualities.none { it.second == w && it.third == h }) {
                                        dynamicQualities.add(Triple(label, w, h))
                                    }
                                }
                            }
                        }
                    }

                    if (dynamicQualities.isEmpty()) {
                        // Standard fallback presets
                        dynamicQualities.add(Triple("1080p FHD (أعلى دقة فائقة)", 1920, 1080))
                        dynamicQualities.add(Triple("720p HD (دقة عالية وسريعة)", 1280, 720))
                        dynamicQualities.add(Triple("480p SD (توفير البيانات)", 854, 480))
                        dynamicQualities.add(Triple("360p (أقصى توفير للباقة)", 640, 360))
                    }

                    dynamicQualities.forEach { (label, w, h) ->
                        val isSelected = currentQualityLabel.startsWith(label.take(5)) || currentQualityLabel == label
                        val subtitle = when {
                            label.contains("1080") -> "أعلى دقة نقية وتفاصيل فائقة"
                            label.contains("720") -> "دقة عالية متوازنة وسريعة الاستجابة"
                            label.contains("480") -> "توفير البيانات ومنع التقطيع على الشبكات العادية"
                            label.contains("360") -> "أقصى توفير للباقة والشبكات الضعيفة"
                            else -> null
                        }

                        SlotQualityOptionRow(
                            title = label,
                            subtitle = subtitle,
                            isSelected = isSelected,
                            onClick = {
                                val shortLabel = when {
                                    h >= 1080 -> "1080p"
                                    h >= 720 -> "720p"
                                    h >= 480 -> "480p"
                                    h >= 360 -> "360p"
                                    else -> "${h}p"
                                }
                                onSelectQuality(shortLabel, w, h)
                                onDismiss()
                            }
                        )
                    }

                    // Audio Tracks (if multiple)
                    val audioGroups = availableTracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList()
                    val totalAudioTracks = audioGroups.sumOf { it.length }
                    if (totalAudioTracks > 1 && onSelectAudioTrack != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "المسارات الصوتية والتعليق:",
                            style = BeoutTextStyles.Caption.copy(
                                color = BeoutColors.BrightBlue,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        audioGroups.forEachIndexed { gIdx, group ->
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val lang = format.language?.uppercase() ?: ""
                                val trackLabel = format.label ?: (if (lang.isNotBlank()) "صوت ($lang)" else "مسار صوتي ${i + 1}")
                                val isTrackSelected = group.isTrackSelected(i)

                                SlotQualityOptionRow(
                                    title = trackLabel,
                                    subtitle = if (format.channelCount == 6) "5.1 Surround" else "Stereo",
                                    isSelected = isTrackSelected,
                                    onClick = {
                                        onSelectAudioTrack(trackLabel, gIdx, i)
                                        onDismiss()
                                    }
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

@Composable
private fun SlotQualityOptionRow(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(8.dp), onEnter = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.20f) else Color(0xFF131A26))
            .border(
                width = 1.dp,
                color = if (isSelected) BeoutColors.BrightBlue else Color(0x1FFFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BeoutTextStyles.SecondaryInfo.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BeoutColors.BrightBlue else Color.White
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = BeoutTextStyles.Caption.copy(
                        color = BeoutColors.TextSecondary,
                        fontSize = 10.sp
                    )
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = BeoutColors.BrightBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
