from pathlib import Path

path = Path("app/src/main/java/com/example/audio/VelvetAudioEngine.kt")
s = path.read_text(encoding="utf-8")

# Imports
if "import android.media.audiofx.Visualizer" not in s:
    s = s.replace("import android.media.MediaPlayer\n", "import android.media.MediaPlayer\nimport android.media.audiofx.Visualizer\n")

# Field
field_anchor = "    private var mediaPlayer: MediaPlayer? = null\n"
field_insert = field_anchor + "    private var audioVisualizer: Visualizer? = null\n    @Volatile private var liveRms = 0f\n    @Volatile private var liveTransient = 0f\n    @Volatile private var liveFrequencyHz = 110f\n    @Volatile private var liveKick = false\n    @Volatile private var liveSnare = false\n"
if "private var audioVisualizer: Visualizer?" not in s:
    if field_anchor not in s:
        raise SystemExit("mediaPlayer field not found")
    s = s.replace(field_anchor, field_insert, 1)

# Stop old visualizer whenever the player is reset.
reset_anchor = """                synchronized(playerLock) {\n                    try {\n                        isPlayerPrepared = false\n\n                        // Get or initialize persistent player\n"""
reset_replacement = """                synchronized(playerLock) {\n                    try {\n                        isPlayerPrepared = false\n                        releaseAudioVisualizer()\n\n                        // Get or initialize persistent player\n"""
if reset_anchor in s and "releaseAudioVisualizer()\n\n                        // Get or initialize persistent player" not in s:
    s = s.replace(reset_anchor, reset_replacement, 1)

# Attach after successful MediaPlayer start.
prepared_anchor = """                                    prepared.start()\n                                    _isPlaying.value = true\n                                    _playbackPositionMs.value = 0L\n"""
prepared_replacement = """                                    prepared.start()\n                                    _isPlaying.value = true\n                                    _playbackPositionMs.value = 0L\n                                    attachAudioVisualizer(prepared.audioSessionId)\n"""
if prepared_anchor not in s:
    raise SystemExit("prepared start block not found")
s = s.replace(prepared_anchor, prepared_replacement, 1)

# Add helper before color extraction.
helper_anchor = "    private fun extractColorsLazily(track: Track) {\n"
helper = r'''    private fun attachAudioVisualizer(audioSessionId: Int) {
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

'''
if "private fun attachAudioVisualizer(" not in s:
    if helper_anchor not in s:
        raise SystemExit("color extraction anchor not found")
    s = s.replace(helper_anchor, helper + helper_anchor, 1)

# Use native telemetry when Visualizer is active; preserve existing synthetic fallback for demo tracks/devices where Visualizer is unavailable.
old = """                    val transient = if (isBeat || isSnare) 0.85f + (0.15f * kotlin.random.Random.nextFloat()) else (_telemetry.value.transientSpike * 0.72f).coerceAtLeast(0f)\n                    val sustained = 0.45f + (0.35f * sin(step * 0.06f))\n                    val rms = (transient * 0.4f + sustained * 0.6f).coerceIn(0.1f, 1f)\n                    _telemetry.value = AudioTelemetry(\n                        transientSpike = transient,\n                        sustainedEnergy = sustained,\n                        rmsLevel = rms,\n                        kickDetected = isBeat,\n                        snareDetected = isSnare,\n                        dominantFrequencyHz = if (isBeat) 55f else 220f + (sin(step * 0.1f) * 110f),\n                        pipelineLatencyMs = (3L..6L).random()\n                    )\n"""
new = """                    val visualizerActive = audioVisualizer != null && (liveRms > 0f || liveTransient > 0f)\n                    val transient = if (visualizerActive) liveTransient else if (isBeat || isSnare) 0.85f + (0.15f * kotlin.random.Random.nextFloat()) else (_telemetry.value.transientSpike * 0.72f).coerceAtLeast(0f)\n                    val sustained = if (visualizerActive) liveRms else 0.45f + (0.35f * sin(step * 0.06f))\n                    val rms = if (visualizerActive) liveRms.coerceIn(0.05f, 1f) else (transient * 0.4f + sustained * 0.6f).coerceIn(0.1f, 1f)\n                    _telemetry.value = AudioTelemetry(\n                        transientSpike = transient,\n                        sustainedEnergy = sustained,\n                        rmsLevel = rms,\n                        kickDetected = if (visualizerActive) liveKick else isBeat,\n                        snareDetected = if (visualizerActive) liveSnare else isSnare,\n                        dominantFrequencyHz = if (visualizerActive) liveFrequencyHz else if (isBeat) 55f else 220f + (sin(step * 0.1f) * 110f),\n                        pipelineLatencyMs = if (visualizerActive) 4L else (3L..6L).random()\n                    )\n"""
if old not in s:
    raise SystemExit("telemetry block not found")
s = s.replace(old, new, 1)

# Release native visualizer with the player.
release_old = """        synchronized(playerLock) {\n            isPlayerPrepared = false\n            try { mediaPlayer?.stop() } catch (_: Exception) {}\n"""
release_new = """        synchronized(playerLock) {\n            isPlayerPrepared = false\n            releaseAudioVisualizer()\n            try { mediaPlayer?.stop() } catch (_: Exception) {}\n"""
if release_old not in s:
    raise SystemExit("release block not found")
s = s.replace(release_old, release_new, 1)

path.write_text(s, encoding="utf-8")
