from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Fix composable-in-remember issue from older revisions.
old_intrinsic = '''        val artworkIntrinsicSize = remember(track.coverResId) {
            painterResource(track.coverResId).intrinsicSize
        }'''
new_intrinsic = '''        // painterResource must be invoked directly from composition.
        val artworkPainter = painterResource(track.coverResId)
        val artworkIntrinsicSize = artworkPainter.intrinsicSize'''
if old_intrinsic in s:
    s = s.replace(old_intrinsic, new_intrinsic, 1)

# Keep BoxWithConstraints and PlayerSheet open through sections 7-10.
up_next_marker = '''\n/**
 * Up Next list track row component:'''
broken_scope = '''\n        }\n}\n\n/**
 * Up Next list track row component:'''
fixed_scope = '''\n        }\n    }\n}\n\n/**
 * Up Next list track row component:'''
if broken_scope in s:
    s = s.replace(broken_scope, fixed_scope, 1)

# Section 2: close the outer moving-artwork Box after the fade block.
fade_end = '''                    .zIndex(1f)\n            )\n        }\n\n        // 3. SONG TITLE & ARTIST.'''
if fade_end in s:
    s = s.replace(fade_end, '''                    .zIndex(1f)\n            )\n        }\n\n        // Close the outer moving-artwork Box opened for section 2.\n        }\n\n        // 3. SONG TITLE & ARTIST.''', 1)
else:
    raise SystemExit('Section 2 fade/outer artwork marker not found; refusing to modify PlayerSheet.kt')

p.write_text(s, encoding='utf-8')
print('PlayerSheet artwork scope compile fix applied.')
