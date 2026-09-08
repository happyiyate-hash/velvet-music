package com.example.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
 * ExploreScreen — Redesigned Search Experience
 *
 * Minimal, premium, and focused on three primary top actions:
 * - Search It: Instant search focus & activation
 * - Paste It: One-tap media link integration (slides up MediaDownloaderSheet)
 * - Say It: Acoustic voice/sound search
 *
 * Features:
 * - Clean title: "Search Anything"
 * - Controlled atmospheric wine-crimson gradient top
 * - Compact refined search input with full width for long pasted URLs & vertical centering
 * - Lightweight search result rows with artwork, clear hierarchy, and far-right menu icon
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
    val clipboardManager = LocalClipboardManager.current
    val searchFocusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Music", "Video"
    var showComingSoonDialog by remember { mutableStateOf<String?>(null) } // "Say It"

    // Detect if entered text is a media link and seamlessly open the downloader sheet
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        val isUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains("tiktok.com", ignoreCase = true) ||
                trimmed.contains("instagram.com", ignoreCase = true)

        if (isUrl && trimmed.length > 8) {
            val urlToOpen = trimmed
            searchQuery = ""
            onOpenMediaDownloader(urlToOpen)
        }
    }

    // Filter tracks based on search query and filter tab
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

    // Acoustic search dialog for "Say It" and "Sing It"
    if (showComingSoonDialog != null) {
        val isSing = showComingSoonDialog?.equals("Sing it", ignoreCase = true) == true
        val dialogTitle = if (isSing) "Sing It" else "Say It"
        val dialogIcon = if (isSing) Icons.Default.GraphicEq else Icons.Default.Mic
        val dialogSubtitle = if (isSing) {
            "Melody recognition & humming search is preparing to launch."
        } else {
            "Voice audio & speech search is preparing to launch."
        }
        val dialogDesc = if (isSing) {
            "Hum, whistle, or sing any tune directly to identify and stream tracks in seconds."
        } else {
            "Speak track titles, artist names, or lyrics to search music instantly."
        }

        BasicAlertDialog(
            onDismissRequest = { showComingSoonDialog = null }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                VelvetOffBloodTop.copy(alpha = 0.96f),
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
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        VelvetBrightCrimson.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, VelvetBrightCrimson.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dialogIcon,
                            contentDescription = dialogTitle,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = dialogTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dialogSubtitle,
                        fontSize = 13.sp,
                        color = VelvetTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dialogDesc,
                        fontSize = 11.5.sp,
                        color = VelvetTextTertiary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
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
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // 1. CLEAN TOP HEADER: Title on left, the 3 Red Glass Action Cards on top-right
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Search",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetTextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Music, videos & links",
                            fontSize = 11.5.sp,
                            color = VelvetTextTertiary
                        )
                    }

                    // 3 Red Glass Cards clustered closely together at the top right corner
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RedGlassActionCard(
                            title = "Paste it",
                            icon = Icons.Default.ContentPaste,
                            onClick = {
                                val clipText = clipboardManager.getText()?.text?.trim()
                                if (!clipText.isNullOrBlank() && (
                                            clipText.startsWith("http://", ignoreCase = true) ||
                                            clipText.startsWith("https://", ignoreCase = true) ||
                                            clipText.contains("tiktok.com", ignoreCase = true) ||
                                            clipText.contains("instagram.com", ignoreCase = true)
                                        )) {
                                    onOpenMediaDownloader(clipText)
                                } else {
                                    onOpenMediaDownloader(null)
                                }
                            },
                            tag = "action_paste_it"
                        )

                        RedGlassActionCard(
                            title = "Say it",
                            icon = Icons.Default.Mic,
                            isComingSoon = true,
                            onClick = { showComingSoonDialog = "Say it" },
                            tag = "action_say_it"
                        )

                        RedGlassActionCard(
                            title = "Sing it",
                            icon = Icons.Default.GraphicEq,
                            isComingSoon = true,
                            onClick = { showComingSoonDialog = "Sing it" },
                            tag = "action_sing_it"
                        )
                    }
                }
            }

            // 2. REFINED SEARCH INPUT (Elevated to top, clean glass container, search action)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .border(
                            width = 0.8.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { searchFocusRequester.requestFocus() }
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Search Icon / Button
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchFocusRequester.requestFocus() }
                                .testTag("search_icon_button")
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Full-width Centered Text Field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search music, artists, or paste URL...",
                                    color = VelvetTextTertiary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .testTag("explore_search_input"),
                                textStyle = TextStyle(
                                    color = VelvetTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(VelvetBrightCrimson),
                                singleLine = true
                            )
                        }

                        // Clear Button when text is present
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = VelvetTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. FILTER TABS (Compact, subtle pills)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Music", "Video").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) VelvetBrightCrimson.copy(alpha = 0.90f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else VelvetTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. RESULTS SECTION HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Search Results" else "Results for \"$searchQuery\"",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextSecondary.copy(alpha = 0.90f),
                        letterSpacing = 0.4.sp
                    )

                    Text(
                        text = "${filteredTracks.size} tracks",
                        fontSize = 11.sp,
                        color = VelvetTextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // 7. LIGHTWEIGHT RESULT ITEMS (Artwork, hierarchy, far-right menu icon, refined padding)
            items(filteredTracks) { track ->
                val isCurrent = track.id == currentTrack.id
                SearchTrackResultRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying && isCurrent,
                    onClick = { onSelectTrack(track) },
                    onMenuClick = { onTrackMenuClick(track) }
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                                modifier = Modifier.size(32.dp)
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

            // Bottom space balancing the compact mini player and bottom navigation
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

/**
 * Lightweight, refined track result row for the Search experience:
 * - [Artwork]  Song title                         ⋮
 *              Artist • Source
 * - Far-right menu action with generous touch target
 * - Reduced vertical padding, no generic heavy dark card
 */
@Composable
private fun SearchTrackResultRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val cleanTitle = track.title.substringBefore(" - ")
    val sourceLabel = if (track.catalogSource.isNotBlank()) track.catalogSource else track.album

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isCurrent) Color(0xFF280C19).copy(alpha = 0.50f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("standalone_track_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact Artwork
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
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
                        tint = VelvetBrightCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Metadata with strong visual hierarchy
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = cleanTitle,
                fontSize = 13.5.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) VelvetBrightCrimson else VelvetTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${track.artist} • $sourceLabel",
                fontSize = 11.5.sp,
                color = VelvetTextSecondary.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Far-right three-dot menu button
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("track_menu_${track.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Track options",
                tint = VelvetTextSecondary.copy(alpha = 0.70f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Compact Red Glass Action Card
 *
 * Sits at the top right of the Search screen.
 * Taller than wide, beautifully translucent red glass with illuminated borders,
 * specular highlight, centered icon, label underneath, and subtle coming-soon indicator.
 */
@Composable
private fun RedGlassActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    isComingSoon: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .width(48.dp)
            .height(58.dp)
            .shadow(
                elevation = 5.dp,
                shape = shape,
                spotColor = Color(0x66E5284D),
                ambientColor = Color.Transparent
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x40FF2A55), // Specular vibrant crimson tint
                        Color(0x2B880E2F), // Translucent deep wine body
                        Color(0x1E420B15)  // Dark velvet foundation
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x80FF6B8B), // Crisp illuminated glass edge at top
                        Color(0x33FFFFFF), // Subtle white glass reflection
                        Color(0x18FF2A55)  // Warm crimson bottom rim
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        // Specular top highlight line for authentic frosted glass look
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.70f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.50f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = VelvetTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Elegant tiny dot indicator for coming soon
        if (isComingSoon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5277))
            )
        }
    }
}

