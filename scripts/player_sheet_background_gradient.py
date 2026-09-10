from pathlib import Path
import subprocess

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
reference = '64168ed5a67d52f116197def0f1873d065f51305'

# Start from the known-good PlayerSheet every time so this automation never stacks old patches.
subprocess.run(['git', 'checkout', reference, '--', str(p)], check=True)
s = p.read_text(encoding='utf-8')

# Use one artwork-derived surface color from top to bottom. ArtworkColorExtractor now exposes
# that same surface through all four background fields and darkBackground.
old_gradient = '''Brush.verticalGradient(
                    colors = listOf(
                        themeColors.bgTop,
                        themeColors.bgMidUpper,
                        themeColors.bgMidLower,
                        themeColors.bgBottom
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )'''
new_gradient = '''Brush.verticalGradient(
                    colors = listOf(
                        themeColors.darkBackground,
                        themeColors.darkBackground,
                        themeColors.darkBackground,
                        themeColors.darkBackground
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )'''
if old_gradient not in s:
    raise SystemExit('Expected PlayerSheet background gradient was not found; refusing to modify the file.')
s = s.replace(old_gradient, new_gradient, 1)

# Disable the separate atmospheric bloom so the page surface is genuinely uniform in brightness.
# Keep the renderer structure intact, but make every bloom stop transparent.
bloom_opaque = 'themeColors.atmosphericBloom.copy(alpha = 0.35f)'
bloom_soft = 'themeColors.atmosphericBloom.copy(alpha = 0.12f)'
if bloom_opaque not in s or bloom_soft not in s:
    raise SystemExit('Expected atmospheric bloom stops were not found; refusing to modify the file.')
s = s.replace(bloom_opaque, 'Color.Transparent', 1)
s = s.replace(bloom_soft, 'Color.Transparent', 1)

# The artwork brush must use the exact same surface color as the page. Because the page is now
# uniform, its fade can safely use darkBackground at every stop without creating a dark strip.
if 'themeColors.bgBottom' not in s:
    raise SystemExit('Expected artwork brush color reference was not found; refusing to modify the file.')
s = s.replace('themeColors.bgBottom', 'themeColors.darkBackground')

# Add a vertically flipped copy of the existing artwork brush at the absolute top of the screen.
# The fully opaque/end color is at y=0 (status-bar edge), then it fades downward into the artwork.
# This mirrors the existing bottom brush without changing any artwork geometry or controls.
anchor = '''        if (artworkFadeAlpha > 0f) {'''
if anchor not in s:
    raise SystemExit('Expected bottom artwork brush block was not found; refusing to modify the file.')

top_brush = '''        if (artworkFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = expArtX, y = expArtY)
                    .width(expArtWidth)
                    .height(expArtHeight * 0.24f)
                    .graphicsLayer { alpha = artworkFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to themeColors.darkBackground,
                                0.16f to themeColors.darkBackground.copy(alpha = 0.86f),
                                0.34f to themeColors.darkBackground.copy(alpha = 0.62f),
                                0.52f to themeColors.darkBackground.copy(alpha = 0.34f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.12f),
                                1.00f to Color.Transparent
                            )
                        )
                    )
                    .zIndex(1f)
            )
        }

'''
s = s.replace(anchor, top_brush + anchor, 1)

p.write_text(s, encoding='utf-8')
print('Applied uniform artwork-derived PlayerSheet background, matching bottom brush, and mirrored top brush.')
