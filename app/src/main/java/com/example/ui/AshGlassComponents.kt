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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
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
    val navShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("ash_glass_navigation_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Floating Frosted Dark Crimson Glass Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = navShape,
                    spotColor = Color(0x60FF2448),
                    ambientColor = Color(0x4035040C)
                )
                .clip(navShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE2E0914), // Frosted dark wine glass
                            Color(0xF618030A)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x45FFFFFF), // Specular top edge hairline
                            Color(0x18FF3B5C),
                            Color(0x10FFFFFF)
                        )
                    ),
                    shape = navShape
                )
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Music / Home (ACTIVE with glowing bloom & ACTIVE pill badge)
                VelvetGlassNavTabItem(
                    icon = Icons.Default.LibraryMusic,
                    contentDescription = "Music",
                    isSelected = selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    testTag = "nav_tab_home"
                )

                // 2. Search
                VelvetGlassNavTabItem(
                    icon = Icons.Default.Search,
                    contentDescription = "Search",
                    isSelected = selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    testTag = "nav_tab_search"
                )

                // 3. Explore / Sparkle
                VelvetGlassNavTabItem(
                    icon = Icons.Default.AutoAwesome,
                    contentDescription = "Discover",
                    isSelected = selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    testTag = "nav_tab_videos"
                )
            }
        }
    }
}

@Composable
private fun VelvetGlassNavTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Radiant glowing red/salmon bloom behind the icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x95FF385C),
                                    Color(0x35FF2048),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) Color.White else Color(0xFF9E8A92),
                modifier = Modifier.size(24.dp)
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            // Active text badge pill
            Text(
                text = "ACTIVE",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = Color(0xFFFFB0BB)
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
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

    val miniShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = 16.dp,
                shape = miniShape,
                spotColor = Color(0x60FF2448),
                ambientColor = Color(0x3035040C)
            )
            .clip(miniShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF82A0914),
                        Color(0xFD18030A)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x35FFFFFF),
                        Color(0x18FF3B5C),
                        Color(0x10FFFFFF)
                    )
                ),
                shape = miniShape
            )
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Column {
            // Slim playback progress bar at the very top edge
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFFFF385C),
                trackColor = Color(0x3035040C)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Uniform Album Art with crisp rounded corners
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF140206))
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = track.artist,
                        fontSize = 11.5.sp,
                        color = Color(0xFFC098A2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action controls
                IconButton(
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_play_pause"),
                    onClick = onTogglePlayPause
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
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

                IconButton(
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_skip_next"),
                    onClick = onSkipNext
                ) {
                    PlayerNextIcon(
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFE2B2BA)
                    )
                }
            }
        }
    }
}
