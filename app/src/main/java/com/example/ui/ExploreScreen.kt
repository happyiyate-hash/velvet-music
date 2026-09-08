package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

/**
 * ExploreScreen (Search Page)
 *
 * The baseline search experience is preserved in its entirety:
 * - Search songs, artists, albums, or videos from the local library / device
 * - Dedicated "Say it" and "Sing it" acoustic search pills
 * - Filter tabs: "All", "Music", "Video"
 * - High-contrast, sleek typography and spacious Velvet aesthetic
 *
 * Integrated capability:
 * - Recognizing media URLs (TikTok, Instagram, Web Media) entered or pasted in the search bar
 * - Seamlessly slides up the premium Media Downloader bottom sheet over the search page
 * - The search page remains intact underneath
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    tracks: List<Track> = emptyList(),
    onSelectTrack: (Track) -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    onAddTrack: ((Track) -> Unit)? = null,
    onOpenMediaDownloader: (initialUrl: String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Music", "Video"
    var showComingSoonDialog by remember { mutableStateOf<String?>(null) } // "Say it" or "Sing it"

    // Detect if entered text is a media link and seamlessly open the downloader sheet
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        val isUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains("tiktok.com", ignoreCase = true) ||
                trimmed.contains("instagram.com", ignoreCase = true)

        if (isUrl && trimmed.length > 8) {
            val urlToOpen = trimmed
            // Clear search query so the search results remain clean underneath
            searchQuery = ""
            onOpenMediaDownloader(urlToOpen)
        }
    }

    // Filter tracks based on normal search query and filter tab
    val filteredTracks = remember(searchQuery, selectedFilter, tracks) {
        tracks.filter { track ->
            val matchesType = when (selectedFilter) {
                "Music" -> true
                "Video" -> track.album.contains("video", ignoreCase = true) || track.title.contains("video", ignoreCase = true)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true) ||
                    track.album.contains(searchQuery, ignoreCase = true)

            matchesType && matchesQuery
        }
    }

    // Coming soon dialog for "Say it" and "Sing it"
    if (showComingSoonDialog != null) {
        val featureName = showComingSoonDialog ?: ""
        BasicAlertDialog(
            onDismissRequest = { showComingSoonDialog = null }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                VelvetOffBloodTop.copy(alpha = 0.95f),
                                Color(0xFF1E0A12).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                VelvetBrightCrimson.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        VelvetBrightCrimson.copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, VelvetBrightCrimson.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (featureName == "Say it") Icons.Default.Mic else Icons.Default.GraphicEq,
                            contentDescription = featureName,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = featureName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sorry for interrupting, this particular feature is coming soon.",
                        fontSize = 14.sp,
                        color = VelvetTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Our high-precision acoustic AI engine for audio and voice recognition is currently in development.",
                        fontSize = 12.sp,
                        color = VelvetTextTertiary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        VelvetBrightCrimson,
                                        VelvetBloodPlum
                                    )
                                )
                            )
                            .clickable { showComingSoonDialog = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Got it",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. HEADER SECTION (Spacious, elegant, uncluttered)
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Search",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search songs, artists, or paste media links",
                            fontSize = 13.sp,
                            color = VelvetTextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    // Subtle Link action chip (opens the downloader bottom sheet directly)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, VelvetBrightCrimson.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank() && (clipText.contains("tiktok") || clipText.contains("instagram") || clipText.startsWith("http"))) {
                                    onOpenMediaDownloader(clipText.trim())
                                } else {
                                    onOpenMediaDownloader(null)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("search_open_link_sheet"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Media Downloader",
                                tint = VelvetBrightCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Link",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VelvetTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. SEARCH / URL INPUT BAR WITH "Say it" & "Sing it" ICONS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp), spotColor = VelvetBrightCrimson.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2E131C).copy(alpha = 0.85f),
                                    Color(0xFF1E0B12).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    VelvetBrightCrimson.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Icon
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VelvetBrightCrimson,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Text Field
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search music or paste media link...",
                                    color = VelvetTextTertiary,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("explore_search_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = VelvetTextPrimary,
                                    unfocusedTextColor = VelvetTextPrimary,
                                    cursorColor = VelvetBrightCrimson
                               ),
                                singleLine = true
                            )
                        }

                        // Clear Button if text present
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = VelvetTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Dedicated "Say it" & "Sing it" Interactive Icons inside the search bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // "Say it" Icon Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showComingSoonDialog = "Say it" }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("search_say_it_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Say it",
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Say it",
                                        color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // "Sing it" Icon Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showComingSoonDialog = "Sing it" }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("search_sing_it_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Sing it",
                                        tint = VelvetBrightCrimson,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Sing it",
                                        color = VelvetBrightCrimson,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // 3. SEARCH FILTER TABS (All / Music / Video)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Music", "Video").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) VelvetBrightCrimson else Color.White.copy(alpha = 0.07f)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else VelvetTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. SEARCH RESULTS / LIBRARY TRACKS LIST
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

        if (filteredTracks.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "No results",
                            tint = VelvetTextTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No music or video matching \"$searchQuery\"",
                            fontSize = 13.sp,
                            color = VelvetTextTertiary
                        )
                    }
                }
            }
        }

        // Bottom space so items aren't obscured by mini player and bottom bar
        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}
