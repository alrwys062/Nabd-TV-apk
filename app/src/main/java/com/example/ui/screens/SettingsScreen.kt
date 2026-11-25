package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.AppSettings
import com.example.ui.components.DeveloperRightsCard
import com.example.ui.components.PinEntryDialog
import com.example.ui.components.TelegramFollowSection
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet

@Composable
fun SettingsScreen(
    settings: AppSettings,
    availableCategories: List<String>,
    onLanguageChange: (String) -> Unit,
    onQualityChange: (String) -> Unit,
    onDataSaverChange: (Boolean) -> Unit,
    onAudioOnlyChange: (Boolean) -> Unit,
    onAutoFullscreenChange: (Boolean) -> Unit,
    onAutoNextEpisodeChange: (Boolean) -> Unit,
    onBackgroundPlaybackChange: (Boolean) -> Unit,
    onHideDeadChange: (Boolean) -> Unit,
    onAutoRefreshChange: (Int) -> Unit,
    onKidsLockToggle: (Boolean, String?) -> Unit,
    onToggleHiddenCategory: (String) -> Unit,
    onChangeAdminPin: (String) -> Unit,
    onRefreshNow: () -> Unit,
    onClearCache: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var showQualityDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }
    var showAdminPinChangeDialog by remember { mutableStateOf(false) }
    var showKidsPinSetupDialog by remember { mutableStateOf(false) }
    var showKidsPinVerifyDialog by remember { mutableStateOf(false) }
    var showHiddenCategoriesDialog by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onBackgroundPlaybackChange(granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.navSettings,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Open Admin Panel Highlight Card
        item {
            Surface(
                color = NabdViolet.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NabdViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAdminPanel)
                    .testTag("settings_open_admin_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NabdCyan, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.openAdminPanel, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(strings.controlPanel, style = MaterialTheme.typography.bodySmall, color = NabdTextSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NabdCyan)
                }
            }
        }

        // Language Toggle
        item {
            SettingsActionCard(
                icon = Icons.Default.Language,
                title = strings.languageSetting,
                subtitle = if (settings.language == "ar") strings.arabicLabel else strings.englishLabel,
                onClick = {
                    val next = if (settings.language == "ar") "en" else "ar"
                    onLanguageChange(next)
                },
                testTag = "settings_language_toggle"
            )
        }

        // Default Quality & Auto-Refresh
        item {
            SettingsActionCard(
                icon = Icons.Default.Tune,
                title = strings.defaultQualitySetting,
                subtitle = settings.defaultQuality,
                onClick = { showQualityDialog = true }
            )
        }

        item {
            SettingsActionCard(
                icon = Icons.Default.Refresh,
                title = strings.autoRefreshSetting,
                subtitle = "${settings.autoRefreshHours}h",
                onClick = { showRefreshDialog = true }
            )
        }

        // Player & Stream Toggles
        item {
            Surface(
                color = NabdSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NabdBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsSwitchRow(strings.dataSaverSetting, settings.dataSaverMode, onDataSaverChange)
                    SettingsSwitchRow(strings.audioOnlySetting, settings.audioOnlyDefault, onAudioOnlyChange)
                    SettingsSwitchRow(strings.autoFullscreenSetting, settings.autoFullscreenOnRotate, onAutoFullscreenChange)
                    SettingsSwitchRow(strings.autoNextEpisodeSetting, settings.autoNextEpisode, onAutoNextEpisodeChange)
                    SettingsSwitchRow(
                        label = strings.backgroundPlaybackSetting,
                        checked = settings.backgroundPlaybackEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@SettingsSwitchRow
                                }
                            }
                            onBackgroundPlaybackChange(enable)
                        }
                    )
                    SettingsSwitchRow(strings.hideDeadChannels, settings.hideDeadChannels, onHideDeadChange)
                }
            }
        }

        // Kids Lock
        item {
            Surface(
                color = NabdSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NabdBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.ChildCare, contentDescription = null, tint = NabdPink)
                            Text(strings.kidsLockSetting, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        }
                        Switch(
                            checked = settings.kidsLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) showKidsPinSetupDialog = true else showKidsPinVerifyDialog = true
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NabdPink)
                        )
                    }
                    OutlinedButton(
                        onClick = { showHiddenCategoriesDialog = true },
                        border = BorderStroke(1.dp, NabdBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${strings.hiddenCategories} (${settings.hiddenCategories.size})",
                            color = NabdCyan
                        )
                    }
                }
            }
        }

        // Admin PIN, Refresh, Clear Cache
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsActionCard(
                    icon = Icons.Default.Key,
                    title = strings.changeAdminPin,
                    subtitle = "••••",
                    onClick = { showAdminPinChangeDialog = true }
                )
                SettingsActionCard(
                    icon = Icons.Default.Refresh,
                    title = strings.refreshPlaylist,
                    subtitle = strings.sourcesSectionTitle,
                    onClick = {
                        onRefreshNow()
                        Toast.makeText(context, strings.refreshing, Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsActionCard(
                    icon = Icons.Default.DeleteSweep,
                    title = strings.clearCache,
                    subtitle = strings.epgGuide,
                    onClick = {
                        onClearCache()
                        Toast.makeText(context, strings.cacheCleared, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Telegram "Follow Us" Section & Developer Rights
        item {
            Spacer(Modifier.height(6.dp))
            TelegramFollowSection(
                botUrl = settings.telegramBotUrl,
                contactUrl = settings.telegramContactUrl,
                channelUrl = settings.telegramChannelUrl
            )
            Spacer(Modifier.height(12.dp))
            DeveloperRightsCard(
                developerName = settings.developerName,
                developerUrl = settings.developerUrl
            )
        }

        // About
        item {
            Surface(
                color = NabdSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NabdBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NabdCyan)
                        Text("${strings.aboutApp} • v1.0", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Text(strings.aboutDescription, style = MaterialTheme.typography.bodySmall, color = NabdTextSecondary)
                }
            }
        }
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = NabdSurface,
            title = { Text(strings.defaultQualitySetting, color = Color.White) },
            text = {
                Column {
                    listOf("Auto", "1080p", "720p", "480p", "360p").forEach { q ->
                        TextButton(onClick = {
                            onQualityChange(q)
                            showQualityDialog = false
                        }) {
                            Text(q + if (settings.defaultQuality == q) " ✓" else "", color = NabdCyan)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text(strings.cancel, color = NabdTextSecondary) }
            }
        )
    }

    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            containerColor = NabdSurface,
            title = { Text(strings.autoRefreshSetting, color = Color.White) },
            text = {
                Column {
                    listOf(6, 12, 24, 72).forEach { hrs ->
                        TextButton(onClick = {
                            onAutoRefreshChange(hrs)
                            showRefreshDialog = false
                        }) {
                            Text("${hrs}h" + if (settings.autoRefreshHours == hrs) " ✓" else "", color = NabdCyan)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRefreshDialog = false }) { Text(strings.cancel, color = NabdTextSecondary) }
            }
        )
    }

    if (showAdminPinChangeDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdminPinChangeDialog = false },
            containerColor = NabdSurface,
            title = { Text(strings.changeAdminPin, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 32) newPin = it },
                    label = { Text(strings.enterPin) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length >= 4) {
                            onChangeAdminPin(newPin)
                            showAdminPinChangeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdViolet)
                ) {
                    Text(strings.confirm, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPinChangeDialog = false }) { Text(strings.cancel, color = NabdTextSecondary) }
            }
        )
    }

    if (showKidsPinSetupDialog) {
        PinEntryDialog(
            title = strings.kidsLockPin,
            expectedPin = null,
            onDismiss = { showKidsPinSetupDialog = false },
            onConfirmed = { entered ->
                val pin = entered.ifBlank { settings.kidsLockPin }
                onKidsLockToggle(true, pin)
                showKidsPinSetupDialog = false
            }
        )
    }

    if (showKidsPinVerifyDialog) {
        PinEntryDialog(
            title = strings.kidsLockPin,
            expectedPin = settings.kidsLockPin,
            onDismiss = { showKidsPinVerifyDialog = false },
            onConfirmed = {
                onKidsLockToggle(false, null)
                showKidsPinVerifyDialog = false
            }
        )
    }

    if (showHiddenCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenCategoriesDialog = false },
            containerColor = NabdSurface,
            title = { Text(strings.hiddenCategories, color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(availableCategories.size) { idx ->
                        val cat = availableCategories[idx]
                        val hidden = settings.hiddenCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleHiddenCategory(cat) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hidden,
                                onCheckedChange = { onToggleHiddenCategory(cat) },
                                colors = CheckboxDefaults.colors(checkedColor = NabdPink)
                            )
                            Text(cat, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenCategoriesDialog = false }) { Text(strings.confirm, color = NabdCyan) }
            }
        )
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NabdBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = NabdCyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NabdTextSecondary, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NabdTextSecondary)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NabdCyan
            )
        )
    }
}
