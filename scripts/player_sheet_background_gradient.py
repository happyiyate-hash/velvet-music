from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Compose exposes animateColorAsState from androidx.compose.animation, not animation.core.
s = s.replace(
    'import androidx.compose.animation.core.animateColorAsState',
    'import androidx.compose.animation.animateColorAsState'
)

# Keep the artwork fade as a separate visual layer, but make its colors come from the
# same pure dynamic background state. There must be no stale references to the removed
# darkened/neutral gradient variables.
s = s.replace('playerBackgroundTop', 'animatedPlayerBackground')
s = s.replace('playerBackgroundBottom', 'animatedPlayerBackground')

# If an older generated gradient block is still present, remove it. The actual sheet
# background is the opaque root .background(animatedPlayerBackground) assignment.
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

if 'import androidx.compose.animation.animateColorAsState' not in s:
    raise SystemExit('Valid animateColorAsState import is missing; refusing to commit.')
if 'playerBackgroundTop' in s or 'playerBackgroundBottom' in s:
    raise SystemExit('Stale background variable reference remains; refusing to commit.')
if '.background(animatedPlayerBackground)' not in s:
    raise SystemExit('Pure dynamic PlayerSheet root background binding is missing; refusing to commit.')

p.write_text(s, encoding='utf-8')
print('Fixed PlayerSheet animation import and stale background references; pure dynamic root binding preserved.')
