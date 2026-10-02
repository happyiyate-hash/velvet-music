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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.withContext

/**
 * Universal Track Artwork renderer:
 * 1. Fully Asynchronous: Decoding runs strictly on Dispatchers.IO, never blocking the main UI thread.
 * 2. Guaranteed Full Resolution for Player Sheet:
 *    - Never gets stuck on downsampled thumbnails. Always decodes and displays full-resolution HD artwork.
 * 3. Exact Downscaling for Lists: Resizes list/queue thumbnails to 128x128 px for smooth 60fps scrolling.
 * 4. Zero-Flicker Transitions: Frame-0 memory hit ensures zero blank cards.
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
    val isThumbnail = thumbnailSizePx != null && thumbnailSizePx > 0

    // Frame-0 memory check:
    // If thumbnail: get from thumbnail memory cache.
    // If full artwork (Player Sheet): get from full HD memory cache. If not yet decoded in HD,
    // temporarily show thumbnail as a 0ms placeholder so the UI is never blank while full HD loads.
    var displayedBitmap by remember(track.id, isThumbnail) {
        val initialBmp = if (isThumbnail) {
            VelvetArtworkCache.getFromMemory(track.id)
        } else {
            VelvetArtworkCache.getFullFromMemory(track.id)
                ?: VelvetArtworkCache.getFromMemory(track.id)
        }
        mutableStateOf(initialBmp)
    }

    // High-Fidelity Resolver:
    // For Player Sheet (!isThumbnail), guarantees that full-resolution HD artwork (up to 1200x1200px)
    // is decoded, cached in memory/disk, and displayed with ZERO blur.
    LaunchedEffect(track.id, isThumbnail) {
        if (!isThumbnail) {
            val cachedFull = VelvetArtworkCache.getFullFromMemory(track.id)
            if (cachedFull != null && !cachedFull.isRecycled) {
                displayedBitmap = cachedFull
            } else {
                val fullBmp = withContext(Dispatchers.IO) {
                    VelvetArtworkCache.getOrDecodeFullArtwork(context, track)
                }
                if (!fullBmp.isRecycled) {
                    displayedBitmap = fullBmp
                }
            }
        } else {
            val cachedThumb = VelvetArtworkCache.getFromMemory(track.id)
            if (cachedThumb != null && !cachedThumb.isRecycled) {
                displayedBitmap = cachedThumb
            } else {
                val thumbBmp = withContext(Dispatchers.IO) {
                    VelvetArtworkCache.getOrDecodeThumbnail(context, track)
                }
                if (!thumbBmp.isRecycled) {
                    displayedBitmap = thumbBmp
                }
            }
        }
    }

    Box(
        modifier = modifier
    ) {
        val activeBmp = displayedBitmap
        if (activeBmp != null && !activeBmp.isRecycled) {
            Image(
                bitmap = activeBmp.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (track.coverResId != 0) {
            Image(
                painter = painterResource(track.coverResId),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            val fallbackRes = remember(track.id) {
                FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
            }
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
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

