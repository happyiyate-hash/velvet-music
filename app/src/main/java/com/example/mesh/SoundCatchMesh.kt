package com.example.mesh

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.audio.AudioTelemetry
import com.example.ui.theme.VelvetAshGray
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetAshGrayLight
import com.example.ui.theme.VelvetAshGrayMedium
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetDarkSmoky
import com.example.ui.theme.VelvetOffBloodTop
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sound Catch Audio-Reactive Mesh Engine
 *
 * Implements the Velvet Blueprint & Visual Design:
 * - Top: "off blood" crimson wine gradient to the very top
 * - Mid to Bottom: "almost ashes... mix with black and white together, blends almost like a gray"
 * - Fast Snap: Sub-4ms instant coordinate impulse on transient spikes (kicks/snares)
 * - Smooth Drift: Continuous linear interpolation (lerp) for sustained tones
 */
@Composable
fun SoundCatchMeshBackground(
    dominantColor: Color,
    secondaryColor: Color,
    audioTelemetry: AudioTelemetry,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isSoundCatchEnabled: Boolean = true
) {
    // Coordinate nodes for Fast Snap + Smooth Drift
    var node1X by remember { mutableFloatStateOf(0.40f) }
    var node1Y by remember { mutableFloatStateOf(0.25f) }
    var node1SnapX by remember { mutableFloatStateOf(0f) }
    var node1SnapY by remember { mutableFloatStateOf(0f) }

    var node2X by remember { mutableFloatStateOf(0.65f) }
    var node2Y by remember { mutableFloatStateOf(0.55f) }
    var node2SnapX by remember { mutableFloatStateOf(0f) }
    var node2SnapY by remember { mutableFloatStateOf(0f) }

    var node3X by remember { mutableFloatStateOf(0.35f) }
    var node3Y by remember { mutableFloatStateOf(0.80f) }

    var timeElapsed by remember { mutableFloatStateOf(0f) }

    // Continuous 60-120 FPS render loop for fluid motion
    LaunchedEffect(isPlaying, isSoundCatchEnabled) {
        if (!isSoundCatchEnabled || !isPlaying) {
            node1SnapX = 0f
            node1SnapY = 0f
            node2SnapX = 0f
            node2SnapY = 0f
            return@LaunchedEffect
        }
        var lastNano = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNano == 0L) lastNano = now
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastNano = now

                timeElapsed += dt * (0.8f + audioTelemetry.sustainedEnergy * 0.8f)

                // FAST SNAP PIPELINE:
                // Instant impulse offset triggers on high-frequency transient spike (<4ms response)
                if (audioTelemetry.kickDetected) {
                    node1SnapX = (if (Math.random() > 0.5) 0.04f else -0.04f) * audioTelemetry.transientSpike
                    node1SnapY = (if (Math.random() > 0.5) 0.05f else -0.05f) * audioTelemetry.transientSpike
                } else {
                    node1SnapX *= 0.82f
                    node1SnapY *= 0.82f
                }

                if (audioTelemetry.snareDetected) {
                    node2SnapX = (if (Math.random() > 0.5) 0.06f else -0.06f) * audioTelemetry.transientSpike
                    node2SnapY = (if (Math.random() > 0.5) 0.04f else -0.04f) * audioTelemetry.transientSpike
                } else {
                    node2SnapX *= 0.82f
                    node2SnapY *= 0.82f
                }

                // SMOOTH DRIFT PIPELINE:
                // Continuous linear interpolation (lerp) creating dragging, fluid motion for sustained tones
                val targetDrift1X = 0.40f + 0.12f * sin(timeElapsed * 0.7f)
                val targetDrift1Y = 0.25f + 0.08f * cos(timeElapsed * 0.5f)
                node1X += (targetDrift1X - node1X) * 0.06f
                node1Y += (targetDrift1Y - node1Y) * 0.06f

                val targetDrift2X = 0.65f + 0.14f * cos(timeElapsed * 0.6f)
                val targetDrift2Y = 0.55f + 0.10f * sin(timeElapsed * 0.8f)
                node2X += (targetDrift2X - node2X) * 0.05f
                node2Y += (targetDrift2Y - node2Y) * 0.05f

                val targetDrift3X = 0.35f + 0.10f * sin(timeElapsed * 0.4f)
                val targetDrift3Y = 0.80f + 0.06f * cos(timeElapsed * 0.3f)
                node3X += (targetDrift3X - node3X) * 0.04f
                node3Y += (targetDrift3Y - node3Y) * 0.04f
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Base Canvas Gradient: Off-Blood to Deep Dark Ash (almost black at bottom)
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to VelvetOffBloodTop,
                        0.28f to VelvetBloodPlum,
                        0.58f to VelvetDarkSmoky,
                        0.82f to VelvetAshGrayMedium,
                        1.0f to VelvetAshGrayDark
                    )
                )
            )

            // 2. Node 1: Upper Crimson Swell (Fast Snap Kick Responsive)
            val pos1 = Offset(
                x = (node1X + node1SnapX).coerceIn(0.1f, 0.9f) * w,
                y = (node1Y + node1SnapY).coerceIn(0.1f, 0.9f) * h
            )
            val radius1 = (w * 0.75f) * (1f + audioTelemetry.transientSpike * 0.18f)
            val alpha1 = (0.55f + audioTelemetry.transientSpike * 0.15f).coerceIn(0.35f, 0.85f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dominantColor.copy(alpha = alpha1),
                        dominantColor.copy(alpha = alpha1 * 0.45f),
                        VelvetOffBloodTop.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = pos1,
                    radius = radius1
                ),
                center = pos1,
                radius = radius1
            )

            // 3. Node 2: Mid Burgundy / Secondary Wave (Fast Snap Snare Responsive)
            val pos2 = Offset(
                x = (node2X + node2SnapX).coerceIn(0.1f, 0.9f) * w,
                y = (node2Y + node2SnapY).coerceIn(0.1f, 0.9f) * h
            )
            val radius2 = (w * 0.85f) * (1f + audioTelemetry.sustainedEnergy * 0.15f)
            val alpha2 = (0.50f + audioTelemetry.sustainedEnergy * 0.12f).coerceIn(0.3f, 0.75f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = alpha2),
                        secondaryColor.copy(alpha = alpha2 * 0.40f),
                        VelvetDarkSmoky.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = pos2,
                    radius = radius2
                ),
                center = pos2,
                radius = radius2,
                blendMode = BlendMode.Plus
            )

            // 4. Node 3: Deep Ash Swell at bottom (dragging fluid motion)
            val pos3 = Offset(
                x = node3X.coerceIn(0.1f, 0.9f) * w,
                y = node3Y.coerceIn(0.1f, 0.9f) * h
            )
            val radius3 = w * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VelvetAshGray.copy(alpha = 0.22f),
                        VelvetAshGrayDark.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = pos3,
                    radius = radius3
                ),
                center = pos3,
                radius = radius3
            )

            // 5. Subtle ambient gradient overlay to seamlessly bind the off-blood and dark ash
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to VelvetOffBloodTop.copy(alpha = 0.25f),
                        0.40f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to VelvetAshGrayDark.copy(alpha = 0.85f)
                    )
                )
            )
        }
    }
}
