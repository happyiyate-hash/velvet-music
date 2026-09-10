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

            val bins = HashMap<Int, Long>()
            val sumR = HashMap<Int, Long>()
            val sumG = HashMap<Int, Long>()
            val sumB = HashMap<Int, Long>()
            val stepX = max(1, width / 24)
            val stepY = max(1, height / 24)

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = (pixel ushr 24) and 0xFF
                    if (a < 128) continue
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = r * 0.299f + g * 0.587f + b * 0.114f
                    if (brightness < 18f || brightness > 245f) continue
                    val qr = r shr 4
                    val qg = g shr 4
                    val qb = b shr 4
                    val key = (qr shl 8) or (qg shl 4) or qb
                    bins[key] = (bins[key] ?: 0L) + 1L
                    sumR[key] = (sumR[key] ?: 0L) + r
                    sumG[key] = (sumG[key] ?: 0L) + g
                    sumB[key] = (sumB[key] ?: 0L) + b
                }
            }
            if (bins.isEmpty()) return null
            val bestKey = bins.keys.maxWithOrNull(
                compareBy<Int> { bins[it] ?: 0L }
                    .thenBy { key ->
                        val count = bins[key] ?: 1L
                        val r = (sumR[key] ?: 0L).toFloat() / count
                        val g = (sumG[key] ?: 0L).toFloat() / count
                        val b = (sumB[key] ?: 0L).toFloat() / count
                        val maxC = max(r, max(g, b))
                        val minC = min(r, min(g, b))
                        if (maxC > 0f) (maxC - minC) / maxC else 0f
                    }
            ) ?: return null
            val count = bins[bestKey] ?: return null
            return Color(
                (sumR[bestKey]!! / count).toInt().coerceIn(0, 255),
                (sumG[bestKey]!! / count).toInt().coerceIn(0, 255),
                (sumB[bestKey]!! / count).toInt().coerceIn(0, 255)
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val sat = hsv[1].coerceIn(0f, 1f)

        // Dominant rich color
        val dominant = Color.hsv(hue, sat, 0.75f)
        // Deep secondary moody tone
        val secondary = Color.hsv(hue, (sat * 0.9f).coerceIn(0f, 1f), 0.16f)
        // Vivid luminous accent for active waveform bars, progress bead, and active shuffle/repeat
        val accent = Color.hsv(hue, (sat * 0.85f).coerceIn(0f, 1f), 0.98f)
        // Ambient soft glow
        val glow = Color.hsv(hue, sat, 0.88f)

        // Atmosphere gradient: top warm atmosphere -> deep warm tone -> dark burnt-hue tone -> very dark hue tone (visibly preserving hue all the way down, never pure black)
        val bgTop = Color.hsv(hue, (sat * 0.72f).coerceIn(0f, 1f), 0.32f)
        val bgMidUpper = Color.hsv(hue, (sat * 0.65f).coerceIn(0f, 1f), 0.22f)
        val bgMidLower = Color.hsv(hue, (sat * 0.60f).coerceIn(0f, 1f), 0.15f)
        val bgBottom = Color.hsv(hue, (sat * 0.55f).coerceIn(0f, 1f), 0.09f)

        val darkBackground = bgBottom
        val atmosphericBloom = Color.hsv(hue, (sat * 0.78f).coerceIn(0f, 1f), 0.40f)

        // Premium gradient circle matching the atmospheric background tones with clean glassmorphic depth
        val playPauseGradTop = Color.hsv(hue, (sat * 0.76f).coerceIn(0f, 1f), 0.48f)
        val playPauseGradBottom = Color.hsv(hue, (sat * 0.85f).coerceIn(0f, 1f), 0.24f)
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
