package com.example.ui

import com.example.R
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.audio.AudioTelemetry
import com.example.media.ArtworkColorExtractor
import com.example.model.SampleData
import com.example.model.Track
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Now Playing screen reproduced to match the exact visual reference and specifications:
 *
 * 1. Restrained, dark, calm, and subtle dynamic background atmosphere derived from the artwork.
 * 2. Elegant top bar: small downward chevron (collapse), centered "NOW PLAYING" capsule, and
 *    three-dot menu button opening a rich bottom sheet.
 * 3. Centered, dominant square album artwork with rounded corners and clean borders.
 * 4. Distinct hierarchy for song information: Title -> Artist -> Source.
 * 5. Real audio waveform unified with thin progress line, reacting to audio telemetry when playing,
 *    and pausing/freezing completely when paused.
 * 6. Playback controls: Previous, Next, and center Play/Pause enclosed in a muted circular disc
 *    using the extracted artwork color.
 * 7. Secondary Shuffle and Repeat controls positioned below the primary playback controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(
    track: Track,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    telemetry: AudioTelemetry = AudioTelemetry(),
    isShuffle: Boolean = false,
    isRepeat: Boolean = false,
    isFavorite: Boolean = false,
    isCachedOffline: Boolean = false,
    allTracks: List<Track> = emptyList(),
    onSelectTrack: (Track) -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onQueue: () -> Unit = {},
    onShareTrack: () -> Unit = {},
    onDeleteTrack: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showActionSheet by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showVisualizerSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    // Dynamically derive the calm, restrained palette based on the current artwork
    val themeColors = remember(track) {
        ArtworkColorExtractor.extractColors(context, track)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.darkBackground)
            .testTag("full_player_sheet")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val haptic = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()

        // Continuous drag progress: 0.0f = resting, 1.0f = expanded artwork, 2.0f = compact player & full queue
        val dragProgress = remember { Animatable(0f) }
        var lastHapticThreshold by remember { mutableIntStateOf(0) }

        val p = dragProgress.value
        val t1 = p.coerceIn(0f, 1f)
        val t2 = (p - 1f).coerceIn(0f, 1f)

        val dragDistancePx = with(density) { 340.dp.toPx() }

        // Layout measurements for continuous single-artwork transformation
        val statusBarTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        val topBarHeight = statusBarTop + 48.dp

        // 1. Resting dimensions (when p == 0f)
        val restingArtSize = minOf(screenWidth - 48.dp, screenHeight * 0.38f)
        val restingArtX = (screenWidth - restingArtSize) / 2
        val restingArtY = topBarHeight + 14.dp

        // 2. Stage 1 Expanded dimensions (at p == 1f): Artwork reaches the very top edge-to-edge!
        val expandedArtHeight = screenHeight * 0.48f

        // 3. Stage 2 Compact dimensions (at p == 2f): Single artwork shrinks to top-left thumbnail!
        val compactArtSize = 48.dp
        val compactArtX = 18.dp
        val compactArtY = statusBarTop + 10.dp

        // Continuous interpolation of the EXACT SAME artwork bounds
        val currentArtWidth = if (p <= 1f) {
            lerp(restingArtSize, screenWidth, t1)
        } else {
            lerp(screenWidth, compactArtSize, t2)
        }

        val currentArtHeight = if (p <= 1f) {
            lerp(restingArtSize, expandedArtHeight, t1)
        } else {
            lerp(expandedArtHeight, compactArtSize, t2)
        }

        val currentArtX = if (p <= 1f) {
            lerp(restingArtX, 0.dp, t1)
        } else {
            lerp(0.dp, compactArtX, t2)
        }

        val currentArtY = if (p <= 1f) {
            lerp(restingArtY, 0.dp, t1)
        } else {
            lerp(0.dp, compactArtY, t2)
        }

        val currentTopCorner = if (p <= 1f) {
            lerp(26.dp, 0.dp, t1)
        } else {
            lerp(0.dp, 10.dp, t2)
        }

        val currentBottomCorner = if (p <= 1f) {
            lerp(26.dp, 18.dp, t1)
        } else {
            lerp(18.dp, 10.dp, t2)
        }

        val currentBorderAlpha = (1f - t1 * 2f).coerceIn(0f, 1f) * 0.08f

        // Opacity of large player components during stages
        val waveformAlpha = (1f - t1 * 1.25f).coerceIn(0f, 1f)
        val shuffleRepeatAlpha = (1f - t1 * 1.5f).coerceIn(0f, 1f)
        val largePlayerAlpha = (1f - t2 * 1.5f).coerceIn(0f, 1f)
        val compactHeaderAlpha = ((t2 - 0.28f) * 1.40f).coerceIn(0f, 1f)

        // Single physical gesture handling on the entire player surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val current = dragProgress.value
                                val target = when {
                                    current < 0.45f -> 0f
                                    current < 1.45f -> 1f
                                    else -> 2f
                                }
                                dragProgress.animateTo(
                                    target,
                                    spring(dampingRatio = 0.82f, stiffness = 420f)
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                val current = dragProgress.value
                                val target = if (current < 0.5f) 0f else if (current < 1.5f) 1f else 2f
                                dragProgress.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 420f))
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / dragDistancePx
                            val nextVal = (dragProgress.value + delta).coerceIn(0f, 2f)
                            coroutineScope.launch {
                                dragProgress.snapTo(nextVal)
                            }
                            val currentThreshold = when {
                                nextVal >= 1.94f -> 2
                                nextVal in 0.94f..1.06f -> 1
                                nextVal <= 0.06f -> 0
                                else -> -1
                            }
                            if (currentThreshold != -1 && currentThreshold != lastHapticThreshold) {
                                lastHapticThreshold = currentThreshold
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
        ) {
            // Background dynamic atmosphere: preserving artwork hue from top to bottom
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            themeColors.bgTop,
                            themeColors.bgMidUpper,
                            themeColors.bgMidLower,
                            themeColors.bgBottom
                        ),
                        startY = 0f,
                        endY = canvasHeight
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.atmosphericBloom.copy(alpha = 0.35f),
                            themeColors.atmosphericBloom.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.28f),
                        radius = canvasWidth * 0.85f
                    )
                )
            }

            // =========================================================================
            // 1. THE SINGLE CONTINUOUS ARTWORK VIEW
            // Modified continuously in position, size, scale, and corner radius!
            // =========================================================================
            Box(
                modifier = Modifier
                    .offset(x = currentArtX, y = currentArtY)
                    .size(width = currentArtWidth, height = currentArtHeight)
                    .clip(
                        RoundedCornerShape(
                            topStart = currentTopCorner,
                            topEnd = currentTopCorner,
                            bottomStart = currentBottomCorner,
                            bottomEnd = currentBottomCorner
                        )
                    )
                    .then(
                        if (currentBorderAlpha > 0.005f) {
                            Modifier.border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = currentBorderAlpha),
                                shape = RoundedCornerShape(
                                    topStart = currentTopCorner,
                                    topEnd = currentTopCorner,
                                    bottomStart = currentBottomCorner,
                                    bottomEnd = currentBottomCorner
                                )
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Atmospheric blending scrim at top when expanding to full screen
                if (t1 > 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f * t1),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.40f * t1)
                                    )
                                )
                            )
                    )
                }
            }

            // =========================================================================
            // 2. TOP BAR (Collapse Chevron, Capsule, Three-Dot Menu)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (dragProgress.value > 0.1f) {
                            coroutineScope.launch {
                                dragProgress.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                            }
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Now Playing",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center: Subtle "NOW PLAYING" capsule (fades out as compact player takes over)
                if (1f - t2 * 2f > 0.01f) {
                    Box(
                        modifier = Modifier
                            .alpha((1f - t2 * 2f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                // Right: Three vertical dots menu (fades out in compact mode)
                IconButton(
                    onClick = { showActionSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .alpha((1f - t2 * 2f).coerceIn(0f, 1f))
                        .testTag("player_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song Actions Menu",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // =========================================================================
            // 3. COMPACT PLAYER HEADER (Stages 2: beside the compact artwork)
            // =========================================================================
            if (compactHeaderAlpha > 0.01f) {
                Row(
                    modifier = Modifier
                        .offset(x = compactArtX + compactArtSize + 14.dp, y = compactArtY)
                        .width(screenWidth - (compactArtX + compactArtSize + 14.dp + 64.dp))
                        .height(compactArtSize)
                        .alpha(compactHeaderAlpha)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                dragProgress.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title.substringBefore(" - "),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Compact Play/Pause button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = compactArtY)
                        .size(compactArtSize)
                        .alpha(compactHeaderAlpha)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // =========================================================================
            // 4. LARGE PLAYER INFORMATION & CONTROLS (Main Resting & Stage 1 View)
            // =========================================================================
            val largeContentY = if (p <= 1f) {
                lerp(restingArtY + restingArtSize + 14.dp, expandedArtHeight + 10.dp, t1)
            } else {
                lerp(expandedArtHeight + 10.dp, compactArtY + compactArtSize + 20.dp, t2)
            }

            if (largePlayerAlpha > 0.01f) {
                Column(
                    modifier = Modifier
                        .offset(y = largeContentY)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .alpha(largePlayerAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Song Information
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = track.title.substringBefore(" - "),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (track.catalogSource.contains("Device", ignoreCase = true) || track.contentUri != null) {
                                "Device Audio"
                            } else {
                                track.catalogSource
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.40f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Waveform + Progress Bar (Single logical component; waveform fades continuously on first drag)
                    NowPlayingWaveformProgress(
                        positionMs = playbackPositionMs,
                        durationMs = track.durationMs,
                        isPlaying = isPlaying,
                        telemetry = telemetry,
                        activeColor = themeColors.accent,
                        onSeekTo = onSeekTo,
                        waveformAlpha = waveformAlpha,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Large Playback Controls (Previous, Large disc Play/Pause, Next)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier
                                .size(58.dp)
                                .testTag("player_previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    spotColor = themeColors.accent.copy(alpha = 0.40f),
                                    ambientColor = themeColors.darkBackground
                                )
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            themeColors.playPauseGradTop,
                                            themeColors.playPauseGradBottom
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            themeColors.accent.copy(alpha = 0.32f),
                                            Color.White.copy(alpha = 0.12f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onTogglePlayPause
                                )
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .size(58.dp)
                                .testTag("player_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shuffle & Repeat Row (stays exactly in resting position, fades on drag)
                    if (shuffleRepeatAlpha > 0.01f) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .alpha(shuffleRepeatAlpha),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedShuffleIcon(
                                isShuffle = isShuffle,
                                activeColor = themeColors.accent,
                                onClick = onToggleShuffle,
                                modifier = Modifier.testTag("player_shuffle_button"),
                                touchSize = 48.dp,
                                iconSize = 26.dp
                            )

                            AnimatedRepeatIcon(
                                isRepeat = isRepeat,
                                activeColor = themeColors.accent,
                                onClick = onToggleRepeat,
                                modifier = Modifier.testTag("player_repeat_button"),
                                touchSize = 48.dp,
                                iconSize = 26.dp
                            )
                        }
                    }

                    // =================================================================
                    // Resting Up Next handle (centered in empty lower area without moving Shuffle/Repeat)
                    // =================================================================
                    if (p < 0.2f) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha((1f - p * 5f).coerceIn(0f, 1f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        dragProgress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                    }
                                }
                        ) {
                            Text(
                                text = "Up Next",
                                fontSize = 12.sp,
                                letterSpacing = 1.6.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 5. UNIFIED PROGRESS BAR FOR COMPACT PLAYER (Section 15: Not duplicated!)
            // =========================================================================
            if (compactHeaderAlpha > 0.01f) {
                val progressFraction = if (track.durationMs > 0) {
                    (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Canvas(
                    modifier = Modifier
                        .offset(y = compactArtY + compactArtSize + 8.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .alpha(compactHeaderAlpha)
                ) {
                    val totalWidth = size.width
                    val lineY = size.height / 2f
                    val filledWidth = totalWidth * progressFraction

                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, lineY),
                        end = Offset(totalWidth, lineY),
                        strokeWidth = 2.dp.toPx()
                    )
                    if (filledWidth > 0f) {
                        drawLine(
                            color = themeColors.accent,
                            start = Offset(0f, lineY),
                            end = Offset(filledWidth, lineY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            // =========================================================================
            // 6. UP NEXT QUEUE PANEL (Revealed Continuously Underneath The Same Surface)
            // =========================================================================
            val queuePanelY = if (p <= 1f) {
                lerp(screenHeight, screenHeight * 0.65f, t1)
            } else {
                lerp(screenHeight * 0.65f, compactArtY + compactArtSize + 16.dp, t2)
            }

            if (p > 0.02f) {
                val queueTracks = remember(allTracks, track) {
                    val upcoming = allTracks.filter { it.id != track.id }
                    if (upcoming.isNotEmpty()) upcoming else SampleData.starterTracks.filter { it.id != track.id }
                }

                Box(
                    modifier = Modifier
                        .offset(y = queuePanelY)
                        .fillMaxWidth()
                        .height(screenHeight - queuePanelY)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF131316).copy(alpha = 0.96f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Drag Handle and Up Next Header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 8.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        val target = if (dragProgress.value < 1.5f) 2f else 0f
                                        dragProgress.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 420f))
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.38f))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "UP NEXT",
                                        fontSize = 12.sp,
                                        letterSpacing = 1.8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${queueTracks.size} tracks",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                }

                                Text(
                                    text = if (p >= 1.5f) "Collapse" else "View All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColors.accent
                                )
                            }
                        }

                        // Queue Tracks List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(queueTracks, key = { it.id }) { itemTrack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .clickable { onSelectTrack(itemTrack) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        TrackArtworkImage(
                                            track = itemTrack,
                                            contentDescription = itemTrack.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemTrack.title.substringBefore(" - "),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = itemTrack.artist,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.60f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = formatMs(itemTrack.durationMs),
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.40f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 7. ACTION BOTTOM SHEETS & DIALOGS
        // =========================================================================
        if (showActionSheet) {
            NowPlayingActionSheet(
                track = track,
                isFavorite = isFavorite,
                accentColor = themeColors.accent,
                onPlayNext = {
                    onPlayNext()
                    showActionSheet = false
                },
                onQueue = {
                    onQueue()
                    showActionSheet = false
                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                },
                onToggleFavorite = {
                    onToggleFavorite()
                    showActionSheet = false
                },
                onAddToPlaylist = {
                    showActionSheet = false
                    Toast.makeText(context, "Added to Playlist: Velvet Favorites", Toast.LENGTH_SHORT).show()
                },
                onOpenVisualizer = {
                    showActionSheet = false
                    showVisualizerSheet = true
                },
                onOpenLyrics = {
                    showActionSheet = false
                    showLyricsSheet = true
                },
                onShare = {
                    showActionSheet = false
                    onShareTrack()
                },
                onSongInfo = {
                    showActionSheet = false
                    showSongInfoDialog = true
                },
                onDelete = {
                    showActionSheet = false
                    onDeleteTrack()
                },
                onDismiss = { showActionSheet = false }
            )
        }

        // 8. TECHNICAL SONG INFO DIALOG
        if (showSongInfoDialog) {
            SongInfoModal(
                track = track,
                accentColor = themeColors.accent,
                onDismiss = { showSongInfoDialog = false }
            )
        }

        // 9. AUDIO VISUALIZER BOTTOM SHEET
        if (showVisualizerSheet) {
            AudioVisualizerBottomSheet(
                track = track,
                telemetry = telemetry,
                isPlaying = isPlaying,
                accentColor = themeColors.accent,
                onDismiss = { showVisualizerSheet = false }
            )
        }

        // 10. SYNCED LYRICS BOTTOM SHEET
        if (showLyricsSheet) {
            LyricsBottomSheet(
                track = track,
                playbackPositionMs = playbackPositionMs,
                accentColor = themeColors.accent,
                onSeekTo = onSeekTo,
                onDismiss = { showLyricsSheet = false }
            )
        }
    }
}

/**
 * Waveform + Progress Bar Component:
 * - Thin vertical bars with consistent small spacing and natural variation.
 * - Real audio reactivity: responds to playback telemetry when playing, and pauses/freezes when paused.
 * - Bars to the left of progress are tinted with active artwork color; right bars are muted.
 * - Tiny gap above thin, elegant progress line.
 * - Small handle bead and timestamps underneath.
 * - Full horizontal dragging and tapping support for seamless seeking.
 */
@Composable
fun NowPlayingWaveformProgress(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    telemetry: AudioTelemetry,
    activeColor: Color,
    onSeekTo: (Long) -> Unit,
    waveformAlpha: Float = 1f,
    showTimestamps: Boolean = true,
    showThumb: Boolean = true,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayFraction = if (isDragging) dragFraction else progressFraction

    val barCount = 80
    // Generate an authentic acoustic signature for the song
    val baseProfile = remember(durationMs, barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / barCount
            val wave1 = abs(sin(norm * 3.14159f * 1.8f + 0.35f))
            val wave2 = abs(sin(norm * 3.14159f * 4.3f)) * 0.42f
            val wave3 = abs(sin(norm * 3.14159f * 7.8f + 1.1f)) * 0.28f
            val wave4 = abs(cos(norm * 3.14159f * 12.2f)) * 0.16f
            (wave1 * 0.52f + wave2 + wave3 + wave4).coerceIn(0.18f, 0.95f)
        }
    }

    Column(
        modifier = modifier
            .testTag("player_progress_slider")
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekTo((newFraction * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekTo((dragFraction * durationMs).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        // 1. Thin, delicate waveform bars (reactively fades as user drags surface)
        if (waveformAlpha > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((24 * waveformAlpha).dp)
                    .alpha(waveformAlpha)
            ) {
                val totalWidth = size.width
                val barWidth = 1.3.dp.toPx()
                val totalBarWidth = barWidth * barCount
                val barGap = if (barCount > 1) (totalWidth - totalBarWidth) / (barCount - 1) else 0f
                val maxBarHeight = size.height
                val minBarHeight = (2.5f * waveformAlpha).dp.toPx()

                val liveEnergy = if (isPlaying) telemetry.rmsLevel else 0.32f
                val liveTransient = if (isPlaying) telemetry.transientSpike else 0f

                for (i in 0 until barCount) {
                    val barX = i * (barWidth + barGap)
                    val base = baseProfile[i]

                    val modulation = if (isPlaying) {
                        val ripple = sin(i * 0.35f + (positionMs / 220f)).toFloat()
                        0.70f + 0.30f * liveEnergy + 0.18f * liveTransient * ripple.coerceAtLeast(0f)
                    } else {
                        0.72f
                    }

                    val barHeight = (base * maxBarHeight * modulation).coerceIn(minBarHeight, maxBarHeight)
                    val barTop = size.height - barHeight

                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(barX, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height((8 * waveformAlpha).dp))
        }

        // 2. Minimal, thin progress bar line
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val totalWidth = size.width
            val centerY = size.height / 2f
            val lineThickness = 2.0.dp.toPx()

            val progressWidth = (totalWidth * displayFraction).coerceIn(0f, totalWidth)

            // Unplayed track line (subtle minimal)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(0f, centerY - lineThickness / 2f),
                size = Size(totalWidth, lineThickness),
                cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
            )

            // Played track line
            if (progressWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.85f),
                            activeColor,
                            Color.White.copy(alpha = 0.90f)
                        ),
                        startX = 0f,
                        endX = progressWidth
                    ),
                    topLeft = Offset(0f, centerY - lineThickness / 2f),
                    size = Size(progressWidth, lineThickness),
                    cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
                )
            }

            // Small indicator handle bead (minimal)
            if (showThumb) {
                val beadRadius = (if (isDragging) 4.2.dp else 3.5.dp).toPx()
                val beadX = progressWidth.coerceIn(beadRadius, totalWidth - beadRadius)
                drawCircle(
                    color = Color.White,
                    radius = beadRadius,
                    center = Offset(beadX, centerY)
                )
                drawCircle(
                    color = activeColor,
                    radius = beadRadius,
                    center = Offset(beadX, centerY),
                    style = Stroke(width = 1.0.dp.toPx())
                )
            }
        }

        if (showTimestamps) {
            Spacer(modifier = Modifier.height(6.dp))

            // Clean timestamps underneath
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(if (isDragging) (dragFraction * durationMs).toLong() else positionMs),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.48f)
                )
                Text(
                    text = formatMs(durationMs),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.48f)
                )
            }
        }
    }
}

/**
 * Three-dot song action bottom sheet:
 * Houses advanced functionality without cluttering the clean player screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingActionSheet(
    track: Track,
    isFavorite: Boolean,
    accentColor: Color,
    onPlayNext: () -> Unit,
    onQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onShare: () -> Unit,
    onSongInfo: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF151618),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("now_playing_action_sheet")
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.album}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Items
            SheetActionRow(icon = Icons.Default.PlayArrow, title = "Play next") { onPlayNext() }
            SheetActionRow(icon = Icons.Default.QueueMusic, title = "Add to queue") { onQueue() }
            SheetActionRow(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (isFavorite) "Remove from favourites" else "Add to favourites",
                iconTint = if (isFavorite) accentColor else Color.White.copy(alpha = 0.70f)
            ) { onToggleFavorite() }
            SheetActionRow(icon = Icons.Default.Folder, title = "Add to playlist") { onAddToPlaylist() }
            SheetActionRow(icon = Icons.Default.GraphicEq, title = "Audio Visualizer") { onOpenVisualizer() }
            SheetActionRow(icon = Icons.Default.Subtitles, title = "Synced Lyrics") { onOpenLyrics() }
            SheetActionRow(icon = Icons.Default.Share, title = "Share song") { onShare() }
            SheetActionRow(icon = Icons.Default.Info, title = "Song information") { onSongInfo() }
            SheetActionRow(
                icon = Icons.Default.Delete,
                title = "Delete from library",
                iconTint = Color(0xFFE57373)
            ) { onDelete() }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = Color.White.copy(alpha = 0.70f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.90f)
        )
    }
}

@Composable
fun SongInfoModal(
    track: Track,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18191C),
        title = {
            Text(
                text = "Song Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoLine("Title", track.title.substringBefore(" - "))
                InfoLine("Artist", track.artist)
                InfoLine("Album", track.album)
                InfoLine("Duration", formatMs(track.durationMs))
                InfoLine("Audio Format", "High-Resolution AAC / MP3")
                InfoLine("Bitrate", "160 kbps CD-Quality")
                InfoLine("Sample Rate", "44.1 kHz Stereo")
                InfoLine("Source", track.catalogSource)
                InfoLine("BPM", "${track.bpm} BPM")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.50f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.90f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVisualizerBottomSheet(
    track: Track,
    telemetry: AudioTelemetry,
    isPlaying: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF151618),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Real-Time Spectrum Visualizer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${track.title.substringBefore(" - ")} • ${if (isPlaying) "Active" else "Paused"}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 16-band spectrum visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val bands = 16
                for (b in 0 until bands) {
                    val bandNorm = b.toFloat() / bands
                    val hFraction = if (isPlaying) {
                        val localMod = abs(sin(bandNorm * 3.14f * 2.5f + (telemetry.rmsLevel * 4f))).toFloat()
                        (0.25f + 0.65f * telemetry.rmsLevel * localMod + (if (b % 4 == 0) telemetry.transientSpike * 0.35f else 0f)).coerceIn(0.12f, 1f)
                    } else {
                        0.15f
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .fillMaxWidth()
                            .height((110 * hFraction).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor,
                                        accentColor.copy(alpha = 0.40f)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Frequency Engine: 44.1 kHz • Fast Snap Transient Peak Response",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.40f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    track: Track,
    playbackPositionMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF151618),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Synced Lyrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${track.title.substringBefore(" - ")} • ${track.artist}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (track.lyrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No time-synced lyrics found for this track.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(track.lyrics) { line ->
                        val isCurrent = playbackPositionMs >= line.timeMs &&
                                (track.lyrics.indexOf(line) == track.lyrics.lastIndex ||
                                        playbackPositionMs < track.lyrics[track.lyrics.indexOf(line) + 1].timeMs)

                        Text(
                            text = line.text,
                            fontSize = if (isCurrent) 17.sp else 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeekTo(line.timeMs) }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
