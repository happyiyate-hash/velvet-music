package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.Track
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TrackThemeColors(
    val dominant: Color,
    val secondary: Color,
    val accent: Color,
    val glow: Color
)

object ArtworkColorExtractor {

    /**
     * Dynamically samples the artwork of a track (from drawable resource or content URI)
     * and extracts the dominant color palette so the background automatically reflects
     * the exact color of the song's picture (e.g., vibrant blue for blue artwork, crimson for red, etc.).
     */
    fun extractColors(context: Context, track: Track): TrackThemeColors {
        val bitmap = loadThumbnailBitmap(context, track)
        if (bitmap != null) {
            val sampled = sampleDominantColor(bitmap)
            if (sampled != null) {
                return generateThemePalette(sampled)
            }
        }
        // Fallback to track's pre-configured dominant color
        return generateThemePalette(track.dominantColor)
    }

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors {
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            if (bitmap != null) {
                val sampled = sampleDominantColor(bitmap)
                if (sampled != null) {
                    return generateThemePalette(sampled)
                }
            }
        } catch (_: Exception) {}
        return generateThemePalette(Color(0xFF880E2F))
    }

    private fun loadThumbnailBitmap(context: Context, track: Track): Bitmap? {
        try {
            if (track.contentUri != null) {
                val uri = Uri.parse(track.contentUri)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val raw = retriever.embeddedPicture
                    if (raw != null) {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4
                        }
                        return BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
                    }
                } finally {
                    retriever.release()
                }
            } else if (track.coverResId != 0) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 8
                }
                return BitmapFactory.decodeResource(context.resources, track.coverResId, options)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
        return null
    }

    private fun sampleDominantColor(bitmap: Bitmap): Color? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return null

            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var sampleCount = 0

            var maxVibrancy = -1f
            var vibrantColor: Color? = null

            // Sample across a grid
            val stepX = max(1, width / 12)
            val stepY = max(1, height / 12)

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = (pixel shr 24) and 0xFF
                    if (a < 128) continue

                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    // Ignore extreme near-blacks and near-whites for dominant chromatic extraction
                    val brightness = (r * 0.299f + g * 0.587f + b * 0.114f)
                    if (brightness in 35.0..225.0) {
                        totalR += r
                        totalG += g
                        totalB += b
                        sampleCount++

                        // Measure chromatic saturation (vibrancy)
                        val maxC = max(r, max(g, b)).toFloat()
                        val minC = min(r, min(g, b)).toFloat()
                        val saturation = if (maxC > 0) (maxC - minC) / maxC else 0f
                        if (saturation > maxVibrancy && saturation > 0.22f) {
                            maxVibrancy = saturation
                            vibrantColor = Color(r, g, b)
                        }
                    }
                }
            }

            if (vibrantColor != null && maxVibrancy > 0.28f) {
                return vibrantColor
            }

            if (sampleCount > 0) {
                val avgR = (totalR / sampleCount).toInt().coerceIn(0, 255)
                val avgG = (totalG / sampleCount).toInt().coerceIn(0, 255)
                val avgB = (totalB / sampleCount).toInt().coerceIn(0, 255)
                return Color(avgR, avgG, avgB)
            }
        } catch (_: Exception) {
            // Fallback
        }
        return null
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0.45f, 0.95f)

        // Dominant rich color
        val dominant = Color.hsv(hue, sat, 0.75f)
        // Deep secondary moody tone
        val secondary = Color.hsv(hue, (sat * 0.9f).coerceIn(0.5f, 1f), 0.16f)
        // Vivid luminous accent for buttons and glows
        val accent = Color.hsv(hue, (sat * 0.85f).coerceIn(0.4f, 0.9f), 0.96f)
        // Ambient soft glow
        val glow = Color.hsv(hue, sat, 0.88f)

        return TrackThemeColors(
            dominant = dominant,
            secondary = secondary,
            accent = accent,
            glow = glow
        )
    }
}
