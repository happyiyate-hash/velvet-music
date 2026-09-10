from pathlib import Path
import re

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Restore the row documentation/comment boundary if an older queue patch left it fused to the function annotation.
s = re.sub(
    r'/\*\*\n \* Up Next list track row component:\n \* Clean, modern row displaying track art thumbnail, title, artist & duratio@Composable\nprivate fun UpNextTrackRow\(',
    '/**\n * Up Next list track row component:\n * Clean, modern row displaying track art thumbnail, title, artist & duration,\n * playing indicator badge if active, and sleek reorder handle.\n */\n@Composable\nprivate fun UpNextTrackRow(',
    s,
    count=1,
)

# Remove the stale duplicate tail that can remain between UpNextTrackRow and AnimatedPlayingBars.
s = s.replace(
    '\nound(Color.White.copy(alpha = .92f)))\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun AnimatedPlayingBars',
    '\n@Composable\nprivate fun AnimatedPlayingBars',
    1,
)

p.write_text(s, encoding='utf-8')
print('repaired', p)
