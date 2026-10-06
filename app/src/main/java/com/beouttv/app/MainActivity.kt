package com.beouttv.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.beouttv.app.data.i18n.LanguageManager
import com.beouttv.app.data.imdb.ImdbMetadataEngine
import com.beouttv.app.data.proto.ProtobufDiskCache
import com.beouttv.app.data.source.stream.LiveGatewayStreamResolver
import com.beouttv.app.ui.navigation.BeoutNavGraph
import com.beouttv.app.ui.screens.player.EXTRA_IS_LIVE
import com.beouttv.app.ui.screens.player.EXTRA_STREAM_URL
import com.beouttv.app.ui.screens.player.EXTRA_TITLE
import com.beouttv.app.ui.screens.player.PlayerActivity
import com.beouttv.app.ui.theme.BeoutTvTheme

import android.content.pm.ActivityInfo
import com.beouttv.app.core.tv.DeviceTypeDetector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DeviceTypeDetector.init(this)
        val isTv = DeviceTypeDetector.isTvDevice(this)
        if (isTv) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            // Unlocked sensor orientation: Allows tablets, foldables, and phones to adapt naturally to both portrait & landscape
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        handleIncomingStreamIntent(intent)

        try {
            com.beouttv.app.data.config.AppGatewayManager.init(this)
            com.beouttv.app.data.repository.ServiceLocator.init(this)
            LanguageManager.init(this)
            ImdbMetadataEngine.init(this)
            ProtobufDiskCache.init(this)
            com.beouttv.app.data.favorites.FavoritesManager.init(this)
            com.beouttv.app.data.notification.NotificationsManager.init(this)

            // Safe Singleton Coil Image Loader for Caching
            val imageLoader = coil.ImageLoader.Builder(this)
                .crossfade(true)
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory((cacheDir ?: filesDir).resolve("image_cache"))
                        .maxSizeBytes(150 * 1024 * 1024L)
                        .build()
                }
                .memoryCache {
                    coil.memory.MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .respectCacheHeaders(false)
                .build()
            coil.Coil.setImageLoader(imageLoader)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            window.navigationBarColor = android.graphics.Color.parseColor("#0A0F1D")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightNavigationBars = false
            insetsController.isAppearanceLightStatusBars = false
        } catch (_: Throwable) {}
        setContent {
            val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
            LaunchedEffect(isTvMode) {
                requestedOrientation = if (isTvMode) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            val currentLanguage by LanguageManager.currentLanguage.collectAsState()
            val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                BeoutTvTheme {
                    BeoutNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingStreamIntent(intent)
    }

    private fun handleIncomingStreamIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val rawUri = incomingIntent.dataString
            ?: incomingIntent.getStringExtra(EXTRA_STREAM_URL)
            ?: incomingIntent.getStringExtra("streamUrl")
            ?: incomingIntent.getStringExtra("stream_url")
            ?: incomingIntent.getStringExtra("url")
            ?: incomingIntent.getStringExtra("channel_url")
            ?: return

        val clean = rawUri.trim()
        if (clean.isNotBlank()) {
            val playerIntent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, clean)
                putExtra(EXTRA_TITLE, incomingIntent.getStringExtra(EXTRA_TITLE) ?: "بث مباشر")
                putExtra(EXTRA_IS_LIVE, true)
            }
            startActivity(playerIntent)
        }
    }
}
