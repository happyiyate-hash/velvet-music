package com.example.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import com.example.media.VelvetArtworkCache

/**
 * Universal Track Artwork renderer:
 * 1. Instant Cache-First: Memory-cached artwork is rendered synchronously on frame 0 (0ms latency).
 * 2. Unblurred & Sharp: Displays crisp, unblurred bitmaps without heavy downsampling.
 * 3. Decoupled Asynchronous Decoding: IO dispatcher resolves new device cover art in background.
 * 4. Guaranteed Fallback: Seamlessly falls back to rich fallback pool artwork.
 */
@Composable
fun TrackArtworkImage(
    track: Track,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSizePx: Int? = null,
    crossfade: Boolean = true
) {
    val context = LocalContext.current
    val isThumbnail = thumbnailSizePx != null && thumbnailSizePx < 256

    var cachedBitmap by androidx.compose.runtime.remember(track.id, isThumbnail) {
        androidx.compose.runtime.mutableStateOf(
            if (isThumbnail) VelvetArtworkCache.getThumbnailBitmap(track.id)
            else VelvetArtworkCache.getArtworkBitmap(track.id)
        )
    }

    androidx.compose.runtime.LaunchedEffect(track.id, isThumbnail) {
        if (cachedBitmap == null) {
            VelvetArtworkCache.loadArtworkAsync(context, track, isThumbnail) { loaded ->
                cachedBitmap = loaded
            }
        }
    }

    val currentBmp = cachedBitmap
    Box(
        modifier = modifier.background(Color(0xFF141518)),
        contentAlignment = Alignment.Center
    ) {
        if (currentBmp != null && !currentBmp.isRecycled) {
            Image(
                bitmap = currentBmp.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
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
            val request = remember(track.id, primaryData, fallbackResId, thumbnailSizePx, crossfade) {
                val builder = ImageRequest.Builder(context)
                    .data(primaryData)
                    .error(fallbackResId)
                    .fallback(fallbackResId)
                    .dispatcher(Dispatchers.IO)
                    .crossfade(crossfade)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey("art_${track.id}_${thumbnailSizePx ?: 0}")
                    .diskCacheKey("art_${track.id}_${thumbnailSizePx ?: 0}")

                if (thumbnailSizePx != null && thumbnailSizePx > 0) {
                    builder.size(thumbnailSizePx, thumbnailSizePx)
                        .precision(Precision.EXACT)
                        .scale(Scale.FILL)
                }
                builder.build()
            }

            AsyncImage(
                model = request,
                imageLoader = imageLoader,
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

