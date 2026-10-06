package com.beouttv.app.data.filter

import com.beouttv.app.data.model.Channel
import com.beouttv.app.data.model.Movie
import com.beouttv.app.data.model.Series

/**
 * محرك الحماية الأسرية والفلترة الأخلاقية
 * يمنع ويحظر تماماً أي محتوى يروج للمثلية أو المواد الإباحية أو الأفكار الدخيلة والسيئة.
 */
object ContentFilterEngine {

    // قائمة الكلمات الدلالية المحظورة (إباحية، مثلية، شذوذ، محتوى غير لائق)
    private val BLOCKED_KEYWORDS = listOf(
        // مصطلحات المثلية والشذوذ بالعربية والإنجليزية
        "مثلي", "مثلية", "شذوذ", "شاذ", "شواذ", "مجتمع الميم", "قوس قزح", "رينبو",
        "lgbt", "lgbtq", "lgbtq+", "gay", "lesbian", "homosexual", "queer", "transgender",
        "bisexual", "pride", "gender identity", "same-sex", "boys love", "bl drama",

        // مصطلحات إباحية ومحتوى للكبار بالعربية والإنجليزية ومحتوى محظور
        "إباحي", "اباحي", "جنسي", "جنس", "للكبار فقط", "سكس", "بورن", "تعري", "عري",
        "+18", "18+", "r-18", "erotic", "erotica", "porn", "porno", "pornography",
        "xxx", "adult only", "nudity", "nsfw", "sensual", "softcore", "hardcore",
        "sex", "sexual", "explicit content", "midnight", "ميدنايت"
    )

    var isAdultFilterEnabled: Boolean = true

    /**
     * فحص النص لمعرفة ما إذا كان يحتوي على أي محتوى محظور
     */
    fun isContentBlocked(vararg texts: String?): Boolean {
        if (!isAdultFilterEnabled) return false
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            val lower = text.lowercase().trim()
            for (keyword in BLOCKED_KEYWORDS) {
                // الفحص الدقيق بالكلمات المستقلة والجزئية المحظورة
                if (lower.contains(keyword)) {
                    return true
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
        for (keyword in BLOCKED_KEYWORDS) {
            clean = clean.replace(Regex("(?i)\\b$keyword\\b"), "")
        }
        return clean.trim()
    }
}
