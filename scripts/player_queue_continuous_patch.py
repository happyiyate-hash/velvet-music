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
        "    // One continuous controller for the player and its in-flow queue.\\n"
        "    // 0f = collapsed player, 1f = queue-focused player.\\n"
        "    val expansionProgress = remember { Animatable(0f) }",
        1,
    )

s = s.replace("upNextExpanded", "expansionProgress")
s = s.replace(
    "    val upNextP = expansionProgress.value",
    "    val upNextP = expansionProgress.value",
    1,
)

# Replace the old tap/open-only behavior with a continuous drag. The handle now updates the
# same expansionProgress while the finger is moving; releasing it settles to the nearest state.
# This intentionally does not create or invoke a ModalBottomSheet.
open_pattern = re.compile(
    r'''(?P<indent>\s*)\.pointerInput\(Unit\) \{\n(?P<body>\s*detectVerticalDragGestures\s*\{.*?\n\s*\}\n\s*\}\n''',
    re.S,
)

# Only replace the first gesture block that is attached to the 20dp queue/player handle.
match = re.search(
    r'''(?P<prefix>\.fillMaxWidth\(\)\n\s*\.height\(20\.dp\)\n)(?P<gesture>\s*\.pointerInput\(Unit\) \{\n\s*detectVerticalDragGestures\s*\{.*?\n\s*\}\n\s*\}\n)''',
    s,
    re.S,
)
if not match:
    raise SystemExit("continuous queue handle anchor not found")

indent = "                            "
new_gesture = '''                            .pointerInput(Unit) {
                                var dragStartProgress = 0f
                                val dragRangePx = with(LocalDensity.current) { 420.dp.toPx() }

                                detectVerticalDragGestures(
                                    onDragStart = {
                                        dragStartProgress = expansionProgress.value
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
                                            expansionProgress.animateTo(
                                                if (expansionProgress.value >= 0.48f) 1f else 0f,
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
# The old block's indentation is already part of the captured source; preserve the prefix and
# replace only the gesture modifier.
s = s[:match.start("gesture")] + new_gesture + s[match.end("gesture"):]

# Remove any queue-specific click handlers that only existed to open a separate queue surface.
# We leave the rest of the player's action sheets (lyrics/info/visualizer) untouched.
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

# Make the in-flow queue explicitly document its architecture and keep it attached to the
# player surface rather than presenting a second sheet/card surface.
if "// CONTINUOUS QUEUE SURFACE" not in s:
    marker = "        // 8. UP NEXT HANDLE & CONTENT"
    if marker in s:
        s = s.replace(
            marker,
            "        // CONTINUOUS QUEUE SURFACE: the queue is part of this same PlayerSheet hierarchy.\\n"
            "        // Its vertical position/visibility is driven only by expansionProgress; no\\n"
            "        // ModalBottomSheet is used for the queue.\\n"
            + marker,
            1,
        )

path.write_text(s, encoding="utf-8")
print("Applied continuous player/queue expansion controller and removed queue open-only gesture behavior.")
