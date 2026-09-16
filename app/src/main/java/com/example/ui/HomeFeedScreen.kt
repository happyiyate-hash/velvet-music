package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.ui.theme.BrightAshGray
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
    val candidatePlayed = mostPlayedTracks.ifEmpty { allTracks }
    val topSlotTrack = candidatePlayed.getOrNull(0)
    val bottomSlotTrack = candidatePlayed.getOrNull(1)

    var activeFilter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All Songs") }

    val displayedTracks = androidx.compose.runtime.remember(allTracks, mostPlayedTracks, activeFilter) {
        when (activeFilter) {
            "Most Played" -> mostPlayedTracks.ifEmpty { allTracks }
            "Hi-Res" -> allTracks.filter { it.isLossless || it.durationMs > 180000L }.ifEmpty { allTracks }
            else -> allTracks
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_screen")
    ) {
        // Atmospheric Multi-Gradient Canvas Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Base obsidian black
            drawRect(color = Color(0xFF040002))

            // Upper crimson ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FF2448),
                        Color(0x1F4A000E),
                        Color(0x0C220005),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.12f),
                    radius = w * 0.78f
                ),
                center = Offset(w * 0.5f, h * 0.12f),
                radius = w * 0.78f
            )

            // Mid-canvas deep wine bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x224A0012),
                        Color(0x0E240008),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.55f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.85f, h * 0.55f),
                radius = w * 0.65f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
        ) {
            // 0. Velvet Signature Branding Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "V E L V E T",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF2448),
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DARK.  AMBIENT.  FLUID AUDIO.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 2.4.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1. Top Header: Title with Badge, and Glass Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Music",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x44FF2448),
                                                Color(0x224A0012)
                                            )
                                        )
                                    )
                                    .border(0.8.dp, Color(0x66FF2448), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${allTracks.size} tracks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF4D6D)
                                )
                            }
                        }
                        Text(
                            text = "Your velvet audio sanctuary",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    // Glass Icon Buttons
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

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 2. HERO GRADIENT CARD: "Most Played / Featured Play"
            item {
                val cardShape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = cardShape,
                            spotColor = Color(0x88FF2448),
                            ambientColor = Color(0x444A0012)
                        )
                        .clip(cardShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF480012),
                                    Color(0xFF220008),
                                    Color(0xFF0E0004)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0x88FF2448),
                                    Color(0x33FFFFFF),
                                    Color(0x66FF2448)
                                )
                            ),
                            shape = cardShape
                        )
                        .padding(14.dp)
                        .testTag("most_played_container")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF2448))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FREQUENTLY PLAYED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF4D6D),
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Text(
                                text = "Velvet Engine",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (topSlotTrack != null) {
                            val count1 = playCounts[topSlotTrack.id] ?: topSlotTrack.playCount.coerceAtLeast(1)
                            GradientMostPlayedTrackRow(
                                track = topSlotTrack,
                                rank = 1,
                                playCount = count1,
                                isCurrent = topSlotTrack.id == currentTrack.id,
                                isPlaying = isPlaying && topSlotTrack.id == currentTrack.id,
                                onClick = { onSelectTrack(topSlotTrack) }
                            )

                            if (bottomSlotTrack != null) {
                                HorizontalDivider(
                                    color = Color(0x33FF2448),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                val count2 = playCounts[bottomSlotTrack.id] ?: bottomSlotTrack.playCount.coerceAtLeast(1)
                                GradientMostPlayedTrackRow(
                                    track = bottomSlotTrack,
                                    rank = 2,
                                    playCount = count2,
                                    isCurrent = bottomSlotTrack.id == currentTrack.id,
                                    isPlaying = isPlaying && bottomSlotTrack.id == currentTrack.id,
                                    onClick = { onSelectTrack(bottomSlotTrack) }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Play music from your device to display your favorites here",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // 3. GRADIENT FILTER PILLS
            item {
                val filters = listOf("All Songs", "Most Played", "Hi-Res")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = activeFilter == filter
                        val pillShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFE51B3E),
                                                Color(0xFF880018)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x2236000C),
                                                Color(0x121A0005)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFF6B8B),
                                                Color(0x44FFFFFF)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x33FF2448),
                                                Color(0x15FFFFFF)
                                            )
                                        )
                                    },
                                    shape = pillShape
                                )
                                .clickable { activeFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF8E8E93)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. SECTION HEADER: "All Music / Plays"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeFilter == "All Songs") "All Music" else activeFilter,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (!hasAudioPermission && onRequestPermission != null) {
                        Button(
                            onClick = { onRequestPermission() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE51B3E),
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
            }

            // 5. GRADIENT MUSIC CARDS (Gradient Plays)
            if (displayedTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 20.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x1F220006))
                            .border(1.dp, Color(0x33FF2448), RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFFFF4D6D),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!hasAudioPermission) "Permission Required" else "No Audio Files Found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (!hasAudioPermission)
                                    "Allow Velvet to access device audio files to play your music"
                                else
                                    "Add audio files to your device storage to see them here",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedTracks, key = { it.id }) { track ->
                    val isCurrent = track.id == currentTrack.id
                    GradientMusicPlayCard(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSelectTrack(track) },
                        onMenuClick = { onTrackMenuClick(track) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Premium Gradient Music Card (Gradient Play)
 */
@Composable
fun GradientMusicPlayCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = track.title.substringBefore(" - ")
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isCurrent) 8.dp else 2.dp,
                shape = cardShape,
                spotColor = if (isCurrent) Color(0x77FF2448) else Color(0x2248000C)
            )
            .clip(cardShape)
            .background(
                if (isCurrent) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0x554A0012),
                            Color(0x30230009),
                            Color(0x180D0004)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0x2436000C),
                            Color(0x141A0005),
                            Color(0x0C080002)
                        )
                    )
                }
            )
            .border(
                width = if (isCurrent) 1.2.dp else 0.8.dp,
                brush = if (isCurrent) {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF2448),
                            Color(0x88FF4D6D),
                            Color(0x44FF2448)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0x44FF2448),
                            Color(0x15FFFFFF),
                            Color(0x22FF2448)
                        )
                    )
                },
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("standalone_track_${track.id}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork Container with Gradient Rim
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isCurrent) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                TrackArtworkImage(
                    track = track,
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
                            tint = Color(0xFFFF2448),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, and Format Pill
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanTitle,
                    fontSize = 14.5.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) Color(0xFFFF4D6D) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = Color(0xFF555555)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x28FF2448))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (track.isLossless) "FLAC" else "320K",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4D6D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Equalizer Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent && isPlaying) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF2448),
                                    Color(0xFFB50E29)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x3348000C),
                                    Color(0x1F220006)
                                )
                            )
                        }
                    )
                    .border(
                        0.8.dp,
                        if (isCurrent && isPlaying) Color(0xFFFF4D6D) else Color(0x33FF2448),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Standing straight vertical three dots
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("track_menu_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Track Menu",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Top Rank Track Row inside Hero Card
 */
@Composable
fun GradientMostPlayedTrackRow(
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
            .background(if (isCurrent) Color(0x33FF2448) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("compact_most_played_rank_$rank"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact artwork (46.dp)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(11.dp))
                .border(
                    1.dp,
                    if (isCurrent) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(11.dp)
                )
        ) {
            TrackArtworkImage(
                track = track,
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
                        tint = Color(0xFFFF2448),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) Color(0xFFFF4D6D) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FF2448))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "TOP #$rank",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF4D6D)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (playCount > 1) "Played ${playCount}x" else track.artist,
                    fontSize = 11.5.sp,
                    color = Color(0xFFB0959B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isCurrent && isPlaying) Color(0xFFE51B3E) else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x3348000C),
                        Color(0x1F220006)
                    )
                )
            )
            .border(1.dp, Color(0x33FF2448), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
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
    GradientMusicPlayCard(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onMenuClick = onMenuClick
    )
}

@Composable
fun RecentlyPlayedRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientMusicPlayCard(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onMenuClick = onMenuClick,
        modifier = modifier
    )
}
