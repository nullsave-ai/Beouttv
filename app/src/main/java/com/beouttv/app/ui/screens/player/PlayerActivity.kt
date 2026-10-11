package com.beouttv.app.ui.screens.player

import android.app.PictureInPictureParams
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.beouttv.app.ui.theme.BeoutTvTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beouttv.app.data.config.AppGatewayManager
import com.beouttv.app.data.config.GatewayStatus
import com.beouttv.app.data.model.StreamServer
import com.beouttv.app.data.model.Episode
import com.beouttv.app.data.model.SubtitleTrack
import com.beouttv.app.data.source.stream.LiveGatewayStreamResolver
import com.beouttv.app.data.source.stream.OkRuStreamResolver
import android.view.KeyEvent
import com.beouttv.app.core.tv.TvRemoteAction
import com.beouttv.app.core.tv.TvRemoteController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerActivity : ComponentActivity() {

    private val remoteActionFlow = MutableSharedFlow<TvRemoteAction>(extraBufferCapacity = 1)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = TvRemoteController.parseKeyEvent(event)
        if (action != TvRemoteAction.Unhandled) {
            remoteActionFlow.tryEmit(action)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppGatewayManager.configFlow.value.status != GatewayStatus.NORMAL) {
            finish()
            return
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Enable true edge-to-edge fullscreen & hide system navigation/status bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Render through camera notch/cutouts on modern screens (19.5:9 / 20:9) for true full screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID) ?: streamUrl
        val posterUrl = intent.getStringExtra(EXTRA_POSTER_URL).orEmpty()
        val backdropUrl = intent.getStringExtra(EXTRA_BACKDROP_URL).orEmpty()
        val seekPosition = intent.getLongExtra(EXTRA_SEEK_POSITION, -1L)
        val isLive = intent.getBooleanExtra(EXTRA_IS_LIVE, false)
        val subtitleUrl = intent.getStringExtra(EXTRA_SUBTITLE_URL)
        val subtitleLabel = intent.getStringExtra(EXTRA_SUBTITLE_LABEL)
        val subtitlesJson = intent.getStringExtra(EXTRA_SUBTITLES_JSON)
        val serversJson = intent.getStringExtra(EXTRA_SERVERS_JSON)
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)
        val referer = intent.getStringExtra(EXTRA_REFERER)
        val origin = intent.getStringExtra(EXTRA_ORIGIN)
        val cookie = intent.getStringExtra(EXTRA_COOKIE)
        val headersJson = intent.getStringExtra(EXTRA_HEADERS_JSON)
        val drmKeyId = intent.getStringExtra(EXTRA_DRM_KEY_ID)
        val drmKey = intent.getStringExtra(EXTRA_DRM_KEY)
        val drmLicenseUrl = intent.getStringExtra(EXTRA_DRM_LICENSE_URL)
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)

        // Parse Subtitles List
        val subtitlesList = mutableListOf<SubtitleTrack>()
        if (!subtitlesJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(subtitlesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    subtitlesList.add(
                        SubtitleTrack(
                            id = obj.optString("id", "sub_$i"),
                            language = obj.optString("language", "ar"),
                            label = obj.optString("label", "العربية"),
                            url = obj.optString("url", ""),
                            format = obj.optString("format", "vtt")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (subtitlesList.isEmpty() && !subtitleUrl.isNullOrBlank()) {
            subtitlesList.add(
                SubtitleTrack(
                    id = "sub_main",
                    language = "ar",
                    label = subtitleLabel ?: "العربية",
                    url = subtitleUrl,
                    format = if (subtitleUrl.endsWith(".srt", true)) "srt" else "vtt"
                )
            )
        }

        // Parse Servers List
        val serversList = mutableListOf<StreamServer>()
        if (!serversJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(serversJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    serversList.add(
                        StreamServer(
                            id = obj.optString("id", "srv_$i"),
                            name = obj.optString("name", "سيرفر البث"),
                            quality = obj.optString("quality", "1080p"),
                            url = obj.optString("url", ""),
                            isDirect = obj.optBoolean("isDirect", true)
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Parse Next Episode Object (for auto-play in Series & Anime)
        val nextEpisodeJson = intent.getStringExtra(EXTRA_NEXT_EPISODE_JSON)
        val initialNextEpisode: Episode? = if (!nextEpisodeJson.isNullOrBlank()) {
            try {
                val obj = org.json.JSONObject(nextEpisodeJson)
                val epSubs = mutableListOf<SubtitleTrack>()
                val sArr = obj.optJSONArray("subtitles")
                if (sArr != null) {
                    for (i in 0 until sArr.length()) {
                        val sObj = sArr.getJSONObject(i)
                        epSubs.add(
                            SubtitleTrack(
                                id = sObj.optString("id", "sub_$i"),
                                language = sObj.optString("language", "ar"),
                                label = sObj.optString("label", "العربية"),
                                url = sObj.optString("url", ""),
                                format = sObj.optString("format", "vtt")
                            )
                        )
                    }
                }
                val epServers = mutableListOf<StreamServer>()
                val srvArr = obj.optJSONArray("servers")
                if (srvArr != null) {
                    for (i in 0 until srvArr.length()) {
                        val svObj = srvArr.getJSONObject(i)
                        epServers.add(
                            StreamServer(
                                id = svObj.optString("id", "srv_$i"),
                                name = svObj.optString("name", "سيرفر البث"),
                                quality = svObj.optString("quality", "1080p"),
                                url = svObj.optString("url", ""),
                                isDirect = svObj.optBoolean("isDirect", true)
                            )
                        )
                    }
                }
                Episode(
                    id = obj.optString("id", ""),
                    episodeNumber = obj.optInt("episodeNumber", 1),
                    title = obj.optString("title", ""),
                    durationMinutes = obj.optInt("durationMinutes", 45),
                    thumbnailUrl = obj.optString("thumbnailUrl", ""),
                    streamUrl = obj.optString("streamUrl", ""),
                    seriesId = obj.optString("seriesId", ""),
                    seasonNumber = obj.optInt("seasonNumber", 1),
                    subtitles = epSubs,
                    servers = epServers
                )
            } catch (_: Exception) { null }
        } else null

        val resolvedInitialStream = if (streamUrl.isNotBlank()) streamUrl else serversList.firstOrNull()?.url.orEmpty()

        setContent {
            BeoutTvTheme {
                var currentStreamUrl by remember { mutableStateOf(resolvedInitialStream) }
                var currentUserAgent by remember { mutableStateOf(userAgent) }
                var currentReferer by remember { mutableStateOf(referer) }
                var currentDrmKeyId by remember { mutableStateOf(drmKeyId) }
                var currentDrmKey by remember { mutableStateOf(drmKey) }
                val isGatewayUrl = remember(resolvedInitialStream) {
                    LiveGatewayStreamResolver.isTargetApiUrl(resolvedInitialStream) || OkRuStreamResolver.isOkRuUrl(resolvedInitialStream)
                }
                var isResolving by remember { mutableStateOf(isGatewayUrl) }

                LaunchedEffect(resolvedInitialStream) {
                    if (LiveGatewayStreamResolver.isTargetApiUrl(resolvedInitialStream)) {
                        val resolved = withContext(Dispatchers.IO) {
                            LiveGatewayStreamResolver.resolve(resolvedInitialStream)
                        }
                        if (resolved != null) {
                            currentStreamUrl = resolved.streamUrl
                            currentUserAgent = resolved.userAgent ?: userAgent
                            currentReferer = resolved.referer ?: referer
                            currentDrmKeyId = resolved.drmKeyId ?: drmKeyId
                            currentDrmKey = resolved.drmKey ?: drmKey
                        }
                        isResolving = false
                    } else if (OkRuStreamResolver.isOkRuUrl(streamUrl)) {
                        val resolved = withContext(Dispatchers.IO) {
                            OkRuStreamResolver.resolve(streamUrl)
                        }
                        if (resolved != null) {
                            currentStreamUrl = resolved.streamUrl
                            currentUserAgent = resolved.userAgent ?: userAgent
                            currentReferer = resolved.referer ?: referer
                            currentDrmKeyId = resolved.drmKeyId ?: drmKeyId
                            currentDrmKey = resolved.drmKey ?: drmKey
                        }
                        isResolving = false
                    }
                }

                if (isResolving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D0E11)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF8A00),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                } else {
                    var nextEpisodeState by remember { mutableStateOf<com.beouttv.app.data.model.Episode?>(initialNextEpisode) }

                    PlayerScreen(
                        streamUrl = currentStreamUrl,
                        title = title,
                        mediaId = mediaId,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        seekPosition = seekPosition,
                        subtitles = subtitlesList,
                        servers = serversList,
                        subtitleUrl = subtitleUrl,
                        subtitleLabel = subtitleLabel,
                        userAgent = currentUserAgent,
                        referer = currentReferer,
                        origin = origin,
                        cookie = cookie,
                        headersJson = headersJson,
                        drmKeyId = currentDrmKeyId,
                        drmKey = currentDrmKey,
                        drmLicenseUrl = drmLicenseUrl,
                        groupId = groupId,
                        isLive = isLive,
                        remoteActionFlow = remoteActionFlow,
                        nextEpisode = nextEpisodeState,
                        onPlayNextEpisode = { ep ->
                            // Seamless transition to next episode
                            val nextStream = if (ep.streamUrl.isNotBlank()) ep.streamUrl else ep.servers.firstOrNull()?.url.orEmpty()
                            val sortedSubs = ep.subtitles.sortedWith(
                                compareByDescending<SubtitleTrack> { it.language == "ar" && (it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox")) }
                                    .thenByDescending { it.language == "ar" }
                                    .thenByDescending { it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox") }
                            )
                            val defaultSub = sortedSubs.firstOrNull { it.language == "ar" } ?: sortedSubs.firstOrNull()
                            
                            val nextIntent = Intent(this@PlayerActivity, PlayerActivity::class.java).apply {
                                putExtra(EXTRA_STREAM_URL, nextStream)
                                putExtra(EXTRA_TITLE, ep.title)
                                putExtra(EXTRA_MEDIA_ID, ep.id)
                                putExtra(EXTRA_POSTER_URL, ep.thumbnailUrl)
                                putExtra(EXTRA_BACKDROP_URL, backdropUrl.ifBlank { ep.thumbnailUrl })
                                putExtra(EXTRA_IS_LIVE, false)
                                if (!defaultSub?.url.isNullOrBlank()) {
                                    putExtra(EXTRA_SUBTITLE_URL, defaultSub?.url)
                                    putExtra(EXTRA_SUBTITLE_LABEL, defaultSub?.label)
                                }
                                if (sortedSubs.isNotEmpty()) {
                                    val arr = JSONArray()
                                    sortedSubs.forEach { sub ->
                                        val obj = JSONObject().apply {
                                            put("id", sub.id)
                                            put("language", sub.language)
                                            put("label", sub.label)
                                            put("url", sub.url)
                                            put("format", sub.format)
                                        }
                                        arr.put(obj)
                                    }
                                    putExtra(EXTRA_SUBTITLES_JSON, arr.toString())
                                }
                                if (ep.servers.isNotEmpty()) {
                                    val srvArr = JSONArray()
                                    ep.servers.forEach { srv ->
                                        val obj = JSONObject().apply {
                                            put("id", srv.id)
                                            put("name", srv.name)
                                            put("quality", srv.quality)
                                            put("url", srv.url)
                                            put("isDirect", srv.isDirect)
                                        }
                                        srvArr.put(obj)
                                    }
                                    putExtra(EXTRA_SERVERS_JSON, srvArr.toString())
                                }
                                if (!currentUserAgent.isNullOrBlank()) putExtra(EXTRA_USER_AGENT, currentUserAgent)
                                if (!currentReferer.isNullOrBlank()) putExtra(EXTRA_REFERER, currentReferer)
                            }
                            finish()
                            startActivity(nextIntent)
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStop() {
        super.onStop()
        // User exited via Home or app switcher: finish activity if not in PiP mode to guarantee no background audio
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            finish()
        }
    }
}
