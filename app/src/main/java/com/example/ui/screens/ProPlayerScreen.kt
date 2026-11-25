package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.ui.components.LiveBadge
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NabdBg
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCyan
import com.example.ui.theme.NabdEmerald
import com.example.ui.theme.NabdPink
import com.example.ui.theme.NabdRed
import com.example.ui.theme.NabdSurface
import com.example.ui.theme.NabdSurfaceElevated
import com.example.ui.theme.NabdTextSecondary
import com.example.ui.theme.NabdViolet
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class PlayerSheetType {
    NONE, QUALITY, AUDIO, SUBTITLES, SPEED, SLEEP, CAST
}

@OptIn(UnstableApi::class)
@Composable
fun ProPlayerScreen(
    contentType: String,
    contentId: String,
    seriesId: String,
    isInPipMode: Boolean,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val configuration = LocalConfiguration.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val aspectMode by viewModel.aspectRatioMode.collectAsStateWithLifecycle()
    val audioOnly by viewModel.audioOnly.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepMinutes by viewModel.sleepMinutesRemaining.collectAsStateWithLifecycle()
    val nextEpCountdown by viewModel.nextEpisodeCountdown.collectAsStateWithLifecycle()

    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.playerManager.isBuffering.collectAsStateWithLifecycle()
    val errorMsg by viewModel.playerManager.errorMessage.collectAsStateWithLifecycle()
    val switchingStatus by viewModel.playerManager.switchingStatus.collectAsStateWithLifecycle()
    val videoTracks by viewModel.playerManager.videoTracks.collectAsStateWithLifecycle()
    val audioTracks by viewModel.playerManager.audioTracks.collectAsStateWithLifecycle()
    val subtitleTracks by viewModel.playerManager.subtitleTracks.collectAsStateWithLifecycle()
    val activeSourceIdx by viewModel.playerManager.activeSourceIndex.collectAsStateWithLifecycle()

    var manualFullscreen by remember { mutableStateOf(false) }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFullscreen = isInPipMode || manualFullscreen || (settings.autoFullscreenOnRotate && isLandscape)

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(PlayerSheetType.NONE) }
    var reportConfirmed by remember { mutableStateOf(false) }

    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    var gestureOverlayText by remember { mutableStateOf<String?>(null) }
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.6f
        )
    }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    LaunchedEffect(contentType, contentId, seriesId) {
        viewModel.loadContent(contentType, contentId, seriesId)
    }

    // Track position polling
    LaunchedEffect(isPlaying) {
        while (true) {
            val p = viewModel.playerManager.player
            currentPosMs = p.currentPosition.coerceAtLeast(0L)
            durationMs = p.duration.coerceAtLeast(0L)
            delay(500L)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !isLocked) {
            delay(4000L)
            showControls = false
        }
    }

    // Auto-hide gesture indicator
    LaunchedEffect(gestureOverlayText) {
        if (gestureOverlayText != null) {
            delay(1200L)
            gestureOverlayText = null
        }
    }

    // Immersive system bars in fullscreen
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen && !isInPipMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Save progress & handle background playback setting on exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveCurrentPosition()
            viewModel.pauseIfBackgroundDisabled()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val pipModifier = Modifier.onGloballyPositioned { coords ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            runCatching {
                val builder = PictureInPictureParams.Builder()
                val vSize = viewModel.playerManager.player.videoSize
                if (vSize != VideoSize.UNKNOWN && vSize.width > 0 && vSize.height > 0) {
                    val ratio = (vSize.width.toFloat() / vSize.height.toFloat()).coerceIn(0.42f, 2.38f)
                    builder.setAspectRatio(Rational((ratio * 100).roundToInt(), 100))
                } else {
                    builder.setAspectRatio(Rational(16, 9))
                }
                val rect = coords.boundsInWindow()
                if (rect.width > 0f && rect.height > 0f) {
                    builder.setSourceRectHint(
                        android.graphics.Rect(
                            rect.left.toInt(),
                            rect.top.toInt(),
                            rect.right.toInt(),
                            rect.bottom.toInt()
                        )
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(isPlaying)
                }
                activity.setPictureInPictureParams(builder.build())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBg)
            .let { if (isFullscreen) it else it.windowInsetsPadding(WindowInsets.safeDrawing) }
    ) {
        // Video Surface Area
        Box(
            modifier = pipModifier
                .fillMaxWidth()
                .let { if (isFullscreen) it.fillMaxSize() else it.aspectRatio(16f / 9f) }
                .background(Color.Black)
                .pointerInput(isLocked) {
                    detectTapGestures(
                        onTap = {
                            if (isLocked) {
                                showControls = !showControls
                            } else {
                                showControls = !showControls
                            }
                        },
                        onDoubleTap = { offset ->
                            if (!isLocked && !mediaState.isLive) {
                                val p = viewModel.playerManager.player
                                val isRight = offset.x > size.width / 2f
                                val delta = if (isRight) 10_000L else -10_000L
                                p.seekTo((p.currentPosition + delta).coerceAtLeast(0L))
                                gestureOverlayText = if (isRight) "+10s" else "-10s"
                            } else if (!isLocked) {
                                val p = viewModel.playerManager.player
                                if (p.isPlaying) p.pause() else p.play()
                            }
                        }
                    )
                }
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaNorm = -dragAmount / size.height.coerceAtLeast(1)
                            if (change.position.x < size.width / 2f) {
                                // Brightness on left half
                                brightnessLevel = (brightnessLevel + deltaNorm).coerceIn(0.05f, 1f)
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = brightnessLevel
                                    win.attributes = lp
                                }
                                gestureOverlayText = "☀ ${(brightnessLevel * 100).roundToInt()}%"
                            } else {
                                // Volume on right half
                                val currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val step = if (deltaNorm > 0.02f) 1 else if (deltaNorm < -0.02f) -1 else 0
                                if (step != 0) {
                                    val nextVol = (currVol + step).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                    val pct = ((nextVol.toFloat() / maxVolume) * 100).roundToInt()
                                    gestureOverlayText = "🔊 $pct%"
                                }
                            }
                        }
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.playerManager.player
                        useController = false
                        keepScreenOn = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { pv ->
                    pv.player = viewModel.playerManager.player
                    pv.resizeMode = aspectMode.resizeMode
                    pv.subtitleView?.apply {
                        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, settings.subtitleFontSize)
                        setStyle(
                            CaptionStyleCompat(
                                Color(settings.subtitleColorHex).toArgb(),
                                AndroidColor.argb(140, 9, 12, 26),
                                AndroidColor.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                AndroidColor.BLACK,
                                null
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Audio-only visual placeholder
            if (audioOnly) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NabdBg.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = strings.audioOnly,
                            tint = NabdCyan,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(strings.audioOnly, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Top LIVE badge & buffering / status
            if (!isInPipMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showControls && !isLocked) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                        if (mediaState.isLive) {
                            LiveBadge(text = strings.liveBadge)
                        }
                        if (sleepMinutes != null) {
                            Surface(color = NabdViolet.copy(alpha = 0.85f), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "${sleepMinutes}${strings.minutesShort}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (switchingStatus != null) {
                        Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(50)) {
                            Text(
                                text = if (switchingStatus == "switching") strings.switchingServer else strings.reconnecting,
                                style = MaterialTheme.typography.labelSmall,
                                color = NabdCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Buffering / Error / Gesture Overlay
            if (isBuffering && errorMsg == null) {
                CircularProgressIndicator(
                    color = NabdCyan,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (errorMsg != null) {
                val displayError = when (errorMsg) {
                    "HTTP_401", "HTTP_403" -> strings.playbackUnauthorizedError
                    "HTTP_503", "HTTP_404" -> strings.playbackUnavailableError
                    else -> strings.playbackError
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = displayError,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.playerManager.retryCurrent() },
                            colors = ButtonDefaults.buttonColors(containerColor = NabdViolet)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.retry, color = Color.White)
                        }
                        if (mediaState.sources.size > 1) {
                            OutlinedButton(
                                onClick = {
                                    val nextIdx = (activeSourceIdx + 1) % mediaState.sources.size
                                    viewModel.playerManager.switchSource(nextIdx)
                                },
                                border = BorderStroke(1.dp, NabdCyan)
                            ) {
                                Text(strings.nextSourceLabel, color = NabdCyan)
                            }
                        }
                    }
                }
            }

            if (gestureOverlayText != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = gestureOverlayText.orEmpty(),
                        color = NabdCyan,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            // Next Episode Auto-Play Overlay
            if (nextEpCountdown != null) {
                Surface(
                    color = NabdSurface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, NabdCyan),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${strings.nextEpisodeIn} ${nextEpCountdown}s",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        TextButton(onClick = { viewModel.cancelNextEpisode() }) {
                            Text(strings.cancel, color = NabdPink)
                        }
                    }
                }
            }

            // Lock Screen Unlock Button
            if (isLocked && showControls && !isInPipMode) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { isLocked = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = strings.unlockScreen, tint = NabdCyan)
                        Text(strings.unlockScreen, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Controls Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isLocked && !isInPipMode && errorMsg == null,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                ) {
                    // Center Transport Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!mediaState.isLive) {
                            IconButton(
                                onClick = {
                                    val p = viewModel.playerManager.player
                                    p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White)
                            }
                        }

                        IconButton(
                            onClick = {
                                val p = viewModel.playerManager.player
                                if (p.isPlaying) p.pause() else p.play()
                            },
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(NabdViolet.copy(alpha = 0.85f))
                                .testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        if (!mediaState.isLive) {
                            IconButton(
                                onClick = {
                                    val p = viewModel.playerManager.player
                                    p.seekTo(p.currentPosition + 10_000L)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White)
                            }
                        }
                    }

                    // Bottom Seekbar + Fullscreen
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!mediaState.isLive && durationMs > 0L) {
                            Text(
                                text = formatDuration(currentPosMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Slider(
                                value = currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                                onValueChange = { viewModel.playerManager.player.seekTo(it.toLong()) },
                                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = NabdCyan,
                                    activeTrackColor = NabdCyan,
                                    inactiveTrackColor = NabdBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatDuration(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(NabdCyan)
                            )
                            Spacer(Modifier.width(10.dp))
                        }

                        IconButton(
                            onClick = {
                                val nextFs = !manualFullscreen
                                manualFullscreen = nextFs
                                activity?.requestedOrientation = if (nextFs) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            },
                            modifier = Modifier.testTag("player_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Below Video (Portrait Mode — matches nabd_tv_style.png middle panel)
        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NabdBg),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title + Now Playing + Quick Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaState.title.ifBlank { strings.appName },
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val nowPlaying = mediaState.currentProgram?.title
                            Text(
                                text = if (!nowPlaying.isNullOrBlank()) {
                                    "${strings.nowPlayingPrefix} $nowPlaying"
                                } else {
                                    "${aspectMode.label} • ${speed}x"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = NabdTextSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (mediaState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = strings.addFavorite,
                                tint = if (mediaState.isFavorite) NabdPink else NabdTextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                val url = mediaState.sources.getOrNull(activeSourceIdx)?.url.orEmpty()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${mediaState.title}\n$url")
                                }
                                context.startActivity(Intent.createChooser(intent, strings.share))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = strings.share, tint = Color.White)
                        }
                    }
                }

                // Aspect Ratio + Audio Only + Report pills
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallControlPill(
                            icon = Icons.Default.AspectRatio,
                            label = aspectMode.label,
                            active = aspectMode != com.example.player.AspectRatioMode.FIT,
                            onClick = { viewModel.cycleAspectRatio() },
                            modifier = Modifier.weight(1f)
                        )
                        SmallControlPill(
                            icon = Icons.Default.Headphones,
                            label = strings.audioOnly,
                            active = audioOnly,
                            onClick = { viewModel.toggleAudioOnly() },
                            modifier = Modifier.weight(1f)
                        )
                        if (mediaState.contentType == com.example.data.model.ContentType.CHANNEL) {
                            SmallControlPill(
                                icon = Icons.Default.ReportProblem,
                                label = if (reportConfirmed) strings.reportSent else strings.reportDead,
                                active = reportConfirmed,
                                onClick = {
                                    viewModel.reportCurrentChannel()
                                    reportConfirmed = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2x4 Pro Control Grid (exact nabd_tv_style.png layout!)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProTileButton(Icons.Default.HighQuality, strings.quality, { activeSheet = PlayerSheetType.QUALITY }, Modifier.weight(1f))
                            ProTileButton(Icons.Default.AudioFile, strings.audio, { activeSheet = PlayerSheetType.AUDIO }, Modifier.weight(1f))
                            ProTileButton(Icons.Default.ClosedCaption, strings.subtitles, { activeSheet = PlayerSheetType.SUBTITLES }, Modifier.weight(1f))
                            ProTileButton(Icons.Default.Speed, strings.speed, { activeSheet = PlayerSheetType.SPEED }, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProTileButton(
                                icon = Icons.Default.PictureInPictureAlt,
                                label = strings.pip,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                        runCatching {
                                            activity.enterPictureInPictureMode(
                                                PictureInPictureParams.Builder()
                                                    .setAspectRatio(Rational(16, 9))
                                                    .build()
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ProTileButton(Icons.Default.Cast, strings.cast, { activeSheet = PlayerSheetType.CAST }, Modifier.weight(1f))
                            ProTileButton(
                                icon = Icons.Default.Bedtime,
                                label = if (sleepMinutes != null) "${sleepMinutes}${strings.minutesShort}" else strings.sleep,
                                onClick = { activeSheet = PlayerSheetType.SLEEP },
                                modifier = Modifier.weight(1f)
                            )
                            ProTileButton(Icons.Default.Lock, strings.lock, { isLocked = true; showControls = false }, Modifier.weight(1f))
                        }
                    }
                }

                // Servers / Sources list
                item {
                    Text(
                        text = strings.serversSources,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mediaState.sources.forEachIndexed { idx, src ->
                            val isSelected = idx == activeSourceIdx
                            Surface(
                                color = if (isSelected) NabdSurfaceElevated else NabdSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isSelected) NabdCyan else NabdBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playerManager.switchSource(idx) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = src.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) NabdEmerald else NabdTextSecondary.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pro Control Dialogs / Sheets
    when (activeSheet) {
        PlayerSheetType.QUALITY -> {
            AlertDialog(
                onDismissRequest = { activeSheet = PlayerSheetType.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.quality, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            viewModel.playerManager.selectAutoVideo()
                            activeSheet = PlayerSheetType.NONE
                        }) {
                            Text(strings.serverAuto, color = NabdCyan)
                        }
                        videoTracks.forEach { tr ->
                            TextButton(onClick = {
                                viewModel.playerManager.selectTrack(androidx.media3.common.C.TRACK_TYPE_VIDEO, tr.groupIndex, tr.trackIndex)
                                activeSheet = PlayerSheetType.NONE
                            }) {
                                Text(
                                    text = tr.label + if (tr.isSelected) " ✓" else "",
                                    color = if (tr.isSelected) NabdCyan else Color.White
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSheet = PlayerSheetType.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        PlayerSheetType.AUDIO -> {
            AlertDialog(
                onDismissRequest = { activeSheet = PlayerSheetType.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.audio, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (audioTracks.isEmpty()) {
                            Text("Default Track", color = NabdTextSecondary)
                        } else {
                            audioTracks.forEach { tr ->
                                TextButton(onClick = {
                                    viewModel.playerManager.selectTrack(androidx.media3.common.C.TRACK_TYPE_AUDIO, tr.groupIndex, tr.trackIndex)
                                    activeSheet = PlayerSheetType.NONE
                                }) {
                                    Text(
                                        text = tr.label + if (tr.isSelected) " ✓" else "",
                                        color = if (tr.isSelected) NabdCyan else Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSheet = PlayerSheetType.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        PlayerSheetType.SUBTITLES -> {
            AlertDialog(
                onDismissRequest = { activeSheet = PlayerSheetType.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.subtitles, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = {
                            viewModel.playerManager.disableSubtitles()
                            activeSheet = PlayerSheetType.NONE
                        }) {
                            Text(strings.sleepOff, color = NabdPink)
                        }
                        subtitleTracks.forEach { tr ->
                            TextButton(onClick = {
                                viewModel.playerManager.selectTrack(androidx.media3.common.C.TRACK_TYPE_TEXT, tr.groupIndex, tr.trackIndex)
                                activeSheet = PlayerSheetType.NONE
                            }) {
                                Text(tr.label + if (tr.isSelected) " ✓" else "", color = Color.White)
                            }
                        }
                        Text("${strings.subtitleSize}: ${settings.subtitleFontSize.toInt()}sp", color = Color.White)
                        Slider(
                            value = settings.subtitleFontSize,
                            onValueChange = { viewModel.setSubtitleStyle(it, settings.subtitleColorHex) },
                            valueRange = 12f..30f,
                            colors = SliderDefaults.colors(thumbColor = NabdCyan, activeTrackColor = NabdCyan)
                        )
                        Text(strings.subtitleColor, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(0xFFFFFFFFL, 0xFFFFEB3BL, 0xFF00E5FFL).forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .clickable { viewModel.setSubtitleStyle(settings.subtitleFontSize, hex) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSheet = PlayerSheetType.NONE }) { Text(strings.confirm, color = NabdCyan) }
                }
            )
        }
        PlayerSheetType.SPEED -> {
            AlertDialog(
                onDismissRequest = { activeSheet = PlayerSheetType.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.speed, color = Color.White) },
                text = {
                    Column {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            TextButton(onClick = {
                                viewModel.setPlaybackSpeed(spd)
                                activeSheet = PlayerSheetType.NONE
                            }) {
                                Text(
                                    text = "${spd}x" + if (speed == spd) " ✓" else "",
                                    color = if (speed == spd) NabdCyan else Color.White
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSheet = PlayerSheetType.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        PlayerSheetType.SLEEP -> {
            AlertDialog(
                onDismissRequest = { activeSheet = PlayerSheetType.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.sleep, color = Color.White) },
                text = {
                    Column {
                        TextButton(onClick = { viewModel.setSleepTimer(null); activeSheet = PlayerSheetType.NONE }) {
                            Text(strings.sleepOff, color = NabdPink)
                        }
                        listOf(15, 30, 45, 60).forEach { mins ->
                            TextButton(onClick = { viewModel.setSleepTimer(mins); activeSheet = PlayerSheetType.NONE }) {
                                Text("$mins ${strings.minutesShort}", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeSheet = PlayerSheetType.NONE }) { Text(strings.cancel, color = NabdTextSecondary) }
                }
            )
        }
        PlayerSheetType.CAST -> {
            AlertDialog(
                onDismissRequest = { activeSheet = PlayerSheetType.NONE },
                containerColor = NabdSurface,
                title = { Text(strings.cast, color = Color.White) },
                text = {
                    val streamUrl = mediaState.sources.getOrNull(activeSourceIdx)?.url.orEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = streamUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = {
                                val castIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(streamUrl), "video/*")
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(castIntent, strings.cast))
                                }.onFailure {
                                    Toast.makeText(context, "No external cast player found", Toast.LENGTH_SHORT).show()
                                }
                                activeSheet = PlayerSheetType.NONE
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NabdViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.cast, color = Color.White)
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(onClick = { activeSheet = PlayerSheetType.NONE }) {
                        Text(strings.cancel, color = NabdTextSecondary)
                    }
                }
            )
        }
        PlayerSheetType.NONE -> {}
    }
}

@Composable
private fun ProTileButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NabdSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NabdBorder),
        modifier = modifier
            .height(74.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NabdCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NabdTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SmallControlPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (active) NabdViolet.copy(alpha = 0.25f) else NabdSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) NabdCyan else NabdBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = if (active) NabdCyan else NabdTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) "%d:%02d:%02d".format(hrs, mins, secs) else "%02d:%02d".format(mins, secs)
}

internal tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
