package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AppSettings
import com.example.data.model.ChannelEntity
import com.example.data.model.ContentType
import com.example.data.model.MovieEntity
import com.example.data.model.SeriesEntity
import com.example.data.model.ServerSourceEntity
import com.example.data.model.WatchHistoryEntity
import com.example.ui.components.CategoryChipRow
import com.example.ui.components.CategoryPill
import com.example.ui.components.ChannelCard
import com.example.ui.components.DeveloperRightsCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FeaturedBannerCard
import com.example.ui.components.NabdLogoIcon
import com.example.ui.components.SectionHeader
import com.example.ui.components.TelegramFollowSection
import com.example.ui.components.VodPosterCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdRed
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdSurfaceElevated
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet

@Composable
fun HomeScreen(
    settings: AppSettings,
    isOnline: Boolean,
    isSyncing: Boolean,
    syncMessage: String? = null,
    categories: List<String>,
    selectedCategory: String,
    featuredChannels: List<ChannelEntity>,
    popularChannels: List<ChannelEntity>,
    recentChannels: List<ChannelEntity>,
    movies: List<MovieEntity>,
    series: List<SeriesEntity>,
    watchHistory: List<WatchHistoryEntity>,
    servers: List<ServerSourceEntity> = emptyList(),
    onSelectServer: (String) -> Unit = {},
    onSelectCategory: (String) -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onFavoriteChannel: (ChannelEntity) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onResumeHistory: (WatchHistoryEntity) -> Unit,
    onSeeAllLive: () -> Unit,
    onSeeAllMovies: () -> Unit,
    onSeeAllSeries: () -> Unit,
    onOpenLibraryTab: (Int) -> Unit,
    onOpenEpg: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isArabic = settings.language == "ar"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBg),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NabdLogoIcon(size = 36.dp)
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeaderCircleAction(
                        icon = Icons.Default.Favorite,
                        contentDescription = strings.favorites,
                        tint = NabdPink,
                        onClick = { onOpenLibraryTab(0) },
                        testTag = "home_favorites_button"
                    )
                    HeaderCircleAction(
                        icon = Icons.Default.CalendarMonth,
                        contentDescription = strings.epgGuide,
                        tint = NabdCyan,
                        onClick = onOpenEpg,
                        testTag = "home_epg_button"
                    )
                    HeaderCircleAction(
                        icon = Icons.Default.Refresh,
                        contentDescription = strings.refreshPlaylist,
                        tint = Color.White,
                        isLoading = isSyncing,
                        onClick = onRefreshClick,
                        testTag = "home_refresh_button"
                    )
                    HeaderCircleAction(
                        icon = Icons.Default.AdminPanelSettings,
                        contentDescription = strings.controlPanel,
                        tint = NabdViolet,
                        onClick = onOpenAdmin,
                        testTag = "home_admin_button"
                    )
                }
            }
        }

        // Search Bar
        item {
            Surface(
                color = NabdSurface,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, NabdBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onSearchClick)
                    .testTag("home_search_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = strings.searchHint,
                        tint = NabdCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.searchHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NabdTextSecondary
                    )
                }
            }
        }

        // Offline Warning
        if (!isOnline) {
            item {
                Surface(
                    color = NabdRed.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NabdRed.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onOpenLibraryTab(2) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = NabdRed)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.noInternetTitle, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(strings.noInternetSubtitle, style = MaterialTheme.typography.bodySmall, color = NabdTextSecondary)
                        }
                    }
                }
            }
        }

        // Admin Announcement
        if (settings.announcementText.isNotBlank()) {
            item {
                Surface(
                    color = NabdViolet.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NabdViolet.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = NabdCyan)
                        Text(
                            text = settings.announcementText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Sync progress bar when refreshing active servers
        if (isSyncing) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.refreshing,
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdCyan
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50)),
                        color = NabdCyan,
                        trackColor = NabdSurfaceElevated
                    )
                }
            }
        } else if (!syncMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = NabdPink.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NabdPink.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onRefreshClick)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.retry, tint = NabdPink)
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Featured Hero Banner Carousel
        item {
            if (featuredChannels.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { featuredChannels.size })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val ch = featuredChannels[page]
                    FeaturedBannerCard(
                        title = ch.name,
                        subtitle = "${ch.category} • ${ch.country}",
                        imageUrl = ch.logoUrl,
                        isLive = true,
                        onWatchClick = { onPlayChannel(ch) }
                    )
                }
            } else if (!isSyncing) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FeaturedBannerCard(
                        title = strings.featuredChannel,
                        subtitle = strings.appTagline,
                        imageUrl = "",
                        isLive = true,
                        onWatchClick = onRefreshClick
                    )
                }
            }
        }

        // Server Source Switcher
        val enabledServers = servers.filter { it.isEnabled }
        if (enabledServers.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryPill(
                            label = strings.allServersLabel,
                            selected = settings.activeServerId == "all",
                            onClick = { onSelectServer("all") },
                            testTag = "server_chip_all"
                        )
                    }
                    items(enabledServers, key = { it.id }) { srv ->
                        CategoryPill(
                            label = srv.name,
                            selected = settings.activeServerId == srv.id,
                            onClick = { onSelectServer(srv.id) },
                            testTag = "server_chip_${srv.id}"
                        )
                    }
                }
            }
        }

        // Category Chips
        if (categories.isNotEmpty()) {
            item {
                CategoryChipRow(
                    categories = categories.take(15),
                    selectedCategory = selectedCategory,
                    isArabic = isArabic,
                    onSelect = onSelectCategory
                )
            }
        }

        // Quick Library Shortcuts
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    QuickShortcutChip(
                        icon = Icons.Default.Favorite,
                        label = strings.favorites,
                        accent = NabdPink,
                        onClick = { onOpenLibraryTab(0) }
                    )
                }
                item {
                    QuickShortcutChip(
                        icon = Icons.Default.History,
                        label = strings.history,
                        accent = NabdCyan,
                        onClick = { onOpenLibraryTab(1) }
                    )
                }
                item {
                    QuickShortcutChip(
                        icon = Icons.Default.Download,
                        label = strings.downloads,
                        accent = NabdViolet,
                        onClick = { onOpenLibraryTab(2) }
                    )
                }
                item {
                    QuickShortcutChip(
                        icon = Icons.Default.CalendarMonth,
                        label = strings.epgGuide,
                        accent = NabdCyan,
                        onClick = onOpenEpg
                    )
                }
            }
        }

        // Continue Watching
        if (watchHistory.isNotEmpty()) {
            item {
                SectionHeader(title = strings.continueWatching)
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watchHistory.take(12), key = { it.contentId }) { item ->
                        ContinueWatchingCard(item = item, onClick = { onResumeHistory(item) })
                    }
                }
            }
        }

        // Popular Channels
        item {
            SectionHeader(
                title = strings.popularChannels,
                actionText = strings.seeAll,
                onActionClick = onSeeAllLive
            )
            Spacer(Modifier.height(10.dp))
            if (popularChannels.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popularChannels, key = { it.id }) { ch ->
                        ChannelCard(
                            channel = ch,
                            onClick = { onPlayChannel(ch) },
                            onFavoriteClick = { onFavoriteChannel(ch) },
                            modifier = Modifier.width(152.dp)
                        )
                    }
                }
            } else if (!isSyncing) {
                val isFilteredToSingleServer = settings.activeServerId != "all"
                EmptyStateView(
                    icon = Icons.Default.LiveTv,
                    title = strings.emptyChannelsTitle,
                    subtitle = strings.emptyChannelsSubtitle,
                    actionLabel = if (isFilteredToSingleServer) strings.allServersLabel else strings.retryLoad,
                    onAction = {
                        if (isFilteredToSingleServer) onSelectServer("all") else onRefreshClick()
                    }
                )
            }
        }

        // Recently Added Channels
        if (recentChannels.isNotEmpty()) {
            item {
                SectionHeader(
                    title = strings.recentlyAdded,
                    actionText = strings.seeAll,
                    onActionClick = onSeeAllLive
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentChannels, key = { "rec_${it.id}" }) { ch ->
                        ChannelCard(
                            channel = ch,
                            onClick = { onPlayChannel(ch) },
                            onFavoriteClick = { onFavoriteChannel(ch) },
                            modifier = Modifier.width(152.dp)
                        )
                    }
                }
            }
        }

        // Movies Carousel
        item {
            SectionHeader(
                title = strings.moviesTitle,
                actionText = strings.seeAll,
                onActionClick = onSeeAllMovies
            )
            Spacer(Modifier.height(10.dp))
            if (movies.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies.take(15), key = { it.id }) { movie ->
                        VodPosterCard(
                            title = movie.title,
                            posterUrl = movie.posterUrl,
                            rating = movie.rating,
                            badge = movie.year.toString(),
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
            } else {
                Surface(
                    color = NabdSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NabdBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onOpenAdmin)
                ) {
                    Text(
                        text = "${strings.noMoviesYet} — ${strings.addFromAdminHint}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NabdTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Series Carousel
        if (series.isNotEmpty()) {
            item {
                SectionHeader(
                    title = strings.seriesTitle,
                    actionText = strings.seeAll,
                    onActionClick = onSeeAllSeries
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(series.take(15), key = { it.id }) { item ->
                        VodPosterCard(
                            title = item.title,
                            posterUrl = item.posterUrl,
                            rating = item.rating,
                            badge = item.year.toString(),
                            onClick = { onSeriesClick(item) },
                            modifier = Modifier.width(136.dp)
                        )
                    }
                }
            }
        }

        // Telegram Subscription Channels & Developer Rights
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TelegramFollowSection(
                    botUrl = settings.telegramBotUrl,
                    contactUrl = settings.telegramContactUrl,
                    channelUrl = settings.telegramChannelUrl
                )
                DeveloperRightsCard(
                    developerName = settings.developerName,
                    developerUrl = settings.developerUrl
                )
            }
        }
    }
}

@Composable
private fun HeaderCircleAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = NabdSurface,
        shape = CircleShape,
        border = BorderStroke(1.dp, NabdBorder),
        modifier = Modifier.size(42.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(testTag)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = NabdCyan
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickShortcutChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit
) {
    val progress = if (item.durationMs > 0L) {
        (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else if (item.contentType == ContentType.CHANNEL) 1f else 0.15f

    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NabdBorder),
        modifier = Modifier
            .width(176.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(NabdSurfaceElevated)
            ) {
                if (item.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NabdCyan
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = NabdPink,
                trackColor = NabdBorder
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
