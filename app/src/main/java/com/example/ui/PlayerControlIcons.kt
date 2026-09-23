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

            val path = Path().apply {
                moveTo(offsetX + 23f * scale, offsetY + 16.8f * scale)
                cubicTo(
                    offsetX + 21.7f * scale, offsetY + 16f * scale,
                    offsetX + 20f * scale, offsetY + 16.9f * scale,
                    offsetX + 20f * scale, offsetY + 18.5f * scale
                )
                lineTo(offsetX + 20f * scale, offsetY + 45.5f * scale)
                cubicTo(
                    offsetX + 20f * scale, offsetY + 47.1f * scale,
                    offsetX + 21.7f * scale, offsetY + 48f * scale,
                    offsetX + 23f * scale, offsetY + 47.2f * scale
                )
                lineTo(offsetX + 45f * scale, offsetY + 33.7f * scale)
                cubicTo(
                    offsetX + 46.4f * scale, offsetY + 32.8f * scale,
                    offsetX + 46.4f * scale, offsetY + 31.2f * scale,
                    offsetX + 45f * scale, offsetY + 30.3f * scale
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

            // Triangle
            val triangle = Path().apply {
                moveTo(offsetX + 44f * scale, offsetY + 18f * scale)
                cubicTo(
                    offsetX + 44f * scale, offsetY + 16.3f * scale,
                    offsetX + 42f * scale, offsetY + 15.4f * scale,
                    offsetX + 40.7f * scale, offsetY + 16.5f * scale
                )
                lineTo(offsetX + 21.5f * scale, offsetY + 30.5f * scale)
                cubicTo(
                    offsetX + 21.5f * scale, offsetY + 32.1f * scale,
                    offsetX + 21.5f * scale, offsetY + 31.9f * scale,
                    offsetX + 23.5f * scale, offsetY + 33.5f * scale
                )
                lineTo(offsetX + 40.7f * scale, offsetY + 47.5f * scale)
                cubicTo(
                    offsetX + 42f * scale, offsetY + 48.6f * scale,
                    offsetX + 44f * scale, offsetY + 47.7f * scale,
                    offsetX + 44f * scale, offsetY + 46f * scale
                )
                close()
            }
            drawPath(path = triangle, color = tint)

            // Bar: x=13, y=16, width=6, height=32, rx=3
            drawRoundRect(
                color = tint,
                topLeft = Offset(offsetX + 13f * scale, offsetY + 16f * scale),
                size = Size(6f * scale, 32f * scale),
                cornerRadius = CornerRadius(3f * scale, 3f * scale)
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

            // Triangle
            val triangle = Path().apply {
                moveTo(offsetX + 20f * scale, offsetY + 18f * scale)
                cubicTo(
                    offsetX + 20f * scale, offsetY + 16.3f * scale,
                    offsetX + 22f * scale, offsetY + 15.4f * scale,
                    offsetX + 23.3f * scale, offsetY + 16.5f * scale
                )
                lineTo(offsetX + 42.5f * scale, offsetY + 30.5f * scale)
                cubicTo(
                    offsetX + 42.5f * scale, offsetY + 32.1f * scale,
                    offsetX + 42.5f * scale, offsetY + 31.9f * scale,
                    offsetX + 40.5f * scale, offsetY + 33.5f * scale
                )
                lineTo(offsetX + 23.3f * scale, offsetY + 47.5f * scale)
                cubicTo(
                    offsetX + 22f * scale, offsetY + 48.6f * scale,
                    offsetX + 20f * scale, offsetY + 47.7f * scale,
                    offsetX + 20f * scale, offsetY + 46f * scale
                )
                close()
            }
            drawPath(path = triangle, color = tint)

            // Bar: x=45, y=16, width=6, height=32, rx=3
            drawRoundRect(
                color = tint,
                topLeft = Offset(offsetX + 45f * scale, offsetY + 16f * scale),
                size = Size(6f * scale, 32f * scale),
                cornerRadius = CornerRadius(3f * scale, 3f * scale)
            )
        }
    }
}
