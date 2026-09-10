from pathlib import Path
import subprocess

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
reference = '64168ed5a67d52f116197def0f1873d065f51305'

# Recover the complete PlayerSheet from the known-good reference before applying the narrowly
# requested artwork-fade change. This also repairs the accidental full-file overwrite.
subprocess.run(['git', 'checkout', reference, '--', str(p)], check=True)
s = p.read_text(encoding='utf-8')

# The visible defect is NOT a missing/weak fade. The fade layer itself was painted with
# themeColors.bgBottom, while the actual page surface underneath is a four-stop vertical
# gradient whose color depends on the layer's absolute Y position. As the sheet moves, the
# underlying color changes but this extra brush remained the same dark bgBottom color.
# Fix that by making every fade stop use the exact background-gradient color at its absolute
# screen Y. Alpha only controls how quickly that already-matching surface replaces the art.
helper_import = 'import androidx.compose.ui.graphics.Color\n'
if helper_import not in s:
    raise SystemExit('Expected Color import was not found; refusing to modify PlayerSheet.kt')

helper = '''\nprivate fun playerBackgroundColorAtY(\n    yPx: Float,\n    totalHeightPx: Float,\n    themeColors: com.example.media.TrackThemeColors\n): Color {\n    val t = (yPx / totalHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)\n    return when {\n        t <= 1f / 3f -> {\n            val local = t * 3f\n            Color(\n                red = themeColors.bgTop.red + (themeColors.bgMidUpper.red - themeColors.bgTop.red) * local,\n                green = themeColors.bgTop.green + (themeColors.bgMidUpper.green - themeColors.bgTop.green) * local,\n                blue = themeColors.bgTop.blue + (themeColors.bgMidUpper.blue - themeColors.bgTop.blue) * local,\n                alpha = 1f\n            )\n        }\n        t <= 2f / 3f -> {\n            val local = (t - 1f / 3f) * 3f\n            Color(\n                red = themeColors.bgMidUpper.red + (themeColors.bgMidLower.red - themeColors.bgMidUpper.red) * local,\n                green = themeColors.bgMidUpper.green + (themeColors.bgMidLower.green - themeColors.bgMidUpper.green) * local,\n                blue = themeColors.bgMidUpper.blue + (themeColors.bgMidLower.blue - themeColors.bgMidUpper.blue) * local,\n                alpha = 1f\n            )\n        }\n        else -> {\n            val local = (t - 2f / 3f) * 3f\n            Color(\n                red = themeColors.bgMidLower.red + (themeColors.bgBottom.red - themeColors.bgMidLower.red) * local,\n                green = themeColors.bgMidLower.green + (themeColors.bgBottom.green - themeColors.bgMidLower.green) * local,\n                blue = themeColors.bgMidLower.blue + (themeColors.bgBottom.blue - themeColors.bgMidLower.blue) * local,\n                alpha = 1f\n            )\n        }\n    }\n}\n'''
if 'private fun playerBackgroundColorAtY(' not in s:
    marker = '/**\n * Now Playing screen reproduced'
    if marker not in s:
        raise SystemExit('PlayerSheet composable marker was not found; refusing to modify PlayerSheet.kt')
    s = s.replace(marker, helper + '\n' + marker, 1)

old = '''        // Fixed expanded-state dissolve. It is deliberately OUTSIDE the moving artwork\n        // box, so during collapse the picture leaves this area while the fade remains stable.\n        // Resting/mini states have no fade. In expanded mode, the final ~24% dissolves\n        // strongly into the player background without changing image height.\n        val artworkFadeAlpha = when {\n            p < 0.30f -> 0f\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n        if (artworkFadeAlpha > 0f) {\n            Box(\n                modifier = Modifier\n                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.76f))\n                    .width(expArtWidth)\n                    .height(expArtHeight * 0.24f)\n                    .graphicsLayer { alpha = artworkFadeAlpha }\n                    .background(\n                        Brush.verticalGradient(\n                            colorStops = arrayOf(\n                                0.00f to Color.Transparent,\n                                0.16f to themeColors.bgBottom.copy(alpha = 0.12f),\n                                0.34f to themeColors.bgBottom.copy(alpha = 0.34f),\n                                0.52f to themeColors.bgBottom.copy(alpha = 0.62f),\n                                0.72f to themeColors.bgBottom.copy(alpha = 0.86f),\n                                1.00f to themeColors.bgBottom\n                            )\n                        )\n                    )\n                    .zIndex(1f)\n            )\n        }'''

new = '''        // The fade is an OVERLAY of the same page background, not an independent dark color.\n        // The page background is a vertical gradient, so its RGB/brightness changes with absolute\n        // Y. Sample that exact gradient at each fade stop; this prevents the extra brush from\n        // becoming a fixed dark strip while the sheet is dragged.\n        val artworkFadeAlpha = when {\n            p < 0.30f -> 0f\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n        if (artworkFadeAlpha > 0f) {\n            val fadeStartY = with(density) { (expArtY + expArtHeight * 0.60f).toPx() }\n            val fadeHeightPx = with(density) { (expArtHeight * 0.40f).toPx() }\n            val pageHeightPx = with(density) { totalHeight.toPx() }\n            val c0 = playerBackgroundColorAtY(fadeStartY, pageHeightPx, themeColors)\n            val c50 = playerBackgroundColorAtY(fadeStartY + fadeHeightPx * 0.50f, pageHeightPx, themeColors)\n            val c80 = playerBackgroundColorAtY(fadeStartY + fadeHeightPx * 0.80f, pageHeightPx, themeColors)\n            val c100 = playerBackgroundColorAtY(fadeStartY + fadeHeightPx, pageHeightPx, themeColors)\n\n            Box(\n                modifier = Modifier\n                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.60f))\n                    .width(expArtWidth)\n                    .height(expArtHeight * 0.40f)\n                    .graphicsLayer { alpha = artworkFadeAlpha }\n                    .background(\n                        Brush.verticalGradient(\n                            colorStops = arrayOf(\n                                0.00f to Color.Transparent,\n                                0.50f to c50.copy(alpha = 0.40f),\n                                0.80f to c80.copy(alpha = 0.90f),\n                                1.00f to c100.copy(alpha = 1.00f)\n                            )\n                        )\n                    )\n                    .zIndex(1f)\n            )\n        }'''

if old not in s:
    raise SystemExit('Expected old artwork fade block was not found; refusing to modify PlayerSheet.kt')
s = s.replace(old, new, 1)

# Keep the existing artwork-derived page background renderer intact.
required = ['themeColors.bgTop', 'themeColors.bgMidUpper', 'themeColors.bgMidLower', 'themeColors.bgBottom']
missing = [token for token in required if token not in s]
if missing:
    raise SystemExit(f'Restored reference background is missing: {missing}; refusing to commit.')

p.write_text(s, encoding='utf-8')
print('PlayerSheet restored and artwork fade now samples the exact page background gradient at its absolute Y positions.')
