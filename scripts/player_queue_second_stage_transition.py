from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
s = path.read_text(encoding="utf-8")

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

# The density value is declared after upNextP in the base PlayerSheet. Insert all
# second-stage derived values after it so the generated Kotlin is always in scope.
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

# Transform the complete artwork frame, not just the image. This keeps the rounded
# frame, border and glass treatment moving together as one physical object.
artwork_frame = """                            Box(
                                modifier = Modifier
                                    .size(artSize)
                                    .shadow(
                                        elevation = artworkElevation,
                                        shape = artworkShape,
                                        spotColor = Color.Black.copy(alpha = 0.72f),
                                        ambientColor = Color.Black.copy(alpha = 0.45f)
                                    )
                                    .clip(artworkShape),
                                contentAlignment = Alignment.Center
                            ) {"""
artwork_frame_replacement = """                            Box(
                                modifier = Modifier
                                    .size(artSize)
                                    .graphicsLayer {
                                        val t = queueFocusProgress
                                        val eased = t * t * (3f - 2f * t)
                                        scaleX = 1f - (0.77f * eased)
                                        scaleY = 1f - (0.77f * eased)
                                        translationX = -(queueFocusArtworkShiftX * eased)
                                        translationY = -(queueFocusArtworkShiftY * eased)
                                    }
                                    .shadow(
                                        elevation = artworkElevation,
                                        shape = artworkShape,
                                        spotColor = Color.Black.copy(alpha = 0.72f),
                                        ambientColor = Color.Black.copy(alpha = 0.45f)
                                    )
                                    .clip(artworkShape),
                                contentAlignment = Alignment.Center
                            ) {"""
if artwork_frame in s:
    s = s.replace(artwork_frame, artwork_frame_replacement, 1)
elif "queueFocusArtworkShiftX" not in s:
    raise SystemExit("artwork frame anchor not found")

# Move the existing title/artist column as one unit. The exact base modifier is stable
# and this avoids fragile parenthesis scanning or malformed Kotlin generated source.
metadata_base = """                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.Start
                        ) {"""
metadata_replacement = """                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .graphicsLayer {
                                    val t = queueFocusProgress
                                    val eased = t * t * (3f - 2f * t)
                                    translationX = queueFocusMetadataShiftX * eased
                                    translationY = -(queueFocusMetadataShiftY * eased)
                                    scaleX = 1f - (0.06f * eased)
                                    scaleY = 1f - (0.06f * eased)
                                },
                            horizontalAlignment = Alignment.Start
                        ) {"""
if metadata_base in s:
    s = s.replace(metadata_base, metadata_replacement, 1)
elif "queueFocusMetadataShiftX" not in s:
    raise SystemExit("song metadata Column anchor not found")

path.write_text(s, encoding="utf-8")
print("Applied second-stage queue-focused artwork and title/artist transition.")
