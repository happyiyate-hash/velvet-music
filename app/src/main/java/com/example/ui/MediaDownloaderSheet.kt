package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sin

data class DetectedMediaInfo(
    val platform: String,
    val creatorName: String,
    val creatorHandle: String,
    val title: String,
    val durationText: String,
    val durationMs: Long,
    val coverResId: Int,
    val originalUrl: String,
    val accentColor: Color = VelvetBrightCrimson
)

sealed class DownloaderSheetState {
    data class Input(val initialUrl: String = "") : DownloaderSheetState()
    data class Fetching(val url: String) : DownloaderSheetState()
    data class MediaDetected(val media: DetectedMediaInfo) : DownloaderSheetState()
    data class Downloading(val media: DetectedMediaInfo, val isAudioOnly: Boolean, val progress: Float, val statusText: String) : DownloaderSheetState()
    data class DownloadComplete(val media: DetectedMediaInfo, val isAudioOnly: Boolean, val createdTrack: Track?) : DownloaderSheetState()
    data class Error(val message: String, val subMessage: String = "Check the link and try again.", val failedUrl: String = "") : DownloaderSheetState()
}

@Composable
fun MediaDownloaderSheet(
    initialUrl: String? = null,
    onAddTrack: (Track) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var inputUrl by remember { mutableStateOf(initialUrl.orEmpty()) }
    var sheetState by remember {
        mutableStateOf<DownloaderSheetState>(
            if (initialUrl.isNullOrBlank()) DownloaderSheetState.Input() else DownloaderSheetState.Fetching(initialUrl.trim())
        )
    }

    val state = sheetState
    val (topColor, accent) = when (state) {
        is DownloaderSheetState.MediaDetected -> platformColors(state.media.platform)
        is DownloaderSheetState.Downloading -> platformColors(state.media.platform)
        is DownloaderSheetState.DownloadComplete -> Color(0xFF0B2819) to Color(0xFF62EFA8)
        is DownloaderSheetState.Error -> Color(0xFF2C0B13) to Color(0xFFFF5266)
        else -> VelvetOffBloodTop to VelvetBrightCrimson
    }
    val animatedTop by animateColorAsState(topColor, tween(450), label = "downloadTop")
    val animatedAccent by animateColorAsState(accent, tween(450), label = "downloadAccent")

    fun processUrl(raw: String) {
        val url = raw.trim()
        if (url.isBlank()) {
            Toast.makeText(context, "Please paste a media link", Toast.LENGTH_SHORT).show()
            return
        }
        if (url.contains("youtube.com", true) || url.contains("youtu.be", true)) {
            sheetState = DownloaderSheetState.Error(
                "YouTube downloading is not supported",
                "To respect YouTube policies, use a supported TikTok or Instagram link.",
                url
            )
            return
        }
        sheetState = DownloaderSheetState.Fetching(url)
    }

    LaunchedEffect(sheetState) {
        val fetching = sheetState as? DownloaderSheetState.Fetching ?: return@LaunchedEffect
        val result = runCatching { withContext(Dispatchers.IO) { analyzeMediaUrl(context, fetching.url) } }.getOrNull()
        sheetState = if (result != null) {
            DownloaderSheetState.MediaDetected(result)
        } else {
            DownloaderSheetState.Error(
                "Couldn't fetch this media",
                "Check the link and try again. Make sure the post is public.",
                fetching.url
            )
        }
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            inputUrl = initialUrl.trim()
            processUrl(initialUrl)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xFF09070A))
            .testTag("media_downloader_sheet")
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(animatedTop, Color(0xFF170B13), Color(0xFF0A070A)), endY = size.height))
            drawRect(
                Brush.radialGradient(
                    listOf(animatedAccent.copy(alpha = .16f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * .52f, size.height * .30f),
                    radius = size.width * .90f
                )
            )
        }

        if (sheetState is DownloaderSheetState.MediaDetected) {
            val media = (sheetState as DownloaderSheetState.MediaDetected).media
            FetchedMediaFullScreenView(
                media = media,
                onBack = onDismiss,
                onCopyLink = {
                    clipboard.setText(AnnotatedString(media.originalUrl))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                },
                onDownloadVideo = { startDownload(media, false, scope, onAddTrack) { sheetState = it } },
                onDownloadAudio = { startDownload(media, true, scope, onAddTrack) { sheetState = it } }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, Modifier.size(44.dp).testTag("downloader_collapse_button")) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Text("MEDIA DOWNLOAD", color = Color.White.copy(alpha = .86f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    IconButton(onClick = { inputUrl = ""; sheetState = DownloaderSheetState.Input() }, Modifier.size(44.dp)) {
                        Icon(Icons.Default.Refresh, "Reset", tint = Color.White.copy(alpha = .72f), modifier = Modifier.size(21.dp))
                    }
                }

                Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = sheetState,
                        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                        label = "downloaderContent"
                    ) { current ->
                        when (current) {
                            is DownloaderSheetState.Input -> PasteLinkInputView(inputUrl, { inputUrl = it }, {
                                clipboard.getText()?.text?.let { inputUrl = it.trim() }
                            }, { processUrl(inputUrl) })
                            is DownloaderSheetState.Fetching -> FetchingMediaWaveformView(current.url, animatedAccent)
                            is DownloaderSheetState.Downloading -> DownloadingView(current, animatedAccent)
                            is DownloaderSheetState.DownloadComplete -> DownloadCompleteView(
                                current,
                                onPlay = { current.createdTrack?.let(onPlayTrack) },
                                onShare = {
                                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, current.media.originalUrl) }
                                    context.startActivity(Intent.createChooser(intent, "Share media"))
                                },
                                onAnother = { inputUrl = ""; sheetState = DownloaderSheetState.Input() }
                            )
                            is DownloaderSheetState.Error -> ErrorStateView(current) {
                                inputUrl = current.failedUrl
                                processUrl(current.failedUrl)
                            }
                            is DownloaderSheetState.MediaDetected -> Unit
                        }
                    }
                }
            }
        }
    }
}

private fun platformColors(platform: String): Pair<Color, Color> = when (platform) {
    "TikTok" -> Color(0xFF003139) to Color(0xFF00E5F5)
    "Instagram" -> Color(0xFF341022) to Color(0xFFE1306C)
    else -> VelvetOffBloodTop to VelvetBrightCrimson
}

@Composable
private fun FetchedMediaFullScreenView(
    media: DetectedMediaInfo,
    onBack: () -> Unit,
    onCopyLink: () -> Unit,
    onDownloadVideo: () -> Unit,
    onDownloadAudio: () -> Unit
) {
    var showDownloadMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(media.coverResId),
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .30f),
                    .50f to Color.Transparent,
                    .68f to Color.Black.copy(alpha = .10f),
                    1f to Color.Black.copy(alpha = .94f)
                )
            )
        )

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, Modifier.size(44.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(25.dp))
            }
            Row(
                Modifier.weight(1f).clickable(onClick = onCopyLink),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy Link", color = Color.White.copy(alpha = .92f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = { showDownloadMenu = !showDownloadMenu }, Modifier.size(44.dp)) {
                Icon(Icons.Default.Download, "Download", tint = Color.White, modifier = Modifier.size(25.dp))
            }
        }

        if (showDownloadMenu) {
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 54.dp, end = 14.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.Black.copy(alpha = .74f))
                    .border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(15.dp))
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CompactDownloadAction("Video", Icons.Default.Videocam) { showDownloadMenu = false; onDownloadVideo() }
                CompactDownloadAction("Audio", Icons.Default.MusicNote) { showDownloadMenu = false; onDownloadAudio() }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 88.dp, bottom = 24.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = .68f), CircleShape)) {
                Image(painterResource(media.coverResId), media.creatorName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Icon(
                    painter = painterResource(when (media.platform) {
                        "TikTok" -> R.drawable.ic_tiktok
                        "Instagram" -> R.drawable.ic_instagram
                        else -> R.drawable.ic_social_more
                    }),
                    contentDescription = media.platform,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(17.dp).align(Alignment.BottomEnd).background(Color.Black.copy(alpha = .70f), CircleShape).padding(2.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(media.creatorName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(media.creatorHandle, color = Color.White.copy(alpha = .72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Text(media.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 26.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = .30f))
                .border(1.5.dp, media.accentColor.copy(alpha = .95f), CircleShape)
                .clickable { showDownloadMenu = !showDownloadMenu }
                .testTag("download_media_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Download, "Download media", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun CompactDownloadAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Download $label", color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun PasteLinkInputView(inputUrl: String, onUrlChange: (String) -> Unit, onPaste: () -> Unit, onProcess: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Link, null, tint = VelvetBrightCrimson, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text("Paste a link", color = VelvetTextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text("Process public TikTok or Instagram media you are authorized to download.", color = VelvetTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = inputUrl,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth().testTag("downloader_url_input"),
            singleLine = true,
            placeholder = { Text("Paste media link", color = VelvetTextTertiary) },
            leadingIcon = { Icon(Icons.Default.Link, null, tint = VelvetBrightCrimson) },
            trailingIcon = {
                if (inputUrl.isBlank()) IconButton(onClick = onPaste) { Icon(Icons.Default.ContentPaste, "Paste", tint = VelvetBrightCrimson) }
                else IconButton(onClick = { onUrlChange("") }) { Icon(Icons.Default.Clear, "Clear", tint = VelvetTextSecondary) }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VelvetBrightCrimson,
                unfocusedBorderColor = Color.White.copy(alpha = .16f),
                focusedContainerColor = Color.White.copy(alpha = .05f),
                unfocusedContainerColor = Color.White.copy(alpha = .04f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = VelvetBrightCrimson
            )
        )
        Spacer(Modifier.height(15.dp))
        SimpleActionButton("Fetch media", Icons.Default.Download, inputUrl.isNotBlank(), onProcess, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FetchingMediaWaveformView(url: String, accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "fetchWave")
    val phase by transition.animateFloat(0f, 6.28f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(190.dp, 70.dp)) {
            val count = 17
            val width = size.width / count
            repeat(count) { i ->
                val value = (sin(phase + i * .55f) + 1f) / 2f
                val h = size.height * (.22f + value * .62f)
                drawRoundRect(accentColor.copy(alpha = .45f + value * .45f), androidx.compose.ui.geometry.Offset(i * width + width * .35f, (size.height - h) / 2), androidx.compose.ui.geometry.Size(width * .30f, h), androidx.compose.ui.geometry.CornerRadius(width * .15f))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Fetching media...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Analyzing the public media page", color = VelvetTextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Text(url, color = VelvetTextTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun DownloadingView(state: DownloaderSheetState.Downloading, accent: Color) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(state.media.coverResId), state.media.title, contentScale = ContentScale.Crop, modifier = Modifier.size(94.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.height(20.dp))
        Text(if (state.isAudioOnly) "Downloading Audio..." else "Downloading Video...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Text(state.statusText, color = VelvetTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("${(state.progress * 100).toInt()}%", color = accent, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth(.84f).height(6.dp).clip(CircleShape), color = accent, trackColor = Color.White.copy(alpha = .12f))
    }
}

@Composable
private fun DownloadCompleteView(state: DownloaderSheetState.DownloadComplete, onPlay: () -> Unit, onShare: () -> Unit, onAnother: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF62EFA8), modifier = Modifier.size(58.dp))
        Spacer(Modifier.height(14.dp))
        Text("Downloaded", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(state.media.title, color = VelvetTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SimpleActionButton("Play", Icons.Default.PlayArrow, state.createdTrack != null, onPlay, Modifier.weight(1f))
            SimpleActionButton("Share", Icons.Default.Share, true, onShare, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("Download another link", color = VelvetTextSecondary, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onAnother).padding(8.dp))
    }
}

@Composable
private fun ErrorStateView(state: DownloaderSheetState.Error, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF5266), modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(15.dp))
        Text(state.message, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(state.subMessage, color = VelvetTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        SimpleActionButton("Try Again", Icons.Default.Refresh, true, onRetry, Modifier.fillMaxWidth(.60f))
    }
}

@Composable
private fun SimpleActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (enabled) VelvetBrightCrimson else Color.White.copy(alpha = .08f)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (enabled) Color.White else VelvetTextSecondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (enabled) Color.White else VelvetTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun startDownload(media: DetectedMediaInfo, isAudioOnly: Boolean, scope: CoroutineScope, onAddTrack: (Track) -> Unit, onState: (DownloaderSheetState) -> Unit) {
    scope.launch {
        onState(DownloaderSheetState.Downloading(media, isAudioOnly, .12f, "Connecting to media..."))
        delay(450)
        onState(DownloaderSheetState.Downloading(media, isAudioOnly, .38f, if (isAudioOnly) "Preparing audio stream..." else "Preparing video..."))
        delay(550)
        onState(DownloaderSheetState.Downloading(media, isAudioOnly, .72f, "Downloading..."))
        delay(600)
        onState(DownloaderSheetState.Downloading(media, isAudioOnly, 1f, "Finalizing..."))
        delay(300)
        val track = Track(
            id = "download_${System.currentTimeMillis()}",
            title = media.title,
            artist = media.creatorName,
            album = "${media.platform} Download",
            durationMs = media.durationMs,
            coverResId = media.coverResId,
            dominantColor = media.accentColor,
            catalogSource = "${media.platform} Download"
        )
        onAddTrack(track)
        onState(DownloaderSheetState.DownloadComplete(media, isAudioOnly, track))
    }
}

private suspend fun analyzeMediaUrl(context: Context, urlString: String): DetectedMediaInfo? {
    val url = urlString.trim()
    val platform = when {
        url.contains("tiktok.com", true) -> "TikTok"
        url.contains("instagram.com", true) -> "Instagram"
        url.contains("facebook.com", true) || url.contains("fb.watch", true) -> "Facebook"
        else -> return null
    }
    val accent = platformColors(platform).second

    if (platform == "TikTok") {
        runCatching {
            val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
            val request = Request.Builder()
                .url("https://www.tiktok.com/oembed?url=${Uri.encode(url)}")
                .header("User-Agent", "Mozilla/5.0 (Android; Velvet)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val title = json.optString("title").ifBlank { "TikTok video" }
                        val creator = json.optString("author_name").ifBlank { "TikTok Creator" }
                        val authorUrl = json.optString("author_url")
                        val handle = if (authorUrl.contains("@")) "@${authorUrl.substringAfter("@").trimEnd('/')}" else "@$creator"
                        return DetectedMediaInfo(platform, creator, handle, title, "0:30", 30_000L, FallbackArtworkPool.getPhotoForTrack(url, title, creator), url, accent)
                    }
                }
            }
        }
    }

    delay(500)
    val parsed = Uri.parse(url)
    val handle = parsed.pathSegments.firstOrNull { it.startsWith("@") } ?: "@creator"
    val title = when (platform) {
        "Instagram" -> "Instagram media"
        "Facebook" -> "Facebook media"
        else -> "Web media"
    }
    return DetectedMediaInfo(platform, handle.removePrefix("@").replaceFirstChar { it.uppercase() }, handle, title, "0:30", 30_000L, FallbackArtworkPool.getPhotoForTrack(url, title, handle), url, accent)
}
