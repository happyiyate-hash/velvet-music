from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Compose exposes animateColorAsState from androidx.compose.animation, not animation.core.
s = s.replace(
    'import androidx.compose.animation.core.animateColorAsState',
    'import androidx.compose.animation.animateColorAsState'
)

# The restored reference PlayerSheet owns the background palette as bgTop/bgMidUpper/
# bgMidLower/bgBottom. The artwork fade must use the restored bottom color, not the
# newer animatedPlayerBackground state that no longer exists in this reference layout.
s = s.replace('animatedPlayerBackground.copy(', 'themeColors.bgBottom.copy(')

# Remove any stale direct references left by the previous patch.
s = s.replace('playerBackgroundTop', 'themeColors.bgBottom')
s = s.replace('playerBackgroundBottom', 'themeColors.bgBottom')
s = s.replace('animatedPlayerBackground', 'themeColors.bgBottom')

# If an obsolete generated gradient block is still present, remove it. The restored
# reference background Canvas below is the authoritative PlayerSheet background.
old_gradient_start = '        // Dynamic player surface: lift the extracted artwork color so it is not nearly black.'
if old_gradient_start in s:
    start = s.index(old_gradient_start)
    end_marker = '        // 2. ARTWORK'
    if end_marker in s[start:]:
        end = s.index(end_marker, start)
        s = s[:start] + s[end:]

# Remove accidental duplicate imports while preserving the first valid one.
lines = s.splitlines(keepends=True)
out = []
seen_animation_import = False
for line in lines:
    if line.strip() == 'import androidx.compose.animation.animateColorAsState':
        if seen_animation_import:
            continue
        seen_animation_import = True
    out.append(line)
s = ''.join(out)

# The restored reference background is intentionally left intact.
if 'import androidx.compose.animation.animateColorAsState' in s:
    # animateColorAsState is not used by the restored reference after this cleanup;
    # remove the import so Kotlin does not carry dead code from the failed patch.
    s = s.replace('import androidx.compose.animation.animateColorAsState\n', '')

if 'playerBackgroundTop' in s or 'playerBackgroundBottom' in s or 'animatedPlayerBackground' in s:
    raise SystemExit('Stale PlayerSheet background variable reference remains; refusing to commit.')
if 'themeColors.bgTop' not in s or 'themeColors.bgMidUpper' not in s or 'themeColors.bgMidLower' not in s or 'themeColors.bgBottom' not in s:
    raise SystemExit('Restored reference background palette is missing; refusing to commit.')

p.write_text(s, encoding='utf-8')
print('Removed stale animated background references and preserved the restored reference background palette.')
