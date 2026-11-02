package com.beouttv.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceView
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.net.Uri
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.beouttv.app.core.dataOrNull
import com.beouttv.app.data.HistoryEntry
import com.beouttv.app.data.Prefs
import com.beouttv.app.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private class Opt(val label: String, val selected: Boolean, val pick: () -> Unit)
private enum class Menu { Volume, Subs, Audio, Quality }

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val ss = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, ss) else "%02d:%02d".format(m, ss)
}

@Composable
fun PlayerScreen(req: PlayReq, nav: NavVM, app: AppVM) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    val view = LocalView.current
    val settings by app.settings.collectAsState()
    val cfgState by app.config.state.collectAsState()
    val maxRetry = cfgState.dataOrNull()?.maxReconnect ?: 6

    var index by rememberSaveable { mutableIntStateOf(req.index) }
    val entry = req.entries[index.coerceIn(0, req.entries.lastIndex)]
    val isChannel = entry.item.isChannel

    // ---- المشغل ----
    val player = remember {
        ExoPlayer.Builder(ctx)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 40_000, 1_500, 3_000)
                    .build(),
            )
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true,
                )
            }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var cueText by remember { mutableStateOf("") }
    var videoRatio by remember { mutableFloatStateOf(16f / 9f) }
    var errTick by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var reconnecting by remember { mutableStateOf(false) }
    var fatal by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var live by remember { mutableStateOf(isChannel) }
    var behindLive by remember { mutableStateOf(false) }
    var ended by remember { mutableIntStateOf(0) }

    var controls by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf<Menu?>(null) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var buf by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(1f) }
    var landscape by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) { attempts = 0; reconnecting = false; fatal = false }
                if (state == Player.STATE_ENDED) ended++
            }
            override fun onTracksChanged(t: Tracks) { tracks = t }
            override fun onCues(cueGroup: CueGroup) {
                cueText = cueGroup.cues.joinToString("\n") { it.text?.toString().orEmpty() }.trim()
            }
            override fun onVideoSizeChanged(v: VideoSize) {
                if (v.width > 0 && v.height > 0) videoRatio = (v.width * v.pixelWidthHeightRatio) / v.height
            }
            override fun onPlayerError(e: PlaybackException) {
                behindLive = e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
                errTick++
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }

    // ---- حفظ التقدم ----
    val curEntry by rememberUpdatedState(entry)
    val released = remember { booleanArrayOf(false) }
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    fun save(e: PlayEntry) {
        if (released[0] || live || e.item.isChannel) return
        val d = player.duration
        if (d <= 0) return
        val h = HistoryEntry(e.key, e.item, e.episodeId, e.label, player.currentPosition, d, System.currentTimeMillis())
        CoroutineScope(Dispatchers.IO).launch { Repo.saveProgress(h) }
    }

    DisposableEffect(index) { onDispose { save(entry) } }
    LaunchedEffect(isPlaying, index) {
        while (isPlaying) { delay(10_000); save(entry) }
    }

    // ---- تحميل الرابط وبدء التشغيل ----
    LaunchedEffect(index, reload) {
        fatal = false
        buffering = true
        val keepPos = if (reload > 0) player.currentPosition else -1L
        try {
            val info = Repo.play(entry.kind, entry.key)
            live = info.live || isChannel
            val s = Prefs.settingsNow()
            val saved = if (keepPos >= 0) keepPos
            else if (!live && s.resume) Prefs.historyEntry(entry.key)?.let { h ->
                if (h.positionMs > 5_000 && h.durationMs > 0 && h.positionMs < h.durationMs * 95 / 100) h.positionMs else 0L
            } ?: 0L else 0L

            val parsed = parseStreamUrl(info.url)
            val uri = Uri.parse(parsed.cleanUrl)
            val httpFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(12_000)
                .setReadTimeoutMs(15_000)

            val ua = parsed.headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            httpFactory.setUserAgent(ua)
            val customHeaders = parsed.headers.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
            if (customHeaders.isNotEmpty()) {
                httpFactory.setDefaultRequestProperties(customHeaders)
            }

            val mediaItem = MediaItem.Builder().setUri(uri).build()
            val mediaSource = if (parsed.cleanUrl.contains(".m3u8", ignoreCase = true) || parsed.cleanUrl.contains("m3u8", ignoreCase = true)) {
                HlsMediaSource.Factory(httpFactory).createMediaSource(mediaItem)
            } else if (parsed.cleanUrl.contains(".mpd", ignoreCase = true)) {
                DashMediaSource.Factory(httpFactory).createMediaSource(mediaItem)
            } else {
                DefaultMediaSourceFactory(httpFactory).createMediaSource(mediaItem)
            }

            if (saved > 0L) {
                player.setMediaSource(mediaSource, saved)
            } else {
                player.setMediaSource(mediaSource)
            }
            player.prepare()
            player.playWhenReady = true
            if (isChannel) Prefs.setLastChannel(entry.item.id)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            errTick++
        }
    }

    // ---- إعادة الاتصال ----
    LaunchedEffect(errTick) {
        if (errTick == 0) return@LaunchedEffect
        attempts++
        if (attempts > maxRetry) { fatal = true; reconnecting = false; return@LaunchedEffect }
        reconnecting = true
        delay((1_000L * attempts).coerceAtMost(8_000L))
        if (attempts >= 2 || player.currentMediaItem == null) reload++ // طلب رابط جديد
        else {
            if (behindLive) player.seekToDefaultPosition()
            player.prepare()
        }
    }

    // ---- الحلقة التالية تلقائياً ----
    LaunchedEffect(ended) {
        if (ended > 0 && !isChannel && index < req.entries.lastIndex) index++
    }

    // ---- الجودة القصوى من الإعدادات ----
    LaunchedEffect(settings.maxHeight) {
        val h = if (settings.maxHeight > 0) settings.maxHeight else Int.MAX_VALUE
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, h).build()
    }

    // ---- النافذة والاتجاه ----
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(Unit) {
        val w = activity.window
        val c = WindowCompat.getInsetsController(w, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            c.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            save(curEntry)          // حفظ الموضع قبل تحرير المشغل
            released[0] = true
            player.release()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) { save(entry); player.pause() } }
        lifecycle.addObserver(o)
        onDispose { lifecycle.removeObserver(o) }
    }

    // ---- إظهار/إخفاء الأدوات ----
    LaunchedEffect(controls, isPlaying, interaction, menu) {
        if (controls && isPlaying && menu == null) { delay(3_500); controls = false }
    }
    LaunchedEffect(controls) {
        while (controls) {
            pos = player.currentPosition; dur = player.duration.coerceAtLeast(0); buf = player.bufferedPosition
            delay(500)
        }
    }

    fun poke() { controls = true; interaction++ }
    fun seekBy(ms: Long) { player.seekTo((player.currentPosition + ms).coerceIn(0, if (player.duration > 0) player.duration else Long.MAX_VALUE)); poke() }
    fun go(delta: Int) { val n = index + delta; if (n in req.entries.indices) index = n }

    BackHandlerCompat(menu != null) { menu = null }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (!controls && ev.key in setOf(Key.DirectionCenter, Key.Enter, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)) {
                    poke(); return@onPreviewKeyEvent true
                }
                when (ev.key) {
                    Key.MediaPlayPause, Key.Spacebar -> { if (player.isPlaying) player.pause() else player.play(); poke(); true }
                    Key.MediaFastForward -> { seekBy(10_000); true }
                    Key.MediaRewind -> { seekBy(-10_000); true }
                    Key.MediaNext, Key.ChannelUp -> { go(1); true }
                    Key.MediaPrevious, Key.ChannelDown -> { go(-1); true }
                    else -> false
                }
            }
            .focusRequester(rootFocus)
            .focusable()
            .pointerInput(live) {
                detectTapGestures(
                    onTap = { controls = !controls; if (controls) interaction++ },
                    onDoubleTap = { o -> if (!live) seekBy(if (o.x < size.width / 2) -10_000 else 10_000) },
                )
            },
    ) {
        AndroidView(
            factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
            modifier = Modifier.align(Alignment.Center).aspectRatio(videoRatio),
        )

        if (cueText.isNotEmpty())
            Txt(
                cueText,
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (controls) 110.dp else 32.dp, start = 24.dp, end = 24.dp)
                    .background(Color(0x99000000), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                T.body, Color.White,
            )

        if (buffering && !fatal) Txt(if (reconnecting) S.reconnecting else "...", Modifier.align(Alignment.Center), T.h2, Color.White)

        if (fatal) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt(S.playbackErr, style = T.h2, color = Color.White)
            PrimaryButton(S.retry, { attempts = 0; errTick = 0; reload++ })
        }

        if (controls) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(110.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent))))
                Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))

                // أعلى
                Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassIconButton(Ic.Back, { nav.pop() })
                    Column(Modifier.weight(1f)) {
                        Txt(entry.item.title, style = T.h2, color = Color.White, maxLines = 1)
                        entry.label?.let { Txt(it, style = T.small, color = Color(0xFFCBD7EC), maxLines = 1) }
                    }
                    if (live) Txt(S.live, Modifier.background(T.c.danger, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp), T.small, Color.White)
                }

                // الوسط
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (req.entries.size > 1) GlassIconButton(Ic.Prev, { go(-1); poke() }, size = 48.dp)
                    if (!live) GlassIconButton(Ic.Back10, { seekBy(-10_000) }, size = 48.dp)
                    GlassIconButton(if (isPlaying) Ic.Pause else Ic.Play, { if (player.isPlaying) player.pause() else player.play(); poke() }, size = 64.dp)
                    if (!live) GlassIconButton(Ic.Forward10, { seekBy(10_000) }, size = 48.dp)
                    if (req.entries.size > 1) GlassIconButton(Ic.Next, { go(1); poke() }, size = 48.dp)
                }

                // أسفل
                Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (!live) {
                        SeekBar(pos, dur, buf) { player.seekTo(it); pos = it; poke() }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Txt(fmt(pos), style = T.small, color = Color.White)
                            Txt(fmt(dur), style = T.small, color = Color.White)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(if (volume == 0f) Ic.Mute else Ic.Volume, { menu = if (menu == Menu.Volume) null else Menu.Volume }, size = 40.dp)
                        Box(Modifier.width(10.dp))
                        GlassIconButton(Ic.Subtitle, { menu = if (menu == Menu.Subs) null else Menu.Subs }, size = 40.dp)
                        Box(Modifier.width(10.dp))
                        GlassIconButton(Ic.Audio, { menu = if (menu == Menu.Audio) null else Menu.Audio }, size = 40.dp)
                        Box(Modifier.width(10.dp))
                        GlassIconButton(Ic.Quality, { menu = if (menu == Menu.Quality) null else Menu.Quality }, size = 40.dp)
                        Box(Modifier.width(10.dp))
                        GlassIconButton(if (landscape) Ic.ExitFullscreen else Ic.Fullscreen, {
                            landscape = !landscape
                            activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            poke()
                        }, size = 40.dp)
                    }
                }

                // القوائم
                menu?.let { m ->
                    Column(
                        Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 96.dp)
                            .width(240.dp).glass(GlassShape, true).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        when (m) {
                            Menu.Volume -> {
                                Txt(S.player, style = T.small, color = T.c.textDim)
                                SeekBar((volume * 1000).toLong(), 1000, 0) { volume = it / 1000f; player.volume = volume; poke() }
                            }
                            Menu.Quality -> OptList(qualityOptions(player, tracks)) { menu = null }
                            Menu.Audio -> OptList(audioOptions(player, tracks)) { menu = null }
                            Menu.Subs -> OptList(subtitleOptions(player, tracks)) { menu = null }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) =
    androidx.activity.compose.BackHandler(enabled, onBack)

@Composable
private fun OptList(opts: List<Opt>, close: () -> Unit) {
    if (opts.size <= 1 && opts.firstOrNull()?.label != S.auto) {
        Txt(S.empty, style = T.small, color = T.c.textDim)
        return
    }
    opts.forEach { o ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).tvFocus(RoundedCornerShape(10.dp)).tap { o.pick(); close() }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.width(20.dp)) { if (o.selected) BIcon(Ic.Check, size = 18.dp, tint = T.c.accent2) }
            Txt(o.label, style = T.body, maxLines = 1)
        }
    }
}

@Composable
private fun SeekBar(position: Long, duration: Long, buffered: Long, onSeek: (Long) -> Unit) {
    var drag by remember { mutableFloatStateOf(-1f) }
    val frac = if (drag >= 0f) drag else if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val bufFrac = if (duration > 0) (buffered.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val c = T.c
    Box(
        Modifier.fillMaxWidth().height(28.dp)
            .pointerInput(duration) {
                detectTapGestures { o -> onSeek((o.x / size.width * duration).toLong().coerceIn(0, duration)) }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { if (drag >= 0f) onSeek((drag * duration).toLong()); drag = -1f },
                    onDragCancel = { drag = -1f },
                    onHorizontalDrag = { ch, _ -> drag = (ch.position.x / size.width).coerceIn(0f, 1f) },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color(0x44FFFFFF)))
        Box(Modifier.fillMaxWidth(bufFrac).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0x55FFFFFF)))
        Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(c.accent2, c.accent))))
    }
}

// ---------- خيارات المسارات ----------
private fun qualityOptions(p: ExoPlayer, tracks: Tracks): List<Opt> {
    val params = p.trackSelectionParameters
    val hasOverride = params.overrides.values.any { it.type == C.TRACK_TYPE_VIDEO }
    val out = ArrayList<Opt>()
    tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            val h = g.getTrackFormat(i).height
            if (h <= 0) continue
            out += Opt("${h}p", hasOverride && g.isTrackSelected(i)) {
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
            }
        }
    }
    val sorted = out.distinctBy { it.label }.sortedByDescending { it.label.dropLast(1).toIntOrNull() ?: 0 }
    return listOf(Opt(S.auto, !hasOverride) {
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build()
    }) + sorted
}

private fun trackLabel(f: androidx.media3.common.Format, i: Int): String =
    f.label ?: f.language?.takeIf { it != "und" }?.let { Locale(it).getDisplayLanguage(Locale("ar")) } ?: "#${i + 1}"

private fun audioOptions(p: ExoPlayer, tracks: Tracks): List<Opt> {
    val out = ArrayList<Opt>()
    tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            out += Opt(trackLabel(g.getTrackFormat(i), out.size), g.isTrackSelected(i)) {
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
            }
        }
    }
    return out
}

private fun subtitleOptions(p: ExoPlayer, tracks: Tracks): List<Opt> {
    val disabled = p.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
    val out = ArrayList<Opt>()
    out += Opt(S.off, disabled || tracks.groups.none { it.type == C.TRACK_TYPE_TEXT && it.isSelected }) {
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
    }
    tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.forEach { g ->
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            out += Opt(trackLabel(g.getTrackFormat(i), out.size - 1), !disabled && g.isTrackSelected(i)) {
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i)).build()
            }
        }
    }
    return if (out.size == 1) emptyList() else out
}

data class ParsedStream(val cleanUrl: String, val headers: Map<String, String>)

fun parseStreamUrl(raw: String): ParsedStream {
    val text = raw.trim()
    val base = text.substringBefore('|').trim()
    val headers = mutableMapOf<String, String>()
    if (text.contains('|')) {
        val query = text.substringAfter('|')
        query.split('&').forEach { param ->
            val idx = param.indexOf('=')
            if (idx > 0) {
                val k = Uri.decode(param.substring(0, idx)).trim()
                val v = Uri.decode(param.substring(idx + 1)).trim()
                if (k.isNotEmpty() && v.isNotEmpty()) {
                    headers[k] = v
                }
            }
        }
    }
    return ParsedStream(base, headers)
}
