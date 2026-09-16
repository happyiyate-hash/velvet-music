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
    val topCurvedShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = topCurvedShape,
                spotColor = Color(0x99FF2448),
                ambientColor = Color(0x6648000C)
            )
            .clip(topCurvedShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF24020A),
                        Color(0xFF130005),
                        Color(0xFF060002)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x15FFFFFF),
                        Color(0x55FF2448),
                        Color(0xAAFFFFFF),
                        Color(0x55FF2448),
                        Color(0x15FFFFFF)
                    )
                ),
                shape = topCurvedShape
            )
            .testTag("ash_glass_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial bloom and top glow
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Center-spread crimson ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x35FF2448),
                        Color(0x1548000C),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.4f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w * 0.55f
            )

            // Specular top highlight reflection line
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x05FFFFFF),
                        Color(0x60FFFFFF),
                        Color(0xFFFF4D6D),
                        Color(0x60FFFFFF),
                        Color(0x05FFFFFF)
                    )
                ),
                start = Offset(24f, 1f),
                end = Offset(w - 24f, 1f),
                strokeWidth = 1.2f
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Search
            AshGlassNavTabItem(
                label = "Search",
                isSelected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                testTag = "nav_tab_search"
            ) { iconColor ->
                Icon(Icons.Default.Search, "Search", tint = iconColor, modifier = Modifier.size(23.dp))
            }

            // 2. Music
            AshGlassNavTabItem(
                label = "Music",
                isSelected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                testTag = "nav_tab_music"
            ) { iconColor ->
                Icon(Icons.Default.MusicNote, "Music", tint = iconColor, modifier = Modifier.size(23.dp))
            }

            // 3. Video
            AshGlassNavTabItem(
                label = "Video",
                isSelected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                testTag = "nav_tab_videos"
            ) { iconColor ->
                Icon(Icons.Default.Videocam, "Video", tint = iconColor, modifier = Modifier.size(23.dp))
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
    val textColor = if (isSelected) Color(0xFFFF4D6D) else Color(0xFF8E8E93)

    val tabBgModifier = if (isSelected) {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0x38FF2448),
                    Color(0x1848000C)
                )
            )
        )
    } else Modifier

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .then(tabBgModifier)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                brush = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x88FF4D6D),
                            Color(0x33FF2448)
                        )
                    )
                } else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 22.dp, vertical = 7.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content(iconColor)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
