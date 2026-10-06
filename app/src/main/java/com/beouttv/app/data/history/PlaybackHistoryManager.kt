package com.beouttv.app.data.history

import android.content.Context
import android.content.SharedPreferences
import com.beouttv.app.data.model.ContinueWatchingItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages playback history and intelligent "Continue Watching" progress
 * with persistent SharedPreferences storage.
 */
object PlaybackHistoryManager {

    private const val PREFS_NAME = "beout_playback_history_prefs"
    private const val KEY_HISTORY = "history_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Updates playback progress for a given media item.
     * If progress is between 2% and 95%, it is saved in Continue Watching.
     * If watched past 95%, it is removed from active continue watching.
     */
    fun saveProgress(
        context: Context,
        id: String,
        title: String,
        streamUrl: String,
        thumbnailUrl: String,
        positionMs: Long,
        durationMs: Long,
        isLive: Boolean = false,
        backdropUrl: String = ""
    ) {
        if (isLive || id.isBlank() || durationMs <= 0L) return
        if (id.startsWith("ch_") || id.startsWith("channel_") || id.startsWith("live_") ||
            streamUrl.contains("/live/") || streamUrl.contains("live.m3u8") || streamUrl.contains("50fps")) {
            return
        }

        try {
            val progressPercent = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val prefs = getPrefs(context)
            val historyList = getRawHistory(context).toMutableList()

            // Remove existing entry for same ID or streamUrl
            historyList.removeAll { it.optString("id") == id || (streamUrl.isNotBlank() && it.optString("streamUrl") == streamUrl) }

            // Only keep in Continue Watching if progress is between 2% and 95%
            if (progressPercent in 0.02f..0.95f && positionMs > 5000L) {
                val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
                val remainingMinutes = (remainingMs / 60000L).toInt()
                val remainingLabel = when {
                    remainingMinutes >= 60 -> {
                        val hours = remainingMinutes / 60
                        val mins = remainingMinutes % 60
                        if (mins > 0) "متبقي $hours س $mins د" else "متبقي $hours ساعة"
                    }
                    remainingMinutes > 0 -> "متبقي $remainingMinutes دقيقة"
                    else -> "متبقي أقل من دقيقة"
                }

                val finalThumb = backdropUrl.ifBlank { thumbnailUrl }
                val itemObj = JSONObject().apply {
                    put("id", id)
                    put("title", title)
                    put("streamUrl", streamUrl)
                    put("thumbnailUrl", finalThumb)
                    put("backdropUrl", backdropUrl)
                    put("positionMs", positionMs)
                    put("durationMs", durationMs)
                    put("progressPercent", progressPercent.toDouble())
                    put("remainingLabel", remainingLabel)
                    put("timestamp", System.currentTimeMillis())
                }

                historyList.add(0, itemObj) // Add to front (most recent)
            }

            // Cap at 20 most recent items
            val capped = historyList.take(20)
            val jsonArr = JSONArray()
            capped.forEach { jsonArr.put(it) }
            prefs.edit().putString(KEY_HISTORY, jsonArr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets saved position in milliseconds for a specific media item.
     */
    fun getSavedPosition(context: Context, id: String, streamUrl: String = ""): Long {
        if (id.isBlank()) return 0L
        val history = getRawHistory(context)
        for (item in history) {
            if (item.optString("id") == id || (streamUrl.isNotBlank() && item.optString("streamUrl") == streamUrl)) {
                return item.optLong("positionMs", 0L)
            }
        }
        return 0L
    }

    /**
     * Returns list of ContinueWatchingItem for HomeScreen display.
     */
    fun getContinueWatchingList(context: Context): List<ContinueWatchingItem> {
        val raw = getRawHistory(context)
        val list = mutableListOf<ContinueWatchingItem>()
        for (item in raw) {
            val id = item.optString("id", "")
            val stream = item.optString("streamUrl", "")
            // Exclude any live channels
            if (id.startsWith("ch_") || id.startsWith("channel_") || id.startsWith("live_") || stream.contains("/live/")) {
                continue
            }
            val title = item.optString("title", "فيديو")
            var thumb = item.optString("backdropUrl", "")
            if (thumb.isBlank()) {
                thumb = com.beouttv.app.data.proto.ProtobufDiskCache.getCachedBackdrop(id).orEmpty()
            }
            if (thumb.isBlank()) {
                thumb = item.optString("thumbnailUrl", "")
            }
            val progress = item.optDouble("progressPercent", 0.0).toFloat()
            val remaining = item.optString("remainingLabel", "متابعة المشاهدة")

            if (id.isNotBlank() && stream.isNotBlank()) {
                list.add(
                    ContinueWatchingItem(
                        id = id,
                        title = title,
                        thumbnailUrl = thumb,
                        progressPercent = progress,
                        remainingLabel = remaining,
                        streamUrl = stream
                    )
                )
            }
        }
        return list
    }

    private fun getRawHistory(context: Context): List<JSONObject> {
        val prefs = getPrefs(context)
        val rawStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<JSONObject>()
        try {
            val jsonArr = JSONArray(rawStr)
            for (i in 0 until jsonArr.length()) {
                jsonArr.optJSONObject(i)?.let { list.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
