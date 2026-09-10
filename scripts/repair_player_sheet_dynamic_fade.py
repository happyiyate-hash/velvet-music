from pathlib import Path
import subprocess

player = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
reference = '64168ed5a67d52f116197def0f1873d065f51305'

# Recover the complete PlayerSheet from the last known-good reference before the accidental overwrite.
subprocess.run(['git', 'checkout', reference, '--', str(player)], check=True)
s = player.read_text(encoding='utf-8')

# The PlayerSheet background is already derived from the artwork. Make the artwork dissolve use
# the exact same dynamic bottom color that the background renderer uses, with no independent
# black/default color. Keep the rest of the PlayerSheet unchanged.
old = '''                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.16f to themeColors.bgBottom.copy(alpha = 0.12f),
                                0.34f to themeColors.bgBottom.copy(alpha = 0.34f),
                                0.52f to themeColors.bgBottom.copy(alpha = 0.62f),
                                0.72f to themeColors.bgBottom.copy(alpha = 0.86f),
                                1.00f to themeColors.bgBottom
                            )'''
new = '''                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.60f to themeColors.bgBottom.copy(alpha = 0.50f),
                                1.00f to themeColors.bgBottom.copy(alpha = 1.00f)
                            )'''
if old not in s:
    raise SystemExit('Expected artwork fade block was not found; refusing to modify PlayerSheet.kt')
s = s.replace(old, new, 1)
player.write_text(s, encoding='utf-8')
print('PlayerSheet restored and artwork fade synchronized with the detected dynamic background color.')
