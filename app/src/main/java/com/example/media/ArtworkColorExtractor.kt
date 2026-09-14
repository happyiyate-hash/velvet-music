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

data class ArtworkColors(
    val background: Int,
    val accent: Int
)

sealed class ArtworkSource {
    data class UriSource(val uri: String) : ArtworkSource()
    data class BitmapSource(val bitmap: Bitmap) : ArtworkSource()
    data class ResourceSource(val resId: Int) : ArtworkSource()
}

data class TrackThemeColors(
    val dominant: Color,
    val secondary: Color,
    val accent: Color,
    val glow: Color,
    // One uniform artwork-derived surface color is used by the PlayerSheet from top to bottom.
    // Keeping this as a named surface color prevents the artwork fade/brush from becoming a
    // separate darker layer during drag.
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

    /**
     * Extracts adaptive artwork colors using Android's Palette API.
     * Supports colorful artwork as well as black & white, grayscale, and muted neutral covers
     * via dominantSwatch, darkMutedSwatch, and lightMutedSwatch inspection.
     */
    fun extractAdaptiveArtworkColors(bitmap: Bitmap): ArtworkColors {
        val palette = Palette.from(bitmap).generate()

        // 1. Try vibrant/dark vibrant first for colorful art
        val vibrantColor = palette.getVibrantColor(0)
        val darkVibrantColor = palette.getDarkVibrantColor(0)

        // 2. Extract Dominant Swatch (Handles Black & White / Grayscale Artwork)
        val dominantSwatch = palette.dominantSwatch
        val darkMutedSwatch = palette.darkMutedSwatch
        val lightMutedSwatch = palette.lightMutedSwatch

        // 3. Fallback hierarchy to guarantee dark/light background match
        val primaryBg = when {
            darkVibrantColor != 0 -> darkVibrantColor
            darkMutedSwatch != null -> darkMutedSwatch.rgb
            dominantSwatch != null -> dominantSwatch.rgb
            else -> 0xFF121212.toInt() // Dark glassmorphism dark mode fallback
        }

        val accentColor = when {
            vibrantColor != 0 -> vibrantColor
            lightMutedSwatch != null -> lightMutedSwatch.rgb
            dominantSwatch != null -> dominantSwatch.rgb
            else -> 0xFFE91E63.toInt()
        }

        return ArtworkColors(background = primaryBg, accent = accentColor)
    }

    /**
     * Converts any ArtworkSource to a non-null Bitmap for Palette, System Media Session,
     * Dynamic Island, and UI renderers.
     */
    fun resolveToBitmap(context: Context, source: ArtworkSource): Bitmap {
        return when (source) {
            is ArtworkSource.BitmapSource -> source.bitmap
            is ArtworkSource.UriSource -> {
                loadBitmapFromUri(context, source.uri) ?: getDefaultBitmap(context)
            }
            is ArtworkSource.ResourceSource -> {
                val resId = if (source.resId != 0) source.resId else R.drawable.art_luminous_echoes
                try {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    BitmapFactory.decodeResource(context.resources, resId, opts) ?: getDefaultBitmap(context)
                } catch (_: Throwable) {
                    getDefaultBitmap(context)
                }
            }
        }
    }

    fun resolveTrackBitmap(context: Context, track: Track): Bitmap {
        val source = when {
            !track.artworkUri.isNullOrBlank() -> ArtworkSource.UriSource(track.artworkUri)
            !track.contentUri.isNullOrBlank() -> ArtworkSource.UriSource(track.contentUri)
            track.coverResId != 0 -> ArtworkSource.ResourceSource(track.coverResId)
            else -> ArtworkSource.ResourceSource(R.drawable.art_luminous_echoes)
        }
        return resolveToBitmap(context, source)
    }

    fun getDefaultBitmap(context: Context): Bitmap {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.art_luminous_echoes)
                ?: createSolidFallbackBitmap()
        } catch (_: Throwable) {
            createSolidFallbackBitmap()
        }
    }

    private fun createSolidFallbackBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(0xFF1E1E24.toInt())
        return bmp
    }

    private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val path = uri.path ?: uriString
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(path, opts)
            } else {
                // Check if audio file has embedded ID3 picture
                val embedded = try {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        retriever.embeddedPicture
                    } finally {
                        retriever.release()
                    }
                } catch (_: Throwable) {
                    null
                }

                if (embedded != null) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeByteArray(embedded, 0, embedded.size, opts)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Dynamically samples the artwork of a track (from drawable resource or content URI)
     * and extracts the dominant color palette so the background automatically reflects
     * the color of the song's picture.
     */
    fun extractColors(context: Context, track: Track): TrackThemeColors {
        val bitmap = resolveTrackBitmap(context, track)
        return extractColorsFromBitmap(bitmap)
    }

    fun extractColorsFromUri(context: Context, artworkUriString: String): TrackThemeColors {
        val bitmap = resolveToBitmap(context, ArtworkSource.UriSource(artworkUriString))
        return extractColorsFromBitmap(bitmap)
    }

    fun getColorsForDrawable(context: Context, @DrawableRes resId: Int): TrackThemeColors {
        val bitmap = resolveToBitmap(context, ArtworkSource.ResourceSource(resId))
        return extractColorsFromBitmap(bitmap)
    }

    fun extractColorsFromBitmap(bitmap: Bitmap): TrackThemeColors {
        val adaptive = extractAdaptiveArtworkColors(bitmap)
        return generateThemePaletteFromArtworkColors(adaptive)
    }

    fun generateThemePaletteFromArtworkColors(colors: ArtworkColors): TrackThemeColors {
        val bgInt = colors.background
        val accentInt = colors.accent

        val hsvBg = FloatArray(3)
        android.graphics.Color.colorToHSV(bgInt, hsvBg)
        val bgHue = hsvBg[0]
        val bgSat = hsvBg[1]
        val bgVal = hsvBg[2]

        val hsvAccent = FloatArray(3)
        android.graphics.Color.colorToHSV(accentInt, hsvAccent)
        val accHue = hsvAccent[0]
        val accSat = hsvAccent[1]
        val accVal = hsvAccent[2]

        // Handles black & white, grayscale, or low-saturation album covers
        val isMonochrome = (bgSat < 0.14f && accSat < 0.14f)

        return if (isMonochrome) {
            val darkBg = Color(0xFF121214)
            val crispAccent = if (accVal > 0.40f) Color(0xFFE8E8EC) else Color(0xFFFFFFFF)
            TrackThemeColors(
                dominant = crispAccent,
                secondary = Color(0xFF26262C),
                accent = crispAccent,
                glow = Color.White.copy(alpha = 0.45f),
                darkBackground = darkBg,
                atmosphericBloom = Color.Transparent,
                playPauseCircle = Color(0xFF2C2C34),
                playPauseBorder = Color(0xFF555560).copy(alpha = 0.45f),
                bgTop = darkBg,
                bgMidUpper = darkBg,
                bgMidLower = darkBg,
                bgBottom = darkBg,
                playPauseGradTop = Color(0xFF34343E),
                playPauseGradBottom = Color(0xFF1E1E24)
            )
        } else {
            val targetHue = if (accSat >= 0.18f) accHue else bgHue
            val targetSat = maxOf(accSat, bgSat).coerceIn(0.40f, 0.95f)

            val dominant = Color.hsv(targetHue, targetSat, 0.75f)
            val secondary = Color.hsv(targetHue, (targetSat * 0.9f).coerceIn(0.5f, 1f), 0.16f)
            val accent = Color(accentInt)
            val glow = Color.hsv(targetHue, targetSat, 0.88f)

            val backgroundSurface = Color.hsv(
                targetHue,
                (targetSat * 0.60f).coerceIn(0.32f, 0.70f),
                0.15f
            )

            val playPauseGradTop = Color.hsv(targetHue, (targetSat * 0.76f).coerceIn(0.45f, 0.88f), 0.48f)
            val playPauseGradBottom = Color.hsv(targetHue, (targetSat * 0.85f).coerceIn(0.55f, 0.92f), 0.24f)
            val playPauseCircle = playPauseGradTop
            val playPauseBorder = Color.hsv(targetHue, targetSat, 0.68f).copy(alpha = 0.40f)

            TrackThemeColors(
                dominant = dominant,
                secondary = secondary,
                accent = accent,
                glow = glow,
                darkBackground = backgroundSurface,
                atmosphericBloom = Color.Transparent,
                playPauseCircle = playPauseCircle,
                playPauseBorder = playPauseBorder,
                bgTop = backgroundSurface,
                bgMidUpper = backgroundSurface,
                bgMidLower = backgroundSurface,
                bgBottom = backgroundSurface,
                playPauseGradTop = playPauseGradTop,
                playPauseGradBottom = playPauseGradBottom
            )
        }
    }

    fun generateThemePalette(baseColor: Color): TrackThemeColors {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        val isMonochrome = sat < 0.14f

        return if (isMonochrome) {
            val darkBg = Color(0xFF121214)
            val crispAccent = if (value > 0.40f) Color(0xFFE8E8EC) else Color(0xFFFFFFFF)
            TrackThemeColors(
                dominant = crispAccent,
                secondary = Color(0xFF26262C),
                accent = crispAccent,
                glow = Color.White.copy(alpha = 0.45f),
                darkBackground = darkBg,
                atmosphericBloom = Color.Transparent,
                playPauseCircle = Color(0xFF2C2C34),
                playPauseBorder = Color(0xFF555560).copy(alpha = 0.45f),
                bgTop = darkBg,
                bgMidUpper = darkBg,
                bgMidLower = darkBg,
                bgBottom = darkBg,
                playPauseGradTop = Color(0xFF34343E),
                playPauseGradBottom = Color(0xFF1E1E24)
            )
        } else {
            val boundedSat = sat.coerceIn(0.40f, 0.95f)
            val dominant = Color.hsv(hue, boundedSat, 0.75f)
            val secondary = Color.hsv(hue, (boundedSat * 0.9f).coerceIn(0.5f, 1f), 0.16f)
            val accent = Color.hsv(hue, (boundedSat * 0.85f).coerceIn(0.50f, 0.95f), 0.98f)
            val glow = Color.hsv(hue, boundedSat, 0.88f)

            val backgroundSurface = Color.hsv(
                hue,
                (boundedSat * 0.60f).coerceIn(0.32f, 0.70f),
                0.15f
            )

            val playPauseGradTop = Color.hsv(hue, (boundedSat * 0.76f).coerceIn(0.45f, 0.88f), 0.48f)
            val playPauseGradBottom = Color.hsv(hue, (boundedSat * 0.85f).coerceIn(0.55f, 0.92f), 0.24f)
            val playPauseCircle = playPauseGradTop
            val playPauseBorder = Color.hsv(hue, boundedSat, 0.68f).copy(alpha = 0.40f)

            TrackThemeColors(
                dominant = dominant,
                secondary = secondary,
                accent = accent,
                glow = glow,
                darkBackground = backgroundSurface,
                atmosphericBloom = Color.Transparent,
                playPauseCircle = playPauseCircle,
                playPauseBorder = playPauseBorder,
                bgTop = backgroundSurface,
                bgMidUpper = backgroundSurface,
                bgMidLower = backgroundSurface,
                bgBottom = backgroundSurface,
                playPauseGradTop = playPauseGradTop,
                playPauseGradBottom = playPauseGradBottom
            )
        }
    }
}

