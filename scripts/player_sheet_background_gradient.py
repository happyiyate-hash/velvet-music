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

p.write_text(s, encoding='utf-8')
print('Applied uniform artwork-derived PlayerSheet background and matching artwork brush.')
