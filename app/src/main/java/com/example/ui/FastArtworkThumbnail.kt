package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

/**
 * YouTube Music Style Ultra-Fast Artwork Thumbnail:
 *
 * 1. Fixed Dimensions: Space is reserved immediately (48.dp x 48.dp), preventing any layout shift.
 * 2. Lightweight Placeholder First: Pure Compose dark surface with subtle icon, rendered in 0ms without loading resources.
 * 3. Scroll-Decoupled Asynchronous Pipeline:
 *    - Memory-cached thumbnails render instantly on frame 0.
 *    - During active fling/scrolling (isScrolling = true), heavy decodes are deprioritized so scrolling stays 60/120 FPS.
 *    - When scrolling slows/stops, the thumbnail decodes in the background on Dispatchers.IO.
 * 4. Exact Downsampling: Downsamples to target thumbnail resolution (128x128 px), never allocating full 1000x1000 bitmaps.
 * 5. Gentle Fade-In: When the image is ready, ONLY the image fades in smoothly (160ms); the row, title, and metadata never flicker.
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

    // Check if thumbnail is already cached in memory
    val isInitiallyCached = remember(cacheKey) {
        VelvetImageLoader.isInMemory(context, cacheKey)
    }

    // If already in memory, load immediately.
    // If not in memory and currently scrolling fast, defer request until scroll settles.
    var shouldLoad by remember(track.id) { mutableStateOf(isInitiallyCached || !isScrolling) }

    LaunchedEffect(isScrolling, isInitiallyCached) {
        if (!shouldLoad) {
            if (!isScrolling) {
                // Short settle debounce so high-speed flings don't trigger burst decodes
                delay(30)
                shouldLoad = true
            }
        }
    }

    val request = remember(track.id, primaryData, fallbackResId, thumbnailSizePx, shouldLoad) {
        if (!shouldLoad) null
        else {
            ImageRequest.Builder(context)
                .data(primaryData)
                .error(fallbackResId)
                .fallback(fallbackResId)
                // Note: NO heavy drawable placeholder here! The Composable placeholder Box handles it in 0ms!
                .dispatcher(Dispatchers.IO)
                .crossfade(160)
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
    }

    // 1. Reserved fixed dimension container
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF1B1D23)),
        contentAlignment = Alignment.Center
    ) {
        // 2. Immediate lightweight placeholder: rendered instantly without APK resource inflation
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier.size(size * 0.44f)
        )

        // 3. Asynchronous thumbnail: fades into place once decoded
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
