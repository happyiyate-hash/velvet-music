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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (audioGranted || DeviceMediaManager.hasAudioPermission(context)) {
            coroutineScope.launch(Dispatchers.IO) {
                val loaded = DeviceMediaManager.loadDeviceTracks(context)
                if (loaded.isNotEmpty()) {
                    audioEngine.setDeviceTracks(loaded)
                }
            }
        }
    }

    // Startup flow:
    // 1. Immediately read cached tracks and display them (<5ms)
    // 2. Quietly scan MediaStore in the background and update if changes exist
    LaunchedEffect(Unit) {
        audioEngine.bindMediaSession(context)

        // Step 1: Instant cache load
        val cached = withContext(Dispatchers.IO) {
            DeviceMediaManager.getCachedTracks(context)
        }
        if (cached.isNotEmpty()) {
            audioEngine.setDeviceTracks(cached)
        }

        // Step 2: Background scan
        if (DeviceMediaManager.hasAudioPermission(context)) {
            val loaded = withContext(Dispatchers.IO) {
                DeviceMediaManager.loadDeviceTracks(context)
            }
            if (loaded.isNotEmpty()) {
                audioEngine.setDeviceTracks(loaded)
            }
        } else {
            mediaPermissionLauncher.launch(DeviceMediaManager.allMediaPermissions)
        }
    }

    DisposableEffect(audioEngine) {
        onDispose {
            audioEngine.release()
        }
    }

    val allTracks = remember(deviceTracks, deletedTrackIds) {
        val filtered = deviceTracks
            .distinctBy { it.id }
            .filterNot { deletedTrackIds.contains(it.id) }
        if (filtered.isNotEmpty()) filtered else SampleData.starterTracks.filterNot { deletedTrackIds.contains(it.id) }
    }

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
    var isMediaDownloaderOpen by remember { mutableStateOf(false) }
    var mediaDownloaderInitialUrl by remember { mutableStateOf<String?>(null) }
    var actionSheetTrack by remember { mutableStateOf<Track?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(VelvetAshGrayDark)
    ) {
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
                modifier = Modifier.fillMaxSize().padding(paddingValues).statusBarsPadding()
            ) {
                when (selectedTab) {
                    0 -> HomeFeedScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        allTracks = allTracks,
                        mostPlayedTracks = mostPlayedTracks,
                        playCounts = trackPlayCounts,
                        hasAudioPermission = DeviceMediaManager.hasAudioPermission(context),
                        onRequestPermission = { mediaPermissionLauncher.launch(DeviceMediaManager.allMediaPermissions) },
                        onSelectTrack = { track ->
                            audioEngine.playTrack(track)
                            isPlayerExpanded = true
                        },
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
                        onTrackMenuClick = { track -> actionSheetTrack = track },
                        onAddTrack = { track -> audioEngine.addDeviceTrack(track) },
                        onOpenMediaDownloader = { url ->
                            mediaDownloaderInitialUrl = url
                            isMediaDownloaderOpen = true
                        }
                    )
                    2 -> VideoLibraryScreen()
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 54.dp, start = 8.dp, end = 8.dp)
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

                AshGlassBottomNavigationBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

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
                queueTracks = allTracks,
                onSelectQueueTrack = { track -> audioEngine.playTrack(track) },
                onTogglePlayPause = { audioEngine.togglePlayPause() },
                onSeekTo = { pos: Long -> audioEngine.seekTo(pos) },
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
                onQueue = { audioEngine.queueNext(currentTrack) },
                onShareTrack = { DeviceMediaManager.shareTrack(context, currentTrack) },
                onDeleteTrack = {
                    audioEngine.deleteTrack(currentTrack.id)
                    isPlayerExpanded = false
                    Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { isPlayerExpanded = false }
            )
        }

        AnimatedVisibility(
            visible = isMediaDownloaderOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing))
        ) {
            MediaDownloaderSheet(
                initialUrl = mediaDownloaderInitialUrl,
                onAddTrack = { track -> audioEngine.addDeviceTrack(track) },
                onPlayTrack = { track ->
                    audioEngine.addDeviceTrack(track)
                    audioEngine.playTrack(track)
                    isMediaDownloaderOpen = false
                    isPlayerExpanded = true
                },
                onDismiss = {
                    isMediaDownloaderOpen = false
                    mediaDownloaderInitialUrl = null
                }
            )
        }

        if (isInspectorOpen) {
            SoundCatchInspectorSheet(
                track = currentTrack,
                telemetry = telemetry,
                isSoundCatchEnabled = isSoundCatchEnabled,
                onToggleSoundCatch = { audioEngine.toggleSoundCatch() },
                onDismiss = { isInspectorOpen = false }
            )
        }

        if (isProTierOpen) ProTierBottomSheet(onDismiss = { isProTierOpen = false })
        if (isSettingsOpen) SettingsBottomSheet(onDismiss = { isSettingsOpen = false })

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
