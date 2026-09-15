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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.sin

/**
 * Redesigned Sing It screen.
 *
 * The important visual change is intentional: the lower atmosphere is no longer
 * a solid red water-fill. It is a layered, translucent, slowly moving red smoke
 * field with large areas of black negative space, matching Velvet's luxury look.
 */
@Composable
fun SingToSearchSheetV2(
    libraryTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { HummingRecognitionEngine(scope) }
    val state by engine.state.collectAsState()
    val amplitude by engine.liveAmplitude.collectAsState()
    var demoIndex by remember { mutableIntStateOf(0) }

    BackHandler {
        engine.stopListening()
        onDismiss()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        engine.startListening(context, libraryTracks)
    }

    LaunchedEffect(Unit) {
        if (DeviceMediaManager.hasRecordAudioPermission(context)) {
            engine.startListening(context, libraryTracks)
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { engine.stopListening() }
    }

    val matched = (state as? HumRecognitionState.Matched)?.result
    val isMatched = matched != null
    val statusText = when (state) {
        is HumRecognitionState.Analyzing -> "Matching..."
        else -> "Listening..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030001))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val micY = maxHeight * 0.35f

            AtmosphericRedRibbons(
                amplitude = amplitude,
                modifier = Modifier.fillMaxSize()
            )

            if (!isMatched) {
                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.2.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = maxOf(topInset + 18.dp, 120.dp))
                )

                ListeningVisualizer(
                    amplitude = amplitude,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = micY - 62.dp),
                    onMicTap = {
                        if (state is HumRecognitionState.Listening) {
                            demoIndex++
                            engine.triggerDemoMatch(demoIndex, libraryTracks)
                        } else {
                            engine.startListening(context, libraryTracks)
                        }
                    }
                )

                CloseButton(
                    bottomPadding = bottomInset + 28.dp,
                    onClick = {
                        engine.stopListening()
                        onDismiss()
                    }
                )
            }

            AnimatedVisibility(
                visible = isMatched,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(260)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (matched != null) {
                    PremiumMatchSheet(
                        result = matched,
                        onPlayInVelvet = {
                            val track = matched.track ?: Track(
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
                            onPlayTrack(track)
                            onDismiss()
                        },
                        onHumAnother = { engine.startListening(context, libraryTracks) },
                        onDismiss = {
                            engine.stopListening()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmosphericRedRibbons(amplitude: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "velvet_atmosphere")
    val phaseA by transition.animateFloat(
        0f, 6.28318f,
        infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_a"
    )
    val phaseB by transition.animateFloat(
        6.28318f, 0f,
        infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_b"
    )
    val smoothAmplitude by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(180, easing = LinearEasing),
        label = "atmosphere_amp"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val baseY = h * 0.79f
        val motion = smoothAmplitude * 18f

        // Very soft vertical haze. It never becomes an opaque red block.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x08180007),
                    Color(0x221B0009),
                    Color(0x401D000B)
                ),
                startY = h * 0.48f,
                endY = h
            )
        )

        fun ribbon(
            yFactor: Float,
            heightFactor: Float,
            opacity: Float,
            speed: Float,
            thickness: Float
        ) {
            val path = Path()
            path.moveTo(-40f, h)
            val top = h * yFactor
            for (x in -40..w.toInt() + 40 step 10) {
                val nx = x / w
                val y = top +
                    sin(nx * 5.0f + phaseA * speed) * (h * heightFactor + motion) +
                    cos(nx * 8.0f + phaseB * speed) * (h * heightFactor * 0.45f)
                if (x == -40) path.lineTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
            }
            path.lineTo(w + 40f, h)
            path.close()
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00FF163C),
                        Color(0x00FF163C),
                        Color(0x12E0002D),
                        Color(0x00A90024)
                    ),
                    startY = top - h * 0.08f,
                    endY = h
                )
            )
            drawPath(
                path = path,
                color = Color(0x22D8002B).copy(alpha = opacity),
                style = Stroke(width = thickness)
            )
        }

        // Multiple very soft ribbons. The dark gaps between them are intentional.
        ribbon(0.78f, 0.045f, 0.55f, 0.45f, 24f)
        ribbon(0.70f, 0.065f, 0.42f, 0.30f, 18f)
        ribbon(0.86f, 0.055f, 0.36f, 0.58f, 28f)
        ribbon(0.63f, 0.035f, 0.25f, 0.22f, 14f)

        // Thin illuminated folds — deliberately sparse, not a solid outline.
        val highlight = Path()
        for (x in 0..w.toInt() step 8) {
            val nx = x / w
            val y = h * 0.80f +
                sin(nx * 4.3f + phaseA * 0.38f) * (h * 0.055f + motion * 0.5f) +
                cos(nx * 7.1f + phaseB * 0.25f) * h * 0.022f
            if (x == 0) highlight.moveTo(0f, y) else highlight.lineTo(x.toFloat(), y)
        }
        drawPath(
            highlight,
            color = Color(0x45FF3B5D),
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
        drawPath(
            highlight,
            color = Color(0x16FF123C),
            style = Stroke(width = 18f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ListeningVisualizer(
    amplitude: Float,
    modifier: Modifier,
    onMicTap: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val pulse by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        VoiceBars(amplitude, mirrored = true, modifier = Modifier.width(108.dp).height(82.dp))
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(104.dp)
                .clickable(onClick = onMicTap),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val base = size.minDimension * 0.34f
                for (i in 0..2) {
                    val r = base + i * 11.dp.toPx() + pulse * 4.dp.toPx()
                    drawCircle(
                        color = Color(0x25FF163D),
                        radius = r,
                        style = Stroke(width = if (i == 0) 2.2f else 1.1f)
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x38FF2448),
                            Color(0x22C5002B),
                            Color(0x05000000)
                        )
                    ),
                    radius = base + 8.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFF1744).copy(alpha = 0.78f + amplitude * 0.2f),
                    radius = base,
                    style = Stroke(width = 2.5f)
                )
            }
            Icon(
                Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color(0xFFFFF5F7),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        VoiceBars(amplitude, mirrored = false, modifier = Modifier.width(108.dp).height(82.dp))
    }
}

@Composable
private fun VoiceBars(amplitude: Float, mirrored: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val count = 17
        val center = (count - 1) / 2f
        val maxHeight = size.height * (0.38f + amplitude * 0.55f)
        for (i in 0 until count) {
            val distance = kotlin.math.abs(i - center) / center
            val taper = 1f - distance * 0.78f
            val h = (maxHeight * taper).coerceAtLeast(3.dp.toPx())
            val x = size.width * i / (count - 1f)
            val y1 = size.height / 2f - h / 2f
            val y2 = size.height / 2f + h / 2f
            drawLine(
                color = Color(0xFFFF1744).copy(alpha = 0.22f + taper * 0.65f),
                start = Offset(x, y1),
                end = Offset(x, y2),
                strokeWidth = if (distance < 0.25f) 2.8f else 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CloseButton(bottomPadding: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .align(Alignment.BottomCenter)
            .offset(y = -bottomPadding)
            .clip(CircleShape)
            .background(Color(0x35120008))
            .border(1.dp, Color(0x50FF3A59), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PremiumMatchSheet(
    result: HumMatchResult,
    onPlayInVelvet: () -> Unit,
    onHumAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF17151A), Color(0xFF111014), Color(0xFF0C0B0F))
                )
            )
            .border(1.dp, Color(0x22FFFFFF), sheetShape)
            .shadow(24.dp, sheetShape)
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .width(54.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x55FFFFFF))
            )
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(result.coverResId),
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${result.matchPercentage}% Match • Found",
                        color = Color(0xFFFF4564),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        result.title,
                        color = Color(0xFFF8F6F8),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(result.artist, color = Color(0xFFB7B2BA), fontSize = 15.sp)
                    Text(result.album, color = Color(0xFF77727B), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color(0xFFAAA5AD), modifier = Modifier.size(21.dp))
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "LISTEN ON",
                color = Color(0xFF77727B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PlatformButton("S", "Spotify", Color(0xFF1DB954)) { uriHandler.openUri(result.spotifyWebUrl) }
                PlatformButton("", "Apple Music", Color(0xFFFF375F)) { uriHandler.openUri(result.appleMusicUrl) }
                PlatformButton("▶", "YouTube", Color(0xFFFF1744)) { uriHandler.openUri(result.youtubeMusicUrl) }
                PlatformButton("A", "Audiomack", Color(0xFFFF9D16)) { uriHandler.openUri(result.audiomackUrl) }
            }

            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFF51D45), Color(0xFFD90D35))
                        )
                    )
                    .clickable(onClick = onPlayInVelvet),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play in Velvet Music", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onHumAnother)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color(0xFF96919A), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hum or Sing Another Song", color = Color(0xFFAAA5AE), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PlatformButton(label: String, name: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.94f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = if (label == "") 25.sp else 19.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            name,
            color = Color(0xFFB6B1B9),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
