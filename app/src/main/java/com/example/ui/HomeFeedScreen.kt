package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.DeviceMediaManager
import com.example.model.Track
import com.example.ui.theme.VelvetAshGray
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetCardBorder
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

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
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    onAddTrack: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Two candidates for the Most Played Card: top slot and bottom slot
    val candidatePlayed = mostPlayedTracks.ifEmpty { allTracks }
    val topSlotTrack = candidatePlayed.getOrNull(0)
    val bottomSlotTrack = candidatePlayed.getOrNull(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_screen"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
    ) {
        // 1. Top Header: "Welcome back, Echo", "Home Feed", Bell, Settings, Search
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back, Echo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = VelvetTextSecondary.copy(alpha = 0.90f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Home Feed",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetTextPrimary
                        )
                    }

                    // Ash Glass Icon Buttons: Bell, Settings, Search
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AshGlassIconButton(
                            icon = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            onClick = onOpenNotifications,
                            tag = "home_notifications_button"
                        )
                        AshGlassIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            tag = "home_settings_button"
                        )
                        AshGlassIconButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search",
                            onClick = onOpenSearch,
                            tag = "home_search_button"
                        )
                    }
                }
            }
        }

        // 2. TOP CARD: "Most Played" (Compact, minimal padding so content enlarges)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color(0xFFE50914).copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                    .padding(8.dp)
                    .testTag("most_played_container")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Most Played",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VelvetBrightCrimson.copy(alpha = 0.22f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FREQUENTLY PLAYED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelvetBrightCrimson
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (topSlotTrack != null) {
                        val count1 = playCounts[topSlotTrack.id] ?: topSlotTrack.playCount.coerceAtLeast(1)
                        // ONE MUSIC AT THE TOP
                        CompactMostPlayedRow(
                            track = topSlotTrack,
                            rank = 1,
                            playCount = count1,
                            isCurrent = topSlotTrack.id == currentTrack.id,
                            isPlaying = isPlaying && topSlotTrack.id == currentTrack.id,
                            onClick = { onSelectTrack(topSlotTrack) }
                        )

                        if (bottomSlotTrack != null) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            val count2 = playCounts[bottomSlotTrack.id] ?: bottomSlotTrack.playCount.coerceAtLeast(1)
                            // ONE MUSIC AT THE BOTTOM
                            CompactMostPlayedRow(
                                track = bottomSlotTrack,
                                rank = 2,
                                playCount = count2,
                                isCurrent = bottomSlotTrack.id == currentTrack.id,
                                isPlaying = isPlaying && bottomSlotTrack.id == currentTrack.id,
                                onClick = { onSelectTrack(bottomSlotTrack) }
                            )
                        }
                    } else {
                        // Empty state inside the card when device has no music
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Play music from your device to display your favorites here",
                                fontSize = 12.sp,
                                color = VelvetTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 3. "All Music" (Standalone list with minimal padding)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "All Music",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${allTracks.size})",
                            fontSize = 14.sp,
                            color = VelvetTextTertiary
                        )
                    }

                    if (!hasAudioPermission && onRequestPermission != null) {
                        Button(
                            onClick = { onRequestPermission() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelvetBrightCrimson,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Grant Access",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Grant Access", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (allTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = VelvetTextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!hasAudioPermission) "Permission Required" else "No Device Music Found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VelvetTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (!hasAudioPermission)
                                    "Allow Velvet to access your device audio files to play your music"
                                else
                                    "Add audio files to your device storage to automatically see them here",
                                fontSize = 12.sp,
                                color = VelvetTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (!hasAudioPermission && onRequestPermission != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onRequestPermission() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetBrightCrimson)
                                ) {
                                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Allow Music Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    allTracks.forEach { track ->
                        val isCurrent = track.id == currentTrack.id
                        StandaloneMusicRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onSelectTrack(track) },
                            onMenuClick = { onTrackMenuClick(track) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/**
 * Standalone music row sitting cleanly in the main background canvas:
 * - Album artwork with crisp border
 * - Clean title and artist
 * - Playing equalizer when active
 * - Clean vertical three dots (standing straight)
 */
@Composable
fun StandaloneMusicRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val cleanTitle = track.title.substringBefore(" - ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color.White.copy(alpha = 0.04f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("standalone_track_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .background(Color.Transparent)
        ) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = VelvetBrightCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                fontSize = 12.sp,
                color = VelvetTextSecondary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // STANDING STRAIGHT VERTICAL THREE DOTS (MoreVert)
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("track_menu_${track.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Track Menu",
                tint = VelvetTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CompactMostPlayedRow(
    track: Track,
    rank: Int,
    playCount: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = track.title.substringBefore(" - ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("compact_most_played_rank_$rank"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact album artwork (48.dp x 48.dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(11.dp))
                .border(
                    1.dp,
                    if (isCurrent) VelvetBrightCrimson else Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(11.dp)
                )
        ) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
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
                        tint = VelvetBrightCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VelvetBrightCrimson.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "TOP #$rank",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetBrightCrimson
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (playCount > 1) "Played ${playCount}x" else track.artist,
                    fontSize = 11.sp,
                    color = VelvetTextSecondary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Curved Play Action Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrent && isPlaying) VelvetBrightCrimson else Color.White.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    if (isCurrent && isPlaying) VelvetBrightCrimson else Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isCurrent && isPlaying) Color.White else VelvetTextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AshGlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VelvetAshGray.copy(alpha = 0.50f))
            .border(1.dp, VelvetCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = VelvetTextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RecentlyPlayedRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    StandaloneMusicRow(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onMenuClick = onMenuClick
    )
}
