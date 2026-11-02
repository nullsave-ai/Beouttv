package com.beouttv.app.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** عنصر محتوى موحد: movie | series | channel | episode */
@Immutable
@Serializable
data class Item(
    val id: String,
    val kind: String,
    val title: String,
    val poster: String? = null,
    val backdrop: String? = null,
    val logo: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val durationMin: Int? = null,
    val quality: String? = null,
    val category: String? = null,
    val live: Boolean = false,
    val online: Boolean = true,
    val parentId: String? = null,
    val streamUrl: String? = null,
) {
    val isChannel get() = kind == "channel"
    val isSeries get() = kind == "series"
}

@Immutable
@Serializable
data class Section(
    val id: String,
    /** hero | row | channels | continue | categories */
    val type: String,
    val title: String = "",
    val items: List<Item> = emptyList(),
)

@Immutable
@Serializable
data class HomeResponse(val sections: List<Section> = emptyList())

@Immutable
@Serializable
data class Category(val id: String, val name: String)

@Immutable
@Serializable
data class Page(val items: List<Item> = emptyList(), val next: String? = null)

@Immutable
@Serializable
data class Episode(
    val id: String,
    val number: Int,
    val title: String = "",
    val durationMin: Int? = null,
    val poster: String? = null,
)

@Immutable
@Serializable
data class Season(
    val id: String,
    val number: Int,
    val title: String = "",
    val episodes: List<Episode> = emptyList(),
)

@Immutable
@Serializable
data class Detail(
    val item: Item,
    val seasons: List<Season> = emptyList(),
    val related: List<Item> = emptyList(),
)

@Immutable
@Serializable
data class PlayInfo(val url: String, val live: Boolean = false)

@Immutable
@Serializable
data class AppConfig(
    val minVersion: Int = 0,
    val latestVersion: Int = 0,
    val updateUrl: String = "",
    val updateTitle: String = "",
    val updateMessage: String = "",
    val forceUpdate: Boolean = false,
    val maxReconnect: Int = 6,
    val maintenance: String? = null,
)

@Serializable
data class CloudCatalog(
    val channels: List<CloudChannel> = emptyList(),
    val matches: List<CloudMatch> = emptyList(),
    val movies: List<CloudMovie> = emptyList(),
    val tournament_ids: List<Int> = emptyList(),
    val app_config: CloudAppConfig? = null,
    val last_updated: Long = 0L,
)

@Serializable
data class CloudChannel(
    val id: Long = 0,
    val name: String = "",
    val group: String = "عام",
    val logo: String = "",
    val stream_url: String? = null,
    val url: String? = null,
    val number: Int = 0,
)

@Serializable
data class CloudMatch(
    val home: String = "",
    val away: String = "",
    val time: String = "",
    val competition: String = "",
    val channel: String = "",
    val day: Int = 0,
    val live: Boolean = false,
    val score: String? = null,
    val stream_url: String? = null,
)

@Serializable
data class CloudEpisode(
    val title: String = "",
    val url: String = "",
)

@Serializable
data class CloudMovie(
    val id: Long = 0,
    val title: String = "",
    val kind: String = "movie",
    val year: Int = 2024,
    val rating: Double = 7.0,
    val duration: Int = 110,
    val genres: List<String> = emptyList(),
    val poster: String = "",
    val backdrop: String = "",
    val url: String = "",
    val synopsis: String = "",
    val episodes: List<CloudEpisode> = emptyList(),
)

@Serializable
data class CloudAppConfig(
    val min_version_code: Int = 1,
    val latest_version_code: Int = 1,
    val update_url: String = "",
    val update_title: String = "",
    val update_message: String = "",
    val force_update: Boolean = false,
)

@Immutable
@Serializable
data class HistoryEntry(
    val key: String,
    val item: Item,
    val episodeId: String? = null,
    val label: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val at: Long = 0,
)

@Immutable
data class Settings(
    val theme: String = "dark",      // light | dark | blue
    val maxHeight: Int = 0,          // 0 = تلقائي
    val resume: Boolean = true,
    val notifications: Boolean = true,
    val keepScreenOn: Boolean = true,
)
