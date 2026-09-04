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
    onSelectTrack: (Track) -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCatalog by remember { mutableStateOf("All Catalogs") }

    val catalogs = listOf("All Catalogs", "Jamendo", "Audius", "Archive.org", "Free Music Archive")
    val allTracks = remember {
        (SampleData.recentlyPlayedTracks + SampleData.newReleases + SampleData.allMixes.flatMap { it.tracks }).distinctBy { it.id }
    }

    val filteredTracks = remember(searchQuery, selectedCatalog) {
        allTracks.filter { track ->
            val matchesQuery = searchQuery.isBlank() ||
                    track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true)

            val matchesCatalog = selectedCatalog == "All Catalogs" ||
                    track.catalogSource.contains(selectedCatalog, ignoreCase = true)

            matchesQuery && matchesCatalog
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
    ) {
        item {
            Text(
                text = "Search & Legal Catalogs",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VelvetTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Stream royalty-free and public domain ambient catalogs with AAC audio",
                fontSize = 13.sp,
                color = VelvetTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input"),
                placeholder = { Text("Search tracks, artists, moods...", color = VelvetTextTertiary) },
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
                shape = RoundedCornerShape(16.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // Catalog Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(catalogs) { catalog ->
                    val isSelected = catalog == selectedCatalog
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) VelvetBrightCrimson else VelvetSurfaceElevated)
                            .border(1.dp, if (isSelected) VelvetBrightCrimson else VelvetBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedCatalog = catalog }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("catalog_chip_$catalog")
                    ) {
                        Text(
                            text = catalog,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else VelvetTextSecondary
                        )
                    }
                }
            }
        }

        items(filteredTracks) { track ->
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

        if (filteredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks found matching \"$searchQuery\"",
                        fontSize = 14.sp,
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
