package com.beouttv.app.data.source.stream

import android.net.Uri
import android.util.Base64
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistParserFactory
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
import androidx.media3.exoplayer.upstream.ParsingLoadable
import info.t4w.vp.view.HlsKeyDecryptor
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.ceil

/**
 * مصنع مفسر ومصلح قوائم HLS الذكي (Intelligent HLS Manifest Sanitizer & Auto-Repair Engine):
 * 1. فك تشفير وتطهير مفاتيح HLS المشفرة بصيغة data:text/plain;base64 عبر JNI (libnext.so).
 * 2. الكشف التلقائي عن قوائم البث المباشر (Media Playlists) التي تفتقر إلى وسم #EXT-X-TARGETDURATION وحسابه وحقنه فورياً لمنع خطأ PARSING_MANIFEST_MALFORMED.
 * 3. تصحيح الفروقات الزمنية في حال كانت مدة أحد المقاطع (#EXTINF) أكبر من المدة المستهدفة لضمان الامتثال الصارم لـ RFC 8216.
 * 4. تزويد القوائم بوسوم #EXT-X-VERSION و #EXT-X-MEDIA-SEQUENCE التلقائية وتنظيف أحرف الـ BOM غير المرئية لمنع أي انهيار في المشغل.
 */
class SanitizedHlsPlaylistParserFactory(
    private val defaultFactory: HlsPlaylistParserFactory = DefaultHlsPlaylistParserFactory()
) : HlsPlaylistParserFactory {

    override fun createPlaylistParser(): ParsingLoadable.Parser<HlsPlaylist> {
        val defaultParser = defaultFactory.createPlaylistParser()
        return ParsingLoadable.Parser { uri, inputStream ->
            val sanitized = getSanitizedStream(inputStream)
            defaultParser.parse(uri, sanitized)
        }
    }

    override fun createPlaylistParser(
        multivariantPlaylist: HlsMultivariantPlaylist,
        previousMediaPlaylist: HlsMediaPlaylist?
    ): ParsingLoadable.Parser<HlsPlaylist> {
        val defaultParser = defaultFactory.createPlaylistParser(multivariantPlaylist, previousMediaPlaylist)
        return ParsingLoadable.Parser { uri, inputStream ->
            val sanitized = getSanitizedStream(inputStream)
            defaultParser.parse(uri, sanitized)
        }
    }

    companion object {
        private const val TAG = "SanitizedHlsParser"
        private val DATA_URI_PATTERN = Pattern.compile("""data:text/plain;base64,([a-zA-Z0-9+/=]+)""")
        private val EXTINF_PATTERN = Pattern.compile("""#EXTINF:\s*([0-9]+(?:\.[0-9]+)?)""")
        private val TARGET_DURATION_PATTERN = Pattern.compile("""#EXT-X-TARGETDURATION:\s*([0-9]+)""")

        private val CLEAN_B64_CACHE = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun getSanitizedStream(inputStream: InputStream): InputStream {
            var text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

            // 1. تنظيف أحرف BOM ومسافات البداية والنهاية
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1)
            }
            text = text.trim()

            // إذا كان النص فارغاً نعيده كما هو
            if (text.isBlank()) {
                return ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
            }

            // 2. تطهير وفك تشفير مفاتيح شبكة الأسطورة المشفرة بـ data: URI مع كاش فوري
            if (text.contains("data:text/plain;base64,")) {
                val matcher = DATA_URI_PATTERN.matcher(text)
                val sb = StringBuffer()
                var replacedCount = 0
                while (matcher.find()) {
                    val b64Payload = matcher.group(1) ?: ""
                    try {
                        val cleanB64 = CLEAN_B64_CACHE.getOrPut(b64Payload) {
                            var paddedB64 = b64Payload.trim()
                            while (paddedB64.length % 4 != 0) paddedB64 += "="
                            val rawBytes = Base64.decode(paddedB64, Base64.DEFAULT)
                            val decryptedKey = HlsKeyDecryptor.decryptHlsKeyPayload(rawBytes)
                            Base64.encodeToString(decryptedKey, Base64.NO_WRAP)
                        }
                        matcher.appendReplacement(sb, Matcher.quoteReplacement("data:text/plain;base64,$cleanB64"))
                        replacedCount++
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Failed to sanitize key payload: ${e.message}")
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0) ?: ""))
                    }
                }
                matcher.appendTail(sb)
                text = sb.toString()
            }

            // 3. معالجة وتأهيل قوائم الوسائط المباشرة (Media Playlists) التي تحوي مقاطع فيديو
            if (text.contains("#EXTINF:")) {
                // التأكد من أن الترويسة القياسية #EXTM3U موجودة في السطر الأول
                if (!text.startsWith("#EXTM3U")) {
                    text = if (text.contains("#EXTM3U")) {
                        "#EXTM3U\n" + text.substringAfter("#EXTM3U").trim()
                    } else {
                        "#EXTM3U\n$text"
                    }
                }

                // حساب أقصى مدة لمقطع في القائمة لمطابقتها مع Target Duration
                var maxSegmentDuration = 6
                val extInfMatcher = EXTINF_PATTERN.matcher(text)
                while (extInfMatcher.find()) {
                    val d = extInfMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                    val ceilD = ceil(d).toInt()
                    if (ceilD > maxSegmentDuration) {
                        maxSegmentDuration = ceilD
                    }
                }

                // الكشف عما إذا كان وسم #EXT-X-TARGETDURATION مفقوداً أو قيمته أقل من أطول مقطع
                val targetMatcher = TARGET_DURATION_PATTERN.matcher(text)
                if (targetMatcher.find()) {
                    val existingTarget = targetMatcher.group(1)?.toIntOrNull() ?: 0
                    if (existingTarget < maxSegmentDuration) {
                        // ترقية Target Duration ليطابق أو يفوق أطول مقطع وفق مواصفات HLS RFC 8216
                        text = targetMatcher.replaceFirst("#EXT-X-TARGETDURATION:$maxSegmentDuration")
                    }
                } else {
                    // حقن الوسم المفقود الذي كان يسبب خطأ PARSING_MANIFEST_MALFORMED
                    val versionIdx = text.indexOf("#EXT-X-VERSION:")
                    if (versionIdx != -1) {
                        val endOfLine = text.indexOf("\n", versionIdx)
                        if (endOfLine != -1) {
                            text = text.substring(0, endOfLine + 1) +
                                    "#EXT-X-TARGETDURATION:$maxSegmentDuration\n" +
                                    text.substring(endOfLine + 1)
                        } else {
                            text += "\n#EXT-X-TARGETDURATION:$maxSegmentDuration"
                        }
                    } else {
                        text = text.replaceFirst(
                            "#EXTM3U",
                            "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:$maxSegmentDuration"
                        )
                    }
                }

                // ضمان وجود وسم #EXT-X-VERSION
                if (!text.contains("#EXT-X-VERSION:")) {
                    text = text.replaceFirst("#EXTM3U", "#EXTM3U\n#EXT-X-VERSION:3")
                }

                // ضمان وجود وسم #EXT-X-MEDIA-SEQUENCE
                if (!text.contains("#EXT-X-MEDIA-SEQUENCE:")) {
                    val targetIdx = text.indexOf("#EXT-X-TARGETDURATION:")
                    if (targetIdx != -1) {
                        val endOfLine = text.indexOf("\n", targetIdx)
                        if (endOfLine != -1) {
                            text = text.substring(0, endOfLine + 1) +
                                    "#EXT-X-MEDIA-SEQUENCE:0\n" +
                                    text.substring(endOfLine + 1)
                        }
                    }
                }
            }

            return ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
        }
    }
}
