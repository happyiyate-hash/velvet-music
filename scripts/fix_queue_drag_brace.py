from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')
# The drag patch replaces the LazyColumn item body; remove the stale extra item-body brace.
s = s.replace(
    '                    }\n                    }\n                }\n            }\n        }\n\n        // 7. THREE-DOT SONG ACTION BOTTOM SHEET',
    '                    }\n                }\n            }\n        }\n\n        // 7. THREE-DOT SONG ACTION BOTTOM SHEET',
    1,
)
p.write_text(s, encoding='utf-8')
