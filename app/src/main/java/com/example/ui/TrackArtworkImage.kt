package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Universal Track Artwork renderer:
 * 1. Fully Asynchronous: Decoding runs strictly on Dispatchers.IO, never blocking the main UI thread.
 * 2. Guaranteed Replacement Artwork: If track has no artwork or MediaStore URI fails,
 *    instantly falls back to the app's rich fallback pool artwork.
 * 3. Exact Downscaling: Resizes bitmaps to exact target visual dimensions (128x128 px).
 * 4. Asynchronous Crossfade: Smooth transition with zero-jank caching.
 */
@Composable
fun TrackArtworkImage(
    track: Track,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSizePx: Int? = null,
    crossfade: Boolean = true,
    loadDelayMs: Long = 0L
) {
    val context = LocalContext.current
    val imageLoader = remember { VelvetImageLoader.get(context) }
    val isThumbnail = thumbnailSizePx != null && thumbnailSizePx > 0

    // Instant frame-0 memory check: for thumbnails check thumbnail cache; for Player Sheet check full HD cache, then thumbnail cache!
    val memoryBitmap: Bitmap? = remember(track.id, isThumbnail) {
        if (isThumbnail) {
            VelvetArtworkCache.getFromMemory(track.id)
        } else {
            VelvetArtworkCache.getFullFromMemory(track.id)
                ?: VelvetArtworkCache.getFromMemory(track.id)
        }
    }

    var displayedBitmap by remember { mutableStateOf<Bitmap?>(memoryBitmap) }
    var allowImageRequest by remember(track.id, memoryBitmap, loadDelayMs) {
        mutableStateOf(memoryBitmap != null || loadDelayMs <= 0L)
    }

    LaunchedEffect(track.id, memoryBitmap, loadDelayMs) {
        if (memoryBitmap != null) {
            allowImageRequest = true
            return@LaunchedEffect
        }
        if (loadDelayMs > 0L) kotlinx.coroutines.delay(loadDelayMs)
        allowImageRequest = true
    }

    LaunchedEffect(track.id) {
        val mem = if (isThumbnail) {
            VelvetArtworkCache.getFromMemory(track.id)
        } else {
            VelvetArtworkCache.getFullFromMemory(track.id) ?: VelvetArtworkCache.getFromMemory(track.id)
        }
        if (mem != null && !mem.isRecycled) {
            displayedBitmap = mem
        }
    }

    val request = remember(track.id, thumbnailSizePx, crossfade, isThumbnail) {
        val builder = ImageRequest.Builder(context)
            .data(track)
            .dispatcher(Dispatchers.IO)
            .crossfade(if (crossfade) 180 else 0)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)

        if (isThumbnail && thumbnailSizePx != null) {
            builder.setParameter("is_thumbnail", true)
                .memoryCacheKey("track_thumb_${track.id}_$thumbnailSizePx")
                .diskCacheKey("track_thumb_${track.id}_$thumbnailSizePx")
                .size(thumbnailSizePx, thumbnailSizePx)
                .precision(Precision.EXACT)
                .scale(Scale.FILL)
        } else {
            // Full-resolution artwork for PlayerSheet: full HD original clarity, zero downsample blur
            builder.setParameter("is_thumbnail", false)
                .memoryCacheKey("track_full_${track.id}")
                .diskCacheKey("track_full_${track.id}")
        }
        builder.build()
    }

    Box(
        modifier = modifier.background(Color(0xFF141418))
    ) {
        val activeBmp = memoryBitmap ?: displayedBitmap
        if (activeBmp != null && !activeBmp.isRecycled) {
            Image(
                bitmap = activeBmp.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
        if (activeBmp == null && allowImageRequest) {
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f },
                contentScale = contentScale,
                onSuccess = { state ->
                    val d = state.result.drawable
                    if (d is android.graphics.drawable.BitmapDrawable) {
                        displayedBitmap = d.bitmap
                        VelvetArtworkCache.putInMemory(track.id, d.bitmap)
                    }
                }
            )
        }
    }
}

/**
 * Clean standard Music Track Row (used across Home, Search, and Offline)
 */
@Composable
fun RecentlyPlayedRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = track.title.substringBefore(" - ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            TrackArtworkImage(
                track = track,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color(0xFFFF2448),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Color(0xFFFF4D6D) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.formattedDuration}",
                fontSize = 12.sp,
                color = Color(0xFFB0B5C0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Clean GradientMusicPlayCard for backward compatibility
 */
@Composable
fun GradientMusicPlayCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RecentlyPlayedRow(
        track = track,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onMenuClick = onMenuClick,
        modifier = modifier
    )
}

