from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Fix the original composable-in-remember issue if an older version is present.
old_intrinsic = '''        val artworkIntrinsicSize = remember(track.coverResId) {\n            painterResource(track.coverResId).intrinsicSize\n        }'''
new_intrinsic = '''        // painterResource must be invoked directly from composition.\n        val artworkPainter = painterResource(track.coverResId)\n        val artworkIntrinsicSize = artworkPainter.intrinsicSize'''
if old_intrinsic in s:
    s = s.replace(old_intrinsic, new_intrinsic, 1)

# The v9 patch accidentally closed PlayerSheet before action sheets/dialogs.
# Keep BoxWithConstraints closed, but keep sections 7-10 inside PlayerSheet.
marker = '''        }\n    }\n\n        // 7. THREE-DOT SONG ACTION BOTTOM SHEET'''
replacement = '''        }\n\n        // 7. THREE-DOT SONG ACTION BOTTOM SHEET'''
if marker in s:
    s = s.replace(marker, replacement, 1)
else:
    # Make the script idempotent when the scope fix has already been applied.
    if '''        // 7. THREE-DOT SONG ACTION BOTTOM SHEET''' not in s:
        raise SystemExit('PlayerSheet action-sheet marker not found')

p.write_text(s, encoding='utf-8')
print('PlayerSheet scope/compile fix applied.')
