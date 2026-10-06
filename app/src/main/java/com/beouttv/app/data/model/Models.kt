package com.beouttv.app.data.model

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val category: String,
    val isLive: Boolean = true,
    val currentProgram: String? = null,
    val isFavorite: Boolean = false,
    val streamUrl: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val origin: String? = null,
    val cookie: String? = null,
    val customHeaders: Map<String, String>? = null,
    val drmKeyId: String? = null,
    val drmKey: String? = null,
    val drmLicenseUrl: String? = null,
    val groupId: String? = null,
    val serverId: String? = null,
    val servers: List<StreamServer> = emptyList()
)

data class CastMember(
    val name: String,
    val character: String = "",
    val profileUrl: String? = null
)

data class StreamServer(
    val id: String,
    val name: String,
    val quality: String = "1080p",
    val url: String,
    val isDirect: Boolean = true
)

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String,
    val format: String = "vtt"
)

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val year: Int,
    val rating: Float,
    val quality: String = "1080p FHD",
    val duration: String = "115 دقيقة",
    val genre: String = "سينما",
    val description: String = "",
    val streamUrl: String = "",
    val originalTitle: String? = null,
    val logoUrl: String? = null,
    val imdbRatingFormatted: String = "${rating}/10",
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val category: String = genre,
    val genresList: List<String> = emptyList(),
    val director: String = "",
    val cast: List<String> = emptyList(),
    val castMembers: List<CastMember> = emptyList(),
    val country: String = "",
    val language: String = "مترجم بالعربية (FHD)",
    val trailerYoutubeId: String? = null,
    val isFavorite: Boolean = false,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val servers: List<StreamServer> = emptyList(),
    val isSeries: Boolean = false
)

data class Series(
    val id: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val year: Int,
    val rating: Float,
    val genre: String,
    val description: String = "",
    val originalTitle: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val isFavorite: Boolean = false,
    val seasons: List<Season> = emptyList(),
    val trailerYoutubeId: String? = null
)

data class Season(
    val seasonNumber: Int,
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val id: String,
    val episodeNumber: Int,
    val title: String,
    val durationMinutes: Int = 45,
    val thumbnailUrl: String = "",
    val progressPercent: Float = 0f,
    val streamUrl: String = "",
    val seriesId: String = "",
    val seasonNumber: Int = 1,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val servers: List<StreamServer> = emptyList()
)

enum class MatchStatus { LIVE, UPCOMING, FINISHED }

data class PlayerInfo(
    val id: String,
    val name: String,
    val jerseyNumber: String = "",
    val position: String = "لاعب",
    val photoUrl: String = "",
    val nationality: String = ""
)

data class LeagueInfo(
    val id: String,
    val name: String,
    val logoUrl: String,
    val country: String = "",
    val isUcl: Boolean = false
)

data class Match(
    val id: String,
    val competition: String,
    val homeTeam: String,
    val homeTeamLogoUrl: String,
    val awayTeam: String,
    val awayTeamLogoUrl: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: MatchStatus,
    val startTimeLabel: String,
    val sport: String = "Football",
    val channel: String? = null,
    val commentator: String? = null,
    val roundOrStadium: String? = null,
    val roundNumber: String? = null,
    val stadium: String? = null,
    val streamUrl: String? = null,
    val dateCategory: String = "today",
    val kickoffDateFormatted: String? = null,
    val isUcl: Boolean = false,
    val competitionLogoUrl: String? = null,
    val gameMinute: String? = null
)

data class ClubInfo(
    val id: String,
    val name: String,
    val logoUrl: String,
    val rank: Int = 0,
    val played: Int = 0,
    val points: Int = 0,
    val won: Int = 0,
    val draw: Int = 0,
    val lost: Int = 0,
    val goalDiff: Int = 0,
    val leagueId: String = "",
    val leagueName: String = "",
    val stadiumName: String = "",
    val notificationsEnabled: Boolean = true,
    val players: List<PlayerInfo> = emptyList()
)

data class LeagueStanding(
    val leagueId: String,
    val leagueName: String,
    val season: String = "2025/2026",
    val rows: List<StandingRow>
)

data class StandingRow(
    val rank: Int,
    val teamName: String,
    val teamLogoUrl: String,
    val played: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val goalDiff: Int,
    val points: Int,
    val form: List<String> = emptyList() // "W", "D", "L"
)

data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val progressPercent: Float,
    val remainingLabel: String,
    val streamUrl: String
)

data class HeroContent(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val backdropUrl: String,
    val streamUrl: String
)

enum class ContentCategory(val label: String) {
    ALL("All"),
    SPORTS("Sports"),
    NEWS("News"),
    ENTERTAINMENT("Entertainment"),
    MOVIES("Movies"),
    KIDS("Kids"),
    DOCUMENTARY("Documentary"),
    MUSIC("Music"),
    ARABIC("Arabic"),
    INTERNATIONAL("International")
}

data class MediaCategory(
    val name: String,
    val slug: String,
    val isSeries: Boolean = false
)

data class MediaSource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val description: String = "",
    val isCustom: Boolean = false,
    val isDefault: Boolean = false,
    val speedLabel: String = "سريع جداً"
)
