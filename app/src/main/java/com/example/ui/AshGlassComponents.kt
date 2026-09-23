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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SmartDisplay
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x332E3440), // Translucent frosted gray-white glass tint
                        Color(0x24222630),
                        Color(0x1A141720)
                    )
                )
            )
            .testTag("ash_glass_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Specular top hairline reflection line across the straight top edge
        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)) {
            val w = size.width
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x00FFFFFF),
                        Color(0x28FFFFFF),
                        Color(0x60FFFFFF),
                        Color(0x28FFFFFF),
                        Color(0x00FFFFFF)
                    )
                ),
                start = Offset(0f, 0f),
                end = Offset(w, 0f),
                strokeWidth = 1f
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            AshGlassNavIconOnlyItem(
                icon = Icons.Default.Home,
                contentDescription = "Home",
                isSelected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                testTag = "nav_tab_home"
            )

            // 2. Search
            AshGlassNavIconOnlyItem(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                isSelected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                testTag = "nav_tab_search"
            )

            // 3. Video
            AshGlassNavIconOnlyItem(
                icon = Icons.Default.SmartDisplay,
                contentDescription = "Video",
                isSelected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                testTag = "nav_tab_videos"
            )
        }
    }
}

@Composable
private fun AshGlassNavIconOnlyItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val iconColor = if (isSelected) Color.White else Color(0x88FFFFFF)

    val pillModifier = if (isSelected) {
        Modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x35FFFFFF),
                        Color(0x20FFFFFF)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(0.7.dp, Color(0x45FFFFFF), RoundedCornerShape(12.dp))
    } else Modifier

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(pillModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(21.dp)
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
                        MorphingPlayPauseIcon(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
                IconButton(modifier = Modifier.size(34.dp).testTag("mini_player_skip_next"), onClick = onSkipNext) {
                    PlayerNextIcon(
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
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
