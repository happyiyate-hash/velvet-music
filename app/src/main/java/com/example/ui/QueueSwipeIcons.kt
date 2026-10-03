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
 * - Cover/lid automatically opens smoothly as the user swipes left.
 * - Icon size is small, refined, and compact. Reduced on full maximum drag.
 */
@Composable
fun AnimatedTrashDeleteIcon(
    openProgress: Float,
    isPastThreshold: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Reduced scale on full maximum drag; stays small and refined
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isPastThreshold -> 0.92f // Reduced on full maximum drag
            openProgress > 0.15f -> 0.68f + openProgress * 0.24f
            else -> 0.58f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trash_bounce_scale"
    )

    val lidAngle by animateFloatAsState(
        targetValue = if (isPastThreshold) 34f else (openProgress * 28f).coerceIn(0f, 34f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trash_lid_angle"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val w = size.width
            val h = size.height

            val strokeWidth = 1.5f.dp.toPx()
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
                lineTo(canBottomLeftX, canBottomY - 1.5f.dp.toPx())
                quadraticTo(canBottomLeftX, canBottomY, canBottomLeftX + 2.dp.toPx(), canBottomY)
                lineTo(canBottomRightX - 2.dp.toPx(), canBottomY)
                quadraticTo(canBottomRightX, canBottomY, canBottomRightX, canBottomY - 1.5f.dp.toPx())
                lineTo(canTopRightX, canTopY)
            }
            drawPath(path = bodyPath, color = tint, style = stroke)

            // Inner vertical ribs of the trash bucket
            val ribTopY = canTopY + 3.dp.toPx()
            val ribBottomY = canBottomY - 2.5f.dp.toPx()
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = Offset(w * 0.41f, ribTopY),
                end = Offset(w * 0.43f, ribBottomY),
                strokeWidth = 1.2f.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = Offset(w * 0.59f, ribTopY),
                end = Offset(w * 0.57f, ribBottomY),
                strokeWidth = 1.2f.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 2. Trash Lid (Rotates and tilts open smoothly)
            val pivotX = w * 0.18f
            val pivotY = canTopY
            val liftOffset = -lidAngle * 0.12f.dp.toPx()

            rotate(degrees = -lidAngle, pivot = Offset(pivotX, pivotY)) {
                translate(left = 0f, top = liftOffset) {
                    // Rim bar
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.16f, canTopY),
                        end = Offset(w * 0.84f, canTopY),
                        strokeWidth = strokeWidth + 0.4f.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Small top handle
                    val handleW = w * 0.22f
                    val handleH = 2.5f.dp.toPx()
                    val handleX = (w - handleW) / 2f
                    val handleY = canTopY - handleH - 0.8f.dp.toPx()

                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(handleX, handleY),
                        size = Size(handleW, handleH),
                        cornerRadius = CornerRadius(1.5f.dp.toPx(), 1.5f.dp.toPx()),
                        style = Stroke(width = 1.2f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * Animated Up Next Icon:
 * - Smooth stroke arrow curving 90 degrees to the right into the next slot with queue lines.
 * - Icon size is small, refined, and compact. Reduced on full maximum drag.
 */
@Composable
fun AnimatedUpNextArrowIcon(
    openProgress: Float,
    isPastThreshold: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Reduced scale on full maximum drag; stays small and refined
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isPastThreshold -> 0.92f // Reduced on full maximum drag
            openProgress > 0.15f -> 0.68f + openProgress * 0.24f
            else -> 0.58f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "upnext_bounce_scale"
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val w = size.width
            val h = size.height

            val strokeWidth = 1.6f.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Queue lines in upper right corner
            drawLine(
                color = tint.copy(alpha = 0.8f),
                start = Offset(w * 0.44f, h * 0.22f),
                end = Offset(w * 0.84f, h * 0.22f),
                strokeWidth = 1.3f.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.8f),
                start = Offset(w * 0.44f, h * 0.42f),
                end = Offset(w * 0.84f, h * 0.42f),
                strokeWidth = 1.3f.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Smooth Down-and-Right bending Arrow
            val arrowStartX = w * 0.22f
            val arrowStartY = h * 0.18f
            val cornerY = h * 0.68f
            val cornerRadius = 4.dp.toPx()
            val arrowEndX = w * 0.78f

            val arrowPath = Path().apply {
                moveTo(arrowStartX, arrowStartY)
                lineTo(arrowStartX, cornerY - cornerRadius)
                quadraticTo(arrowStartX, cornerY, arrowStartX + cornerRadius, cornerY)
                lineTo(arrowEndX, cornerY)
            }
            drawPath(path = arrowPath, color = tint, style = stroke)

            // Arrowhead at arrowEndX pointing to the right
            val headLen = 3.8f.dp.toPx()
            val headPath = Path().apply {
                moveTo(arrowEndX - headLen, cornerY - headLen)
                lineTo(arrowEndX, cornerY)
                lineTo(arrowEndX - headLen, cornerY + headLen)
            }
            drawPath(path = headPath, color = tint, style = stroke)
        }
    }
}
