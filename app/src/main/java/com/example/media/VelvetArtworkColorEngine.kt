package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.Track
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/** Per-cluster diagnostics exposed for Velvet's developer/debug tools. */
data class VelvetClusterDebug(
    val index: Int,
    val coverage: Double,
    val chroma: Double,
    val chromaScore: Double,
    val midToneScore: Double,
    val dominance: Double,
    val neutral: Boolean,
    val selectedAsDominant: Boolean,
    val selectedAsAccent: Boolean,
    val color: Color,
    val oklabL: Double,
    val oklabA: Double,
    val oklabB: Double
) {
    val coveragePercent: Double get() = coverage * 100.0
}

data class VelvetPaletteDebug(
    val clusters: List<VelvetClusterDebug>,
    val chromaticCoverage: Double,
    val artworkIsNeutral: Boolean,
    val dominantClusterIndex: Int,
    val accentClusterIndex: Int?
) {
    fun formatForLog(): String = buildString {
        appendLine("════════ VELVET PALETTE DEBUG ════════")
        appendLine("Artwork neutral: $artworkIsNeutral")
        appendLine("Chromatic coverage: ${f(chromaticCoverage * 100.0)}%")
        appendLine("Dominant cluster: #$dominantClusterIndex")
        appendLine("Accent cluster: ${accentClusterIndex?.let { "#$it" } ?: "NONE"}")
        appendLine("CLUSTERS")
        clusters.forEach { c ->
            val role = when {
                c.selectedAsDominant -> "DOMINANT"
                c.selectedAsAccent -> "ACCENT"
                else -> "-"
            }
            appendLine(
                "#${c.index} | coverage=${f(c.coveragePercent)}% " +
                    "| chroma=${f4(c.chroma)} | dominance=${f4(c.dominance)} " +
                    "| neutral=${c.neutral} | role=$role | color=${c.color.toHex()}"
            )
        }
        appendLine("══════════════════════════════════════")
    }

    fun toDebugMap(): Map<String, Any?> = mapOf(
        "artworkIsNeutral" to artworkIsNeutral,
        "chromaticCoverage" to chromaticCoverage,
        "dominantCluster" to dominantClusterIndex,
        "accentCluster" to accentClusterIndex,
        "clusters" to clusters.map { c ->
            mapOf(
                "index" to c.index,
                "coverage" to c.coverage,
                "coveragePercent" to c.coveragePercent,
                "chroma" to c.chroma,
                "chromaScore" to c.chromaScore,
                "midToneScore" to c.midToneScore,
                "dominance" to c.dominance,
                "neutral" to c.neutral,
                "selectedAsDominant" to c.selectedAsDominant,
                "selectedAsAccent" to c.selectedAsAccent,
                "color" to c.color.toHex(),
                "oklabL" to c.oklabL,
                "oklabA" to c.oklabA,
                "oklabB" to c.oklabB
            )
        }
    )

    private fun f(value: Double) = String.format(Locale.US, "%.2f", value)
    private fun f4(value: Double) = String.format(Locale.US, "%.4f", value)
}

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
    val playerSheetBackgroundBottom: Color = bgBottom,
    val debug: VelvetPaletteDebug? = null,
    val isNeutralArtwork: Boolean = false,
    val dominantCoverage: Double = 0.0,
    val accentCoverage: Double = 0.0
)

private data class Lab(val l: Double, val a: Double, val b: Double) {
    val chroma get() = Math.sqrt(a * a + b * b)
}

private data class Cluster(var center: Lab, var count: Int = 0)

private data class Scored(
    val cluster: Cluster,
    val coverage: Double,
    val chroma: Double,
    val chromaScore: Double,
    val midToneScore: Double,
    val dominance: Double,
    val neutral: Boolean
)

/**
 * OKLab + coverage-first artwork extraction used by the existing
 * ArtworkColorExtractor facade.
 */
object VelvetArtworkColorEngine {
    private const val TAG = "VELVET_PALETTE"
    private const val SAMPLE = 64
    private const val K = 7
    private const val ITERATIONS = 12
    private const val NEUTRAL_CHROMA = 0.035
    private const val NEUTRAL_COVERAGE = 0.12
    private const val MIN_ACCENT_COVERAGE = 0.025

    var debugLoggingEnabled = true

    fun extractColors(context: Context, track: Track): TrackThemeColors =
        extractColorsFromBitmap(resolveTrackBitmap(context, track))

    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? = try {
        when {
            !track.artworkUri.isNullOrBlank() -> decodeUri(context, Uri.parse(track.artworkUri), 4)
            track.contentUri != null -> {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(track.contentUri))
                    retriever.embeddedPicture?.let {
                        BitmapFactory.decodeByteArray(it, 0, it.size, BitmapFactory.Options().apply { inSampleSize = 4 })
                    }
                } finally {
                    retriever.release()
                }
            }
            track.coverResId != 0 -> BitmapFactory.decodeResource(
                context.resources, track.coverResId,
                BitmapFactory.Options().apply { inSampleSize = 8 }
            )
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    fun extractColorsFromBitmap(bitmap: Bitmap?): TrackThemeColors {
        if (bitmap == null) return fallback()
        return try {
            val pixels = samplePixels(bitmap)
            if (pixels.isEmpty()) return fallback()

            val clusters = kMeans(pixels)
            val total = pixels.size.toDouble()
            val scored = clusters.map { cluster ->
                val coverage = cluster.count / total
                val chroma = cluster.center.chroma
                val chromaScore = clamp(chroma / 0.25)
                val midToneScore = 1.0 - clamp(abs(cluster.center.l - 0.55) / 0.55)
                // Coverage dominates. Saturation can refine, but never hijack.
                val dominance = coverage * 0.75 + chromaScore * 0.10 + midToneScore * 0.15
                Scored(
                    cluster, coverage, chroma, chromaScore, midToneScore,
                    dominance, chroma < NEUTRAL_CHROMA
                )
            }.sortedByDescending { it.dominance }

            val chromaticCoverage = pixels.count { it.chroma >= NEUTRAL_CHROMA }.toDouble() / total
            val neutralArtwork = chromaticCoverage < NEUTRAL_COVERAGE
            val dominant = scored.first()

            // Accent selection has a different objective: meaningful chroma + meaningful area.
            // It is disabled for mostly-neutral artwork so a tiny blue/green/red detail cannot win.
            val accent = if (neutralArtwork) null else scored
                .filter { it.cluster !== dominant.cluster && !it.neutral && it.coverage >= MIN_ACCENT_COVERAGE }
                .maxByOrNull {
                    clamp(it.chroma / 0.25) * 0.40 +
                        it.coverage * 0.50 +
                        it.midToneScore * 0.10
                }

            val debugClusters = scored.mapIndexed { index, s ->
                VelvetClusterDebug(
                    index = index,
                    coverage = s.coverage,
                    chroma = s.chroma,
                    chromaScore = s.chromaScore,
                    midToneScore = s.midToneScore,
                    dominance = s.dominance,
                    neutral = s.neutral,
                    selectedAsDominant = s.cluster === dominant.cluster,
                    selectedAsAccent = accent?.cluster === s.cluster,
                    color = toColor(s.cluster.center),
                    oklabL = s.cluster.center.l,
                    oklabA = s.cluster.center.a,
                    oklabB = s.cluster.center.b
                )
            }

            val debug = VelvetPaletteDebug(
                clusters = debugClusters,
                chromaticCoverage = chromaticCoverage,
                artworkIsNeutral = neutralArtwork,
                dominantClusterIndex = debugClusters.indexOfFirst { it.selectedAsDominant },
                accentClusterIndex = debugClusters.indexOfFirst { it.selectedAsAccent }.takeIf { it >= 0 }
            )

            if (debugLoggingEnabled) Log.d(TAG, debug.formatForLog())

            buildTheme(
                dominant = toColor(dominant.cluster.center),
                accent = accent?.let { toColor(it.cluster.center) },
                neutral = neutralArtwork,
                dominantCoverage = dominant.coverage,
                accentCoverage = accent?.coverage ?: 0.0,
                debug = debug
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Palette extraction failed; using fallback", error)
            fallback()
        }
    }

    fun extractColorsFromUri(context: Context, artworkUriString: String): TrackThemeColors = try {
        extractColorsFromBitmap(decodeUri(context, Uri.parse(artworkUriString), 4))
    } catch (_: Exception) {
        fallback()
    }

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors = try {
        extractColorsFromBitmap(
            BitmapFactory.decodeResource(
                context.resources, resId,
                BitmapFactory.Options().apply { inSampleSize = 8 }
            )
        )
    } catch (_: Exception) {
        fallback()
    }

    fun getDefaultBitmap(context: Context): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color(0xFF18070D).toArgb())
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors =
        buildTheme(baseColor, null, false, 1.0, 0.0, null)

    private fun decodeUri(context: Context, uri: Uri, sample: Int): Bitmap? =
        if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } else {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }

    private fun samplePixels(bitmap: Bitmap): List<Lab> {
        val side = min(bitmap.width, bitmap.height)
        if (side <= 0) return emptyList()
        val cropped = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, SAMPLE, SAMPLE, true)
        if (cropped !== bitmap) cropped.recycle()

        val result = ArrayList<Lab>(SAMPLE * SAMPLE)
        for (y in 0 until SAMPLE) for (x in 0 until SAMPLE) {
            val pixel = scaled.getPixel(x, y)
            if (((pixel ushr 24) and 0xFF) < 128) continue
            result += rgbToOklab(
                ((pixel ushr 16) and 0xFF) / 255.0,
                ((pixel ushr 8) and 0xFF) / 255.0,
                (pixel and 0xFF) / 255.0
            )
        }
        if (scaled !== bitmap) scaled.recycle()
        return result
    }

    private fun kMeans(pixels: List<Lab>): List<Cluster> {
        val count = min(K, pixels.size)
        val random = Random(42)
        val centers = MutableList(count) { pixels[random.nextInt(pixels.size)] }.toMutableList()

        repeat(ITERATIONS) {
            val buckets = Array(count) { ArrayList<Lab>() }
            for (pixel in pixels) {
                var best = 0
                var bestDistance = Double.MAX_VALUE
                for (i in centers.indices) {
                    val d = distance(pixel, centers[i])
                    if (d < bestDistance) {
                        bestDistance = d
                        best = i
                    }
                }
                buckets[best].add(pixel)
            }
            for (i in centers.indices) {
                val bucket = buckets[i]
                if (bucket.isEmpty()) continue
                centers[i] = Lab(
                    bucket.sumOf { it.l } / bucket.size,
                    bucket.sumOf { it.a } / bucket.size,
                    bucket.sumOf { it.b } / bucket.size
                )
            }
        }

        val counts = IntArray(count)
        for (pixel in pixels) {
            var best = 0
            var bestDistance = Double.MAX_VALUE
            for (i in centers.indices) {
                val d = distance(pixel, centers[i])
                if (d < bestDistance) {
                    bestDistance = d
                    best = i
                }
            }
            counts[best]++
        }
        return centers.mapIndexed { i, center -> Cluster(center, counts[i]) }.filter { it.count > 0 }
    }

    private fun distance(a: Lab, b: Lab): Double {
        val dl = a.l - b.l
        val da = a.a - b.a
        val db = a.b - b.b
        return dl * dl + da * da + db * db
    }

    private fun rgbToOklab(r: Double, g: Double, b: Double): Lab {
        val rl = linear(r)
        val gl = linear(g)
        val bl = linear(b)
        val l = 0.4122214708 * rl + 0.5363325363 * gl + 0.0514459929 * bl
        val m = 0.2119034982 * rl + 0.6806995451 * gl + 0.1073969566 * bl
        val s = 0.0883024619 * rl + 0.2817188376 * gl + 0.6299787005 * bl
        val lr = Math.cbrt(l)
        val mr = Math.cbrt(m)
        val sr = Math.cbrt(s)
        return Lab(
            0.2104542553 * lr + 0.7936177850 * mr - 0.0040720468 * sr,
            1.9779984951 * lr - 2.4285922050 * mr + 0.4505937099 * sr,
            0.0259040371 * lr + 0.7827717662 * mr - 0.8086757660 * sr
        )
    }

    private fun toColor(lab: Lab): Color {
        val l = Math.pow(lab.l + 0.3963377774 * lab.a + 0.2158037573 * lab.b, 3.0)
        val m = Math.pow(lab.l - 0.1055613458 * lab.a - 0.0638541728 * lab.b, 3.0)
        val s = Math.pow(lab.l - 0.0894841775 * lab.a - 1.2914855480 * lab.b, 3.0)
        val r = clamp(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s)
        val g = clamp(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s)
        val b = clamp(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)
        return Color(gamma(r), gamma(g), gamma(b))
    }

    private fun linear(v: Double): Double =
        if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)

    private fun gamma(v: Double): Int {
        val encoded = if (v <= 0.0031308) 12.92 * v else 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055
        return (encoded.coerceIn(0.0, 1.0) * 255.0 + 0.5).toInt()
    }

    private fun buildTheme(
        dominant: Color,
        accent: Color?,
        neutral: Boolean,
        dominantCoverage: Double,
        accentCoverage: Double,
        debug: VelvetPaletteDebug?
    ): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(dominant.toArgb(), hsv)
        val hue = hsv[0]
        val sat = if (hsv[1] > 0.05f) hsv[1].coerceIn(0.35f, 0.95f) else 0f
        val bgTop = Color.hsv(hue, sat, 0.22f)
        val bgMidUpper = Color.hsv(hue, sat, 0.18f)
        val bgMidLower = Color.hsv(hue, sat, 0.14f)
        val bgBottom = Color.hsv(hue, sat, 0.10f)

        val resolvedAccent = if (neutral || accent == null) Color.hsv(hue, sat, 0.72f) else accent
        val accentHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(resolvedAccent.toArgb(), accentHsv)
        val accentHue = accentHsv[0]
        val accentSat = if (accentHsv[1] > 0.05f) accentHsv[1].coerceIn(0.35f, 0.95f) else sat
        val glow = Color.hsv(accentHue, accentSat, 0.72f)
        val bloom = Color.hsv(accentHue, accentSat, 0.28f)
        val playTop = Color.hsv(accentHue, accentSat, 0.48f)
        val playBottom = Color.hsv(accentHue, accentSat, 0.24f)

        return TrackThemeColors(
            dominant = bgTop,
            secondary = bgMidLower,
            accent = resolvedAccent,
            glow = glow,
            darkBackground = bgBottom,
            atmosphericBloom = bloom,
            playPauseCircle = playTop,
            playPauseBorder = Color.hsv(accentHue, accentSat, 0.68f).copy(alpha = 0.40f),
            bgTop = bgTop,
            bgMidUpper = bgMidUpper,
            bgMidLower = bgMidLower,
            bgBottom = bgBottom,
            playPauseGradTop = playTop,
            playPauseGradBottom = playBottom,
            ambient1 = bgTop,
            ambient2 = resolvedAccent,
            ambient3 = resolvedAccent,
            ambientCharcoal = Color(0xFF101117),
            rawExtractedColor = dominant,
            cardBackground = bgTop,
            cardBackgroundBottom = bgMidLower,
            cardBorder = Color.hsv(accentHue, accentSat, 0.65f).copy(alpha = 0.45f),
            playerSheetBackground = bgTop,
            playerSheetBackgroundBottom = bgBottom,
            debug = debug,
            isNeutralArtwork = neutral,
            dominantCoverage = dominantCoverage,
            accentCoverage = accentCoverage
        )
    }

    private fun fallback(): TrackThemeColors = generateThemePalette(Color(0xFF880E2F))
    private fun clamp(v: Double) = v.coerceIn(0.0, 1.0)
    private fun Color.toHex() = "#${toArgb().toUInt().toString(16).padStart(8, '0')}"
}
