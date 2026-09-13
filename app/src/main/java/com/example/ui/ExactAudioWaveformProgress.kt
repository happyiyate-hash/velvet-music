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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

/**
 * Player Sheet visualizer + progress component.
 *
 * Provides a mature, music-reactive visualizer that plays dynamically to playback:
 * - Responds directly to the highest beat (sub-bass / kick hits) and highest sounds (sharp transients / snares)
 * - Strictly bounded within its canvas height without overflowing or clipping
 * - Studio-grade attack & decay physics (instant rise on beats, fluid decay)
 * - Sits gracefully on a tranquil resting wave when paused
 * - One thin horizontal playback progress bar with small thumb and timestamps underneath
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

    // 80 bars provides high spectral detail and perfect spacing across standard phone screens
    val barCount = 80

    // Calibrated baseline resting profile when paused or between beats
    val restingProfile = remember(barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            val wave1 = abs(sin(norm * 3.14159f * 1.5f + 0.35f)) * 0.16f
            val wave2 = abs(sin(norm * 3.14159f * 3.8f)) * 0.10f
            val wave3 = abs(cos(norm * 3.14159f * 7.2f)) * 0.06f
            (0.12f + wave1 + wave2 + wave3).coerceIn(0.12f, 0.32f)
        }
    }

    // Persisted amplitudes across frames for natural, studio-grade attack/decay physics
    val liveAmplitudes = remember(barCount) {
        FloatArray(barCount) { i -> restingProfile[i] }
    }

    // Continuous 60/120 FPS hardware animation ticker while playing
    val frameTicker = remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                withFrameNanos { timeNanos ->
                    frameTicker.longValue = timeNanos
                }
            }
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
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            // Read frameTicker to drive frame-by-frame draw invalidate without recomposing
            val frameTime = frameTicker.longValue
            val timeSeconds = (frameTime / 1_000_000L) * 0.003f

            val totalWidth = size.width
            val barWidth = 1.3.dp.toPx()
            val totalBarWidth = barWidth * barCount
            val barGap = if (barCount > 1) (totalWidth - totalBarWidth) / (barCount - 1) else 0f

            // Strictly constrain height bounds so bars never go against canvas height
            val maxBarHeight = size.height
            val minBarHeight = 2.5.dp.toPx().coerceAtMost(maxBarHeight)
            val usableRange = (maxBarHeight - minBarHeight).coerceAtLeast(0f)

            val rms = telemetry.rmsLevel.coerceIn(0f, 1f)
            val transient = telemetry.transientSpike.coerceIn(0f, 1f)
            val isKick = telemetry.kickDetected
            val isSnare = telemetry.snareDetected
            val domFreq = telemetry.dominantFrequencyHz.coerceIn(40f, 16000f)

            // Normalized center of pitch/dominant frequency (0.0 to 1.0)
            val domNorm = (log10(domFreq / 40f) / log10(400f)).coerceIn(0.08f, 0.92f)

            // Highest beat impact (Bass & Kick)
            val highestBeat = if (isKick) 0.92f else 0f

            // Highest sound impact (Transients, highs & Snares)
            val highestSound = transient

            val fft = telemetry.fftBars
            val hasLiveFft = fft.isNotEmpty()

            for (i in 0 until barCount) {
                val norm = i.toFloat() / (barCount - 1).coerceAtLeast(1)

                val targetFraction = if (isPlaying) {
                    if (hasLiveFft) {
                        // FFT frequency mapping:
                        // Low frequencies (bass) on the left (norm ~ 0)
                        // High frequencies (treble) on the right (norm ~ 1)
                        val fftBin = (norm * (fft.size - 1)).toInt().coerceIn(0, fft.size - 1)
                        val fftMag = fft[fftBin]
                        val spectralCurve = 1.0f + (1f - norm) * 0.35f
                        (fftMag * spectralCurve).coerceIn(0.06f, 1.0f)
                    } else {
                        // 1. Bass / Highest Beat Zone (bars 0..32%)
                        // Responds decisively to kick drums and sub bass
                        val bassWeight = (1f - (norm / 0.32f)).coerceIn(0f, 1f)
                        val beatResponse = (highestBeat * bassWeight * 0.95f) + (rms * bassWeight * 0.70f)

                        // 2. Highs / Highest Sound Zone (bars 50..100%)
                        // Responds decisively to crisp transient peaks, snares, and cymbals
                        val trebleWeight = ((norm - 0.48f) / 0.52f).coerceIn(0f, 1f)
                        val snarePulse = if (isSnare && norm in 0.52f..0.85f) 0.75f else 0f
                        val soundResponse = (highestSound * trebleWeight * 0.90f) + snarePulse

                        // 3. Mids / Body Zone (bars 22..75%)
                        // Melodic harmonic resonance and vocal sustained warmth
                        val midWeight = sin(norm * 3.14159f).coerceAtLeast(0f)
                        val midResponse = (telemetry.sustainedEnergy * 0.45f + rms * 0.40f) * midWeight

                        // 4. Acoustic pitch resonance around current dominant frequency
                        val distToDom = abs(norm - domNorm)
                        val resonanceBoost = (1f - (distToDom / 0.22f)).coerceAtLeast(0f) * rms * 0.38f

                        // 5. Subtle micro-harmonic rhythm variation
                        val harmonic = sin(i * 0.42f + timeSeconds).toFloat() * 0.06f

                        (restingProfile[i] * 0.30f + beatResponse + soundResponse + midResponse + resonanceBoost + harmonic).coerceIn(0.06f, 1.0f)
                    }
                } else {
                    // Freezing entirely when paused: hold previous frame
                    liveAmplitudes[i]
                }

                // Studio attack/decay physics:
                val current = liveAmplitudes[i]
                val updated = if (isPlaying) {
                    if (targetFraction > current) {
                        // Instantaneous attack: leap up with the beat
                        current * 0.20f + targetFraction * 0.80f
                    } else {
                        // Smooth, fluid studio decay
                        maxOf(targetFraction, current * 0.88f)
                    }
                } else {
                    // FREEZING ENTIRELY WHEN PAUSED:
                    // Maintain current amplitude without decaying or moving
                    current
                }
                liveAmplitudes[i] = updated.coerceIn(0f, 1f)

                // STRICT HEIGHT ENFORCEMENT:
                // barHeight is strictly clamped between minBarHeight and maxBarHeight.
                // barTop is strictly >= 0 and never exceeds size.height.
                val clampedFraction = liveAmplitudes[i].coerceIn(0f, 1f)
                val barHeight = (minBarHeight + usableRange * clampedFraction)
                    .coerceIn(minBarHeight, maxBarHeight)
                val barTop = size.height - barHeight
                val barX = i * (barWidth + barGap)

                // Mature color response: tips illuminate brightly on highest beats/sounds
                val isPeak = clampedFraction > 0.78f
                val tipColor = if (isPeak) {
                    Color.White.copy(alpha = 0.92f)
                } else {
                    activeColor.copy(alpha = (0.75f + clampedFraction * 0.25f).coerceIn(0f, 1f))
                }
                val baseColor = activeColor.copy(alpha = 0.70f)

                val barBrush = Brush.verticalGradient(
                    colors = listOf(tipColor, baseColor),
                    startY = barTop,
                    endY = size.height
                )

                drawRoundRect(
                    brush = barBrush,
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
