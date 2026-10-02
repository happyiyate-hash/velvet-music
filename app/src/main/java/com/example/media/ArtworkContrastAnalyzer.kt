package com.example.media

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

data class ZoneContrast(
    val isLightBackground: Boolean,
    val primaryColor: Color,     // Crisp Black (#0F1115) on light, White (#FFFFFF) on dark
    val secondaryColor: Color,   // Subtitle / inactive text
    val tertiaryColor: Color,    // Track / subtle elements
    val luminance: Float
) {
    companion object {
        val DefaultDark = ZoneContrast(
            isLightBackground = false,
            primaryColor = Color.White,
            secondaryColor = Color.White.copy(alpha = 0.68f),
            tertiaryColor = Color.White.copy(alpha = 0.18f),
            luminance = 0.05f
        )
        val DefaultLight = ZoneContrast(
            isLightBackground = true,
            primaryColor = Color(0xFF0F1115),
            secondaryColor = Color(0xFF0F1115).copy(alpha = 0.72f),
            tertiaryColor = Color(0xFF0F1115).copy(alpha = 0.22f),
            luminance = 0.90f
        )
    }
}

data class PlayerAdaptiveContrast(
    val topBar: ZoneContrast = ZoneContrast.DefaultDark,
    val title: ZoneContrast = ZoneContrast.DefaultDark,
    val progress: ZoneContrast = ZoneContrast.DefaultDark,
    val visualizer: ZoneContrast = ZoneContrast.DefaultDark
)

/**
 * Region-Based Automatic Contrast Adaptation Engine:
 * Analyzes local pixel luminance and contrast ratios directly underneath each UI element
 * (Top Bar, Title/Artist, Progress Bar, Visualizer) independently.
 */
object ArtworkContrastAnalyzer {

    /**
     * WCAG relative luminance calculation for sRGB
     */
    fun calculateLuminance(r: Int, g: Int, b: Int): Float {
        fun channel(c: Int): Float {
            val s = c / 255f
            return if (s <= 0.04045f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
        }
        return (0.2126f * channel(r) + 0.7152f * channel(g) + 0.0722f * channel(b)).coerceIn(0f, 1f)
    }

    private fun contrastAgainstWhite(luminance: Float): Float {
        return (1.0f + 0.05f) / (luminance + 0.05f)
    }

    private fun contrastAgainstBlack(luminance: Float): Float {
        return (luminance + 0.05f) / 0.05f
    }

    /**
     * Analyzes a specific normalized region [topFraction..bottomFraction] of a bitmap.
     * Takes < 0.05ms on a 128x128 thumbnail.
     */
    fun analyzeRegion(
        bitmap: Bitmap?,
        topFraction: Float,
        bottomFraction: Float,
        leftFraction: Float = 0.05f,
        rightFraction: Float = 0.95f,
        previousState: Boolean = false // hysteresis
    ): ZoneContrast {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return ZoneContrast.DefaultDark
        }

        val startX = (bitmap.width * leftFraction).toInt().coerceIn(0, bitmap.width - 1)
        val endX = (bitmap.width * rightFraction).toInt().coerceIn(startX + 1, bitmap.width)
        val startY = (bitmap.height * topFraction).toInt().coerceIn(0, bitmap.height - 1)
        val endY = (bitmap.height * bottomFraction).toInt().coerceIn(startY + 1, bitmap.height)

        val stepX = ((endX - startX) / 24).coerceAtLeast(1)
        val stepY = ((endY - startY) / 16).coerceAtLeast(1)

        var totalLum = 0.0
        var count = 0

        for (y in startY until endY step stepY) {
            for (x in startX until endX step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalLum += calculateLuminance(r, g, b)
                count++
            }
        }

        if (count == 0) return ZoneContrast.DefaultDark

        val avgLuminance = (totalLum / count).toFloat()
        val crWhite = contrastAgainstWhite(avgLuminance)
        val crBlack = contrastAgainstBlack(avgLuminance)

        // Hysteresis threshold to prevent rapid fluttering
        val isLight = if (previousState) {
            avgLuminance > 0.38f || crBlack >= crWhite
        } else {
            avgLuminance > 0.48f && crBlack >= crWhite
        }

        return if (isLight) {
            ZoneContrast(
                isLightBackground = true,
                primaryColor = Color(0xFF0F1115),
                secondaryColor = Color(0xFF0F1115).copy(alpha = 0.72f),
                tertiaryColor = Color(0xFF0F1115).copy(alpha = 0.22f),
                luminance = avgLuminance
            )
        } else {
            ZoneContrast(
                isLightBackground = false,
                primaryColor = Color.White,
                secondaryColor = Color.White.copy(alpha = 0.68f),
                tertiaryColor = Color.White.copy(alpha = 0.18f),
                luminance = avgLuminance
            )
        }
    }

    /**
     * Analyzes all 4 zones of the artwork independently.
     */
    fun analyzeArtwork(
        bitmap: Bitmap?,
        previous: PlayerAdaptiveContrast = PlayerAdaptiveContrast()
    ): PlayerAdaptiveContrast {
        if (bitmap == null || bitmap.isRecycled) {
            return PlayerAdaptiveContrast()
        }

        // 1. Top Zone: Status Bar and Top Actions (0% to 15% height)
        val topBar = analyzeRegion(
            bitmap = bitmap,
            topFraction = 0.02f,
            bottomFraction = 0.15f,
            previousState = previous.topBar.isLightBackground
        )

        // 2. Title Zone: Title and artist region (60% to 77% height)
        val title = analyzeRegion(
            bitmap = bitmap,
            topFraction = 0.60f,
            bottomFraction = 0.77f,
            previousState = previous.title.isLightBackground
        )

        // 3. Progress Zone: Progress bar and timestamps (77% to 88% height)
        val progress = analyzeRegion(
            bitmap = bitmap,
            topFraction = 0.77f,
            bottomFraction = 0.88f,
            previousState = previous.progress.isLightBackground
        )

        // 4. Visualizer Zone: Bottom vertical spectrum needles (88% to 100% height)
        val visualizer = analyzeRegion(
            bitmap = bitmap,
            topFraction = 0.88f,
            bottomFraction = 1.00f,
            previousState = previous.visualizer.isLightBackground
        )

        return PlayerAdaptiveContrast(
            topBar = topBar,
            title = title,
            progress = progress,
            visualizer = visualizer
        )
    }
}
