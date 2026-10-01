package com.example.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import kotlinx.coroutines.Dispatchers

/**
 * YouTube Music Style Ultra-Fast Artwork Thumbnail:
 *
 * 1. Cache-First: Serves pre-warmed bitmaps directly from VelvetArtworkCache (0ms memory lookup).
 * 2. Zero Placeholders: No dark square or placeholder box. The container seamlessly blends with the row background.
 * 3. Water-Emergence Fade: Gently surfaces and fades in smoothly as if emerging from the background liquid surface.
 * 4. Fixed Dimensions: Space is reserved immediately (48.dp x 48.dp), preventing any layout shift.
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
    val cachedThumb = remember(track.id) {
        VelvetArtworkCache.getThumbnail(track.id)
    }

    // Soft emergence animation: fades in smoothly from the background like surfacing from water
    val emergenceAlpha = remember(track.id) { Animatable(if (cachedThumb != null) 0.65f else 0f) }
    LaunchedEffect(track.id) {
        emergenceAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
        )
    }

    // 1. Reserved fixed dimension container without harsh dark background cutoffs
    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (cachedThumb != null) {
            Image(
                bitmap = cachedThumb.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = emergenceAlpha.value },
                contentScale = ContentScale.Crop
            )
        } else {
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

            val imageLoader = remember { VelvetImageLoader.get(context) }
            val request = remember(track.id, primaryData, fallbackResId, thumbnailSizePx) {
                ImageRequest.Builder(context)
                    .data(primaryData)
                    .error(fallbackResId)
                    .fallback(fallbackResId)
                    .dispatcher(Dispatchers.IO)
                    .crossfade(180)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey("art_thumb_${track.id}")
                    .diskCacheKey("art_thumb_${track.id}")
                    .size(thumbnailSizePx, thumbnailSizePx)
                    .precision(Precision.EXACT)
                    .scale(Scale.FILL)
                    .build()
            }

            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = emergenceAlpha.value },
                contentScale = ContentScale.Crop
            )
        }
    }
}

object FastArtworkThumbnailDefaults {
    /**
     * Prefetches upcoming thumbnails into the universal VelvetArtworkCache in the background.
     */
    fun prefetch(context: Context, track: Track, thumbnailSizePx: Int = 128) {
        if (VelvetArtworkCache.hasArtwork(track.id)) return
        VelvetArtworkCache.warmCache(context, listOf(track))
    }
}
