package com.example.ui

import com.example.R
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import android.graphics.Bitmap
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.audio.AudioTelemetry
import com.example.media.ArtworkColorExtractor
import com.example.media.TrackThemeColors
import com.example.model.Track
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

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
    onPlayNextTrack: (Track) -> Unit = {},
    onReorderQueue: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onUpdateQueue: ((List<Track>) -> Unit)? = null,
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

    // Dynamically derive the calm, restrained palette based on the current artwork.
    // The resolved bitmap is passed directly into the color extractor state update loop.
    var resolvedArtworkBitmap by remember(track.id, track.artworkUri, track.coverResId) {
        mutableStateOf<Bitmap?>(null)
    }
    var themeColors by remember(track.id) {
        val initial = ArtworkColorExtractor.resolveTrackBitmap(context, track)
            ?: if (track.coverResId != 0) {
                runCatching { BitmapFactory.decodeResource(context.resources, track.coverResId) }.getOrNull()
            } else null
        mutableStateOf(ArtworkColorExtractor.extractColorsFromBitmap(initial))
    }

    LaunchedEffect(track.id, track.artworkUri, track.coverResId) {
        withContext(Dispatchers.IO) {
            val bitmap = ArtworkColorExtractor.resolveTrackBitmap(context, track)
                ?: if (track.coverResId != 0) {
                    runCatching { BitmapFactory.decodeResource(context.resources, track.coverResId) }.getOrNull()
                } else null
            val extractedColors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
            withContext(Dispatchers.Main) {
                resolvedArtworkBitmap = bitmap
                themeColors = extractedColors
            }
        }
    }

    val upNextExpanded = remember { Animatable(0f) }
    val upNextP = upNextExpanded.value
    val haptic = LocalHapticFeedback.current
    val queueDragDensity = LocalDensity.current

    // Up Next Queue items (uses passed queueTracks or falls back to sample queue tracks)
    // YouTube Music Hierarchy: Index 0 is currently playing track, Index 1 is Up Next, etc.
    // Preserve the queue's physical order. Selecting a track must not move it to the top.
    val queueItems = remember(queueTracks, track.id) {
        val raw = if (queueTracks.isNotEmpty()) queueTracks.distinctBy { it.id }
        else com.example.model.SampleData.starterTracks.distinctBy { it.id }
        if (raw.any { it.id == track.id }) raw else raw + track
    }
    var orderedQueueItems by remember(queueItems) { mutableStateOf(queueItems) }
    val queueListState = rememberLazyListState()

    var activeQueueDragId by remember { mutableStateOf<String?>(null) }
    var activeQueueDragIndex by remember { mutableIntStateOf(-1) }
    var queueDragOffsetY by remember { mutableFloatStateOf(0f) }
    var queueDragTargetIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(queueItems) {
        if (activeQueueDragId == null) {
            orderedQueueItems = queueItems
        }
    }

    fun beginQueueDrag(id: String, index: Int, canDrag: Boolean = true) {
        if (!canDrag || index < 0 || activeQueueDragId != null) return
        activeQueueDragId = id
        activeQueueDragIndex = index
        queueDragOffsetY = 0f
        queueDragTargetIndex = index
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun updateQueueDrag(deltaY: Float) {
        val dragId = activeQueueDragId ?: return
        val startIndex = orderedQueueItems.indexOfFirst { it.id == dragId }
        if (startIndex < 0) return
        queueDragOffsetY += deltaY
        val itemHeightPx = with(queueDragDensity) { 60.dp.toPx() }
        val slots = (queueDragOffsetY / itemHeightPx).roundToInt()
        val newTarget = (startIndex + slots).coerceIn(0, orderedQueueItems.lastIndex.coerceAtLeast(0))
        if (newTarget != queueDragTargetIndex) {
            queueDragTargetIndex = newTarget
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun finishQueueDrag() {
        val dragId = activeQueueDragId
        val from = if (dragId != null) orderedQueueItems.indexOfFirst { it.id == dragId } else -1
        val to = queueDragTargetIndex
        if (dragId != null && from >= 0 && to >= 0 && from < orderedQueueItems.size && to < orderedQueueItems.size && from != to) {
            val updatedQueue = orderedQueueItems.toMutableList().apply {
                add(to, removeAt(from))
            }
            orderedQueueItems = updatedQueue
            onReorderQueue?.invoke(from, to)
            onUpdateQueue?.invoke(updatedQueue)
        }
        activeQueueDragId = null
        activeQueueDragIndex = -1
        queueDragOffsetY = 0f
        queueDragTargetIndex = -1
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16181F)) // Ashes gray background matching user screenshot
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("full_player_sheet")
    ) {
        val totalHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP HEADER (Subtle collapse on left, empty center - NO "NOW PLAYING" text, three-dot options on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center is empty as shown in the screenshot (no "NOW PLAYING" pill)
                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { showActionSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("player_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More Options",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. LARGE MAIN PLAYER GLASS CARD
            // Smoothly mixing ashes gray with the track's color in an ultra-slow, continuous ambient drift
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
            ) {
                SmokyAtmosphericCardBackground(
                    themeColors = themeColors,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Track Title & Artist (Centered at top of card)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = track.title,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_track_title")
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = track.artist.uppercase(),
                            fontSize = 12.5.sp,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.58f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_track_artist")
                        )
                    }

                    // Centered Album Artwork floating in middle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val artSize = minOf(maxWidth - 36.dp, maxHeight * 0.88f).coerceIn(150.dp, 230.dp)
                            Box(
                                modifier = Modifier
                                    .size(artSize)
                                    .shadow(
                                        elevation = 18.dp,
                                        shape = RoundedCornerShape(22.dp),
                                        spotColor = Color.Black.copy(alpha = 0.70f),
                                        ambientColor = Color.Black
                                    )
                                    .clip(RoundedCornerShape(22.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                TrackArtworkImage(
                                    track = track,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Progress Bar & Timestamps
                    NowPlayingProgressBar(
                        positionMs = playbackPositionMs,
                        durationMs = track.durationMs,
                        onSeekTo = onSeekTo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Waveform Visualizer (Hugging the bottom of the card as a dark silhouette)
                    DarkSilhouetteWaveform(
                        isPlaying = isPlaying,
                        telemetry = telemetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. CONTROLS ROW (Directly underneath the card on the ash background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                AnimatedShuffleIcon(
                    isShuffle = isShuffle,
                    activeColor = themeColors.accent,
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("player_shuffle_button"),
                    touchSize = 46.dp,
                    iconSize = 24.dp
                )

                // Previous
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Circular Glass Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.50f))
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.11f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
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
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Repeat
                AnimatedRepeatIcon(
                    isRepeat = isRepeat,
                    activeColor = themeColors.accent,
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("player_repeat_button"),
                    touchSize = 46.dp,
                    iconSize = 24.dp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. DISCREET UP NEXT SWIPE-UP HANDLE (Keeps screen clean while leaving Up Next easily accessible)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            coroutineScope.launch {
                                upNextExpanded.animateTo(1f)
                            }
                        }
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -12f) {
                                coroutineScope.launch {
                                    upNextExpanded.animateTo(1f)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // 5. EXPANDED UP NEXT QUEUE SHEET (Revealed on swipe-up or handle tap)
        if (upNextP > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (1f - upNextP) * totalHeight.toPx()
                        alpha = upNextP.coerceIn(0f, 1f)
                    }
                    .background(Color(0xFF14151C))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    upNextExpanded.animateTo(0f)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse Queue",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Playing from Queue",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${orderedQueueItems.size} tracks",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.60f)
                            )
                        }

                        TextButton(
                            onClick = {
                                Toast.makeText(context, "Queue saved to playlist", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "Save",
                                color = themeColors.accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Pull-down handle to collapse back to player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 15f) {
                                        coroutineScope.launch {
                                            upNextExpanded.animateTo(0f)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.30f))
                        )
                    }

                    // Full interactive reorderable/swipeable queue
                    LazyColumn(
                        state = queueListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            items = orderedQueueItems,
                            key = { _, item -> item.id }
                        ) { queueIndex, queueTrack ->
                            val isCurrent = queueTrack.id == track.id
                            val isDragging = queueTrack.id == activeQueueDragId

                            UpNextTrackRow(
                                track = queueTrack,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying,
                                accentColor = themeColors.accent,
                                surfaceColor = Color(0xFF1E2028),
                                onClick = { onSelectQueueTrack(queueTrack) },
                                onPlayNext = {
                                    val playingIndex = orderedQueueItems.indexOfFirst { it.id == track.id }
                                    val currentIndex = orderedQueueItems.indexOfFirst { it.id == queueTrack.id }
                                    if (currentIndex >= 0) {
                                        val destination = if (playingIndex >= 0) playingIndex + 1 else 0
                                        val updated = orderedQueueItems.toMutableList().apply {
                                            val moved = removeAt(currentIndex)
                                            add(destination.coerceAtMost(size), moved)
                                        }
                                        orderedQueueItems = updated
                                        onUpdateQueue?.invoke(updated)
                                        coroutineScope.launch {
                                            queueListState.animateScrollToItem(playingIndex.coerceAtLeast(0))
                                        }
                                    }
                                },
                                onDelete = {
                                    if (!isCurrent && activeQueueDragId == null) {
                                        val updated = orderedQueueItems.filterNot { it.id == queueTrack.id }
                                        orderedQueueItems = updated
                                        onUpdateQueue?.invoke(updated)
                                    }
                                },
                                onDragStart = { beginQueueDrag(queueTrack.id, queueIndex) },
                                onDragBy = { dy -> if (activeQueueDragId == queueTrack.id) updateQueueDrag(dy) },
                                onDragEnd = { if (activeQueueDragId == queueTrack.id) finishQueueDrag() },
                                isDragging = isDragging,
                                dragOffsetY = if (isDragging) queueDragOffsetY else 0f,
                                virtualDisplacementY = run {
                                    val dragId = activeQueueDragId ?: return@run 0f
                                    if (isDragging) return@run 0f
                                    val dragStartIndex = orderedQueueItems.indexOfFirst { it.id == dragId }
                                    if (dragStartIndex < 0 || queueDragTargetIndex < 0 || dragStartIndex == queueDragTargetIndex) return@run 0f
                                    val itemHeightPx = with(queueDragDensity) { 60.dp.toPx() }
                                    when {
                                        dragStartIndex < queueDragTargetIndex -> {
                                            if (queueIndex in (dragStartIndex + 1)..queueDragTargetIndex) -itemHeightPx else 0f
                                        }
                                        dragStartIndex > queueDragTargetIndex -> {
                                            if (queueIndex in queueDragTargetIndex until dragStartIndex) itemHeightPx else 0f
                                        }
                                        else -> 0f
                                    }
                                },
                                isDropTarget = false
                            )
                        }
                    }
                }
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


@Composable
private fun SmokyAtmosphericCardBackground(
    themeColors: TrackThemeColors,
    modifier: Modifier = Modifier
) {
    // Ultra-slow, smooth ambient drift: 36 seconds cycle, continuous and calming
    // Strictly NO blinking or flashing up and down!
    val infiniteTransition = rememberInfiniteTransition(label = "smoke_drift")
    val smokePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "smoke_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Ashes gray dark base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E2028),
                    Color(0xFF1A1C23),
                    Color(0xFF16171E)
                ),
                startY = 0f,
                endY = h
            )
        )

        // The dictated track color (crimson / burgundy wine / dominant hue)
        val dictatedColor = themeColors.dominant

        // 2. Slow drifting color pool on left/center: Dictated color softly diffusing with ashes gray
        val x1 = w * (0.30f + 0.12f * sin(smokePhase))
        val y1 = h * (0.38f + 0.10f * cos(smokePhase * 0.75f))
        val radius1 = w * 0.90f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    dictatedColor.copy(alpha = 0.54f),
                    Color(0xFF282A34).copy(alpha = 0.32f),
                    Color.Transparent
                ),
                center = Offset(x1, y1),
                radius = radius1
            ),
            center = Offset(x1, y1),
            radius = radius1
        )

        // 3. Second slow drifting cloud: Ashes gray Slate + subtle secondary tone
        val x2 = w * (0.68f + 0.10f * cos(smokePhase * 0.85f))
        val y2 = h * (0.46f + 0.12f * sin(smokePhase * 0.70f))
        val radius2 = w * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2E313E).copy(alpha = 0.48f),
                    themeColors.secondary.copy(alpha = 0.26f),
                    Color.Transparent
                ),
                center = Offset(x2, y2),
                radius = radius2
            ),
            center = Offset(x2, y2),
            radius = radius2
        )

        // 4. Third slow drifting pool: Dictated color bloom on lower-left / center
        val x3 = w * (0.42f + 0.09f * sin(smokePhase * 0.60f + 1.2f))
        val y3 = h * (0.64f + 0.08f * cos(smokePhase * 0.55f))
        val radius3 = w * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    themeColors.atmosphericBloom.copy(alpha = 0.42f),
                    Color(0xFF20222B).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(x3, y3),
                radius = radius3
            ),
            center = Offset(x3, y3),
            radius = radius3
        )

        // 5. Soft contrast shading at top and bottom to ensure text and waveform clarity
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF14151C).copy(alpha = 0.45f),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFF121319).copy(alpha = 0.65f)
                ),
                startY = 0f,
                endY = h
            )
        )
    }
}

@Composable
private fun NowPlayingProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayFraction = if (isDragging) dragFraction else progressFraction

    Column(
        modifier = modifier
    ) {
        // Draggable Progress Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L && size.width > 0f) {
                            val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekTo((newFraction * durationMs).toLong())
                        }
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
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val totalWidth = size.width
                val centerY = size.height / 2f
                val lineThickness = 2.5.dp.toPx()
                val progressWidth = (totalWidth * displayFraction).coerceIn(0f, totalWidth)

                // Unplayed track - subtle translucent ash white
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(0f, centerY - lineThickness / 2f),
                    size = Size(totalWidth, lineThickness),
                    cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
                )

                // Played track - clean solid milk-white
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(0f, centerY - lineThickness / 2f),
                        size = Size(progressWidth, lineThickness),
                        cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Timestamps Row directly underneath the line (1:34 on left, 3:45 on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(if (isDragging) (dragFraction * durationMs).toLong() else positionMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = formatMs(durationMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun DarkSilhouetteWaveform(
    isPlaying: Boolean,
    telemetry: AudioTelemetry,
    modifier: Modifier = Modifier
) {
    // 56 dense vertical bars forming the dark wave silhouette at the bottom of the card
    val barCount = 56
    val restingProfile = remember(barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            // Multi-harmonic natural undulating wave silhouette
            val wave1 = abs(sin(norm * 3.14159f * 1.6f + 0.30f)) * 0.35f
            val wave2 = abs(sin(norm * 3.14159f * 3.6f)) * 0.20f
            val wave3 = abs(cos(norm * 3.14159f * 6.5f)) * 0.12f
            (0.18f + wave1 + wave2 + wave3).coerceIn(0.15f, 0.70f)
        }
    }
    val liveAmplitudes = remember(barCount) {
        FloatArray(barCount) { i -> restingProfile[i] }
    }

    val frameTicker = remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                withFrameNanos { timeNanos ->
                    frameTicker.longValue = timeNanos
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        @Suppress("UNUSED_VARIABLE")
        val ticker = frameTicker.longValue
        val rawFft = telemetry.fftBars
        val subBassEnergy = if (rawFft.size >= 4) {
            (rawFft[0] + rawFft[1] + rawFft[2] + rawFft[3]) / 4f
        } else 0f

        val targetHeights = calculateCompressedWaveform(rawFft, barCount, subBassEnergy)

        val totalWidth = size.width
        val barWidth = 2.6.dp.toPx()
        val barGap = ((totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)).coerceAtLeast(1f)

        val minBarHeight = 4.dp.toPx()
        val maxBarHeight = size.height * 0.95f
        val usableRange = (maxBarHeight - minBarHeight).coerceAtLeast(0f)

        val attackRate = 0.45f
        val decayRate = 0.16f

        for (i in 0 until barCount) {
            if (isPlaying) {
                val target = targetHeights[i].coerceIn(0f, 1f)
                val current = liveAmplitudes[i]
                val updated = if (target > current) {
                    current + (target - current) * attackRate
                } else {
                    current - (current - target) * decayRate
                }
                liveAmplitudes[i] = updated.coerceIn(0f, 1f)
            }

            val clampedFraction = liveAmplitudes[i].coerceIn(0f, 1f)
            val barHeight = (minBarHeight + usableRange * clampedFraction).coerceIn(minBarHeight, maxBarHeight)
            val barTop = size.height - barHeight
            val barX = i * (barWidth + barGap)

            // Dark charcoal silhouette tone with restrained contrast edge nestled at bottom of card
            val tipColor = Color(0xFF2B2D38)
            val baseColor = Color(0xFF14151B)

            val barBrush = Brush.verticalGradient(
                colors = listOf(tipColor, baseColor),
                startY = barTop,
                endY = size.height
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(barX, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun UpNextTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragBy: (Float) -> Unit,
    onDragEnd: () -> Unit,
    isDragging: Boolean,
    dragOffsetY: Float,
    virtualDisplacementY: Float,
    isDropTarget: Boolean = false,
    modifier: Modifier = Modifier
) {
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    var thresholdLatched by remember(track.id) { mutableStateOf(false) }
    val swipeSettle = remember(track.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = if (rowWidthPx > 0f) rowWidthPx * 0.38f else with(density) { 140.dp.toPx() }
    val swipeLimitPx = with(density) { 600.dp.toPx() }

    val isSwiping = abs(swipeOffset) > 1f
    val isPastThreshold = abs(swipeOffset) >= thresholdPx

    // Trigger one short haptic vibration exactly at the instant threshold is crossed into action mode
    LaunchedEffect(isPastThreshold) {
        if (isPastThreshold && !thresholdLatched) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            thresholdLatched = true
        } else if (!isPastThreshold) {
            thresholdLatched = false
        }
    }

    // Dynamic background transition during swipe:
    // Initial drag state: Standard dark/black (#141418).
    // Threshold trigger state: Transitions to vibrant green (#22C55E) for Play Next and vibrant red (#EF4444) for Delete.
    val targetActionColor = when {
        !isPastThreshold -> Color(0xFF141418)
        swipeOffset > 0f -> Color(0xFF22C55E)
        else -> Color(0xFFEF4444)
    }
    val animatedActionBg by animateColorAsState(
        targetValue = targetActionColor,
        animationSpec = tween(160),
        label = "swipe_action_bg"
    )

    // Icon scale and alpha animation as drag reaches action threshold
    val progress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)
    val iconScale by animateFloatAsState(
        targetValue = if (isPastThreshold) 1.2f else (0.65f + progress * 0.35f),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "swipe_icon_scale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isPastThreshold) 1f else (0.35f + progress * 0.65f),
        animationSpec = tween(120),
        label = "swipe_icon_alpha"
    )

    val displacement by animateFloatAsState(
        targetValue = virtualDisplacementY,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
        ),
        label = "queue_virtual_displacement"
    )
    val scale by animateFloatAsState(if (isDragging) 1.01f else 1f, tween(120), label = "queue_drag_scale")
    val elevation by animateFloatAsState(if (isDragging) 4f else 0f, tween(120), label = "queue_drag_elevation")

    // Active Item Highlight:
    // Solid, fully opaque surface color (#121214) prevents any background bleeding through the card.
    // Dragged item has a subtle flat dark surface lift (#24242A), matching YouTube Music aesthetic.
    val activeRowBg = when {
        isDragging -> Color(0xFF24242A)
        isCurrent -> accentColor.copy(alpha = 0.16f).compositeOver(Color(0xFF121214))
        else -> Color(0xFF121214)
    }

    // Full-Bleed Surface Layer: Spans 100% width, no border, subtle flat elevation
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else displacement
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 5f else 0f)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.45f),
                clip = false
            )
    ) {
        // Step 1: Background Reveal Layer - strictly sits behind moving card, revealed only as card is swiped open
        if (isSwiping && !isCurrent) {
            val showingPlayNext = swipeOffset > 0f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedActionBg),
                contentAlignment = if (showingPlayNext) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = iconAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showingPlayNext) Icons.Default.QueueMusic else Icons.Default.Delete,
                        contentDescription = if (showingPlayNext) "Play Next" else "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Step 2: Track Row Content - Solid opaque surface
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .background(activeRowBg)
                .pointerInput(track.id, isDragging) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            thresholdLatched = false
                        },
                        onDragCancel = {
                            scope.launch {
                                swipeSettle.snapTo(swipeOffset)
                                swipeSettle.animateTo(0f, tween(180)) { swipeOffset = value }
                                thresholdLatched = false
                            }
                        },
                        onDragEnd = {
                            val releaseOffset = swipeOffset
                            val crossed = abs(releaseOffset) >= thresholdPx
                            if (!crossed) {
                                scope.launch {
                                    swipeSettle.snapTo(releaseOffset)
                                    swipeSettle.animateTo(0f, androidx.compose.animation.core.spring(
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                                    )) { swipeOffset = value }
                                    thresholdLatched = false
                                }
                            } else if (releaseOffset > 0f) {
                                scope.launch {
                                    swipeSettle.snapTo(releaseOffset)
                                    swipeSettle.animateTo(swipeLimitPx, tween(190, easing = FastOutSlowInEasing)) { swipeOffset = value }
                                    onPlayNext()
                                    swipeSettle.snapTo(0f)
                                    swipeOffset = 0f
                                    thresholdLatched = false
                                }
                            } else {
                                scope.launch {
                                    swipeSettle.snapTo(releaseOffset)
                                    swipeSettle.animateTo(-swipeLimitPx, tween(190, easing = FastOutSlowInEasing)) { swipeOffset = value }
                                    onDelete()
                                    swipeSettle.snapTo(0f)
                                    swipeOffset = 0f
                                    thresholdLatched = false
                                }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            if (!isDragging && !isCurrent) {
                                val next = (swipeOffset + amount).coerceIn(-swipeLimitPx, swipeLimitPx)
                                swipeOffset = next
                            }
                        }
                    )
                }
                .pointerInput(track.id, isDragging) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            swipeOffset = 0f
                            onDragStart()
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            onDragBy(amount.y)
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art with rounded corners
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    thumbnailSizePx = 128,
                    crossfade = false
                )
                if (isCurrent) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPlayingBars(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            isPlaying = isPlaying
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Subtitle with Live Waveform Indicator for active track
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 15.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isCurrent) accentColor else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrent) {
                        LiveWaveformIndicator(
                            isPlaying = isPlaying,
                            color = accentColor,
                            modifier = Modifier.size(width = 16.dp, height = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${track.artist} • ${track.formattedDuration}",
                    fontSize = 13.sp,
                    color = if (isCurrent) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Drag Handle: 3 clean lines
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(track.id) {
                        detectDragGestures(
                            onDragStart = { swipeOffset = 0f; onDragStart() },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, amount -> change.consume(); onDragBy(amount.y) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isDragging) Color.White else Color.White.copy(alpha = 0.65f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * YouTube Music Up Next Track Item:
 * - Spans edge-to-edge across screen with fillMaxWidth().
 * - Slightly brighter extraction tint if playing (dominantColor.copy(alpha = 0.18f)).
 * - Padding inside item content reaching almost edge to edge (horizontal = 8.dp, vertical = 6.dp).
 * - Music picture with sharp edges (2.dp) instead of full rounded corner.
 * - 3-line drag handle reaching near edge.
 */
@Composable
fun UpNextTrackItem(
    track: Track,
    isPlaying: Boolean,
    dominantColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val activeRowBg = if (isPlaying) {
        dominantColor.copy(alpha = 0.18f).compositeOver(Color(0xFF101215))
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth() // Spans edge-to-edge across screen
            .background(activeRowBg)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), // Reaching almost edge to edge
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Artwork with sharp edges (2.dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            TrackArtworkImage(
                track = track,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                thumbnailSizePx = 128,
                crossfade = false
            )
            if (isPlaying) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)),
                    contentAlignment = Alignment.Center
                ) { AnimatedPlayingBars(Color.White, Modifier.size(24.dp)) }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title Column with clean spacing
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title.substringBefore(" - "),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.artist} • ${track.formattedDuration}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Drag Handle Icon: 3 lines reaching near edge
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

private fun interpolateColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Composable
private fun LiveWaveformIndicator(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "live_waveform")
    val a by infinite.animateFloat(0.30f, 1.0f, androidx.compose.animation.core.infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "w1")
    val b by infinite.animateFloat(0.85f, 0.25f, androidx.compose.animation.core.infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "w2")
    val c by infinite.animateFloat(0.40f, 0.95f, androidx.compose.animation.core.infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "w3")
    val d by infinite.animateFloat(0.75f, 0.35f, androidx.compose.animation.core.infiniteRepeatable(tween(440, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "w4")

    val h1 = if (isPlaying) a else 0.40f
    val h2 = if (isPlaying) b else 0.75f
    val h3 = if (isPlaying) c else 0.50f
    val h4 = if (isPlaying) d else 0.35f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(Modifier.width(2.5.dp).height((13f * h1).dp).clip(RoundedCornerShape(1.dp)).background(color))
        Box(Modifier.width(2.5.dp).height((13f * h2).dp).clip(RoundedCornerShape(1.dp)).background(color))
        Box(Modifier.width(2.5.dp).height((13f * h3).dp).clip(RoundedCornerShape(1.dp)).background(color))
        Box(Modifier.width(2.5.dp).height((13f * h4).dp).clip(RoundedCornerShape(1.dp)).background(color))
    }
}

@Composable
private fun AnimatedPlayingBars(color: Color, modifier: Modifier = Modifier, isPlaying: Boolean = true) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "queue_playing")
    val a by infinite.animateFloat(.30f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "bar1")
    val b by infinite.animateFloat(.75f, .25f, androidx.compose.animation.core.infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "bar2")
    val c by infinite.animateFloat(.45f, .95f, androidx.compose.animation.core.infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "bar3")

    val h1 = if (isPlaying) a else 0.40f
    val h2 = if (isPlaying) b else 0.75f
    val h3 = if (isPlaying) c else 0.50f

    Row(modifier, Arrangement.spacedBy(2.dp), Alignment.Bottom) {
        Box(Modifier.width(3.dp).height((18f * h1).dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.width(3.dp).height((18f * h2).dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.width(3.dp).height((18f * h3).dp).clip(RoundedCornerShape(2.dp)).background(color))
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
    waveformAlpha: Float = 1f,
    timestampAlpha: Float = 1f,
    progressAlpha: Float = 1f,
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
                .graphicsLayer { alpha = waveformAlpha.coerceIn(0f, 1f) }
        ) {
            val totalWidth = size.width
            val barWidth = 1.3.dp.toPx()
            val totalBarWidth = barWidth * barCount
            val barGap = if (barCount > 1) (totalWidth - totalBarWidth) / (barCount - 1) else 0f
            val maxBarHeight = size.height
            val minBarHeight = 2.5.dp.toPx()

            val liveEnergy = if (isPlaying) telemetry.rmsLevel else 0.32f
            val liveTransient = if (isPlaying) telemetry.transientSpike else 0f

            val fft = telemetry.fftBars
            val peakBass = if (fft.isNotEmpty()) {
                var maxB = 0.08f
                for (b in 0 until minOf(8, fft.size)) {
                    if (fft[b] > maxB) maxB = fft[b]
                }
                maxB
            } else if (telemetry.kickDetected) 0.85f else liveEnergy

            val subBassEnergy = if (telemetry.kickDetected) {
                maxOf(peakBass * 1.25f, 0.85f)
            } else {
                peakBass
            }

            val effectiveFft = if (fft.isNotEmpty()) {
                fft
            } else {
                FloatArray(barCount) { idx ->
                    val n = idx.toFloat() / barCount
                    val wave = abs(sin(n * 3.14159f * 2.5f)).toFloat()
                    (0.18f + 0.45f * liveEnergy * wave).coerceIn(0.06f, 0.75f)
                }
            }

            val compressedTargets = calculateCompressedWaveform(
                rawFft = effectiveFft,
                barCount = barCount,
                subBassEnergy = subBassEnergy
            )

            for (i in 0 until barCount) {
                val barX = i * (barWidth + barGap)
                val targetFraction = if (isPlaying) compressedTargets[i] else 0.20f
                val barHeight = (minBarHeight + (maxBarHeight - minBarHeight) * targetFraction).coerceIn(minBarHeight, maxBarHeight)
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
                .graphicsLayer { alpha = progressAlpha.coerceIn(0f, 1f) }
                .zIndex(2f)
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
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = timestampAlpha.coerceIn(0f, 1f) },
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
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    val bands = 32
    val liveBandHeights = remember(bands) {
        FloatArray(bands) { 0.12f }
    }

    // Capture and smooth real FFT frequency data
    val fft = telemetry.fftBars
    val hasFft = fft.isNotEmpty()

    if (isPlaying) {
        val peakBass = if (hasFft) {
            var maxB = 0.10f
            for (i in 0 until minOf(6, fft.size)) {
                if (fft[i] > maxB) maxB = fft[i]
            }
            maxB
        } else {
            telemetry.rmsLevel
        }

        val subBassEnergy = if (telemetry.kickDetected) {
            maxOf(peakBass * 1.25f, 0.85f)
        } else if (hasFft) {
            peakBass
        } else {
            telemetry.rmsLevel
        }

        val effectiveFft = if (hasFft) {
            fft
        } else {
            FloatArray(bands) { b ->
                val norm = b.toFloat() / (bands - 1).coerceAtLeast(1)
                val wave = kotlin.math.abs(kotlin.math.sin(norm * 3.14f * 2.5f + (telemetry.rmsLevel * 4f))).toFloat()
                (0.18f + 0.50f * telemetry.rmsLevel * wave).coerceIn(0.06f, 0.75f)
            }
        }

        val targetHeights = calculateCompressedWaveform(
            rawFft = effectiveFft,
            barCount = bands,
            subBassEnergy = subBassEnergy
        )

        for (b in 0 until bands) {
            val target = targetHeights[b]
            val current = liveBandHeights[b]
            // Instant Beat Snap (100%) & Fast Snappy Falloff (0.32)
            val updated = if (target > current) {
                target // Instant attack on beat
            } else {
                current - (current - target) * 0.32f // Fast gravitational drop
            }
            liveBandHeights[b] = updated.coerceIn(0.06f, 1.0f)
        }
    }
    // When isPlaying == false: FREEZE ENTIRELY! Maintain current liveBandHeights values.

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
                text = "${track.title.substringBefore(" - ")} • ${if (isPlaying) "Active" else "Frozen (Paused)"}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            if (!hasAudioPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Button(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Audio Permission for Live FFT")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 32-band live FFT spectrum canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val spacing = 3.dp.toPx()
                val totalSpacing = spacing * (bands - 1)
                val barWidth = (canvasWidth - totalSpacing) / bands

                for (b in 0 until bands) {
                    val hFraction = liveBandHeights[b]
                    val barHeight = (canvasHeight * hFraction).coerceIn(4.dp.toPx(), canvasHeight)
                    val x = b * (barWidth + spacing)
                    val y = canvasHeight - barHeight

                    // Frequency gradient coloring:
                    // Low frequencies (bass) on the left, high frequencies (treble) on the right
                    val norm = b.toFloat() / (bands - 1).coerceAtLeast(1)
                    val barColor = if (norm < 0.33f) {
                        accentColor
                    } else if (norm < 0.66f) {
                        Color(0xFFBB86FC)
                    } else {
                        Color(0xFF03DAC6)
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.90f),
                                barColor,
                                barColor.copy(alpha = 0.45f)
                            ),
                            startY = y,
                            endY = canvasHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Frequency spectrum labels: 5 Bass Focal Centers with Interleaved Spectrum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "◀ 10% • 30% Bass",
                    fontSize = 10.sp,
                    color = Color(0xFF03DAC6).copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "▲ 50% Center Ripple ▲",
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "70% • 90% Bass ▶",
                    fontSize = 10.sp,
                    color = Color(0xFF03DAC6).copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dynamic Interleaved Spectrum • 5 Water Wave Centers • Zero-Delay Physics",
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
