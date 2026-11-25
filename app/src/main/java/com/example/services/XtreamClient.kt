package com.example.services

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
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class XtreamSyncResult(
    val channels: List<ChannelEntity>,
    val movies: List<MovieEntity>,
    val series: List<SeriesEntity>,
    val episodes: List<EpisodeEntity>
)

data class XtreamLiveSnapshot(
    val liveCatMap: Map<String, String>,
    val liveArr: JsonArray
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
        snapshotCache: MutableMap<String, XtreamLiveSnapshot>? = null
    ): XtreamSyncResult = withContext(Dispatchers.IO) {
        val base = normalizeHost(host)
        val cacheKey = "$base|${username.trim()}|${password.trim()}"

        // 1. Resolve live categories + live streams (reuse snapshot if same account already fetched)
        val cachedSnapshot = snapshotCache?.get(cacheKey)
        val liveCatMap: Map<String, String>
        val liveArr: JsonArray?
        var m3uFallbackChannels: List<ChannelEntity>? = null

        if (cachedSnapshot != null) {
            liveCatMap = cachedSnapshot.liveCatMap
            liveArr = cachedSnapshot.liveArr
        } else {
            val fetchedCats = fetchCategories(base, username, password, "get_live_categories")
            liveCatMap = dhsScriptCategoryDefaults + fetchedCats

            var fetchedLive = fetchLiveRaw(base, username, password)
            if (fetchedLive == null && allowedCategoryIds.isNotEmpty()) {
                // Fallback matching dhs.html: fetch per category_id in small batches
                fetchedLive = fetchLiveByCategories(base, username, password, allowedCategoryIds)
            }
            liveArr = fetchedLive

            if (liveArr != null) {
                snapshotCache?.put(cacheKey, XtreamLiveSnapshot(liveCatMap, liveArr))
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
            liveArr != null -> buildChannels(serverId, base, username, password, liveCatMap, liveArr, allowedCategoryIds)
            m3uFallbackChannels != null -> m3uFallbackChannels
            else -> emptyList()
        }

        // 2. Fetch VOD & Series sequentially (after live) to avoid overwhelming Xtream panels with 6 concurrent requests
        var movies: List<MovieEntity> = emptyList()
        var series: List<SeriesEntity> = emptyList()
        if (!skipVodAndSeries) {
            val vodCatMap = fetchCategories(base, username, password, "get_vod_categories")
            val vodArr = fetchVodRaw(base, username, password)
            movies = buildMovies(serverId, base, username, password, vodCatMap, vodArr)

            val seriesCatMap = fetchCategories(base, username, password, "get_series_categories")
            val seriesArr = fetchSeriesRaw(base, username, password)
            series = buildSeries(serverId, seriesCatMap, seriesArr)
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
        val body = getStringOrNull(url) ?: return emptyMap()
        val arr = runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull() ?: return emptyMap()
        val map = LinkedHashMap<String, String>(arr.size)
        arr.forEach { elem ->
            val obj = elem as? JsonObject ?: return@forEach
            val id = obj["category_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val name = obj["category_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (id.isNotEmpty() && name.isNotEmpty()) {
                map[id] = name
            }
        }
        return map
    }

    private suspend fun fetchLiveRaw(base: String, user: String, pass: String): JsonArray? {
        val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=get_live_streams"
        val body = getStringOrNull(url) ?: return null
        return runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull()
    }

    private suspend fun fetchLiveByCategories(
        base: String,
        user: String,
        pass: String,
        categoryIds: List<String>
    ): JsonArray? = coroutineScope {
        val semaphore = Semaphore(3)
        val lists = categoryIds.map { catId ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=get_live_streams&category_id=${encodePart(catId)}"
                    val body = getStringOrNull(url) ?: return@withPermit emptyList()
                    runCatching { json.parseToJsonElement(body) as? JsonArray }
                        .getOrNull()
                        ?.toList()
                        .orEmpty()
                }
            }
        }.awaitAll().flatten()

        if (lists.isEmpty()) null else JsonArray(lists)
    }

    private suspend fun fetchVodRaw(base: String, user: String, pass: String): JsonArray? {
        val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=get_vod_streams"
        val body = getStringOrNull(url) ?: return null
        return runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull()
    }

    private suspend fun fetchSeriesRaw(base: String, user: String, pass: String): JsonArray? {
        val url = "$base/player_api.php?username=${encodePart(user)}&password=${encodePart(pass)}&action=get_series"
        val body = getStringOrNull(url) ?: return null
        return runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull()
    }

    private fun buildChannels(
        serverId: String,
        base: String,
        user: String,
        pass: String,
        categoryMap: Map<String, String>,
        arr: JsonArray?,
        allowedCategoryIds: List<String>
    ): List<ChannelEntity> {
        if (arr == null) return emptyList()
        val encUser = encodePart(user)
        val encPass = encodePart(pass)
        val allowSet = allowedCategoryIds.toSet()
        val orderLookup = allowedCategoryIds.withIndex().associate { it.value to it.index }

        val parsed = arr.mapIndexedNotNull { idx, elem ->
            val obj = elem as? JsonObject ?: return@mapIndexedNotNull null
            val streamId = obj["stream_id"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@mapIndexedNotNull null
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (allowSet.isNotEmpty() && !allowSet.contains(catId)) {
                return@mapIndexedNotNull null
            }

            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "Live $streamId" }
                ?: "Live $streamId"
            val icon = obj["stream_icon"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val epgId = obj["epg_channel_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val catName = categoryMap[catId]?.takeIf { it.isNotBlank() }
                ?: if (catId.isNotBlank()) "قسم $catId" else "General"

            val directSource = obj["direct_source"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val rawExt = obj["container_extension"]?.jsonPrimitive?.contentOrNull?.trim()?.trimStart('.')?.takeIf { it.isNotBlank() } ?: "ts"
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
                if (directSource.isNotBlank() &&
                    directSource != primaryUrl &&
                    directSource != m3u8Url &&
                    directSource != tsUrl &&
                    directSource != legacyUrl
                ) {
                    add(PlayableSource(label = "Direct Source", url = directSource, quality = "Auto"))
                }
            }
            val backupsJson = json.encodeToString(backupSources)
            val catOrder = orderLookup[catId]?.times(10_000) ?: 0

            ChannelEntity(
                id = "${serverId}_live_$streamId",
                name = name,
                logoUrl = icon,
                streamUrl = primaryUrl,
                backupUrlsJson = backupsJson,
                category = catName,
                country = "AR",
                language = "Arabic",
                tvgId = epgId,
                serverId = serverId,
                sortOrder = catOrder + idx
            )
        }
        return if (allowSet.isNotEmpty()) parsed.sortedBy { it.sortOrder } else parsed
    }

    private fun buildMovies(
        serverId: String,
        base: String,
        user: String,
        pass: String,
        categoryMap: Map<String, String>,
        arr: JsonArray?
    ): List<MovieEntity> {
        if (arr == null) return emptyList()
        val encUser = encodePart(user)
        val encPass = encodePart(pass)
        return arr.take(1500).mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val streamId = obj["stream_id"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "Movie $streamId" }
                ?: "Movie $streamId"
            val ext = obj["container_extension"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "mp4" } ?: "mp4"
            val icon = obj["stream_icon"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val catName = categoryMap[catId]?.takeIf { it.isNotBlank() } ?: "Movies"
            val rawRating = obj["rating"]?.jsonPrimitive?.floatOrNull
                ?: obj["rating_5based"]?.jsonPrimitive?.floatOrNull?.times(2f)
                ?: 7.5f
            val addedSec = obj["added"]?.jsonPrimitive?.longOrNull
            val addedMs = if (addedSec != null && addedSec > 0L) addedSec * 1000L else System.currentTimeMillis()
            val movieUrl = "$base/movie/$encUser/$encPass/$streamId.$ext"

            MovieEntity(
                id = "${serverId}_vod_$streamId",
                title = name,
                posterUrl = icon,
                backdropUrl = icon,
                rating = rawRating.coerceIn(0f, 10f),
                category = catName,
                streamUrl = movieUrl,
                serverId = serverId,
                addedAt = addedMs
            )
        }
    }

    private fun buildSeries(
        serverId: String,
        categoryMap: Map<String, String>,
        arr: JsonArray?
    ): List<SeriesEntity> {
        if (arr == null) return emptyList()
        return arr.take(600).mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val sid = obj["series_id"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "Series $sid" }
                ?: "Series $sid"
            val cover = obj["cover"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val plot = obj["plot"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val catId = obj["category_id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val catName = categoryMap[catId]?.takeIf { it.isNotBlank() } ?: "Series"
            val rawRating = obj["rating"]?.jsonPrimitive?.floatOrNull
                ?: obj["rating_5based"]?.jsonPrimitive?.floatOrNull?.times(2f)
                ?: 8.0f

            SeriesEntity(
                id = "${serverId}_series_$sid",
                title = name,
                posterUrl = cover,
                backdropUrl = cover,
                description = plot,
                rating = rawRating.coerceIn(0f, 10f),
                category = catName,
                serverId = serverId
            )
        }
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
