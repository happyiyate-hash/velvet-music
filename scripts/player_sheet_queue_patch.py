from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Imports are idempotent.
if 'detectDragGesturesAfterLongPress' not in s:
    s = s.replace(
        'import androidx.compose.foundation.gestures.detectHorizontalDragGestures\n',
        'import androidx.compose.foundation.gestures.detectHorizontalDragGestures\nimport androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress\n'
    )
if 'import androidx.compose.ui.unit.IntOffset' not in s:
    s = s.replace(
        'import androidx.compose.ui.unit.sp\n',
        'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.unit.IntOffset\n'
    )
if 'import kotlin.math.roundToInt' not in s:
    s = s.replace('import kotlin.math.sin\n', 'import kotlin.math.sin\nimport kotlin.math.roundToInt\n')

# Host callback for putting the selected queue item into the real audio engine's next slot.
if 'onPlayNextTrack: (Track) -> Unit' not in s:
    s = s.replace(
        '    onSelectQueueTrack: (Track) -> Unit = {},\n',
        '    onSelectQueueTrack: (Track) -> Unit = {},\n    onPlayNextTrack: (Track) -> Unit = {},\n'
    )

# Keep queue order independent from currentTrack so tapping a song does not make it jump to row 1.
old_queue_state = '''    val queueItems = remember(queueTracks, track) {
        if (queueTracks.isNotEmpty()) {
            val otherTracks = queueTracks.filter { it.id != track.id }
            listOf(track) + otherTracks
        } else {
            listOf(track) + com.example.model.SampleData.starterTracks.filter { it.id != track.id }
        }
    }

    val queueListState = rememberLazyListState()'''
new_queue_state = '''    val queueItems = remember(queueTracks) {
        if (queueTracks.isNotEmpty()) queueTracks.distinctBy { it.id }
        else com.example.model.SampleData.starterTracks.distinctBy { it.id }
    }
    var orderedQueueItems by remember(queueItems) { mutableStateOf(queueItems) }
    val queueListState = rememberLazyListState()'''
if old_queue_state in s:
    s = s.replace(old_queue_state, new_queue_state, 1)

# Replace queue list rendering once.
old_items = '''                    items(queueItems) { queueTrack ->
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
                    }'''
new_items = '''                    items(
                        items = orderedQueueItems,
                        key = { it.id }
                    ) { queueTrack ->
                        val isCurrent = queueTrack.id == track.id
                        val queueIndex = orderedQueueItems.indexOfFirst { it.id == queueTrack.id }
                        UpNextTrackRow(
                            track = queueTrack,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            accentColor = themeColors.accent,
                            onClick = { onSelectQueueTrack(queueTrack) },
                            onPlayNext = {
                                onPlayNextTrack(queueTrack)
                                val currentIndex = orderedQueueItems.indexOfFirst { it.id == queueTrack.id }
                                val playingIndex = orderedQueueItems.indexOfFirst { it.id == track.id }
                                if (currentIndex >= 0 && playingIndex >= 0 && currentIndex != playingIndex + 1) {
                                    val destination = if (currentIndex < playingIndex) playingIndex else playingIndex + 1
                                    orderedQueueItems = orderedQueueItems.toMutableList().apply {
                                        val moved = removeAt(currentIndex)
                                        add(destination.coerceAtMost(size), moved)
                                    }
                                }
                            },
                            onDelete = {
                                if (!isCurrent) orderedQueueItems = orderedQueueItems.filterNot { it.id == queueTrack.id }
                            },
                            onMove = { from, direction ->
                                val target = from + direction
                                if (target in orderedQueueItems.indices && from != 0 && target != 0) {
                                    orderedQueueItems = orderedQueueItems.toMutableList().apply {
                                        add(target, removeAt(from))
                                    }
                                }
                            },
                            index = queueIndex
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }'''
if old_items in s:
    s = s.replace(old_items, new_items, 1)

# Replace the old rounded queue card with compact rows and gesture actions.
if 'private fun UpNextTrackRow(' in s and 'private fun AnimatedPlayingBars(' not in s:
    start = s.index('@Composable\nprivate fun UpNextTrackRow(')
    end = s.index('\nprivate fun formatTrackDuration', start)
    new_row = r'''@Composable
private fun UpNextTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int, Int) -> Unit,
    index: Int,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    var reordering by remember(track.id) { mutableStateOf(false) }
    var reorderAccumulatedY by remember(track.id) { mutableFloatStateOf(0f) }
    val maxSwipe = 132f
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxWidth().height(58.dp)) {
        if (swipeOffset != 0f) {
            val isDelete = swipeOffset < 0f
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = if (isDelete) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(92.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDelete) Color.White.copy(alpha = 0.07f) else accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.SkipNext,
                        contentDescription = if (isDelete) "Delete from queue" else "Play next",
                        tint = Color.White.copy(alpha = (abs(swipeOffset) / maxSwipe).coerceIn(0.35f, 1f)),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .clip(RoundedCornerShape(9.dp))
                .background(if (isCurrent) accentColor.copy(alpha = 0.07f) else Color.Transparent)
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {},
                        onDragCancel = { swipeOffset = 0f },
                        onDragEnd = {
                            when {
                                swipeOffset <= -92f -> { onDelete(); swipeOffset = 0f }
                                swipeOffset >= 92f -> { onPlayNext(); swipeOffset = 0f }
                                else -> swipeOffset = 0f
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            if (!reordering) swipeOffset = (swipeOffset + amount).coerceIn(-maxSwipe, maxSwipe)
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(0.7.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkImage(track = track, contentDescription = track.title, modifier = Modifier.fillMaxSize())
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPlayingBars(color = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = track.title.substringBefore(" - "),
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) accentColor else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "${track.artist} • ${formatTrackDuration(track.durationMs)}",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .size(38.dp)
                    .pointerInput(track.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                reordering = true
                                reorderAccumulatedY = 0f
                                swipeOffset = 0f
                            },
                            onDragEnd = { reordering = false; reorderAccumulatedY = 0f },
                            onDragCancel = { reordering = false; reorderAccumulatedY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                reorderAccumulatedY += dragAmount.y
                                val threshold = with(density) { 28.dp.toPx() }
                                if (abs(reorderAccumulatedY) >= threshold) {
                                    val direction = if (reorderAccumulatedY < 0f) -1 else 1
                                    onMove(index, direction)
                                    reorderAccumulatedY = 0f
                                }
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(17.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.88f)))
                Box(Modifier.width(17.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.88f)))
            }
        }
    }
}

@Composable
private fun AnimatedPlayingBars(color: Color, modifier: Modifier = Modifier) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "queue_playing")
    val a by infinite.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val b by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val c by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar3"
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height((18f * a).dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.width(3.dp).height((18f * b).dp).clip(RoundedCornerShape(2.dp)).background(color))
        Box(Modifier.width(3.dp).height((18f * c).dp).clip(RoundedCornerShape(2.dp)).background(color))
    }
}
'''
    s = s[:start] + new_row + s[end:]

p.write_text(s, encoding='utf-8')
print('Applied compact queue rows, stable positions, white handles, playing animation, long-press reorder, swipe delete and Play Next.')
