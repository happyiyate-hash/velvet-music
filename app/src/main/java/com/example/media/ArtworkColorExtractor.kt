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
    val playPauseGradBottom: Color = Color(0xFF2E0C15),
    val ambient1: Color = dominant,
    val ambient2: Color = secondary,
    val ambient3: Color = accent,
    val ambientCharcoal: Color = Color(0xFF101117),
    val rawExtractedColor: Color = dominant,
    val cardBackground: Color = dominant,
    val cardBackgroundBottom: Color = dominant,
    val cardBorder: Color = accent,
    val playerSheetBackground: Color = darkBackground,
    val playerSheetBackgroundBottom: Color = bgBottom
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
            if (sampled != null) {
                val baseTheme = generateThemePalette(sampled)
                val distinctPalette = extractDistinctPalette(bitmap, sampled)
                val amb1 = distinctPalette.getOrNull(0) ?: baseTheme.dominant
                val amb2 = distinctPalette.getOrNull(1) ?: baseTheme.secondary
                val amb3 = distinctPalette.getOrNull(2) ?: baseTheme.accent
                return baseTheme.copy(
                    ambient1 = amb1,
                    ambient2 = amb2,
                    ambient3 = amb3,
                    ambientCharcoal = Color(0xFF101117)
                )
            }
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

            // 1. Use Android Palette's quantization if available
            val palette = try {
                Palette.from(bitmap).maximumColorCount(32).generate()
            } catch (_: Throwable) {
                null
            }

            if (palette != null) {
                val swatches = palette.swatches
                if (swatches.isNotEmpty()) {
                    val hsv = FloatArray(3)

                    // Find swatches that carry true visual color (filter out pure black/white)
                    val coloredSwatches = swatches.filter { s ->
                        android.graphics.Color.colorToHSV(s.rgb, hsv)
                        val sat = hsv[1]
                        val value = hsv[2]
                        sat >= 0.16f && value in 0.12f..0.94f
                    }

                    if (coloredSwatches.isNotEmpty()) {
                        // If dominantSwatch has clear visual saturation, use it
                        val dominant = palette.dominantSwatch
                        if (dominant != null) {
                            android.graphics.Color.colorToHSV(dominant.rgb, hsv)
                            if (hsv[1] >= 0.28f && hsv[2] in 0.15f..0.90f) {
                                return Color(dominant.rgb)
                            }
                        }

                        val vibrantCandidates = listOfNotNull(
                            palette.vibrantSwatch,
                            palette.darkVibrantSwatch,
                            palette.lightVibrantSwatch
                        ).filter { s ->
                            android.graphics.Color.colorToHSV(s.rgb, hsv)
                            hsv[1] >= 0.25f && hsv[2] in 0.15f..0.90f
                        }

                        // Score by population and saturation: pick the most prominent colored visual swatch
                        val best = (coloredSwatches + vibrantCandidates).distinctBy { it.rgb }.maxByOrNull { s ->
                            android.graphics.Color.colorToHSV(s.rgb, hsv)
                            val sat = hsv[1]
                            val isVibrantBonus = if (s == palette.vibrantSwatch || s == palette.darkVibrantSwatch) 1.5 else 1.0
                            s.population.toDouble() * (sat.toDouble() + 0.35) * isVibrantBonus
                        }

                        if (best != null) {
                            return Color(best.rgb)
                        }
                    } else {
                        // Monochromatic/grayscale artwork: pick dominant swatch
                        palette.dominantSwatch?.let { return Color(it.rgb) }
                    }
                }
            }

            // 2. Direct pixel sampling with hue clustering fallback
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var sampleCount = 0
            val hsvTemp = FloatArray(3)

            class HueBucket(var count: Int = 0, var totalR: Long = 0, var totalG: Long = 0, var totalB: Long = 0, var maxSat: Float = 0f)
            val buckets = Array(12) { HueBucket() }

            val stepX = max(1, width / 16)
            val stepY = max(1, height / 16)
            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = (pixel ushr 24) and 0xFF
                    if (a < 128) continue
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    android.graphics.Color.RGBToHSV(r, g, b, hsvTemp)
                    val sat = hsvTemp[1]
                    val value = hsvTemp[2]
                    if (sat >= 0.18f && value in 0.12f..0.92f) {
                        val bucketIdx = ((hsvTemp[0] / 30f).toInt()).coerceIn(0, 11)
                        buckets[bucketIdx].count++
                        buckets[bucketIdx].totalR += r
                        buckets[bucketIdx].totalG += g
                        buckets[bucketIdx].totalB += b
                        if (sat > buckets[bucketIdx].maxSat) {
                            buckets[bucketIdx].maxSat = sat
                        }
                    }
                    totalR += r
                    totalG += g
                    totalB += b
                    sampleCount++
                }
            }

            val bestBucket = buckets.maxByOrNull { it.count * (it.maxSat + 0.2f) }
            if (bestBucket != null && bestBucket.count > 0) {
                Color(
                    (bestBucket.totalR / bestBucket.count).toInt().coerceIn(0, 255),
                    (bestBucket.totalG / bestBucket.count).toInt().coerceIn(0, 255),
                    (bestBucket.totalB / bestBucket.count).toInt().coerceIn(0, 255)
                )
            } else if (sampleCount > 0) {
                Color(
                    (totalR / sampleCount).toInt().coerceIn(0, 255),
                    (totalG / sampleCount).toInt().coerceIn(0, 255),
                    (totalB / sampleCount).toInt().coerceIn(0, 255)
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val rawSat = hsv[1]

        // YouTube Music Artwork Background Color System:
        // 1. Preserve exact hue and character of the artwork. Do NOT blend with another color,
        //    do NOT neutralize it, and do NOT replace it with generic dark/brown/red.
        // 2. Preserve saturation: keep enough saturation so it never washes out or grays.
        val sat = if (rawSat > 0.05f) rawSat.coerceIn(0.55f, 0.95f) else 0f

        // 3. Reduce ONLY brightness/luminance to create a rich, dark player background.
        //    Provide subtle tonal variation across the player sheet from top to bottom
        //    using the EXACT same hue and saturation, creating natural atmospheric depth:
        //    - Top (header & status bar): slightly more luminous (~0.22f)
        //    - Mid-upper (around album art): ~0.18f
        //    - Mid-lower (around title, artist, progress): ~0.14f
        //    - Bottom (around playback controls & handle): ~0.10f
        val bgTop = Color.hsv(hue, sat, 0.22f)
        val bgMidUpper = Color.hsv(hue, sat, 0.18f)
        val bgMidLower = Color.hsv(hue, sat, 0.14f)
        val bgBottom = Color.hsv(hue, sat, 0.10f)

        val darkBackground = bgBottom
        val dominant = bgTop
        val secondary = bgMidLower
        val accent = Color.hsv(hue, sat, 0.95f)
        val glow = Color.hsv(hue, sat, 0.75f)
        val atmosphericBloom = Color.hsv(hue, sat, 0.28f)
        val cardBorder = Color.hsv(hue, sat, 0.65f).copy(alpha = 0.45f)

        val playPauseGradTop = Color.hsv(hue, (sat * 0.85f).coerceIn(0.45f, 0.88f), 0.48f)
        val playPauseGradBottom = Color.hsv(hue, (sat * 0.90f).coerceIn(0.55f, 0.92f), 0.24f)
        val playPauseCircle = playPauseGradTop
        val playPauseBorder = Color.hsv(hue, sat, 0.68f).copy(alpha = 0.40f)

        val amb1 = dominant
        val amb2 = Color.hsv((hue + 25f) % 360f, (sat * 0.85f).coerceIn(0.40f, 0.90f), 0.70f)
        val amb3 = Color.hsv((hue + 140f) % 360f, (sat * 0.75f).coerceIn(0.35f, 0.85f), 0.65f)
        val ambientCharcoal = Color(0xFF101117)

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
            playPauseGradBottom = playPauseGradBottom,
            ambient1 = amb1,
            ambient2 = amb2,
            ambient3 = amb3,
            ambientCharcoal = ambientCharcoal,
            rawExtractedColor = baseColor,
            cardBackground = bgTop,
            cardBackgroundBottom = bgMidLower,
            cardBorder = cardBorder,
            playerSheetBackground = bgTop,
            playerSheetBackgroundBottom = bgBottom
        )
    }

    private fun extractDistinctPalette(bitmap: Bitmap, primaryColor: Color): List<Color> {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return emptyList()

            val distinct = mutableListOf(primaryColor)
            val primaryHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primaryColor.toArgb(), primaryHsv)

            class HueBucket(var count: Int = 0, var totalR: Long = 0, var totalG: Long = 0, var totalB: Long = 0, var maxSat: Float = 0f)
            val buckets = Array(12) { HueBucket() }
            val tempHsv = FloatArray(3)

            val stepX = max(1, width / 16)
            val stepY = max(1, height / 16)
            for (x in 0 until width step stepX) {
                for (y in 0 until height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val a = (pixel ushr 24) and 0xFF
                    if (a < 128) continue
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = r * 0.299f + g * 0.587f + b * 0.114f
                    if (brightness in 25.0..235.0) {
                        android.graphics.Color.RGBToHSV(r, g, b, tempHsv)
                        val sat = tempHsv[1]
                        val hue = tempHsv[0]
                        if (sat > 0.18f) {
                            val bucketIndex = ((hue / 30f).toInt()).coerceIn(0, 11)
                            buckets[bucketIndex].count++
                            buckets[bucketIndex].totalR += r
                            buckets[bucketIndex].totalG += g
                            buckets[bucketIndex].totalB += b
                            if (sat > buckets[bucketIndex].maxSat) {
                                buckets[bucketIndex].maxSat = sat
                            }
                        }
                    }
                }
            }

            val rankedBuckets = buckets.indices
                .filter { buckets[it].count > 0 }
                .sortedByDescending { buckets[it].count * (1f + buckets[it].maxSat * 1.5f) }

            for (bIdx in rankedBuckets) {
                val b = buckets[bIdx]
                val avgR = (b.totalR / b.count).toInt().coerceIn(0, 255)
                val avgG = (b.totalG / b.count).toInt().coerceIn(0, 255)
                val avgB = (b.totalB / b.count).toInt().coerceIn(0, 255)
                val candidate = Color(avgR, avgG, avgB)
                android.graphics.Color.RGBToHSV(avgR, avgG, avgB, tempHsv)

                val isDistinct = distinct.none { existing ->
                    val exHsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(existing.toArgb(), exHsv)
                    val diff = abs(tempHsv[0] - exHsv[0])
                    val hueDist = min(diff, 360f - diff)
                    hueDist < 30f
                }
                if (isDistinct) {
                    distinct.add(candidate)
                    if (distinct.size >= 3) break
                }
            }

            if (distinct.size < 2) {
                val secHue = (primaryHsv[0] + 36f) % 360f
                val secSat = (primaryHsv[1] * 0.85f).coerceIn(0.40f, 0.90f)
                distinct.add(Color.hsv(secHue, secSat, 0.70f))
            }
            if (distinct.size < 3) {
                val tertHue = (primaryHsv[0] + 160f) % 360f
                val tertSat = (primaryHsv[1] * 0.75f).coerceIn(0.35f, 0.85f)
                distinct.add(Color.hsv(tertHue, tertSat, 0.65f))
            }

            distinct
        } catch (_: Exception) {
            emptyList()
        }
    }
}
