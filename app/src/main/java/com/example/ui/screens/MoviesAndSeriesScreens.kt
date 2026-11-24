package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DownloadEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SeriesEntity
import com.example.ui.components.CategoryPill
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VodPosterCard
import com.example.ui.theme.DarkCardScrimBrush
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdEmerald
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdSurfaceElevated
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet
import com.example.ui.theme.VioletPinkBrush

@Composable
fun MoviesScreen(
    movies: List<MovieEntity>,
    onMovieClick: (MovieEntity) -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(movies) { movies.map { it.category }.distinct().filter { it.isNotBlank() } }

    val filtered = remember(movies, query, selectedCategory) {
        movies.filter {
            (selectedCategory == "All" || it.category.equals(selectedCategory, true)) &&
                (query.isBlank() || it.title.contains(query, true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBg)
    ) {
        Text(
            text = strings.moviesTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchHint, color = NabdTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NabdCyan) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NabdSurface,
                unfocusedContainerColor = NabdSurface,
                focusedBorderColor = NabdViolet,
                unfocusedBorderColor = NabdBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryPill(
                        label = strings.all,
                        selected = selectedCategory == "All",
                        onClick = { selectedCategory = "All" },
                        testTag = "movies_cat_all"
                    )
                }
                items(categories) { cat ->
                    CategoryPill(
                        label = cat,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        testTag = "movies_cat_$cat"
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Movie,
                title = strings.noMoviesYet,
                subtitle = strings.addFromAdminHint,
                actionLabel = strings.openAdminPanel,
                onAction = onOpenAdmin,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { movie ->
                    VodPosterCard(
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        rating = movie.rating,
                        badge = movie.year.toString(),
                        onClick = { onMovieClick(movie) }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailScreen(
    movie: MovieEntity,
    downloadState: DownloadEntity?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(VioletPinkBrush)
        ) {
            val img = movie.backdropUrl.ifBlank { movie.posterUrl }
            if (img.isNotBlank()) {
                AsyncImage(
                    model = img,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkCardScrimBrush)
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = NabdViolet, shape = RoundedCornerShape(50)) {
                        Text(
                            text = movie.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text("${movie.year}", style = MaterialTheme.typography.labelLarge, color = NabdTextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f".format(movie.rating), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("movie_play_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(strings.play, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleFavorite,
                    border = BorderStroke(1.dp, NabdPink),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (movie.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = NabdPink
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (movie.isFavorite) strings.removeFavorite else strings.addFavorite,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                if (movie.allowDownload) {
                    OutlinedButton(
                        onClick = onDownload,
                        border = BorderStroke(1.dp, NabdCyan),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (downloadState?.status == "COMPLETED") Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (downloadState?.status == "COMPLETED") NabdEmerald else NabdCyan
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (downloadState?.status) {
                                "COMPLETED" -> strings.downloaded
                                "DOWNLOADING" -> "${downloadState.progressPercent}%"
                                else -> strings.download
                            },
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${movie.title}\n${movie.streamUrl}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, strings.share))
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NabdSurface)
                ) {
                    Icon(Icons.Default.Share, contentDescription = strings.share, tint = Color.White)
                }
            }

            if (downloadState?.status == "DOWNLOADING") {
                LinearProgressIndicator(
                    progress = { downloadState.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = NabdCyan,
                    trackColor = NabdSurfaceElevated
                )
            }

            if (movie.description.isNotBlank()) {
                Surface(
                    color = NabdSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NabdBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NabdTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesScreen(
    series: List<SeriesEntity>,
    onSeriesClick: (SeriesEntity) -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(series, query) {
        if (query.isBlank()) series else series.filter { it.title.contains(query, true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBg)
    ) {
        Text(
            text = strings.seriesTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchHint, color = NabdTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NabdCyan) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NabdSurface,
                unfocusedContainerColor = NabdSurface,
                focusedBorderColor = NabdViolet,
                unfocusedBorderColor = NabdBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Tv,
                title = strings.noSeriesYet,
                subtitle = strings.addFromAdminHint,
                actionLabel = strings.openAdminPanel,
                onAction = onOpenAdmin,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { item ->
                    VodPosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        rating = item.rating,
                        badge = item.year.toString(),
                        onClick = { onSeriesClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesDetailScreen(
    series: SeriesEntity,
    episodes: List<EpisodeEntity>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayEpisode: (EpisodeEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    val seasons = remember(episodes) {
        episodes.map { it.seasonNumber }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember(seasons) { mutableIntStateOf(seasons.first()) }
    val seasonEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.seasonNumber == selectedSeason }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .background(VioletPinkBrush)
            ) {
                val img = series.backdropUrl.ifBlank { series.posterUrl }
                if (img.isNotBlank()) {
                    AsyncImage(
                        model = img,
                        contentDescription = series.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkCardScrimBrush)
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = strings.addFavorite,
                        tint = if (series.isFavorite) NabdPink else Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "${series.category} • ${series.year} • ★ ${"%.1f".format(series.rating)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NabdCyan
                    )
                }
            }
        }

        if (series.description.isNotBlank()) {
            item {
                Text(
                    text = series.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NabdTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seasons) { s ->
                    CategoryPill(
                        label = "${strings.seasonPrefix} $s",
                        selected = selectedSeason == s,
                        onClick = { selectedSeason = s },
                        testTag = "season_tab_$s"
                    )
                }
            }
        }

        items(seasonEpisodes, key = { it.id }) { ep ->
            Surface(
                color = NabdSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, NabdBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onPlayEpisode(ep) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NabdViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = strings.play, tint = NabdCyan)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.episodePrefix} ${ep.episodeNumber} • ${ep.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${ep.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdTextSecondary
                        )
                    }
                }
            }
        }
    }
}
