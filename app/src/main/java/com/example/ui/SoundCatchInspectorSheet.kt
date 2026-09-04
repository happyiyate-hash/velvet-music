package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioTelemetry
import com.example.model.Track
import com.example.ui.theme.VelvetActiveGlow
import com.example.ui.theme.VelvetActivePill
import com.example.ui.theme.VelvetBorder
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetObsidian
import com.example.ui.theme.VelvetSurfaceElevated
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundCatchInspectorSheet(
    track: Track,
    telemetry: AudioTelemetry,
    isSoundCatchEnabled: Boolean,
    onToggleSoundCatch: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VelvetObsidian,
        contentColor = VelvetTextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VelvetBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .testTag("sound_catch_inspector_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sound Catch™ Engine",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelvetTextPrimary
                    )
                    Text(
                        text = "Real-Time Audio-Reactive Mesh Telemetry",
                        fontSize = 12.sp,
                        color = VelvetTextSecondary
                    )
                }

                Switch(
                    checked = isSoundCatchEnabled,
                    onCheckedChange = { onToggleSoundCatch() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VelvetBrightCrimson,
                        uncheckedThumbColor = VelvetTextSecondary,
                        uncheckedTrackColor = VelvetSurfaceElevated
                    ),
                    modifier = Modifier.testTag("sound_catch_toggle")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. FAST SNAP PIPELINE CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VelvetSurfaceElevated)
                    .border(1.dp, if (telemetry.transientSpike > 0.6f) VelvetBrightCrimson else VelvetBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Fast Snap",
                            tint = VelvetBrightCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fast Snap Pipeline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VelvetTextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (telemetry.kickDetected || telemetry.snareDetected) VelvetBrightCrimson else VelvetDarkBurgundy)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (telemetry.kickDetected) "KICK TRANSIENT" else if (telemetry.snareDetected) "SNARE SPIKE" else "ACTIVE (<5ms)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instant coordinate updates responding within milliseconds to high-frequency transient spikes without disruptive strobe flashing.",
                        fontSize = 12.sp,
                        color = VelvetTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // Transient level bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Peak Impulse: ${(telemetry.transientSpike * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = VelvetTextTertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(VelvetBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(telemetry.transientSpike.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(VelvetBrightCrimson)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. SMOOTH DRIFT PIPELINE CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VelvetSurfaceElevated)
                    .border(1.dp, VelvetBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = "Smooth Drift",
                            tint = Color(0xFFD4517A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smooth Drift Pipeline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VelvetTextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Continuous Lerp (α = 0.05)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = VelvetTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Continuous linear interpolation creating dragging, fluid motion for sustained vocal tones, piano decays, and ambient synths.",
                        fontSize = 12.sp,
                        color = VelvetTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sustained Energy: ${(telemetry.sustainedEnergy * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = VelvetTextTertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(VelvetBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(telemetry.sustainedEnergy.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD4517A))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. PALETTE EXTRACTOR CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VelvetSurfaceElevated)
                    .border(1.dp, VelvetBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Palette Extractor",
                            tint = Color(0xFFF094AB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Palette Extractor Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VelvetTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Automatically sampled dominant colors from \"${track.title}\" album artwork to drive background mesh nodes.",
                        fontSize = 12.sp,
                        color = VelvetTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(track.dominantColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dominant Crimson",
                                fontSize = 11.sp,
                                color = VelvetTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(track.secondaryColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secondary Burgundy",
                                fontSize = 11.sp,
                                color = VelvetTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
