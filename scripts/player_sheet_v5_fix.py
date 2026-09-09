from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# Brighter artwork-derived player surface: noticeably lighter at the top, with only a
# gentle darkening toward the bottom.
text = text.replace(
'''        val playerBackgroundTop = Color(
            red = (extractedBg.red * 1.18f + 0.025f).coerceAtMost(1f),
            green = (extractedBg.green * 1.18f + 0.025f).coerceAtMost(1f),
            blue = (extractedBg.blue * 1.18f + 0.025f).coerceAtMost(1f),
            alpha = 1f
        )
        val playerBackgroundBottom = Color(
            red = (playerBackgroundTop.red * 0.88f).coerceAtLeast(0f),
            green = (playerBackgroundTop.green * 0.88f).coerceAtLeast(0f),
            blue = (playerBackgroundTop.blue * 0.88f).coerceAtLeast(0f),
            alpha = 1f
        )''',
'''        val playerBackgroundTop = Color(
            red = (extractedBg.red * 1.30f + 0.035f).coerceAtMost(1f),
            green = (extractedBg.green * 1.30f + 0.035f).coerceAtMost(1f),
            blue = (extractedBg.blue * 1.30f + 0.035f).coerceAtMost(1f),
            alpha = 1f
        )
        val playerBackgroundBottom = Color(
            red = (playerBackgroundTop.red * 0.90f).coerceAtLeast(0f),
            green = (playerBackgroundTop.green * 0.90f).coerceAtLeast(0f),
            blue = (playerBackgroundTop.blue * 0.90f).coerceAtLeast(0f),
            alpha = 1f
        )''', 1)

# Replace the fade that was INSIDE the artwork with a fixed expanded-state fade layer.
# This is the key behavior: when the artwork shrinks toward the compact thumbnail, the
# dissolve remains in the expanded position instead of shrinking/disappearing with the image.
inner_fade = '''            // Thick lower dissolve. It is completely absent at rest and on the compact
            // thumbnail, and appears only while opening the large artwork state.
            val artworkFadeAlpha = if (p <= 1f) {
                ((p - 0.08f) / 0.55f).coerceIn(0f, 1f)
            } else {
                0f
            }
            if (artworkFadeAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = artworkFadeAlpha }
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.50f to Color.Transparent,
                                    0.56f to playerBackgroundTop.copy(alpha = 0.04f),
                                    0.62f to playerBackgroundTop.copy(alpha = 0.16f),
                                    0.69f to playerBackgroundTop.copy(alpha = 0.36f),
                                    0.76f to playerBackgroundTop.copy(alpha = 0.58f),
                                    0.84f to playerBackgroundBottom.copy(alpha = 0.78f),
                                    0.91f to playerBackgroundBottom.copy(alpha = 0.93f),
                                    0.97f to playerBackgroundBottom.copy(alpha = 0.98f),
                                    1.00f to playerBackgroundBottom
                                )
                            )
                        )
                )
            }
'''
if inner_fade not in text:
    raise SystemExit("Expected inner artwork fade block was not found")
text = text.replace(inner_fade, "", 1)

fixed_fade = '''        // Fixed expanded artwork dissolve. It belongs to the player surface, NOT to the
        // artwork's shrinking box. It is absent at rest, reaches full strength by the
        // expanded state, and remains in place while the artwork shrinks back to the compact
        // thumbnail so the image visually blends back into the same painted dissolve.
        val artworkFadeAlpha = when {
            p < 0.45f -> 0f
            p < 0.80f -> ((p - 0.45f) / 0.35f).coerceIn(0f, 1f)
            else -> 1f
        }
        if (artworkFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = expArtY + (expArtHeight * 0.52f))
                    .width(expArtWidth)
                    .height(expArtHeight * 0.48f)
                    .graphicsLayer { alpha = artworkFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.12f to playerBackgroundTop.copy(alpha = 0.08f),
                                0.24f to playerBackgroundTop.copy(alpha = 0.20f),
                                0.38f to playerBackgroundTop.copy(alpha = 0.42f),
                                0.52f to playerBackgroundTop.copy(alpha = 0.64f),
                                0.68f to playerBackgroundBottom.copy(alpha = 0.82f),
                                0.82f to playerBackgroundBottom.copy(alpha = 0.94f),
                                1.00f to playerBackgroundBottom
                            )
                        )
                    )
            )
        }

'''
anchor = "        // 3. SONG TITLE & ARTIST."
if fixed_fade not in text:
    if anchor not in text:
        raise SystemExit("Song title anchor not found")
    text = text.replace(anchor, fixed_fade + anchor, 1)

# Explicitly define previous/next by their BUTTON CENTERS around the play center. This avoids
# asymmetric left/right offsets and keeps both controls locked to equal slots throughout the drag.
old_controls = '''        val basePlayX = centerX - 38.dp
        val basePrevX = basePlayX - 72.dp
        val baseNextX = basePlayX + 88.dp

        val expandedPlayX = centerX - 38.dp
        val expandedPrevX = expandedPlayX - 76.dp
        val expandedNextX = expandedPlayX + 92.dp

        val prevX = lerp(basePrevX, expandedPrevX, controlT)
        val playX = lerp(basePlayX, expandedPlayX, controlT)
        val nextX = lerp(baseNextX, expandedNextX, controlT)
        val shuffleX = lerp(24.dp, expandedShuffleX, controlT)
        val repeatX = lerp(totalWidth - 72.dp, expandedRepeatX, controlT)'''
new_controls = '''        val playCenterX = centerX
        val baseSideSpacing = 84.dp
        val expandedSideSpacing = 84.dp

        val basePlayX = playCenterX - 38.dp
        val basePrevX = (playCenterX - baseSideSpacing) - 30.dp
        val baseNextX = (playCenterX + baseSideSpacing) - 30.dp

        val expandedPlayX = playCenterX - 38.dp
        val expandedPrevX = (playCenterX - expandedSideSpacing) - 30.dp
        val expandedNextX = (playCenterX + expandedSideSpacing) - 30.dp

        val prevX = lerp(basePrevX, expandedPrevX, controlT)
        val playX = lerp(basePlayX, expandedPlayX, controlT)
        val nextX = lerp(baseNextX, expandedNextX, controlT)
        val shuffleX = lerp(24.dp, expandedShuffleX, controlT)
        val repeatX = lerp(totalWidth - 72.dp, expandedRepeatX, controlT)'''
if old_controls not in text:
    raise SystemExit("Expected playback control geometry was not found")
text = text.replace(old_controls, new_controls, 1)

# Keep the real progress line above the dissolve during stage 0->1, then fade it only as
# the queue takes over during stage 1->2.
old_progress = '''        val progressComponentAlpha = (1f - (p / 1.05f)).coerceIn(0f, 1f)
        if (progressComponentAlpha > 0f) {'''
new_progress = '''        val progressComponentAlpha = if (p <= 1f) {
            1f
        } else {
            (1f - ((p - 1f) / 0.55f)).coerceIn(0f, 1f)
        }
        if (progressComponentAlpha > 0f) {'''
if old_progress not in text:
    raise SystemExit("Progress alpha block was not found")
text = text.replace(old_progress, new_progress, 1)

path.write_text(text, encoding="utf-8")
print("PlayerSheet v5 fixes applied")
