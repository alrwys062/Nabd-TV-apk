package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.model.PlayableSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

enum class AspectRatioMode(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    RATIO_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    RATIO_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

data class MediaTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean,
    val height: Int = 0
)

@OptIn(UnstableApi::class)
class ProPlayerManager(
    context: Context,
    okHttpClient: OkHttpClient
) {
    private val appContext = context.applicationContext
    private val trackSelector = DefaultTrackSelector(appContext)

    private val fallbackUserAgents = listOf(
        "IPTVSmartersPlayer",
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "VLC/3.0.20 LibVLC/3.0.20"
    )

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()
    private val playerOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .cache(null)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host].orEmpty().filter { it.expiresAt > System.currentTimeMillis() }
            }
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val httpFactory = OkHttpDataSource.Factory(playerOkHttpClient)
        .setUserAgent(fallbackUserAgents.first())
        .setDefaultRequestProperties(
            mapOf(
                "Accept" to "*/*",
                "Connection" to "keep-alive"
            )
        )
    private val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setTrackSelector(trackSelector)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 45_000, 1_500, 3_000)
                .build()
        )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _videoTracks = MutableStateFlow<List<MediaTrackInfo>>(emptyList())
    val videoTracks: StateFlow<List<MediaTrackInfo>> = _videoTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<MediaTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<MediaTrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<MediaTrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<MediaTrackInfo>> = _subtitleTracks.asStateFlow()

    private val _activeSourceIndex = MutableStateFlow(0)
    val activeSourceIndex: StateFlow<Int> = _activeSourceIndex.asStateFlow()

    private val _switchingStatus = MutableStateFlow<String?>(null)
    val switchingStatus: StateFlow<String?> = _switchingStatus.asStateFlow()

    private var sources: List<PlayableSource> = emptyList()
    private var currentTitle: String = ""
    private var currentSubtitleUrl: String = ""
    private var retryCount = 0
    private var uaIndex = 0
    private var lastHttpStatusCode: Int? = null
    var onEndedCallback: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _errorMessage.value = null
                    _switchingStatus.value = null
                    retryCount = 0
                }
                if (playbackState == Player.STATE_ENDED) {
                    onEndedCallback?.invoke()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                parseTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackFailure(error)
            }
        })
    }

    fun loadMedia(
        title: String,
        sourcesList: List<PlayableSource>,
        subtitleUrl: String = "",
        startPositionMs: Long = 0L,
        audioOnly: Boolean = false,
        preferredQuality: String = "Auto"
    ) {
        currentTitle = title
        sources = sourcesList.filter { it.url.isNotBlank() }
        currentSubtitleUrl = subtitleUrl
        retryCount = 0
        uaIndex = 0
        lastHttpStatusCode = null
        _errorMessage.value = null
        _switchingStatus.value = null

        setAudioOnly(audioOnly)
        applyPreferredQuality(preferredQuality)

        if (sources.isNotEmpty()) {
            playSourceAt(0, startPositionMs)
        }
    }

    fun switchSource(index: Int) {
        if (index in sources.indices) {
            retryCount = 0
            uaIndex = 0
            lastHttpStatusCode = null
            _errorMessage.value = null
            val pos = player.currentPosition.coerceAtLeast(0L)
            playSourceAt(index, pos)
        }
    }

    fun retryCurrent() {
        retryCount = 0
        uaIndex = 0
        lastHttpStatusCode = null
        _errorMessage.value = null
        playSourceAt(_activeSourceIndex.value, player.currentPosition.coerceAtLeast(0L))
    }

    private fun playSourceAt(index: Int, startPositionMs: Long) {
        val source = sources.getOrNull(index) ?: return
        _activeSourceIndex.value = index
        val effectiveUserAgent = if (source.userAgent.isNotBlank()) {
            source.userAgent
        } else {
            fallbackUserAgents.getOrElse(uaIndex) { fallbackUserAgents.first() }
        }
        httpFactory.setUserAgent(effectiveUserAgent)

        val mimeType = inferMimeType(source.url)
        val itemBuilder = MediaItem.Builder()
            .setUri(source.url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(currentTitle).build())
        if (mimeType != null) {
            itemBuilder.setMimeType(mimeType)
        }
        if (currentSubtitleUrl.isNotBlank()) {
            val subMime = if (currentSubtitleUrl.endsWith(".vtt", true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(currentSubtitleUrl))
                .setMimeType(subMime)
                .setLanguage("ar")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            itemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        player.setMediaItem(itemBuilder.build())
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
    }

    private fun handlePlaybackFailure(error: PlaybackException) {
        val httpCode = extractHttpStatusCode(error)
        if (httpCode != null) {
            lastHttpStatusCode = httpCode
        }

        if (httpCode == 401 || httpCode == 403) {
            val currentSource = sources.getOrNull(_activeSourceIndex.value)
            if (currentSource != null && currentSource.userAgent.isBlank() && uaIndex + 1 < fallbackUserAgents.size) {
                uaIndex++
                _switchingStatus.value = "reconnecting"
                playSourceAt(_activeSourceIndex.value, 0L)
                return
            }

            switchToNextSourceOrFail(error)
            return
        }

        // For definitive upstream HTTP errors (503, 502, 504, 404, 410, 429, etc.),
        // immediately move to the next backup/failover source instead of re-hammering the same URL.
        if (httpCode != null && httpCode >= 400) {
            switchToNextSourceOrFail(error)
            return
        }

        if (retryCount < 1) {
            retryCount++
            _switchingStatus.value = "reconnecting"
            player.prepare()
            player.play()
            return
        }

        switchToNextSourceOrFail(error)
    }

    private fun switchToNextSourceOrFail(error: PlaybackException) {
        val nextIndex = _activeSourceIndex.value + 1
        if (nextIndex < sources.size) {
            retryCount = 0
            uaIndex = 0
            _switchingStatus.value = "switching"
            playSourceAt(nextIndex, 0L)
        } else {
            _isBuffering.value = false
            _switchingStatus.value = null
            _errorMessage.value = when (lastHttpStatusCode) {
                401 -> "HTTP_401"
                403 -> "HTTP_403"
                404, 410 -> "HTTP_404"
                500, 502, 503, 504 -> "HTTP_503"
                else -> error.localizedMessage ?: "Playback failed"
            }
        }
    }

    private fun extractHttpStatusCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        var depth = 0
        while (cause != null && depth < 10) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
            depth++
        }
        return null
    }

    fun setAudioOnly(audioOnly: Boolean) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, audioOnly)
            .build()
    }

    fun applyPreferredQuality(quality: String) {
        val maxHeight = when (quality) {
            "1080p" -> 1080
            "720p" -> 720
            "480p" -> 480
            "360p" -> 360
            else -> Int.MAX_VALUE
        }
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .build()
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun selectAutoVideo() {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            .build()
    }

    fun selectTrack(trackType: @C.TrackType Int, groupIndex: Int, trackIndex: Int) {
        val groups = player.currentTracks.groups
        val group = groups.getOrNull(groupIndex) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(override)
            .build()
    }

    fun disableSubtitles() {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    private fun parseTracks(tracks: Tracks) {
        val videos = mutableListOf<MediaTrackInfo>()
        val audios = mutableListOf<MediaTrackInfo>()
        val subs = mutableListOf<MediaTrackInfo>()

        tracks.groups.forEachIndexed { gIdx, group ->
            for (tIdx in 0 until group.length) {
                val format = group.getTrackFormat(tIdx)
                val selected = group.isTrackSelected(tIdx)
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        val h = format.height.takeIf { it > 0 } ?: continue
                        val bitrate = if (format.bitrate > 0) " • ${format.bitrate / 1000}kbps" else ""
                        videos.add(MediaTrackInfo(gIdx, tIdx, "${h}p$bitrate", selected, h))
                    }
                    C.TRACK_TYPE_AUDIO -> {
                        val label = format.label ?: format.language?.uppercase() ?: "Audio ${audios.size + 1}"
                        audios.add(MediaTrackInfo(gIdx, tIdx, label, selected))
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val label = format.label ?: format.language?.uppercase() ?: "Subtitle ${subs.size + 1}"
                        subs.add(MediaTrackInfo(gIdx, tIdx, label, selected))
                    }
                }
            }
        }
        _videoTracks.value = videos.distinctBy { it.label }.sortedByDescending { it.height }
        _audioTracks.value = audios
        _subtitleTracks.value = subs
    }

    private fun inferMimeType(url: String): String? {
        val lower = url.lowercase().substringBefore('?')
        return when {
            lower.endsWith(".m3u8") || lower.contains("m3u8") -> MimeTypes.APPLICATION_M3U8
            lower.endsWith(".mpd") || lower.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            lower.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
            lower.endsWith(".mp4") -> MimeTypes.VIDEO_MP4
            lower.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
            else -> null
        }
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun release() {
        player.release()
    }
}
