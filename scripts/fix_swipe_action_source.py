from pathlib import Path
import re

# Keep this repair pass before the generated swipe patch so the Kotlin source boundaries stay valid.
p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Older queue patch revisions could fuse the row documentation into the @Composable annotation.
s = re.sub(
    r'/\*\*\n \* Up Next list track row component:\n.*?@Composable\nprivate fun UpNextTrackRow\(',
    '/**\n * Up Next list track row component:\n * Clean, modern row displaying track art thumbnail, title, artist & duration,\n * playing indicator badge if active, and sleek reorder handle.\n */\n@Composable\nprivate fun UpNextTrackRow(',
    s,
    count=1,
    flags=re.DOTALL,
)

# Remove the stale duplicate tail that older queue patches could leave before AnimatedPlayingBars.
s = s.replace(
    '\nound(Color.White.copy(alpha = .92f)))\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun AnimatedPlayingBars',
    '\n@Composable\nprivate fun AnimatedPlayingBars',
    1,
)

p.write_text(s, encoding='utf-8')
print('repaired', p)
