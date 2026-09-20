package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
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
import kotlin.math.hypot
import kotlin.math.sin

data class AudioTelemetry(
    val transientSpike: Float = 0f,
    val sustainedEnergy: Float = 0.4f,
    val rmsLevel: Float = 0.3f,
    val kickDetected: Boolean = false,
    val snareDetected: Boolean = false,
    val dominantFrequencyHz: Float = 110f,
    val pipelineLatencyMs: Long = 4L,
    val fftBars: FloatArray = FloatArray(64) { 0f }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioTelemetry
        if (transientSpike != other.transientSpike) return false
        if (sustainedEnergy != other.sustainedEnergy) return false
        if (rmsLevel != other.rmsLevel) return false
        if (kickDetected != other.kickDetected) return false
        if (snareDetected != other.snareDetected) return false
        if (dominantFrequencyHz != other.dominantFrequencyHz) return false
        if (pipelineLatencyMs != other.pipelineLatencyMs) return false
        if (!fftBars.contentEquals(other.fftBars)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = transientSpike.hashCode()
        result = 31 * result + sustainedEnergy.hashCode()
        result = 31 * result + rmsLevel.hashCode()
        result = 31 * result + kickDetected.hashCode()
        result = 31 * result + snareDetected.hashCode()
        result = 31 * result + dominantFrequencyHz.hashCode()
        result = 31 * result + pipelineLatencyMs.hashCode()
        result = 31 * result + fftBars.contentHashCode()
        return result
    }
}

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
    private val liveFftBars = FloatArray(64) { 0f }
    @Volatile private var isPlayerPrepared = false

    private var playbackJob: Job? = null
    private var colorExtractionJob: Job? = null
    private val playbackRequestId = AtomicLong(0L)

    private val _currentTrack = MutableStateFlow<Track>(SampleData.defaultIdleTrack)
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

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

    private val _activeQueue = MutableStateFlow<List<Track>>(emptyList())
    val activeQueue: StateFlow<List<Track>> = _activeQueue.asStateFlow()

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

    /**
     * Pushes metadata and resolved artwork bitmap directly to the system media session.
     */
    fun updateMediaSessionMetadata(title: String, artist: String, artworkBitmap: Bitmap) {
        VelvetMediaSessionManager.updateMediaSessionMetadata(title, artist, artworkBitmap)
    }

    fun setDeviceTracks(tracks: List<Track>) {
        _deviceTracks.value = tracks
        val currentId = _currentTrack.value.id
        if ((currentId == "device_music_idle" || currentId == "idle_device_track" || currentId == "le_1" || currentId == "track_1") && tracks.isNotEmpty()) {
            val first = tracks.first()
            _currentTrack.value = first
            _activeQueue.value = tracks
            _isPlaying.value = false
            _playbackPositionMs.value = 0L
            updateMediaSession()
            extractColorsLazily(first)
        } else if (_activeQueue.value.isEmpty() && tracks.isNotEmpty()) {
            val current = _currentTrack.value
            val remaining = tracks.filterNot { it.id == current.id }
            _activeQueue.value = listOf(current) + remaining
        }
    }

    fun addDeviceTrack(track: Track) {
        _deviceTracks.value = listOf(track) + _deviceTracks.value.filter { it.id != track.id }
        _activeQueue.value = listOf(track) + _activeQueue.value.filter { it.id != track.id }
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
    fun playTrack(track: Track, updateQueue: Boolean = true) {
        val requestId = playbackRequestId.incrementAndGet()

        // 1. Instant UI update
        _currentTrack.value = track
        _playbackPositionMs.value = 0L

        if (updateQueue) {
            // Selecting a track must never reorder the visible queue.
            val current = _activeQueue.value
            if (current.none { it.id == track.id }) {
                _activeQueue.value = current + track
            }
        }

        val counts = _trackPlayCounts.value.toMutableMap()
        counts[track.id] = (counts[track.id] ?: 0) + 1
        _trackPlayCounts.value = counts

        updateMediaSession()

        // 2. Extract colors lazily in background
        extractColorsLazily(track)

        // 3. Playback: Resolve track URI, falling back to rich synthesized audio if local URI is absent
        val contentUriString = if (!track.contentUri.isNullOrBlank()) {
            track.contentUri
        } else {
            appContext?.let { SynthesizedAudioProvider.getOrCreateDemoAudioUri(it).toString() }
        }

        if (contentUriString.isNullOrBlank()) {
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
                val trackUri = Uri.parse(contentUriString)

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
                                try { player.stop() } catch (_: Exception) {}
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
                                    _audioSessionId.value = prepared.audioSessionId
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
        _audioSessionId.value = audioSessionId
        if (audioSessionId <= 0) return
        val ctx = appContext
        if (ctx != null && ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            val range = Visualizer.getCaptureSizeRange()
            val targetSize = range[1].coerceAtMost(1024)
            audioVisualizer = Visualizer(audioSessionId).apply {
                captureSize = targetSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                        ) {}

                        override fun onFftDataCapture(
                            visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int
                        ) {
                            if (fft == null || fft.isEmpty()) return

                            // Logarithmic frequency binning & dB scaling across 64 bars
                            val barCount = liveFftBars.size
                            val fftSize = fft.size / 2
                            if (fftSize >= 4) {
                                val minFreqBin = 1.0
                                val maxFreqBin = (fftSize - 1).toDouble()

                                for (i in 0 until barCount) {
                                    val fracStart = i.toDouble() / barCount
                                    val fracEnd = (i + 1).toDouble() / barCount

                                    // Log-spaced start and end bin indices in FFT buffer
                                    val logStart = (minFreqBin * Math.pow(maxFreqBin / minFreqBin, fracStart)).toInt().coerceIn(1, fftSize - 1)
                                    val logEnd = (minFreqBin * Math.pow(maxFreqBin / minFreqBin, fracEnd)).toInt().coerceIn(logStart + 1, fftSize)

                                    var sum = 0.0
                                    var count = 0
                                    for (j in logStart until logEnd) {
                                        val r = fft[2 * j].toDouble()
                                        val img = fft[2 * j + 1].toDouble()
                                        sum += hypot(r, img)
                                        count++
                                    }

                                    val avgMagnitude = if (count > 0) sum / count else 0.0

                                    // Apply Logarithmic scaling & dB conversion to boost quiet frequencies
                                    val db = 20.0 * kotlin.math.log10(avgMagnitude.coerceAtLeast(1.0))

                                    // Natural treble roll-off compensation:
                                    // High frequencies have naturally lower energy in music.
                                    // A progressive tilt (+0 to +14 dB) normalizes perceived amplitude across the spectrum.
                                    val norm = i.toDouble() / (barCount - 1).coerceAtLeast(1)
                                    val trebleTiltDb = norm * 14.0

                                    // Calibrated dynamic range floor & ceiling:
                                    // 8 dB noise floor to 48 dB max peak
                                    val minDb = 8.0
                                    val maxDb = 48.0
                                    val targetNormalized = (((db + trebleTiltDb) - minDb) / (maxDb - minDb)).coerceIn(0.06, 1.0).toFloat()

                                    // Instant Up (fast attack) & Fast Falloff (fast decay)
                                    val current = liveFftBars[i]
                                    val smoothed = if (targetNormalized > current) {
                                        targetNormalized // 100% Instant attack
                                    } else {
                                        current - (current - targetNormalized) * 0.45f // Fast snappy drop
                                    }
                                    liveFftBars[i] = smoothed.coerceIn(0.06f, 1.0f)
                                }
                            }

                            // Split zones from the logarithmic spectrum:
                            // Low bass (bars 0..10), Mids (bars 11..38), Treble (bars 39..63)
                            var bassSum = 0f
                            for (b in 0..minOf(10, liveFftBars.size - 1)) bassSum += liveFftBars[b]
                            val bassEnergy = bassSum / 11f

                            var midSum = 0f
                            for (m in 11..minOf(38, liveFftBars.size - 1)) midSum += liveFftBars[m]
                            val midEnergy = midSum / 28f

                            var trebleSum = 0f
                            for (t in 39..minOf(63, liveFftBars.size - 1)) trebleSum += liveFftBars[t]
                            val trebleEnergy = trebleSum / 25f

                            liveRms = (bassEnergy * 0.45f + midEnergy * 0.35f + trebleEnergy * 0.20f).coerceIn(0.05f, 1.0f)
                            liveTransient = maxOf(bassEnergy, midEnergy * 0.90f, trebleEnergy * 0.85f)
                            liveKick = bassEnergy > 0.40f && bassEnergy > midEnergy * 1.10f
                            liveSnare = midEnergy > 0.35f && (midEnergy > bassEnergy * 0.88f || trebleEnergy > 0.32f)
                            liveFrequencyHz = (40f * Math.pow(16000.0 / 40.0, ((liveFftBars.indices.maxByOrNull { liveFftBars[it] } ?: 0).toFloat() / (barCount - 1)).toDouble())).toFloat().coerceIn(20f, 20_000f)
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    false, // Waveform
                    true   // FFT
                )
                enabled = _isPlaying.value
            }
        } catch (_: Throwable) {
            releaseAudioVisualizer()
        }
    }

    private fun releaseAudioVisualizer() {
        try { audioVisualizer?.enabled = false } catch (_: Throwable) {}
        try { audioVisualizer?.release() } catch (_: Throwable) {}
        audioVisualizer = null
        _audioSessionId.value = 0
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
        val current = _activeQueue.value.filterNot { it.id == track.id }
        val playingIndex = current.indexOfFirst { it.id == _currentTrack.value.id }
        val updated = if (playingIndex != -1) {
            current.take(playingIndex + 1) + track + current.drop(playingIndex + 1)
        } else if (current.isNotEmpty()) {
            listOf(current[0], track) + current.drop(1)
        } else {
            listOf(track)
        }
        _activeQueue.value = updated
        _nextQueueTrack.value = track
    }

    /**
     * Synchronizes UI drag-and-drop directly with the active playback queue.
     * When dragging an item, the active queue order is updated immediately.
     * Preserves physical position of all items without unexpected rotation.
     */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val current = _activeQueue.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        // 1. Move item inside internal player queue
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _activeQueue.value = current

        // Clear one-off queued track so explicit drag order takes full priority
        _nextQueueTrack.value = null

        // 2. If shuffle is enabled, update shuffle order or sync active list
        if (_isShuffle.value) {
            _isShuffle.value = false
        }
    }

    fun updateQueueList(updatedQueue: List<Track>) {
        _activeQueue.value = updatedQueue
        _nextQueueTrack.value = null
        if (_isShuffle.value) {
            _isShuffle.value = false
        }
    }

    fun removeFromQueue(trackId: String) {
        _activeQueue.value = _activeQueue.value.filterNot { it.id == trackId }
    }

    fun toggleFavorite(trackId: String) {
        val current = _favoriteTrackIds.value.toMutableSet()
        if (!current.add(trackId)) current.remove(trackId)
        _favoriteTrackIds.value = current
    }

    fun deleteTrack(trackId: String) {
        _deletedTrackIds.value = _deletedTrackIds.value + trackId
        _activeQueue.value = _activeQueue.value.filterNot { it.id == trackId }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun pause() {
        synchronized(playerLock) {
            try {
                if (isPlayerPrepared) {
                    mediaPlayer?.pause()
                }
            } catch (_: Exception) {}
        }
        try { audioVisualizer?.enabled = false } catch (_: Throwable) {}
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
            try { audioVisualizer?.enabled = true } catch (_: Throwable) {}
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

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        if (newShuffle) {
            // YouTube Music shuffle behavior:
            // Index 0 (currently playing track) remains at index 0.
            // The remaining upcoming tracks in the active queue are shuffled.
            val current = _currentTrack.value
            val upcoming = _activeQueue.value.filterNot { it.id == current.id }.shuffled()
            _activeQueue.value = listOf(current) + upcoming
        } else {
            // Restore normal available order starting with the current track
            val current = _currentTrack.value
            val all = getAllAvailableTracks().filterNot { it.id == current.id }
            _activeQueue.value = listOf(current) + all
        }
    }

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
            playTrack(queued, updateQueue = false)
            return
        }
        val queue = _activeQueue.value.filterNot { _deletedTrackIds.value.contains(it.id) }
        if (queue.isNotEmpty()) {
            val currentIndex = queue.indexOfFirst { it.id == _currentTrack.value.id }
            val nextIndex = if (currentIndex != -1 && currentIndex < queue.lastIndex) {
                currentIndex + 1
            } else if (_isRepeat.value || queue.size == 1) {
                0
            } else {
                -1
            }
            if (nextIndex != -1) {
                val nextTrack = queue[nextIndex]
                playTrack(nextTrack, updateQueue = false)
                return
            }
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
        val queue = _activeQueue.value.filterNot { _deletedTrackIds.value.contains(it.id) }
        if (queue.isNotEmpty()) {
            val currentIndex = queue.indexOfFirst { it.id == _currentTrack.value.id }
            val prevIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else if (_isRepeat.value) {
                queue.lastIndex
            } else {
                0
            }
            val prevTrack = queue[prevIndex]
            playTrack(prevTrack, updateQueue = false)
            return
        }
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
                        if (isPlayerPrepared && _isPlaying.value) {
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
                delay(25L)
                if (_isPlaying.value) {
                    step++
                    val bpm = _currentTrack.value.bpm.coerceAtLeast(1)
                    val beatIntervalSteps = (60000 / bpm / 25).coerceAtLeast(6)
                    val isBeat = step % beatIntervalSteps == 0
                    val isSnare = step % (beatIntervalSteps * 2) == beatIntervalSteps
                    val visualizerActive = audioVisualizer != null && (liveRms > 0f || liveTransient > 0f)

                    val kick = if (visualizerActive) {
                        val k = liveKick
                        liveKick = false
                        k
                    } else isBeat

                    val snare = if (visualizerActive) {
                        val s = liveSnare
                        liveSnare = false
                        s
                    } else isSnare

                    val transient = if (visualizerActive) {
                        liveTransient
                    } else if (kick || snare) {
                        0.88f + (0.12f * kotlin.random.Random.nextFloat())
                    } else {
                        (_telemetry.value.transientSpike * 0.78f).coerceAtLeast(0f)
                    }

                    val sustained = if (visualizerActive) liveRms else 0.45f + (0.35f * sin(step * 0.04f))
                    val rms = if (visualizerActive) liveRms.coerceIn(0.05f, 1f) else (transient * 0.45f + sustained * 0.55f).coerceIn(0.1f, 1f)
                    _telemetry.value = AudioTelemetry(
                        transientSpike = transient,
                        sustainedEnergy = sustained,
                        rmsLevel = rms,
                        kickDetected = kick,
                        snareDetected = snare,
                        dominantFrequencyHz = if (visualizerActive) liveFrequencyHz else if (kick) 55f else 220f + (sin(step * 0.08f) * 110f),
                        pipelineLatencyMs = if (visualizerActive) 4L else (3L..6L).random(),
                        fftBars = liveFftBars.clone()
                    )
                } else {
                    // Freezing entirely when paused: retain the current fftBars strictly in place
                    _telemetry.value = _telemetry.value.copy(
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
