package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recognition.RecognitionDiagnostics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen modal diagnostic sheet presenting complete, un-truncated diagnostic information
 * from the network, provider, and exception layers.
 *
 * Works in both DEBUG and RELEASE APK builds.
 */
@Composable
fun RecognitionDiagnosticsModal(
    diagnostics: RecognitionDiagnostics?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var justCopied by remember { mutableStateOf(false) }

    val reportText = remember(diagnostics) {
        diagnostics?.toFormattedReport() ?: "VELVET RECOGNITION DIAGNOSTICS\n\nNo diagnostic report available yet."
    }

    val copyAction = {
        clipboardManager.setText(AnnotatedString(reportText))
        justCopied = true
        Toast.makeText(context, "Diagnostic report copied to clipboard", Toast.LENGTH_SHORT).show()
        scope.launch {
            delay(2000)
            justCopied = false
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* block click through */ }
            )
            .testTag("recognition_diagnostics_modal"),
        color = Color(0xFF090204)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 12.dp, bottom = bottomInset + 12.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FF2448))
                            .border(1.dp, Color(0x44FF2448), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFFFF405A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Recognition Diagnostics",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF1F2)
                        )
                        Text(
                            text = diagnostics?.failureStage?.let { "Failure: $it" } ?: diagnostics?.mode ?: "Recognition Trace",
                            fontSize = 12.sp,
                            color = Color(0xFFB5A7AA)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy button in header
                    IconButton(
                        onClick = { copyAction() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("header_copy_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy diagnostics",
                            tint = if (justCopied) Color(0xFF4EFA8B) else Color(0xFFE28492),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close button in header
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("header_close_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFD7D0D2),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Quick Status Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val httpStatus = diagnostics?.httpStatus
                val failureStage = diagnostics?.failureStage
                val statusBadgeColor = when {
                    failureStage != null -> Color(0xFFC62828)
                    httpStatus == 200 -> Color(0xFF2E7D32)
                    httpStatus != null && httpStatus in 400..599 -> Color(0xFFC62828)
                    else -> Color(0xFF424242)
                }
                val statusBadgeText = when {
                    failureStage != null -> diagnostics?.failureCode ?: "Failure"
                    httpStatus != null -> "HTTP $httpStatus"
                    else -> "No error recorded"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBadgeColor.copy(alpha = 0.25f))
                        .border(1.dp, statusBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        color = Color(0xFFFFF1F2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!diagnostics?.requestId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x19FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Req: ${diagnostics?.requestId?.take(12)}…",
                            color = Color(0xFFD7D0D2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                if (diagnostics?.auddStatus != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x19FF2448))
                            .border(1.dp, Color(0x33FF2448), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "AudD: ${diagnostics.auddStatus}",
                            color = Color(0xFFFFA0B0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                if (diagnostics?.acrcloudStatus != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x19FF2448))
                            .border(1.dp, Color(0x33FF2448), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACR: ${diagnostics.acrcloudStatus}",
                            color = Color(0xFFFFA0B0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Scrollable Diagnostic Log Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F0407))
                    .border(1.dp, Color(0x33FF2448), RoundedCornerShape(10.dp))
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = reportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFEDE0E3),
                            modifier = Modifier.testTag("diagnostic_report_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Actions Bar (Copy diagnostics & Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("close_diagnostics_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD7D0D2)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44D51035))
                ) {
                    Text(
                        text = "Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { copyAction() },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("copy_diagnostics_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (justCopied) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (justCopied) "Copied!" else "Copy diagnostics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
