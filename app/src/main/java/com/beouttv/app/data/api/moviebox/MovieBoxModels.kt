package com.beouttv.app.data.api.moviebox

import com.google.gson.annotations.SerializedName

data class MovieBoxResponse<T>(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

data class MovieBoxListData(
    @SerializedName("pager") val pager: MovieBoxPager? = null,
    @SerializedName("items") val items: List<MovieBoxItem> = emptyList()
)

data class MovieBoxPager(
    @SerializedName("hasMore") val hasMore: Boolean = false,
    @SerializedName("nextPage") val nextPage: String? = null,
    @SerializedName("page") val page: String? = null,
    @SerializedName("perPage") val perPage: Int = 20,
    @SerializedName("totalCount") val totalCount: Long = 0L
)

data class MovieBoxCover(
    @SerializedName("url") val url: String? = null,
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
    @SerializedName("thumbnail") val thumbnail: String? = null
)

data class MovieBoxItem(
    @SerializedName("subjectId") val subjectId: String = "",
    @SerializedName("subjectType") val subjectType: Int = 1, // 1 = Movie / Anime Movie, 2 = Series / Anime Series
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("genre") val genre: String = "",
    @SerializedName("cover") val cover: MovieBoxCover? = null,
    @SerializedName("countryName") val countryName: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("imdbRatingValue") val imdbRatingValue: String? = null,
    @SerializedName("score") val score: String? = null,
    @SerializedName("resourceDetectors") val resourceDetectors: List<MovieBoxResourceDetector>? = null,
    @SerializedName("trailer") val trailer: MovieBoxTrailer? = null
)

data class MovieBoxVideoAddress(
    @SerializedName("videoId") val videoId: String = "",
    @SerializedName("definition") val definition: String = "sd",
    @SerializedName("url") val url: String = "",
    @SerializedName("duration") val duration: Long = 0L,
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
    @SerializedName("fps") val fps: Int = 0,
    @SerializedName("bitrate") val bitrate: Int = 0,
    @SerializedName("type") val type: Int = 1
)

data class MovieBoxTrailer(
    @SerializedName("VideoAddress") val videoAddress: MovieBoxVideoAddress? = null,
    @SerializedName("cover") val cover: MovieBoxCover? = null
)

data class MovieBoxStoryItem(
    val id: String,
    val title: String,
    val coverUrl: String,
    val videoUrl: String,
    val durationSeconds: Long = 15L,
    val subjectId: String,
    val subjectType: Int = 1,
    val score: String = "8.0",
    val genre: String = "",
    val subtitleUrl: String? = null,
    val isDubbed: Boolean = false,
    val isArabic: Boolean = true,
    val studioBadge: String = "NETFLIX",
    val studioColorHex: Long = 0xFFE50914L
)

data class MovieBoxStaff(
    @SerializedName("staffId") val staffId: String = "",
    @SerializedName("staffType") val staffType: Int = 1,
    @SerializedName("name") val name: String = "",
    @SerializedName("character") val character: String = "",
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class MovieBoxExtCaption(
    @SerializedName("id") val id: String = "",
    @SerializedName("lan") val lan: String = "",
    @SerializedName("lanName") val lanName: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("size") val size: String? = null,
    @SerializedName("delay") val delay: Int = 0
)

data class MovieBoxResolution(
    @SerializedName("resolution") val resolution: Int = 1080,
    @SerializedName("resourceLink") val resourceLink: String? = null,
    @SerializedName("sourceUrl") val sourceUrl: String? = null,
    @SerializedName("size") val size: com.google.gson.JsonElement? = null,
    @SerializedName("codecName") val codecName: String = "h264",
    @SerializedName("duration") val duration: Long = 0L,
    @SerializedName("se") val se: Int = 0,
    @SerializedName("ep") val ep: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("extCaptions") val extCaptions: List<MovieBoxExtCaption>? = null
)

data class MovieBoxResourceDetector(
    @SerializedName("type") val type: Int = 1,
    @SerializedName("totalEpisode") val totalEpisode: Int = 1,
    @SerializedName("totalSize") val totalSize: String? = null,
    @SerializedName("uploadBy") val uploadBy: String? = null,
    @SerializedName("resourceLink") val resourceLink: String? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("resourceId") val resourceId: String? = null,
    @SerializedName("postId") val postId: String? = null,
    @SerializedName("resolutionList") val resolutionList: List<MovieBoxResolution>? = null,
    @SerializedName("extCaptions") val extCaptions: List<MovieBoxExtCaption>? = null,
    @SerializedName("subjectId") val subjectId: String? = null,
    @SerializedName("codecName") val codecName: String? = null
)

data class MovieBoxSubjectDetail(
    @SerializedName("subjectId") val subjectId: String = "",
    @SerializedName("subjectType") val subjectType: Int = 1,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("durationSeconds") val durationSeconds: Long = 0L,
    @SerializedName("genre") val genre: String = "",
    @SerializedName("cover") val cover: MovieBoxCover? = null,
    @SerializedName("countryName") val countryName: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("imdbRatingValue") val imdbRatingValue: String? = null,
    @SerializedName("staffList") val staffList: List<MovieBoxStaff>? = null,
    @SerializedName("resourceDetectors") val resourceDetectors: List<MovieBoxResourceDetector>? = null,
    @SerializedName("stills") val stills: com.google.gson.JsonElement? = null,
    @SerializedName("trailer") val trailer: com.google.gson.JsonElement? = null
)
