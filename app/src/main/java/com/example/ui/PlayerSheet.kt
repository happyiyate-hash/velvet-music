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
import androidx.compose.ui.unit.lerp
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
    // The resolved bitmap is passed directly into the color extractor state update loop.
    var resolvedArtworkBitmap by remember(track.id, track.artworkUri, track.coverResId) {
        val initial = ArtworkColorExtractor.resolveTrackBitmap(context, track)
            ?: if (track.coverResId != 0) {
                runCatching { BitmapFactory.decodeResource(context.resources, track.coverResId) }.getOrNull()
            } else null
        mutableStateOf<Bitmap?>(initial)
    }
    var themeColors by remember(track.id, track.artworkUri, track.coverResId) {
        val initial = resolvedArtworkBitmap
            ?: ArtworkColorExtractor.resolveTrackBitmap(context, track)
            ?: if (track.coverResId != 0) {
                runCatching { BitmapFactory.decodeResource(context.resources, track.coverResId) }.getOrNull()
            } else null
        mutableStateOf(
            if (initial != null) ArtworkColorExtractor.extractColorsFromBitmap(initial)
            else ArtworkColorExtractor.generateThemePalette(track.dominantColor)
        )
    }

    LaunchedEffect(track.id, track.artworkUri, track.coverResId) {
        if (resolvedArtworkBitmap == null) {
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
    }

    // One continuous controller for the player and its in-flow queue.
    // 0f = collapsed player, 0.55f = player + queue peek, 1f = queue-focused player.
    val expansionProgress = remember { Animatable(0f) }
    val p = expansionProgress.value.coerceIn(0f, 1f)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    BackHandler {
        if (expansionProgress.value > 0.1f) {
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
        val itemHeightPx = with(density) { 60.dp.toPx() }
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

    val playerBackgroundGradient = remember(animatedBgTop, animatedBgMidUpper, animatedBgMidLower, animatedBgBottom) {
        Brush.verticalGradient(
            0.0f to animatedBgTop,
            0.35f to animatedBgMidUpper,
            0.68f to animatedBgMidLower,
            1.0f to animatedBgBottom
        )
    }

    val playerSheetGradient = playerBackgroundGradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(playerBackgroundGradient)
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
                .navigationBarsPadding()
        ) {
            val totalWidth = maxWidth
            val totalHeight = maxHeight
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            // One continuous progress value drives everything: 0f (collapsed full player) to 1f (stage 1 endpoint)
            val p = expansionProgress.value.coerceIn(0f, 1f)

            // 1. Artwork Geometry
            // Aspect ratio is strictly preserved (1:1 square artwork).
            val artworkAspectRatio = 1.0f

            // Collapsed state (p = 0f):
            // The card ends right below the waveform. The controls row (64dp) + handle (24dp) + margins (12dp) = 100dp
            // sit outside at the bottom, matching the exact original Velvet player boundary without extra height or empty gap.
            val collapsedCardHeight = totalHeight - 100.dp
            val collapsedWaveformY = collapsedCardHeight - 24.dp
            val collapsedProgressY = collapsedWaveformY - 28.dp
            val collapsedMetadataY = collapsedProgressY - 46.dp
            val collapsedArtworkBottom = collapsedMetadataY - 10.dp
            val collapsedArtworkHeight = (minOf(totalWidth - 28.dp, collapsedArtworkBottom - (statusBarTop + 46.dp) - 6.dp)).coerceIn(250.dp, 350.dp)
            val collapsedArtworkWidth = collapsedArtworkHeight * artworkAspectRatio
            val collapsedArtworkTop = ((statusBarTop + 46.dp) + (collapsedArtworkBottom - (statusBarTop + 46.dp) - collapsedArtworkHeight) / 2f)
            val collapsedArtworkLeft = (totalWidth - collapsedArtworkWidth) / 2f
            val collapsedArtworkRadius = 14.dp

            // Expanded state (p = 1f):
            // Artwork expands horizontally to full screen width and top (behind status bar).
            val expandedArtworkWidth = totalWidth
            val expandedArtworkHeight = expandedArtworkWidth / artworkAspectRatio
            val expandedArtworkTop = 0.dp
            val expandedArtworkLeft = 0.dp
            val expandedArtworkRadius = 0.dp
            val expandedArtworkBottom = expandedArtworkTop + expandedArtworkHeight // = totalWidth

            // The card bottom boundary tracks the bottom boundary of the artwork!
            val expandedCardHeight = expandedArtworkBottom

            // In expanded state, metadata, progress, and waveform move into the lower region of the artwork:
            val expandedWaveformY = expandedArtworkBottom - 26.dp
            val expandedProgressY = expandedWaveformY - 26.dp
            val expandedMetadataY = expandedProgressY - 44.dp

            // Continuous lerp for Artwork and Card:
            val artworkWidth = lerp(collapsedArtworkWidth, expandedArtworkWidth, p)
            val artworkHeight = artworkWidth / artworkAspectRatio
            val artworkTop = lerp(collapsedArtworkTop, expandedArtworkTop, p)
            val artworkLeft = lerp(collapsedArtworkLeft, expandedArtworkLeft, p)
            val artworkRadius = lerp(collapsedArtworkRadius, expandedArtworkRadius, p)
            val artworkBottom = artworkTop + artworkHeight

            val cardHeight = lerp(collapsedCardHeight, expandedCardHeight, p)

            val currentCardCornerRadius = lerp(30.dp, 22.dp, p)
            val dynamicCardShape = RoundedCornerShape(
                bottomStart = currentCardCornerRadius,
                bottomEnd = currentCardCornerRadius
            )

            // Continuous lerp for elements inside the Card:
            val metadataY = lerp(collapsedMetadataY, expandedMetadataY, p)
            val progressY = lerp(collapsedProgressY, expandedProgressY, p)
            val waveformY = lerp(collapsedWaveformY, expandedWaveformY, p)
            val contentPaddingHorizontal = lerp(22.dp, 18.dp, p)

            // Transport Controls, Handle, and Queue sit OUTSIDE (UNDER) the Card:
            // Controls maintain their constant full size (do NOT shrink or reduce when dragging):
            val controlsHeight = 64.dp
            val controlsY = cardHeight + 8.dp
            val handleHeight = 24.dp
            val handleY = controlsY + controlsHeight + 4.dp
            val queueY = handleY + handleHeight + 4.dp
            val queueHeight = (totalHeight - queueY).coerceAtLeast(0.dp)

            val playButtonSize = 62.dp
            val playIconSize = 38.dp
            val secondaryIconTouchSize = 46.dp
            val secondaryIconSize = 26.dp
            val skipButtonSize = 48.dp
            val skipIconSize = 32.dp

            val dragRangePx = with(density) { (collapsedCardHeight - expandedCardHeight).toPx() }.coerceAtLeast(100f)
            val velocityTracker = remember { VelocityTracker() }

            fun settleExpansion(velocity: Float = 0f) {
                coroutineScope.launch {
                    val current = expansionProgress.value
                    val target = when {
                        velocity < -500f -> 1.0f  // Fast upward swipe -> snap to compressed player & revealed queue
                        velocity > 500f -> 0.0f   // Fast downward swipe -> snap to full player card
                        current > 0.45f -> 1.0f   // Past halfway -> snap to stage 1 endpoint
                        else -> 0.0f              // Otherwise snap back to full collapsed player
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

            val nestedScrollConnection = remember(dragRangePx) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val dy = available.y
                        if (dy < 0f && expansionProgress.value < 1f) {
                            val deltaP = -dy / dragRangePx
                            val newP = (expansionProgress.value + deltaP).coerceAtMost(1f)
                            val consumedP = newP - expansionProgress.value
                            val consumedY = -consumedP * dragRangePx
                            coroutineScope.launch { expansionProgress.snapTo(newP) }
                            return Offset(0f, consumedY)
                        }
                        if (dy > 0f && queueListState.firstVisibleItemIndex == 0 && queueListState.firstVisibleItemScrollOffset == 0 && expansionProgress.value > 0f) {
                            val deltaP = -dy / dragRangePx
                            val newP = (expansionProgress.value + deltaP).coerceAtLeast(0f)
                            val consumedP = expansionProgress.value - newP
                            val consumedY = consumedP * dragRangePx
                            coroutineScope.launch { expansionProgress.snapTo(newP) }
                            return Offset(0f, consumedY)
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val dy = available.y
                        if (dy > 0f && expansionProgress.value > 0f) {
                            val deltaP = -dy / dragRangePx
                            val newP = (expansionProgress.value + deltaP).coerceAtLeast(0f)
                            val consumedP = expansionProgress.value - newP
                            val consumedY = consumedP * dragRangePx
                            coroutineScope.launch { expansionProgress.snapTo(newP) }
                            return Offset(0f, consumedY)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (expansionProgress.value > 0f && expansionProgress.value < 1f) {
                            settleExpansion(available.y)
                            return available
                        }
                        return Velocity.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        if (expansionProgress.value > 0f && expansionProgress.value < 1f) {
                            settleExpansion(available.y)
                            return available
                        }
                        return Velocity.Zero
                    }
                }
            }

            // 1. THE RESHAPING PLAYER CARD (Contains TopBar, Artwork, Metadata, Progress, Waveform; compresses upward)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .shadow(
                        elevation = lerp(16.dp, 8.dp, p),
                        shape = dynamicCardShape,
                        spotColor = Color.Black.copy(alpha = 0.80f),
                        ambientColor = Color.Black.copy(alpha = 0.45f)
                    )
                    .clip(dynamicCardShape)
                    .background(cardGradient)
                    .drawWithContent {
                        drawContent()
                        val r = currentCardCornerRadius.toPx()
                        val strokeWidth = 1.6.dp.toPx()
                        val sideExtension = 20.dp.toPx()
                        val path = Path().apply {
                            moveTo(0f, size.height - r - sideExtension)
                            lineTo(0f, size.height - r)
                            arcTo(
                                rect = Rect(0f, size.height - 2 * r, 2 * r, size.height),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            lineTo(size.width - r, size.height)
                            arcTo(
                                rect = Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height),
                                startAngleDegrees = 90f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            lineTo(size.width, size.height - r - sideExtension)
                        }
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    animatedCardBorderColor.copy(alpha = 0.20f),
                                    animatedCardBorderColor.copy(alpha = 0.65f),
                                    animatedCardBorderColor.copy(alpha = 0.20f)
                                )
                            ),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                coroutineScope.launch { expansionProgress.stop() }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                                val deltaP = -dragAmount / dragRangePx
                                val newP = (expansionProgress.value + deltaP).coerceIn(0f, 1f)
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
                // ALBUM ARTWORK (Continuous positioning, aspect ratio strictly preserved, extends behind status bar at p=1f)
                val dynamicArtworkShape = RoundedCornerShape(artworkRadius)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(artworkLeft.roundToPx(), artworkTop.roundToPx()) }
                        .size(width = artworkWidth, height = artworkHeight)
                        .shadow(
                            elevation = lerp(18.dp, 0.dp, p),
                            shape = dynamicArtworkShape,
                            spotColor = Color.Black.copy(alpha = 0.72f),
                            ambientColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .clip(dynamicArtworkShape)
                ) {
                    TrackArtworkImage(
                        track = track,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Specular reflection sheen (subtle glass highlight)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    0.0f to Color.White.copy(alpha = 0.16f),
                                    0.25f to Color.White.copy(alpha = 0.05f),
                                    0.50f to Color.Transparent,
                                    1.0f to Color.White.copy(alpha = 0.03f)
                                )
                            )
                    )

                    // Glass depth vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    0.0f to Color.Transparent,
                                    0.80f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.24f)
                                )
                            )
                    )

                    // Chamfered glass border (fades as artwork reaches screen edges)
                    if (p < 0.95f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        0.0f to Color.White.copy(alpha = 0.45f * (1f - p)),
                                        0.35f to Color.White.copy(alpha = 0.18f * (1f - p)),
                                        0.70f to Color.White.copy(alpha = 0.08f * (1f - p)),
                                        1.0f to Color.White.copy(alpha = 0.30f * (1f - p))
                                    ),
                                    shape = dynamicArtworkShape
                                )
                        )
                    }

                    // Scrim gradient in lower portion to guarantee crystal-clear contrast as metadata/progress/waveform move in
                    if (p > 0.02f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.70f to Color.Black.copy(alpha = 0.45f * p),
                                        0.85f to Color.Black.copy(alpha = 0.72f * p),
                                        1.0f to Color.Black.copy(alpha = 0.88f * p)
                                    )
                                )
                        )
                    }
                }

                // TOP BAR (Stays at status bar level, sits cleanly above artwork with .zIndex(10f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, (statusBarTop + 4.dp).roundToPx()) }
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                        .zIndex(10f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (expansionProgress.value > 0.05f) {
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.28f * p)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse Player",
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (p > 0.65f) {
                        Text(
                            text = "UP NEXT",
                            fontSize = 12.sp,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f))
                        )
                    }

                    IconButton(
                        onClick = { showActionSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_menu_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.28f * p)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // TRACK IDENTITY (Title & Artist, moves continuously into lower artwork region)
                key(track.id) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, metadataY.roundToPx()) }
                            .padding(horizontal = contentPaddingHorizontal)
                            .zIndex(6f),
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

                // PROGRESS BAR & TIMESTAMPS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, progressY.roundToPx()) }
                        .padding(horizontal = contentPaddingHorizontal)
                        .zIndex(6f)
                ) {
                    NowPlayingProgressBar(
                        positionMs = playbackPositionMs,
                        durationMs = track.durationMs,
                        isPlaying = isPlaying,
                        onSeekTo = onSeekTo,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SILHOUETTE WAVEFORM
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, waveformY.roundToPx()) }
                        .padding(horizontal = contentPaddingHorizontal)
                        .zIndex(6f)
                ) {
                    DarkSilhouetteWaveform(
                        isPlaying = isPlaying,
                        telemetry = telemetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } // End of Player Card Box

            // 2. TRANSPORT CONTROLS ROW (Always OUTSIDE & UNDER the artwork/card, never inside artwork!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, controlsY.roundToPx()) }
                    .height(controlsHeight)
                    .padding(horizontal = contentPaddingHorizontal)
                    .zIndex(6f),
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

            // 3. DRAG HANDLE (Tapping toggles expansion, dragging scrubs expansionProgress)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, handleY.roundToPx()) }
                    .height(handleHeight)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                coroutineScope.launch { expansionProgress.stop() }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                                val deltaP = -dragAmount / dragRangePx
                                val newP = (expansionProgress.value + deltaP).coerceIn(0f, 1f)
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
                            val target = if (expansionProgress.value > 0.5f) 0f else 1f
                            coroutineScope.launch {
                                expansionProgress.animateTo(
                                    target,
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    )
                    .zIndex(6f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.32f))
                )
            }

            // 4. THE QUEUE CONTENT
            //
            // The queue is a separate viewport from the player. As the player collapses,
            // queueY moves upward and the queue automatically takes over the reclaimed
            // empty space. Once the queue reaches its expanded position, ONLY the track
            // rows scroll. The "UP NEXT" boundary/header remains fixed.
            if (queueHeight > 1.dp) {
                val queueHeaderHeight = 44.dp
                val queueListHeight = (queueHeight - queueHeaderHeight).coerceAtLeast(0.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(queueHeight)
                        .offset { IntOffset(0, queueY.roundToPx()) }
                        .clipToBounds()
                        .zIndex(4f)
                ) {
                    // FIXED QUEUE BOUNDARY
                    // This header moves upward with the collapsing player, but it is
                    // deliberately outside LazyColumn so it never scrolls away.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(queueHeaderHeight)
                            // Transparent: inherit the exact player-sheet background.
                            // Do not introduce a brighter queue-colored rectangle here.
                            .background(Color.Transparent)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UP NEXT",
                            fontSize = 12.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                        Text(
                            text = "${orderedQueueItems.size} tracks",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                    }

                    // ONLY THIS REGION SCROLLS.
                    // Keeping LazyColumn below the fixed boundary prevents the queue
                    // header/upper block from being dragged away with the songs.
                    LazyColumn(
                        state = queueListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(queueListHeight)
                            .offset(y = queueHeaderHeight)
                            .nestedScroll(nestedScrollConnection),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 32.dp)
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
                                surfaceColor = cardColor,
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
                                    val itemHeightPx = with(density) { 60.dp.toPx() }
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

@Composable
private fun DarkSilhouetteWaveform(
    isPlaying: Boolean,
    telemetry: AudioTelemetry,
    modifier: Modifier = Modifier
) {
    // 92 tiny, tightly packed vertical bars with minimal gap, in pure black
    val barCount = 92
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
        // Very tiny bars brought tightly together so there is minimal space between them
        val barWidth = 1.8.dp.toPx()
        val barGap = ((totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)).coerceIn(0.8.dp.toPx(), 1.5.dp.toPx())

        val minBarHeight = 1.8.dp.toPx()
        val maxBarHeight = size.height * 0.85f
        val usableRange = (maxBarHeight - minBarHeight).coerceAtLeast(0f)

        val attackRate = 0.45f
        val decayRate = 0.16f

        // Pure solid black color
        val barColor = Color.Black

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

            drawRoundRect(
                color = barColor,
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
    isDropTarget: Boolean,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    val maxSwipe = 132f

    // First reveal is always BLACK. The colored action is intentionally delayed
    // until the user has swiped far enough to make the intent obvious.
    val actionRevealStart = 54f
    val actionRevealProgress =
        ((abs(swipeOffset) - actionRevealStart) / (maxSwipe - actionRevealStart))
            .coerceIn(0f, 1f)

    val displacement by animateFloatAsState(
        targetValue = virtualDisplacementY,
        animationSpec = spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
        ),
        label = "queue_virtual_displacement"
    )
    val scale by animateFloatAsState(
        if (isDragging) 1.02f else 1f,
        tween(120),
        label = "queue_drag_scale"
    )
    val elevation by animateFloatAsState(
        if (isDragging) 14f else 0f,
        tween(120),
        label = "queue_drag_elevation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else displacement
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 10f else 0f)
            .shadow(elevation.dp, RoundedCornerShape(9.dp), clip = false)
            .clip(RoundedCornerShape(9.dp))
    ) {
        // Base swipe reveal layer.
        // This is deliberately BLACK, not transparent, so the page/background
        // can never show through while a row is being swiped.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalArrangement = if (swipeOffset < 0f) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (swipeOffset != 0f && !isDragging) {
                val deleting = swipeOffset < 0f

                Box(
                    modifier = Modifier
                        .width(92.dp)
                        .height(50.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (actionRevealProgress > 0f) {
                                Color(0xFFB91C1C).copy(
                                    alpha = 0.16f + 0.34f * actionRevealProgress
                                )
                            } else {
                                Color.Black
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (deleting) Icons.Default.Delete else Icons.Default.SkipNext,
                        contentDescription = if (deleting) "Delete from queue" else "Play next",
                        tint = Color.White.copy(
                            alpha = if (actionRevealProgress > 0f) {
                                0.45f + 0.55f * actionRevealProgress
                            } else {
                                0f
                            }
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // The song row slides over the reveal layer.
        Row(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isDragging -> accentColor.copy(alpha = .13f)
                        isDropTarget -> accentColor.copy(alpha = .06f)
                        isCurrent -> accentColor.copy(alpha = .07f)
                        else -> surfaceColor
                    }
                )
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {},
                        onDragCancel = { swipeOffset = 0f },
                        onDragEnd = {
                            if (!isDragging) {
                                when {
                                    swipeOffset <= -92f -> {
                                        onDelete()
                                        swipeOffset = 0f
                                    }
                                    swipeOffset >= 92f -> {
                                        onPlayNext()
                                        swipeOffset = 0f
                                    }
                                    else -> swipeOffset = 0f
                                }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            if (!isDragging) {
                                swipeOffset = (swipeOffset + amount)
                                    .coerceIn(-maxSwipe, maxSwipe)
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(
                        .7.dp,
                        Color.White.copy(alpha = .10f),
                        RoundedCornerShape(7.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize()
                )

                if (isCurrent && isPlaying) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = .28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPlayingBars(
                            Color.White,
                            Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(9.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    track.title.substringBefore(" - "),
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) accentColor else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    "${track.artist} • ${formatTrackDuration(track.durationMs)}",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = .58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                Modifier
                    .size(38.dp)
                    .pointerInput(track.id) {
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
                    },
                verticalArrangement = Arrangement.spacedBy(
                    3.dp,
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .width(17.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = .92f))
                )
                Box(
                    Modifier
                        .width(17.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = .92f))
                )
            }
        }
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
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Start,
        letterSpacing = (-0.2).sp
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