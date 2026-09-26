from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Queue rows must visually belong to the same continuous player surface.
# Keep the active/current row on the existing card color, but make inactive rows transparent.
s = s.replace(
    'surfaceColor = cardColor,\n                                onClick = { onSelectQueueTrack(queueTrack) },',
    'surfaceColor = if (isCurrent) cardColor else Color.Transparent,\n                                onClick = { onSelectQueueTrack(queueTrack) },',
    1,
)

# Horizontal swipe: reveal a true black underlay first. Only after the threshold is crossed
# should the action color emerge, and it should ramp continuously to the full action color.
old_swipe = '''    // Dynamic background transition during swipe:\n    // Initial drag state: Matches player sheet background (surfaceColor).\n    // Threshold trigger state: Transitions to vibrant green (#22C55E) for Play Next and vibrant red (#EF4444) for Delete.\n    val targetActionColor = when {\n        !isPastThreshold -> surfaceColor\n        swipeOffset > 0f -> Color(0xFF22C55E)\n        else -> Color(0xFFEF4444)\n    }\n'''
new_swipe = '''    // Dynamic background transition during swipe:\n    // Stage 1: the revealed underlay is always pure black.\n    // Stage 2: after the action threshold, black continuously blends into the directional color.\n    val actionRevealProgress = if (isPastThreshold) {\n        val transitionDistance = (swipeLimitPx - thresholdPx).coerceAtLeast(1f)\n        ((abs(swipeOffset) - thresholdPx) / transitionDistance).coerceIn(0f, 1f)\n    } else {\n        0f\n    }\n    val actionTargetColor = if (swipeOffset > 0f) Color(0xFF22C55E) else Color(0xFFEF4444)\n    val targetActionColor = actionTargetColor.copy(alpha = actionRevealProgress)\n        .compositeOver(Color.Black)\n'''
if old_swipe in s:
    s = s.replace(old_swipe, new_swipe, 1)
else:
    # Fallback for the generated threshold-based form used by an earlier patch revision.
    old_simple = '''    val targetActionColor = when {\n        !isPastThreshold -> surfaceColor\n        swipeOffset > 0f -> Color(0xFF22C55E)\n        else -> Color(0xFFEF4444)\n    }\n'''
    if old_simple in s:
        s = s.replace(old_simple, new_swipe, 1)
    elif 'val actionRevealProgress' not in s:
        raise SystemExit('queue swipe color block not found')

p.write_text(s, encoding='utf-8')
print('Applied continuous queue surface and black-first swipe color transition.')
