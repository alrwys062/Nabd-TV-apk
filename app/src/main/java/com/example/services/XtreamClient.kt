package com.example.services

import com.example.data.model.ChannelEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.SeriesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class XtreamSyncResult(
    val channels: List<ChannelEntity>,
    val movies: List<MovieEntity>,
    val series: List<SeriesEntity>,
    val episodes: List<EpisodeEntity>
)

class XtreamClient(private val httpClient: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun testConnection(host: String, username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val base = normalizeHost(host)
            val url = "$base/player_api.php?username=$username&password=$password"
            val req = Request.Builder().url(url).build()
            runCatching {
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use false
                    val body = resp.body?.string().orEmpty()
                    val obj = json.parseToJsonElement(body).jsonObject
                    val auth = obj["user_info"]?.jsonObject?.get("auth")?.jsonPrimitive?.intOrNull
                    auth == 1
                }
            }.getOrDefault(false)
        }

    suspend fun fetchCatalog(
        serverId: String,
        host: String,
        username: String,
        password: String
    ): XtreamSyncResult = withContext(Dispatchers.IO) {
        val base = normalizeHost(host)
        val channels = fetchLiveStreams(serverId, base, username, password)
        val movies = fetchVodStreams(serverId, base, username, password)
        val (series, episodes) = fetchSeriesCatalog(serverId, base, username, password)
        XtreamSyncResult(channels, movies, series, episodes)
    }

    private fun fetchLiveStreams(
        serverId: String,
        base: String,
        user: String,
        pass: String
    ): List<ChannelEntity> {
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_live_streams"
        val body = getString(url) ?: return emptyList()
        val arr = runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull() ?: return emptyList()
        return arr.mapIndexedNotNull { idx, elem ->
            val obj = elem as? JsonObject ?: return@mapIndexedNotNull null
            val streamId = obj["stream_id"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Live $streamId"
            val icon = obj["stream_icon"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val epgId = obj["epg_channel_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            ChannelEntity(
                id = "${serverId}_live_$streamId",
                name = name,
                logoUrl = icon,
                streamUrl = "$base/live/$user/$pass/$streamId.m3u8",
                category = if (catId.isNotBlank()) "Xtream $catId" else "General",
                tvgId = epgId,
                serverId = serverId,
                sortOrder = idx
            )
        }
    }

    private fun fetchVodStreams(
        serverId: String,
        base: String,
        user: String,
        pass: String
    ): List<MovieEntity> {
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_vod_streams"
        val body = getString(url) ?: return emptyList()
        val arr = runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull() ?: return emptyList()
        return arr.take(300).mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val streamId = obj["stream_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Movie $streamId"
            val ext = obj["container_extension"]?.jsonPrimitive?.contentOrNull ?: "mp4"
            val icon = obj["stream_icon"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val rating = obj["rating_5based"]?.jsonPrimitive?.floatOrNull?.times(2f) ?: 7.5f
            MovieEntity(
                id = "${serverId}_vod_$streamId",
                title = name,
                posterUrl = icon,
                backdropUrl = icon,
                rating = rating,
                category = "Movies",
                streamUrl = "$base/movie/$user/$pass/$streamId.$ext",
                serverId = serverId
            )
        }
    }

    private fun fetchSeriesCatalog(
        serverId: String,
        base: String,
        user: String,
        pass: String
    ): Pair<List<SeriesEntity>, List<EpisodeEntity>> {
        val url = "$base/player_api.php?username=$user&password=$pass&action=get_series"
        val body = getString(url) ?: return emptyList<SeriesEntity>() to emptyList()
        val arr = runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull() ?: return emptyList<SeriesEntity>() to emptyList()
        val seriesList = arr.take(150).mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val sid = obj["series_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Series $sid"
            val cover = obj["cover"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val plot = obj["plot"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val rating = obj["rating_5based"]?.jsonPrimitive?.floatOrNull?.times(2f) ?: 8.0f
            SeriesEntity(
                id = "${serverId}_series_$sid",
                title = name,
                posterUrl = cover,
                backdropUrl = cover,
                description = plot,
                rating = rating,
                serverId = serverId
            )
        }
        return seriesList to emptyList()
    }

    private fun getString(url: String): String? {
        val req = Request.Builder().url(url).build()
        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull()
    }

    private fun normalizeHost(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }
}
