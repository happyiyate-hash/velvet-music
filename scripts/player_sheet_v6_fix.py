from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# 1) Brighten the artwork-derived surface while retaining a slightly darker lower end.
text = re.sub(
    r'''        val playerBackgroundTop = Color\(\n            red = \(extractedBg\.red \* [0-9.]+f \+ [0-9.]+f\)\.coerceAtMost\(1f\),\n            green = \(extractedBg\.green \* [0-9.]+f \+ [0-9.]+f\)\.coerceAtMost\(1f\),\n            blue = \(extractedBg\.blue \* [0-9.]+f \+ [0-9.]+f\)\.coerceAtMost\(1f\),\n            alpha = 1f\n        \)\n        val playerBackgroundBottom = Color\(\n            red = \(playerBackgroundTop\.red \* [0-9.]+f\)\.coerceAtLeast\(0f\),\n            green = \(playerBackgroundTop\.green \* [0-9.]+f\)\.coerceAtLeast\(0f\),\n            blue = \(playerBackgroundTop\.blue \* [0-9.]+f\)\.coerceAtLeast\(0f\),\n            alpha = 1f\n        \)''',
    '''        val playerBackgroundTop = Color(\n            red = (extractedBg.red * 1.30f + 0.035f).coerceAtMost(1f),\n            green = (extractedBg.green * 1.30f + 0.035f).coerceAtMost(1f),\n            blue = (extractedBg.blue * 1.30f + 0.035f).coerceAtMost(1f),\n            alpha = 1f\n        )\n        val playerBackgroundBottom = Color(\n            red = (playerBackgroundTop.red * 0.90f).coerceAtLeast(0f),\n            green = (playerBackgroundTop.green * 0.90f).coerceAtLeast(0f),\n            blue = (playerBackgroundTop.blue * 0.90f).coerceAtLeast(0f),\n            alpha = 1f\n        )''',
    text,
    count=1,
)

# 2) Never crop the primary artwork. Preserve the complete source image inside its
#    allocated height instead of forcing cover/crop behavior.
text = text.replace(
    '''                TrackArtworkImage(\n                    track = track,\n                    contentDescription = track.title,\n                    contentScale = ContentScale.Crop,\n                    modifier = Modifier.fillMaxSize()\n                )''',
    '''                TrackArtworkImage(\n                    track = track,\n                    contentDescription = track.title,\n                    contentScale = ContentScale.Fit,\n                    modifier = Modifier.fillMaxSize()\n                )''',
    1,
)

# 3) Lock Previous/Play/Next to equal fixed slots around the exact screen center.
#    The buttons are 60dp wide, so their LEFT positions are center +/- spacing - 30dp.
old_controls = '''        val basePlayX = centerX - 38.dp\n        val basePrevX = basePlayX - 72.dp\n        val baseNextX = basePlayX + 88.dp\n\n        val expandedPlayX = centerX - 38.dp\n        val expandedPrevX = expandedPlayX - 46.dp\n        val expandedNextX = expandedPlayX + 92.dp\n        val expandedShuffleX = 12.dp\n        val expandedRepeatX = totalWidth - 64.dp\n\n        val prevX = lerp(basePrevX, expandedPrevX, controlT)\n        val playX = lerp(basePlayX, expandedPlayX, controlT)\n        val nextX = lerp(baseNextX, expandedNextX, controlT)'''
new_controls = '''        val playCenterX = centerX\n        val sideButtonCenterSpacing = 96.dp\n\n        val basePlayX = playCenterX - 38.dp\n        val basePrevX = playCenterX - sideButtonCenterSpacing - 30.dp\n        val baseNextX = playCenterX + sideButtonCenterSpacing - 30.dp\n\n        val expandedPlayX = playCenterX - 38.dp\n        val expandedPrevX = playCenterX - sideButtonCenterSpacing - 30.dp\n        val expandedNextX = playCenterX + sideButtonCenterSpacing - 30.dp\n        val expandedShuffleX = 12.dp\n        val expandedRepeatX = totalWidth - 64.dp\n\n        val prevX = lerp(basePrevX, expandedPrevX, controlT)\n        val playX = lerp(basePlayX, expandedPlayX, controlT)\n        val nextX = lerp(baseNextX, expandedNextX, controlT)'''
if old_controls not in text:
    raise SystemExit("Expected current playback control geometry was not found")
text = text.replace(old_controls, new_controls, 1)

# 4) Replace the artwork-bound fade with a fixed expanded-position dissolve. It stays painted
#    in the expanded artwork area while the image shrinks away, rather than disappearing with it.
old_fade_pattern = re.compile(
    r'''            // Thick lower dissolve\. It is completely absent at rest and on the compact\n            // thumbnail, and appears only while opening the large artwork state\.\n            val artworkFadeAlpha = if \(p <= 1f\) \{.*?\n            \}\n''',
    re.S,
)
fade_match = old_fade_pattern.search(text)
if not fade_match:
    raise SystemExit("Current artwork fade block was not found")
new_fade = '''        // Fixed expanded-state dissolve. The fade is not attached to the shrinking image.\n        // It starts only once the user has meaningfully opened the artwork, reaches full\n        // strength near the expanded state, and remains painted in that same place while\n        // the artwork moves back toward the compact thumbnail.\n        val artworkFadeAlpha = when {\n            p < 0.35f -> 0f\n            p < 0.78f -> ((p - 0.35f) / 0.43f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n        if (artworkFadeAlpha > 0f) {\n            Box(\n                modifier = Modifier\n                    .offset(x = 0.dp, y = expArtY + (expArtHeight * 0.50f))\n                    .width(expArtWidth)\n                    .height(expArtHeight * 0.50f)\n                    .graphicsLayer { alpha = artworkFadeAlpha }\n                    .background(\n                        Brush.verticalGradient(\n                            colorStops = arrayOf(\n                                0.00f to Color.Transparent,\n                                0.10f to playerBackgroundTop.copy(alpha = 0.05f),\n                                0.24f to playerBackgroundTop.copy(alpha = 0.16f),\n                                0.40f to playerBackgroundTop.copy(alpha = 0.38f),\n                                0.56f to playerBackgroundTop.copy(alpha = 0.62f),\n                                0.72f to playerBackgroundBottom.copy(alpha = 0.82f),\n                                0.86f to playerBackgroundBottom.copy(alpha = 0.95f),\n                                1.00f to playerBackgroundBottom\n                            )\n                        )\n                    )\n            )\n        }\n\n'''
text = text[:fade_match.start()] + new_fade + text[fade_match.end():]

# 5) The scrubber must remain above the dissolve during the artwork expansion, then fade only
#    during the actual queue takeover (stage 1 -> stage 2).
text = text.replace(
    '''        val progressComponentAlpha = (1f - (p / 1.05f)).coerceIn(0f, 1f)''',
    '''        val progressComponentAlpha = if (p <= 1f) {\n            1f\n        } else {\n            (1f - ((p - 1f) / 0.60f)).coerceIn(0f, 1f)\n        }''',
    1,
)

# Add a z-index to the existing progress Canvas so the scrubber is unambiguously above
# the artwork dissolve regardless of declaration order.
if "import androidx.compose.ui.zIndex" not in text:
    text = text.replace(
        "import androidx.compose.ui.unit.sp\n",
        "import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.zIndex\n",
        1,
    )
text = text.replace(
    '''                .height(8.dp)\n                .graphicsLayer { alpha = progressAlpha.coerceIn(0f, 1f) }''',
    '''                .height(8.dp)\n                .graphicsLayer { alpha = progressAlpha.coerceIn(0f, 1f) }\n                .zIndex(2f)''',
    1,
)

# Keep waveform/timestamps visible through stage 1; the complete progress component handles
# the later queue fade.
text = text.replace(
    '''        val movingWaveformAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)\n        val movingTimestampAlpha = (1f - (p / 0.58f)).coerceIn(0f, 1f)''',
    '''        val movingWaveformAlpha = if (p <= 1f) 1f else (1f - ((p - 1f) / 0.60f)).coerceIn(0f, 1f)\n        val movingTimestampAlpha = if (p <= 1f) 1f else (1f - ((p - 1f) / 0.60f)).coerceIn(0f, 1f)''',
    1,
)

path.write_text(text, encoding="utf-8")
print("PlayerSheet v6 fixes applied: centered primary controls, fixed dissolve timeline, progress above dissolve, and non-cropped artwork.")
