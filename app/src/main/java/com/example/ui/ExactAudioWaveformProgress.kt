package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioTelemetry
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Player Sheet visualizer + progress component.
 *
 * This intentionally restores the visual treatment used by the earlier PlayerSheet:
 * - a compact live audio visualizer made from animated amplitude bars
 * - a clear gap between the visualizer and the progress bar
 * - one thin horizontal playback progress bar with a small thumb
 * - timestamps underneath
 * - the visualizer responds to live AudioTelemetry while playing and freezes when paused
 * - tapping/dragging the component seeks the track
 *
 * It does NOT decode the audio file into a static waveform. The bars are the live visualizer
 * the player uses while music is playing.
 */
@Composable
fun ExactAudioWaveformProgress(
    audioUri: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    telemetry: AudioTelemetry,
    activeColor: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayFraction = if (isDragging) dragFraction else progressFraction

    // Same visual profile and compact geometry as the PlayerSheet visualizer from
    // commit 64168ed5a67d52f116197def0f1873d065f51305.
    val barCount = 80
    val baseProfile = remember(durationMs, barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / barCount
            val wave1 = abs(sin(norm * 3.14159f * 1.8f + 0.35f))
            val wave2 = abs(sin(norm * 3.14159f * 4.3f)) * 0.42f
            val wave3 = abs(sin(norm * 3.14159f * 7.8f + 1.1f)) * 0.28f
            val wave4 = abs(cos(norm * 3.14159f * 12.2f)) * 0.16f
            (wave1 * 0.52f + wave2 + wave3 + wave4).coerceIn(0.18f, 0.95f)
        }
    }

    Column(
        modifier = modifier
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0L && size.width > 0f) {
                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekTo((newFraction * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekTo((dragFraction * durationMs).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        // 1. LIVE AUDIO VISUALIZER
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val totalWidth = size.width
            val barWidth = 1.3.dp.toPx()
            val totalBarWidth = barWidth * barCount
            val barGap = if (barCount > 1) (totalWidth - totalBarWidth) / (barCount - 1) else 0f
            val maxBarHeight = size.height
            val minBarHeight = 2.5.dp.toPx()

            val liveEnergy = if (isPlaying) telemetry.rmsLevel else 0.32f
            val liveTransient = if (isPlaying) telemetry.transientSpike else 0f

            for (i in 0 until barCount) {
                val barX = i * (barWidth + barGap)
                val base = baseProfile[i]

                val modulation = if (isPlaying) {
                    val ripple = sin(i * 0.35f + (positionMs / 220f)).toFloat()
                    0.70f + 0.30f * liveEnergy + 0.18f * liveTransient * ripple.coerceAtLeast(0f)
                } else {
                    0.72f
                }

                val barHeight = (base * maxBarHeight * modulation)
                    .coerceIn(minBarHeight, maxBarHeight)
                val barTop = size.height - barHeight

                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(barX, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        // The visualizer and progress bar are deliberately separated by a visible gap.
        Spacer(modifier = Modifier.height(8.dp))

        // 2. THIN PLAYBACK PROGRESS BAR
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val totalWidth = size.width
            val centerY = size.height / 2f
            val lineThickness = 2.dp.toPx()
            val progressWidth = (totalWidth * displayFraction).coerceIn(0f, totalWidth)

            // Unplayed track.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(0f, centerY - lineThickness / 2f),
                size = Size(totalWidth, lineThickness),
                cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
            )

            // Played track.
            if (progressWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.85f),
                            activeColor,
                            Color.White.copy(alpha = 0.90f)
                        ),
                        startX = 0f,
                        endX = progressWidth
                    ),
                    topLeft = Offset(0f, centerY - lineThickness / 2f),
                    size = Size(progressWidth, lineThickness),
                    cornerRadius = CornerRadius(lineThickness / 2f, lineThickness / 2f)
                )
            }

            // Small playback thumb.
            val beadRadius = (if (isDragging) 4.2.dp else 3.5.dp).toPx()
            val beadX = progressWidth.coerceIn(beadRadius, totalWidth - beadRadius)
            drawCircle(
                color = Color.White,
                radius = beadRadius,
                center = Offset(beadX, centerY)
            )
            drawCircle(
                color = activeColor,
                radius = beadRadius,
                center = Offset(beadX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. TIMESTAMPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(if (isDragging) (dragFraction * durationMs).toLong() else positionMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.48f)
            )
            Text(
                text = formatMs(durationMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.48f)
            )
        }
    }
}
