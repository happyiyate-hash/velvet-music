package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
    val transientSpike: Float = 0f,    // 0..1, kicks & snares (Fast Snap)
    val sustainedEnergy: Float = 0.4f,  // 0..1, vocals & synths (Smooth Drift)
    val rmsLevel: Float = 0.3f,         // 0..1 overall energy
    val kickDetected: Boolean = false,
    val snareDetected: Boolean = false,
    val dominantFrequencyHz: Float = 110f,
    val pipelineLatencyMs: Long = 4L
)

class VelvetAudioEngine(
    private val scope: CoroutineScope
) {
    private val _currentTrack = MutableStateFlow<Track>(SampleData.trackAfterHours)
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

    private val _offlineCachedTrackIds = MutableStateFlow(setOf("track_1", "track_2"))
    val offlineCachedTrackIds: StateFlow<Set<String>> = _offlineCachedTrackIds.asStateFlow()

    private var playbackJob: Job? = null
    private var synthJob: Job? = null
    private var nativeAudioTrack: AudioTrack? = null

    init {
        startTelemetryLoop()
    }

    fun playTrack(track: Track) {
        _currentTrack.value = track
        _playbackPositionMs.value = 0L
        _isPlaying.value = true
        startPlaybackProgress()
        startAudioSynthesis()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        _isPlaying.value = false
        stopAudioSynthesis()
    }

    fun resume() {
        _isPlaying.value = true
        startPlaybackProgress()
        startAudioSynthesis()
    }

    fun seekTo(positionMs: Long) {
        val duration = _currentTrack.value.durationMs
        _playbackPositionMs.value = positionMs.coerceIn(0L, duration)
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
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        _offlineCachedTrackIds.value = current
    }

    fun playNext() {
        val all = SampleData.recentlyPlayedTracks + SampleData.newReleases
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val nextIndex = if (currentIndex != -1 && currentIndex < all.lastIndex) currentIndex + 1 else 0
        playTrack(all[nextIndex])
    }

    fun playPrevious() {
        val all = SampleData.recentlyPlayedTracks + SampleData.newReleases
        val currentIndex = all.indexOfFirst { it.id == _currentTrack.value.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else all.lastIndex
        playTrack(all[prevIndex])
    }

    private fun startPlaybackProgress() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isPlaying.value) {
                delay(100L)
                val current = _playbackPositionMs.value + 100L
                val max = _currentTrack.value.durationMs
                if (current >= max) {
                    if (_isRepeat.value) {
                        _playbackPositionMs.value = 0L
                    } else {
                        playNext()
                    }
                } else {
                    _playbackPositionMs.value = current
                }
            }
        }
    }

    private fun startTelemetryLoop() {
        scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive) {
                delay(40L) // 25 fps telemetry update
                if (_isPlaying.value) {
                    step++
                    val bpm = _currentTrack.value.bpm
                    val beatIntervalSteps = (60000 / bpm / 40).coerceAtLeast(6)
                    val isBeat = (step % beatIntervalSteps) == 0
                    val isSnare = (step % (beatIntervalSteps * 2)) == beatIntervalSteps

                    // Fast Snap transient spike (Kick or Snare)
                    val transient = if (isBeat || isSnare) 0.85f + (0.15f * kotlin.random.Random.nextFloat()) else {
                        (_telemetry.value.transientSpike * 0.72f).coerceAtLeast(0f)
                    }

                    // Smooth Drift sustained energy (smooth sine modulation)
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

    private fun startAudioSynthesis() {
        stopAudioSynthesis()
        synthJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                nativeAudioTrack = track
                track.play()

                val buffer = ShortArray(1024)
                var phaseDrone = 0.0
                var phaseHarmonic = 0.0
                var beatCounter = 0

                while (isActive && _isPlaying.value) {
                    val bpm = _currentTrack.value.bpm
                    val samplesPerBeat = (sampleRate * 60) / bpm

                    for (i in buffer.indices) {
                        beatCounter++
                        if (beatCounter >= samplesPerBeat) {
                            beatCounter = 0
                        }

                        // Warm dark ambient drone (55Hz / 110Hz warm sub chord)
                        val droneFreq = 55.0
                        val harmonicFreq = 110.0
                        phaseDrone += 2.0 * PI * droneFreq / sampleRate
                        phaseHarmonic += 2.0 * PI * harmonicFreq / sampleRate

                        var sample = (sin(phaseDrone) * 0.25 + sin(phaseHarmonic) * 0.12)

                        // Subtle warm kick pulse on downbeats
                        val beatFraction = beatCounter.toDouble() / samplesPerBeat
                        if (beatFraction < 0.1) {
                            val kickEnvelope = (1.0 - (beatFraction / 0.1))
                            sample += sin(phaseDrone * 1.5) * 0.35 * kickEnvelope
                        }

                        buffer[i] = (sample * 8000.0).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
                // Audio synthesis fallback for devices without audio access
            }
        }
    }

    private fun stopAudioSynthesis() {
        synthJob?.cancel()
        synthJob = null
        try {
            nativeAudioTrack?.stop()
            nativeAudioTrack?.release()
        } catch (_: Exception) {}
        nativeAudioTrack = null
    }

    fun release() {
        playbackJob?.cancel()
        stopAudioSynthesis()
    }
}
