package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
    hasMiniPlayerAbove: Boolean = false,
    modifier: Modifier = Modifier
) {
    // When the music card sits above, top corners turn sharp (0.dp) to unite with the card seamlessly.
    // When no music card is showing, only the top left and right curve (16.dp).
    // The bottom corners are ALWAYS sharp (0.dp) so it stays flush at the very bottom of the screen.
    val animatedTopRadius by animateDpAsState(
        targetValue = if (hasMiniPlayerAbove) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "navTopCornerRadius"
    )

    val navShape = RoundedCornerShape(
        topStart = animatedTopRadius,
        topEnd = animatedTopRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ash_glass_navigation_bar")
            .shadow(
                elevation = if (hasMiniPlayerAbove) 0.dp else 12.dp,
                shape = navShape,
                spotColor = Color(0x50FF2448),
                ambientColor = Color(0x3018030A)
            )
            .clip(navShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF260510), // rich dark wine
                        Color(0xFF140208)  // deep midnight wine base
                    )
                )
            )
            .then(
                if (!hasMiniPlayerAbove) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x38FFFFFF),
                                Color(0x15FF3B5C),
                                Color(0x05FFFFFF)
                            )
                        ),
                        shape = navShape
                    )
                } else Modifier
            )
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Reduced compact height for bottom navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Music / Home
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

            // 3. Explore / Discover
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
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Subtle glowing bloom behind active icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x85FF385C),
                                    Color(0x20FF2048),
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
                tint = if (isSelected) Color.White else Color(0xFF8E7D84),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (isSelected) {
            // Sleek active crimson indicator pill
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFFFF385C))
            )
        } else {
            Spacer(modifier = Modifier.height(2.5.dp))
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (track.durationMs > 0) {
        (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // The card sits directly on top of the bottom navigation.
    // It is curved both left and right only on top (16.dp).
    // The bottom corners are sharp (0.dp) to sit seamlessly against the sharp top of the bottom navigation.
    val cardShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                spotColor = Color(0x60FF2448),
                ambientColor = Color(0x4018030A)
            )
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF380818), // rich dark crimson wine
                        Color(0xFF260510)  // seamlessly matches bottom nav top
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x40FFFFFF), // specular top rim
                        Color(0x18FF3B5C),
                        Color.Transparent
                    )
                ),
                shape = cardShape
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
                    .height(2.5.dp),
                color = Color(0xFFFF2448),
                trackColor = Color(0x2535040C)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Uniform Album Art with crisp rounded corners
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF140206))
                        .border(0.8.dp, Color(0x25FFAAB8), RoundedCornerShape(8.dp))
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
                    Spacer(modifier = Modifier.height(1.5.dp))
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

                Spacer(modifier = Modifier.width(2.dp))

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
