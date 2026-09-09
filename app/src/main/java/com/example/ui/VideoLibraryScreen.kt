package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.DeviceMediaManager
import com.example.model.DeviceVideo
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetDeepCrimson
import com.example.ui.theme.VelvetObsidian
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<DeviceVideo>>(DeviceMediaManager.getCachedVideos(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedVideoForAction by remember { mutableStateOf<DeviceVideo?>(null) }
    var previewVideo by remember { mutableStateOf<DeviceVideo?>(null) }
    var hasPermission by remember { mutableStateOf(DeviceMediaManager.hasVideoPermission(context)) }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = DeviceMediaManager.hasVideoPermission(context)
        hasPermission = granted
        if (granted) {
            coroutineScope.launch(Dispatchers.IO) {
                val loaded = DeviceMediaManager.loadDeviceVideos(context)
                videos = loaded
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            videoPermissionLauncher.launch(DeviceMediaManager.requiredVideoPermissions)
        } else {
            val loaded = withContext(Dispatchers.IO) {
                DeviceMediaManager.loadDeviceVideos(context)
            }
            videos = loaded
        }
    }

    val filteredVideos = remember(videos, searchQuery, selectedFilter) {
        videos.filter { video ->
            val matchesQuery = video.title.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "4K / HD" -> video.resolution.contains("4K", ignoreCase = true) || video.resolution.contains("1080", ignoreCase = true)
                "Shorts" -> video.durationMs < 120000L
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_library_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Media Library",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = VelvetTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Device Videos",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelvetTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Videos",
                            tint = VelvetBrightCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search device videos...", color = VelvetTextTertiary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VelvetTextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .testTag("video_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelvetBrightCrimson.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                        focusedTextColor = VelvetTextPrimary,
                        unfocusedTextColor = VelvetTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Pills
                val filterOptions = listOf("All", "4K / HD", "Shorts")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) VelvetBrightCrimson.copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) VelvetBrightCrimson else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) VelvetBrightCrimson else VelvetTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${filteredVideos.size} Videos on Device",
                    fontSize = 12.sp,
                    color = VelvetTextTertiary
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (filteredVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = VelvetTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!hasPermission) "Video Permission Needed" else "No Videos Found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VelvetTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!hasPermission)
                                "Allow Velvet to access your device videos to watch them here"
                            else
                                "No video files found on your device storage",
                            fontSize = 12.sp,
                            color = VelvetTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        if (!hasPermission) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { videoPermissionLauncher.launch(DeviceMediaManager.requiredVideoPermissions) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelvetBrightCrimson)
                            ) {
                                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Allow Video Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Video Items List
        items(filteredVideos, key = { it.id }) { video ->
            DeviceVideoCard(
                video = video,
                onClick = { previewVideo = video },
                onMenuClick = { selectedVideoForAction = video }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    // Video Action Bottom Sheet
    selectedVideoForAction?.let { video ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedVideoForAction = null },
            sheetState = sheetState,
            containerColor = VelvetObsidian,
            contentColor = VelvetTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelvetTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDuration(video.durationMs)} • ${video.resolution} • ${formatFileSize(video.sizeBytes)}",
                    fontSize = 13.sp,
                    color = VelvetTextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                ActionRowItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Play Video",
                    subtitle = "Open video playback",
                    onClick = {
                        selectedVideoForAction = null
                        previewVideo = video
                    }
                )

                ActionRowItem(
                    icon = Icons.Default.Share,
                    title = "Share Video",
                    subtitle = "Share file via device intent",
                    onClick = {
                        selectedVideoForAction = null
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_SUBJECT, video.title)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                    }
                )

                ActionRowItem(
                    icon = Icons.Default.Delete,
                    title = "Delete Video",
                    subtitle = "Remove from video list",
                    iconTint = Color(0xFFFF5252),
                    onClick = {
                        videos = videos.filterNot { it.id == video.id }
                        selectedVideoForAction = null
                    }
                )
            }
        }
    }

    // Video Preview Dialog
    previewVideo?.let { video ->
        AlertDialog(
            onDismissRequest = { previewVideo = null },
            containerColor = VelvetObsidian,
            title = {
                Text(
                    text = video.title,
                    color = VelvetTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        VelvetDarkBurgundy,
                                        Color(0xFF1E0B16),
                                        Color.Black
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(VelvetBrightCrimson)
                                    .clickable { previewVideo = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Playing",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Previewing ${video.resolution}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Duration: ${formatDuration(video.durationMs)}  |  Size: ${formatFileSize(video.sizeBytes)}",
                        color = VelvetTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { previewVideo = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = VelvetBrightCrimson)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DeviceVideoCard(
    video: DeviceVideo,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .testTag("device_video_${video.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with Duration Pill - enlarged
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(74.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            VelvetDarkBurgundy.copy(alpha = 0.90f),
                            Color(0xFF2B0A1A),
                            Color(0xFF14050D)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.40f),
                modifier = Modifier.size(32.dp)
            )

            // Play Icon overlay
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Duration tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(video.durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VelvetTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Resolution Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VelvetBrightCrimson.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.resolution,
                        color = VelvetBrightCrimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatFileSize(video.sizeBytes),
                    fontSize = 12.sp,
                    color = VelvetTextSecondary
                )
            }
        }

        // Clean Vertical Three Dots (stands straight!)
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("video_menu_${video.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = VelvetTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatFileSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "120 MB"
    val mb = sizeBytes / (1024 * 1024)
    return if (mb > 1024) {
        "%.1f GB".format(mb / 1024f)
    } else {
        "$mb MB"
    }
}
