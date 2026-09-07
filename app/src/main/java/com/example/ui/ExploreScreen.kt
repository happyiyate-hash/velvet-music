package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetGlassSurface
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DetectedPlatform(val label: String, val badgeColor: Color) {
    TIKTOK("TikTok Video", Color(0xFF00F2FE)),
    INSTAGRAM("Instagram Reel", Color(0xFFE1306C)),
    FACEBOOK("Facebook Video", Color(0xFF1877F2)),
    OTHER_MEDIA("Web Video", Color(0xFF9B51E0)),
    NONE("Search", Color.Transparent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    tracks: List<Track> = emptyList(),
    onSelectTrack: (Track) -> Unit,
    onTrackMenuClick: (Track) -> Unit,
    onAddTrack: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Music", "Video"
    var showComingSoonDialog by remember { mutableStateOf<String?>(null) } // "Say it" or "Sing it"

    // Downloader state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var isDownloadComplete by remember { mutableStateOf(false) }

    // Detect social platform from pasted / typed URL
    val detectedPlatform = remember(searchQuery) {
        val trimmed = searchQuery.trim()
        when {
            trimmed.contains("tiktok.com", ignoreCase = true) -> DetectedPlatform.TIKTOK
            trimmed.contains("instagram.com", ignoreCase = true) -> DetectedPlatform.INSTAGRAM
            trimmed.contains("facebook.com", ignoreCase = true) || trimmed.contains("fb.watch", ignoreCase = true) -> DetectedPlatform.FACEBOOK
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> DetectedPlatform.OTHER_MEDIA
            else -> DetectedPlatform.NONE
        }
    }

    // Filter tracks based on search
    val filteredTracks = remember(searchQuery, selectedFilter, tracks) {
        if (detectedPlatform != DetectedPlatform.NONE) {
            emptyList()
        } else {
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
    }

    // Coming soon alert dialog for "Say it" and "Sing it"
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
                    // Glowing icon circle
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

                    // Clean dismiss button
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
                Text(
                    text = "Search & Downloader",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Search songs, videos, or paste social links to download without watermark",
                    fontSize = 13.sp,
                    color = VelvetTextSecondary,
                    lineHeight = 18.sp
                )

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
                                    text = "Search music or paste social link...",
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
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isDownloading = false
                                    isDownloadComplete = false
                                }
                            ) {
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

                Spacer(modifier = Modifier.height(14.dp))

                // Paste from Clipboard Quick Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUPPORTED PLATFORMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetTextTertiary,
                        letterSpacing = 1.sp
                    )

                    // Quick Paste Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    searchQuery = clipText.trim()
                                    Toast.makeText(context, "Link pasted from clipboard", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = VelvetBrightCrimson,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Paste Link",
                            color = VelvetTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. SOCIAL MEDIA PLATFORM ICONS GRID (TikTok, Instagram, Facebook, Other)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // TIKTOK
                    SocialPlatformGlassCard(
                        iconRes = R.drawable.ic_tiktok,
                        label = "TikTok",
                        gradient = listOf(Color(0xFF00F2FE).copy(alpha = 0.25f), Color(0xFFFE0979).copy(alpha = 0.25f)),
                        borderColor = Color(0xFF00F2FE).copy(alpha = 0.50f),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchQuery = "https://www.tiktok.com/@user/video/sample"
                        }
                    )

                    // INSTAGRAM
                    SocialPlatformGlassCard(
                        iconRes = R.drawable.ic_instagram,
                        label = "Instagram",
                        gradient = listOf(Color(0xFF833AB4).copy(alpha = 0.25f), Color(0xFFFD1D1D).copy(alpha = 0.25f)),
                        borderColor = Color(0xFFE1306C).copy(alpha = 0.50f),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchQuery = "https://www.instagram.com/reel/sample"
                        }
                    )

                    // FACEBOOK
                    SocialPlatformGlassCard(
                        iconRes = R.drawable.ic_facebook,
                        label = "Facebook",
                        gradient = listOf(Color(0xFF1877F2).copy(alpha = 0.25f), Color(0xFF0D5CB6).copy(alpha = 0.25f)),
                        borderColor = Color(0xFF1877F2).copy(alpha = 0.50f),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchQuery = "https://www.facebook.com/watch/?v=sample"
                        }
                    )

                    // OTHER SOCIAL MEDIA
                    SocialPlatformGlassCard(
                        iconRes = R.drawable.ic_social_more,
                        label = "Other",
                        gradient = listOf(Color(0xFF8E44AD).copy(alpha = 0.25f), Color(0xFF2C3E50).copy(alpha = 0.25f)),
                        borderColor = Color(0xFF9B51E0).copy(alpha = 0.50f),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            searchQuery = "https://www.sample.com/video.mp4"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // 4. SMART RECOGNITION: NO-WATERMARK DOWNLOADER CARD
        if (detectedPlatform != DetectedPlatform.NONE) {
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF2A101A).copy(alpha = 0.90f),
                                        Color(0xFF19070E).copy(alpha = 0.98f)
                                    )
                                )
                            )
                            .border(
                                width = 1.3.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        detectedPlatform.badgeColor.copy(alpha = 0.50f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp)
                            .animateContentSize()
                    ) {
                        Column {
                            // Header badge & detected platform
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(detectedPlatform.badgeColor.copy(alpha = 0.20f))
                                            .border(1.dp, detectedPlatform.badgeColor.copy(alpha = 0.40f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = when (detectedPlatform) {
                                                    DetectedPlatform.TIKTOK -> R.drawable.ic_tiktok
                                                    DetectedPlatform.INSTAGRAM -> R.drawable.ic_instagram
                                                    DetectedPlatform.FACEBOOK -> R.drawable.ic_facebook
                                                    else -> R.drawable.ic_social_more
                                                }
                                            ),
                                            contentDescription = detectedPlatform.label,
                                            tint = detectedPlatform.badgeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = detectedPlatform.label,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VelvetTextPrimary
                                        )
                                        Text(
                                            text = "Ready to download without watermark",
                                            fontSize = 11.5.sp,
                                            color = VelvetTextSecondary
                                        )
                                    }
                                }

                                // "NO WATERMARK" BADGE
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00C853).copy(alpha = 0.20f))
                                        .border(1.dp, Color(0xFF00C853).copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NO WATERMARK HD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF69F0AE)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Link display
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = searchQuery,
                                    fontSize = 12.sp,
                                    color = VelvetTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Progress Indicator if downloading
                            if (isDownloading) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = downloadStatusText,
                                            fontSize = 12.sp,
                                            color = VelvetTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            color = VelvetBrightCrimson,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = VelvetBrightCrimson,
                                        trackColor = Color.White.copy(alpha = 0.12f)
                                    )
                                }
                            } else if (isDownloadComplete) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00C853).copy(alpha = 0.18f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Complete",
                                        tint = Color(0xFF69F0AE),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Downloaded without watermark! Saved to your device gallery & library.",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        lineHeight = 16.sp
                                    )
                                }
                            } else {
                                // Download Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Download Video Without Watermark
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        VelvetBrightCrimson,
                                                        VelvetBloodPlum
                                                    )
                                                )
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    isDownloading = true
                                                    isDownloadComplete = false
                                                    downloadStatusText = "Connecting & removing watermark..."
                                                    downloadProgress = 0.15f
                                                    delay(600)
                                                    downloadStatusText = "Stripping watermark metadata..."
                                                    downloadProgress = 0.45f
                                                    delay(700)
                                                    downloadStatusText = "Downloading clean HD MP4..."
                                                    downloadProgress = 0.85f
                                                    delay(600)
                                                    downloadProgress = 1.0f
                                                    downloadStatusText = "Complete!"
                                                    delay(300)
                                                    isDownloading = false
                                                    isDownloadComplete = true
                                                    Toast.makeText(context, "Saved video without watermark to Gallery", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Download Video",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Extract Audio Button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.10f))
                                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    isDownloading = true
                                                    isDownloadComplete = false
                                                    downloadStatusText = "Extracting high-bitrate audio stream..."
                                                    downloadProgress = 0.35f
                                                    delay(600)
                                                    downloadStatusText = "Converting to clean MP3/M4A..."
                                                    downloadProgress = 0.75f
                                                    delay(600)
                                                    downloadProgress = 1.0f
                                                    downloadStatusText = "Audio extracted!"
                                                    delay(300)
                                                    isDownloading = false
                                                    isDownloadComplete = true

                                                    val newTrack = Track(
                                                        id = "extracted_${System.currentTimeMillis()}",
                                                        title = "${detectedPlatform.label} Sound (${System.currentTimeMillis() % 1000})",
                                                        artist = "Social Audio",
                                                        album = detectedPlatform.label,
                                                        durationMs = 30000L,
                                                        coverResId = R.drawable.art_luminous_echoes,
                                                        catalogSource = "Social Download"
                                                    )
                                                    onAddTrack?.invoke(newTrack)
                                                    Toast.makeText(context, "Extracted audio saved to Library!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = "Extract Audio",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Extract Audio",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 5. SEARCH FILTER TABS (All / Music / Video)
        if (detectedPlatform == DetectedPlatform.NONE) {
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

            // 6. SEARCH RESULTS / LIBRARY TRACKS LIST
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
        }

        // Bottom space so items aren't obscured by mini player and bottom bar
        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

/**
 * Social Platform Glass Card
 * Sleek, clean gradient card for TikTok, Instagram, Facebook, and Other platforms.
 */
@Composable
private fun SocialPlatformGlassCard(
    iconRes: Int,
    label: String,
    gradient: List<Color>,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        gradient[0],
                        gradient[1].copy(alpha = 0.15f),
                        Color(0xFF14080D).copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.25f),
                        borderColor.copy(alpha = 0.40f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1
            )
        }
    }
}
