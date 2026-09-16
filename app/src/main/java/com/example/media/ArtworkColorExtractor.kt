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
    val glow: Color,
    val darkBackground: Color = Color(0xFF14080D),
    val atmosphericBloom: Color = Color(0xFF42101C),
    val playPauseCircle: Color = Color(0xFF5A1422),
    val playPauseBorder: Color = Color(0xFF8C1E34).copy(alpha = 0.40f),
    val bgTop: Color = Color(0xFF38101A),
    val bgMidUpper: Color = Color(0xFF280B13),
    val bgMidLower: Color = Color(0xFF1C070D),
    val bgBottom: Color = Color(0xFF130509),
    val playPauseGradTop: Color = Color(0xFF5E1B2C),
    val playPauseGradBottom: Color = Color(0xFF2E0C15)
)

object ArtworkColorExtractor {

    fun extractColors(context: Context, track: Track): TrackThemeColors {
        return extractColorsFromBitmap(resolveTrackBitmap(context, track))
    }

    /** Resolves artwork for callers that need the actual bitmap, not only its palette. */
    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? = loadThumbnailBitmap(context, track)

    /** Extracts a theme directly from an already-resolved bitmap. */
    fun extractColorsFromBitmap(bitmap: Bitmap?): TrackThemeColors {
        if (bitmap != null) {
            val sampled = sampleDominantColor(bitmap)
            if (sampled != null) return generateThemePalette(sampled)
        }
        return generateThemePalette(Color(0xFF880E2F))
    }

    /** Small deterministic fallback used by media-session metadata/notification code. */
    fun getDefaultBitmap(context: Context): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color(0xFF18070D).toArgb())
    }

    fun extractColorsFromUri(context: Context, artworkUriString: String): TrackThemeColors {
        return try {
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
            extractColorsFromBitmap(bitmap)
        } catch (_: Exception) {
            generateThemePalette(Color(0xFF880E2F))
        }
    }

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors {
        return try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            extractColorsFromBitmap(bitmap)
        } catch (_: Exception) {
            generateThemePalette(Color(0xFF880E2F))
        }
    }

    private fun loadThumbnailBitmap(context: Context, track: Track): Bitmap? {
        return try {
            if (!track.artworkUri.isNullOrBlank()) {
                val uri = Uri.parse(track.artworkUri)
                if (uri.scheme == "file") {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(uri.path, opts)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }
            } else if (track.contentUri != null) {
                val uri = Uri.parse(track.contentUri)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val raw = retriever.embeddedPicture
                    if (raw != null) {
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeByteArray(raw, 0, raw.size, options)
                    } else null
                } finally {
                    retriever.release()
                }
            } else if (track.coverResId != 0) {
                val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeResource(context.resources, track.coverResId, options)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun sampleDominantColor(bitmap: Bitmap): Color? {
        return try {
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
                    val a = (pixel ushr 24) and 0xFF
                    if (a < 128) continue
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = r * 0.299f + g * 0.587f + b * 0.114f
                    if (brightness in 35.0..225.0) {
                        totalR += r
                        totalG += g
                        totalB += b
                        sampleCount++
                        val maxC = max(r, max(g, b)).toFloat()
                        val minC = min(r, min(g, b)).toFloat()
                        val saturation = if (maxC > 0f) (maxC - minC) / maxC else 0f
                        if (saturation > maxVibrancy && saturation > 0.22f) {
                            maxVibrancy = saturation
                            vibrantColor = Color(r, g, b)
                        }
                    }
                }
            }

            when {
                vibrantColor != null && maxVibrancy > 0.28f -> vibrantColor
                sampleCount > 0 -> Color(
                    (totalR / sampleCount).toInt().coerceIn(0, 255),
                    (totalG / sampleCount).toInt().coerceIn(0, 255),
                    (totalB / sampleCount).toInt().coerceIn(0, 255)
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0.45f, 0.95f)
        val dominant = Color.hsv(hue, sat, 0.75f)
        val secondary = Color.hsv(hue, (sat * 0.9f).coerceIn(0.5f, 1f), 0.16f)
        val accent = Color.hsv(hue, (sat * 0.85f).coerceIn(0.50f, 0.95f), 0.98f)
        val glow = Color.hsv(hue, sat, 0.88f)
        val bgTop = Color.hsv(hue, (sat * 0.72f).coerceIn(0.40f, 0.82f), 0.32f)
        val bgMidUpper = Color.hsv(hue, (sat * 0.65f).coerceIn(0.36f, 0.76f), 0.22f)
        val bgMidLower = Color.hsv(hue, (sat * 0.60f).coerceIn(0.32f, 0.70f), 0.15f)
        val bgBottom = Color.hsv(hue, (sat * 0.55f).coerceIn(0.28f, 0.65f), 0.09f)
        val darkBackground = bgBottom
        val atmosphericBloom = Color.hsv(hue, (sat * 0.78f).coerceIn(0.45f, 0.88f), 0.40f)
        val playPauseGradTop = Color.hsv(hue, (sat * 0.76f).coerceIn(0.45f, 0.88f), 0.48f)
        val playPauseGradBottom = Color.hsv(hue, (sat * 0.85f).coerceIn(0.55f, 0.92f), 0.24f)
        val playPauseCircle = playPauseGradTop
        val playPauseBorder = Color.hsv(hue, sat, 0.68f).copy(alpha = 0.40f)

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
