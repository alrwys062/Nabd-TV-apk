package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NabdApplication
import com.example.data.local.AppSettings
import com.example.data.model.ContentType
import com.example.data.model.EpgProgramEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.PlayableSource
import com.example.data.repository.NabdRepository
import com.example.player.AspectRatioMode
import com.example.player.ProPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class ActiveMediaUiState(
    val contentType: ContentType = ContentType.CHANNEL,
    val contentId: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val subtitleUrl: String = "",
    val isLive: Boolean = true,
    val isFavorite: Boolean = false,
    val sources: List<PlayableSource> = emptyList(),
    val currentProgram: EpgProgramEntity? = null,
    val nextProgram: EpgProgramEntity? = null,
    val seriesEpisodes: List<EpisodeEntity> = emptyList(),
    val currentEpisodeIndex: Int = -1
)

class PlayerViewModel(
    private val repository: NabdRepository,
    val playerManager: ProPlayerManager
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _mediaState = MutableStateFlow(ActiveMediaUiState())
    val mediaState: StateFlow<ActiveMediaUiState> = _mediaState.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _audioOnly = MutableStateFlow(false)
    val audioOnly: StateFlow<Boolean> = _audioOnly.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepMinutesRemaining = MutableStateFlow<Int?>(null)
    val sleepMinutesRemaining: StateFlow<Int?> = _sleepMinutesRemaining.asStateFlow()

    private val _nextEpisodeCountdown = MutableStateFlow<Int?>(null)
    val nextEpisodeCountdown: StateFlow<Int?> = _nextEpisodeCountdown.asStateFlow()

    private var sleepJob: Job? = null
    private var positionSaveJob: Job? = null
    private var nextEpisodeJob: Job? = null

    init {
        playerManager.onEndedCallback = {
            handlePlaybackEnded()
        }
    }

    fun loadContent(contentTypeStr: String, contentId: String, seriesId: String = "") {
        viewModelScope.launch {
            val prefs = repository.settingsFlow.first()
            val quality = if (prefs.dataSaverMode && prefs.defaultQuality == "Auto") "480p" else prefs.defaultQuality
            _audioOnly.value = prefs.audioOnlyDefault && contentTypeStr == "CHANNEL"

            when (contentTypeStr) {
                "CHANNEL" -> {
                    val ch = repository.getChannelById(contentId) ?: return@launch
                    val backups = parseBackupSources(ch.backupUrlsJson)
                    val failoverChannels = repository.findFailoverChannelsFor(ch)
                    val crossServerSources = failoverChannels.flatMap { alt ->
                        val altBackups = parseBackupSources(alt.backupUrlsJson)
                        listOf(
                            PlayableSource(
                                label = "${alt.name} (Backup Auto)",
                                url = alt.streamUrl,
                                userAgent = alt.userAgent,
                                quality = "Auto"
                            )
                        ) + altBackups.take(1).map { b ->
                            b.copy(label = "${alt.name} (Backup TS)")
                        }
                    }
                    val sources = (listOf(
                        PlayableSource("Server 1 - Auto", ch.streamUrl, ch.userAgent, "Auto")
                    ) + backups + crossServerSources).distinctBy { it.url }
                    val (currProg, nextProg) = repository.getCurrentAndNextProgram(ch.tvgId)
                    _mediaState.value = ActiveMediaUiState(
                        contentType = ContentType.CHANNEL,
                        contentId = ch.id,
                        title = ch.name,
                        posterUrl = ch.logoUrl,
                        isLive = true,
                        isFavorite = ch.isFavorite,
                        sources = sources,
                        currentProgram = currProg,
                        nextProgram = nextProg
                    )
                    playerManager.loadMedia(
                        title = ch.name,
                        sourcesList = sources,
                        audioOnly = _audioOnly.value,
                        preferredQuality = quality
                    )
                    repository.recordWatchHistory(
                        contentId = ch.id,
                        contentType = ContentType.CHANNEL,
                        title = ch.name,
                        posterUrl = ch.logoUrl,
                        streamUrl = ch.streamUrl
                    )
                }
                "MOVIE" -> {
                    val movie = repository.getMovieById(contentId) ?: return@launch
                    val backups = parseBackupSources(movie.backupUrlsJson)
                    val sources = listOf(PlayableSource("Server 1 - Auto", movie.streamUrl)) + backups
                    val resumeMs = repository.getResumePositionMs(movie.id)
                    _mediaState.value = ActiveMediaUiState(
                        contentType = ContentType.MOVIE,
                        contentId = movie.id,
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        subtitleUrl = movie.subtitleUrl,
                        isLive = false,
                        isFavorite = movie.isFavorite,
                        sources = sources
                    )
                    playerManager.loadMedia(
                        title = movie.title,
                        sourcesList = sources,
                        subtitleUrl = movie.subtitleUrl,
                        startPositionMs = resumeMs,
                        audioOnly = false,
                        preferredQuality = quality
                    )
                    startSavingProgress()
                }
                "EPISODE" -> {
                    val episodes = repository.getEpisodesForSeriesOnce(seriesId)
                    val idx = episodes.indexOfFirst { it.id == contentId }.coerceAtLeast(0)
                    val ep = episodes.getOrNull(idx) ?: return@launch
                    val series = repository.getSeriesById(seriesId)
                    val backups = parseBackupSources(ep.backupUrlsJson)
                    val sources = listOf(PlayableSource("Server 1 - Auto", ep.streamUrl)) + backups
                    val resumeMs = repository.getResumePositionMs(ep.id)
                    val displayTitle = if (series != null) {
                        "${series.title} • S${ep.seasonNumber}E${ep.episodeNumber} - ${ep.title}"
                    } else ep.title

                    _mediaState.value = ActiveMediaUiState(
                        contentType = ContentType.EPISODE,
                        contentId = ep.id,
                        title = displayTitle,
                        posterUrl = series?.posterUrl ?: ep.thumbnailUrl,
                        subtitleUrl = ep.subtitleUrl,
                        isLive = false,
                        isFavorite = series?.isFavorite ?: false,
                        sources = sources,
                        seriesEpisodes = episodes,
                        currentEpisodeIndex = idx
                    )
                    playerManager.loadMedia(
                        title = displayTitle,
                        sourcesList = sources,
                        subtitleUrl = ep.subtitleUrl,
                        startPositionMs = resumeMs,
                        audioOnly = false,
                        preferredQuality = quality
                    )
                    startSavingProgress(seriesId, ep.seasonNumber, ep.episodeNumber)
                }
                "DOWNLOAD" -> {
                    val downloads = repository.downloadsFlow.first()
                    val dl = downloads.firstOrNull { it.contentId == contentId } ?: return@launch
                    val uri = "file://${dl.localFilePath}"
                    val sources = listOf(PlayableSource("Offline File", uri))
                    _mediaState.value = ActiveMediaUiState(
                        contentType = ContentType.MOVIE,
                        contentId = dl.contentId,
                        title = dl.title,
                        posterUrl = dl.posterUrl,
                        isLive = false,
                        sources = sources
                    )
                    playerManager.loadMedia(title = dl.title, sourcesList = sources)
                }
            }
        }
    }

    private fun startSavingProgress(seriesId: String = "", season: Int = 0, episode: Int = 0) {
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            while (true) {
                delay(5_000L)
                saveCurrentPosition(seriesId, season, episode)
            }
        }
    }

    fun saveCurrentPosition(seriesId: String = "", season: Int = 0, episode: Int = 0) {
        val state = _mediaState.value
        if (state.contentId.isBlank() || state.isLive) return
        val pos = playerManager.player.currentPosition.coerceAtLeast(0L)
        val dur = playerManager.player.duration.coerceAtLeast(0L)
        viewModelScope.launch {
            repository.recordWatchHistory(
                contentId = state.contentId,
                contentType = state.contentType,
                title = state.title,
                posterUrl = state.posterUrl,
                streamUrl = state.sources.firstOrNull()?.url.orEmpty(),
                subtitleUrl = state.subtitleUrl,
                positionMs = pos,
                durationMs = dur,
                seriesId = seriesId,
                seasonNumber = season,
                episodeNumber = episode
            )
        }
    }

    private fun handlePlaybackEnded() {
        val state = _mediaState.value
        if (state.contentType != ContentType.EPISODE) return
        if (!settings.value.autoNextEpisode) return
        val nextIdx = state.currentEpisodeIndex + 1
        if (nextIdx >= state.seriesEpisodes.size) return

        nextEpisodeJob?.cancel()
        nextEpisodeJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                _nextEpisodeCountdown.value = sec
                delay(1000L)
            }
            _nextEpisodeCountdown.value = null
            val nextEp = state.seriesEpisodes[nextIdx]
            loadContent("EPISODE", nextEp.id, nextEp.seriesId)
        }
    }

    fun cancelNextEpisode() {
        nextEpisodeJob?.cancel()
        _nextEpisodeCountdown.value = null
    }

    fun cycleAspectRatio() {
        val all = AspectRatioMode.entries
        val next = all[(_aspectRatioMode.value.ordinal + 1) % all.size]
        _aspectRatioMode.value = next
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun toggleAudioOnly() {
        val next = !_audioOnly.value
        _audioOnly.value = next
        playerManager.setAudioOnly(next)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        playerManager.setSpeed(speed)
    }

    fun setSubtitleStyle(fontSize: Float, colorHex: Long) {
        viewModelScope.launch {
            repository.settingsStore.setSubtitleStyle(fontSize, colorHex)
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepMinutesRemaining.value = null
            return
        }
        _sleepMinutesRemaining.value = minutes
        sleepJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                _sleepMinutesRemaining.value = remaining
            }
            playerManager.pause()
            _sleepMinutesRemaining.value = null
        }
    }

    fun toggleFavorite() {
        val state = _mediaState.value
        viewModelScope.launch {
            when (state.contentType) {
                ContentType.CHANNEL -> repository.getChannelById(state.contentId)?.let {
                    repository.toggleChannelFavorite(it)
                    _mediaState.value = state.copy(isFavorite = !state.isFavorite)
                }
                ContentType.MOVIE -> repository.getMovieById(state.contentId)?.let {
                    repository.toggleMovieFavorite(it)
                    _mediaState.value = state.copy(isFavorite = !state.isFavorite)
                }
                else -> {}
            }
        }
    }

    fun reportCurrentChannel(reason: String = "Stream offline") {
        val state = _mediaState.value
        if (state.contentType == ContentType.CHANNEL) {
            viewModelScope.launch {
                repository.reportBrokenChannel(state.contentId, state.title, reason)
            }
        }
    }

    fun pauseIfBackgroundDisabled() {
        if (!settings.value.backgroundPlaybackEnabled) {
            playerManager.pause()
        }
    }

    private fun parseBackupSources(rawJson: String): List<PlayableSource> {
        return runCatching {
            json.decodeFromString<List<PlayableSource>>(rawJson)
        }.getOrDefault(emptyList())
    }

    override fun onCleared() {
        saveCurrentPosition()
        positionSaveJob?.cancel()
        nextEpisodeJob?.cancel()
        super.onCleared()
    }

    class Factory(private val app: NabdApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(app.repository, app.playerManager) as T
        }
    }
}
