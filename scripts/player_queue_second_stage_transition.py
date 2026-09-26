from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

# The continuous queue controller is deliberately extended rather than replaced:
# 0 = collapsed player, 1 = queue revealed, 2 = queue focused.
s = s.replace(
    "val next = (expansionProgress.value - (dragAmount / dragRangePx))\n                                            .coerceIn(0f, 1f)",
    "val next = (expansionProgress.value - (dragAmount / dragRangePx))\n                                            .coerceIn(0f, 2f)",
    1,
)

s = s.replace(
    "val target = if (expansionProgress.value >= 0.48f) 1f else 0f",
    """val target = when {
                                                expansionProgress.value < 0.5f -> 0f
                                                expansionProgress.value < 1.5f -> 1f
                                                else -> 2f
                                            }""",
    1,
)

s = s.replace(
    """if (expansionProgress.value >= 0.5f) 0f else 1f,""",
    """when {
                                            expansionProgress.value < 0.5f -> 1f
                                            expansionProgress.value < 1.5f -> 2f
                                            else -> 0f
                                        },""",
    1,
)

# Shared second-stage progress. Keep the first-stage queue reveal untouched.
anchor = "    val upNextP = expansionProgress.value\n"
if anchor not in s:
    anchor = "    val upNextP = expansionProgress.value.coerceIn(0f, 1f)\n"
if anchor not in s:
    raise SystemExit("continuous expansion anchor not found")

if "val queueFocusProgress =" not in s:
    s = s.replace(
        anchor,
        anchor
        + "    // VELVET SECOND-STAGE QUEUE TRANSITION\n"
        + "    val queueFocusProgress = ((expansionProgress.value - 1f) / 1f).coerceIn(0f, 1f)\n"
        + "    val queueFocusArtworkShiftX = with(queueDragDensity) { 120.dp.toPx() }\n"
        + "    val queueFocusArtworkShiftY = with(queueDragDensity) { 70.dp.toPx() }\n"
        + "    val queueFocusMetadataShiftX = with(queueDragDensity) { 72.dp.toPx() }\n"
        + "    val queueFocusMetadataShiftY = with(queueDragDensity) { 48.dp.toPx() }\n",
        1,
    )

# Locate the primary artwork after the artwork section marker. The source has evolved
# over several player-sheet fixes, so do not depend on an exact whitespace/layout form.
art_start = s.find("// 2. SINGLE PHYSICAL ARTWORK INSTANCE.")
if art_start < 0:
    art_start = 0
art_match = re.search(
    r"TrackArtworkImage\(\s*track\s*=\s*track,\s*contentDescription\s*=\s*track\.title,\s*contentScale\s*=\s*[^,]+,\s*modifier\s*=\s*Modifier\.fillMaxSize\(\)\s*\)",
    s[art_start:],
    re.S,
)
if not art_match:
    raise SystemExit("primary TrackArtworkImage block not found")
art_abs_start = art_start + art_match.start()
art_abs_end = art_start + art_match.end()
art_block = art_match.group(0)
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
                        }""",
)
s = s[:art_abs_start] + art_replacement + s[art_abs_end:]

# Move the title/artist block together. The old script looked for a historical
# "// 3. SONG TITLE & ARTIST." marker, but current PlayerSheet uses a keyed identity
# Column instead. Target that stable semantic marker instead.
metadata_marker = "// Song identity sits immediately under the artwork."
marker_index = s.find(metadata_marker)
if marker_index < 0:
    metadata_marker = "// 3. SONG TITLE & ARTIST."
    marker_index = s.find(metadata_marker)
if marker_index < 0:
    raise SystemExit("song identity section marker not found")

key_index = s.find("key(track.id)", marker_index)
if key_index < 0:
    raise SystemExit("song identity key block not found")
column_index = s.find("Column(", key_index)
if column_index < 0:
    raise SystemExit("song identity Column not found")

# Find the matching closing parenthesis for this Column invocation. Kotlin source here
# contains nested calls, so use a small balanced-parenthesis scanner.
def matching_paren(text: str, opening: int) -> int:
    depth = 0
    in_string = False
    escaped = False
    for i in range(opening, len(text)):
        ch = text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0:
                return i
    return -1

column_end = matching_paren(s, column_index + len("Column") - 1)
if column_end < 0:
    raise SystemExit("song identity Column closing parenthesis not found")

column_body = s[column_index:column_end + 1]
if "queueFocusMetadataShiftX" not in column_body:
    metadata_layer = """.graphicsLayer {
                                val t = queueFocusProgress
                                val eased = t * t * (3f - 2f * t)
                                translationX = queueFocusMetadataShiftX * eased
                                translationY = -(queueFocusMetadataShiftY * eased)
                                scaleX = 1f - (0.06f * eased)
                                scaleY = 1f - (0.06f * eased)
                            }"""
    # Prefer the existing Modifier chain if this Column has one.
    modifier_match = re.search(r"modifier\s*=\s*Modifier(?P<chain>(?:\n\s*\.[^\n]+)*)", column_body)
    if modifier_match:
        replacement = "modifier = Modifier" + modifier_match.group("chain") + "\n                            " + metadata_layer
        column_body = column_body[:modifier_match.start()] + replacement + column_body[modifier_match.end():]
    else:
        insert_at = column_body.rfind(")")
        params = "modifier = Modifier\n                            " + metadata_layer + "\n"
        if insert_at > 0 and column_body[insert_at - 1] != '(':
            params = ",\n                            " + params
        column_body = column_body[:insert_at] + params + column_body[insert_at:]
    s = s[:column_index] + column_body + s[column_end + 1:]

path.write_text(s, encoding="utf-8")
print("Applied robust second-stage artwork and title/artist transition.")
