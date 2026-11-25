package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ServerSourceEntity
import com.example.data.model.ServerType
import com.example.ui.components.PinEntryDialog
import com.example.ui.theme.CyanVioletBrush
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
import com.example.ui.theme.VioletPinkBrush
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

private enum class AdminSheet {
    NONE, SERVERS, CHANNELS, MOVIES, SERIES, IMPORT_M3U, CATEGORIES, TELEGRAM, BACKUP
}

@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val seriesList by viewModel.series.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val toastMsg by viewModel.statusToast.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var unlocked by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(AdminSheet.NONE) }

    LaunchedEffect(toastMsg) {
        if (!toastMsg.isNullOrBlank()) {
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusToast()
        }
    }

    if (!unlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NabdBg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NabdCyan, modifier = Modifier.size(56.dp))
                Text(strings.controlPanel, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                PinEntryDialog(
                    title = strings.controlPanel,
                    expectedPin = settings.adminPin,
                    onDismiss = onBack,
                    onConfirmed = { unlocked = true }
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = strings.controlPanel,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NabdCyan, strokeWidth = 2.dp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2x2 Gradient KPI Cards (exact nabd_tv_style.png right panel!)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminKpiCard(
                            value = formatCompactCount(stats.channelsCount),
                            label = strings.channelsStat,
                            brush = CyanVioletBrush,
                            modifier = Modifier.weight(1f)
                        )
                        AdminKpiCard(
                            value = formatCompactCount(stats.moviesCount),
                            label = strings.moviesStat,
                            brush = VioletPinkBrush,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminKpiCard(
                            value = formatCompactCount(stats.seriesCount),
                            label = strings.seriesStat,
                            brush = CyanVioletBrush,
                            modifier = Modifier.weight(1f)
                        )
                        AdminKpiCard(
                            value = formatCompactCount(stats.serversCount),
                            label = strings.serversStat,
                            brush = VioletPinkBrush,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            item { AdminActionRow(strings.addServer, { activeSheet = AdminSheet.SERVERS }, "admin_servers_btn") }
            item { AdminActionRow(strings.addChannel, { activeSheet = AdminSheet.CHANNELS }, "admin_channels_btn") }
            item { AdminActionRow(strings.addMovie, { activeSheet = AdminSheet.MOVIES }, "admin_movies_btn") }
            item { AdminActionRow(strings.addSeries, { activeSheet = AdminSheet.SERIES }, "admin_series_btn") }
            item { AdminActionRow(strings.importM3u, { activeSheet = AdminSheet.IMPORT_M3U }, "admin_import_btn") }
            item { AdminActionRow(strings.categoriesBanners, { activeSheet = AdminSheet.CATEGORIES }, "admin_categories_btn") }
            item { AdminActionRow(strings.telegramLinksAction, { activeSheet = AdminSheet.TELEGRAM }, "admin_telegram_btn") }
            item { AdminActionRow(strings.backupRestore, { activeSheet = AdminSheet.BACKUP }, "admin_backup_btn") }

            if (reports.isNotEmpty()) {
                item {
                    Text(
                        text = "${strings.reportDead} (${reports.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = NabdPink
                    )
                }
                items(reports.take(5), key = { it.id }) { rep ->
                    Surface(
                        color = NabdSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NabdRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${rep.channelName} — ${rep.reason}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { viewModel.publishAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("admin_save_publish_btn")
                ) {
                    Text(strings.saveAndPublish, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }

    // Sub-Dialogs for all Admin features
    when (activeSheet) {
        AdminSheet.SERVERS -> {
            ServersManagerDialog(
                servers = servers,
                onDismiss = { activeSheet = AdminSheet.NONE },
                onSaveServer = { id, name, type, url, host, user, pass, enabled, order ->
                    viewModel.saveServer(id, name, type, url, host, user, pass, enabled, order)
                },
                onToggleEnabled = { viewModel.toggleServerEnabled(it) },
                onMoveOrder = { s, d -> viewModel.moveServerOrder(s, d) },
                onTestAndSync = { viewModel.testAndSyncServer(it) },
                onDelete = { viewModel.deleteServer(it.id) },
                onResetServers = { viewModel.resetServersToDefault() }
            )
        }
        AdminSheet.CHANNELS -> {
            ChannelsManagerDialog(
                channels = channels.take(50),
                onDismiss = { activeSheet = AdminSheet.NONE },
                onAddChannel = { name, logo, url, backup, cat, country, lang, featured ->
                    viewModel.saveChannel(null, name, logo, url, backup, cat, country, lang, featured)
                },
                onToggleFeatured = { viewModel.toggleChannelFeatured(it) },
                onDelete = { viewModel.deleteChannel(it.id) }
            )
        }
        AdminSheet.MOVIES -> {
            MoviesManagerDialog(
                movies = movies,
                onDismiss = { activeSheet = AdminSheet.NONE },
                onAddMovie = { title, poster, desc, year, rating, cat, url, backup, sub, featured ->
                    viewModel.saveMovie(null, title, poster, desc, year, rating, cat, url, backup, sub, featured)
                },
                onToggleFeatured = { viewModel.toggleMovieFeatured(it) },
                onDelete = { viewModel.deleteMovie(it.id) }
            )
        }
        AdminSheet.SERIES -> {
            SeriesManagerDialog(
                seriesList = seriesList,
                viewModel = viewModel,
                onDismiss = { activeSheet = AdminSheet.NONE }
            )
        }
        AdminSheet.IMPORT_M3U -> {
            var rawM3u by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeSheet = AdminSheet.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.importM3u, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rawM3u,
                            onValueChange = { rawM3u = it },
                            label = { Text("#EXTM3U ...") },
                            minLines = 5,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val exported = viewModel.getExportM3uText()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, exported.take(80_000))
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export M3U"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export M3U", color = NabdCyan)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rawM3u.isNotBlank()) viewModel.importM3uRaw(rawM3u)
                            activeSheet = AdminSheet.NONE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NabdViolet)
                    ) { Text(strings.confirm, color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { activeSheet = AdminSheet.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        AdminSheet.CATEGORIES -> {
            AlertDialog(
                onDismissRequest = { activeSheet = AdminSheet.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.categoriesBanners, color = Color.White) },
                text = {
                    LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it.name }) { cfg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cfg.name, color = Color.White, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = cfg.isVisible,
                                    onCheckedChange = { viewModel.toggleCategoryVisible(cfg) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSheet = AdminSheet.NONE }) { Text(strings.confirm, color = NabdCyan) }
                }
            )
        }
        AdminSheet.TELEGRAM -> {
            var bot by remember(settings) { mutableStateOf(settings.telegramBotUrl) }
            var contact by remember(settings) { mutableStateOf(settings.telegramContactUrl) }
            var channel by remember(settings) { mutableStateOf(settings.telegramChannelUrl) }
            var devName by remember(settings) { mutableStateOf(settings.developerName) }
            var devUrl by remember(settings) { mutableStateOf(settings.developerUrl) }
            var announcement by remember(settings) { mutableStateOf(settings.announcementText) }
            var epg by remember(settings) { mutableStateOf(settings.epgUrl) }

            AlertDialog(
                onDismissRequest = { activeSheet = AdminSheet.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.telegramLinksAction, color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = bot, onValueChange = { bot = it }, label = { Text("Bot URL") }, singleLine = true)
                        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact / Channel URL 2") }, singleLine = true)
                        OutlinedTextField(value = channel, onValueChange = { channel = it }, label = { Text("Channel URL 3") }, singleLine = true)
                        OutlinedTextField(value = devName, onValueChange = { devName = it }, label = { Text("Developer Handle") }, singleLine = true)
                        OutlinedTextField(value = devUrl, onValueChange = { devUrl = it }, label = { Text("Developer Link") }, singleLine = true)
                        OutlinedTextField(value = announcement, onValueChange = { announcement = it }, label = { Text("In-App Announcement") })
                        OutlinedTextField(value = epg, onValueChange = { epg = it }, label = { Text("XMLTV EPG URL") }, singleLine = true)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveTelegramAndAnnouncement(bot, contact, channel, announcement, epg, devName, devUrl)
                            activeSheet = AdminSheet.NONE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NabdViolet)
                    ) { Text(strings.confirm, color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { activeSheet = AdminSheet.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        AdminSheet.BACKUP -> {
            var remoteUrl by remember(settings) { mutableStateOf(settings.remoteJsonSyncUrl) }
            var restoreJson by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { activeSheet = AdminSheet.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.backupRestore, color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = remoteUrl,
                            onValueChange = { remoteUrl = it },
                            label = { Text("Remote JSON URL (e.g. GitHub Raw)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.syncRemoteJson(remoteUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = NabdCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sync Remote JSON", color = NabdBg)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val backup = viewModel.getBackupJsonText()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_TEXT, backup)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export Backup JSON"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Full Backup JSON", color = NabdCyan)
                        }
                        OutlinedTextField(
                            value = restoreJson,
                            onValueChange = { restoreJson = it },
                            label = { Text("Paste Backup JSON to Restore") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (restoreJson.isNotBlank()) viewModel.restoreBackupJson(restoreJson)
                            activeSheet = AdminSheet.NONE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NabdViolet)
                    ) { Text(strings.confirm, color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { activeSheet = AdminSheet.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        AdminSheet.NONE -> {}
    }
}

@Composable
private fun AdminKpiCard(
    value: String,
    label: String,
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun AdminActionRow(
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NabdBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NabdCyan)
        }
    }
}

@Composable
private fun ServersManagerDialog(
    servers: List<ServerSourceEntity>,
    onDismiss: () -> Unit,
    onSaveServer: (String?, String, ServerType, String, String, String, String, Boolean, Int) -> Unit,
    onToggleEnabled: (ServerSourceEntity) -> Unit,
    onMoveOrder: (ServerSourceEntity, Int) -> Unit,
    onTestAndSync: (ServerSourceEntity) -> Unit,
    onDelete: (ServerSourceEntity) -> Unit,
    onResetServers: () -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ServerType.M3U) }
    var url by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NabdSurface,
        title = { Text(strings.addServer, color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                servers.forEach { srv ->
                    Surface(color = NabdSurfaceElevated, shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(srv.name, color = Color.White, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Switch(checked = srv.isEnabled, onCheckedChange = { onToggleEnabled(srv) })
                            }
                            Text("${srv.type} • ${srv.url.ifBlank { srv.xtreamHost }}", color = NabdTextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row {
                                IconButton(onClick = { onMoveOrder(srv, -1) }) { Icon(Icons.Default.ArrowUpward, null, tint = NabdCyan) }
                                IconButton(onClick = { onMoveOrder(srv, 1) }) { Icon(Icons.Default.ArrowDownward, null, tint = NabdCyan) }
                                IconButton(onClick = { onTestAndSync(srv) }) { Icon(Icons.Default.Sync, strings.testConnection, tint = NabdEmerald) }
                                IconButton(onClick = { onDelete(srv) }) { Icon(Icons.Default.Delete, strings.delete, tint = NabdRed) }
                            }
                        }
                    }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Server Name") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ServerType.entries.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { type = t }) {
                            RadioButton(selected = type == t, onClick = { type = t })
                            Text(t.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (type == ServerType.XTREAM) {
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Xtream Host (http://...)") }, singleLine = true)
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true)
                } else {
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("M3U / JSON URL") }, singleLine = true)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank() && (url.isNotBlank() || host.isNotBlank())) {
                            onSaveServer(null, name, type, url, host, user, pass, true, servers.size)
                            name = ""; url = ""; host = ""; user = ""; pass = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.addServer, color = Color.White)
                }
                OutlinedButton(
                    onClick = onResetServers,
                    border = BorderStroke(1.dp, NabdCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إعادة ضبط السيرفرات (1 + 2 + 3)", color = NabdCyan)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.confirm, color = NabdCyan) }
        }
    )
}

@Composable
private fun ChannelsManagerDialog(
    channels: List<com.example.data.model.ChannelEntity>,
    onDismiss: () -> Unit,
    onAddChannel: (String, String, String, String, String, String, String, Boolean) -> Unit,
    onToggleFeatured: (com.example.data.model.ChannelEntity) -> Unit,
    onDelete: (com.example.data.model.ChannelEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var backupUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sports") }
    var country by remember { mutableStateOf("SA") }
    var language by remember { mutableStateOf("Arabic") }
    var featured by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NabdSurface,
        title = { Text(strings.addChannel, color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Channel Name") }, singleLine = true)
                OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it }, label = { Text("Stream URL (m3u8/mpd/ts)") }, singleLine = true)
                OutlinedTextField(value = backupUrl, onValueChange = { backupUrl = it }, label = { Text("Backup Stream URL (optional)") }, singleLine = true)
                OutlinedTextField(value = logo, onValueChange = { logo = it }, label = { Text("Logo URL") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country Code") }, singleLine = true)
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured on Home Banner", color = Color.White)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank() && streamUrl.isNotBlank()) {
                            onAddChannel(name, logo, streamUrl, backupUrl, category, country, language, featured)
                            name = ""; streamUrl = ""; backupUrl = ""; logo = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.addChannel, color = Color.White) }

                channels.take(15).forEach { ch ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(ch.name, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onToggleFeatured(ch) }) {
                            Icon(Icons.Default.Star, null, tint = if (ch.isFeatured) NabdCyan else NabdTextSecondary)
                        }
                        IconButton(onClick = { onDelete(ch) }) {
                            Icon(Icons.Default.Delete, null, tint = NabdRed)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.confirm, color = NabdCyan) } }
    )
}

@Composable
private fun MoviesManagerDialog(
    movies: List<com.example.data.model.MovieEntity>,
    onDismiss: () -> Unit,
    onAddMovie: (String, String, String, Int, Float, String, String, String, String, Boolean) -> Unit,
    onToggleFeatured: (com.example.data.model.MovieEntity) -> Unit,
    onDelete: (com.example.data.model.MovieEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var poster by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2025") }
    var rating by remember { mutableStateOf("8.5") }
    var category by remember { mutableStateOf("Action") }
    var streamUrl by remember { mutableStateOf("") }
    var backupUrl by remember { mutableStateOf("") }
    var subtitleUrl by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NabdSurface,
        title = { Text(strings.addMovie, color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Movie Title") }, singleLine = true)
                OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it }, label = { Text("Stream URL (mp4/mkv/m3u8)") }, singleLine = true)
                OutlinedTextField(value = backupUrl, onValueChange = { backupUrl = it }, label = { Text("Backup URL (optional)") }, singleLine = true)
                OutlinedTextField(value = poster, onValueChange = { poster = it }, label = { Text("Poster URL") }, singleLine = true)
                OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Subtitle URL (.srt/.vtt)") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Genre") }, singleLine = true)
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, singleLine = true)
                OutlinedTextField(value = rating, onValueChange = { rating = it }, label = { Text("Rating") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured", color = Color.White)
                }
                Button(
                    onClick = {
                        if (title.isNotBlank() && streamUrl.isNotBlank()) {
                            onAddMovie(
                                title, poster, desc,
                                year.toIntOrNull() ?: 2025,
                                rating.toFloatOrNull() ?: 8.0f,
                                category, streamUrl, backupUrl, subtitleUrl, featured
                            )
                            title = ""; streamUrl = ""; poster = ""; desc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.addMovie, color = Color.White) }

                movies.forEach { mv ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(mv.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onToggleFeatured(mv) }) {
                            Icon(Icons.Default.Star, null, tint = if (mv.isFeatured) NabdPink else NabdTextSecondary)
                        }
                        IconButton(onClick = { onDelete(mv) }) {
                            Icon(Icons.Default.Delete, null, tint = NabdRed)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.confirm, color = NabdCyan) } }
    )
}

@Composable
private fun SeriesManagerDialog(
    seriesList: List<com.example.data.model.SeriesEntity>,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var poster by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedSeriesId by remember(seriesList) { mutableStateOf(seriesList.firstOrNull()?.id.orEmpty()) }
    var seasonNum by remember { mutableStateOf("1") }
    var epNum by remember { mutableStateOf("1") }
    var epTitle by remember { mutableStateOf("") }
    var epUrl by remember { mutableStateOf("") }
    var epSub by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NabdSurface,
        title = { Text(strings.addSeries, color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Series Title") }, singleLine = true)
                OutlinedTextField(value = poster, onValueChange = { poster = it }, label = { Text("Poster URL") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.saveSeries(null, title, poster, desc, 2025, 8.5f, "Series", false)
                            title = ""; poster = ""; desc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.addSeries, color = Color.White) }

                if (seriesList.isNotEmpty()) {
                    Text("Select Series for Episode:", color = NabdCyan, style = MaterialTheme.typography.labelLarge)
                    seriesList.forEach { s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSeriesId = s.id }
                        ) {
                            RadioButton(selected = selectedSeriesId == s.id, onClick = { selectedSeriesId = s.id })
                            Text(s.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { viewModel.deleteSeries(s.id) }) {
                                Icon(Icons.Default.Delete, null, tint = NabdRed)
                            }
                        }
                    }

                    OutlinedTextField(value = seasonNum, onValueChange = { seasonNum = it }, label = { Text("Season #") }, singleLine = true)
                    OutlinedTextField(value = epNum, onValueChange = { epNum = it }, label = { Text("Episode #") }, singleLine = true)
                    OutlinedTextField(value = epTitle, onValueChange = { epTitle = it }, label = { Text("Episode Title") }, singleLine = true)
                    OutlinedTextField(value = epUrl, onValueChange = { epUrl = it }, label = { Text("Episode Stream URL") }, singleLine = true)
                    OutlinedTextField(value = epSub, onValueChange = { epSub = it }, label = { Text("Subtitle URL (optional)") }, singleLine = true)
                    Button(
                        onClick = {
                            if (selectedSeriesId.isNotBlank() && epUrl.isNotBlank()) {
                                viewModel.addEpisode(
                                    seriesId = selectedSeriesId,
                                    seasonNumber = seasonNum.toIntOrNull() ?: 1,
                                    episodeNumber = epNum.toIntOrNull() ?: 1,
                                    title = epTitle,
                                    streamUrl = epUrl,
                                    subtitleUrl = epSub
                                )
                                epTitle = ""; epUrl = ""; epSub = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NabdCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Add Episode", color = NabdBg) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.confirm, color = NabdCyan) } }
    )
}

private fun formatCompactCount(count: Int): String {
    return if (count >= 1000) {
        "%.1fK".format(count / 1000f)
    } else count.toString()
}
