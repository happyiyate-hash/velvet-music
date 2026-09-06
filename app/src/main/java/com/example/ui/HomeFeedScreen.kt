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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.VelvetAshGray
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetCardBorder
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

private val QuickAccessGlass = Color(0xE20A0A0B)
private val QuickAccessInner = Color(0xB9141416)

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
    // Quick Access is strictly real play history: never populate it with unplayed music.
    // Distinct IDs prevent duplicate cards, while mostPlayedTracks is already count-ranked.
    val quickAccessTracks = mostPlayedTracks
        .filter { (playCounts[it.id] ?: it.playCount) > 0 }
        .distinctBy { it.id }
        .take(6)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_screen"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
    ) {
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AshGlassIconButton(Icons.Default.Notifications, "Notifications", onOpenNotifications, "home_notifications_button")
                        AshGlassIconButton(Icons.Default.Settings, "Settings", onOpenSettings, "home_settings_button")
                        AshGlassIconButton(Icons.Default.Search, "Search", onOpenSearch, "home_search_button")
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.48f), spotColor = Color.Black.copy(alpha = 0.62f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(QuickAccessGlass)
                    .border(1.dp, VelvetCardBorder.copy(alpha = 0.82f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .testTag("most_played_container")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quick Access",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetTextPrimary
                            )
                            Text(
                                text = "Music you keep coming back to",
                                fontSize = 10.sp,
                                color = VelvetTextSecondary.copy(alpha = 0.78f)
                            )
                        }
                        if (quickAccessTracks.isNotEmpty()) {
                            Text(
                                text = "MOST PLAYED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetTextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    if (quickAccessTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Play music from your device to build Quick Access",
                                fontSize = 12.sp,
                                color = VelvetTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        quickAccessTracks.forEachIndexed { index, track ->
                            CompactMostPlayedRow(
                                track = track,
                                rank = index + 1,
                                playCount = playCounts[track.id] ?: track.playCount,
                                isCurrent = track.id == currentTrack.id,
                                isPlaying = isPlaying && track.id == currentTrack.id,
                                onClick = { onSelectTrack(track) },
                                modifier = Modifier.background(
                                    QuickAccessInner.copy(alpha = if (track.id == currentTrack.id) 0.70f else 0.32f),
                                    RoundedCornerShape(14.dp)
                                )
                            )
                            if (index < quickAccessTracks.lastIndex) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.065f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

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
                        Text("All Music", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = VelvetTextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(${allTracks.size})", fontSize = 14.sp, color = VelvetTextTertiary)
                    }

                    if (!hasAudioPermission && onRequestPermission != null) {
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetBrightCrimson, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, "Grant Access", Modifier.size(14.dp))
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
                            Icon(Icons.Default.FolderOpen, null, tint = VelvetTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(38.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (!hasAudioPermission) "Permission Required" else "No Device Music Found",
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = VelvetTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (!hasAudioPermission) "Allow Velvet to access your device audio files to play your music"
                                else "Add audio files to your device storage to automatically see them here",
                                fontSize = 12.sp, color = VelvetTextSecondary, textAlign = TextAlign.Center
                            )
                            if (!hasAudioPermission && onRequestPermission != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRequestPermission,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetBrightCrimson)
                                ) {
                                    Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(16.dp))
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
        ) {
            Image(painterResource(track.coverResId), track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (isCurrent && isPlaying) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.GraphicEq, "Playing", tint = VelvetBrightCrimson, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(cleanTitle, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium, color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text("${track.artist} • ${track.album}", fontSize = 12.sp, color = VelvetTextSecondary.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp).testTag("track_menu_${track.id}")) {
            Icon(Icons.Default.MoreVert, "Track Menu", tint = VelvetTextSecondary, modifier = Modifier.size(20.dp))
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
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 6.dp)
            .testTag("compact_most_played_rank_$rank"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 62.dp is only ~19% larger than the 52.dp artwork used by All Music.
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(
                    1.dp,
                    if (isCurrent) VelvetBrightCrimson else Color.White.copy(alpha = 0.13f),
                    RoundedCornerShape(13.dp)
                )
        ) {
            Image(painterResource(track.coverResId), track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (isCurrent && isPlaying) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.GraphicEq, "Playing", tint = VelvetBrightCrimson, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(cleanTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${rank}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VelvetTextTertiary)
                Spacer(modifier = Modifier.width(7.dp))
                Text(if (playCount == 1) "Played once" else "Played ${playCount}x", fontSize = 11.sp, color = VelvetTextSecondary.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrent && isPlaying) VelvetBrightCrimson else Color.White.copy(alpha = 0.08f))
                .border(1.dp, if (isCurrent && isPlaying) VelvetBrightCrimson else Color.White.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
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
        Icon(icon, contentDescription, tint = VelvetTextPrimary, modifier = Modifier.size(20.dp))
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
    StandaloneMusicRow(track, isCurrent, isPlaying, onClick, onMenuClick)
}
