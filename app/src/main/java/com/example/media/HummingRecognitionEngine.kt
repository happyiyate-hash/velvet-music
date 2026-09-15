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
    val id: String, val title: String, val artist: String, val album: String, val coverResId: Int,
    val matchPercentage: Int = 0, val matchedSnippet: String = "", val isrc: String = "", val track: Track? = null,
    val artworkUrl: String? = null, val spotifyUrl: String? = null, val appleMusicUrl: String? = null,
    val youtubeMusicUrl: String? = null, val audiomackUrl: String? = null
) {
    private val searchQuery: String get() = Uri.encode("$artist $title")
    val spotifyUri: String get() = "spotify:search:$searchQuery"
    val spotifyWebUrl: String get() = spotifyUrl ?: "https://open.spotify.com/search/$searchQuery"
    val audiomackUrlResolved: String get() = audiomackUrl ?: "https://audiomack.com/search?q=$searchQuery"
    val youtubeMusicUrlResolved: String get() = youtubeMusicUrl ?: "https://music.youtube.com/search?q=$searchQuery"
    val appleMusicUrlResolved: String get() = appleMusicUrl ?: "https://music.apple.com/us/search?term=$searchQuery"
}

sealed class HumRecognitionState {
    object Idle : HumRecognitionState()
    data class Listening(val elapsedSeconds: Int, val maxSeconds: Int = 8, val amplitude: Float = 0f, val detectedPitchHz: Float = 0f, val noteName: String = "") : HumRecognitionState()
    data class Analyzing(val message: String = "Sending audio to Velvet recognition...") : HumRecognitionState()
    data class Matched(val result: HumMatchResult) : HumRecognitionState()
    data class NoMatch(val reason: String = "No confident song match found. Try humming a longer, clearer melody.") : HumRecognitionState()
}

/** Real microphone capture and server-side recognition. No hardcoded song catalog or fake matches. */
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

    /** Auto mode sends one capture to the Worker batch endpoint; the Worker fans out to AudD and ACRCloud. */
    fun startListening(context: Context, libraryTracks: List<Track> = emptyList()) = startRecognition(context, libraryTracks, RecognitionMode.AUTO)
    fun startAmbientListening(context: Context, libraryTracks: List<Track> = emptyList()) = startRecognition(context, libraryTracks, RecognitionMode.AMBIENT)
    fun startHummingListening(context: Context, libraryTracks: List<Track> = emptyList()) = startRecognition(context, libraryTracks, RecognitionMode.HUMMING)
    fun triggerDemoMatch(index: Int = 0, libraryTracks: List<Track> = emptyList()) { lastContext?.let { startListening(it, libraryTracks) } }

    private enum class RecognitionMode { AUTO, HUMMING, AMBIENT }

    private fun startRecognition(context: Context, libraryTracks: List<Track>, mode: RecognitionMode) {
        stopListening()
        lastContext = context.applicationContext
        activeJob = scope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                _state.value = HumRecognitionState.NoMatch("Microphone permission is required to identify a song.")
                return@launch
            }
            val capture = captureAudio()
            if (capture == null || capture.wavData.isEmpty()) {
                _state.value = HumRecognitionState.NoMatch("Velvet could not capture microphone audio. Check microphone permission and try again.")
                return@launch
            }
            _state.value = HumRecognitionState.Analyzing(
                when (mode) {
                    RecognitionMode.HUMMING -> "Matching your melody with Velvet's recognition service..."
                    RecognitionMode.AMBIENT, RecognitionMode.AUTO -> "Identifying the music you are hearing..."
                }
            )
            val result = withContext(Dispatchers.IO) {
                when (mode) {
                    RecognitionMode.HUMMING -> repository.recognizeHumming(capture.wavData)
                    RecognitionMode.AMBIENT -> repository.recognizeAmbientAudio(capture.wavData)
                    RecognitionMode.AUTO -> repository.recognizeBatch(capture.wavData)
                }
            }
            when (result) {
                is RecognitionResult.Match -> _state.value = HumRecognitionState.Matched(toHumMatchResult(result.song, libraryTracks))
                is RecognitionResult.NoMatch -> _state.value = HumRecognitionState.NoMatch(result.reason)
                is RecognitionResult.Error -> _state.value = HumRecognitionState.NoMatch(result.reason)
            }
        }
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
                _liveAmplitude.value = amplitude; _livePitchHz.value = pitch
                val second = (elapsedMs / 1000L).toInt()
                if (second != lastPublishedSecond) {
                    lastPublishedSecond = second
                    _state.value = HumRecognitionState.Listening(second.coerceAtMost(MAX_CAPTURE_SECONDS), MAX_CAPTURE_SECONDS, amplitude, pitch, frequencyToNote(pitch))
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { recorder.stop() }; recorder.release()
        }
        _liveAmplitude.value = 0f; _livePitchHz.value = 0f
        if (pcm.size() < MIN_PCM_BYTES) return@withContext null
        AudioCapture(pcm.toWav(sampleRate, 1, 16), System.currentTimeMillis() - startedAt)
    }

    private fun toHumMatchResult(song: RecognizedSong, libraryTracks: List<Track>): HumMatchResult {
        val local = libraryTracks.firstOrNull { it.title.equals(song.title, true) && it.artist.equals(song.artist, true) }
        val fallbackCover = local?.coverResId ?: R.drawable.art_luminous_echoes
        val track = local ?: Track(id = "recognized:${song.id}", title = song.title, artist = song.artist, album = song.album, durationMs = song.durationMs, coverResId = fallbackCover, dominantColor = VelvetDeepCrimson, secondaryColor = VelvetBloodPlum, catalogSource = "Velvet Recognition", artworkUri = song.artworkUrl)
        return HumMatchResult(song.id, song.title, song.artist, song.album, track.coverResId, song.confidence, "Matched by Velvet's remote music recognition service", song.isrc.orEmpty(), track, song.artworkUrl, song.spotifyUrl, song.appleMusicUrl, song.youtubeMusicUrl, song.audiomackUrl)
    }

    fun stopListening() { activeJob?.cancel(); activeJob = null; _liveAmplitude.value = 0f; _livePitchHz.value = 0f }
    fun reset() { stopListening(); _state.value = HumRecognitionState.Idle }

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
        fun launchSpotify(context: Context, artist: String, title: String) = launchUrl(context, Uri.parse("spotify:search:${Uri.encode("$artist $title")}"), Uri.parse("https://open.spotify.com/search/${Uri.encode("$artist $title")}"), "Spotify")
        fun launchAudiomack(context: Context, artist: String, title: String) = launchUrl(context, Uri.parse("https://audiomack.com/search?q=${Uri.encode("$artist $title")}"), null, "Audiomack")
        fun launchYouTubeMusic(context: Context, artist: String, title: String) = launchUrl(context, Uri.parse("https://music.youtube.com/search?q=${Uri.encode("$artist $title")}"), null, "YouTube Music")
        fun launchAppleMusic(context: Context, artist: String, title: String) = launchUrl(context, Uri.parse("https://music.apple.com/us/search?term=${Uri.encode("$artist $title")}"), null, "Apple Music")
        private fun launchUrl(context: Context, uri: Uri, fallback: Uri?, label: String) {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
            catch (_: Exception) {
                if (fallback != null) try { context.startActivity(Intent(Intent.ACTION_VIEW, fallback).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }); return } catch (_: Exception) { }
                Toast.makeText(context, "Cannot open $label link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private data class AudioCapture(val wavData: ByteArray, val durationMs: Long)

private fun ByteArrayOutputStream.toWav(sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
    val pcm = toByteArray(); val header = ByteArray(44); val byteRate = sampleRate * channels * bitsPerSample / 8; val blockAlign = channels * bitsPerSample / 8
    fun intLE(offset: Int, value: Int) { header[offset] = (value and 0xFF).toByte(); header[offset + 1] = ((value ushr 8) and 0xFF).toByte(); header[offset + 2] = ((value ushr 16) and 0xFF).toByte(); header[offset + 3] = ((value ushr 24) and 0xFF).toByte() }
    fun shortLE(offset: Int, value: Int) { header[offset] = (value and 0xFF).toByte(); header[offset + 1] = ((value ushr 8) and 0xFF).toByte() }
    fun ascii(offset: Int, value: String) = value.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
    ascii(0, "RIFF"); intLE(4, 36 + pcm.size); ascii(8, "WAVE"); ascii(12, "fmt "); intLE(16, 16); shortLE(20, 1); shortLE(22, channels); intLE(24, sampleRate); intLE(28, byteRate); shortLE(32, blockAlign); shortLE(34, bitsPerSample); ascii(36, "data"); intLE(40, pcm.size)
    return header + pcm
}
