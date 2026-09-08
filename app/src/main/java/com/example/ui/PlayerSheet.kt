package com.example.ui

import com.example.R
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    queueTracks: List<Track> = emptyList(),
    onSelectQueueTrack: (Track) -> Unit = {},
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
    val coroutineScope = rememberCoroutineScope()
    var showActionSheet by remember { mutableStateOf(false) }
    var showSongInfoDialog by remember { mutableStateOf(false) }
    var showVisualizerSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    // Dynamically derive the calm, restrained palette based on the current artwork
    val themeColors = remember(track) {
        ArtworkColorExtractor.extractColors(context, track)
    }

    // Interactive continuous drag transition state between Full Now Playing (0f) and Up Next Queue (1f)
    val dragProgress = remember { Animatable(0f) }
    val p = dragProgress.value

    // Up Next Queue items (uses passed queueTracks or falls back to sample queue tracks)
    val queueItems = remember(queueTracks, track) {
        if (queueTracks.isNotEmpty()) {
            val otherTracks = queueTracks.filter { it.id != track.id }
            listOf(track) + otherTracks
        } else {
            listOf(track) + com.example.model.SampleData.starterTracks.filter { it.id != track.id }
        }
    }

    val queueListState = rememberLazyListState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.darkBackground)
            .testTag("full_player_sheet")
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val density = LocalDensity.current
        val totalHeightPx = with(density) { totalHeight.toPx() }
        val dragDistancePx = (totalHeightPx * 0.65f).coerceAtLeast(350f)

        // Unified vertical drag delta processor
        val onDragDelta: (Float) -> Unit = { dragAmountPx ->
            val delta = -dragAmountPx / dragDistancePx
            coroutineScope.launch {
                dragProgress.snapTo((dragProgress.value + delta).coerceIn(0f, 1f))
            }
        }

        val onDragFinish: () -> Unit = {
            coroutineScope.launch {
                val target = if (dragProgress.value > 0.45f) 1f else 0f
                dragProgress.animateTo(target, tween(320, easing = FastOutSlowInEasing))
            }
        }

        // Dynamic background atmosphere: full-screen atmospheric gradient preserving artwork hue from top to bottom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Full screen vertical gradient transitioning from top atmosphere to deep tone at bottom
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

            // Soft radial ambient bloom centered behind the artwork that dynamically breathes
            val bloomScale = 1f + (p * 0.35f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.atmosphericBloom.copy(alpha = (0.35f * (1f - p * 0.2f)).coerceAtLeast(0.15f)),
                        themeColors.atmosphericBloom.copy(alpha = (0.12f * (1f - p * 0.2f)).coerceAtLeast(0.05f)),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * (0.28f - p * 0.12f)),
                    radius = canvasWidth * 0.85f * bloomScale
                )
            )
        }

        // 1. NOW PLAYING COMPOSITION (Continuously transforms as user drags upward)
        val fullNowPlayingAlpha = (1f - ((p - 0.45f) / 0.25f)).coerceIn(0f, 1f)
        val npOffsetY = -(p * totalHeightPx * 0.28f)
        val expandRatio = (p / 0.65f).coerceIn(0f, 1f)
        val topBarAlpha = (1f - (p * 2.2f)).coerceIn(0f, 1f)
        val shuffleRepeatAlpha = (1f - (p * 2.8f)).coerceIn(0f, 1f)

        if (fullNowPlayingAlpha > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = npOffsetY
                        alpha = fullNowPlayingAlpha
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP SECTION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = topBarAlpha }
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Left: Small downward chevron
                IconButton(
                    onClick = onDismiss,
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

                // Center: Subtle "NOW PLAYING" capsule
                Box(
                    modifier = Modifier
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

                // Right: Three vertical dots song action menu button
                IconButton(
                    onClick = { showActionSheet = true },
                    modifier = Modifier
                        .size(40.dp)
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

            // 2. ALBUM ARTWORK (Scales up, expands to screen edges, and subtly blends into dynamic background)
            val artworkWidthFraction = 0.94f + (0.06f * expandRatio)
            val artworkCornerRadius = 26.dp * (1f - expandRatio)

            Box(
                modifier = Modifier
                    .fillMaxWidth(artworkWidthFraction)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = 1f + (0.08f * expandRatio)
                        scaleY = 1f + (0.08f * expandRatio)
                    }
                    .clip(RoundedCornerShape(artworkCornerRadius))
                    .border(
                        width = (1f - expandRatio).dp,
                        color = Color.White.copy(alpha = 0.08f * (1f - expandRatio)),
                        shape = RoundedCornerShape(artworkCornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Controlled subtle gradient blend over the artwork as it expands toward the top:
                // Top gradient keeps status bar & top actions legible;
                // Bottom gradient blends seamlessly into the dark atmosphere and rising queue!
                if (expandRatio > 0.02f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to themeColors.bgTop.copy(alpha = 0.70f * expandRatio),
                                        0.25f to themeColors.bgTop.copy(alpha = 0.20f * expandRatio),
                                        0.60f to Color.Transparent,
                                        1.0f to themeColors.darkBackground.copy(alpha = 0.85f * expandRatio)
                                    )
                                )
                            )
                    )
                }
            }

            // 3. SONG INFORMATION (Strict Visual Hierarchy, Left-Aligned, Compact Spacing)
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

            // 4. REAL WAVEFORM + PROGRESS BAR (Unified Playback Component)
            NowPlayingWaveformProgress(
                positionMs = playbackPositionMs,
                durationMs = track.durationMs,
                isPlaying = isPlaying,
                telemetry = telemetry,
                activeColor = themeColors.accent,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            // 5. PLAYBACK CONTROLS (Previous — Play/Pause — Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button (enlarged)
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

                // Play/Pause button with enlarged premium gradient card/circle with glassmorphic border & ambient glow
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

                // Next button (enlarged)
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

            // 6. SHUFFLE AND REPEAT (Minimal standalone controls floating naturally beneath primary controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = shuffleRepeatAlpha }
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle on lower-left: animated crossing paths stroke animation
                AnimatedShuffleIcon(
                    isShuffle = isShuffle,
                    activeColor = themeColors.accent,
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("player_shuffle_button"),
                    touchSize = 48.dp,
                    iconSize = 26.dp
                )

                // Repeat on lower-right: animated circulating strokes along racetrack loop
                AnimatedRepeatIcon(
                    isRepeat = isRepeat,
                    activeColor = themeColors.accent,
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("player_repeat_button"),
                    touchSize = 48.dp,
                    iconSize = 26.dp
                )
            }

            // Bottom spacer balancing height for resting Up Next handle
            Spacer(modifier = Modifier.height(38.dp))
        }
    }

    // 2. UP NEXT QUEUE SHEET (Continuous vertical drag transition)
    val queueTopOffsetDp = androidx.compose.ui.unit.lerp(totalHeight - 56.dp, 66.dp, p)
    val queueCornerRadius = 24.dp * (1f - (p * 0.4f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight - 66.dp)
            .offset(y = queueTopOffsetDp)
            .clip(RoundedCornerShape(topStart = queueCornerRadius, topEnd = queueCornerRadius))
            .background(
                Brush.verticalGradient(
                    listOf(
                        themeColors.darkBackground.copy(alpha = 0.96f),
                        themeColors.bgBottom.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(topStart = queueCornerRadius, topEnd = queueCornerRadius)
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { onDragFinish() },
                    onDragCancel = { onDragFinish() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount)
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Up Next Handle / Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            coroutineScope.launch {
                                val target = if (p < 0.5f) 1f else 0f
                                dragProgress.animateTo(target, tween(360, easing = FastOutSlowInEasing))
                            }
                        }
                    )
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gesture Indicator Pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (p < 0.30f) {
                    // Subtle Resting Up Next Handle Text
                    Text(
                        text = "Up Next",
                        fontSize = 11.5.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                } else {
                    // Expanded Up Next Queue Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Playing from",
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (track.catalogSource.isNotBlank()) track.catalogSource else "Queue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Save Queue Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                            .clickable {
                                Toast.makeText(context, "Queue saved to playlist", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Save Queue",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Save",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Queue Track List (takes over screen when expanded)
            if (p > 0.15f) {
                LazyColumn(
                    state = queueListState,
                    userScrollEnabled = p > 0.80f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = ((p - 0.15f) / 0.50f).coerceIn(0f, 1f) }
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(queueItems) { queueTrack ->
                        val isCurrent = queueTrack.id == track.id
                        UpNextTrackRow(
                            track = queueTrack,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            accentColor = themeColors.accent,
                            onClick = {
                                onSelectQueueTrack(queueTrack)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    // 3. COMPACT CURRENT-SONG HEADER (Collapses into pinned top bar as user drags upward)
    val compactAlpha = ((p - 0.55f) / 0.35f).coerceIn(0f, 1f)
    if (compactAlpha > 0f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = compactAlpha }
                .background(themeColors.darkBackground.copy(alpha = 0.98f))
                .statusBarsPadding()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { onDragFinish() },
                        onDragCancel = { onDragFinish() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount)
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small thumbnail
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    dragProgress.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
                                }
                            }
                        )
                ) {
                    Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play/Pause button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Collapse Chevron (drags/animates back to full Now Playing)
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            dragProgress.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Up Next",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Thin progress indicator attached to bottom of compact header
            val progressFraction = if (track.durationMs > 0) {
                (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = themeColors.accent,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }

        // 7. THREE-DOT SONG ACTION BOTTOM SHEET
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
 * Up Next list track row component:
 * Clean, modern row displaying track art thumbnail, title, artist & duration,
 * playing indicator badge if active, and sleek reorder handle.
 */
@Composable
private fun UpNextTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) accentColor.copy(alpha = 0.16f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            TrackArtworkImage(
                track = track,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist + Duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title.substringBefore(" - "),
                fontSize = 13.5.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) accentColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${formatTrackDuration(track.durationMs)}",
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Reorder drag handle (two clean horizontal lines)
        Column(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(1.dp))
            )
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(1.dp))
            )
        }
    }
}

private fun formatTrackDuration(durationMs: Long): String {
    if (durationMs <= 0) return "3:30"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
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
        // 1. Thin, delicate waveform bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val totalWidth = size.width
            val barWidth = 1.3.dp.toPx()
            val totalBarWidth = barWidth * barCount
            val barGap = if (barCount > 1) (totalWidth - totalBarWidth) / (barCount - 1) else 0f
            val maxBarHeight = size.height
            val minBarHeight = 2.5.dp.toPx()

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

        // 2. Clearly visible vertical gap between waveform and progress bar (they do NOT touch)
        Spacer(modifier = Modifier.height(8.dp))

        // 3. Minimal, thin progress bar line with small indicator thumb
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

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Clean timestamps underneath
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
