from pathlib import Path
import re

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Keep the queue truly edge-to-edge: the LazyColumn owns the continuous surface.
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

# Stable IDs plus contentType let Compose retain item identity and reuse the same row layout.
if 'import androidx.compose.foundation.lazy.itemsIndexed' not in s:
    s = s.replace(
        'import androidx.compose.foundation.lazy.items\n',
        'import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.itemsIndexed\n'
    )

# Prefer itemsIndexed so the row receives its position directly instead of doing an
# indexOfFirst scan for every visible item during recomposition.
needle = '''                        items(
                        items = orderedQueueItems,
                        key = { it.id }
                    ) { queueTrack ->'''
replacement = '''                        itemsIndexed(
                        items = orderedQueueItems,
                        key = { _, item -> item.id },
                        contentType = { _, _ -> "track_row" }
                    ) { queueIndex, queueTrack ->'''
if needle in s:
    s = s.replace(needle, replacement, 1)
else:
    s = re.sub(
        r'items\(\s*items\s*=\s*orderedQueueItems,\s*key\s*=\s*\{\s*it\.id\s*\}\s*\)\s*\{\s*queueTrack\s*->',
        'itemsIndexed(\n                        items = orderedQueueItems,\n                        key = { _, item -> item.id },\n                        contentType = { _, _ -> "track_row" }\n                    ) { queueIndex, queueTrack ->',
        s,
        count=1,
    )

# Remove the old per-row index scan if the generated revision still contains it.
s = re.sub(
    r'\n\s*val queueIndex = orderedQueueItems\.indexOfFirst \{ it\.id == queueTrack\.id \}',
    '',
    s,
    count=1,
)

# Queue artwork is only 48dp on screen. Bound Coil's decode to a small square and
# disable per-row crossfade so fast flings do not upload large bitmaps or animate them.
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

if 'itemsIndexed(' not in s or 'contentType = { _, _ -> "track_row" }' not in s:
    raise SystemExit('indexed queue insertion point not found')
if 'thumbnailSizePx = 128' not in s:
    raise SystemExit('queue artwork optimization insertion point not found')

p.write_text(s, encoding='utf-8')
print('Applied single-surface queue, stable indexed keys/contentType, bounded artwork decoding, and no crossfade.')
