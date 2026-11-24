package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class ServerType {
    M3U,
    JSON,
    XTREAM
}

@Serializable
enum class StreamHealthStatus {
    UNKNOWN,
    ONLINE,
    OFFLINE
}

@Serializable
enum class ContentType {
    CHANNEL,
    MOVIE,
    EPISODE
}

@Serializable
data class PlayableSource(
    val label: String,
    val url: String,
    val userAgent: String = "",
    val quality: String = "Auto"
)

@Serializable
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["category"]),
        Index(value = ["country"]),
        Index(value = ["language"]),
        Index(value = ["isFavorite"]),
        Index(value = ["name"])
    ]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String = "",
    val streamUrl: String,
    val backupUrlsJson: String = "[]",
    val category: String = "General",
    val country: String = "INT",
    val language: String = "Arabic",
    val tvgId: String = "",
    val userAgent: String = "",
    val serverId: String = "default_iptv_org",
    val isFavorite: Boolean = false,
    val isFeatured: Boolean = false,
    val healthStatus: StreamHealthStatus = StreamHealthStatus.UNKNOWN,
    val viewCount: Int = 0,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "movies",
    indices = [Index(value = ["category"]), Index(value = ["isFavorite"])]
)
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val description: String = "",
    val year: Int = 2025,
    val rating: Float = 8.0f,
    val category: String = "Movies",
    val streamUrl: String,
    val backupUrlsJson: String = "[]",
    val subtitleUrl: String = "",
    val durationMinutes: Int = 120,
    val isFavorite: Boolean = false,
    val isFeatured: Boolean = false,
    val allowDownload: Boolean = true,
    val serverId: String = "manual",
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "series",
    indices = [Index(value = ["category"]), Index(value = ["isFavorite"])]
)
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val description: String = "",
    val year: Int = 2025,
    val rating: Float = 8.5f,
    val category: String = "Series",
    val isFavorite: Boolean = false,
    val isFeatured: Boolean = false,
    val serverId: String = "manual",
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "episodes",
    indices = [Index(value = ["seriesId", "seasonNumber", "episodeNumber"])]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val title: String,
    val streamUrl: String,
    val backupUrlsJson: String = "[]",
    val subtitleUrl: String = "",
    val durationMinutes: Int = 45,
    val thumbnailUrl: String = ""
)

@Serializable
@Entity(tableName = "servers")
data class ServerSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ServerType,
    val url: String,
    val xtreamHost: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = "",
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val lastSyncedAt: Long = 0L,
    val lastStatusOk: Boolean = true
)

@Serializable
@Entity(
    tableName = "epg_programs",
    indices = [Index(value = ["channelTvgId", "startTimeMs", "endTimeMs"])]
)
data class EpgProgramEntity(
    @PrimaryKey val id: String,
    val channelTvgId: String,
    val channelName: String,
    val title: String,
    val description: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long
)

@Serializable
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val contentId: String,
    val contentType: ContentType,
    val title: String,
    val posterUrl: String = "",
    val streamUrl: String,
    val subtitleUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val seriesId: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val contentId: String,
    val title: String,
    val posterUrl: String = "",
    val remoteUrl: String,
    val localFilePath: String = "",
    val progressPercent: Int = 0,
    val status: String = "PENDING", // PENDING, DOWNLOADING, COMPLETED, FAILED
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "category_configs")
data class CategoryConfigEntity(
    @PrimaryKey val name: String,
    val isVisible: Boolean = true,
    val isKidsSafe: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "channel_reports")
data class ChannelReportEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val channelName: String,
    val reason: String,
    val reportedAt: Long = System.currentTimeMillis()
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val servers: List<ServerSourceEntity> = emptyList(),
    val customChannels: List<ChannelEntity> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
    val series: List<SeriesEntity> = emptyList(),
    val episodes: List<EpisodeEntity> = emptyList(),
    val categories: List<CategoryConfigEntity> = emptyList(),
    val announcementText: String = "",
    val epgUrl: String = "",
    val telegramBotUrl: String = "https://t.me/qweqwe2019bot2bot",
    val telegramContactUrl: String = "https://t.me/Abu2Saleh",
    val telegramChannelUrl: String = "https://t.me/pixels_x_mods_apk"
)

@Serializable
data class AdminStats(
    val channelsCount: Int = 0,
    val moviesCount: Int = 0,
    val seriesCount: Int = 0,
    val serversCount: Int = 0
)
