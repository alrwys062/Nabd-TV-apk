package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.ContentType
import com.example.ui.components.MainTab
import com.example.ui.components.NabdFloatingBottomBar
import com.example.ui.components.NabdNavigationRail
import com.example.ui.navigation.NabdRoute
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.EpgScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LiveChannelsScreen
import com.example.ui.screens.MovieDetailScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.ProPlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeriesDetailScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WelcomeLanguageScreen
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdTvTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NabdApplication

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(app))
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            NabdTvTheme(languageCode = settings.language) {
                NabdAppRoot(
                    app = app,
                    mainViewModel = mainViewModel,
                    isInPipMode = isInPipModeState.value
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val app = application as NabdApplication
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.R &&
            app.playerManager.isPlaying.value
        ) {
            runCatching {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            }
        }
    }
}

@Composable
private fun NabdAppRoot(
    app: NabdApplication,
    mainViewModel: MainViewModel,
    isInPipMode: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteString = navBackStackEntry?.destination?.route.orEmpty()

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val isOnline by mainViewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by mainViewModel.isSyncing.collectAsStateWithLifecycle()
    val categories by mainViewModel.categories.collectAsStateWithLifecycle()
    val countries by mainViewModel.countries.collectAsStateWithLifecycle()
    val languages by mainViewModel.languages.collectAsStateWithLifecycle()
    val selectedHomeCat by mainViewModel.selectedHomeCategory.collectAsStateWithLifecycle()
    val featuredChannels by mainViewModel.featuredChannels.collectAsStateWithLifecycle()
    val popularChannels by mainViewModel.popularChannels.collectAsStateWithLifecycle()
    val recentChannels by mainViewModel.recentChannels.collectAsStateWithLifecycle()
    val filteredLiveChannels by mainViewModel.filteredLiveChannels.collectAsStateWithLifecycle()
    val visibleChannels by mainViewModel.visibleChannels.collectAsStateWithLifecycle()
    val liveFilter by mainViewModel.liveFilter.collectAsStateWithLifecycle()
    val healthProgress by mainViewModel.healthCheckProgress.collectAsStateWithLifecycle()
    val movies by mainViewModel.allMovies.collectAsStateWithLifecycle()
    val series by mainViewModel.allSeries.collectAsStateWithLifecycle()
    val favChannels by mainViewModel.favoriteChannels.collectAsStateWithLifecycle()
    val favMovies by mainViewModel.favoriteMovies.collectAsStateWithLifecycle()
    val favSeries by mainViewModel.favoriteSeries.collectAsStateWithLifecycle()
    val watchHistory by mainViewModel.watchHistory.collectAsStateWithLifecycle()
    val downloads by mainViewModel.downloads.collectAsStateWithLifecycle()
    val epgPrograms by mainViewModel.epgPrograms.collectAsStateWithLifecycle()
    val searchHistory by mainViewModel.searchHistory.collectAsStateWithLifecycle()

    val activeMainTab = remember(currentRouteString) {
        when {
            currentRouteString.contains("Home") -> MainTab.HOME
            currentRouteString.contains("Live") -> MainTab.LIVE
            currentRouteString.contains("Movies") && !currentRouteString.contains("Detail") -> MainTab.MOVIES
            currentRouteString.contains("Series") && !currentRouteString.contains("Detail") -> MainTab.SERIES
            currentRouteString.contains("Settings") -> MainTab.SETTINGS
            else -> null
        }
    }

    val navigateToTab: (MainTab) -> Unit = { tab ->
        val dest: NabdRoute = when (tab) {
            MainTab.HOME -> NabdRoute.Home
            MainTab.LIVE -> NabdRoute.Live
            MainTab.MOVIES -> NabdRoute.Movies
            MainTab.SERIES -> NabdRoute.Series
            MainTab.SETTINGS -> NabdRoute.Settings
        }
        navController.navigate(dest) {
            popUpTo(NabdRoute.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail && activeMainTab != null && !isInPipMode) {
                NabdNavigationRail(
                    currentTab = activeMainTab,
                    onTabSelected = navigateToTab
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = NabdRoute.Splash,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<NabdRoute.Splash> {
                        SplashScreen(
                            isFirstLaunch = settings.isFirstLaunch,
                            onSplashFinished = { goToWelcome ->
                                val next: NabdRoute = if (goToWelcome) NabdRoute.Welcome else NabdRoute.Home
                                navController.navigate(next) {
                                    popUpTo(NabdRoute.Splash) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<NabdRoute.Welcome> {
                        WelcomeLanguageScreen(
                            settings = settings,
                            onLanguagePicked = { mainViewModel.setLanguage(it) },
                            onContinue = { chosenLang ->
                                mainViewModel.completeFirstLaunch(chosenLang)
                                navController.navigate(NabdRoute.Home) {
                                    popUpTo(NabdRoute.Welcome) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<NabdRoute.Home> {
                        HomeScreen(
                            settings = settings,
                            isOnline = isOnline,
                            isSyncing = isSyncing,
                            categories = categories,
                            selectedCategory = selectedHomeCat,
                            featuredChannels = featuredChannels,
                            popularChannels = popularChannels,
                            recentChannels = recentChannels,
                            movies = movies,
                            series = series,
                            watchHistory = watchHistory,
                            onSelectCategory = { mainViewModel.selectHomeCategory(it) },
                            onSearchClick = { navController.navigate(NabdRoute.Search) },
                            onRefreshClick = { mainViewModel.refreshPlaylists(force = true) },
                            onPlayChannel = { ch ->
                                navController.navigate(NabdRoute.Player("CHANNEL", ch.id))
                            },
                            onFavoriteChannel = { mainViewModel.toggleChannelFavorite(it) },
                            onMovieClick = { mv -> navController.navigate(NabdRoute.MovieDetail(mv.id)) },
                            onSeriesClick = { sr -> navController.navigate(NabdRoute.SeriesDetail(sr.id)) },
                            onResumeHistory = { item ->
                                when (item.contentType) {
                                    ContentType.CHANNEL -> navController.navigate(NabdRoute.Player("CHANNEL", item.contentId))
                                    ContentType.MOVIE -> navController.navigate(NabdRoute.Player("MOVIE", item.contentId))
                                    ContentType.EPISODE -> navController.navigate(NabdRoute.Player("EPISODE", item.contentId, item.seriesId))
                                }
                            },
                            onSeeAllLive = { navigateToTab(MainTab.LIVE) },
                            onSeeAllMovies = { navigateToTab(MainTab.MOVIES) },
                            onSeeAllSeries = { navigateToTab(MainTab.SERIES) },
                            onOpenLibraryTab = { tab -> navController.navigate(NabdRoute.Library(tab)) },
                            onOpenEpg = { navController.navigate(NabdRoute.Epg) },
                            onOpenAdmin = { navController.navigate(NabdRoute.Admin) },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }

                    composable<NabdRoute.Live> {
                        LiveChannelsScreen(
                            channels = filteredLiveChannels,
                            categories = categories,
                            countries = countries,
                            languages = languages,
                            filter = liveFilter,
                            isArabic = settings.language == "ar",
                            isSyncing = isSyncing,
                            healthCheckProgress = healthProgress,
                            onFilterChange = { mainViewModel.updateLiveFilter(it) },
                            onToggleHideDead = { mainViewModel.setHideDeadChannels(it) },
                            onRunHealthCheck = { mainViewModel.runChannelHealthCheck() },
                            onRefresh = { mainViewModel.refreshPlaylists(force = true) },
                            onPlayChannel = { ch -> navController.navigate(NabdRoute.Player("CHANNEL", ch.id)) },
                            onFavoriteChannel = { mainViewModel.toggleChannelFavorite(it) },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }

                    composable<NabdRoute.Movies> {
                        MoviesScreen(
                            movies = movies,
                            onMovieClick = { mv -> navController.navigate(NabdRoute.MovieDetail(mv.id)) },
                            onOpenAdmin = { navController.navigate(NabdRoute.Admin) },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }

                    composable<NabdRoute.Series> {
                        SeriesScreen(
                            series = series,
                            onSeriesClick = { sr -> navController.navigate(NabdRoute.SeriesDetail(sr.id)) },
                            onOpenAdmin = { navController.navigate(NabdRoute.Admin) },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }

                    composable<NabdRoute.Settings> {
                        SettingsScreen(
                            settings = settings,
                            availableCategories = categories,
                            onLanguageChange = { mainViewModel.setLanguage(it) },
                            onQualityChange = { mainViewModel.setDefaultQuality(it) },
                            onDataSaverChange = { mainViewModel.setDataSaverMode(it) },
                            onAudioOnlyChange = { mainViewModel.setAudioOnlyDefault(it) },
                            onAutoFullscreenChange = { mainViewModel.setAutoFullscreen(it) },
                            onAutoNextEpisodeChange = { mainViewModel.setAutoNextEpisode(it) },
                            onBackgroundPlaybackChange = { mainViewModel.setBackgroundPlayback(it) },
                            onHideDeadChange = { mainViewModel.setHideDeadChannels(it) },
                            onAutoRefreshChange = { mainViewModel.setAutoRefreshHours(it) },
                            onKidsLockToggle = { en, pin -> mainViewModel.setKidsLock(en, pin) },
                            onToggleHiddenCategory = { mainViewModel.toggleHiddenCategory(it) },
                            onChangeAdminPin = { mainViewModel.setAdminPin(it) },
                            onRefreshNow = { mainViewModel.refreshPlaylists(force = true) },
                            onClearCache = { mainViewModel.clearCache() },
                            onOpenAdminPanel = { navController.navigate(NabdRoute.Admin) },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                        )
                    }

                    composable<NabdRoute.MovieDetail> { entry ->
                        val args = entry.toRoute<NabdRoute.MovieDetail>()
                        val movie = movies.firstOrNull { it.id == args.movieId }
                        if (movie != null) {
                            val dl = downloads.firstOrNull { it.contentId == movie.id }
                            MovieDetailScreen(
                                movie = movie,
                                downloadState = dl,
                                onBack = { navController.popBackStack() },
                                onPlay = { navController.navigate(NabdRoute.Player("MOVIE", movie.id)) },
                                onToggleFavorite = { mainViewModel.toggleMovieFavorite(movie) },
                                onDownload = { mainViewModel.downloadMovie(movie) }
                            )
                        }
                    }

                    composable<NabdRoute.SeriesDetail> { entry ->
                        val args = entry.toRoute<NabdRoute.SeriesDetail>()
                        val item = series.firstOrNull { it.id == args.seriesId }
                        if (item != null) {
                            val episodes by mainViewModel.observeEpisodesForSeries(item.id)
                                .collectAsStateWithLifecycle(initialValue = emptyList())
                            SeriesDetailScreen(
                                series = item,
                                episodes = episodes,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = { mainViewModel.toggleSeriesFavorite(item) },
                                onPlayEpisode = { ep ->
                                    navController.navigate(NabdRoute.Player("EPISODE", ep.id, item.id))
                                }
                            )
                        }
                    }

                    composable<NabdRoute.Library> { entry ->
                        val args = entry.toRoute<NabdRoute.Library>()
                        LibraryScreen(
                            initialTab = args.initialTab,
                            favoriteChannels = favChannels,
                            favoriteMovies = favMovies,
                            favoriteSeries = favSeries,
                            watchHistory = watchHistory,
                            downloads = downloads,
                            onBack = { navController.popBackStack() },
                            onPlayChannel = { ch -> navController.navigate(NabdRoute.Player("CHANNEL", ch.id)) },
                            onMovieClick = { mv -> navController.navigate(NabdRoute.MovieDetail(mv.id)) },
                            onSeriesClick = { sr -> navController.navigate(NabdRoute.SeriesDetail(sr.id)) },
                            onResumeHistory = { item ->
                                when (item.contentType) {
                                    ContentType.CHANNEL -> navController.navigate(NabdRoute.Player("CHANNEL", item.contentId))
                                    ContentType.MOVIE -> navController.navigate(NabdRoute.Player("MOVIE", item.contentId))
                                    ContentType.EPISODE -> navController.navigate(NabdRoute.Player("EPISODE", item.contentId, item.seriesId))
                                }
                            },
                            onClearHistory = { mainViewModel.clearHistory() },
                            onPlayDownload = { dl -> navController.navigate(NabdRoute.Player("DOWNLOAD", dl.contentId)) },
                            onDeleteDownload = { mainViewModel.removeDownload(it) }
                        )
                    }

                    composable<NabdRoute.Epg> {
                        EpgScreen(
                            programs = epgPrograms,
                            currentEpgUrl = settings.epgUrl,
                            onBack = { navController.popBackStack() },
                            onLoadEpgUrl = { mainViewModel.loadEpg(it) }
                        )
                    }

                    composable<NabdRoute.Search> {
                        SearchScreen(
                            channels = visibleChannels,
                            movies = movies,
                            series = series,
                            searchHistory = searchHistory,
                            onBack = { navController.popBackStack() },
                            onSaveQuery = { mainViewModel.saveSearchQuery(it) },
                            onClearHistory = { mainViewModel.clearSearchHistory() },
                            onPlayChannel = { ch -> navController.navigate(NabdRoute.Player("CHANNEL", ch.id)) },
                            onMovieClick = { mv -> navController.navigate(NabdRoute.MovieDetail(mv.id)) },
                            onSeriesClick = { sr -> navController.navigate(NabdRoute.SeriesDetail(sr.id)) }
                        )
                    }

                    composable<NabdRoute.Admin> {
                        val adminViewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory(app))
                        AdminPanelScreen(
                            viewModel = adminViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<NabdRoute.Player> { entry ->
                        val args = entry.toRoute<NabdRoute.Player>()
                        val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory(app))
                        ProPlayerScreen(
                            contentType = args.contentType,
                            contentId = args.contentId,
                            seriesId = args.seriesId,
                            isInPipMode = isInPipMode,
                            viewModel = playerViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (!useNavRail && activeMainTab != null && !isInPipMode) {
                    NabdFloatingBottomBar(
                        currentTab = activeMainTab,
                        onTabSelected = navigateToTab,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
