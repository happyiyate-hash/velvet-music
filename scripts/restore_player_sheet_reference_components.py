from pathlib import Path

PLAYER = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
COLORS = Path('app/src/main/java/com/example/media/ArtworkColorExtractor.kt')

s = PLAYER.read_text(encoding='utf-8')

# Reference commit: 80b5ec8847c72e1ee47321ce198e0c610e2092c8.
# Restore only the requested rendering pieces; do not replace the entire PlayerSheet.
start = s.index('    // Pure dynamic artwork color for the entire PlayerSheet surface.')
end_marker = '    ) {\n        val totalHeight = maxHeight'
end = s.index(end_marker, start) + len('    ) {')
reference_background = '''    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.darkBackground)
            .testTag("full_player_sheet")
    ) {
        // Reference background rendering restored from commit 80b5ec8.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        themeColors.bgTop,
                        themeColors.bgMidUpper,
                        themeColors.bgMidLower,
                        themeColors.bgBottom
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.atmosphericBloom.copy(alpha = 0.35f),
                        themeColors.atmosphericBloom.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.28f),
                    radius = canvasWidth * 0.85f
                )
            )
        }'''
s = s[:start] + reference_background + s[end:]

old_play = '''            Box(
                modifier = Modifier
                    .offset(x = playX, y = -4.dp)
                    .size(76.dp)
                    .shadow(10.dp, CircleShape, spotColor = themeColors.accent.copy(alpha = 0.34f))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(themeColors.playPauseGradTop, themeColors.playPauseGradBottom)))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTogglePlayPause
                    )
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }'''
reference_play = '''            Box(
                modifier = Modifier
                    .offset(x = playX, y = -4.dp)
                    .size(78.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = themeColors.accent.copy(alpha = 0.40f),
                        ambientColor = themeColors.darkBackground
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                themeColors.playPauseGradTop,
                                themeColors.playPauseGradBottom
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                themeColors.accent.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTogglePlayPause
                    )
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }'''
if old_play not in s:
    raise SystemExit('Reference play/pause block target not found; refusing to modify PlayerSheet.')
s = s.replace(old_play, reference_play, 1)
s = s.replace('contentScale = if (isTallArtwork) ContentScale.FillHeight else ContentScale.Fit,', 'contentScale = ContentScale.Crop,', 1)
s = s.replace('import androidx.compose.animation.animateColorAsState\n', '', 1)
PLAYER.write_text(s, encoding='utf-8')

# The reference and current extractor were identical, but its old logic selected the single
# most-saturated pixel and then forced saturation to >= 0.45. That can turn a mostly-white
# cover with a tiny red detail into a red theme. Use a quantized dominant cluster instead.
c = COLORS.read_text(encoding='utf-8')
start = c.index('    private fun sampleDominantColor')
end = c.index('\n    fun generateThemePalette', start)
new_sampler = '''    private fun sampleDominantColor(bitmap: Bitmap): Color? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return null

            val bins = HashMap<Int, Long>()
            val sumR = HashMap<Int, Long>()
            val sumG = HashMap<Int, Long>()
            val sumB = HashMap<Int, Long>()
            val stepX = max(1, width / 24)
            val stepY = max(1, height / 24)

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = (pixel ushr 24) and 0xFF
                    if (a < 128) continue
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = r * 0.299f + g * 0.587f + b * 0.114f
                    if (brightness < 18f || brightness > 245f) continue

                    val qr = r shr 4
                    val qg = g shr 4
                    val qb = b shr 4
                    val key = (qr shl 8) or (qg shl 4) or qb
                    bins[key] = (bins[key] ?: 0L) + 1L
                    sumR[key] = (sumR[key] ?: 0L) + r
                    sumG[key] = (sumG[key] ?: 0L) + g
                    sumB[key] = (sumB[key] ?: 0L) + b
                }
            }
            if (bins.isEmpty()) return null

            val bestKey = bins.keys.maxWithOrNull(
                compareBy<Int> { bins[it] ?: 0L }
                    .thenBy { key ->
                        val count = bins[key] ?: 1L
                        val r = (sumR[key] ?: 0L).toFloat() / count
                        val g = (sumG[key] ?: 0L).toFloat() / count
                        val b = (sumB[key] ?: 0L).toFloat() / count
                        val maxC = max(r, max(g, b))
                        val minC = min(r, min(g, b))
                        if (maxC > 0f) (maxC - minC) / maxC else 0f
                    }
            ) ?: return null

            val count = bins[bestKey] ?: return null
            return Color(
                (sumR[bestKey]!! / count).toInt().coerceIn(0, 255),
                (sumG[bestKey]!! / count).toInt().coerceIn(0, 255),
                (sumB[bestKey]!! / count).toInt().coerceIn(0, 255)
            )
        } catch (_: Exception) {
            return null
        }
    }
'''
c = c[:start] + new_sampler + c[end:]
c = c.replace('val sat = hsv[1].coerceIn(0.45f, 0.95f)', 'val sat = hsv[1].coerceIn(0f, 1f)', 1)
COLORS.write_text(c, encoding='utf-8')

print('Reference PlayerSheet background/play card/artwork rendering and color extraction fix applied.')
