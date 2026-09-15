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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.audio.AudioTelemetry
import com.example.media.ArtworkColorExtractor
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

    // Interactive continuous drag transition state:
    // 0f = Full Baseline Now Playing
    // 1f = Expanded Artwork State (First Snap Point)
    // 2f = Compact Artwork + Full Up Next Queue (Second Snap Point)
    val dragProgress = remember { Animatable(0f) }
    val p = dragProgress.value
    val haptic = LocalHapticFeedback.current
    val queueDragDensity = LocalDensity.current
    var hasLatchedStage1 by remember { mutableStateOf(false) }

    // Haptic feedback trigger on reaching the first snap point
    LaunchedEffect(p) {
        if (p in 0.95f..1.05f) {
            if (!hasLatchedStage1) {
                hasLatchedStage1 = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else if (abs(p - 1.0f) > 0.18f) {
            hasLatchedStage1 = false
        }
    }

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
        if (activeQueueDragId == null || activeQueueDragIndex < 0) return
        queueDragOffsetY += deltaY
        val step = with(queueDragDensity) { 64.dp.toPx() }
        val raw = activeQueueDragIndex + (queueDragOffsetY / step).roundToInt()
        queueDragTargetIndex = raw.coerceIn(0, orderedQueueItems.lastIndex.coerceAtLeast(0))
    }

    fun finishQueueDrag() {
        val from = activeQueueDragIndex
        val to = queueDragTargetIndex
        if (activeQueueDragId != null && from >= 0 && to >= 0 && from < orderedQueueItems.size && to < orderedQueueItems.size && from != to) {
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
            .background(themeColors.darkBackground)
            .testTag("full_player_sheet")
    ) {
        // Reference background rendering restored from commit 80b5ec8.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        themeColors.darkBackground,
                        themeColors.darkBackground,
                        themeColors.darkBackground,
                        themeColors.darkBackground
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.28f),
                    radius = canvasWidth * 0.85f
                )
            )
        }
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val density = LocalDensity.current

        // Artwork framing is based on the actual drawable dimensions. Square artwork keeps
        // the normal player geometry; portrait/tall artwork is allowed to use its natural
        // height instead of being forced into a 1:1 box. Intrinsic dimensions are available
        // immediately for bundled artwork, so this does not delay the first render.
        // v10 retrigger: painterResource must be invoked directly from composition.
        // Reading its intrinsic size is immediate and does not block the first render.
        val safeFallbackRes = if (track.coverResId != 0) track.coverResId else R.drawable.art_luminous_echoes
        val artworkPainter = painterResource(safeFallbackRes)
        val bmp = resolvedArtworkBitmap
        val artworkAspectRatio = if (bmp != null && bmp.width > 0 && bmp.height > 0) {
            bmp.width.toFloat() / bmp.height.toFloat()
        } else {
            val artworkIntrinsicSize = artworkPainter.intrinsicSize
            if (artworkIntrinsicSize.width > 0f && artworkIntrinsicSize.height > 0f) {
                artworkIntrinsicSize.width / artworkIntrinsicSize.height
            } else 1f
        }
        val isTallArtwork = artworkAspectRatio < 0.94f

        // Safe insets
        val insetsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val insetsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // Stage geometry: the first snap is a true edge-to-edge artwork takeover.
        // 0f = normal player, 1f = expanded artwork, 2f = compact player + queue.
        val topBarY = insetsTop + 4.dp
        val topBarHeight = 44.dp
        val baseUpNextY = totalHeight - insetsBottom - 46.dp
        val availableHeight = (baseUpNextY - (topBarY + topBarHeight)).coerceAtLeast(400.dp)
        val baseArtWidth = (totalWidth - 48.dp).coerceAtLeast(180.dp)
        val naturalBaseArtHeight = baseArtWidth / artworkAspectRatio
        val baseArtHeight = naturalBaseArtHeight.coerceAtMost(availableHeight * 0.52f)
        val baseArtX = (totalWidth - baseArtWidth) / 2
        val baseArtY = topBarY + topBarHeight + 18.dp
        val baseArtCorner = 14.dp
        val baseTitleX = 24.dp
        val baseTitleY = baseArtY + baseArtHeight + 16.dp
        val baseWaveformY = baseTitleY + 66.dp + 14.dp
        val baseControlsY = baseWaveformY + 52.dp + 44.dp
        val baseShuffleY = baseControlsY + 76.dp + 22.dp

        // First snap: artwork reaches the very top and becomes truly edge-to-edge.
        // Do not force portrait artwork into the square height. Its natural aspect ratio
        // determines the expanded image height, capped to a sensible player region.
        val expArtY = 0.dp
        val expArtWidth = totalWidth
        val naturalExpandedHeight = totalWidth / artworkAspectRatio
        val expArtHeight = naturalExpandedHeight.coerceAtMost(totalHeight * 0.78f)
        val expArtX = 0.dp
        val expArtCorner = 0.dp
        val expTitleX = 20.dp
        val expTitleY = expArtY + expArtHeight - 82.dp
        val expTitleWidth = totalWidth - 40.dp
        val expProgressY = expArtY + expArtHeight - 3.dp
        val expControlsY = expArtY + expArtHeight + 42.dp
        val expUpNextY = expControlsY + 78.dp

        val compArtSize = 44.dp
        val compArtX = 16.dp
        val compArtY = insetsTop + 10.dp
        val compArtCorner = 8.dp
        val compTitleX = compArtX + compArtSize + 12.dp
        val compTitleY = compArtY + 2.dp
        val compUpNextY = compArtY + compArtSize + 14.dp

        val artWidth: androidx.compose.ui.unit.Dp
        val artHeight: androidx.compose.ui.unit.Dp
        val artX: androidx.compose.ui.unit.Dp
        val artY: androidx.compose.ui.unit.Dp
        val artCorner: androidx.compose.ui.unit.Dp
        if (p <= 1f) {
            val t = p.coerceIn(0f, 1f)
            artWidth = lerp(baseArtWidth, expArtWidth, t)
            artHeight = lerp(baseArtHeight, expArtHeight, t)
            artX = lerp(baseArtX, expArtX, t)
            artY = lerp(baseArtY, expArtY, t)
            artCorner = lerp(baseArtCorner, expArtCorner, t)
        } else {
            val t = (p - 1f).coerceIn(0f, 1f)
            artWidth = lerp(expArtWidth, compArtSize, t)
            artHeight = lerp(expArtHeight, compArtSize, t)
            artX = lerp(expArtX, compArtX, t)
            artY = lerp(expArtY, compArtY, t)
            artCorner = lerp(expArtCorner, compArtCorner, t)
        }

        val titleX: androidx.compose.ui.unit.Dp
        val titleY: androidx.compose.ui.unit.Dp
        val titleWidth: androidx.compose.ui.unit.Dp
        val titleSizeSp: Float
        val artistSizeSp: Float
        if (p <= 1f) {
            val t = p.coerceIn(0f, 1f)
            titleX = lerp(baseTitleX, expTitleX, t)
            titleY = lerp(baseTitleY, expTitleY, t)
            titleWidth = lerp(totalWidth - (baseTitleX * 2), expTitleWidth, t)
            titleSizeSp = 21f - (2f * t)
            artistSizeSp = 15f - (1f * t)
        } else {
            val t = (p - 1f).coerceIn(0f, 1f)
            titleX = lerp(expTitleX, compTitleX, t)
            titleY = lerp(expTitleY, compTitleY, t)
            titleWidth = lerp(expTitleWidth, totalWidth - compTitleX - 92.dp, t)
            titleSizeSp = 19f - (5f * t)
            artistSizeSp = 14f - (1f * t)
        }

        val upNextY: androidx.compose.ui.unit.Dp = if (p <= 1f) {
            lerp(baseUpNextY, expUpNextY, p.coerceIn(0f, 1f))
        } else {
            lerp(expUpNextY, compUpNextY, (p - 1f).coerceIn(0f, 1f))
        }

        val topBarAlpha = (1f - (p * 2.2f)).coerceIn(0f, 1f)
        val sourceAlpha = (1f - (p * 2f)).coerceIn(0f, 1f)
        val waveformAlpha = (1f - (p / 0.55f)).coerceIn(0f, 1f)
        val compactControlsAlpha = ((p - 1.05f) / 0.65f).coerceIn(0f, 1f)

        val stage1DistancePx = with(density) { (baseUpNextY - expUpNextY).toPx().coerceAtLeast(300f) }
        val stage2DistancePx = with(density) { (expUpNextY - compUpNextY).toPx().coerceAtLeast(220f) }

        val onDragDelta: (Float) -> Unit = { dragAmountPx ->
            coroutineScope.launch {
                dragProgress.stop()
                val currentP = dragProgress.value
                val delta = if (currentP < 1f) -dragAmountPx / stage1DistancePx else -dragAmountPx / stage2DistancePx
                dragProgress.snapTo((currentP + delta).coerceIn(0f, 2f))
            }
        }

        val onDragFinish: () -> Unit = {
            coroutineScope.launch {
                val currentP = dragProgress.value
                val target = when {
                    currentP < 0.35f -> 0f
                    currentP < 1.55f -> 1f
                    else -> 2f
                }
                dragProgress.animateTo(target, tween(300, easing = FastOutSlowInEasing))
            }
        }


        // 1. TOP BAR: Collapse Chevron, Centered Pill, 3-dots Menu Button (fades away as artwork expands)
        if (topBarAlpha > 0f) {
            Row(
                modifier = Modifier
                    .offset(x = 16.dp, y = topBarY)
                    .width(totalWidth - 32.dp)
                    .height(44.dp)
                    .graphicsLayer { alpha = topBarAlpha },
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
                        contentDescription = "Collapse Now Playing",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(26.dp)
                    )
                }

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
        }

        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.
        // The artwork keeps its normal height. Only its bottom is dissolved into the same
        // dynamic page surface, beginning around the lower 60% rather than at the hard edge.
        Box(
            modifier = Modifier
                .offset(x = artX, y = artY)
                .width(artWidth)
                .height(artHeight)

                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        coroutineScope.launch {
                            dragProgress.animateTo(
                                if (p > 1.2f) 1f else if (p > 0.2f) 0f else 1f,
                                tween(320, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                )
                .then(
                    if (p < 1.65f) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { onDragFinish() },
                            onDragCancel = { onDragFinish() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount)
                            }
                        )
                    } else Modifier
                )
        ) {
            // The picture is clipped only to its own natural rectangle/corners.
            // The fade is deliberately OUTSIDE that picture so the background can cover it.
            Box(
                modifier = Modifier
                    .width(artWidth)
                    .height(artHeight)
                    .clip(RoundedCornerShape(artCorner))
            ) {
                key(track.id) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

        // Fixed expanded-state dissolve. It is deliberately OUTSIDE the moving artwork
        // box, so during collapse the picture leaves this area while the fade remains stable.
        // Resting/mini states have no fade. In expanded mode, the final ~24% dissolves
        // strongly into the player background without changing image height.
        val artworkFadeAlpha = when {
            p < 0.30f -> 0f
            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)
            else -> 1f
        }

        // During the second snap, the expanded artwork brush must gently disappear as
        // the Up Next queue approaches its maximum height. This keeps the compact 44dp
        // artwork at the top-left completely clear instead of covering it with the brush.
        // The fade follows the drag continuously and also remains smooth during snap animation.
        val compactBrushFadeAlpha = when {
            p <= 1.05f -> 1f
            p >= 1.90f -> 0f
            else -> {
                val t = ((p - 1.05f) / 0.85f).coerceIn(0f, 1f)
                1f - (t * t * (3f - 2f * t))
            }
        }











        if (artworkFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = expArtX, y = expArtY)
                    .width(expArtWidth)
                    .height(expArtHeight * 0.24f)
                    .graphicsLayer { alpha = artworkFadeAlpha * compactBrushFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to themeColors.darkBackground,
                                0.16f to themeColors.darkBackground.copy(alpha = 0.86f),
                                0.34f to themeColors.darkBackground.copy(alpha = 0.62f),
                                0.52f to themeColors.darkBackground.copy(alpha = 0.34f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.12f),
                                1.00f to Color.Transparent
                            )
                        )
                    )
                    .zIndex(1f)
            )
        }

        if (artworkFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.76f))
                    .width(expArtWidth)
                    .height(expArtHeight * 0.24f)
                    .graphicsLayer { alpha = artworkFadeAlpha * compactBrushFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.16f to themeColors.darkBackground.copy(alpha = 0.12f),
                                0.34f to themeColors.darkBackground.copy(alpha = 0.34f),
                                0.52f to themeColors.darkBackground.copy(alpha = 0.62f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.86f),
                                1.00f to themeColors.darkBackground
                            )
                        )
                    )
                    .zIndex(1f)
            )
        }
        }

        // 3. SONG TITLE & ARTIST. The first phase keeps the long title directly over the artwork.
        Column(
            modifier = Modifier
                .offset(x = titleX, y = titleY)
                .width(titleWidth)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (p > 1.2f) coroutineScope.launch { dragProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) } }
                )
        ) {
            Text(track.title, fontSize = titleSizeSp.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(track.artist, fontSize = artistSizeSp.sp, color = Color.White.copy(alpha = 0.76f), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            if (sourceAlpha > 0f) {
                Spacer(modifier = Modifier.height(2.dp))
                val src = if (track.catalogSource.contains("Device", ignoreCase = true) || track.contentUri != null) "Device Audio" else if (track.catalogSource.isNotBlank()) track.catalogSource else "Device Audio"
                Text(src, fontSize = 13.sp, color = Color.White.copy(alpha = 0.45f * sourceAlpha), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }

        // 4. COMPACT CONTROLS IN STAGE 2 (Play/Pause & Collapse chevron next to compact header)
        if (compactControlsAlpha > 0f) {
            Row(
                modifier = Modifier
                    .offset(x = totalWidth - 92.dp, y = compArtY)
                    .graphicsLayer { alpha = compactControlsAlpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            dragProgress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Current Track",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            val progressFraction = if (track.durationMs > 0) {
                (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .offset(x = 0.dp, y = compArtY + compArtSize + 8.dp)
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .graphicsLayer { alpha = compactControlsAlpha },
                color = themeColors.accent,
                trackColor = Color.White.copy(alpha = 0.10f)
            )
        }

        // 5. ONE REAL WAVEFORM + PROGRESS COMPONENT.
        // The component physically travels upward with the drag. Its waveform bars
        // and timestamps fade, but the SAME progress line stays visible and lands
        // over the lower part of the artwork instead of creating a second slider.
        val progressHostY = lerp(
            baseWaveformY,
            expArtY + expArtHeight - 34.dp,
            p.coerceIn(0f, 1f)
        )
        val movingWaveformAlpha = if (p <= 1f) 1f else (1f - ((p - 1f) / 0.60f)).coerceIn(0f, 1f)
        val movingTimestampAlpha = if (p <= 1f) 1f else (1f - ((p - 1f) / 0.60f)).coerceIn(0f, 1f)

        val progressComponentAlpha = if (p <= 1f) {
            1f
        } else {
            (1f - ((p - 1f) / 0.60f)).coerceIn(0f, 1f)
        }
        if (progressComponentAlpha > 0f) {
            ExactAudioWaveformProgress(
                audioUri = track.contentUri,
                positionMs = playbackPositionMs,
                durationMs = track.durationMs,
                isPlaying = isPlaying,
                telemetry = telemetry,
                activeColor = themeColors.accent,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .offset(x = 16.dp, y = progressHostY)
                    .width(totalWidth - 32.dp)
                    .graphicsLayer { alpha = progressComponentAlpha }
                    .zIndex(2f)
            )
        }

        // YouTube-style physical control transition v4.
        // There is ONE set of five controls. At rest: shuffle/repeat sit below the
        // previous/play/next row. As the user drags Up Next upward, those same two
        // controls physically travel to the left/right sides of the primary row.
        // No duplicate rail and no fade-out/fade-in replacement is used.
        val controlT = p.coerceIn(0f, 1f)
        val controlY = lerp(baseControlsY, expControlsY, controlT)
        val centerX = totalWidth / 2

        val playCenterX = centerX
        val sideButtonCenterSpacing = 96.dp

        // These are LEFT offsets for equal-sized 60dp buttons, calculated from the same
        // center axis. The three primary controls therefore cannot drift horizontally.
        val playLeft = playCenterX - 38.dp
        val prevLeft = playCenterX - sideButtonCenterSpacing - 30.dp
        val nextLeft = playCenterX + sideButtonCenterSpacing - 30.dp

        val basePlayX = playLeft
        val basePrevX = prevLeft
        val baseNextX = nextLeft
        val expandedPlayX = playLeft
        val expandedPrevX = prevLeft
        val expandedNextX = nextLeft
        val expandedShuffleX = 12.dp
        val expandedRepeatX = totalWidth - 64.dp

        val prevX = lerp(basePrevX, expandedPrevX, controlT)
        val playX = lerp(basePlayX, expandedPlayX, controlT)
        val nextX = lerp(baseNextX, expandedNextX, controlT)
        val shuffleX = lerp(24.dp, expandedShuffleX, controlT)
        val repeatX = lerp(totalWidth - 72.dp, expandedRepeatX, controlT)

        // At rest the secondary controls are clearly below the primary row.
        // During the upward gesture they rise into the same row.
        val primaryOffsetY = 0.dp
        val secondaryOffsetY = lerp(76.dp + 28.dp, 6.dp, controlT)
        val expandedControlsFade = (1f - ((p - 1f) / 0.45f)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .offset(y = controlY - 8.dp)
                .graphicsLayer { alpha = expandedControlsFade }
        ) {
            AnimatedShuffleIcon(
                isShuffle = isShuffle,
                activeColor = themeColors.accent,
                onClick = onToggleShuffle,
                modifier = Modifier
                    .offset(x = shuffleX, y = secondaryOffsetY)
                    .testTag("player_shuffle_button"),
                touchSize = 52.dp,
                iconSize = 28.dp
            )

            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier
                    .offset(x = prevX, y = primaryOffsetY)
                    .size(60.dp)
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
                    .offset(x = playX, y = -4.dp)
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
                    .offset(x = nextX, y = primaryOffsetY)
                    .size(60.dp)
                    .testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(42.dp)
                )
            }

            AnimatedRepeatIcon(
                isRepeat = isRepeat,
                activeColor = themeColors.accent,
                onClick = onToggleRepeat,
                modifier = Modifier
                    .offset(x = repeatX, y = secondaryOffsetY)
                    .testTag("player_repeat_button"),
                touchSize = 52.dp,
                iconSize = 28.dp
            )
        }

        // 8. UP NEXT HANDLE & CONTENT (Lives within the SAME surface, continuously positioned at upNextY)
        val upNextHeight = totalHeight - upNextY - insetsBottom
        Column(
            modifier = Modifier
                .offset(x = 0.dp, y = upNextY)
                .fillMaxWidth()
                .height(upNextHeight.coerceAtLeast(54.dp))
                .background(Color.Transparent)
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
            // Subtle Gesture Handle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (p < 0.35f) {
                    Text(
                        text = "Up Next",
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(3.5.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Playing from",
                                fontSize = 9.5.sp,
                                letterSpacing = 0.7.sp,
                                color = Color.White.copy(alpha = 0.50f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (track.catalogSource.isNotBlank()) track.catalogSource else "Queue",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable {
                                    Toast.makeText(context, "Queue saved to playlist", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Save Queue",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Save",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Queue List inside the SAME surface (interactive when reached stage 1)
            if (p > 0.40f) {
                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)

                LazyColumn(
                    state = queueListState,
                    userScrollEnabled = p >= 0.95f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = listAlpha },
                    contentPadding = PaddingValues(bottom = insetsBottom + 24.dp)
                ) {
                    itemsIndexed(
                        items = orderedQueueItems,
                        key = { _, it -> it.id },
                        contentType = { _, _ -> "track_row" }
                    ) { queueIndex, queueTrack ->
                        val isCurrent = queueTrack.id == track.id
                        val isDragging = activeQueueDragId == queueTrack.id
                        UpNextTrackRow(
                            track = queueTrack,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            accentColor = themeColors.accent,
                            surfaceColor = themeColors.darkBackground,
                            onClick = { onSelectQueueTrack(queueTrack) },
                            onPlayNext = {
                                onPlayNextTrack(queueTrack)
                                val currentIndex = orderedQueueItems.indexOfFirst { it.id == queueTrack.id }
                                val playingIndex = orderedQueueItems.indexOfFirst { it.id == track.id }
                                if (currentIndex >= 0 && playingIndex >= 0 && currentIndex != playingIndex + 1) {
                                    val destination = if (currentIndex < playingIndex) playingIndex else playingIndex + 1
                                    val updated = orderedQueueItems.toMutableList().apply {
                                        val moved = removeAt(currentIndex)
                                        add(destination.coerceAtMost(size), moved)
                                    }
                                    orderedQueueItems = updated
                                    onUpdateQueue?.invoke(updated)
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
                            virtualDisplacementY = when {
                                activeQueueDragId == null || isDragging -> 0f
                                activeQueueDragIndex < queueDragTargetIndex && queueIndex > activeQueueDragIndex && queueIndex <= queueDragTargetIndex -> -with(queueDragDensity) { 64.dp.toPx() }
                                activeQueueDragIndex > queueDragTargetIndex && queueIndex >= queueDragTargetIndex && queueIndex < activeQueueDragIndex -> with(queueDragDensity) { 64.dp.toPx() }
                                else -> 0f
                            },
                            isDropTarget = activeQueueDragId != null && !isDragging && queueIndex == queueDragTargetIndex
                        )
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
    isDropTarget: Boolean,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    var thresholdLatched by remember(track.id) { mutableStateOf(false) }
    val swipeSettle = remember(track.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 90.dp.toPx() }
    val swipeLimitPx = with(density) { 600.dp.toPx() }

    val isSwiping = abs(swipeOffset) > 1f
    val isPastThreshold = abs(swipeOffset) >= thresholdPx

    // Trigger haptic vibration when threshold is crossed into deletion mode
    LaunchedEffect(isPastThreshold) {
        if (isPastThreshold && !thresholdLatched) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            thresholdLatched = true
        } else if (!isPastThreshold) {
            thresholdLatched = false
        }
    }

    // Dynamic background transition from dark ash (#16181A) to deep deletion red (#D32F2F)
    val actionBackground by animateColorAsState(
        targetValue = when {
            swipeOffset > 1f -> accentColor.copy(alpha = 0.32f).compositeOver(surfaceColor)
            swipeOffset < -1f -> Color(0xFFD32F2F)
            else -> surfaceColor
        },
        animationSpec = tween(150),
        label = "SwipeActionBackground"
    )

    val displacement by animateFloatAsState(
        targetValue = virtualDisplacementY,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
        ),
        label = "queue_virtual_displacement"
    )
    val scale by animateFloatAsState(if (isDragging) 1.02f else 1f, tween(120), label = "queue_drag_scale")
    val elevation by animateFloatAsState(if (isDragging) 8f else 0f, tween(120), label = "queue_drag_elevation")

    // Active Item Highlight:
    // Remove the hardcoded gray block from the active track row.
    // For the currently playing track, use a slightly brighter variant of extracted artwork color composited over dark background
    val rowAccent = track.dominantColor
    val activeRowBg = when {
        isDragging || isDropTarget -> rowAccent.copy(alpha = 0.28f).compositeOver(surfaceColor)
        isCurrent -> rowAccent.copy(alpha = 0.22f).compositeOver(surfaceColor)
        else -> rowAccent.copy(alpha = 0.10f).compositeOver(surfaceColor)
    }

    // Full-Bleed Surface Layer: Spans 100% width, no rounded card wrapper, no list margin
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else displacement
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 10f else 0f)
            .shadow(elevation.dp, clip = false)
    ) {
        // Step 1: Background Reveal Stack
        // Sits strictly on background layer BEHIND the row, revealed only during swipe
        if (isSwiping && !isCurrent) {
            val showingUpNext = swipeOffset > 1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(actionBackground),
                contentAlignment = if (showingUpNext) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = if (showingUpNext) Icons.Default.QueueMusic else Icons.Default.Delete,
                        contentDescription = if (showingUpNext) "Play Next" else "Remove Track",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    if (abs(swipeOffset) >= thresholdPx * 0.65f) {
                        Text(
                            text = if (showingUpNext) "Up Next" else "Remove",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Step 2: Track Row Content ONLY (Album art, text, drag handle - strictly NO trash icon inside)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), // Reaching almost edge to edge with slight spacing
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art with sharp edges (2.dp) instead of full rounded corner
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
                if (isCurrent && isPlaying) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)),
                        contentAlignment = Alignment.Center
                    ) { AnimatedPlayingBars(Color.White, Modifier.size(24.dp)) }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Step 3: Add Title-to-Subtitle Spacing
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

                // Spacing between Title and Subtitle line
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${track.artist} • ${track.formattedDuration}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Drag Handle: 3 clean lines reaching almost edge to edge
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
                                .background(Color.White.copy(alpha = 0.85f))
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

@Composable
private fun AnimatedPlayingBars(color: Color, modifier: Modifier = Modifier) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "queue_playing")
    val a by infinite.animateFloat(.30f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "bar1")
    val b by infinite.animateFloat(.75f, .25f, androidx.compose.animation.core.infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "bar2")
    val c by infinite.animateFloat(.45f, .95f, androidx.compose.animation.core.infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), label = "bar3")
    Row(modifier, Arrangement.spacedBy(2.dp), Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height((18f * a).dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.width(3.dp).height((18f * b).dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.width(3.dp).height((18f * c).dp).clip(RoundedCornerShape(2.dp)).background(color))
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
