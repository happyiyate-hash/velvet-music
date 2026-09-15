from pathlib import Path
import re

player = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = player.read_text()

s, n = re.subn(
    r'''    val queueItems = remember\(queueTracks, track\.id\) \{.*?\n    \}''',
    '''    // Preserve the queue's physical order. Selecting a track must not move it to the top.\n    val queueItems = remember(queueTracks, track.id) {\n        val raw = if (queueTracks.isNotEmpty()) queueTracks.distinctBy { it.id }\n        else com.example.model.SampleData.starterTracks.distinctBy { it.id }\n        if (raw.any { it.id == track.id }) raw else raw + track\n    }''',
    s, count=1, flags=re.S)
assert n == 1, 'queueItems block not found'

s, n = re.subn(
    r'''    fun beginQueueDrag\(id: String, index: Int, canDrag: Boolean\).*?\n    }\n\n    fun updateQueueDrag\(deltaY: Float\).*?\n    }\n\n    fun finishQueueDrag\(\).*?\n    }''',
    '''    fun beginQueueDrag(id: String, index: Int, canDrag: Boolean = true) {\n        if (!canDrag || index < 0 || activeQueueDragId != null) return\n        activeQueueDragId = id\n        activeQueueDragIndex = index\n        queueDragOffsetY = 0f\n        queueDragTargetIndex = index\n        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)\n    }\n\n    fun updateQueueDrag(deltaY: Float) {\n        if (activeQueueDragId == null || activeQueueDragIndex < 0) return\n        queueDragOffsetY += deltaY\n        val step = with(queueDragDensity) { 64.dp.toPx() }\n        val raw = activeQueueDragIndex + (queueDragOffsetY / step).roundToInt()\n        queueDragTargetIndex = raw.coerceIn(0, orderedQueueItems.lastIndex.coerceAtLeast(0))\n    }\n\n    fun finishQueueDrag() {\n        val from = activeQueueDragIndex\n        val to = queueDragTargetIndex\n        if (activeQueueDragId != null && from >= 0 && to >= 0 && from < orderedQueueItems.size && to < orderedQueueItems.size && from != to) {\n            val updatedQueue = orderedQueueItems.toMutableList().apply {\n                add(to, removeAt(from))\n            }\n            orderedQueueItems = updatedQueue\n            onReorderQueue?.invoke(from, to)\n            onUpdateQueue?.invoke(updatedQueue)\n        }\n        activeQueueDragId = null\n        activeQueueDragIndex = -1\n        queueDragOffsetY = 0f\n        queueDragTargetIndex = -1\n    }''',
    s, count=1, flags=re.S)
assert n == 1, 'queue drag functions not found'

s, n = re.subn(
    r'''\n                var selectedQueueFilter by remember \{ mutableStateOf\("All"\) \}.*?\n                LazyColumn''',
    '''\n\n                LazyColumn''',
    s, count=1, flags=re.S)
assert n == 1, 'queue filter chips block not found'

s = s.replace('fontSize = 10.5.sp,\n                                letterSpacing = 1.sp,', 'fontSize = 9.5.sp,\n                                letterSpacing = 0.7.sp,', 1)
s = s.replace('fontSize = 15.sp,\n                                fontWeight = FontWeight.Bold,', 'fontSize = 13.5.sp,\n                                fontWeight = FontWeight.Bold,', 1)

s = s.replace('''    val thresholdPx = with(density) { 90.dp.toPx() }\n    val dismissPx = with(density) { 600.dp.toPx() }\n\n    val isSwiping = swipeOffset < -1f''', '''    val thresholdPx = with(density) { 90.dp.toPx() }\n    val swipeLimitPx = with(density) { 600.dp.toPx() }\n\n    val isSwiping = abs(swipeOffset) > 1f''', 1)

s, n = re.subn(
    r'''    val bgColor by animateColorAsState\(.*?\n    \)''',
    '''    val actionBackground by animateColorAsState(\n        targetValue = when {\n            swipeOffset > 1f -> accentColor.copy(alpha = 0.32f).compositeOver(surfaceColor)\n            swipeOffset < -1f -> Color(0xFFD32F2F)\n            else -> surfaceColor\n        },\n        animationSpec = tween(150),\n        label = "SwipeActionBackground"\n    )''',
    s, count=1, flags=re.S)
assert n == 1, 'swipe background state not found'

s, n = re.subn(
    r'''        if \(isSwiping && !isCurrent\) \{.*?\n        \}''',
    '''        if (isSwiping && !isCurrent) {\n            val showingUpNext = swipeOffset > 1f\n            Box(\n                modifier = Modifier\n                    .fillMaxSize()\n                    .background(actionBackground),\n                contentAlignment = if (showingUpNext) Alignment.CenterEnd else Alignment.CenterStart\n            ) {\n                Row(\n                    modifier = Modifier.padding(horizontal = 20.dp),\n                    verticalAlignment = Alignment.CenterVertically,\n                    horizontalArrangement = Arrangement.spacedBy(7.dp)\n                ) {\n                    Icon(\n                        imageVector = if (showingUpNext) Icons.Default.QueueMusic else Icons.Default.Delete,\n                        contentDescription = if (showingUpNext) "Play Next" else "Remove Track",\n                        tint = Color.White,\n                        modifier = Modifier.size(22.dp)\n                    )\n                    if (abs(swipeOffset) >= thresholdPx * 0.65f) {\n                        Text(\n                            text = if (showingUpNext) "Up Next" else "Remove",\n                            fontSize = 12.sp,\n                            fontWeight = FontWeight.SemiBold,\n                            color = Color.White\n                        )\n                    }\n                }\n            }\n        }''',
    s, count=1, flags=re.S)
assert n == 1, 'swipe action background block not found'

old = '''                        onDragEnd = {\n                            val releaseOffset = swipeOffset\n                            val crossed = abs(releaseOffset) >= thresholdPx\n                            if (!crossed) {\n                                scope.launch {\n                                    swipeSettle.snapTo(releaseOffset)\n                                    swipeSettle.animateTo(\n                                        0f,\n                                        androidx.compose.animation.core.spring(\n                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,\n                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy\n                                        )\n                                    ) { swipeOffset = value }\n                                    thresholdLatched = false\n                                }\n                            } else {\n                                scope.launch {\n                                    swipeSettle.snapTo(releaseOffset)\n                                    swipeSettle.animateTo(\n                                        -dismissPx,\n                                        tween(190, easing = FastOutSlowInEasing)\n                                    ) { swipeOffset = value }\n                                    onDelete()\n                                    swipeOffset = 0f\n                                    thresholdLatched = false\n                                }\n                            }\n                        },'''
new = '''                        onDragEnd = {\n                            val releaseOffset = swipeOffset\n                            val crossed = abs(releaseOffset) >= thresholdPx\n                            if (!crossed) {\n                                scope.launch {\n                                    swipeSettle.snapTo(releaseOffset)\n                                    swipeSettle.animateTo(0f, androidx.compose.animation.core.spring(\n                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,\n                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy\n                                    )) { swipeOffset = value }\n                                    thresholdLatched = false\n                                }\n                            } else if (releaseOffset > 0f) {\n                                scope.launch {\n                                    swipeSettle.snapTo(releaseOffset)\n                                    swipeSettle.animateTo(swipeLimitPx, tween(190, easing = FastOutSlowInEasing)) { swipeOffset = value }\n                                    onPlayNext()\n                                    swipeSettle.snapTo(0f)\n                                    swipeOffset = 0f\n                                    thresholdLatched = false\n                                }\n                            } else {\n                                scope.launch {\n                                    swipeSettle.snapTo(releaseOffset)\n                                    swipeSettle.animateTo(-swipeLimitPx, tween(190, easing = FastOutSlowInEasing)) { swipeOffset = value }\n                                    onDelete()\n                                    swipeOffset = 0f\n                                    thresholdLatched = false\n                                }\n                            }\n                        },'''
assert old in s, 'swipe end block not found'
s = s.replace(old, new, 1)

s = s.replace('val next = (swipeOffset + amount).coerceIn(-dismissPx, 0f)', 'val next = (swipeOffset + amount).coerceIn(-swipeLimitPx, swipeLimitPx)', 1)

old = '''    val activeRowBg = if (isCurrent) {\n        accentColor.copy(alpha = 0.18f).compositeOver(Color(0xFF101215))\n    } else if (isDragging || isDropTarget) {\n        Color(0xFF1F2227)\n    } else {\n        Color(0xFF0F1113) // Continuous solid dark surface, prevents background bleed\n    }'''
new = '''    val rowAccent = track.dominantColor\n    val activeRowBg = when {\n        isDragging || isDropTarget -> rowAccent.copy(alpha = 0.28f).compositeOver(surfaceColor)\n        isCurrent -> rowAccent.copy(alpha = 0.22f).compositeOver(surfaceColor)\n        else -> rowAccent.copy(alpha = 0.10f).compositeOver(surfaceColor)\n    }'''
assert old in s, 'row background block not found'
s = s.replace(old, new, 1)
s = s.replace('onDragStart = { beginQueueDrag(queueTrack.id, queueIndex, !isCurrent) }', 'onDragStart = { beginQueueDrag(queueTrack.id, queueIndex) }', 1)
player.write_text(s)

engine = Path('app/src/main/java/com/example/audio/VelvetAudioEngine.kt')
e = engine.read_text()
old = '''        if (updateQueue) {\n            val current = _activeQueue.value\n            val existingIndex = current.indexOfFirst { it.id == track.id }\n            if (existingIndex >= 0) {\n                // Rotate queue so selected track becomes index 0, and upcoming tracks follow\n                _activeQueue.value = current.drop(existingIndex) + current.take(existingIndex)\n            } else {\n                val all = getAllAvailableTracks().filterNot { it.id == track.id }\n                _activeQueue.value = listOf(track) + all\n            }\n        }'''
new = '''        if (updateQueue) {\n            // Selecting a track must never reorder the visible queue.\n            val current = _activeQueue.value\n            if (current.none { it.id == track.id }) {\n                _activeQueue.value = current + track\n            }\n        }'''
assert old in e, 'playTrack queue rotation block not found'
engine.write_text(e.replace(old, new, 1))

print('Queue UX patch applied successfully.')
