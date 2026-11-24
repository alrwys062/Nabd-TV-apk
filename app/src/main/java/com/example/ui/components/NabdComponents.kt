package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ChannelEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SeriesEntity
import com.example.data.model.StreamHealthStatus
import com.example.ui.theme.CyanPinkHeroBrush
import com.example.ui.theme.CyanVioletBrush
import com.example.ui.theme.DarkCardScrimBrush
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdEmerald
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdRed
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdSurfaceElevated
import com.example.ui.theme.NabdTextMuted
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet
import com.example.ui.theme.VioletPinkBrush
import com.example.ui.theme.localizeCategoryName

@Composable
fun NabdLogoIcon(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.nabd_tv_icon),
        contentDescription = LocalAppStrings.current.appName,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun LiveBadge(
    text: String = LocalAppStrings.current.liveNow,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_dot"
    )
    Surface(
        color = NabdPink,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FeaturedBannerCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    isLive: Boolean,
    onWatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(CyanPinkHeroBrush)
            .border(
                width = if (focused) 2.5.dp else 1.dp,
                color = if (focused) NabdCyan else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(30.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onWatchClick)
            .testTag("featured_banner_card")
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.45f
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xD9090C1A),
                            Color(0x667C4DFF),
                            Color(0x4DFF4081)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            if (isLive) {
                LiveBadge(text = strings.liveNow)
                Spacer(Modifier.height(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clickable(onClick = onWatchClick)
                    .testTag("featured_watch_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NabdBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.watchNow,
                        style = MaterialTheme.typography.labelLarge,
                        color = NabdBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChipRow(
    categories: List<String>,
    selectedCategory: String,
    isArabic: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "All") {
            CategoryPill(
                label = strings.all,
                selected = selectedCategory == "All",
                onClick = { onSelect("All") },
                testTag = "category_chip_all"
            )
        }
        items(categories, key = { it }) { cat ->
            CategoryPill(
                label = localizeCategoryName(cat, isArabic),
                selected = selectedCategory.equals(cat, ignoreCase = true),
                onClick = { onSelect(cat) },
                testTag = "category_chip_${cat.lowercase()}"
            )
        }
    }
}

@Composable
fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bg by animateColorAsState(
        targetValue = if (selected) NabdViolet else NabdSurface,
        label = "chip_bg"
    )
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (selected) NabdViolet else NabdBorder),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else NabdTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

@Composable
fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (focused) 2.5.dp else 1.dp,
            color = if (focused) NabdCyan else NabdBorder
        ),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .testTag("channel_card_${channel.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(CyanVioletBrush)
                    .padding(10.dp)
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(6.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }

                // Health dot
                val dotColor = when (channel.healthStatus) {
                    StreamHealthStatus.ONLINE -> NabdEmerald
                    StreamHealthStatus.OFFLINE -> NabdRed
                    StreamHealthStatus.UNKNOWN -> NabdPink
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                if (onFavoriteClick != null) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (channel.isFavorite) NabdPink else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1326))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${channel.country} • ${channel.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VodPosterCard(
    title: String,
    posterUrl: String,
    rating: Float,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (focused) 2.5.dp else 1.dp,
            color = if (focused) NabdPink else NabdBorder
        ),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(VioletPinkBrush)
            ) {
                if (posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = title,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkCardScrimBrush)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "%.1f".format(rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                if (badge.isNotBlank()) {
                    Surface(
                        color = NabdViolet,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1326))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

enum class MainTab(val routeKey: String, val icon: ImageVector) {
    HOME("home", Icons.Default.Home),
    LIVE("live", Icons.Default.LiveTv),
    MOVIES("movies", Icons.Default.Movie),
    SERIES("series", Icons.Default.Tv),
    SETTINGS("settings", Icons.Default.Settings)
}

@Composable
fun NabdFloatingBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Surface(
            color = NabdSurface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NabdBorder),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    val label = when (tab) {
                        MainTab.HOME -> strings.navHome
                        MainTab.LIVE -> strings.navLive
                        MainTab.MOVIES -> strings.navMovies
                        MainTab.SERIES -> strings.navSeries
                        MainTab.SETTINGS -> strings.navSettings
                    }
                    val selected = tab == currentTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 6.dp)
                            .testTag("bottom_nav_${tab.routeKey}")
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = label,
                            tint = if (selected) NabdCyan else NabdTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) NabdCyan else NabdTextMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NabdNavigationRail(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    NavigationRail(
        containerColor = NabdSurface,
        modifier = modifier
    ) {
        Spacer(Modifier.height(12.dp))
        NabdLogoIcon(size = 40.dp)
        Spacer(Modifier.height(20.dp))
        MainTab.entries.forEach { tab ->
            val label = when (tab) {
                MainTab.HOME -> strings.navHome
                MainTab.LIVE -> strings.navLive
                MainTab.MOVIES -> strings.navMovies
                MainTab.SERIES -> strings.navSeries
                MainTab.SETTINGS -> strings.navSettings
            }
            NavigationRailItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NabdCyan,
                    selectedTextColor = NabdCyan,
                    indicatorColor = NabdSurfaceElevated,
                    unselectedIconColor = NabdTextMuted,
                    unselectedTextColor = NabdTextMuted
                )
            )
        }
    }
}

@Composable
fun TelegramFollowSection(
    botUrl: String,
    contactUrl: String,
    channelUrl: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = strings.followUs,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            text = strings.followUsSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = NabdTextSecondary
        )
        TelegramLinkButton(
            label = strings.telegramBotLabel,
            url = botUrl,
            accent = NabdCyan,
            onClick = { openExternalUrl(context, botUrl) },
            testTag = "telegram_bot_button"
        )
        TelegramLinkButton(
            label = strings.telegramContactLabel,
            url = contactUrl,
            accent = NabdViolet,
            onClick = { openExternalUrl(context, contactUrl) },
            testTag = "telegram_contact_button"
        )
        TelegramLinkButton(
            label = strings.telegramChannelLabel,
            url = channelUrl,
            accent = NabdPink,
            onClick = { openExternalUrl(context, channelUrl) },
            testTag = "telegram_channel_button"
        )
    }
}

@Composable
private fun TelegramLinkButton(
    label: String,
    url: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Telegram",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent
            )
        }
    }
}

fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NabdSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NabdCyan,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NabdTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(actionLabel, color = Color.White)
            }
        }
    }
}

@Composable
fun PinEntryDialog(
    title: String,
    expectedPin: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NabdSurface,
        title = { Text(title, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8) {
                            pin = it
                            error = false
                        }
                    },
                    label = { Text(strings.enterPin) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )
                if (error) {
                    Text(
                        text = strings.wrongPin,
                        color = NabdRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (expectedPin == null || pin == expectedPin) {
                        onConfirmed(pin)
                    } else {
                        error = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                Text(strings.confirm, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(strings.cancel, color = NabdTextSecondary)
            }
        }
    )
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontSize = 18.sp
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = NabdCyan
                )
            }
        }
    }
}
