package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Track

/**
 * Universal Track Artwork renderer:
 * 1. If the track has real artwork, render that artwork.
 * 2. Only when there is no real artwork, render that track's deterministic fallback cover.
 *
 * The composition is explicitly keyed by the track's visual identity. This is important for
 * fallback artwork: two consecutive device tracks can both have artworkUri == null but must
 * still replace one another immediately when their coverResId differs.
 */
@Composable
fun TrackArtworkImage(
    track: Track,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val artUri = track.artworkUri
    key(track.id, artUri, track.coverResId) {
        if (!artUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artUri)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                placeholder = painterResource(id = track.coverResId),
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
}
