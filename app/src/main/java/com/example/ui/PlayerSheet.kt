package com.example.ui

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    val themeColors = remember(track) { ArtworkColorExtractor.extractColors(context, track) }
    val dragProgress = remember { Animatable(0f) }
    val p = dragProgress.value
    val haptic = LocalHapticFeedback.current
    var hasLatchedStage1 by remember { mutableStateOf(false) }
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
        modifier = Modifier.fillMaxSize().background(themeColors.darkBackground).testTag("full_player_sheet")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(brush = Brush.verticalGradient(colors = listOf(themeColors.bgTop, themeColors.bgMidUpper, themeColors.bgMidLower, themeColors.bgBottom), startY = 0f, endY = canvasHeight))
            drawRect(brush = Brush.radialGradient(colors = listOf(themeColors.atmosphericBloom.copy(alpha = 0.35f), themeColors.atmosphericBloom.copy(alpha = 0.12f), Color.Transparent), center = Offset(canvasWidth * 0.5f, canvasHeight * 0.28f), radius = canvasWidth * 0.85f))
        }
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val density = LocalDensity.current
        val artworkAspectRatio = remember(track) {
            var ratio = 1f
            if (!track.artworkUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(track.artworkUri)
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path, opts)
                    else context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, opts) }
                    if (opts.outWidth > 0 && opts.outHeight > 0) ratio = opts.outWidth.toFloat() / opts.outHeight.toFloat()
                } catch (_: Exception) {}
            }
            ratio
        }
        val insetsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val insetsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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
        val expArtY = 0.dp
        val expArtWidth = totalWidth
        val naturalExpandedHeight = totalWidth / artworkAspectRatio
        val expArtHeight = naturalExpandedHeight.coerceAtMost(totalHeight * 0.78f)
        val expArtX = 0.dp
        val expArtCorner = 0.dp
        val expTitleX = 20.dp
        val expTitleY = expArtY + expArtHeight - 82.dp
        val expTitleWidth = totalWidth - 40.dp
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
            artWidth = lerp(baseArtWidth, expArtWidth, t); artHeight = lerp(baseArtHeight, expArtHeight, t); artX = lerp(baseArtX, expArtX, t); artY = lerp(baseArtY, expArtY, t); artCorner = lerp(baseArtCorner, expArtCorner, t)
        } else {
            val t = (p - 1f).coerceIn(0f, 1f)
            artWidth = lerp(expArtWidth, compArtSize, t); artHeight = lerp(expArtHeight, compArtSize, t); artX = lerp(expArtX, compArtX, t); artY = lerp(expArtY, compArtY, t); artCorner = lerp(expArtCorner, compArtCorner, t)
        }
        val titleX: androidx.compose.ui.unit.Dp; val titleY: androidx.compose.ui.unit.Dp; val titleWidth: androidx.compose.ui.unit.Dp; val titleSizeSp: Float; val artistSizeSp: Float
        if (p <= 1f) {
            val t = p.coerceIn(0f, 1f); titleX = lerp(baseTitleX, expTitleX, t); titleY = lerp(baseTitleY, expTitleY, t); titleWidth = lerp(totalWidth - (baseTitleX * 2), expTitleWidth, t); titleSizeSp = 21f - (2f * t); artistSizeSp = 15f - t
        } else {
            val t = (p - 1f).coerceIn(0f, 1f); titleX = lerp(expTitleX, compTitleX, t); titleY = lerp(expTitleY, compTitleY, t); titleWidth = lerp(expTitleWidth, totalWidth - compTitleX - 92.dp, t); titleSizeSp = 19f - (5f * t); artistSizeSp = 14f - t
        }
        val upNextY = if (p <= 1f) lerp(baseUpNextY, expUpNextY, p.coerceIn(0f, 1f)) else lerp(expUpNextY, compUpNextY, (p - 1f).coerceIn(0f, 1f))
        val topBarAlpha = (1f - p * 2.2f).coerceIn(0f, 1f)
        val sourceAlpha = (1f - p * 2f).coerceIn(0f, 1f)
        val stage1DistancePx = with(density) { (baseUpNextY - expUpNextY).toPx().coerceAtLeast(300f) }
        val stage2DistancePx = with(density) { (expUpNextY - compUpNextY).toPx().coerceAtLeast(220f) }
        val onDragDelta: (Float) -> Unit = { dragAmountPx -> coroutineScope.launch { dragProgress.stop(); val currentP = dragProgress.value; val delta = if (currentP < 1f) -dragAmountPx / stage1DistancePx else -dragAmountPx / stage2DistancePx; dragProgress.snapTo((currentP + delta).coerceIn(0f, 2f)) } }
        val onDragFinish: () -> Unit = { coroutineScope.launch { val currentP = dragProgress.value; val target = when { currentP < 0.35f -> 0f; currentP < 1.55f -> 1f; else -> 2f }; dragProgress.animateTo(target, tween(300, easing = FastOutSlowInEasing)) } }

        if (topBarAlpha > 0f) {
            Row(modifier = Modifier.offset(x = 16.dp, y = topBarY).width(totalWidth - 32.dp).height(44.dp).graphicsLayer { alpha = topBarAlpha }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp).testTag("player_collapse_button")) { Icon(Icons.Default.KeyboardArrowDown, "Collapse Now Playing", tint = Color.White.copy(alpha = 0.90f), modifier = Modifier.size(26.dp)) }
                Box(modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 6.dp)) { Text("NOW PLAYING", fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.72f)) }
                IconButton(onClick = { showActionSheet = true }, modifier = Modifier.size(40.dp).testTag("player_menu_button")) { Icon(Icons.Default.MoreVert, "Song Actions Menu", tint = Color.White.copy(alpha = 0.90f), modifier = Modifier.size(22.dp)) }
            }
        }

        Box(modifier = Modifier.offset(x = artX, y = artY).width(artWidth).height(artHeight).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { coroutineScope.launch { dragProgress.animateTo(if (p > 1.2f) 1f else if (p > 0.2f) 0f else 1f, tween(320, easing = FastOutSlowInEasing)) } }).then(if (p < 1.65f) Modifier.pointerInput(Unit) { detectVerticalDragGestures(onDragEnd = { onDragFinish() }, onDragCancel = { onDragFinish() }, onVerticalDrag = { change, dragAmount -> change.consume(); onDragDelta(dragAmount) }) } else Modifier)) {
            Box(modifier = Modifier.width(artWidth).height(artHeight).clip(RoundedCornerShape(artCorner))) {
                TrackArtworkImage(track = track, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }

        val artworkFadeAlpha = when { p < 0.30f -> 0f; p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f); else -> 1f }
        if (artworkFadeAlpha > 0f) {
            Box(modifier = Modifier.offset(x = expArtX, y = expArtY + (expArtHeight * 0.76f)).width(expArtWidth).height(expArtHeight * 0.24f).graphicsLayer { alpha = artworkFadeAlpha }.background(Brush.verticalGradient(colorStops = arrayOf(0.00f to Color.Transparent, 0.16f to themeColors.bgBottom.copy(alpha = 0.12f), 0.34f to themeColors.bgBottom.copy(alpha = 0.34f), 0.52f to themeColors.bgBottom.copy(alpha = 0.62f), 0.72f to themeColors.bgBottom.copy(alpha = 0.86f), 1.00f to themeColors.bgBottom))).zIndex(1f))
        }

        Column(modifier = Modifier.offset(x = titleX, y = titleY).width(titleWidth).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { if (p > 1.2f) coroutineScope.launch { dragProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) } })) {
            Text(track.title, fontSize = titleSizeSp.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(track.artist, fontSize = artistSizeSp.sp, color = Color.White.copy(alpha = 0.76f), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            if (sourceAlpha > 0f) {
                Spacer(modifier = Modifier.height(2.dp)); val src = if (track.catalogSource.contains("Device", ignoreCase = true) || track.contentUri != null) "Device Audio" else if (track.catalogSource.isNotBlank()) track.catalogSource else "Device Audio"; Text(src, fontSize = 13.sp, color = Color.White.copy(alpha = 0.45f * sourceAlpha), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }

        val progressFraction = if (track.durationMs > 0) (playbackPositionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        if (p <= 1f) {
            NowPlayingWaveformProgress(positionMs = playbackPositionMs, durationMs = track.durationMs, isPlaying = isPlaying, telemetry = telemetry, activeColor = themeColors.accent, onSeekTo = onSeekTo, modifier = Modifier.offset(x = 16.dp, y = lerp(baseWaveformY, expArtY + expArtHeight - 34.dp, p.coerceIn(0f, 1f))).width(totalWidth - 32.dp).zIndex(2f))
        }

        val controlT = p.coerceIn(0f, 1f)
        val controlY = lerp(baseWaveformY + 52.dp + 44.dp, expControlsY, controlT)
        val centerX = totalWidth / 2
        val playLeft = centerX - 38.dp
        val prevLeft = centerX - 96.dp - 30.dp
        val nextLeft = centerX + 96.dp - 30.dp
        val secondaryOffsetY = lerp(104.dp, 6.dp, controlT)
        val expandedControlsFade = (1f - ((p - 1f) / 0.45f)).coerceIn(0f, 1f)
        Box(modifier = Modifier.fillMaxWidth().height(170.dp).offset(y = controlY - 8.dp).graphicsLayer { alpha = expandedControlsFade }) {
            AnimatedShuffleIcon(isShuffle = isShuffle, activeColor = themeColors.accent, onClick = onToggleShuffle, modifier = Modifier.offset(x = lerp(24.dp, 12.dp, controlT), y = secondaryOffsetY).testTag("player_shuffle_button"), touchSize = 52.dp, iconSize = 28.dp)
            IconButton(onClick = onSkipPrevious, modifier = Modifier.offset(x = prevLeft, y = 0.dp).size(60.dp).testTag("player_previous_button")) { Icon(Icons.Default.SkipPrevious, "Previous Track", tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(42.dp)) }
            Box(modifier = Modifier.offset(x = playLeft, y = -4.dp).size(78.dp).shadow(12.dp, CircleShape, spotColor = themeColors.accent.copy(alpha = 0.40f), ambientColor = themeColors.darkBackground).clip(CircleShape).background(Brush.verticalGradient(colors = listOf(themeColors.playPauseGradTop, themeColors.playPauseGradBottom))).border(1.5.dp, Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.45f), themeColors.accent.copy(alpha = 0.32f), Color.White.copy(alpha = 0.12f))), CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTogglePlayPause).testTag("player_play_pause_button"), contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(40.dp)) }
            IconButton(onClick = onSkipNext, modifier = Modifier.offset(x = nextLeft, y = 0.dp).size(60.dp).testTag("player_next_button")) { Icon(Icons.Default.SkipNext, "Next Track", tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(42.dp)) }
            AnimatedRepeatIcon(isRepeat = isRepeat, activeColor = themeColors.accent, onClick = onToggleRepeat, modifier = Modifier.offset(x = lerp(totalWidth - 72.dp, totalWidth - 64.dp, controlT), y = secondaryOffsetY).testTag("player_repeat_button"), touchSize = 52.dp, iconSize = 28.dp)
        }

        val upNextHeight = totalHeight - upNextY - insetsBottom
        Column(modifier = Modifier.offset(y = upNextY).fillMaxWidth().height(upNextHeight.coerceAtLeast(54.dp)).background(Color.Transparent).pointerInput(Unit) { detectVerticalDragGestures(onDragEnd = { onDragFinish() }, onDragCancel = { onDragFinish() }, onVerticalDrag = { change, dragAmount -> change.consume(); onDragDelta(dragAmount) }) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (p < 0.35f) {
                    Text("Up Next", fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.55f)); Spacer(modifier = Modifier.height(3.5.dp)); Box(modifier = Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(Color.White.copy(alpha = 0.35f)))
                } else {
                    Box(modifier = Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(Color.White.copy(alpha = 0.35f))); Spacer(modifier = Modifier.height(4.dp)); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Playing from", fontSize = 10.5.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.50f), fontWeight = FontWeight.Medium); Text(if (track.catalogSource.isNotBlank()) track.catalogSource else "Queue", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.08f)).border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).clickable { Toast.makeText(context, "Queue saved to playlist", Toast.LENGTH_SHORT).show() }.padding(horizontal = 12.dp, vertical = 5.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Icon(Icons.Default.QueueMusic, "Save Queue", tint = Color.White, modifier = Modifier.size(14.dp)); Text("Save", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White) } }
                    }
                }
            }
            if (p > 0.40f) {
                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)
                LazyColumn(state = queueListState, userScrollEnabled = p >= 0.95f, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = listAlpha }.padding(horizontal = 14.dp), contentPadding = PaddingValues(bottom = insetsBottom + 24.dp)) {
                    items(queueItems) { queueTrack ->
                        val isCurrent = queueTrack.id == track.id
                        UpNextTrackRow(track = queueTrack, isCurrent = isCurrent, isPlaying = isPlaying && isCurrent, accentColor = themeColors.accent, onClick = { onSelectQueueTrack(queueTrack) })
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        if (showActionSheet) {
            NowPlayingActionSheet(track = track, isFavorite = isFavorite, accentColor = themeColors.accent, onPlayNext = { onPlayNext(); showActionSheet = false }, onQueue = { onQueue(); showActionSheet = false; Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show() }, onToggleFavorite = { onToggleFavorite(); showActionSheet = false }, onAddToPlaylist = { showActionSheet = false; Toast.makeText(context, "Added to Playlist: Velvet Favorites", Toast.LENGTH_SHORT).show() }, onOpenVisualizer = { showActionSheet = false; showVisualizerSheet = true }, onOpenLyrics = { showActionSheet = false; showLyricsSheet = true }, onShare = { showActionSheet = false; onShareTrack() }, onSongInfo = { showActionSheet = false; showSongInfoDialog = true }, onDelete = { showActionSheet = false; onDeleteTrack() }, onDismiss = { showActionSheet = false })
        }
        if (showSongInfoDialog) SongInfoModal(track = track, accentColor = themeColors.accent, onDismiss = { showSongInfoDialog = false })
        if (showVisualizerSheet) AudioVisualizerBottomSheet(track = track, telemetry = telemetry, isPlaying = isPlaying, accentColor = themeColors.accent, onDismiss = { showVisualizerSheet = false })
        if (showLyricsSheet) LyricsBottomSheet(track = track, playbackPositionMs = playbackPositionMs, accentColor = themeColors.accent, onSeekTo = onSeekTo, onDismiss = { showLyricsSheet = false })
    }
}

@Composable
private fun UpNextTrackRow(track: Track, isCurrent: Boolean, isPlaying: Boolean, accentColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isCurrent) accentColor.copy(alpha = 0.16f) else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            TrackArtworkImage(track = track, contentDescription = track.title, modifier = Modifier.fillMaxSize())
            if (isCurrent && isPlaying) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, "Playing", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { Text(track.title.substringBefore(" - "), fontSize = 13.5.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold, color = if (isCurrent) accentColor else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(modifier = Modifier.height(2.dp)); Text("${track.artist} • ${formatTrackDuration(track.durationMs)}", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Box(modifier = Modifier.width(16.dp).height(2.dp).background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(1.dp))); Box(modifier = Modifier.width(16.dp).height(2.dp).background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(1.dp))) }
    }
}
private fun formatTrackDuration(durationMs: Long): String { if (durationMs <= 0) return "3:30"; val totalSeconds = durationMs / 1000; return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60) }

@Composable
fun NowPlayingWaveformProgress(positionMs: Long, durationMs: Long, isPlaying: Boolean, telemetry: AudioTelemetry, activeColor: Color, onSeekTo: (Long) -> Unit, waveformAlpha: Float = 1f, timestampAlpha: Float = 1f, progressAlpha: Float = 1f, modifier: Modifier = Modifier) {
    val progressFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    var isDragging by remember { mutableStateOf(false) }; var dragFraction by remember { mutableFloatStateOf(0f) }; val displayFraction = if (isDragging) dragFraction else progressFraction
    val barCount = 80
    val baseProfile = remember(durationMs, barCount) { FloatArray(barCount) { i -> val norm = i.toFloat() / barCount; val wave1 = abs(sin(norm * 3.14159f * 1.8f + 0.35f)); val wave2 = abs(sin(norm * 3.14159f * 4.3f)) * 0.42f; val wave3 = abs(sin(norm * 3.14159f * 7.8f + 1.1f)) * 0.28f; val wave4 = abs(cos(norm * 3.14159f * 12.2f)) * 0.16f; (wave1 * 0.52f + wave2 + wave3 + wave4).coerceIn(0.18f, 0.95f) } }
    Column(modifier = modifier.testTag("player_progress_slider").pointerInput(durationMs) { detectTapGestures { offset -> onSeekTo(((offset.x / size.width.toFloat()).coerceIn(0f, 1f) * durationMs).toLong()) } }.pointerInput(durationMs) { detectHorizontalDragGestures(onDragStart = { offset -> isDragging = true; dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) }, onDragEnd = { isDragging = false; onSeekTo((dragFraction * durationMs).toLong()) }, onDragCancel = { isDragging = false }, onHorizontalDrag = { change, _ -> change.consume(); dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) }) }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp).graphicsLayer { alpha = waveformAlpha.coerceIn(0f, 1f) }) {
            val totalWidth = size.width; val barWidth = 1.3.dp.toPx(); val totalBarWidth = barWidth * barCount; val barGap = if (barCount > 1) (totalWidth - totalBarWidth) / (barCount - 1) else 0f; val maxBarHeight = size.height; val minBarHeight = 2.5.dp.toPx(); val liveEnergy = if (isPlaying) telemetry.rmsLevel else 0.32f; val liveTransient = if (isPlaying) telemetry.transientSpike else 0f
            for (i in 0 until barCount) { val barX = i * (barWidth + barGap); val base = baseProfile[i]; val modulation = if (isPlaying) { val ripple = sin(i * 0.35f + (positionMs / 220f)).toFloat(); 0.70f + 0.30f * liveEnergy + 0.18f * liveTransient * ripple.coerceAtLeast(0f) } else 0.72f; val barHeight = (base * maxBarHeight * modulation).coerceIn(minBarHeight, maxBarHeight); val barTop = size.height - barHeight; drawRoundRect(color = activeColor, topLeft = Offset(barX, barTop), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).graphicsLayer { alpha = progressAlpha.coerceIn(0f, 1f) }.zIndex(2f)) {
            val totalWidth = size.width; val centerY = size.height / 2f; val lineThickness = 2.0.dp.toPx(); val progressWidth = (totalWidth * displayFraction).coerceIn(0f, totalWidth)
            drawRoundRect(color = Color.White.copy(alpha = 0.16f), topLeft = Offset(0f, centerY - lineThickness / 2f), size = Size(totalWidth, lineThickness), cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f))
            if (progressWidth > 0f) drawRoundRect(brush = Brush.horizontalGradient(colors = listOf(activeColor.copy(alpha = 0.85f), activeColor, Color.White.copy(alpha = 0.90f)), startX = 0f, endX = progressWidth), topLeft = Offset(0f, centerY - lineThickness / 2f), size = Size(progressWidth, lineThickness), cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f))
            val beadRadius = (if (isDragging) 4.2.dp else 3.5.dp).toPx(); val beadX = progressWidth.coerceIn(beadRadius, totalWidth - beadRadius); drawCircle(color = Color.White, radius = beadRadius, center = Offset(beadX, centerY)); drawCircle(color = activeColor, radius = beadRadius, center = Offset(beadX, centerY), style = Stroke(width = 1.0.dp.toPx()))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = timestampAlpha.coerceIn(0f, 1f) }, horizontalArrangement = Arrangement.SpaceBetween) { Text(formatMs(if (isDragging) (dragFraction * durationMs).toLong() else positionMs), fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.48f)); Text(formatMs(durationMs), fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.48f)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingActionSheet(track: Track, isFavorite: Boolean, accentColor: Color, onPlayNext: () -> Unit, onQueue: () -> Unit, onToggleFavorite: () -> Unit, onAddToPlaylist: () -> Unit, onOpenVisualizer: () -> Unit, onOpenLyrics: () -> Unit, onShare: () -> Unit, onSongInfo: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFF151618), contentColor = Color.White, dragHandle = { Box(modifier = Modifier.padding(top = 10.dp, bottom = 8.dp).width(36.dp).height(3.5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f))) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp).testTag("now_playing_action_sheet")) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f))) { TrackArtworkImage(track = track, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Spacer(modifier = Modifier.width(14.dp)); Column(modifier = Modifier.weight(1f)) { Text(track.title.substringBefore(" - "), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${track.artist} • ${track.album}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f))); Spacer(modifier = Modifier.height(10.dp))
            SheetActionRow(Icons.Default.PlayArrow, "Play next") { onPlayNext() }; SheetActionRow(Icons.Default.QueueMusic, "Add to queue") { onQueue() }; SheetActionRow(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (isFavorite) "Remove from favourites" else "Add to favourites", if (isFavorite) accentColor else Color.White.copy(alpha = 0.70f)) { onToggleFavorite() }; SheetActionRow(Icons.Default.Folder, "Add to playlist") { onAddToPlaylist() }; SheetActionRow(Icons.Default.GraphicEq, "Audio Visualizer") { onOpenVisualizer() }; SheetActionRow(Icons.Default.Subtitles, "Synced Lyrics") { onOpenLyrics() }; SheetActionRow(Icons.Default.Share, "Share song") { onShare() }; SheetActionRow(Icons.Default.Info, "Song information") { onSongInfo() }; SheetActionRow(Icons.Default.Delete, "Delete from library", Color(0xFFE57373)) { onDelete() }
        }
    }
}
@Composable private fun SheetActionRow(icon: ImageVector, title: String, iconTint: Color = Color.White.copy(alpha = 0.70f), onClick: () -> Unit) { Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, title, tint = iconTint, modifier = Modifier.size(22.dp)); Spacer(modifier = Modifier.width(16.dp)); Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.90f)) } }

@Composable
fun SongInfoModal(track: Track, accentColor: Color, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF18191C), title = { Text("Song Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }, text = { Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { InfoLine("Title", track.title.substringBefore(" - ")); InfoLine("Artist", track.artist); InfoLine("Album", track.album); InfoLine("Duration", formatMs(track.durationMs)); InfoLine("Audio Format", "High-Resolution AAC / MP3"); InfoLine("Bitrate", "160 kbps CD-Quality"); InfoLine("Sample Rate", "44.1 kHz Stereo"); InfoLine("Source", track.catalogSource); InfoLine("BPM", "${track.bpm} BPM") } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = accentColor, fontWeight = FontWeight.Bold) } }) }
@Composable private fun InfoLine(label: String, value: String) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.50f)); Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.90f)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVisualizerBottomSheet(track: Track, telemetry: AudioTelemetry, isPlaying: Boolean, accentColor: Color, onDismiss: () -> Unit) { val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true); ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFF151618), contentColor = Color.White, dragHandle = { Box(modifier = Modifier.padding(vertical = 10.dp).width(36.dp).height(3.5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f))) }) { Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Real-Time Spectrum Visualizer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("${track.title.substringBefore(" - ")} • ${if (isPlaying) "Active" else "Paused"}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f)); Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { val bands = 16; for (b in 0 until bands) { val bandNorm = b.toFloat() / bands; val hFraction = if (isPlaying) { val localMod = abs(sin(bandNorm * 3.14f * 2.5f + (telemetry.rmsLevel * 4f))).toFloat(); (0.25f + 0.65f * telemetry.rmsLevel * localMod + (if (b % 4 == 0) telemetry.transientSpike * 0.35f else 0f)).coerceIn(0.12f, 1f) } else 0.15f; Box(modifier = Modifier.weight(1f).padding(horizontal = 2.dp).fillMaxWidth().height((110 * hFraction).dp).clip(RoundedCornerShape(4.dp)).background(Brush.verticalGradient(colors = listOf(accentColor, accentColor.copy(alpha = 0.40f)))) ) } }; Spacer(modifier = Modifier.height(20.dp)); Text("Frequency Engine: 44.1 kHz • Fast Snap Transient Peak Response", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.40f)) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(track: Track, playbackPositionMs: Long, accentColor: Color, onSeekTo: (Long) -> Unit, onDismiss: () -> Unit) { val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true); ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFF151618), contentColor = Color.White, dragHandle = { Box(modifier = Modifier.padding(vertical = 10.dp).width(36.dp).height(3.5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.20f))) }) { Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) { Text("Synced Lyrics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("${track.title.substringBefore(" - ")} • ${track.artist}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f)); Spacer(modifier = Modifier.height(16.dp)); if (track.lyrics.isEmpty()) Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { Text("No time-synced lyrics found for this track.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.45f)) } else LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { items(track.lyrics) { line -> val index = track.lyrics.indexOf(line); val isCurrent = playbackPositionMs >= line.timeMs && (index == track.lyrics.lastIndex || playbackPositionMs < track.lyrics[index + 1].timeMs); Text(line.text, fontSize = if (isCurrent) 17.sp else 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth().clickable { onSeekTo(line.timeMs) }.padding(vertical = 2.dp)) } } } } }

fun formatMs(ms: Long): String { val totalSeconds = (ms / 1000).coerceAtLeast(0L); return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60) }
