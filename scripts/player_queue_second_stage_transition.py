from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

# The first continuous queue patch already owns the gesture. Extend that same controller
# from one expansion stage to two: 0 = collapsed, 1 = queue revealed, 2 = queue focused.
s = s.replace(
    "val next = (expansionProgress.value - (dragAmount / dragRangePx))\n                                            .coerceIn(0f, 1f)",
    "val next = (expansionProgress.value - (dragAmount / dragRangePx))\n                                            .coerceIn(0f, 2f)",
    1,
)
s = s.replace(
    "val dragRangePx = with(queueDragDensity) { 420.dp.toPx() }",
    "val dragRangePx = with(queueDragDensity) { 420.dp.toPx() }",
    1,
)

old_snap = '''val target = if (expansionProgress.value >= 0.48f) 1f else 0f'''
new_snap = '''val target = when {
                                                expansionProgress.value < 0.5f -> 0f
                                                expansionProgress.value < 1.5f -> 1f
                                                else -> 2f
                                            }'''
s = s.replace(old_snap, new_snap)

old_tap = '''expansionProgress.animateTo(
                                        if (expansionProgress.value >= 0.5f) 0f else 1f,'''
new_tap = '''expansionProgress.animateTo(
                                        when {
                                            expansionProgress.value < 0.5f -> 1f
                                            expansionProgress.value < 1.5f -> 2f
                                            else -> 0f
                                        },'''
s = s.replace(old_tap, new_tap, 1)

# Keep all existing stage-1 geometry driven by a clamped progress value. The new stage is
# layered on top of the same hierarchy and does not create another surface.
anchor = "    val upNextP = expansionProgress.value\n"
if anchor not in s:
    raise SystemExit("continuous expansion anchor not found")
s = s.replace(
    anchor,
    anchor
    + "    // Second-stage transition: 0..1 is the existing player-to-queue reveal; 1..2\n"
      "    // progressively compacts the artwork into the upper-left and makes room for the queue.\n"
      "    val queueFocusProgress = ((expansionProgress.value - 1f) / 1f).coerceIn(0f, 1f)\n",
    1,
)

# The existing player code expects its expansion value to top out at 1. Keep that behavior
# while the new queueFocusProgress independently drives the second-stage transformation.
s = s.replace(
    "val upNextP = expansionProgress.value\n    // Second-stage transition",
    "val upNextP = expansionProgress.value.coerceIn(0f, 1f)\n    // Second-stage transition",
    1,
)

# Artwork: apply the second-stage transform to the existing artwork instance. The modifier is
# changed in-place, so the artwork remains part of PlayerSheet rather than becoming a new sheet.
artwork_pattern = re.compile(
    r'''(TrackArtworkImage\(\s*track\s*=\s*track,\s*contentDescription\s*=\s*track\.title,\s*contentScale\s*=\s*[^,]+,\s*modifier\s*=\s*Modifier\.fillMaxSize\(\))''',
    re.S,
)
art_match = artwork_pattern.search(s)
if not art_match:
    raise SystemExit("primary TrackArtworkImage block not found")
art_block = art_match.group(1)
art_replacement = art_block.replace(
    "modifier = Modifier.fillMaxSize()",
    """modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Collapsed and first queue-reveal stages remain visually unchanged.
                            // Only the second drag moves the artwork toward the upper-left.
                            val t = queueFocusProgress
                            val eased = t * t * (3f - 2f * t)
                            scaleX = 1f - (0.77f * eased)
                            scaleY = 1f - (0.77f * eased)
                            translationX = -((size.width * 0.34f) * eased)
                            translationY = -((size.height * 0.22f) * eased)
                        }"""
)
s = s[:art_match.start()] + art_replacement + s[art_match.end():]

# Title/artist: find the first Box immediately following the existing song-title section and
# transform that existing text group as one unit. This keeps title and artist aligned beside
# the compact artwork rather than introducing a second metadata row.
marker = "// 3. SONG TITLE & ARTIST."
marker_index = s.find(marker)
if marker_index < 0:
    raise SystemExit("song title section marker not found")
box_index = s.find("Box(\n", marker_index)
if box_index < 0:
    # Some revisions use Box { instead of Box(\n.
    box_index = s.find("Box {", marker_index)
if box_index < 0:
    raise SystemExit("song title container not found")
modifier_index = s.find("modifier = Modifier", box_index)
if modifier_index < 0:
    raise SystemExit("song title container modifier not found")
insert_at = modifier_index + len("modifier = Modifier")
metadata_modifier = """\n                    .graphicsLayer {
                        val t = queueFocusProgress
                        val eased = t * t * (3f - 2f * t)
                        translationX = (72.dp.toPx() * eased)
                        translationY = -(48.dp.toPx() * eased)
                        scaleX = 1f - (0.06f * eased)
                        scaleY = 1f - (0.06f * eased)
                    }"""
s = s[:insert_at] + metadata_modifier + s[insert_at:]

# Add a semantic marker for tests/log inspection.
if "// VELVET SECOND-STAGE QUEUE TRANSITION" not in s:
    s = s.replace(
        "    val queueFocusProgress = ((expansionProgress.value - 1f) / 1f).coerceIn(0f, 1f)\n",
        "    // VELVET SECOND-STAGE QUEUE TRANSITION\n"
        "    val queueFocusProgress = ((expansionProgress.value - 1f) / 1f).coerceIn(0f, 1f)\n",
        1,
    )

path.write_text(s, encoding="utf-8")
print("Applied continuous second-stage artwork/title transition: stage 1 queue reveal -> stage 2 compact upper-left artwork with aligned metadata.")
