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
import kotlin.math.max
import kotlin.math.min

data class TrackThemeColors(
    val dominant: Color,
    val secondary: Color,
    val accent: Color,
    val glow: Color,
    val darkBackground: Color = Color(0xFF14080D),
    val atmosphericBloom: Color = Color.Transparent,
    val playPauseCircle: Color = Color(0xFF5A1422),
    val playPauseBorder: Color = Color(0xFF8C1E34).copy(alpha = 0.40f),
    val bgTop: Color = Color(0xFF1C070D),
    val bgMidUpper: Color = Color(0xFF1C070D),
    val bgMidLower: Color = Color(0xFF1C070D),
    val bgBottom: Color = Color(0xFF1C070D),
    val playPauseGradTop: Color = Color(0xFF5E1B2C),
    val playPauseGradBottom: Color = Color(0xFF2E0C15)
)

object ArtworkColorExtractor {

    fun extractColors(context: Context, track: Track): TrackThemeColors {
        val bitmap = loadThumbnailBitmap(context, track)
        if (bitmap != null) {
            val sampled = sampleDominantColor(bitmap)
            if (sampled != null) {
                return generateThemePalette(sampled)
            }
        }
        return generateThemePalette(track.dominantColor)
    }

    fun extractColorsFromUri(context: Context, artworkUriString: String): TrackThemeColors {
        try {
            val uri = Uri.parse(artworkUriString)
            val bitmap = if (uri.scheme == "file") {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(uri.path, opts)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            }
            if (bitmap != null) {
                val sampled = sampleDominantColor(bitmap)
                if (sampled != null) {
                    return generateThemePalette(sampled)
                }
            }
        } catch (_: Exception) {}
        return generateThemePalette(Color(0xFF880E2F))
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
            if (!track.artworkUri.isNullOrBlank()) {
                val uri = Uri.parse(track.artworkUri)
                if (uri.scheme == "file") {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    return BitmapFactory.decodeFile(uri.path, opts)
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    return BitmapFactory.decodeStream(stream, null, opts)
                }
            } else if (track.contentUri != null) {
                val uri = Uri.parse(track.contentUri)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val raw = retriever.embeddedPicture
                    if (raw != null) {
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                        return BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
                    }
                } finally {
                    retriever.release()
                }
            } else if (track.coverResId != 0) {
                val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                return BitmapFactory.decodeResource(context.resources, track.coverResId, options)
            }
        } catch (_: Exception) {}
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

                    // Allow dark/light bounds (5 to 250) so pure white and black & white art aren't ignored
                    val brightness = (r * 0.299f + g * 0.587f + b * 0.114f)
                    if (brightness in 5.0..250.0) {
                        totalR += r
                        totalG += g
                        totalB += b
                        sampleCount++

                        val maxC = max(r, max(g, b)).toFloat()
                        val minC = min(r, min(g, b)).toFloat()
                        val saturation = if (maxC > 0) (maxC - minC) / maxC else 0f
                        
                        // Pick out rich vibrant colors if present
                        if (saturation > maxVibrancy && saturation > 0.18f) {
                            maxVibrancy = saturation
                            vibrantColor = Color(r, g, b)
                        }
                    }
                }
            }

            // Return strong vibrant color if found
            if (vibrantColor != null && maxVibrancy > 0.22f) return vibrantColor

            // Otherwise, calculate dynamic average (handles Ash, Black/White, and Pure White)
            if (sampleCount > 0) {
                val avgR = (totalR / sampleCount).toInt().coerceIn(0, 255)
                val avgG = (totalG / sampleCount).toInt().coerceIn(0, 255)
                val avgB = (totalB / sampleCount).toInt().coerceIn(0, 255)
                return Color(avgR, avgG, avgB)
            }
        } catch (_: Exception) {}
        return null
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val rawSat = hsv[1]
        val rawVal = hsv[2]

        // Check if artwork is Monochromatic / Black & White / Pure White / Ash Gray
        val isAshOrMonochrome = rawSat < 0.18f

        val dominant: Color
        val secondary: Color
        val accent: Color
        val glow: Color
        val backgroundSurface: Color
        val playPauseGradTop: Color
        val playPauseGradBottom: Color

        if (isAshOrMonochrome) {
            // Sleek Ash Charcoal Theme Engine (for B&W, Ash, and Pure White covers)
            dominant = Color.hsv(hue, 0.05f, 0.70f) // Soft Silver Ash
            secondary = Color.hsv(hue, 0.05f, 0.18f) // Dark Slate
            accent = Color.hsv(hue, 0.04f, 0.95f) // Crisp Platinum White
            glow = Color.hsv(hue, 0.05f, 0.82f) // Cool Ash Glow

            // Deep Ash Surface (#16181A tone)
            backgroundSurface = Color.hsv(hue, 0.06f, 0.12f)

            playPauseGradTop = Color.hsv(hue, 0.08f, 0.35f)
            playPauseGradBottom = Color.hsv(hue, 0.08f, 0.18f)
        } else {
            // Standard Vibrant Theme Engine (for Colorful covers)
            val sat = rawSat.coerceIn(0.40f, 0.95f)

            dominant = Color.hsv(hue, sat, 0.75f)
            secondary = Color.hsv(hue, (sat * 0.9f).coerceIn(0.40f, 1f), 0.16f)
            accent = Color.hsv(hue, (sat * 0.85f).coerceIn(0.45f, 0.95f), 0.98f)
            glow = Color.hsv(hue, sat, 0.88f)

            backgroundSurface = Color.hsv(
                hue,
                (sat * 0.60f).coerceIn(0.32f, 0.70f),
                0.15f
            )

            playPauseGradTop = Color.hsv(hue, (sat * 0.76f).coerceIn(0.45f, 0.88f), 0.48f)
            playPauseGradBottom = Color.hsv(hue, (sat * 0.85f).coerceIn(0.55f, 0.92f), 0.24f)
        }

        val bgTop = backgroundSurface
        val bgMidUpper = backgroundSurface
        val bgMidLower = backgroundSurface
        val bgBottom = backgroundSurface
        val darkBackground = backgroundSurface
        val atmosphericBloom = Color.Transparent

        val playPauseCircle = playPauseGradTop
        val playPauseBorder = if (isAshOrMonochrome) {
            Color.hsv(hue, 0.05f, 0.60f).copy(alpha = 0.35f)
        } else {
            Color.hsv(hue, rawSat.coerceIn(0.4f, 0.95f), 0.68f).copy(alpha = 0.40f)
        }

        return TrackThemeColors(
            dominant = dominant,
            secondary = secondary,
            accent = accent,
            glow = glow,
            darkBackground = darkBackground,
            atmosphericBloom = atmosphericBloom,
            playPauseCircle = playPauseCircle,
            playPauseBorder = playPauseBorder,
            bgTop = bgTop,
            bgMidUpper = bgMidUpper,
            bgMidLower = bgMidLower,
            bgBottom = bgBottom,
            playPauseGradTop = playPauseGradTop,
            playPauseGradBottom = playPauseGradBottom
        )
    }
}
