package com.beouttv.app.data.model

import androidx.annotation.Keep

@Keep
data class ShortDramaCategory(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val count: Int = 0
)

@Keep
data class ShortDramaSeries(
    val id: String,
    val name: String,
    val coverUrl: String,
    val episodeCount: Int = 0,
    val platform: String = "",
    val synopsis: String = "",
    val categoryId: String = ""
)

@Keep
data class ShortDramaEpisode(
    val id: String,
    val seriesId: String,
    val episodeNumber: Int,
    val title: String,
    val thumbnailUrl: String = "",
    val isFree: Boolean = true
)

@Keep
data class ShortDramaSource(
    val id: String,
    val name: String,
    val streamUrl: String,
    val userAgent: String = "okhttp/4.12.0",
    val quality: String = "HD",
    val isDirectCdn: Boolean = true,
    val subtitleUrl: String = ""
)

@Keep
data class ShortDramaFeedItem(
    val series: ShortDramaSeries,
    val episode: ShortDramaEpisode,
    val sources: List<ShortDramaSource> = emptyList(),
    val currentSourceIndex: Int = 0
)
