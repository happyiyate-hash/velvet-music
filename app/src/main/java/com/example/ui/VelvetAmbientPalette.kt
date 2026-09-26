package com.example.ui

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Quiet, continuously drifting ambient palette for the upper Velvet player card.
 *
 * Palette changes crossfade over 500 ms while the spatial phase keeps running.
 * There is intentionally no RepeatMode.Reverse / ping-pong animation.
 */
@Composable
fun VelvetAmbientPalette(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val targetPalette = remember(colors) {
        normalizePalette(colors)
    }

    var displayedPalette by remember { mutableStateOf(targetPalette) }
    var previousPalette by remember { mutableStateOf(targetPalette) }
    var paletteTransitionStartNanos by remember { mutableLongStateOf(Long.MIN_VALUE) }
    var elapsedNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetPalette, enabled) {
        if (!enabled) {
            previousPalette = targetPalette
            displayedPalette = targetPalette
            paletteTransitionStartNanos = Long.MIN_VALUE
            elapsedNanos = 0L
            return@LaunchedEffect
        }

        // Capture the currently displayed palette so a rapid track change never
        // jumps back to the previous track's original colors.
        previousPalette = displayedPalette
        paletteTransitionStartNanos = Long.MIN_VALUE
    }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect

        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos ->
                elapsedNanos = frameNanos

                if (paletteTransitionStartNanos == Long.MIN_VALUE) {
                    paletteTransitionStartNanos = frameNanos
                }

                val progress = (
                    (frameNanos - paletteTransitionStartNanos).toFloat() / PALETTE_CROSSFADE_NANOS
                ).coerceIn(0f, 1f)

                // Smoothstep gives the palette a gentle arrival/departure instead
                // of a mechanical linear color swap.
                val eased = progress * progress * (3f - 2f * progress)
                displayedPalette = blendPalette(previousPalette, targetPalette, eased)
            }
        }
    }

    Canvas(modifier = modifier.clipToBounds()) {
        if (size.minDimension <= 0f) return@Canvas

        val timeSeconds = elapsedNanos / 1_000_000_000f
        drawAmbientFields(displayedPalette, timeSeconds)
    }
}

private const val PALETTE_CROSSFADE_NANOS = 500_000_000f

private fun normalizePalette(colors: List<Color>): List<Color> = buildList {
    colors.filter { it != Color.Transparent }.take(4).forEach(::add)
    while (size < 4) add(Color.Transparent)
}

private fun blendPalette(
    from: List<Color>,
    to: List<Color>,
    progress: Float,
): List<Color> = List(4) { index ->
    val start = from.getOrElse(index) { Color.Transparent }
    val end = to.getOrElse(index) { Color.Transparent }
    lerpColor(start, end, progress)
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    if (from == Color.Transparent) return to.copy(alpha = to.alpha * fraction)
    if (to == Color.Transparent) return from.copy(alpha = from.alpha * (1f - fraction))
    return androidx.compose.ui.graphics.lerp(from, to, fraction)
}

private fun DrawScope.drawAmbientFields(
    colors: List<Color>,
    timeSeconds: Float,
) {
    val periods = floatArrayOf(22f, 28f, 34f, 40f)
    val phases = floatArrayOf(0f, 1.7f, 3.4f, 5.1f)
    val orbitRadii = floatArrayOf(0.23f, 0.27f, 0.21f, 0.25f)
    val radii = floatArrayOf(1.05f, 1.12f, 0.98f, 1.08f)

    val width = size.width
    val height = size.height
    val centerX = width * 0.5f
    val centerY = height * 0.5f
    val baseRadius = size.maxDimension

    colors.forEachIndexed { index, color ->
        if (color == Color.Transparent) return@forEachIndexed

        val phase = positiveModulo(
            timeSeconds / periods[index] + phases[index] / (2f * PI.toFloat()),
            1f,
        )
        val angle = phase * 2f * PI.toFloat()

        val center = Offset(
            x = centerX + cos(angle) * width * orbitRadii[index],
            y = centerY + sin(angle) * height * (orbitRadii[index] * 0.62f),
        )

        val alpha = when (index) {
            0 -> 0.30f
            1 -> 0.24f
            2 -> 0.20f
            else -> 0.17f
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.42f),
                    Color.Transparent,
                ),
                center = center,
                radius = baseRadius * radii[index],
            ),
            size = size,
        )
    }
}

private fun positiveModulo(value: Float, modulus: Float): Float {
    val result = value % modulus
    return if (result < 0f) result + modulus else result
}
