package com.example.ui

import android.content.Context
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import kotlinx.coroutines.Dispatchers

/**
 * YouTube Music Style Ultra-Fast Artwork Thumbnail:
 *
 * 1. Fixed Dimensions: Space is reserved immediately (48.dp x 48.dp), preventing any layout shift.
 * 2. Cache-First Asynchronous Pipeline:
 *    - Every visible thumbnail starts loading immediately, including while the list is actively scrolling.
 *    - Memory-cached artwork is rendered immediately when the row is recreated.
 *    - Disk-cached artwork is reused without returning to a music-icon placeholder.
 * 3. Exact Downsampling: Downsamples to target thumbnail resolution (128x128 px), never allocating full 1000x1000 bitmaps.
 * 4. Gentle Fade-In: When new artwork is decoded, ONLY the image fades in smoothly; the row, title, and metadata never flicker.
 */
@Composable
fun FastArtworkThumbnail(
    track: Track,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    thumbnailSizePx: Int = 128,
    isScrolling: Boolean = false,
    contentDescription: String? = track.title
) {
    val context = LocalContext.current
    val imageLoader = remember { VelvetImageLoader.get(context) }

    val fallbackResId = remember(track.id, track.coverResId, track.title, track.artist) {
        if (track.coverResId != 0) {
            track.coverResId
        } else {
            FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
        }
    }

    val primaryData: Any = remember(track.artworkUri, fallbackResId) {
        val uri = track.artworkUri
        if (!uri.isNullOrBlank()) uri else fallbackResId
    }

    val cacheKey = remember(track.id, thumbnailSizePx) {
        "art_thumb_${track.id}_$thumbnailSizePx"
    }

    // Always enqueue immediately. Do NOT gate artwork behind scroll-idle state:
    // fast scrolling is exactly when newly exposed rows need to begin decoding.
    val request = remember(track.id, primaryData, fallbackResId, thumbnailSizePx) {
        ImageRequest.Builder(context)
            .data(primaryData)
            .error(fallbackResId)
            .fallback(fallbackResId)
            .dispatcher(Dispatchers.IO)
            .crossfade(220)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .size(thumbnailSizePx, thumbnailSizePx)
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .build()
    }

    // 1. Reserved fixed dimension container
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF1B1D23)),
        contentAlignment = Alignment.Center
    ) {
        // No icon, border, or card-style placeholder. The clean surface remains underneath
        // only until the actual artwork is available, then the artwork fades in.
        if (request != null) {
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
     * Prefetches upcoming thumbnails into the memory cache in the background.
     * Uses Dispatchers.IO and exact downsampling to keep memory usage minimal.
     */
    fun prefetch(context: Context, track: Track, thumbnailSizePx: Int = 128) {
        val loader = VelvetImageLoader.get(context)
        val fallbackResId = if (track.coverResId != 0) {
            track.coverResId
        } else {
            FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
        }
        val data: Any = if (!track.artworkUri.isNullOrBlank()) track.artworkUri else fallbackResId
        val cacheKey = "art_thumb_${track.id}_$thumbnailSizePx"
        if (VelvetImageLoader.isInMemory(context, cacheKey)) return

        val request = ImageRequest.Builder(context)
            .data(data)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .size(thumbnailSizePx, thumbnailSizePx)
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .allowHardware(true)
            .dispatcher(Dispatchers.IO)
            .build()
        loader.enqueue(request)
    }
}
