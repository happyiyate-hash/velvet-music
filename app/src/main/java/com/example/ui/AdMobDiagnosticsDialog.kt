package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.AdMobManager

@Composable
fun AdMobDiagnosticsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activity = context as? Activity ?: AdMobManager.currentActivity

    val useTestAds by AdMobManager.useTestAds.collectAsState()
    val isAppOpenLoaded by AdMobManager.isAppOpenAdLoaded.collectAsState()
    val isRewardedLoaded by AdMobManager.isRewardedAdLoaded.collectAsState()
    val lastError by AdMobManager.lastError.collectAsState()
    val events by AdMobManager.recentEvents.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141416),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                .testTag("admob_diagnostics_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE50914).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AdMob Diagnostics",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Inspect ad status & copy error codes",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Test Ads Switcher
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F24)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (useTestAds) "Using Google Test Ads (100% Fill)" else "Using Real AdMob Production IDs",
                                        color = if (useTestAds) Color(0xFF4CAF50) else Color(0xFFFFB300),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (useTestAds) {
                                            "Test ads verify app functionality instantly without risk to AdMob account."
                                        } else {
                                            "Live IDs from your AdMob account. If new, Google may take 1+ hour to fill."
                                        },
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = useTestAds,
                                    onCheckedChange = { AdMobManager.setUseTestAds(it, context) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4CAF50),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFF444444)
                                    )
                                )
                            }
                        }
                    }

                    // Status Cards (App Open & Rewarded)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // App Open Ad Status
                            AdUnitStatusRow(
                                title = "App Open Ad",
                                adUnitId = AdMobManager.activeAppOpenAdUnitId,
                                isLoaded = isAppOpenLoaded,
                                onReload = { AdMobManager.loadAppOpenAd(context) },
                                onTestShow = {
                                    if (activity != null) {
                                        AdMobManager.showAppOpenAdIfAvailable(activity)
                                    } else {
                                        Toast.makeText(context, "Activity not available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Rewarded Ad Status
                            AdUnitStatusRow(
                                title = "Rewarded Ad (Music Found / Bonus)",
                                adUnitId = AdMobManager.activeRewardedAdUnitId,
                                isLoaded = isRewardedLoaded,
                                onReload = { AdMobManager.loadRewardedAd(context) },
                                onTestShow = {
                                    if (activity != null) {
                                        AdMobManager.showRewardedAd(
                                            activity = activity,
                                            onRewardEarned = {
                                                Toast.makeText(context, "🎉 Reward Earned: +${it.amount} ${it.type}!", Toast.LENGTH_LONG).show()
                                            },
                                            onAdFailedToShow = { err ->
                                                Toast.makeText(context, "Ad show failed: ${err.errorName}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, "Activity not available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    // Latest Error Section with Copy Button
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF261214)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C1B20)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Latest AdMob Error",
                                            color = Color(0xFFFF8A80),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    if (lastError != null) {
                                        Button(
                                            onClick = {
                                                val err = lastError!!
                                                val report = buildString {
                                                    appendLine("=== GOOGLE ADMOB ERROR REPORT ===")
                                                    appendLine("App: Velvet Music (Android)")
                                                    appendLine("Ad Type: ${err.adType}")
                                                    appendLine("Error Code: ${err.errorCode}")
                                                    appendLine("Error Name: ${err.errorName}")
                                                    appendLine("Error Message: ${err.errorMessage}")
                                                    appendLine("Ad Unit ID: ${err.adUnitId}")
                                                    appendLine("App ID: ${AdMobManager.REAL_APP_ID}")
                                                    appendLine("Timestamp: ${err.timestamp}")
                                                    appendLine("Explanation: ${err.explanation}")
                                                    appendLine("Response Info: ${err.responseInfo}")
                                                    appendLine("=================================")
                                                }
                                                clipboardManager.setText(AnnotatedString(report))
                                                Toast.makeText(context, "✅ Error details copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE50914)
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                horizontal = 10.dp,
                                                vertical = 4.dp
                                            ),
                                            modifier = Modifier.testTag("copy_admob_error_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Error", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (lastError == null) {
                                    Text(
                                        text = "No AdMob errors captured yet. If an ad request fails, full error code and server responses will appear here.",
                                        color = Color(0xFF999999),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    val err = lastError!!
                                    Text(
                                        text = "${err.adType}: ${err.errorName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Message: ${err.errorMessage}",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Why this happened: ${err.explanation}",
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Unit ID: ${err.adUnitId} • Time: ${err.timestamp}",
                                        color = Color(0xFF78909C),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Live Event Log
                    item {
                        Column {
                            Text(
                                text = "Live AdMob Event Log",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D10)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (events.isEmpty()) {
                                        item {
                                            Text(
                                                text = "Waiting for AdMob events...",
                                                color = Color(0xFF666666),
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    } else {
                                        items(events) { event ->
                                            Text(
                                                text = event,
                                                color = when {
                                                    event.contains("failed", ignoreCase = true) -> Color(0xFFFF5252)
                                                    event.contains("success", ignoreCase = true) || event.contains("loaded", ignoreCase = true) -> Color(0xFF69F0AE)
                                                    event.contains("showing", ignoreCase = true) -> Color(0xFFFFD54F)
                                                    else -> Color(0xFFB0BEC5)
                                                },
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B30)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Diagnostics", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AdUnitStatusRow(
    title: String,
    adUnitId: String,
    isLoaded: Boolean,
    onReload: () -> Unit,
    onTestShow: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                // Loaded indicator badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isLoaded) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f),
                            CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isLoaded) "Ready" else "Not Ready",
                        color = if (isLoaded) Color(0xFF81C784) else Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Unit ID: $adUnitId",
                color = Color(0xFF777777),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReload,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Request Ad", fontSize = 11.sp)
                }

                Button(
                    onClick = onTestShow,
                    enabled = isLoaded,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE50914),
                        disabledContainerColor = Color(0xFF333338)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Show Ad", fontSize = 11.sp)
                }
            }
        }
    }
}
