package com.example.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.audio.VelvetAudioEngine
import com.example.media.DeviceMediaManager
import com.example.mesh.SoundCatchMeshBackground
import com.example.model.SampleData
import com.example.model.Track
import com.example.ui.theme.VelvetActiveGlow
import com.example.ui.theme.VelvetAshGray
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetAshGrayLight
import com.example.ui.theme.VelvetAshGrayMedium
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBottomBarBorder
import com.example.ui.theme.VelvetBottomBarGlass
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetCardBorder
import com.example.ui.theme.VelvetGlassSurface
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import com.example.ui.theme.VelvetWhiteAsh

@Composable
fun VelvetApp() {
    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { VelvetAudioEngine(coroutineScope) }

    val context = LocalContext.current
    val currentTrack by audioEngine.currentTrack.collectAsState()
    val isPlaying by audioEngine.isPlaying.collectAsState()
    val playbackPositionMs by audioEngine.playbackPositionMs.collectAsState()
    val telemetry by audioEngine.telemetry.collectAsState()
    val isSoundCatchEnabled by audioEngine.isSoundCatchEnabled.collectAsState()
    val isShuffle by audioEngine.isShuffle.collectAsState()
    val isRepeat by audioEngine.isRepeat.collectAsState()
    val offlineCachedIds by audioEngine.offlineCachedTrackIds.collectAsState()
    val trackPlayCounts by audioEngine.trackPlayCounts.collectAsState()
    val favoriteTrackIds by audioEngine.favoriteTrackIds.collectAsState()
    val deviceTracks by audioEngine.deviceTracks.collectAsState()
    val deletedTrackIds by audioEngine.deletedTrackIds.collectAsState()

    // Automatic permission launcher on mount: asks for permission to access user device audio & video
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (audioGranted || DeviceMediaManager.hasAudioPermission(context)) {
            val loaded = DeviceMediaManager.loadDeviceTracks(context)
            if (loaded.isNotEmpty()) {
                audioEngine.setDeviceTracks(loaded)
            }
        }
    }

    // Initialize media session lock screen controls and auto-query device audio files upon launch
    LaunchedEffect(Unit) {
        audioEngine.bindMediaSession(context)
        if (DeviceMediaManager.hasAudioPermission(context)) {
            val loaded = DeviceMediaManager.loadDeviceTracks(context)
            if (loaded.isNotEmpty()) {
                audioEngine.setDeviceTracks(loaded)
            }
        } else {
            // Automatically prompt for permission when mounting so user music & video can be detected
            mediaPermissionLauncher.launch(DeviceMediaManager.allMediaPermissions)
        }
    }

    // Real device tracks with starter showcase fallback (minus deleted tracks)
    val allTracks = remember(deviceTracks, deletedTrackIds) {
        val filtered = deviceTracks
            .distinctBy { it.id }
            .filterNot { deletedTrackIds.contains(it.id) }
        if (filtered.isNotEmpty()) filtered else com.example.model.SampleData.starterTracks.filterNot { deletedTrackIds.contains(it.id) }
    }

    // Most played tracks (user plays mostly / all the time, ranked by real play counts)
    val mostPlayedTracks = remember(allTracks, trackPlayCounts) {
        val played = allTracks.filter { (trackPlayCounts[it.id] ?: 0) > 0 }
            .sortedByDescending { trackPlayCounts[it.id] ?: 0 }
        if (played.isNotEmpty()) played else allTracks
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isInspectorOpen by remember { mutableStateOf(false) }
    var isProTierOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var actionSheetTrack by remember { mutableStateOf<Track?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetAshGrayDark)
    ) {
        // App background remains in its exact velvet theme color (Off-Blood to Ash Gray)
        // Dynamic music artwork colors are strictly isolated to the PlayerSheet!
        SoundCatchMeshBackground(
            dominantColor = VelvetOffBloodTop,
            secondaryColor = VelvetBloodPlum,
            audioTelemetry = telemetry,
            isPlaying = isPlaying,
            isSoundCatchEnabled = isSoundCatchEnabled
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = VelvetTextPrimary,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
            ) {
                // Active Screen Tab
                when (selectedTab) {
                    0 -> HomeFeedScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        allTracks = allTracks,
                        mostPlayedTracks = mostPlayedTracks,
                        playCounts = trackPlayCounts,
                        hasAudioPermission = DeviceMediaManager.hasAudioPermission(context),
                        onRequestPermission = { mediaPermissionLauncher.launch(DeviceMediaManager.allMediaPermissions) },
                        onSelectTrack = { track -> audioEngine.playTrack(track) },
                        onOpenSearch = { selectedTab = 1 },
                        onOpenSettings = { isSettingsOpen = true },
                        onOpenNotifications = { isProTierOpen = true },
                        onTrackMenuClick = { track -> actionSheetTrack = track },
                        onAddTrack = { track -> audioEngine.addDeviceTrack(track) }
                    )

                    1 -> ExploreScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        tracks = allTracks,
                        onSelectTrack = { track -> audioEngine.playTrack(track) },
                        onTrackMenuClick = { track -> actionSheetTrack = track }
                    )

                    2 -> VideoLibraryScreen()
                }

                // Mini Player Bar (rests directly above the curved ash glass bottom bar when active)
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 68.dp, start = 6.dp, end = 6.dp)
                    ) {
                        AshGlassMiniPlayerBar(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            playbackPositionMs = playbackPositionMs,
                            onTogglePlayPause = { audioEngine.togglePlayPause() },
                            onSkipNext = { audioEngine.playNext() },
                            onClick = { isPlayerExpanded = true }
                        )
                    }
                }

                // Bottom Navigation Bar:
                AshGlassBottomNavigationBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Full Screen Player Sheet
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            PlayerSheet(
                track = currentTrack,
                isPlaying = isPlaying,
                playbackPositionMs = playbackPositionMs,
                telemetry = telemetry,
                isShuffle = isShuffle,
                isRepeat = isRepeat,
                isFavorite = favoriteTrackIds.contains(currentTrack.id),
                isCachedOffline = offlineCachedIds.contains(currentTrack.id),
                onTogglePlayPause = { audioEngine.togglePlayPause() },
                onSeekTo = { pos -> audioEngine.seekTo(pos) },
                onSkipNext = { audioEngine.playNext() },
                onSkipPrevious = { audioEngine.playPrevious() },
                onToggleShuffle = { audioEngine.toggleShuffle() },
                onToggleRepeat = { audioEngine.toggleRepeat() },
                onToggleFavorite = {
                    audioEngine.toggleFavorite(currentTrack.id)
                    val isFav = !favoriteTrackIds.contains(currentTrack.id)
                    Toast.makeText(context, if (isFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                },
                onPlayNext = {
                    audioEngine.queueNext(currentTrack)
                    Toast.makeText(context, "Playing next: ${currentTrack.title.substringBefore(" - ")}", Toast.LENGTH_SHORT).show()
                },
                onQueue = {
                    audioEngine.queueNext(currentTrack)
                },
                onShareTrack = { DeviceMediaManager.shareTrack(context, currentTrack) },
                onDeleteTrack = {
                    audioEngine.deleteTrack(currentTrack.id)
                    isPlayerExpanded = false
                    Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { isPlayerExpanded = false }
            )
        }

        // Sound Catch Inspector Sheet
        if (isInspectorOpen) {
            SoundCatchInspectorSheet(
                track = currentTrack,
                telemetry = telemetry,
                isSoundCatchEnabled = isSoundCatchEnabled,
                onToggleSoundCatch = { audioEngine.toggleSoundCatch() },
                onDismiss = { isInspectorOpen = false }
            )
        }

        // Pro Tier Sheet
        if (isProTierOpen) {
            ProTierBottomSheet(
                onDismiss = { isProTierOpen = false }
            )
        }

        // Settings Sheet
        if (isSettingsOpen) {
            SettingsBottomSheet(
                onDismiss = { isSettingsOpen = false }
            )
        }

        // Track Action Bottom Sheet
        actionSheetTrack?.let { track ->
            TrackActionBottomSheet(
                track = track,
                isCached = offlineCachedIds.contains(track.id),
                isFavorite = favoriteTrackIds.contains(track.id),
                onPlayNow = { audioEngine.playTrack(track) },
                onPlayNext = {
                    audioEngine.queueNext(track)
                    Toast.makeText(context, "Playing next: ${track.title.substringBefore(" - ")}", Toast.LENGTH_SHORT).show()
                },
                onToggleFavorite = {
                    audioEngine.toggleFavorite(track.id)
                    val isFav = !favoriteTrackIds.contains(track.id)
                    Toast.makeText(context, if (isFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                },
                onShareTrack = { DeviceMediaManager.shareTrack(context, track) },
                onDeleteTrack = {
                    audioEngine.deleteTrack(track.id)
                    Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                },
                onToggleOfflineCache = { audioEngine.toggleOfflineCache(track.id) },
                onViewLyrics = {
                    audioEngine.playTrack(track)
                    isPlayerExpanded = true
                },
                onViewSoundCatch = {
                    audioEngine.playTrack(track)
                    isInspectorOpen = true
                },
                onDismiss = { actionSheetTrack = null }
            )
        }
    }
}

/**
 * Ash Glass Bottom Navigation Bar matching user description and screenshot 1788538785231.png:
 * - Low-profile, sleek height flush to screen bottom with curved top corners
 * - Material: Translucent almost-white ashes frosted glass with micro-stippled dot texture
 *   ("dot dot in the surface, but very small that the space of each dot is not visible... looks almost smooth")
 * - Ambient glowing shadow spreading inside the frosted glass under whichever icon is chosen
 * - Icons float cleanly on the glass WITHOUT any wrapping card or circular container
 * - Left: Outlined music note tile with soft rose-ash tone and "ACTIVE" indicator label
 * - Center: Minimalist search icon
 * - Right: Profile outline with 4-point diamond star badge
 */
@Composable
fun AshGlassBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBarShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(navBarShape)
            .testTag("ash_glass_navigation_bar"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Sticky Transparent Frosted Glass Canvas with Micro-Stipple Texture & Top Rim
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val w = size.width
            val h = size.height

            // Sticky Transparent Glass: semi-opaque smoky dark base that diffuses behind it without showing full clarity
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB8231C28), // Sticky frosted ash top (~72% opacity)
                        Color(0xC817121D), // Sticky smoky body (~78% opacity)
                        Color(0xD80E0A14)  // Rich sticky charcoal bottom (~85% opacity)
                    )
                )
            )

            // App's signature glass glaze (frosted specular sheen + subtle crimson infusion)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color(0xFFE50914).copy(alpha = 0.035f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )

            // Tactile micro-stippled dot texture (fine dots scattering light so surface looks almost smooth)
            val dotColor1 = Color(0x1AFFFFFF)
            val dotColor2 = Color(0x12CCD4E0)
            val stepX = 3.8f
            val stepY = 3.8f
            var curY = 1.5f
            while (curY < h) {
                var curX = if (((curY / stepY).toInt() % 2) == 0) 1.5f else 3.4f
                while (curX < w) {
                    drawCircle(
                        color = if (((curX + curY).toInt() % 3) == 0) dotColor1 else dotColor2,
                        radius = 0.65f,
                        center = Offset(curX, curY)
                    )
                    curX += stepX
                }
                curY += stepY
            }

            // Crisp frosted top edge highlight
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x20FFFFFF),
                        Color(0x70FFFFFF),
                        Color(0x70FFFFFF),
                        Color(0x20FFFFFF)
                    )
                ),
                start = Offset(0f, 1f),
                end = Offset(w, 1f),
                strokeWidth = 1.4f
            )
        }

        // 2. Navigation Action Bar: Sleek 56.dp height, strictly centered on the same horizontal line
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 42.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TAB 0: Standalone Music Note Icon (no border circle, no "active" text)
                    AshGlassNavTabItem(
                        isSelected = selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        testTag = "nav_tab_music"
                    ) { iconColor ->
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // TAB 1: Standalone Search Icon
                    AshGlassNavTabItem(
                        isSelected = selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        testTag = "nav_tab_search"
                    ) { iconColor ->
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // TAB 2: Standalone Video Icon
                    AshGlassNavTabItem(
                        isSelected = selectedTab == 2,
                        onClick = { onSelectTab(2) },
                        testTag = "nav_tab_videos"
                    ) { iconColor ->
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Videos",
                            tint = iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ash Glass Nav Tab Item:
 * - Standalone icon on the exact same horizontal center line
 * - Water wave animation that ripples and spreads far outward upon selection
 * - Soft red glowing shadow settling beneath the icon
 * - Icon turns a distinct, vibrant red when selected so it stands out sharply
 */
@Composable
private fun AshGlassNavTabItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable (iconColor: Color) -> Unit
) {
    // Water wave expansion animation (spreads far outward like a water wave when clicked)
    val waveProgress = remember { Animatable(if (isSelected) 1f else 0f) }
    // Glow spread animation (waves out wide, then shrinks to a smaller spread beneath the icon)
    val glowSpread = remember { Animatable(if (isSelected) 1.0f else 0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            waveProgress.snapTo(0f)
            glowSpread.snapTo(0.2f)
            launch {
                // Water wave ripple expanding far outward (from 14dp up to 68dp)
                waveProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                // Glow waves out wide (overshooting to 1.45x), then shrinks smoothly back to 1.0x spread
                glowSpread.animateTo(
                    targetValue = 1.45f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
                glowSpread.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                )
            }
        } else {
            waveProgress.snapTo(0f)
            glowSpread.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220)
            )
        }
    }

    // Icon color: when selected, changes to a distinct vivid red so it stands out cleanly against the glow beneath
    val iconColor = if (isSelected) Color(0xFFFF3054) else Color(0xFF8E95A2)

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Red glow and water wave ripple rendered BENEATH the icon
        if (isSelected || glowSpread.value > 0.01f || (waveProgress.value in 0.01f..0.99f)) {
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.Center)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // 1. Water wave ripple spreading far outward
                if (waveProgress.value in 0.01f..0.99f) {
                    val p = waveProgress.value
                    // Spreads far outward (radius from 14dp up to 68dp)
                    val waveRadius = 14.dp.toPx() + (p * 54.dp.toPx())
                    val waveAlpha = (1f - p) * 0.52f

                    // Soft outer water wave crest ring
                    drawCircle(
                        color = Color(0xFFFF1A44).copy(alpha = waveAlpha),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 2.4.dp.toPx() * (1f - p * 0.4f))
                    )

                    // Diffuse secondary wave halo
                    drawCircle(
                        color = Color(0xFFE5284D).copy(alpha = waveAlpha * 0.38f),
                        radius = waveRadius * 0.85f,
                        center = center
                    )
                }

                // 2. Steady red glow beneath the icon (shrinks to a nice smaller spread around the icon)
                val currentSpread = glowSpread.value
                if (currentSpread > 0.01f) {
                    val spreadRadius = 32.dp.toPx() * currentSpread
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD6183C).copy(alpha = 0.55f * currentSpread.coerceAtMost(1f)),
                                Color(0xFFA81432).copy(alpha = 0.32f * currentSpread.coerceAtMost(1f)),
                                Color(0xFF6B0B1E).copy(alpha = 0.12f * currentSpread.coerceAtMost(1f)),
                                Color.Transparent
                            ),
                            center = center,
                            radius = spreadRadius
                        ),
                        center = center,
                        radius = spreadRadius
                    )
                }
            }
        }

        // 3. Standalone icon cleanly rendered on top
        content(iconColor)
    }
}

@Composable
fun AshGlassMiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit
) {
    val progressFraction = if (track.durationMs > 0) {
        (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        VelvetBloodPlum.copy(alpha = 0.85f),
                        VelvetAshGrayMedium.copy(alpha = 0.90f),
                        VelvetAshGrayDark.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, VelvetCardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, VelvetCardBorder, RoundedCornerShape(10.dp))
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelvetTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = VelvetTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(36.dp).testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = VelvetBrightCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(36.dp).testTag("mini_player_skip_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = VelvetWhiteAsh,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = VelvetBrightCrimson,
                trackColor = Color.Transparent
            )
        }
    }
}
