from pathlib import Path
import subprocess

PLAYER = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
REFERENCE = '64168ed5a67d52f116197def0f1873d065f51305'

# Restore the complete known-good PlayerSheet before applying the narrowly requested fade fix.
subprocess.run(['git', 'checkout', REFERENCE, '--', str(PLAYER)], check=True)
s = PLAYER.read_text(encoding='utf-8')

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
    raise SystemExit('Expected artwork fade block not found; refusing to modify PlayerSheet.kt')

PLAYER.write_text(s.replace(old, new, 1), encoding='utf-8')
print('PlayerSheet restored and artwork fade synchronized with the existing artwork-derived background color.')
