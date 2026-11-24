package com.example.data.repository

import android.content.Context
import com.example.data.local.AppSettings
import com.example.data.local.NabdDao
import com.example.data.local.SettingsDataStore
import com.example.data.model.BackupPayload
import com.example.data.model.CategoryConfigEntity
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelReportEntity
import com.example.data.model.ContentType
import com.example.data.model.DownloadEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SearchHistoryEntity
import com.example.data.model.SeriesEntity
import com.example.data.model.ServerSourceEntity
import com.example.data.model.ServerType
import com.example.data.model.StreamHealthStatus
import com.example.data.model.WatchHistoryEntity
import com.example.services.ChannelHealthChecker
import com.example.services.DownloadService
import com.example.services.M3uParser
import com.example.services.XmltvParser
import com.example.services.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.io.StringReader
import java.util.UUID

class NabdRepository(
    private val context: Context,
    private val dao: NabdDao,
    val settingsStore: SettingsDataStore,
    val httpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val xtreamClient = XtreamClient(httpClient)
    private val healthChecker = ChannelHealthChecker(httpClient)
    private val downloadService = DownloadService(context, httpClient, dao)

    // Reactive streams
    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
    val allChannelsFlow: Flow<List<ChannelEntity>> = dao.observeAllChannels()
    val featuredChannelsFlow: Flow<List<ChannelEntity>> = dao.observeFeaturedChannels()
    val popularChannelsFlow: Flow<List<ChannelEntity>> = dao.observePopularChannels()
    val recentChannelsFlow: Flow<List<ChannelEntity>> = dao.observeRecentChannels()
    val favoriteChannelsFlow: Flow<List<ChannelEntity>> = dao.observeFavoriteChannels()
    val channelCategoriesFlow: Flow<List<String>> = dao.observeChannelCategories()
    val channelCountriesFlow: Flow<List<String>> = dao.observeChannelCountries()
    val channelLanguagesFlow: Flow<List<String>> = dao.observeChannelLanguages()
    val channelCountFlow: Flow<Int> = dao.observeChannelCount()

    val allMoviesFlow: Flow<List<MovieEntity>> = dao.observeAllMovies()
    val favoriteMoviesFlow: Flow<List<MovieEntity>> = dao.observeFavoriteMovies()
    val featuredMoviesFlow: Flow<List<MovieEntity>> = dao.observeFeaturedMovies()
    val movieCountFlow: Flow<Int> = dao.observeMovieCount()

    val allSeriesFlow: Flow<List<SeriesEntity>> = dao.observeAllSeries()
    val favoriteSeriesFlow: Flow<List<SeriesEntity>> = dao.observeFavoriteSeries()
    val seriesCountFlow: Flow<Int> = dao.observeSeriesCount()

    val serversFlow: Flow<List<ServerSourceEntity>> = dao.observeServers()
    val serverCountFlow: Flow<Int> = dao.observeServerCount()

    val watchHistoryFlow: Flow<List<WatchHistoryEntity>> = dao.observeWatchHistory()
    val downloadsFlow: Flow<List<DownloadEntity>> = dao.observeDownloads()
    val categoryConfigsFlow: Flow<List<CategoryConfigEntity>> = dao.observeCategoryConfigs()
    val searchHistoryFlow: Flow<List<SearchHistoryEntity>> = dao.observeSearchHistory()
    val reportsFlow: Flow<List<ChannelReportEntity>> = dao.observeReports()

    fun observeUpcomingEpg(): Flow<List<EpgProgramEntity>> =
        dao.observeUpcomingEpg(System.currentTimeMillis() - 3600_000L)

    fun observeEpisodesForSeries(seriesId: String): Flow<List<EpisodeEntity>> =
        dao.observeEpisodesForSeries(seriesId)

    suspend fun ensureDefaultServerRegistered() = withContext(Dispatchers.IO) {
        val existing = dao.getAllServersOnce()
        if (existing.none { it.id == "default_iptv_org" }) {
            val primaryUrl = settingsStore.settingsFlow.first().primaryM3uUrl
            dao.upsertServer(
                ServerSourceEntity(
                    id = "default_iptv_org",
                    name = "IPTV-Org Global",
                    type = ServerType.M3U,
                    url = primaryUrl,
                    isEnabled = true,
                    sortOrder = 0
                )
            )
        }
    }

    suspend fun refreshAllEnabledServers(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        ensureDefaultServerRegistered()
        val settings = settingsStore.settingsFlow.first()
        val now = System.currentTimeMillis()
        val refreshIntervalMs = settings.autoRefreshHours * 3600_000L
        val currentCount = dao.observeChannelCount().first()

        if (!force && currentCount > 0 && (now - settings.lastPlaylistRefreshMs) < refreshIntervalMs) {
            return@withContext Result.success(currentCount)
        }

        val servers = dao.getAllServersOnce().filter { it.isEnabled }
        var totalImported = 0
        var lastError: Throwable? = null

        // Also sync remote JSON if configured
        if (settings.remoteJsonSyncUrl.isNotBlank()) {
            runCatching { syncFromRemoteJsonUrl(settings.remoteJsonSyncUrl) }
        }

        for (server in servers) {
            val res = syncSingleServer(server)
            res.onSuccess { count ->
                totalImported += count
            }.onFailure { err ->
                lastError = err
            }
        }

        if (totalImported > 0 || currentCount > 0) {
            settingsStore.setLastPlaylistRefresh(System.currentTimeMillis())
            Result.success(totalImported)
        } else {
            Result.failure(lastError ?: IllegalStateException("Failed to fetch playlists"))
        }
    }

    suspend fun syncSingleServer(server: ServerSourceEntity): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            when (server.type) {
                ServerType.M3U -> {
                    val req = Request.Builder()
                        .url(server.url)
                        .header("User-Agent", "NabdTV/1.0 (Android)")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        val body = resp.body ?: error("Empty response body")
                        val parseResult = InputStreamReader(body.byteStream(), Charsets.UTF_8).use { reader ->
                            M3uParser.parse(reader, server.id)
                        }
                        dao.replaceServerChannels(server.id, parseResult.channels)
                        syncCategoriesFromChannels(parseResult.channels)
                        val currentEpg = settingsStore.settingsFlow.first().epgUrl
                        if (currentEpg.isBlank() && !parseResult.detectedEpgUrl.isNullOrBlank()) {
                            settingsStore.setEpgUrl(parseResult.detectedEpgUrl)
                        }
                        dao.upsertServer(
                            server.copy(lastSyncedAt = System.currentTimeMillis(), lastStatusOk = true)
                        )
                        parseResult.channels.size
                    }
                }
                ServerType.JSON -> {
                    val req = Request.Builder().url(server.url).build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        val raw = resp.body?.string().orEmpty()
                        val payload = json.decodeFromString<BackupPayload>(raw)
                        applyRemotePayload(payload, server.id)
                        dao.upsertServer(
                            server.copy(lastSyncedAt = System.currentTimeMillis(), lastStatusOk = true)
                        )
                        payload.customChannels.size + payload.movies.size
                    }
                }
                ServerType.XTREAM -> {
                    val result = xtreamClient.fetchCatalog(
                        serverId = server.id,
                        host = server.xtreamHost.ifBlank { server.url },
                        username = server.xtreamUsername,
                        password = server.xtreamPassword
                    )
                    dao.replaceServerChannels(server.id, result.channels)
                    if (result.movies.isNotEmpty()) dao.insertMovies(result.movies)
                    if (result.series.isNotEmpty()) dao.insertSeries(result.series)
                    if (result.episodes.isNotEmpty()) dao.insertEpisodes(result.episodes)
                    syncCategoriesFromChannels(result.channels)
                    dao.upsertServer(
                        server.copy(lastSyncedAt = System.currentTimeMillis(), lastStatusOk = true)
                    )
                    result.channels.size + result.movies.size + result.series.size
                }
            }
        }.onFailure {
            dao.upsertServer(server.copy(lastStatusOk = false))
        }
    }

    suspend fun testServerConnection(server: ServerSourceEntity): Boolean = withContext(Dispatchers.IO) {
        when (server.type) {
            ServerType.M3U, ServerType.JSON -> {
                runCatching {
                    val req = Request.Builder().url(server.url).head().build()
                    httpClient.newCall(req).execute().use { it.isSuccessful }
                }.getOrElse {
                    runCatching {
                        val req = Request.Builder().url(server.url).header("Range", "bytes=0-512").get().build()
                        httpClient.newCall(req).execute().use { it.isSuccessful }
                    }.getOrDefault(false)
                }
            }
            ServerType.XTREAM -> {
                xtreamClient.testConnection(
                    server.xtreamHost.ifBlank { server.url },
                    server.xtreamUsername,
                    server.xtreamPassword
                )
            }
        }
    }

    suspend fun importM3uText(rawText: String, serverId: String = "manual"): Int = withContext(Dispatchers.IO) {
        val result = M3uParser.parse(StringReader(rawText), serverId)
        if (result.channels.isNotEmpty()) {
            result.channels.chunked(500).forEach { dao.insertChannels(it) }
            syncCategoriesFromChannels(result.channels)
        }
        result.channels.size
    }

    suspend fun exportAllChannelsM3u(): String = withContext(Dispatchers.IO) {
        val list = dao.observeAllChannels().first()
        M3uParser.exportM3u(list)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsStore.settingsFlow.first()
        val payload = BackupPayload(
            servers = dao.getAllServersOnce(),
            customChannels = dao.getManualChannels(),
            movies = dao.getAllMoviesOnce(),
            series = dao.getAllSeriesOnce(),
            episodes = dao.getAllEpisodesOnce(),
            categories = dao.getCategoryConfigsOnce(),
            announcementText = settings.announcementText,
            epgUrl = settings.epgUrl,
            telegramBotUrl = settings.telegramBotUrl,
            telegramContactUrl = settings.telegramContactUrl,
            telegramChannelUrl = settings.telegramChannelUrl
        )
        json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.decodeFromString<BackupPayload>(rawJson)
            applyRemotePayload(payload)
        }
    }

    suspend fun syncFromRemoteJsonUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val raw = resp.body?.string().orEmpty()
                val payload = json.decodeFromString<BackupPayload>(raw)
                applyRemotePayload(payload)
            }
        }
    }

    private suspend fun applyRemotePayload(payload: BackupPayload, overrideServerId: String? = null) {
        if (payload.servers.isNotEmpty() && overrideServerId == null) {
            dao.insertServers(payload.servers)
        }
        if (payload.customChannels.isNotEmpty()) {
            val list = if (overrideServerId != null) {
                payload.customChannels.map { it.copy(serverId = overrideServerId) }
            } else payload.customChannels
            dao.insertChannels(list)
        }
        if (payload.movies.isNotEmpty()) dao.insertMovies(payload.movies)
        if (payload.series.isNotEmpty()) dao.insertSeries(payload.series)
        if (payload.episodes.isNotEmpty()) dao.insertEpisodes(payload.episodes)
        if (payload.categories.isNotEmpty()) dao.insertCategoryConfigs(payload.categories)
        if (payload.announcementText.isNotBlank()) {
            settingsStore.setAnnouncementText(payload.announcementText)
        }
        if (payload.epgUrl.isNotBlank()) {
            settingsStore.setEpgUrl(payload.epgUrl)
        }
        settingsStore.setTelegramLinks(
            payload.telegramBotUrl,
            payload.telegramContactUrl,
            payload.telegramChannelUrl
        )
    }

    private suspend fun syncCategoriesFromChannels(channels: List<ChannelEntity>) {
        val existing = dao.getCategoryConfigsOnce().associateBy { it.name }
        val newOnes = channels.map { it.category }.distinct().filter { !existing.containsKey(it) }
            .mapIndexed { idx, cat ->
                CategoryConfigEntity(name = cat, isVisible = true, isKidsSafe = true, sortOrder = existing.size + idx)
            }
        if (newOnes.isNotEmpty()) dao.insertCategoryConfigs(newOnes)
    }

    suspend fun loadEpgFromUrl(url: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("Empty EPG")
                val programs = InputStreamReader(body.byteStream(), Charsets.UTF_8).use { reader ->
                    XmltvParser.parse(reader)
                }
                dao.clearEpg()
                programs.chunked(500).forEach { dao.insertEpgPrograms(it) }
                programs.size
            }
        }
    }

    suspend fun getCurrentAndNextProgram(tvgId: String): Pair<EpgProgramEntity?, EpgProgramEntity?> =
        withContext(Dispatchers.IO) {
            if (tvgId.isBlank()) return@withContext null to null
            val now = System.currentTimeMillis()
            dao.getCurrentProgram(tvgId, now) to dao.getNextProgram(tvgId, now)
        }

    suspend fun runHealthCheck(
        channels: List<ChannelEntity>,
        onProgress: (done: Int, total: Int) -> Unit
    ) {
        healthChecker.checkBatch(channels.take(80)) { id, status, done, total ->
            dao.updateChannelHealth(id, status)
            onProgress(done, total)
        }
    }

    suspend fun checkSingleStreamHealth(url: String, userAgent: String = ""): StreamHealthStatus =
        healthChecker.checkStream(url, userAgent)

    // Favorites / History / Reports / Downloads
    suspend fun toggleChannelFavorite(channel: ChannelEntity) =
        dao.setChannelFavorite(channel.id, !channel.isFavorite)

    suspend fun toggleMovieFavorite(movie: MovieEntity) =
        dao.setMovieFavorite(movie.id, !movie.isFavorite)

    suspend fun toggleSeriesFavorite(series: SeriesEntity) =
        dao.setSeriesFavorite(series.id, !series.isFavorite)

    suspend fun recordWatchHistory(
        contentId: String,
        contentType: ContentType,
        title: String,
        posterUrl: String,
        streamUrl: String,
        subtitleUrl: String = "",
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        seriesId: String = "",
        seasonNumber: Int = 0,
        episodeNumber: Int = 0
    ) {
        if (contentType == ContentType.CHANNEL) {
            dao.incrementChannelViewCount(contentId)
        }
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                contentId = contentId,
                contentType = contentType,
                title = title,
                posterUrl = posterUrl,
                streamUrl = streamUrl,
                subtitleUrl = subtitleUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getResumePositionMs(contentId: String): Long =
        dao.getWatchHistoryItem(contentId)?.positionMs ?: 0L

    suspend fun clearHistory() = dao.clearWatchHistory()

    suspend fun saveSearchQuery(query: String) {
        val q = query.trim()
        if (q.isNotEmpty()) dao.upsertSearchQuery(SearchHistoryEntity(query = q))
    }

    suspend fun clearSearchHistory() = dao.clearSearchHistory()

    suspend fun reportBrokenChannel(channelId: String, channelName: String, reason: String) {
        dao.insertReport(
            ChannelReportEntity(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                channelName = channelName,
                reason = reason
            )
        )
        dao.updateChannelHealth(channelId, StreamHealthStatus.OFFLINE)
    }

    suspend fun downloadMovie(movie: MovieEntity) =
        downloadService.downloadMedia(movie.id, movie.title, movie.posterUrl, movie.streamUrl)

    suspend fun removeDownload(item: DownloadEntity) = downloadService.removeDownload(item)

    // Admin CRUD operations
    suspend fun upsertServer(server: ServerSourceEntity) = dao.upsertServer(server)
    suspend fun deleteServer(id: String) {
        dao.deleteChannelsByServer(id)
        dao.deleteServer(id)
    }

    suspend fun upsertChannel(channel: ChannelEntity) {
        dao.upsertChannel(channel)
        syncCategoriesFromChannels(listOf(channel))
    }

    suspend fun deleteChannel(id: String) = dao.deleteChannel(id)
    suspend fun setChannelFeatured(id: String, featured: Boolean) = dao.setChannelFeatured(id, featured)

    suspend fun upsertMovie(movie: MovieEntity) = dao.upsertMovie(movie)
    suspend fun deleteMovie(id: String) = dao.deleteMovie(id)
    suspend fun setMovieFeatured(id: String, featured: Boolean) = dao.setMovieFeatured(id, featured)

    suspend fun upsertSeries(series: SeriesEntity) = dao.upsertSeries(series)
    suspend fun deleteSeries(id: String) {
        dao.deleteEpisodesBySeries(id)
        dao.deleteSeries(id)
    }

    suspend fun upsertEpisode(episode: EpisodeEntity) = dao.upsertEpisode(episode)
    suspend fun deleteEpisode(id: String) = dao.deleteEpisode(id)

    suspend fun upsertCategoryConfig(config: CategoryConfigEntity) = dao.upsertCategoryConfig(config)

    suspend fun getChannelById(id: String) = dao.getChannelById(id)
    suspend fun getMovieById(id: String) = dao.getMovieById(id)
    suspend fun getSeriesById(id: String) = dao.getSeriesById(id)
    suspend fun getEpisodesForSeriesOnce(seriesId: String) = dao.getEpisodesForSeriesOnce(seriesId)

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearEpg()
        settingsStore.setLastPlaylistRefresh(0L)
        runCatching { context.cacheDir.deleteRecursively() }
    }
}
