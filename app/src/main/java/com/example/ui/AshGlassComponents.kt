package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary

@Composable
fun AshGlassBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(navBarShape)
            .testTag("ash_glass_navigation_bar"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB8231C28),
                        Color(0xC817121D),
                        Color(0xD80E0A14)
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color(0xFFE50914).copy(alpha = 0.035f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            val dotColor1 = Color(0x1AFFFFFF)
            val dotColor2 = Color(0x12CCD4E0)
            val stepX = 3.8f
            val stepY = 3.8f
            var curY = 1.5f
            while (curY < h) {
                var curX = if (((curY / stepY).toInt() % 2) == 0) 1.5f else 3.4f
                while (curX < w) {
                    drawCircle(
                        color = if (((curX + curY).toInt() % 3) == 0) dotColor1 else dotColor2,
                        radius = 0.65f,
                        center = Offset(curX, curY)
                    )
                    curX += stepX
                }
                curY += stepY
            }
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x20FFFFFF),
                        Color(0x70FFFFFF),
                        Color(0x70FFFFFF),
                        Color(0x20FFFFFF)
                    )
                ),
                start = Offset(0f, 1f),
                end = Offset(w, 1f),
                strokeWidth = 1.4f
            )
        }

        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AshGlassNavTabItem(
                        isSelected = selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        testTag = "nav_tab_music"
                    ) { iconColor ->
                        Icon(Icons.Default.MusicNote, "Music", tint = iconColor, modifier = Modifier.size(23.dp))
                    }
                    AshGlassNavTabItem(
                        isSelected = selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        testTag = "nav_tab_search"
                    ) { iconColor ->
                        Icon(Icons.Default.Search, "Search", tint = iconColor, modifier = Modifier.size(23.dp))
                    }
                    AshGlassNavTabItem(
                        isSelected = selectedTab == 2,
                        onClick = { onSelectTab(2) },
                        testTag = "nav_tab_videos"
                    ) { iconColor ->
                        Icon(Icons.Default.Videocam, "Videos", tint = iconColor, modifier = Modifier.size(23.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AshGlassNavTabItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable (iconColor: Color) -> Unit
) {
    val activeAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(240),
        label = "nav_active_alpha"
    )
    val iconColor = if (isSelected) Color(0xFFFF2E54) else Color(0xFF7A808E)

    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (activeAlpha > 0.01f) {
            Canvas(modifier = Modifier.size(44.dp).align(Alignment.Center)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE51D44).copy(alpha = 0.38f * activeAlpha),
                            Color(0xFF8A0F26).copy(alpha = 0.16f * activeAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 20.dp.toPx()
                    ),
                    center = center,
                    radius = 20.dp.toPx()
                )
            }
        }
        content(iconColor)
    }
}

@Composable
fun AshGlassMiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit
) {
    val progressFraction = if (track.durationMs > 0) {
        (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF260D19).copy(alpha = 0.94f),
                        Color(0xFF190913).copy(alpha = 0.96f)
                    )
                )
            )
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    androidx.compose.material3.Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = VelvetTextSecondary.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(modifier = Modifier.size(32.dp).testTag("mini_player_play_pause"), onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = VelvetBrightCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(modifier = Modifier.size(32.dp).testTag("mini_player_skip_next"), onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(1.5.dp),
                color = VelvetBrightCrimson,
                trackColor = Color.Transparent
            )
        }
    }
}
