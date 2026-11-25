package com.example.services

import android.util.JsonReader
import android.util.JsonToken
import com.example.data.model.ChannelEntity
import com.example.data.model.EpisodeEntity
import com.example.data.model.MovieEntity
import com.example.data.model.PlayableSource
import com.example.data.model.SeriesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class XtreamRawLiveItem(
    val streamId: String,
    val categoryId: String,
    val name: String,
    val streamIcon: String,
    val epgChannelId: String,
    val directSource: String,
    val containerExtension: String
)

data class XtreamSyncResult(
    val channels: List<ChannelEntity>,
    val movies: List<MovieEntity>,
    val series: List<SeriesEntity>,
    val episodes: List<EpisodeEntity>
)

data class XtreamLiveSnapshot(
    val liveCatMap: Map<String, String>,
    val liveItems: List<XtreamRawLiveItem>
)

class XtreamClient(httpClient: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    private val xtreamHttp: OkHttpClient = httpClient.newBuilder()
        .cache(null)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host].orEmpty()
                    .filter { it.expiresAt > System.currentTimeMillis() }
            }
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val requestUserAgents = listOf(
        "IPTVSmartersPlayer",
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "VLC/3.0.20 LibVLC/3.0.20"
    )

    suspend fun testConnection(host: String, username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val base = normalizeHost(host)
            val encUser = encodePart(username)
            val encPass = encodePart(password)
            val url = "$base/player_api.php?username=$encUser&password=$encPass"
            val body = getStringOrNull(url) ?: return@withContext false
            runCatching {
                val obj = json.parseToJsonElement(body).jsonObject
                val auth = obj["user_info"]?.jsonObject?.get("auth")?.jsonPrimitive?.intOrNull
                auth == 1
            }.getOrDefault(false)
        }

    suspend fun fetchCatalog(
        serverId: String,
        host: String,
        username: String,
        password: String,
        allowedCategoryIds: List<String> = emptyList(),
        skipVodAndSeries: Boolean = allowedCategoryIds.isNotEmpty(),
        snapshotCache: MutableMap<String, XtreamLiveSnapshot>? = null,
        onChannelsReady: suspend (List<ChannelEntity>) -> Unit = {},
        onMoviesReady: suspend (List<MovieEntity>) -> Unit = {},
        onSeriesReady: suspend (List<SeriesEntity>) -> Unit = {}
    ): XtreamSyncResult = withContext(Dispatchers.IO) {
        val base = normalizeHost(host)
        val cacheKey = "$base|${username.trim()}|${password.trim()}"

        // 1. Resolve live categories + live streams via streaming JsonReader (low RAM footprint)
        val cachedSnapshot = snapshotCache?.get(cacheKey)
        val liveCatMap: Map<String, String>
        val liveItems: List<XtreamRawLiveItem>?
        var m3uFallbackChannels: List<ChannelEntity>? = null

        if (cachedSnapshot != null) {
            liveCatMap = cachedSnapshot.liveCatMap
            liveItems = cachedSnapshot.liveItems
        } else {
            val fetchedCats = fetchCategories(base, username, password, "get_live_categories")
            liveCatMap = dhsScriptCategoryDefaults + fetchedCats

            var fetchedLive = fetchLiveRawStreaming(base, username, password)
            if (fetchedLive == null && allowedCategoryIds.isNotEmpty()) {
                fetchedLive = fetchLiveByCategoriesStreaming(base, username, password, allowedCategoryIds)
            }
            liveItems = fetchedLive

            if (liveItems != null && liveItems.isNotEmpty()) {
                snapshotCache?.put(cacheKey, XtreamLiveSnapshot(liveCatMap, liveItems))
            } else {
                // Last-resort fallback: M3U get.php endpoint
                val m3uUrl = "$base/get.php?username=${encodePart(username)}&password=${encodePart(password)}&type=m3u_plus&output=ts"
                val m3uRaw = getStringOrNull(m3uUrl)
                if (!m3uRaw.isNullOrBlank() && m3uRaw.contains("#EXTINF", ignoreCase = true)) {
                    val parsed = M3uParser.parse(StringReader(m3uRaw), serverId)
                    val allowedNames = allowedCategoryIds
                        .mapNotNull { liveCatMap[it] ?: dhsScriptCategoryDefaults[it] }
                        .map { it.lowercase() }
                        .toSet()
                    m3uFallbackChannels = if (allowedNames.isEmpty()) {
                        parsed.channels
                    } else {
                        parsed.channels.filter { ch ->
                            allowedNames.contains(ch.category.trim().lowercase())
                        }
                    }
                }
            }
        }

        val channels = when {
            liveItems != null -> buildChannels(serverId, base, username, password, liveCatMap, liveItems, allowedCategoryIds)
            m3uFallbackChannels != null -> m3uFallbackChannels
            else -> emptyList()
        }

        if (channels.isNotEmpty()) {
            onChannelsReady(channels)
        }

        // 2. Fetch VOD & Series sequentially in isolated blocks so neither blocks Live Channels
        var movies: List<MovieEntity> = emptyList()
        var series: List<SeriesEntity> = emptyList()
        if (!skipVodAndSeries) {
            movies = runCatching {
                val vodCatMap = fetchCategories(base, username, password, "get_vod_categories")
                fetchVodStreaming(serverId, base, username, password, vodCatMap, maxItems = 1500).orEmpty()
            }.getOrDefault(emptyList())

            if (movies.isNotEmpty()) {
                onMoviesReady(movies)
            }

            series = runCatching {
                val seriesCatMap = fetchCategories(base, username, password, "get_series_categories")
                fetchSeriesStreaming(serverId, base, username, password, seriesCatMap, maxItems = 600).orEmpty()
            }.getOrDefault(emptyList())

            if (series.isNotEmpty()) {
                onSeriesReady(series)
            }
        }

        if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
            throw IllegalStateException("تعذر جلب البيانات من السيرفر ($base)")
        }

        XtreamSyncResult(channels, movies, series, emptyList())
    }

    suspend fun fetchSeriesEpisodes(
        serverId: String,
        host: String,
        username: String,
        password: String,
        seriesRoomId: String
    ): List<EpisodeEntity> = withContext(Dispatchers.IO) {
        val prefix = "${serverId}_series_"
        if (!seriesRoomId.startsWith(prefix)) return@withContext emptyList()
        val rawSeriesId = seriesRoomId.removePrefix(prefix)
        val base = normalizeHost(host)
        val encUser = encodePart(username)
        val encPass = encodePart(password)
        val url = "$base/player_api.php?username=$encUser&password=$encPass&action=get_series_info&series_id=${encodePart(rawSeriesId)}"
        val body = getStringOrNull(url) ?: return@withContext emptyList()
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return@withContext emptyList()
        val episodesElem = root["episodes"] ?: return@withContext emptyList()

        val result = mutableListOf<EpisodeEntity>()
        if (episodesElem is JsonObject) {
            episodesElem.forEach { (seasonKey, arrElem) ->
                val seasonNum = seasonKey.toIntOrNull() ?: 1
                val arr = arrElem as? JsonArray ?: return@forEach
                arr.forEachIndexed { idx, epElem ->
                    val obj = epElem as? JsonObject ?: return@forEachIndexed
                    parseEpisodeObj(obj, base, encUser, encPass, seriesRoomId, seasonNum, idx + 1)?.let {
                        result.add(it)
                    }
                }
            }
        } else if (episodesElem is JsonArray) {
            episodesElem.forEachIndexed { idx, epElem ->
                val obj = epElem as? JsonObject ?: return@forEachIndexed
                val seasonNum = obj["season"]?.jsonPrimitive?.intOrNull ?: 1
                parseEpisodeObj(obj, base, encUser, encPass, seriesRoomId, seasonNum, idx + 1)?.let {
                    result.add(it)
                }
            }
        }
        result.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
    }

    private fun parseEpisodeObj(
        obj: JsonObject,
        base: String,
        encUser: String,
        encPass: String,
        seriesRoomId: String,
        fallbackSeason: Int,
        fallbackEpisode: Int
    ): EpisodeEntity? {
        val epId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val epNum = obj["episode_num"]?.jsonPrimitive?.intOrNull ?: fallbackEpisode
        val seasonNum = obj["season"]?.jsonPrimitive?.intOrNull ?: fallbackSeason
        val title = obj["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: "Episode $epNum"
        val ext = obj["container_extension"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "mp4"
        val infoObj = obj["info"] as? JsonObject
        val thumb = infoObj?.get("movie_image")?.jsonPrimitive?.contentOrNull.orEmpty()
        val durationSec = infoObj?.get("duration_secs")?.jsonPrimitive?.intOrNull ?: 0
        val streamUrl = "$base/series/$encUser/$encPass/$epId.$ext"
        return EpisodeEntity(
            id = "${seriesRoomId}_ep_${epId}",
            seriesId = seriesRoomId,
            seasonNumber = seasonNum,
            episodeNumber = epNum,
            title = title,
            streamUrl = streamUrl,
            durationMinutes = (durationSec / 60).takeIf { it > 0 } ?: 45,
            thumbnailUrl = thumb
        )
    }

    private suspend fun fetchCategories(
        base: String,
        user: String,
        pass: String,
        action: String
    ): Map<String, String> {
        val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=$action"
        return streamJsonUrl(url) { reader ->
            if (reader.peek() != JsonToken.BEGIN_ARRAY) return@streamJsonUrl null
            val map = LinkedHashMap<String, String>()
            reader.beginArray()
            while (reader.hasNext()) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    continue
                }
                var id = ""
                var name = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "category_id" -> id = readScalarString(reader)
                        "category_name" -> name = readScalarString(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    map[id] = name
                }
            }
            reader.endArray()
            map
        }.orEmpty()
    }

    private suspend fun fetchLiveRawStreaming(
        base: String,
        user: String,
        pass: String,
        categoryId: String? = null
    ): List<XtreamRawLiveItem>? {
        val catParam = if (!categoryId.isNullOrBlank()) "&category_id=${encodePart(categoryId)}" else ""
        val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=get_live_streams$catParam"
        return streamJsonUrl(url) { reader ->
            if (reader.peek() != JsonToken.BEGIN_ARRAY) return@streamJsonUrl null
            val list = ArrayList<XtreamRawLiveItem>(256)
            reader.beginArray()
            while (reader.hasNext()) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    continue
                }
                var streamId = ""
                var catId = ""
                var name = ""
                var icon = ""
                var epgId = ""
                var directSource = ""
                var containerExt = ""

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id" -> streamId = readScalarString(reader)
                        "category_id" -> catId = readScalarString(reader)
                        "name" -> name = readScalarString(reader)
                        "stream_icon" -> icon = readScalarString(reader)
                        "epg_channel_id" -> epgId = readScalarString(reader)
                        "direct_source" -> directSource = readScalarString(reader)
                        "container_extension" -> containerExt = readScalarString(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (streamId.isNotEmpty()) {
                    list.add(
                        XtreamRawLiveItem(
                            streamId = streamId,
                            categoryId = catId,
                            name = name,
                            streamIcon = icon,
                            epgChannelId = epgId,
                            directSource = directSource,
                            containerExtension = containerExt
                        )
                    )
                }
            }
            reader.endArray()
            list
        }
    }

    private suspend fun fetchLiveByCategoriesStreaming(
        base: String,
        user: String,
        pass: String,
        categoryIds: List<String>
    ): List<XtreamRawLiveItem>? = coroutineScope {
        val semaphore = Semaphore(3)
        val lists = categoryIds.map { catId ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    fetchLiveRawStreaming(base, user, pass, catId).orEmpty()
                }
            }
        }.awaitAll().flatten()

        lists.takeIf { it.isNotEmpty() }
    }

    private suspend fun fetchVodStreaming(
        serverId: String,
        base: String,
        user: String,
        pass: String,
        categoryMap: Map<String, String>,
        maxItems: Int
    ): List<MovieEntity>? {
        val encUser = encodePart(user)
        val encPass = encodePart(pass)
        val nowMs = System.currentTimeMillis()
        val url = "$base/player_api.php?username=$encUser&password=$encPass&action=get_vod_streams"
        return streamJsonUrl(url) { reader ->
            if (reader.peek() != JsonToken.BEGIN_ARRAY) return@streamJsonUrl null
            val list = ArrayList<MovieEntity>(256)
            reader.beginArray()
            while (reader.hasNext() && list.size < maxItems) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    continue
                }
                var streamId = ""
                var name = ""
                var ext = ""
                var icon = ""
                var catId = ""
                var ratingStr = ""
                var rating5Str = ""
                var addedStr = ""

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "stream_id" -> streamId = readScalarString(reader)
                        "name" -> name = readScalarString(reader)
                        "container_extension" -> ext = readScalarString(reader)
                        "stream_icon" -> icon = readScalarString(reader)
                        "category_id" -> catId = readScalarString(reader)
                        "rating" -> ratingStr = readScalarString(reader)
                        "rating_5based" -> rating5Str = readScalarString(reader)
                        "added" -> addedStr = readScalarString(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (streamId.isNotEmpty()) {
                    val safeName = name.ifBlank { "Movie $streamId" }
                    val safeExt = ext.ifBlank { "mp4" }.trimStart('.').ifBlank { "mp4" }
                    val catName = categoryMap[catId]?.takeIf { it.isNotBlank() } ?: "Movies"
                    val rawRating = ratingStr.toFloatOrNull()
                        ?: rating5Str.toFloatOrNull()?.times(2f)
                        ?: 7.5f
                    val addedSec = addedStr.toLongOrNull()
                    val addedMs = if (addedSec != null && addedSec > 0L) addedSec * 1000L else nowMs
                    list.add(
                        MovieEntity(
                            id = "${serverId}_vod_$streamId",
                            title = safeName,
                            posterUrl = icon,
                            backdropUrl = icon,
                            rating = rawRating.coerceIn(0f, 10f),
                            category = catName,
                            streamUrl = "$base/movie/$encUser/$encPass/$streamId.$safeExt",
                            serverId = serverId,
                            addedAt = addedMs
                        )
                    )
                }
            }
            list
        }
    }

    private suspend fun fetchSeriesStreaming(
        serverId: String,
        base: String,
        user: String,
        pass: String,
        categoryMap: Map<String, String>,
        maxItems: Int
    ): List<SeriesEntity>? {
        val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=get_series"
        return streamJsonUrl(url) { reader ->
            if (reader.peek() != JsonToken.BEGIN_ARRAY) return@streamJsonUrl null
            val list = ArrayList<SeriesEntity>(128)
            reader.beginArray()
            while (reader.hasNext() && list.size < maxItems) {
                if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    continue
                }
                var sid = ""
                var name = ""
                var cover = ""
                var plot = ""
                var catId = ""
                var ratingStr = ""
                var rating5Str = ""

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "series_id" -> sid = readScalarString(reader)
                        "name" -> name = readScalarString(reader)
                        "cover" -> cover = readScalarString(reader)
                        "plot" -> plot = readScalarString(reader)
                        "category_id" -> catId = readScalarString(reader)
                        "rating" -> ratingStr = readScalarString(reader)
                        "rating_5based" -> rating5Str = readScalarString(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (sid.isNotEmpty()) {
                    val safeName = name.ifBlank { "Series $sid" }
                    val catName = categoryMap[catId]?.takeIf { it.isNotBlank() } ?: "Series"
                    val rawRating = ratingStr.toFloatOrNull()
                        ?: rating5Str.toFloatOrNull()?.times(2f)
                        ?: 8.0f
                    list.add(
                        SeriesEntity(
                            id = "${serverId}_series_$sid",
                            title = safeName,
                            posterUrl = cover,
                            backdropUrl = cover,
                            description = plot,
                            rating = rawRating.coerceIn(0f, 10f),
                            category = catName,
                            serverId = serverId
                        )
                    )
                }
            }
            list
        }
    }

    private fun buildChannels(
        serverId: String,
        base: String,
        user: String,
        pass: String,
        categoryMap: Map<String, String>,
        items: List<XtreamRawLiveItem>,
        allowedCategoryIds: List<String>
    ): List<ChannelEntity> {
        val encUser = encodePart(user)
        val encPass = encodePart(pass)
        val allowSet = allowedCategoryIds.toSet()
        val orderLookup = allowedCategoryIds.withIndex().associate { it.value to it.index }

        val parsed = items.mapIndexedNotNull { idx, item ->
            if (allowSet.isNotEmpty() && !allowSet.contains(item.categoryId)) {
                return@mapIndexedNotNull null
            }

            val streamId = item.streamId
            val catId = item.categoryId
            val name = item.name.ifBlank { "Live $streamId" }
            val catName = categoryMap[catId]?.takeIf { it.isNotBlank() }
                ?: if (catId.isNotBlank()) "قسم $catId" else "General"

            val rawExt = item.containerExtension.trimStart('.').ifBlank { "ts" }
            val m3u8Url = "$base/live/$encUser/$encPass/$streamId.m3u8"
            val tsUrl = "$base/live/$encUser/$encPass/$streamId.$rawExt"
            val legacyUrl = "$base/$encUser/$encPass/$streamId"
            val preferTsPrimary = allowSet.isNotEmpty()
            val primaryUrl = if (preferTsPrimary) tsUrl else m3u8Url
            val backupSources = buildList {
                if (preferTsPrimary) {
                    add(PlayableSource(label = "Server HLS (Backup)", url = m3u8Url, quality = "Auto"))
                } else {
                    add(PlayableSource(label = "Server 2 - TS", url = tsUrl, quality = "TS"))
                }
                add(PlayableSource(label = "Server 3 - Direct", url = legacyUrl, quality = "Auto"))
                if (item.directSource.isNotBlank() &&
                    item.directSource != primaryUrl &&
                    item.directSource != m3u8Url &&
                    item.directSource != tsUrl &&
                    item.directSource != legacyUrl
                ) {
                    add(PlayableSource(label = "Direct Source", url = item.directSource, quality = "Auto"))
                }
            }
            val backupsJson = json.encodeToString(backupSources)
            val catOrder = orderLookup[catId]?.times(10_000) ?: 0

            ChannelEntity(
                id = "${serverId}_live_$streamId",
                name = name,
                logoUrl = item.streamIcon,
                streamUrl = primaryUrl,
                backupUrlsJson = backupsJson,
                category = catName,
                country = "AR",
                language = "Arabic",
                tvgId = item.epgChannelId,
                serverId = serverId,
                sortOrder = catOrder + idx
            )
        }
        return if (allowSet.isNotEmpty()) parsed.sortedBy { it.sortOrder } else parsed
    }

    private fun readScalarString(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString().trim()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            JsonToken.NULL -> {
                reader.nextNull()
                ""
            }
            else -> {
                reader.skipValue()
                ""
            }
        }
    }

    private suspend fun <T> streamJsonUrl(url: String, parser: (JsonReader) -> T?): T? {
        for ((idx, ua) in requestUserAgents.withIndex()) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header("Accept", "application/json, text/plain, */*")
                .header("Connection", if (idx == 0) "keep-alive" else "close")
                .build()
            val parsed = runCatching {
                xtreamHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body ?: return@use null
                    JsonReader(BufferedReader(body.charStream(), 16 * 1024)).use { reader ->
                        reader.isLenient = true
                        parser(reader)
                    }
                }
            }.getOrNull()
            if (parsed != null) {
                return parsed
            }
            if (idx + 1 < requestUserAgents.size) {
                delay(250L)
            }
        }
        return null
    }

    private suspend fun getStringOrNull(url: String): String? {
        for ((idx, ua) in requestUserAgents.withIndex()) {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header("Accept", "application/json, text/plain, */*")
                .header("Connection", if (idx == 0) "keep-alive" else "close")
                .build()
            val body = runCatching {
                xtreamHttp.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            }.getOrNull()
            if (!body.isNullOrBlank()) {
                return body
            }
            if (idx + 1 < requestUserAgents.size) {
                delay(250L)
            }
        }
        return null
    }

    private fun normalizeHost(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }

    private fun encodePart(raw: String): String =
        runCatching { URLEncoder.encode(raw, "UTF-8") }.getOrDefault(raw)

    private val dhsScriptCategoryDefaults = mapOf(
        "76" to "MUSIC",
        "556" to "SERIE TN  LIVE",
        "62" to "MBC MYHD",
        "209" to "MAROC",
        "51" to "ALGERIE",
        "13" to "NEWS",
        "530" to "DRAMA ARAB",
        "529" to "MOVIE ARAB",
        "63" to "EGYPT",
        "83" to "LEBANON",
        "575" to "SHAHID",
        "528" to "KING",
        "52" to "SPORT ARAB",
        "438" to "TUNISIE*",
        "439" to "BEINSPORTS*",
        "441" to "BEINSPORTS H265*",
        "440" to "BEIN ENTERTAINMENT*",
        "442" to "FRANCE*",
        "477" to "FRANCE H265*",
        "443" to "CARTOON*",
        "444" to "SPORT ARAB*",
        "445" to "ALGERIE*",
        "446" to "MAROC*",
        "447" to "MBC ROTANA*",
        "448" to "OSN-ART*",
        "501" to "CHALLENGE ONDEMAND*",
        "455" to "EGYPT*",
        "456" to "NEWS*",
        "457" to "ISLAM*",
        "458" to "LIBAN",
        "459" to "SYRIA*",
        "460" to "MUSIQUE*",
        "461" to "ARABE DIVERS*"
    )
}
