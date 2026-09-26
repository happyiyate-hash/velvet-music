from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

# The queue is already rendered inside PlayerSheet; keep it there and make the existing
# expansion value the single source of truth for the player -> queue transition.
anchor = "    val upNextExpanded = remember { Animatable(0f) }"
if anchor in s:
    s = s.replace(
        anchor,
        "    // One continuous controller for the player and its in-flow queue.\n"
        "    // 0f = collapsed player, 1f = queue-focused player.\n"
        "    val expansionProgress = remember { Animatable(0f) }",
        1,
    )

s = s.replace("upNextExpanded", "expansionProgress")

# Replace the old open-only handle gesture with a continuous drag. The handle now updates
# the same expansionProgress while the finger is moving; releasing it settles to the nearest state.
match = re.search(
    r'''(?P<prefix>\.fillMaxWidth\(\)\n\s*\.height\(20\.dp\)\n)(?P<gesture>\s*\.pointerInput\(Unit\) \{\n\s*detectVerticalDragGestures\s*\{.*?\n\s*\}\n\s*\}\n)''',
    s,
    re.S,
)
if not match:
    raise SystemExit("continuous queue handle anchor not found")

new_gesture = '''                            .pointerInput(Unit) {
                                val dragRangePx = with(queueDragDensity) { 420.dp.toPx() }

                                detectVerticalDragGestures(
                                    onDragStart = {
                                        coroutineScope.launch { expansionProgress.stop() }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        // Dragging up increases expansion; dragging down collapses it.
                                        val next = (expansionProgress.value - (dragAmount / dragRangePx))
                                            .coerceIn(0f, 1f)
                                        coroutineScope.launch {
                                            expansionProgress.snapTo(next)
                                        }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            val target = if (expansionProgress.value >= 0.48f) 1f else 0f
                                            expansionProgress.animateTo(
                                                target,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            val target = if (expansionProgress.value >= 0.48f) 1f else 0f
                                            expansionProgress.animateTo(
                                                target,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                )
                            }
'''
s = s[:match.start("gesture")] + new_gesture + s[match.end("gesture"):]

# Keep tap behavior as a convenience, but it toggles the SAME continuous player/queue surface.
s = s.replace(
    '''                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    expansionProgress.animateTo(1f)
                                }
                            }
''',
    '''                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    expansionProgress.animateTo(
                                        if (expansionProgress.value >= 0.5f) 0f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
''',
    1,
)

# Document the architecture at the existing in-flow queue section. No queue-specific
# ModalBottomSheet is introduced or invoked here.
if "// CONTINUOUS QUEUE SURFACE" not in s:
    marker = "        // 8. UP NEXT HANDLE & CONTENT"
    if marker in s:
        s = s.replace(
            marker,
            "        // CONTINUOUS QUEUE SURFACE: the queue is part of this same PlayerSheet hierarchy.\n"
            "        // Its vertical position/visibility is driven only by expansionProgress; no\n"
            "        // ModalBottomSheet is used for the queue.\n"
            + marker,
            1,
        )

path.write_text(s, encoding="utf-8")
print("Applied continuous player/queue expansion controller and continuous handle drag.")
