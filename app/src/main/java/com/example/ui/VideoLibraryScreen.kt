package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.media.DeviceMediaManager
import com.example.model.DeviceVideo
import com.example.model.SampleData
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CuratedMusicVideo(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String,
    val viewsText: String,
    val category: String,
    val thumbnailResId: Int,
    val resolutionBadge: String = "4K",
    val associatedTrackId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    modifier: Modifier = Modifier,
    onSelectTrackAudio: ((Track) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var deviceVideos by remember { mutableStateOf<List<DeviceVideo>>(DeviceMediaManager.getCachedVideos(context)) }
    var activeCategory by remember { mutableStateOf("All") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activePlayingVideo by remember { mutableStateOf<CuratedMusicVideo?>(null) }
    var selectedVideoForShare by remember { mutableStateOf<CuratedMusicVideo?>(null) }

    // Curated high quality music videos
    val curatedVideos = remember {
        listOf(
            CuratedMusicVideo(
                id = "vid_after_hours",
                title = "After Hours (Short Film & Official Video)",
                artist = "The Weeknd",
                durationText = "05:20",
                viewsText = "84M views • 2 weeks ago",
                category = "Official Videos",
                thumbnailResId = R.drawable.art_after_hours,
                resolutionBadge = "4K HDR",
                associatedTrackId = "starter_after_hours"
            ),
            CuratedMusicVideo(
                id = "vid_good_news",
                title = "Good News (Official Music Video)",
                artist = "Mac Miller",
                durationText = "04:45",
                viewsText = "112M views • 1 month ago",
                category = "Official Videos",
                thumbnailResId = R.drawable.art_good_news,
                resolutionBadge = "4K",
                associatedTrackId = "starter_good_news"
            ),
            CuratedMusicVideo(
                id = "vid_blinding_lights",
                title = "Blinding Lights (Official Music Video)",
                artist = "The Weeknd",
                durationText = "04:22",
                viewsText = "980M views • 3 months ago",
                category = "Official Videos",
                thumbnailResId = R.drawable.art_blinding_lights,
                resolutionBadge = "4K HDR",
                associatedTrackId = "starter_blinding_lights"
            ),
            CuratedMusicVideo(
                id = "vid_luminous_echoes",
                title = "Luminous Echoes (Psychedelic Visualizer)",
                artist = "Tame Impala",
                durationText = "04:05",
                viewsText = "14M views • 5 days ago",
                category = "Visualizers",
                thumbnailResId = R.drawable.art_luminous_echoes,
                resolutionBadge = "1080p",
                associatedTrackId = "starter_luminous_echoes"
            ),
            CuratedMusicVideo(
                id = "vid_sunset_beats",
                title = "Sunset Beats (Live Studio Horizon Set)",
                artist = "Kaytranada",
                durationText = "03:50",
                viewsText = "9.8M views • 3 weeks ago",
                category = "Live Sets",
                thumbnailResId = R.drawable.art_sunset_beats,
                resolutionBadge = "4K",
                associatedTrackId = "starter_sunset_beats"
            ),
            CuratedMusicVideo(
                id = "vid_heaven_baby",
                title = "Heaven Baby (Official Studio Performance)",
                artist = "Ayra Starr ft. ZAYN",
                durationText = "03:30",
                viewsText = "28M views • 1 month ago",
                category = "Live Sets",
                thumbnailResId = R.drawable.art_night_grooves,
                resolutionBadge = "4K",
                associatedTrackId = "starter_heaven_baby"
            ),
            CuratedMusicVideo(
                id = "vid_acoustic_waves",
                title = "Acoustic Waves (Ethereal Visual Experience)",
                artist = "Bon Iver",
                durationText = "04:10",
                viewsText = "19M views • 2 months ago",
                category = "Visualizers",
                thumbnailResId = R.drawable.art_acoustic_waves,
                resolutionBadge = "1080p",
                associatedTrackId = "starter_good_news"
            )
        )
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (DeviceMediaManager.hasVideoPermission(context)) {
            coroutineScope.launch(Dispatchers.IO) {
                deviceVideos = DeviceMediaManager.loadDeviceVideos(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (DeviceMediaManager.hasVideoPermission(context)) {
            val loaded = withContext(Dispatchers.IO) {
                DeviceMediaManager.loadDeviceVideos(context)
            }
            deviceVideos = loaded
        } else {
            videoPermissionLauncher.launch(DeviceMediaManager.requiredVideoPermissions)
        }
    }

    val categories = listOf("All", "Official Videos", "Live Sets", "Visualizers")

    val displayedVideos = remember(curatedVideos, activeCategory, searchQuery) {
        curatedVideos.filter { video ->
            val matchesCategory = activeCategory == "All" || video.category == activeCategory
            val matchesQuery = searchQuery.isBlank() ||
                video.title.contains(searchQuery, ignoreCase = true) ||
                video.artist.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_library_screen")
    ) {
        // Deep obsidian background with upper ruby aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF030002))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x35FF2448),
                        Color(0x1545000F),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.08f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.5f, h * 0.08f),
                radius = w * 0.85f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {
            // 1. Clean Top Header: Title & Action Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Music Videos",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Curated music videos, visualizers & live sets",
                            fontSize = 12.5.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    // Frosted Search Toggle Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0x28380010))
                            .border(0.8.dp, Color(0x33FF2448), RoundedCornerShape(11.dp))
                            .clickable { isSearchVisible = !isSearchVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search videos",
                            tint = if (isSearchVisible) Color(0xFFFF2448) else Color(0xFFE2E4EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Optional Search Bar
            if (isSearchVisible) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search title, artist...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1F220006)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF2448),
                                unfocusedBorderColor = Color(0x33FF2448),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 2. Clean Category Filter Pills
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = activeCategory == category
                        val pillShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFE51B3E),
                                                Color(0xFF99001C)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x28380010),
                                                Color(0x18180006)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = 0.8.dp,
                                    brush = if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFF5277),
                                                Color(0x55FFFFFF)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x28FFFFFF),
                                                Color(0x10FFFFFF)
                                            )
                                        )
                                    },
                                    shape = pillShape
                                )
                                .clickable { activeCategory = category }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFD0D3DC)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // 3. Clean Video Items (Clean 16:9 widescreen thumbnails directly on canvas, NO bloated cards!)
            items(displayedVideos, key = { it.id }) { video ->
                CleanVideoItem(
                    video = video,
                    onPlayVideo = { activePlayingVideo = video },
                    onPlayAudio = {
                        val track = SampleData.starterTracks.find { it.id == video.associatedTrackId }
                            ?: SampleData.starterTracks.firstOrNull()
                        track?.let { onSelectTrackAudio?.invoke(it) }
                    },
                    onShare = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Watching ${video.title} on Velvet Music")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Interactive Video Player Bottom Sheet Dialog
        activePlayingVideo?.let { video ->
            CleanVideoPlayerSheet(
                video = video,
                onDismiss = { activePlayingVideo = null },
                onPlayAudio = {
                    val track = SampleData.starterTracks.find { it.id == video.associatedTrackId }
                        ?: SampleData.starterTracks.firstOrNull()
                    track?.let { onSelectTrackAudio?.invoke(it) }
                    activePlayingVideo = null
                }
            )
        }
    }
}

/**
 * Clean Modern Video Item
 */
@Composable
private fun CleanVideoItem(
    video: CuratedMusicVideo,
    onPlayVideo: () -> Unit,
    onPlayAudio: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onPlayVideo() }
    ) {
        // 16:9 Widescreen Video Thumbnail Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF140206))
                .border(0.6.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = video.thumbnailResId),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Center Frosted Play Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color(0x66FF2448), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color(0xFFFF2448),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Duration and Resolution Badge (Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${video.durationText} • ${video.resolutionBadge}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Video Info & Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${video.artist} • ${video.viewsText}",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Audio & Share Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Play as audio",
                        tint = Color(0xFFFF4D6D),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Clean Video Player Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanVideoPlayerSheet(
    video: CuratedMusicVideo,
    onDismiss: () -> Unit,
    onPlayAudio: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progressFraction by remember { mutableFloatStateOf(0.24f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0C0104),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Bar of Player
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Now Playing Video",
                    fontSize = 13.sp,
                    color = Color(0xFFFF4D6D),
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Video Screen Mockup with Real Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                Image(
                    painter = painterResource(id = video.thumbnailResId),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (isPlaying) 0.25f else 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE51B3E))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Resolution Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = video.resolutionBadge, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrubber
            Slider(
                value = progressFraction,
                onValueChange = { progressFraction = it },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF2448),
                    activeTrackColor = Color(0xFFFF2448),
                    inactiveTrackColor = Color(0x33480010)
                ),
                modifier = Modifier.fillMaxWidth().height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "01:18", fontSize = 11.sp, color = Color(0xFF8E8E93))
                Text(text = video.durationText, fontSize = 11.sp, color = Color(0xFF8E8E93))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video Title & Artist
            Text(
                text = video.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = video.artist,
                fontSize = 13.sp,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Switch to Audio Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x28FF2448))
                    .clickable { onPlayAudio() }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = Color(0xFFFF4D6D),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Listen as Audio Only",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF4D6D)
                )
            }
        }
    }
}
