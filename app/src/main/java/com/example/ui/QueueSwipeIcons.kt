package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Animated Trash Can with Opening Lid:
 * - Starts at subtle compact scale, then bounces out larger when reaching the threshold.
 * - Cover/lid stays closed while sliding, and tilts open smoothly when threshold & vibration trigger.
 */
@Composable
fun AnimatedTrashDeleteIcon(
    openProgress: Float,
    isPastThreshold: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Appears at comfortable base scale (~1.0f), then bounces out larger (1.32f) when threshold/vibration triggers
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isPastThreshold -> 1.32f // Bounces out larger, prominent and clear
            openProgress > 0.05f -> 0.90f + (openProgress * 0.10f)
            else -> 0.80f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trash_bounce_scale"
    )

    // Cover opens when the vibration and action threshold appear
    val lidAngle by animateFloatAsState(
        targetValue = if (isPastThreshold) 40f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trash_lid_angle"
    )

    Box(
        modifier = modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val w = size.width
            val h = size.height

            val strokeWidth = 2.0f.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // 1. Can Body (Bucket)
            val canTopY = h * 0.34f
            val canBottomY = h * 0.92f
            val canTopLeftX = w * 0.22f
            val canTopRightX = w * 0.78f
            val canBottomLeftX = w * 0.28f
            val canBottomRightX = w * 0.72f

            val bodyPath = Path().apply {
                moveTo(canTopLeftX, canTopY)
                lineTo(canBottomLeftX, canBottomY - 2.5f.dp.toPx())
                quadraticTo(canBottomLeftX, canBottomY, canBottomLeftX + 3.dp.toPx(), canBottomY)
                lineTo(canBottomRightX - 3.dp.toPx(), canBottomY)
                quadraticTo(canBottomRightX, canBottomY, canBottomRightX, canBottomY - 2.5f.dp.toPx())
                lineTo(canTopRightX, canTopY)
            }
            drawPath(path = bodyPath, color = tint, style = stroke)

            // Inner vertical ribs of the trash bucket
            val ribTopY = canTopY + 4f.dp.toPx()
            val ribBottomY = canBottomY - 3.5f.dp.toPx()
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = Offset(w * 0.41f, ribTopY),
                end = Offset(w * 0.43f, ribBottomY),
                strokeWidth = 1.5f.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = Offset(w * 0.59f, ribTopY),
                end = Offset(w * 0.57f, ribBottomY),
                strokeWidth = 1.5f.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 2. Trash Lid (Opens smoothly when vibration & threshold appear)
            val pivotX = w * 0.18f
            val pivotY = canTopY
            val liftOffset = -lidAngle * 0.16f.dp.toPx()

            rotate(degrees = -lidAngle, pivot = Offset(pivotX, pivotY)) {
                translate(left = 0f, top = liftOffset) {
                    // Rim bar
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.14f, canTopY),
                        end = Offset(w * 0.86f, canTopY),
                        strokeWidth = strokeWidth + 0.6f.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Small top handle
                    val handleW = w * 0.24f
                    val handleH = 3.5f.dp.toPx()
                    val handleX = (w - handleW) / 2f
                    val handleY = canTopY - handleH - 1.dp.toPx()

                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(handleX, handleY),
                        size = Size(handleW, handleH),
                        cornerRadius = CornerRadius(1.75f.dp.toPx(), 1.75f.dp.toPx()),
                        style = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * Animated Up Next / Play Next Icon:
 * - Bent queue line (M6 17V8H10) into play arrow (M9 5L14 8L9 11V5Z) with 3 queue lines.
 * - Appears at comfortable base scale (~1.0f), then bounces out larger when reaching the threshold.
 */
@Composable
fun AnimatedUpNextArrowIcon(
    openProgress: Float,
    isPastThreshold: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Appears at comfortable base scale (~1.0f), then bounces out larger (1.32f) when threshold/vibration triggers
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isPastThreshold -> 1.32f // Bounces out larger, prominent and clear
            openProgress > 0.05f -> 0.90f + (openProgress * 0.10f)
            else -> 0.80f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "upnext_bounce_scale"
    )

    Box(
        modifier = modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val s = size.width / 24f
            val strokeWidth = 1.8f * s
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // 1. Bent queue line: M6 17V8H10
            val bentPath = Path().apply {
                moveTo(6f * s, 17f * s)
                lineTo(6f * s, 8f * s)
                lineTo(10f * s, 8f * s)
            }
            drawPath(path = bentPath, color = tint, style = stroke)

            // 2. Play arrow, moved upward: M9 5L14 8L9 11V5Z
            val playPath = Path().apply {
                moveTo(9f * s, 5f * s)
                lineTo(14f * s, 8f * s)
                lineTo(9f * s, 11f * s)
                close()
            }
            drawPath(path = playPath, color = tint)

            // 3. Queue lines
            // Line 1: M15.5 7H19
            drawLine(
                color = tint,
                start = Offset(15.5f * s, 7f * s),
                end = Offset(19f * s, 7f * s),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Line 2: M11 12H19
            drawLine(
                color = tint,
                start = Offset(11f * s, 12f * s),
                end = Offset(19f * s, 12f * s),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Line 3: M11 17H19
            drawLine(
                color = tint,
                start = Offset(11f * s, 17f * s),
                end = Offset(19f * s, 17f * s),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
