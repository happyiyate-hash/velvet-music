package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import com.example.media.ArtworkColorExtractor
import com.example.media.VelvetMediaSessionManager
import com.example.model.SampleData
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sin

data class AudioTelemetry(
    val transientSpike: Float = 0f,
    val sustainedEnergy: Float = 0.4f,
    val rmsLevel: Float = 0.3f,
    val kickDetected: Boolean = false,
    val snareDetected: Boolean = false,
    val dominantFrequencyHz: Float = 110f,
    val pipelineLatencyMs: Long = 4L
)

/**
 * Bulletproof Velvet Audio Engine:
 * - Reusable, persistent MediaPlayer lifecycle (avoids repeated object creation & SIGSEGV/crashes).
 * - Serialized track transition state machine with atomic request IDs.
 * - Thread-safe state synchronization protecting against rapid previous/next toggling.
 * - Non-blocking asynchronous artwork color extraction off the UI thread.
 * - Safe error handling guarding against missing files, empty playlists, and released states.
 */
class VelvetAudioEngine(
    private val scope: CoroutineScope
) {
    private var appContext: Context? = null

    // Synchronization primitives
    private val playerLock = Any()
    private val transitionMutex = Mutex()
    private var mediaPlayer: MediaPlayer? = null
    private var audioVisualizer: Visualizer? = null
    @Volatile private var liveRms = 0f
    @Volatile private var liveTransient = 0f
    @Volatile private var liveFrequencyHz = 110f
    @Volatile private var liveKick = false
    @Volatile private var liveSnare = false
    private var isPlayerPrepared = false

    private var playbackJob: Job? = null
    private var colorExtractionJob: Job? = null
    private val playbackRequestId = AtomicLong(0L)

    private val _currentTrack = MutableStateFlow<Track>(SampleData.defaultIdleTrack)
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _telemetry = MutableStateFlow(AudioTelemetry())
    val telemetry: StateFlow<AudioTelemetry> = _telemetry.asStateFlow()

    private val _isSoundCatchEnabled = MutableStateFlow(true)
    val isSoundCatchEnabled: StateFlow<Boolean> = _isSoundCatchEnabled.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _offlineCachedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val offlineCachedTrackIds: StateFlow<Set<String>> = _offlineCachedTrackIds.asStateFlow()

    private val _trackPlayCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val trackPlayCounts: StateFlow<Map<String, Int>> = _trackPlayCounts.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteTrackIds: StateFlow<Set<String>> = _favoriteTrackIds.asStateFlow()

    private val _nextQueueTrack = MutableStateFlow<Track?>(null)
    val nextQueueTrack: StateFlow<Track?> = _nextQueueTrack.asStateFlow()

    private val _deviceTracks = MutableStateFlow<List<Track>>(emptyList())
    val deviceTracks: StateFlow<List<Track>> = _deviceTracks.asStateFlow()

    private val _deletedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedTrackIds: StateFlow<Set<String>> = _deletedTrackIds.asStateFlow()

    init {
        startTelemetryLoop()
        startPlaybackProgress()
    }

    fun bindMediaSession(context: Context) {
        appContext = context.applicationContext
        VelvetMediaSessionManager.initialize(context)
        VelvetMediaSessionManager.onPlayAction = { resume() }
        VelvetMediaSessionManager.onPauseAction = { pause() }
        VelvetMediaSessionManager.onNextAction = { playNext() }
        VelvetMediaSessionManager.onPreviousAction = { playPrevious() }
        VelvetMediaSessionManager.onSeekAction = { pos -> seekTo(pos) }
        updateMediaSession()
    }

    private fun updateMediaSession() {
        val ctx = appContext ?: return
        VelvetMediaSessionManager.updatePlaybackState(
            context = ctx,
            track = _currentTrack.value,
            isPlaying = _isPlaying.value,
            playbackPositionMs = _playbackPositionMs.value
        )
    }

    fun setDeviceTracks(tracks: List<Track>) {
        _deviceTracks.value = tracks
        val currentId = _currentTrack.value.id
        if ((currentId == "device_music_idle" || currentId == "idle_device_track" || currentId == "le_1" || currentId == "track_1") && tracks.isNotEmpty()) {
            val first = tracks.first()
            _currentTrack.value = first
            _isPlaying.value = false
            _playbackPositionMs.value = 0L
            updateMediaSession()
            extractColorsLazily(first)
        }
    }

    fun addDeviceTrack(track: Track) {
        _deviceTracks.value = listOf(track) + _deviceTracks.value.filter { it.id != track.id }
        val currentId = _currentTrack.value.id
        if (currentId == "device_music_idle" || currentId == "idle_device_track") {
            _currentTrack.value = track
            _isPlaying.value = false
            _playbackPositionMs.value = 0L
            updateMediaSession()
            extractColorsLazily(track)
        }
    }

    /**
     * Serialized, bulletproof track switching:
     * 1. Updates UI instantly (0ms latency, no blocking on artwork or bitmap decoders).
     * 2. Atomic requestId cancels stale asynchronous player preparation.
     * 3. Mutex + lock protects MediaPlayer from concurrent setDataSource/release race conditions.
     */
    fun playTrack(track: Track) {
        val requestId = playbackRequestId.incrementAndGet()

        // 1. Instant UI update
        _currentTrack.value = track
        _playbackPositionMs.value = 0L

        val counts = _trackPlayCounts.value.toMutableMap()
        counts[track.id] = (counts[track.id] ?: 0) + 1
        _trackPlayCounts.value = counts

        updateMediaSession()

        // 2. Extract colors lazily in background
        extractColorsLazily(track)

        // 3. Simulated/Demo Playback if no contentUri (e.g. starter built-in tracks)
        if (track.contentUri.isNullOrBlank()) {
            synchronized(playerLock) {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.reset()
                } catch (_: Exception) {}
                isPlayerPrepared = false
            }
            _isPlaying.value = true
            startPlaybackProgress()
            updateMediaSession()
            return
        }

        // 4. Serialized player preparation on IO dispatcher
        scope.launch(Dispatchers.IO) {
            transitionMutex.withLock {
                if (requestId != playbackRequestId.get()) {
                    return@withLock
                }

                val ctx = appContext ?: return@withLock
                val trackUri = Uri.parse(track.contentUri)

                synchronized(playerLock) {
                    try {
                        isPlayerPrepared = false
                        releaseAudioVisualizer()

                        // Get or initialize persistent player
                        var player = mediaPlayer
                        if (player == null) {
                            player = MediaPlayer()
                            mediaPlayer = player
                        } else {
                            try {
                                player.reset()
                            } catch (_: Exception) {
                                try { player.release() } catch (_: Exception) {}
                                player = MediaPlayer()
                                mediaPlayer = player
                            }
                        }

                        player.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )

                        // Safely set data source via FileDescriptor or Context URI
                        var dataSourceSet = false
                        try {
                            val pfd = ctx.contentResolver.openFileDescriptor(trackUri, "r")
                            if (pfd != null) {
                                player.setDataSource(pfd.fileDescriptor)
                                pfd.close()
                                dataSourceSet = true
                            }
                        } catch (_: Exception) {}

                        if (!dataSourceSet) {
                            player.setDataSource(ctx, trackUri)
                        }

                        player.setOnPreparedListener { prepared ->
                            if (requestId != playbackRequestId.get()) {
                                return@setOnPreparedListener
                            }
                            synchronized(playerLock) {
                                isPlayerPrepared = true
                                try {
                                    prepared.start()
                                    _isPlaying.value = true
                                    _playbackPositionMs.value = 0L
                                    attachAudioVisualizer(prepared.audioSessionId)
                                } catch (e: Exception) {
                                    Log.e("VelvetAudioEngine", "Failed starting MediaPlayer", e)
                                    _isPlaying.value = false
                                }
                            }
                            startPlaybackProgress()
                            updateMediaSession()
                        }

                        player.setOnCompletionListener {
                            if (requestId == playbackRequestId.get()) {
                                _isPlaying.value = false
                                _playbackPositionMs.value = track.durationMs
                                updateMediaSession()
                                if (_isRepeat.value) {
                                    preparedRestart()
                                } else {
                                    playNext()
                                }
                            }
                        }

                        player.setOnErrorListener { _, what, extra ->
                            Log.e("VelvetAudioEngine", "MediaPlayer error: what=$what extra=$extra")
                            if (requestId == playbackRequestId.get()) {
                                synchronized(playerLock) {
                                    isPlayerPrepared = false
                                }
                                _isPlaying.value = false
                                playbackJob?.cancel()
                                updateMediaSession()
                            }
                            true // Return true to prevent triggering onCompletion
                        }

                        player.prepareAsync()
                    } catch (e: Exception) {
                        Log.e("VelvetAudioEngine", "Error preparing track: ${track.title}", e)
                        if (requestId == playbackRequestId.get()) {
                            synchronized(playerLock) {
                                isPlayerPrepared = false
                            }
                            _isPlaying.value = false
                            updateMediaSession()
                        }
                    }
                }
            }
        }
    }

    private fun attachAudioVisualizer(audioSessionId: Int) {
        releaseAudioVisualizer()
        if (audioSessionId <= 0) return
        try {
            audioVisualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                        ) {
                            if (waveform.isNullOrEmpty()) return
                            var sum = 0.0
                            var peak = 0f
                            waveform.forEach { value ->
                                val sample = (value.toInt() - 128) / 128f
                                val magnitude = kotlin.math.abs(sample)
                                sum += sample * sample
                                if (magnitude > peak) peak = magnitude
                            }
                            liveRms = kotlin.math.sqrt(sum / waveform.size).toFloat().coerceIn(0f, 1f)
                            liveTransient = (peak * 0.75f + liveRms * 0.25f).coerceIn(0f, 1f)
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 4) return
                            var bestMagnitude = 0f
                            var bestBin = 1
                            var lowEnergy = 0f
                            var midEnergy = 0f
                            var bin = 1
                            while (2 * bin + 1 < fft.size) {
                                val real = fft[2 * bin].toInt()
                                val imag = fft[2 * bin + 1].toInt()
                                val magnitude = kotlin.math.hypot(real.toFloat(), imag.toFloat())
                                val frequency = bin * (samplingRate / 1000f) / fft.size
                                if (magnitude > bestMagnitude) {
                                    bestMagnitude = magnitude
                                    bestBin = bin
                                }
                                when {
                                    frequency < 120f -> lowEnergy += magnitude
                                    frequency < 450f -> midEnergy += magnitude
                                }
                                bin++
                            }
                            val frequencyHz = (bestBin * (samplingRate / 1000f) / fft.size)
                                .coerceIn(20f, 20_000f)
                            liveFrequencyHz = frequencyHz
                            val fftEnergy = (bestMagnitude / 128f).coerceIn(0f, 1f)
                            liveRms = maxOf(liveRms * 0.65f, fftEnergy * 0.85f)
                            liveTransient = maxOf(liveTransient * 0.70f, fftEnergy)
                            liveKick = lowEnergy > midEnergy * 1.25f && liveTransient > 0.30f
                            liveSnare = midEnergy > lowEnergy * 1.10f && liveTransient > 0.34f
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                enabled = true
            }
        } catch (_: Throwable) {
            releaseAudioVisualizer()
        }
    }

    private fun releaseAudioVisualizer() {
        try { audioVisualizer?.enabled = false } catch (_: Throwable) {}
        try { audioVisualizer?.release() } catch (_: Throwable) {}
        audioVisualizer = null
        liveRms = 0f
        liveTransient = 0f
        liveFrequencyHz = 110f
        liveKick = false
        liveSnare = false
    }

    private fun extractColorsLazily(track: Track) {
        colorExtractionJob?.cancel()
        colorExtractionJob = scope.launch(Dispatchers.Default) {
            val ctx = appContext ?: return@launch
            try {
                val colors = if (!track.artworkUri.isNullOrBlank()) {
                    ArtworkColorExtractor.extractColorsFromUri(ctx, track.artworkUri)
                } else {
                    ArtworkColorExtractor.extractColors(ctx, track)
                }
                if (_currentTrack.value.id == track.id) {
                    _currentTrack.value = _currentTrack.value.copy(
                        dominantColor = colors.dominant,
                        secondaryColor = colors.secondary
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun preparedRestart() {
        synchronized(playerLock) {
            try {
                if (isPlayerPrepared) {
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                    _playbackPositionMs.value = 0L
                    _isPlaying.value = true
                    startPlaybackProgress()
                    updateMediaSession()
                }
            } catch (_: Exception) {
                _isPlaying.value = false
            }
        }
    }

    fun queueNext(track: Track) {
        _nextQueueTrack.value = track
    }

    fun toggleFavorite(trackId: String) {
        val current = _favoriteTrackIds.value.toMutableSet()
        if (!current.add(trackId)) current.remove(trackId)
        _favoriteTrackIds.value = current
    }

    fun deleteTrack(trackId: String) {
        _deletedTrackIds.value = _deletedTrackIds.value + trackId
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun pause() {
        synchronized(playerLock) {
            try {
                if (isPlayerPrepared && mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (_: Exception) {}
        }
        _isPlaying.value = false
        playbackJob?.cancel()
        updateMediaSession()
    }

    fun resume() {
        var resumed = false
        synchronized(playerLock) {
            try {
                if (isPlayerPrepared) {
                    mediaPlayer?.start()
                    resumed = true
                }
            } catch (_: Exception) {}
        }

        if (resumed) {
            _isPlaying.value = true
            startPlaybackProgress()
            updateMediaSession()
        } else {
            val track = _currentTrack.value
            if (!track.contentUri.isNullOrBlank()) {
                playTrack(track)
            } else {
                val available = getAllAvailableTracks()
                if (available.isNotEmpty()) {
                    playTrack(available.first())
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _currentTrack.value.durationMs.coerceAtLeast(0L)
        val target = positionMs.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        synchronized(playerLock) {
            try {
                if (isPlayerPrepared) {
                    mediaPlayer?.seekTo(target.toInt())
                }
            } catch (_: Exception) {}
        }
        _playbackPositionMs.value = target
        updateMediaSession()
    }

    fun toggleShuffle() { _isShuffle.value = !_isShuffle.value }
    fun toggleRepeat() { _isRepeat.value = !_isRepeat.value }
    fun toggleSoundCatch() { _isSoundCatchEnabled.value = !_isSoundCatchEnabled.value }

    fun toggleOfflineCache(trackId: String) {
        val current = _offlineCachedTrackIds.value.toMutableSet()
        if (!current.add(trackId)) current.remove(trackId)
        _offlineCachedTrackIds.value = current
    }

    fun playNext() {
        val queued = _nextQueueTrack.value
        if (queued != null) {
            _nextQueueTrack.value = null
            playTrack(queued)
            return
        }
        val all = getAllAvailableTracks()
        if (all.isEmpty()) {
            _isPlaying.value = false
            return
        }
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val nextIndex = if (currentIndex != -1 && currentIndex < all.lastIndex) currentIndex + 1 else 0
        playTrack(all[nextIndex])
    }

    fun playPrevious() {
        val all = getAllAvailableTracks()
        if (all.isEmpty()) {
            _isPlaying.value = false
            return
        }
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else all.lastIndex
        playTrack(all[prevIndex])
    }

    fun getAllAvailableTracks(): List<Track> {
        return _deviceTracks.value
            .distinctBy { it.id }
            .filterNot { _deletedTrackIds.value.contains(it.id) }
    }

    private fun startPlaybackProgress() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isPlaying.value) {
                delay(200L)
                val currentPos = synchronized(playerLock) {
                    try {
                        if (isPlayerPrepared && mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.currentPosition?.toLong()
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
                if (currentPos != null) {
                    _playbackPositionMs.value = currentPos
                    updateMediaSession()
                } else if (!isPlayerPrepared && _isPlaying.value) {
                    // Simulated progress for demo track
                    val nextPos = (_playbackPositionMs.value + 200L)
                    val dur = _currentTrack.value.durationMs.coerceAtLeast(1000L)
                    if (nextPos >= dur) {
                        _playbackPositionMs.value = 0L
                    } else {
                        _playbackPositionMs.value = nextPos
                    }
                    updateMediaSession()
                }
            }
        }
    }

    private fun startTelemetryLoop() {
        scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive) {
                delay(40L)
                if (_isPlaying.value) {
                    step++
                    val bpm = _currentTrack.value.bpm.coerceAtLeast(1)
                    val beatIntervalSteps = (60000 / bpm / 40).coerceAtLeast(6)
                    val isBeat = step % beatIntervalSteps == 0
                    val isSnare = step % (beatIntervalSteps * 2) == beatIntervalSteps
                    val visualizerActive = audioVisualizer != null && (liveRms > 0f || liveTransient > 0f)
                    val transient = if (visualizerActive) liveTransient else if (isBeat || isSnare) 0.85f + (0.15f * kotlin.random.Random.nextFloat()) else (_telemetry.value.transientSpike * 0.72f).coerceAtLeast(0f)
                    val sustained = if (visualizerActive) liveRms else 0.45f + (0.35f * sin(step * 0.06f))
                    val rms = if (visualizerActive) liveRms.coerceIn(0.05f, 1f) else (transient * 0.4f + sustained * 0.6f).coerceIn(0.1f, 1f)
                    _telemetry.value = AudioTelemetry(
                        transientSpike = transient,
                        sustainedEnergy = sustained,
                        rmsLevel = rms,
                        kickDetected = if (visualizerActive) liveKick else isBeat,
                        snareDetected = if (visualizerActive) liveSnare else isSnare,
                        dominantFrequencyHz = if (visualizerActive) liveFrequencyHz else if (isBeat) 55f else 220f + (sin(step * 0.1f) * 110f),
                        pipelineLatencyMs = if (visualizerActive) 4L else (3L..6L).random()
                    )
                } else {
                    _telemetry.value = _telemetry.value.copy(
                        transientSpike = 0f,
                        sustainedEnergy = 0.2f,
                        rmsLevel = 0.05f,
                        kickDetected = false,
                        snareDetected = false
                    )
                }
            }
        }
    }

    fun release() {
        playbackRequestId.incrementAndGet()
        playbackJob?.cancel()
        colorExtractionJob?.cancel()
        synchronized(playerLock) {
            isPlayerPrepared = false
            releaseAudioVisualizer()
            try { mediaPlayer?.stop() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
        }
        _isPlaying.value = false
    }
}
