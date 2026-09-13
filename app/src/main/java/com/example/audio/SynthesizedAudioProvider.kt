package com.example.audio

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * Generates an authentic ambient WAV audio loop with distinct sub-bass,
 * warm mid harmonics, and crisp treble overtones.
 * This guarantees real audio playback and real FFT frequency data on any device or emulator.
 */
object SynthesizedAudioProvider {

    private var cachedUri: Uri? = null

    @Synchronized
    fun getOrCreateDemoAudioUri(context: Context): Uri {
        cachedUri?.let { return it }

        val targetFile = File(context.cacheDir, "velvet_ambient_soundscape.wav")
        if (targetFile.exists() && targetFile.length() > 44) {
            val uri = Uri.fromFile(targetFile)
            cachedUri = uri
            return uri
        }

        try {
            val sampleRate = 44100
            val durationSeconds = 12
            val totalSamples = sampleRate * durationSeconds
            val numChannels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * numChannels * bitsPerSample / 8
            val blockAlign = numChannels * bitsPerSample / 8
            val dataSize = totalSamples * blockAlign

            FileOutputStream(targetFile).use { fos ->
                // Write 44-byte WAV header
                val header = ByteBuffer.allocate(44).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    put("RIFF".toByteArray())
                    putInt(36 + dataSize)
                    put("WAVE".toByteArray())
                    put("fmt ".toByteArray())
                    putInt(16) // Subchunk1Size (16 for PCM)
                    putShort(1) // AudioFormat (1 for PCM)
                    putShort(numChannels.toShort())
                    putInt(sampleRate)
                    putInt(byteRate)
                    putShort(blockAlign.toShort())
                    putShort(bitsPerSample.toShort())
                    put("data".toByteArray())
                    putInt(dataSize)
                }
                fos.write(header.array())

                // Stream PCM audio samples in chunks
                val chunkSize = 4096
                val buffer = ByteBuffer.allocate(chunkSize * 2).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                }

                val twoPi = 2.0 * Math.PI
                var sampleIndex = 0

                // Chord progression in Hz: Low bass notes (55Hz, 65Hz, 73Hz) + mids (220Hz, 330Hz) + high sparkle (2200Hz)
                while (sampleIndex < totalSamples) {
                    buffer.clear()
                    val toWrite = minOf(chunkSize, totalSamples - sampleIndex)

                    for (j in 0 until toWrite) {
                        val t = (sampleIndex + j).toDouble() / sampleRate
                        val barTime = (t % 4.0)

                        // Deep rhythmic sub-bass pulse (55Hz / 65Hz)
                        val bassFreq = if (barTime < 2.0) 55.0 else 65.4
                        val bassEnvelope = (1.0 - (barTime % 1.0) * 0.6).coerceIn(0.2, 1.0)
                        val bass = sin(twoPi * bassFreq * t) * 0.45 * bassEnvelope

                        // Warm melodic mid pad (220Hz / 277Hz / 330Hz)
                        val mid1 = sin(twoPi * 220.0 * t) * 0.22
                        val mid2 = sin(twoPi * 277.18 * t) * 0.18
                        val mid3 = sin(twoPi * 329.63 * t) * 0.15
                        val pad = (mid1 + mid2 + mid3) * 0.6

                        // Crisp rhythmic high transient / hi-hat tick (3500Hz) on beats
                        val beatPos = barTime % 0.5
                        val highTick = if (beatPos < 0.04) {
                            val decay = 1.0 - (beatPos / 0.04)
                            (sin(twoPi * 3500.0 * t) + sin(twoPi * 5200.0 * t)) * 0.20 * decay
                        } else 0.0

                        val mixed = (bass + pad + highTick).coerceIn(-0.95, 0.95)
                        val sample16 = (mixed * 32767.0).toInt().toShort()
                        buffer.putShort(sample16)
                    }

                    fos.write(buffer.array(), 0, toWrite * 2)
                    sampleIndex += toWrite
                }
            }

            val uri = Uri.fromFile(targetFile)
            cachedUri = uri
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            return Uri.EMPTY
        }
    }
}
