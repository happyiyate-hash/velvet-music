from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# The restored reference PlayerSheet uses bgTop/bgMidUpper/bgMidLower/bgBottom.
# Remove only stale names from the failed animated-background patch. Do not alter
# artwork geometry, controls, or the restored background renderer.
s = s.replace('animatedPlayerBackground', 'themeColors.bgBottom')
s = s.replace('playerBackgroundTop', 'themeColors.bgBottom')
s = s.replace('playerBackgroundBottom', 'themeColors.bgBottom')

if 'animatedPlayerBackground' in s or 'playerBackgroundTop' in s or 'playerBackgroundBottom' in s:
    raise SystemExit('Stale PlayerSheet background variable reference remains; refusing to commit.')

required = [
    'themeColors.bgTop',
    'themeColors.bgMidUpper',
    'themeColors.bgMidLower',
    'themeColors.bgBottom',
]
missing = [token for token in required if token not in s]
if missing:
    raise SystemExit(f'Restored reference background is missing: {missing}; refusing to commit.')

p.write_text(s, encoding='utf-8')
print('PlayerSheet cleanup verified: restored reference background palette is intact and stale names are removed.')
