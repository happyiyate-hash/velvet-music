package com.example.mesh

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.audio.AudioTelemetry
import com.example.ui.theme.VelvetAshGray
import com.example.ui.theme.VelvetAshGrayDark
import com.example.ui.theme.VelvetAshGrayMedium
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetDarkSmoky
import com.example.ui.theme.VelvetOffBloodTop
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance state holder for the Sound Catch Audio-Reactive Mesh.
 * Keeping state outside Compose composition prevents heavy 60-120fps recompositions
 * while scrolling, ensuring butter-smooth 60-120fps scrolling.
 */
class MeshNodesState {
    var node1X: Float = 0.40f
    var node1Y: Float = 0.25f
    var node1SnapX: Float = 0f
    var node1SnapY: Float = 0f

    var node2X: Float = 0.65f
    var node2Y: Float = 0.55f
    var node2SnapX: Float = 0f
    var node2SnapY: Float = 0f

    var node3X: Float = 0.35f
    var node3Y: Float = 0.80f

    var timeElapsed: Float = 0f

    // Read exclusively in the Canvas draw phase to trigger draw-only updates without Composable recomposition
    val drawInvalidator = mutableLongStateOf(0L)
}

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
    val mesh = remember { MeshNodesState() }

    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "meshDominant"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(durationMillis = 800),
        label = "meshSecondary"
    )

    // Continuous render loop for fluid motion
    LaunchedEffect(isPlaying, isSoundCatchEnabled) {
        if (!isSoundCatchEnabled || !isPlaying) {
            mesh.node1SnapX = 0f
            mesh.node1SnapY = 0f
            mesh.node2SnapX = 0f
            mesh.node2SnapY = 0f
            mesh.drawInvalidator.longValue++
            return@LaunchedEffect
        }
        var lastNano = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNano == 0L) lastNano = now
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastNano = now

                mesh.timeElapsed += dt * (0.8f + audioTelemetry.sustainedEnergy * 0.8f)

                // FAST SNAP PIPELINE:
                // Instant impulse offset triggers on high-frequency transient spike (<4ms response)
                if (audioTelemetry.kickDetected) {
                    mesh.node1SnapX = (if (Math.random() > 0.5) 0.04f else -0.04f) * audioTelemetry.transientSpike
                    mesh.node1SnapY = (if (Math.random() > 0.5) 0.05f else -0.05f) * audioTelemetry.transientSpike
                } else {
                    mesh.node1SnapX *= 0.82f
                    mesh.node1SnapY *= 0.82f
                }

                if (audioTelemetry.snareDetected) {
                    mesh.node2SnapX = (if (Math.random() > 0.5) 0.06f else -0.06f) * audioTelemetry.transientSpike
                    mesh.node2SnapY = (if (Math.random() > 0.5) 0.04f else -0.04f) * audioTelemetry.transientSpike
                } else {
                    mesh.node2SnapX *= 0.82f
                    mesh.node2SnapY *= 0.82f
                }

                // SMOOTH DRIFT PIPELINE:
                val targetDrift1X = 0.40f + 0.12f * sin(mesh.timeElapsed * 0.7f)
                val targetDrift1Y = 0.25f + 0.08f * cos(mesh.timeElapsed * 0.5f)
                mesh.node1X += (targetDrift1X - mesh.node1X) * 0.06f
                mesh.node1Y += (targetDrift1Y - mesh.node1Y) * 0.06f

                val targetDrift2X = 0.65f + 0.14f * cos(mesh.timeElapsed * 0.6f)
                val targetDrift2Y = 0.55f + 0.10f * sin(mesh.timeElapsed * 0.8f)
                mesh.node2X += (targetDrift2X - mesh.node2X) * 0.05f
                mesh.node2Y += (targetDrift2Y - mesh.node2Y) * 0.05f

                val targetDrift3X = 0.35f + 0.10f * sin(mesh.timeElapsed * 0.4f)
                val targetDrift3Y = 0.80f + 0.06f * cos(mesh.timeElapsed * 0.3f)
                mesh.node3X += (targetDrift3X - mesh.node3X) * 0.04f
                mesh.node3Y += (targetDrift3Y - mesh.node3Y) * 0.04f

                mesh.drawInvalidator.longValue++
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read drawInvalidator strictly inside Canvas draw scope to only re-execute drawing, NOT composition
            @Suppress("UNUSED_VARIABLE")
            val tick = mesh.drawInvalidator.longValue

            val w = size.width
            val h = size.height

            // 1. Base Canvas Gradient: Dynamically capturing the song's picture color
            // (e.g. if the song picture is blue, the background smoothly reflects that rich sapphire blue!)
            val topTone = animatedDominant.copy(alpha = 0.85f)
            val midTone = animatedSecondary.copy(alpha = 0.90f)
            val deepTone = Color(
                red = (animatedDominant.red * 0.12f + 0.05f).coerceIn(0f, 1f),
                green = (animatedDominant.green * 0.12f + 0.05f).coerceIn(0f, 1f),
                blue = (animatedDominant.blue * 0.12f + 0.07f).coerceIn(0f, 1f)
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to topTone,
                        0.32f to midTone,
                        0.62f to deepTone,
                        1.0f to VelvetAshGrayDark
                    )
                )
            )

            // 2. Node 1: Dynamic Primary Chromatic Swell (Fast Snap Kick Responsive)
            val pos1 = Offset(
                x = (mesh.node1X + mesh.node1SnapX).coerceIn(0.1f, 0.9f) * w,
                y = (mesh.node1Y + mesh.node1SnapY).coerceIn(0.1f, 0.9f) * h
            )
            val radius1 = (w * 0.75f) * (1f + audioTelemetry.transientSpike * 0.18f)
            val alpha1 = (0.55f + audioTelemetry.transientSpike * 0.15f).coerceIn(0.35f, 0.85f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedDominant.copy(alpha = alpha1),
                        animatedDominant.copy(alpha = alpha1 * 0.45f),
                        topTone.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = pos1,
                    radius = radius1
                ),
                center = pos1,
                radius = radius1
            )

            // 3. Node 2: Dynamic Secondary Chromatic Swell (Fast Snap Snare Responsive)
            val pos2 = Offset(
                x = (mesh.node2X + mesh.node2SnapX).coerceIn(0.1f, 0.9f) * w,
                y = (mesh.node2Y + mesh.node2SnapY).coerceIn(0.1f, 0.9f) * h
            )
            val radius2 = (w * 0.85f) * (1f + audioTelemetry.sustainedEnergy * 0.15f)
            val alpha2 = (0.50f + audioTelemetry.sustainedEnergy * 0.12f).coerceIn(0.3f, 0.75f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedSecondary.copy(alpha = alpha2),
                        animatedSecondary.copy(alpha = alpha2 * 0.40f),
                        midTone.copy(alpha = 0.15f),
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
                x = mesh.node3X.coerceIn(0.1f, 0.9f) * w,
                y = mesh.node3Y.coerceIn(0.1f, 0.9f) * h
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

            // 5. Ambient gradient overlay dynamically reflecting the music color
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to topTone.copy(alpha = 0.25f),
                        0.40f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to VelvetAshGrayDark.copy(alpha = 0.85f)
                    )
                )
            )
        }
    }
}
