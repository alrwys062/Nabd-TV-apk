package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NabdApplication
import com.example.data.local.AppSettings
import com.example.data.model.AdminStats
import com.example.data.model.CategoryConfigEntity
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelReportEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.PlayableSource
import com.example.data.model.SeriesEntity
import com.example.data.model.ServerSourceEntity
import com.example.data.model.ServerType
import com.example.data.repository.NabdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class AdminViewModel(private val repository: NabdRepository) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val stats: StateFlow<AdminStats> = combine(
        repository.channelCountFlow,
        repository.movieCountFlow,
        repository.seriesCountFlow,
        repository.serverCountFlow
    ) { ch, mv, sr, sv ->
        AdminStats(ch, mv, sr, sv)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminStats())

    val servers: StateFlow<List<ServerSourceEntity>> = repository.serversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> = repository.allChannelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movies: StateFlow<List<MovieEntity>> = repository.allMoviesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val series: StateFlow<List<SeriesEntity>> = repository.allSeriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryConfigEntity>> = repository.categoryConfigsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ChannelReportEntity>> = repository.reportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun clearStatusToast() {
        _statusToast.value = null
    }

    fun observeEpisodes(seriesId: String): Flow<List<EpisodeEntity>> =
        repository.observeEpisodesForSeries(seriesId)

    // Servers
    fun saveServer(
        id: String?,
        name: String,
        type: ServerType,
        url: String,
        xtreamHost: String,
        xtreamUser: String,
        xtreamPass: String,
        enabled: Boolean,
        sortOrder: Int
    ) {
        viewModelScope.launch {
            val server = ServerSourceEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                url = url.trim(),
                xtreamHost = xtreamHost.trim(),
                xtreamUsername = xtreamUser.trim(),
                xtreamPassword = xtreamPass.trim(),
                isEnabled = enabled,
                sortOrder = sortOrder
            )
            repository.upsertServer(server)
            _statusToast.value = "Server saved: ${server.name}"
        }
    }

    fun toggleServerEnabled(server: ServerSourceEntity) {
        viewModelScope.launch {
            repository.upsertServer(server.copy(isEnabled = !server.isEnabled))
        }
    }

    fun moveServerOrder(server: ServerSourceEntity, delta: Int) {
        viewModelScope.launch {
            repository.upsertServer(server.copy(sortOrder = (server.sortOrder + delta).coerceAtLeast(0)))
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            repository.deleteServer(id)
            _statusToast.value = "Server removed"
        }
    }

    fun resetServersToDefault() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.resetServersToDefault()
            val res = repository.refreshAllEnabledServers(force = true)
            _statusToast.value = if (res.isSuccess) {
                "تمت إعادة ضبط السيرفرات وتحديث القنوات"
            } else {
                "تمت إعادة ضبط السيرفرات"
            }
            _isBusy.value = false
        }
    }

    fun testAndSyncServer(server: ServerSourceEntity) {
        viewModelScope.launch {
            _isBusy.value = true
            val ok = repository.testServerConnection(server)
            if (ok) {
                val res = repository.syncSingleServer(server)
                res.onSuccess { count ->
                    _statusToast.value = "Connected! Synced $count items"
                }.onFailure {
                    _statusToast.value = "Connected, sync error: ${it.localizedMessage}"
                }
            } else {
                repository.upsertServer(server.copy(lastStatusOk = false))
                _statusToast.value = "Connection failed for ${server.name}"
            }
            _isBusy.value = false
        }
    }

    // Channels
    fun saveChannel(
        id: String?,
        name: String,
        logoUrl: String,
        streamUrl: String,
        backupUrl: String,
        category: String,
        country: String,
        language: String,
        featured: Boolean
    ) {
        viewModelScope.launch {
            val backups = if (backupUrl.isNotBlank()) {
                json.encodeToString(listOf(PlayableSource("Backup", backupUrl.trim())))
            } else "[]"
            val channel = ChannelEntity(
                id = id ?: "manual_${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                logoUrl = logoUrl.trim(),
                streamUrl = streamUrl.trim(),
                backupUrlsJson = backups,
                category = category.trim().ifBlank { "General" },
                country = country.trim().ifBlank { "INT" },
                language = language.trim().ifBlank { "Arabic" },
                serverId = "manual",
                isFeatured = featured
            )
            repository.upsertChannel(channel)
            _statusToast.value = "Channel saved: ${channel.name}"
        }
    }

    fun deleteChannel(id: String) {
        viewModelScope.launch {
            repository.deleteChannel(id)
        }
    }

    fun toggleChannelFeatured(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.setChannelFeatured(channel.id, !channel.isFeatured)
        }
    }

    // Movies
    fun saveMovie(
        id: String?,
        title: String,
        posterUrl: String,
        description: String,
        year: Int,
        rating: Float,
        category: String,
        streamUrl: String,
        backupUrl: String,
        subtitleUrl: String,
        featured: Boolean
    ) {
        viewModelScope.launch {
            val backups = if (backupUrl.isNotBlank()) {
                json.encodeToString(listOf(PlayableSource("Server 2", backupUrl.trim())))
            } else "[]"
            val movie = MovieEntity(
                id = id ?: "movie_${UUID.randomUUID().toString().take(8)}",
                title = title.trim(),
                posterUrl = posterUrl.trim(),
                backdropUrl = posterUrl.trim(),
                description = description.trim(),
                year = year,
                rating = rating,
                category = category.trim().ifBlank { "Movies" },
                streamUrl = streamUrl.trim(),
                backupUrlsJson = backups,
                subtitleUrl = subtitleUrl.trim(),
                isFeatured = featured
            )
            repository.upsertMovie(movie)
            _statusToast.value = "Movie saved: ${movie.title}"
        }
    }

    fun deleteMovie(id: String) {
        viewModelScope.launch { repository.deleteMovie(id) }
    }

    fun toggleMovieFeatured(movie: MovieEntity) {
        viewModelScope.launch { repository.setMovieFeatured(movie.id, !movie.isFeatured) }
    }

    // Series & Episodes
    fun saveSeries(
        id: String?,
        title: String,
        posterUrl: String,
        description: String,
        year: Int,
        rating: Float,
        category: String,
        featured: Boolean
    ) {
        viewModelScope.launch {
            val s = SeriesEntity(
                id = id ?: "series_${UUID.randomUUID().toString().take(8)}",
                title = title.trim(),
                posterUrl = posterUrl.trim(),
                backdropUrl = posterUrl.trim(),
                description = description.trim(),
                year = year,
                rating = rating,
                category = category.trim().ifBlank { "Series" },
                isFeatured = featured
            )
            repository.upsertSeries(s)
            _statusToast.value = "Series saved: ${s.title}"
        }
    }

    fun deleteSeries(id: String) {
        viewModelScope.launch { repository.deleteSeries(id) }
    }

    fun addEpisode(
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        title: String,
        streamUrl: String,
        subtitleUrl: String
    ) {
        viewModelScope.launch {
            val ep = EpisodeEntity(
                id = "ep_${UUID.randomUUID().toString().take(8)}",
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                title = title.trim().ifBlank { "Episode $episodeNumber" },
                streamUrl = streamUrl.trim(),
                subtitleUrl = subtitleUrl.trim()
            )
            repository.upsertEpisode(ep)
            _statusToast.value = "Episode added"
        }
    }

    fun deleteEpisode(id: String) {
        viewModelScope.launch { repository.deleteEpisode(id) }
    }

    // Import / Export / Sync
    fun importM3uRaw(raw: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val count = repository.importM3uText(raw)
            _statusToast.value = "Imported $count channels"
            _isBusy.value = false
        }
    }

    suspend fun getExportM3uText(): String = repository.exportAllChannelsM3u()

    suspend fun getBackupJsonText(): String = repository.exportBackupJson()

    fun restoreBackupJson(raw: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.importBackupJson(raw)
            _statusToast.value = if (res.isSuccess) "Backup restored" else "Invalid JSON backup"
            _isBusy.value = false
        }
    }

    fun syncRemoteJson(url: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.settingsStore.setRemoteJsonSyncUrl(url.trim())
            if (url.isNotBlank()) {
                val res = repository.syncFromRemoteJsonUrl(url.trim())
                _statusToast.value = if (res.isSuccess) "Remote JSON synced" else "Remote sync failed"
            }
            _isBusy.value = false
        }
    }

    // Categories, Telegram & Announcements
    fun toggleCategoryVisible(config: CategoryConfigEntity) {
        viewModelScope.launch {
            repository.upsertCategoryConfig(config.copy(isVisible = !config.isVisible))
        }
    }

    fun saveTelegramAndAnnouncement(
        botUrl: String,
        contactUrl: String,
        channelUrl: String,
        announcement: String,
        epgUrl: String,
        developerName: String = "Hamodi9005",
        developerUrl: String = "http://t.me/Hamodi9005"
    ) {
        viewModelScope.launch {
            repository.settingsStore.setTelegramLinks(botUrl.trim(), contactUrl.trim(), channelUrl.trim())
            repository.settingsStore.setAnnouncementText(announcement.trim())
            repository.settingsStore.setEpgUrl(epgUrl.trim())
            repository.settingsStore.setDeveloperInfo(
                developerName.trim().ifBlank { "Hamodi9005" },
                developerUrl.trim().ifBlank { "http://t.me/Hamodi9005" }
            )
            _statusToast.value = "Saved & Published"
        }
    }

    fun publishAll() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.refreshAllEnabledServers(force = true)
            _isBusy.value = false
            _statusToast.value = "Saved & Published"
        }
    }

    class Factory(private val app: NabdApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(app.repository) as T
        }
    }
}
