package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SampleData
import com.example.model.Track

@Composable
fun HomeFeedScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    allTracks: List<Track>,
    mostPlayedTracks: List<Track>,
    recentlyAddedTracks: List<Track> = emptyList(),
    playCounts: Map<String, Int> = emptyMap(),
    hasAudioPermission: Boolean = true,
    onRequestPermission: (() -> Unit)? = null,
    onSelectTrack: (Track) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onTrackMenuClick: (Track) -> Unit,
    onAddTrack: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayTracks = remember(allTracks) {
        if (allTracks.isNotEmpty()) allTracks else SampleData.starterTracks
    }

    val velvetBackgroundGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF140207), // midnight plum top matching top card base
                Color(0xFF100206)  // deep pitch velvet
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(velvetBackgroundGradient)
            .testTag("home_feed_screen")
    ) {
        // 1. TOP HEADER CARD:
        // Fills the very status bar of the phone to remove extra gap,
        // same color as bottom navigation, sharp top corners, curved bottom left & right,
        // logo on the left with compact app name, action icons on the right.
        VelvetTopHeaderCard(
            trackCount = displayTracks.size,
            onOpenNotifications = onOpenNotifications,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings
        )

        // 2. MAIN BACKGROUND:
        // All lists of music in the user's device directly in the main background.
        // No recently played, no discovered new releases, no mock internet cards.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 120.dp
            )
        ) {
            // Permission request banner if local media permission is missing
            if (!hasAudioPermission && allTracks.isEmpty()) {
                item {
                    DeviceAudioPermissionBanner(onRequestPermission = onRequestPermission)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            items(displayTracks, key = { it.id }) { track ->
                val isCurrent = track.id == currentTrack.id
                DeviceTrackRowItem(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying && isCurrent,
                    onClick = { onSelectTrack(track) },
                    onMenuClick = { onTrackMenuClick(track) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.5.dp)
                )
            }
        }
    }
}

/**
 * Top Header Card:
 * Same color as bottom navigation, flows from the very top status bar,
 * sharp on top, curved on bottom left and right, with compact logo + wordmark and action icons.
 */
@Composable
private fun VelvetTopHeaderCard(
    trackCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topCardShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = topCardShape,
                spotColor = Color(0x50FF2448),
                ambientColor = Color(0x3018030A)
            )
            .clip(topCardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF19030A), // matches bottom navigation
                        Color(0xFF100206)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x30FFAAB8)
                    )
                ),
                shape = topCardShape
            )
    ) {
        // Internal statusBarsPadding keeps text and icons positioned safely below the system status bar,
        // while the card's background fills the status bar area completely.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Logo + Compact App Name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek Velvet Logo badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFF2448),
                                    Color(0xFF900822)
                                )
                            )
                        )
                        .border(0.8.dp, Color(0x40FFFFFF), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Velvet Logo",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "VELVET",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$trackCount TRACKS • DEVICE AUDIO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFC098A2)
                    )
                }
            }

            // Right: Compact Action Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopBarActionButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    onClick = onOpenNotifications
                )
                TopBarActionButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search",
                    onClick = onOpenSearch
                )
                TopBarActionButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun TopBarActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x18FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFFE2CCD2),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Bigger, Rounded Device Track Row with Reduced Padding.
 */
@Composable
private fun DeviceTrackRowItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = track.title.substringBefore(" - ")
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(rowShape)
            .background(
                if (isCurrent) Color(0x35600E1C)
                else Color(0x14FFFFFF)
            )
            .border(
                width = 0.8.dp,
                color = if (isCurrent) Color(0x55FF2448) else Color(0x12FFAAB8),
                shape = rowShape
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quite bigger album artwork thumbnail (52x52dp) with rounded corners
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF20060E))
                .border(0.8.dp, Color(0x22FFAAB8), RoundedCornerShape(10.dp))
        ) {
            TrackArtworkImage(
                track = track,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                thumbnailSizePx = 128,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color(0xFFFF2448),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and artist metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) Color(0xFFFF5575) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.formattedDuration}",
                fontSize = 12.sp,
                color = Color(0xFFB89EA6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Menu
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Track options",
                tint = Color(0xFF9E848D),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DeviceAudioPermissionBanner(
    onRequestPermission: (() -> Unit)?
) {
    val bannerShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(bannerShape)
            .background(Color(0x30600C1C))
            .border(1.dp, Color(0x30FFAAB8), bannerShape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Device Audio Access",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Grant permission to read and play all music files stored on this device.",
                fontSize = 11.5.sp,
                color = Color(0xFFC098A2)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Button(
            onClick = { onRequestPermission?.invoke() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE51B3E),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = "Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
