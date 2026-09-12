from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text()

# Remove accidental unused/incorrect imports if present.
for line in [
    'import android.graphics.drawable.Drawable\n',
    'import coil.size.Size\n',
]:
    text = text.replace(line, '')

if 'import coil.ImageLoader' not in text:
    marker = 'import androidx.compose.ui.platform.LocalContext\n'
    imports = 'import coil.ImageLoader\nimport coil.request.ImageRequest\nimport coil.size.Precision\n'
    text = text.replace(marker, marker + imports, 1)

if 'private fun QueueArtworkPreloader(' not in text:
    needle = '        // 8. UP NEXT HANDLE & CONTENT (Lives within the SAME surface, continuously positioned at upNextY)'
    helper = '''@Composable\nprivate fun QueueArtworkPreloader(\n    tracks: List<Track>,\n    enabled: Boolean\n) {\n    if (!enabled || tracks.isEmpty()) return\n\n    val context = LocalContext.current\n    val imageLoader = remember(context) { ImageLoader(context) }\n    val warmTracks = remember(tracks) { tracks.take(24) }\n\n    LaunchedEffect(warmTracks) {\n        warmTracks.forEach { queueTrack ->\n            val uri = queueTrack.artworkUri ?: queueTrack.contentUri ?: return@forEach\n            val request = ImageRequest.Builder(context)\n                .data(uri)\n                .size(128, 128)\n                .precision(Precision.EXACT)\n                .allowHardware(true)\n                .crossfade(false)\n                .memoryCacheKey("thumb_${queueTrack.id}_$uri")\n                .diskCacheKey("thumb_${queueTrack.id}_$uri")\n                .build()\n            imageLoader.enqueue(request)\n        }\n    }\n}\n\n'''
    if needle not in text:
        raise SystemExit('queue section marker not found')
    text = text.replace(needle, helper + needle, 1)

old = '''            if (p > 0.40f) {\n                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)\n                LazyColumn('''
new = '''            if (p > 0.40f) {\n                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)\n                QueueArtworkPreloader(\n                    tracks = orderedQueueItems,\n                    enabled = p >= 0.40f\n                )\n                LazyColumn('''
if old not in text:
    raise SystemExit('queue lazy column marker not found')
text = text.replace(old, new, 1)

text = text.replace('''                        .fillMaxSize()\n                        .graphicsLayer { alpha = listAlpha }\n                        .padding(horizontal = 14.dp),''','''                        .fillMaxSize()\n                        .graphicsLayer { alpha = listAlpha },''',1)

path.write_text(text)
print('Applied queue artwork preload')
