package com.example.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetOffBloodTop
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import kotlinx.coroutines.delay

private enum class DownloadUiState { INPUT, FETCHING, RESULT, ERROR, DOWNLOADING, COMPLETE }

@Composable
fun MediaDownloadSheet(initialUrl: String? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val resolver = remember { MediaLinkResolver() }
    var url by remember(initialUrl) { mutableStateOf(initialUrl.orEmpty()) }
    var state by remember(initialUrl) { mutableStateOf(if (initialUrl.isNullOrBlank()) DownloadUiState.INPUT else DownloadUiState.FETCHING) }
    var media by remember { mutableStateOf<ResolvedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var dominant by remember { mutableStateOf(VelvetOffBloodTop) }

    fun fetch() { if (url.trim().isNotBlank()) { state = DownloadUiState.FETCHING; error = "" } }

    LaunchedEffect(initialUrl) { if (!initialUrl.isNullOrBlank()) fetch() }
    LaunchedEffect(state) {
        if (state == DownloadUiState.FETCHING && url.isNotBlank()) {
            delay(180)
            runCatching { resolver.resolve(url.trim()) }
                .onSuccess { media = it; state = DownloadUiState.RESULT }
                .onFailure { error = it.message ?: "Unable to fetch this media"; state = DownloadUiState.ERROR }
        }
    }

    fun download(source: String?, audio: Boolean) {
        if (source.isNullOrBlank()) { Toast.makeText(context, "This link does not expose a downloadable media file.", Toast.LENGTH_LONG).show(); return }
        state = DownloadUiState.DOWNLOADING; progress = .12f
        val request = DownloadManager.Request(Uri.parse(source))
            .setTitle(media?.title ?: "Velvet media")
            .setDescription(if (audio) "Downloading audio" else "Downloading video")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true).setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Velvet/${safeName(media?.title ?: "media")}.${if (audio) "m4a" else "mp4"}")
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request); progress = .35f
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show(); state = DownloadUiState.COMPLETE
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .46f)))
        AnimatedVisibility(visible = true, enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(360)) + fadeIn(tween(220)), exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280)) + fadeOut(tween(180))) {
            Column(Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Brush.verticalGradient(listOf(dominant.copy(alpha = .99f), Color(0xFF160D15), Color(0xFF09070A)))).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Close", tint = VelvetTextPrimary) }
                    Text("Media Download", color = VelvetTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(24.dp))
                AnimatedContent(state, label = "download_state") { current ->
                    when (current) {
                        DownloadUiState.INPUT -> InputContent(url, { url = it }, { url = clipboard.getText()?.text.orEmpty() }, ::fetch)
                        DownloadUiState.FETCHING -> FetchingContent()
                        DownloadUiState.RESULT -> ResultContent(media, { dominant = it }, ::download)
                        DownloadUiState.ERROR -> ErrorContent(error, ::fetch, { state = DownloadUiState.INPUT })
                        DownloadUiState.DOWNLOADING -> ProgressContent(progress)
                        DownloadUiState.COMPLETE -> CompleteContent()
                    }
                }
            }
        }
    }
}

@Composable private fun InputContent(url: String, onUrl: (String) -> Unit, onPaste: () -> Unit, onFetch: () -> Unit) {
    val valid = url.trim().startsWith("http://") || url.trim().startsWith("https://")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Paste a link", color = VelvetTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Process public media you are authorized to download.", color = VelvetTextSecondary, fontSize = 13.sp)
        OutlinedTextField(value = url, onValueChange = onUrl, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://...", color = VelvetTextSecondary) }, leadingIcon = { Icon(Icons.Default.Link, null, tint = VelvetTextSecondary) }, trailingIcon = { IconButton(onClick = onPaste) { Icon(Icons.Default.ContentPaste, "Paste", tint = VelvetBrightCrimson) } }, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VelvetBrightCrimson, unfocusedBorderColor = Color.White.copy(alpha = .16f), focusedTextColor = VelvetTextPrimary, unfocusedTextColor = VelvetTextPrimary, focusedContainerColor = Color.White.copy(alpha = .05f), unfocusedContainerColor = Color.White.copy(alpha = .04f)))
        ActionButton("Fetch media", Icons.Default.Link, valid, onFetch)
    }
}

@Composable private fun FetchingContent() {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "phase")
    Column(Modifier.fillMaxWidth().padding(top = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { repeat(7) { i -> val h = 18.dp + ((kotlin.math.sin((i + phase) * 1.2f) + 1f) * 13f).dp; Box(Modifier.width(5.dp).height(h).clip(RoundedCornerShape(5.dp)).background(VelvetBrightCrimson.copy(alpha = .55f + i * .05f))) } }
        Spacer(Modifier.height(24.dp)); Text("Fetching media...", color = VelvetTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(7.dp)); Text("Analyzing the public media page", color = VelvetTextSecondary, fontSize = 13.sp)
    }
}

@Composable private fun ResultContent(media: ResolvedMedia?, onDominant: (Color) -> Unit, onDownload: (String?, Boolean) -> Unit) {
    if (media == null) return
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(media.platform, color = VelvetTextSecondary, fontSize = 13.sp)
        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(media.thumbnailUrl).crossfade(true).build(), contentDescription = media.title, modifier = Modifier.fillMaxWidth().height(245.dp).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop, onSuccess = { result -> val bitmap = (result.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap; if (bitmap != null) { val p = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2); onDominant(Color(android.graphics.Color.rgb(android.graphics.Color.red(p), android.graphics.Color.green(p), android.graphics.Color.blue(p)))) } })
        Text(media.title, color = VelvetTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(media.creator, color = VelvetTextSecondary, fontSize = 13.sp)
        ActionButton("Download Video", Icons.Default.VideoFile, !media.mediaUrl.isNullOrBlank()) { onDownload(media.mediaUrl, false) }
        ActionButton("Download Audio", Icons.Default.MusicNote, !media.audioUrl.isNullOrBlank()) { onDownload(media.audioUrl, true) }
        if (media.mediaUrl.isNullOrBlank() && media.audioUrl.isNullOrBlank()) Text("No downloadable public media URL was exposed. Velvet will not bypass platform protections.", color = VelvetTextSecondary, fontSize = 12.sp)
    }
}

@Composable private fun ErrorContent(error: String, onRetry: () -> Unit, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn't fetch this media", color = VelvetTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text("Check the link and try again.", color = VelvetTextSecondary, fontSize = 13.sp)
        if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = VelvetTextSecondary, fontSize = 11.sp) }
        Spacer(Modifier.height(22.dp)); ActionButton("Try Again", Icons.Default.Link, true, onRetry); Spacer(Modifier.height(8.dp)); ActionButton("Edit Link", Icons.Default.ContentPaste, true, onEdit)
    }
}

@Composable private fun ProgressContent(progress: Float) {
    Column(Modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Download, null, tint = VelvetBrightCrimson, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(22.dp)); Text("Downloading...", color = VelvetTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp)), color = VelvetBrightCrimson, trackColor = Color.White.copy(alpha = .12f)); Spacer(Modifier.height(8.dp)); Text("${(progress * 100).toInt()}%", color = VelvetTextSecondary, fontSize = 13.sp) }
}

@Composable private fun CompleteContent() {
    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Check, null, tint = Color(0xFF69F0AE), modifier = Modifier.size(54.dp)); Spacer(Modifier.height(16.dp)); Text("Download started", color = VelvetTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text("Android Download Manager will finish the transfer in the background.", color = VelvetTextSecondary, fontSize = 13.sp); Spacer(Modifier.height(22.dp)); ActionButton("Open Downloads", Icons.Default.PlayArrow, true) { }; Spacer(Modifier.height(8.dp)); ActionButton("Share", Icons.Default.Share, true) { } }
}

@Composable private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(15.dp)).background(if (enabled) VelvetBrightCrimson else Color.White.copy(alpha = .08f)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(icon, null, tint = if (enabled) Color.White else VelvetTextSecondary, modifier = Modifier.size(19.dp)); Spacer(Modifier.size(8.dp)); Text(label, color = if (enabled) Color.White else VelvetTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}

private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(70).ifBlank { "velvet_media" }
