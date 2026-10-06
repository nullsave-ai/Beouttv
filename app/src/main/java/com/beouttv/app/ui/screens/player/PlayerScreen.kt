package com.beouttv.app.ui.screens.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.view.ViewGroup
import com.beouttv.app.ui.components.InAppAdOverlay
import com.beouttv.app.ui.components.FloatingCircleButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.beouttv.app.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import java.io.File
import androidx.annotation.OptIn
import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener

import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.beouttv.app.data.api.CoreApiClient
import com.beouttv.app.data.config.PlayerPreferences
import com.beouttv.app.data.config.SubtitleSizePreset
import com.beouttv.app.data.config.SubtitleStylePreset
import com.beouttv.app.data.history.PlaybackHistoryManager
import com.beouttv.app.data.i18n.S
import com.beouttv.app.data.i18n.tr
import com.beouttv.app.data.model.StreamServer
import com.beouttv.app.data.model.SubtitleTrack
import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.repository.ServiceLocator
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.core.tv.TvRemoteAction
import com.beouttv.app.core.tv.TvRemoteController
import com.beouttv.app.ui.screens.player.tv.TvQuickChannelDrawer
import com.beouttv.app.ui.screens.player.tv.TvRemoteShortcutsLegend
import com.beouttv.app.ui.theme.tv.tvFocusable
import com.beouttv.app.data.source.stream.OstoraApiResolver
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.theme.BeoutShapes
import com.beouttv.app.ui.theme.BeoutTextStyles
import com.beouttv.app.ui.theme.BeoutTvTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val EXTRA_STREAM_URL = "extra_stream_url"
const val EXTRA_TITLE = "extra_title"
const val EXTRA_MEDIA_ID = "extra_media_id"
const val EXTRA_POSTER_URL = "extra_poster_url"
const val EXTRA_BACKDROP_URL = "extra_backdrop_url"
const val EXTRA_SEEK_POSITION = "extra_seek_position"
const val EXTRA_SUBTITLE_URL = "extra_subtitle_url"
const val EXTRA_SUBTITLE_LABEL = "extra_subtitle_label"
const val EXTRA_USER_AGENT = "extra_user_agent"
const val EXTRA_REFERER = "extra_referer"
const val EXTRA_ORIGIN = "extra_origin"
const val EXTRA_COOKIE = "extra_cookie"
const val EXTRA_HEADERS_JSON = "extra_headers_json"
const val EXTRA_DRM_KEY_ID = "extra_drm_key_id"
const val EXTRA_DRM_KEY = "extra_drm_key"
const val EXTRA_DRM_LICENSE_URL = "extra_drm_license_url"

const val EXTRA_SUBTITLES_JSON = "extra_subtitles_json"
const val EXTRA_SERVERS_JSON = "extra_servers_json"
const val EXTRA_IS_LIVE = "extra_is_live"
const val EXTRA_GROUP_ID = "extra_group_id"

private fun shiftSrtTimestamps(text: String, offsetMs: Long): String {
    if (offsetMs == 0L || text.isBlank()) return text
    val pattern = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""")
    return pattern.replace(text) { match ->
        val g = match.groupValues
        fun parseMs(h: String, m: String, s: String, ms: String): Long {
            return h.toLong() * 3600000L + m.toLong() * 60000L + s.toLong() * 1000L + ms.toLong()
        }
        fun formatMs(totalMs: Long): String {
            val clamped = maxOf(0L, totalMs)
            val h = clamped / 3600000L
            val m = (clamped % 3600000L) / 60000L
            val s = (clamped % 60000L) / 1000L
            val ms = clamped % 1000L
            return String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d", h, m, s, ms)
        }
        val start = parseMs(g[1], g[2], g[3], g[4]) + offsetMs
        val end = parseMs(g[5], g[6], g[7], g[8]) + offsetMs
        "${formatMs(start)} --> ${formatMs(end)}"
    }
}

enum class PlayerSheetType {
    QUALITY,
    AUDIO_SUBTITLES,
    ASPECT_RATIO,
    SERVERS
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    mediaId: String = streamUrl,
    posterUrl: String = "",
    backdropUrl: String = "",
    seekPosition: Long = -1L,
    subtitles: List<com.beouttv.app.data.model.SubtitleTrack> = emptyList(),
    servers: List<com.beouttv.app.data.model.StreamServer> = emptyList(),
    subtitleUrl: String? = null,
    subtitleLabel: String? = null,
    userAgent: String? = null,
    referer: String? = null,
    origin: String? = null,
    cookie: String? = null,
    headersJson: String? = null,
    drmKeyId: String? = null,
    drmKey: String? = null,
    drmLicenseUrl: String? = null,
    groupId: String? = null,
    isLive: Boolean = false,
    remoteActionFlow: kotlinx.coroutines.flow.SharedFlow<com.beouttv.app.core.tv.TvRemoteAction>? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Check if media belongs to an expired Xtream account
    val (isExpiredAccount, expiredAccountName) = remember(mediaId, streamUrl) {
        com.beouttv.app.data.xtream.XtreamAccountManager.isMediaExpired(mediaId.ifBlank { streamUrl })
    }

    if (isExpiredAccount) {
        androidx.activity.compose.BackHandler { onBack() }
        var showXtreamDialogFromPlayer by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BeoutColors.Background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(BeoutShapes.Large)
                    .background(BeoutColors.Card)
                    .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f), BeoutShapes.Large)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "انتهى اشتراكك في الاكستريم: ${expiredAccountName ?: "الحساب الشخصي"}",
                        style = BeoutTextStyles.ScreenTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = "عذراً، انتهت فترة صلاحية هذا الاشتراك الشخصي ولا يمكن تشغيل البث. يرجى تجديد اشتراكك لدى مزود الخدمة أو إدارة حساباتك في الإعدادات.",
                        style = BeoutTextStyles.SecondaryInfo.copy(fontSize = 13.sp),
                        color = BeoutColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.ElevatedSurface),
                            shape = BeoutShapes.Medium,
                            modifier = Modifier.tvFocusable(shape = BeoutShapes.Medium)
                        ) {
                            Text(S.back(), color = BeoutColors.TextPrimary, style = BeoutTextStyles.ButtonLabel)
                        }

                        Button(
                            onClick = { showXtreamDialogFromPlayer = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                            shape = BeoutShapes.Medium,
                            modifier = Modifier.tvFocusable(shape = BeoutShapes.Medium)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إدارة الاشتراكات", color = Color.White, style = BeoutTextStyles.ButtonLabel)
                        }
                    }
                }
            }
        }

        if (showXtreamDialogFromPlayer) {
            com.beouttv.app.ui.screens.settings.XtreamManagerDialog(
                onDismiss = { showXtreamDialogFromPlayer = false }
            )
        }
        return
    }

    val isTvDevice = remember { DeviceTypeDetector.isTvDevice(context) }
    var showTvChannelDrawer by remember { mutableStateOf(false) }
    var availableTvChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val list = withContext(Dispatchers.IO) {
                ServiceLocator.repository.getChannels("All")
            }
            availableTvChannels = list
        } catch (_: Exception) {}
    }

    var activeStreamUrl by remember { mutableStateOf(streamUrl) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var webEmbedPlaying by remember { mutableStateOf(true) }
    val isWebEmbed = remember(activeStreamUrl) {
        val lower = activeStreamUrl.lowercase()
        lower.contains("vidlink.pro") ||
        lower.contains("videasy.to") ||
        lower.contains("zxcstream") ||
        lower.contains("2embed") ||
        (lower.contains("/embed") && !lower.contains(".m3u8") && !lower.contains(".mp4"))
    }
    var controlsVisible by remember { mutableStateOf(true) }
    var webEmbedControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(webEmbedControlsVisible, isWebEmbed) {
        if (isWebEmbed && webEmbedControlsVisible) {
            kotlinx.coroutines.delay(3500)
            webEmbedControlsVisible = false
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var hasRenderedFirstFrame by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Read initial preferences
    val initialAspectMode = remember { PlayerPreferences.getAspectRatioMode(context) }
    var resizeModeIndex by remember {
        mutableIntStateOf(
            when (initialAspectMode) {
                "Fill" -> 1
                "Zoom" -> 2
                else -> 0
            }
        )
    }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isLiveStream by remember { mutableStateOf(isLive) }

    // Dynamic Subtitles list that updates in real time
    val activeSubtitles = remember {
        mutableStateListOf<com.beouttv.app.data.model.SubtitleTrack>().apply {
            val list = subtitles.toMutableList()
            if (list.none { it.language == "ar" } && !subtitleUrl.isNullOrBlank()) {
                list.add(
                    0,
                    com.beouttv.app.data.model.SubtitleTrack(
                        id = "sub_main",
                        language = "ar",
                        label = subtitleLabel ?: "العربية",
                        url = subtitleUrl,
                        format = if (subtitleUrl.endsWith(".srt", true)) "srt" else "vtt"
                    )
                )
            }
            val sorted = list.sortedWith(
                compareByDescending<com.beouttv.app.data.model.SubtitleTrack> { it.language == "ar" && it.label.contains("MovieBox") }
                    .thenByDescending { it.language == "ar" }
                    .thenByDescending { it.label.contains("MovieBox") }
            )
            addAll(sorted)
        }
    }

    // Pre-resolve and sanitize all remote external subtitles (e.g. MovieBox CP-1256 SRTs) to clean UTF-8 local files
    LaunchedEffect(subtitles) {
        withContext(Dispatchers.IO) {
            subtitles.forEach { sub ->
                if (sub.url.startsWith("http") && com.beouttv.app.data.subtitle.SubtitleCharsetHelper.isSubtitleUrl(sub.url)) {
                    val cleanUri = com.beouttv.app.data.subtitle.SubtitleCharsetHelper.resolveCleanSubtitleUri(context, sub.url)
                    if (cleanUri.startsWith("file://") && cleanUri != sub.url) {
                        withContext(Dispatchers.Main) {
                            val idx = activeSubtitles.indexOfFirst { it.id == sub.id || it.url == sub.url }
                            if (idx != -1) {
                                activeSubtitles[idx] = activeSubtitles[idx].copy(url = cleanUri)
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // [OSTORA ENGINE & STREAM IDENTIFICATION]
    // التحقق الدقيق من قنوات وسيرفرات الأسطورة الرياضية الموثوقة 100%
    // =========================================================================
    val isOstoraStream = remember(groupId, streamUrl, mediaId, title) {
        !groupId.isNullOrBlank() ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isOstoraUrl(streamUrl) ||
                mediaId.startsWith("ostora") ||
                mediaId.startsWith("sp_") ||
                mediaId.startsWith("ent_") ||
                mediaId.startsWith("osn_") ||
                streamUrl.startsWith("412<F>") ||
                streamUrl.startsWith("413<F>") ||
                streamUrl.startsWith("414<F>") ||
                streamUrl.contains("<F>", ignoreCase = true) ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(mediaId) ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(title) ||
                title.contains("ألوان") || title.contains("الوان") || title.contains("alwan", true) ||
                title.contains("osn", true) || title.contains("bein", true) || title.contains("بي ان") ||
                title.contains("بى ان") || title.contains("ssc", true) || title.contains("كأس") ||
                title.contains("alkass", true) || title.contains("ثمانية") || title.contains("thmanyah", true)
    }

    var lastTokenRefreshUrl by remember { mutableStateOf("") }
    var lastTokenRefreshTime by remember { mutableLongStateOf(0L) }

    val isBeinSportsChannel = remember(title, mediaId, streamUrl, groupId) {
        val t = title.lowercase(java.util.Locale.ROOT)
        val m = mediaId.lowercase(java.util.Locale.ROOT)
        val u = streamUrl.lowercase(java.util.Locale.ROOT)
        val g = groupId ?: ""

        val isOtherChannel = t.contains("ثمانية") || t.contains("thmanyah") ||
                t.contains("أون") || t.contains("اون") || t.contains("ontime") || t.contains("on sport") ||
                t.contains("ssc") || t.contains("alkass") || t.contains("كاس") || t.contains("كأس") ||
                t.contains("أبوظبي") || t.contains("ابوظبي") || t.contains("ad sport") ||
                t.contains("دبي") || t.contains("dubai") || t.contains("mbc") || t.contains("rotana") ||
                t.contains("قنوات عربية") || t.contains("الأخبار") || t.contains("اخبار") ||
                m.contains("thmanyah") || m.contains("ontime") || m.contains("onsport") || m.contains("ssc") ||
                m.contains("alkass") || m.contains("mbc") || m.contains("rotana")

        if (isOtherChannel) {
            false
        } else {
            val hasBeinTitle = t.contains("bein") ||
                    t.contains("بي ان") ||
                    t.contains("بي إن") ||
                    t.contains("بين سبورت") ||
                    (t.contains("بين") && (t.contains("سبورت") || t.any { it.isDigit() }))

            val hasBeinIdOrUrl = m.contains("bein") || u.contains("bein")

            val isAlwanBein = (t.contains("ألوان") || t.contains("الوان") || t.contains("alwan") ||
                    m.contains("alwan") || m.startsWith("sp_alwan") || g == "7193") &&
                    (hasBeinTitle || hasBeinIdOrUrl || u.contains("maziika") || t.contains("سبورت") || t.any { it.isDigit() })

            hasBeinTitle || hasBeinIdOrUrl || isAlwanBein
        }
    }

    // Settings & Server States
    var activeSheet by remember { mutableStateOf<PlayerSheetType?>(null) }
    var selectedQualityLabel by remember { mutableStateOf("تلقائي (Auto)") }
    var selectedAudioLabel by remember { mutableStateOf("الصوت الافتراضي (عربي / أصلي)") }
    val defaultArabicSub = remember {
        activeSubtitles.firstOrNull { it.language == "ar" && it.label.contains("MovieBox") }
            ?: activeSubtitles.firstOrNull { it.language == "ar" }
            ?: activeSubtitles.firstOrNull()
    }
    var selectedSubtitleLabel by remember {
        mutableStateOf(
            if (defaultArabicSub != null) "${defaultArabicSub.label} (${defaultArabicSub.language.uppercase()})"
            else "إيقاف الترجمة (Off)"
        )
    }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var selectedSubtitleTrackObj by remember { mutableStateOf<SubtitleTrack?>(defaultArabicSub) }

    // Multi-Server state: holds all authentic available servers for this channel
    var activeServersList by remember(servers) { mutableStateOf(servers) }
    var selectedServerId by remember { mutableStateOf("") }
    var isResolvingServers by remember { mutableStateOf(false) }

    var availableTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    var videoWidth by remember { mutableIntStateOf(1920) }
    var videoHeight by remember { mutableIntStateOf(1080) }

    val shouldShowWatermark = remember(isBeinSportsChannel) {
        isBeinSportsChannel
    }

    val isInPipMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        (context as? android.app.Activity)?.isInPictureInPictureMode == true
    } else {
        false
    }

    val defaultHeaders = remember(referer, origin, cookie, headersJson, isOstoraStream, activeStreamUrl) {
        val headers = mutableMapOf<String, String>()
        val streamLower = activeStreamUrl.lowercase()
        val isMovieBoxStream = streamLower.contains("hakunaymatata") ||
                streamLower.contains("aoneroom") ||
                streamLower.contains("shalltry") ||
                streamLower.contains("moviebox") ||
                streamLower.contains("wefeed")

        val effectiveRef = when {
            !referer.isNullOrBlank() -> referer
            isMovieBoxStream -> null
            streamLower.contains("edgenextcdn.net") || streamLower.contains("shahid") -> "https://shahid.mbc.net/"
            streamLower.contains("catiacast.video") || streamLower.contains("imn.iq") -> "https://imn.iq/"
            streamLower.contains("alrabiaa") -> "https://www.alrabiaa.tv/"
            streamLower.contains("ok.ru") || streamLower.contains("okcdn.ru") || streamLower.contains("mycdn.me") || streamLower.contains("odnoklassniki") -> com.beouttv.app.data.source.stream.OkRuStreamResolver.DEFAULT_REFERER
            isOstoraStream -> "https://ostora.tv/"
            else -> "https://wecima.bar/"
        }
        if (effectiveRef != null) {
            headers["Referer"] = effectiveRef
        }

        if (!origin.isNullOrBlank()) {
            headers["Origin"] = origin
        } else if (streamLower.contains("edgenextcdn.net") || streamLower.contains("shahid")) {
            headers["Origin"] = "https://shahid.mbc.net"
        } else if (!isOstoraStream && !isMovieBoxStream) {
            headers["Origin"] = "https://wecima.bar"
        }

        if (!cookie.isNullOrBlank()) {
            headers["Cookie"] = cookie
        }
        if (!headersJson.isNullOrBlank()) {
            try {
                val json = org.json.JSONObject(headersJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    headers[k] = json.optString(k, "")
                }
            } catch (_: Exception) {}
        }
        headers
    }

    val customAgent = remember(userAgent, isOstoraStream, activeStreamUrl) {
        val streamLower = activeStreamUrl.lowercase()
        when {
            streamLower.contains("ok.ru") || streamLower.contains("okcdn.ru") || streamLower.contains("mycdn.me") || streamLower.contains("odnoklassniki") -> {
                if (!userAgent.isNullOrBlank() && !userAgent.contains("ExoPlayer", true)) {
                    userAgent
                } else {
                    com.beouttv.app.data.source.stream.OkRuStreamResolver.DEFAULT_USER_AGENT
                }
            }
            isOstoraStream -> {
                if (!userAgent.isNullOrBlank() && !userAgent.contains("ExoPlayer", true)) {
                    userAgent
                } else {
                    com.beouttv.app.data.source.stream.OstoraApiResolver.DEFAULT_USER_AGENT
                }
            }
            !userAgent.isNullOrBlank() && !userAgent.contains("ExoPlayer", true) -> userAgent
            else -> "2a8ade1f412ed55f409cf8b31d3466df"
        }
    }

    val isSlowNetPref = remember { PlayerPreferences.isSlowInternetModeEnabled(context) }
    var isSlowNetMode by remember { mutableStateOf(isSlowNetPref) }
    val renderEngine = remember { PlayerPreferences.getRenderMode(context) }
    var showStatsOverlay by remember { mutableStateOf(PlayerPreferences.isShowPlayerStatsEnabled(context)) }

    // Stream Analytics & Technical Diagnostics (from mytv VideoPlayerMetadata)
    var streamMimeType by remember { mutableStateOf("") }
    var streamDecoder by remember { mutableStateOf("") }
    var streamBitrate by remember { mutableLongStateOf(0L) }
    var streamFrameRate by remember { mutableFloatStateOf(0f) }
    var streamAudioMime by remember { mutableStateOf("") }
    var streamAudioDecoder by remember { mutableStateOf("") }
    var streamAudioChannels by remember { mutableIntStateOf(0) }
    var streamAudioSampleRate by remember { mutableIntStateOf(0) }
    var streamBufferedSeconds by remember { mutableFloatStateOf(0f) }

    val loadTimeoutSecs = remember { PlayerPreferences.getLoadTimeoutSeconds(context) }
    val loadTimeoutMs = remember(loadTimeoutSecs, isSlowNetMode) {
        if (isSlowNetMode) 35000 else (loadTimeoutSecs * 1000).coerceAtLeast(10000)
    }
    val isSoftAudioDecodePref = remember { PlayerPreferences.isAudioSoftDecodeEnabled(context) }
    var isSoftAudioDecode by remember { mutableStateOf(isSoftAudioDecodePref) }

    // Multi-View state: holds active channel slots (1 to 4 streams)
    var multiViewSlots by remember {
        mutableStateOf(
            listOf(
                MultiViewSlot(
                    slotId = 0,
                    channelId = mediaId,
                    name = title,
                    logoUrl = posterUrl,
                    streamUrl = activeStreamUrl,
                    userAgent = userAgent,
                    referer = referer,
                    drmKeyId = drmKeyId,
                    drmKey = drmKey,
                    drmLicenseUrl = drmLicenseUrl,
                    groupId = groupId,
                    isLive = isLiveStream,
                    resizeModeIndex = resizeModeIndex
                )
            )
        )
    }
    var focusedSlotIndex by remember { mutableIntStateOf(0) }
    var audioSlotIndex by remember { mutableIntStateOf(0) }
    var showMultiViewChannelPicker by remember { mutableStateOf(false) }
    var expandedFullscreenSlotIndex by remember { mutableStateOf<Int?>(null) }
    val isMultiViewActive = multiViewSlots.size > 1

    // Intercept back gestures: dismiss TV drawer, settings sheet, picker sheet, restore grid from fullscreen, or exit Multi-View gracefully before exiting the screen
    BackHandler(enabled = showTvChannelDrawer || showMultiViewChannelPicker || activeSheet != null || expandedFullscreenSlotIndex != null || isMultiViewActive) {
        if (showTvChannelDrawer) {
            showTvChannelDrawer = false
        } else if (activeSheet != null) {
            activeSheet = null
        } else if (showMultiViewChannelPicker) {
            showMultiViewChannelPicker = false
        } else if (expandedFullscreenSlotIndex != null) {
            expandedFullscreenSlotIndex = null
        } else if (isMultiViewActive) {
            multiViewSlots = listOf(multiViewSlots[0])
            focusedSlotIndex = 0
            audioSlotIndex = 0
        }
    }

    // Keep Slot 0 synchronized with activeStreamUrl, title, and resizeModeIndex
    LaunchedEffect(activeStreamUrl, title, resizeModeIndex) {
        if (multiViewSlots.isNotEmpty()) {
            multiViewSlots = multiViewSlots.toMutableList().apply {
                this[0] = this[0].copy(
                    streamUrl = activeStreamUrl,
                    name = title,
                    resizeModeIndex = resizeModeIndex
                )
            }
        }
    }

    val rawOstoraDataSourceFactory = remember(customAgent, defaultHeaders, loadTimeoutMs) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(customAgent)
            .setKeepPostFor302Redirects(true)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(loadTimeoutMs)
            .setReadTimeoutMs(loadTimeoutMs)
            .setDefaultRequestProperties(defaultHeaders)

        com.beouttv.app.data.source.stream.OstoraStreamDataSource.Factory(
            upstreamFactory = httpDataSourceFactory
        )
    }

    val ostoraDataSourceFactory = remember(rawOstoraDataSourceFactory) {
        androidx.media3.datasource.DefaultDataSource.Factory(context, rawOstoraDataSourceFactory)
    }

    val defaultMediaSourceFactory = remember(ostoraDataSourceFactory, drmKeyId, drmKey) {
        val factory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(ostoraDataSourceFactory)
            .setLoadErrorHandlingPolicy(com.beouttv.app.data.source.stream.ResilientHlsLoadErrorHandlingPolicy())
        try {
            if (!drmKeyId.isNullOrBlank() && !drmKey.isNullOrBlank()) {
                val clearKeyBytes = buildClearKeyJson(drmKeyId, drmKey)
                val callback = LocalMediaDrmCallback(clearKeyBytes)
                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .build(callback)
                factory.setDrmSessionManagerProvider { drmSessionManager }
            }
        } catch (_: Exception) {}
        factory
    }

    val exoPlayer = remember(isSoftAudioDecode) {

        // Bind user preferences & adaptive live buffer engine: eliminates live stalls and RAM starvation on 32-bit devices
        val isLivePlayback = isLive || isLiveStream || mediaId.startsWith("sp_") || mediaId.startsWith("ostora") || activeStreamUrl.contains(".m3u8") || activeStreamUrl.contains(".mpd")

        val minBufferMs: Int
        val maxBufferMs: Int
        val playStartMs: Int
        val rebufferMs: Int
        val backBufferMs: Int

        if (isLivePlayback) {
            // Ultra-responsive live broadcast buffer: prevents live HLS playlist starvation and avoids GC freezes
            minBufferMs = if (isSlowNetMode) 4000 else 2500
            maxBufferMs = if (isSlowNetMode) 12000 else 8000
            playStartMs = if (isSlowNetMode) 1500 else 1000
            rebufferMs = if (isSlowNetMode) 2500 else 1800
            backBufferMs = 0
        } else {
            // VOD buffer for Movies & Series
            val userBufferSecs = PlayerPreferences.getBufferSizeSeconds(context)
            val effectiveBufferSecs = if (isSlowNetMode) 90 else maxOf(30, userBufferSecs)
            minBufferMs = if (isSlowNetMode) 30000 else 15000
            maxBufferMs = maxOf(35000, effectiveBufferSecs * 1000)
            playStartMs = if (isSlowNetMode) 4000 else 2500
            rebufferMs = if (isSlowNetMode) 6000 else 3500
            backBufferMs = if (isSlowNetMode) 30000 else 15000
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                playStartMs,
                rebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(backBufferMs, backBufferMs > 0)
            .build()

        // Bind user preferences: Hardware Acceleration, Audio Soft Decoding & VSync frame skipping
        val isHwAccel = PlayerPreferences.isHwAccelerationEnabled(context)
        val skipFramesOnVsync = PlayerPreferences.isSkipFramesOnSameVSyncEnabled(context)
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(
                if (isSoftAudioDecode || !isHwAccel) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            )
            .setEnableDecoderFallback(true)
            .setAllowedVideoJoiningTimeMs(if (skipFramesOnVsync) 0L else 5000L)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(defaultMediaSourceFactory)
            .setLoadControl(loadControl)
            .build()

        // Bind user preferences: Default Quality / Ultra-Low Network Clamping
        if (isSlowNetMode) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(854, 480)
                .build()
        } else {
            val initialQualityPref = PlayerPreferences.getDefaultQuality(context)
            val (initW, initH) = when {
                initialQualityPref.contains("1080") -> 1920 to 1080
                initialQualityPref.contains("720") -> 1280 to 720
                initialQualityPref.contains("480") -> 854 to 480
                else -> Int.MAX_VALUE to Int.MAX_VALUE
            }
            if (initW != Int.MAX_VALUE) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(initW, initH)
                    .build()
            }
        }

        // Subtitles default: If MovieBox or Arabic subtitle exists, enable Arabic text track by default!
        val initialArabicSub = subtitles.firstOrNull { it.language == "ar" && it.label.contains("MovieBox") }
            ?: subtitles.firstOrNull { it.language == "ar" }
            ?: subtitles.firstOrNull()

        if (initialArabicSub != null) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage("ar")
                .build()
        } else {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
        }

        player
    }

    // Manage primary player volume based on audio focus
    LaunchedEffect(audioSlotIndex, isMultiViewActive) {
        exoPlayer.volume = if (!isMultiViewActive || audioSlotIndex == 0) 1f else 0f
    }

    val coroutineScope = rememberCoroutineScope()

    fun loadStreamInternal(
        targetUrl: String,
        startPosMs: Long = -1L,
        overrideDrmKeyId: String? = null,
        overrideDrmKey: String? = null
    ) {
        if (targetUrl.isBlank()) {
            val fallback = activeServersList.firstOrNull()?.url.orEmpty()
            if (fallback.isNotBlank()) {
                loadStreamInternal(fallback, startPosMs, overrideDrmKeyId, overrideDrmKey)
                return
            }
            errorMessage = "رابط البث غير متوفر حالياً."
            return
        }
        try {
            isLoading = true
            hasRenderedFirstFrame = false
            errorMessage = null
            val trimmed = targetUrl.trim()
            val uri = Uri.parse(trimmed)
            val isDash = trimmed.contains(".mpd", ignoreCase = true)
            val isTs = !isDash && (
                trimmed.endsWith(".ts", ignoreCase = true) ||
                trimmed.contains(".ts?", ignoreCase = true) ||
                trimmed.contains("format=ts", ignoreCase = true)
            )
            val isHls = !isDash && !isTs && (
                trimmed.contains(".m3u8", ignoreCase = true) ||
                trimmed.contains("/hls", ignoreCase = true) ||
                (trimmed.contains(".json", ignoreCase = true) && trimmed.contains("token=")) ||
                trimmed.contains("maziika", ignoreCase = true) ||
                trimmed.contains(".shop/x", ignoreCase = true) ||
                (isLive && !trimmed.endsWith(".mp4", ignoreCase = true) && !trimmed.contains(".mp4?", ignoreCase = true))
            )

            val builder = when {
                isHls -> {
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                isDash -> {
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                }
                isTs -> {
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(MimeTypes.VIDEO_MP2T)
                }
                else -> MediaItem.Builder().setUri(uri)
            }

            val effectiveDrmKeyId = overrideDrmKeyId ?: drmKeyId
            val effectiveDrmKey = overrideDrmKey ?: drmKey

            if (!drmLicenseUrl.isNullOrBlank()) {
                builder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.WIDEVINE_UUID)
                        .setLicenseUri(drmLicenseUrl)
                        .build()
                )
            } else if (!effectiveDrmKeyId.isNullOrBlank() && !effectiveDrmKey.isNullOrBlank()) {
                builder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.CLEARKEY_UUID)
                        .build()
                )
            }

            if (activeSubtitles.isNotEmpty()) {
                val subConfigs = activeSubtitles.map { sub ->
                    val mime = if (sub.url.endsWith(".srt", true) || sub.format == "srt") {
                        MimeTypes.APPLICATION_SUBRIP
                    } else {
                        MimeTypes.TEXT_VTT
                    }
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                        .setId(sub.id)
                        .setMimeType(mime)
                        .setLanguage(sub.language)
                        .setLabel(sub.label)
                        .setSelectionFlags(0)
                        .build()
                }
                builder.setSubtitleConfigurations(subConfigs)
            }

            val mediaItem = builder.build()

            var streamDrmManager: androidx.media3.exoplayer.drm.DrmSessionManager? = null
            if (!effectiveDrmKeyId.isNullOrBlank() && !effectiveDrmKey.isNullOrBlank()) {
                try {
                    val clearKeyBytes = buildClearKeyJson(effectiveDrmKeyId, effectiveDrmKey)
                    val callback = LocalMediaDrmCallback(clearKeyBytes)
                    streamDrmManager = DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                        .build(callback)
                } catch (_: Exception) {}
            }

            val primarySource: MediaSource = when {
                isHls -> {
                    val hlsParserFactory = com.beouttv.app.data.source.stream.SanitizedHlsPlaylistParserFactory()
                    val hlsFactory = HlsMediaSource.Factory(ostoraDataSourceFactory)
                        .setPlaylistParserFactory(hlsParserFactory)
                        .setAllowChunklessPreparation(false)
                        .setLoadErrorHandlingPolicy(com.beouttv.app.data.source.stream.ResilientHlsLoadErrorHandlingPolicy())
                    if (streamDrmManager != null) {
                        hlsFactory.setDrmSessionManagerProvider { streamDrmManager }
                    }
                    hlsFactory.createMediaSource(mediaItem)
                }
                isDash -> {
                    val dashFactory = DashMediaSource.Factory(ostoraDataSourceFactory)
                    if (streamDrmManager != null) {
                        dashFactory.setDrmSessionManagerProvider { streamDrmManager }
                    }
                    dashFactory.createMediaSource(mediaItem)
                }
                else -> {
                    defaultMediaSourceFactory.createMediaSource(mediaItem)
                }
            }

            val finalSource: MediaSource = if ((isHls || isDash) && activeSubtitles.isNotEmpty()) {
                val subSources = activeSubtitles.map { sub ->
                    val mime = if (sub.url.endsWith(".srt", true) || sub.format == "srt") {
                        MimeTypes.APPLICATION_SUBRIP
                    } else {
                        MimeTypes.TEXT_VTT
                    }
                    val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                        .setId(sub.id)
                        .setMimeType(mime)
                        .setLanguage(sub.language)
                        .setLabel(sub.label)
                        .setSelectionFlags(0)
                        .build()
                    androidx.media3.exoplayer.source.SingleSampleMediaSource.Factory(ostoraDataSourceFactory)
                        .createMediaSource(subConfig, androidx.media3.common.C.TIME_UNSET)
                }
                androidx.media3.exoplayer.source.MergingMediaSource(primarySource, *subSources.toTypedArray())
            } else {
                primarySource
            }

            exoPlayer.setMediaSource(finalSource)
            exoPlayer.prepare()
            if (startPosMs > 0L && !isHls) {
                exoPlayer.seekTo(startPosMs)
            }
            val isAdShowing = !com.beouttv.app.data.ads.AdsterraManager.activeAdUrl.value.isNullOrBlank()
            exoPlayer.playWhenReady = !isAdShowing
        } catch (e: Exception) {
            errorMessage = "خطأ في تهيئة المشغل: ${e.localizedMessage ?: "رابط غير صالح"}"
        }
    }

    fun loadStream(
        targetUrl: String,
        startPosMs: Long = -1L,
        overrideDrmKeyId: String? = null,
        overrideDrmKey: String? = null
    ) {
        if (targetUrl.isBlank()) {
            val fallbackUrl = activeServersList.firstOrNull()?.url.orEmpty()
            if (fallbackUrl.isNotBlank()) {
                loadStream(fallbackUrl, startPosMs, overrideDrmKeyId, overrideDrmKey)
                return
            }
            if (mediaId.isNotBlank()) {
                isLoading = true
                errorMessage = null
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val fetched = ServiceLocator.repository.getMovieById(mediaId)
                        val sUrl = fetched?.streamUrl?.ifBlank { fetched?.servers?.firstOrNull()?.url.orEmpty() }.orEmpty()
                        withContext(Dispatchers.Main) {
                            if (sUrl.isNotBlank()) {
                                if (fetched != null && fetched.servers.isNotEmpty() && activeServersList.isEmpty()) {
                                    activeServersList = fetched.servers
                                }
                                if (fetched != null && fetched.subtitles.isNotEmpty()) {
                                    val sorted = fetched.subtitles.sortedWith(
                                        compareByDescending<com.beouttv.app.data.model.SubtitleTrack> { it.language == "ar" && it.label.contains("MovieBox") }
                                            .thenByDescending { it.language == "ar" }
                                            .thenByDescending { it.label.contains("MovieBox") }
                                    )
                                    sorted.forEach { sub: com.beouttv.app.data.model.SubtitleTrack ->
                                        if (activeSubtitles.none { it.url == sub.url }) {
                                            activeSubtitles.add(sub)
                                        }
                                    }
                                }
                                loadStream(sUrl, startPosMs, overrideDrmKeyId, overrideDrmKey)
                            } else {
                                isLoading = false
                                errorMessage = "رابط البث غير متوفر حالياً."
                            }
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            errorMessage = "رابط البث غير متوفر حالياً."
                        }
                    }
                }
                return
            }
            errorMessage = "رابط البث غير متوفر حالياً."
            return
        }
        val cleaned = com.beouttv.app.data.source.stream.OstoraApiResolver.cleanStreamUrl(targetUrl.trim())

        val isTargetWebEmbed = run {
            val lower = cleaned.lowercase()
            lower.contains("vidlink.pro") ||
            lower.contains("videasy.to") ||
            lower.contains("zxcstream") ||
            lower.contains("2embed") ||
            (lower.contains("/embed") && !lower.contains(".m3u8") && !lower.contains(".mp4"))
        }

        if (isTargetWebEmbed) {
            activeStreamUrl = cleaned
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            isLoading = false
            errorMessage = null
            hasRenderedFirstFrame = true
            return
        }

        if (com.beouttv.app.data.source.stream.LiveGatewayStreamResolver.isTargetApiUrl(cleaned) || mediaId.startsWith("gw_")) {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                val resolved = com.beouttv.app.data.source.stream.LiveGatewayStreamResolver.resolve(cleaned)
                if (resolved != null && resolved.streamUrl.isNotBlank()) {
                    activeStreamUrl = resolved.streamUrl
                    loadStreamInternal(
                        targetUrl = resolved.streamUrl,
                        startPosMs = startPosMs,
                        overrideDrmKeyId = resolved.drmKeyId ?: overrideDrmKeyId ?: drmKeyId,
                        overrideDrmKey = resolved.drmKey ?: overrideDrmKey ?: drmKey
                    )
                } else {
                    errorMessage = "تعذر تشغيل هذا السيرفر، يرجى اختيار سيرفر آخر من القائمة بالأعلى."
                    isLoading = false
                }
            }
            return
        }

        if (com.beouttv.app.data.source.stream.OkRuStreamResolver.isOkRuUrl(cleaned) || mediaId.startsWith("ok_") || mediaId.startsWith("okru_")) {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                val resolved = com.beouttv.app.data.source.stream.OkRuStreamResolver.resolve(cleaned)
                if (resolved != null && resolved.streamUrl.isNotBlank()) {
                    activeStreamUrl = resolved.streamUrl
                    loadStreamInternal(
                        targetUrl = resolved.streamUrl,
                        startPosMs = startPosMs,
                        overrideDrmKeyId = resolved.drmKeyId ?: overrideDrmKeyId ?: drmKeyId,
                        overrideDrmKey = resolved.drmKey ?: overrideDrmKey ?: drmKey
                    )
                } else {
                    errorMessage = "تعذر تشغيل بث OK.ru، يرجى التحقق من اتصال الإنترنت أو حالة القناة."
                    isLoading = false
                }
            }
            return
        }

        val isOstora = com.beouttv.app.data.source.stream.OstoraApiResolver.isOstoraUrl(cleaned) ||
                !groupId.isNullOrBlank() ||
                mediaId.startsWith("ostora") ||
                mediaId.startsWith("sp_") ||
                mediaId.startsWith("ent_") ||
                mediaId.startsWith("osn_") ||
                cleaned.startsWith("412<F>") ||
                cleaned.startsWith("413<F>") ||
                cleaned.startsWith("414<F>") ||
                cleaned.contains("<F>", ignoreCase = true) ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(mediaId) ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(title) ||
                title.contains("ألوان") || title.contains("الوان") || title.contains("alwan", true) ||
                title.contains("osn", true) || title.contains("bein", true) || title.contains("بي ان") ||
                title.contains("بى ان") || title.contains("ssc", true) || title.contains("كأس") ||
                title.contains("alkass", true) || title.contains("ثمانية") || title.contains("thmanyah", true)

        val now = System.currentTimeMillis()
        val isAlreadyResolved = isOstora && (cleaned == lastTokenRefreshUrl) && ((now - lastTokenRefreshTime) < 45_000L)

        if (isOstora && !isAlreadyResolved) {
            isLoading = true
            errorMessage = null
            coroutineScope.launch {
                val resolved = withContext(Dispatchers.IO) {
                    try {
                        com.beouttv.app.data.source.stream.OstoraApiResolver.resolveDynamicStream(
                            streamUrl = cleaned,
                            channelId = mediaId,
                            channelName = title,
                            groupId = groupId
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                if (resolved != null && resolved.streamUrl.isNotBlank() && resolved.streamUrl != cleaned) {
                    lastTokenRefreshUrl = resolved.streamUrl
                    lastTokenRefreshTime = System.currentTimeMillis()
                    activeStreamUrl = resolved.streamUrl
                    loadStreamInternal(
                        targetUrl = resolved.streamUrl,
                        startPosMs = startPosMs,
                        overrideDrmKeyId = resolved.drmKeyId ?: overrideDrmKeyId ?: drmKeyId,
                        overrideDrmKey = resolved.drmKey ?: overrideDrmKey ?: drmKey
                    )
                } else {
                    lastTokenRefreshUrl = cleaned
                    lastTokenRefreshTime = System.currentTimeMillis()
                    val (cleanUrl, parsedKid, parsedKey) = com.beouttv.app.data.source.stream.OstoraApiResolver.parseDrmKeysFromUrl(cleaned)
                    val effKid = parsedKid ?: overrideDrmKeyId ?: drmKeyId
                    val effKey = parsedKey ?: overrideDrmKey ?: drmKey
                    loadStreamInternal(cleanUrl, startPosMs, effKid, effKey)
                }
            }
            return
        }

        val (cleanUrl, parsedKid, parsedKey) = com.beouttv.app.data.source.stream.OstoraApiResolver.parseDrmKeysFromUrl(cleaned)
        val effKid = parsedKid ?: overrideDrmKeyId ?: drmKeyId
        val effKey = parsedKey ?: overrideDrmKey ?: drmKey

        loadStreamInternal(cleanUrl, startPosMs, effKid, effKey)
    }

    // Smart TV Remote Actions Collector
    remoteActionFlow?.let { flow ->
        LaunchedEffect(flow, isMultiViewActive, multiViewSlots.size, focusedSlotIndex, audioSlotIndex, showTvChannelDrawer, showMultiViewChannelPicker, activeSheet, expandedFullscreenSlotIndex, availableTvChannels, isWebEmbed, webEmbedPlaying) {
            flow.collect { action ->
                when (action) {
                    is TvRemoteAction.TogglePlayPause -> {
                        if (showTvChannelDrawer) {
                            showTvChannelDrawer = false
                        } else if (activeSheet != null) {
                            // Sheet is open
                        } else if (!controlsVisible) {
                            controlsVisible = true
                        } else {
                            if (isWebEmbed) {
                                webEmbedPlaying = !webEmbedPlaying
                                val js = if (webEmbedPlaying) "document.querySelector('video')?.play();" else "document.querySelector('video')?.pause();"
                                webViewRef?.evaluateJavascript(js, null)
                            } else {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            }
                            controlsVisible = true
                        }
                    }
                    is TvRemoteAction.ShowHud -> {
                        if (showTvChannelDrawer) {
                            showTvChannelDrawer = false
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    }
                    is TvRemoteAction.ToggleQualitySheet -> {
                        activeSheet = if (activeSheet == PlayerSheetType.SERVERS || activeSheet == PlayerSheetType.QUALITY) null
                                      else if (activeServersList.isNotEmpty()) PlayerSheetType.SERVERS
                                      else PlayerSheetType.QUALITY
                        controlsVisible = true
                    }
                    is TvRemoteAction.ToggleAudioTrack -> {
                        activeSheet = if (activeSheet == PlayerSheetType.AUDIO_SUBTITLES) null else PlayerSheetType.AUDIO_SUBTITLES
                        controlsVisible = true
                    }
                    is TvRemoteAction.AddMultiViewChannel -> {
                        if (multiViewSlots.size < 4) {
                            showMultiViewChannelPicker = true
                        }
                    }
                    is TvRemoteAction.CloseSlotOrPip -> {
                        if (showTvChannelDrawer) {
                            showTvChannelDrawer = false
                        } else if (showMultiViewChannelPicker) {
                            showMultiViewChannelPicker = false
                        } else if (activeSheet != null) {
                            activeSheet = null
                        } else if (expandedFullscreenSlotIndex != null) {
                            expandedFullscreenSlotIndex = null
                        } else if (isMultiViewActive) {
                            multiViewSlots = listOf(multiViewSlots[0])
                            focusedSlotIndex = 0
                            audioSlotIndex = 0
                        } else {
                            onBack()
                        }
                    }
                    is TvRemoteAction.NextChannel -> {
                        if (availableTvChannels.isNotEmpty()) {
                            val currentIdx = availableTvChannels.indexOfFirst { it.id == mediaId }
                            val nextIdx = if (currentIdx >= 0 && currentIdx + 1 < availableTvChannels.size) currentIdx + 1 else 0
                            val nextChannel = availableTvChannels[nextIdx]
                            coroutineScope.launch {
                                val resolved = resolveMultiViewChannelStream(nextChannel)
                                activeStreamUrl = resolved.streamUrl
                                loadStream(resolved.streamUrl)
                            }
                        }
                    }
                    is TvRemoteAction.PreviousChannel -> {
                        if (availableTvChannels.isNotEmpty()) {
                            val currentIdx = availableTvChannels.indexOfFirst { it.id == mediaId }
                            val prevIdx = if (currentIdx > 0) currentIdx - 1 else availableTvChannels.size - 1
                            val prevChannel = availableTvChannels[prevIdx]
                            coroutineScope.launch {
                                val resolved = resolveMultiViewChannelStream(prevChannel)
                                activeStreamUrl = resolved.streamUrl
                                loadStream(resolved.streamUrl)
                            }
                        }
                    }
                    is TvRemoteAction.StepUp -> {
                        if (!showTvChannelDrawer && activeSheet == null && !showMultiViewChannelPicker) {
                            showTvChannelDrawer = true
                            controlsVisible = false
                        } else if (showTvChannelDrawer) {
                            showTvChannelDrawer = false
                        } else {
                            controlsVisible = true
                        }
                    }
                    is TvRemoteAction.StepDown -> {
                        if (showTvChannelDrawer) {
                            showTvChannelDrawer = false
                        } else if (!controlsVisible) {
                            controlsVisible = true
                        }
                    }
                    is TvRemoteAction.StepLeft -> {
                        if (!showTvChannelDrawer && activeSheet == null) {
                            if (isMultiViewActive) {
                                val nextFocus = if (focusedSlotIndex > 0) focusedSlotIndex - 1 else multiViewSlots.size - 1
                                focusedSlotIndex = nextFocus
                                audioSlotIndex = nextFocus
                            } else if (isWebEmbed) {
                                webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) v.currentTime = Math.max(0, v.currentTime - 10);", null)
                                controlsVisible = true
                            } else {
                                exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10000L))
                                controlsVisible = true
                            }
                        }
                    }
                    is TvRemoteAction.StepRight -> {
                        if (!showTvChannelDrawer && activeSheet == null) {
                            if (isMultiViewActive) {
                                val nextFocus = if (focusedSlotIndex < multiViewSlots.size - 1) focusedSlotIndex + 1 else 0
                                focusedSlotIndex = nextFocus
                                audioSlotIndex = nextFocus
                            } else if (isWebEmbed) {
                                webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) v.currentTime = v.currentTime + 10;", null)
                                controlsVisible = true
                            } else {
                                val target = if (duration > 0L) minOf(duration, exoPlayer.currentPosition + 10000L) else exoPlayer.currentPosition + 10000L
                                exoPlayer.seekTo(target)
                                controlsVisible = true
                            }
                        }
                    }
                    is TvRemoteAction.SelectSlotNumber -> {
                        if (action.slotNumber < multiViewSlots.size) {
                            focusedSlotIndex = action.slotNumber
                            audioSlotIndex = action.slotNumber
                        }
                    }
                    TvRemoteAction.Unhandled -> Unit
                }
            }
        }
    }

    var hasSeekedInitially by remember { mutableStateOf(false) }
    val initialSeek = remember {
        if (isLive || isOstoraStream) -1L
        else if (seekPosition > 0L) seekPosition
        else PlaybackHistoryManager.getSavedPosition(context, mediaId, streamUrl)
    }

    // Auto-fetch Arabic and multilingual subtitles in the background if none were provided
    LaunchedEffect(mediaId, title) {
        if (activeSubtitles.isEmpty() && title.isNotBlank() && !isLive) {
            withContext(Dispatchers.IO) {
                try {
                    val onlineSubs = CoreApiClient().fetchSubtitles(query = title)
                    if (onlineSubs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onlineSubs.forEach { sub ->
                                if (activeSubtitles.none { it.url == sub.url || it.label == sub.label }) {
                                    activeSubtitles.add(sub)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Initialize player stream with dynamic token resolution (Bypassing Neo Player prompt)
    LaunchedEffect(exoPlayer) {
        com.beouttv.app.data.ads.AdsterraManager.onStreamLaunch(context)
        var finalUrl = activeStreamUrl.ifBlank { activeServersList.firstOrNull()?.url.orEmpty() }
        var dynamicDrmKeyId: String? = null
        var dynamicDrmKey: String? = null
        val resumePos = if (hasRenderedFirstFrame && currentPosition > 0L) currentPosition else initialSeek

        val isOstora = (
            com.beouttv.app.data.source.stream.OstoraApiResolver.isOstoraUrl(finalUrl) ||
            !groupId.isNullOrBlank() ||
            mediaId.startsWith("ostora") ||
            mediaId.startsWith("sp_") ||
            mediaId.startsWith("ent_") ||
            mediaId.startsWith("osn_") ||
            finalUrl.startsWith("412<F>") ||
            finalUrl.startsWith("413<F>") ||
            finalUrl.startsWith("414<F>") ||
            finalUrl.contains("<F>", ignoreCase = true) ||
            com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(mediaId) ||
            com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(title) ||
            title.contains("بي ان") || title.contains("بى ان") || title.contains("beIN", ignoreCase = true) ||
            title.contains("osn", ignoreCase = true) || title.contains("كأس") || title.contains("alkass", ignoreCase = true) ||
            title.contains("ثمانية") || title.contains("ألوان") || title.contains("ssc", ignoreCase = true)
        )

        if (isOstora) {
            withContext(Dispatchers.IO) {
                try {
                    val resolved = com.beouttv.app.data.source.stream.OstoraApiResolver.resolveDynamicStream(
                        streamUrl = finalUrl,
                        channelId = mediaId,
                        channelName = title,
                        groupId = groupId
                    )
                    if (resolved.streamUrl.isNotBlank()) {
                        finalUrl = resolved.streamUrl
                        dynamicDrmKeyId = resolved.drmKeyId
                        dynamicDrmKey = resolved.drmKey
                    }
                } catch (_: Exception) {
                    finalUrl = com.beouttv.app.data.source.stream.OstoraApiResolver.cleanStreamUrl(finalUrl)
                }
            }
            lastTokenRefreshUrl = finalUrl
            lastTokenRefreshTime = System.currentTimeMillis()
        } else if (finalUrl.contains("govid.live") || finalUrl.contains("prem-") || finalUrl.contains("wecima")) {
            val realId = when {
                finalUrl.contains("prem-") -> {
                    finalUrl.substringAfter("prem-").substringBefore(".m3u8").substringBefore("?").filter { it.isDigit() }
                }
                finalUrl.contains("realid=") -> {
                    finalUrl.substringAfter("realid=").substringBefore("&").filter { it.isDigit() }
                }
                mediaId.isNotBlank() && mediaId.length >= 4 && mediaId.all { it.isDigit() } -> {
                    mediaId
                }
                else -> ""
            }

            if (realId.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    try {
                        val tokenUrl = CoreApiClient().resolveStreamUrl(realId)
                        if (!tokenUrl.isNullOrBlank()) {
                            finalUrl = tokenUrl
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (mediaId.startsWith("mb_") || (finalUrl.isBlank() && mediaId.isNotBlank())) {
            withContext(Dispatchers.IO) {
                try {
                    val resolvedMovie = ServiceLocator.repository.getMovieById(mediaId)
                    if (resolvedMovie != null) {
                        val sUrl = resolvedMovie.streamUrl.ifBlank { resolvedMovie.servers.firstOrNull()?.url.orEmpty() }
                        if (sUrl.isNotBlank() && finalUrl.isBlank()) {
                            finalUrl = sUrl
                        }
                        if (resolvedMovie.servers.isNotEmpty() && activeServersList.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                activeServersList = resolvedMovie.servers
                            }
                        }
                        if (resolvedMovie.subtitles.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                val sorted = resolvedMovie.subtitles.sortedWith(
                                    compareByDescending<com.beouttv.app.data.model.SubtitleTrack> { it.language == "ar" && it.label.contains("MovieBox") }
                                        .thenByDescending { it.language == "ar" }
                                        .thenByDescending { it.label.contains("MovieBox") }
                                )
                                sorted.forEach { sub: com.beouttv.app.data.model.SubtitleTrack ->
                                    if (activeSubtitles.none { it.url == sub.url }) {
                                        activeSubtitles.add(sub)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (finalUrl.isBlank() && activeServersList.isNotEmpty()) {
            finalUrl = activeServersList.first().url
        }
        if (finalUrl.isNotBlank()) {
            activeStreamUrl = finalUrl
        }
        loadStream(finalUrl, resumePos, dynamicDrmKeyId, dynamicDrmKey)
    }

    // Dynamic resolution of authentic channel servers (Ostora Smart Engine)
    LaunchedEffect(title, mediaId, isBeinSportsChannel, isOstoraStream, isSlowNetMode) {
        val shouldResolve = isOstoraStream || isBeinSportsChannel ||
                OstoraApiResolver.isKnownSportsChannel(title) ||
                OstoraApiResolver.isKnownSportsChannel(mediaId) ||
                activeServersList.size <= 1

        if (shouldResolve) {
            isResolvingServers = true
            withContext(Dispatchers.IO) {
                try {
                    val resolved = OstoraApiResolver.resolveRealChannelServers(
                        currentStreamUrl = activeStreamUrl,
                        channelId = mediaId,
                        channelName = title,
                        groupId = groupId
                    )
                    if (resolved.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            activeServersList = resolved
                            // Synchronize server selection with default server preference or slow net mode
                            val cleanActive = OstoraApiResolver.cleanStreamUrl(activeStreamUrl).substringBefore("?").trim()
                            val defaultSrvPref = PlayerPreferences.getDefaultServer(context)
                            val preferredMatch = if (isSlowNetMode) {
                                resolved.firstOrNull { it.quality.contains("LOW", true) || it.quality.contains("360") }
                                    ?: resolved.firstOrNull { it.quality.contains("SD", true) || it.quality.contains("480") }
                                    ?: resolved.firstOrNull { it.quality.contains("HD", true) || it.quality.contains("720") }
                                    ?: resolved.lastOrNull()
                            } else {
                                resolved.firstOrNull { it.id == defaultSrvPref }
                            }

                            val currentMatch = preferredMatch ?: resolved.firstOrNull { srv ->
                                val cleanSrv = OstoraApiResolver.cleanStreamUrl(srv.url).substringBefore("?").trim()
                                cleanSrv == cleanActive
                            } ?: resolved.firstOrNull()

                            if (currentMatch != null && (selectedServerId.isBlank() || (isSlowNetMode && currentMatch.url != activeStreamUrl))) {
                                selectedServerId = currentMatch.id
                                selectedQualityLabel = currentMatch.quality
                                if (isSlowNetMode && currentMatch.url != activeStreamUrl && currentMatch.url.isNotBlank()) {
                                    activeStreamUrl = currentMatch.url
                                    loadStream(currentMatch.url, exoPlayer.currentPosition)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    withContext(Dispatchers.Main) {
                        isResolvingServers = false
                    }
                }
            }
        }
    }

    // Safety Watchdog: Prevent infinite loading spinner if playback buffers indefinitely (mytv timeout engine)
    val loadTimeoutWatchdogMs = remember(loadTimeoutSecs) { (loadTimeoutSecs * 1000L).coerceAtLeast(10000L) }
    LaunchedEffect(isLoading, activeStreamUrl, loadTimeoutWatchdogMs) {
        if (isLoading) {
            delay(loadTimeoutWatchdogMs)
            if (isLoading && !hasRenderedFirstFrame && exoPlayer.playbackState == Player.STATE_BUFFERING) {
                val autoSwitch = PlayerPreferences.isAutoSwitchServerOnErrorEnabled(context)
                if (autoSwitch && activeServersList.size > 1) {
                    val currentIndex = activeServersList.indexOfFirst { it.url == activeStreamUrl || it.id == selectedServerId }
                    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % activeServersList.size else 0
                    if (nextIndex != currentIndex && nextIndex in activeServersList.indices) {
                        val nextServer = activeServersList[nextIndex]
                        selectedServerId = nextServer.id
                        selectedQualityLabel = nextServer.quality
                        activeStreamUrl = nextServer.url
                        loadStream(nextServer.url, exoPlayer.currentPosition)
                        android.widget.Toast.makeText(
                            context,
                            "جاري التحويل تلقائياً إلى سيرفر بديل: ${nextServer.name}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@LaunchedEffect
                    }
                }
                isLoading = false
                errorMessage = "تعذر الاتصال بسيرفر البث، يرجى اختيار سيرفر آخر من القائمة بالأعلى."
            }
        }
    }

    // Stream Stalling Watchdog (mytv onInterrupt pattern: auto-recovers frozen live broadcasts)
    LaunchedEffect(isPlaying, isLiveStream, activeStreamUrl, loadTimeoutSecs) {
        if (isPlaying && isLiveStream) {
            var lastRecordedPos = -1L
            var stallCounter = 0
            while (true) {
                delay(1000L)
                if (exoPlayer.isPlaying) {
                    val currentPos = exoPlayer.currentPosition
                    if (currentPos == lastRecordedPos && lastRecordedPos != -1L) {
                        stallCounter++
                        if (stallCounter >= loadTimeoutSecs) {
                            android.util.Log.w("PlayerScreen", "mytv stall watchdog: stream freeze detected (${stallCounter}s), auto-recovering...")
                            stallCounter = 0
                            exoPlayer.seekToDefaultPosition()
                            exoPlayer.prepare()
                        }
                    } else {
                        lastRecordedPos = currentPos
                        stallCounter = 0
                    }
                } else {
                    stallCounter = 0
                }
            }
        }
    }

    fun getDetailedPlayerErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "تعذر الاتصال بسيرفر البث (قد يكون السيرفر متوقفاً من المصدر أو هناك انقطاع في الشبكة). يرجى فحص الاتصال أو اختيار سيرفر آخر."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "انتهت مهلة استجابة السيرفر بسبب بطء الشبكة. ننصح بتفعيل 'وضع النت شديد الضعف' أو اختيار سيرفر بديل."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "السيرفر أعاد استجابة خطأ (HTTP Error). السيرفر ربما يواجه ضغطاً كبيراً، يرجى التبديل لسيرفر آخر من القائمة."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                "تعذر تشغيل معالج فك التشفير العتادي على جهازك. تم التبديل إلى المعالجة البرمجية تلقائياً."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                "تنسيق رابط البث غير متوافق مع الحاوية الحالية. جاري فحص الصيغ البديلة..."
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                "تأخر تدفق البث المباشر عن نافذة العرض، جاري المزامنة التلقائية مع البث الحي..."
            PlaybackException.ERROR_CODE_TIMEOUT ->
                "استغرقت عملية تحميل البث وقتاً أطول من المعتاد. يرجى التحقق من سرعة الإنترنت."
            else -> "تعذر تشغيل هذا السيرفر (${error.errorCodeName}): يرجى فحص الاتصال أو التبديل إلى سيرفر آخر من الأعلى."
        }
    }

    var liveAutoRetryCount by remember { mutableIntStateOf(0) }

    // Attach Player listeners & Analytics
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                isLoading = false
                liveAutoRetryCount = 0
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("PlayerScreen", "ExoPlayer playback error: ${error.errorCodeName} (code=${error.errorCode})", error)

                val isLiveOrDynamic = isLive ||
                        com.beouttv.app.data.source.stream.OkRuStreamResolver.isOkRuUrl(activeStreamUrl) ||
                        com.beouttv.app.data.source.stream.LiveGatewayStreamResolver.isTargetApiUrl(activeStreamUrl) ||
                        mediaId.startsWith("ok_") || mediaId.startsWith("okru_") || mediaId.startsWith("gw_")

                // 1. Live Window Drift, Corrupt Manifest, or Transient Network Auto-Recovery
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
                        if (isLiveOrDynamic && liveAutoRetryCount < 5) {
                            liveAutoRetryCount++
                            android.util.Log.w("PlayerScreen", "Auto-recovering live stream from ${error.errorCodeName}, attempt $liveAutoRetryCount/5")
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(500L * liveAutoRetryCount)
                                val isOkRu = com.beouttv.app.data.source.stream.OkRuStreamResolver.isOkRuUrl(activeStreamUrl) ||
                                        mediaId.startsWith("ok_") || mediaId.startsWith("okru_")
                                if (isOkRu) {
                                    val resolved = com.beouttv.app.data.source.stream.OkRuStreamResolver.resolve(activeStreamUrl)
                                    if (resolved != null && resolved.streamUrl.isNotBlank()) {
                                        activeStreamUrl = resolved.streamUrl
                                        loadStreamInternal(resolved.streamUrl)
                                        return@launch
                                    }
                                }
                                exoPlayer.seekToDefaultPosition()
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                            return
                        }
                    }
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                        if (!isSoftAudioDecode) {
                            android.util.Log.w("PlayerScreen", "mytv audio fallback: hardware decoder failed, activating software decode...")
                            isSoftAudioDecode = true
                            return
                        }
                    }
                }

                // 2. Auto-Switch to Alternative Server if enabled (mytv multi-source switching)
                val autoSwitch = PlayerPreferences.isAutoSwitchServerOnErrorEnabled(context) || activeServersList.size > 1
                if (autoSwitch && activeServersList.size > 1) {
                    val currentIndex = activeServersList.indexOfFirst { it.url == activeStreamUrl || it.id == selectedServerId }
                    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % activeServersList.size else 0
                    if (nextIndex != currentIndex && nextIndex in activeServersList.indices) {
                        val nextServer = activeServersList[nextIndex]
                        selectedServerId = nextServer.id
                        selectedQualityLabel = nextServer.quality
                        activeStreamUrl = nextServer.url
                        loadStream(nextServer.url, exoPlayer.currentPosition)
                        android.widget.Toast.makeText(
                            context,
                            "جاري التحويل تلقائياً إلى سيرفر بديل: ${nextServer.name}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                }

                errorMessage = getDetailedPlayerErrorMessage(error)
                isLoading = false
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                availableTracks = tracks

                // Auto-apply MovieBox / Arabic subtitle track if selected or available
                val targetSub = selectedSubtitleTrackObj 
                    ?: activeSubtitles.firstOrNull { it.language == "ar" && it.label.contains("MovieBox") }
                    ?: activeSubtitles.firstOrNull { it.language == "ar" }

                if (targetSub != null && selectedSubtitleLabel != "إيقاف الترجمة (Off)") {
                    var applied = false
                    tracks.groups.forEach { group ->
                        if (group.type == C.TRACK_TYPE_TEXT) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val isMatch = (targetSub.id.isNotBlank() && format.id == targetSub.id) ||
                                        (targetSub.label.isNotBlank() && format.label.equals(targetSub.label, ignoreCase = true)) ||
                                        (format.language == "ar" || format.language == "ara")
                                if (isMatch) {
                                    val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .addOverride(override)
                                        .build()
                                    applied = true
                                    break
                                }
                            }
                        }
                        if (applied) return@forEach
                    }
                    if (!applied) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setPreferredTextLanguage("ar")
                            .build()
                    }
                }

                if (isSlowNetMode) {
                    var minW = Int.MAX_VALUE
                    var minH = Int.MAX_VALUE
                    var minBitrate = Int.MAX_VALUE
                    tracks.groups.forEach { group ->
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                if (format.width > 0 && format.height > 0) {
                                    if (format.height < minH) {
                                        minH = format.height
                                        minW = format.width
                                        minBitrate = if (format.bitrate > 0) format.bitrate else minBitrate
                                    }
                                }
                            }
                        }
                    }
                    if (minH != Int.MAX_VALUE && minW != Int.MAX_VALUE) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoSize(minW, minH)
                            .apply {
                                if (minBitrate != Int.MAX_VALUE && minBitrate > 0) {
                                    setMaxVideoBitrate((minBitrate * 1.25f).toInt())
                                }
                            }
                            .build()
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        errorMessage = null
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                        isLiveStream = exoPlayer.isCurrentMediaItemLive || duration <= 0L

                        if (initialSeek > 0L && !hasSeekedInitially && duration > 0L) {
                            hasSeekedInitially = true
                            if (initialSeek < duration - 10000L) {
                                exoPlayer.seekTo(initialSeek)
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        isLoading = false
                        isPlaying = false
                        if (!isLive && !isLiveStream && mediaId.isNotBlank()) {
                            PlaybackHistoryManager.saveProgress(
                                context = context,
                                id = mediaId,
                                title = title,
                                streamUrl = activeStreamUrl,
                                thumbnailUrl = backdropUrl.ifBlank { posterUrl },
                                positionMs = duration,
                                durationMs = duration,
                                isLive = isLive || isLiveStream,
                                backdropUrl = backdropUrl
                            )
                        }
                    }
                    Player.STATE_IDLE -> Unit
                }
            }
        }

        // Real-time Technical Analytics & Stream Diagnostics (mytv VideoPlayerMetadata)
        val analyticsListener = object : AnalyticsListener {
            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                streamMimeType = format.sampleMimeType ?: format.containerMimeType ?: ""
                val b = if (format.bitrate > 0) format.bitrate.toLong() else if (format.averageBitrate > 0) format.averageBitrate.toLong() else 0L
                if (b > 0L) streamBitrate = b
                if (format.frameRate > 0) streamFrameRate = format.frameRate
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                streamDecoder = decoderName
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                streamAudioMime = format.sampleMimeType ?: ""
                streamAudioChannels = format.channelCount
                streamAudioSampleRate = format.sampleRate
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                streamAudioDecoder = decoderName
            }
        }

        exoPlayer.addListener(listener)
        exoPlayer.addAnalyticsListener(analyticsListener)
        onDispose {
            val finalPos = exoPlayer.currentPosition
            val finalDur = exoPlayer.duration
            if (!isLive && !isLiveStream && finalDur > 10000L && finalPos > 3000L) {
                PlaybackHistoryManager.saveProgress(
                    context = context,
                    id = mediaId,
                    title = title,
                    streamUrl = activeStreamUrl,
                    thumbnailUrl = backdropUrl.ifBlank { posterUrl },
                    positionMs = finalPos,
                    durationMs = finalDur,
                    isLive = isLive || isLiveStream,
                    backdropUrl = backdropUrl
                )
            }
            exoPlayer.removeListener(listener)
            exoPlayer.removeAnalyticsListener(analyticsListener)
            exoPlayer.release()
        }
    }

    // Stop playback immediately when user exits via Home button or minimizes app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    val activity = context as? Activity
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true
                    if (!inPip) {
                        exoPlayer.pause()
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

    // Position tracking & continuous history saving
    LaunchedEffect(isPlaying) {
        var lastSavedPos = 0L
        while (true) {
            val curPos = exoPlayer.currentPosition
            val bufPos = exoPlayer.bufferedPosition
            streamBufferedSeconds = if (bufPos > curPos) (bufPos - curPos) / 1000f else 0f

            if (exoPlayer.isPlaying) {
                currentPosition = curPos
                duration = exoPlayer.duration.coerceAtLeast(0L)
                val liveItem = exoPlayer.isCurrentMediaItemLive || duration <= 0L
                if (liveItem) isLiveStream = true

                if (!isLive && !isLiveStream && currentPosition > 3000L && Math.abs(currentPosition - lastSavedPos) >= 5000L && mediaId.isNotBlank()) {
                    lastSavedPos = currentPosition
                    PlaybackHistoryManager.saveProgress(
                        context = context,
                        id = mediaId,
                        title = title,
                        streamUrl = activeStreamUrl,
                        thumbnailUrl = backdropUrl.ifBlank { posterUrl },
                        positionMs = currentPosition,
                        durationMs = duration,
                        isLive = false,
                        backdropUrl = backdropUrl
                    )
                }
            }
            delay(1000)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4500)
            if (activeSheet == null) {
                controlsVisible = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (activeSheet != null) {
                            activeSheet = null
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.4f) {
                            exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10000L))
                        } else if (offset.x > screenWidth * 0.6f) {
                            val target = if (duration > 0L) minOf(duration, exoPlayer.currentPosition + 10000L) else exoPlayer.currentPosition + 10000L
                            exoPlayer.seekTo(target)
                        }
                    }
                )
            }
    ) {
        // Fast First-Frame Poster Backdrop (Eliminates harsh black screen void)
        if (!hasRenderedFirstFrame && posterUrl.isNotBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            radius = 1200f
                        )
                    )
            )
        }

        val primaryPlayerContent: @Composable () -> Unit = {
            if (isWebEmbed) {
                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 15; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = false
                            }
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                            setOnTouchListener { _, event ->
                                if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_UP) {
                                    webEmbedControlsVisible = true
                                }
                                false
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {}
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val reqUrl = request?.url?.toString().orEmpty()
                                    val lowerReq = reqUrl.lowercase()
                                    if (lowerReq.contains("vidlink.pro") || lowerReq.contains("videasy.to") ||
                                        lowerReq.contains("zxcstream") || lowerReq.contains("2embed") ||
                                        lowerReq.contains("google") || lowerReq.contains("gstatic") ||
                                        lowerReq.contains("cloudflare") || lowerReq.contains("jsdelivr") ||
                                        lowerReq.contains("vidsrc") || lowerReq.contains("stream") ||
                                        lowerReq.contains("embed")) {
                                        return false
                                    }
                                    return true
                                }
                            }
                            loadUrl(activeStreamUrl)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        if (webView.url != activeStreamUrl) {
                            webView.loadUrl(activeStreamUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val displayModeModifier = when (resizeModeIndex) {
                    2 -> Modifier.wrapContentSize(Alignment.Center, unbounded = true)
                    else -> Modifier.fillMaxSize()
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.resizeMode = when (resizeModeIndex) {
                                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            keepScreenOn = true
                            setKeepContentOnPlayerReset(true)
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            PlayerPreferences.applySubtitleSettings(this.subtitleView, ctx)
                            playerViewRef = this
                        }
                    },
                    update = { view ->
                        view.resizeMode = when (resizeModeIndex) {
                            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        PlayerPreferences.applySubtitleSettings(view.subtitleView, view.context)
                        playerViewRef = view
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(displayModeModifier)
                )
            }
        }

        if (isMultiViewActive) {
            MultiViewGrid(
                slots = multiViewSlots,
                focusedSlotIndex = focusedSlotIndex,
                audioSlotIndex = audioSlotIndex,
                expandedSlotIndex = expandedFullscreenSlotIndex,
                forcePrimaryWatermark = shouldShowWatermark,
                primaryVideoWidth = videoWidth,
                primaryVideoHeight = videoHeight,
                primaryQualityLabel = selectedQualityLabel,
                onOpenQualitySlot = { idx ->
                    if (idx == 0) {
                        activeSheet = PlayerSheetType.QUALITY
                    }
                },
                onFocusSlot = { idx ->
                    focusedSlotIndex = idx
                    audioSlotIndex = idx
                },
                onToggleAudioSlot = { idx ->
                    audioSlotIndex = idx
                    focusedSlotIndex = idx
                },
                onCloseSlot = { idx ->
                    if (expandedFullscreenSlotIndex == idx) {
                        expandedFullscreenSlotIndex = null
                    }
                    if (idx == 0) {
                        if (multiViewSlots.size > 1) {
                            val promoted = multiViewSlots[1]
                            multiViewSlots = multiViewSlots.drop(1).mapIndexed { i, s -> s.copy(slotId = i) }
                            activeStreamUrl = promoted.streamUrl
                            loadStream(promoted.streamUrl, 0L)
                            focusedSlotIndex = 0
                            audioSlotIndex = 0
                        } else {
                            onBack()
                        }
                    } else {
                        multiViewSlots = multiViewSlots.filterIndexed { i, _ -> i != idx }.mapIndexed { i, s -> s.copy(slotId = i) }
                        if (focusedSlotIndex >= multiViewSlots.size) focusedSlotIndex = 0
                        if (audioSlotIndex >= multiViewSlots.size) audioSlotIndex = 0
                    }
                },
                onExpandFullscreen = { idx ->
                    expandedFullscreenSlotIndex = idx
                    focusedSlotIndex = idx
                    audioSlotIndex = idx
                },
                onExitFullscreen = {
                    expandedFullscreenSlotIndex = null
                },
                onCycleAspectSlot = { idx ->
                    if (idx == 0) {
                        resizeModeIndex = (resizeModeIndex + 1) % 3
                    } else {
                        multiViewSlots = multiViewSlots.mapIndexed { i, s ->
                            if (i == idx) s.copy(resizeModeIndex = (s.resizeModeIndex + 1) % 3) else s
                        }
                    }
                },
                aspectLabels = listOf("Fit", "Fill", "Zoom"),
                onReloadSlot = { idx ->
                    if (idx == 0) {
                        loadStream(activeStreamUrl, exoPlayer.currentPosition)
                    }
                },
                onOpenChannelPicker = {
                    showMultiViewChannelPicker = true
                },
                primaryPlayerContent = primaryPlayerContent
            )
        } else {
            primaryPlayerContent()
        }

        // Official BeoutTV Watermark (Permanently covers Ostora logo on beIN Sports streams across ALL modes: loading, errors, retries, and PiP)
        if (shouldShowWatermark && !isMultiViewActive) {
            val videoAspectRatio = remember(videoWidth, videoHeight) {
                if (videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else (16f / 9f)
            }
            val containerAspectRatio = maxWidth.value / maxHeight.value

            // Compute precise video frame geometry relative to screen
            val (videoLeft, videoTop, currentVideoWidth, currentVideoHeight) = remember(
                resizeModeIndex,
                containerAspectRatio,
                videoAspectRatio,
                maxWidth,
                maxHeight
            ) {
                when (resizeModeIndex) {
                    1 -> {
                        // RESIZE_MODE_FILL (Stretched to screen)
                        listOf(0.dp, 0.dp, maxWidth, maxHeight)
                    }
                    2 -> {
                        // RESIZE_MODE_ZOOM (Cropped to fill screen)
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
                        // RESIZE_MODE_FIT (Letterbox / Pillarbox)
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

            // Empirically measured from live Ostora broadcasts:
            // Ostora banner is horizontally centered at 9.87% from video frame's right edge
            // and vertically centered at 8.70% from video frame's top edge.
            // Watermark size adjusted per user request: width -10%, height shortened by 12%
            val baseWatermarkWidth = (currentVideoWidth * 0.168f) * 0.90f
            val watermarkWidth = baseWatermarkWidth.coerceIn(if (isInPipMode) 14.dp else 43.dp, 103.dp)
            val watermarkHeight = (watermarkWidth / 3.0f) * 0.88f

            val effectiveVideoLeft = maxOf(0.dp, videoLeft)
            val effectiveVideoTop = maxOf(0.dp, videoTop)

            val ostoraCenterX = effectiveVideoLeft + currentVideoWidth - (currentVideoWidth * 0.0987f)
            val ostoraCenterY = effectiveVideoTop + (currentVideoHeight * 0.0870f)

            val watermarkX = (ostoraCenterX - (watermarkWidth / 2f))
                .coerceIn(0.dp, (maxWidth - watermarkWidth).coerceAtLeast(0.dp))
            val watermarkY = (ostoraCenterY - (watermarkHeight / 2f))
                .coerceIn(0.dp, (maxHeight - watermarkHeight).coerceAtLeast(0.dp))

            // Enforce LTR coordinate system so watermark is strictly placed on the physical RIGHT side
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
                            .clip(RoundedCornerShape(5.dp))
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

        // Loading Indicator Overlay
        if (isLoading && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BeoutColors.BrightBlue,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        // Error State Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .zIndex(180f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = "Error",
                        tint = BeoutColors.LiveRed,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "حدث خطأ غير متوقع",
                        style = BeoutTextStyles.CardTitle.copy(fontSize = 15.sp),
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                errorMessage = null
                                loadStream(activeStreamUrl, currentPosition)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BeoutColors.BrightBlue),
                            shape = BeoutShapes.Small,
                            modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إعادة المحاولة", color = Color.White)
                        }
                        if (activeServersList.size > 1) {
                            Button(
                                onClick = {
                                    val currentIndex = activeServersList.indexOfFirst { it.url == activeStreamUrl || it.id == selectedServerId }
                                    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % activeServersList.size else 0
                                    val nextServer = activeServersList[nextIndex]
                                    selectedServerId = nextServer.id
                                    selectedQualityLabel = nextServer.quality
                                    activeStreamUrl = nextServer.url
                                    errorMessage = null
                                    loadStream(nextServer.url, exoPlayer.currentPosition)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = BeoutShapes.Small,
                                modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تبديل السيرفر", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    activeSheet = PlayerSheetType.SERVERS
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                shape = BeoutShapes.Small,
                                modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                            ) {
                                Icon(Icons.Rounded.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("قائمة السيرفرات", color = Color.White)
                            }
                        }
                        OutlinedButton(
                            onClick = onBack,
                            shape = BeoutShapes.Small,
                            modifier = Modifier.tvFocusable(shape = BeoutShapes.Small)
                        ) {
                            Text("رجوع", color = Color.White)
                        }
                    }
                }
            }
        }

        // Stats for Nerds (Technical Diagnostics Card)
        if (showStatsOverlay && !isInPipMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 56.dp, start = 16.dp)
                    .widthIn(max = 290.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xDD0D131F))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .zIndex(500f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إحصائيات البث المباشر (Stats for Nerds)",
                            style = BeoutTextStyles.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                color = BeoutColors.BrightBlue,
                                fontSize = 11.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Stats",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    showStatsOverlay = false
                                    PlayerPreferences.setShowPlayerStatsEnabled(context, false)
                                }
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    val statBitrate = if (streamBitrate > 0) {
                        if (streamBitrate >= 1_000_000) String.format(java.util.Locale.US, "%.2f Mbps", streamBitrate / 1_000_000f)
                        else "${streamBitrate / 1000} kbps"
                    } else "تلقائي (Adaptive)"

                    val statFps = if (streamFrameRate > 0) String.format(java.util.Locale.US, "%.1f fps", streamFrameRate) else "--"
                    val statBuffer = String.format(java.util.Locale.US, "%.1fs", streamBufferedSeconds)
                    val statRes = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x${videoHeight}" else "--"
                    val statVideoCodec = if (streamMimeType.isNotBlank()) streamMimeType.substringAfter("/") else "H.264 / HEVC"
                    val statAudioCodec = if (streamAudioMime.isNotBlank()) streamAudioMime.substringAfter("/") else "AAC"

                    StatRow("الدقة الحالية", statRes)
                    StatRow("معدل البت", statBitrate)
                    StatRow("معدل الإطارات", statFps)
                    StatRow("البفر المتبقي", statBuffer)
                    StatRow("فيديو كودك", "$statVideoCodec ${if (streamDecoder.isNotBlank()) "($streamDecoder)" else ""}".trim())
                    StatRow("صوت كودك", "$statAudioCodec ${if (streamAudioChannels > 0) "($streamAudioChannels ch)" else ""}".trim())
                    StatRow("وضع النت الضعيف", if (isSlowNetMode) "مُفعّل (Booster ON)" else "معطل (عادي)")
                    StatRow("محرك الإخراج", if (renderEngine == "texture") "TextureView" else "SurfaceView (عتادي)")
                }
            }
        }

        // Discreet Auto-Hiding Back Button for Web Embed (Zero servers button, syncs with touch)
        AnimatedVisibility(
            visible = isWebEmbed && webEmbedControlsVisible && errorMessage == null && activeSheet == null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(350)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .zIndex(70f)
        ) {
            FloatingCircleButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = S.back(),
                onClick = onBack,
                modifier = if (isTvDevice) Modifier.tvFocusable(shape = CircleShape) else Modifier
            )
        }

        // Main Controls Overlay (ExoPlayer only, NEVER shown on Web Embed)
        AnimatedVisibility(
            visible = !isWebEmbed && controlsVisible && errorMessage == null && activeSheet == null && !showMultiViewChannelPicker && !showTvChannelDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(50f)
        ) {
            PlayerControlsOverlay(
                title = title,
                isPlaying = if (isWebEmbed) webEmbedPlaying else isPlaying,
                isLive = isLiveStream,
                currentPosition = currentPosition,
                duration = duration,
                resizeModeIndex = resizeModeIndex,
                hasServers = activeServersList.isNotEmpty(),
                servers = activeServersList,
                selectedServerId = selectedServerId,
                isResolvingServers = isResolvingServers,
                activeStreamUrl = activeStreamUrl,
                onSelectServer = { server ->
                    selectedServerId = server.id
                    selectedQualityLabel = server.quality
                    if (server.url != activeStreamUrl) {
                        com.beouttv.app.data.ads.AdsterraManager.onServerSwitched(context, server.name)
                        activeStreamUrl = server.url
                        loadStream(server.url, exoPlayer.currentPosition)
                    }
                },
                onBack = {
                    if (isMultiViewActive) {
                        multiViewSlots = listOf(multiViewSlots[0])
                        focusedSlotIndex = 0
                        audioSlotIndex = 0
                    } else {
                        onBack()
                    }
                },
                onPlayPause = {
                    if (isWebEmbed) {
                        webEmbedPlaying = !webEmbedPlaying
                        val js = if (webEmbedPlaying) "document.querySelector('video')?.play();" else "document.querySelector('video')?.pause();"
                        webViewRef?.evaluateJavascript(js, null)
                    } else {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                },
                onRewind10 = {
                    if (isWebEmbed) {
                        webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) v.currentTime = Math.max(0, v.currentTime - 10);", null)
                    } else {
                        exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10000L))
                    }
                },
                onForward10 = {
                    if (isWebEmbed) {
                        webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) v.currentTime = v.currentTime + 10;", null)
                    } else {
                        val target = if (duration > 0L) minOf(duration, exoPlayer.currentPosition + 10000L) else exoPlayer.currentPosition + 10000L
                        exoPlayer.seekTo(target)
                    }
                },
                onSeek = { targetMs ->
                    exoPlayer.seekTo(targetMs)
                    currentPosition = targetMs
                },
                onOpenServers = { activeSheet = PlayerSheetType.SERVERS },
                onOpenQuality = { activeSheet = PlayerSheetType.QUALITY },
                onOpenAudioSubtitles = { activeSheet = PlayerSheetType.AUDIO_SUBTITLES },
                onOpenAspectRatio = { activeSheet = PlayerSheetType.ASPECT_RATIO },
                onOpenMultiView = {
                    controlsVisible = false
                    showMultiViewChannelPicker = true
                },
                multiViewCount = multiViewSlots.size,
                isMultiViewActive = isMultiViewActive,
                multiViewSlots = multiViewSlots,
                focusedSlotIndex = focusedSlotIndex,
                audioSlotIndex = audioSlotIndex,
                onFocusSlot = { idx ->
                    focusedSlotIndex = idx
                    audioSlotIndex = idx
                },
                onToggleAudioSlot = { idx ->
                    audioSlotIndex = idx
                    focusedSlotIndex = idx
                },
                onExitMultiView = {
                    multiViewSlots = listOf(multiViewSlots[0])
                    focusedSlotIndex = 0
                    audioSlotIndex = 0
                },
                onVolumeUp = {
                    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                    audioManager?.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                    if (isWebEmbed) {
                        webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) v.volume = Math.min(1.0, v.volume + 0.1);", null)
                    }
                },
                onVolumeDown = {
                    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                    audioManager?.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                    if (isWebEmbed) {
                        webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) v.volume = Math.max(0.0, v.volume - 0.1);", null)
                    }
                }
            )
        }

        // Smart TV Remote Shortcuts Legend (Color Keys)
        if (!isWebEmbed && isTvDevice && controlsVisible && !showTvChannelDrawer && activeSheet == null) {
            TvRemoteShortcutsLegend(
                isMultiViewActive = isMultiViewActive,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(60f)
            )
        }

        // Smart TV Quick Channel Flipping Drawer (D-Pad UP/DOWN)
        if (isTvDevice && showTvChannelDrawer) {
            TvQuickChannelDrawer(
                channels = availableTvChannels,
                currentChannelId = mediaId,
                onSelectChannel = { channel ->
                    showTvChannelDrawer = false
                    coroutineScope.launch {
                        val resolved = resolveMultiViewChannelStream(channel)
                        if (isMultiViewActive) {
                            multiViewSlots = multiViewSlots.toMutableList().apply {
                                this[focusedSlotIndex] = this[focusedSlotIndex].copy(
                                    channelId = resolved.id,
                                    name = resolved.name,
                                    logoUrl = resolved.logoUrl,
                                    streamUrl = resolved.streamUrl,
                                    userAgent = resolved.userAgent,
                                    referer = resolved.referer,
                                    drmKeyId = resolved.drmKeyId,
                                    drmKey = resolved.drmKey,
                                    groupId = resolved.groupId,
                                    isLive = resolved.isLive
                                )
                            }
                            if (focusedSlotIndex == 0) {
                                activeStreamUrl = resolved.streamUrl
                                loadStream(resolved.streamUrl)
                            }
                        } else {
                            activeStreamUrl = resolved.streamUrl
                            loadStream(resolved.streamUrl)
                        }
                    }
                },
                onDismiss = { showTvChannelDrawer = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(30f)
            )
        }

        // Multi-View Channel Picker Sheet
        if (showMultiViewChannelPicker) {
            MultiViewChannelPickerSheet(
                activeSlots = multiViewSlots,
                onChannelSelected = { selectedChannel ->
                    if (multiViewSlots.size < 4) {
                        val newSlot = MultiViewSlot(
                            slotId = multiViewSlots.size,
                            channelId = selectedChannel.id,
                            name = selectedChannel.name,
                            logoUrl = selectedChannel.logoUrl,
                            streamUrl = selectedChannel.streamUrl,
                            userAgent = selectedChannel.userAgent,
                            referer = selectedChannel.referer,
                            drmKeyId = selectedChannel.drmKeyId,
                            drmKey = selectedChannel.drmKey,
                            groupId = selectedChannel.groupId,
                            isLive = selectedChannel.isLive
                        )
                        multiViewSlots = multiViewSlots + newSlot
                        focusedSlotIndex = multiViewSlots.size - 1
                        audioSlotIndex = multiViewSlots.size - 1
                        showMultiViewChannelPicker = false
                    }
                },
                onDismiss = { showMultiViewChannelPicker = false }
            )
        }

        // Settings Glass Sheet / Modal
        AnimatedVisibility(
            visible = activeSheet != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(200f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { activeSheet = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                activeSheet?.let { sheetType ->
                    PlayerSettingsSheet(
                    type = sheetType,
                    selectedQuality = selectedQualityLabel,
                    selectedAudio = selectedAudioLabel,
                    selectedSubtitle = selectedSubtitleLabel,
                    subtitleDelayMs = subtitleDelayMs,
                    selectedSubtitleTrack = selectedSubtitleTrackObj,
                    onAdjustSubtitleDelay = { newOffset ->
                        subtitleDelayMs = newOffset
                        val track = selectedSubtitleTrackObj
                        if (track != null && track.url.isNotBlank()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val rawUrl = track.url
                                    val rawContent = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                                        val conn = java.net.URL(rawUrl).openConnection() as java.net.HttpURLConnection
                                        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                                        conn.connectTimeout = 8000
                                        conn.readTimeout = 8000
                                        conn.inputStream.bufferedReader().use { it.readText() }
                                    } else if (rawUrl.startsWith("file://")) {
                                        File(Uri.parse(rawUrl).path ?: "").readText()
                                    } else {
                                        File(rawUrl).readText()
                                    }
                                    if (rawContent.isNotBlank()) {
                                        val shifted = shiftSrtTimestamps(rawContent, newOffset)
                                        val safeId = track.id.filter { it.isLetterOrDigit() }.ifBlank { "sub" }
                                        val cacheFile = File(context.cacheDir, "synced_sub_${safeId}.srt")
                                        cacheFile.writeText(shifted)
                                        withContext(Dispatchers.Main) {
                                            val newUri = Uri.fromFile(cacheFile).toString()
                                            val updatedTrack = track.copy(url = newUri)
                                            val idx = activeSubtitles.indexOfFirst { it.id == track.id }
                                            if (idx != -1) {
                                                activeSubtitles[idx] = updatedTrack
                                            }
                                            loadStream(activeStreamUrl, exoPlayer.currentPosition)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    resizeModeIndex = resizeModeIndex,
                    availableTracks = availableTracks,
                    subtitles = activeSubtitles,
                    servers = activeServersList,
                    selectedServerId = selectedServerId,
                    isResolvingServers = isResolvingServers,
                    activeStreamUrl = activeStreamUrl,
                    title = title,
                    isSlowNetMode = isSlowNetMode,
                    onToggleSlowNetMode = { enabled ->
                        isSlowNetMode = enabled
                        PlayerPreferences.setSlowInternetModeEnabled(context, enabled)
                        if (enabled) {
                            var minWidth = Int.MAX_VALUE
                            var minHeight = Int.MAX_VALUE
                            var minBitrate = Int.MAX_VALUE
                            availableTracks?.groups?.forEach { group ->
                                if (group.type == C.TRACK_TYPE_VIDEO) {
                                    for (i in 0 until group.length) {
                                        val f = group.getTrackFormat(i)
                                        if (f.height in 1..<minHeight) {
                                            minHeight = f.height
                                            minWidth = f.width
                                            if (f.bitrate > 0) minBitrate = f.bitrate
                                        }
                                    }
                                }
                            }
                            val targetMaxW = if (minWidth != Int.MAX_VALUE) minWidth else 854
                            val targetMaxH = if (minHeight != Int.MAX_VALUE) minHeight else 480

                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setMaxVideoSize(targetMaxW, targetMaxH)
                                .setMaxVideoBitrate(Int.MAX_VALUE)
                                .build()

                            val lowServer = activeServersList.minByOrNull { srv ->
                                val q = srv.quality.uppercase()
                                when {
                                    q.contains("360") || q.contains("LOW") -> 360
                                    q.contains("480") || q.contains("SD") -> 480
                                    q.contains("720") || q.contains("HD") -> 720
                                    q.contains("1080") || q.contains("FHD") -> 1080
                                    else -> 9999
                                }
                            }
                            if (lowServer != null && lowServer.url.isNotBlank() && lowServer.url != activeStreamUrl) {
                                selectedServerId = lowServer.id
                                selectedQualityLabel = lowServer.quality
                                activeStreamUrl = lowServer.url
                                loadStream(lowServer.url, exoPlayer.currentPosition)
                            }
                        } else {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                                .setMaxVideoBitrate(Int.MAX_VALUE)
                                .build()
                        }
                    },
                    showPlayerStats = showStatsOverlay,
                    onTogglePlayerStats = { enabled ->
                        showStatsOverlay = enabled
                        PlayerPreferences.setShowPlayerStatsEnabled(context, enabled)
                    },
                    onSelectQuality = { qualityLabel, maxW, maxH, totalQualities ->
                        selectedQualityLabel = qualityLabel

                        // Smart Adsterra Direct Link: Trigger when user selects high definition (1080p, 720p, 740p)
                        // ONLY if multiple qualities are available!
                        com.beouttv.app.data.ads.AdsterraManager.onQualitySelected(context, qualityLabel, totalQualities)

                        // Smart Adaptive Engine: Map to matching quality server if present
                        val matchingServer = activeServersList.firstOrNull {
                            (qualityLabel.contains("1080") && (it.quality.contains("1080") || it.name.contains("1080"))) ||
                            (qualityLabel.contains("720") && (it.quality.contains("720") || it.name.contains("720"))) ||
                            (qualityLabel.contains("480") && (it.quality.contains("480") || it.name.contains("480"))) ||
                            (qualityLabel.contains("360") && (it.quality.contains("360") || it.name.contains("360")))
                        }
                        if (matchingServer != null && matchingServer.url.isNotBlank() && matchingServer.url != activeStreamUrl) {
                            selectedServerId = matchingServer.id
                            activeStreamUrl = matchingServer.url
                            loadStream(matchingServer.url, exoPlayer.currentPosition)
                        } else if (maxW == Int.MAX_VALUE) {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                                .setMinVideoSize(0, 0)
                                .build()
                        } else {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setMaxVideoSize(maxW, maxH)
                                .setMinVideoSize(maxW, maxH)
                                .build()
                        }
                        activeSheet = null
                    },
                    onSelectAudio = { label, groupIdx, trackIdx ->
                        selectedAudioLabel = label
                        if (groupIdx >= 0 && trackIdx >= 0 && groupIdx < availableTracks.groups.size) {
                            val group = availableTracks.groups[groupIdx]
                            if (trackIdx < group.length) {
                                val override = TrackSelectionOverride(group.mediaTrackGroup, trackIdx)
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                    .addOverride(override)
                                    .build()
                            }
                        } else {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .build()
                        }
                        activeSheet = null
                    },
                    onSelectSubtitle = { track, disable ->
                        if (disable || track == null) {
                            selectedSubtitleLabel = "إيقاف الترجمة (Off)"
                            selectedSubtitleTrackObj = null
                            subtitleDelayMs = 0L
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .build()
                        } else {
                            selectedSubtitleLabel = "${track.label} (${track.language.uppercase()})"
                            selectedSubtitleTrackObj = track
                            subtitleDelayMs = 0L
                            var applied = false
                            availableTracks.groups.forEach { group ->
                                if (group.type == C.TRACK_TYPE_TEXT) {
                                    for (i in 0 until group.length) {
                                        val format = group.getTrackFormat(i)
                                        val isMatch = (track.id.isNotBlank() && format.id == track.id) ||
                                                (track.label.isNotBlank() && format.label.equals(track.label, ignoreCase = true))
                                        if (isMatch) {
                                            val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                                .addOverride(override)
                                                .build()
                                            applied = true
                                            break
                                        }
                                    }
                                }
                                if (applied) return@forEach
                            }
                            if (!applied) {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setPreferredTextLanguage(track.language)
                                    .build()

                                if (track.url.isNotBlank()) {
                                    coroutineScope.launch {
                                        val cleanUri = if (track.url.startsWith("http") && com.beouttv.app.data.subtitle.SubtitleCharsetHelper.isSubtitleUrl(track.url)) {
                                            com.beouttv.app.data.subtitle.SubtitleCharsetHelper.resolveCleanSubtitleUri(context, track.url)
                                        } else track.url
                                        val updatedTrack = track.copy(url = cleanUri)
                                        val existingIdx = activeSubtitles.indexOfFirst { it.id == track.id || it.label == track.label }
                                        if (existingIdx != -1) {
                                            activeSubtitles[existingIdx] = updatedTrack
                                        } else {
                                            activeSubtitles.add(updatedTrack)
                                        }
                                        loadStream(activeStreamUrl, exoPlayer.currentPosition)
                                    }
                                }
                            }
                        }
                        activeSheet = null
                    },
                    onSelectServer = { server ->
                        selectedServerId = server.id
                        selectedQualityLabel = server.quality
                        if (server.url != activeStreamUrl) {
                            com.beouttv.app.data.ads.AdsterraManager.onServerSwitched(context, server.name)
                            activeStreamUrl = server.url
                            loadStream(server.url, exoPlayer.currentPosition)
                        }
                        activeSheet = null
                    },
                    onSelectAspectRatio = { newIndex ->
                        resizeModeIndex = newIndex
                        val modeStr = when (newIndex) {
                            1 -> "Fill"
                            2 -> "Zoom"
                            else -> "Fit"
                        }
                        PlayerPreferences.setAspectRatioMode(context, modeStr)
                        activeSheet = null
                    },
                    onFetchSubtitlesOnline = {
                        coroutineScope.launch {
                            try {
                                val found = CoreApiClient().fetchSubtitles(query = title)
                                if (found.isNotEmpty()) {
                                    found.forEach { s ->
                                        if (activeSubtitles.none { it.url == s.url || it.label == s.label }) {
                                            activeSubtitles.add(s)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    onApplySubtitleAppearance = {
                        playerViewRef?.subtitleView?.let {
                            PlayerPreferences.applySubtitleSettings(it, context)
                        }
                    },
                    onClose = { activeSheet = null }
                )
            }
        }
    }

        // In-App Smart Ad Overlay (Adsterra Direct Link inside BeoutTV without opening Chrome)
        val activeAdUrl by com.beouttv.app.data.ads.AdsterraManager.activeAdUrl.collectAsState()
        var wasPlayingBeforeAd by remember { mutableStateOf(true) }

        LaunchedEffect(activeAdUrl) {
            if (!activeAdUrl.isNullOrBlank()) {
                wasPlayingBeforeAd = exoPlayer.isPlaying
                exoPlayer.pause()
            }
        }

        if (!activeAdUrl.isNullOrBlank()) {
            InAppAdOverlay(
                adUrl = activeAdUrl!!,
                onDismiss = {
                    com.beouttv.app.data.ads.AdsterraManager.dismissAd()
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }
            )
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    isLive: Boolean,
    currentPosition: Long,
    duration: Long,
    resizeModeIndex: Int,
    hasServers: Boolean = false,
    servers: List<StreamServer> = emptyList(),
    selectedServerId: String = "",
    isResolvingServers: Boolean = false,
    activeStreamUrl: String = "",
    onSelectServer: (StreamServer) -> Unit = {},
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenServers: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenAudioSubtitles: () -> Unit,
    onOpenAspectRatio: () -> Unit,
    onOpenMultiView: () -> Unit = {},
    multiViewCount: Int = 1,
    isMultiViewActive: Boolean = false,
    multiViewSlots: List<MultiViewSlot> = emptyList(),
    focusedSlotIndex: Int = 0,
    audioSlotIndex: Int = 0,
    onFocusSlot: (Int) -> Unit = {},
    onToggleAudioSlot: (Int) -> Unit = {},
    onExitMultiView: () -> Unit = {},
    onVolumeUp: () -> Unit = {},
    onVolumeDown: () -> Unit = {}
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isDraggingSlider) {
        dragProgress
    } else {
        if (duration > 0L) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val aspectLabels = listOf("Fit", "Fill", "Zoom")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        // Multi-View Control Bar (if multi-view is active)
        if (isMultiViewActive) {
            MultiViewControlBar(
                slots = multiViewSlots,
                focusedSlotIndex = focusedSlotIndex,
                audioSlotIndex = audioSlotIndex,
                onFocusSlot = onFocusSlot,
                onToggleAudioSlot = onToggleAudioSlot,
                onOpenAddChannel = onOpenMultiView,
                onExitMultiView = onExitMultiView
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .tvFocusable(
                        focusedScale = 1.15f,
                        focusedBorderColor = BeoutColors.BrightBlue,
                        focusedBorderWidth = 2.dp,
                        shape = CircleShape,
                        onEnter = onBack
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = title,
                style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        // Multi-Server Switcher Bar (Clean, smart, focused on active server and quality)
        if (servers.size > 1) {
            val serverScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(serverScrollState)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                servers.forEachIndexed { idx, srv ->
                    val isSelected = when {
                        selectedServerId.isNotBlank() && srv.id.isNotBlank() -> srv.id == selectedServerId
                        srv.url.isNotBlank() && activeStreamUrl.isNotBlank() -> {
                            val cleanA = OstoraApiResolver.cleanStreamUrl(activeStreamUrl).substringBefore("?").trim()
                            val cleanB = OstoraApiResolver.cleanStreamUrl(srv.url).substringBefore("?").trim()
                            cleanA == cleanB
                        }
                        else -> false
                    }
                    val label = sanitizeServerName(srv.name, idx, srv.quality)
                    val qualityTag = when {
                        srv.quality.contains("1080") || srv.quality.contains("FHD", true) -> "1080p"
                        srv.quality.contains("720") || srv.quality.contains("HD", true) -> "720p"
                        srv.quality.contains("480") || srv.quality.contains("SD", true) -> "480p"
                        srv.quality.contains("360") || srv.quality.contains("Low", true) -> "360p"
                        else -> srv.quality.take(5)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) BeoutColors.BrightBlue else Color(0x99121620),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .tvFocusable(
                                shape = RoundedCornerShape(16.dp),
                                onEnter = { onSelectServer(srv) }
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectServer(srv) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (qualityTag.isNotBlank() && !label.contains(qualityTag)) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = qualityTag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Center Transport Controls (Rewind 10s, Play/Pause, Forward 10s)
        if (!isMultiViewActive && multiViewCount <= 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLive) {
                    IconButton(
                        onClick = onRewind10,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .tvFocusable(
                                focusedScale = 1.15f,
                                focusedBorderColor = BeoutColors.BrightBlue,
                                focusedBorderWidth = 2.dp,
                                shape = CircleShape,
                                onEnter = onRewind10
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.width(36.dp))
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BeoutColors.BrightBlue)
                        .tvFocusable(
                            focusedScale = 1.15f,
                            focusedBorderColor = Color.White,
                            focusedBorderWidth = 3.dp,
                            shape = CircleShape,
                            onEnter = onPlayPause
                        )
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                if (!isLive) {
                    Spacer(Modifier.width(36.dp))

                    IconButton(
                        onClick = onForward10,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .tvFocusable(
                                focusedScale = 1.15f,
                                focusedBorderColor = BeoutColors.BrightBlue,
                                focusedBorderWidth = 2.dp,
                                shape = CircleShape,
                                onEnter = onForward10
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom Controls: Slider, Time, and Active Setting Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            if (!isLive && duration > 0L) {
                Slider(
                    value = currentProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val targetMs = (dragProgress * duration).toLong()
                        onSeek(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = BeoutColors.BrightBlue,
                        activeTrackColor = BeoutColors.BrightBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    val pulseTransition = rememberInfiniteTransition(label = "liveDotPulse")
                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(750, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotScale"
                    )
                    val pulseAlpha by pulseTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(750, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .background(Color.Red.copy(alpha = 0.45f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(Color(0xFFFF1744), CircleShape)
                        )
                    }
                } else {
                    val formattedElapsed = formatMillis(if (isDraggingSlider) (dragProgress * duration).toLong() else currentPosition)
                    val formattedTotal = formatMillis(duration)
                    Text(
                        text = "$formattedElapsed / $formattedTotal",
                        style = BeoutTextStyles.Caption.copy(fontSize = 13.sp),
                        color = Color.White
                    )
                }

                // Clean Action Buttons in Bottom Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Servers Switcher (if available)
                    if (hasServers) {
                        PlayerActionButton(
                            icon = Icons.Rounded.Dns,
                            label = tr("السيرفر", "Server", "Sunucu", "سرور", "Serveur"),
                            onClick = onOpenServers
                        )
                    }

                    // 2. Audio & Subtitles Button
                    PlayerActionButton(
                        icon = Icons.Rounded.Subtitles,
                        label = tr("الترجمة والصوت", "Audio & Subtitles", "Ses ve Altyazı", "صدا و زیرنویس", "Audio et sous-titres"),
                        onClick = onOpenAudioSubtitles
                    )

                    // 3. Quality Button
                    PlayerActionButton(
                        icon = Icons.Rounded.Hd,
                        label = S.quality(),
                        onClick = onOpenQuality
                    )

                    // 4. Aspect Ratio / Fullscreen Button
                    PlayerActionButton(
                        icon = Icons.Rounded.Fullscreen,
                        label = tr("الشاشة", "Screen", "Ekran", "صفحه", "Écran"),
                        onClick = onOpenAspectRatio
                    )

                    // 5. Volume Up & Down Buttons (TV & Touch Volume Control)
                    PlayerActionButton(
                        icon = Icons.Rounded.VolumeUp,
                        label = tr("رفع الصوت", "Vol +", "Ses +", "افزایش صدا", "Vol +"),
                        onClick = onVolumeUp
                    )

                    PlayerActionButton(
                        icon = Icons.Rounded.VolumeDown,
                        label = tr("خفض الصوت", "Vol -", "Ses -", "کاهش صدا", "Vol -"),
                        onClick = onVolumeDown
                    )

                    // 6. Multi-View (بث متعدد) Button
                    if (isLive) {
                        PlayerActionButton(
                            icon = Icons.Rounded.GridView,
                            label = if (multiViewCount > 1) "متعدد ($multiViewCount/4)" else tr("بث متعدد", "Multi-View", "Çoklu Yayın", "پخش چندگانه", "Multi-Vue"),
                            onClick = onOpenMultiView
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .tvFocusable(
                focusedScale = 1.15f,
                focusedBorderColor = BeoutColors.BrightBlue,
                focusedBorderWidth = 2.dp,
                shape = RoundedCornerShape(6.dp),
                onEnter = onClick
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = BeoutTextStyles.Caption.copy(fontSize = 9.sp),
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/**
 * Universal Glass Settings Sheet for Servers, Quality, Audio/Subtitles, Aspect Ratio.
 */
@OptIn(UnstableApi::class)
@Composable
private fun PlayerSettingsSheet(
    type: PlayerSheetType,
    selectedQuality: String,
    selectedAudio: String,
    selectedSubtitle: String,
    resizeModeIndex: Int,
    availableTracks: Tracks? = null,
    subtitles: List<SubtitleTrack> = emptyList(),
    servers: List<StreamServer> = emptyList(),
    selectedServerId: String = "",
    isResolvingServers: Boolean = false,
    activeStreamUrl: String = "",
    title: String = "",
    isSlowNetMode: Boolean = false,
    onToggleSlowNetMode: (Boolean) -> Unit = {},
    showPlayerStats: Boolean = false,
    onTogglePlayerStats: (Boolean) -> Unit = {},
    onSelectQuality: (String, Int, Int, Int) -> Unit,
    onSelectAudio: (String, Int, Int) -> Unit,
    onSelectSubtitle: (SubtitleTrack?, Boolean) -> Unit,
    subtitleDelayMs: Long = 0L,
    selectedSubtitleTrack: SubtitleTrack? = null,
    onAdjustSubtitleDelay: (Long) -> Unit = {},
    onSelectServer: (StreamServer) -> Unit,
    onSelectAspectRatio: (Int) -> Unit,
    onFetchSubtitlesOnline: () -> Unit,
    onApplySubtitleAppearance: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentSubSize by remember { mutableStateOf(PlayerPreferences.getSubtitleSizePreset(context)) }
    var currentSubStyle by remember { mutableStateOf(PlayerPreferences.getSubtitleStylePreset(context)) }
    var isSearchingOnlineSubs by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isCompactLandscape = screenHeight <= 450.dp
    val sheetPadding = if (isCompactLandscape) 8.dp else 16.dp
    val innerPadding = if (isCompactLandscape) 12.dp else 20.dp
    val listMaxHeight = if (isCompactLandscape) (screenHeight * 0.62f) else 340.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(sheetPadding)
            .clip(BeoutShapes.Large)
            .background(BeoutColors.Card.copy(alpha = 0.96f))
            .border(1.dp, BeoutColors.Divider, BeoutShapes.Large)
            .padding(innerPadding)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sheetTitle = when (type) {
                    PlayerSheetType.SERVERS -> tr("سيرفرات البث والمصادر المتاحة", "Streaming Servers & Sources", "Yayın Sunucuları", "سرورهای پخش", "Serveurs de streaming")
                    PlayerSheetType.QUALITY -> "جودة ودقة الفيديو ومحرك التكيف (Quality Engine)"
                    PlayerSheetType.AUDIO_SUBTITLES -> "المسارات الصوتية والترجمة الحقيقية (Subtitles)"
                    PlayerSheetType.ASPECT_RATIO -> "أبعاد وعرض الشاشة (Aspect Ratio)"
                }
                Text(
                    text = sheetTitle,
                    style = BeoutTextStyles.CardTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = BeoutColors.TextPrimary
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            when (type) {
                PlayerSheetType.SERVERS -> {
                    Text(
                        text = "محرك السيرفرات الذكي يكتشف تلقائياً السيرفرات الحقيقية المتاحة للقناة لضمان استقرار البث وجودته.",
                        style = BeoutTextStyles.Caption.copy(fontSize = 12.sp),
                        color = BeoutColors.TextSecondary
                    )

                    Spacer(Modifier.height(10.dp))

                    if (isResolvingServers) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BeoutColors.BrightBlue.copy(alpha = 0.10f))
                                .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BeoutColors.BrightBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "جاري فحص وتحديث السيرفرات الحقيقية المتاحة...",
                                style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                color = BeoutColors.BrightBlue
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (servers.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = listMaxHeight)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            servers.forEachIndexed { index, server ->
                                val isSelected = when {
                                    selectedServerId.isNotBlank() && server.id.isNotBlank() -> server.id == selectedServerId
                                    server.url.isNotBlank() && activeStreamUrl.isNotBlank() -> {
                                        val cleanA = OstoraApiResolver.cleanStreamUrl(activeStreamUrl).substringBefore("?").trim()
                                        val cleanB = OstoraApiResolver.cleanStreamUrl(server.url).substringBefore("?").trim()
                                        cleanA == cleanB
                                    }
                                    else -> index == 0
                                }
                                ServerSelectionCard(
                                    server = server,
                                    index = index,
                                    isSelected = isSelected,
                                    onClick = { onSelectServer(server) }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161A24))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "سيرفر البث التلقائي (Auto)",
                                    style = BeoutTextStyles.CardTitle.copy(fontSize = 14.sp),
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "يعمل البث حالياً بأعلى جودة متوفرة تلقائياً وفق سرعة اتصالك.",
                                    style = BeoutTextStyles.Caption,
                                    color = BeoutColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                PlayerSheetType.QUALITY -> {
                    Text(
                        text = "خيارات الدقة ومحرك البث التكيفي:",
                        style = BeoutTextStyles.Caption.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(6.dp))

                    // 1. Dynamic Video tracks from stream
                    val dynamicQualities = mutableListOf<Triple<String, Int, Int>>()
                    dynamicQualities.add(Triple("تلقائي (Auto - أفضل جودة حسب سرعة الإنترنت)", Int.MAX_VALUE, Int.MAX_VALUE))

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

                    // 2. If single-stream or direct video URL without adaptive playlist
                    if (dynamicQualities.size <= 1) {
                        dynamicQualities.add(Triple("1080p FHD (أعلى دقة أصلية نقية)", 1920, 1080))
                        dynamicQualities.add(Triple("720p HD (دقة عالية متوازنة وسريعة)", 1280, 720))
                        dynamicQualities.add(Triple("480p SD (توفير البيانات والشبكات الضعيفة)", 854, 480))
                        dynamicQualities.add(Triple("360p (أقصى توفير للباقة والبيانات)", 640, 360))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dynamicQualities.forEach { (label, maxW, maxH) ->
                            val isSelected = selectedQuality.startsWith(label.take(5)) || selectedQuality == label
                            val matchingSrv = servers.firstOrNull { 
                                (label.contains("1080") && (it.quality.contains("1080") || it.name.contains("1080"))) ||
                                (label.contains("720") && (it.quality.contains("720") || it.name.contains("720"))) ||
                                (label.contains("480") && (it.quality.contains("480") || it.name.contains("480")))
                            }
                            val cleanSubtitle = when {
                                label.contains("Auto") || label.contains("تلقائي") -> "محرك التكيف التلقائي لأفضل سلاسة وثبات حسب اتصالك"
                                label.contains("1080") -> "أعلى دقة نقية وتفاصيل فائقة"
                                label.contains("720") -> "دقة عالية متوازنة وسريعة الاستجابة"
                                label.contains("480") -> "توفير البيانات ومنع التقطيع على الشبكات العادية"
                                label.contains("360") -> "أقصى توفير للباقة والشبكات الضعيفة"
                                matchingSrv != null -> "سيرفر مخصص (${matchingSrv.name})"
                                else -> null
                            }

                            SheetOptionRow(
                                title = label,
                                subtitle = cleanSubtitle,
                                badge = null,
                                isSelected = isSelected,
                                onClick = { onSelectQuality(label, maxW, maxH, dynamicQualities.size) }
                            )
                        }
                    }
                }

                PlayerSheetType.AUDIO_SUBTITLES -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Audio Tracks
                        Text("المسارات الصوتية المتاحة في البث:", style = BeoutTextStyles.Caption.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))

                        val realAudioChoices = mutableListOf<Triple<String, Int, Int>>() // label, gIdx, tIdx
                        val groups = availableTracks?.groups ?: emptyList()
                        groups.forEachIndexed { gIdx, group ->
                            if (group.type == C.TRACK_TYPE_AUDIO) {
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    val lang = format.language?.uppercase() ?: ""
                                    val channelCount = format.channelCount
                                    val audioDetails = buildString {
                                        if (lang.isNotBlank()) append(lang)
                                        if (channelCount > 0) {
                                            if (isNotEmpty()) append(" • ")
                                            append(if (channelCount == 6) "5.1 Surround" else if (channelCount == 2) "Stereo" else "$channelCount ch")
                                        }
                                        if (!format.sampleMimeType.isNullOrBlank()) {
                                            val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                                            if (codec.isNotBlank()) {
                                                if (isNotEmpty()) append(" • ")
                                                append(codec)
                                            }
                                        }
                                    }
                                    val label = format.label ?: if (audioDetails.isNotBlank()) "صوت ($audioDetails)" else "المسار الصوتي ${realAudioChoices.size + 1}"
                                    realAudioChoices.add(Triple(label, gIdx, i))
                                }
                            }
                        }

                        if (realAudioChoices.isEmpty()) {
                            SheetOptionRow(
                                title = "الصوت الأساسي للبث (Default Audio)",
                                isSelected = true,
                                onClick = { onSelectAudio("الصوت الأساسي للبث (Default Audio)", -1, -1) }
                            )
                        } else {
                            realAudioChoices.forEach { (label, gIdx, tIdx) ->
                                val isSelected = (gIdx < groups.size && groups[gIdx].isTrackSelected(tIdx)) ||
                                        selectedAudio == label ||
                                        selectedAudio.startsWith(label.take(6))
                                SheetOptionRow(
                                    title = label,
                                    isSelected = isSelected,
                                    onClick = { onSelectAudio(label, gIdx, tIdx) }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("ملفات الترجمة الفعلية (Subtitles):", style = BeoutTextStyles.Caption.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))

                        // 1. Off option
                        val isOffSelected = selectedSubtitle == "إيقاف الترجمة (Off)" || selectedSubtitle.isBlank()
                        SheetOptionRow(
                            title = "إيقاف الترجمة (Off)",
                            isSelected = isOffSelected,
                            onClick = { onSelectSubtitle(null, true) }
                        )

                        // 2. Real Embedded and External Subtitles
                        val realSubtitles = mutableListOf<com.beouttv.app.data.model.SubtitleTrack>()
                        availableTracks?.groups?.forEach { group ->
                            if (group.type == C.TRACK_TYPE_TEXT) {
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    val lang = format.language ?: "ar"
                                    val label = format.label ?: "ترجمة مدمجة (${lang.uppercase()})"
                                    realSubtitles.add(com.beouttv.app.data.model.SubtitleTrack(id = "sub_emb_$i", language = lang, label = label, url = "", format = "vtt"))
                                }
                            }
                        }

                        subtitles.forEach { sub ->
                            if (realSubtitles.none { it.label == sub.label || (it.url.isNotBlank() && it.url == sub.url) }) {
                                realSubtitles.add(sub)
                            }
                        }

                        if (realSubtitles.isNotEmpty()) {
                            realSubtitles.forEach { sub ->
                                val subLabel = "${sub.label} (${sub.language.uppercase()})"
                                val isSelected = selectedSubtitle == subLabel || selectedSubtitle == sub.label || selectedSubtitle.startsWith(sub.label)
                                SheetOptionRow(
                                    title = subLabel,
                                    subtitle = "ملف ترجمة حقيقي (${sub.format.uppercase()})",
                                    isSelected = isSelected,
                                    onClick = { onSelectSubtitle(sub, false) }
                                )
                            }
                        } else {
                            Text(
                                text = "لا تتوفر مسارات ترجمة مدمجة حالياً.",
                                style = BeoutTextStyles.Caption.copy(color = BeoutColors.TextDisabled),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                        }

                        // Online Subtitles Fetch Button
                        OutlinedButton(
                            onClick = {
                                isSearchingOnlineSubs = true
                                onFetchSubtitlesOnline()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = BeoutColors.BrightBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSearchingOnlineSubs) "جاري البحث عن ترجمات..." else "البحث عن ترجمات إضافية عبر الإنترنت (OpenSubtitles / Wyzie)",
                                fontSize = 12.sp,
                                color = BeoutColors.BrightBlue
                            )
                        }

                        // Subtitle Timing / Sync Section
                        val isSubActive = selectedSubtitle != "إيقاف الترجمة (Off)" && selectedSubtitle.isNotBlank()
                        if (isSubActive) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "مزامنة توقيت الترجمة (تقديم / تأخير):",
                                    style = BeoutTextStyles.Caption.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (subtitleDelayMs == 0L) "متطابق (0.0 ث)" else "${if (subtitleDelayMs > 0) "+${subtitleDelayMs / 1000f}" else "${subtitleDelayMs / 1000f}"} ثانية",
                                    style = BeoutTextStyles.Caption.copy(
                                        color = if (subtitleDelayMs == 0L) Color.White.copy(alpha = 0.7f) else BeoutColors.BrightBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val delaySteps = listOf(
                                    "-2.0s" to -2000L,
                                    "-1.0s" to -1000L,
                                    "-0.5s" to -500L,
                                    "إعادة ضبط (0s)" to 0L,
                                    "+0.5s" to 500L,
                                    "+1.0s" to 1000L,
                                    "+2.0s" to 2000L
                                )
                                delaySteps.forEach { (label, delta) ->
                                    val isReset = delta == 0L
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isReset && subtitleDelayMs == 0L) BeoutColors.BrightBlue.copy(alpha = 0.3f) else Color(0xFF16202E))
                                            .border(1.dp, if (isReset) BeoutColors.BrightBlue.copy(alpha = 0.5f) else Color(0xFF243246), RoundedCornerShape(6.dp))
                                            .clickable {
                                                val target = if (isReset) 0L else (subtitleDelayMs + delta)
                                                onAdjustSubtitleDelay(target)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("تخصيص حجم ومظهر الترجمة الفورية:", style = BeoutTextStyles.Caption.copy(color = BeoutColors.BrightBlue, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))

                        // Subtitle Size chips
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubtitleSizePreset.entries.forEach { preset ->
                                val isSelected = currentSubSize == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) BeoutColors.BrightBlue else Color(0xFF16202E))
                                        .clickable {
                                            currentSubSize = preset
                                            PlayerPreferences.setSubtitleSizePreset(context, preset)
                                            onApplySubtitleAppearance()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(preset.displayNameAr, fontSize = 11.sp, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Subtitle Style chips
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubtitleStylePreset.entries.forEach { preset ->
                                val isSelected = currentSubStyle == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) BeoutColors.BrightBlue else Color(0xFF16202E))
                                        .clickable {
                                            currentSubStyle = preset
                                            PlayerPreferences.setSubtitleStylePreset(context, preset)
                                            onApplySubtitleAppearance()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(preset.displayNameAr.substringBefore(" ("), fontSize = 11.sp, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                PlayerSheetType.ASPECT_RATIO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val aspectOptions = listOf(
                            Triple(0, "Fit (ملاءمة الشاشة 16:9)", "عرض الفيديو بأبعاده الأصلية بالكامل دون أي مط أو قص"),
                            Triple(1, "Fill (ملء كامل الشاشة)", "تمديد وتوسيع الفيديو ليملأ الشاشة بالكامل من الحافة للحافة"),
                            Triple(2, "Zoom (تكبير وقص متناسق)", "تكبير المشهد ليملأ الشاشة بالكامل دون تشويه أبعاد الصورة")
                        )
                        aspectOptions.forEach { (idx, title, desc) ->
                            val isSelected = resizeModeIndex == idx
                            SheetOptionRow(
                                title = title,
                                subtitle = desc,
                                isSelected = isSelected,
                                onClick = { onSelectAspectRatio(idx) }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BeoutColors.Divider))
                        Spacer(Modifier.height(10.dp))

                        // Stats for Nerds Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(BeoutShapes.Medium)
                                .background(if (showPlayerStats) BeoutColors.PrimaryBlue.copy(alpha = 0.2f) else Color(0xFF131B29))
                                .border(1.dp, if (showPlayerStats) BeoutColors.BrightBlue.copy(alpha = 0.4f) else Color.Transparent, BeoutShapes.Medium)
                                .clickable { onTogglePlayerStats(!showPlayerStats) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Analytics,
                                    contentDescription = null,
                                    tint = if (showPlayerStats) BeoutColors.BrightBlue else BeoutColors.TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "عرض إحصائيات البث التقنية (Stats for Nerds)",
                                        style = BeoutTextStyles.SecondaryInfo.copy(
                                            fontWeight = if (showPlayerStats) FontWeight.Bold else FontWeight.Normal,
                                            color = if (showPlayerStats) BeoutColors.BrightBlue else Color.White
                                        )
                                    )
                                    Text(
                                        text = "معدل البت، الإطارات/ث، حجم البفر اللحظي، وأكواد فك التشفير",
                                        style = BeoutTextStyles.Caption.copy(fontSize = 11.sp),
                                        color = BeoutColors.TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = showPlayerStats,
                                onCheckedChange = { onTogglePlayerStats(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BeoutColors.BrightBlue,
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetOptionRow(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = BeoutShapes.Medium, onEnter = onClick)
            .clip(BeoutShapes.Medium)
            .background(if (isSelected) BeoutColors.PrimaryBlue.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = BeoutTextStyles.SecondaryInfo.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) BeoutColors.BrightBlue else Color.White
                    )
                )
                if (!badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BeoutColors.BrightBlue.copy(alpha = 0.15f))
                            .border(1.dp, BeoutColors.BrightBlue.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeoutColors.BrightBlue
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = BeoutTextStyles.Caption.copy(
                        color = BeoutColors.TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = BeoutColors.BrightBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp),
            color = Color(0xFF94A3B8),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = BeoutTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ServerSelectionCard(
    server: StreamServer,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cleanTitle = sanitizeServerName(server.name, index, server.quality)
    val qualUpper = server.quality.uppercase()

    // Unified BeoutTV styling matching app theme identity
    val (badgeBg, badgeBorder, badgeText) = if (isSelected) {
        Triple(
            BeoutColors.BrightBlue.copy(alpha = 0.20f),
            BeoutColors.BrightBlue.copy(alpha = 0.70f),
            BeoutColors.BrightBlue
        )
    } else {
        Triple(
            Color.White.copy(alpha = 0.06f),
            BeoutColors.Divider,
            BeoutColors.TextSecondary
        )
    }

    val subtitle = when {
        isSelected -> "السيرفر النشط حالياً • بث مستقر فائق السرعة"
        server.name.contains("بديل") -> "سيرفر احتياطي عالي الثبات • انقر للتبديل"
        qualUpper.contains("1080") -> "بث فائق الدقة 50fps • انقر للتبديل"
        qualUpper.contains("720") -> "دقة عالية سريعة التجاوب • انقر للتبديل"
        qualUpper.contains("480") -> "مناسب للباقات والشبكات المتوسطة • انقر للتبديل"
        qualUpper.contains("360") -> "أقصى توفير للبيانات والشبكات الضعيفة • انقر للتبديل"
        else -> "سيرفر مباشر ومعتمد • انقر للتبديل"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF0D1B2E) else BeoutColors.Card,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.Divider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(14.dp), onEnter = onClick)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Server Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                    .border(
                        1.dp,
                        if (isSelected) BeoutColors.BrightBlue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = if (isSelected) BeoutColors.BrightBlue else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cleanTitle,
                        style = BeoutTextStyles.CardTitle.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = Color.White
                    )

                    // Distinct Quality Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = server.quality,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = BeoutTextStyles.Caption.copy(
                        fontSize = 11.sp,
                        color = if (isSelected) BeoutColors.BrightBlue else BeoutColors.TextSecondary
                    )
                )
            }

            Spacer(Modifier.width(10.dp))

            // Radio / Checkmark Icon
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = BeoutColors.BrightBlue,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Unselected",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

internal fun buildClearKeyJson(rawKeyId: String, rawKey: String): ByteArray {
    fun toBase64Url(input: String): String {
        val trimmed = input.trim()
        val isHex = trimmed.length % 2 == 0 &&
                trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } &&
                !trimmed.contains("/") && !trimmed.contains("+") && !trimmed.contains("=")
        val bytes = if (isHex) {
            val clean = trimmed.replace(":", "").replace("-", "").trim()
            val result = ByteArray(clean.length / 2)
            for (i in result.indices) {
                val idx = i * 2
                result[i] = clean.substring(idx, idx + 2).toInt(16).toByte()
            }
            result
        } else {
            var padded = trimmed.replace("-", "+").replace("_", "/")
            while (padded.length % 4 != 0) padded += "="
            android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
        }
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        ).trim()
    }
    val kid = toBase64Url(rawKeyId)
    val k = toBase64Url(rawKey)
    val json = """{"keys":[{"kty":"oct","k":"$k","kid":"$kid"}],"type":"temporary"}"""
    return json.toByteArray(Charsets.UTF_8)
}

/**
 * تنظيف وتنسيق أسماء السيرفرات في المشغل بدون ذكر اسم الأسطورة أو ياسين
 * يظهر بدقة: سيرفر 1 (1080p FHD)، سيرفر 2 (720p HD)، سيرفر بديل 1 (720p HD)، إلخ.
 */
internal fun sanitizeServerName(rawName: String, index: Int, quality: String): String {
    val qClean = quality.trim()
    val rawClean = rawName.trim()
    if (rawClean.startsWith("سيرفر") &&
        !rawClean.contains("الأسطورة") &&
        !rawClean.contains("ياسين") &&
        !rawClean.contains("Ostora", ignoreCase = true) &&
        !rawClean.contains("Yacine", ignoreCase = true)
    ) {
        return rawClean
    }
    val isAlternative = rawClean.contains("بديل") || rawClean.contains("alt", ignoreCase = true)
    val baseName = if (isAlternative) "سيرفر بديل ${index + 1}" else "سيرفر ${index + 1}"
    val qualTag = when {
        rawClean.contains("FHD", true) || qClean.contains("1080") || rawClean.contains("1080") -> "1080p FHD"
        rawClean.contains("HD", true) || qClean.contains("720") || rawClean.contains("720") -> "720p HD"
        rawClean.contains("SD", true) || qClean.contains("480") || rawClean.contains("480") -> "480p SD"
        rawClean.contains("Low", true) || qClean.contains("360") || rawClean.contains("360") || rawClean.contains("244") -> "360p Low"
        qClean.isNotBlank() && !qClean.contains("الأسطورة") && !qClean.contains("ياسين") && !qClean.contains("Ostora", true) && !qClean.contains("Yacine", true) -> qClean
        else -> ""
    }
    return if (qualTag.isNotBlank()) "$baseName ($qualTag)" else baseName
}

