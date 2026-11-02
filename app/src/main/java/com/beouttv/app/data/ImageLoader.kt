package com.beouttv.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** محمّل صور صغير: ذاكرة LRU + قرص + تصغير أثناء فك الترميز. */
object Img {
    private lateinit var dir: File
    private val mem = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 10).toInt().coerceAtMost(48 shl 20)) {
        override fun sizeOf(k: String, v: Bitmap) = v.byteCount
    }

    fun init(c: Context) {
        dir = File(c.cacheDir, "img").apply { mkdirs() }
    }

    private fun name(url: String) =
        MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }

    suspend fun load(url: String, reqW: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$url@$reqW"
        mem.get(key)?.let { return@withContext it }
        val f = File(dir, name(url))
        if (!f.exists()) {
            runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 12000
                if (c.responseCode in 200..299) {
                    val tmp = File(dir, name(url) + ".tmp")
                    c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                    tmp.renameTo(f)
                }
                c.disconnect()
            }
        }
        if (!f.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var s = 1
        while (bounds.outWidth / (s * 2) >= reqW) s *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = s; inPreferredConfig = Bitmap.Config.RGB_565 }
        BitmapFactory.decodeFile(f.path, opts)?.also { mem.put(key, it) }
    }

    fun clear() {
        mem.evictAll()
        dir.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
}

@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    reqWidthPx: Int = 480,
    scale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {},
) {
    val bmp: ImageBitmap? by produceState<ImageBitmap?>(null, url, reqWidthPx) {
        value = if (url.isNullOrBlank()) null else Img.load(url, reqWidthPx)?.asImageBitmap()
    }
    Box(modifier.clipToBounds()) {
        val b = bmp
        if (b == null) placeholder() else Image(b, null, Modifier.matchParentSize(), contentScale = scale)
    }
}
