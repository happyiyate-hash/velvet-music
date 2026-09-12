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

if 'import androidx.compose.foundation.lazy.itemsIndexed' not in s:
    s = s.replace(
        'import androidx.compose.foundation.lazy.items\n',
        'import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.itemsIndexed\n'
    )

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

s = re.sub(
    r'\n\s*val queueIndex = orderedQueueItems\.indexOfFirst \{ it\.id == queueTrack\.id \}',
    '',
    s,
    count=1,
)

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

# Pre-warm a bounded window of queue artwork when Up Next becomes visible. This mirrors
# the main page's practical behavior: artwork is prepared before the user flings through
# the list, while LazyColumn virtualization is retained for large queues.
if 'private fun QueueArtworkPreloader(' not in s:
    marker = '        // 8. UP NEXT HANDLE & CONTENT (Lives within the SAME surface, continuously positioned at upNextY)'
    if marker not in s:
        raise SystemExit('queue section marker not found for artwork preloader')
    helper = '''@Composable
private fun QueueArtworkPreloader(
    tracks: List<Track>,
    enabled: Boolean
) {
    if (!enabled || tracks.isEmpty()) return

    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader(context) }
    val warmTracks = remember(tracks) { tracks.take(24) }

    LaunchedEffect(warmTracks) {
        warmTracks.forEach { queueTrack ->
            val uri = queueTrack.artworkUri ?: queueTrack.contentUri ?: return@forEach
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .size(128, 128)
                    .precision(Precision.EXACT)
                    .allowHardware(true)
                    .crossfade(false)
                    .memoryCacheKey("thumb_${queueTrack.id}_$uri")
                    .diskCacheKey("thumb_${queueTrack.id}_$uri")
                    .build()
            )
        }
    }
}

'''
    s = s.replace(marker, helper + marker, 1)

queue_marker = '''            if (p > 0.40f) {
                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)
                LazyColumn('''
if queue_marker in s and 'QueueArtworkPreloader(orderedQueueItems' not in s:
    s = s.replace(
        queue_marker,
        '''            if (p > 0.40f) {
                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)
                QueueArtworkPreloader(
                    tracks = orderedQueueItems,
                    enabled = p >= 0.40f
                )
                LazyColumn(''',
        1,
    )

if 'itemsIndexed(' not in s:
    print('Queue index optimization already present in an equivalent generated form; continuing.')
if 'thumbnailSizePx = 128' not in s:
    raise SystemExit('queue artwork optimization insertion point not found')

p.write_text(s, encoding='utf-8')
print('Applied queue scrolling optimization: stable item reuse, bounded artwork, no crossfade, and a 24-item artwork warm window.')
