package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
import kotlin.math.PI
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

class VelvetAudioEngine(
    private val scope: CoroutineScope
) {
    private var appContext: Context? = null

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

    private var playbackJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    init {
        startTelemetryLoop()
    }

    fun bindMediaSession(context: Context) {
        this.appContext = context.applicationContext
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
        if ((_currentTrack.value.id == "idle_device_track" || _currentTrack.value.id == "track_1") && tracks.isNotEmpty()) {
            val ctx = appContext
            val first = tracks.first()
            val themedFirst = if (ctx != null) {
                val colors = ArtworkColorExtractor.extractColors(ctx, first)
                first.copy(dominantColor = colors.dominant, secondaryColor = colors.secondary)
            } else {
                first
            }
            _currentTrack.value = themedFirst
            updateMediaSession()
        }
    }

    fun addDeviceTrack(track: Track) {
        val updated = listOf(track) + _deviceTracks.value.filter { it.id != track.id }
        _deviceTracks.value = updated
        if (_currentTrack.value.id == "idle_device_track") {
            _currentTrack.value = track
            updateMediaSession()
        }
    }

    fun playTrack(track: Track) {
        val ctx = appContext
        val themedTrack = if (ctx != null) {
            val colors = ArtworkColorExtractor.extractColors(ctx, track)
            track.copy(dominantColor = colors.dominant, secondaryColor = colors.secondary)
        } else {
            track
        }

        _currentTrack.value = themedTrack
        _playbackPositionMs.value = 0L
        _isPlaying.value = false
        isPrepared = false

        val counts = _trackPlayCounts.value.toMutableMap()
        counts[themedTrack.id] = (counts[themedTrack.id] ?: 0) + 1
        _trackPlayCounts.value = counts

        playbackJob?.cancel()
        releaseMediaPlayer()

        val contentUri = themedTrack.contentUri
        if (ctx == null || contentUri.isNullOrBlank()) {
            // Only real device/imported tracks can be played by this local player.
            updateMediaSession()
            return
        }

        val player = MediaPlayer()
        mediaPlayer = player

        try {
            player.setOnPreparedListener { preparedPlayer ->
                if (mediaPlayer !== preparedPlayer) return@setOnPreparedListener
                isPrepared = true
                _isPlaying.value = true
                preparedPlayer.start()
                startPlaybackProgress()
                updateMediaSession()
            }

            player.setOnCompletionListener {
                if (mediaPlayer !== player) return@setOnCompletionListener
                isPrepared = false
                _isPlaying.value = false
                _playbackPositionMs.value = 0L
                updateMediaSession()

                if (_isRepeat.value) {
                    playTrack(_currentTrack.value)
                } else {
                    playNext()
                }
            }

            player.setOnErrorListener { _, _, _ ->
                if (mediaPlayer === player) {
                    isPrepared = false
                    _isPlaying.value = false
                    updateMediaSession()
                }
                true
            }

            // This is the actual MediaStore/content-provider URI of the user's audio file.
            player.setDataSource(ctx, Uri.parse(contentUri))
            player.prepareAsync()
        } catch (_: Exception) {
            releaseMediaPlayer()
            _isPlaying.value = false
            updateMediaSession()
        }
    }

    fun queueNext(track: Track) {
        _nextQueueTrack.value = track
    }

    fun toggleFavorite(trackId: String) {
        val current = _favoriteTrackIds.value.toMutableSet()
        if (current.contains(trackId)) current.remove(trackId) else current.add(trackId)
        _favoriteTrackIds.value = current
    }

    fun deleteTrack(trackId: String) {
        _deletedTrackIds.value = _deletedTrackIds.value + trackId
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun pause() {
        if (isPrepared) {
            try {
                mediaPlayer?.pause()
            } catch (_: Exception) {
            }
        }
        _isPlaying.value = false
        playbackJob?.cancel()
        updateMediaSession()
    }

    fun resume() {
        val player = mediaPlayer
        if (isPrepared && player != null) {
            try {
                player.start()
                _isPlaying.value = true
                startPlaybackProgress()
                updateMediaSession()
            } catch (_: Exception) {
                _isPlaying.value = false
                updateMediaSession()
            }
        } else {
            // Re-open the actual selected device URI if the player was released.
            val track = _currentTrack.value
            if (!track.contentUri.isNullOrBlank()) {
                playTrack(track)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _currentTrack.value.durationMs
        val target = positionMs.coerceIn(0L, duration)
        _playbackPositionMs.value = target
        if (isPrepared) {
            try {
                mediaPlayer?.seekTo(target.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            } catch (_: Exception) {
            }
        }
        updateMediaSession()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun toggleSoundCatch() {
        _isSoundCatchEnabled.value = !_isSoundCatchEnabled.value
    }

    fun toggleOfflineCache(trackId: String) {
        val current = _offlineCachedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) current.remove(trackId) else current.add(trackId)
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
        if (all.isEmpty()) return
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val nextIndex = if (currentIndex != -1 && currentIndex < all.lastIndex) currentIndex + 1 else 0
        playTrack(all[nextIndex])
    }

    fun getAllAvailableTracks(): List<Track> {
        val deleted = _deletedTrackIds.value
        return _deviceTracks.value
            .distinctBy { it.id }
            .filterNot { deleted.contains(it.id) }
    }

    fun playPrevious() {
        val all = getAllAvailableTracks()
        if (all.isEmpty()) return
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else all.lastIndex
        playTrack(all[prevIndex])
    }

    private fun startPlaybackProgress() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isPlaying.value) {
                delay(200L)
                val player = mediaPlayer
                if (player == null || !isPrepared) continue

                try {
                    val position = player.currentPosition.toLong()
                    _playbackPositionMs.value = position.coerceIn(0L, _currentTrack.value.durationMs)
                } catch (_: Exception) {
                    break
                }

                updateMediaSession()
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
                    val bpm = _currentTrack.value.bpm
                    val beatIntervalSteps = (60000 / bpm / 40).coerceAtLeast(6)
                    val isBeat = (step % beatIntervalSteps) == 0
                    val isSnare = (step % (beatIntervalSteps * 2)) == beatIntervalSteps
                    val transient = if (isBeat || isSnare) {
                        0.85f + (0.15f * kotlin.random.Random.nextFloat())
                    } else {
                        (_telemetry.value.transientSpike * 0.72f).coerceAtLeast(0f)
                    }
                    val sustained = 0.45f + (0.35f * sin(step * 0.06f))
                    val rms = (transient * 0.4f + sustained * 0.6f).coerceIn(0.1f, 1f)

                    _telemetry.value = AudioTelemetry(
                        transientSpike = transient,
                        sustainedEnergy = sustained,
                        rmsLevel = rms,
                        kickDetected = isBeat,
                        snareDetected = isSnare,
                        dominantFrequencyHz = if (isBeat) 55f else 220f + (sin(step * 0.1f) * 110f),
                        pipelineLatencyMs = (3L..6L).random()
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

    private fun releaseMediaPlayer() {
        playbackJob?.cancel()
        playbackJob = null
        isPrepared = false
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    fun release() {
        releaseMediaPlayer()
    }
}
