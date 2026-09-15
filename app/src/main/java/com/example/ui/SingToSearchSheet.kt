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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
 * Refactored to exact design specifications:
 * 1. Audio Wave Bars: Symmetric Amplitude Gaussian Curve (tallest ~28.dp near orb tapering to 2.dp points at ends),
 *    thin line width (strictly 2.dp), rounded caps, 4.dp spacing, vertical gradient fading.
 * 2. Center Orb & Glassmorphic Ring: Refined compact diameter (78.dp), subtle inner glass reflection rim (1.dp),
 *    razor-thin outer circular ring (1.dp stroke, alpha = 0.3f) positioned ~8.dp outside orb.
 * 3. Bottom Ethereal Ambient Smoke: Additive Blend Overlay (BlendMode.Screen) with multi-layered translucent bezier paths,
 *    subtle silk filaments, and deep red ambient glow.
 * 4. Typography & Spacing: Quiet, letter-spaced "Listening..." text (fontSize = 14.sp, letterSpacing = 2.sp) in muted white/gray.
 * 5. Full touch-capture modal interaction barrier.
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

    // Modal root: strictly consumes all gestures and clicks so nothing bleeds through
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
            val density = LocalDensity.current

            val topStatusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            // Calculate responsive positions
            val textTopPadding = maxOf(topStatusBarInset + 18.dp, 120.dp)
            val micCenterY = totalHeight * 0.35f
            val closeButtonSize = 52.dp
            val closeButtonBottomPadding = bottomNavBarInset + 28.dp

            // 1. Subtle Atmospheric Background Gradient
            FullAtmosphericBackground(modifier = Modifier.fillMaxSize())

            // 2. Bottom Ethereal Ambient Smoke Overlay (NOT Solid Wave, Additive Blend Screen Mode)
            BottomEtherealSmoke(
                amplitude = liveAmplitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight * 0.45f)
                    .align(Alignment.BottomCenter)
            )

            // 3. "Listening..." Muted Letter-Spaced Text
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

            // 4. Center Visualizer Engine (Thin Tapered Bars + Subtle Orb)
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

            // 5. Matched Song Result Card (floats elegantly when recognized)
            AnimatedVisibility(
                visible = state is HumRecognitionState.Matched,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = closeButtonBottomPadding + closeButtonSize + 20.dp)
                    .padding(horizontal = 20.dp)
            ) {
                val matched = (state as? HumRecognitionState.Matched)?.result
                if (matched != null) {
                    LuxuryMatchedCard(
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
                        }
                    )
                }
            }

            // 6. Minimalist Glass Close Button ('X')
            Box(
                modifier = Modifier
                    .size(closeButtonSize)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.dp)
                    .offset(y = -closeButtonBottomPadding)
                    .clip(CircleShape)
                    .background(Color(0x22180004))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x66D51035),
                                Color(0x338F071F),
                                Color(0x1A50000D)
                            )
                        ),
                        shape = CircleShape
                    )
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color(0x33D51035))
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
                    tint = Color(0xFFD7D0D2),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 1. Deep Atmospheric Background
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
                    Color(0x1F4A000E),
                    Color(0x0C220005),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.35f),
                radius = width * 0.52f
            ),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width * 0.52f
        )
    }
}

/**
 * 1. Audio Wave Bars (Side Visualizer):
 * - Symmetric Amplitude Gaussian Curve: tallest (~28.dp) adjacent to orb, tapering smoothly down to tiny points (2.dp) at far edges.
 * - Line width strictly 2.dp with rounded caps (StrokeCap.Round) and 4.dp gap.
 * - Vertical gradient fading: Color.Red transitioning to Color.Transparent on ends.
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
            // Index distance from the orb:
            // For left side: bars go from left edge (i=0) to near orb (i=barCount-1)
            // For right side: bars go from near orb (i=0) to right edge (i=barCount-1)
            val distFromOrb = if (isLeft) (barCount - 1 - i) else i
            val normDist = distFromOrb.toFloat() / (barCount - 1) // 0.0 at orb, 1.0 at outer tip

            // Symmetric Amplitude Gaussian Curve:
            // Tallest (~28.dp) adjacent to the orb, tapering smoothly down to tiny points (2.dp) at the outer edge
            val gaussian = exp(-((normDist * 2.3f) * (normDist * 2.3f)))

            // Organic subtle breathing per bar
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

            // Vertical gradient fading: Red transitioning to Transparent on top and bottom ends
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
 * 2. Center Orb & Glassmorphic Ring:
 * - Refined diameter: 78.dp
 * - Subtle inner glass reflection rim on the top edge using fine white/rose radial gradient stroke (1.dp thickness).
 * - Single razor-thin outer circular ring (1.dp stroke width) positioned 8.dp outside the main orb with low opacity (alpha = 0.3f).
 * - Concentric secondary outer ring at 18.dp with alpha = 0.12f.
 * - Soft warm white (#FFF1F2) minimalist microphone icon.
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
        modifier = modifier
            .size(orbSize + 40.dp), // allows outer rings to draw comfortably
        contentAlignment = Alignment.Center
    ) {
        // Outer rings and glass orb canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbRadius = orbSize.toPx() / 2f

            // Single razor-thin outer circular ring (1.dp stroke width) 8.dp outside main orb (alpha ~0.3f)
            val ring1Radius = orbRadius + 8.dp.toPx()
            val ring1Alpha = (pulseAlpha + animatedAmp * 0.20f).coerceIn(0.2f, 0.55f)
            drawCircle(
                color = Color(0xFFFF2448).copy(alpha = ring1Alpha),
                radius = ring1Radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Secondary subtle outer ring at 18.dp outside orb (alpha ~0.12f)
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

            // Luminous crimson gradient perimeter ring (fine 1.5.dp stroke)
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

            // Subtle inner glass reflection rim on top edge using fine white/rose gradient stroke (1.dp thickness)
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
 * 3. Bottom Ethereal Ambient Smoke Overlay (NOT Solid Wave):
 * - Additive Blend Overlay (BlendMode.Screen)
 * - Multi-layered bezier paths with low opacity (alpha = 0.15f to 0.35f)
 * - Heavy blurring (Modifier.blur(24.dp)) so it looks like light glowing through deep red silk/smoke, not a solid wave block.
 * - Delicate luminous silk filament lines along flowing crests.
 */
@Composable
private fun BottomEtherealSmoke(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ethereal_smoke_loop")

    val phaseSlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_slow"
    )

    val phaseFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_fast"
    )

    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(120, easing = LinearEasing),
        label = "smoke_amp"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .blur(24.dp) // heavy radial blur for ethereal diffusion
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // --- Layer 1: Ethereal Deep Wine Ambient Mist (BlendMode.Screen) ---
            val path1 = Path()
            path1.moveTo(0f, height)
            val baseY1 = height * (0.32f - animatedAmp * 0.10f)
            for (x in 0..width.toInt() step 16) {
                val nx = x / width
                val y = baseY1 +
                        sin(nx * 3.4f + phaseSlow) * 22f +
                        cos(nx * 6.8f - phaseSlow * 0.8f) * 14f
                path1.lineTo(x.toFloat(), y)
            }
            path1.lineTo(width, height)
            path1.close()

            drawPath(
                path = path1,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3348000C),
                        Color(0x558F071F).copy(alpha = 0.22f + animatedAmp * 0.12f),
                        Color(0x66260006),
                        Color(0x22120003)
                    ),
                    startY = baseY1 - 20f,
                    endY = height
                ),
                blendMode = BlendMode.Screen
            )

            // --- Layer 2: Translucent Silky Wave Billows (BlendMode.Screen, alpha 0.18f - 0.32f) ---
            val path2 = Path()
            path2.moveTo(0f, height)
            val baseY2 = height * (0.45f - animatedAmp * 0.12f)
            for (x in 0..width.toInt() step 12) {
                val nx = x / width
                val y = baseY2 +
                        sin(nx * 4.2f - phaseFast) * 28f +
                        sin(nx * 8.6f + phaseSlow * 1.1f) * 16f
                path2.lineTo(x.toFloat(), y)
            }
            path2.lineTo(width, height)
            path2.close()

            drawPath(
                path = path2,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x338F071F),
                        Color(0x55D51035).copy(alpha = 0.25f + animatedAmp * 0.15f),
                        Color(0x66FF2448).copy(alpha = 0.30f + animatedAmp * 0.18f),
                        Color(0x44D51035),
                        Color(0x2250000D)
                    ),
                    startX = 0f,
                    endX = width
                ),
                blendMode = BlendMode.Screen
            )

            // --- Layer 3: Ethereal Light Ribbon Glow (BlendMode.Screen, alpha 0.20f - 0.35f) ---
            val path3 = Path()
            path3.moveTo(0f, height)
            val baseY3 = height * (0.60f - animatedAmp * 0.10f)
            for (x in 0..width.toInt() step 12) {
                val nx = x / width
                val y = baseY3 +
                        sin(nx * 3.6f + phaseFast * 1.2f) * 24f +
                        cos(nx * 7.2f - phaseSlow) * 15f
                path3.lineTo(x.toFloat(), y)
            }
            path3.lineTo(width, height)
            path3.close()

            drawPath(
                path = path3,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x66FF2448).copy(alpha = 0.28f + animatedAmp * 0.15f),
                        Color(0x55D51035).copy(alpha = 0.24f + animatedAmp * 0.12f),
                        Color(0x338F071F),
                        Color(0x11120003)
                    ),
                    startY = baseY3 - 10f,
                    endY = height
                ),
                blendMode = BlendMode.Screen
            )

            // --- Layer 4: Luminous Crest Filaments (BlendMode.Screen) ---
            val filamentPath = Path()
            for (x in 0..width.toInt() step 10) {
                val nx = x / width
                val y = baseY2 +
                        sin(nx * 4.2f - phaseFast) * 28f +
                        sin(nx * 8.6f + phaseSlow * 1.1f) * 16f
                if (x == 0) filamentPath.moveTo(0f, y) else filamentPath.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = filamentPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x11FF2448),
                        Color(0x88FF2448).copy(alpha = 0.35f + animatedAmp * 0.20f),
                        Color(0xAAFFA0B0).copy(alpha = 0.45f + animatedAmp * 0.20f),
                        Color(0x77FF2448).copy(alpha = 0.30f + animatedAmp * 0.15f),
                        Color(0x11FF2448)
                    )
                ),
                style = Stroke(width = 3.dp.toPx()),
                blendMode = BlendMode.Screen
            )
        }
    }
}

/**
 * Luxury Matched Track Card
 */
@Composable
private fun LuxuryMatchedCard(
    result: HumMatchResult,
    onPlayInVelvet: () -> Unit,
    onHumAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF0120205),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color(0x77FF2448),
                    Color(0x338F071F),
                    Color(0x1150000D)
                )
            )
        ),
        shadowElevation = 20.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("luxury_matched_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33FF2448),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x66FF2448))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFFF405A),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${result.matchPercentage}% Match • Query-by-Humming",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFF1F2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .border(0.6.dp, Color(0x33FF2448), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = result.coverResId),
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFF1F2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.artist,
                        fontSize = 13.5.sp,
                        color = Color(0xFFFF405A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.album,
                        fontSize = 11.5.sp,
                        color = Color(0xFF9E9295),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onPlayInVelvet,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD51035),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("sing_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Play in Velvet Music",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExternalPillButton(
                    label = "Spotify",
                    color = Color(0xFF1DB954),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        HummingRecognitionEngine.launchSpotify(context, result.artist, result.title)
                    }
                )

                ExternalPillButton(
                    label = "YouTube Music",
                    color = Color(0xFFFF0000),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        HummingRecognitionEngine.launchYouTubeMusic(context, result.artist, result.title)
                    }
                )

                OutlinedButton(
                    onClick = onHumAnother,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x44FFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD7D0D2)),
                    modifier = Modifier
                        .height(38.dp)
                        .weight(1f)
                        .testTag("sing_again_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Another", fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ExternalPillButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0x1AFFFFFF),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, color.copy(alpha = 0.5f)),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE0D8DA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Color(0x88D7D0D2),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}
