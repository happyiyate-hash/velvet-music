package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

private data class MockMix(
    val id: String,
    val title: String,
    val curator: String,
    val topColor: Color,
    val bottomColor: Color,
    val hasWaveform: Boolean = false,
    val hasSun: Boolean = false,
    val hasMoon: Boolean = false
)

private val curatedMixes = listOf(
    MockMix("mix_1", "Luminous Echoes", "(Curated by Tame Impala)", Color(0xFF6E1828), Color(0xFF280812), hasMoon = true),
    MockMix("mix_2", "Acoustic Waves", "(Curated by Bon Iver)", Color(0xFF2D3B4C), Color(0xFF141A22), hasWaveform = true),
    MockMix("mix_3", "Sunset Beats", "(Curated by Kaytranada)", Color(0xFF7E2A38), Color(0xFF380E18), hasSun = true),
    MockMix("mix_4", "Night Grooves", "(Curated by Khruangbin)", Color(0xFF451E4E), Color(0xFF1A0A20), hasMoon = true),
    MockMix("mix_5", "Braw Beats", "(Curated by SZA)", Color(0xFF602835), Color(0xFF220C14), hasSun = true)
)

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
    val recentTracks = remember(mostPlayedTracks, allTracks) {
        if (mostPlayedTracks.isNotEmpty()) mostPlayedTracks.take(8)
        else if (allTracks.isNotEmpty()) allTracks.take(8)
        else SampleData.starterTracks.take(6)
    }

    val displayTracks = remember(allTracks) {
        if (allTracks.isNotEmpty()) allTracks else SampleData.starterTracks
    }

    val velvetBackgroundGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4C0E1B), // Rich atmospheric crimson wine top
                Color(0xFF300812),
                Color(0xFF1B040A),
                Color(0xFF100206)  // Midnight plum black base
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(velvetBackgroundGradient)
            .testTag("home_feed_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {
            // 1. TOP HEADER: "Welcome back, Echo" + "Home Feed" + 3 action buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back, Echo",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFDCA8B0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Home Feed",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 3 Frosted squircle action buttons matching mockup
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderFrostedButton(
                            icon = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            onClick = onOpenNotifications
                        )
                        HeaderFrostedButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings
                        )
                        HeaderFrostedButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Search",
                            onClick = onOpenSearch
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. PERSONALIZED MIXES: Large frosted glass card container
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x354E0E1B))
                        .border(
                            width = 1.dp,
                            color = Color(0x30FFAAB8),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Personalized Mixes",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF8D8DE)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Curated mixes grid / horizontal flow
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(curatedMixes, key = { it.id }) { mix ->
                                CuratedMixCard(
                                    mix = mix,
                                    onClick = {
                                        val match = displayTracks.firstOrNull() ?: currentTrack
                                        onSelectTrack(match)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
            }

            // 3. RECENTLY PLAYED SECTION
            item {
                Text(
                    text = "Recently Played",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(recentTracks, key = { "recent_${it.id}" }) { track ->
                val isCurrent = track.id == currentTrack.id
                MockupTrackRowItem(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying && isCurrent,
                    onClick = { onSelectTrack(track) },
                    onMenuClick = { onTrackMenuClick(track) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }

            // 4. DISCOVER NEW RELEASES SECTION: Rounded card container
            item {
                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x28380A14))
                        .border(1.dp, Color(0x22FFAAB8), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Discover New Releases",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF8D8DE)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayTracks.take(6), key = { "new_${it.id}" }) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .clickable { onSelectTrack(track) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF220812))
                                    ) {
                                        TrackArtworkImage(
                                            track = track,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = track.title.substringBefore(" - "),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFC098A2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderFrostedButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x35601220))
            .border(1.dp, Color(0x35FFAAB8), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFF2D0D6),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun CuratedMixCard(
    mix: MockMix,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        // Artwork Canvas Box with custom art matching mockup
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(mix.topColor, mix.bottomColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (mix.hasWaveform) {
                    // Acoustic waves pattern
                    val barWidth = 3.dp.toPx()
                    val barSpacing = 5.dp.toPx()
                    val bars = listOf(0.3f, 0.55f, 0.85f, 1.0f, 0.75f, 0.45f, 0.25f)
                    val startX = (w - (bars.size * (barWidth + barSpacing))) / 2f
                    bars.forEachIndexed { i, factor ->
                        val barH = h * 0.35f * factor
                        drawRoundRect(
                            color = Color(0xFFFFB0BD).copy(alpha = 0.85f),
                            topLeft = Offset(startX + i * (barWidth + barSpacing), (h - barH) / 2f),
                            size = androidx.compose.ui.geometry.Size(barWidth, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                        )
                    }
                } else if (mix.hasSun) {
                    // Sunset Beats warm sun over dunes
                    drawCircle(
                        color = Color(0xFFFF6B7D).copy(alpha = 0.8f),
                        radius = w * 0.22f,
                        center = Offset(w * 0.5f, h * 0.45f)
                    )
                    drawCircle(
                        color = mix.bottomColor.copy(alpha = 0.95f),
                        radius = w * 0.5f,
                        center = Offset(w * 0.5f, h * 1.1f)
                    )
                } else if (mix.hasMoon) {
                    // Crescent moon silhouette
                    drawCircle(
                        color = Color(0xFFFFD4DC).copy(alpha = 0.8f),
                        radius = w * 0.22f,
                        center = Offset(w * 0.55f, h * 0.45f)
                    )
                    drawCircle(
                        color = mix.topColor,
                        radius = w * 0.20f,
                        center = Offset(w * 0.62f, h * 0.40f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = mix.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = mix.curator,
            fontSize = 10.5.sp,
            color = Color(0xFFD4A5AC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MockupTrackRowItem(
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with 8dp rounded corners
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF280A12))
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
                        tint = Color(0xFFFF3B5C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) Color(0xFFFF6078) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = Color(0xFFC098A2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Horizontal 3 dots matching mockup!
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Track options",
                tint = Color(0xFF90757C),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
