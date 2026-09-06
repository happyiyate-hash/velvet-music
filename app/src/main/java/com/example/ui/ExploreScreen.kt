package com.example.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SampleData
import com.example.model.Track
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
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCatalog by remember { mutableStateOf("All Audio") }

    val catalogs = listOf("All Audio", "Device Tracks", "Internal Storage")

    val filteredTracks = remember(searchQuery, selectedCatalog, tracks) {
        tracks.filter { track ->
            val matchesQuery = searchQuery.isBlank() ||
                    track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true) ||
                    track.album.contains(searchQuery, ignoreCase = true)

            matchesQuery
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

                // Search Bar with reduced margins
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input"),
                    placeholder = { Text("Search songs, artists, albums...", color = VelvetTextTertiary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VelvetTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = VelvetTextSecondary
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelvetBrightCrimson,
                        unfocusedBorderColor = VelvetBorder,
                        focusedContainerColor = VelvetSurfaceElevated,
                        unfocusedContainerColor = VelvetSurfaceElevated,
                        focusedTextColor = VelvetTextPrimary,
                        unfocusedTextColor = VelvetTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

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
