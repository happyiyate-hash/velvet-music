package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A quiet, continuously drifting ambient palette for the upper Velvet player card.
 *
 * The supplied colors never change during a frame. Only their spatial influence moves.
 * Motion is phase-based and wraps continuously; there is intentionally no
 * RepeatMode.Reverse / ping-pong animation.
 *
 * Keep this composable behind the artwork/waveform/title content and clip the parent
 * card so the ambient fields cannot bleed into the lower control surface.
 */
@Composable
fun VelvetAmbientPalette(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = remember(colors) {
        buildList {
            colors.filter { it != Color.Transparent }.take(4).forEach(::add)
            while (size < 4) add(Color.Transparent)
        }
    }

    var elapsedNanos by remember { mutableLongStateOf(0L) }

    if (enabled) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            withInfiniteAnimationFrameNanos { frameNanos ->
                elapsedNanos = frameNanos
            }
        }
    }

    Canvas(
        modifier = modifier.clipToBounds()
    ) {
        val timeSeconds = elapsedNanos / 1_000_000_000f
        drawAmbientFields(palette, timeSeconds)
    }
}

private fun DrawScope.drawAmbientFields(
    colors: List<Color>,
    timeSeconds: Float,
) {
    if (size.minDimension <= 0f) return

    // Long, intentionally different periods prevent obvious synchronization.
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

        // Continuous normalized phase in [0, 1). The path never reverses.
        val phase = positiveModulo(timeSeconds / periods[index] + phases[index] / (2f * PI.toFloat()), 1f)
        val angle = phase * 2f * PI.toFloat()

        // A slow elliptical orbit gives organic movement without a visible ping-pong.
        val center = androidx.compose.ui.geometry.Offset(
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
