package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioTelemetry
import com.example.mesh.SoundCatchMeshBackground
import com.example.model.Track
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(
    track: Track,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    telemetry: AudioTelemetry,
    isSoundCatchEnabled: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isCachedOffline: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleSoundCatch: () -> Unit,
    onToggleOfflineCache: () -> Unit,
    onOpenInspector: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLyrics by remember { mutableStateOf(false) }

    // Dynamically animated background colors capturing the playing song's picture (blue, crimson, etc.)
    val dynamicDominant by animateColorAsState(
        targetValue = track.dominantColor,
        animationSpec = tween(durationMillis = 700),
        label = "playerDominant"
    )
    val dynamicSecondary by animateColorAsState(
        targetValue = track.secondaryColor,
        animationSpec = tween(durationMillis = 700),
        label = "playerSecondary"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetAshGrayDark)
            .testTag("full_player_sheet")
    ) {
        // Sound Catch background live under the player
        SoundCatchMeshBackground(
            dominantColor = dynamicDominant,
            secondaryColor = dynamicSecondary,
            audioTelemetry = telemetry,
            isPlaying = isPlaying,
            isSoundCatchEnabled = isSoundCatchEnabled
        )

        // Dynamic atmospheric gradient flood directly capturing the music's cover color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            dynamicDominant.copy(alpha = 0.40f),
                            dynamicSecondary.copy(alpha = 0.20f),
                            Color.Transparent,
                            VelvetAshGrayDark.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 44.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Collapse",
                        tint = VelvetTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextTertiary
                    )
                    Text(
                        text = track.album,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = VelvetTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onOpenInspector,
                    modifier = Modifier.testTag("open_sound_catch_inspector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Sound Catch Mesh Inspector",
                        tint = if (isSoundCatchEnabled) dynamicDominant else VelvetTextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Central Area: Artwork with dynamic colored glow or Synced Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!showLyrics) {
                    // Ambient halo capturing the picture's color
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .background(dynamicDominant.copy(alpha = 0.25f))
                    )

                    // Album Artwork Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(26.dp))
                            .border(1.2.dp, dynamicDominant.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                            .background(VelvetSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = track.coverResId),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    SyncedLyricsView(
                        lyrics = track.lyrics,
                        currentPositionMs = playbackPositionMs,
                        onSeekTo = onSeekTo
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Track metadata & Action pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        fontSize = 14.sp,
                        color = VelvetTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lyrics toggle
                    IconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier.testTag("toggle_lyrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Toggle Lyrics",
                            tint = if (showLyrics) dynamicDominant else VelvetTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Offline cache button
                    IconButton(
                        onClick = onToggleOfflineCache,
                        modifier = Modifier.testTag("toggle_offline_cache_button")
                    ) {
                        Icon(
                            imageVector = if (isCachedOffline) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                            contentDescription = "Offline Cache",
                            tint = if (isCachedOffline) dynamicDominant else VelvetTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SLIM PROGRESS BAR (Very slim, no crossing line!)
            SlimMusicProgressBar(
                positionMs = playbackPositionMs,
                durationMs = track.durationMs,
                activeColor = dynamicDominant,
                onSeekTo = onSeekTo,
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(playbackPositionMs),
                    fontSize = 12.sp,
                    color = VelvetTextTertiary
                )
                Text(
                    text = formatMs(track.durationMs),
                    fontSize = 12.sp,
                    color = VelvetTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PLAYBACK CONTROLS ROW (Enlarged curved buttons with unique colors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button (Curved frosted enclosure)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isShuffle) dynamicDominant.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isShuffle) dynamicDominant.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleShuffle() }
                        .testTag("player_shuffle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) dynamicDominant else VelvetTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous Button (Enlarged with curved edges - not sharp)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    dynamicDominant.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(1.2.dp, dynamicDominant.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                        .clickable { onSkipPrevious() }
                        .testTag("player_previous_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = VelvetTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause central button (Curved squircle with unique radiant theme glow)
                Box(
                    modifier = Modifier
                        .size(width = 78.dp, height = 64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    dynamicDominant,
                                    dynamicDominant.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .clickable { onTogglePlayPause() }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button (Enlarged with curved edges - not sharp)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    dynamicDominant.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(1.2.dp, dynamicDominant.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                        .clickable { onSkipNext() }
                        .testTag("player_next_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = VelvetTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat button (Curved frosted enclosure)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isRepeat) dynamicDominant.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isRepeat) dynamicDominant.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleRepeat() }
                        .testTag("player_repeat_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) dynamicDominant else VelvetTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Catalog source and Sound Catch status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VelvetSurfaceElevated)
                        .border(1.dp, VelvetBorder, RoundedCornerShape(20.dp))
                        .clickable { onOpenInspector() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("sound_catch_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSoundCatchEnabled) dynamicDominant else VelvetTextTertiary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSoundCatchEnabled) "Sound Catch Active: ${telemetry.pipelineLatencyMs}ms Fast Snap" else "Sound Catch Paused",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = VelvetTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ultra-slim, elegant progress scrubber bar with NO crossing line!
 * Supports both smooth tapping and dragging with instant seek feedback.
 */
@Composable
fun SlimMusicProgressBar(
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isDragging) dragFraction else progressFraction

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp) // Generous touch target for easy tapping and dragging
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekTo((newFraction * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekTo((dragFraction * durationMs).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = newFraction
                    }
                )
            }
            .testTag("player_progress_slider"),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val currentProgressWidth = (widthPx * displayFraction).coerceIn(0f, widthPx)

        // 1. Inactive Background Track (Very slim 3.5dp, rounded smooth ends)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )

        // 2. Active Progress Track (Slim 3.5dp, dynamic vibrant gradient)
        Box(
            modifier = Modifier
                .width(with(LocalDensity.current) { currentProgressWidth.toDp() })
                .height(3.5.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.65f),
                            activeColor,
                            Color.White
                        )
                    )
                )
        )

        // 3. Sleek glowing round bead on tip - NO CROSSING LINE!
        // Smoothly rides on top of the slim track
        val beadSize = if (isDragging) 11.dp else 8.5.dp
        Box(
            modifier = Modifier
                .padding(
                    start = with(LocalDensity.current) {
                        (currentProgressWidth - (beadSize.toPx() / 2f)).coerceAtLeast(0f).toDp()
                    }
                )
                .size(beadSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.2.dp, activeColor, CircleShape)
        )
    }
}

fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
