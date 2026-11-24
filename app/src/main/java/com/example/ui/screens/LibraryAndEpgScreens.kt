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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.data.model.DownloadEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SearchHistoryEntity
import com.example.data.model.SeriesEntity
import com.example.data.model.WatchHistoryEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdEmerald
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdRed
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdSurfaceElevated
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    initialTab: Int,
    favoriteChannels: List<ChannelEntity>,
    favoriteMovies: List<MovieEntity>,
    favoriteSeries: List<SeriesEntity>,
    watchHistory: List<WatchHistoryEntity>,
    downloads: List<DownloadEntity>,
    onBack: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onResumeHistory: (WatchHistoryEntity) -> Unit,
    onClearHistory: () -> Unit,
    onPlayDownload: (DownloadEntity) -> Unit,
    onDeleteDownload: (DownloadEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val tabs = listOf(strings.favorites, strings.history, strings.downloads)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = tabs[selectedTab],
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (selectedTab == 1 && watchHistory.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text(strings.clearHistory, color = NabdPink)
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NabdSurface,
            contentColor = NabdCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NabdCyan
                )
            }
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            color = if (selectedTab == idx) NabdCyan else NabdTextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (favoriteChannels.isEmpty() && favoriteMovies.isEmpty() && favoriteSeries.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Favorite,
                        title = strings.noFavorites,
                        subtitle = strings.emptyChannelsSubtitle,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoriteChannels, key = { "fc_${it.id}" }) { ch ->
                            LibraryItemRow(
                                title = ch.name,
                                subtitle = "${ch.category} • ${ch.country}",
                                badge = strings.liveBadge,
                                accent = NabdPink,
                                onClick = { onPlayChannel(ch) }
                            )
                        }
                        items(favoriteMovies, key = { "fm_${it.id}" }) { mv ->
                            LibraryItemRow(
                                title = mv.title,
                                subtitle = "${mv.category} • ${mv.year}",
                                badge = strings.moviesTitle,
                                accent = NabdViolet,
                                onClick = { onMovieClick(mv) }
                            )
                        }
                        items(favoriteSeries, key = { "fs_${it.id}" }) { sr ->
                            LibraryItemRow(
                                title = sr.title,
                                subtitle = "${sr.category} • ${sr.year}",
                                badge = strings.seriesTitle,
                                accent = NabdCyan,
                                onClick = { onSeriesClick(sr) }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (watchHistory.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = strings.noHistory,
                        subtitle = strings.appTagline,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(watchHistory, key = { it.contentId }) { item ->
                            LibraryItemRow(
                                title = item.title,
                                subtitle = item.contentType.name,
                                badge = strings.resume,
                                accent = NabdCyan,
                                onClick = { onResumeHistory(item) }
                            )
                        }
                    }
                }
            }
            2 -> {
                if (downloads.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Download,
                        title = strings.noDownloads,
                        subtitle = strings.moviesTitle,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(downloads, key = { it.contentId }) { dl ->
                            Surface(
                                color = NabdSurface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, NabdBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = dl.status == "COMPLETED") { onPlayDownload(dl) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dl.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                            Text(
                                                text = "${dl.status} • ${dl.progressPercent}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (dl.status == "COMPLETED") NabdEmerald else NabdCyan
                                            )
                                        }
                                        IconButton(onClick = { onDeleteDownload(dl) }) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = NabdRed)
                                        }
                                    }
                                    if (dl.status == "DOWNLOADING") {
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { dl.progressPercent / 100f },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = NabdCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemRow(
    title: String,
    subtitle: String,
    badge: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NabdBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NabdTextSecondary)
            }
            Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun EpgScreen(
    programs: List<EpgProgramEntity>,
    currentEpgUrl: String,
    onBack: () -> Unit,
    onLoadEpgUrl: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var urlInput by remember(currentEpgUrl) { mutableStateOf(currentEpgUrl) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nowMs = System.currentTimeMillis()
    val grouped = remember(programs) { programs.groupBy { it.channelName.ifBlank { it.channelTvgId } } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(strings.epgGuide, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = { Text("https://.../epg.xml", color = NabdTextSecondary) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NabdSurface,
                    unfocusedContainerColor = NabdSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Button(
                onClick = { if (urlInput.isNotBlank()) onLoadEpgUrl(urlInput.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = NabdViolet)
            ) {
                Text(strings.confirm, color = Color.White)
            }
        }

        Spacer(Modifier.height(10.dp))

        if (grouped.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CalendarMonth,
                title = strings.epgEmpty,
                subtitle = strings.epgLoadUrl,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                grouped.forEach { (channelName, itemsList) ->
                    item(key = "epg_ch_$channelName") {
                        Surface(
                            color = NabdSurface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, NabdBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = channelName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NabdCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                itemsList.take(5).forEach { prog ->
                                    val isLive = nowMs in prog.startTimeMs..prog.endTimeMs
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = if (isLive) NabdPink else NabdSurfaceElevated,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${timeFormat.format(Date(prog.startTimeMs))} - ${timeFormat.format(Date(prog.endTimeMs))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = prog.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    channels: List<ChannelEntity>,
    movies: List<MovieEntity>,
    series: List<SeriesEntity>,
    searchHistory: List<SearchHistoryEntity>,
    onBack: () -> Unit,
    onSaveQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }

    val matchedChannels = remember(channels, query) {
        if (query.isBlank()) emptyList()
        else channels.filter {
            it.name.contains(query, true) || it.category.contains(query, true) || it.country.contains(query, true)
        }.take(40)
    }
    val matchedMovies = remember(movies, query) {
        if (query.isBlank()) emptyList() else movies.filter { it.title.contains(query, true) }.take(20)
    }
    val matchedSeries = remember(series, query) {
        if (query.isBlank()) emptyList() else series.filter { it.title.contains(query, true) }.take(20)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.searchHint, color = NabdTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NabdCyan) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = NabdTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NabdSurface,
                    unfocusedContainerColor = NabdSurface,
                    focusedBorderColor = NabdCyan,
                    unfocusedBorderColor = NabdBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("smart_search_input")
            )
        }

        if (query.isBlank()) {
            if (searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.recentSearches, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    TextButton(onClick = onClearHistory) { Text(strings.clearHistory, color = NabdPink) }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchHistory, key = { it.query }) { item ->
                        Surface(
                            color = NabdSurface,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, NabdBorder),
                            modifier = Modifier.clickable { query = item.query }
                        ) {
                            Text(
                                text = item.query,
                                color = NabdCyan,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = strings.searchEmpty,
                    subtitle = strings.searchHint,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matchedChannels, key = { "sc_${it.id}" }) { ch ->
                    LibraryItemRow(
                        title = ch.name,
                        subtitle = "${ch.category} • ${ch.country}",
                        badge = strings.liveBadge,
                        accent = NabdCyan,
                        onClick = {
                            onSaveQuery(query)
                            onPlayChannel(ch)
                        }
                    )
                }
                items(matchedMovies, key = { "sm_${it.id}" }) { mv ->
                    LibraryItemRow(
                        title = mv.title,
                        subtitle = "${mv.category} • ${mv.year}",
                        badge = strings.moviesTitle,
                        accent = NabdPink,
                        onClick = {
                            onSaveQuery(query)
                            onMovieClick(mv)
                        }
                    )
                }
                items(matchedSeries, key = { "ss_${it.id}" }) { sr ->
                    LibraryItemRow(
                        title = sr.title,
                        subtitle = "${sr.category} • ${sr.year}",
                        badge = strings.seriesTitle,
                        accent = NabdViolet,
                        onClick = {
                            onSaveQuery(query)
                            onSeriesClick(sr)
                        }
                    )
                }
                if (matchedChannels.isEmpty() && matchedMovies.isEmpty() && matchedSeries.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.LiveTv,
                            title = strings.emptyChannelsTitle,
                            subtitle = strings.emptyChannelsSubtitle
                        )
                    }
                }
            }
        }
    }
}
