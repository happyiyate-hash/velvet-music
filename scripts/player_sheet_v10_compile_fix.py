from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Fix the original composable-in-remember issue if an older version is present.
old_intrinsic = '''        val artworkIntrinsicSize = remember(track.coverResId) {
            painterResource(track.coverResId).intrinsicSize
        }'''
new_intrinsic = '''        // painterResource must be invoked directly from composition.
        val artworkPainter = painterResource(track.coverResId)
        val artworkIntrinsicSize = artworkPainter.intrinsicSize'''
if old_intrinsic in s:
    s = s.replace(old_intrinsic, new_intrinsic, 1)

# Keep the PlayerSheet/BoxWithConstraints scope correct at the end of the main sheet.
up_next_marker = '''\n/**
 * Up Next list track row component:'''
broken_scope = '''\n        }\n}\n\n/**
 * Up Next list track row component:'''
fixed_scope = '''\n        }\n    }\n}\n\n/**
 * Up Next list track row component:'''
if broken_scope in s:
    s = s.replace(broken_scope, fixed_scope, 1)
elif up_next_marker not in s:
    raise SystemExit('Up Next marker not found; refusing to modify PlayerSheet.kt')

# Section 2 opens a Box around the moving artwork. The fade layer is nested inside
# that Box, so one additional brace is required after the fade's closing if-block.
artwork_box_marker = '''                    .zIndex(1f)\n            )\n        }\n\n        // 3. SONG TITLE & ARTIST.'''
artwork_box_fixed = '''                    .zIndex(1f)\n            )\n        }\n        }\n\n        // 3. SONG TITLE & ARTIST.'''
if artwork_box_marker in s:
    s = s.replace(artwork_box_marker, artwork_box_fixed, 1)
elif '''                    .zIndex(1f)\n            )\n        }\n        }\n\n        // 3. SONG TITLE & ARTIST.''' not in s:
    raise SystemExit('Artwork Box scope marker not found; refusing to modify PlayerSheet.kt')

p.write_text(s, encoding='utf-8')
print('PlayerSheet compile scope fix applied.')
