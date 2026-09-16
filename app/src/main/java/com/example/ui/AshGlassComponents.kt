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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.VelvetLuminousCrimson
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary

@Composable
fun AshGlassBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(
                elevation = 18.dp,
                shape = pillShape,
                spotColor = Color(0x77FF2448),
                ambientColor = Color(0x3348000C)
            )
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF5180309),
                        Color(0xFB0D0105),
                        Color(0xFF040002)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x28FFFFFF),
                        Color(0x66FF2448),
                        Color(0x28FFFFFF)
                    )
                ),
                shape = pillShape
            )
            .testTag("ash_glass_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // Radial bloom in center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x26FF2448),
                        Color(0x0F48000C),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.45f
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.45f
            )
            // Subtle top highlight reflection line
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x05FFFFFF),
                        Color(0x40FFFFFF),
                        Color(0x60FF2448),
                        Color(0x40FFFFFF),
                        Color(0x05FFFFFF)
                    )
                ),
                start = Offset(16f, 1f),
                end = Offset(w - 16f, 1f),
                strokeWidth = 1f
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AshGlassNavTabItem(
                label = "Home",
                isSelected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                testTag = "nav_tab_music"
            ) { iconColor ->
                Icon(Icons.Default.MusicNote, "Home", tint = iconColor, modifier = Modifier.size(21.dp))
            }
            AshGlassNavTabItem(
                label = "Explore",
                isSelected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                testTag = "nav_tab_explore"
            ) { iconColor ->
                Icon(Icons.Default.Explore, "Explore", tint = iconColor, modifier = Modifier.size(21.dp))
            }
            AshGlassNavTabItem(
                label = "Library",
                isSelected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                testTag = "nav_tab_videos"
            ) { iconColor ->
                Icon(Icons.Default.VideoLibrary, "Library", tint = iconColor, modifier = Modifier.size(21.dp))
            }
            AshGlassNavTabItem(
                label = "Premium",
                isSelected = selectedTab == 3,
                onClick = { onSelectTab(3) },
                testTag = "nav_tab_premium"
            ) { iconColor ->
                Icon(Icons.Default.GraphicEq, "Premium", tint = iconColor, modifier = Modifier.size(21.dp))
            }
        }
    }
}

@Composable
private fun AshGlassNavTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable (iconColor: Color) -> Unit
) {
    val iconColor = if (isSelected) Color(0xFFFF2448) else Color(0xFF8E8E93)
    val textColor = if (isSelected) Color(0xFFFF2448) else Color(0xFF8E8E93)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0x22FF2448) else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content(iconColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
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

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = Color(0x77FF2448),
                ambientColor = Color(0x3348000C)
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xF5260009),
                        Color(0xFA140005)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color(0x66FF2448),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            )
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0x44FF2448), RoundedCornerShape(10.dp))
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
                    Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = track.artist,
                        fontSize = 11.5.sp,
                        color = Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    modifier = Modifier.size(36.dp).testTag("mini_player_play_pause"),
                    onClick = onTogglePlayPause
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFE51B3E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(modifier = Modifier.size(34.dp).testTag("mini_player_skip_next"), onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color(0xFFFF2448),
                trackColor = Color(0x3348000C)
            )
        }
    }
}
