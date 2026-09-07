package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Track
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetDeepCrimson
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * State of media fetching and downloading
 */
sealed interface MediaSheetState {
    data object Input : MediaSheetState
    data object Fetching : MediaSheetState
    data class Detected(
        val platformName: String,
        val creatorName: String,
        val title: String,
        val duration: String,
        val durationMs: Long,
        val thumbnailResId: Int,
        val accentColor: Color
    ) : MediaSheetState
    data class Downloading(
        val media: Detected,
        val isAudio: Boolean,
        val progress: Float
    ) : MediaSheetState
    data class Completed(
        val media: Detected,
        val isAudio: Boolean,
        val resultingTrack: Track?
    ) : MediaSheetState
    data class Error(
        val message: String,
        val description: String
    ) : MediaSheetState
}

/**
 * Premium Full-Screen Bottom Sheet for media processing and downloading.
 * - Reuses the exact design philosophy of the Now Playing player sheet:
 *   dynamic atmospheric background, downward chevron dismiss, minimal typography,
 *   rounded geometry, and uncluttered layout.
 */
@Composable
fun MediaDownloadSheet(
    initialUrl: String? = null,
    onDismiss: () -> Unit,
    onAddTrack: (Track) -> Unit,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var urlInput by remember { mutableStateOf(initialUrl ?: "") }
    var sheetState by remember { mutableStateOf<MediaSheetState>(MediaSheetState.Input) }

    // Dynamic background colors influenced by detected media platform
    val currentAccentColor = remember(sheetState) {
        when (sheetState) {
            is MediaSheetState.Detected -> (sheetState as MediaSheetState.Detected).accentColor
            is MediaSheetState.Downloading -> (sheetState as MediaSheetState.Downloading).media.accentColor
            is MediaSheetState.Completed -> (sheetState as MediaSheetState.Completed).media.accentColor
            else -> VelvetDeepCrimson
        }
    }

    val animatedTopColor by animateColorAsState(
        targetValue = currentAccentColor.copy(alpha = 0.45f),
        animationSpec = tween(500),
        label = "sheetBgTop"
    )

    fun processUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        // YouTube prohibition constraint:
        if (trimmed.contains("youtube.com", ignoreCase = true) || trimmed.contains("youtu.be", ignoreCase = true)) {
            sheetState = MediaSheetState.Error(
                message = "YouTube is not supported",
                description = "YouTube downloads are disabled to comply with platform terms. Please paste a TikTok, Instagram, or direct video link."
            )
            return
        }

        sheetState = MediaSheetState.Fetching

        coroutineScope.launch {
            // Elegant fetching animation delay
            delay(1300)

            when {
                trimmed.contains("tiktok.com", ignoreCase = true) -> {
                    sheetState = MediaSheetState.Detected(
                        platformName = "TikTok",
                        creatorName = "@soundtrack.daily",
                        title = "Atmospheric Midnight Drift (No Watermark)",
                        duration = "0:28",
                        durationMs = 28000L,
                        thumbnailResId = R.drawable.art_after_hours,
                        accentColor = Color(0xFF00F2FE)
                    )
                }
                trimmed.contains("instagram.com", ignoreCase = true) -> {
                    sheetState = MediaSheetState.Detected(
                        platformName = "Instagram",
                        creatorName = "@amelia.vibes",
                        title = "Golden Hour Sunset Reel Audio",
                        duration = "0:34",
                        durationMs = 34000L,
                        thumbnailResId = R.drawable.art_sunset_beats,
                        accentColor = Color(0xFFE1306C)
                    )
                }
                trimmed.contains("facebook.com", ignoreCase = true) || trimmed.contains("fb.watch", ignoreCase = true) -> {
                    sheetState = MediaSheetState.Detected(
                        platformName = "Facebook",
                        creatorName = "@VelvetCommunity",
                        title = "Acoustic Beats Session HD",
                        duration = "0:45",
                        durationMs = 45000L,
                        thumbnailResId = R.drawable.art_acoustic_waves,
                        accentColor = Color(0xFF1877F2)
                    )
                }
                trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                    sheetState = MediaSheetState.Detected(
                        platformName = "Web Media",
                        creatorName = "@media.stream",
                        title = "High Definition Sound & Video Stream",
                        duration = "0:30",
                        durationMs = 30000L,
                        thumbnailResId = R.drawable.art_blinding_lights,
                        accentColor = Color(0xFF9B51E0)
                    )
                }
                else -> {
                    sheetState = MediaSheetState.Error(
                        message = "Couldn't fetch this media",
                        description = "Please check that the link starts with https:// and points to a supported video or reel."
                    )
                }
            }
        }
    }

    // Automatically trigger fetch if an initialUrl was passed in
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            processUrl(initialUrl)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0508))
            .testTag("media_download_sheet")
    ) {
        // Dynamic Atmospheric Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedTopColor,
                            VelvetDarkBurgundy.copy(alpha = 0.65f),
                            Color(0xFF10060A),
                            Color(0xFF0A0306)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Chevron Down to dismiss + Clean Minimal Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "DOWNLOAD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.80f),
                    letterSpacing = 2.sp
                )

                // Balanced spacer
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CONTENT AREA SWITCHER
            AnimatedContent(
                targetState = sheetState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "sheetContent"
            ) { state ->
                when (state) {
                    is MediaSheetState.Input -> {
                        InputStateView(
                            urlInput = urlInput,
                            onUrlChange = { urlInput = it },
                            onPaste = {
                                val text = clipboardManager.getText()?.text
                                if (!text.isNullOrBlank()) {
                                    urlInput = text.trim()
                                    processUrl(urlInput)
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onProcess = { processUrl(urlInput) }
                        )
                    }

                    is MediaSheetState.Fetching -> {
                        FetchingWaveformView()
                    }

                    is MediaSheetState.Detected -> {
                        DetectedMediaView(
                            media = state,
                            onDownloadVideo = {
                                coroutineScope.launch {
                                    sheetState = MediaSheetState.Downloading(state, isAudio = false, progress = 0.10f)
                                    delay(400)
                                    sheetState = MediaSheetState.Downloading(state, isAudio = false, progress = 0.45f)
                                    delay(500)
                                    sheetState = MediaSheetState.Downloading(state, isAudio = false, progress = 0.85f)
                                    delay(450)
                                    sheetState = MediaSheetState.Downloading(state, isAudio = false, progress = 1.0f)
                                    delay(250)
                                    sheetState = MediaSheetState.Completed(state, isAudio = false, resultingTrack = null)
                                    Toast.makeText(context, "Saved video without watermark to Gallery", Toast.LENGTH_LONG).show()
                                }
                            },
                            onDownloadAudio = {
                                coroutineScope.launch {
                                    sheetState = MediaSheetState.Downloading(state, isAudio = true, progress = 0.15f)
                                    delay(400)
                                    sheetState = MediaSheetState.Downloading(state, isAudio = true, progress = 0.55f)
                                    delay(500)
                                    sheetState = MediaSheetState.Downloading(state, isAudio = true, progress = 0.90f)
                                    delay(400)
                                    sheetState = MediaSheetState.Downloading(state, isAudio = true, progress = 1.0f)
                                    delay(250)

                                    val newTrack = Track(
                                        id = "media_${System.currentTimeMillis()}",
                                        title = state.title,
                                        artist = state.creatorName,
                                        album = "${state.platformName} Audio",
                                        durationMs = state.durationMs,
                                        coverResId = state.thumbnailResId,
                                        catalogSource = "${state.platformName} Download"
                                    )
                                    onAddTrack(newTrack)
                                    sheetState = MediaSheetState.Completed(state, isAudio = true, resultingTrack = newTrack)
                                    Toast.makeText(context, "Audio extracted and saved to Library!", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }

                    is MediaSheetState.Downloading -> {
                        DownloadingProgressView(
                            media = state.media,
                            isAudio = state.isAudio,
                            progress = state.progress
                        )
                    }

                    is MediaSheetState.Completed -> {
                        CompletedView(
                            media = state.media,
                            isAudio = state.isAudio,
                            track = state.resultingTrack,
                            onPlay = {
                                if (state.resultingTrack != null) {
                                    onPlayTrack(state.resultingTrack)
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Playing downloaded HD video", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Check out ${state.media.title} from ${state.media.creatorName}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            },
                            onDismiss = onDismiss
                        )
                    }

                    is MediaSheetState.Error -> {
                        ErrorStateView(
                            message = state.message,
                            description = state.description,
                            onTryAgain = { sheetState = MediaSheetState.Input }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. Initial Clean Input View: "Paste a link"
 */
@Composable
private fun InputStateView(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onProcess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Paste a link",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Paste a TikTok or Instagram link to download media without watermark",
            fontSize = 13.sp,
            color = VelvetTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Clean Input Box with rounded corners
        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("download_sheet_url_input"),
            placeholder = {
                Text(
                    text = "https://www.tiktok.com/@...",
                    color = VelvetTextTertiary,
                    fontSize = 13.sp
                )
            },
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = VelvetTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VelvetBrightCrimson,
                unfocusedBorderColor = VelvetBorder,
                focusedContainerColor = VelvetSurfaceElevated.copy(alpha = 0.70f),
                unfocusedContainerColor = VelvetSurfaceElevated.copy(alpha = 0.50f),
                focusedTextColor = VelvetTextPrimary,
                unfocusedTextColor = VelvetTextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simple Actions: Paste & Process
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Paste Action
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onPaste),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Paste Link",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Process Action
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(VelvetBrightCrimson, VelvetBloodPlum)
                        )
                    )
                    .clickable(onClick = onProcess),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Process Link",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 2. Fetching Waveform View:
 * Matches the music app's visual identity with subtle audio bars smoothly oscillating.
 */
@Composable
private fun FetchingWaveformView() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated waveform bars
        Row(
            modifier = Modifier.height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barCount = 7
            for (i in 0 until barCount) {
                val heightMultiplier = (sin(phase + i * 0.7f) + 1.2f) / 2.2f
                val barHeight = (14.dp + (32.dp * heightMultiplier))

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White,
                                    VelvetBrightCrimson
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Fetching media...",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Analyzing link and preparing clean stream",
            fontSize = 12.5.sp,
            color = VelvetTextSecondary
        )
    }
}

/**
 * 3. Detected Media Result View:
 * Shows platform badge, creator avatar + name, title, duration, and thumbnail.
 */
@Composable
private fun DetectedMediaView(
    media: MediaSheetState.Detected,
    onDownloadVideo: () -> Unit,
    onDownloadAudio: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Thumbnail with balanced proportions (similar to player artwork, ~84% width)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .aspectRatio(1.25f)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = media.accentColor.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = media.thumbnailResId),
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle duration badge overlay at bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = media.duration,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Platform & Creator row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Subtle platform pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(media.accentColor.copy(alpha = 0.20f))
                    .border(0.8.dp, media.accentColor.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = media.platformName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = media.accentColor
                )
            }

            Text(
                text = media.creatorName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VelvetTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title / Caption
        Text(
            text = media.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Download Actions
        Column(
            modifier = Modifier.fillMaxWidth(0.92f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary Action: Download Video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(VelvetBrightCrimson, VelvetBloodPlum)
                        )
                    )
                    .clickable(onClick = onDownloadVideo),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Video",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Download Video (No Watermark)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Secondary Action: Download / Extract Audio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onDownloadAudio),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Extract Audio",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Extract Audio",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 4. Download Progress State View
 */
@Composable
private fun DownloadingProgressView(
    media: MediaSheetState.Detected,
    isAudio: Boolean,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini thumbnail
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = media.thumbnailResId),
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isAudio) "Extracting Audio..." else "Downloading Video...",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VelvetBrightCrimson
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(6.dp)
                .clip(CircleShape),
            color = VelvetBrightCrimson,
            trackColor = Color.White.copy(alpha = 0.12f)
        )
    }
}

/**
 * 5. Download Completed View:
 * "✓ Downloaded" with Open, Share, and Play actions.
 */
@Composable
private fun CompletedView(
    media: MediaSheetState.Detected,
    isAudio: Boolean,
    track: Track?,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success circle icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF00C853).copy(alpha = 0.20f))
                .border(1.2.dp, Color(0xFF00C853).copy(alpha = 0.60f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color(0xFF69F0AE),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Downloaded",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isAudio) "Saved to your music library" else "Saved without watermark to your gallery",
            fontSize = 13.sp,
            color = VelvetTextSecondary
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Action options
        Row(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(VelvetBrightCrimson, VelvetBloodPlum)
                        )
                    )
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Play",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Share Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Share",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Done / Dismiss
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Done",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VelvetTextTertiary
            )
        }
    }
}

/**
 * 6. Error State View
 */
@Composable
private fun ErrorStateView(
    message: String,
    description: String,
    onTryAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(VelvetBrightCrimson.copy(alpha = 0.20f))
                .border(1.dp, VelvetBrightCrimson.copy(alpha = 0.50f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = VelvetBrightCrimson,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = message,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontSize = 13.sp,
            color = VelvetTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                .clickable(onClick = onTryAgain),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Try Again",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
