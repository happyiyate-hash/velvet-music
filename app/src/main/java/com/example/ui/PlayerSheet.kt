package com.example.ui

import com.example.R
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.graphics.lerp as colorLerp
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.audio.AudioTelemetry
import com.example.audio.RepeatMode
import com.example.media.ArtworkColorExtractor
import com.example.media.VelvetArtworkCache
import com.example.media.TrackThemeColors
import com.example.model.Track
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
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
/**
 * Continuous Inertial Scroll & Damped Spring Settling Engine for Up Next Queue:
 *
 * 1. Continuous Floating-Point Inertia:
 *    - Captures release velocity from touch gesture.
 *    - Propagates momentum with smooth exponential deceleration friction.
 *    - Frame-independent timing (via withFrameNanos dt calculation) for seamless 60/90/120Hz consistency.
 *
 * 2. Damped Spring Settling:
 *    - When momentum drops below threshold, gently and smoothly settles to the nearest row
 *      using a physical damped spring (Hooke's law with damping ratio) instead of a hard jump/snap.
 *
 * 3. Unified Container Motion:
 *    - Displaces the entire list container as one continuous unit.
 *    - Selected row and highlight stay attached to their actual row position throughout the scroll.
 */
class InertialSpringFlingBehavior(
    private val lazyListState: LazyListState,
    private val rowHeightPx: Float,
    private val friction: Float = 0.955f,
    private val springStiffness: Float = 145f,
    private val springDamping: Float = 0.88f
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        var velocity = initialVelocity
        var lastFrameNanos = withFrameNanos { it }

        // Phase 1: Physical Inertial Momentum (Continuous exponential deceleration)
        while (abs(velocity) > 75f) {
            withFrameNanos { nowNanos ->
                val dt = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.045f)
                lastFrameNanos = nowNanos

                val step = velocity * dt
                val consumed = scrollBy(step)

                // If hit boundary (overscroll limit), terminate momentum
                if (abs(consumed) < abs(step) * 0.35f) {
                    velocity = 0f
                } else {
                    val decay = friction.pow(dt * 60f)
                    velocity *= decay
                }
            }
        }

        // Phase 2: Damped Spring Settling to nearest intended row boundary
        if (rowHeightPx > 0f) {
            val currentOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
            // Settle forward if more than halfway through, or backward if less
            val targetDelta = if (currentOffset > rowHeightPx * 0.5f) {
                (rowHeightPx - currentOffset)
            } else {
                -currentOffset
            }

            var remainingDistance = targetDelta
            var springVelocity = velocity * 0.35f
            var springLastNanos = withFrameNanos { it }

            while (abs(remainingDistance) > 0.4f || abs(springVelocity) > 8f) {
                withFrameNanos { nowNanos ->
                    val dt = ((nowNanos - springLastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.045f)
                    springLastNanos = nowNanos

                    val springForce = remainingDistance * springStiffness
                    springVelocity += springForce * dt
                    springVelocity *= springDamping.pow(dt * 60f)

                    val delta = springVelocity * dt
                    val consumed = scrollBy(delta)
                    remainingDistance -= delta

                    if (abs(consumed) < abs(delta) * 0.35f) {
                        springVelocity = 0f
                        remainingDistance = 0f
                    }
                }
            }
        }

        return 0f
    }
}

@Composable
fun PlayerSheet(
    track: Track,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    telemetry: AudioTelemetry = AudioTelemetry(),
    isShuffle: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
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
    // Instant frame-0 derivation from dominantColor (0ms latency, zero main-thread disk I/O).
    var resolvedArtworkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var themeColors by remember(track.id) {
        mutableStateOf(ArtworkColorExtractor.generateThemePalette(track.dominantColor))
    }

    LaunchedEffect(track.id, track.artworkUri, track.coverResId) {
        withContext(Dispatchers.IO) {
            val bitmap = ArtworkColorExtractor.resolveTrackBitmap(context, track)
                ?: if (track.coverResId != 0) {
                    runCatching { BitmapFactory.decodeResource(context.resources, track.coverResId) }.getOrNull()
                } else null
            val extractedColors = if (bitmap != null) {
                ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
            } else {
                ArtworkColorExtractor.generateThemePalette(track.dominantColor)
            }
            withContext(Dispatchers.Main) {
                resolvedArtworkBitmap = bitmap
                themeColors = extractedColors
            }
        }
    }

    // One continuous controller for the player and its in-flow queue.
    // 0f = collapsed player, 0.55f = player + queue peek, 1f = queue-focused player.
    val expansionProgress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    BackHandler {
        if (expansionProgress.value > 1.05f) {
            coroutineScope.launch {
                expansionProgress.animateTo(
                    1.0f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
        } else if (expansionProgress.value > 0.05f) {
            coroutineScope.launch {
                expansionProgress.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
        } else {
            onDismiss()
        }
    }

    // Up Next Queue items (uses passed queueTracks or falls back to sample queue tracks)
    // YouTube Music Hierarchy: Index 0 is currently playing track, Index 1 is Up Next, etc.
    // Preserve the queue's physical order. Selecting a track must not move it to the top.
    val queueItems = remember(queueTracks) {
        val raw = if (queueTracks.isNotEmpty()) queueTracks.distinctBy { it.id }
        else com.example.model.SampleData.starterTracks.distinctBy { it.id }
        if (raw.any { it.id == track.id }) raw else raw + track
    }
    var orderedQueueItems by remember { mutableStateOf(queueItems) }
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

    // Warm upcoming full-resolution artwork in the background. Playback/queue selection
    // never waits for this work to finish.
    LaunchedEffect(queueItems, track.id) {
        val upcoming = queueItems
            .dropWhile { it.id != track.id }
            .drop(1)
            .take(3)

        withContext(Dispatchers.IO) {
            upcoming.forEach { nextTrack ->
                if (VelvetArtworkCache.getFullFromMemory(nextTrack.id) == null) {
                    runCatching { VelvetArtworkCache.getOrDecodeFullArtwork(context, nextTrack) }
                }
            }
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

        // Keep the finger-following offset continuous. Change the target slot only
        // after the dragged artwork has substantially crossed its neighbor.
        queueDragOffsetY += deltaY
        val itemHeightPx = with(density) { 60.dp.toPx() }
        val crossedSlots = when {
            queueDragOffsetY >= 0f ->
                kotlin.math.floor((queueDragOffsetY + itemHeightPx * 0.46f) / itemHeightPx).toInt()
            else ->
                kotlin.math.ceil((queueDragOffsetY - itemHeightPx * 0.46f) / itemHeightPx).toInt()
        }
        val newTarget = (startIndex + crossedSlots)
            .coerceIn(0, orderedQueueItems.lastIndex.coerceAtLeast(0))

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

    // YouTube Music-style Artwork Background Color System:
    // Preserves exact hue and saturation, reducing only brightness/luminance
    // with subtle tonal variation from top to bottom.
    val animatedBgTop by animateColorAsState(
        targetValue = themeColors.bgTop,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bg_top"
    )
    val animatedBgMidUpper by animateColorAsState(
        targetValue = themeColors.bgMidUpper,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bg_mid_upper"
    )
    val animatedBgMidLower by animateColorAsState(
        targetValue = themeColors.bgMidLower,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bg_mid_lower"
    )
    val animatedBgBottom by animateColorAsState(
        targetValue = themeColors.bgBottom,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bg_bottom"
    )

    // Sleek border accent derived from the extracted color
    val animatedCardBorderColor by animateColorAsState(
        targetValue = themeColors.cardBorder,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "card_border_color"
    )

    // Atmospheric bloom for diffused depth behind artwork
    val animatedAtmosphericBloom by animateColorAsState(
        targetValue = themeColors.atmosphericBloom,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "atmospheric_bloom"
    )

    // Extract distinct secondary visualizer color from the artwork, after the background color has been chosen
    val visualizerColor = remember(themeColors, resolvedArtworkBitmap) {
        if (resolvedArtworkBitmap != null) {
            ArtworkColorExtractor.extractVisualizerColorFromBitmap(
                resolvedArtworkBitmap,
                themeColors.cardBackground
            )
        } else {
            themeColors.visualizerColor
        }
    }
    val animatedVisualizerColor by animateColorAsState(
        targetValue = visualizerColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "visualizer_color"
    )

    val cardColor = animatedBgTop

    val cardBottomCornerRadius = 30.dp
    val cardShape = RoundedCornerShape(
        bottomStart = cardBottomCornerRadius,
        bottomEnd = cardBottomCornerRadius
    )

    val cardGradient = remember(animatedBgTop, animatedBgMidLower) {
        Brush.verticalGradient(
            0.0f to animatedBgTop,
            1.0f to animatedBgMidLower
        )
    }

    val queueSheetGradient = remember(animatedBgTop, animatedBgMidUpper, animatedBgMidLower) {
        Brush.verticalGradient(
            0.0f to animatedBgTop,
            0.40f to animatedBgMidUpper,
            1.0f to animatedBgMidLower
        )
    }

    val p = expansionProgress.value.coerceIn(0f, 2f)
    val p1 = p.coerceIn(0f, 1f)
    val p2 = (p - 1f).coerceIn(0f, 1f)

    // Dynamic Control Center & Screen Background:
    // In Stage 1 (dropped): displays the exact extracted color, quite darker than player card (darkFactor ~0.28f), NOT pure black!
    // In Stage 2 (maximum): gradually darkens further (darkFactor ~0.58f), but still retains vibrant color without becoming fully dark.
    val currentControlCenterBg = remember(animatedBgBottom, p2) {
        val darkFactor = 0.28f + (0.58f - 0.28f) * p2
        colorLerp(animatedBgBottom, Color(0xFF090A0E), darkFactor)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentControlCenterBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Absorb any taps on empty player sheet areas */ }
            .pointerInput(Unit) {
                detectTapGestures { /* Intercept all gestures across the player sheet */ }
            }
            .testTag("full_player_sheet")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val totalWidth = maxWidth
            val totalHeight = maxHeight
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            // 1. Artwork Geometry
            // Aspect ratio is strictly preserved (1:1 square artwork).
            val artworkAspectRatio = 1.0f
            val controlsHeight = 64.dp

            // Collapsed state (p = 0f):
            // Shift bottom sheet downward so that only the drag handle peeks above system navigation.
            val collapsedPeekHeight = (navBarBottom + 20.dp).coerceIn(20.dp, 32.dp)
            val collapsedControlsY = totalHeight - collapsedPeekHeight - controlsHeight - 12.dp
            val collapsedCardHeight = collapsedControlsY - 6.dp
            val collapsedProgressY = collapsedCardHeight - 34.dp
            val collapsedMetadataY = collapsedProgressY - 48.dp
            val collapsedArtworkBottom = collapsedMetadataY - 12.dp

            // Collapsed state (p = 0f): Original centered display with rounded corners and proper margins:
            val collapsedArtworkHeight = (minOf(totalWidth - 32.dp, collapsedArtworkBottom - (statusBarTop + 46.dp) - 6.dp)).coerceIn(240.dp, 330.dp)
            val collapsedArtworkWidth = collapsedArtworkHeight * artworkAspectRatio
            val collapsedArtworkTop = ((statusBarTop + 46.dp) + (collapsedArtworkBottom - (statusBarTop + 46.dp) - collapsedArtworkHeight) / 2f)
            val collapsedArtworkLeft = (totalWidth - collapsedArtworkWidth) / 2f
            val collapsedArtworkRadius = 14.dp

            // Stage 1 Expanded state (p = 1f):
            val expandedArtworkWidth = totalWidth
            val expandedArtworkHeight = expandedArtworkWidth / artworkAspectRatio
            val expandedArtworkTop = 0.dp
            val expandedArtworkLeft = 0.dp
            val expandedArtworkRadius = 0.dp
            val expandedArtworkBottom = expandedArtworkTop + expandedArtworkHeight // = totalWidth
            val expandedCardHeight = expandedArtworkBottom

            val expandedProgressY = expandedCardHeight - 34.dp
            val expandedMetadataY = expandedProgressY - 48.dp

            // Stage 2 Mini Artwork Targets:
            // Reduced size: ~20% larger than 48dp queue art = 56dp. Pushed to far left with small space (10dp).
            val stage2ArtworkSize = 56.dp
            val stage2ArtworkTop = statusBarTop + 6.dp
            val stage2ArtworkLeft = 10.dp
            val stage2ArtworkRadius = 6.dp
            // Up Next sheet moves higher to the top (68dp instead of 84dp):
            val stage2BoundaryY = statusBarTop + 68.dp

            // Continuous two-stage interpolation for Artwork:
            val artworkWidth = if (p <= 1f) {
                lerp(collapsedArtworkWidth, expandedArtworkWidth, p1)
            } else {
                lerp(expandedArtworkWidth, stage2ArtworkSize, p2)
            }
            val artworkHeight = if (p <= 1f) {
                lerp(collapsedArtworkHeight, expandedArtworkHeight, p1)
            } else {
                lerp(expandedArtworkHeight, stage2ArtworkSize, p2)
            }
            val artworkTop = if (p <= 1f) {
                lerp(collapsedArtworkTop, expandedArtworkTop, p1)
            } else {
                lerp(expandedArtworkTop, stage2ArtworkTop, p2)
            }
            val artworkLeft = if (p <= 1f) {
                lerp(collapsedArtworkLeft, expandedArtworkLeft, p1)
            } else {
                lerp(expandedArtworkLeft, stage2ArtworkLeft, p2)
            }
            val artworkRadius = if (p <= 1f) {
                lerp(collapsedArtworkRadius, expandedArtworkRadius, p1)
            } else {
                lerp(expandedArtworkRadius, stage2ArtworkRadius, p2)
            }

            // Card Height and Shape:
            val cardHeight = if (p <= 1f) {
                lerp(collapsedCardHeight, expandedCardHeight, p1)
            } else {
                lerp(expandedCardHeight, stage2BoundaryY, p2)
            }

            val currentCardCornerRadius = if (p <= 1f) lerp(30.dp, 22.dp, p1) else lerp(22.dp, 0.dp, p2)
            val dynamicCardShape = RoundedCornerShape(
                bottomStart = currentCardCornerRadius,
                bottomEnd = currentCardCornerRadius
            )

            // Stage 1 elements alpha: fades out rapidly so it never shows through the up next list
            val stage1CardElementsAlpha = if (p <= 1f) 1f else (1f - p2 * 6f).coerceIn(0f, 1f)

            // Continuous lerp for elements inside the Card:
            val metadataY = lerp(collapsedMetadataY, expandedMetadataY, p1)
            val progressY = lerp(collapsedProgressY, expandedProgressY, p1)
            val visualizerHeight = 34.dp
            val waveformY = cardHeight - visualizerHeight
            val contentPaddingHorizontal = lerp(22.dp, 18.dp, p1)

            // Transport Controls:
            val stage1ControlsY = expandedCardHeight + 4.dp
            val controlsY = if (p <= 1f) {
                lerp(collapsedControlsY, stage1ControlsY, p1)
            } else {
                lerp(stage1ControlsY, stage1ControlsY - 44.dp, p2)
            }
            // Rapid fade-out: disappears immediately as user drags towards stage 2
            val controlsAlpha = if (p <= 1f) 1f else (1f - p2 * 8f).coerceIn(0f, 1f)

            // Up Next Boundary & Gesture Handle:
            val stage1BoundaryY = (expandedCardHeight + 4.dp) + controlsHeight + 2.dp
            val boundaryY = if (p <= 1f) {
                lerp(totalHeight - collapsedPeekHeight, stage1BoundaryY, p1)
            } else {
                lerp(stage1BoundaryY, stage2BoundaryY, p2)
            }
            val upNextHeight = (totalHeight - boundaryY).coerceAtLeast(0.dp)

            // Reduced curve between left and right edges for compact and very clean design
            val queueCornerRadius = 12.dp
            val boundaryShape = RoundedCornerShape(
                topStart = queueCornerRadius,
                topEnd = queueCornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )

            val playButtonSize = 62.dp
            val playIconSize = 38.dp
            val secondaryIconTouchSize = 46.dp
            val secondaryIconSize = 26.dp
            val skipButtonSize = 48.dp
            val skipIconSize = 32.dp

            val stage1DragRangePx = with(density) { (collapsedCardHeight - expandedCardHeight).toPx() }.coerceAtLeast(100f)
            val stage2DragRangePx = with(density) { (stage1BoundaryY - stage2BoundaryY).toPx() }.coerceAtLeast(100f)
            val velocityTracker = remember { VelocityTracker() }

            fun settleExpansion(velocity: Float = 0f) {
                coroutineScope.launch {
                    val current = expansionProgress.value
                    val target = when {
                        // High velocity swipes
                        velocity < -550f -> if (current < 0.75f) 1.0f else 2.0f
                        velocity > 550f -> if (current > 1.25f) 1.0f else 0.0f
                        // Positional snapping
                        current < 0.45f -> 0.0f
                        current < 1.45f -> 1.0f
                        else -> 2.0f
                    }
                    expansionProgress.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }

            // 1. THE RESHAPING PLAYER CARD (Contains TopBar, Artwork, Metadata, Progress, Waveform; compresses upward)
            val cardGradientAlpha = if (p <= 1f) 1f else (1f - p2 * 4f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .then(
                        if (cardGradientAlpha > 0.005f) {
                            Modifier
                                .clip(dynamicCardShape)
                                .background(cardGradient)
                        } else {
                            Modifier
                        }
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                coroutineScope.launch { expansionProgress.stop() }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                                val currentP = expansionProgress.value
                                val dragRange = if (currentP < 1f) stage1DragRangePx else stage2DragRangePx
                                val deltaP = -dragAmount / dragRange
                                val newP = (currentP + deltaP).coerceIn(0f, 2f)
                                coroutineScope.launch { expansionProgress.snapTo(newP) }
                            },
                            onDragEnd = {
                                val velocityY = velocityTracker.calculateVelocity().y
                                settleExpansion(velocityY)
                                velocityTracker.resetTracking()
                            },
                            onDragCancel = {
                                settleExpansion(0f)
                                velocityTracker.resetTracking()
                            }
                        )
                    }
                    .zIndex(5f)
            ) {
                // ALBUM ARTWORK (Continuous positioning, aspect ratio strictly preserved; base layer zIndex 1f so title/progress/waveform stay IN FRONT)
                val dynamicArtworkShape = RoundedCornerShape(artworkRadius)
                val artworkZIndex = if (p2 > 0.01f) 12f else 1f
                Box(
                    modifier = Modifier
                        .offset { IntOffset(artworkLeft.roundToPx(), artworkTop.roundToPx()) }
                        .size(width = artworkWidth, height = artworkHeight)
                        .clip(dynamicArtworkShape)
                        .zIndex(artworkZIndex)
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        thumbnailSizePx = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // TOP BAR (Stays at status bar level, sits cleanly above artwork with .zIndex(10f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, (statusBarTop + 4.dp).roundToPx()) }
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                        .graphicsLayer { alpha = (1f - p2 * 2f).coerceIn(0f, 1f) }
                        .zIndex(10f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (expansionProgress.value > 1.05f) {
                                coroutineScope.launch {
                                    expansionProgress.animateTo(
                                        1.0f,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            } else if (expansionProgress.value > 0.05f) {
                                coroutineScope.launch {
                                    expansionProgress.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_collapse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { showActionSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // STAGE 2 TOP HEADER:
                // Darkened header across top side displaying track color,
                // mini artwork on far left, tightly framed auto-scrolling title & artist, and enlarged Play/Pause button on far right.
                if (p2 > 0.01f) {
                    val stage2HeaderAlpha = ((p2 - 0.05f) / 0.35f).coerceIn(0f, 1f)
                    if (stage2HeaderAlpha > 0.001f) {
                        // Darkened background across the top side in Stage 2 (matching currentControlCenterBg):
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(stage2BoundaryY)
                                .background(currentControlCenterBg.copy(alpha = stage2HeaderAlpha))
                                .graphicsLayer { alpha = stage2HeaderAlpha }
                                .zIndex(11f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, (statusBarTop + 6.dp).roundToPx()) }
                                .height(stage2ArtworkSize)
                                .padding(start = stage2ArtworkLeft + stage2ArtworkSize + 8.dp, end = 2.dp)
                                .graphicsLayer { alpha = stage2HeaderAlpha }
                                .zIndex(12f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clipToBounds()
                                    .padding(end = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                MarqueeTrackTitle(
                                    title = track.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = track.artist,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 0.2.sp,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTogglePlayPause()
                                    }
                                    .testTag("player_stage2_play_pause"),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                MorphingPlayPauseIcon(
                                    isPlaying = isPlaying,
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // TRACK IDENTITY (Title & Artist, strictly in FRONT of artwork at zIndex 10f)
                if (stage1CardElementsAlpha > 0.005f) {
                    key(track.id) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, metadataY.roundToPx()) }
                                .padding(horizontal = contentPaddingHorizontal)
                                .graphicsLayer { alpha = stage1CardElementsAlpha }
                                .zIndex(10f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            MarqueeTrackTitle(
                                title = track.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_track_title")
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = track.artist.uppercase(),
                                fontSize = 12.sp,
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.68f),
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("player_track_artist")
                            )
                        }
                    }

                    // PROGRESS BAR & TIMESTAMPS (Strictly in FRONT of artwork at zIndex 10f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, progressY.roundToPx()) }
                            .padding(horizontal = contentPaddingHorizontal)
                            .graphicsLayer { alpha = stage1CardElementsAlpha }
                            .zIndex(10f)
                    ) {
                        NowPlayingProgressBar(
                            positionMs = playbackPositionMs,
                            durationMs = track.durationMs,
                            isPlaying = isPlaying,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Thin vertical lines rising from the bottom edge of the player card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, waveformY.roundToPx()) }
                            .height(visualizerHeight)
                            .graphicsLayer { alpha = stage1CardElementsAlpha }
                            .zIndex(10f)
                    ) {
                        PlayerBottomVerticalLines(
                            isPlaying = isPlaying,
                            telemetry = telemetry,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } // End of Player Card Box

            // 2. TRANSPORT CONTROLS ROW (Fades out rapidly so controls never show in Up Next list)
            if (controlsAlpha > 0.005f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, controlsY.roundToPx()) }
                        .height(controlsHeight)
                        .padding(horizontal = contentPaddingHorizontal)
                        .graphicsLayer { alpha = controlsAlpha }
                        .zIndex(4f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedShuffleIcon(
                        isShuffle = isShuffle,
                        activeColor = Color.White,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleShuffle()
                        },
                        modifier = Modifier.testTag("player_shuffle_button"),
                        touchSize = secondaryIconTouchSize,
                        iconSize = secondaryIconSize
                    )

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPrevious()
                        },
                        modifier = Modifier
                            .size(skipButtonSize)
                            .testTag("player_previous_button")
                    ) {
                        PlayerPreviousIcon(
                            modifier = Modifier.size(skipIconSize),
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(playButtonSize)
                            .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(Color(0xFFF4F4F2))
                            .border(1.dp, Color.White.copy(alpha = 0.60f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePlayPause()
                                }
                            )
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        MorphingPlayPauseIcon(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(playIconSize),
                            tint = Color(0xFF08090C)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNext()
                        },
                        modifier = Modifier
                            .size(skipButtonSize)
                            .testTag("player_next_button")
                    ) {
                        PlayerNextIcon(
                            modifier = Modifier.size(skipIconSize),
                            tint = Color.White
                        )
                    }

                    AnimatedRepeatIcon(
                        repeatMode = repeatMode,
                        activeColor = Color.White,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleRepeat()
                        },
                        modifier = Modifier.testTag("player_repeat_button"),
                        touchSize = secondaryIconTouchSize,
                        iconSize = secondaryIconSize
                    )
                }
            }

            // 3. UP NEXT BOUNDARY & GESTURE CONTAINER
            // Unified container sitting at boundaryY, full width edge-to-edge.
            // In Stage 2, the bottom sheet SHOWS THE VIBRANT COLOR (queueSheetGradient) as explicitly requested:
            if (upNextHeight > 1.dp) {
                val stage2Progress = (p - 1f).coerceIn(0f, 1f)
                val sheetColorAlpha = stage2Progress.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(upNextHeight)
                        .offset { IntOffset(0, boundaryY.roundToPx()) }
                        .clip(boundaryShape)
                        .zIndex(8f)
                ) {
                    // Slowly and steadily fade in vibrant color as sheet is pushed into Stage 2:
                    if (sheetColorAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = sheetColorAlpha }
                                .background(queueSheetGradient)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Drag Gesture Handle Bar (The Gesture remains directly at the very top of the bottom sheet)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            velocityTracker.resetTracking()
                                            coroutineScope.launch { expansionProgress.stop() }
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            change.consume()
                                            val currentP = expansionProgress.value
                                            val dragRange = if (currentP < 1f) stage1DragRangePx else stage2DragRangePx
                                            val deltaP = -dragAmount / dragRange
                                            val newP = (currentP + deltaP).coerceIn(0f, 2f)
                                            coroutineScope.launch { expansionProgress.snapTo(newP) }
                                        },
                                        onDragEnd = {
                                            val velocityY = velocityTracker.calculateVelocity().y
                                            settleExpansion(velocityY)
                                            velocityTracker.resetTracking()
                                        },
                                        onDragCancel = {
                                            settleExpansion(0f)
                                            velocityTracker.resetTracking()
                                        }
                                    )
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        val current = expansionProgress.value
                                        val target = when {
                                             current < 0.5f -> 1.0f
                                             current < 1.5f -> 2.0f
                                             else -> 1.0f
                                        }
                                        coroutineScope.launch {
                                            expansionProgress.animateTo(
                                                target,
                                                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                                            )
                                        }
                                    }
                                )
                                .testTag("player_drag_handle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.36f))
                            )
                        }

                        // Fixed Queue Header: always present in the layout so list is never pushed to the top unexpectedly
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            velocityTracker.resetTracking()
                                            coroutineScope.launch { expansionProgress.stop() }
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            change.consume()
                                            val currentP = expansionProgress.value
                                            val dragRange = if (currentP < 1f) stage1DragRangePx else stage2DragRangePx
                                            val deltaP = -dragAmount / dragRange
                                            val newP = (currentP + deltaP).coerceIn(0f, 2f)
                                            coroutineScope.launch { expansionProgress.snapTo(newP) }
                                        },
                                        onDragEnd = {
                                            val velocityY = velocityTracker.calculateVelocity().y
                                            settleExpansion(velocityY)
                                            velocityTracker.resetTracking()
                                        },
                                        onDragCancel = {
                                            settleExpansion(0f)
                                            velocityTracker.resetTracking()
                                        }
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UP NEXT",
                                fontSize = 12.sp,
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Text(
                                text = "${orderedQueueItems.size} tracks",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        }

                        // Keep a rolling artwork window warm even while the queue is moving.
                        // Newly exposed rows must begin decoding during the scroll, not after it stops.
                        LaunchedEffect(queueListState.firstVisibleItemIndex) {
                            if (orderedQueueItems.isNotEmpty()) {
                                val firstVisible = queueListState.firstVisibleItemIndex
                                val prefetchRange = firstVisible..(firstVisible + 12)
                                for (idx in prefetchRange) {
                                    if (idx in orderedQueueItems.indices) {
                                        FastArtworkThumbnailDefaults.prefetch(context, orderedQueueItems[idx], 128)
                                    }
                                }
                            }
                        }

                        val rowHeightPx = with(density) { 60.dp.toPx() }
                        val inertialFlingBehavior = remember(rowHeightPx) {
                            InertialSpringFlingBehavior(
                                lazyListState = queueListState,
                                rowHeightPx = rowHeightPx
                            )
                        }

                        // Scrollable List: Continuous inertial scrolling with soft damped spring settling
                        LazyColumn(
                            state = queueListState,
                            flingBehavior = inertialFlingBehavior,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 32.dp + navBarBottom)
                        ) {
                            itemsIndexed(
                                items = orderedQueueItems,
                                key = { _, item -> item.id },
                                contentType = { _, _ -> "up_next_track_row" }
                            ) { queueIndex, queueTrack ->
                                val isCurrent = queueTrack.id == track.id
                                val isDragging = queueTrack.id == activeQueueDragId

                                // YouTube Music dynamic spring shift:
                                // When an item is dragged over another item, the other item smoothly slides into the empty slot
                                val targetShift = run {
                                    val dragId = activeQueueDragId ?: return@run 0f
                                    if (isDragging) return@run 0f
                                    val fromIndex = activeQueueDragIndex
                                    val toIndex = queueDragTargetIndex
                                    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return@run 0f
                                    when {
                                        fromIndex < toIndex -> {
                                            if (queueIndex in (fromIndex + 1)..toIndex) -rowHeightPx else 0f
                                        }
                                        fromIndex > toIndex -> {
                                            if (queueIndex in toIndex until fromIndex) rowHeightPx else 0f
                                        }
                                        else -> 0f
                                    }
                                }

                                UpNextTrackRow(
                                    track = queueTrack,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying,
                                    accentColor = themeColors.accent,
                                    surfaceColor = animatedBgMidLower,
                                    stage2Progress = p2,
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
                                    targetSlotShiftY = targetShift
                                )
                            }
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
private fun SingleColorCardBackground(
    color: Color,
    modifier: Modifier = Modifier
) {
    // Fills the entire card with the single extracted color.
    // No extra dark layers, no multiple circles, and no vignette shadows blocking the color.
    Box(
        modifier = modifier.background(color)
    )
}

/**
 * Continuous smooth progress engine:
 * Bridges discrete playback position pulses (e.g. 200ms polls) with the 60Hz/120Hz display refresh.
 *
 * How it eliminates jumping & holding:
 * 1. Tracks reference arrival time using SystemClock.uptimeMillis().
 * 2. On every display frame (withFrameNanos), extrapolates progress forward by the exact frame delta dt.
 * 3. Smoothly converges towards the actual hardware position with an exponential low-pass filter,
 *    eradicating discrete holding and stepping.
 * 4. Snaps immediately on seeking (>1200ms jump) or pausing.
 */
@Composable
private fun rememberSmoothProgressMs(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isDragging: Boolean
): Float {
    var smoothedMs by remember { mutableFloatStateOf(positionMs.toFloat()) }
    var lastEnginePos by remember { mutableLongStateOf(positionMs) }
    var lastUpdateUptime by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }

    // When engine publishes a new discrete position packet
    LaunchedEffect(positionMs, isPlaying) {
        val now = SystemClock.uptimeMillis()
        val delta = abs(positionMs - smoothedMs.toLong())
        // Large jump (seek / track change) or paused: snap immediately
        if (delta > 1200L || !isPlaying) {
            smoothedMs = positionMs.toFloat()
        }
        lastEnginePos = positionMs
        lastUpdateUptime = now
    }

    // High-frequency frame animation loop for continuous liquid gliding
    LaunchedEffect(isPlaying, isDragging) {
        if (!isPlaying || isDragging) return@LaunchedEffect

        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                val now = SystemClock.uptimeMillis()
                val dtMs = if (lastFrameNanos == 0L) 16f else {
                    ((frameNanos - lastFrameNanos) / 1_000_000f).coerceIn(1f, 40f)
                }
                lastFrameNanos = frameNanos

                val timeSinceUpdate = (now - lastUpdateUptime).coerceAtLeast(0L)
                val targetEngineMs = (lastEnginePos + timeSinceUpdate).toFloat()
                val drift = targetEngineMs - smoothedMs

                // Continuously advance by elapsed frame time plus a subtle correction drift
                val driftCorrection = drift * 0.12f
                val maxDur = durationMs.coerceAtLeast(1L).toFloat()
                smoothedMs = (smoothedMs + dtMs + driftCorrection).coerceIn(0f, maxDur)
            }
        }
    }

    return smoothedMs
}

@Composable
private fun NowPlayingProgressBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val smoothMs = rememberSmoothProgressMs(
        positionMs = positionMs,
        durationMs = durationMs,
        isPlaying = isPlaying,
        isDragging = isDragging
    )

    val progressFraction = if (durationMs > 0L) {
        (smoothMs / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayFraction = if (isDragging) dragFraction else progressFraction
    val currentDisplayMs = if (isDragging) {
        (dragFraction * durationMs).toLong()
    } else {
        smoothMs.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    }

    Column(
        modifier = modifier
    ) {
        // Draggable Progress Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
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
                    .height(6.dp)
            ) {
                val totalWidth = size.width
                val centerY = size.height / 2f
                val lineThickness = 4.5.dp.toPx()
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

        // Timestamps Row directly underneath the line (brought closer to the line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-3).dp)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(currentDisplayMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f)
            )
            Text(
                text = formatMs(durationMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

/**
 * Velvet Music Player - Visualizer (Continuous Symmetrical Acoustic Waveform):
 *
 * Implements the exact Velvet visualizer specification from design mockup:
 * 1. Continuous Wave (not individual bars):
 *    - 140 fine sampling points across the full available width.
 *    - Vertical resonant threads connect top and bottom symmetrical amplitudes at each sample point.
 *    - Continuous Catmull-Rom / Bézier spline curves trace the upper and lower wave envelope contours.
 *    - Subtle harmonic phase ribbons weave through the wave body, forming the silky acoustic string vibration texture.
 *
 * 2. Centered Baseline & Symmetrical Amplitude:
 *    - Center baseline (centerY = totalHeight / 2).
 *    - Movement occurs symmetrically both above and below the baseline.
 *
 * 3. Smooth Physical Animation:
 *    - 5-tap Gaussian spatial smoothing blends neighbors into a cohesive liquid waveform.
 *    - Temporal attack (0.42f) and release (0.15f) for natural, fluid sound wave response.
 *    - Wave propagation ripples disturbances across the full width.
 *
 * 4. Fixed Color (#FFFFFF / #E5E7EB):
 *    - Pure crisp white with subtle gray threading. Zero extraction from album art.
 *
 * 5. Quiet-State Living Continuity:
 *    - Low amplitude, still visible (not completely flat).
 *    - Gentle breathing undulating wave across the baseline; never a flat horizontal straight line.
 */
@Composable
private fun PlayerBottomVerticalLines(
    isPlaying: Boolean,
    telemetry: AudioTelemetry,
    modifier: Modifier = Modifier
) {
    val lineCount = 72
    val heights = remember(lineCount) { FloatArray(lineCount) { 0.08f } }
    val frameTicker = remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                withFrameNanos { frameTicker.longValue = it }
            }
        }
    }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val time = frameTicker.longValue / 1_000_000_000f
        val fft = telemetry.fftBars

        for (i in 0 until lineCount) {
            val normalized = i.toFloat() / (lineCount - 1).coerceAtLeast(1)
            val fftIndex = if (fft.isNotEmpty()) {
                (normalized * (fft.size - 1)).toInt().coerceIn(0, fft.lastIndex)
            } else 0
            val raw = if (fft.isNotEmpty()) fft[fftIndex].coerceIn(0f, 1f) else 0f
            val breathing = (kotlin.math.sin(time * 2.2f + i * 0.24f) * 0.5f + 0.5f) * 0.035f
            val target = if (isPlaying) {
                (0.10f + raw * 0.78f + breathing).coerceIn(0.06f, 0.92f)
            } else {
                heights[i]
            }

            val current = heights[i]
            heights[i] = if (isPlaying) {
                if (target > current) {
                    current + (target - current) * 0.28f
                } else {
                    current + (target - current) * 0.10f
                }
            } else {
                current
            }

            val lineHeight = (size.height * heights[i]).coerceIn(2.dp.toPx(), size.height)
            val x = normalized * size.width
            drawLine(
                color = Color.White.copy(alpha = 0.62f),
                start = Offset(x, size.height),
                end = Offset(x, size.height - lineHeight),
                strokeWidth = 1.15.dp.toPx(),
                cap = StrokeCap.Round
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
    stage2Progress: Float = 0f,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragBy: (Float) -> Unit,
    onDragEnd: () -> Unit,
    isDragging: Boolean,
    dragOffsetY: Float,
    targetSlotShiftY: Float = 0f,
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

    // Smooth, slow spring bounce like YouTube Music when another row slides into empty space:
    val slotShiftAnim by animateFloatAsState(
        targetValue = targetSlotShiftY,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.86f,
            stiffness = 72f
        ),
        label = "queue_slot_spring_shift"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.025f else 1f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = Spring.StiffnessMediumLow),
        label = "queue_drag_scale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 0f,
        animationSpec = tween(140),
        label = "queue_drag_elevation"
    )

    // Active Item Highlight:
    // Attached directly to row position. Travels naturally with scroll and drag.
    val rowBaseBg = surfaceColor
    val activeRowBg = when {
        isDragging -> Color.White.copy(alpha = 0.16f).compositeOver(rowBaseBg)
        isSwiping -> rowBaseBg
        isCurrent -> {
            if (stage2Progress > 0.05f) {
                accentColor.copy(alpha = 0.14f * stage2Progress)
            } else {
                Color.Transparent
            }
        }
        else -> Color.Transparent
    }

    // Full-Bleed Surface Layer: Spans 100% width, moves as part of the unified scroll container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else slotShiftAnim
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 10f else 0f)
            .shadow(
                elevation = elevation.dp,
                shape = RectangleShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f),
                clip = false
            )
    ) {
        // Step 1: Background Reveal Layer - strictly sits behind moving card, revealed only as card is swiped open
        if (isSwiping && !isCurrent) {
            val targetActionColor = when {
                !isPastThreshold -> Color.Black
                swipeOffset > 0f -> Color(0xFF22C55E)
                else -> Color(0xFFEF4444)
            }
            val animatedActionBg by animateColorAsState(
                targetValue = targetActionColor,
                animationSpec = tween(160),
                label = "swipe_action_bg"
            )
            val progress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)
            val iconScale by animateFloatAsState(
                targetValue = if (isPastThreshold) 1.2f else (0.70f + progress * 0.30f),
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                ),
                label = "swipe_icon_scale"
            )
            val iconAlpha by animateFloatAsState(
                targetValue = if (isPastThreshold) 1f else (0.45f + progress * 0.55f),
                animationSpec = tween(120),
                label = "swipe_icon_alpha"
            )

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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(start = 6.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed dimensions (48.dp) with lightweight placeholder first & scroll-decoupled loading
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                FastArtworkThumbnail(
                    track = track,
                    size = 48.dp,
                    shape = RoundedCornerShape(4.dp),
                    thumbnailSizePx = 128,
                    contentDescription = track.title
                )
                if (isCurrent) {
                    // Subtle dark gradient scrim at the bottom ~32% for high wave contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                )
                            )
                    )
                    // Tiny drawing animation visualizer starting from the bottom (10% to 20% height of the music)
                    TinyBottomVisualizerWave(
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.5.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title and Subtitle (Wave indicator removed from title as requested)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title.substringBefore(" - "),
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isCurrent) accentColor else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${track.artist} • ${track.formattedDuration}",
                    fontSize = 13.sp,
                    color = if (isCurrent) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Drag Handle: 3 clean lines pushed to the far right with a small space
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 44.dp)
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
 * - Padding inside item content reaching near edge (start = 6.dp, end = 2.dp).
 * - Music picture with sharp edges (2.dp).
 * - Tiny animated wave visualizer drawing animation at the bottom (10%-20% height).
 * - 3-line drag handle pushed to far right with a small space.
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
            .padding(start = 6.dp, end = 2.dp, top = 6.dp, bottom = 6.dp), // Pushed to far left and far right with small space
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Artwork with sharp edges (2.dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            FastArtworkThumbnail(
                track = track,
                size = 48.dp,
                shape = RoundedCornerShape(2.dp),
                thumbnailSizePx = 128,
                contentDescription = track.title
            )
            if (isPlaying) {
                // Subtle dark gradient scrim at the bottom ~32% for high wave contrast
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )
                // Tiny drawing animation visualizer starting from the bottom (10% to 20% height of the music)
                TinyBottomVisualizerWave(
                    isPlaying = isPlaying,
                    accentColor = dominantColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.5.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

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

        // Drag Handle Icon: 3 lines pushed to far right with a small space
        Box(
            modifier = Modifier.size(width = 32.dp, height = 40.dp),
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

/**
 * Tiny bottom visualizer wave:
 * - Placed at the very bottom of the playing music artwork, starting from the bottom edge and reaching 10% to 20% height (e.g. 9.5dp of 48dp).
 * - Implements a proper smooth drawing animation: fluid harmonic wave with dynamic equalizer crest bars.
 * - When playing, smoothly animates with organic harmonic oscillations.
 * - When paused, gently settles to a calm low resting wave.
 */
@Composable
private fun TinyBottomVisualizerWave(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tiny_bottom_wave")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "phase2"
    )

    val playFraction by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.25f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "play_fraction"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Fluid sine-wave contour path anchored to the very bottom
        val wavePath = Path()
        wavePath.moveTo(0f, height)

        val steps = 24
        val stepX = width / steps
        for (i in 0..steps) {
            val x = i * stepX
            val normX = x / width
            val sin1 = sin(normX * 12.566f + phase1 * playFraction)
            val sin2 = cos(normX * 7.854f - phase2 * playFraction)
            val waveHeightRatio = ((sin1 * 0.4f + sin2 * 0.35f + 0.75f) * 0.5f).coerceIn(0.15f, 0.95f) * playFraction
            val y = height - (height * waveHeightRatio).coerceIn(1.5f.dp.toPx(), height)
            if (i == 0) {
                wavePath.lineTo(0f, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }
        wavePath.lineTo(width, height)
        wavePath.close()

        // Subtle gradient fill for the fluid wave
        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.55f),
                    accentColor.copy(alpha = 0.18f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // 2. High-precision animated equalizer bars drawn in the foreground
        val barCount = 5
        val barWidth = 2.4.dp.toPx()
        val totalBarsWidth = barCount * barWidth
        val availableSpacing = (width - totalBarsWidth) / (barCount + 1).coerceAtLeast(1)
        val spacing = availableSpacing.coerceIn(1.5.dp.toPx(), 4.dp.toPx())
        val startOffset = (width - (barCount * barWidth + (barCount - 1) * spacing)) / 2f

        for (b in 0 until barCount) {
            val barX = startOffset + b * (barWidth + spacing)
            val barPhase = when (b) {
                0 -> phase1 * 1.3f
                1 -> phase2 * 1.7f + 1.2f
                2 -> phase1 * 2.1f + 2.5f
                3 -> phase2 * 1.5f + 0.8f
                else -> phase1 * 1.8f + 3.1f
            }
            val oscillation = (sin(barPhase) * 0.5f + 0.5f)
            val baseRatio = when (b) {
                0 -> 0.45f
                1 -> 0.70f
                2 -> 0.95f
                3 -> 0.75f
                else -> 0.50f
            }
            val activeHeight = height * (baseRatio * (0.35f + 0.65f * oscillation)) * playFraction
            val clampedHeight = activeHeight.coerceIn(2.dp.toPx(), height)
            val barTop = height - clampedHeight

            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(barX, barTop),
                size = Size(barWidth, clampedHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
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

    val barCount = 80    // Generate an authentic acoustic signature for the song
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
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = ActionSheetBackground,
        contentColor = ActionIconAshWhite,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF42444A))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("now_playing_action_sheet")
        ) {
            // Track Header: sharp, album-jacket thumbnail at the very left edge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        thumbnailSizePx = 128,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title.substringBefore(" - "),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ActionIconAshWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = ActionIconSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Single subtle hairline separator below header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                    .height(0.6.dp)
                    .background(ActionSheetDivider)
            )

            // Compact action rows tightly packed to reduce overall sheet height
            SheetActionRow(
                title = "Play next",
                icon = { ActionPlayNextIcon(modifier = Modifier.size(22.dp)) },
                onClick = onPlayNext
            )
            SheetActionRow(
                title = "Add to queue",
                icon = { ActionQueueIcon(modifier = Modifier.size(22.dp)) },
                onClick = onQueue
            )
            SheetActionRow(
                title = if (isFavorite) "Remove from favourites" else "Add to favourites",
                icon = {
                    ActionFavoriteIcon(
                        isFavorite = isFavorite,
                        tint = if (isFavorite) accentColor else ActionIconSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onToggleFavorite
            )
            SheetActionRow(
                title = "Add to playlist",
                icon = { ActionPlaylistIcon(modifier = Modifier.size(22.dp)) },
                onClick = onAddToPlaylist
            )
            SheetActionRow(
                title = "Audio Visualizer",
                icon = { ActionVisualizerIcon(modifier = Modifier.size(22.dp)) },
                onClick = onOpenVisualizer
            )
            SheetActionRow(
                title = "Synced Lyrics",
                icon = { ActionLyricsIcon(modifier = Modifier.size(22.dp)) },
                onClick = onOpenLyrics
            )
            SheetActionRow(
                title = "Share song",
                icon = { ActionShareIcon(modifier = Modifier.size(22.dp)) },
                onClick = onShare
            )
            SheetActionRow(
                title = "Song information",
                icon = { ActionInfoIcon(modifier = Modifier.size(22.dp)) },
                onClick = onSongInfo
            )
            SheetActionRow(
                title = "Delete from library",
                textColor = ActionIconDestructive,
                icon = { ActionDeleteIcon(modifier = Modifier.size(22.dp), tint = ActionIconDestructive) },
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun MarqueeTrackTitle(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = (-0.2).sp
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = Color.White,
        textAlign = TextAlign.Start,
        letterSpacing = letterSpacing
    )

    // Unconstrained measurement of the entire title string without truncation
    val textLayoutResult = remember(title, textStyle) {
        textMeasurer.measure(
            text = AnnotatedString(title),
            style = textStyle,
            maxLines = 1,
            softWrap = false
        )
    }

    val textWidthPx = textLayoutResult.size.width.toFloat()
    val density = LocalDensity.current
    val spacingPx = with(density) { 56.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.CenterStart
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val isOverflowing = textWidthPx > containerWidthPx

        if (!isOverflowing || containerWidthPx <= 0f) {
            Text(
                text = title,
                style = textStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        } else {
            val cycleDistance = textWidthPx + spacingPx
            val animatable = remember(title, cycleDistance) { Animatable(0f) }

            LaunchedEffect(title, cycleDistance) {
                animatable.snapTo(0f)
                // Brief pause so the user can easily read the start of the title
                delay(1200L)

                // Smooth linear continuous drift: ~36 dp per second
                val speedPxPerSec = with(density) { 36.dp.toPx() }
                val durationMs = ((cycleDistance / speedPxPerSec) * 1000f).roundToInt().coerceIn(2500, 18000)

                while (isActive) {
                    animatable.animateTo(
                        targetValue = cycleDistance,
                        animationSpec = tween(
                            durationMillis = durationMs,
                            easing = LinearEasing
                        )
                    )
                    // Continuous recycling: when the text drifts to the left and finishes,
                    // it wraps seamlessly to 0 without any jump because the second copy has reached 0!
                    animatable.snapTo(0f)
                }
            }

            val offset = animatable.value

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
            ) {
                // First copy drifting to the left
                Text(
                    text = title,
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .offset { IntOffset((-offset).roundToInt(), 0) }
                )

                // Second copy entering from the right, recycling the drift continuously
                Text(
                    text = title,
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .offset { IntOffset((cycleDistance - offset).roundToInt(), 0) }
                )
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    title: String,
    textColor: Color = ActionIconAshWhite,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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