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
        // Saturation: keep the exact color rich and authentic, never mix with white so it does not wash out
        val pureSat = hsv[1].coerceIn(0.55f, 0.95f)

        // 1. Card background:
        // Premium, deep, non-shiny glassmorphic tone (brightness ~0.24f-0.28f instead of 0.50f)
        val cardSat = (pureSat * 0.75f).coerceIn(0.38f, 0.75f)
        val cardValTop = 0.28f
        val cardValBottom = 0.20f
        val cardBgTop = Color.hsv(hue, cardSat, cardValTop)
        val cardBgBottom = Color.hsv(hue, cardSat, cardValBottom)
        val cardBackground = Color.hsv(hue, cardSat, 0.24f)

        // 2. Main player sheet background:
        // Ultra-deep dark shade of the extracted color (~0.08f-0.12f)
        val mainBgSat = (pureSat * 0.70f).coerceIn(0.30f, 0.65f)
        val bgTop = Color.hsv(hue, mainBgSat, 0.12f)
        val bgMidUpper = Color.hsv(hue, mainBgSat, 0.10f)
        val bgMidLower = Color.hsv(hue, mainBgSat, 0.08f)
        val bgBottom = Color.hsv(hue, mainBgSat, 0.06f)
        val darkBackground = bgBottom

        // 3. Sleek glass rim border wrapping the bottom of the card
        val borderSat = (pureSat * 0.50f).coerceIn(0.20f, 0.55f)
        val cardBorder = Color.hsv(hue, borderSat, 0.75f)

        val dominant = cardBackground
        val secondary = Color.hsv(hue, (pureSat * 0.85f).coerceIn(0.40f, 0.90f), 0.20f)
        val accent = Color.hsv(hue, (pureSat * 0.85f).coerceIn(0.50f, 0.95f), 0.95f)
        val glow = Color.hsv(hue, pureSat, 0.80f)
        val atmosphericBloom = Color.hsv(hue, pureSat.coerceIn(0.45f, 0.85f), 0.40f)
        val playPauseGradTop = Color.hsv(hue, (pureSat * 0.76f).coerceIn(0.45f, 0.88f), 0.48f)
        val playPauseGradBottom = Color.hsv(hue, (pureSat * 0.85f).coerceIn(0.55f, 0.92f), 0.24f)
        val playPauseCircle = playPauseGradTop
        val playPauseBorder = Color.hsv(hue, pureSat, 0.68f).copy(alpha = 0.40f)

        val amb1 = dominant
        val amb2 = Color.hsv((hue + 36f) % 360f, (pureSat * 0.85f).coerceIn(0.40f, 0.90f), 0.70f)
        val amb3 = Color.hsv((hue + 160f) % 360f, (pureSat * 0.75f).coerceIn(0.35f, 0.85f), 0.65f)
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
            cardBackground = cardBgTop,
            cardBackgroundBottom = cardBgBottom,
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
