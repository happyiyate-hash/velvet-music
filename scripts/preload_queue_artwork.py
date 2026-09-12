from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text()

# Add the queue artwork preloader imports once.
imports = '''import android.graphics.drawable.Drawable\nimport coil.ImageLoader\nimport coil.request.ImageRequest\nimport coil.size.Size\nimport coil.size.Precision\nimport androidx.compose.ui.platform.LocalContext\n'''
marker = 'import androidx.compose.ui.platform.LocalContext\n'
if 'import coil.ImageLoader' not in text:
    text = text.replace(marker, imports, 1)

# Insert a small preloader composable immediately before the queue section.
needle = '        // 8. UP NEXT HANDLE & CONTENT (Lives within the SAME surface, continuously positioned at upNextY)'
if 'fun QueueArtworkPreloader(' not in text:
    helper = r'''@Composable
private fun QueueArtworkPreloader(
    tracks: List<Track>,
    enabled: Boolean
) {
    if (!enabled || tracks.isEmpty()) return

    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader(context) }

    // Keep a small warm window rather than decoding the entire queue. The request is
    // identical to the queue thumbnail request so the row can hit memory cache directly.
    val warmTracks = remember(tracks) { tracks.take(24) }

    LaunchedEffect(warmTracks) {
        warmTracks.forEach { queueTrack ->
            val uri = queueTrack.artworkUri ?: queueTrack.contentUri ?: return@forEach
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(128, 128)
                .precision(Precision.EXACT)
                .allowHardware(true)
                .crossfade(false)
                .memoryCacheKey("thumb_${queueTrack.id}_$uri")
                .diskCacheKey("thumb_${queueTrack.id}_$uri")
                .build()
            imageLoader.enqueue(request)
        }
    }
}

'''
    text = text.replace(needle, helper + needle, 1)

# Add preloader just before the queue LazyColumn, without changing queue visuals.
old = '''            if (p > 0.40f) {
                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)
                LazyColumn('''
new = '''            if (p > 0.40f) {
                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)
                QueueArtworkPreloader(
                    tracks = orderedQueueItems,
                    enabled = p >= 0.40f
                )
                LazyColumn('''
if old not in text:
    raise SystemExit('queue insertion point not found')
text = text.replace(old, new, 1)

# Make the queue list consume the full width; keep bottom inset only.
text = text.replace('''                        .fillMaxSize()
                        .graphicsLayer { alpha = listAlpha }
                        .padding(horizontal = 14.dp),''','''                        .fillMaxSize()
                        .graphicsLayer { alpha = listAlpha },''',1)

path.write_text(text)
print('patched', path)
