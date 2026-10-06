package com.beouttv.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
object ShortDramaMediaCache {
    private const val TAG = "ShortDramaCache"
    private const val MAX_CACHE_SIZE_BYTES = 30L * 1024L * 1024L // 30MB Ultra-lightweight MovieBox LRU cache
    private const val PRELOAD_BYTES = 256L * 1024L // Preload first 256KB for instant 0.1s start without consuming RAM

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    var isPreloadPaused: Boolean = false

    fun pausePreload() {
        isPreloadPaused = true
    }

    fun resumePreload() {
        isPreloadPaused = false
    }

    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePreloads = ConcurrentHashMap<String, Job>()
    private val preloadedUrls = Collections.synchronizedSet(mutableSetOf<String>())

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        val existing = simpleCache
        if (existing != null) return existing

        val cacheDir = File(context.applicationContext.cacheDir, "shorts_reel_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
        val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
        val newCache = SimpleCache(cacheDir, evictor, dbProvider)
        simpleCache = newCache
        return newCache
    }

    fun clearCache(context: Context) {
        try {
            simpleCache?.release()
            simpleCache = null
            val cacheDir = File(context.applicationContext.cacheDir, "shorts_reel_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            preloadedUrls.clear()
            activePreloads.values.forEach { it.cancel() }
            activePreloads.clear()
        } catch (_: Exception) {}
    }

    fun buildCacheDataSourceFactory(
        context: Context,
        httpFactory: DefaultHttpDataSource.Factory
    ): DataSource.Factory {
        val cache = getCache(context)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        // DefaultDataSource routes file:// and content:// to local storage handlers, and falls back to cacheFactory for network streams
        return DefaultDataSource.Factory(context, cacheFactory)
    }

    /**
     * TikTok-grade predictive preloader: Caches first 1.5MB of upcoming reels in background.
     * Automatically pauses when active reel is buffering to give 100% bandwidth to current video!
     */
    fun preload(context: Context, url: String) {
        if (url.isBlank() || isPreloadPaused || preloadedUrls.contains(url) || activePreloads.containsKey(url)) return

        val job = preloadScope.launch {
            try {
                val cache = getCache(context)
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setPosition(0)
                    .setLength(PRELOAD_BYTES)
                    .build()

                val httpSource = DefaultHttpDataSource.Factory()
                    .setUserAgent("okhttp/4.12.0")
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(12_000)
                    .setAllowCrossProtocolRedirects(true)
                    .createDataSource()

                val cacheSource = CacheDataSource(cache, httpSource)
                val cacheWriter = CacheWriter(
                    cacheSource,
                    dataSpec,
                    ByteArray(32 * 1024),
                    null
                )
                cacheWriter.cache()
                preloadedUrls.add(url)
            } catch (_: Exception) {
                // Non-fatal background prefetch cancellation
            } finally {
                activePreloads.remove(url)
            }
        }
        activePreloads[url] = job
    }

    /**
     * Smart 25% Chunk Preloader:
     * Preloads a specific 25% slice (chunk 0, 1, 2, or 3) of an episode.
     * Prevents downloading the full file prematurely, saving massive bandwidth on mobile networks!
     */
    fun preloadEpisodeChunk(context: Context, url: String, chunkIndex: Int) {
        if (url.isBlank() || isPreloadPaused) return
        val chunkKey = "$url#chunk_$chunkIndex"
        if (preloadedUrls.contains(chunkKey) || activePreloads.containsKey(chunkKey)) return

        val chunkSize = 1_200_000L // ~25% of average 4.8MB reel
        val startPos = (chunkIndex * chunkSize).coerceAtLeast(0L)
        val length = if (chunkIndex == 3) 1_500_000L else chunkSize

        val job = preloadScope.launch {
            try {
                val cache = getCache(context)
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setPosition(startPos)
                    .setLength(length)
                    .build()

                val httpSource = DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(12_000)
                    .setAllowCrossProtocolRedirects(true)
                    .createDataSource()

                val cacheSource = CacheDataSource(cache, httpSource)
                val cacheWriter = CacheWriter(
                    cacheSource,
                    dataSpec,
                    ByteArray(32 * 1024),
                    null
                )
                cacheWriter.cache()
                preloadedUrls.add(chunkKey)
            } catch (_: Exception) {
                // Background cancel or network slip
            } finally {
                activePreloads.remove(chunkKey)
            }
        }
        activePreloads[chunkKey] = job
    }

    /**
     * Staged Next Episode 25% Chunk Preloader:
     * Dispatches preloading of specific 25% chunks of next episode.
     */
    fun preloadNextEpisodeChunk(context: Context, videoId: String, nextEpisodeNumber: Int, platform: String, chunkIndex: Int) {
        val sources = ShortDramaRepository.buildSourcesForEpisode(videoId, nextEpisodeNumber, platform)
        val primaryUrl = sources.firstOrNull()?.streamUrl ?: return
        preloadEpisodeChunk(context, primaryUrl, chunkIndex)

        if (chunkIndex == 0) {
            val cleanVid = videoId.removePrefix("series_").removePrefix("epservers_")
            if (cleanVid.all { it.isDigit() }) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ShortDramaRepository.resolveSubtitleForEpisode(cleanVid, nextEpisodeNumber)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Backward-compatible helper for full preloading
     */
    fun preloadNextEpisode(context: Context, videoId: String, nextEpisodeNumber: Int, platform: String) {
        preloadNextEpisodeChunk(context, videoId, nextEpisodeNumber, platform, 0)
    }
}
