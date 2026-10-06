package com.beouttv.app.data.filter

import com.beouttv.app.data.model.Channel
import java.util.Locale

/**
 * Intelligent Channel Matcher that links broadcast metadata from 365Scores / Sports APIs
 * to the actual streaming TV channels inside Beout TV.
 */
object ChannelMatcher {

    /**
     * Finds matching channel in app channels list based on API broadcaster string.
     */
    fun findMatchedChannel(channelQuery: String?, channels: List<Channel>): Channel? {
        if (channelQuery.isNullOrBlank() || channelQuery.equals("null", ignoreCase = true)) {
            return null
        }

        val rawQuery = channelQuery.trim()
        val queryClean = cleanChannelName(rawQuery)
        if (queryClean.isBlank()) return null

        // 1. Direct exact or substring match
        val matched = channels.firstOrNull { ch ->
            val chClean = cleanChannelName(ch.name)
            chClean == queryClean || chClean.contains(queryClean) || queryClean.contains(chClean) || matchTokens(queryClean, chClean)
        }

        return matched
    }

    /**
     * Cleans and normalizes channel name for matching (e.g. "beIN Sports 1 HD" -> "bein1").
     */
    fun cleanChannelName(name: String): String {
        var str = name.lowercase(Locale.ROOT)
            .replace("باقة", "")
            .replace("قناة", "")
            .replace("(1080p fhd)", "")
            .replace("(720p hd)", "")
            .replace("(480p sd)", "")
            .replace("1080p", "")
            .replace("720p", "")
            .replace("480p", "")
            .replace("fhd", "")
            .replace("hd", "")
            .replace("sd", "")
            .replace("premium", "")
            .replace("sports", "sport")
            .replace("سبورتس", "sport")
            .replace("سبورت", "sport")
            .replace("بي ان", "bein")
            .replace("بي إن", "bein")
            .replace("الكاس", "alkass")
            .replace("الكأس", "alkass")
            .replace("أبوظبي", "ad")
            .replace("ابوظبي", "ad")
            .replace("السعودية الرياضية", "ssc")
            .replace("دبي الرياضية", "dubaisport")
            .replace("العراقية الرياضية", "iraqisport")
            .replace("الرابعة الرياضية", "alrabiaa")
            .replace("الرابعة", "alrabiaa")
            .replace("الشارقة الرياضية", "sharjahsport")
            .replace("أون تايم سبورت", "ontime")
            .replace("اون تايم سبورت", "ontime")
            .replace("اون تايم", "ontime")
            .replace("on time", "ontime")
            .replace("on sport", "ontime")

        // Normalize Arabic digits to English
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in arabicDigits.indices) {
            str = str.replace(arabicDigits[i], ('0' + i))
        }

        return str.replace(Regex("[^a-z0-9]"), "").trim()
    }

    private fun matchTokens(q: String, target: String): Boolean {
        val qDigits = q.filter { it.isDigit() }
        val tDigits = target.filter { it.isDigit() }

        // If numbers exist in both, they must match exactly (e.g. 1 == 1, not 1 == 2)
        if (qDigits.isNotEmpty() && tDigits.isNotEmpty() && qDigits != tDigits) {
            return false
        }

        val qLetters = q.filter { it.isLetter() }
        val tLetters = target.filter { it.isLetter() }

        if (qLetters.isEmpty() || tLetters.isEmpty()) return false

        return qLetters.contains(tLetters) || tLetters.contains(qLetters)
    }

    /**
     * Cleans broadcaster display text to ensure no "null" or garbage string is shown.
     */
    fun cleanBroadcastTitle(channel: String?): String? {
        if (channel.isNullOrBlank() || channel.equals("null", ignoreCase = true) || channel.equals("none", ignoreCase = true)) {
            return null
        }
        return channel.trim()
    }
}
