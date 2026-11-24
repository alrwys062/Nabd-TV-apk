package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nabd_tv_prefs")

data class AppSettings(
    val isFirstLaunch: Boolean = true,
    val language: String = "ar",
    val adminPin: String = "1234",
    val kidsLockEnabled: Boolean = false,
    val kidsLockPin: String = "0000",
    val hiddenCategories: Set<String> = emptySet(),
    val defaultQuality: String = "Auto",
    val hideDeadChannels: Boolean = false,
    val dataSaverMode: Boolean = false,
    val audioOnlyDefault: Boolean = false,
    val autoFullscreenOnRotate: Boolean = true,
    val autoNextEpisode: Boolean = true,
    val backgroundPlaybackEnabled: Boolean = true,
    val subtitleFontSize: Float = 18f,
    val subtitleColorHex: Long = 0xFFFFFFFF,
    val autoRefreshHours: Int = 24,
    val lastPlaylistRefreshMs: Long = 0L,
    val primaryM3uUrl: String = DEFAULT_IPTV_ORG_M3U,
    val epgUrl: String = "",
    val remoteJsonSyncUrl: String = "",
    val announcementText: String = "",
    val telegramBotUrl: String = DEFAULT_TELEGRAM_BOT,
    val telegramContactUrl: String = DEFAULT_TELEGRAM_CONTACT,
    val telegramChannelUrl: String = DEFAULT_TELEGRAM_CHANNEL
) {
    companion object {
        const val DEFAULT_IPTV_ORG_M3U = "https://iptv-org.github.io/iptv/index.m3u"
        const val DEFAULT_TELEGRAM_BOT = "https://t.me/qweqwe2019bot2bot"
        const val DEFAULT_TELEGRAM_CONTACT = "https://t.me/Abu2Saleh"
        const val DEFAULT_TELEGRAM_CHANNEL = "https://t.me/pixels_x_mods_apk"
    }
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LANGUAGE = stringPreferencesKey("language")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
        val KIDS_LOCK_ENABLED = booleanPreferencesKey("kids_lock_enabled")
        val KIDS_LOCK_PIN = stringPreferencesKey("kids_lock_pin")
        val HIDDEN_CATEGORIES = stringSetPreferencesKey("hidden_categories")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val HIDE_DEAD_CHANNELS = booleanPreferencesKey("hide_dead_channels")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val AUDIO_ONLY_DEFAULT = booleanPreferencesKey("audio_only_default")
        val AUTO_FULLSCREEN = booleanPreferencesKey("auto_fullscreen")
        val AUTO_NEXT_EPISODE = booleanPreferencesKey("auto_next_episode")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        val SUBTITLE_COLOR = longPreferencesKey("subtitle_color")
        val AUTO_REFRESH_HOURS = intPreferencesKey("auto_refresh_hours")
        val LAST_REFRESH_MS = longPreferencesKey("last_refresh_ms")
        val PRIMARY_M3U_URL = stringPreferencesKey("primary_m3u_url")
        val EPG_URL = stringPreferencesKey("epg_url")
        val REMOTE_JSON_URL = stringPreferencesKey("remote_json_url")
        val ANNOUNCEMENT_TEXT = stringPreferencesKey("announcement_text")
        val TG_BOT = stringPreferencesKey("tg_bot")
        val TG_CONTACT = stringPreferencesKey("tg_contact")
        val TG_CHANNEL = stringPreferencesKey("tg_channel")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isFirstLaunch = prefs[Keys.FIRST_LAUNCH] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "ar",
            adminPin = prefs[Keys.ADMIN_PIN] ?: "1234",
            kidsLockEnabled = prefs[Keys.KIDS_LOCK_ENABLED] ?: false,
            kidsLockPin = prefs[Keys.KIDS_LOCK_PIN] ?: "0000",
            hiddenCategories = prefs[Keys.HIDDEN_CATEGORIES] ?: emptySet(),
            defaultQuality = prefs[Keys.DEFAULT_QUALITY] ?: "Auto",
            hideDeadChannels = prefs[Keys.HIDE_DEAD_CHANNELS] ?: false,
            dataSaverMode = prefs[Keys.DATA_SAVER] ?: false,
            audioOnlyDefault = prefs[Keys.AUDIO_ONLY_DEFAULT] ?: false,
            autoFullscreenOnRotate = prefs[Keys.AUTO_FULLSCREEN] ?: true,
            autoNextEpisode = prefs[Keys.AUTO_NEXT_EPISODE] ?: true,
            backgroundPlaybackEnabled = prefs[Keys.BACKGROUND_PLAYBACK] ?: true,
            subtitleFontSize = prefs[Keys.SUBTITLE_FONT_SIZE] ?: 18f,
            subtitleColorHex = prefs[Keys.SUBTITLE_COLOR] ?: 0xFFFFFFFF,
            autoRefreshHours = prefs[Keys.AUTO_REFRESH_HOURS] ?: 24,
            lastPlaylistRefreshMs = prefs[Keys.LAST_REFRESH_MS] ?: 0L,
            primaryM3uUrl = prefs[Keys.PRIMARY_M3U_URL] ?: AppSettings.DEFAULT_IPTV_ORG_M3U,
            epgUrl = prefs[Keys.EPG_URL] ?: "",
            remoteJsonSyncUrl = prefs[Keys.REMOTE_JSON_URL] ?: "",
            announcementText = prefs[Keys.ANNOUNCEMENT_TEXT] ?: "",
            telegramBotUrl = prefs[Keys.TG_BOT] ?: AppSettings.DEFAULT_TELEGRAM_BOT,
            telegramContactUrl = prefs[Keys.TG_CONTACT] ?: AppSettings.DEFAULT_TELEGRAM_CONTACT,
            telegramChannelUrl = prefs[Keys.TG_CHANNEL] ?: AppSettings.DEFAULT_TELEGRAM_CHANNEL
        )
    }

    suspend fun completeFirstLaunch(language: String) {
        context.dataStore.edit {
            it[Keys.FIRST_LAUNCH] = false
            it[Keys.LANGUAGE] = language
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setAdminPin(pin: String) {
        context.dataStore.edit { it[Keys.ADMIN_PIN] = pin }
    }

    suspend fun setKidsLock(enabled: Boolean, pin: String? = null) {
        context.dataStore.edit {
            it[Keys.KIDS_LOCK_ENABLED] = enabled
            if (pin != null) it[Keys.KIDS_LOCK_PIN] = pin
        }
    }

    suspend fun setHiddenCategories(categories: Set<String>) {
        context.dataStore.edit { it[Keys.HIDDEN_CATEGORIES] = categories }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { it[Keys.DEFAULT_QUALITY] = quality }
    }

    suspend fun setHideDeadChannels(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_DEAD_CHANNELS] = hide }
    }

    suspend fun setDataSaverMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    suspend fun setAudioOnlyDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_ONLY_DEFAULT] = enabled }
    }

    suspend fun setAutoFullscreen(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_FULLSCREEN] = enabled }
    }

    suspend fun setAutoNextEpisode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_NEXT_EPISODE] = enabled }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun setSubtitleStyle(fontSize: Float, colorHex: Long) {
        context.dataStore.edit {
            it[Keys.SUBTITLE_FONT_SIZE] = fontSize
            it[Keys.SUBTITLE_COLOR] = colorHex
        }
    }

    suspend fun setAutoRefreshHours(hours: Int) {
        context.dataStore.edit { it[Keys.AUTO_REFRESH_HOURS] = hours }
    }

    suspend fun setLastPlaylistRefresh(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_REFRESH_MS] = timestampMs }
    }

    suspend fun setPrimaryM3uUrl(url: String) {
        context.dataStore.edit { it[Keys.PRIMARY_M3U_URL] = url }
    }

    suspend fun setEpgUrl(url: String) {
        context.dataStore.edit { it[Keys.EPG_URL] = url }
    }

    suspend fun setRemoteJsonSyncUrl(url: String) {
        context.dataStore.edit { it[Keys.REMOTE_JSON_URL] = url }
    }

    suspend fun setAnnouncementText(text: String) {
        context.dataStore.edit { it[Keys.ANNOUNCEMENT_TEXT] = text }
    }

    suspend fun setTelegramLinks(bot: String, contact: String, channel: String) {
        context.dataStore.edit {
            it[Keys.TG_BOT] = bot
            it[Keys.TG_CONTACT] = contact
            it[Keys.TG_CHANNEL] = channel
        }
    }
}
