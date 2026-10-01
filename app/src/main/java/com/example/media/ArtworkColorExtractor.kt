package com.example.media

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import com.example.model.Track

/**
 * Backward-compatible facade for the OKLab-based Velvet artwork engine.
 * Existing callers keep using ArtworkColorExtractor while palette selection
 * now happens inside VelvetArtworkColorEngine.
 */
object ArtworkColorExtractor {
    var debugLoggingEnabled: Boolean
        get() = VelvetArtworkColorEngine.debugLoggingEnabled
        set(value) {
            VelvetArtworkColorEngine.debugLoggingEnabled = value
        }

    fun extractColors(context: Context, track: Track): TrackThemeColors =
        VelvetArtworkCache.getColors(track.id)
            ?: VelvetArtworkColorEngine.extractColors(context, track).also {
                VelvetArtworkCache.putColors(track.id, it)
            }

    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? =
        VelvetArtworkCache.getBitmap(track.id)
            ?: VelvetArtworkColorEngine.resolveTrackBitmap(context, track)

    fun extractColorsFromBitmap(bitmap: Bitmap?): TrackThemeColors =
        VelvetArtworkColorEngine.extractColorsFromBitmap(bitmap)

    fun extractColorsFromUri(context: Context, artworkUriString: String): TrackThemeColors =
        VelvetArtworkColorEngine.extractColorsFromUri(context, artworkUriString)

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors =
        VelvetArtworkColorEngine.getColorsForDrawable(context, resId)

    fun getDefaultBitmap(context: Context): Bitmap =
        VelvetArtworkColorEngine.getDefaultBitmap(context)

    fun generateThemePalette(baseColor: androidx.compose.ui.graphics.Color): TrackThemeColors =
        VelvetArtworkColorEngine.generateThemePalette(baseColor)

    fun extractVisualizerColor(
        context: Context,
        track: Track,
        chosenBackgroundColor: androidx.compose.ui.graphics.Color
    ): androidx.compose.ui.graphics.Color =
        VelvetArtworkColorEngine.extractVisualizerColor(context, track, chosenBackgroundColor)

    fun extractVisualizerColorFromBitmap(
        bitmap: Bitmap?,
        chosenBackgroundColor: androidx.compose.ui.graphics.Color
    ): androidx.compose.ui.graphics.Color =
        VelvetArtworkColorEngine.extractVisualizerColorFromBitmap(bitmap, chosenBackgroundColor)
}
