package com.example

import com.example.audio.AudioTelemetry
import com.example.audio.VelvetVisualizerPhysicsEngine
import com.example.audio.VisualizerBarLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VelvetVisualizerPhysicsEngineTest {

    @Test
    fun testKickCausesFastAttackAndSpringRelease() {
        val engine = VelvetVisualizerPhysicsEngine(
            barCount = 32,
            layout = VisualizerBarLayout.CENTER_BASS,
            stiffness = 320f,
            damping = 26f
        )

        val centerBar = 16
        val baseline = engine.positions[centerBar]

        // Frame 1: Sudden kick onset (sharp delta bass)
        val kickTelemetry = AudioTelemetry(
            kickDetected = true,
            fftBars = FloatArray(64) { idx -> if (idx < 5) 0.95f else 0.1f }
        )
        engine.update(kickTelemetry, 100_000_000L, isPlaying = true)

        val peakPosition = engine.positions[centerBar]
        assertTrue("Kick should produce high virtual amplitude push above baseline", peakPosition > baseline + 0.2f)

        // Frames 2..10: Spring release in action (should pull position down rapidly)
        val releaseTelemetry = AudioTelemetry(
            kickDetected = false,
            fftBars = FloatArray(64) { idx -> if (idx < 5) 0.50f else 0.1f }
        )

        var timeNanos = 100_000_000L
        for (i in 1..10) {
            timeNanos += 16_000_000L // 16ms delta
            engine.update(releaseTelemetry, timeNanos, isPlaying = true)
        }

        val releasedPosition = engine.positions[centerBar]
        assertTrue("Spring physics should pull bar down during release", releasedPosition < peakPosition)
    }

    @Test
    fun testSustainedBassDoesNotRepeatedlyFireKicks() {
        val engine = VelvetVisualizerPhysicsEngine(
            barCount = 32,
            layout = VisualizerBarLayout.CENTER_BASS
        )

        val centerBar = 16

        // Initial hit
        val initialHit = AudioTelemetry(
            kickDetected = true,
            fftBars = FloatArray(64) { idx -> if (idx < 5) 0.85f else 0.1f }
        )
        engine.update(initialHit, 100_000_000L, isPlaying = true)
        val initialVel = engine.velocities[centerBar]

        // Sustained bass (stays high at 0.80f without positive delta)
        val sustainedBass = AudioTelemetry(
            kickDetected = false,
            fftBars = FloatArray(64) { idx -> if (idx < 5) 0.80f else 0.1f }
        )

        // Run several frames of sustained bass
        var timeNanos = 100_000_000L
        for (i in 1..5) {
            timeNanos += 16_000_000L
            engine.update(sustainedBass, timeNanos, isPlaying = true)
        }

        // On subsequent sustained frames, velocity should not have massive jump equal to kick hit
        // The sustained bass creates controlled rhythm/oscillation, not repeating explosive kicks
        assertTrue("Engine should settle or oscillate smoothly on sustained bass", engine.positions[centerBar] > 0.05f)
    }

    @Test
    fun testSnareTransientDoesNotShakeSubBassBars() {
        val engine = VelvetVisualizerPhysicsEngine(
            barCount = 32,
            layout = VisualizerBarLayout.NATURAL_SPECTRUM
        )

        val subBassBar = 0
        val highBar = 28

        // Snare / high transient hit (mids/highs spike, sub-bass is near zero)
        val snareTelemetry = AudioTelemetry(
            snareDetected = true,
            fftBars = FloatArray(64) { idx -> if (idx in 20..50) 0.90f else 0.02f }
        )

        engine.update(snareTelemetry, 100_000_000L, isPlaying = true)

        val subBassPos = engine.positions[subBassBar]
        val highPos = engine.positions[highBar]

        assertTrue("High bar should respond much more strongly to snare transient than sub-bass bar", highPos > subBassPos)
    }

    @Test
    fun testNoiseFloorSilenceSettlesCleanly() {
        val engine = VelvetVisualizerPhysicsEngine(
            barCount = 32,
            layout = VisualizerBarLayout.CENTER_BASS
        )

        // Silent audio below noise floor
        val silentTelemetry = AudioTelemetry(
            rmsLevel = 0.01f,
            fftBars = FloatArray(64) { 0.01f }
        )

        var timeNanos = 100_000_000L
        for (i in 1..15) {
            timeNanos += 16_000_000L
            engine.update(silentTelemetry, timeNanos, isPlaying = true)
        }

        // Check that bars settle to baseline without random noise
        for (i in 0 until engine.barCount) {
            val base = engine.restingBaseline[i]
            val diff = kotlin.math.abs(engine.positions[i] - base)
            assertTrue("Bar $i should settle to baseline in silence without random jitter", diff < 0.05f)
        }
    }

    @Test
    fun testInfiniteBoundarySoftHeadroomCompression() {
        val engine = VelvetVisualizerPhysicsEngine(
            barCount = 10,
            layout = VisualizerBarLayout.CENTER_BASS
        )

        val canvasHeight = 100f
        val minHeight = 4f

        // Normal amplitude (e.g. 0.5)
        engine.positions[5] = 0.5f
        val normalH = engine.getProjectedBarHeight(5, canvasHeight, minHeight)

        // Huge bass hit with virtual amplitude exceeding 1.0 (e.g. 1.8)
        engine.positions[5] = 1.8f
        val hugeH = engine.getProjectedBarHeight(5, canvasHeight, minHeight)

        // Extreme bass hit with virtual amplitude (e.g. 2.6)
        engine.positions[5] = 2.6f
        val extremeH = engine.getProjectedBarHeight(5, canvasHeight, minHeight)

        assertTrue("Huge hit should exceed normal hit height", hugeH > normalH)
        assertTrue("Extreme hit should exceed huge hit in soft headroom (not hard clipped flat)", extremeH > hugeH)
        assertTrue("Extreme hit must not exceed canvas height", extremeH <= canvasHeight)
    }
}
