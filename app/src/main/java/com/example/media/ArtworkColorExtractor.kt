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
     * Dynamically samples the artwork of a track and extracts its dominant
     * color palette.
     *
     * Special handling:
     * - Colorful artwork keeps the existing color extraction behavior.
     * - Black/white or mostly grayscale artwork gets a neutral ash/charcoal
     *   palette instead of being incorrectly converted to an unrelated hue.
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

    fun extractColorsFromUri(
        context: Context,
        artworkUriString: String
    ): TrackThemeColors {
        try {
            val uri = Uri.parse(artworkUriString)

            val bitmap = if (uri.scheme == "file") {
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }

                BitmapFactory.decodeFile(uri.path, opts)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }

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
            val options = BitmapFactory.Options().apply {
                inSampleSize = 8
            }

            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                resId,
                options
            )

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
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }

                    return BitmapFactory.decodeFile(uri.path, opts)
                }

                context.contentResolver
                    .openInputStream(uri)
                    ?.use { stream ->
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = 4
                        }

                        return BitmapFactory.decodeStream(
                            stream,
                            null,
                            opts
                        )
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

                        return BitmapFactory.decodeByteArray(
                            raw,
                            0,
                            raw.size,
                            options
                        )
                    }
                } finally {
                    retriever.release()
                }
            } else if (track.coverResId != 0) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 8
                }

                return BitmapFactory.decodeResource(
                    context.resources,
                    track.coverResId,
                    options
                )
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        return null
    }

    /**
     * Extracts the dominant artwork color.
     *
     * Important:
     * The extractor now detects grayscale/black-and-white artwork BEFORE
     * discarding neutral pixels.
     *
     * This prevents artwork such as:
     *
     *   █████████
     *   ░░ WHITE ░
     *   █ BLACK █
     *
     * from incorrectly falling back to a random red/crimson color.
     */
    private fun sampleDominantColor(bitmap: Bitmap): Color? {
        try {
            val width = bitmap.width
            val height = bitmap.height

            if (width <= 0 || height <= 0) {
                return null
            }

            var totalR = 0L
            var totalG = 0L
            var totalB = 0L

            var sampleCount = 0

            // Track all valid pixels so we can identify grayscale artwork.
            var grayscaleCount = 0
            var darkCount = 0
            var lightCount = 0

            var grayscaleBrightnessTotal = 0L

            var maxVibrancy = -1f
            var vibrantColor: Color? = null

            // Sample across a grid.
            val stepX = max(1, width / 12)
            val stepY = max(1, height / 12)

            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {

                    val pixel = bitmap.getPixel(x, y)

                    val a = (pixel shr 24) and 0xFF

                    if (a < 128) {
                        continue
                    }

                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    val brightness =
                        r * 0.299f +
                        g * 0.587f +
                        b * 0.114f

                    sampleCount++

                    /*
                     * Detect grayscale.
                     *
                     * The channels don't need to be perfectly identical.
                     * A tolerance of 18 allows slightly warm/cool whites,
                     * silver, charcoal and similar neutral artwork to still
                     * be treated as neutral.
                     */
                    val channelRange =
                        max(r, max(g, b)) - min(r, min(g, b))

                    val isGrayscale = channelRange <= 18

                    if (isGrayscale) {
                        grayscaleCount++
                        grayscaleBrightnessTotal += brightness.toLong()
                    }

                    if (brightness <= 45f) {
                        darkCount++
                    }

                    if (brightness >= 210f) {
                        lightCount++
                    }

                    /*
                     * Preserve the original chromatic extraction.
                     *
                     * Near-black and near-white pixels are ignored for
                     * COLOR extraction, because they don't provide useful hue.
                     */
                    if (brightness in 35.0..225.0) {
                        totalR += r
                        totalG += g
                        totalB += b

                        // Measure chromatic saturation/vibrancy.
                        val maxC = max(r, max(g, b)).toFloat()
                        val minC = min(r, min(g, b)).toFloat()

                        val saturation =
                            if (maxC > 0f) {
                                (maxC - minC) / maxC
                            } else {
                                0f
                            }

                        if (
                            saturation > maxVibrancy &&
                            saturation > 0.22f
                        ) {
                            maxVibrancy = saturation

                            vibrantColor = Color(
                                r,
                                g,
                                b
                            )
                        }
                    }
                }
            }

            if (sampleCount <= 0) {
                return null
            }

            /*
             * NEW:
             * Detect predominantly black/white/grayscale artwork.
             *
             * If at least 82% of the sampled artwork is grayscale, we don't
             * attempt to invent a hue. Instead we return an actual neutral
             * ash tone.
             */
            val grayscaleRatio =
                grayscaleCount.toFloat() / sampleCount.toFloat()

            if (grayscaleRatio >= 0.82f) {

                val averageGrayscaleBrightness =
                    if (grayscaleCount > 0) {
                        grayscaleBrightnessTotal.toFloat() /
                            grayscaleCount.toFloat()
                    } else {
                        128f
                    }

                /*
                 * Mostly black artwork:
                 *
                 * Use a deep ash/charcoal base.
                 */
                if (darkCount.toFloat() / sampleCount >= 0.55f) {
                    return Color(0xFF707070)
                }

                /*
                 * Mostly white artwork:
                 *
                 * Use a soft silver/ash base instead of pure white.
                 * Pure white would make the generated UI too bright.
                 */
                if (lightCount.toFloat() / sampleCount >= 0.55f) {
                    return Color(0xFF9A9A9A)
                }

                /*
                 * Mixed black + white artwork.
                 *
                 * This is the important case for album covers containing
                 * strong black and white sections.
                 *
                 * Return an ash color whose brightness follows the artwork.
                 */
                return when {
                    averageGrayscaleBrightness < 85f ->
                        Color(0xFF626262)

                    averageGrayscaleBrightness < 130f ->
                        Color(0xFF7A7A7A)

                    averageGrayscaleBrightness < 175f ->
                        Color(0xFF929292)

                    else ->
                        Color(0xFFA5A5A5)
                }
            }

            /*
             * EXISTING FUNCTIONALITY:
             *
             * If the artwork has a meaningful chromatic color, keep using
             * the vibrant color exactly as before.
             */
            if (
                vibrantColor != null &&
                maxVibrancy > 0.28f
            ) {
                return vibrantColor
            }

            /*
             * Existing average-color fallback.
             */
            if (sampleCount > 0) {
                val avgR =
                    (totalR / sampleCount)
                        .toInt()
                        .coerceIn(0, 255)

                val avgG =
                    (totalG / sampleCount)
                        .toInt()
                        .coerceIn(0, 255)

                val avgB =
                    (totalB / sampleCount)
                        .toInt()
                        .coerceIn(0, 255)

                /*
                 * If the average itself is nearly grayscale, return an ash
                 * color rather than accidentally producing a muddy hue.
                 */
                val averageRange =
                    max(avgR, max(avgG, avgB)) -
                        min(avgR, min(avgG, avgB))

                if (averageRange <= 14) {
                    val average =
                        ((avgR + avgG + avgB) / 3)
                            .coerceIn(45, 180)

                    return Color(
                        average,
                        average,
                        average
                    )
                }

                return Color(
                    avgR,
                    avgG,
                    avgB
                )
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        return null
    }

    fun generateThemePalette(
        baseColor: Color
    ): TrackThemeColors {

        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            baseColor.toArgb(),
            hsv
        )

        val hue = hsv[0]
        val originalSaturation = hsv[1]
        val brightness = hsv[2]

        /*
         * NEW:
         * Detect neutral colors before forcing saturation.
         *
         * The old code did:
         *
         *     sat = hsv[1].coerceIn(0.45f, 0.95f)
         *
         * That means a gray/white/black color with almost zero saturation
         * would be artificially turned into a strong hue.
         *
         * For example:
         *
         *     Gray -> saturation 0
         *     0 coerced to 0.45
         *     hue 0 -> RED
         *
         * That's why black/white artwork could become crimson/red.
         */
        val isNeutral =
            originalSaturation <= 0.12f

        if (isNeutral) {
            return generateNeutralAshPalette(
                brightness = brightness
            )
        }

        /*
         * EXISTING COLORFUL-ARTWORK BEHAVIOR
         */
        val sat =
            originalSaturation.coerceIn(
                0.45f,
                0.95f
            )

        // Dominant rich color.
        val dominant = Color.hsv(
            hue,
            sat,
            0.75f
        )

        // Deep secondary moody tone.
        val secondary = Color.hsv(
            hue,
            (sat * 0.9f).coerceIn(
                0.5f,
                1f
            ),
            0.16f
        )

        // Vivid luminous accent.
        val accent = Color.hsv(
            hue,
            (sat * 0.85f).coerceIn(
                0.50f,
                0.95f
            ),
            0.98f
        )

        // Ambient soft glow.
        val glow = Color.hsv(
            hue,
            sat,
            0.88f
        )

        // Atmospheric gradient.
        val bgTop = Color.hsv(
            hue,
            (sat * 0.72f).coerceIn(
                0.40f,
                0.82f
            ),
            0.32f
        )

        val bgMidUpper = Color.hsv(
            hue,
            (sat * 0.65f).coerceIn(
                0.36f,
                0.76f
            ),
            0.22f
        )

        val bgMidLower = Color.hsv(
            hue,
            (sat * 0.60f).coerceIn(
                0.32f,
                0.70f
            ),
            0.15f
        )

        val bgBottom = Color.hsv(
            hue,
            (sat * 0.55f).coerceIn(
                0.28f,
                0.65f
            ),
            0.09f
        )

        val darkBackground = bgBottom

        val atmosphericBloom = Color.hsv(
            hue,
            (sat * 0.78f).coerceIn(
                0.45f,
                0.88f
            ),
            0.40f
        )

        // Premium gradient circle.
        val playPauseGradTop = Color.hsv(
            hue,
            (sat * 0.76f).coerceIn(
                0.45f,
                0.88f
            ),
            0.48f
        )

        val playPauseGradBottom = Color.hsv(
            hue,
            (sat * 0.85f).coerceIn(
                0.55f,
                0.92f
            ),
            0.24f
        )

        val playPauseCircle = playPauseGradTop

        val playPauseBorder = Color.hsv(
            hue,
            sat,
            0.68f
        ).copy(alpha = 0.40f)

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
     * Generates a neutral luxury palette for black, white and grayscale
     * artwork.
     *
     * The goal is NOT to make the UI white.
     *
     * Instead:
     *
     * Artwork:
     *     Black + White
     *
     * Velvet:
     *     Charcoal + Ash + Silver
     *
     * This keeps the dark luxury appearance of the app while still matching
     * the artwork.
     */
    private fun generateNeutralAshPalette(
        brightness: Float
    ): TrackThemeColors {

        /*
         * Keep the base dark because Velvet is a dark music interface.
         */
        val dominant = when {
            brightness < 0.25f ->
                Color(0xFF666666)

            brightness < 0.50f ->
                Color(0xFF7A7A7A)

            brightness < 0.75f ->
                Color(0xFF909090)

            else ->
                Color(0xFFA5A5A5)
        }

        val secondary = Color(0xFF252525)

        /*
         * Ash/silver accent.
         *
         * This replaces the old behavior where HSV saturation could create
         * a fake red/crimson accent from grayscale artwork.
         */
        val accent = when {
            brightness < 0.30f ->
                Color(0xFFB0B0B0)

            brightness < 0.65f ->
                Color(0xFFC0C0C0)

            else ->
                Color(0xFFD0D0D0)
        }

        val glow = Color(0xFF9E9E9E)

        /*
         * Dark charcoal atmospheric gradient.
         */
        val bgTop = Color(0xFF303030)

        val bgMidUpper = Color(0xFF252525)

        val bgMidLower = Color(0xFF191919)

        val bgBottom = Color(0xFF0D0D0D)

        val darkBackground = bgBottom

        /*
         * Soft ash bloom.
         */
        val atmosphericBloom = Color(0xFF3A3A3A)

     
