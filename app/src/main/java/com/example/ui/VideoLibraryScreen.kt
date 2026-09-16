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
import androidx.compose.ui.draw.shadow
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

    val featuredVideo = remember(filteredVideos) { filteredVideos.firstOrNull() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_library_screen")
    ) {
        // Atmospheric Multi-Gradient Canvas Background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF040002))

            // Upper crimson ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FF2448),
                        Color(0x1F4A000E),
                        Color(0x0C220005),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.12f),
                    radius = w * 0.78f
                ),
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.12f),
                radius = w * 0.78f
            )

            // Mid-canvas deep wine bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x224A0012),
                        Color(0x0E240008),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.55f),
                    radius = w * 0.65f
                ),
                center = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.55f),
                radius = w * 0.65f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
        ) {
            // 0. Velvet Signature Branding Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "V E L V E T",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF2448),
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DARK.  AMBIENT.  FLUID CINEMA.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 2.4.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1. Top Header: Title with Badge & Action Icon
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Video",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x44FF2448),
                                                Color(0x224A0012)
                                            )
                                        )
                                    )
                                    .border(0.8.dp, Color(0x66FF2448), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${videos.size} videos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF4D6D)
                                )
                            }
                        }
                        Text(
                            text = "Gradient video plays & reels",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x3348000C),
                                        Color(0x1F220006)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x33FF2448), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Videos",
                            tint = Color(0xFFFF4D6D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 2. FEATURED GRADIENT PLAY (Spotlight Cinema Card)
            if (featuredVideo != null) {
                item {
                    val heroShape = RoundedCornerShape(22.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = heroShape,
                                spotColor = Color(0x88FF2448),
                                ambientColor = Color(0x444A0012)
                            )
                            .clip(heroShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF480012),
                                        Color(0xFF220008),
                                        Color(0xFF0E0004)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color(0x88FF2448),
                                        Color(0x33FFFFFF),
                                        Color(0x66FF2448)
                                    )
                                ),
                                shape = heroShape
                            )
                            .clickable { previewVideo = featuredVideo }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF2448))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GRADIENT PLAY SPOTLIGHT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF4D6D),
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x33FF2448))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = featuredVideo.resolution.ifEmpty { "HD 1080P" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Cinema Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF5A0017),
                                                Color(0xFF2B000A),
                                                Color(0xFF120004)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(56.dp)
                                )

                                // Glowing Circular Play Button
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFFF2448),
                                                    Color(0xFFB50E29)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Duration tag bottom-right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .border(0.5.dp, Color(0x66FF2448), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formatDuration(featuredVideo.durationMs),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = featuredVideo.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${featuredVideo.resolution} • ${formatFileSize(featuredVideo.sizeBytes)} • Tap to Play",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 3. Search Bar
            item {
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search videos...", color = Color(0xFF8E8E93), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFFF4D6D)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1F220006))
                            .testTag("video_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF2448),
                            unfocusedBorderColor = Color(0x33FF2448),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Gradient Filter Pills
            item {
                val filterOptions = listOf("All", "4K / HD", "Shorts")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val pillShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFE51B3E),
                                                Color(0xFF880018)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x2236000C),
                                                Color(0x121A0005)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFF6B8B),
                                                Color(0x44FFFFFF)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0x33FF2448),
                                                Color(0x15FFFFFF)
                                            )
                                        )
                                    },
                                    shape = pillShape
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF8E8E93)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gradient Plays",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${filteredVideos.size} items",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. Video Items List
            if (filteredVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 20.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x1F220006))
                            .border(1.dp, Color(0x33FF2448), RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color(0xFFFF4D6D),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!hasPermission) "Video Permission Needed" else "No Videos Found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (!hasPermission)
                                    "Allow Velvet to access device videos to watch them here"
                                else
                                    "No video files found matching filter criteria",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93),
                                textAlign = TextAlign.Center
                            )
                            if (!hasPermission) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { videoPermissionLauncher.launch(DeviceMediaManager.requiredVideoPermissions) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE51B3E))
                                ) {
                                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Allow Video Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredVideos, key = { it.id }) { video ->
                    DeviceVideoCard(
                        video = video,
                        onClick = { previewVideo = video },
                        onMenuClick = { selectedVideoForAction = video }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
}

/**
 * Premium Gradient Video Play Card
 */
@Composable
fun GradientVideoPlayCard(
    video: DeviceVideo,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                spotColor = Color(0x55FF2448),
                ambientColor = Color(0x2248000C)
            )
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0x2848000C),
                        Color(0x14220006),
                        Color(0x0C120003)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0x44FF2448),
                        Color(0x18FFFFFF),
                        Color(0x22FF2448)
                    )
                ),
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("device_video_${video.id}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Container with Gradient Rim & Duration Pill
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF480012),
                                Color(0xFF220008),
                                Color(0xFF0E0004)
                            )
                        )
                    )
                    .border(1.dp, Color(0x33FF2448), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(30.dp)
                )

                // Play Icon overlay
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF2448),
                                    Color(0xFFB50E29)
                                )
                            )
                        ),
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
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(0.5.dp, Color(0x66FF2448), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FF2448))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = video.resolution.ifEmpty { "HD" },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4D6D)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatFileSize(video.sizeBytes),
                        fontSize = 11.5.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            // Standing straight vertical three dots
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("video_menu_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Video Menu",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun DeviceVideoCard(
    video: DeviceVideo,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    GradientVideoPlayCard(
        video = video,
        onClick = onClick,
        onMenuClick = onMenuClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
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
