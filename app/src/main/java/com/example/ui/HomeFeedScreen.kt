package com.example.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    recentlyAddedTracks: List<Track>,
    playCounts: Map<String, Int> = emptyMap(),
    onSelectTrack: (Track) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val topTrack = mostPlayedTracks.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_screen"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
    ) {
        // 1. Top Header: "Welcome back, Echo", "Home Feed", Bell, Settings, Search
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back, Echo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = VelvetTextSecondary.copy(alpha = 0.90f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Home Feed",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetTextPrimary
                        )
                    }

                    // Ash Glass Icon Buttons: Bell, Settings, Search
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // 2. TOP CARD: "Most Played" (Music user plays all the time - always first to play)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color(0xFFE50914).copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(18.dp)
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VelvetBrightCrimson.copy(alpha = 0.22f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HEAVY ROTATION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelvetBrightCrimson
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hero Highlight for #1 Most Played track with instant "Play First" button
                    topTrack?.let { hero ->
                        val heroCount = playCounts[hero.id] ?: hero.playCount.coerceAtLeast(1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                                .clickable { onSelectTrack(hero) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = hero.coverResId),
                                    contentDescription = hero.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hero.title.substringBefore(" - "),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VelvetTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Played $heroCount times • Top Pick",
                                    fontSize = 11.sp,
                                    color = VelvetBrightCrimson,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = { onSelectTrack(hero) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VelvetBrightCrimson,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("play_first_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play First",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play First", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Horizontal scrolling list of other top played tracks with play counts
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(mostPlayedTracks) { track ->
                            val count = playCounts[track.id] ?: track.playCount.coerceAtLeast(1)
                            MostPlayedMiniCard(
                                track = track,
                                playCount = count,
                                isCurrent = track.id == currentTrack.id,
                                isPlaying = isPlaying && track.id == currentTrack.id,
                                onClick = { onSelectTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        // 3. BOTTOM CARD: "Recently Added" (New music the user actually added)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color(0xFFE50914).copy(alpha = 0.03f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                    .padding(18.dp)
                    .testTag("recently_added_container")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Recently Added",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyAddedTracks) { track ->
                            RecentlyAddedMiniCard(
                                track = track,
                                isCurrent = track.id == currentTrack.id,
                                onClick = { onSelectTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        // 4. THE REST: "All Music" (Standalone list of user & device music on main background)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 18.dp, bottom = 8.dp)
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${allTracks.size})",
                            fontSize = 14.sp,
                            color = VelvetTextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                allTracks.forEach { track ->
                    val isCurrent = track.id == currentTrack.id
                    StandaloneMusicRow(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSelectTrack(track) },
                        onMenuClick = { onTrackMenuClick(track) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
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
                        modifier = Modifier.size(20.dp)
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
fun MostPlayedMiniCard(
    track: Track,
    playCount: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("most_played_${track.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (isCurrent) VelvetBrightCrimson else Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(14.dp)
                )
        ) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Play count pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${playCount}x",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetBrightCrimson
                )
            }

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = VelvetBrightCrimson,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title.substringBefore(" - "),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = VelvetTextSecondary.copy(alpha = 0.80f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentlyAddedMiniCard(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("recently_added_${track.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (isCurrent) VelvetBrightCrimson else Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(14.dp)
                )
        ) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(VelvetBrightCrimson)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title.substringBefore(" - "),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = VelvetTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontSize = 11.sp,
            color = VelvetTextSecondary.copy(alpha = 0.80f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
