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

# PlayerSheet contains an outer BoxWithConstraints. The closing brace immediately
# before the Up Next helper must close that BoxWithConstraints, while the following
# brace closes PlayerSheet itself. Add the missing BoxWithConstraints brace only when
# the file is in the known broken state; keep sections 7-10 inside PlayerSheet.
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

p.write_text(s, encoding='utf-8')
print('PlayerSheet compile scope fix applied.')
