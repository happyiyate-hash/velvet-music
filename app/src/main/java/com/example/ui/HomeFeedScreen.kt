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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.example.model.Mix
import com.example.model.SampleData
import com.example.model.Track
import com.example.ui.theme.VelvetAshGray
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetAshGrayLight
import com.example.ui.theme.VelvetAshGrayMedium
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetCardBorder
import com.example.ui.theme.VelvetGlassSurface
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import com.example.ui.theme.VelvetWhiteAsh

@Composable
fun HomeFeedScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    onSelectMix: (Mix) -> Unit,
    onSelectTrack: (Track) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_screen"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
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

        // 2. "Personalized Mixes" Frosted Ash-Glass Container (matching screenshot)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to VelvetOffBloodTop.copy(alpha = 0.40f),
                                0.35f to VelvetGlassSurface.copy(alpha = 0.65f),
                                1.0f to VelvetAshGrayDark.copy(alpha = 0.80f)
                            )
                        )
                    )
                    .border(1.dp, VelvetCardBorder, RoundedCornerShape(28.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Personalized Mixes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val mixes = SampleData.allMixes

                    // Row 1 of mixes (scrollable/peeking)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(mixes.take(2)) { mix ->
                            MixGridCard(
                                mix = mix,
                                onClick = { onSelectMix(mix) },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2 of mixes (scrollable/peeking)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(mixes.drop(2)) { mix ->
                            MixGridCard(
                                mix = mix,
                                onClick = { onSelectMix(mix) },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. "Recently Played" Section (matching screenshot)
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
                    Text(
                        text = "Recently Played",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetTextPrimary
                    )

                    IconButton(
                        onClick = { /* More recent options */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = VelvetTextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SampleData.recentlyPlayedTracks.forEach { track ->
                    val isCurrent = track.id == currentTrack.id
                    RecentlyPlayedRow(
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

        // 4. "Discover New Releases" Ash Glass Container (matching screenshot)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                VelvetAshGrayMedium.copy(alpha = 0.40f),
                                VelvetAshGrayDark.copy(alpha = 0.70f)
                            )
                        )
                    )
                    .border(1.dp, VelvetCardBorder, RoundedCornerShape(26.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Discover New Releases",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SampleData.newReleases) { release ->
                            NewReleaseMiniCard(
                                track = release,
                                onClick = { onSelectTrack(release) }
                            )
                        }
                    }
                }
            }
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
            tint = VelvetWhiteAsh,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun MixGridCard(
    mix: Mix,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("mix_card_${mix.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, VelvetCardBorder, RoundedCornerShape(16.dp))
                .background(VelvetAshGrayDark)
        ) {
            Image(
                painter = painterResource(id = mix.coverResId),
                contentDescription = mix.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mix.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VelvetTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = mix.curator,
            fontSize = 11.sp,
            color = VelvetTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) VelvetBloodPlum.copy(alpha = 0.35f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("recently_played_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, VelvetCardBorder, RoundedCornerShape(10.dp))
                .background(VelvetAshGrayDark)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${track.title} - ${track.artist}",
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = VelvetTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("track_menu_${track.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Track Menu",
                tint = VelvetTextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun NewReleaseMiniCard(
    track: Track,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("new_release_${track.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, VelvetCardBorder, RoundedCornerShape(14.dp))
                .background(VelvetAshGrayDark)
        ) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VelvetTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            fontSize = 11.sp,
            color = VelvetTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
