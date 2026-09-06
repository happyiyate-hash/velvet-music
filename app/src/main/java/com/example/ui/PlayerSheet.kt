package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetBorder
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
        SoundCatchMeshBackground(
            dominantColor = dynamicDominant,
            secondaryColor = dynamicSecondary,
            audioTelemetry = telemetry,
            isPlaying = isPlaying,
            isSoundCatchEnabled = isSoundCatchEnabled
        )

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
                .padding(horizontal = 10.dp)
                .padding(top = 18.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            1.2.dp,
                            dynamicDominant.copy(alpha = 0.35f),
                            RoundedCornerShape(28.dp)
                        )
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = track.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontSize = 14.sp,
                    color = VelvetTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val duration = track.durationMs.coerceAtLeast(1L)
            val position = playbackPositionMs.coerceIn(0L, duration)
            Slider(
                value = position.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = dynamicDominant,
                    activeTrackColor = dynamicDominant,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .testTag("player_progress_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlayerTime(position),
                    fontSize = 12.sp,
                    color = VelvetTextTertiary
                )
                Text(
                    text = formatPlayerTime(track.durationMs),
                    fontSize = 12.sp,
                    color = VelvetTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerSmallControl(
                    icon = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    active = isShuffle,
                    activeColor = dynamicDominant,
                    onClick = onToggleShuffle,
                    tag = "player_shuffle_button"
                )
                PlayerLargeControl(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    activeColor = dynamicDominant,
                    onClick = onSkipPrevious,
                    tag = "player_previous_button"
                )
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
                PlayerLargeControl(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    activeColor = dynamicDominant,
                    onClick = onSkipNext,
                    tag = "player_next_button"
                )
                PlayerSmallControl(
                    icon = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    active = isRepeat,
                    activeColor = dynamicDominant,
                    onClick = onToggleRepeat,
                    tag = "player_repeat_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kept for now; this bottom Sound Catch element can be redesigned separately later.
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
                                .clip(RoundedCornerShape(3.dp))
                                .background(dynamicDominant)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (isSoundCatchEnabled) "Sound Catch Active" else "Sound Catch Paused",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = VelvetTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSmallControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) activeColor.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (active) activeColor.copy(alpha = 0.50f)
                else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) activeColor else VelvetTextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PlayerLargeControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    activeColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        activeColor.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.2.dp, activeColor.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VelvetTextPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun formatPlayerTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
