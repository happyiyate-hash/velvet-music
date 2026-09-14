package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.hypot

@Composable
fun AudioVisualizerScreen(
    audioUrl: String,
    barCount: Int = 32
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    if (!hasAudioPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant Audio Record Permission")
            }
        }
    } else {
        RealTimeAudioPlayerVisualizer(
            audioUrl = audioUrl,
            barCount = barCount
        )
    }
}

@Composable
fun RealTimeAudioPlayerVisualizer(
    audioUrl: String,
    barCount: Int = 32
) {
    val context = LocalContext.current

    // Store array of normalized bar amplitudes (0.0f to 1.0f)
    val fftMagnitudes = remember { mutableStateListOf(*Array(barCount) { 0f }) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            if (audioUrl.isNotBlank()) {
                setMediaItem(MediaItem.fromUri(audioUrl))
                prepare()
            }
        }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    // Attach native Android Visualizer to ExoPlayer's audio session
    DisposableEffect(exoPlayer) {
        var visualizer: Visualizer? = null

        fun attachVisualizer(sessionId: Int) {
            if (sessionId <= 0 || visualizer != null) return
            try {
                val range = Visualizer.getCaptureSizeRange()
                val targetCaptureSize = range[1].coerceAtMost(1024)
                visualizer = Visualizer(sessionId).apply {
                    captureSize = targetCaptureSize

                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(
                                visualizer: Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) {}

                            override fun onFftDataCapture(
                                visualizer: Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {
                                if (fft == null || fft.isEmpty()) return

                                val fftSize = fft.size / 2
                                if (fftSize < 4) return

                                val minFreqBin = 1.0
                                val maxFreqBin = (fftSize - 1).toDouble()

                                for (i in 0 until barCount) {
                                    val fracStart = i.toDouble() / barCount
                                    val fracEnd = (i + 1).toDouble() / barCount

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
                                    val db = 20.0 * kotlin.math.log10(avgMagnitude.coerceAtLeast(1.0))
                                    val norm = i.toDouble() / (barCount - 1).coerceAtLeast(1)
                                    val trebleTiltDb = norm * 14.0

                                    val minDb = 8.0
                                    val maxDb = 48.0
                                    val targetNormalized = (((db + trebleTiltDb) - minDb) / (maxDb - minDb)).coerceIn(0.05, 1.0).toFloat()

                                    val current = fftMagnitudes[i]
                                    val smoothed = if (targetNormalized > current) {
                                        current + (targetNormalized - current) * 0.60f
                                    } else {
                                        current - (current - targetNormalized) * 0.15f
                                    }
                                    fftMagnitudes[i] = smoothed.coerceIn(0.05f, 1.0f)
                                }
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        false, // Waveform
                        true   // FFT
                    )
                    enabled = isPlaying
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val playerListener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachVisualizer(audioSessionId)
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                visualizer?.enabled = playing
            }
        }

        exoPlayer.addListener(playerListener)

        val initialAudioSessionId = exoPlayer.audioSessionId
        if (initialAudioSessionId != 0) {
            attachVisualizer(initialAudioSessionId)
        }

        onDispose {
            exoPlayer.removeListener(playerListener)
            visualizer?.enabled = false
            visualizer?.release()
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Equalizer Canvas Bar Visualization
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val spacing = 6.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (canvasWidth - totalSpacing) / barCount

            fftMagnitudes.forEachIndexed { index, magnitude ->
                val barHeight = canvasHeight * magnitude
                val x = index * (barWidth + spacing)
                val y = canvasHeight - barHeight

                drawRoundRect(
                    color = Color(0xFF6200EE),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Play/Pause Controls
        Button(
            onClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }
        ) {
            Text(if (isPlaying) "Pause" else "Play")
        }
    }
}
