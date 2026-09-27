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
import com.example.model.Track
import kotlinx.coroutines.Dispatchers

/**
 * Universal Track Artwork renderer (YouTube Music Asynchronous Architecture):
 * 1. Fully Asynchronous: Decoding runs strictly on Dispatchers.IO, never blocking the main UI thread.
 * 2. Instant Zero-Jank Placeholder: Instant lightweight grey/ash background box renders immediately
 *    without synchronous painterResource() or BitmapFactory decoding on the UI thread.
 * 3. Exact Downscaling: Resizes bitmaps to exact target visual dimensions (128x128 px)
 *    before passing to Compose, avoiding memory bloat and scroll lag.
 * 4. Asynchronous Crossfade: Progressive fade-in smoothly reveals images as background loading finishes.
 * 5. Hardware Bitmaps & Aggressive Caching: Uses hardware bitmap memory cache for butter-smooth scrolling.
 */
@Composable
fun TrackArtworkImage(
    track: Track,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSizePx: Int? = 128,
    crossfade: Boolean = true
) {
    val context = LocalContext.current
    val effectiveArtTarget: Any? = remember(track.id, track.artworkUri, track.coverResId) {
        val uri = track.artworkUri
        if (!uri.isNullOrBlank() && !uri.startsWith("content://media/external/audio/media")) {
            uri
        } else if (track.coverResId != 0) {
            track.coverResId
        } else {
            null
        }
    }

    val targetSize = thumbnailSizePx ?: 128

    val request = remember(track.id, effectiveArtTarget, targetSize, crossfade) {
        ImageRequest.Builder(context)
            .data(effectiveArtTarget)
            .dispatcher(Dispatchers.IO)
            .crossfade(crossfade)
            .size(targetSize, targetSize)
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCacheKey("thumb_${track.id}_$targetSize")
            .diskCacheKey("thumb_${track.id}_$targetSize")
            .build()
    }

    // Instant grey placeholder background (Color(0xFF222226)) prevents blank/white pops
    Box(
        modifier = modifier.background(Color(0xFF222226))
    ) {
        if (effectiveArtTarget != null) {
            AsyncImage(
                model = request,
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

