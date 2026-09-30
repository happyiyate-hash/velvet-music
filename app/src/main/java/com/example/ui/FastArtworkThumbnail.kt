package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.media.VelvetArtworkCache
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * YouTube Music Style Ultra-Fast Artwork Thumbnail:
 *
 * 1. Frame-0 Memory Hit: Checks VelvetArtworkCache RAM before anything else. If already
 *    cached from previous scrolling, renders immediately on frame 0 with ZERO placeholder.
 * 2. Clean Neutral Surface: Absolutely NO placeholder card, NO borders, and NO music icon.
 *    The image fades in smoothly and quickly directly over the row.
 * 3. In-Order Rapid Decoding: Newly exposed rows decode asynchronously on Dispatchers.IO,
 *    fading in quickly one-by-one from top to bottom.
 * 4. Bidirectional Caching: Scrolling down caches every visible thumbnail. Scrolling back up
 *    finds 100% hits in memory cache—never reverting to placeholders or re-decoding.
 */
@Composable
fun FastArtworkThumbnail(
    track: Track,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    thumbnailSizePx: Int = 128,
    contentDescription: String? = track.title
) {
    val context = LocalContext.current
    val imageLoader = remember { VelvetImageLoader.get(context) }

    // Instant frame-0 memory check: if track was already displayed, show it synchronously!
    val memoryBitmap: Bitmap? = remember(track.id) {
        VelvetArtworkCache.getFromMemory(track.id)
    }

    val request = remember(track.id, thumbnailSizePx) {
        ImageRequest.Builder(context)
            .data(track)
            .setParameter("is_thumbnail", true)
            .dispatcher(Dispatchers.IO)
            .crossfade(180)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCacheKey("track_${track.id}_$thumbnailSizePx")
            .diskCacheKey("track_${track.id}_$thumbnailSizePx")
            .size(thumbnailSizePx, thumbnailSizePx)
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .build()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF141418)),
        contentAlignment = Alignment.Center
    ) {
        if (memoryBitmap != null && !memoryBitmap.isRecycled) {
            // Instant zero-delay memory cache render
            Image(
                bitmap = memoryBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Asynchronous fade-in via Coil and TrackArtworkFetcher
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

object FastArtworkThumbnailDefaults {
    /**
     * Prefetches upcoming thumbnails into the dual memory and disk cache in background.
     */
    fun prefetch(context: Context, track: Track, thumbnailSizePx: Int = 128) {
        if (VelvetArtworkCache.getFromMemory(track.id) != null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VelvetArtworkCache.getOrDecodeThumbnail(context, track)
            } catch (_: Throwable) {}
        }
    }
}
