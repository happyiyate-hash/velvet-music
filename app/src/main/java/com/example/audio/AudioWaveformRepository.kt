package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Decodes a local audio source to PCM off the main thread and reduces it to a small,
 * deterministic set of amplitude peaks. Results are cached by URI so the same track
 * is decoded only once per process.
 */
object AudioWaveformRepository {
    private val cache = ConcurrentHashMap<String, List<Float>>()

    suspend fun getWaveform(
        context: Context,
        audioUri: Uri,
        targetBarCount: Int = 96
    ): List<Float> = withContext(Dispatchers.IO) {
        val key = "${audioUri}|$targetBarCount"
        cache[key]?.let { return@withContext it }

        val result = decodeWaveform(context.applicationContext, audioUri, targetBarCount)
        if (result.isNotEmpty()) cache[key] = result
        result
    }

    private fun decodeWaveform(
        context: Context,
        uri: Uri,
        targetBarCount: Int
    ): List<Float> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return emptyList()

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val durationUs = format.getLong(MediaFormat.KEY_DURATION, 0L).coerceAtLeast(1L)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceAtLeast(1)
            val pcmEncoding = format.getInteger(
                MediaFormat.KEY_PCM_ENCODING,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val peaks = FloatArray(targetBarCount)
            val counts = IntArray(targetBarCount)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex)
                        if (input != null) {
                            input.clear()
                            val sampleSize = extractor.readSampleData(input, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000L)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    else -> if (outputIndex >= 0) {
                        if (info.size > 0) {
                            val output = codec.getOutputBuffer(outputIndex)
                            if (output != null) {
                                output.position(info.offset)
                                output.limit(info.offset + info.size)
                                val data = output.slice().order(ByteOrder.LITTLE_ENDIAN)
                                val amplitude = rms(data, pcmEncoding, channels)
                                val bar = ((info.presentationTimeUs.toDouble() / durationUs) * targetBarCount)
                                    .toInt().coerceIn(0, targetBarCount - 1)
                                peaks[bar] += amplitude
                                counts[bar]++
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            val averaged = peaks.indices.map { i ->
                if (counts[i] == 0) 0f else peaks[i] / counts[i]
            }
            val max = averaged.maxOrNull()?.coerceAtLeast(0.0001f) ?: 1f
            averaged.map { (it / max).coerceIn(0.08f, 1f) }
        } catch (_: Exception) {
            emptyList()
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    private fun rms(buffer: ByteBuffer, encoding: Int, channels: Int): Float {
        if (encoding == android.media.AudioFormat.ENCODING_PCM_FLOAT) {
            val floats = buffer.asFloatBuffer()
            var sum = 0.0
            var count = 0
            while (floats.hasRemaining()) {
                val sample = floats.get().coerceIn(-1f, 1f)
                sum += (sample * sample).toDouble()
                count++
            }
            return if (count == 0) 0f else sqrt(sum / count).toFloat()
        }

        val samples = buffer.asShortBuffer()
        var sum = 0.0
        var count = 0
        while (samples.hasRemaining()) {
            val sample = samples.get() / 32768f
            sum += (sample * sample).toDouble()
            count++
        }
        return if (count == 0) 0f else sqrt(sum / count).toFloat()
    }
}
