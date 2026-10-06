package com.beouttv.app.data.repository

import android.content.Context
import com.beouttv.app.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for content in this scaffold.
 */
interface BeoutRepository {
    val moviesFlow: StateFlow<List<Movie>> get() = MutableStateFlow(emptyList())
    val seriesFlow: StateFlow<List<Series>> get() = MutableStateFlow(emptyList())
    val channelsFlow: StateFlow<List<Channel>> get() = MutableStateFlow(emptyList())
    suspend fun getHero(): HeroContent
    suspend fun getContinueWatching(): List<ContinueWatchingItem>
    suspend fun getChannels(category: String = "All"): List<Channel>
    suspend fun getMovies(): List<Movie>
    suspend fun getSeries(): List<Series>
    suspend fun getMatches(): List<Match>
    suspend fun getMatchesByDate(date: String): List<Match> = getMatches()
    suspend fun getStandings(): List<LeagueStanding> = emptyList()
    suspend fun getStandingsByLeague(leagueId: String, leagueName: String): LeagueStanding? = null
    suspend fun getTeamsForLeague(leagueId: String): List<ClubInfo> = emptyList()
    suspend fun getTeamFixtures(teamId: String, teamName: String): List<Match> = emptyList()
    suspend fun getTeamSquad(teamId: String, teamName: String): List<PlayerInfo> = emptyList()
    suspend fun getSeriesById(id: String): Series?
    suspend fun getMovieById(id: String): Movie?
    suspend fun toggleFavoriteChannel(id: String)
    suspend fun toggleFavoriteMovie(id: String)
    suspend fun toggleFavoriteSeries(id: String)
    suspend fun getCategories(): List<MediaCategory> = emptyList()
    suspend fun getMoviesByCategory(categorySlug: String, page: Int = 1): List<Movie> = emptyList()
    suspend fun getSeriesByCategory(categorySlug: String, page: Int = 1): List<Series> = emptyList()
    suspend fun getMoviesPaged(categorySlug: String = "all", page: Int = 1, perPage: Int = 30): List<Movie> = getMoviesByCategory(categorySlug, page)
    suspend fun getSeriesPaged(categorySlug: String = "all", page: Int = 1, perPage: Int = 30): List<Series> = getSeriesByCategory(categorySlug, page)
    suspend fun reloadCurrentSourceContent() {}
    suspend fun getFavoriteChannels(): List<Channel>
    suspend fun getFavoriteMovies(): List<Movie>
    suspend fun getFavoriteSeries(): List<Series>
    fun cacheDiscoveredSeries(series: Series) {}
    fun cacheDiscoveredMovie(movie: Movie) {}
}

class FakeBeoutRepository : BeoutRepository {

    private val _moviesFlow = MutableStateFlow<List<Movie>>(emptyList())
    override val moviesFlow: StateFlow<List<Movie>> = _moviesFlow

    private val channels = mutableListOf(
        Channel("c1", "BEOUT Sports 1", "https://picsum.photos/seed/c1/200/200", "Sports", currentProgram = "Champions League Live", streamUrl = ""),
        Channel("c2", "BEOUT Sports 2", "https://picsum.photos/seed/c2/200/200", "Sports", currentProgram = "Match Analysis", streamUrl = ""),
        Channel("c3", "BEOUT News", "https://picsum.photos/seed/c3/200/200", "News", currentProgram = "Evening Bulletin", streamUrl = ""),
        Channel("c4", "BEOUT Cinema", "https://picsum.photos/seed/c4/200/200", "Movies", currentProgram = "Prime Time Movie", streamUrl = ""),
        Channel("c5", "BEOUT Kids", "https://picsum.photos/seed/c5/200/200", "Kids", currentProgram = "Cartoon Hour", streamUrl = ""),
        Channel("c6", "BEOUT Drama", "https://picsum.photos/seed/c6/200/200", "Entertainment", currentProgram = "Season Finale", streamUrl = ""),
        Channel("c7", "BEOUT Music", "https://picsum.photos/seed/c7/200/200", "Music", currentProgram = "Top 40 Countdown", streamUrl = ""),
        Channel("c8", "BEOUT World", "https://picsum.photos/seed/c8/200/200", "International", currentProgram = "Global Report", streamUrl = "")
    )

    private val movies = mutableListOf(
        Movie(id = "m1", title = "The Brink of War", posterUrl = "https://wecima.bar/images/1218902dacb906c0ff5e3592b10ea895.png", backdropUrl = "https://wecima.bar/images/1218902dacb906c0ff5e3592b10ea895.png", year = 2026, rating = 7.8f, quality = "1080p FHD", duration = "116 دقيقة", genre = "دراما • حرب", description = "يدور العمل خلال فترة الحرب الباردة في آيسلندا.", streamUrl = ""),
        Movie(id = "m2", title = "Bury the Devil", posterUrl = "https://picsum.photos/seed/m2poster/400/600", backdropUrl = "https://picsum.photos/seed/m2backdrop/800/450", year = 2026, rating = 7.5f, quality = "1080p FHD", duration = "105 دقيقة", genre = "إثارة • غموض", description = "صراع غامض في مدينة ساحلية.", streamUrl = ""),
        Movie(id = "m3", title = "Atlas King", posterUrl = "https://picsum.photos/seed/m3poster/400/600", backdropUrl = "https://picsum.photos/seed/m3backdrop/800/450", year = 2026, rating = 8.6f, quality = "4K Ultra HD", duration = "128 دقيقة", genre = "أكشن • مغامرات", description = "مغامرة ملحمية لإنقاذ العالم.", streamUrl = "")
    )

    private val _channelsFlow = MutableStateFlow<List<Channel>>(emptyList())
    override val channelsFlow: StateFlow<List<Channel>> = _channelsFlow

    init {
        _moviesFlow.value = movies
        _channelsFlow.value = channels
    }

    private val seriesList = mutableListOf(
        Series(
            "s1", "The Last Signal", "https://picsum.photos/seed/s1poster/400/600", "https://picsum.photos/seed/s1backdrop/800/450", 2024, 8.3f, "Sci-Fi",
            "A crew races to decode a signal from beyond.",
            seasons = listOf(
                Season(1, listOf(
                    Episode("s1e1", 1, "Arrival", 42, "https://picsum.photos/seed/s1e1/400/225", 0.6f, ""),
                    Episode("s1e2", 2, "Static", 45, "https://picsum.photos/seed/s1e2/400/225", 0f, "")
                ))
            )
        )
    )

    private val matches = mutableListOf(
        Match("mt1", "Champions League", "Al Rayyan", "https://picsum.photos/seed/mt1home/80/80", "Al Sadd", "https://picsum.photos/seed/mt1away/80/80", 1, 0, MatchStatus.LIVE, "LIVE • 63'", "Football")
    )

    private val continueWatching = listOf(
        ContinueWatchingItem("cw1", "The Last Signal — S1E1", "https://picsum.photos/seed/cw1/400/225", 0.6f, "17 min left", "")
    )

    override suspend fun getHero(): HeroContent {
        delay(150)
        return HeroContent(
            id = "hero1",
            title = "Desert Protocol",
            description = "An elite operative races against time to stop a covert plot unfolding across the desert.",
            category = "Thriller • 2025 • 4K",
            backdropUrl = "https://picsum.photos/seed/heroBanner/1200/675",
            streamUrl = ""
        )
    }

    override suspend fun getContinueWatching(): List<ContinueWatchingItem> {
        delay(150)
        return continueWatching
    }

    override suspend fun getChannels(category: String): List<Channel> {
        delay(200)
        return if (category == "All") channels else channels.filter { it.category.equals(category, true) }
    }

    override suspend fun getMovies(): List<Movie> {
        delay(200)
        return movies
    }

    override suspend fun getSeries(): List<Series> {
        delay(200)
        return seriesList
    }

    override suspend fun getMatches(): List<Match> {
        delay(200)
        return matches
    }

    override suspend fun getSeriesById(id: String): Series? {
        delay(100)
        return seriesList.find { it.id == id }
    }

    override suspend fun getMovieById(id: String): Movie? {
        delay(100)
        return movies.find { it.id == id }
    }

    override suspend fun toggleFavoriteChannel(id: String) {
        val index = channels.indexOfFirst { it.id == id }
        if (index != -1) channels[index] = channels[index].copy(isFavorite = !channels[index].isFavorite)
    }

    override suspend fun toggleFavoriteMovie(id: String) {
        val index = movies.indexOfFirst { it.id == id }
        if (index != -1) movies[index] = movies[index].copy(isFavorite = !movies[index].isFavorite)
    }

    override suspend fun toggleFavoriteSeries(id: String) {
        val index = seriesList.indexOfFirst { it.id == id }
        if (index != -1) seriesList[index] = seriesList[index].copy(isFavorite = !seriesList[index].isFavorite)
    }

    override suspend fun getFavoriteChannels(): List<Channel> = channels.filter { it.isFavorite }
    override suspend fun getFavoriteMovies(): List<Movie> = movies.filter { it.isFavorite }
    override suspend fun getFavoriteSeries(): List<Series> = seriesList.filter { it.isFavorite }
}

object ServiceLocator {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAppContext(): Context? = appContext

    val repository: BeoutRepository by lazy {
        RemoteMediaRepository(context = appContext)
    }

    val mediaRepository: BeoutRepository get() = repository
}
