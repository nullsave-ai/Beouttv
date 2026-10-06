package com.beouttv.app.data.subtitle

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Universal Subtitle Encoding & ASS Tag Sanitizer for MovieBox & External Subtitles.
 *
 * Resolves the infamous Arabic subtitle issue where SRT files encoded in Windows-1256 (CP-1256)
 * cause ExoPlayer's SubripDecoder (which strictly expects UTF-8) to fail byte decoding and
 * render all Arabic dialogue as question marks `??? ??? ???` (e.g. Aquaman and older titles).
 *
 * Also strips embedded ASS/SSA style tags (e.g. {\an9...}, {\c&H...}, {\pos...}) that disrupt
 * clean SubRip rendering.
 */
object SubtitleCharsetHelper {

    private val CP1256: Charset by lazy {
        try {
            Charset.forName("windows-1256")
        } catch (_: Exception) {
            StandardCharsets.ISO_8859_1
        }
    }

    private val ISO8859_6: Charset by lazy {
        try {
            Charset.forName("ISO-8859-6")
        } catch (_: Exception) {
            CP1256
        }
    }

    private val ASS_TAG_REGEX = Regex("""\{[^}]*\}""")

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Checks if a URL or URI points to a SubRip (.srt) subtitle stream.
     */
    fun isSubtitleUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".srt") ||
                lower.contains(".srt?") ||
                lower.contains("/subtitle/") ||
                lower.contains("subtitles")
    }

    /**
     * Sanitizes raw subtitle bytes:
     * 1. Strips UTF-8 BOM if present.
     * 2. Tests strict UTF-8 decoding.
     * 3. If UTF-8 fails or is malformed, decodes via Windows-1256 (CP-1256 Arabic) or ISO-8859-6.
     * 4. Strips embedded ASS/SSA tags like {\an8...}.
     * 5. Encodes clean text as standard UTF-8 ByteArray.
     */
    fun sanitizeSubtitleBytes(rawBytes: ByteArray): ByteArray {
        if (rawBytes.isEmpty()) return rawBytes

        // Strip UTF-8 BOM (0xEF, 0xBB, 0xBF) if present
        val contentBytes = if (rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte()
        ) {
            rawBytes.copyOfRange(3, rawBytes.size)
        } else {
            rawBytes
        }

        var decodedString: String? = null

        // Try strict UTF-8 decoding
        try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val charBuffer = decoder.decode(ByteBuffer.wrap(contentBytes))
            decodedString = charBuffer.toString()
        } catch (_: Exception) {
            // Malformed UTF-8 sequence, proceed to Windows-1256 / ISO-8859-6
        }

        // If UTF-8 decoding failed, try Windows-1256 (Arabic Windows ANSI)
        if (decodedString == null) {
            try {
                val winText = String(contentBytes, CP1256)
                if (winText.any { it in '\u0600'..'\u06FF' }) {
                    decodedString = winText
                } else {
                    val isoText = String(contentBytes, ISO8859_6)
                    decodedString = if (isoText.any { it in '\u0600'..'\u06FF' }) isoText else winText
                }
            } catch (_: Exception) {
                decodedString = String(contentBytes, StandardCharsets.UTF_8)
            }
        }

        // Clean ASS tags like {\an9\fn...}
        val cleanText = ASS_TAG_REGEX.replace(decodedString, "").trim()
        return cleanText.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Converts a remote subtitle URL to a clean, cached local UTF-8 .srt file.
     * Returns the file:// URI if resolved, or originalUrl on error.
     */
    suspend fun resolveCleanSubtitleUri(context: Context, originalUrl: String): String = withContext(Dispatchers.IO) {
        if (originalUrl.isBlank() || originalUrl.startsWith("file://") || originalUrl.startsWith("/")) {
            return@withContext originalUrl
        }

        val cacheDir = File(context.cacheDir, "clean_subtitles")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val safeHash = md5(originalUrl)
        val targetFile = File(cacheDir, "sub_${safeHash}.srt")

        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext Uri.fromFile(targetFile).toString()
        }

        try {
            val request = Request.Builder()
                .url(originalUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext originalUrl

            val bodyBytes = response.body?.bytes() ?: return@withContext originalUrl
            val cleanBytes = sanitizeSubtitleBytes(bodyBytes)

            FileOutputStream(targetFile).use { fos ->
                fos.write(cleanBytes)
            }

            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            originalUrl
        }
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }
}
