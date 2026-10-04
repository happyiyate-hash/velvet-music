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
 * - Perfectly sized (24dp canvas in 28dp box), popping out cleanly at 1.08f without becoming oversized.
 * - Reduced delicate stroke width (1.4dp) matching sleek YouTube Music styling.
 * - Cover/lid stays closed while sliding, and tilts open smoothly when threshold & vibration trigger.
 */
@Composable
fun AnimatedTrashDeleteIcon(
    openProgress: Float,
    isPastThreshold: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Appears at subtle scale, then pops out cleanly at 1.08f when threshold/vibration triggers
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isPastThreshold -> 1.08f // Crisp, elegant pop without being too big
            openProgress > 0.05f -> 0.85f + (openProgress * 0.15f)
            else -> 0.78f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trash_bounce_scale"
    )

    // Cover opens when the vibration and action threshold appear
    val lidAngle by animateFloatAsState(
        targetValue = if (isPastThreshold) 38f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trash_lid_angle"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height

            val strokeWidth = 1.4f.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // 1. Can Body (Bucket)
            val canTopY = h * 0.35f
            val canBottomY = h * 0.91f
            val canTopLeftX = w * 0.22f
            val canTopRightX = w * 0.78f
            val canBottomLeftX = w * 0.27f
            val canBottomRightX = w * 0.73f

            val bodyPath = Path().apply {
                moveTo(canTopLeftX, canTopY)
                lineTo(canBottomLeftX, canBottomY - 2.dp.toPx())
                quadraticTo(canBottomLeftX, canBottomY, canBottomLeftX + 2.5f.dp.toPx(), canBottomY)
                lineTo(canBottomRightX - 2.5f.dp.toPx(), canBottomY)
                quadraticTo(canBottomRightX, canBottomY, canBottomRightX, canBottomY - 2.dp.toPx())
                lineTo(canTopRightX, canTopY)
            }
            drawPath(path = bodyPath, color = tint, style = stroke)

            // Inner vertical ribs of the trash bucket
            val ribTopY = canTopY + 3.5f.dp.toPx()
            val ribBottomY = canBottomY - 3f.dp.toPx()
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = Offset(w * 0.42f, ribTopY),
                end = Offset(w * 0.43f, ribBottomY),
                strokeWidth = 1.1f.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = Offset(w * 0.58f, ribTopY),
                end = Offset(w * 0.57f, ribBottomY),
                strokeWidth = 1.1f.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 2. Trash Lid (Opens smoothly when vibration & threshold appear)
            val pivotX = w * 0.18f
            val pivotY = canTopY
            val liftOffset = -lidAngle * 0.13f.dp.toPx()

            rotate(degrees = -lidAngle, pivot = Offset(pivotX, pivotY)) {
                translate(left = 0f, top = liftOffset) {
                    // Rim bar
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.15f, canTopY),
                        end = Offset(w * 0.85f, canTopY),
                        strokeWidth = strokeWidth + 0.35f.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Small top handle
                    val handleW = w * 0.22f
                    val handleH = 2.8f.dp.toPx()
                    val handleX = (w - handleW) / 2f
                    val handleY = canTopY - handleH - 0.8f.dp.toPx()

                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(handleX, handleY),
                        size = Size(handleW, handleH),
                        cornerRadius = CornerRadius(1.4f.dp.toPx(), 1.4f.dp.toPx()),
                        style = Stroke(width = 1.1f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * Animated Up Next / Play Next Icon:
 * - Tiny play pointing arrow, sleek bent line, and extended queue lines with clean YouTube Music styling.
 * - Perfectly sized (24dp canvas in 28dp box), popping out cleanly at 1.08f without being too big.
 */
@Composable
fun AnimatedUpNextArrowIcon(
    openProgress: Float,
    isPastThreshold: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    // Appears at subtle scale, then pops out cleanly at 1.08f when threshold/vibration triggers
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isPastThreshold -> 1.08f // Crisp, elegant pop without being too big
            openProgress > 0.05f -> 0.85f + (openProgress * 0.15f)
            else -> 0.78f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "upnext_bounce_scale"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val s = size.width / 24f
            val strokeWidth = 1.35f * s
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // 1. Sleek bent queue line: M5.5 17.5V8.5H9.0
            val bentPath = Path().apply {
                moveTo(5.5f * s, 17.5f * s)
                lineTo(5.5f * s, 8.5f * s)
                lineTo(9.0f * s, 8.5f * s)
            }
            drawPath(path = bentPath, color = tint, style = stroke)

            // 2. Play arrow, tiny and sharp: M8.5 6.2L12.5 8.5L8.5 10.8V6.2Z
            val playPath = Path().apply {
                moveTo(8.5f * s, 6.2f * s)
                lineTo(12.5f * s, 8.5f * s)
                lineTo(8.5f * s, 10.8f * s)
                close()
            }
            drawPath(path = playPath, color = tint)

            // 3. Queue lines: extended nicely to the right (x=20.5), sleek and clean
            // Line 1: from x=14.2 to x=20.5 at y=7.2
            drawLine(
                color = tint,
                start = Offset(14.2f * s, 7.2f * s),
                end = Offset(20.5f * s, 7.2f * s),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Line 2: from x=9.5 to x=20.5 at y=12.5
            drawLine(
                color = tint,
                start = Offset(9.5f * s, 12.5f * s),
                end = Offset(20.5f * s, 12.5f * s),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Line 3: from x=9.5 to x=20.5 at y=17.5
            drawLine(
                color = tint,
                start = Offset(9.5f * s, 17.5f * s),
                end = Offset(20.5f * s, 17.5f * s),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
