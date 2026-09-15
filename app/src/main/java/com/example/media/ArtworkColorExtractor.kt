package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.example.R
import com.example.model.Track
import kotlin.math.max
import kotlin.math.min

object DynamicThemeExtractor {

    /**
     * Extracts the primary dominant theme color prioritizing dominant & high-population chromatic swatches,
     * sampling edge borders for promotional cover art, and clamping HSV brightness for dark mode surfaces.
     */
    fun extractDominantThemeColor(bitmap: Bitmap, defaultColor: Color = Color(0xFF121212)): Color {
        try {
            val sampleBitmap = if (bitmap.width > 160 || bitmap.height > 160) {
                Bitmap.createScaledBitmap(bitmap, 128, 128, true)
            } else {
                bitmap
            }

            val palette = Palette.from(sampleBitmap)
                .maximumColorCount(32) // Increases color sampling accuracy
                .generate()

            // 1. Edge & Outer sampling: sample perimeter pixels (outer 8%)
            // Many promotional covers (e.g. green Naira Marley) have logos/text in the center,
            // while the true artwork background spans the edges.
            val perimeterColorInt = samplePerimeterColor(sampleBitmap)

            // Filter out near-pure-black (value < 0.10) and near-pure-white/gray (sat < 0.15 && val > 0.85)
            // so chromatic tones (e.g. red circular logo on black, bright green) are never masked.
            val chromaticSwatches = palette.swatches.filter { swatch ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                hsv[1] >= 0.15f && hsv[2] >= 0.10f
            }

            // If perimeter has a distinct chromatic saturation, prioritize swatches close to that hue
            val candidateSwatch = if (perimeterColorInt != null) {
                val edgeHsv = FloatArray(3)
                android.graphics.Color.colorToHSV(perimeterColorInt, edgeHsv)
                if (edgeHsv[1] >= 0.20f) {
                    chromaticSwatches.minByOrNull { swatch ->
                        val sHsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(swatch.rgb, sHsv)
                        val hueDiff = Math.abs(sHsv[0] - edgeHsv[0]).let { if (it > 180f) 360f - it else it }
                        hueDiff
                    } ?: chromaticSwatches.maxByOrNull { it.population }
                } else {
                    chromaticSwatches.maxByOrNull { it.population }
                }
            } else {
                chromaticSwatches.maxByOrNull { it.population }
            }
                ?: palette.swatches.maxByOrNull { it.population }
                ?: palette.dominantSwatch
                ?: palette.vibrantSwatch
                ?: palette.mutedSwatch

            val rawColorInt = candidateSwatch?.rgb ?: perimeterColorInt ?: defaultColor.toArgb()

            // 2. Convert to HSV to clamp brightness for comfortable dark-mode surfaces
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(rawColorInt, hsv)

            // Preserve Hue (hsv[0]), balance Saturation (hsv[1]), clamp max Brightness (hsv[2])
            hsv[1] = hsv[1].coerceIn(0.3f, 0.85f) // Ensures color isn't completely washed out or gray
            hsv[2] = hsv[2].coerceIn(0.12f, 0.28f) // Keeps player background dark & readable

            return Color(android.graphics.Color.HSVToColor(hsv))
        } catch (_: Exception) {
            return defaultColor
        }
    }

    private fun samplePerimeterColor(bitmap: Bitmap): Int? {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 10 || h < 10) return null

        val insetX = (w * 0.08f).toInt().coerceAtLeast(1)
        val insetY = (h * 0.08f).toInt().coerceAtLeast(1)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        for (x in insetX until (w - insetX) step 2) {
            val topP = bitmap.getPixel(x, insetY)
            val botP = bitmap.getPixel(x, h - 1 - insetY)
            for (p in intArrayOf(topP, botP)) {
                if ((p ushr 24) >= 128) {
                    totalR += (p shr 16) and 0xFF
                    totalG += (p shr 8) and 0xFF
                    totalB += p and 0xFF
                    count++
                }
            }
        }
        for (y in insetY until (h - insetY) step 2) {
            val leftP = bitmap.getPixel(insetX, y)
            val rightP = bitmap.getPixel(w - 1 - insetX, y)
            for (p in intArrayOf(leftP, rightP)) {
                if ((p ushr 24) >= 128) {
                    totalR += (p shr 16) and 0xFF
                    totalG += (p shr 8) and 0xFF
                    totalB += p and 0xFF
                    count++
                }
            }
        }
        if (count == 0) return null
        return android.graphics.Color.rgb(
            (totalR / count).toInt().coerceIn(0, 255),
            (totalG / count).toInt().coerceIn(0, 255),
            (totalB / count).toInt().coerceIn(0, 255)
        )
    }
}

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

    private val paletteCache = android.util.LruCache<String, TrackThemeColors>(100)
    private val bitmapCache = android.util.LruCache<String, Bitmap>(40)

    fun getCachedPalette(trackId: String): TrackThemeColors? {
        return paletteCache.get(trackId)
    }

    fun extractColors(context: Context, track: Track): TrackThemeColors {
        paletteCache.get(track.id)?.let { return it }
        val bitmap = resolveTrackBitmap(context, track)
        val colors = extractColorsFromBitmap(bitmap)
        paletteCache.put(track.id, colors)
        return colors
    }

    /**
     * Resolves the artwork bitmap used both by color extraction and Android media metadata.
     */
    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? {
        val cacheKey = track.artworkUri ?: track.contentUri ?: "res_${track.coverResId}_${track.id}"
        bitmapCache.get(cacheKey)?.let { return it }

        val loaded = loadThumbnailBitmap(context, track)
        if (loaded != null) {
            bitmapCache.put(cacheKey, loaded)
        }
        return loaded
    }

    /**
     * Returns a guaranteed non-null artwork bitmap for APIs that require Bitmap rather than Bitmap?.
     */
    fun getDefaultBitmap(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.art_luminous_echoes)
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    /**
     * Extracts the theme palette from an already-resolved artwork bitmap.
     */
    fun extractColorsFromBitmap(bitmap: Bitmap?): TrackThemeColors {
        if (bitmap != null) {
            val dominant = DynamicThemeExtractor.extractDominantThemeColor(bitmap)
            return generateThemePalette(dominant)
        }
        return generateThemePalette(Color(0xFF880E2F))
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
                val dominant = DynamicThemeExtractor.extractDominantThemeColor(bitmap)
                return generateThemePalette(dominant)
            }
        } catch (_: Exception) {}
        return generateThemePalette(Color(0xFF880E2F))
    }

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors {
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            if (bitmap != null) {
                val dominant = DynamicThemeExtractor.extractDominantThemeColor(bitmap)
                return generateThemePalette(dominant)
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

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val rawSat = hsv[1]
        val rawVal = hsv[2]

        val isAshOrMonochrome = rawSat < 0.18f

        val dominant: Color
        val secondary: Color
        val accent: Color
        val glow: Color
        val backgroundSurface: Color
        val playPauseGradTop: Color
        val playPauseGradBottom: Color

        if (isAshOrMonochrome) {
            dominant = Color.hsv(hue, 0.05f, 0.70f)
            secondary = Color.hsv(hue, 0.05f, 0.18f)
            accent = Color.hsv(hue, 0.04f, 0.95f)
            glow = Color.hsv(hue, 0.05f, 0.82f)
            backgroundSurface = Color.hsv(hue, 0.06f, 0.12f)
            playPauseGradTop = Color.hsv(hue, 0.08f, 0.35f)
            playPauseGradBottom = Color.hsv(hue, 0.08f, 0.18f)
        } else {
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
