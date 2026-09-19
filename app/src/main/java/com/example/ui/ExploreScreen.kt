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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
    onOpenSingToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val searchFocusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
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
        // Atmospheric Multi-Gradient Canvas Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base deep obsidian velvet
            drawRect(color = Color(0xFF040002))

            // Upper crimson ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x55FF2448),
                        Color(0x33B50E29),
                        Color(0x1848000C),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.08f),
                    radius = w * 0.90f
                ),
                center = Offset(w * 0.50f, h * 0.08f),
                radius = w * 0.90f
            )

            // Mid-right deep burgundy orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3085001E),
                        Color(0x123B000B),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.42f),
                    radius = w * 0.70f
                ),
                center = Offset(w * 0.88f, h * 0.42f),
                radius = w * 0.70f
            )

            // Lower-left subtle warm wine bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28600015),
                        Color(0x0E200008),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.78f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.15f, h * 0.78f),
                radius = w * 0.65f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp)
        ) {
            // 1. SIGNATURE BRANDING HEADER (Exact V E L V E T styling)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "V E L V E T",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF2448),
                        letterSpacing = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DARK.  AMBIENT.  FLUID AUDIO.",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 2.8.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. REFINED SEARCH INPUT (Glowing wine-crimson glass capsule)
            item {
                val capsuleShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            elevation = if (isSearchFocused) 8.dp else 4.dp,
                            shape = capsuleShape,
                            spotColor = if (isSearchFocused) Color(0x66FF2448) else Color(0x3348000C),
                            ambientColor = Color(0x22120003)
                        )
                        .clip(capsuleShape)
                        .background(
                            Brush.horizontalGradient(
                                if (isSearchFocused) {
                                    listOf(Color(0x5548000C), Color(0x3D260006))
                                } else {
                                    listOf(Color(0x3D48000C), Color(0x28260006))
                                }
                            )
                        )
                        .border(
                            width = if (isSearchFocused) 1.2.dp else 1.dp,
                            brush = Brush.horizontalGradient(
                                if (isSearchFocused) {
                                    listOf(
                                        Color(0xFFFF3355),
                                        Color(0xFFFF889E),
                                        Color(0xFFFF3355)
                                    )
                                } else {
                                    listOf(
                                        Color(0x66FF2448),
                                        Color(0x28FFFFFF),
                                        Color(0x44FF2448)
                                    )
                                }
                            ),
                            shape = capsuleShape
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
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x24FF2448)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFFF3355),
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable { searchFocusRequester.requestFocus() }
                                    .testTag("search_icon_button")
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search music, artists, or paste URL...",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 13.5.sp,
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
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                                    .testTag("explore_search_input"),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(Color(0xFFFF2448)),
                                singleLine = true
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFB0B0B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onOpenSingToSearch,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("search_quick_sing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Sing to Search",
                                    tint = Color(0xFFFF4D6D),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. ACTION PILLS ROW (Paste it, Say it, Sing it)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RedGlassActionCard(
                        title = "Paste it",
                        icon = Icons.Default.ContentPaste,
                        modifier = Modifier.weight(1f),
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
                        modifier = Modifier.weight(1f),
                        isComingSoon = true,
                        onClick = { showComingSoonDialog = "Say it" },
                        tag = "action_say_it"
                    )

                    RedGlassActionCard(
                        title = "Sing it",
                        icon = Icons.Default.GraphicEq,
                        modifier = Modifier.weight(1f),
                        isHighlighted = true,
                        isComingSoon = false,
                        onClick = onOpenSingToSearch,
                        tag = "action_sing_it"
                    )
                }
            }

            // 4. MAIN SECTIONS WHEN NOT SEARCHING
            if (searchQuery.isBlank()) {
                // Section: Recently Played
                if (tracks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Played",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "See All >",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF2448),
                                modifier = Modifier.clickable { }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tracks.take(6), key = { "recent_${it.id}" }) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(116.dp)
                                        .clickable { onSelectTrack(track) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(116.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track.artist,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF8E8E93),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                    }
                }

                // Section: For You (Based on your taste)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "For You",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Based on your taste",
                                fontSize = 11.5.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        Text(
                            text = "See All >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF2448),
                            modifier = Modifier.clickable { }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val forYouPlaylists = listOf(
                        Triple("Chill Vibes", "Playlist • 50 tracks", Color(0xFF48000C)),
                        Triple("Afrobeats Essentials", "Playlist • 62 tracks", Color(0xFF3B0014)),
                        Triple("Late Night", "Playlist • 45 tracks", Color(0xFF260009)),
                        Triple("Midnight Acoustic", "Playlist • 38 tracks", Color(0xFF1E000A))
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(forYouPlaylists) { (title, subtitle, baseBg) ->
                            val cardShape = RoundedCornerShape(16.dp)
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(115.dp)
                                    .shadow(6.dp, cardShape, spotColor = Color(0x55FF2448))
                                    .clip(cardShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                baseBg.copy(alpha = 0.90f),
                                                Color(0xFF0F0004)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x55FF2448),
                                                Color(0x22FFFFFF)
                                            )
                                        ),
                                        cardShape
                                    )
                                    .clickable {
                                        if (tracks.isNotEmpty()) {
                                            onSelectTrack(tracks.random())
                                        }
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFB0959B)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE51B3E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play $title",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Section: Your Library
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Library",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "See All >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF2448),
                            modifier = Modifier.clickable { }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val libraryItems = listOf(
                        Triple("Liked Songs", "${tracks.size.coerceAtLeast(1)} songs", Icons.Default.Favorite),
                        Triple("Playlists", "12 playlists", Icons.Default.QueueMusic),
                        Triple("Downloaded", "86 offline", Icons.Default.ArrowDownward)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        libraryItems.forEach { (title, subtitle, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1F260009))
                                    .border(0.8.dp, Color(0x33FF2448), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (tracks.isNotEmpty()) onSelectTrack(tracks.first())
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x33FF2448)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = Color(0xFFFF2448),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 5. FILTER TABS (When searching)
            if (searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Music", "Video").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val pillShape = RoundedCornerShape(18.dp)
                            Box(
                                modifier = Modifier
                                    .shadow(if (isSelected) 4.dp else 0.dp, pillShape, spotColor = Color(0x66FF2448))
                                    .clip(pillShape)
                                    .background(
                                        if (isSelected) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFFF2448),
                                                    Color(0xFFB50E29)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0x2848000C),
                                                    Color(0x15220006)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = if (isSelected) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0x99FFFFFF),
                                                    Color(0x66FF4D6D)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0x44FF2448),
                                                    Color(0x18FFFFFF)
                                                )
                                            )
                                        },
                                        shape = pillShape
                                    )
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF8E8E93)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Results Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Results for \"$searchQuery\"",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
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

                // Result items
                items(
                    items = filteredTracks,
                    key = { it.id },
                    contentType = { "explore_track_row" }
                ) { track ->
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

                if (filteredTracks.isEmpty()) {
                    item {
                        val emptyCardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 20.dp)
                                .clip(emptyCardShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x3848000C),
                                            Color(0x18260006)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color(0x44FF2448),
                                            Color(0x15FFFFFF)
                                        )
                                    ),
                                    shape = emptyCardShape
                                )
                                .padding(vertical = 32.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x22FF2448))
                                        .border(1.dp, Color(0x55FF2448), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "No results",
                                        tint = Color(0xFFFF4D6D),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No matches found",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "We couldn't find any music matching \"$searchQuery\". Try checking the spelling or search by artist name.",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF8E8E93),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x28FF2448))
                                        .border(1.dp, Color(0x66FF2448), RoundedCornerShape(12.dp))
                                        .clickable { searchQuery = "" }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Clear Search",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF6B84)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom space balancing floating navigation dock
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

/**
 * Premium Gradient Music Play Card for the Search / Explore experience
 */
@Composable
private fun SearchTrackResultRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    GradientMusicPlayCard(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onMenuClick = onMenuClick,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp)
    )
}

/**
 * Compact Red Glass Action Card
 *
 * Taller than wide, beautifully translucent red glass with illuminated borders,
 * specular highlight, centered icon, label underneath, and subtle coming-soon or highlight indicator.
 */
@Composable
private fun RedGlassActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isComingSoon: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = if (isHighlighted) 8.dp else 4.dp,
                shape = shape,
                spotColor = if (isHighlighted) Color(0x88FF2448) else Color(0x44FF2448),
                ambientColor = Color(0x3348000C)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (isHighlighted) {
                        listOf(
                            Color(0x88FF2448),
                            Color(0x448F071F),
                            Color(0x33260006)
                        )
                    } else {
                        listOf(
                            Color(0x44FF2448),
                            Color(0x2848000C),
                            Color(0x1F260006)
                        )
                    }
                )
            )
            .border(
                width = if (isHighlighted) 1.2.dp else 1.dp,
                brush = Brush.verticalGradient(
                    colors = if (isHighlighted) {
                        listOf(
                            Color(0xFFFF889E),
                            Color(0xFFFF2448),
                            Color(0x66FF2448)
                        )
                    } else {
                        listOf(
                            Color(0x66FF4D6D),
                            Color(0x28FFFFFF),
                            Color(0x22FF2448)
                        )
                    }
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
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isHighlighted) Color.White else Color(0xFFFF4D6D),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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

