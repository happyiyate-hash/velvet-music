package com.example.ui

import android.app.Activity
import android.widget.Toast
import com.example.ads.AdMobManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SampleData
import com.example.model.Track

enum class HomeFilter { ALL, FAVORITES, RECENT }

@Composable
fun HomeFeedScreen(
    currentTrack: Track,
    isPlaying: Boolean,
    allTracks: List<Track>,
    mostPlayedTracks: List<Track>,
    recentlyAddedTracks: List<Track> = emptyList(),
    favoriteTrackIds: Set<String> = emptySet(),
    playCounts: Map<String, Int> = emptyMap(),
    hasAudioPermission: Boolean = true,
    onRequestPermission: (() -> Unit)? = null,
    onSelectTrack: (Track) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onTrackMenuClick: (Track) -> Unit,
    onAddTrack: ((Track) -> Unit)? = null,
    onToggleShuffle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseTracks = remember(allTracks) {
        if (allTracks.isNotEmpty()) allTracks else SampleData.starterTracks
    }

    var activeFilter by remember { mutableStateOf(HomeFilter.ALL) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showPlaylistsSheet by remember { mutableStateOf(false) }
    var showFoldersSheet by remember { mutableStateOf(false) }
    var showTempoSheet by remember { mutableStateOf(false) }
    var showAudioFxSheet by remember { mutableStateOf(false) }

    val displayTracks = remember(baseTracks, activeFilter, recentlyAddedTracks, favoriteTrackIds) {
        when (activeFilter) {
            HomeFilter.ALL -> baseTracks
            HomeFilter.FAVORITES -> {
                val favs = baseTracks.filter { favoriteTrackIds.contains(it.id) }
                if (favs.isNotEmpty()) favs else baseTracks.take(15)
            }
            HomeFilter.RECENT -> if (recentlyAddedTracks.isNotEmpty()) recentlyAddedTracks else baseTracks.reversed()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("home_feed_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 2. TOP NAVIGATION HEADER:
            // - Borderless, flows directly from the very top and left/right edges with zero cutoff.
            // - Blends seamlessly into pure black.
            // - Transparent App logo without borders or boxes (ready for custom velvet_home_logo.png in GitHub).
            // - Name "VELVET" at top.
            // - Track count text completely removed.
            // - Only Search and Settings icons (Notification icon removed).
            // - Horizontally scrollable bar of feature icons (Library, Adjustment, etc.).
            VelvetTopHeaderCard(
                activeFilter = activeFilter,
                onSelectFilter = { filter ->
                    activeFilter = filter
                    val label = when (filter) {
                        HomeFilter.ALL -> "Showing All Tracks"
                        HomeFilter.FAVORITES -> "Showing Favorite Tracks"
                        HomeFilter.RECENT -> "Showing Recently Added"
                    }
                    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                },
                onOpenEqualizer = { showEqualizerSheet = true },
                onOpenSleepTimer = { showSleepTimerSheet = true },
                onOpenPlaylists = { showPlaylistsSheet = true },
                onOpenFolders = { showFoldersSheet = true },
                onOpenTempo = { showTempoSheet = true },
                onOpenAudioFx = { showAudioFxSheet = true },
                onShuffleAll = {
                    if (onToggleShuffle != null) {
                        onToggleShuffle()
                    } else if (displayTracks.isNotEmpty()) {
                        onSelectTrack(displayTracks.random())
                    }
                    Toast.makeText(context, "Shuffling library", Toast.LENGTH_SHORT).show()
                },
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings
            )

            // 3. MAIN BACKGROUND MUSIC LIST:
            // Direct list over the illuminated pink/magenta matrix background with NO card wrapper
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 6.dp,
                    top = 10.dp,
                    bottom = 120.dp
                )
            ) {
                // Permission request banner if local media permission is missing
                if (!hasAudioPermission && allTracks.isEmpty()) {
                    item {
                        DeviceAudioPermissionBanner(onRequestPermission = onRequestPermission)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                itemsIndexed(displayTracks, key = { _, track -> track.id }) { index, track ->
                    val isCurrent = track.id == currentTrack.id
                    DeviceTrackRowItem(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSelectTrack(track) },
                        onMenuClick = { onTrackMenuClick(track) },
                        artworkLoadDelayMs = (index % 6) * 18L,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Interactive Sheets for the horizontal icons
    if (showEqualizerSheet) {
        EqualizerBottomSheet(onDismiss = { showEqualizerSheet = false })
    }
    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(onDismiss = { showSleepTimerSheet = false })
    }
    if (showPlaylistsSheet) {
        PlaylistsBottomSheet(onDismiss = { showPlaylistsSheet = false })
    }
    if (showFoldersSheet) {
        FoldersBottomSheet(trackCount = baseTracks.size, onDismiss = { showFoldersSheet = false })
    }
    if (showTempoSheet) {
        TempoBottomSheet(onDismiss = { showTempoSheet = false })
    }
    if (showAudioFxSheet) {
        AudioFxBottomSheet(onDismiss = { showAudioFxSheet = false })
    }
}

/**
 * Top Header Card:
 * - Rich Pink and Red blend (`#6B0E35` -> `#450824` -> `#280415` -> `#15020B`)
 * - Made the app logo smaller and dragged it to the top corner.
 * - Dedicated space for transparent app logo (ready for GitHub repo upload).
 * - App title "VELVET" and track text moved to the top.
 * - Remaining bottom space houses the horizontally scrollable action bar (libraries, adjustment, etc.).
 */
@Composable
private fun VelvetTopHeaderCard(
    activeFilter: HomeFilter,
    onSelectFilter: (HomeFilter) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenTempo: () -> Unit,
    onOpenAudioFx: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5E092B), // Rich pinkish wine top
                        Color(0xFF38051A),
                        Color(0xFF1B020D),
                        Color.Black        // Blends smoothly into the pure black screen
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // ==========================================
            // TOP ROW: Transparent Logo + VELVET Name (top) & Search/Settings
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Transparent Logo (V) + ELVET Glass Wordmark
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VelvetCornerAppLogo(
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    ElvetGlassWordmark(
                        modifier = Modifier.height(18.dp)
                    )
                }

                // Right: Search and Settings (Notification icon removed!)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarActionButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search",
                        onClick = onOpenSearch
                    )
                    TopBarActionButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // BOTTOM AREA: Horizontally Scrollable Bar of Feature Icons (Libraries, Adjustment, etc.)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 0. Rewards / VIP AdMob bonus
                HeaderActionPill(
                    icon = Icons.Default.CardGiftcard,
                    label = "Rewards",
                    isSelected = false,
                    onClick = {
                        val activity = context as? Activity ?: AdMobManager.currentActivity
                        if (activity != null) {
                            AdMobManager.showRewardedAd(
                                activity = activity,
                                onRewardEarned = { reward ->
                                    Toast.makeText(context, "🎁 Reward Unlocked: +${reward.amount} VIP credits!", Toast.LENGTH_SHORT).show()
                                },
                                onAdDismissed = {}
                            )
                        } else {
                            Toast.makeText(context, "Ad loading...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 1. Library
                HeaderActionPill(
                    icon = Icons.Default.LibraryMusic,
                    label = "Library",
                    isSelected = activeFilter == HomeFilter.ALL,
                    onClick = { onSelectFilter(HomeFilter.ALL) }
                )

                // 2. Adjustment / Equalizer
                HeaderActionPill(
                    icon = Icons.Default.Tune,
                    label = "Adjustment",
                    isSelected = false,
                    onClick = onOpenEqualizer
                )

                // 3. Favorites
                HeaderActionPill(
                    icon = Icons.Default.Favorite,
                    label = "Favorites",
                    isSelected = activeFilter == HomeFilter.FAVORITES,
                    onClick = { onSelectFilter(HomeFilter.FAVORITES) }
                )

                // 4. Playlists
                HeaderActionPill(
                    icon = Icons.Default.QueueMusic,
                    label = "Playlists",
                    isSelected = false,
                    onClick = onOpenPlaylists
                )

                // 5. Sleep Timer
                HeaderActionPill(
                    icon = Icons.Default.Timer,
                    label = "Sleep Timer",
                    isSelected = false,
                    onClick = onOpenSleepTimer
                )

                // 6. Sound Effects / Audio FX
                HeaderActionPill(
                    icon = Icons.Default.SurroundSound,
                    label = "Sound FX",
                    isSelected = false,
                    onClick = onOpenAudioFx
                )

                // 7. Recently Added
                HeaderActionPill(
                    icon = Icons.Default.History,
                    label = "Recent",
                    isSelected = activeFilter == HomeFilter.RECENT,
                    onClick = { onSelectFilter(HomeFilter.RECENT) }
                )

                // 8. Tempo / Speed
                HeaderActionPill(
                    icon = Icons.Default.Speed,
                    label = "Tempo",
                    isSelected = false,
                    onClick = onOpenTempo
                )

                // 9. Folders
                HeaderActionPill(
                    icon = Icons.Default.FolderOpen,
                    label = "Folders",
                    isSelected = false,
                    onClick = onOpenFolders
                )

                // 10. Shuffle All
                HeaderActionPill(
                    icon = Icons.Default.Shuffle,
                    label = "Shuffle All",
                    isSelected = false,
                    onClick = onShuffleAll
                )
            }
        }
    }
}

/**
 * App Logo in the top navigation:
 * Directly calls app/src/main/res/drawable/velvet_home_logo.png (R.drawable.velvet_home_logo)
 * Treated as a pure transparent vector/emblem without borders or backgrounds.
 */
@Composable
private fun VelvetCornerAppLogo(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.velvet_home_logo),
        contentDescription = "Velvet App Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * Premium Glass Logotype Wordmark: "ELVET"
 * - Starts with 'E' because the preceding logo itself acts as the initial 'V'
 * - Pushed closely to the logo picture
 * - Increased height with a thin, refined stroke
 * - Premium glass design: frosted crystal specular highlight, electric pink refraction, ruby glow
 * - Curved edges: The remaining 'V' features a smooth, rounded bottom apex (no sharp corner),
 *   while 'L' and 'E' feature smooth rounded corner transitions.
 */
@Composable
private fun ElvetGlassWordmark(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .height(18.dp)
            .width(68.dp)
    ) {
        val h = size.height
        val strokeWidth = 2.8.dp.toPx()
        val glowStrokeWidth = 4.6.dp.toPx()

        val charSpacing = 3.8.dp.toPx()
        val eWidth = 9.2.dp.toPx()
        val lWidth = 8.0.dp.toPx()
        val vWidth = 10.8.dp.toPx()
        val tWidth = 9.2.dp.toPx()

        val cornerR = 2.0.dp.toPx()
        val paths = mutableListOf<Path>()

        // 1. First 'E' (Rounded corners on outer joins)
        var currentX = 1.0.dp.toPx()
        val pathE1 = Path().apply {
            moveTo(currentX + eWidth, 0f)
            lineTo(currentX + cornerR, 0f)
            quadraticTo(currentX, 0f, currentX, cornerR)
            lineTo(currentX, h - cornerR)
            quadraticTo(currentX, h, currentX + cornerR, h)
            lineTo(currentX + eWidth, h)
            moveTo(currentX, h * 0.5f)
            lineTo(currentX + eWidth * 0.70f, h * 0.5f)
        }
        paths.add(pathE1)

        // 2. 'L' (Smooth rounded bend at bottom)
        currentX += eWidth + charSpacing
        val pathL = Path().apply {
            moveTo(currentX, 0f)
            lineTo(currentX, h - cornerR)
            quadraticTo(currentX, h, currentX + cornerR, h)
            lineTo(currentX + lWidth, h)
        }
        paths.add(pathL)

        // 3. Middle 'V' (Curved bottom apex - NO sharp edge!)
        currentX += lWidth + charSpacing
        val pathV = Path().apply {
            moveTo(currentX, 0f)
            val vMidX = currentX + vWidth / 2f
            val curveRx = vWidth * 0.22f
            val curveRy = h * 0.16f
            lineTo(vMidX - curveRx, h - curveRy)
            // Quadratic Bézier creates a sleek U-curved apex
            quadraticTo(vMidX, h, vMidX + curveRx, h - curveRy)
            lineTo(currentX + vWidth, 0f)
        }
        paths.add(pathV)

        // 4. Second 'E' (Rounded corners)
        currentX += vWidth + charSpacing
        val pathE2 = Path().apply {
            moveTo(currentX + eWidth, 0f)
            lineTo(currentX + cornerR, 0f)
            quadraticTo(currentX, 0f, currentX, cornerR)
            lineTo(currentX, h - cornerR)
            quadraticTo(currentX, h, currentX + cornerR, h)
            lineTo(currentX + eWidth, h)
            moveTo(currentX, h * 0.5f)
            lineTo(currentX + eWidth * 0.70f, h * 0.5f)
        }
        paths.add(pathE2)

        // 5. 'T' (Crossbar and vertical stem)
        currentX += eWidth + charSpacing
        val pathT = Path().apply {
            moveTo(currentX, 0f)
            lineTo(currentX + tWidth, 0f)
            val tCenter = currentX + tWidth / 2f
            moveTo(tCenter, 0f)
            lineTo(tCenter, h)
        }
        paths.add(pathT)

        // Ambient Frosted Glass Glow (Inner illumination)
        val glassGlowBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0x35FFCCD8),
                Color(0x55FF2A6D),
                Color(0x35E50914)
            )
        )

        // Premium Translucent Glass Body Shader
        val glassBodyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xF5FFFFFF), // Specular light reflection at top
                Color(0xD8FFD6E2), // Frosted crystal pink
                Color(0xCCFF3366), // Translucent electric pink body
                Color(0xA0E50914)  // Translucent ruby red refraction
            )
        )

        // 1. Draw ambient glass glow pass
        paths.forEach { path ->
            drawPath(
                path = path,
                brush = glassGlowBrush,
                style = Stroke(
                    width = glowStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 2. Draw refined glass strokes with increased thickness
        paths.forEach { path ->
            drawPath(
                path = path,
                brush = glassBodyBrush,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Horizontally Scrollable Action Pill without harsh borders
 */
@Composable
private fun HeaderActionPill(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isSelected) {
        Brush.horizontalGradient(listOf(Color(0xFFFF2A6D), Color(0xFFE50914)))
    } else {
        Brush.horizontalGradient(listOf(Color(0x35FFFFFF), Color(0x18FFFFFF)))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(pillBg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color(0xFFFF85A1),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFFFE6EC)
        )
    }
}

@Composable
private fun TopBarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFFFE6EC),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Cardless Device Track Row with pink-and-red highlights:
 * Direct on the background, big 58×58dp album artwork thumbnail with clean rounded corners.
 */
@Composable
private fun DeviceTrackRowItem(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    artworkLoadDelayMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val cleanTitle = track.title.substringBefore(" - ")

    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BIG Album Artwork (58x58dp) with smooth rounded corners and no placeholder border
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF22050E))
        ) {
            TrackArtworkImage(
                track = track,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                thumbnailSizePx = 128,
                loadDelayMs = artworkLoadDelayMs,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color(0xFFFF2A6D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(13.dp))

        // Title and artist metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTitle,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) Color(0xFFFF2A6D) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.formattedDuration}",
                fontSize = 12.sp,
                color = if (isCurrent) Color(0xFFFFB0BD) else Color(0xFFC098A2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Menu: Vertical 3-dots (standing straight), pure white, bigger & mature
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Track options",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DeviceAudioPermissionBanner(
    onRequestPermission: (() -> Unit)?
) {
    val bannerShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(bannerShape)
            .background(Color(0x35600C1C))
            .border(1.dp, Color(0x35FFAAB8), bannerShape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Device Audio Access",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Grant permission to read and play all music files stored on this device.",
                fontSize = 11.5.sp,
                color = Color(0xFFC098A2)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Button(
            onClick = { onRequestPermission?.invoke() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF2A6D),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = "Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// FEATURE BOTTOM SHEETS (Equalizer, Sleep Timer, Playlists, Folders, Tempo, Audio FX)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bassBoost by remember { mutableFloatStateOf(0.65f) }
    var band60 by remember { mutableFloatStateOf(0.70f) }
    var band230 by remember { mutableFloatStateOf(0.55f) }
    var band910 by remember { mutableFloatStateOf(0.50f) }
    var band36k by remember { mutableFloatStateOf(0.60f) }
    var band14k by remember { mutableFloatStateOf(0.75f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B030D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Adjustments & Equalizer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Text(
                text = "5-band parametric equalizer with dynamic harmonic tuning",
                fontSize = 12.sp,
                color = Color(0xFFFFB3C6)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Velvet Bass", "Vocal Clarity", "Electronic", "Rock", "Acoustic", "Flat").forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x35FF2A6D))
                            .border(0.8.dp, Color(0x50FF6384), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = preset, fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sliders for 5 bands
            val bands = listOf(
                "60 Hz (Sub Bass)" to band60,
                "230 Hz (Bass)" to band230,
                "910 Hz (Mids)" to band910,
                "3.6 kHz (High Mids)" to band36k,
                "14 kHz (Air/Treble)" to band14k
            )

            bands.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = Color(0xFFFFD4DC),
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = value,
                        onValueChange = { newVal ->
                            when (index) {
                                0 -> band60 = newVal
                                1 -> band230 = newVal
                                2 -> band910 = newVal
                                3 -> band36k = newVal
                                4 -> band14k = newVal
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF2A6D),
                            activeTrackColor = Color(0xFFE50914),
                            inactiveTrackColor = Color(0x30FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bass Boost",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.width(110.dp)
                )
                Slider(
                    value = bassBoost,
                    onValueChange = { bassBoost = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF2A6D),
                        activeTrackColor = Color(0xFFFF2A6D),
                        inactiveTrackColor = Color(0x30FFFFFF)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val timerOptions = listOf("15 Minutes", "30 Minutes", "45 Minutes", "60 Minutes", "Turn Off Timer")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B030D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Sleep Timer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Automatically pauses music playback after the timer finishes",
                fontSize = 12.sp,
                color = Color(0xFFFFB3C6)
            )
            Spacer(modifier = Modifier.height(16.dp))

            timerOptions.forEach { opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            Toast.makeText(context, "Sleep Timer: $opt", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFF2A6D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = opt, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val playlists = listOf(
        "Velvet Favorites" to "24 tracks",
        "Midnight Grooves" to "18 tracks",
        "Workout Energy" to "32 tracks",
        "Deep Chillout" to "40 tracks"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B030D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Playlists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            playlists.forEach { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            Toast.makeText(context, "Opening playlist: $name", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFFFF2A6D),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = count, fontSize = 11.5.sp, color = Color(0xFFFFB3C6))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoldersBottomSheet(trackCount: Int, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val folders = listOf(
        "Music/Download" to "${(trackCount * 0.45).toInt()} tracks",
        "Music/Velvet" to "${(trackCount * 0.35).toInt()} tracks",
        "Recordings/Audio" to "${(trackCount * 0.20).toInt()} tracks"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B030D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Device Audio Folders",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "All music files indexed directly on local storage",
                fontSize = 12.sp,
                color = Color(0xFFFFB3C6)
            )
            Spacer(modifier = Modifier.height(16.dp))

            folders.forEach { (path, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFFFF2A6D),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = path, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = count, fontSize = 11.5.sp, color = Color(0xFFFFB3C6))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TempoBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val speeds = listOf("0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B030D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Playback Speed & Tempo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            speeds.forEach { speed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            Toast.makeText(context, "Playback speed: $speed", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFFFF2A6D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = speed, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioFxBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var spatialAudio by remember { mutableStateOf(true) }
    var dynamicBass by remember { mutableStateOf(true) }
    var tubeWarmth by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B030D),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Spatial Audio & Sound FX",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "High-fidelity acoustics & 3D soundstage optimization",
                fontSize = 12.sp,
                color = Color(0xFFFFB3C6)
            )
            Spacer(modifier = Modifier.height(20.dp))

            listOf(
                Triple("3D Spatial Surround", "Expands soundstage with binaural head staging", spatialAudio) to { spatialAudio = !spatialAudio },
                Triple("Velvet Dynamic Bass", "Sub-bass harmonic synthesis without distortion", dynamicBass) to { dynamicBass = !dynamicBass },
                Triple("Tube Amp Warmth", "Analog even-harmonic saturation", tubeWarmth) to { tubeWarmth = !tubeWarmth }
            ).forEach { (item, toggle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { toggle() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.first, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = item.second, fontSize = 11.5.sp, color = Color(0xFFFFB3C6))
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (item.third) Color(0xFFFF2A6D) else Color(0x30FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.third) {
                            Icon(
                                imageVector = Icons.Default.SurroundSound,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
