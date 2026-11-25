package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CategoryConfigEntity
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelReportEntity
import com.example.data.model.DownloadEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SearchHistoryEntity
import com.example.data.model.SeriesEntity
import com.example.data.model.ServerSourceEntity
import com.example.data.model.StreamHealthStatus
import com.example.data.model.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NabdDao {

    // Channels
    @Query("SELECT * FROM channels ORDER BY sortOrder ASC, viewCount DESC, name ASC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFeatured = 1 ORDER BY sortOrder ASC LIMIT 10")
    fun observeFeaturedChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY viewCount DESC, name ASC LIMIT 25")
    fun observePopularChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY addedAt DESC LIMIT 25")
    fun observeRecentChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT category FROM channels WHERE category != '' ORDER BY category ASC")
    fun observeChannelCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT country FROM channels WHERE country != '' ORDER BY country ASC")
    fun observeChannelCountries(): Flow<List<String>>

    @Query("SELECT DISTINCT language FROM channels WHERE language != '' ORDER BY language ASC")
    fun observeChannelLanguages(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM channels")
    fun observeChannelCount(): Flow<Int>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE serverId = :serverId")
    suspend fun getChannelsByServer(serverId: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE serverId = 'manual'")
    suspend fun getManualChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE serverId != :excludeServerId AND LOWER(name) LIKE :namePattern LIMIT 15")
    suspend fun findChannelsOnOtherServers(excludeServerId: String, namePattern: String): List<ChannelEntity>

    @Query("SELECT * FROM channels LIMIT :limit OFFSET :offset")
    suspend fun getChannelsBatch(limit: Int, offset: Int): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannel(id: String)

    @Query("DELETE FROM channels WHERE serverId = :serverId")
    suspend fun deleteChannelsByServer(serverId: String)

    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteChannelsByIds(ids: List<String>)

    @Transaction
    suspend fun replaceServerChannels(serverId: String, newChannels: List<ChannelEntity>) {
        val existingFavs = getChannelsByServer(serverId).filter { it.isFavorite }.map { it.id }.toSet()
        deleteChannelsByServer(serverId)
        val merged = newChannels.map {
            if (existingFavs.contains(it.id)) it.copy(isFavorite = true) else it
        }
        merged.chunked(500).forEach { chunk ->
            insertChannels(chunk)
        }
    }

    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id")
    suspend fun setChannelFavorite(id: String, favorite: Boolean)

    @Query("UPDATE channels SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementChannelViewCount(id: String)

    @Query("UPDATE channels SET healthStatus = :status WHERE id = :id")
    suspend fun updateChannelHealth(id: String, status: StreamHealthStatus)

    @Query("UPDATE channels SET isFeatured = :featured WHERE id = :id")
    suspend fun setChannelFeatured(id: String, featured: Boolean)

    // Movies
    @Query("SELECT * FROM movies ORDER BY addedAt DESC")
    fun observeAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY title ASC")
    fun observeFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFeatured = 1 ORDER BY addedAt DESC LIMIT 10")
    fun observeFeaturedMovies(): Flow<List<MovieEntity>>

    @Query("SELECT COUNT(*) FROM movies")
    fun observeMovieCount(): Flow<Int>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    suspend fun getMovieById(id: String): MovieEntity?

    @Query("SELECT * FROM movies")
    suspend fun getAllMoviesOnce(): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovie(movie: MovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM movies WHERE serverId = :serverId")
    suspend fun getMoviesByServer(serverId: String): List<MovieEntity>

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteMovie(id: String)

    @Query("DELETE FROM movies WHERE serverId = :serverId")
    suspend fun deleteMoviesByServer(serverId: String)

    @Query("DELETE FROM movies WHERE id IN (:ids)")
    suspend fun deleteMoviesByIds(ids: List<String>)

    @Transaction
    suspend fun replaceServerMovies(serverId: String, newMovies: List<MovieEntity>) {
        val existingFavs = getMoviesByServer(serverId).filter { it.isFavorite }.map { it.id }.toSet()
        deleteMoviesByServer(serverId)
        val merged = newMovies.map {
            if (existingFavs.contains(it.id)) it.copy(isFavorite = true) else it
        }
        merged.chunked(500).forEach { chunk ->
            insertMovies(chunk)
        }
    }

    @Query("UPDATE movies SET isFavorite = :favorite WHERE id = :id")
    suspend fun setMovieFavorite(id: String, favorite: Boolean)

    @Query("UPDATE movies SET isFeatured = :featured WHERE id = :id")
    suspend fun setMovieFeatured(id: String, featured: Boolean)

    // Series & Episodes
    @Query("SELECT * FROM series ORDER BY addedAt DESC")
    fun observeAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE isFavorite = 1 ORDER BY title ASC")
    fun observeFavoriteSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT COUNT(*) FROM series")
    fun observeSeriesCount(): Flow<Int>

    @Query("SELECT * FROM series WHERE id = :id LIMIT 1")
    suspend fun getSeriesById(id: String): SeriesEntity?

    @Query("SELECT * FROM series")
    suspend fun getAllSeriesOnce(): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeries(series: SeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Query("SELECT * FROM series WHERE serverId = :serverId")
    suspend fun getSeriesByServer(serverId: String): List<SeriesEntity>

    @Query("DELETE FROM series WHERE id = :id")
    suspend fun deleteSeries(id: String)

    @Query("DELETE FROM series WHERE serverId = :serverId")
    suspend fun deleteSeriesByServer(serverId: String)

    @Query("DELETE FROM series WHERE id IN (:ids)")
    suspend fun deleteSeriesByIds(ids: List<String>)

    @Transaction
    suspend fun replaceServerSeries(serverId: String, newSeries: List<SeriesEntity>) {
        val existingFavs = getSeriesByServer(serverId).filter { it.isFavorite }.map { it.id }.toSet()
        deleteSeriesByServer(serverId)
        val merged = newSeries.map {
            if (existingFavs.contains(it.id)) it.copy(isFavorite = true) else it
        }
        merged.chunked(500).forEach { chunk ->
            insertSeries(chunk)
        }
    }

    @Query("UPDATE series SET isFavorite = :favorite WHERE id = :id")
    suspend fun setSeriesFavorite(id: String, favorite: Boolean)

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodesForSeries(seriesId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesForSeriesOnce(seriesId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes")
    suspend fun getAllEpisodesOnce(): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(episode: EpisodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun deleteEpisode(id: String)

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun deleteEpisodesBySeries(seriesId: String)

    // Servers
    @Query("SELECT * FROM servers ORDER BY sortOrder ASC, name ASC")
    fun observeServers(): Flow<List<ServerSourceEntity>>

    @Query("SELECT * FROM servers ORDER BY sortOrder ASC")
    suspend fun getAllServersOnce(): List<ServerSourceEntity>

    @Query("SELECT COUNT(*) FROM servers")
    fun observeServerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServer(server: ServerSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerSourceEntity>)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServer(id: String)

    @Query("DELETE FROM channels")
    suspend fun clearAllChannels()

    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()

    @Query("DELETE FROM series")
    suspend fun clearAllSeries()

    @Query("DELETE FROM episodes")
    suspend fun clearAllEpisodes()

    @Query("DELETE FROM servers")
    suspend fun clearAllServers()

    @Query("DELETE FROM category_configs")
    suspend fun clearAllCategoryConfigs()

    @Transaction
    suspend fun wipeAllSourcesAndCatalog() {
        clearAllChannels()
        clearAllMovies()
        clearAllSeries()
        clearAllEpisodes()
        clearAllServers()
        clearAllCategoryConfigs()
    }

    // EPG
    @Query("SELECT * FROM epg_programs WHERE endTimeMs >= :nowMs ORDER BY startTimeMs ASC LIMIT 500")
    fun observeUpcomingEpg(nowMs: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :tvgId AND startTimeMs <= :nowMs AND endTimeMs >= :nowMs LIMIT 1")
    suspend fun getCurrentProgram(tvgId: String, nowMs: Long): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :tvgId AND startTimeMs > :nowMs ORDER BY startTimeMs ASC LIMIT 1")
    suspend fun getNextProgram(tvgId: String, nowMs: Long): EpgProgramEntity?

    @Query("DELETE FROM epg_programs")
    suspend fun clearEpg()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgPrograms(programs: List<EpgProgramEntity>)

    // Watch History
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC LIMIT 50")
    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentId = :contentId LIMIT 1")
    suspend fun getWatchHistoryItem(contentId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE contentId = :contentId LIMIT 1")
    suspend fun getDownloadById(contentId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE contentId = :contentId")
    suspend fun deleteDownload(contentId: String)

    // Category Configs
    @Query("SELECT * FROM category_configs ORDER BY sortOrder ASC, name ASC")
    fun observeCategoryConfigs(): Flow<List<CategoryConfigEntity>>

    @Query("SELECT * FROM category_configs")
    suspend fun getCategoryConfigsOnce(): List<CategoryConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryConfig(config: CategoryConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryConfigs(configs: List<CategoryConfigEntity>)

    @Query("DELETE FROM category_configs WHERE name IN (:names)")
    suspend fun deleteCategoryConfigsByNames(names: List<String>)

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 12")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchQuery(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Reports
    @Query("SELECT * FROM channel_reports ORDER BY reportedAt DESC")
    fun observeReports(): Flow<List<ChannelReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ChannelReportEntity)
}
