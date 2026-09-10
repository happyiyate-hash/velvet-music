package com.example.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audio.AudioTelemetry
import com.example.audio.AudioWaveformRepository
import kotlin.math.abs
import kotlin.math.sin

/**
 * Real waveform: the outline comes from decoded PCM amplitude peaks from the actual audio file.
 * While playing, the same bars receive a restrained live multiplier from AudioTelemetry.
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
    val context = LocalContext.current
    var amplitudes by remember(audioUri) { mutableStateOf<List<Float>>(emptyList()) }
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LaunchedEffect(audioUri) {
        amplitudes = if (!audioUri.isNullOrBlank()) {
            AudioWaveformRepository.getWaveform(context, Uri.parse(audioUri), 96)
        } else emptyList()
    }

    val bars = if (amplitudes.isNotEmpty()) amplitudes else List(96) { 0.10f }

    Canvas(
        modifier = modifier
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0L && size.width > 0f) {
                        onSeekTo((offset.x / size.width).coerceIn(0f, 1f).times(durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        if (durationMs > 0L && size.width > 0f) {
                            onSeekTo((change.position.x / size.width).coerceIn(0f, 1f).times(durationMs).toLong())
                        }
                    }
                )
            }
    ) {
        val count = bars.size
        val barWidth = 1.6.dp.toPx()
        val gap = if (count > 1) (size.width - barWidth * count) / (count - 1) else 0f
        val baseMax = size.height * 0.90f
        val energy = telemetry.rmsLevel.coerceIn(0f, 1f)
        val transient = telemetry.transientSpike.coerceIn(0f, 1f)
        val frequencyFactor = (telemetry.dominantFrequencyHz / 440f).coerceIn(0.2f, 3f)

        bars.forEachIndexed { index, raw ->
            val x = index * (barWidth + gap)
            val played = index.toFloat() / count <= progress
            val wavePhase = index * 0.42f + frequencyFactor * 2.2f
            val livePulse = if (isPlaying) {
                1f + (energy * 0.20f) + (transient * 0.30f * abs(sin(wavePhase)))
            } else 1f
            val height = (baseMax * raw.coerceIn(0.08f, 1f) * livePulse).coerceIn(2.5.dp.toPx(), size.height)
            val y = (size.height - height) / 2f
            val alpha = if (played) 0.96f else 0.28f

            drawRoundRect(
                color = activeColor.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }

        val progressX = size.width * progress
        drawLine(
            color = activeColor.copy(alpha = 0.85f),
            start = Offset(progressX, size.height * 0.08f),
            end = Offset(progressX, size.height * 0.92f),
            strokeWidth = 1.dp.toPx()
        )
    }
}
