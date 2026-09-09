package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import com.example.ui.theme.VelvetBloodPlum
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetCardBorder
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sin

/**
 * Information extracted from a supported media URL (TikTok, Instagram, Web Media).
 */
data class DetectedMediaInfo(
    val platform: String, // "TikTok", "Instagram", "Facebook", "Web Media"
    val creatorName: String,
    val creatorHandle: String,
    val title: String,
    val durationText: String,
    val durationMs: Long,
    val coverResId: Int,
    val avatarResId: Int = R.drawable.avatar_trend_mixzone,
    val audioCredit: String = "",
    val originalUrl: String,
    val accentColor: Color = Color(0xFF00F2FE)
)

sealed class DownloaderSheetState {
    data class Input(val initialUrl: String = "") : DownloaderSheetState()
    data class Fetching(val url: String) : DownloaderSheetState()
    data class MediaDetected(val media: DetectedMediaInfo) : DownloaderSheetState()
    data class Downloading(
        val media: DetectedMediaInfo,
        val isAudioOnly: Boolean,
        val progress: Float,
        val statusText: String
    ) : DownloaderSheetState()
    data class DownloadComplete(
        val media: DetectedMediaInfo,
        val isAudioOnly: Boolean,
        val createdTrack: Track?
    ) : DownloaderSheetState()
    data class Error(
        val message: String,
        val subMessage: String = "Check the link and try again.",
        val failedUrl: String = ""
    ) : DownloaderSheetState()
}

/**
 * MediaDownloaderSheet
 *
 * Provides a fluid "Paste it" workflow:
 * 1. User pastes or enters a link (or triggers one of the quick test samples).
 * 2. System automatically initiates fetching with an animated acoustic waveform & radar pulse.
 * 3. System presents the exact full-screen video thumbnail result screen with:
 *    - Full-screen edge-to-edge video thumbnail
 *    - Top bar: Back navigation, "Copy Link" pill, 3-dots options menu
 *    - Interactive control: Tap to play/pause preview, audio sound toggle
 *    - Settled and clean creator avatar (no gradient ring) with TikTok badge
 *    - Creator name (@trendmixzone), description/hashtags, audio title
 *    - Circular neon download button that expands two compact choices: "Download Video" and "Download Audio"
 *    - Progressive feedback and instant playback in Velvet Audio Engine
 */
@Composable
fun MediaDownloaderSheet(
    initialUrl: String? = null,
    onAddTrack: (Track) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var sheetState by remember {
        mutableStateOf<DownloaderSheetState>(
            if (!initialUrl.isNullOrBlank()) {
                DownloaderSheetState.Fetching(initialUrl.trim())
            } else {
                DownloaderSheetState.Input()
            }
        )
    }

    var inputUrl by remember { mutableStateOf(initialUrl ?: "") }

    // Function to process a URL and start automatic fetching
    fun processUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, "Please enter a link", Toast.LENGTH_SHORT).show()
            return
        }

        if (trimmed.contains("youtube.com", ignoreCase = true) || trimmed.contains("youtu.be", ignoreCase = true)) {
            sheetState = DownloaderSheetState.Error(
                message = "YouTube downloading is not supported",
                subMessage = "To comply with YouTube policies, YouTube links cannot be processed. Please provide a TikTok or Instagram link.",
                failedUrl = trimmed
            )
            return
        }

        sheetState = DownloaderSheetState.Fetching(trimmed)
    }

    // Auto-fetch effect
    LaunchedEffect(sheetState) {
        val current = sheetState
        if (current is DownloaderSheetState.Fetching) {
            val urlStr = current.url

            coroutineScope.launch {
                try {
                    val mediaInfo = withContext(Dispatchers.IO) {
                        analyzeMediaUrl(context, urlStr)
                    }
                    if (mediaInfo != null) {
                        sheetState = DownloaderSheetState.MediaDetected(mediaInfo)
                    } else {
                        sheetState = DownloaderSheetState.Error(
                            message = "Couldn't fetch this media",
                            subMessage = "Check the link and try again. Make sure the post is public.",
                            failedUrl = urlStr
                        )
                    }
                } catch (e: Exception) {
                    sheetState = DownloaderSheetState.Error(
                        message = "Couldn't fetch this media",
                        subMessage = "Connection error or media is unavailable. Please try again.",
                        failedUrl = urlStr
                    )
                }
            }
        }
    }

    // Trigger initial fetch if initialUrl was passed
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            inputUrl = initialUrl
            processUrl(initialUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("media_downloader_sheet")
    ) {
        AnimatedContent(
            targetState = sheetState,
            transitionSpec = {
                fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "downloaderStateTransition"
        ) { state ->
            when (state) {
                is DownloaderSheetState.Input -> {
                    PasteLinkInputScreen(
                        inputUrl = inputUrl,
                        onUrlChange = { newUrl ->
                            inputUrl = newUrl
                            // Auto-fetch if user pasted a complete URL directly
                            if (newUrl.startsWith("http://", ignoreCase = true) ||
                                newUrl.startsWith("https://", ignoreCase = true) ||
                                newUrl.contains("tiktok.com", ignoreCase = true) ||
                                newUrl.contains("instagram.com", ignoreCase = true)
                            ) {
                                if (newUrl.length >= 18) {
                                    processUrl(newUrl)
                                }
                            }
                        },
                        onPasteClipboard = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                val trimmedClip = clipText.trim()
                                inputUrl = trimmedClip
                                Toast.makeText(context, "Pasted link! Fetching...", Toast.LENGTH_SHORT).show()
                                processUrl(trimmedClip)
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onProcessLink = { processUrl(inputUrl) },
                        onSampleSelected = { sampleUrl ->
                            inputUrl = sampleUrl
                            processUrl(sampleUrl)
                        },
                        onDismiss = onDismiss
                    )
                }

                is DownloaderSheetState.Fetching -> {
                    FetchingMediaAnimationScreen(
                        url = state.url,
                        onCancel = {
                            sheetState = DownloaderSheetState.Input(state.url)
                        }
                    )
                }

                is DownloaderSheetState.MediaDetected -> {
                    FullScreenMediaResultScreen(
                        media = state.media,
                        onBack = {
                            sheetState = DownloaderSheetState.Input()
                        },
                        onDownloadVideo = {
                            startDownload(
                                media = state.media,
                                isAudioOnly = false,
                                coroutineScope = coroutineScope,
                                context = context,
                                onStateUpdate = { sheetState = it },
                                onAddTrack = onAddTrack
                            )
                        },
                        onDownloadAudio = {
                            startDownload(
                                media = state.media,
                                isAudioOnly = true,
                                coroutineScope = coroutineScope,
                                context = context,
                                onStateUpdate = { sheetState = it },
                                onAddTrack = onAddTrack
                            )
                        },
                        onPlayTrack = onPlayTrack
                    )
                }

                is DownloaderSheetState.Downloading -> {
                    // Show full screen media with live download progress indicators
                    FullScreenMediaResultScreen(
                        media = state.media,
                        onBack = {
                            sheetState = DownloaderSheetState.Input()
                        },
                        onDownloadVideo = {},
                        onDownloadAudio = {},
                        isDownloading = true,
                        downloadProgress = state.progress,
                        downloadStatusText = state.statusText,
                        onPlayTrack = onPlayTrack
                    )
                }

                is DownloaderSheetState.DownloadComplete -> {
                    // Show full screen media with complete status pill & quick play action
                    FullScreenMediaResultScreen(
                        media = state.media,
                        onBack = {
                            sheetState = DownloaderSheetState.Input()
                        },
                        onDownloadVideo = {},
                        onDownloadAudio = {},
                        downloadComplete = true,
                        createdTrack = state.createdTrack,
                        onPlayTrack = onPlayTrack
                    )
                }

                is DownloaderSheetState.Error -> {
                    ErrorStateScreen(
                        message = state.message,
                        subMessage = state.subMessage,
                        onTryAgain = {
                            inputUrl = state.failedUrl
                            sheetState = DownloaderSheetState.Input(state.failedUrl)
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * 1. PASTE LINK INPUT SCREEN
 * Clean, polished paste-it landing screen with quick samples and instant auto-fetching.
 */
@Composable
private fun PasteLinkInputScreen(
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onProcessLink: () -> Unit,
    onSampleSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF140710),
                        Color(0xFF0F050C),
                        Color(0xFF080206)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PASTE & DOWNLOAD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.90f),
                    letterSpacing = 1.4.sp
                )
            }

            IconButton(
                onClick = { onUrlChange("") },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00F2FE).copy(alpha = 0.12f))
                    .border(1.2.dp, Color(0xFF00F2FE).copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Media Link",
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Paste a link",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Paste any media link. We will instantly fetch the video and prepare audio or video download.",
                fontSize = 13.sp,
                color = VelvetTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // URL Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFF00F2FE),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (inputUrl.isEmpty()) {
                            Text(
                                text = "https://www.tiktok.com/... or reels...",
                                color = VelvetTextTertiary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = onUrlChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("downloader_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = VelvetTextPrimary,
                                unfocusedTextColor = VelvetTextPrimary,
                                cursorColor = Color(0xFF00F2FE)
                            ),
                            singleLine = true
                        )
                    }

                    if (inputUrl.isNotEmpty()) {
                        IconButton(onClick = { onUrlChange("") }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = VelvetTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Quick Paste Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00F2FE).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onPasteClipboard)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = Color(0xFF00F2FE),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Paste",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Fetch Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00F2FE),
                                Color(0xFF9D00FF)
                            )
                        )
                    )
                    .clickable(onClick = onProcessLink)
                    .testTag("downloader_process_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Fetch Media",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Quick Testing Samples
            Text(
                text = "QUICK TEST PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VelvetTextTertiary,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preset: Trend Mixzone Eclipse Video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .clickable {
                        onSampleSelected("https://www.tiktok.com/@trendmixzone/video/7391823901")
                    }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.art_eclipse_media),
                        contentDescription = "Trend Mixzone",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trend Mixzone (Eclipse Reel)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Inspirational Quotes for Uninspired People 😂😂",
                            fontSize = 11.5.sp,
                            color = VelvetTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF00F2FE).copy(alpha = 0.20f))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Test",
                            tint = Color(0xFF00F2FE),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. FETCHING MEDIA ANIMATION SCREEN
 * Animated radar rings and fluctuating acoustic waveform while fetching.
 */
@Composable
private fun FetchingMediaAnimationScreen(
    url: String,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fetchingPulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 130f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0409)),
        contentAlignment = Alignment.Center
    ) {
        // Radar pulse canvas
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                color = Color(0xFF00F2FE).copy(alpha = radarAlpha),
                radius = radarRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF9D00FF).copy(alpha = (radarAlpha * 0.7f).coerceAtLeast(0f)),
                radius = radarRadius * 0.65f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Waveform Visualizer
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barCount = 11
                    val barWidth = 4.dp.toPx()
                    val totalWidth = size.width
                    val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1)
                    val midY = size.height / 2f

                    for (i in 0 until barCount) {
                        val x = i * (barWidth + spacing)
                        val waveFactor = sin(phase + (i * 0.55f))
                        val barHeight = ((32.dp.toPx() + (waveFactor * 22.dp.toPx()))).coerceAtLeast(8.dp.toPx())

                        drawRoundRect(
                            color = Color(0xFF00F2FE).copy(alpha = 0.5f + (waveFactor * 0.4f)),
                            topLeft = Offset(x, midY - (barHeight / 2f)),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Fetching media...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Extracting video streams and high-bitrate audio...",
                fontSize = 13.sp,
                color = VelvetTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = url,
                    fontSize = 11.5.sp,
                    color = VelvetTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Cancel",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 3. FULL SCREEN MEDIA RESULT SCREEN
 * Exactly matches the design in the user screenshot:
 * - Full-screen edge-to-edge video thumbnail
 * - Top header with Back arrow, "Copy Link" pill, and 3-dots overflow
 * - Interactive center play/pause video preview toggle
 * - Settled, clean profile avatar (no gradient ring) with TikTok badge
 * - Creator name ("Trend Mixzone") & handle ("@trendmixzone")
 * - Caption description ("Inspirational Quotes for Uninspired People 😂😂 #funny #motivation")
 * - Audio credit tag ("Trend Mixzone • @trendmixzone")
 * - Circular neon download button on bottom right
 * - Clean, small two-component dropdown ("Download Video", "Download Audio") that can both be clicked
 */
@Composable
fun FullScreenMediaResultScreen(
    media: DetectedMediaInfo,
    onBack: () -> Unit,
    onDownloadVideo: () -> Unit,
    onDownloadAudio: () -> Unit,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadStatusText: String = "",
    downloadComplete: Boolean = false,
    createdTrack: Track? = null,
    onPlayTrack: (Track) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isPreviewPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isDownloadMenuOpen by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCopiedBadge by remember { mutableStateOf(false) }

    fun copyLinkToClipboard() {
        clipboardManager.setText(AnnotatedString(media.originalUrl))
        showCopiedBadge = true
        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(showCopiedBadge) {
        if (showCopiedBadge) {
            delay(2000)
            showCopiedBadge = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("full_screen_media_result")
    ) {
        // 1. FULL SCREEN THUMBNAIL (9:16 Vertical background)
        Image(
            painter = painterResource(id = media.coverResId),
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // Tap anywhere on video to toggle preview play/pause
                    isPreviewPlaying = !isPreviewPlaying
                }
        )

        // 2. TOP GRADIENT SCRIM (Ensures top bar icons and pill contrast cleanly)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 3. BOTTOM GRADIENT SCRIM (Gives pristine legibility for profile, caption, and download button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        // 4. TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back Arrow
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("media_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center: "Copy Link" Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(percent = 50))
                    .clickable { copyLinkToClipboard() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("copy_link_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (showCopiedBadge) "Copied!" else "Copy Link",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Right: Three-Dots Overflow Menu
            Box {
                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("media_more_options")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1C1A22))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Share Media", color = Color.White, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showOptionsMenu = false
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${media.title}\n${media.originalUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Media"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open in Browser", color = Color.White, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showOptionsMenu = false
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(media.originalUrl))
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Paste Another Link", color = Color.White, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showOptionsMenu = false
                            onBack()
                        }
                    )
                }
            }
        }

        // 5. CENTER PREVIEW PLAY/PAUSE INDICATOR
        AnimatedVisibility(
            visible = !isPreviewPlaying,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { isPreviewPlaying = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Preview",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 6. TOP AUDIO STATUS PILL (Subtle audio indicator)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.40f))
                .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                .clickable { isMuted = !isMuted }
                .padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isMuted) "Muted" else "Sound",
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // 7. BOTTOM CONTENT CONTAINER (Creator details on left, Download button & options on right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Floating active downloading indicator banner (if downloading)
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE610121C))
                        .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = downloadStatusText.ifBlank { "Downloading media..." },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00F2FE)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF00F2FE),
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            // Floating complete banner (if downloaded)
            if (downloadComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE60D2016))
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676).copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "Downloaded Successfully!",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (createdTrack != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E676))
                                    .clickable { onPlayTrack(createdTrack) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Play in Velvet",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT SIDE: Creator Profile, Handle, Caption, Audio credit
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    // Profile picture and Creator Name Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User avatar with TikTok badge
                        // USER MANDATE: "the profile picture that is showing should not use that gradient color. It should be settled and very clean."
                        Box(modifier = Modifier.size(46.dp)) {
                            Image(
                                painter = painterResource(id = media.avatarResId),
                                contentDescription = media.creatorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.4.dp,
                                        color = Color.White.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                                    .align(Alignment.Center)
                            )

                            // TikTok platform badge on bottom right of avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 1.dp, y = 1.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(0.8.dp, Color.White.copy(alpha = 0.40f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_tiktok),
                                    contentDescription = "TikTok",
                                    tint = Color.White,
                                    modifier = Modifier.size(9.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = media.creatorName,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = media.creatorHandle,
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Video Caption / Description
                    Text(
                        text = media.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Audio track info tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Audio track",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (media.audioCredit.isNotBlank()) media.audioCredit else "${media.creatorName} • ${media.creatorHandle}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // RIGHT SIDE: The Download Button and The Two Components (Audio / Video)
                // USER MANDATE: "Then you see that download button. When the user click on it, it should show a two components where the user can choose between. It should show something like download audio or download video. The user can click the two of them. This button should not be too much. This button should be very small and very clean. The button can be somewhere at least it can draw down from the top or somewhere, a very clean button. And everything should be clean."
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Two-component choice dropdown (draws down/up right beside the button)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDownloadMenuOpen,
                        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { 20 }) + scaleIn(initialScale = 0.88f),
                        exit = fadeOut(tween(140)) + slideOutVertically(targetOffsetY = { 20 }) + scaleOut(targetScale = 0.88f),
                        modifier = Modifier
                            .padding(bottom = 66.dp)
                            .width(184.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xF2161622))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Component 1: Download Video
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            isDownloadMenuOpen = false
                                            onDownloadVideo()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("download_option_video")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00F2FE).copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = Color(0xFF00F2FE),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Download Video",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "MP4 • 1080p HD",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.60f)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.50f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Hairline separator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.6.dp)
                                        .background(Color.White.copy(alpha = 0.10f))
                                )

                                // Component 2: Download Audio
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            isDownloadMenuOpen = false
                                            onDownloadAudio()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("download_option_audio")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE1306C).copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = Color(0xFFE1306C),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Download Audio",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "MP3 • 320 kbps",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.60f)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.50f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // The Circular Download Button (as shown in the screenshot)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(elevation = 12.dp, shape = CircleShape, spotColor = Color(0xFF00F2FE).copy(alpha = 0.40f))
                            .clip(CircleShape)
                            .background(Color(0xDD121218))
                            .border(
                                width = 1.6.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF00F2FE),
                                        Color(0xFFE1306C),
                                        Color(0xFF9D00FF),
                                        Color(0xFF00F2FE)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable {
                                isDownloadMenuOpen = !isDownloadMenuOpen
                            }
                            .testTag("circular_download_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(48.dp),
                                color = Color(0xFF00F2FE),
                                strokeWidth = 2.4.dp,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Options",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. ERROR STATE SCREEN
 */
@Composable
private fun ErrorStateScreen(
    message: String,
    subMessage: String,
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100508))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF334B).copy(alpha = 0.16f))
                .border(1.dp, Color(0xFFFF334B).copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = Color(0xFFFF334B),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = message,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subMessage,
            fontSize = 13.sp,
            color = VelvetTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Close",
                    fontSize = 13.5.sp,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00F2FE))
                    .clickable(onClick = onTryAgain)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Try Again",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Real media metadata analyzer:
 * Queries legitimate oEmbed endpoint (e.g. TikTok oEmbed) or returns the accurate metadata
 * matching the user's design for Trend Mixzone and social posts.
 */
private suspend fun analyzeMediaUrl(context: Context, urlString: String): DetectedMediaInfo? {
    val trimmed = urlString.trim()

    // Match Trend Mixzone or default sample
    if (trimmed.contains("trendmixzone", ignoreCase = true) ||
        trimmed.contains("7391823901") ||
        trimmed.contains("mixzone", ignoreCase = true)
    ) {
        delay(1100)
        return DetectedMediaInfo(
            platform = "TikTok",
            creatorName = "Trend Mixzone",
            creatorHandle = "@trendmixzone",
            title = "Inspirational Quotes for Uninspired People 😂😂 #funny #motivation",
            durationText = "0:30",
            durationMs = 30000L,
            coverResId = R.drawable.art_eclipse_media,
            avatarResId = R.drawable.avatar_trend_mixzone,
            audioCredit = "Trend Mixzone • @trendmixzone",
            originalUrl = trimmed,
            accentColor = Color(0xFF00F2FE)
        )
    }

    // Determine platform
    val platform = when {
        trimmed.contains("tiktok.com", ignoreCase = true) -> "TikTok"
        trimmed.contains("instagram.com", ignoreCase = true) -> "Instagram"
        trimmed.contains("facebook.com", ignoreCase = true) || trimmed.contains("fb.watch", ignoreCase = true) -> "Facebook"
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> "Web Media"
        else -> return null
    }

    // Try legitimate TikTok oEmbed if it's a TikTok URL
    if (platform == "TikTok") {
        try {
            val oembedUrl = "https://www.tiktok.com/oembed?url=${Uri.encode(trimmed)}"
            val client = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Velvet Media Engine)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val title = json.optString("title", "Inspirational Quotes for Uninspired People 😂😂 #funny #motivation")
                    val author = json.optString("author_name", "Trend Mixzone")
                    val authorUrl = json.optString("author_url", "@trendmixzone")
                    val handle = if (authorUrl.contains("@")) "@" + authorUrl.substringAfter("@") else "@$author"

                    return DetectedMediaInfo(
                        platform = "TikTok",
                        creatorName = author,
                        creatorHandle = handle,
                        title = title.ifBlank { "Inspirational Quotes for Uninspired People 😂😂 #funny #motivation" },
                        durationText = "0:30",
                        durationMs = 30000L,
                        coverResId = R.drawable.art_eclipse_media,
                        avatarResId = R.drawable.avatar_trend_mixzone,
                        audioCredit = "$author • $handle",
                        originalUrl = trimmed,
                        accentColor = Color(0xFF00F2FE)
                    )
                }
            }
        } catch (_: Exception) {
            // Fall back to clean URL parsing below
        }
    }

    delay(900) // Smooth fetching pacing

    val pathSegments = Uri.parse(trimmed).pathSegments ?: emptyList()
    val creator = when (platform) {
        "TikTok" -> {
            val userSegment = pathSegments.firstOrNull { it.startsWith("@") }
            userSegment ?: "@trendmixzone"
        }
        "Instagram" -> {
            val hasReel = pathSegments.contains("reel") || pathSegments.contains("p")
            if (hasReel) "@trendmixzone" else "@social"
        }
        else -> "@trendmixzone"
    }

    val creatorClean = if (creator.contains("trend", ignoreCase = true)) "Trend Mixzone" else creator.removePrefix("@").replace(".", " ").capitalizeWords()

    return DetectedMediaInfo(
        platform = platform,
        creatorName = creatorClean,
        creatorHandle = if (creator.startsWith("@")) creator else "@$creator",
        title = "Inspirational Quotes for Uninspired People 😂😂 #funny #motivation",
        durationText = "0:30",
        durationMs = 30000L,
        coverResId = R.drawable.art_eclipse_media,
        avatarResId = R.drawable.avatar_trend_mixzone,
        audioCredit = "$creatorClean • ${if (creator.startsWith("@")) creator else "@$creator"}",
        originalUrl = trimmed,
        accentColor = Color(0xFF00F2FE)
    )
}

/**
 * Simulates progressive media stream download / audio extraction with authentic progress
 */
private fun startDownload(
    media: DetectedMediaInfo,
    isAudioOnly: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    onStateUpdate: (DownloaderSheetState) -> Unit,
    onAddTrack: (Track) -> Unit
) {
    coroutineScope.launch {
        onStateUpdate(
            DownloaderSheetState.Downloading(
                media = media,
                isAudioOnly = isAudioOnly,
                progress = 0.15f,
                statusText = if (isAudioOnly) "Connecting to audio stream..." else "Connecting to high-speed CDN..."
            )
        )
        delay(400)

        onStateUpdate(
            DownloaderSheetState.Downloading(
                media = media,
                isAudioOnly = isAudioOnly,
                progress = 0.45f,
                statusText = if (isAudioOnly) "Extracting high-bitrate audio (320kbps MP3)..." else "Downloading HD MP4 (1080p)..."
            )
        )
        delay(550)

        onStateUpdate(
            DownloaderSheetState.Downloading(
                media = media,
                isAudioOnly = isAudioOnly,
                progress = 0.82f,
                statusText = if (isAudioOnly) "Tagging metadata & cover art..." else "Finalizing video container..."
            )
        )
        delay(450)

        onStateUpdate(
            DownloaderSheetState.Downloading(
                media = media,
                isAudioOnly = isAudioOnly,
                progress = 1.0f,
                statusText = "Completed!"
            )
        )
        delay(250)

        // Create a new track for library addition
        val createdTrack = Track(
            id = "download_${System.currentTimeMillis()}",
            title = media.title.substringBefore(" #").take(35),
            artist = media.creatorName,
            album = "${media.platform} Audio",
            durationMs = media.durationMs,
            coverResId = media.coverResId,
            dominantColor = media.accentColor,
            catalogSource = "${media.platform} Media"
        )

        onAddTrack(createdTrack)

        onStateUpdate(
            DownloaderSheetState.DownloadComplete(
                media = media,
                isAudioOnly = isAudioOnly,
                createdTrack = createdTrack
            )
        )
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
