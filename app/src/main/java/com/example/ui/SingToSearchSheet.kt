package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import com.example.R
import com.example.media.DeviceMediaManager
import com.example.media.HumMatchResult
import com.example.media.HumRecognitionState
import com.example.media.HummingRecognitionEngine
import com.example.model.Track
import com.example.recognition.RecognitionDiagnostics
import com.example.recognition.SongArtworkResolver
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
    val lastSavedDiagnostics by recognitionEngine.lastDiagnostics.collectAsState()
    var showDiagnosticsModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recognitionEngine.loadSavedDiagnostics(context)
    }

    val activeDiagnostics: RecognitionDiagnostics? = when (val s = state) {
        is HumRecognitionState.Matched -> s.diagnostics
        is HumRecognitionState.NoMatch -> s.diagnostics
        is HumRecognitionState.ConnectionError -> s.diagnostics
        is HumRecognitionState.ProviderError -> s.diagnostics
        is HumRecognitionState.ResponseParsingError -> s.diagnostics
        else -> null
    } ?: lastSavedDiagnostics

    // Smooth ambient breathing visualizer pulse during Searching ("Identifying…")
    val pulseTransition = rememberInfiniteTransition(label = "identifying_pulse")
    val searchingPulse by pulseTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searching_amp"
    )
    val visualizerAmplitude = when (state) {
        is HumRecognitionState.Listening -> liveAmplitude
        is HumRecognitionState.Searching -> searchingPulse
        else -> 0f
    }

    val statusText = when (state) {
        is HumRecognitionState.Listening -> "Listening…"
        is HumRecognitionState.Searching -> "Identifying…"
        is HumRecognitionState.Matched -> "Song Identified"
        is HumRecognitionState.NoMatch -> "Couldn't Identify"
        is HumRecognitionState.ConnectionError -> "Connection Notice"
        is HumRecognitionState.ProviderError -> "Service Notice"
        is HumRecognitionState.ResponseParsingError -> "Response Notice"
        is HumRecognitionState.Idle -> "Listening…"
    }

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

            // 2. Listening & Searching View (Microphone + Orb + Ambient Smoke)
            AnimatedVisibility(
                visible = state is HumRecognitionState.Listening || state is HumRecognitionState.Searching || state is HumRecognitionState.Idle,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(350)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Ethereal Ambient Smoke Overlay
                    BottomEtherealSmoke(
                        amplitude = visualizerAmplitude,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight * 0.45f)
                            .align(Alignment.BottomCenter)
                    )

                    // Status Text ("Listening…" or "Identifying…")
                    Text(
                        text = statusText,
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

                    // Subtle Header Diagnostics Button if previous diagnostics exist
                    if (lastSavedDiagnostics != null) {
                        IconButton(
                            onClick = { showDiagnosticsModal = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = topStatusBarInset + 12.dp, end = 16.dp)
                                .testTag("sing_header_diagnostics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "View diagnostic details",
                                tint = Color(0xFFE28492).copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Center Visualizer Engine (Tapered Waveform Bars + Central Glowing Orb)
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
                            TaperedWaveformBars(
                                isLeft = true,
                                amplitude = visualizerAmplitude,
                                modifier = Modifier.width(100.dp).height(80.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            CentralGlassOrb(
                                orbSize = 78.dp,
                                amplitude = visualizerAmplitude,
                                onTap = {
                                    recognitionEngine.startListening(context, libraryTracks)
                                }
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            TaperedWaveformBars(
                                isLeft = false,
                                amplitude = visualizerAmplitude,
                                modifier = Modifier.width(100.dp).height(80.dp)
                            )
                        }
                    }

                    // Bottom Close Button ('X')
                    Box(
                        modifier = Modifier
                            .size(closeButtonSize)
                            .align(Alignment.BottomCenter)
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

            // 3. Matched Song Result View (Smoothly fades in and replaces the microphone completely)
            AnimatedVisibility(
                visible = state is HumRecognitionState.Matched,
                enter = fadeIn(animationSpec = tween(450, delayMillis = 100)) + scaleIn(initialScale = 0.94f, animationSpec = tween(450, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.94f, animationSpec = tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                val matched = (state as? HumRecognitionState.Matched)?.result
                if (matched != null) {
                    SeamlessMatchedResultView(
                        result = matched,
                        topPadding = topStatusBarInset + 16.dp,
                        bottomPadding = bottomNavBarInset + 20.dp,
                        onPlayInVelvet = {
                            val trackToPlay = matched.track ?: Track(
                                id = matched.id,
                                title = matched.title,
                                artist = matched.artist,
                                album = matched.album,
                                durationMs = if (matched.durationMs > 0L) matched.durationMs else 210000L,
                                coverResId = matched.coverResId,
                                dominantColor = VelvetDeepCrimson,
                                secondaryColor = VelvetBloodPlum,
                                catalogSource = "Sing It Matched",
                                artworkUri = matched.artworkUrl
                            )
                            onPlayTrack(trackToPlay)
                            onDismiss()
                        },
                        onRestartMic = {
                            recognitionEngine.startListening(context, libraryTracks)
                        },
                        onClose = {
                            recognitionEngine.stopListening()
                            onDismiss()
                        },
                        onViewDiagnostics = {
                            showDiagnosticsModal = true
                        }
                    )
                }
            }

            // 4. "Couldn't identify that song" or "Couldn't connect" Notice View
            AnimatedVisibility(
                visible = state is HumRecognitionState.NoMatch ||
                        state is HumRecognitionState.ConnectionError ||
                        state is HumRecognitionState.ProviderError ||
                        state is HumRecognitionState.ResponseParsingError,
                enter = fadeIn(animationSpec = tween(450, delayMillis = 100)) + scaleIn(initialScale = 0.94f, animationSpec = tween(450, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                val noticeTitle = when (val s = state) {
                    is HumRecognitionState.NoMatch -> s.title
                    is HumRecognitionState.ConnectionError -> s.title
                    is HumRecognitionState.ProviderError -> s.title
                    is HumRecognitionState.ResponseParsingError -> s.title
                    else -> "No Match"
                }
                val noticeMsg = when (val s = state) {
                    is HumRecognitionState.NoMatch -> s.message
                    is HumRecognitionState.ConnectionError -> s.message
                    is HumRecognitionState.ProviderError -> s.message
                    is HumRecognitionState.ResponseParsingError -> s.message
                    else -> "Please try again."
                }
                val noticeTag = when (state) {
                    is HumRecognitionState.NoMatch -> "no_match_card"
                    is HumRecognitionState.ProviderError -> "provider_error_card"
                    is HumRecognitionState.ResponseParsingError -> "parsing_error_card"
                    else -> "connection_error_card"
                }

                SeamlessNoticeResultView(
                    title = noticeTitle,
                    message = noticeMsg,
                    testTag = noticeTag,
                    topPadding = topStatusBarInset + 24.dp,
                    bottomPadding = bottomNavBarInset + 24.dp,
                    onViewDiagnostics = {
                        showDiagnosticsModal = true
                    },
                    onRestartMic = {
                        recognitionEngine.startListening(context, libraryTracks)
                    },
                    onClose = {
                        recognitionEngine.stopListening()
                        onDismiss()
                    }
                )
            }
        }

        // Fullscreen diagnostics modal overlay
        if (showDiagnosticsModal) {
            RecognitionDiagnosticsModal(
                diagnostics = activeDiagnostics ?: lastSavedDiagnostics,
                onClose = { showDiagnosticsModal = false }
            )
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
 * Seamless Fullscreen Matched Result View
 *
 * Implements the user's requested cinematic animation:
 * - When recognition succeeds, the microphone & wave visualizer gracefully fade out.
 * - The matched track artwork, metadata, and streaming platforms fade in.
 * - Under the music: Title, Artist, Album, and an explicit "Copy Title" button.
 * - Platform buttons: If specific links are unavailable from the backend, smart search links
 *   (YouTube Music, Spotify, Apple Music, Audiomack) are automatically generated.
 * - At the bottom: A glowing microphone button allows the user to restart/fetch another song immediately.
 */
/**
 * Music Recognition Result Screen — Exact UI & Animation Direction
 *
 * 1. Hero Artwork: Large, edge-to-edge at top with no duplicate card underneath.
 * 2. Soft Brush/Fade: Seamless organic fade into the dark background without hard lines, waves, or U-shapes.
 * 3. Soundwave & Track Information: Natural screen typography (Artist, Title, Album metadata).
 * 4. Action Controls: Play (brand gradient), Copy Title (dark glass), Share (dark glass circle).
 * 5. Native "Available on" List: Full-width items for YouTube Music, Spotify, Apple Music, Audiomack, SoundCloud.
 * 6. Staggered 5-Step Animation: Fast, overlapping sequence settling smoothly within ~1.1–1.2s.
 */
@Composable
private fun SeamlessMatchedResultView(
    result: HumMatchResult,
    topPadding: Dp,
    bottomPadding: Dp,
    onPlayInVelvet: () -> Unit,
    onRestartMic: () -> Unit,
    onClose: () -> Unit,
    onViewDiagnostics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Animation states for the 5-step entrance sequence
    val artworkAlpha = remember { Animatable(0f) }
    val artworkScale = remember { Animatable(0.96f) }
    val brushFadeProgress = remember { Animatable(0f) }
    val trackInfoAlpha = remember { Animatable(0f) }
    val trackInfoOffsetY = remember { Animatable(20f) }
    val actionsAlpha = remember { Animatable(0f) }
    val actionsOffsetY = remember { Animatable(16f) }
    val platformAlphas = remember { List(6) { Animatable(0f) } }
    val platformOffsetsY = remember { List(6) { Animatable(16f) } }

    LaunchedEffect(result) {
        // Step 1: Artwork appears (~260ms)
        launch {
            artworkAlpha.animateTo(1f, animationSpec = tween(260, easing = LinearOutSlowInEasing))
        }
        launch {
            artworkScale.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
        }

        // Step 2: Brush fade animates into background (~340ms, starts at 120ms)
        delay(120)
        launch {
            brushFadeProgress.animateTo(1f, animationSpec = tween(340, easing = FastOutSlowInEasing))
        }

        // Step 3: Track information appears (~220ms, starts at 300ms)
        delay(180)
        launch {
            trackInfoAlpha.animateTo(1f, animationSpec = tween(220, easing = LinearOutSlowInEasing))
        }
        launch {
            trackInfoOffsetY.animateTo(0f, animationSpec = tween(220, easing = FastOutSlowInEasing))
        }

        // Step 4: Action buttons appear (~200ms, starts at 440ms)
        delay(140)
        launch {
            actionsAlpha.animateTo(1f, animationSpec = tween(200, easing = LinearOutSlowInEasing))
        }
        launch {
            actionsOffsetY.animateTo(0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
        }

        // Step 5: Platform rows stagger in one by one (starts at 560ms, 90ms gap)
        delay(120)
        for (i in 0 until 6) {
            val idx = i
            launch {
                platformAlphas[idx].animateTo(1f, animationSpec = tween(200, easing = LinearOutSlowInEasing))
            }
            launch {
                platformOffsetsY[idx].animateTo(0f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            }
            delay(90)
        }
    }

    // Dynamic state for resolving and displaying high-resolution artwork
    var resolvedArtworkUrl by remember(result) {
        mutableStateOf(SongArtworkResolver.cleanArtworkUrl(result.artworkUrl))
    }

    LaunchedEffect(result) {
        val fetched = SongArtworkResolver.resolveArtwork(
            artist = result.artist,
            title = result.title,
            rawArtworkUrl = result.artworkUrl
        )
        if (!fetched.isNullOrBlank()) {
            resolvedArtworkUrl = fetched
        }
    }

    // Native platform items derived from single source of truth MUSIC_PLATFORMS
    val platforms = remember(result) {
        val query = "${result.artist} ${result.title}".trim()
        MUSIC_PLATFORMS.map { config ->
            val directUrl = when (config.id) {
                "youtube-music" -> result.youtubeMusicUrl
                "spotify" -> result.spotifyUrl
                "apple-music" -> result.appleMusicUrl
                "audiomack" -> result.audiomackUrl
                "soundcloud" -> result.soundcloudUrl
                "boomplay" -> result.boomplayUrl
                else -> null
            }
            NativePlatformItem(
                config = config,
                directUrl = directUrl,
                query = query
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070204))
            .testTag("luxury_matched_card")
    ) {
        // Scrollable content so all platforms and controls are accessible on every display
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. HERO ARTWORK CONTAINER WITH GLOWING CURVED WAVE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    // Compact hero so the result details and platform list sit higher.
                    .graphicsLayer {
                        alpha = artworkAlpha.value
                        scaleX = artworkScale.value
                        scaleY = artworkScale.value
                    }
            ) {
                // Large edge-to-edge artwork image with loading skeleton
                if (!resolvedArtworkUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedArtworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        },
                        error = {
                            ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                        }
                    )
                } else {
                    ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                }

                // Glowing Organic Curved Wave Transition into Dark Background
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = brushFadeProgress.value
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Path for the curved wave cutting into the background
                    val wavePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.70f)
                        cubicTo(
                            w * 0.28f, h * 0.80f,
                            w * 0.44f, h * 0.94f,
                            w * 0.66f, h * 0.90f
                        )
                        cubicTo(
                            w * 0.80f, h * 0.87f,
                            w * 0.90f, h * 0.80f,
                            w, h * 0.81f
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    // Fill below the wave with the screen background color
                    drawPath(
                        path = wavePath,
                        color = Color(0xFF070204)
                    )

                    // Stroke along the wave edge for the vibrant crimson neon glow
                    val strokePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.70f)
                        cubicTo(
                            w * 0.28f, h * 0.80f,
                            w * 0.44f, h * 0.94f,
                            w * 0.66f, h * 0.90f
                        )
                        cubicTo(
                            w * 0.80f, h * 0.87f,
                            w * 0.90f, h * 0.80f,
                            w, h * 0.81f
                        )
                    }

                    // Layer 1: Soft diffuse ambient glow
                    drawPath(
                        path = strokePath,
                        color = Color(0x35FF1838),
                        style = Stroke(width = 20f, cap = StrokeCap.Round)
                    )
                    // Layer 2: Vivid mid glow
                    drawPath(
                        path = strokePath,
                        color = Color(0x80FF2448),
                        style = Stroke(width = 7f, cap = StrokeCap.Round)
                    )
                    // Layer 3: Crisp high-luminance core edge
                    drawPath(
                        path = strokePath,
                        color = Color(0xFFFF5270),
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }
            }

            // 2. TRACK HEADER: Left Album Thumbnail + Right Track Metadata & Soundwave
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Keep the thumbnail closer to the left edge like the reference.
                    .graphicsLayer {
                        alpha = trackInfoAlpha.value
                        translationY = trackInfoOffsetY.value - 10f
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail (Left Side)
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141418))
                        .border(1.dp, Color(0x33FF2448), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!resolvedArtworkUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(resolvedArtworkUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = result.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                            },
                            success = {
                                SubcomposeAsyncImageContent()
                            },
                            error = {
                                ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                            }
                        )
                    } else {
                        ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier .width(12.dp))

                // Track Metadata Column (Right Side)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 5-bar crimson soundwave indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(3.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                    }

                    // Prominent Artist Name
                    Text(
                        text = result.artist,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Song Title
                    Text(
                        text = result.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Album / Subtitle Metadata
                    if (result.album.isNotBlank() && !result.album.equals(result.title, ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = result.album,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. ACTION CONTROLS (Play | Copy Title | Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = actionsAlpha.value
                        translationY = actionsOffsetY.value
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button (Brand gradient pill)
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF2448), Color(0xFFD51035))
                            )
                        )
                        .clickable(onClick = onPlayInVelvet)
                        .testTag("sing_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Copy Title Button (Dark glass pill with subtle border)
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141418))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable {
                            val textToCopy = "${result.artist} - ${result.title}"
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "Title copied", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("copy_title_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Title",
                            tint = Color(0xFFE5DEE0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Title",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE5DEE0)
                        )
                    }
                }

                // Share Button (Dark glass circular button)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141418))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val shareUrl = result.spotifyUrl ?: result.youtubeMusicUrl ?: result.appleMusicUrl ?: ""
                                putExtra(Intent.EXTRA_SUBJECT, "${result.artist} - ${result.title}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Found this song via Velvet: ${result.artist} - ${result.title}${if (shareUrl.isNotBlank()) "\n$shareUrl" else ""}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                        .testTag("sing_share_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFFE5DEE0),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. "AVAILABLE ON" HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Available on",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Open in your preferred music app",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. FULL-WIDTH PLATFORMS LIST (Stretched full width with no horizontal padding)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                platforms.forEachIndexed { index, platform ->
                    NativePlatformRow(
                        platform = platform,
                        alpha = platformAlphas[index].value,
                        offsetY = platformOffsetsY[index].value,
                        onClick = { platform.launch(context) }
                    )
                }
            }
        }

        // TOP NAV OVERLAY: Circular Back Button (Left) and More Options (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding + 10.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Glass Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onClose)
                    .testTag("sing_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Circular Glass More Options / Diagnostics Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable { onViewDiagnostics?.invoke() }
                    .testTag("sing_more_options_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Single source of truth for streaming platforms.
 * Configured with the exact expected local file paths under /platform-logos/.
 */
data class MusicPlatformConfig(
    val id: String,
    val name: String,
    val logo: String,
    val subtitle: String
)

val MUSIC_PLATFORMS = listOf(
    MusicPlatformConfig(
        id = "youtube-music",
        name = "YouTube Music",
        logo = "/platform-logos/youtube-music.png",
        subtitle = "Stream on YouTube Music"
    ),
    MusicPlatformConfig(
        id = "spotify",
        name = "Spotify",
        logo = "/platform-logos/spotify.png",
        subtitle = "Stream on Spotify"
    ),
    MusicPlatformConfig(
        id = "apple-music",
        name = "Apple Music",
        logo = "/platform-logos/apple-music.png",
        subtitle = "Stream on Apple Music"
    ),
    MusicPlatformConfig(
        id = "audiomack",
        name = "Audiomack",
        logo = "/platform-logos/audiomack.png",
        subtitle = "Stream on Audiomack"
    ),
    MusicPlatformConfig(
        id = "soundcloud",
        name = "SoundCloud",
        logo = "/platform-logos/soundcloud.png",
        subtitle = "Stream on SoundCloud"
    ),
    MusicPlatformConfig(
        id = "boomplay",
        name = "Boomplay",
        logo = "/platform-logos/boomplay.png",
        subtitle = "Stream on Boomplay"
    )
)

/**
 * Atmospheric loading skeleton for album artwork.
 * Displays a pulsing velvet-crimson gradient while resolving or downloading high-res artwork.
 */
@Composable
private fun ArtworkLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "artwork_skeleton")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF24060E).copy(alpha = shimmerAlpha),
                        Color(0xFF130407).copy(alpha = shimmerAlpha),
                        Color(0xFF070204)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = Color(0x33FF2448),
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Native Platform Item representing one of the 5 streaming destinations.
 */
private data class NativePlatformItem(
    val config: MusicPlatformConfig,
    val directUrl: String?,
    val query: String
) {
    val id: String get() = config.id
    val name: String get() = config.name
    val logo: String get() = config.logo
    val subtitle: String get() = config.subtitle

    fun launch(context: Context) {
        val targetUrl = if (!directUrl.isNullOrBlank()) {
            directUrl
        } else {
            val encoded = Uri.encode(query)
            when (id) {
                "youtube-music" -> "https://music.youtube.com/search?q=$encoded"
                "spotify" -> "https://open.spotify.com/search/$encoded"
                "apple-music" -> "https://music.apple.com/search?term=$encoded"
                "audiomack" -> "https://audiomack.com/search?q=$encoded"
                "soundcloud" -> "https://soundcloud.com/search?q=$encoded"
                "boomplay" -> "https://www.boomplay.com/search/default-$encoded"
                else -> "https://www.google.com/search?q=$encoded"
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(context, "Cannot open $name", Toast.LENGTH_SHORT).show()
            }
    }
}

/**
 * Prominent local platform logo renderer.
 * Loads the platform logo from the exact local path (e.g., /platform-logos/youtube-music.png).
 * Does not crash if the file is not yet uploaded; uses a graceful dark glass fallback.
 * Once the files are uploaded to public/platform-logos/, they appear automatically.
 */
@Composable
private fun PlatformLogoView(
    platform: NativePlatformItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val relativeAssetPath = remember(platform.logo) { platform.logo.removePrefix("/") }
    val assetUri = "file:///android_asset/$relativeAssetPath"

    val assetExists = remember(relativeAssetPath) {
        runCatching {
            context.assets.open(relativeAssetPath).use { }
            true
        }.getOrDefault(false)
    }

    val localDiskFile = remember(relativeAssetPath) {
        listOf(
            File("public/$relativeAssetPath"),
            File(relativeAssetPath),
            File(context.filesDir, relativeAssetPath)
        ).firstOrNull { it.exists() }
    }

    val imageModel: Any = when {
        localDiskFile != null -> localDiskFile
        else -> assetUri
    }

    var isLoaded by remember { mutableStateOf(false) }

    Box(
        // The uploaded asset supplies its own branded shape/background.
        // Do not wrap it in another card or border.
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoaded) {
            // Elegant, non-crashing fallback placeholder with initial letter
            Text(
                text = platform.name.take(1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC0BAC0)
            )
        }

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = platform.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp)),
            onSuccess = { isLoaded = true },
            onError = { isLoaded = false }
        )
    }
}

/**
 * Full-width native platform row stretching edge-to-edge with prominent icon, typography, and chevron.
 */
@Composable
private fun NativePlatformRow(
    platform: NativePlatformItem,
    alpha: Float,
    offsetY: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RectangleShape,
        // Transparent row: no contrasting strip behind each platform.
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformLogoView(
                platform = platform
            )

            Spacer(modifier = Modifier .width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = platform.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = platform.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF6B6B75),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Very settled, clean retry / microphone spinner.
 * Features a quiet, slow sweeping arc around a tranquil microphone button.
 */
@Composable
private fun SettledMicRetrySpinner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "settledSpinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRetry
                )
                .testTag("sing_refresh_mic_button")
                .testTag("sing_again_button"),
            contentAlignment = Alignment.Center
        ) {
            // Settled rotating spinner ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                rotate(rotation) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0x11FF2448),
                                Color(0x55FF2448),
                                Color(0xFFFF2448),
                                Color(0x11FF2448)
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Settled central inner disk with mic icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33180004))
                    .border(1.dp, Color(0x44FF2448), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Tap to search again",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap to search again",
            fontSize = 12.sp,
            color = Color(0x99FFF1F2),
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Notice view for "Couldn't identify that song" and "Couldn't connect".
 * Clean, tranquil, and uncrowded.
 */
@Composable
private fun SeamlessNoticeResultView(
    title: String,
    message: String,
    testTag: String,
    topPadding: Dp,
    bottomPadding: Dp,
    onViewDiagnostics: () -> Unit,
    onRestartMic: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding, bottom = bottomPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x26FF2448))
                    .border(1.2.dp, Color(0x55FF2448), CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = Color(0x66FF2448)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFFFF405A),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFF1F2),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFFB5A7AA),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Diagnostic button directly exposed on notice/error screen
            OutlinedButton(
                onClick = onViewDiagnostics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp)
                    .testTag("view_diagnostic_details_button"),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0x55FF2448)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x19FF2448),
                    contentColor = Color(0xFFFF8A9E)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFF6B85)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View diagnostic details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settled Microphone Spinner for retry
            SettledMicRetrySpinner(
                onRetry = onRestartMic
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Minimal Dismiss button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x22180004))
                    .border(1.dp, Color(0x33D51035), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                    .testTag("sing_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Sing It",
                    tint = Color(0xFFD7D0D2),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
