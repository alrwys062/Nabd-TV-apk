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
import com.example.services.AdultContentFilter
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
    companion object {
        const val SERVER_1_ID = "script_server_1"
        const val SERVER_2_ID = "script_server_2"
        const val SERVER_3_ID = "script_server_3"
        const val SERVER_3_ALLOWED_CATS =
            "76,556,62,209,51,13,530,529,63,83,575,528,52,438,439,441,440,442,477,443,444,445,446,447,448,501,455,456,457,458,459,460,461"
    }

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

    private fun defaultScriptServers(): List<ServerSourceEntity> = listOf(
        ServerSourceEntity(
            id = SERVER_1_ID,
            name = "السيرفر 1",
            type = ServerType.XTREAM,
            url = "http://line.irav25.biz:8080",
            xtreamHost = "http://line.irav25.biz:8080",
            xtreamUsername = "542500725351739675",
            xtreamPassword = "8457561352755875",
            allowedCategoryIdsCsv = "",
            isEnabled = true,
            sortOrder = 0
        ),
        ServerSourceEntity(
            id = SERVER_2_ID,
            name = "السيرفر 2",
            type = ServerType.XTREAM,
            url = "http://startseven.tn:2052",
            xtreamHost = "http://startseven.tn:2052",
            xtreamUsername = "09746171",
            xtreamPassword = "15445345",
            allowedCategoryIdsCsv = "",
            isEnabled = true,
            sortOrder = 1
        ),
        ServerSourceEntity(
            id = SERVER_3_ID,
            name = "سيرفر 3 (الباقة المختارة)",
            type = ServerType.XTREAM,
            url = "http://startseven.tn:2052",
            xtreamHost = "http://startseven.tn:2052",
            xtreamUsername = "09746171",
            xtreamPassword = "15445345",
            allowedCategoryIdsCsv = SERVER_3_ALLOWED_CATS,
            isEnabled = true,
            sortOrder = 2
        )
    )

    suspend fun ensureScriptSourcesRegistered() = withContext(Dispatchers.IO) {
        val migrated = settingsStore.isScriptMigrated()
        if (!migrated) {
            dao.wipeAllSourcesAndCatalog()
            dao.insertServers(defaultScriptServers())
            settingsStore.markScriptMigratedAndResetDefaults()
        } else {
            settingsStore.ensureDefaultPin123()
            val existing = dao.getAllServersOnce()
            if (existing.isEmpty()) {
                dao.insertServers(defaultScriptServers())
            }
        }
    }

    suspend fun resetServersToDefault() = withContext(Dispatchers.IO) {
        dao.wipeAllSourcesAndCatalog()
        dao.insertServers(defaultScriptServers())
        settingsStore.markScriptMigratedAndResetDefaults()
    }

    suspend fun refreshAllEnabledServers(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        ensureScriptSourcesRegistered()
        val settings = settingsStore.settingsFlow.first()
        val now = System.currentTimeMillis()
        val refreshIntervalMs = settings.autoRefreshHours * 3600_000L
        val currentCount = dao.observeChannelCount().first()
        val allEnabled = dao.getAllServersOnce().filter { it.isEnabled }

        val serversNeedingSync = if (!force && currentCount > 0 && (now - settings.lastPlaylistRefreshMs) < refreshIntervalMs) {
            allEnabled.filter { srv ->
                !srv.lastStatusOk || dao.getChannelsByServer(srv.id).isEmpty()
            }
        } else {
            allEnabled
        }

        if (serversNeedingSync.isEmpty()) {
            return@withContext Result.success(currentCount)
        }

        var totalImported = 0
        var lastError: Throwable? = null
        val failedServerNames = mutableListOf<String>()
        val xtreamSnapshotCache = mutableMapOf<String, com.example.services.XtreamLiveSnapshot>()

        if (force && settings.remoteJsonSyncUrl.isNotBlank()) {
            runCatching { syncFromRemoteJsonUrl(settings.remoteJsonSyncUrl) }
        }

        for (server in serversNeedingSync) {
            val res = syncSingleServer(server, xtreamSnapshotCache)
            res.onSuccess { count ->
                totalImported += count
            }.onFailure { err ->
                lastError = err
                failedServerNames.add(server.name)
            }
        }

        if (totalImported > 0) {
            settingsStore.setLastPlaylistRefresh(System.currentTimeMillis())
            Result.success(totalImported)
        } else if (currentCount > 0 && failedServerNames.isEmpty()) {
            settingsStore.setLastPlaylistRefresh(System.currentTimeMillis())
            Result.success(currentCount)
        } else if (failedServerNames.isNotEmpty()) {
            Result.failure(
                IllegalStateException("تعذر الاتصال بـ: ${failedServerNames.joinToString(" ، ")}")
            )
        } else {
            Result.failure(lastError ?: IllegalStateException("تعذر الاتصال بالسيرفرات"))
        }
    }

    suspend fun syncSingleServer(
        server: ServerSourceEntity,
        snapshotCache: MutableMap<String, com.example.services.XtreamLiveSnapshot>? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            when (server.type) {
                ServerType.M3U -> {
                    val deleteAdult = settingsStore.settingsFlow.first().adultDeletePermanently
                    val req = Request.Builder()
                        .url(server.url)
                        .header("User-Agent", "IPTVSmartersPlayer")
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        val body = resp.body ?: error("Empty response body")
                        val parseResult = InputStreamReader(body.byteStream(), Charsets.UTF_8).use { reader ->
                            M3uParser.parse(reader, server.id)
                        }
                        val filteredChannels = if (deleteAdult) {
                            parseResult.channels.filterNot { AdultContentFilter.isAdultChannel(it) }
                        } else {
                            parseResult.channels
                        }
                        if (filteredChannels.isEmpty()) error("Empty M3U playlist")
                        dao.replaceServerChannels(server.id, filteredChannels)
                        syncCategoriesFromChannels(filteredChannels)
                        val currentEpg = settingsStore.settingsFlow.first().epgUrl
                        if (currentEpg.isBlank() && !parseResult.detectedEpgUrl.isNullOrBlank()) {
                            settingsStore.setEpgUrl(parseResult.detectedEpgUrl)
                        }
                        dao.upsertServer(
                            server.copy(lastSyncedAt = System.currentTimeMillis(), lastStatusOk = true)
                        )
                        filteredChannels.size
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
                    val deleteAdult = settingsStore.settingsFlow.first().adultDeletePermanently
                    val allowedCats = server.allowedCategoryIdsCsv
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    var channelsSaved = false
                    var moviesSaved = false
                    var seriesSaved = false

                    val result = xtreamClient.fetchCatalog(
                        serverId = server.id,
                        host = server.xtreamHost.ifBlank { server.url },
                        username = server.xtreamUsername,
                        password = server.xtreamPassword,
                        allowedCategoryIds = allowedCats,
                        snapshotCache = snapshotCache,
                        onChannelsReady = { rawChannels ->
                            val clean = if (deleteAdult) {
                                rawChannels.filterNot { AdultContentFilter.isAdultChannel(it) }
                            } else rawChannels
                            if (clean.isNotEmpty()) {
                                dao.replaceServerChannels(server.id, clean)
                                syncCategoriesFromChannels(clean)
                                channelsSaved = true
                            }
                        },
                        onMoviesReady = { rawMovies ->
                            val clean = if (deleteAdult) {
                                rawMovies.filterNot { AdultContentFilter.isAdultMovie(it) }
                            } else rawMovies
                            if (clean.isNotEmpty()) {
                                dao.replaceServerMovies(server.id, clean)
                                moviesSaved = true
                            }
                        },
                        onSeriesReady = { rawSeries ->
                            val clean = if (deleteAdult) {
                                rawSeries.filterNot { AdultContentFilter.isAdultSeries(it) }
                            } else rawSeries
                            if (clean.isNotEmpty()) {
                                dao.replaceServerSeries(server.id, clean)
                                seriesSaved = true
                            }
                        }
                    )
                    val cleanChannels = if (deleteAdult) result.channels.filterNot { AdultContentFilter.isAdultChannel(it) } else result.channels
                    val cleanMovies = if (deleteAdult) result.movies.filterNot { AdultContentFilter.isAdultMovie(it) } else result.movies
                    val cleanSeries = if (deleteAdult) result.series.filterNot { AdultContentFilter.isAdultSeries(it) } else result.series

                    if (!channelsSaved && cleanChannels.isNotEmpty()) {
                        dao.replaceServerChannels(server.id, cleanChannels)
                        syncCategoriesFromChannels(cleanChannels)
                    }
                    if (!moviesSaved && cleanMovies.isNotEmpty()) {
                        dao.replaceServerMovies(server.id, cleanMovies)
                    }
                    if (!seriesSaved && cleanSeries.isNotEmpty()) {
                        dao.replaceServerSeries(server.id, cleanSeries)
                    }
                    if (result.episodes.isNotEmpty()) dao.insertEpisodes(result.episodes)
                    dao.upsertServer(
                        server.copy(lastSyncedAt = System.currentTimeMillis(), lastStatusOk = true)
                    )
                    cleanChannels.size + cleanMovies.size + cleanSeries.size
                }
            }
        }.onFailure {
            dao.upsertServer(server.copy(lastStatusOk = false))
        }
    }

    suspend fun loadSeriesEpisodesIfNeeded(series: SeriesEntity) = withContext(Dispatchers.IO) {
        val existing = dao.getEpisodesForSeriesOnce(series.id)
        if (existing.isNotEmpty()) return@withContext
        val server = dao.getAllServersOnce().firstOrNull { it.id == series.serverId } ?: return@withContext
        if (server.type != ServerType.XTREAM) return@withContext
        val fetched = xtreamClient.fetchSeriesEpisodes(
            serverId = server.id,
            host = server.xtreamHost.ifBlank { server.url },
            username = server.xtreamUsername,
            password = server.xtreamPassword,
            seriesRoomId = series.id
        )
        if (fetched.isNotEmpty()) {
            fetched.chunked(500).forEach { dao.insertEpisodes(it) }
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
            telegramChannelUrl = settings.telegramChannelUrl,
            developerName = settings.developerName,
            developerUrl = settings.developerUrl
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
        if (payload.developerName.isNotBlank() || payload.developerUrl.isNotBlank()) {
            settingsStore.setDeveloperInfo(payload.developerName, payload.developerUrl)
        }
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
        dao.deleteMoviesByServer(id)
        dao.deleteSeriesByServer(id)
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

    suspend fun findFailoverChannelsFor(channel: ChannelEntity): List<ChannelEntity> =
        withContext(Dispatchers.IO) {
            val cleaned = channel.name
                .replace(Regex("[\\[\\]()|•:\\-_]+"), " ")
                .trim()
            val token = cleaned.split(Regex("\\s+"))
                .firstOrNull { it.length >= 3 }
                ?.lowercase()
                ?: cleaned.lowercase()
            if (token.isBlank()) return@withContext emptyList()
            val candidates = dao.findChannelsOnOtherServers(channel.serverId, "%$token%")
            val targetNorm = normalizeChannelName(channel.name)
            candidates.sortedByDescending { candidate ->
                val candNorm = normalizeChannelName(candidate.name)
                when {
                    candNorm == targetNorm -> 3
                    candNorm.contains(targetNorm) || targetNorm.contains(candNorm) -> 2
                    else -> 1
                }
            }.take(3)
        }

    private fun normalizeChannelName(raw: String): String =
        raw.lowercase()
            .replace(Regex("\\b(hd|fhd|uhd|sd|4k|h265|hevc|vip|raw|ar)\\b"), "")
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

    suspend fun purgeAdultContentFromDatabase(): Int = withContext(Dispatchers.IO) {
        val adultChannelIds = dao.observeAllChannels().first()
            .filter { AdultContentFilter.isAdultChannel(it) }
            .map { it.id }
        val adultMovieIds = dao.getAllMoviesOnce()
            .filter { AdultContentFilter.isAdultMovie(it) }
            .map { it.id }
        val adultSeriesIds = dao.getAllSeriesOnce()
            .filter { AdultContentFilter.isAdultSeries(it) }
            .map { it.id }
        val adultCategoryNames = dao.getCategoryConfigsOnce()
            .filter { AdultContentFilter.isAdultCategory(it.name) }
            .map { it.name }

        adultChannelIds.chunked(500).forEach { dao.deleteChannelsByIds(it) }
        adultMovieIds.chunked(500).forEach { dao.deleteMoviesByIds(it) }
        adultSeriesIds.chunked(500).forEach {
            it.forEach { sid -> dao.deleteEpisodesBySeries(sid) }
            dao.deleteSeriesByIds(it)
        }
        adultCategoryNames.chunked(500).forEach { dao.deleteCategoryConfigsByNames(it) }

        adultChannelIds.size + adultMovieIds.size + adultSeriesIds.size
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearEpg()
        settingsStore.setLastPlaylistRefresh(0L)
        runCatching { context.cacheDir.deleteRecursively() }
    }
}
