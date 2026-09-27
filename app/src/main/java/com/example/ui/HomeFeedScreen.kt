package com.example.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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

    // Rich atmospheric crimson wine gradient: slightly more red at the top, deep velvet at bottom
    val velvetBackgroundGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF5E0E22), // Vibrant rich red wine top
                Color(0xFF380814),
                Color(0xFF20040B),
                Color(0xFF120206)  // Deep velvet plum base
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
        // rich crimson frosted wine matching the navigation, sharp top corners, curved bottom left & right,
        // custom acoustic V logo, gradient wordmark, and action icons.
        VelvetTopHeaderCard(
            trackCount = displayTracks.size,
            onOpenNotifications = onOpenNotifications,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings
        )

        // 2. MAIN BACKGROUND MUSIC LIST:
        // Direct list on the background with NO cards, NO extra card padding, and BIG artwork thumbnails.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 8.dp,
                bottom = 120.dp
            )
        ) {
            // Permission request banner if local media permission is missing
            if (!hasAudioPermission && allTracks.isEmpty()) {
                item {
                    DeviceAudioPermissionBanner(onRequestPermission = onRequestPermission)
                    Spacer(modifier = Modifier.height(10.dp))
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Top Header Card:
 * Same rich crimson wine as bottom navigation, flows from the very top status bar,
 * sharp on top, curved on bottom left and right, with custom acoustic logo, gradient wordmark and action icons.
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
        bottomStart = 18.dp,
        bottomEnd = 18.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = topCardShape,
                spotColor = Color(0x70FF2448),
                ambientColor = Color(0x4035040C)
            )
            .clip(topCardShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4C0A1C), // Rich radiant crimson top
                        Color(0xFF2A0612),
                        Color(0xFF1B030A)  // Deep wine base
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x35FF385C)  // Luminous crimson specular rim
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
                .padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Custom-Drawn Acoustic Logo + Designed Gradient Wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bespoke acoustic "V" audio wave logo
                VelvetAcousticLogo()

                Spacer(modifier = Modifier.width(11.dp))

                Column {
                    // Designed gradient title
                    Text(
                        text = "VELVET",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFFFCCD5),
                                    Color(0xFFFF4D6D),
                                    Color(0xFFFF2448)
                                )
                            ),
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2E54))
                        )
                        Spacer(modifier = Modifier.width(4.5.dp))
                        Text(
                            text = "$trackCount TRACKS • DEVICE AUDIO",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.9.sp,
                            color = Color(0xFFFFB2BF)
                        )
                    }
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

/**
 * Custom-drawn Acoustic Wave "V" Logo:
 * Draws 5 acoustic resonance bars with rounded caps forming a sleek "V" silhouette.
 */
@Composable
private fun VelvetAcousticLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE51B3E), // vibrant velvet crimson
                        Color(0xFF990A26),
                        Color(0xFF550415)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0x80FFFFFF),
                        Color(0x30FFAAB8)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val barCount = 5
            val barWidth = w / 7.5f
            val heights = listOf(0.92f, 0.65f, 0.42f, 0.65f, 0.92f)
            val spacing = (w - (barCount * barWidth)) / (barCount - 1)

            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFFFD4DC),
                    Color(0xFFFF859B)
                )
            )

            for (i in 0 until barCount) {
                val barH = h * heights[i]
                val x = i * (barWidth + spacing)
                val topY = (h - barH) / 2f
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
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
                .background(Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFFFFD0D8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Cardless Device Track Row:
 * Direct on the background, NO card container, NO outer border, NO bloated padding.
 * Features a BIG 58×58dp album artwork thumbnail with clean rounded corners.
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

    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BIG Album Artwork (58x58dp) with smooth rounded corners
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF22050E))
                .border(
                    width = 0.8.dp,
                    color = if (isCurrent) Color(0x60FF2448) else Color(0x20FF385C),
                    shape = RoundedCornerShape(12.dp)
                )
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(13.dp))

        // Title and artist metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) Color(0xFFFF3B5C) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.formattedDuration}",
                fontSize = 12.sp,
                color = if (isCurrent) Color(0xFFFFB0BD) else Color(0xFFC098A2),
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
                tint = Color(0xFFC098A2),
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
            .background(Color(0x35600C1C))
            .border(1.dp, Color(0x35FFAAB8), bannerShape)
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
