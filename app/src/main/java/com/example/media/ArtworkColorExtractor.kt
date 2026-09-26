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
        VelvetArtworkColorEngine.extractColors(context, track)

    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? =
        VelvetArtworkColorEngine.resolveTrackBitmap(context, track)

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
}
