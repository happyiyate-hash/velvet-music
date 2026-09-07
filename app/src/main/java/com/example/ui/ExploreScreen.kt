package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
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
    onOpenLinkDownloader: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCatalog by remember { mutableStateOf("All Audio") }
    var downloaderUrl by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val filteredTracks = remember(searchQuery, selectedCatalog, tracks) {
        tracks.filter { track ->
            searchQuery.isBlank() ||
                track.title.contains(searchQuery, ignoreCase = true) ||
                track.artist.contains(searchQuery, ignoreCase = true) ||
                track.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val isMediaUrl = remember(searchQuery) { isSupportedMediaUrl(searchQuery) }

    fun openDownloader(value: String?) {
        downloaderUrl = value
        onOpenLinkDownloader(value)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("explore_screen"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text("Music Library", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VelvetTextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${tracks.size} audio tracks on your device", fontSize = 13.sp, color = VelvetTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("explore_search_input"),
                        placeholder = { Text("Search songs, artists, albums...", color = VelvetTextTertiary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = VelvetTextSecondary) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "Clear", tint = VelvetTextSecondary) }
                                }
                                AnimatedContent(targetState = isMediaUrl, label = "link_action") { detected ->
                                    IconButton(onClick = {
                                        if (detected) {
                                            openDownloader(searchQuery.trim())
                                        } else {
                                            val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                            if (pasted.isNotBlank()) {
                                                searchQuery = pasted
                                                if (isSupportedMediaUrl(pasted)) openDownloader(pasted)
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (detected) Icons.Default.Download else Icons.Default.ContentPaste,
                                            contentDescription = if (detected) "Process link" else "Paste link",
                                            tint = if (detected) VelvetBrightCrimson else VelvetTextSecondary
                                        )
                                    }
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
                    if (isMediaUrl) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .05f)).clickable { openDownloader(searchQuery.trim()) }.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Download, "Process link", tint = VelvetBrightCrimson, modifier = Modifier.size(17.dp))
                            Text("Process media link", color = VelvetTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            items(filteredTracks) { track ->
                val isCurrent = track.id == currentTrack.id
                StandaloneMusicRow(track = track, isCurrent = isCurrent, isPlaying = isPlaying && isCurrent, onClick = { onSelectTrack(track) }, onMenuClick = { onTrackMenuClick(track) })
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (filteredTracks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isNotBlank()) "No tracks found matching \"$searchQuery\"" else "No audio files found on device", fontSize = 13.sp, color = VelvetTextTertiary)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        if (downloaderUrl != null) {
            Dialog(
                onDismissRequest = { downloaderUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                MediaDownloadSheet(initialUrl = downloaderUrl, onDismiss = { downloaderUrl = null })
            }
        }
    }
}

private fun isSupportedMediaUrl(value: String): Boolean {
    val v = value.trim().lowercase()
    return (v.startsWith("https://") || v.startsWith("http://")) &&
        (v.contains("tiktok.com") || v.contains("instagram.com") || v.contains("facebook.com") || v.contains("fb.watch") || v.contains(".mp4") || v.contains(".mp3"))
}
