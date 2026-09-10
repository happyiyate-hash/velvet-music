from pathlib import Path
import subprocess

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
reference = '64168ed5a67d52f116197def0f1873d065f51305'

# Recover the complete PlayerSheet from the known-good reference before applying the narrowly
# requested artwork-fade change. This also repairs the accidental full-file overwrite.
subprocess.run(['git', 'checkout', reference, '--', str(p)], check=True)
s = p.read_text(encoding='utf-8')

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
                                0.50f to themeColors.bgBottom.copy(alpha = 0.40f),
                                0.80f to themeColors.bgBottom.copy(alpha = 0.90f),
                                1.00f to themeColors.bgBottom.copy(alpha = 1.00f)
                            )'''
if old not in s:
    raise SystemExit('Expected artwork fade block was not found; refusing to modify PlayerSheet.kt')
s = s.replace(old, new, 1)

# Extend the fade across the lower 40% of the expanded artwork instead of only the final 24%.
old_geometry = '''                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.76f))
                    .width(expArtWidth)
                    .height(expArtHeight * 0.24f)'''
new_geometry = '''                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.60f))
                    .width(expArtWidth)
                    .height(expArtHeight * 0.40f)'''
if old_geometry not in s:
    raise SystemExit('Expected artwork fade geometry was not found; refusing to modify PlayerSheet.kt')
s = s.replace(old_geometry, new_geometry, 1)

# Keep the existing artwork-derived PlayerSheet background renderer intact.
required = ['themeColors.bgTop', 'themeColors.bgMidUpper', 'themeColors.bgMidLower', 'themeColors.bgBottom']
missing = [token for token in required if token not in s]
if missing:
    raise SystemExit(f'Restored reference background is missing: {missing}; refusing to commit.')

p.write_text(s, encoding='utf-8')
print('PlayerSheet restored and artwork fade now uses a 40% deep multi-stage dissolve into the exact dynamic bottom background color.')
