package com.beouttv.app.data.source.stream

import android.net.Uri
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import info.t4w.vp.view.HlsKeyDecryptor
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * معالج وسائط احترافي فائق الكفاءة لسيرفرات الأسطورة IPTV مطابق لهندسة مشغل الأسطورة الرسمي (info.t4w.vp):
 * 1. معالجة مفاتيح HLS المشفرة من نوع data: URI واشتقاق مفتاح AES-128 عبر JNI (libnext.so)
 * 2. حقن ترويسات User-Agent و Referer الإلزامية لسيرفرات البث لمنع خطأ 403 Forbidden.
 * 3. كشف ونزع ترويسة PNG الوهمية ديناميكياً فور فتح أي مقطع فيديو بالبحث عن كتلة IEND + 8 بايت.
 * 4. تسليم دفق MPEG-TS إلى مشغل ExoPlayer الداخلي لفك التشفير والتشغيل بسلاسة تامة داخل التطبيق.
 */
class OstoraStreamDataSource(
    private val upstream: DataSource
) : DataSource {

    private var isDataUri = false
    private var dataUriStream: ByteArrayInputStream? = null
    private var currentUri: Uri? = null

    // Buffer to preserve payload bytes peeked during PNG header removal
    private var prefixBuffer: ByteArray? = null
    private var prefixOffset = 0

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        val scheme = uri.scheme?.lowercase()
        val uriStr = uri.toString().lowercase()
        currentUri = uri

        // 1. معالجة مفاتيح HLS المشفرة من نوع data: URI مباشرة
        if (scheme == "data") {
            isDataUri = true
            prefixBuffer = null
            prefixOffset = 0
            val schemePart = uri.schemeSpecificPart
            val commaIdx = schemePart.indexOf(',')
            if (commaIdx != -1) {
                val header = schemePart.substring(0, commaIdx)
                val data = schemePart.substring(commaIdx + 1)
                val cleanData = data.trim().replace(" ", "+")
                val rawBytes = if (header.contains(";base64")) {
                    var padded = cleanData
                    while (padded.length % 4 != 0) padded += "="
                    Base64.decode(padded, Base64.DEFAULT)
                } else {
                    data.toByteArray(StandardCharsets.UTF_8)
                }
                val keyBytes = HlsKeyDecryptor.decryptHlsKeyPayload(rawBytes)
                val pos = dataSpec.position
                val stream = ByteArrayInputStream(keyBytes)
                if (pos > 0) {
                    stream.skip(pos)
                }
                dataUriStream = stream
                val remaining = (keyBytes.size - pos).coerceAtLeast(0L)
                return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else remaining
            }
        }

        isDataUri = false
        dataUriStream = null
        prefixBuffer = null
        prefixOffset = 0

        // 2. حقن الترويسات الإلزامية ديناميكياً بحسب نوع السيرفر لمنع 403 Forbidden
        val isOstora = isOstoraHost(uriStr)
        val isEdgeNext = uriStr.contains("edgenextcdn.net") || uriStr.contains("shahid")
        val isCatiaCast = uriStr.contains("catiacast.video") || uriStr.contains("imn.iq")
        val isAlRabiaa = uriStr.contains("alrabiaa")
        val isOkRu = uriStr.contains("ok.ru") || uriStr.contains("okcdn.ru") || uriStr.contains("mycdn.me") || uriStr.contains("odnoklassniki")

        val effectiveSpec = if (isOstora || isEdgeNext || isCatiaCast || isAlRabiaa || isOkRu) {
            val headers = HashMap(dataSpec.httpRequestHeaders)
            when {
                isOkRu -> {
                    headers["Referer"] = OkRuStreamResolver.DEFAULT_REFERER
                    headers["User-Agent"] = OkRuStreamResolver.DEFAULT_USER_AGENT
                }
                isOstora -> {
                    headers["User-Agent"] = OstoraApiResolver.DEFAULT_USER_AGENT
                    headers["Referer"] = OstoraApiResolver.DEFAULT_REFERER
                }
                isEdgeNext -> {
                    headers["Referer"] = "https://shahid.mbc.net/"
                    headers["Origin"] = "https://shahid.mbc.net"
                }
                isCatiaCast -> {
                    headers["Referer"] = "https://imn.iq/"
                }
                isAlRabiaa -> {
                    headers["Referer"] = "https://www.alrabiaa.tv/"
                }
            }
            dataSpec.buildUpon().setHttpRequestHeaders(headers).build()
        } else {
            dataSpec
        }

        // Subtitle Sanitization: Intercept and convert Windows-1256 (CP-1256) to clean UTF-8
        if (com.beouttv.app.data.subtitle.SubtitleCharsetHelper.isSubtitleUrl(uriStr)) {
            val upstreamLen = upstream.open(effectiveSpec)
            val outStream = java.io.ByteArrayOutputStream()
            val tempBuffer = ByteArray(8192)
            while (true) {
                val r = upstream.read(tempBuffer, 0, tempBuffer.size)
                if (r == C.RESULT_END_OF_INPUT || r <= 0) break
                outStream.write(tempBuffer, 0, r)
            }
            try { upstream.close() } catch (_: Exception) {}

            val cleanBytes = com.beouttv.app.data.subtitle.SubtitleCharsetHelper.sanitizeSubtitleBytes(outStream.toByteArray())
            isDataUri = true
            val stream = ByteArrayInputStream(cleanBytes)
            val pos = dataSpec.position
            if (pos > 0) {
                stream.skip(pos)
            }
            dataUriStream = stream
            val remaining = (cleanBytes.size - pos).coerceAtLeast(0L)
            return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else remaining
        }

        val upstreamLen = upstream.open(effectiveSpec)

        // 3. فحص ونزع رأس الـ PNG الوهمي عند فتح مقطع الفيديو المباشر (.php أو .ts)
        val pathOnly = uriStr.substringBefore("?")
        val isManifest = pathOnly.endsWith(".m3u8") || (pathOnly.endsWith(".json") && !uriStr.contains("format=ts"))
        if (!isManifest && dataSpec.position == 0L) {
            val skipped = stripPngHeaderSync()
            return if (upstreamLen != C.LENGTH_UNSET.toLong() && skipped > 0) {
                (upstreamLen - skipped).coerceAtLeast(0L)
            } else {
                upstreamLen
            }
        }

        return upstreamLen
    }

    /**
     * فحص ديناميكي لرأس ملف PNG الوهمي (\x89PNG\r\n\x1a\n) وتخطي الكتل حتى كتلة IEND + 8 بايت.
     */
    private fun stripPngHeaderSync(): Long {
        val magic = ByteArray(8)
        var magicRead = 0
        while (magicRead < 8) {
            val r = upstream.read(magic, magicRead, 8 - magicRead)
            if (r == C.RESULT_END_OF_INPUT || r <= 0) break
            magicRead += r
        }

        if (magicRead < 8) {
            if (magicRead > 0) {
                prefixBuffer = magic.copyOf(magicRead)
                prefixOffset = 0
            }
            return 0L
        }

        val isPng = magic[0] == 0x89.toByte() &&
                magic[1] == 0x50.toByte() && // 'P'
                magic[2] == 0x4E.toByte() && // 'N'
                magic[3] == 0x47.toByte() && // 'G'
                magic[4] == 0x0D.toByte() && // '\r'
                magic[5] == 0x0A.toByte() && // '\n'
                magic[6] == 0x1A.toByte() &&
                magic[7] == 0x0A.toByte()

        if (!isPng) {
            prefixBuffer = magic
            prefixOffset = 0
            return 0L
        }

        // هو ملف PNG وهمي: نقرأ حتى نجد كتلة IEND + 8 بايت (4 بايت IEND + 4 بايت CRC)
        val scanBuf = ByteArray(8192)
        var totalRead = 0
        var iendIndex = -1

        while (totalRead < scanBuf.size) {
            val r = upstream.read(scanBuf, totalRead, scanBuf.size - totalRead)
            if (r == C.RESULT_END_OF_INPUT || r <= 0) break
            totalRead += r

            // ابحث عن كتلة IEND ('I', 'E', 'N', 'D')
            for (i in 0..totalRead - 4) {
                if (scanBuf[i] == 0x49.toByte() && // 'I'
                    scanBuf[i + 1] == 0x45.toByte() && // 'E'
                    scanBuf[i + 2] == 0x4E.toByte() && // 'N'
                    scanBuf[i + 3] == 0x44.toByte()    // 'D'
                ) {
                    iendIndex = i
                    break
                }
            }
            if (iendIndex != -1) break
        }

        if (iendIndex != -1) {
            val payloadStart = iendIndex + 8 // تجاوز 'IEND' (4 بايت) و CRC (4 بايت)
            val skipped = 8L + payloadStart.toLong() // 8 بايت magic + payloadStart
            if (totalRead > payloadStart) {
                val remainingLen = totalRead - payloadStart
                val remainingBytes = ByteArray(remainingLen)
                System.arraycopy(scanBuf, payloadStart, remainingBytes, 0, remainingLen)
                prefixBuffer = remainingBytes
                prefixOffset = 0
            } else {
                prefixBuffer = null
                prefixOffset = 0
            }
            return skipped
        }

        // في حال لم يتم العثور على IEND، يتم الاحتفاظ بكامل البايتات المقروءة لعدم فقدان أي بيانات
        val allRead = ByteArray(8 + totalRead)
        System.arraycopy(magic, 0, allRead, 0, 8)
        if (totalRead > 0) {
            System.arraycopy(scanBuf, 0, allRead, 8, totalRead)
        }
        prefixBuffer = allRead
        prefixOffset = 0
        return 0L
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (isDataUri) {
            val stream = dataUriStream ?: return C.RESULT_END_OF_INPUT
            val bytesRead = stream.read(buffer, offset, length)
            return if (bytesRead == -1) C.RESULT_END_OF_INPUT else bytesRead
        }

        // تسليم البايتات المحفوظة من فحص رأس الـ PNG أولاً
        val pBuf = prefixBuffer
        if (pBuf != null && prefixOffset < pBuf.size) {
            val available = pBuf.size - prefixOffset
            val toDeliver = minOf(available, length)
            System.arraycopy(pBuf, prefixOffset, buffer, offset, toDeliver)
            prefixOffset += toDeliver
            if (prefixOffset >= pBuf.size) {
                prefixBuffer = null
            }
            return toDeliver
        }

        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = if (isDataUri) currentUri else (upstream.uri ?: currentUri)

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    @Throws(IOException::class)
    override fun close() {
        dataUriStream?.close()
        dataUriStream = null
        prefixBuffer = null
        prefixOffset = 0
        currentUri = null
        if (!isDataUri) {
            try {
                upstream.close()
            } catch (_: Exception) {}
        }
    }

    private fun isOstoraHost(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("edgenextcdn.net") ||
            lower.contains("catiacast.video") ||
            lower.contains("yacinelive") ||
            lower.contains("yacine") ||
            lower.contains("alrabiaa") ||
            lower.contains("imn.iq") ||
            lower.contains("shahid") ||
            lower.contains("wecima") ||
            lower.contains("govid.live")
        ) {
            return false
        }
        return lower.contains("maziika") ||
                lower.contains("ostora") ||
                lower.contains(".shop") ||
                lower.contains(".top") ||
                lower.contains(".space") ||
                lower.contains(".site") ||
                lower.contains(".fun") ||
                lower.contains("/x1/") ||
                lower.contains("/x2/") ||
                lower.contains("/x3/") ||
                lower.contains("/x4/") ||
                lower.contains("/x5/") ||
                lower.contains("/x6/") ||
                (lower.contains(".json") && lower.contains("token="))
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return OstoraStreamDataSource(upstreamFactory.createDataSource())
        }
    }
}
