package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

@Composable
fun ExploreScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    tracks: List<Track> = emptyList(),
    onSelectTrack: (Track) -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    onOpenDownloadSheet: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCatalog by remember { mutableStateOf("All Audio") }

    // Recognize whether current input is a supported media link
    val isMediaLink = remember(searchQuery) {
        val trimmed = searchQuery.trim()
        trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains("tiktok.com", ignoreCase = true) ||
                trimmed.contains("instagram.com", ignoreCase = true) ||
                trimmed.contains("facebook.com", ignoreCase = true) ||
                trimmed.contains("fb.watch", ignoreCase = true)
    }

    val filteredTracks = remember(searchQuery, selectedCatalog, tracks, isMediaLink) {
        if (isMediaLink) {
            tracks // Keep all tracks accessible beneath link state
        } else {
            tracks.filter { track ->
                val matchesQuery = searchQuery.isBlank() ||
                        track.title.contains(searchQuery, ignoreCase = true) ||
                        track.artist.contains(searchQuery, ignoreCase = true) ||
                        track.album.contains(searchQuery, ignoreCase = true)

                matchesQuery
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "Music Library",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tracks.size} audio tracks on your device",
                    fontSize = 13.sp,
                    color = VelvetTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Intelligent Search & Paste-Link Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("explore_search_input"),
                    placeholder = {
                        Text(
                            text = "Search songs, artists, albums... or paste link",
                            color = VelvetTextTertiary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        AnimatedContent(
                            targetState = isMediaLink,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                            },
                            label = "leadingIcon"
                        ) { hasLink ->
                            if (hasLink) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Media Link",
                                    tint = VelvetBrightCrimson
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = VelvetTextSecondary
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (isMediaLink) {
                                // Intelligent Process Link action button inside the search bar
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(VelvetBrightCrimson, VelvetBloodPlum)
                                            )
                                        )
                                        .clickable {
                                            onOpenDownloadSheet(searchQuery.trim())
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Download",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Open Downloader",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = VelvetTextSecondary
                                    )
                                }
                            } else {
                                // Quick Paste pill when field is empty
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text
                                        if (!clipText.isNullOrBlank()) {
                                            val trimmed = clipText.trim()
                                            searchQuery = trimmed
                                            val isUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                                                    trimmed.startsWith("https://", ignoreCase = true) ||
                                                    trimmed.contains("tiktok.com", ignoreCase = true) ||
                                                    trimmed.contains("instagram.com", ignoreCase = true)
                                            if (isUrl) {
                                                onOpenDownloadSheet(trimmed)
                                            }
                                        } else {
                                            onOpenDownloadSheet(null)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Link",
                                        tint = VelvetTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isMediaLink) VelvetBrightCrimson else VelvetBrightCrimson,
                        unfocusedBorderColor = if (isMediaLink) VelvetBrightCrimson.copy(alpha = 0.6f) else VelvetBorder,
                        focusedContainerColor = VelvetSurfaceElevated,
                        unfocusedContainerColor = VelvetSurfaceElevated,
                        focusedTextColor = VelvetTextPrimary,
                        unfocusedTextColor = VelvetTextPrimary
                    ),
                    singleLine = true
                )

                // Smooth transform banner when a media link is typed/pasted
                AnimatedVisibility(
                    visible = isMediaLink,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VelvetBrightCrimson.copy(alpha = 0.15f))
                            .border(1.dp, VelvetBrightCrimson.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                onOpenDownloadSheet(searchQuery.trim())
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Supported Media Link Detected",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap to open full media downloader",
                                    fontSize = 11.5.sp,
                                    color = VelvetTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = VelvetBrightCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Original standalone music tracks list
        items(filteredTracks) { track ->
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

        if (filteredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No tracks found matching \"$searchQuery\"" else "No audio files found on device",
                        fontSize = 13.sp,
                        color = VelvetTextTertiary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
