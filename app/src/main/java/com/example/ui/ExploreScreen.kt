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
        // Atmospheric Canvas Background from Sing-to-Search aesthetic
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF030001))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FF2448),
                        Color(0x1F4A000E),
                        Color(0x0C220005),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.10f),
                    radius = w * 0.72f
                ),
                center = Offset(w * 0.5f, h * 0.10f),
                radius = w * 0.72f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x4048000C),
                                    Color(0x2B260006)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0x66FF2448),
                                    Color(0x28FFFFFF),
                                    Color(0x44FF2448)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { searchFocusRequester.requestFocus() }
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFFF2448),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { searchFocusRequester.requestFocus() }
                                .testTag("search_icon_button")
                        )

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
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(Color(0xFFFF2448)),
                                singleLine = true
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(16.dp)
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color(0xFFE51B3E)
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
            }

            // Bottom space balancing floating navigation dock
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
                thumbnailSizePx = 120,
                crossfade = false,
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
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = Color(0x66FF2448),
                ambientColor = Color(0x3348000C)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x55FF2448),
                        Color(0x3348000C),
                        Color(0x22260006)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x80FF4D6D),
                        Color(0x33FFFFFF),
                        Color(0x22FF2448)
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
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFFF4D6D),
                modifier = Modifier.size(19.dp)
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

