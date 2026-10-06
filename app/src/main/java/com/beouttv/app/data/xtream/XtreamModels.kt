package com.beouttv.app.data.xtream

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class XtreamAccountType {
    XTREAM,
    M3U
}

/**
 * Represents a personal IPTV subscription (Xtream Codes API or M3U playlist).
 * Supports up to 3 accounts with automatic expiration validation and Protobuf caching.
 */
data class XtreamAccount(
    val id: String,
    val name: String,
    val type: XtreamAccountType = XtreamAccountType.XTREAM,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val expiryTimestamp: Long = 0L, // Unix timestamp in seconds (0 = unlimited / unknown)
    val isActive: Boolean = true,
    val channelsCount: Int = 0,
    val moviesCount: Int = 0,
    val seriesCount: Int = 0,
    val showChannels: Boolean = true,
    val showMovies: Boolean = true,
    val showSeries: Boolean = true,
    val lastSyncTime: Long = 0L
) {
    /**
     * Checks whether this Xtream account subscription is expired.
     */
    fun isExpired(): Boolean {
        if (expiryTimestamp <= 0L) return false
        val nowSecs = System.currentTimeMillis() / 1000L
        return nowSecs > expiryTimestamp
    }

    /**
     * Formats expiry date for display in UI (Arabic locale).
     */
    fun getFormattedExpiry(): String {
        if (expiryTimestamp <= 0L) return "غير محدد / مفتوح"
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateStr = sdf.format(Date(expiryTimestamp * 1000L))
        return if (isExpired()) {
            "منتهي الصلاحية ($dateStr)"
        } else {
            "ينتهي في: $dateStr"
        }
    }

    fun getDaysRemaining(): Long {
        if (expiryTimestamp <= 0L) return Long.MAX_VALUE
        val nowSecs = System.currentTimeMillis() / 1000L
        val diff = expiryTimestamp - nowSecs
        return if (diff > 0) diff / 86400L else 0L
    }
}

data class XtreamAuthResponse(
    val success: Boolean,
    val message: String = "",
    val serverName: String = "",
    val username: String = "",
    val status: String = "",
    val expDate: Long = 0L,
    val maxConnections: String = "1"
)
