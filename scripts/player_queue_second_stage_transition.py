from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

# If second-stage transition is already present, exit cleanly without modifying
if "queueFocusProgress" in s and "queueFocusArtworkShiftX" in s:
    print("Second-stage queue transition already present in PlayerSheet.kt; skipping patch.")
    raise SystemExit(0)

# The continuous queue controller is extended to a second, queue-focused stage:
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

# Keep all second-stage values in scope after the existing density declaration.
density_anchor = "    val queueDragDensity = LocalDensity.current\n"
if density_anchor not in s:
    raise SystemExit("queue drag density anchor not found")

if "val queueFocusProgress =" not in s:
    s = s.replace(
        density_anchor,
        density_anchor
        + "\n    // VELVET SECOND-STAGE QUEUE TRANSITION\n"
        + "    // 0..1 is the normal player-to-queue reveal; 1..2 focuses the queue.\n"
        + "    val queueFocusProgress = ((expansionProgress.value - 1f) / 1f).coerceIn(0f, 1f)\n"
        + "    val queueFocusArtworkShiftX = with(queueDragDensity) { 120.dp.toPx() }\n"
        + "    val queueFocusArtworkShiftY = with(queueDragDensity) { 70.dp.toPx() }\n"
        + "    val queueFocusMetadataShiftX = with(queueDragDensity) { 72.dp.toPx() }\n"
        + "    val queueFocusMetadataShiftY = with(queueDragDensity) { 48.dp.toPx() }\n",
        1,
    )

# IMPORTANT: do not replace complete Box/Column blocks. The continuous queue patch
# can change their surrounding structure. Inject only graphicsLayer modifiers into
# the existing modifier chains so the patch cannot create unmatched braces/parentheses.
artwork_size_anchor = "                                    .size(artSize)"
artwork_graphics = """                                    .graphicsLayer {
                                        val t = queueFocusProgress
                                        val eased = t * t * (3f - 2f * t)
                                        scaleX = 1f - (0.77f * eased)
                                        scaleY = 1f - (0.77f * eased)
                                        translationX = -(queueFocusArtworkShiftX * eased)
                                        translationY = -(queueFocusArtworkShiftY * eased)
                                    }"""

if "queueFocusArtworkShiftX" not in s:
    if artwork_size_anchor not in s:
        raise SystemExit("artwork size anchor not found")
    s = s.replace(
        artwork_size_anchor,
        artwork_size_anchor + "\n" + artwork_graphics,
        1,
    )

# Inject the metadata transform into the existing title/artist Column modifier.
metadata_padding_anchor = "                                .padding(horizontal = 24.dp),"
metadata_graphics = """                                .graphicsLayer {
                                    val t = queueFocusProgress
                                    val eased = t * t * (3f - 2f * t)
                                    translationX = queueFocusMetadataShiftX * eased
                                    translationY = -(queueFocusMetadataShiftY * eased)
                                    scaleX = 1f - (0.06f * eased)
                                    scaleY = 1f - (0.06f * eased)
                                },"""

if "queueFocusMetadataShiftX" not in s:
    if metadata_padding_anchor not in s:
        raise SystemExit("song metadata padding anchor not found")
    s = s.replace(
        metadata_padding_anchor,
        metadata_padding_anchor + "\n" + metadata_graphics,
        1,
    )

path.write_text(s, encoding="utf-8")
print("Applied second-stage queue-focused transition using modifier-only injections.")
