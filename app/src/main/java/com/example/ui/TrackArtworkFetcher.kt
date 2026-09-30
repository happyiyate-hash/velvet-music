package com.example.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.example.media.VelvetArtworkCache
import com.example.model.Track

/**
 * Direct Coil Fetcher for Track models:
 * Resolves local storage audio album art and resource covers via VelvetArtworkCache
 * without making invalid MediaStore ContentProvider queries.
 */
class TrackArtworkFetcher(
    private val context: Context,
    private val track: Track,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bitmap = VelvetArtworkCache.getOrDecodeThumbnail(context, track)
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Track> {
        override fun create(data: Track, options: Options, imageLoader: ImageLoader): Fetcher {
            return TrackArtworkFetcher(context, data, options)
        }
    }
}
