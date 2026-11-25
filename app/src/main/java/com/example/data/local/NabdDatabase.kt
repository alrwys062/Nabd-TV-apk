package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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

class NabdTypeConverters {
    @TypeConverter
    fun fromServerType(value: ServerType): String = value.name

    @TypeConverter
    fun toServerType(value: String): ServerType =
        runCatching { ServerType.valueOf(value) }.getOrDefault(ServerType.M3U)

    @TypeConverter
    fun fromHealthStatus(value: StreamHealthStatus): String = value.name

    @TypeConverter
    fun toHealthStatus(value: String): StreamHealthStatus =
        runCatching { StreamHealthStatus.valueOf(value) }.getOrDefault(StreamHealthStatus.UNKNOWN)

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): ContentType =
        runCatching { ContentType.valueOf(value) }.getOrDefault(ContentType.CHANNEL)
}

@Database(
    entities = [
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class,
        ServerSourceEntity::class,
        EpgProgramEntity::class,
        WatchHistoryEntity::class,
        DownloadEntity::class,
        CategoryConfigEntity::class,
        SearchHistoryEntity::class,
        ChannelReportEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(NabdTypeConverters::class)
abstract class NabdDatabase : RoomDatabase() {
    abstract fun nabdDao(): NabdDao

    companion object {
        @Volatile
        private var INSTANCE: NabdDatabase? = null

        fun getInstance(context: Context): NabdDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NabdDatabase::class.java,
                    "nabd_tv.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
