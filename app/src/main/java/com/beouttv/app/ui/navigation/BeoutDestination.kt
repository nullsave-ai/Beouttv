package com.beouttv.app.ui.navigation

sealed class BeoutDestination(val route: String) {
    data object Home : BeoutDestination("home")
    data object ShortDrama : BeoutDestination("short_drama")
    data object LiveTv : BeoutDestination("live_tv")
    data object Sports : BeoutDestination("sports")
    data object Search : BeoutDestination("search")
    data object More : BeoutDestination("more")

    data object Movies : BeoutDestination("movies")
    data object Series : BeoutDestination("series")
    data object Favorites : BeoutDestination("favorites")
    data object Settings : BeoutDestination("settings")
    data object Account : BeoutDestination("account")
    data object Notifications : BeoutDestination("notifications")
    data object AnimeCatalog : BeoutDestination("anime_catalog")
    data object TrendingCatalog : BeoutDestination("trending_catalog")
    data object MovieBoxMoviesCatalog : BeoutDestination("mb_movies_catalog")
    data object MovieBoxSeriesCatalog : BeoutDestination("mb_series_catalog")
    data object Downloads : BeoutDestination("downloads_screen")

    data object SeriesDetails : BeoutDestination("series_details/{seriesId}") {
        fun createRoute(seriesId: String): String {
            val encoded = try {
                java.net.URLEncoder.encode(seriesId, "UTF-8").replace("+", "%20")
            } catch (_: Exception) { seriesId }
            return "series_details/$encoded"
        }
    }

    data object MovieDetails : BeoutDestination("movie_details/{movieId}") {
        fun createRoute(movieId: String): String {
            val encoded = try {
                java.net.URLEncoder.encode(movieId, "UTF-8").replace("+", "%20")
            } catch (_: Exception) { movieId }
            return "movie_details/$encoded"
        }
    }
}

/** Destinations shown as bottom-nav tabs, in display order. */
val bottomNavDestinations = listOf(
    BeoutDestination.Home,
    BeoutDestination.ShortDrama,
    BeoutDestination.LiveTv,
    BeoutDestination.Sports,
    BeoutDestination.More
)
