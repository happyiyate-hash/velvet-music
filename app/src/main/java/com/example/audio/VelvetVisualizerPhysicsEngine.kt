package com.example.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Velvet Studio Audio Feature Extraction & Physics Animation Engine:
 *
 * Implements the decoupled audio-to-motion architecture:
 *
 *              AUDIO
 *                │
 *        ┌───────┼───────┐
 *        ↓       ↓       ↓
 *      BASS   MID/HIGH NOISE
 *        │       │       │
 *     ┌──┴──┐    │       │
 *     ↓     ↓    ↓       ↓
 *   KICK SUSTAIN TRANSIENT IGNORE
 *     │     │    │
 *     └──┬──┴────┘
 *        ↓
 *   ┌─────────────────┐
 *   │ Animation       │
 *   │ Physics         │
 *   │ • impulse       │
 *   │ • spring        │
 *   │ • damping       │
 *   │ • release       │
 *   │ • virtual       │
 *   │   amplitude     │
 *   └────────┬────────┘
 *            ↓
 *       VISUALIZER
 *
 * Key Principles:
 * 1. KICK / BASS HIT: Fast attack impulse (0 -> 100 instantly) followed by physical spring return
 *    (100 -> 80 -> 55 -> 30 -> 10 -> 0). Only fires on sudden bass onset, NEVER repeatedly
 *    just because bass remains present.
 * 2. SUSTAINED BASS: Generates a controlled, smooth rhythmic oscillation (30 -> 35 -> 40 -> 38 -> 42 -> 40)
 *    without repeatedly firing giant kick animations.
 * 3. TRANSIENT / SNARE / HIGHS: Localized spike applied specifically to mid/high frequency bars
 *    without shaking or blowing out the entire visualizer bass response.
 * 4. NOISE FLOOR & SILENCE: Sounds below the noise floor or silence cause zero arbitrary vibration.
 *    No random noise jitter (bar += random() is forbidden). Settles cleanly toward resting baseline.
 * 5. INFINITE BOUNDARY (VIRTUAL AMPLITUDE): Internal amplitude is unbounded (can reach 1.5, 2.0, 3.0+),
 *    preventing clipping against a hard ceiling. The renderer compresses the virtual amplitude
 *    into visible headroom smoothly, while the spring strongly pulls it down on the next frame.
 * 6. FREQUENCY DISTRIBUTION: Center-weighted bass dispersion (e.g. ▂ ▃ ▅ ▇ █ █ ▇ ▅ ▃ ▂) with
 *    localized mid/treble response, eliminating solid uniform blocks.
 */

enum class VisualizerBarLayout {
    CENTER_BASS,    // Sub/punch bass concentrated toward the center, highs toward edges
    NATURAL_SPECTRUM, // Low bass on left, mids center, treble on right
    SCATTERED_NEEDLES // Interleaved scattered frequency bins across the full width
}

data class AudioFeatures(
    val kickImpulse: Float = 0f,
    val isKickHit: Boolean = false,
    val sustainedBass: Float = 0f,
    val snareImpulse: Float = 0f,
    val isSnareHit: Boolean = false,
    val midEnergy: Float = 0f,
    val trebleEnergy: Float = 0f,
    val overallRms: Float = 0f,
    val isSilent: Boolean = false
)

class VelvetAudioFeatureDetector {
    private var prevBass = 0f
    private var prevMids = 0f
    private var prevTreble = 0f
    private var lastKickTimeNanos = 0L
    private var lastSnareTimeNanos = 0L

    // Noise floor threshold below which sounds are treated as silence
    var noiseFloor: Float = 0.045f

    fun analyze(
        telemetry: AudioTelemetry,
        currentTimeNanos: Long
    ): AudioFeatures {
        val fft = telemetry.fftBars
        val hasFft = fft.isNotEmpty()

        val subBass: Float
        val punchBass: Float
        val mids: Float
        val treble: Float
        val overallRms: Float

        if (hasFft) {
            var sb = 0f
            for (i in 0..min(5, fft.lastIndex)) {
                if (fft[i] > sb) sb = fft[i]
            }
            subBass = sb

            var pbSum = 0f
            val pbEnd = min(13, fft.lastIndex)
            val pbCount = (pbEnd - 6 + 1).coerceAtLeast(1)
            for (i in 6..pbEnd) pbSum += fft[i]
            punchBass = pbSum / pbCount

            var midSum = 0f
            val midEnd = min(36, fft.lastIndex)
            val midCount = (midEnd - 14 + 1).coerceAtLeast(1)
            for (i in 14..midEnd) midSum += fft[i]
            mids = midSum / midCount

            var trSum = 0f
            val trEnd = min(63, fft.lastIndex)
            val trCount = (trEnd - 37 + 1).coerceAtLeast(1)
            for (i in 37..trEnd) trSum += fft[i]
            treble = trSum / trCount

            overallRms = (subBass * 0.35f + punchBass * 0.25f + mids * 0.25f + treble * 0.15f)
        } else {
            val rms = telemetry.rmsLevel.coerceIn(0f, 1f)
            val transient = telemetry.transientSpike.coerceIn(0f, 1f)
            subBass = if (telemetry.kickDetected) 0.95f else rms * 0.65f
            punchBass = if (telemetry.kickDetected) 0.82f else rms * 0.55f
            mids = rms * 0.50f
            treble = if (telemetry.snareDetected) 0.88f else transient * 0.60f
            overallRms = rms
        }

        // 1. Noise Floor Gate: if audio is silent or near zero, ignore and settle cleanly
        if (overallRms < noiseFloor && !telemetry.kickDetected && !telemetry.snareDetected) {
            prevBass *= 0.85f
            prevMids *= 0.85f
            prevTreble *= 0.85f
            return AudioFeatures(
                overallRms = overallRms,
                isSilent = true
            )
        }

        // 2. Separate Kick / Bass Hit (Transient Impulse) from Sustained Bass
        val currentBass = max(subBass, punchBass * 0.90f)
        val deltaBass = currentBass - prevBass
        val dtKickSec = if (lastKickTimeNanos == 0L) 1.0f else (currentTimeNanos - lastKickTimeNanos) / 1_000_000_000f

        val isKickOnset = (deltaBass > 0.14f && currentBass > 0.28f && dtKickSec > 0.09f) ||
                (telemetry.kickDetected && dtKickSec > 0.10f)

        val kickImpulse = if (isKickOnset) {
            lastKickTimeNanos = currentTimeNanos
            // Virtual kick impulse: can exceed 1.0 into 1.5 - 2.5 on massive hits!
            val rawImpulse = (deltaBass * 2.4f + currentBass * 0.95f).coerceIn(0.5f, 2.8f)
            rawImpulse
        } else {
            0f
        }

        // Update previous bass with controlled decay
        prevBass = if (currentBass > prevBass) {
            currentBass
        } else {
            prevBass - (prevBass - currentBass) * 0.35f
        }

        // 3. Sustained Bass: smooth continuous presence envelope
        val sustainedBass = if (currentBass > 0.12f) {
            currentBass.coerceIn(0f, 1f)
        } else 0f

        // 4. Mid/High Transient (Snare, Clap, Hi-hat Hit)
        val deltaMids = mids - prevMids
        val deltaTreble = treble - prevTreble
        val dtSnareSec = if (lastSnareTimeNanos == 0L) 1.0f else (currentTimeNanos - lastSnareTimeNanos) / 1_000_000_000f

        val isSnareOnset = ((deltaMids > 0.16f || deltaTreble > 0.18f) && dtSnareSec > 0.08f) ||
                (telemetry.snareDetected && dtSnareSec > 0.09f)

        val snareImpulse = if (isSnareOnset) {
            lastSnareTimeNanos = currentTimeNanos
            (max(deltaMids, deltaTreble) * 2.2f + mids * 0.70f).coerceIn(0.4f, 2.0f)
        } else {
            0f
        }

        prevMids = if (mids > prevMids) mids else prevMids - (prevMids - mids) * 0.40f
        prevTreble = if (treble > prevTreble) treble else prevTreble - (prevTreble - treble) * 0.40f

        return AudioFeatures(
            kickImpulse = kickImpulse,
            isKickHit = isKickOnset,
            sustainedBass = sustainedBass,
            snareImpulse = snareImpulse,
            isSnareHit = isSnareOnset,
            midEnergy = mids,
            trebleEnergy = treble,
            overallRms = overallRms,
            isSilent = false
        )
    }

    fun reset() {
        prevBass = 0f
        prevMids = 0f
        prevTreble = 0f
        lastKickTimeNanos = 0L
        lastSnareTimeNanos = 0L
    }
}

/**
 * Studio Spring Physics Engine for Velvet Visualizer.
 *
 * Implements real physical second-order spring-mass-damper dynamics:
 *   force = -stiffness * (position - target) - damping * velocity
 *
 * Supports unbounded virtual amplitude (infinite boundary) with soft-knee projection.
 */
class VelvetVisualizerPhysicsEngine(
    val barCount: Int,
    val layout: VisualizerBarLayout = VisualizerBarLayout.CENTER_BASS,
    // Physics constants tuned for immediate punchy attack and natural spring release
    var stiffness: Float = 320f,
    var damping: Float = 26f,
    var kickVelocityGain: Float = 14.5f,
    var snareVelocityGain: Float = 11.0f
) {
    val positions = FloatArray(barCount) { 0.06f }
    val velocities = FloatArray(barCount) { 0f }
    val targets = FloatArray(barCount) { 0.06f }
    val restingBaseline = FloatArray(barCount)

    // Per-bar frequency coupling weights
    private val kickWeights = FloatArray(barCount)
    private val sustainedBassWeights = FloatArray(barCount)
    private val snareWeights = FloatArray(barCount)
    private val midWeights = FloatArray(barCount)
    private val trebleWeights = FloatArray(barCount)
    private val fftBinIndices = IntArray(barCount)

    private val detector = VelvetAudioFeatureDetector()
    private var lastUpdateNanos = 0L
    private var sustainedEnvelope = 0f

    init {
        configureBarWeights()
    }

    private fun configureBarWeights() {
        for (i in 0 until barCount) {
            val norm = if (barCount > 1) i.toFloat() / (barCount - 1) else 0.5f

            // Baseline calm profile when paused or idle
            val wave1 = abs(sin(norm * 3.14159f * 1.5f + 0.35f)) * 0.035f
            val wave2 = abs(sin(norm * 3.14159f * 3.8f)) * 0.025f
            restingBaseline[i] = (0.05f + wave1 + wave2).coerceIn(0.04f, 0.10f)
            positions[i] = restingBaseline[i]
            targets[i] = restingBaseline[i]

            when (layout) {
                VisualizerBarLayout.CENTER_BASS -> {
                    // Center-dominant bass distribution: kicks form a majestic mountain in the center:
                    // ▂ ▃ ▅ ▇ █ █ ▇ ▅ ▃ ▂
                    val distFromCenter = abs(norm - 0.5f)
                    val centerBassFactor = exp(-(distFromCenter * distFromCenter) / (2f * 0.22f * 0.22f))
                    kickWeights[i] = (centerBassFactor * 1.15f).coerceIn(0.18f, 1.25f)
                    sustainedBassWeights[i] = (centerBassFactor * 0.85f + 0.15f).coerceIn(0.12f, 1.0f)

                    // Snares and high transients respond strongly near intermediate / outer regions
                    val snareDist = abs(norm - 0.5f)
                    val snareFactor = (1f - exp(-(snareDist * snareDist) / (2f * 0.25f * 0.25f)))
                    snareWeights[i] = (snareFactor * 1.2f).coerceIn(0.10f, 1.20f)
                    midWeights[i] = (sin(norm * 3.14159f) * 0.70f + 0.30f).coerceIn(0.2f, 1f)
                    trebleWeights[i] = (norm * 0.85f + (1f - norm) * 0.15f).coerceIn(0.1f, 1f)
                    fftBinIndices[i] = ((1f - centerBassFactor) * 55f).toInt().coerceIn(0, 63)
                }

                VisualizerBarLayout.NATURAL_SPECTRUM -> {
                    // Traditional acoustic frequency analyzer: Sub-bass on left, treble on right
                    kickWeights[i] = exp(-(norm * norm) / (2f * 0.28f * 0.28f)).coerceIn(0.08f, 1.2f)
                    sustainedBassWeights[i] = exp(-(norm * norm) / (2f * 0.32f * 0.32f)).coerceIn(0.05f, 1.0f)
                    val midCenter = abs(norm - 0.45f)
                    midWeights[i] = exp(-(midCenter * midCenter) / (2f * 0.20f * 0.20f)).coerceIn(0.1f, 1f)
                    snareWeights[i] = (norm * 1.1f).coerceIn(0.05f, 1.2f)
                    trebleWeights[i] = (norm * 1.2f).coerceIn(0.05f, 1.2f)
                    fftBinIndices[i] = (norm * 63f).toInt().coerceIn(0, 63)
                }

                VisualizerBarLayout.SCATTERED_NEEDLES -> {
                    // Fully scattered independent needles across full width
                    val band = (i * 3 + (i / 4)) % 4
                    when (band) {
                        0 -> { // Sub-bass needle
                            kickWeights[i] = 1.20f
                            sustainedBassWeights[i] = 1.0f
                            snareWeights[i] = 0.08f
                            midWeights[i] = 0.20f
                            trebleWeights[i] = 0.05f
                            fftBinIndices[i] = (i * 7) % 5
                        }
                        2 -> { // Punch-bass needle
                            kickWeights[i] = 0.95f
                            sustainedBassWeights[i] = 0.75f
                            snareWeights[i] = 0.15f
                            midWeights[i] = 0.35f
                            trebleWeights[i] = 0.10f
                            fftBinIndices[i] = 5 + ((i * 7) % 7)
                        }
                        1 -> { // Vocal / Mid articulation needle
                            kickWeights[i] = 0.25f
                            sustainedBassWeights[i] = 0.20f
                            snareWeights[i] = 0.65f
                            midWeights[i] = 1.0f
                            trebleWeights[i] = 0.30f
                            fftBinIndices[i] = 13 + ((i * 11) % 23)
                        }
                        else -> { // Treble / Hi-hat / Air needle
                            kickWeights[i] = 0.08f
                            sustainedBassWeights[i] = 0.05f
                            snareWeights[i] = 1.25f
                            midWeights[i] = 0.25f
                            trebleWeights[i] = 1.20f
                            fftBinIndices[i] = 37 + ((i * 13) % 26)
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates physics with new audio telemetry and timestamp.
     * Integrates spring physics over elapsed delta time dt.
     */
    fun update(
        telemetry: AudioTelemetry,
        currentTimeNanos: Long,
        isPlaying: Boolean
    ) {
        if (!isPlaying) {
            // When paused: freeze or settle smoothly without random vibration
            lastUpdateNanos = currentTimeNanos
            return
        }

        val dt = if (lastUpdateNanos == 0L) {
            1f / 60f
        } else {
            ((currentTimeNanos - lastUpdateNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        }
        lastUpdateNanos = currentTimeNanos

        // 1. Analyze Audio & Separate Events
        val features = detector.analyze(telemetry, currentTimeNanos)

        // 2. Continuous Sustained Bass Envelope Follower (Controlled Rhythmic Movement)
        // Sustained bass envelope changes gently: 30 -> 35 -> 40 -> 38 -> 42 -> 40
        val targetSustained = if (!features.isSilent) features.sustainedBass else 0f
        sustainedEnvelope += (targetSustained - sustainedEnvelope) * 0.12f

        val timeSeconds = currentTimeNanos / 1_000_000_000f
        // 46-64Hz controlled acoustic vibration flutter (only active when sustained bass exists)
        val vibrationFrequency = 46f + sustainedEnvelope * 18f
        val sustainedFlutter = if (sustainedEnvelope > 0.08f) {
            sin(timeSeconds * vibrationFrequency * 6.28318f) * (sustainedEnvelope * 0.065f)
        } else 0f

        // 3. Inject Transient Impulses into Physics Velocities
        // KICK IMPULSE: Fast push (0 -> 100 instantly)
        if (features.isKickHit && features.kickImpulse > 0f) {
            val impulse = features.kickImpulse
            for (i in 0 until barCount) {
                val kickVel = impulse * kickWeights[i] * kickVelocityGain
                velocities[i] += kickVel
            }
        }

        // SNARE / TRANSIENT HIT: Localized sharp push only on mid/treble bars
        if (features.isSnareHit && features.snareImpulse > 0f) {
            val impulse = features.snareImpulse
            for (i in 0 until barCount) {
                val snareVel = impulse * snareWeights[i] * snareVelocityGain
                velocities[i] += snareVel
            }
        }

        // 4. Calculate Bar Resting Targets
        val fft = telemetry.fftBars
        val hasFft = fft.isNotEmpty()

        for (i in 0 until barCount) {
            val base = restingBaseline[i]
            if (features.isSilent) {
                // Silence: zero arbitrary movement, pull smoothly to tranquil baseline
                targets[i] = base
            } else {
                val binIdx = fftBinIndices[i]
                val localFft = if (hasFft && binIdx in fft.indices) fft[binIdx] else 0f

                // Sustained component provides gentle ongoing presence without explosive kicks
                val sustainedComponent = (sustainedEnvelope * 0.40f + sustainedFlutter) * sustainedBassWeights[i]
                val midComponent = features.midEnergy * 0.28f * midWeights[i]
                val trebleComponent = features.trebleEnergy * 0.25f * trebleWeights[i]
                val fftComponent = localFft * 0.35f

                val calculatedTarget = base + sustainedComponent + midComponent + trebleComponent + fftComponent
                targets[i] = calculatedTarget.coerceIn(base, 2.5f)
            }
        }

        // 5. Integrate Spring-Damper Equations of Motion:
        //    force = -stiffness * (position - target) - damping * velocity
        //    velocity += (force / mass) * dt
        //    position += velocity * dt
        for (i in 0 until barCount) {
            val displacement = positions[i] - targets[i]
            val springForce = -stiffness * displacement
            val dampingForce = -damping * velocities[i]
            val acceleration = springForce + dampingForce

            velocities[i] += acceleration * dt
            positions[i] += velocities[i] * dt

            // Mechanical base floor stop
            val minFloor = restingBaseline[i]
            if (positions[i] < minFloor) {
                positions[i] = minFloor
                if (velocities[i] < 0f) velocities[i] = 0f
            }
        }
    }

    /**
     * Projects virtual amplitude (unbounded: 0.0 to 3.0+) into the visible canvas height.
     * Uses soft-knee headroom compression so that extreme bass hits push high without
     * hard clipping against a flat ceiling, and immediately spring back down.
     */
    fun getProjectedBarHeight(
        index: Int,
        canvasHeight: Float,
        minHeight: Float
    ): Float {
        val virtualAmp = positions[index]
        val usableRange = (canvasHeight - minHeight).coerceAtLeast(0f)

        // Soft-knee headroom curve:
        // Under 0.82: linear mapping.
        // Over 0.82: smooth hyperbolic roll-off into headroom [0.82 .. 1.0].
        // Even an extreme amplitude like 2.5 pushes to ~0.96 with distinct height,
        // never flattening into a solid clipped block!
        val normalized = if (virtualAmp <= 0.82f) {
            virtualAmp
        } else {
            val excess = virtualAmp - 0.82f
            0.82f + 0.18f * (1f - 1f / (1f + excess * 1.6f))
        }.coerceIn(0f, 1f)

        return (minHeight + usableRange * normalized).coerceIn(minHeight, canvasHeight)
    }

    fun reset() {
        detector.reset()
        for (i in 0 until barCount) {
            positions[i] = restingBaseline[i]
            velocities[i] = 0f
            targets[i] = restingBaseline[i]
        }
        sustainedEnvelope = 0f
        lastUpdateNanos = 0L
    }
}
