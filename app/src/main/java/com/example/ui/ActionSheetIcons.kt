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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium unified icon family for the Now Playing Action Sheet:
 * - Consistent 32x32 canonical coordinate grid
 * - Uniform corner radii and balanced visual weight
 * - High-contrast ash-white (#F4F4F2) default with restrained destructive red (#FF6B72)
 */

val ActionIconAshWhite = Color(0xFFF4F4F2)
val ActionIconSecondary = Color(0xFFAEB0B5)
val ActionIconDestructive = Color(0xFFFF5252)
val ActionFavoriteRed = Color(0xFFFF3358)
val ActionSheetBackground = Color(0xFF16171A)
val ActionSheetDivider = Color(0xFF27292E)

@Composable
fun ActionPlayNextIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Play next"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f

            // Smoothly rounded play triangle pointing right
            val left = ox + 9.5f * scale
            val right = ox + 25f * scale
            val top = oy + 7.5f * scale
            val bottom = oy + 24.5f * scale
            val midY = oy + 16f * scale

            val path = Path().apply {
                moveTo(left + 2f * scale, top)
                cubicTo(
                    left + 0.8f * scale, top - 0.2f * scale,
                    left, top + 1f * scale,
                    left, top + 2.2f * scale
                )
                lineTo(left, bottom - 2.2f * scale)
                cubicTo(
                    left, bottom - 1f * scale,
                    left + 0.8f * scale, bottom + 0.2f * scale,
                    left + 2f * scale, bottom
                )
                lineTo(right - 1.8f * scale, midY + 1.4f * scale)
                cubicTo(
                    right + 0.6f * scale, midY + 0.7f * scale,
                    right + 0.6f * scale, midY - 0.7f * scale,
                    right - 1.8f * scale, midY - 1.4f * scale
                )
                close()
            }
            drawPath(path = path, color = tint)
        }
    }
}

@Composable
fun ActionQueueIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Add to queue"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.4f * scale

            // Queue list bars on the left
            drawLine(
                color = tint,
                start = Offset(ox + 5.5f * scale, oy + 8.5f * scale),
                end = Offset(ox + 18.5f * scale, oy + 8.5f * scale),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(ox + 5.5f * scale, oy + 15.5f * scale),
                end = Offset(ox + 15f * scale, oy + 15.5f * scale),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(ox + 5.5f * scale, oy + 22.5f * scale),
                end = Offset(ox + 13f * scale, oy + 22.5f * scale),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Musical note with stem & notehead on the right
            val stemX = ox + 24.5f * scale
            val noteHeadCenter = Offset(ox + 22f * scale, oy + 22.5f * scale)
            val noteHeadRadius = 3f * scale

            drawCircle(
                color = tint,
                radius = noteHeadRadius,
                center = noteHeadCenter
            )
            drawLine(
                color = tint,
                start = Offset(stemX, oy + 22.5f * scale),
                end = Offset(stemX, oy + 10f * scale),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            // Musical note flag / hook
            val flagPath = Path().apply {
                moveTo(stemX, oy + 10f * scale)
                cubicTo(
                    stemX + 3.5f * scale, oy + 10.5f * scale,
                    stemX + 4.5f * scale, oy + 13.5f * scale,
                    stemX + 3.5f * scale, oy + 16.5f * scale
                )
            }
            drawPath(
                path = flagPath,
                color = tint,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun ActionFavoriteIcon(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = ActionFavoriteRed,
    contentDescription: String? = "Favourite"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.4f * scale

            // Balanced, proportional heart path (thinner than previous wide version, perfectly centered)
            val path = Path().apply {
                moveTo(ox + 16f * scale, oy + 25.5f * scale)
                // Left flank up
                cubicTo(
                    ox + 11.2f * scale, oy + 21.2f * scale,
                    ox + 6.0f * scale, oy + 17.5f * scale,
                    ox + 6.0f * scale, oy + 13.0f * scale
                )
                // Left shoulder curve
                cubicTo(
                    ox + 6.0f * scale, oy + 8.8f * scale,
                    ox + 9.2f * scale, oy + 6.5f * scale,
                    ox + 12.5f * scale, oy + 6.5f * scale
                )
                // Dip into center cleft
                cubicTo(
                    ox + 14.2f * scale, oy + 6.5f * scale,
                    ox + 15.4f * scale, oy + 7.6f * scale,
                    ox + 16f * scale, oy + 9.5f * scale
                )
                // Right lobe out from center cleft
                cubicTo(
                    ox + 16.6f * scale, oy + 7.6f * scale,
                    ox + 17.8f * scale, oy + 6.5f * scale,
                    ox + 19.5f * scale, oy + 6.5f * scale
                )
                // Right shoulder curve
                cubicTo(
                    ox + 22.8f * scale, oy + 6.5f * scale,
                    ox + 26.0f * scale, oy + 8.8f * scale,
                    ox + 26.0f * scale, oy + 13.0f * scale
                )
                // Right flank down to bottom tip
                cubicTo(
                    ox + 26.0f * scale, oy + 17.5f * scale,
                    ox + 20.8f * scale, oy + 21.2f * scale,
                    ox + 16f * scale, oy + 25.5f * scale
                )
                close()
            }

            if (isFavorite) {
                drawPath(path = path, color = tint)
            } else {
                // Consistent 2.4dp stroke matching all action sheet icons
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun ActionPlaylistIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Add to playlist"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.4f * scale

            // Rounded Folder Outline
            val folderPath = Path().apply {
                moveTo(ox + 6.5f * scale, oy + 9f * scale)
                lineTo(ox + 12f * scale, oy + 9f * scale)
                lineTo(ox + 14.5f * scale, oy + 11.5f * scale)
                lineTo(ox + 25.5f * scale, oy + 11.5f * scale)
                cubicTo(
                    ox + 26.5f * scale, oy + 11.5f * scale,
                    ox + 27f * scale, oy + 12f * scale,
                    ox + 27f * scale, oy + 13f * scale
                )
                lineTo(ox + 27f * scale, oy + 23f * scale)
                cubicTo(
                    ox + 27f * scale, oy + 24.5f * scale,
                    ox + 25.8f * scale, oy + 25f * scale,
                    ox + 24.5f * scale, oy + 25f * scale
                )
                lineTo(ox + 7.5f * scale, oy + 25f * scale)
                cubicTo(
                    ox + 6.2f * scale, oy + 25f * scale,
                    ox + 5f * scale, oy + 24.5f * scale,
                    ox + 5f * scale, oy + 23f * scale
                )
                lineTo(ox + 5f * scale, oy + 10.5f * scale)
                cubicTo(
                    ox + 5f * scale, oy + 9.5f * scale,
                    ox + 5.5f * scale, oy + 9f * scale,
                    ox + 6.5f * scale, oy + 9f * scale
                )
                close()
            }
            drawPath(
                path = folderPath,
                color = tint,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Centered Plus symbol inside folder
            val cx = ox + 16f * scale
            val cy = oy + 18.2f * scale
            val plusR = 3.6f * scale
            drawLine(
                color = tint,
                start = Offset(cx - plusR, cy),
                end = Offset(cx + plusR, cy),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(cx, cy - plusR),
                end = Offset(cx, cy + plusR),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ActionVisualizerIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Audio Visualizer"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val barW = 2.8f * scale
            val r = barW / 2f

            // 5 Balanced audio equalizer bars
            val bars = listOf(
                Pair(ox + 6.5f * scale, 12f * scale),
                Pair(ox + 11.25f * scale, 20f * scale),
                Pair(ox + 16f * scale, 15f * scale),
                Pair(ox + 20.75f * scale, 22f * scale),
                Pair(ox + 25.5f * scale, 13f * scale)
            )

            bars.forEach { (bx, h) ->
                val topY = oy + 16f * scale - (h / 2f)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(bx - r, topY),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(r, r)
                )
            }
        }
    }
}

@Composable
fun ActionLyricsIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Synced Lyrics"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.2f * scale

            // Rounded card / speech frame
            val rectWidth = 22f * scale
            val rectHeight = 18f * scale
            val left = ox + 5f * scale
            val top = oy + 7f * scale
            val cornerR = 5f * scale

            drawRoundRect(
                color = tint,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(cornerR, cornerR),
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 3 Clean lyric text lines inside card
            val lineStroke = 2f * scale
            drawLine(
                color = tint,
                start = Offset(left + 4f * scale, top + 5f * scale),
                end = Offset(left + 14f * scale, top + 5f * scale),
                strokeWidth = lineStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(left + 4f * scale, top + 9f * scale),
                end = Offset(left + 18f * scale, top + 9f * scale),
                strokeWidth = lineStroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(left + 4f * scale, top + 13f * scale),
                end = Offset(left + 11f * scale, top + 13f * scale),
                strokeWidth = lineStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ActionShareIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Share song"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.4f * scale

            val nodeA = Offset(ox + 8.5f * scale, oy + 16f * scale)
            val nodeB = Offset(ox + 23.5f * scale, oy + 9f * scale)
            val nodeC = Offset(ox + 23.5f * scale, oy + 23f * scale)
            val nodeR = 3.2f * scale

            // Connective lines
            drawLine(color = tint, start = nodeA, end = nodeB, strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = tint, start = nodeA, end = nodeC, strokeWidth = strokeW, cap = StrokeCap.Round)

            // 3 Nodes with solid white fill
            drawCircle(color = tint, radius = nodeR, center = nodeA)
            drawCircle(color = tint, radius = nodeR, center = nodeB)
            drawCircle(color = tint, radius = nodeR, center = nodeC)
        }
    }
}

@Composable
fun ActionInfoIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconAshWhite,
    contentDescription: String? = "Song information"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.3f * scale

            val center = Offset(ox + 16f * scale, oy + 16f * scale)
            val ringR = 10.5f * scale

            // Outer ring
            drawCircle(
                color = tint,
                radius = ringR,
                center = center,
                style = Stroke(width = strokeW)
            )

            // Top dot
            val dotCenter = Offset(ox + 16f * scale, oy + 11.2f * scale)
            drawCircle(color = tint, radius = 1.4f * scale, center = dotCenter)

            // Bottom line
            drawLine(
                color = tint,
                start = Offset(ox + 16f * scale, oy + 14.5f * scale),
                end = Offset(ox + 16f * scale, oy + 21f * scale),
                strokeWidth = 2.4f * scale,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ActionDeleteIcon(
    modifier: Modifier = Modifier,
    tint: Color = ActionIconDestructive,
    contentDescription: String? = "Delete from library"
) {
    Box(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
            this.role = Role.Image
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scale = size.minDimension / 32f
            val ox = (size.width - 32f * scale) / 2f
            val oy = (size.height - 32f * scale) / 2f
            val strokeW = 2.2f * scale

            // Top Handle
            val handlePath = Path().apply {
                moveTo(ox + 13f * scale, oy + 8f * scale)
                lineTo(ox + 13f * scale, oy + 6.2f * scale)
                cubicTo(
                    ox + 13f * scale, oy + 5.2f * scale,
                    ox + 13.8f * scale, oy + 4.8f * scale,
                    ox + 14.8f * scale, oy + 4.8f * scale
                )
                lineTo(ox + 17.2f * scale, oy + 4.8f * scale)
                cubicTo(
                    ox + 18.2f * scale, oy + 4.8f * scale,
                    ox + 19f * scale, oy + 5.2f * scale,
                    ox + 19f * scale, oy + 6.2f * scale
                )
                lineTo(ox + 19f * scale, oy + 8f * scale)
            }
            drawPath(path = handlePath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Lid Bar
            drawLine(
                color = tint,
                start = Offset(ox + 7.5f * scale, oy + 8f * scale),
                end = Offset(ox + 24.5f * scale, oy + 8f * scale),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Can Body (tapering slightly downwards with smooth rounded bottom corners)
            val canPath = Path().apply {
                moveTo(ox + 9.5f * scale, oy + 10.5f * scale)
                lineTo(ox + 10.5f * scale, oy + 23.5f * scale)
                cubicTo(
                    ox + 10.6f * scale, oy + 25.5f * scale,
                    ox + 12f * scale, oy + 26.5f * scale,
                    ox + 13.5f * scale, oy + 26.5f * scale
                )
                lineTo(ox + 18.5f * scale, oy + 26.5f * scale)
                cubicTo(
                    ox + 20f * scale, oy + 26.5f * scale,
                    ox + 21.4f * scale, oy + 25.5f * scale,
                    ox + 21.5f * scale, oy + 23.5f * scale
                )
                lineTo(ox + 22.5f * scale, oy + 10.5f * scale)
            }
            drawPath(path = canPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Two subtle inner vertical ribs
            drawLine(
                color = tint,
                start = Offset(ox + 13.8f * scale, oy + 13f * scale),
                end = Offset(ox + 14.2f * scale, oy + 23f * scale),
                strokeWidth = 1.8f * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(ox + 18.2f * scale, oy + 13f * scale),
                end = Offset(ox + 17.8f * scale, oy + 23f * scale),
                strokeWidth = 1.8f * scale,
                cap = StrokeCap.Round
            )
        }
    }
}
