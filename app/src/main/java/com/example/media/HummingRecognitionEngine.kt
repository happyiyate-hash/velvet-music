package com.example.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.Track
import com.example.recognition.ACRCloudNativeRecognitionRepository
import com.example.recognition.RecognitionDiagnostics
import com.example.recognition.RecognitionDiagnosticsStore
import com.example.recognition.RecognitionResult
import com.example.recognition.RecognizedSong
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetDeepCrimson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HumMatchResult(
    val id: String, val title: String, val artist: String, val album: String,
    val coverResId: Int, val matchPercentage: Int = 0, val matchedSnippet: String = "",
    val isrc: String = "", val track: Track? = null, val artworkUrl: String? = null,
    val spotifyUrl: String? = null, val appleMusicUrl: String? = null,
    val youtubeMusicUrl: String? = null, val audiomackUrl: String? = null,
    val soundcloudUrl: String? = null, val boomplayUrl: String? = null, val durationMs: Long = 0L
)

sealed class HumRecognitionState {
    object Idle : HumRecognitionState()
    data class Listening(val elapsedSeconds: Int, val maxSeconds: Int = 0, val amplitude: Float = 0f,
        val detectedPitchHz: Float = 0f, val noteName: String = "") : HumRecognitionState()
    data class Searching(val message: String = "Identifying…") : HumRecognitionState()
    data class Matched(val result: HumMatchResult, val diagnostics: RecognitionDiagnostics? = null) : HumRecognitionState()
    data class NoMatch(val title: String = "Couldn't identify that song", val message: String = "Try singing a little louder or move closer to the music.", val diagnostics: RecognitionDiagnostics? = null) : HumRecognitionState()
    data class ConnectionError(val title: String = "Couldn't connect", val message: String = "Check your connection and try again.", val diagnostics: RecognitionDiagnostics? = null) : HumRecognitionState()
    data class ProviderError(val title: String = "Couldn't search right now", val message: String = "Recognition services encountered an issue. Please try again.", val diagnostics: RecognitionDiagnostics? = null) : HumRecognitionState()
    data class ResponseParsingError(val title: String = "Couldn't parse response", val message: String = "Failed to parse recognition response.", val diagnostics: RecognitionDiagnostics? = null) : HumRecognitionState()
}

/** Live microphone -> ACRCloud Android SDK. No AudioRecord/WAV/HTTP batch fallback. */
class HummingRecognitionEngine(private val scope: CoroutineScope) {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<HumRecognitionState>(HumRecognitionState.Idle)
    val state: kotlinx.coroutines.flow.StateFlow<HumRecognitionState> = _state
    private val _liveAmplitude = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val liveAmplitude: kotlinx.coroutines.flow.StateFlow<Float> = _liveAmplitude
    private val _livePitchHz = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val livePitchHz: kotlinx.coroutines.flow.StateFlow<Float> = _livePitchHz
    private val _lastDiagnostics = kotlinx.coroutines.flow.MutableStateFlow<RecognitionDiagnostics?>(null)
    val lastDiagnostics: kotlinx.coroutines.flow.StateFlow<RecognitionDiagnostics?> = _lastDiagnostics

    private var activeJob: Job? = null
    private var nativeVolumeJob: Job? = null
    private var nativeRecognizer: ACRCloudNativeRecognitionRepository? = null
    private var lastContext: Context? = null

    fun loadSavedDiagnostics(context: Context) {
        RecognitionDiagnosticsStore.getLastDiagnostics(context)?.let { _lastDiagnostics.value = it }
    }

    fun startListening(context: Context, libraryTracks: List<Track> = emptyList()) {
        stopListening()
        lastContext = context.applicationContext
        loadSavedDiagnostics(context)

        activeJob = scope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                val diag = RecognitionDiagnostics(
                    timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                    exceptionClass = "SecurityException",
                    exceptionMessage = "Microphone permission (RECORD_AUDIO) was not granted.",
                    causeMessage = "Permission denied at runtime",
                    isNetworkFailure = false
                )
                _lastDiagnostics.value = diag
                RecognitionDiagnosticsStore.saveLastDiagnostics(context, diag)
                _state.value = HumRecognitionState.ConnectionError("Permission needed", "Microphone permission is required to identify a song.", diag)
                return@launch
            }

            val direct = nativeRecognizer ?: ACRCloudNativeRecognitionRepository(context).also { nativeRecognizer = it }
            _state.value = HumRecognitionState.Listening(0, 0)
            _liveAmplitude.value = 0f
            _livePitchHz.value = 0f

            val started = try {
                direct.startRecognition { result -> handleRecognitionResult(context, result, libraryTracks) }
            } catch (t: Throwable) {
                android.util.Log.e("HummingRecognition", "Native ACRCloud start failed", t)
                false
            }

            if (!started) {
                val diag = RecognitionDiagnostics(
                    timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                    exceptionClass = "ACRCloudStartException",
                    exceptionMessage = "Native ACRCloud microphone recognition could not start.",
                    causeMessage = "ACRCloud SDK initialization or startRecognize() failed",
                    isNetworkFailure = false
                )
                _lastDiagnostics.value = diag
                RecognitionDiagnosticsStore.saveLastDiagnostics(context, diag)
                _state.value = HumRecognitionState.ProviderError(
                    "Couldn't start recognition",
                    "Velvet could not start live microphone recognition. Check the ACRCloud configuration used to build this APK.",
                    diag
                )
                return@launch
            }

            nativeVolumeJob?.cancel()
            nativeVolumeJob = launch {
                direct.volume.collect { value -> _liveAmplitude.value = (value.toFloat() / 100f).coerceIn(0f, 1f) }
            }

            launch {
                var seconds = 0
                while (isActive) {
                    _state.value = HumRecognitionState.Listening(seconds, 0, _liveAmplitude.value)
                    delay(1000L)
                    seconds++
                }
            }

            awaitCancellation()
        }
    }

    fun startAmbientListening(context: Context, libraryTracks: List<Track> = emptyList()) = startListening(context, libraryTracks)
    fun startHummingListening(context: Context, libraryTracks: List<Track> = emptyList()) = startListening(context, libraryTracks)

    /** Kept for API compatibility; starts the real live recognizer. */
    fun triggerDemoMatch(index: Int = 0, libraryTracks: List<Track> = emptyList()) {
        lastContext?.let { startListening(it, libraryTracks) }
    }

    private fun handleRecognitionResult(context: Context, result: RecognitionResult, libraryTracks: List<Track>) {
        val diagnostics = when (result) {
            is RecognitionResult.Match -> result.diagnostics
            is RecognitionResult.NoMatch -> result.diagnostics
            is RecognitionResult.ProviderError -> result.diagnostics
            is RecognitionResult.ResponseParsingError -> result.diagnostics
            is RecognitionResult.ConnectionError -> result.diagnostics
        }
        diagnostics?.let {
            _lastDiagnostics.value = it
            RecognitionDiagnosticsStore.saveLastDiagnostics(context, it)
        }
        when (result) {
            is RecognitionResult.Match -> _state.value = HumRecognitionState.Matched(toHumMatchResult(result.song, libraryTracks), diagnostics)
            is RecognitionResult.NoMatch -> _state.value = HumRecognitionState.NoMatch(result.title, result.reason, diagnostics)
            is RecognitionResult.ProviderError -> _state.value = HumRecognitionState.ProviderError(result.title, result.reason, diagnostics)
            is RecognitionResult.ResponseParsingError -> _state.value = HumRecognitionState.ResponseParsingError(result.title, result.reason, diagnostics)
            is RecognitionResult.ConnectionError -> _state.value = HumRecognitionState.ConnectionError(result.title, result.reason, diagnostics)
        }
    }

    private fun toHumMatchResult(song: RecognizedSong, libraryTracks: List<Track>): HumMatchResult {
        val local = libraryTracks.firstOrNull { it.title.equals(song.title, true) && it.artist.equals(song.artist, true) }
        val cleanedArtwork = com.example.recognition.SongArtworkResolver.cleanArtworkUrl(song.artworkUrl)
        val fallbackCover = local?.coverResId ?: R.drawable.art_luminous_echoes
        val track = local?.copy(artworkUri = cleanedArtwork ?: local.artworkUri) ?: Track(
            id = "recognized:${song.id}",
            title = song.title, artist = song.artist, album = song.album,
            durationMs = if (song.durationMs > 0L) song.durationMs else 210000L,
            coverResId = fallbackCover, dominantColor = VelvetDeepCrimson,
            secondaryColor = VelvetBloodPlum, catalogSource = "Velvet Recognition", artworkUri = cleanedArtwork
        )
        return HumMatchResult(
            id = song.id, title = song.title, artist = song.artist, album = song.album,
            coverResId = track.coverResId, matchPercentage = song.confidence,
            matchedSnippet = "Identified by Velvet's live recognition pipeline",
            isrc = song.isrc.orEmpty(), track = track, artworkUrl = cleanedArtwork,
            spotifyUrl = song.spotifyUrl, appleMusicUrl = song.appleMusicUrl,
            youtubeMusicUrl = song.youtubeMusicUrl, audiomackUrl = song.audiomackUrl,
            soundcloudUrl = song.soundcloudUrl, boomplayUrl = song.boomplayUrl, durationMs = song.durationMs
        )
    }

    fun stopListening() {
        nativeVolumeJob?.cancel()
        nativeVolumeJob = null
        activeJob?.cancel()
        activeJob = null
        nativeRecognizer?.stopRecognition()
        _liveAmplitude.value = 0f
        _livePitchHz.value = 0f
    }

    fun reset() {
        stopListening()
        _state.value = HumRecognitionState.Idle
    }
}
