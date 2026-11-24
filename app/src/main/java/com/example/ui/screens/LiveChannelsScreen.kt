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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ChannelEntity
import com.example.data.model.StreamHealthStatus
import com.example.ui.components.CategoryChipRow
import com.example.ui.components.ChannelCard
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
import com.example.viewmodel.LiveFilterState

@Composable
fun LiveChannelsScreen(
    channels: List<ChannelEntity>,
    categories: List<String>,
    countries: List<String>,
    languages: List<String>,
    filter: LiveFilterState,
    isArabic: Boolean,
    isSyncing: Boolean,
    healthCheckProgress: Pair<Int, Int>?,
    onFilterChange: ((LiveFilterState) -> LiveFilterState) -> Unit,
    onToggleHideDead: (Boolean) -> Unit,
    onRunHealthCheck: () -> Unit,
    onRefresh: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onFavoriteChannel: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showCountryMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBg)
    ) {
        // Header + Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = strings.liveChannels,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "${channels.size} ${strings.channelsCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdCyan
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onFilterChange { it.copy(sortByAZ = !it.sortByAZ) } },
                    modifier = Modifier.testTag("live_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = strings.sortAZ,
                        tint = if (filter.sortByAZ) NabdCyan else NabdTextSecondary
                    )
                }
                IconButton(
                    onClick = { onFilterChange { it.copy(isGridView = !it.isGridView) } },
                    modifier = Modifier.testTag("live_view_toggle_button")
                ) {
                    Icon(
                        imageVector = if (filter.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (filter.isGridView) strings.listView else strings.gridView,
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onRunHealthCheck,
                    modifier = Modifier.testTag("live_health_check_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = strings.checkHealth,
                        tint = if (healthCheckProgress != null) NabdEmerald else NabdTextSecondary
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("live_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = strings.refreshPlaylist,
                        tint = NabdCyan
                    )
                }
            }
        }

        // Instant Search Field
        OutlinedTextField(
            value = filter.query,
            onValueChange = { q -> onFilterChange { it.copy(query = q) } },
            placeholder = { Text(strings.searchHint, color = NabdTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NabdCyan) },
            trailingIcon = {
                if (filter.query.isNotEmpty()) {
                    IconButton(onClick = { onFilterChange { it.copy(query = "") } }) {
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
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("live_search_input")
        )

        // Category Chips
        CategoryChipRow(
            categories = categories,
            selectedCategory = filter.category,
            isArabic = isArabic,
            onSelect = { cat -> onFilterChange { it.copy(category = cat) } }
        )

        Spacer(Modifier.height(8.dp))

        // Country, Language, Hide Dead Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box {
                    FilterChip(
                        selected = filter.country != "All",
                        onClick = { showCountryMenu = true },
                        label = {
                            Text(
                                if (filter.country == "All") strings.filterCountry else "${strings.filterCountry}: ${filter.country}"
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NabdViolet,
                            selectedLabelColor = Color.White,
                            containerColor = NabdSurface,
                            labelColor = NabdTextSecondary
                        )
                    )
                    DropdownMenu(
                        expanded = showCountryMenu,
                        onDismissRequest = { showCountryMenu = false },
                        containerColor = NabdSurfaceElevated
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.all, color = Color.White) },
                            onClick = {
                                onFilterChange { it.copy(country = "All") }
                                showCountryMenu = false
                            }
                        )
                        countries.take(40).forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c, color = Color.White) },
                                onClick = {
                                    onFilterChange { it.copy(country = c) }
                                    showCountryMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box {
                    FilterChip(
                        selected = filter.language != "All",
                        onClick = { showLangMenu = true },
                        label = {
                            Text(
                                if (filter.language == "All") strings.filterLanguage else "${strings.filterLanguage}: ${filter.language}"
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NabdViolet,
                            selectedLabelColor = Color.White,
                            containerColor = NabdSurface,
                            labelColor = NabdTextSecondary
                        )
                    )
                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false },
                        containerColor = NabdSurfaceElevated
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.all, color = Color.White) },
                            onClick = {
                                onFilterChange { it.copy(language = "All") }
                                showLangMenu = false
                            }
                        )
                        languages.take(30).forEach { l ->
                            DropdownMenuItem(
                                text = { Text(l, color = Color.White) },
                                onClick = {
                                    onFilterChange { it.copy(language = l) }
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                FilterChip(
                    selected = filter.hideDead,
                    onClick = { onToggleHideDead(!filter.hideDead) },
                    label = { Text(strings.hideDeadChannels) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NabdPink,
                        selectedLabelColor = Color.White,
                        containerColor = NabdSurface,
                        labelColor = NabdTextSecondary
                    )
                )
            }
        }

        if (healthCheckProgress != null) {
            val (done, total) = healthCheckProgress
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "${strings.checkingHealth} ($done/$total)",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdEmerald
                )
                LinearProgressIndicator(
                    progress = { if (total > 0) done.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = NabdEmerald,
                    trackColor = NabdSurfaceElevated
                )
            }
        }

        if (isSyncing && channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.refreshing, color = NabdCyan)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.width(200.dp), color = NabdCyan)
                }
            }
        } else if (channels.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.LiveTv,
                title = strings.emptyChannelsTitle,
                subtitle = strings.emptyChannelsSubtitle,
                actionLabel = strings.retryLoad,
                onAction = onRefresh,
                modifier = Modifier.fillMaxSize()
            )
        } else if (filter.isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 154.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { ch ->
                    ChannelCard(
                        channel = ch,
                        onClick = { onPlayChannel(ch) },
                        onFavoriteClick = { onFavoriteChannel(ch) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { ch ->
                    ChannelListRow(
                        channel = ch,
                        onClick = { onPlayChannel(ch) },
                        onFavoriteClick = { onFavoriteChannel(ch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelListRow(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NabdBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NabdSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = NabdCyan
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${channel.category} • ${channel.country} • ${channel.language}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val badgeColor = when (channel.healthStatus) {
                StreamHealthStatus.ONLINE -> NabdEmerald
                StreamHealthStatus.OFFLINE -> NabdRed
                StreamHealthStatus.UNKNOWN -> NabdPink
            }
            val badgeLabel = when (channel.healthStatus) {
                StreamHealthStatus.OFFLINE -> strings.deadBadge
                else -> strings.liveBadge
            }
            Surface(
                color = badgeColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) NabdPink else NabdTextSecondary
                )
            }
        }
    }
}
