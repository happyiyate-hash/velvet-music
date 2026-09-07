package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var playbackRequestId = 0L

    private val _currentTrack = MutableStateFlow<Track>(SampleData.defaultIdleTrack)
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(94000L)
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
            val ctx = appContext
            _currentTrack.value = if (ctx != null) {
                val colors = if (!first.artworkUri.isNullOrBlank()) {
                    ArtworkColorExtractor.extractColorsFromUri(ctx, first.artworkUri)
                } else {
                    ArtworkColorExtractor.extractColors(ctx, first)
                }
                first.copy(dominantColor = colors.dominant, secondaryColor = colors.secondary)
            } else first
            _isPlaying.value = false
            _playbackPositionMs.value = 0L
            updateMediaSession()
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
        }
    }

    /** Plays the exact device URI supplied by DeviceMediaManager. No synthesized/fallback audio is generated. */
    fun playTrack(track: Track) {
        val ctx = appContext ?: return
        val requestId = ++playbackRequestId
        val themedTrack = try {
            val colors = if (!track.artworkUri.isNullOrBlank()) {
                ArtworkColorExtractor.extractColorsFromUri(ctx, track.artworkUri)
            } else {
                ArtworkColorExtractor.extractColors(ctx, track)
            }
            track.copy(dominantColor = colors.dominant, secondaryColor = colors.secondary)
        } catch (_: Exception) {
            track
        }

        _currentTrack.value = themedTrack
        _playbackPositionMs.value = 0L
        val counts = _trackPlayCounts.value.toMutableMap()
        counts[themedTrack.id] = (counts[themedTrack.id] ?: 0) + 1
        _trackPlayCounts.value = counts

        releasePlayerOnly()
        playbackJob?.cancel()

        if (themedTrack.contentUri.isNullOrBlank()) {
            Log.w("VelvetAudioEngine", "Cannot play track with empty contentUri: ${themedTrack.title}")
            _isPlaying.value = false
            updateMediaSession()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                val trackUri = Uri.parse(themedTrack.contentUri)
                try {
                    val pfd = ctx.contentResolver.openFileDescriptor(trackUri, "r")
                    if (pfd != null) {
                        player.setDataSource(pfd.fileDescriptor)
                        pfd.close()
                    } else {
                        player.setDataSource(ctx, trackUri)
                    }
                } catch (_: Exception) {
                    player.setDataSource(ctx, trackUri)
                }

                player.setOnPreparedListener { prepared ->
                    if (requestId != playbackRequestId) {
                        prepared.release()
                        return@setOnPreparedListener
                    }
                    mediaPlayer = prepared
                    _isPlaying.value = true
                    _playbackPositionMs.value = 0L
                    prepared.start()
                    startPlaybackProgress()
                    updateMediaSession()
                }
                player.setOnCompletionListener {
                    if (requestId == playbackRequestId) {
                        _isPlaying.value = false
                        _playbackPositionMs.value = themedTrack.durationMs
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
                    if (requestId == playbackRequestId) {
                        _isPlaying.value = false
                        playbackJob?.cancel()
                        updateMediaSession()
                    }
                    true
                }
                player.prepareAsync()
            } catch (e: Exception) {
                Log.e("VelvetAudioEngine", "Error initializing MediaPlayer for URI: ${themedTrack.contentUri}", e)
                if (requestId == playbackRequestId) {
                    _isPlaying.value = false
                    playbackJob?.cancel()
                    updateMediaSession()
                }
            }
        }
    }

    private fun preparedRestart() {
        try {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
            _playbackPositionMs.value = 0L
            _isPlaying.value = true
            startPlaybackProgress()
            updateMediaSession()
        } catch (_: Exception) {
            _isPlaying.value = false
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
        try { mediaPlayer?.pause() } catch (_: Exception) {}
        _isPlaying.value = false
        playbackJob?.cancel()
        updateMediaSession()
    }

    fun resume() {
        val player = mediaPlayer
        if (player != null) {
            try {
                player.start()
                _isPlaying.value = true
                startPlaybackProgress()
                updateMediaSession()
            } catch (_: Exception) {
                _isPlaying.value = false
            }
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
        val target = positionMs.coerceIn(0L, _currentTrack.value.durationMs)
        try { mediaPlayer?.seekTo(target.toInt()) } catch (_: Exception) {}
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
        if (all.isEmpty()) return
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val nextIndex = if (currentIndex != -1 && currentIndex < all.lastIndex) currentIndex + 1 else 0
        playTrack(all[nextIndex])
    }

    fun getAllAvailableTracks(): List<Track> {
        return _deviceTracks.value
            .distinctBy { it.id }
            .filterNot { _deletedTrackIds.value.contains(it.id) }
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
                if (player != null && player.isPlaying) {
                    try {
                        val position = player.currentPosition.toLong()
                        _playbackPositionMs.value = position
                        updateMediaSession()
                    } catch (_: Exception) {
                        break
                    }
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
                    val transient = if (isBeat || isSnare) 0.85f + (0.15f * kotlin.random.Random.nextFloat()) else (_telemetry.value.transientSpike * 0.72f).coerceAtLeast(0f)
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

    private fun releasePlayerOnly() {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        playbackRequestId++
        playbackJob?.cancel()
        releasePlayerOnly()
        _isPlaying.value = false
    }
}
