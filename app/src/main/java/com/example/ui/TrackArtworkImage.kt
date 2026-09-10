package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
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
 *    this component renders that real photo without jarring placeholder flicker!
 * 2. If the song has NO photo, it renders the deterministic distributed fallback cover image directly.
 */
@Composable
fun TrackArtworkImage(
    track: Track,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val artUri = track.artworkUri
    if (!artUri.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artUri)
                .crossfade(false)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            error = painterResource(id = track.coverResId),
            fallback = painterResource(id = track.coverResId)
        )
    } else {
        Image(
            painter = painterResource(id = track.coverResId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
