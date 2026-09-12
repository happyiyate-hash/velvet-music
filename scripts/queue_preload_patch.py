from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text()
if 'import coil.ImageLoader' not in s:
    s = s.replace('import androidx.compose.ui.platform.LocalContext\n', 'import androidx.compose.ui.platform.LocalContext\nimport coil.ImageLoader\nimport coil.request.ImageRequest\nimport coil.size.Precision\n', 1)
if 'private fun QueueArtworkPreloader(' not in s:
    marker = '        // 8. UP NEXT HANDLE & CONTENT'
    helper = '''@Composable\nprivate fun QueueArtworkPreloader(tracks: List<Track>, enabled: Boolean) {\n    if (!enabled || tracks.isEmpty()) return\n    val context = LocalContext.current\n    val imageLoader = remember(context) { ImageLoader(context) }\n    val warmTracks = remember(tracks) { tracks.take(24) }\n    LaunchedEffect(warmTracks) {\n        warmTracks.forEach { queueTrack ->\n            val uri = queueTrack.artworkUri ?: queueTrack.contentUri ?: return@forEach\n            imageLoader.enqueue(ImageRequest.Builder(context).data(uri).size(128, 128).precision(Precision.EXACT).allowHardware(true).crossfade(false).memoryCacheKey("thumb_${queueTrack.id}_$uri").diskCacheKey("thumb_${queueTrack.id}_$uri").build())\n        }\n    }\n}\n\n'''
    s = s.replace(marker, helper + marker, 1)
needle = '''            if (p > 0.40f) {\n                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)\n                LazyColumn('''
if needle in s and 'QueueArtworkPreloader(' not in s[s.index(needle):s.index(needle)+500]:
    s = s.replace(needle, '''            if (p > 0.40f) {\n                val listAlpha = ((p - 0.40f) / 0.40f).coerceIn(0f, 1f)\n                QueueArtworkPreloader(orderedQueueItems, p >= 0.40f)\n                LazyColumn(''', 1)
s = s.replace('''                        .fillMaxSize()\n                        .graphicsLayer { alpha = listAlpha }\n                        .padding(horizontal = 14.dp),''','''                        .fillMaxSize()\n                        .graphicsLayer { alpha = listAlpha },''',1)
p.write_text(s)
