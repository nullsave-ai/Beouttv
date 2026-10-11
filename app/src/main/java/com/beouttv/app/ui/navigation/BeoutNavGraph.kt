package com.beouttv.app.ui.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beouttv.app.data.config.AppGatewayManager
import com.beouttv.app.data.config.GatewayStatus
import com.beouttv.app.data.config.PlayerPreferences
import com.beouttv.app.ui.components.BeoutBottomBar
import com.beouttv.app.ui.theme.BeoutColors
import com.beouttv.app.ui.screens.control.MaintenanceOverlay
import com.beouttv.app.ui.screens.favorites.FavoritesScreen
import com.beouttv.app.ui.screens.home.HomeScreen
import com.beouttv.app.ui.screens.live.LiveTvScreen
import com.beouttv.app.ui.screens.movies.MovieDetailsScreen
import com.beouttv.app.ui.screens.movies.MoviesScreen
import com.beouttv.app.ui.screens.movies.MoviesViewModel
import com.beouttv.app.ui.screens.player.*
import com.beouttv.app.ui.screens.search.SearchScreen
import com.beouttv.app.ui.screens.series.SeriesDetailsScreen
import com.beouttv.app.ui.screens.series.SeriesListScreen
import com.beouttv.app.ui.screens.settings.MoreScreen
import com.beouttv.app.ui.screens.settings.SettingsScreen
import com.beouttv.app.ui.screens.sports.SportsScreen
import com.beouttv.app.ui.screens.notifications.NotificationsScreen
import com.beouttv.app.ui.screens.profile.AccountProfileScreen
import com.beouttv.app.data.ads.AdsterraManager
import com.beouttv.app.ui.components.FloatingSocialBarCapsule
import com.beouttv.app.core.tv.DeviceTypeDetector
import com.beouttv.app.ui.components.tv.BeoutTvNavigationRail

@Composable
fun BeoutNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val config by AppGatewayManager.configFlow.collectAsState()

    if (config.status != GatewayStatus.NORMAL) {
        MaintenanceOverlay(config = config)
        return
    }

    fun openPlayer(
        streamUrl: String,
        title: String,
        mediaId: String = streamUrl,
        posterUrl: String = "",
        backdropUrl: String = "",
        seekPosition: Long = -1L,
        subtitleUrl: String? = null,
        subtitleLabel: String? = null,
        subtitles: List<com.beouttv.app.data.model.SubtitleTrack> = emptyList(),
        servers: List<com.beouttv.app.data.model.StreamServer> = emptyList(),
        userAgent: String? = null,
        referer: String? = null,
        origin: String? = null,
        cookie: String? = null,
        drmKeyId: String? = null,
        drmKey: String? = null,
        drmLicenseUrl: String? = null,
        groupId: String? = null
    ) {
        val isOstora = !groupId.isNullOrBlank() ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isOstoraUrl(streamUrl) ||
                mediaId.startsWith("ostora") ||
                streamUrl.startsWith("412<F>") ||
                streamUrl.startsWith("413<F>") ||
                streamUrl.contains("<F>", ignoreCase = true) ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(mediaId) ||
                com.beouttv.app.data.source.stream.OstoraApiResolver.isKnownSportsChannel(title)

        val isLiveStream = isOstora ||
                mediaId.startsWith("ch_") ||
                mediaId.startsWith("live_") ||
                mediaId.startsWith("ostora") ||
                streamUrl.contains("/live/") ||
                streamUrl.contains("50fps") ||
                streamUrl.endsWith(".m3u8")

        val effectiveAgent = if (!userAgent.isNullOrBlank() && !userAgent.contains("ExoPlayer", true)) {
            userAgent
        } else if (isOstora) {
            com.beouttv.app.data.source.stream.OstoraApiResolver.DEFAULT_USER_AGENT
        } else null

        val streamLower = streamUrl.lowercase()
        val effectiveReferer = when {
            !referer.isNullOrBlank() -> referer
            streamLower.contains("edgenextcdn.net") || streamLower.contains("shahid") -> "https://shahid.mbc.net/"
            streamLower.contains("catiacast.video") || streamLower.contains("imn.iq") -> "https://imn.iq/"
            streamLower.contains("alrabiaa") -> "https://www.alrabiaa.tv/"
            isOstora -> com.beouttv.app.data.source.stream.OstoraApiResolver.DEFAULT_REFERER
            else -> null
        }

        val defaultServerKey = PlayerPreferences.getDefaultServer(context)
        val preferredServer = if (servers.isNotEmpty()) servers.firstOrNull { it.id == defaultServerKey } else null
        val effectiveStream = preferredServer?.url?.takeIf { it.isNotBlank() } ?: streamUrl

        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(EXTRA_STREAM_URL, effectiveStream)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_POSTER_URL, posterUrl)
            putExtra(EXTRA_BACKDROP_URL, backdropUrl.ifBlank { posterUrl })
            putExtra(EXTRA_IS_LIVE, isLiveStream)
            if (seekPosition > 0L) {
                putExtra(EXTRA_SEEK_POSITION, seekPosition)
            }
            if (!subtitleUrl.isNullOrBlank()) putExtra(EXTRA_SUBTITLE_URL, subtitleUrl)
            if (!subtitleLabel.isNullOrBlank()) putExtra(EXTRA_SUBTITLE_LABEL, subtitleLabel)
            
            // Serialize full subtitles list
            if (subtitles.isNotEmpty()) {
                val arr = org.json.JSONArray()
                subtitles.forEach { sub ->
                    val obj = org.json.JSONObject().apply {
                        put("id", sub.id)
                        put("language", sub.language)
                        put("label", sub.label)
                        put("url", sub.url)
                        put("format", sub.format)
                    }
                    arr.put(obj)
                }
                putExtra(EXTRA_SUBTITLES_JSON, arr.toString())
            }

            // Serialize servers list
            if (servers.isNotEmpty()) {
                val srvArr = org.json.JSONArray()
                servers.forEach { srv ->
                    val obj = org.json.JSONObject().apply {
                        put("id", srv.id)
                        put("name", srv.name)
                        put("quality", srv.quality)
                        put("url", srv.url)
                        put("isDirect", srv.isDirect)
                    }
                    srvArr.put(obj)
                }
                putExtra(EXTRA_SERVERS_JSON, srvArr.toString())
            }

            if (!groupId.isNullOrBlank()) putExtra(EXTRA_GROUP_ID, groupId)
            if (!effectiveAgent.isNullOrBlank()) putExtra(EXTRA_USER_AGENT, effectiveAgent)
            if (!effectiveReferer.isNullOrBlank()) putExtra(EXTRA_REFERER, effectiveReferer)
            if (!origin.isNullOrBlank()) putExtra(EXTRA_ORIGIN, origin)
            if (!cookie.isNullOrBlank()) putExtra(EXTRA_COOKIE, cookie)
            if (!drmKeyId.isNullOrBlank()) putExtra(EXTRA_DRM_KEY_ID, drmKeyId)
            if (!drmKey.isNullOrBlank()) putExtra(EXTRA_DRM_KEY, drmKey)
            if (!drmLicenseUrl.isNullOrBlank()) putExtra(EXTRA_DRM_LICENSE_URL, drmLicenseUrl)
        }
        context.startActivity(intent)
    }

    fun openChannelPlayer(channel: com.beouttv.app.data.model.Channel) {
        openPlayer(
            streamUrl = channel.streamUrl,
            title = channel.name,
            mediaId = channel.id,
            posterUrl = channel.logoUrl,
            userAgent = channel.userAgent,
            referer = channel.referer,
            origin = channel.origin,
            cookie = channel.cookie,
            drmKeyId = channel.drmKeyId,
            drmKey = channel.drmKey,
            drmLicenseUrl = channel.drmLicenseUrl,
            groupId = channel.groupId,
            servers = channel.servers
        )
    }

    fun safeNavigate(route: String) {
        val isDetail = route.startsWith("series_details") || route.startsWith("movie_details")
        if (isDetail) {
            AdsterraManager.onMediaDetailsOpened(context)
        }
        navController.navigate(route) {
            launchSingleTop = !isDetail
        }
    }

    val showSocialBar by AdsterraManager.showSocialBar.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "root_tabs",
            modifier = Modifier
                .fillMaxSize()
                .background(BeoutColors.Background),
            enterTransition = { BeoutNavTransitions.enter(this) },
            exitTransition = { BeoutNavTransitions.exit(this) },
            popEnterTransition = { BeoutNavTransitions.popEnter(this) },
            popExitTransition = { BeoutNavTransitions.popExit(this) }
        ) {
        composable("root_tabs") {
            RootTabsContainer(
                onNavigate = ::safeNavigate,
                onOpenChannelPlayer = ::openChannelPlayer,
                onOpenMoviePlayer = { movie ->
                    val stream = if (movie.streamUrl.isNotBlank()) movie.streamUrl else movie.servers.firstOrNull()?.url.orEmpty()
                    openPlayer(
                        streamUrl = stream,
                        title = movie.title,
                        mediaId = movie.id,
                        posterUrl = movie.posterUrl,
                        backdropUrl = movie.backdropUrl.ifBlank { movie.posterUrl },
                        subtitleUrl = movie.subtitles.firstOrNull()?.url,
                        subtitleLabel = movie.subtitles.firstOrNull()?.label,
                        subtitles = movie.subtitles,
                        servers = movie.servers
                    )
                },
                onOpenPlayer = { url, name, id, logo, pos, ua, ref, orig, cook, dId, dKey, dLic ->
                    openPlayer(
                        streamUrl = url,
                        title = name,
                        mediaId = id,
                        posterUrl = logo,
                        seekPosition = pos,
                        userAgent = ua,
                        referer = ref,
                        origin = orig,
                        cookie = cook,
                        drmKeyId = dId,
                        drmKey = dKey,
                        drmLicenseUrl = dLic
                    )
                }
            )
        }

        composable(BeoutDestination.Account.route) {
            AccountProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(BeoutDestination.Movies.route) {
            MoviesScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) }
            )
        }

        composable(BeoutDestination.Series.route) {
            SeriesListScreen(
                onBack = { navController.popBackStack() },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) }
            )
        }

        composable(BeoutDestination.Favorites.route) {
            FavoritesScreen(
                onExploreClick = { navController.popBackStack() },
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) },
                onChannelClick = { openChannelPlayer(it) }
            )
        }

        composable(BeoutDestination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(BeoutDestination.Search.route) {
            SearchScreen(
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) },
                onChannelClick = ::openChannelPlayer,
                onMatchClick = { match ->
                    val stream = match.streamUrl ?: "https://govid.live/prem-217481.m3u8"
                    openPlayer(
                        streamUrl = stream,
                        title = "${match.homeTeam} vs ${match.awayTeam} (${match.competition})",
                        mediaId = match.id,
                        posterUrl = match.homeTeamLogoUrl
                    )
                }
            )
        }

        composable(BeoutDestination.Notifications.route) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(BeoutDestination.AnimeCatalog.route) {
            com.beouttv.app.ui.screens.movies.MovieBoxCatalogScreen(
                catalogType = com.beouttv.app.ui.screens.movies.CatalogType.ANIME,
                onBack = { navController.popBackStack() },
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) }
            )
        }

        composable(BeoutDestination.TrendingCatalog.route) {
            com.beouttv.app.ui.screens.movies.MovieBoxCatalogScreen(
                catalogType = com.beouttv.app.ui.screens.movies.CatalogType.TRENDING,
                onBack = { navController.popBackStack() },
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) }
            )
        }

        composable(BeoutDestination.MovieBoxMoviesCatalog.route) {
            com.beouttv.app.ui.screens.movies.MovieBoxCatalogScreen(
                catalogType = com.beouttv.app.ui.screens.movies.CatalogType.ALL_MOVIES,
                onBack = { navController.popBackStack() },
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) }
            )
        }

        composable(BeoutDestination.MovieBoxSeriesCatalog.route) {
            com.beouttv.app.ui.screens.movies.MovieBoxCatalogScreen(
                catalogType = com.beouttv.app.ui.screens.movies.CatalogType.ALL_SERIES,
                onBack = { navController.popBackStack() },
                onMovieClick = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                onSeriesClick = { safeNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) }
            )
        }

        composable(BeoutDestination.Downloads.route) {
            com.beouttv.app.ui.screens.downloads.DownloadsScreen(
                onBack = { navController.popBackStack() },
                onPlayOffline = { localPath, title, subPath ->
                    openPlayer(
                        streamUrl = localPath,
                        title = title,
                        subtitleUrl = subPath
                    )
                }
            )
        }

        composable(BeoutDestination.SeriesDetails.route) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            val seriesId = try { java.net.URLDecoder.decode(rawId, "UTF-8") } catch (_: Exception) { rawId }
            SeriesDetailsScreen(
                seriesId = seriesId,
                onBack = { navController.popBackStack() },
                onEpisodeClick = { ep ->
                    val stream = if (ep.streamUrl.isNotBlank()) ep.streamUrl else ep.servers.firstOrNull()?.url.orEmpty()
                    val sortedSubs = ep.subtitles.sortedWith(
                        compareByDescending<com.beouttv.app.data.model.SubtitleTrack> { it.language == "ar" && (it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox")) }
                            .thenByDescending { it.language == "ar" }
                            .thenByDescending { it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox") }
                    )
                    val defaultSub = sortedSubs.firstOrNull { it.language == "ar" } ?: sortedSubs.firstOrNull()
                    openPlayer(
                        streamUrl = stream,
                        title = ep.title,
                        mediaId = ep.id,
                        posterUrl = ep.thumbnailUrl,
                        subtitleUrl = defaultSub?.url,
                        subtitleLabel = defaultSub?.label,
                        subtitles = sortedSubs,
                        servers = ep.servers
                    )
                }
            )
        }

        composable(BeoutDestination.MovieDetails.route) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("movieId") ?: return@composable
            val movieId = try { java.net.URLDecoder.decode(rawId, "UTF-8") } catch (_: Exception) { rawId }
            MovieDetailsScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
                onPlayClick = { movie ->
                    val stream = if (movie.streamUrl.isNotBlank()) movie.streamUrl else movie.servers.firstOrNull()?.url.orEmpty()
                    val sortedSubs = movie.subtitles.sortedWith(
                        compareByDescending<com.beouttv.app.data.model.SubtitleTrack> { it.language == "ar" && (it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox")) }
                            .thenByDescending { it.language == "ar" }
                            .thenByDescending { it.label.contains("ONE+") || it.label.contains("وان+") || it.label.contains("MovieBox") }
                    )
                    val defaultSub = sortedSubs.firstOrNull { it.language == "ar" } ?: sortedSubs.firstOrNull()
                    openPlayer(
                        streamUrl = stream,
                        title = movie.title,
                        mediaId = movie.id,
                        posterUrl = movie.posterUrl,
                        backdropUrl = movie.backdropUrl,
                        subtitleUrl = defaultSub?.url,
                        subtitleLabel = defaultSub?.label,
                        subtitles = sortedSubs,
                        servers = movie.servers
                    )
                },
                onNavigateToMovie = { safeNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) }
            )
        }
    }

        AnimatedVisibility(
            visible = showSocialBar,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp)
        ) {
            FloatingSocialBarCapsule(
                onDismiss = { AdsterraManager.dismissSocialBar() }
            )
        }
    }
}

@Composable
private fun RootTabsContainer(
    onNavigate: (String) -> Unit,
    onOpenChannelPlayer: (com.beouttv.app.data.model.Channel) -> Unit,
    onOpenMoviePlayer: (com.beouttv.app.data.model.Movie) -> Unit,
    onOpenPlayer: (String, String, String, String, Long, String?, String?, String?, String?, String?, String?, String?) -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(BeoutDestination.Home.route) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val context = LocalContext.current
    val isTvMode by DeviceTypeDetector.isTvModeFlow.collectAsState()
    val railFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    var isRailFocused by remember { mutableStateOf(false) }

    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }

    // Android Hardware / Remote Back Button handling:
    // If in TV mode and focus is in content, Back smoothly returns focus to the navigation rail.
    // If on the navigation rail (or phone mode), Back switches to Home tab.
    BackHandler(enabled = isTvMode && !isRailFocused && !drawerState.isOpen) {
        railFocusRequester.requestFocus()
    }
    BackHandler(enabled = (!isTvMode || isRailFocused) && !drawerState.isOpen && activeTab != BeoutDestination.Home.route) {
        activeTab = BeoutDestination.Home.route
    }

    val currentDestination = bottomNavDestinations.find { it.route == activeTab } ?: BeoutDestination.Home

    val contentBlock = @Composable {
        // High-Performance iOS Direction-Aware Dynamic Content Switcher
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                val initialIndex = bottomNavDestinations.indexOfFirst { it.route == initialState }.coerceAtLeast(0)
                val targetIndex = bottomNavDestinations.indexOfFirst { it.route == targetState }.coerceAtLeast(0)
                val isMovingForward = targetIndex >= initialIndex

                val direction = if (isMovingForward) {
                    AnimatedContentTransitionScope.SlideDirection.Start
                } else {
                    AnimatedContentTransitionScope.SlideDirection.End
                }

                val tabSlideSpec = tween<androidx.compose.ui.unit.IntOffset>(
                    durationMillis = 240,
                    easing = BeoutNavTransitions.IOSEasing
                )
                val fadeSpec = tween<Float>(
                    durationMillis = 180,
                    easing = LinearEasing
                )

                (slideIntoContainer(towards = direction, animationSpec = tabSlideSpec) +
                 fadeIn(animationSpec = fadeSpec))
                    .togetherWith(
                        slideOutOfContainer(towards = direction, animationSpec = tabSlideSpec) +
                        fadeOut(animationSpec = fadeSpec)
                    )
            },
            label = "RootTabsContent"
        ) { targetRoute ->
            saveableStateHolder.SaveableStateProvider(key = targetRoute) {
                when (targetRoute) {
                    BeoutDestination.Home.route -> {
                        HomeScreen(
                            onProfileClick = { onNavigate(BeoutDestination.Account.route) },
                            onMenuClick = { drawerScope.launch { drawerState.open() } },
                            onNotificationsClick = { onNavigate(BeoutDestination.Notifications.route) },
                            onSearchClick = { onNavigate(BeoutDestination.Search.route) },
                            onChannelClick = onOpenChannelPlayer,
                            onMovieClick = { onNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                            onPlayMovieDirect = onOpenMoviePlayer,
                            onContinueWatchingClick = { onOpenPlayer(it.streamUrl, it.title, it.id, it.thumbnailUrl, -1L, null, null, null, null, null, null, null) },
                            onSeriesClick = { onNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) },
                            onSeeAllChannels = { activeTab = BeoutDestination.LiveTv.route },
                            onSeeAllMovies = {
                                MoviesViewModel.initialCategorySlug = "all"
                                onNavigate(BeoutDestination.Movies.route)
                            },
                            onSeeAllAsianMovies = { slug ->
                                MoviesViewModel.initialCategorySlug = slug
                                com.beouttv.app.data.xtream.XtreamAccountManager.initialMoviesSourceFilter = 0
                                onNavigate(BeoutDestination.Movies.route)
                            },
                            onSeeAllSeries = { onNavigate(BeoutDestination.Series.route) },
                            onSeeAllTrending = { onNavigate(BeoutDestination.TrendingCatalog.route) },
                            onSeeAllAnime = { onNavigate(BeoutDestination.AnimeCatalog.route) }
                        )
                    }

                    BeoutDestination.LiveTv.route -> {
                        LiveTvScreen(
                            onChannelClick = onOpenChannelPlayer
                        )
                    }

                    BeoutDestination.Sports.route -> {
                        SportsScreen(
                            onWatchMatch = { match ->
                                val stream = match.streamUrl ?: "https://govid.live/prem-217481.m3u8"
                                onOpenPlayer(
                                    stream,
                                    "${match.homeTeam} vs ${match.awayTeam} (${match.competition})",
                                    match.id,
                                    match.homeTeamLogoUrl,
                                    -1L, null, null, null, null, null, null, null
                                )
                            },
                            onChannelClick = onOpenChannelPlayer
                        )
                    }

                    BeoutDestination.Search.route -> {
                        SearchScreen(
                            onMovieClick = { onNavigate(BeoutDestination.MovieDetails.createRoute(it.id)) },
                            onSeriesClick = { onNavigate(BeoutDestination.SeriesDetails.createRoute(it.id)) },
                            onChannelClick = onOpenChannelPlayer,
                            onMatchClick = { match ->
                                val stream = match.streamUrl ?: "https://govid.live/prem-217481.m3u8"
                                onOpenPlayer(
                                    stream,
                                    "${match.homeTeam} vs ${match.awayTeam} (${match.competition})",
                                    match.id,
                                    match.homeTeamLogoUrl,
                                    -1L, null, null, null, null, null, null, null
                                )
                            }
                        )
                    }

                    BeoutDestination.More.route -> {
                        MoreScreen(
                            onFavoritesClick = { onNavigate(BeoutDestination.Favorites.route) },
                            onSettingsClick = { onNavigate(BeoutDestination.Settings.route) },
                            onAccountClick = { onNavigate(BeoutDestination.Account.route) },
                            onMoviesClick = { onNavigate(BeoutDestination.Movies.route) },
                            onSeriesClick = { onNavigate(BeoutDestination.Series.route) }
                        )
                    }

                    else -> {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Side navigation is strictly for TV devices or real Tablets (sw600dp+).
    val useSideNavigation = isTvMode || (isTablet && isLandscape) || (isTablet && configuration.screenWidthDp >= 720)

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isTvMode,
        drawerContent = {
            com.beouttv.app.ui.components.BeoutNavigationDrawerContent(
                currentRoute = activeTab,
                onNavigate = { targetRoute ->
                    drawerScope.launch { drawerState.close() }
                    when (targetRoute) {
                        BeoutDestination.Home.route,
                        BeoutDestination.LiveTv.route,
                        BeoutDestination.Sports.route,
                        BeoutDestination.More.route -> {
                            activeTab = targetRoute
                        }
                        else -> {
                            onNavigate(targetRoute)
                        }
                    }
                },
                onCloseDrawer = {
                    drawerScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        if (useSideNavigation) {
            val layoutDirection = LocalLayoutDirection.current
            val isRtl = layoutDirection == LayoutDirection.Rtl

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BeoutColors.Background)
            ) {
                // Full Canvas: Content scrolls and flows seamlessly underneath or beside the rail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(contentFocusRequester)
                        .focusProperties {
                            if (isRtl) {
                                right = railFocusRequester
                            } else {
                                left = railFocusRequester
                            }
                        }
                ) {
                    contentBlock()
                }

                // Adaptive Floating Navigation Rail for Tablets, Foldables & Smart Screens
                BeoutTvNavigationRail(
                    currentDestination = currentDestination,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .zIndex(20f),
                    selectedTabFocusRequester = railFocusRequester,
                    onMoveToContent = {
                        contentFocusRequester.requestFocus()
                    },
                    onFocusChanged = { hasFocus ->
                        isRailFocused = hasFocus
                    },
                    onNavigateToDestination = { destination ->
                        if (activeTab != destination.route) {
                            activeTab = destination.route
                        }
                    }
                )
            }
        } else {
            val showBottomBar = true

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BeoutColors.Background)
            ) {
                contentBlock()

                // Floating Matte Obsidian Bottom Navigation Bar (Smoothly hides in TikTok-style clean screen mode)
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BeoutBottomBar(
                        currentDestination = currentDestination,
                    ) { destination ->
                        if (activeTab != destination.route) {
                            activeTab = destination.route
                        }
                    }
                }
            }
        }
    }
}
