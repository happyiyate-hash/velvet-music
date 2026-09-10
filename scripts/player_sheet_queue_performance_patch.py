from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Keep the queue truly edge-to-edge: padding belongs to row content, not the LazyColumn slot.
s = s.replace(
    '''                    LazyColumn(\n                    state = queueListState,\n                    userScrollEnabled = p >= 0.95f,\n                    modifier = Modifier\n                        .fillMaxSize()\n                        .graphicsLayer { alpha = listAlpha }\n                        .padding(horizontal = 14.dp),''',
    '''                    LazyColumn(\n                    state = queueListState,\n                    userScrollEnabled = p >= 0.95f,\n                    modifier = Modifier\n                        .fillMaxSize()\n                        .background(themeColors.darkBackground)\n                        .graphicsLayer { alpha = listAlpha },'''
)

# The current queue renderer already has stable IDs. Add contentType so Compose can reuse
# the same track-row composition efficiently as the queue scrolls.
needle = '''                        items(\n                        items = orderedQueueItems,\n                        key = { it.id }\n                    ) { queueTrack ->'''
replacement = '''                        items(\n                        items = orderedQueueItems,\n                        key = { it.id },\n                        contentType = { "track_row" }\n                    ) { queueTrack ->'''
if needle in s:
    s = s.replace(needle, replacement, 1)

# Queue artwork is a small 48dp thumbnail. Ask Coil for a bounded decode and skip the
# per-row crossfade; both reduce decode/upload work during fast flings without changing
# artwork appearance once settled.
s = s.replace(
    'TrackArtworkImage(track = track, contentDescription = track.title, modifier = Modifier.fillMaxSize())',
    'TrackArtworkImage(\n                    track = track,\n                    contentDescription = track.title,\n                    modifier = Modifier.fillMaxSize(),\n                    thumbnailSizePx = 128,\n                    crossfade = false\n                )',
    1
)

# Avoid accidental reapplication of this patch.
if 'contentType = { "track_row" }' not in s:
    raise SystemExit('queue contentType insertion point not found')

p.write_text(s, encoding='utf-8')
print('Applied queue LazyColumn performance optimizations.')
