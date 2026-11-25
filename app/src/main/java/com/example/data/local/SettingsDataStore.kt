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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nabd_tv_prefs")

data class AppSettings(
    val isFirstLaunch: Boolean = true,
    val language: String = "ar",
    val adminPin: String = DEFAULT_ADMIN_PIN,
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
    val primaryM3uUrl: String = "",
    val epgUrl: String = "",
    val remoteJsonSyncUrl: String = "",
    val announcementText: String = "",
    val telegramBotUrl: String = DEFAULT_TELEGRAM_BOT,
    val telegramContactUrl: String = DEFAULT_TELEGRAM_CONTACT,
    val telegramChannelUrl: String = DEFAULT_TELEGRAM_CHANNEL,
    val developerName: String = DEFAULT_DEVELOPER_NAME,
    val developerUrl: String = DEFAULT_DEVELOPER_URL,
    val activeServerId: String = "all",
    val adultLockEnabled: Boolean = true,
    val adultDeletePermanently: Boolean = false,
    val adultPin: String = DEFAULT_ADULT_PIN
) {
    companion object {
        const val DEFAULT_ADMIN_PIN = "123+098123"
        const val DEFAULT_ADULT_PIN = "5555"
        const val DEFAULT_TELEGRAM_BOT = "https://t.me/qweqwe2019bot2bot"
        const val DEFAULT_TELEGRAM_CONTACT = "https://t.me/Abu2Saleh"
        const val DEFAULT_TELEGRAM_CHANNEL = "https://t.me/pixels_x_mods_apk"
        const val DEFAULT_DEVELOPER_NAME = "Hamodi9005"
        const val DEFAULT_DEVELOPER_URL = "http://t.me/Hamodi9005"
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
        val DEV_NAME = stringPreferencesKey("dev_name")
        val DEV_URL = stringPreferencesKey("dev_url")
        val ACTIVE_SERVER_ID = stringPreferencesKey("active_server_id")
        val MIGRATED_SCRIPT_V4 = booleanPreferencesKey("migrated_script_v4")
        val PIN_RESET_123_V5 = booleanPreferencesKey("pin_reset_123_v5")
        val ADULT_LOCK_ENABLED = booleanPreferencesKey("adult_lock_enabled")
        val ADULT_DELETE_PERMANENTLY = booleanPreferencesKey("adult_delete_permanently")
        val ADULT_PIN = stringPreferencesKey("adult_pin")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val isV4Migrated = prefs[Keys.MIGRATED_SCRIPT_V4] ?: false
        val isPinResetDone = prefs[Keys.PIN_RESET_123_V5] ?: false
        AppSettings(
            isFirstLaunch = prefs[Keys.FIRST_LAUNCH] ?: true,
            language = prefs[Keys.LANGUAGE] ?: "ar",
            adminPin = if (isV4Migrated && isPinResetDone) {
                prefs[Keys.ADMIN_PIN] ?: AppSettings.DEFAULT_ADMIN_PIN
            } else {
                AppSettings.DEFAULT_ADMIN_PIN
            },
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
            primaryM3uUrl = prefs[Keys.PRIMARY_M3U_URL] ?: "",
            epgUrl = prefs[Keys.EPG_URL] ?: "",
            remoteJsonSyncUrl = prefs[Keys.REMOTE_JSON_URL] ?: "",
            announcementText = prefs[Keys.ANNOUNCEMENT_TEXT] ?: "",
            telegramBotUrl = prefs[Keys.TG_BOT] ?: AppSettings.DEFAULT_TELEGRAM_BOT,
            telegramContactUrl = prefs[Keys.TG_CONTACT] ?: AppSettings.DEFAULT_TELEGRAM_CONTACT,
            telegramChannelUrl = prefs[Keys.TG_CHANNEL] ?: AppSettings.DEFAULT_TELEGRAM_CHANNEL,
            developerName = prefs[Keys.DEV_NAME] ?: AppSettings.DEFAULT_DEVELOPER_NAME,
            developerUrl = prefs[Keys.DEV_URL] ?: AppSettings.DEFAULT_DEVELOPER_URL,
            activeServerId = prefs[Keys.ACTIVE_SERVER_ID] ?: "all",
            adultLockEnabled = prefs[Keys.ADULT_LOCK_ENABLED] ?: true,
            adultDeletePermanently = prefs[Keys.ADULT_DELETE_PERMANENTLY] ?: false,
            adultPin = prefs[Keys.ADULT_PIN] ?: AppSettings.DEFAULT_ADULT_PIN
        )
    }

    suspend fun isScriptMigrated(): Boolean {
        return context.dataStore.data.first()[Keys.MIGRATED_SCRIPT_V4] ?: false
    }

    suspend fun markScriptMigratedAndResetDefaults() {
        context.dataStore.edit {
            it[Keys.ADMIN_PIN] = AppSettings.DEFAULT_ADMIN_PIN
            it[Keys.PRIMARY_M3U_URL] = ""
            it[Keys.LAST_REFRESH_MS] = 0L
            it[Keys.TG_BOT] = AppSettings.DEFAULT_TELEGRAM_BOT
            it[Keys.TG_CONTACT] = AppSettings.DEFAULT_TELEGRAM_CONTACT
            it[Keys.TG_CHANNEL] = AppSettings.DEFAULT_TELEGRAM_CHANNEL
            it[Keys.DEV_NAME] = AppSettings.DEFAULT_DEVELOPER_NAME
            it[Keys.DEV_URL] = AppSettings.DEFAULT_DEVELOPER_URL
            it[Keys.ACTIVE_SERVER_ID] = "all"
            it[Keys.MIGRATED_SCRIPT_V4] = true
            it[Keys.PIN_RESET_123_V5] = true
        }
    }

    suspend fun ensureDefaultPin123() {
        val done = context.dataStore.data.first()[Keys.PIN_RESET_123_V5] ?: false
        if (!done) {
            context.dataStore.edit {
                it[Keys.ADMIN_PIN] = AppSettings.DEFAULT_ADMIN_PIN
                it[Keys.PIN_RESET_123_V5] = true
            }
        }
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
        context.dataStore.edit {
            it[Keys.ADMIN_PIN] = pin
            it[Keys.MIGRATED_SCRIPT_V4] = true
            it[Keys.PIN_RESET_123_V5] = true
        }
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

    suspend fun setDeveloperInfo(name: String, url: String) {
        context.dataStore.edit {
            it[Keys.DEV_NAME] = name
            it[Keys.DEV_URL] = url
        }
    }

    suspend fun setActiveServerId(serverId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_SERVER_ID] = serverId }
    }

    suspend fun setAdultLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADULT_LOCK_ENABLED] = enabled }
    }

    suspend fun setAdultDeletePermanently(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.ADULT_DELETE_PERMANENTLY] = enabled
            if (enabled) it[Keys.ADULT_LOCK_ENABLED] = true
        }
    }

    suspend fun setAdultPin(pin: String) {
        context.dataStore.edit { it[Keys.ADULT_PIN] = pin }
    }
}
