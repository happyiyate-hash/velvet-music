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
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

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
        appendLine("Chromatic coverage: ${"%.2f".format(chromaticCoverage * 100.0)}%")
        appendLine("Dominant cluster: #$dominantClusterIndex")
        appendLine("Accent cluster: ${accentClusterIndex?.let { "#$it" } ?: "NONE"}")
        appendLine("CLUSTERS")
        clusters.forEach { cluster ->
            val role = when {
                cluster.selectedAsDominant -> "DOMINANT"
                cluster.selectedAsAccent -> "ACCENT"
                else -> "-"
            }
            appendLine(
                "#${cluster.index} " +
                    "coverage=${"%.2f".format(cluster.coveragePercent)}% " +
                    "chroma=${"%.4f".format(cluster.chroma)} " +
                    "dominance=${"%.4f".format(cluster.dominance)} " +
                    "neutral=${cluster.neutral} " +
                    "role=$role " +
                    "color=${cluster.color.toHex()}"
            )
        }
        appendLine("══════════════════════════════════════")
    }

    fun toDebugMap(): Map<String, Any?> = mapOf(
        "artworkIsNeutral" to artworkIsNeutral,
        "chromaticCoverage" to chromaticCoverage,
        "dominantCluster" to dominantClusterIndex,
        "accentCluster" to accentClusterIndex,
        "clusters" to clusters.map { cluster ->
            mapOf(
                "index" to cluster.index,
                "coverage" to cluster.coverage,
                "coveragePercent" to cluster.coveragePercent,
                "chroma" to cluster.chroma,
                "chromaScore" to cluster.chromaScore,
                "midToneScore" to cluster.midToneScore,
                "dominance" to cluster.dominance,
                "neutral" to cluster.neutral,
                "selectedAsDominant" to cluster.selectedAsDominant,
                "selectedAsAccent" to cluster.selectedAsAccent,
                "color" to cluster.color.toHex(),
                "oklabL" to cluster.oklabL,
                "oklabA" to cluster.oklabA,
                "oklabB" to cluster.oklabB
            )
        }
    )
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

private data class OklabColor(val l: Double, val a: Double, val b: Double) {
    val chroma: Double get() = kotlin.math.sqrt(a * a + b * b)
}

private data class Cluster(
    var center: OklabColor,
    var count: Int = 0
)

private data class ScoredCluster(
    val cluster: Cluster,
    val coverage: Double,
    val chroma: Double,
    val chromaScore: Double,
    val midToneScore: Double,
    val dominance: Double,
    val neutral: Boolean
)

/**
 * Velvet's artwork color engine.
 *
 * The selector is intentionally coverage-first. A small saturated detail cannot
 * replace a color that occupies most of the artwork. Neutral artwork is protected
 * from being hijacked by tiny blue/red/green regions.
 */
object VelvetArtworkColorEngine {
    private const val TAG = "VELVET_PALETTE"
    private const val SAMPLE_SIZE = 64
    private const val CLUSTER_COUNT = 7
    private const val KMEANS_ITERATIONS = 12
    private const val NEUTRAL_CHROMA_THRESHOLD = 0.035
    private const val ARTWORK_CHROMATIC_COVERAGE_THRESHOLD = 0.12
    private const val MIN_ACCENT_COVERAGE = 0.025

    /** Disable this for release builds if verbose palette logs are not desired. */
    var debugLoggingEnabled: Boolean = true

    fun extractColors(context: Context, track: Track): TrackThemeColors =
        extractColorsFromBitmap(resolveTrackBitmap(context, track))

    fun resolveTrackBitmap(context: Context, track: Track): Bitmap? = loadThumbnailBitmap(context, track)

    fun extractColorsFromBitmap(bitmap: Bitmap?): TrackThemeColors {
        if (bitmap == null) return fallbackTheme()

        return try {
            val pixels = sampleOklabPixels(bitmap)
            if (pixels.isEmpty()) return fallbackTheme()

            val clusters = kMeans(pixels, CLUSTER_COUNT, KMEANS_ITERATIONS)
            val totalPixels = pixels.size.toDouble()

            val scored = clusters.map { cluster ->
                val coverage = cluster.count / totalPixels
                val chroma = cluster.center.chroma
                val chromaScore = clamp(chroma / 0.25)
                val midToneScore = 1.0 - clamp(abs(cluster.center.l - 0.55) / 0.55)
                val dominance =
                    (coverage * 0.75) +
                        (chromaScore * 0.10) +
                        (midToneScore * 0.15)

                ScoredCluster(
                    cluster = cluster,
                    coverage = coverage,
                    chroma = chroma,
                    chromaScore = chromaScore,
                    midToneScore = midToneScore,
                    dominance = dominance,
                    neutral = chroma < NEUTRAL_CHROMA_THRESHOLD
                )
            }.sortedByDescending { it.dominance }

            val chromaticCoverage = pixels.count {
                it.chroma >= NEUTRAL_CHROMA_THRESHOLD
            }.toDouble() / totalPixels

            val artworkIsNeutral =
                chromaticCoverage < ARTWORK_CHROMATIC_COVERAGE_THRESHOLD

            val dominant = scored.first()

            val accent = if (artworkIsNeutral) {
                null
            } else {
                scored
                    .filter { entry ->
                        entry.cluster !== dominant.cluster &&
                            !entry.neutral &&
                            entry.coverage >= MIN_ACCENT_COVERAGE
                    }
                    .maxByOrNull { entry ->
                        val accentChroma = clamp(entry.chroma / 0.25)
                        (entry.coverage * 0.50) +
                            (accentChroma * 0.40) +
                            (entry.midToneScore * 0.10)
                    }
            }

            val debugClusters = scored.mapIndexed { index, entry ->
                VelvetClusterDebug(
                    index = index,
                    coverage = entry.coverage,
                    chroma = entry.chroma,
                    chromaScore = entry.chromaScore,
                    midToneScore = entry.midToneScore,
                    dominance = entry.dominance,
                    neutral = entry.neutral,
                    selectedAsDominant = entry.cluster === dominant.cluster,
                    selectedAsAccent = accent?.cluster === entry.cluster,
                    color = oklabToColor(entry.cluster.center),
                    oklabL = entry.cluster.center.l,
                    oklabA = entry.cluster.center.a,
                    oklabB = entry.cluster.center.b
                )
            }

            val debug = VelvetPaletteDebug(
                clusters = debugClusters,
                chromaticCoverage = chromaticCoverage,
                artworkIsNeutral = artworkIsNeutral,
                dominantClusterIndex = debugClusters.indexOfFirst { it.selectedAsDominant },
                accentClusterIndex = debugClusters.indexOfFirst { it.selectedAsAccent }
                    .takeIf { it >= 0 }
            )

            if (debugLoggingEnabled) {
                Log.d(TAG, debug.formatForLog())
            }

            buildTheme(
                dominant = oklabToColor(dominant.cluster.center),
                accent = accent?.let { oklabToColor(it.cluster.center) },
                neutralArtwork = artworkIsNeutral,
                dominantCoverage = dominant.coverage,
                accentCoverage = accent?.coverage ?: 0.0,
                debug = debug
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Palette extraction failed; using fallback", error)
            fallbackTheme()
        }
    }

    fun extractColorsFromUri(context: Context, artworkUriString: String): TrackThemeColors = try {
        val uri = Uri.parse(artworkUriString)
        val bitmap = if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path, BitmapFactory.Options().apply { inSampleSize = 4 })
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 4 })
            }
        }
        extractColorsFromBitmap(bitmap)
    } catch (_: Exception) {
        fallbackTheme()
    }

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors = try {
        val bitmap = BitmapFactory.decodeResource(
            context.resources,
            resId,
            BitmapFactory.Options().apply { inSampleSize = 8 }
        )
        extractColorsFromBitmap(bitmap)
    } catch (_: Exception) {
        fallbackTheme()
    }

    fun getDefaultBitmap(context: Context): Bitmap = Bitmap.createBitmap(
        2,
        2,
        Bitmap.Config.ARGB_8888
    ).apply {
        eraseColor(Color(0xFF18070D).toArgb())
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors =
        buildTheme(baseColor, null, false, 1.0, 0.0, null)

    private fun buildTheme(
        dominant: Color,
        accent: Color?,
        neutralArtwork: Boolean,
        dominantCoverage: Double,
        accentCoverage: Double,
        debug: VelvetPaletteDebug?
    ): TrackThemeColors {
        val dominantHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(dominant.toArgb(), dominantHsv)

        val hue = dominantHsv[0]
        val saturation = if (dominantHsv[1] > 0.05f) {
            dominantHsv[1].coerceIn(0.35f, 0.95f)
        } else {
            0f
        }

        // Keep the dominant artwork identity, but map it into Velvet's dark canvas.
        val bgTop = Color.hsv(hue, saturation, 0.22f)
        val bgMidUpper = Color.hsv(hue, saturation, 0.18f)
        val bgMidLower = Color.hsv(hue, saturation, 0.14f)
        val bgBottom = Color.hsv(hue, saturation, 0.10f)

        val resolvedAccent = when {
            neutralArtwork || accent == null -> Color.hsv(hue, saturation, 0.72f)
            else -> accent
        }

        val accentHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(resolvedAccent.toArgb(), accentHsv)
        val accentHue = accentHsv[0]
        val accentSat = if (accentHsv[1] > 0.05f) {
            accentHsv[1].coerceIn(0.35f, 0.95f)
        } else {
            saturation
        }

        val glow = Color.hsv(accentHue, accentSat, 0.72f)
        val atmosphericBloom = Color.hsv(accentHue, accentSat, 0.28f)
        val cardBorder = Color.hsv(accentHue, accentSat, 0.65f).copy(alpha = 0.45f)
        val playPauseGradTop = Color.hsv(accentHue, accentSat, 0.48f)
        val playPauseGradBottom = Color.hsv(accentHue, accentSat, 0.24f)

        return TrackThemeColors(
            dominant = bgTop,
            secondary = bgMidLower,
            accent = resolvedAccent,
            glow = glow,
            darkBackground = bgBottom,
            atmosphericBloom = atmosphericBloom,
            playPauseCircle = playPauseGradTop,
            playPauseBorder = Color.hsv(accentHue, accentSat, 0.68f).copy(alpha = 0.40f),
            bgTop = bgTop,
            bgMidUpper = bgMidUpper,
            bgMidLower = bgMidLower,
            bgBottom = bgBottom,
            playPauseGradTop = playPauseGradTop,
            playPauseGradBottom = playPauseGradBottom,
            ambient1 = bgTop,
            ambient2 = accent,
            ambient3 = resolvedAccent,
            ambientCharcoal = Color(0xFF101117),
            rawExtractedColor = dominant,
            cardBackground = bgTop,
            cardBackgroundBottom = bgMidLower,
            cardBorder = cardBorder,
            playerSheetBackground = bgTop,
            playerSheetBackgroundBottom = bgBottom,
            debug = debug,
            isNeutralArtwork = neutralArtwork,
            dominantCoverage = dominantCoverage,
            accentCoverage = accentCoverage
        )
    }

    private fun fallbackTheme(): TrackThemeColors =
        generateThemePalette(Color(0xFF880E2F))

    private fun loadThumbnailBitmap(context: Context, track: Track): Bitmap? = try {
        when {
            !track.artworkUri.isNullOrBlank() -> {
                val uri = Uri.parse(track.artworkUri)
                if (uri.scheme == "file") {
                    BitmapFactory.decodeFile(uri.path, BitmapFactory.Options().apply { inSampleSize = 4 })
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                    }
                }
            }

            track.contentUri != null -> {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(track.contentUri))
                    val raw = retriever.embeddedPicture
                    raw?.let {
                        BitmapFactory.decodeByteArray(
                            it,
                            0,
                            it.size,
                            BitmapFactory.Options().apply { inSampleSize = 4 }
                        )
                    }
                } finally {
                    retriever.release()
                }
            }

            track.coverResId != 0 -> BitmapFactory.decodeResource(
                context.resources,
                track.coverResId,
                BitmapFactory.Options().apply { inSampleSize = 8 }
            )

            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun sampleOklabPixels(bitmap: Bitmap): List<OklabColor> {
        val size = min(bitmap.width, bitmap.height)
        if (size <= 0) return emptyList()

        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, left, top, size, size)
        val sampled = Bitmap.createScaledBitmap(cropped, SAMPLE_SIZE, SAMPLE_SIZE, true)

        if (cropped !== bitmap) cropped.recycle()

        val result = ArrayList<OklabColor>(SAMPLE_SIZE * SAMPLE_SIZE)
        for (y in 0 until SAMPLE_SIZE) {
            for (x in 0 until SAMPLE_SIZE) {
                val pixel = sampled.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                val r = ((pixel ushr 16) and 0xFF) / 255.0
                val g = ((pixel ushr 8) and 0xFF) / 255.0
                val b = (pixel and 0xFF) / 255.0
                result += rgbToOklab(r, g, b)
            }
        }

        if (sampled !== bitmap) sampled.recycle()
        return result
    }

    private fun kMeans(
        pixels: List<OklabColor>,
        clusterCount: Int,
        iterations: Int
    ): List<Cluster> {
        val count = min(clusterCount, pixels.size)
        val random = Random(42)
        val centers = MutableList(count) {
            pixels[random.nextInt(pixels.size)]
        }.toMutableList()

        repeat(iterations) {
            val buckets = Array(count) { mutableListOf<OklabColor>() }

            pixels.forEach { pixel ->
                var nearest = 0
                var nearestDistance = Double.MAX_VALUE

                centers.forEachIndexed { index, center ->
                    val distance = distanceSquared(pixel, center)
                    if (distance < nearestDistance) {
                        nearestDistance = distance
                        nearest = index
                    }
                }

                buckets[nearest] += pixel
            }

            centers.indices.forEach { index ->
                val bucket = buckets[index]
                if (bucket.isEmpty()) return@forEach

                centers[index] = OklabColor(
                    bucket.sumOf { it.l } / bucket.size,
                    bucket.sumOf { it.a } / bucket.size,
                    bucket.sumOf { it.b } / bucket.size
                )
            }
        }

        val counts = IntArray(count)
        pixels.forEach { pixel ->
            var nearest = 0
            var nearestDistance = Double.MAX_VALUE
            centers.forEachIndexed { index, center ->
                val distance = distanceSquared(pixel, center)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearest = index
                }
            }
            counts[nearest]++
        }

        return centers.mapIndexed { index, center ->
            Cluster(center = center, count = counts[index])
        }.filter { it.count > 0 }
    }

    private fun distanceSquared(a: OklabColor, b: OklabColor): Double {
        val dl = a.l - b.l
        val da = a.a - b.a
        val db = a.b - b.b
        return (dl * dl) + (da * da) + (db * db)
    }

    private fun rgbToOklab(r: Double, g: Double, b: Double): OklabColor {
        val rl = linearize(r)
        val gl = linearize(g)
        val bl = linearize(b)

        val l = (0.4122214708 * rl) + (0.5363325363 * gl) + (0.0514459929 * bl)
        val m = (0.2119034982 * rl) + (0.6806995451 * gl) + (0.1073969566 * bl)
        val s = (0.0883024619 * rl) + (0.2817188376 * gl) + (0.6299787005 * bl)

        val lRoot = cbrt(l)
        val mRoot = cbrt(m)
        val sRoot = cbrt(s)

        return OklabColor(
            (0.2104542553 * lRoot) + (0.7936177850 * mRoot) - (0.0040720468 * sRoot),
            (1.9779984951 * lRoot) - (2.4285922050 * mRoot) + (0.4505937099 * sRoot),
            (0.0259040371 * lRoot) + (0.7827717662 * mRoot) - (0.8086757660 * sRoot)
        )
    }

    private fun oklabToColor(lab: OklabColor): Color {
        val l = (lab.l + (0.3963377774 * lab.a) + (0.2158037573 * lab.b)).pow(3.0)
        val m = (lab.l - (0.1055613458 * lab.a) - (0.0638541728 * lab.b)).pow(3.0)
        val s = (lab.l - (0.0894841775 * lab.a) - (1.2914855480 * lab.b)).pow(3.0)

        val r = clamp(
            (4.0767416621 * l) - (3.3077115913 * m) + (0.2309699292 * s)
        )
        val g = clamp(
            (-1.2684380046 * l) + (2.6097574011 * m) - (0.3413193965 * s)
        )
        val b = clamp(
            (-0.0041960863 * l) - (0.7034186147 * m) + (1.7076147010 * s)
        )

        return Color(
            (delinearize(r) * 255.0).roundToIntSafe(),
            (delinearize(g) * 255.0).roundToIntSafe(),
            (delinearize(b) * 255.0).roundToIntSafe()
        )
    }

    private fun linearize(value: Double): Double =
        if (value <= 0.04045) value / 12.92
        else ((value + 0.055) / 1.055).pow(2.4)

    private fun delinearize(value: Double): Double =
        if (value <= 0.0031308) 12.92 * value
        else (1.055 * value.pow(1.0 / 2.4)) - 0.055

    private fun clamp(value: Double): Double = value.coerceIn(0.0, 1.0)

    private fun Double.roundToIntSafe(): Int =
        (this.coerceIn(0.0, 255.0) + 0.5).toInt()

    private fun Color.toHex(): String =
        "#${toArgb().toUInt().toString(16).padStart(8, '0')}"
}
