from pathlib import Path

PLAYER = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = PLAYER.read_text(encoding="utf-8")

# Keep the queue completely below the viewport in the collapsed state. As the
# existing shared expansionProgress rises, the queue boundary moves upward into
# the reserved space instead of permanently exposing an empty queue region.
old_queue_geometry = '''            val handleHeight = 24.dp\n            val handleY = controlsY + controlsHeight + 4.dp\n            val queueY = handleY + handleHeight + 4.dp\n            val queueHeight = (totalHeight - queueY).coerceAtLeast(0.dp)\n'''
new_queue_geometry = '''            val handleHeight = 24.dp\n            val handleY = controlsY + controlsHeight + 4.dp\n            val expandedQueueY = handleY + handleHeight + 4.dp\n            // 0f: queue is completely below the viewport. 1f: queue begins\n            // immediately below the existing handle. This keeps the collapsed\n            // player visually identical while continuously consuming the\n            // previously empty space during the upward drag.\n            val queueY = lerp(totalHeight, expandedQueueY, p)\n            val queueHeight = (totalHeight - queueY).coerceAtLeast(0.dp)\n'''
if old_queue_geometry in text and 'val expandedQueueY = handleY + handleHeight + 4.dp' not in text:
    text = text.replace(old_queue_geometry, new_queue_geometry, 1)

# The queue header is part of the same queue surface but must remain pinned while
# only the actual music rows scroll. stickyHeader preserves the continuous layout
# and does not introduce another surface or bottom sheet.
text = text.replace(
    'item(key = "queue_header_section") {\n                            Row(',
    'stickyHeader(key = "queue_header_section") {\n                            Row(',
    1,
)

PLAYER.write_text(text, encoding="utf-8")
print("Applied continuous queue reveal + sticky queue header refinement.")
