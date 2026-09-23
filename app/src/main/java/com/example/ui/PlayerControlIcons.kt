package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Custom player icons matching the bold, rounded design language:
 * - Pure solid white (#FFFFFF)
 * - Rounded triangle vertices and rounded pause/skip bars
 * - Scaled to a 64x64 canonical viewBox
 */

@Composable
fun PlayerPlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    contentDescription: String? = "Play"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 64f
            // Center in canvas if aspect ratio is not 1:1
            val offsetX = (size.width - 64f * scale) / 2f
            val offsetY = (size.height - 64f * scale) / 2f

            val left = offsetX + 17f * scale
            val right = offsetX + 51f * scale
            val top = offsetY + 14f * scale
            val bottom = offsetY + 50f * scale
            val midY = offsetY + 32f * scale

            val path = Path().apply {
                moveTo(left + 3f * scale, top)
                cubicTo(
                    left + 1.2f * scale, top - 0.2f * scale,
                    left, top + 1.4f * scale,
                    left, top + 3.2f * scale
                )
                lineTo(left, bottom - 3.2f * scale)
                cubicTo(
                    left, bottom - 1.4f * scale,
                    left + 1.2f * scale, bottom + 0.2f * scale,
                    left + 3f * scale, bottom
                )
                lineTo(right - 2.5f * scale, midY + 1.8f * scale)
                cubicTo(
                    right + 0.8f * scale, midY + 0.9f * scale,
                    right + 0.8f * scale, midY - 0.9f * scale,
                    right - 2.5f * scale, midY - 1.8f * scale
                )
                close()
            }
            drawPath(path = path, color = tint)
        }
    }
}

@Composable
fun PlayerPauseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    contentDescription: String? = "Pause"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 64f
            val offsetX = (size.width - 64f * scale) / 2f
            val offsetY = (size.height - 64f * scale) / 2f

            // Left bar: x=19, y=15, width=10, height=34, rx=5
            drawRoundRect(
                color = tint,
                topLeft = Offset(offsetX + 19f * scale, offsetY + 15f * scale),
                size = Size(10f * scale, 34f * scale),
                cornerRadius = CornerRadius(5f * scale, 5f * scale)
            )

            // Right bar: x=35, y=15, width=10, height=34, rx=5
            drawRoundRect(
                color = tint,
                topLeft = Offset(offsetX + 35f * scale, offsetY + 15f * scale),
                size = Size(10f * scale, 34f * scale),
                cornerRadius = CornerRadius(5f * scale, 5f * scale)
            )
        }
    }
}

@Composable
fun PlayerPreviousIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    contentDescription: String? = "Previous"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 64f
            val offsetX = (size.width - 64f * scale) / 2f
            val offsetY = (size.height - 64f * scale) / 2f

            // Elongated triangle pointing left directly at the standing line
            val backX = offsetX + 53f * scale
            val tipX = offsetX + 20f * scale
            val topY = offsetY + 14f * scale
            val bottomY = offsetY + 50f * scale
            val midY = offsetY + 32f * scale

            val triangle = Path().apply {
                moveTo(backX - 2.5f * scale, topY)
                cubicTo(
                    backX - 0.5f * scale, topY - 0.4f * scale,
                    backX, topY + 1.2f * scale,
                    backX, topY + 3.2f * scale
                )
                lineTo(backX, bottomY - 3.2f * scale)
                cubicTo(
                    backX, bottomY - 1.2f * scale,
                    backX - 0.5f * scale, bottomY + 0.4f * scale,
                    backX - 2.5f * scale, bottomY
                )
                lineTo(tipX + 2.5f * scale, midY + 1.8f * scale)
                cubicTo(
                    tipX - 0.8f * scale, midY + 0.9f * scale,
                    tipX - 0.8f * scale, midY - 0.9f * scale,
                    tipX + 2.5f * scale, midY - 1.8f * scale
                )
                close()
            }
            drawPath(path = triangle, color = tint)

            // Standing line (bar)
            drawRoundRect(
                color = tint,
                topLeft = Offset(offsetX + 10f * scale, offsetY + 14f * scale),
                size = Size(6.5f * scale, 36f * scale),
                cornerRadius = CornerRadius(3.25f * scale, 3.25f * scale)
            )
        }
    }
}

@Composable
fun PlayerNextIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    contentDescription: String? = "Next"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 64f
            val offsetX = (size.width - 64f * scale) / 2f
            val offsetY = (size.height - 64f * scale) / 2f

            // Elongated triangle pointing right directly at the standing line
            val backX = offsetX + 11f * scale
            val tipX = offsetX + 44f * scale
            val topY = offsetY + 14f * scale
            val bottomY = offsetY + 50f * scale
            val midY = offsetY + 32f * scale

            val triangle = Path().apply {
                moveTo(backX + 2.5f * scale, topY)
                cubicTo(
                    backX + 0.5f * scale, topY - 0.4f * scale,
                    backX, topY + 1.2f * scale,
                    backX, topY + 3.2f * scale
                )
                lineTo(backX, bottomY - 3.2f * scale)
                cubicTo(
                    backX, bottomY - 1.2f * scale,
                    backX + 0.5f * scale, bottomY + 0.4f * scale,
                    backX + 2.5f * scale, bottomY
                )
                lineTo(tipX - 2.5f * scale, midY + 1.8f * scale)
                cubicTo(
                    tipX + 0.8f * scale, midY + 0.9f * scale,
                    tipX + 0.8f * scale, midY - 0.9f * scale,
                    tipX - 2.5f * scale, midY - 1.8f * scale
                )
                close()
            }
            drawPath(path = triangle, color = tint)

            // Standing line (bar)
            drawRoundRect(
                color = tint,
                topLeft = Offset(offsetX + 47.5f * scale, offsetY + 14f * scale),
                size = Size(6.5f * scale, 36f * scale),
                cornerRadius = CornerRadius(3.25f * scale, 3.25f * scale)
            )
        }
    }
}
