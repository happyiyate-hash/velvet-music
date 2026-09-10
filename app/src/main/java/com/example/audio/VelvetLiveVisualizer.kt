package com.example.audio

import android.media.Visualizer
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Native Android playback visualizer attached to the app's MediaPlayer audio session.
 * It never records microphone input; it observes the playback audio session.
 */
class VelvetLiveVisualizer(
    private val onTelemetry: (rms: Float, transient: Float, frequencyHz: Float, kick: Boolean, snare: Boolean) -> Unit
) {
    private var visualizer: Visualizer? = null

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) return
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform.isNullOrEmpty()) return
                            var sum = 0.0
                            var peak = 0f
                            for (b in waveform) {
                                val sample = (b.toInt() - 128) / 128f
                                val magnitude = kotlin.math.abs(sample)
                                sum += sample * sample
                                if (magnitude > peak) peak = magnitude
                            }
                            val rms = sqrt(sum / waveform.size).toFloat().coerceIn(0f, 1f)
                            val transient = (peak * 0.72f + rms * 0.28f).coerceIn(0f, 1f)
                            onTelemetry(rms, transient, 0f, false, false)
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 4) return
                            var bestMagnitude = 0f
                            var bestBin = 1
                            var lowEnergy = 0f
                            var midEnergy = 0f
                            var totalEnergy = 0f
                            var bin = 1
                            while (bin < fft.size / 2) {
                                val real = fft[2 * bin].toInt()
                                val imag = fft[2 * bin + 1].toInt()
                                val magnitude = hypot(real.toFloat(), imag.toFloat())
                                totalEnergy += magnitude
                                if (magnitude > bestMagnitude) {
                                    bestMagnitude = magnitude
                                    bestBin = bin
                                }
                                val frequency = bestBin * (samplingRate / 1000f) / fft.size
                                when {
                                    frequency < 120f -> lowEnergy += magnitude
                                    frequency < 450f -> midEnergy += magnitude
                                }
                                bin++
                            }
                            val rms = (bestMagnitude / 128f).coerceIn(0f, 1f)
                            val frequencyHz = (bestBin * (samplingRate / 1000f) / fft.size).coerceIn(20f, 20_000f)
                            val kick = lowEnergy > midEnergy * 1.25f && rms > 0.25f
                            val snare = midEnergy > lowEnergy * 1.10f && rms > 0.30f
                            onTelemetry(rms, rms, frequencyHz, kick, snare)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
        } catch (_: Throwable) {
            release()
        }
    }

    fun release() {
        try { visualizer?.enabled = false } catch (_: Throwable) {}
        try { visualizer?.release() } catch (_: Throwable) {}
        visualizer = null
    }
}
