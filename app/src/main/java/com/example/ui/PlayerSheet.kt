package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioTelemetry
import com.example.mesh.SoundCatchMeshBackground
import com.example.model.Track
import com.example.ui.theme.VelvetActiveGlow
import com.example.ui.theme.VelvetActivePill
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetObsidian
import com.example.ui.theme.VelvetPureBlack
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetAshGrayDark)
            .testTag("full_player_sheet")
    ) {
        // Sound Catch background live under the player
        SoundCatchMeshBackground(
            dominantColor = track.dominantColor,
            secondaryColor = track.secondaryColor,
            audioTelemetry = telemetry,
            isPlaying = isPlaying,
            isSoundCatchEnabled = isSoundCatchEnabled
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 44.dp, bottom = 32.dp),
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
                        text = "PLAYING FROM MIX",
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextTertiary
                    )
                    Text(
                        text = "Dark Ambient Sessions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = VelvetTextSecondary
                    )
                }

                IconButton(
                    onClick = onOpenInspector,
                    modifier = Modifier.testTag("open_sound_catch_inspector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Sound Catch Mesh Inspector",
                        tint = if (isSoundCatchEnabled) VelvetBrightCrimson else VelvetTextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Central Area: Artwork or Synced Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!showLyrics) {
                    // Album Artwork Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, VelvetBorder, RoundedCornerShape(24.dp))
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

            Spacer(modifier = Modifier.height(20.dp))

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
                            tint = if (showLyrics) VelvetBrightCrimson else VelvetTextSecondary,
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
                            tint = if (isCachedOffline) VelvetBrightCrimson else VelvetTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider
            val progressFraction = if (track.durationMs > 0) {
                (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = { fraction ->
                    onSeekTo((fraction * track.durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = VelvetBrightCrimson,
                    inactiveTrackColor = VelvetBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider")
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(playbackPositionMs),
                    fontSize = 11.sp,
                    color = VelvetTextTertiary
                )
                Text(
                    text = formatMs(track.durationMs),
                    fontSize = 11.sp,
                    color = VelvetTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("player_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) VelvetBrightCrimson else VelvetTextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = VelvetTextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause central button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(VelvetBrightCrimson)
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

                // Next
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = VelvetTextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("player_repeat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) VelvetBrightCrimson else VelvetTextTertiary,
                        modifier = Modifier.size(22.dp)
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
                                .background(if (isSoundCatchEnabled) VelvetBrightCrimson else VelvetTextTertiary)
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

fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
