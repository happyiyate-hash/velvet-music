from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

# Extend the same shared controller from one continuous expansion stage to two:
# 0 = collapsed, 1 = queue revealed, 2 = queue focused.
s = s.replace(
    "val next = (expansionProgress.value - (dragAmount / dragRangePx))\n                                            .coerceIn(0f, 1f)",
    "val next = (expansionProgress.value - (dragAmount / dragRangePx))\n                                            .coerceIn(0f, 2f)",
    1,
)

snap = '''val target = if (expansionProgress.value >= 0.48f) 1f else 0f'''
snap_new = '''val target = when {
                                                expansionProgress.value < 0.5f -> 0f
                                                expansionProgress.value < 1.5f -> 1f
                                                else -> 2f
                                            }'''
s = s.replace(snap, snap_new)

s = s.replace(
    '''expansionProgress.animateTo(
                                        if (expansionProgress.value >= 0.5f) 0f else 1f,''',
    '''expansionProgress.animateTo(
                                        when {
                                            expansionProgress.value < 0.5f -> 1f
                                            expansionProgress.value < 1.5f -> 2f
                                            else -> 0f
                                        },''',
    1,
)

anchor = "    val upNextP = expansionProgress.value\n"
if anchor not in s:
    raise SystemExit("continuous expansion anchor not found")
s = s.replace(
    anchor,
    "    val upNextP = expansionProgress.value.coerceIn(0f, 1f)\n"
    "    // VELVET SECOND-STAGE QUEUE TRANSITION\n"
    "    val queueFocusProgress = ((expansionProgress.value - 1f) / 1f).coerceIn(0f, 1f)\n"
    "    val queueFocusArtworkShiftX = with(queueDragDensity) { 120.dp.toPx() }\n"
    "    val queueFocusArtworkShiftY = with(queueDragDensity) { 70.dp.toPx() }\n"
    "    val queueFocusMetadataShiftX = with(queueDragDensity) { 72.dp.toPx() }\n"
    "    val queueFocusMetadataShiftY = with(queueDragDensity) { 48.dp.toPx() }\n",
    1,
)

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
                            val t = queueFocusProgress
                            val eased = t * t * (3f - 2f * t)
                            scaleX = 1f - (0.77f * eased)
                            scaleY = 1f - (0.77f * eased)
                            translationX = -(queueFocusArtworkShiftX * eased)
                            translationY = -(queueFocusArtworkShiftY * eased)
                        }"""
)
s = s[:art_match.start()] + art_replacement + s[art_match.end():]

marker = "// 3. SONG TITLE & ARTIST."
marker_index = s.find(marker)
if marker_index < 0:
    raise SystemExit("song title section marker not found")
box_index = s.find("Box(\n", marker_index)
if box_index < 0:
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
                        translationX = queueFocusMetadataShiftX * eased
                        translationY = -(queueFocusMetadataShiftY * eased)
                        scaleX = 1f - (0.06f * eased)
                        scaleY = 1f - (0.06f * eased)
                    }"""
s = s[:insert_at] + metadata_modifier + s[insert_at:]

path.write_text(s, encoding="utf-8")
print("Applied density-safe second-stage artwork/title transition.")
