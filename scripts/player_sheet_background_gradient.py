from pathlib import Path
import re
import subprocess

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
reference = '64168ed5a67d52f116197def0f1873d065f51305'

# Always start from the known-good PlayerSheet so this workflow changes only the requested
# background/brush behavior and does not accumulate previous patch attempts.
subprocess.run(['git', 'checkout', reference, '--', str(p)], check=True)
s = p.read_text(encoding='utf-8')

# Velvet now uses ONE artwork-derived surface color from the top of PlayerSheet to the bottom.
# ArtworkColorExtractor supplies that same color through bgTop/bgMidUpper/bgMidLower/bgBottom.
# Repeating the same value here removes every top-to-bottom brightness shift.
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

# Remove the extra atmospheric bloom from the page surface. Even a subtle bloom would create a
# second brightness/color layer, defeating the purpose of a completely uniform background.
radial_pattern = re.compile(
    r'''\n            drawRect\(\n                brush = Brush\.radialGradient\(.*?\n                \)\n            \)''',
    re.DOTALL,
)
s, removed = radial_pattern.subn('', s, count=1)
if removed != 1:
    raise SystemExit('Expected atmospheric radial background layer was not found; refusing to modify the file.')

# The artwork fade/brush must use the same surface color too. Keep the existing fade geometry,
# but replace every independent bgBottom reference in the artwork fade with darkBackground.
fade_marker = 'val artworkFadeAlpha = when {'
fade_start = s.find(fade_marker)
if fade_start == -1:
    raise SystemExit('Artwork fade section was not found; refusing to modify the file.')

# Only replace references after the fade starts so unrelated controls/colors are untouched.
fade_tail = s[fade_start:]
if 'themeColors.bgBottom' not in fade_tail:
    raise SystemExit('Expected artwork fade color reference was not found; refusing to modify the file.')
fade_tail = fade_tail.replace('themeColors.bgBottom', 'themeColors.darkBackground')
s = s[:fade_start] + fade_tail

p.write_text(s, encoding='utf-8')
print('Applied uniform artwork-derived PlayerSheet background and matching artwork brush.')
