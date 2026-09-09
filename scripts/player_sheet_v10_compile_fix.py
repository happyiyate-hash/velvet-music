from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')
old = '''        val artworkIntrinsicSize = remember(track.coverResId) {\n            painterResource(track.coverResId).intrinsicSize\n        }'''
new = '''        # v10 retrigger: painterResource must be invoked directly from composition.\n        # Reading its intrinsic size is immediate and does not block the first render.\n        val artworkPainter = painterResource(track.coverResId)\n        val artworkIntrinsicSize = artworkPainter.intrinsicSize'''
# Kotlin comments are required in the replacement source; this Python comment marker is only
# part of the patch text and is converted below.
new = new.replace('# v10 retrigger:', '// v10 retrigger:').replace('# Reading', '// Reading')
if old not in s:
    raise SystemExit('v9 composable-in-remember block not found')
s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')
print('PlayerSheet v10 compile fix applied.')
