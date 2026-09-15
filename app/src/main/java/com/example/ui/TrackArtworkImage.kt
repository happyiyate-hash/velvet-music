package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Track

/**
 * Universal Track Artwork renderer:
 * 1. If the music fetched/searched from user device has a photo (embedded in ID3 tag, MediaStore, or local cache),
 *    this component renders that real photo.
 * 2. If the song has NO photo, it cleanly and reliably resolves to the app's fallback artwork resource.
 * 3. Wrapped with a key(track.id) and remember(track.id) so track transitions always force a full reset
 *    and re-evaluation of the active artwork target.
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
    key(track.id) {
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

        if (effectiveArtTarget != null) {
            val request = remember(track.id, effectiveArtTarget) {
                ImageRequest.Builder(context)
                    .data(effectiveArtTarget)
                    .crossfade(crossfade)
                    .apply {
                        thumbnailSizePx?.let {
                            size(it, it)
                            val cacheKey = "thumb_${track.id}_$effectiveArtTarget"
                            memoryCacheKey(cacheKey)
                            diskCacheKey(cacheKey)
                            allowHardware(true)
                        }
                    }
                    .build()
            }
            val placeholderPainter = if (track.coverResId != 0) painterResource(id = track.coverResId) else null
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter
            )
        } else if (track.coverResId != 0) {
            Image(
                painter = painterResource(id = track.coverResId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

