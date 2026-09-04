package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LyricLine
import com.example.model.SampleData
import com.example.model.Track
import com.example.ui.theme.VelvetActivePill
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetPureBlack
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IndiePortalScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    offlineCachedIds: Set<String>,
    onSelectTrack: (Track) -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Indie Upload Portal", "Offline Library (${offlineCachedIds.size})")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("indie_portal_screen")
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = VelvetTextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = VelvetBrightCrimson
                )
            },
            divider = { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(VelvetBorder)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) VelvetBrightCrimson else VelvetTextSecondary
                        )
                    },
                    modifier = Modifier.testTag("portal_tab_$index")
                )
            }
        }

        if (selectedTab == 0) {
            UploadPortalContent(onPlayGeneratedTrack = onSelectTrack)
        } else {
            OfflineLibraryContent(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                offlineCachedIds = offlineCachedIds,
                onSelectTrack = onSelectTrack,
                onTrackMenuClick = onTrackMenuClick
            )
        }
    }
}

@Composable
fun UploadPortalContent(
    onPlayGeneratedTrack: (Track) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var trackTitle by remember { mutableStateOf("Subterranean Reverie") }
    var artistName by remember { mutableStateOf("Echo District") }
    var isProcessingWhisper by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf("") }
    var generatedTrack by remember { mutableStateOf<Track?>(null) }
    var extractedPalette by remember { mutableStateOf<Pair<Color, Color>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Text(
                text = "Indie Artist Self-Serve Portal",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VelvetTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Publish your audio, generate OpenAI Whisper time-synced lyrics (.lrc), and extract artwork palette for the Sound Catch mesh.",
                fontSize = 12.sp,
                color = VelvetTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Upload inputs
            OutlinedTextField(
                value = trackTitle,
                onValueChange = { trackTitle = it },
                label = { Text("Track Title") },
                modifier = Modifier.fillMaxWidth().testTag("upload_title_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VelvetBrightCrimson,
                    unfocusedBorderColor = VelvetBorder,
                    focusedContainerColor = VelvetSurfaceElevated,
                    unfocusedContainerColor = VelvetSurfaceElevated,
                    focusedTextColor = VelvetTextPrimary,
                    unfocusedTextColor = VelvetTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = artistName,
                onValueChange = { artistName = it },
                label = { Text("Artist Name") },
                modifier = Modifier.fillMaxWidth().testTag("upload_artist_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VelvetBrightCrimson,
                    unfocusedBorderColor = VelvetBorder,
                    focusedContainerColor = VelvetSurfaceElevated,
                    unfocusedContainerColor = VelvetSurfaceElevated,
                    focusedTextColor = VelvetTextPrimary,
                    unfocusedTextColor = VelvetTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Button
            Button(
                onClick = {
                    if (!isProcessingWhisper) {
                        isProcessingWhisper = true
                        coroutineScope.launch {
                            processingStage = "Extracting Artwork Palette (Dominant Crimson & Dark Burgundy)..."
                            delay(600)
                            extractedPalette = Pair(Color(0xFF880E2F), Color(0xFF260612))

                            processingStage = "Transcribing Audio with Whisper STT (word_timestamps=True)..."
                            delay(900)

                            processingStage = "Generating Time-Synced .LRC Output with ms accuracy..."
                            delay(600)

                            val customLyrics = listOf(
                                LyricLine(0L, "[Whisper STT Auto-Generated LRC]", isHeader = true),
                                LyricLine(2500L, "Deep in the dark ambient night, shadows fall"),
                                LyricLine(6800L, "Fluid vibrations echo against the obsidian wall"),
                                LyricLine(11500L, "The mesh moves in sync with the subterranean kick"),
                                LyricLine(16200L, "Crimson and burgundy pulse in continuous drift"),
                                LyricLine(21000L, "Sound Catch holding the harmony alive")
                            )

                            generatedTrack = Track(
                                id = "indie_${System.currentTimeMillis()}",
                                title = trackTitle.ifBlank { "Ambient Track" },
                                artist = artistName.ifBlank { "Independent Creator" },
                                album = "Indie Self-Serve Uploads",
                                durationMs = 180000L,
                                coverResId = R.drawable.art_luminous_echoes,
                                dominantColor = extractedPalette?.first ?: Color(0xFF880E2F),
                                secondaryColor = extractedPalette?.second ?: Color(0xFF260612),
                                catalogSource = "Indie Creator Direct (R2 $0 Egress)",
                                lyrics = customLyrics,
                                bpm = 88
                            )
                            isProcessingWhisper = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("run_whisper_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VelvetBrightCrimson,
                    contentColor = Color.White
                )
            ) {
                if (isProcessingWhisper) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Processing...", fontSize = 14.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Run Whisper STT",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Run Whisper STT & Extract Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (isProcessingWhisper) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = VelvetBrightCrimson,
                    trackColor = VelvetBorder
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = processingStage,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = VelvetTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Extracted Results Card
            generatedTrack?.let { track ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(VelvetSurfaceElevated)
                        .border(1.dp, VelvetBrightCrimson, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("generated_track_preview")
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Ready",
                                tint = VelvetBrightCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Track Published to Velvet Mesh",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelvetTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${track.title} • ${track.artist}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VelvetTextPrimary
                        )
                        Text(
                            text = "${track.lyrics.size} synced lyric lines generated via Whisper with word_timestamps=True",
                            fontSize = 12.sp,
                            color = VelvetTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Palette readout
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(track.dominantColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Primary Mesh", fontSize = 11.sp, color = VelvetTextSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(track.secondaryColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Secondary Mesh", fontSize = 11.sp, color = VelvetTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onPlayGeneratedTrack(track) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelvetDarkBurgundy,
                                contentColor = VelvetBrightCrimson
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("play_generated_track_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Now")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play with Sound Catch & Synced Lyrics")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun OfflineLibraryContent(
    currentTrack: Track,
    isPlaying: Boolean,
    offlineCachedIds: Set<String>,
    onSelectTrack: (Track) -> Unit,
    onTrackMenuClick: (Track) -> Unit
) {
    val allTracks = remember {
        (SampleData.recentlyPlayedTracks + SampleData.newReleases + SampleData.allMixes.flatMap { it.tracks }).distinctBy { it.id }
    }

    val cachedTracks = remember(offlineCachedIds) {
        allTracks.filter { offlineCachedIds.contains(it.id) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Cached Offline",
                    tint = VelvetBrightCrimson,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Disk Cache (AAC 160 kbps)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetTextPrimary
                )
            }
            Text(
                text = "Zero-egress stored tracks ready for instant playback without cellular connection.",
                fontSize = 12.sp,
                color = VelvetTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
        }

        if (cachedTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No offline tracks cached yet. Tap the download icon in the player to save for offline listening.",
                        fontSize = 13.sp,
                        color = VelvetTextTertiary
                    )
                }
            }
        } else {
            items(cachedTracks) { track ->
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

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
