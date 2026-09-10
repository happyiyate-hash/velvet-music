from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

if 'import androidx.compose.animation.core.Animatable' not in s:
    s = s.replace('import androidx.compose.animation.AnimatedVisibility\n', 'import androidx.compose.animation.AnimatedVisibility\nimport androidx.compose.animation.core.Animatable\n', 1)

start = s.index('@Composable\nprivate fun UpNextTrackRow(')
end = s.index('@Composable\nprivate fun AnimatedPlayingBars', start)

new_func = r'''@Composable
private fun UpNextTrackRow(
    track: Track, isCurrent: Boolean, isPlaying: Boolean, accentColor: Color,
    onClick: () -> Unit, onPlayNext: () -> Unit, onDelete: () -> Unit,
    onDragStart: () -> Unit, onDragBy: (Float) -> Unit, onDragEnd: () -> Unit,
    isDragging: Boolean, dragOffsetY: Float, virtualDisplacementY: Float,
    isDropTarget: Boolean, modifier: Modifier = Modifier
) {
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    var thresholdLatched by remember(track.id) { mutableStateOf(false) }
    val swipeSettle = remember(track.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val maxRevealPx = with(density) { 132.dp.toPx() }
    val dismissPx = with(density) { 760.dp.toPx() }
    val neutralAction = Color(0xFF1E1E1E)
    val deleteAction = Color(0xFFE53935)
    val playNextAction = Color(0xFF43A047)
    val swipingRight = swipeOffset > 0f
    val swipingLeft = swipeOffset < 0f
    val stage2 = abs(swipeOffset) >= thresholdPx
    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)
    val actionColor = if (stage2) {
        if (swipingLeft) deleteAction else playNextAction
    } else {
        neutralAction
    }
    val actionIconAlpha = 0.38f + (0.62f * stageProgress)
    val actionIconScale = 0.70f + (0.30f * stageProgress)
    val actionBackgroundAlpha = if (stage2) 1f else 0.96f
    val actionColorAnimated by androidx.compose.animation.animateColorAsState(
        targetValue = actionColor,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "queue_swipe_action_color"
    )
    val actionScaleAnimated by animateFloatAsState(
        targetValue = actionIconScale,
        animationSpec = tween(110, easing = FastOutSlowInEasing),
        label = "queue_swipe_action_scale"
    )
    val actionAlphaAnimated by animateFloatAsState(
        targetValue = actionIconAlpha,
        animationSpec = tween(110, easing = FastOutSlowInEasing),
        label = "queue_swipe_action_alpha"
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
    val elevation by animateFloatAsState(if (isDragging) 14f else 0f, tween(120), label = "queue_drag_elevation")

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
    ) {
        // UNDERLAY: the action surface is always behind the song card. The card slides over it,
        // revealing the nested action icon instead of placing an icon on top of the row.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(9.dp))
                .background(actionColorAnimated.copy(alpha = actionBackgroundAlpha)),
            contentAlignment = Alignment.Center
        ) {
            if (swipingLeft || swipingRight) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(92.dp)
                        .align(if (swipingLeft) Alignment.CenterEnd else Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (swipingLeft) Icons.Default.Delete else Icons.Default.SkipNext,
                        contentDescription = if (swipingLeft) "Delete from queue" else "Play next",
                        tint = Color.White.copy(alpha = actionAlphaAnimated),
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = actionScaleAnimated
                                scaleY = actionScaleAnimated
                            }
                    )
                }
            }
        }

        // TOP CARD: only this layer translates. The underlying action layer never moves.
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
                        else -> Color.Transparent
                    }
                )
                .pointerInput(track.id, isDragging) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            thresholdLatched = false
                            swipeSettle.stop()
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
                                    swipeSettle.animateTo(
                                        0f,
                                        androidx.compose.animation.core.spring(
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                                        )
                                    ) { swipeOffset = value }
                                    thresholdLatched = false
                                }
                            } else {
                                val direction = if (releaseOffset > 0f) 1f else -1f
                                scope.launch {
                                    swipeSettle.snapTo(releaseOffset)
                                    swipeSettle.animateTo(
                                        direction * dismissPx,
                                        tween(190, easing = FastOutSlowInEasing)
                                    ) { swipeOffset = value }
                                    if (direction > 0f) onPlayNext() else onDelete()
                                    swipeOffset = 0f
                                    thresholdLatched = false
                                }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            if (!isDragging && !isCurrent) {
                                val next = (swipeOffset + amount).coerceIn(-maxRevealPx, maxRevealPx)
                                if (!thresholdLatched && abs(swipeOffset) < thresholdPx && abs(next) >= thresholdPx) {
                                    thresholdLatched = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
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
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(.7.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                TrackArtworkImage(track = track, contentDescription = track.title, modifier = Modifier.fillMaxSize())
                if (isCurrent && isPlaying) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)),
                        contentAlignment = Alignment.Center
                    ) { AnimatedPlayingBars(Color.White, Modifier.size(24.dp)) }
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
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
                Modifier.size(38.dp).pointerInput(track.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { swipeOffset = 0f; onDragStart() },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, amount -> change.consume(); onDragBy(amount.y) }
                    )
                },
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(17.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = .92f)))
                Box(Modifier.width(17.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = .92f)))
            }
        }
    }
}

'''

s = s[:start] + new_func + s[end:]
p.write_text(s, encoding='utf-8')
print('patched', p)
