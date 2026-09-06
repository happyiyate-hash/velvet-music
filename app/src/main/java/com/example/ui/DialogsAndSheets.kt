package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.VelvetActivePill
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetDeepCrimson
import com.example.ui.theme.VelvetObsidian
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

private val DeviceSheetAsh = Color(0xD9161719)
private val DeviceSheetAshLine = Color(0xFF343638)
private val DeviceSheetIconGray = Color(0xFFA5A8AA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionBottomSheet(
    track: Track,
    isCached: Boolean,
    isFavorite: Boolean = false,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onShareTrack: () -> Unit = {},
    onDeleteTrack: () -> Unit = {},
    onToggleOfflineCache: () -> Unit,
    onViewLyrics: () -> Unit,
    onViewSoundCatch: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeviceSheetAsh,
        contentColor = VelvetTextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(DeviceSheetAshLine)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 14.dp)
                .testTag("track_action_sheet")
        ) {
            Text(
                text = track.title,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = VelvetTextPrimary,
                maxLines = 2
            )
            Text(
                text = "${track.artist} • ${track.album}",
                fontSize = 12.sp,
                color = VelvetTextSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            ActionRowItem(Icons.Default.PlayArrow, "Play Now", "Start playback") { onPlayNow(); onDismiss() }
            ActionRowItem(Icons.Default.QueueMusic, "Play Next", "Play after the current track") { onPlayNext(); onDismiss() }
            ActionRowItem(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (isFavorite) "Favorited" else "Add to Favorites",
                if (isFavorite) "Saved to favorites" else "Save this track",
                DeviceSheetIconGray
            ) { onToggleFavorite(); onDismiss() }
            ActionRowItem(Icons.Default.Share, "Share Track", "Share song details") { onShareTrack(); onDismiss() }
            ActionRowItem(Icons.Default.Delete, "Delete Track", "Remove from your library", DeviceSheetIconGray) { onDeleteTrack(); onDismiss() }
        }
    }
}

@Composable
fun ActionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = DeviceSheetIconGray,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, title, tint = iconTint, modifier = Modifier.size(27.dp).padding(1.dp))
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 1.dp)) {
            Text(title, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = VelvetTextPrimary, maxLines = 1)
            Text(subtitle, fontSize = 10.sp, lineHeight = 13.sp, color = VelvetTextSecondary, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProTierBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isAnnual by remember { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = VelvetObsidian, contentColor = VelvetTextPrimary, dragHandle = {
        Box(Modifier.padding(vertical = 12.dp).width(44.dp).height(4.dp).clip(CircleShape).background(VelvetBorder))
    }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp).testTag("pro_tier_sheet"), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Brush.radialGradient(listOf(VelvetBrightCrimson, VelvetDarkBurgundy))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, "Velvet Pro", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Velvet Pro Tier", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VelvetTextPrimary)
            Text("Dark. Ambient. Uninterrupted Luxury Audio.", fontSize = 13.sp, color = VelvetTextSecondary)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.clip(RoundedCornerShape(24.dp)).background(VelvetSurfaceElevated).border(1.dp, VelvetBorder, RoundedCornerShape(24.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (isAnnual) VelvetBrightCrimson else Color.Transparent).clickable { isAnnual = true }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("$1.99 / month (Billed Annually)", fontSize = 12.sp, fontWeight = if (isAnnual) FontWeight.Bold else FontWeight.Normal, color = if (isAnnual) Color.White else VelvetTextSecondary)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (!isAnnual) VelvetBrightCrimson else Color.Transparent).clickable { isAnnual = false }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("$2.99 / month", fontSize = 12.sp, fontWeight = if (!isAnnual) FontWeight.Bold else FontWeight.Normal, color = if (!isAnnual) Color.White else VelvetTextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VelvetSurfaceElevated).border(1.dp, VelvetBorder, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProFeatureItem("Zero audio ads & uninterrupted playback flow")
                ProFeatureItem("Unlimited offline disk caching ($0 egress storage)")
                ProFeatureItem("High-bitrate AAC streaming (160 kbps CD-quality)")
                ProFeatureItem("120 FPS high-precision Sound Catch Mesh shaders")
                ProFeatureItem("Full access to OpenAI Whisper word-synced lyrics (.lrc)")
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("subscribe_pro_button"), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = VelvetBrightCrimson, contentColor = Color.White)) {
                Text(if (isAnnual) "Start Velvet Pro • $1.99/mo" else "Start Velvet Pro • $2.99/mo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProFeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, "Included", tint = VelvetBrightCrimson, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = VelvetTextPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = VelvetObsidian, contentColor = VelvetTextPrimary, dragHandle = {
        Box(Modifier.padding(vertical = 12.dp).width(44.dp).height(4.dp).clip(CircleShape).background(VelvetBorder))
    }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp).testTag("settings_sheet")) {
            Text("Velvet System Architecture", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VelvetTextPrimary)
            Text("Dark. Ambient. Fluid Audio. Blueprint Specs.", fontSize = 12.sp, color = VelvetTextSecondary)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VelvetSurfaceElevated).border(1.dp, VelvetBorder, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ArchitectureSpecRow("Frontend Engine", "Native GPU Fragment Mesh (60–120 FPS)")
                ArchitectureSpecRow("Audio Encoding", "AAC 128 kbps – 160 kbps CD-Quality")
                ArchitectureSpecRow("Backend APIs", "Cloudflare Workers Serverless")
                ArchitectureSpecRow("Media Storage", "Cloudflare R2 / Backblaze ($0 egress)")
                ArchitectureSpecRow("Database", "Serverless Neon Postgres")
                ArchitectureSpecRow("STT Engine", "OpenAI Whisper (word_timestamps=True)")
                ArchitectureSpecRow("Sound Catch Pipeline", "Fast Snap (<5ms) + Smooth Drift (lerp)")
            }
        }
    }
}

@Composable
fun ArchitectureSpecRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = VelvetTextSecondary)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, color = VelvetBrightCrimson)
    }
}
