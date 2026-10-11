package com.beouttv.app.data.filter

import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series

/**
 * محرك الحماية الأسرية والفلترة الأخلاقية
 * يمنع ويحظر تماماً أي محتوى يروج للمثلية أو المواد الإباحية أو الأفكار الدخيلة والسيئة.
 */
object ContentFilterEngine {

    // Base64-encoded adult and explicit filter keywords (Arabic and International)
    // Decoded at runtime to maintain maximum content filtering coverage
    private val ENCODED_BLOCKED_TERMS = listOf(
        "2YPYsw==", "2KfZhNmD2LM=", "2YPYs9mH2Kc=", "2YPYs9mD", "2LfZitiy", "2KfZhNi32YrYsg==",
        "2LfZitiy2YfYpw==", "2LfZitiy2YM=", "2LLYqA==", "2KfZhNiy2Kg=", "2LLYqNmH", "2LLYqNmD",
        "2YbZitmD", "2KfZhNmG2YrZgw==", "2YrZhtmK2YM=", "2KrZhtin2YrZgw==", "2YXYqtmG2KfZg9ip",
        "2YXYqtmG2KfZgw==", "2YXZhtmK2YjZgw==", "2YXZhtmK2YjZg9ip", "2LTYsdmF2YjYt9ip", "2KfZhNi02LHZhdmI2LfYqQ==",
        "2LTYsdmF2YjYt9mH", "2YLYrdio2Kk=", "2KfZhNmC2K3YqNip", "2YLYrdio2Yc=", "2LnYp9mH2LHYqQ==",
        "2LnYp9mH2LHYp9iq", "2K/ZitmI2Ks=", "2YTZiNin2Lc=", "2LPYrdin2YI=", "2KjYstin2LI=",
        "2YXYpNiu2LHYqQ==", "2YLYttmK2Kg=", "2YHYsdis", "2YXZh9io2YQ=", "2KfYs9iq2YXZhtin2KE=",
        "2KfZgdmE2KfZhSDYs9mD2LM=", "2LPZg9iz", "2KjZiNix2YY=", "2KrYudix2Yo=", "2LnYsdmK",
        "2KzZhtiz2Yo=", "2KfYqNin2K3Zig==", "2KXYqNin2K3Zig=="
    )

    // Standard moral & family safety blocked keywords
    private val STANDARD_KEYWORDS = listOf(
        // LGBTQ / Non-family content
        "مثلي", "مثلية", "شذوذ", "شاذ", "شواذ", "مجتمع الميم", "قوس قزح", "رينبو",
        "lgbt", "lgbtq", "lgbtq+", "gay", "lesbian", "homosexual", "queer", "transgender",
        "bisexual", "pride", "gender identity", "same-sex", "boys love", "bl drama",

        // Adult classification tags
        "إباحي", "اباحي", "جنسي", "جنس", "للكبار فقط", "سكس", "بورن", "تعري", "عري",
        "+18", "18+", "r-18", "erotic", "erotica", "porn", "porno", "pornography",
        "xxx", "adult only", "nudity", "nsfw", "sensual", "softcore", "hardcore",
        "sex", "sexual", "explicit content", "midnight", "ميدنايت",
        "xvideos", "xnxx", "pornhub", "brazzers", "playboy", "onlyfans", "xhamster",
        "blowjob", "cumshot", "deepthroat", "boobs", "pussy", "vagina", "penis", "dildo"
    )

    private val ALL_BLOCKED_TERMS: Set<String> by lazy {
        val decoded = ENCODED_BLOCKED_TERMS.mapNotNull { b64 ->
            try {
                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8).lowercase().trim()
            } catch (_: Exception) {
                null
            }
        }
        (STANDARD_KEYWORDS.map { it.lowercase().trim() } + decoded).toSet()
    }

    // Permanently forced enabled for strict family protection
    @Volatile
    var isAdultFilterEnabled: Boolean = true
        get() = true
        set(_) { field = true }

    /**
     * Normalize Arabic text by removing tashkeel and unifying letter variations
     */
    private fun normalizeArabic(input: String): String {
        return input
            .replace(Regex("[\\u064B-\\u0652\\u0670]"), "") // Remove diacritics
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .lowercase()
            .trim()
    }

    /**
     * فحص النص لمعرفة ما إذا كان يحتوي على أي محتوى محظور
     */
    fun isContentBlocked(vararg texts: String?): Boolean {
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            val rawLower = text.lowercase().trim()
            val normalized = normalizeArabic(rawLower)

            // Split into tokens for precise word boundary matching
            val tokens = normalized.split(Regex("[\\s\\p{Punct}0-9_]+")).filter { it.isNotBlank() }

            for (term in ALL_BLOCKED_TERMS) {
                val normTerm = normalizeArabic(term)
                if (normTerm.isBlank()) continue

                if (normTerm.length <= 3 && !normTerm.contains(" ")) {
                    // For short terms, match full token or token starting with 'ال'
                    for (token in tokens) {
                        if (token == normTerm || token == "ال$normTerm" || token == "و$normTerm" || token == "ف$normTerm") {
                            return true
                        }
                    }
                } else {
                    // For phrases and longer terms, check direct substring
                    if (normalized.contains(normTerm) || rawLower.contains(term)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * فلترة قائمة الأفلام وإزالة أي فيلم مخالف تماماً
     */
    fun filterMovies(movies: List<Movie>): List<Movie> {
        return movies.filter { movie ->
            !isContentBlocked(
                movie.title,
                movie.originalTitle,
                movie.genre,
                movie.description,
                movie.genresList.joinToString(" ")
            )
        }
    }

    /**
     * فلترة قائمة القنوات وإزالة أي قناة مخالفة
     */
    fun filterChannels(channels: List<Channel>): List<Channel> {
        return channels.filter { channel ->
            !isContentBlocked(
                channel.name,
                channel.category,
                channel.currentProgram
            )
        }
    }

    /**
     * فلترة قائمة المسلسلات
     */
    fun filterSeries(seriesList: List<Series>): List<Series> {
        return seriesList.filter { series ->
            !isContentBlocked(
                series.title,
                series.genre,
                series.description
            )
        }
    }

    /**
     * تنقية النصوص والقصص من أي مصطلحات دخيلة إن وجدت
     */
    fun sanitizeText(rawText: String): String {
        var clean = rawText
        for (keyword in ALL_BLOCKED_TERMS) {
            clean = clean.replace(Regex("(?i)\\b$keyword\\b"), "")
        }
        return clean.trim()
    }
}
