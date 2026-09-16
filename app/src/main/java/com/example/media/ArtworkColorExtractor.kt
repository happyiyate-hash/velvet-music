package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.R
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
     * the exact mood of the song's picture:
     *
     * - Colorful artwork (blue, red, gold, green, etc.): preserves vibrant chromatic extraction
     *   and rich atmospheric gradients.
     * - Black, white, black-and-white, and grayscale artwork: automatically detected and styled
     *   with an elegant, luxury ashes / smoked charcoal & silver palette, preventing incorrect
     *   crimson or muddy fallbacks.
     */
    fun extractColors(context: Context, track: Track): TrackThemeColors {
        val bitmap = resolveTrackBitmap(context, track)
        return extractColorsFromBitmap(bitmap, track.dominantColor)
    }

    /**
     * Resolves the artwork bitmap used both by color extraction and Android media metadata.
     */
    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? {
        return loadThumbnailBitmap(context, track)
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
     * If the artwork is white, black, or monochrome, it returns the ashes palette.
     * If colorful, it returns the vibrant palette.
     */
    fun extractColorsFromBitmap(
        bitmap: Bitmap?,
        fallbackColor: Color = Color(0xFF880E2F)
    ): TrackThemeColors {
        if (bitmap != null) {
            val sampled = sampleDominantColor(bitmap)
            if (sampled != null) {
                return generateThemePalette(sampled)
            }
        }
        return generateThemePalette(fallbackColor)
    }

    fun extractColorsFromUri(
        context: Context,
        artworkUriString: String
    ): TrackThemeColors {
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
        } catch (_: Exception) {
            // Graceful fallback
        }
        return generateThemePalette(Color(0xFF880E2F))
    }

    fun getColorsForDrawable(
        context: Context,
        @DrawableRes resId: Int
    ): TrackThemeColors {
        try {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            if (bitmap != null) {
                val sampled = sampleDominantColor(bitmap)
                if (sampled != null) {
                    return generateThemePalette(sampled)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
        return generateThemePalette(Color(0xFF880E2F))
    }

    private fun loadThumbnailBitmap(
        context: Context,
        track: Track
    ): Bitmap? {
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
        } catch (_: Exception) {
            // Graceful fallback
        }
        return null
    }

    /**
     * Samples the dominant color of the bitmap:
     * - Accurately detects Black & White, pure Black, pure White, and Grayscale artwork.
     * - In that case, returns a dedicated Ash color tone matching the light/dark balance.
     * - For colorful artwork, extracts the dominant vibrant chromatic color as before.
     */
    fun sampleDominantColor(bitmap: Bitmap): Color? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return null

            val stepX = max(1, width / 16)
            val stepY = max(1, height / 16)

            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var totalValidSamples = 0

            var neutralCount = 0
            var darkCount = 0
            var lightCount = 0
            var totalBrightness = 0.0

            var maxVibrancy = -1f
            var vibrantColor: Color? = null
            var chromaticCount = 0
            var chromaticTotalR = 0L
            var chromaticTotalG = 0L
            var chromaticTotalB = 0L

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = (pixel shr 24) and 0xFF
                    if (a < 128) continue

                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    val brightness = (r * 0.299f + g * 0.587f + b * 0.114f)
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    val channelDiff = maxC - minC
                    val saturation = if (maxC > 0) channelDiff.toFloat() / maxC.toFloat() else 0f

                    totalR += r
                    totalG += g
                    totalB += b
                    totalBrightness += brightness
                    totalValidSamples++

                    // Grayscale / Black / White detection:
                    // Digital B&W images and JPEGs may have slight compression noise around edges.
                    // channelDiff <= 22 or saturation <= 0.15 indicates neutral tones.
                    val isNeutralPixel = channelDiff <= 22 || saturation <= 0.15f ||
                            (brightness <= 32f && channelDiff <= 26) ||
                            (brightness >= 225f && channelDiff <= 28)

                    if (isNeutralPixel) {
                        neutralCount++
                    }

                    if (brightness <= 48f) {
                        darkCount++
                    }
                    if (brightness >= 205f) {
                        lightCount++
                    }

                    // For chromatic extraction (colorful artwork)
                    if (brightness in 35.0..225.0 && channelDiff > 24 && saturation > 0.18f) {
                        chromaticCount++
                        chromaticTotalR += r
                        chromaticTotalG += g
                        chromaticTotalB += b
                        if (saturation > maxVibrancy && saturation > 0.22f) {
                            maxVibrancy = saturation
                            vibrantColor = Color(r, g, b)
                        }
                    }
                }
            }

            if (totalValidSamples == 0) return null

            val neutralRatio = neutralCount.toFloat() / totalValidSamples.toFloat()
            val chromaticRatio = chromaticCount.toFloat() / totalValidSamples.toFloat()
            val avgBrightness = (totalBrightness / totalValidSamples).toFloat()
            val darkRatio = darkCount.toFloat() / totalValidSamples.toFloat()
            val lightRatio = lightCount.toFloat() / totalValidSamples.toFloat()

            // Detect black, white, black-and-white, or grayscale artwork:
            // 1. Predominantly neutral (>68% neutral pixels and low chromatic ratio)
            // 2. Overwhelmingly dark/black (>70% dark pixels and low chromatic ratio)
            // 3. Overwhelmingly light/white (>70% light pixels and low chromatic ratio)
            // 4. Mixed high-contrast black & white (dark + light >= 65% with neutral >= 60%)
            // 5. Very low overall vibrancy across the entire canvas
            val isBlackAndWhiteArtwork = (neutralRatio >= 0.68f && chromaticRatio < 0.20f) ||
                    (darkRatio >= 0.70f && chromaticRatio < 0.15f) ||
                    (lightRatio >= 0.70f && chromaticRatio < 0.15f) ||
                    ((darkRatio + lightRatio) >= 0.65f && neutralRatio >= 0.60f && chromaticRatio < 0.15f) ||
                    (maxVibrancy < 0.22f && chromaticRatio < 0.08f)

            if (isBlackAndWhiteArtwork) {
                return getAshColorForBrightness(avgBrightness, darkRatio, lightRatio)
            }

            // Existing colorful artwork behavior:
            if (vibrantColor != null && maxVibrancy > 0.26f) {
                return vibrantColor
            }

            if (chromaticCount > 0 && chromaticRatio >= 0.12f) {
                val avgR = (chromaticTotalR / chromaticCount).toInt().coerceIn(0, 255)
                val avgG = (chromaticTotalG / chromaticCount).toInt().coerceIn(0, 255)
                val avgB = (chromaticTotalB / chromaticCount).toInt().coerceIn(0, 255)
                return Color(avgR, avgG, avgB)
            }

            val avgR = (totalR / totalValidSamples).toInt().coerceIn(0, 255)
            val avgG = (totalG / totalValidSamples).toInt().coerceIn(0, 255)
            val avgB = (totalB / totalValidSamples).toInt().coerceIn(0, 255)
            val overallRange = max(avgR, max(avgG, avgB)) - min(avgR, min(avgG, avgB))
            if (overallRange <= 22) {
                return getAshColorForBrightness(avgBrightness, darkRatio, lightRatio)
            }

            return Color(avgR, avgG, avgB)
        } catch (_: Exception) {
            // Graceful fallback
        }
        return null
    }

    /**
     * Determines the optimal ash tone representing the black / white / monochrome artwork.
     */
    fun getAshColorForBrightness(
        avgBrightness: Float,
        darkRatio: Float = 0f,
        lightRatio: Float = 0f
    ): Color {
        return when {
            darkRatio >= 0.60f || avgBrightness < 60f -> Color(0xFF636670)
            lightRatio >= 0.60f || avgBrightness > 190f -> Color(0xFF989CA8)
            avgBrightness < 120f -> Color(0xFF727682)
            else -> Color(0xFF868A96)
        }
    }

    /**
     * Generates the comprehensive theme palette:
     * - If baseColor is neutral, ash, black, or white: generates the neutral ashes theme.
     * - Otherwise, generates the rich, colorful atmospheric palette as before.
     */
    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val originalSaturation = hsv[1]
        val brightness = hsv[2]

        val argb = baseColor.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val rgbSpread = max(r, max(g, b)) - min(r, min(g, b))

        // Neutral / ash / black / white check:
        val isNeutral = originalSaturation <= 0.16f || rgbSpread <= 24

        if (isNeutral) {
            return generateNeutralAshPalette(brightness = brightness)
        }

        // Existing colorful artwork behavior
        val sat = originalSaturation.coerceIn(0.45f, 0.95f)

        // Dominant rich color
        val dominant = Color.hsv(hue, sat, 0.75f)
        // Deep secondary moody tone
        val secondary = Color.hsv(hue, (sat * 0.9f).coerceIn(0.5f, 1f), 0.16f)
        // Vivid luminous accent for active waveform bars, progress bead, and active shuffle/repeat
        val accent = Color.hsv(hue, (sat * 0.85f).coerceIn(0.50f, 0.95f), 0.98f)
        // Ambient soft glow
        val glow = Color.hsv(hue, sat, 0.88f)

        // Atmosphere gradient
        val bgTop = Color.hsv(hue, (sat * 0.72f).coerceIn(0.40f, 0.82f), 0.32f)
        val bgMidUpper = Color.hsv(hue, (sat * 0.65f).coerceIn(0.36f, 0.76f), 0.22f)
        val bgMidLower = Color.hsv(hue, (sat * 0.60f).coerceIn(0.32f, 0.70f), 0.15f)
        val bgBottom = Color.hsv(hue, (sat * 0.55f).coerceIn(0.28f, 0.65f), 0.09f)

        val darkBackground = bgBottom
        val atmosphericBloom = Color.hsv(hue, (sat * 0.78f).coerceIn(0.45f, 0.88f), 0.40f)

        // Premium gradient circle matching the atmospheric background tones with clean depth
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

    /**
     * Generates a neutral luxury "Ashes" palette for black, white, and grayscale artwork.
     * Prevents saturated crimson / nonsense fallbacks and produces a refined smoked
     * charcoal and silver-ash theme.
     */
    private fun generateNeutralAshPalette(brightness: Float): TrackThemeColors {
        val dominant = when {
            brightness < 0.30f -> Color(0xFF646772)
            brightness < 0.60f -> Color(0xFF787C88)
            brightness < 0.80f -> Color(0xFF8C909D)
            else -> Color(0xFFA0A4B2)
        }

        val secondary = Color(0xFF1E2024)

        val accent = when {
            brightness < 0.35f -> Color(0xFFD4D6DE)
            brightness < 0.70f -> Color(0xFFE2E4EB)
            else -> Color(0xFFF0F1F5)
        }

        val glow = Color(0xFFA4A8B6)

        // Refined dark smoked ash gradient
        val bgTop = Color(0xFF2C2E34)
        val bgMidUpper = Color(0xFF202227)
        val bgMidLower = Color(0xFF16171B)
        val bgBottom = Color(0xFF0E0F12)

        val darkBackground = bgBottom
        val atmosphericBloom = Color(0xFF383B44)

        val playPauseGradTop = Color(0xFF484B56)
        val playPauseGradBottom = Color(0xFF23252A)
        val playPauseCircle = playPauseGradTop
        val playPauseBorder = Color(0xFF727684).copy(alpha = 0.45f)

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
