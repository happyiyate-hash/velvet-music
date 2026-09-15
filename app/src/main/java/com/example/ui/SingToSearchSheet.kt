package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.DeviceMediaManager
import com.example.media.HumMatchResult
import com.example.media.HumRecognitionState
import com.example.media.HummingRecognitionEngine
import com.example.model.Track
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetDeepCrimson
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Velvet "Sing It" Voice / Song Identification Sheet
 *
 * Design updates:
 * 1. Bottom Thick Red Water Wave:
 *    - Rich, thick crimson liquid filling the bottom of the screen.
 *    - Floating undulating water wave crest at the top, not over-dark.
 * 2. Center Orb & Gaussian Audio Visualizer:
 *    - Symmetric tapered amplitude curve, razor-thin outer glass ring, centered mic orb.
 * 3. Matched Song Bottom Sheet:
 *    - Dedicated clean gray bottom sheet when a song is recognized.
 *    - Music thumbnail on the left, title & details on the right.
 *    - Circular platforms row (Spotify, Apple Music, YouTube Music, Audiomack) that resize evenly.
 *    - Full-width play button at the bottom.
 */
@Composable
fun SingToSearchSheet(
    libraryTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recognitionEngine = remember { HummingRecognitionEngine(scope) }

    val state by recognitionEngine.state.collectAsState()
    val liveAmplitude by recognitionEngine.liveAmplitude.collectAsState()
    var demoIndex by remember { mutableIntStateOf(0) }

    // Intercept back button to dismiss cleanly
    BackHandler {
        recognitionEngine.stopListening()
        onDismiss()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        recognitionEngine.startListening(context, libraryTracks)
    }

    // Auto-start listening on open
    LaunchedEffect(Unit) {
        if (DeviceMediaManager.hasRecordAudioPermission(context)) {
            recognitionEngine.startListening(context, libraryTracks)
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recognitionEngine.stopListening()
        }
    }

    // Modal root: strictly consumes all gestures so nothing bleeds through
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { /* consume all pointer events */ }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* block all click-through */ }
            .testTag("sing_to_search_sheet")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalHeight = maxHeight

            val topStatusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            val textTopPadding = maxOf(topStatusBarInset + 18.dp, 120.dp)
            val micCenterY = totalHeight * 0.35f
            val closeButtonSize = 52.dp
            val closeButtonBottomPadding = bottomNavBarInset + 28.dp

            // 1. Subtle Atmospheric Backdrop
            FullAtmosphericBackground(modifier = Modifier.fillMaxSize())

            // 2. Thick Red Liquid Water Wave (fills lower screen, floating water wave crest on top)
            BottomThickWaterWave(
                amplitude = liveAmplitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight * 0.44f)
                    .align(Alignment.BottomCenter)
            )

            // 3. "Listening..." Status Text
            val isMatched = state is HumRecognitionState.Matched
            if (!isMatched) {
                Text(
                    text = "Listening...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = textTopPadding)
                        .testTag("sing_status_text")
                )
            }

            // 4. Center Visualizer Engine (Tapered Gaussian Bars + Glowing Mic Orb)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = micCenterY - 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Tapered Amplitude Bars
                    TaperedWaveformBars(
                        isLeft = true,
                        amplitude = liveAmplitude,
                        modifier = Modifier.width(100.dp).height(80.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Glassmorphic Glowing Mic Orb
                    CentralGlassOrb(
                        orbSize = 78.dp,
                        amplitude = liveAmplitude,
                        onTap = {
                            if (state is HumRecognitionState.Listening) {
                                demoIndex++
                                recognitionEngine.triggerDemoMatch(demoIndex, libraryTracks)
                            } else {
                                recognitionEngine.startListening(context, libraryTracks)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Tapered Amplitude Bars
                    TaperedWaveformBars(
                        isLeft = false,
                        amplitude = liveAmplitude,
                        modifier = Modifier.width(100.dp).height(80.dp)
                    )
                }
            }

            // 5. Minimalist Glass Close Button ('X') - visible when listening
            if (!isMatched) {
                Box(
                    modifier = Modifier
                        .size(closeButtonSize)
                        .align(Alignment.BottomCenter)
                        .offset(y = -closeButtonBottomPadding)
                        .clip(CircleShape)
                        .background(Color(0x33180004))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x88FF405A),
                                    Color(0x44D51035),
                                    Color(0x2250000D)
                                )
                            ),
                            shape = CircleShape
                        )
                        .shadow(elevation = 10.dp, shape = CircleShape, spotColor = Color(0x44D51035))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                recognitionEngine.stopListening()
                                onDismiss()
                            }
                        )
                        .testTag("sing_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sing It",
                        tint = Color(0xFFFFF1F2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 6. Matched Song Bottom Sheet (Clean gray background, thumbnail on left, title on right, platform circles, play button)
            AnimatedVisibility(
                visible = state is HumRecognitionState.Matched,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val matched = (state as? HumRecognitionState.Matched)?.result
                if (matched != null) {
                    MatchedSongBottomSheet(
                        result = matched,
                        onPlayInVelvet = {
                            val trackToPlay = matched.track ?: Track(
                                id = matched.id,
                                title = matched.title,
                                artist = matched.artist,
                                album = matched.album,
                                durationMs = 210000L,
                                coverResId = matched.coverResId,
                                dominantColor = VelvetDeepCrimson,
                                secondaryColor = VelvetBloodPlum,
                                catalogSource = "Sing It Matched"
                            )
                            onPlayTrack(trackToPlay)
                            onDismiss()
                        },
                        onHumAnother = {
                            recognitionEngine.startListening(context, libraryTracks)
                        },
                        onDismiss = {
                            recognitionEngine.stopListening()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 1. Deep Atmospheric Backdrop
 */
@Composable
private fun FullAtmosphericBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Pure black base
        drawRect(color = Color(0xFF030001))

        // Center subtle burgundy glow behind visualizer
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x224A000E),
                    Color(0x0E220005),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.35f),
                radius = width * 0.55f
            ),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width * 0.55f
        )
    }
}

/**
 * 2. Bottom Thick Red Water Wave
 *
 * Requirements:
 * - Thick, saturated crimson red filling the lower section of the screen (not over-dark).
 * - The top edge of this red region floats and rolls like an authentic water wave.
 * - Luminous floating water crest highlight along the wave surface.
 * - Sings/voice reactive dynamic amplitude modulation.
 */
@Composable
private fun BottomThickWaterWave(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "water_wave_motion")

    // Slow organic rolling phase for the water wave
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_phase_1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_phase_2"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(90, easing = LinearEasing),
        label = "wave_amp"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Ambient soft red atmospheric glow directly above the water wave
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x22FF2448),
                    Color(0x55D51035)
                ),
                startY = 0f,
                endY = height * 0.35f
            )
        )

        // --- Layer 1: Back Water Wave (Translucent rich crimson with slight phase offset) ---
        val pathBack = Path()
        pathBack.moveTo(0f, height)
        val baseBackY = height * (0.24f - animatedAmp * 0.08f)

        for (x in 0..width.toInt() step 8) {
            val nx = x / width
            val y = baseBackY +
                    sin(nx * 3.8f + wavePhase1 * 0.85f) * (18f + animatedAmp * 24f) +
                    cos(nx * 7.5f - wavePhase2 * 0.7f) * (12f + animatedAmp * 16f)
            pathBack.lineTo(x.toFloat(), y)
        }
        pathBack.lineTo(width, height)
        pathBack.close()

        drawPath(
            path = pathBack,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xE6FF3355),
                    Color(0xEECC082A),
                    Color(0xFF9E021E),
                    Color(0xFF700014)
                ),
                startY = baseBackY - 10f,
                endY = height
            )
        )

        // --- Layer 2: Front Primary Thick Red Water Wave (Solid, thick, not over-dark) ---
        val pathFront = Path()
        pathFront.moveTo(0f, height)
        val baseFrontY = height * (0.32f - animatedAmp * 0.10f)

        // Collect crest points for specular highlight stroke
        val crestPoints = mutableListOf<Offset>()

        for (x in 0..width.toInt() step 6) {
            val nx = x / width
            val y = baseFrontY +
                    sin(nx * 4.2f + wavePhase2) * (22f + animatedAmp * 30f) +
                    cos(nx * 8.6f - wavePhase1 * 1.1f) * (14f + animatedAmp * 18f)
            pathFront.lineTo(x.toFloat(), y)
            crestPoints.add(Offset(x.toFloat(), y))
        }
        pathFront.lineTo(width, height)
        pathFront.close()

        // Thick rich red fluid fill (vibrant and deep, avoiding muddy near-black)
        drawPath(
            path = pathFront,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFF2448), // Glowing bright crimson at the floating crest
                    Color(0xFFE5153A),
                    Color(0xFFD51035),
                    Color(0xFFB50827),
                    Color(0xFF8F031D),
                    Color(0xFF6E0014)  // Rich solid base at the bottom
                ),
                startY = baseFrontY - 20f,
                endY = height
            )
        )

        // --- Layer 3: Luminous Floating Water Crest Line ---
        if (crestPoints.isNotEmpty()) {
            val crestPath = Path()
            crestPoints.forEachIndexed { index, point ->
                if (index == 0) crestPath.moveTo(point.x, point.y) else crestPath.lineTo(point.x, point.y)
            }

            // Glow line behind the crest
            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x44FFA0B0),
                        Color(0xCCFF4D6D),
                        Color(0xEEFFFFFF),
                        Color(0xCCFF4D6D),
                        Color(0x44FFA0B0)
                    )
                ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Sharp specular water crest line
            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x99FFCCD5),
                        Color(0xFFFFF0F2),
                        Color(0xFFFF8095),
                        Color(0xFFFFF0F2),
                        Color(0x99FFCCD5)
                    )
                ),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 3. Audio Wave Bars (Side Visualizer):
 * - Symmetric Amplitude Gaussian Curve: tallest (~28.dp) adjacent to orb, tapering smoothly down to tiny points (2.dp) at far edges.
 * - Line width strictly 2.dp with rounded caps (StrokeCap.Round) and 4.5.dp gap.
 * - Vertical gradient fading: Red transitioning to Transparent on ends.
 */
@Composable
private fun TaperedWaveformBars(
    isLeft: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tapered_wave_loop")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(90, easing = LinearEasing),
        label = "animated_amplitude"
    )

    Canvas(modifier = modifier) {
        val barCount = 14
        val barWidthPx = 2.dp.toPx()
        val barGapPx = 4.5.dp.toPx()
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val distFromOrb = if (isLeft) (barCount - 1 - i) else i
            val normDist = distFromOrb.toFloat() / (barCount - 1)

            // Gaussian curve: tallest adjacent to orb, tapering smoothly to tiny points
            val gaussian = exp(-((normDist * 2.3f) * (normDist * 2.3f)))
            val organicJitter = sin(wavePhase * 2.1f + distFromOrb * 0.65f) * 0.18f

            val baseH = (2.dp.toPx() + 26.dp.toPx() * gaussian.toFloat())
            val voiceBoost = animatedAmp * (20.dp.toPx() * gaussian.toFloat())
            val totalH = ((baseH + voiceBoost) * (1f + organicJitter)).coerceIn(2.dp.toPx(), 54.dp.toPx())

            val xPos = if (isLeft) {
                size.width - ((distFromOrb + 0.5f) * (barWidthPx + barGapPx))
            } else {
                (distFromOrb + 0.5f) * (barWidthPx + barGapPx)
            }

            val barAlpha = (0.35f + 0.65f * gaussian.toFloat() + animatedAmp * 0.25f).coerceIn(0.2f, 1f)

            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x00FF2448),
                    Color(0xFFFF3355).copy(alpha = barAlpha),
                    Color(0xFFE51B3E).copy(alpha = barAlpha),
                    Color(0xFFFF3355).copy(alpha = barAlpha),
                    Color(0x00FF2448)
                ),
                startY = centerY - (totalH / 2f),
                endY = centerY + (totalH / 2f)
            )

            drawLine(
                brush = barBrush,
                start = Offset(xPos, centerY - (totalH / 2f)),
                end = Offset(xPos, centerY + (totalH / 2f)),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 4. Center Glass Orb & Thin Outer Ring
 */
@Composable
private fun CentralGlassOrb(
    orbSize: Dp,
    amplitude: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(90, easing = LinearEasing),
        label = "orb_amp"
    )

    Box(
        modifier = modifier.size(orbSize + 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbRadius = orbSize.toPx() / 2f

            // Single razor-thin outer circular ring (1.dp stroke width) 8.dp outside main orb
            val ring1Radius = orbRadius + 8.dp.toPx()
            val ring1Alpha = (pulseAlpha + animatedAmp * 0.20f).coerceIn(0.2f, 0.55f)
            drawCircle(
                color = Color(0xFFFF2448).copy(alpha = ring1Alpha),
                radius = ring1Radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Secondary subtle outer ring at 18.dp
            val ring2Radius = orbRadius + 18.dp.toPx()
            drawCircle(
                color = Color(0xFFE51B3E).copy(alpha = 0.12f + animatedAmp * 0.10f),
                radius = ring2Radius,
                center = center,
                style = Stroke(width = 0.8.dp.toPx())
            )

            // Soft radial ambient bloom behind orb
            val bloomRadius = orbRadius * 1.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x55FF2448).copy(alpha = 0.35f + animatedAmp * 0.30f),
                        Color(0x22D51035),
                        Color.Transparent
                    ),
                    center = center,
                    radius = bloomRadius
                ),
                radius = bloomRadius,
                center = center
            )

            // Dark glass interior
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF040001),
                        Color(0xFF100004),
                        Color(0xFF240008),
                        Color(0xFF38000C)
                    ),
                    center = center,
                    radius = orbRadius
                ),
                radius = orbRadius,
                center = center
            )

            // Soft inner red illumination at bottom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x44FF2448),
                        Color.Transparent
                    ),
                    center = Offset(center.x, center.y + orbRadius * 0.40f),
                    radius = orbRadius * 0.65f
                ),
                radius = orbRadius * 0.65f,
                center = Offset(center.x, center.y + orbRadius * 0.40f)
            )

            // Luminous crimson gradient perimeter ring
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF889E),
                        Color(0xFFFF2448),
                        Color(0xFFD51035),
                        Color(0xFF700012),
                        Color(0xFFD51035),
                        Color(0xFFFF2448),
                        Color(0xFFFF889E)
                    ),
                    center = center
                ),
                radius = orbRadius - 0.75.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Top specular crescent highlight
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f),
                        Color(0x66FF889E),
                        Color.Transparent
                    ),
                    start = Offset(center.x - orbRadius * 0.6f, center.y - orbRadius * 0.9f),
                    end = Offset(center.x + orbRadius * 0.6f, center.y - orbRadius * 0.4f)
                ),
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x - orbRadius + 1.dp.toPx(), center.y - orbRadius + 1.dp.toPx()),
                size = Size((orbRadius - 1.dp.toPx()) * 2, (orbRadius - 1.dp.toPx()) * 2),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Tap target & Centered Microphone Icon
        Box(
            modifier = Modifier
                .size(orbSize)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .testTag("sing_center_mic_orb"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Sing or Hum",
                tint = Color(0xFFFFF1F2),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * 5. Matched Song Bottom Sheet
 *
 * Requirements:
 * - Dedicated bottom sheet with clean dark gray background.
 * - Music thumbnail on the left side.
 * - Title and artist close to it on the right side.
 * - Circular music platforms: Spotify, Apple Music, YouTube Music, Audiomack.
 * - Sized to resize together responsively with platform names under each circle.
 * - Remaining full-width play button at the bottom.
 */
@Composable
private fun MatchedSongBottomSheet(
    result: HumMatchResult,
    onPlayInVelvet: () -> Unit,
    onHumAnother: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFF1E1E22), // Clean dark gray background
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color(0x38FFFFFF),
                    Color(0x18FFFFFF),
                    Color.Transparent
                )
            )
        ),
        shadowElevation = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("matched_song_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF232328),
                            Color(0xFF1B1B1E),
                            Color(0xFF151518)
                        )
                    )
                )
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = navBottomInset + 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF4C4C54))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Track Header: Thumbnail on the left, Title & details on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Music Thumbnail at the left side
                Image(
                    painter = painterResource(id = result.coverResId),
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Title and artist close to it at the right side
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3355))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${result.matchPercentage}% Match • Found",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF405A)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = result.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF6F6F8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = result.artist,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFB5B5BE),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = result.album,
                        fontSize = 12.sp,
                        color = Color(0xFF7A7A84),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dismiss button at top-right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9E9EAA),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Section Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "LISTEN ON STREAMING PLATFORMS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF888892)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Platforms Row: Spotify, Apple Music, YouTube Music, Audiomack (Circles with equal weight resizing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Spotify
                PlatformCircleItem(
                    name = "Spotify",
                    modifier = Modifier.weight(1f),
                    onClick = { HummingRecognitionEngine.launchSpotify(context, result.artist, result.title) }
                ) {
                    SpotifyCircleLogo()
                }

                // 2. Apple Music
                PlatformCircleItem(
                    name = "Apple Music",
                    modifier = Modifier.weight(1f),
                    onClick = { HummingRecognitionEngine.launchAppleMusic(context, result.artist, result.title) }
                ) {
                    AppleMusicCircleLogo()
                }

                // 3. YouTube Music
                PlatformCircleItem(
                    name = "YouTube Music",
                    modifier = Modifier.weight(1f),
                    onClick = { HummingRecognitionEngine.launchYouTubeMusic(context, result.artist, result.title) }
                ) {
                    YouTubeMusicCircleLogo()
                }

                // 4. Audiomack
                PlatformCircleItem(
                    name = "Audiomack",
                    modifier = Modifier.weight(1f),
                    onClick = { HummingRecognitionEngine.launchAudiomack(context, result.artist, result.title) }
                ) {
                    AudiomackCircleLogo()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Remaining Full-Width Play Button at the bottom
            Button(
                onClick = onPlayInVelvet,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF2448),
                                Color(0xFFD51035)
                            )
                        )
                    )
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x66FF2448))
                    .testTag("sing_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play in Velvet Music",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary option: "Hum or Sing Another Song"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onHumAnother)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF9E9EAA),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hum or Sing Another Song",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9EAA)
                )
            }
        }
    }
}

/**
 * Single Platform Item with responsive equal weight resizing
 */
@Composable
private fun PlatformCircleItem(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    logoContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        logoContent()
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = name,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFD4D4DC),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Authentic Circular Logo: Spotify
 */
@Composable
private fun SpotifyCircleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xFF1DB954))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .shadow(6.dp, CircleShape, spotColor = Color(0x441DB954)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val center = Offset(size.width / 2f, size.height * 0.65f)

            // 3 curved Spotify soundwave arcs
            // Arc 1 (Top)
            drawArc(
                color = Color(0xFF121212),
                startAngle = 210f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - 12.dp.toPx(), center.y - 14.dp.toPx()),
                size = Size(24.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )
            // Arc 2 (Middle)
            drawArc(
                color = Color(0xFF121212),
                startAngle = 212f,
                sweepAngle = 116f,
                useCenter = false,
                topLeft = Offset(center.x - 9.5.dp.toPx(), center.y - 10.dp.toPx()),
                size = Size(19.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Arc 3 (Bottom)
            drawArc(
                color = Color(0xFF121212),
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - 7.dp.toPx(), center.y - 6.5.dp.toPx()),
                size = Size(14.dp.toPx(), 10.5.dp.toPx()),
                style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Authentic Circular Logo: Apple Music
 */
@Composable
private fun AppleMusicCircleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFA2D48),
                        Color(0xFFFD5E70)
                    )
                )
            )
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .shadow(6.dp, CircleShape, spotColor = Color(0x44FA2D48)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Apple Music",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Authentic Circular Logo: YouTube Music
 */
@Composable
private fun YouTubeMusicCircleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFF0033),
                        Color(0xFFCC0000)
                    )
                )
            )
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .shadow(6.dp, CircleShape, spotColor = Color(0x44FF0033)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer white ring
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Inner play triangle
            val trianglePath = Path().apply {
                val halfW = 4.dp.toPx()
                val halfH = 4.5.dp.toPx()
                moveTo(center.x - halfW * 0.7f, center.y - halfH)
                lineTo(center.x + halfW * 1.3f, center.y)
                lineTo(center.x - halfW * 0.7f, center.y + halfH)
                close()
            }
            drawPath(trianglePath, color = Color.White)
        }
    }
}

/**
 * Authentic Circular Logo: Audiomack
 */
@Composable
private fun AudiomackCircleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFA200),
                        Color(0xFFFF7700)
                    )
                )
            )
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .shadow(6.dp, CircleShape, spotColor = Color(0x44FFA200)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val barW = 2.2.dp.toPx()
            val heights = listOf(6.dp.toPx(), 11.dp.toPx(), 16.dp.toPx(), 12.dp.toPx(), 7.dp.toPx())
            val spacing = 3.6.dp.toPx()
            val totalW = (heights.size - 1) * spacing
            val startX = center.x - (totalW / 2f)

            heights.forEachIndexed { idx, h ->
                val x = startX + idx * spacing
                drawLine(
                    color = Color.White,
                    start = Offset(x, center.y - (h / 2f)),
                    end = Offset(x, center.y + (h / 2f)),
                    strokeWidth = barW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
