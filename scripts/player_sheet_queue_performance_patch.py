from pathlib import Path
import re

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Keep the queue truly edge-to-edge: the LazyColumn owns the continuous surface.
# Row content may still have its own small internal spacing/artwork radius.
s = s.replace(
    '''                    LazyColumn(
                    state = queueListState,
                    userScrollEnabled = p >= 0.95f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = listAlpha }
                        .padding(horizontal = 14.dp),''',
    '''                    LazyColumn(
                    state = queueListState,
                    userScrollEnabled = p >= 0.95f,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColors.darkBackground)
                        .graphicsLayer { alpha = listAlpha },'''
)

# Stable identity is already used by the queue. Add contentType so Compose can reuse
# the same track-row layout type while items enter/leave the viewport.
needle = '''                        items(
                        items = orderedQueueItems,
                        key = { it.id }
                    ) { queueTrack ->'''
replacement = '''                        items(
                        items = orderedQueueItems,
                        key = { it.id },
                        contentType = { "track_row" }
                    ) { queueTrack ->'''
if needle in s:
    s = s.replace(needle, replacement, 1)

# Some generated PlayerSheet revisions use a different indentation. Handle the equivalent
# items block without touching unrelated LazyColumns.
if 'contentType = { "track_row" }' not in s:
    s = re.sub(
        r'(items\(\s*items\s*=\s*orderedQueueItems,\s*key\s*=\s*\{\s*it\.id\s*\}\s*)(\)\s*\{\s*queueTrack\s*->)',
        r'\1,\n                        contentType = { "track_row" }\2',
        s,
        count=1,
    )

# Queue artwork is only 48dp on screen. Bound Coil's decode to a small square and
# disable per-row crossfade so fast flings do not upload large bitmaps or run animations.
old_art = 'TrackArtworkImage(track = track, contentDescription = track.title, modifier = Modifier.fillMaxSize())'
new_art = '''TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    thumbnailSizePx = 128,
                    crossfade = false
                )'''
if old_art in s:
    s = s.replace(old_art, new_art, 1)

if 'contentType = { "track_row" }' not in s:
    raise SystemExit('queue contentType insertion point not found')
if 'thumbnailSizePx = 128' not in s:
    raise SystemExit('queue artwork optimization insertion point not found')

p.write_text(s, encoding='utf-8')
print('Applied queue LazyColumn, stable content type, single-surface, and bounded artwork optimizations.')
