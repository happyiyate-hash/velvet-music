package com.example.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.Track
import com.example.recognition.RecognitionResult
import com.example.recognition.RecognizedSong
import com.example.recognition.RemoteSongRecognitionRepository
import com.example.recognition.SongRecognitionRepository
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetDeepCrimson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.log2
import kotlin.math.sqrt

data class HumMatchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverResId: Int,
    val matchPercentage: Int = 0,
    val matchedSnippet: String = "",
    val isrc: String = "",
    val track: Track? = null,
    val artworkUrl: String? = null,
    val spotifyUrl: String? = null,
    val appleMusicUrl: String? = null,
    val youtubeMusicUrl: String? = null,
    val audiomackUrl: String? = null,
    val durationMs: Long = 0L
)

sealed class HumRecognitionState {
    object Idle : HumRecognitionState()
    data class Listening(
        val elapsedSeconds: Int,
        val maxSeconds: Int = 8,
        val amplitude: Float = 0f,
        val detectedPitchHz: Float = 0f,
        val noteName: String = ""
    ) : HumRecognitionState()
    data class Searching(
        val message: String = "Identifying…"
    ) : HumRecognitionState()
    data class Matched(
        val result: HumMatchResult
    ) : HumRecognitionState()
    data class NoMatch(
        val title: String = "Couldn't identify that song",
        val message: String = "Try singing a little louder or move closer to the music."
    ) : HumRecognitionState()
    data class ConnectionError(
        val title: String = "Couldn't connect",
        val message: String = "Check your connection and try again."
    ) : HumRecognitionState()
}

/**
 * Real microphone capture and server-side recognition.
 * No hardcoded song catalog, no fake matches.
 * The only successful match originates from the real backend response.
 */
class HummingRecognitionEngine(
    private val scope: CoroutineScope,
    private val repository: SongRecognitionRepository = RemoteSongRecognitionRepository()
) {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<HumRecognitionState>(HumRecognitionState.Idle)
    val state: kotlinx.coroutines.flow.StateFlow<HumRecognitionState> = _state
    private val _liveAmplitude = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val liveAmplitude: kotlinx.coroutines.flow.StateFlow<Float> = _liveAmplitude
    private val _livePitchHz = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val livePitchHz: kotlinx.coroutines.flow.StateFlow<Float> = _livePitchHz
    private var activeJob: Job? = null
    private var lastContext: Context? = null

    /** Starts capturing microphone audio and sends it to the batch recognition endpoint. */
    fun startListening(context: Context, libraryTracks: List<Track> = emptyList()) {
        stopListening()
        lastContext = context.applicationContext
        activeJob = scope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                _state.value = HumRecognitionState.ConnectionError(
                    title = "Permission needed",
                    message = "Microphone permission is required to identify a song."
                )
                return@launch
            }

            _state.value = HumRecognitionState.Listening(0, MAX_CAPTURE_SECONDS)
            val capture = captureAudio()
            if (capture == null || capture.wavData.isEmpty()) {
                _state.value = HumRecognitionState.ConnectionError(
                    title = "Microphone error",
                    message = "Velvet could not capture microphone audio. Check microphone permission and try again."
                )
                return@launch
            }

            // Transition smoothly to searching state
            _state.value = HumRecognitionState.Searching("Identifying…")

            val result = withContext(Dispatchers.IO) {
                repository.recognizeBatch(capture.wavData)
            }

            when (result) {
                is RecognitionResult.Match -> {
                    _state.value = HumRecognitionState.Matched(toHumMatchResult(result.song, libraryTracks))
                }
                is RecognitionResult.NoMatch -> {
                    _state.value = HumRecognitionState.NoMatch(
                        title = result.title,
                        message = result.reason
                    )
                }
                is RecognitionResult.ProviderError -> {
                    _state.value = HumRecognitionState.ConnectionError(
                        title = result.title,
                        message = result.reason
                    )
                }
                is RecognitionResult.ResponseParsingError -> {
                    _state.value = HumRecognitionState.ConnectionError(
                        title = result.title,
                        message = result.reason
                    )
                }
                is RecognitionResult.ConnectionError -> {
                    _state.value = HumRecognitionState.ConnectionError(
                        title = result.title,
                        message = result.reason
                    )
                }
            }
        }
    }

    fun startAmbientListening(context: Context, libraryTracks: List<Track> = emptyList()) =
        startListening(context, libraryTracks)

    fun startHummingListening(context: Context, libraryTracks: List<Track> = emptyList()) =
        startListening(context, libraryTracks)

    /** Kept only for API compatibility; initiates real listening, NEVER fake simulation. */
    fun triggerDemoMatch(index: Int = 0, libraryTracks: List<Track> = emptyList()) {
        lastContext?.let { startListening(it, libraryTracks) }
    }

    private suspend fun captureAudio(): AudioCapture? = withContext(Dispatchers.IO) {
        val sampleRate = 16_000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minimum = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minimum <= 0) return@withContext null
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, (minimum * 2).coerceAtLeast(4096))
        } catch (_: Throwable) { return@withContext null }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); return@withContext null }
        val pcm = ByteArrayOutputStream(sampleRate * 2 * MAX_CAPTURE_SECONDS)
        val samples = ShortArray(1024)
        val startedAt = System.currentTimeMillis()
        var lastPublishedSecond = -1
        try {
            recorder.startRecording()
            while (scope.coroutineContext.isActive) {
                val elapsedMs = System.currentTimeMillis() - startedAt
                if (elapsedMs >= MAX_CAPTURE_SECONDS * 1000L) break
                val read = recorder.read(samples, 0, samples.size)
                if (read <= 0) continue
                var sumSquares = 0.0
                var crossings = 0
                for (i in 0 until read) {
                    val sample = samples[i].toInt()
                    sumSquares += sample.toDouble() * sample.toDouble()
                    if (i > 0) { val previous = samples[i - 1].toInt(); if ((previous < 0 && sample >= 0) || (previous >= 0 && sample < 0)) crossings++ }
                    pcm.write(sample and 0xFF); pcm.write((sample ushr 8) and 0xFF)
                }
                val rms = sqrt(sumSquares / read).toFloat()
                val amplitude = (rms / 3500f).coerceIn(0f, 1f)
                val durationSeconds = read.toFloat() / sampleRate
                val pitch = if (durationSeconds > 0f) (crossings / (2f * durationSeconds)).coerceIn(55f, 1200f) else 0f
                _liveAmplitude.value = amplitude
                _livePitchHz.value = pitch
                val second = (elapsedMs / 1000L).toInt()
                if (second != lastPublishedSecond) {
                    lastPublishedSecond = second
                    _state.value = HumRecognitionState.Listening(
                        second.coerceAtMost(MAX_CAPTURE_SECONDS),
                        MAX_CAPTURE_SECONDS,
                        amplitude,
                        pitch,
                        frequencyToNote(pitch)
                    )
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        _liveAmplitude.value = 0f
        _livePitchHz.value = 0f
        if (pcm.size() < MIN_PCM_BYTES) return@withContext null
        AudioCapture(pcm.toWav(sampleRate, 1, 16), System.currentTimeMillis() - startedAt)
    }

    private fun toHumMatchResult(song: RecognizedSong, libraryTracks: List<Track>): HumMatchResult {
        val local = libraryTracks.firstOrNull {
            it.title.equals(song.title, true) && it.artist.equals(song.artist, true)
        }
        val fallbackCover = local?.coverResId ?: R.drawable.art_luminous_echoes
        val track = local ?: Track(
            id = "recognized:${song.id}",
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = if (song.durationMs > 0L) song.durationMs else 210000L,
            coverResId = fallbackCover,
            dominantColor = VelvetDeepCrimson,
            secondaryColor = VelvetBloodPlum,
            catalogSource = "Velvet Recognition",
            artworkUri = song.artworkUrl
        )
        return HumMatchResult(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            coverResId = track.coverResId,
            matchPercentage = song.confidence,
            matchedSnippet = "Identified by Velvet's recognition pipeline",
            isrc = song.isrc.orEmpty(),
            track = track,
            artworkUrl = song.artworkUrl,
            spotifyUrl = song.spotifyUrl,
            appleMusicUrl = song.appleMusicUrl,
            youtubeMusicUrl = song.youtubeMusicUrl,
            audiomackUrl = song.audiomackUrl,
            durationMs = song.durationMs
        )
    }

    fun stopListening() {
        activeJob?.cancel()
        activeJob = null
        _liveAmplitude.value = 0f
        _livePitchHz.value = 0f
    }

    fun reset() {
        stopListening()
        _state.value = HumRecognitionState.Idle
    }

    private fun frequencyToNote(freqHz: Float): String {
        if (freqHz < 55f) return ""
        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val midi = (69 + 12 * log2(freqHz / 440.0)).toInt()
        if (midi !in 12..120) return ""
        return "${notes[midi % 12]}${midi / 12 - 1}"
    }

    companion object {
        private const val MAX_CAPTURE_SECONDS = 8
        private const val MIN_PCM_BYTES = 8_000
    }
}

private data class AudioCapture(val wavData: ByteArray, val durationMs: Long)

private fun ByteArrayOutputStream.toWav(sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
    val pcm = toByteArray()
    val header = ByteArray(44)
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    fun intLE(offset: Int, value: Int) {
        header[offset] = (value and 0xFF).toByte()
        header[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        header[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        header[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
    fun shortLE(offset: Int, value: Int) {
        header[offset] = (value and 0xFF).toByte()
        header[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }
    fun ascii(offset: Int, value: String) = value.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
    ascii(0, "RIFF")
    intLE(4, 36 + pcm.size)
    ascii(8, "WAVE")
    ascii(12, "fmt ")
    intLE(16, 16)
    shortLE(20, 1)
    shortLE(22, channels)
    intLE(24, sampleRate)
    intLE(28, byteRate)
    shortLE(32, blockAlign)
    shortLE(34, bitsPerSample)
    ascii(36, "data")
    intLE(40, pcm.size)
    return header + pcm
}

