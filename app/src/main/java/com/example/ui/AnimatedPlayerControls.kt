package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Repeat Icon:
 * Rather than rotating the whole icon, the actual vector strokes travel along their
 * racetrack path. When tapped, the two curved arrows circulate completely around each other
 * for one full cycle (~600ms) with arrowheads dynamically pointing along the tangents,
 * settling into the active state in the artwork's accent color.
 */
@Composable
fun AnimatedRepeatIcon(
    isRepeat: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchSize: Dp = 48.dp,
    iconSize: Dp = 26.dp
) {
    val travelOffset = remember { Animatable(0f) }

    LaunchedEffect(isRepeat) {
        if (isRepeat) {
            // Circulate forward one full revolution (0f -> 1f) then reset to 0f
            travelOffset.snapTo(0f)
            travelOffset.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
            travelOffset.snapTo(0f)
        } else {
            // Subtle reverse easing when turning off
            travelOffset.animateTo(
                targetValue = -0.20f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            travelOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
            )
        }
    }

    val iconColor by animateColorAsState(
        targetValue = if (isRepeat) activeColor else Color.White.copy(alpha = 0.40f),
        animationSpec = tween(durationMillis = 350),
        label = "repeatColor"
    )

    Box(
        modifier = modifier
            .size(touchSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val scale = size.width / 48f
            val strokeWidthPx = 2.4.dp.toPx()

            // Construct the canonical racetrack closed path in 48x48 space:
            // Clockwise: top run (14,14)->(34,14), right cap (arc r=10), bottom run (34,34)->(14,34), left cap (arc r=10)
            val racetrack = Path().apply {
                moveTo(14f * scale, 14f * scale)
                lineTo(34f * scale, 14f * scale)
                arcTo(
                    rect = Rect(24f * scale, 14f * scale, 44f * scale, 34f * scale),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(14f * scale, 34f * scale)
                arcTo(
                    rect = Rect(4f * scale, 14f * scale, 24f * scale, 34f * scale),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(racetrack, forceClosed = true)
            val totalLength = pathMeasure.length
            if (totalLength <= 0f) return@Canvas

            // Resting positions:
            // Arrow 1 head at distance (15 * scale) on top run, pointing right
            // Arrow 2 head is at (15 * scale + totalLength / 2) on bottom run, pointing left
            val baseHead1 = 15f * scale
            val baseHead2 = baseHead1 + totalLength * 0.5f
            val strokeLength = totalLength * 0.39f // each stroke occupies ~39% of circumference

            val offsetDist = travelOffset.value * totalLength

            // Draw both circulating arrows
            val heads = listOf(
                (baseHead1 + offsetDist).mod(totalLength),
                (baseHead2 + offsetDist).mod(totalLength)
            )

            for (headDist in heads) {
                val tailDist = (headDist - strokeLength).mod(totalLength)

                // Extract and draw the body segment
                val segmentPath = Path()
                if (tailDist <= headDist) {
                    pathMeasure.getSegment(tailDist, headDist, segmentPath, true)
                } else {
                    pathMeasure.getSegment(tailDist, totalLength, segmentPath, true)
                    pathMeasure.getSegment(0f, headDist, segmentPath, true)
                }

                drawPath(
                    path = segmentPath,
                    color = iconColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw the dynamic arrowhead at the head distance aligned with local tangent
                val headPos = pathMeasure.getPosition(headDist)
                val tangent = pathMeasure.getTangent(headDist)
                val angle = atan2(tangent.y, tangent.x)
                val wingAngle = 0.72f // ~41 degrees
                val wingLen = 4.8f * scale

                val wing1 = headPos - Offset(
                    cos(angle - wingAngle) * wingLen,
                    sin(angle - wingAngle) * wingLen
                )
                val wing2 = headPos - Offset(
                    cos(angle + wingAngle) * wingLen,
                    sin(angle + wingAngle) * wingLen
                )

                drawLine(
                    color = iconColor,
                    start = wing1,
                    end = headPos,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = iconColor,
                    start = wing2,
                    end = headPos,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Animated Shuffle Icon:
 * Instead of rotating or bouncing the whole icon, the two crossing paths flex,
 * slide through their crossing point, and the arrowheads push forward along their tracks,
 * giving the visual feeling that the two lines switch places and settle cleanly into place.
 */
@Composable
fun AnimatedShuffleIcon(
    isShuffle: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchSize: Dp = 48.dp,
    iconSize: Dp = 26.dp
) {
    val shuffleAnim = remember { Animatable(0f) }

    LaunchedEffect(isShuffle) {
        if (isShuffle) {
            // Energetic flex & stroke slide through crossing point (0f -> 1f)
            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
            )
            shuffleAnim.snapTo(0f)
        } else {
            // Gentle reverse pulse
            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(
                targetValue = 0.6f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
            shuffleAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
            )
        }
    }

    val iconColor by animateColorAsState(
        targetValue = if (isShuffle) activeColor else Color.White.copy(alpha = 0.40f),
        animationSpec = tween(durationMillis = 350),
        label = "shuffleColor"
    )

    Box(
        modifier = modifier
            .size(touchSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val scale = size.width / 48f
            val strokeWidthPx = 2.4.dp.toPx()

            val progress = shuffleAnim.value
            // Smooth half-sine curve peaking at halfway through animation
            val pulse = sin(progress * PI.toFloat())
            val midYOffset = pulse * 3.4f * scale
            val arrowNudge = pulse * 3.0f * scale
            val leftPull = pulse * 1.5f * scale

            // Path A: Top-left to bottom-right
            // Rest: (6, 14) -> (12, 14) -> S-curve to (36, 34) -> (42, 34)
            val pathA = Path().apply {
                moveTo((6f + leftPull) * scale, 14f * scale)
                lineTo((12f + leftPull) * scale, 14f * scale)
                cubicTo(
                    17f * scale, (14f + midYOffset * 0.4f) * scale,
                    20f * scale, (24f - midYOffset) * scale,
                    24f * scale, (24f - midYOffset) * scale
                )
                cubicTo(
                    28f * scale, (24f - midYOffset) * scale,
                    31f * scale, (34f - midYOffset * 0.4f) * scale,
                    36f * scale, 34f * scale
                )
                lineTo((42f + arrowNudge) * scale, 34f * scale)
            }

            // Path B: Bottom-left to top-right
            // Rest: (6, 34) -> (12, 34) -> S-curve to (36, 14) -> (42, 14)
            val pathB = Path().apply {
                moveTo((6f + leftPull) * scale, 34f * scale)
                lineTo((12f + leftPull) * scale, 34f * scale)
                cubicTo(
                    17f * scale, (34f - midYOffset * 0.4f) * scale,
                    20f * scale, (24f + midYOffset) * scale,
                    24f * scale, (24f + midYOffset) * scale
                )
                cubicTo(
                    28f * scale, (24f + midYOffset) * scale,
                    31f * scale, (14f + midYOffset * 0.4f) * scale,
                    36f * scale, 14f * scale
                )
                lineTo((42f + arrowNudge) * scale, 14f * scale)
            }

            // Draw crossing paths
            drawPath(
                path = pathA,
                color = iconColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = pathB,
                color = iconColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Arrowheads at the ends
            val wingLen = 5.0f * scale
            val wingX = 5.0f * scale

            // Arrow for Path A (bottom-right: tip at (42 + arrowNudge, 34))
            val tipA = Offset((42f + arrowNudge) * scale, 34f * scale)
            drawLine(
                color = iconColor,
                start = Offset(tipA.x - wingX, tipA.y - wingLen),
                end = tipA,
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
            drawLine(
                color = iconColor,
                start = Offset(tipA.x - wingX, tipA.y + wingLen),
                end = tipA,
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )

            // Arrow for Path B (top-right: tip at (42 + arrowNudge, 14))
            val tipB = Offset((42f + arrowNudge) * scale, 14f * scale)
            drawLine(
                color = iconColor,
                start = Offset(tipB.x - wingX, tipB.y - wingLen),
                end = tipB,
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
            drawLine(
                color = iconColor,
                start = Offset(tipB.x - wingX, tipB.y + wingLen),
                end = tipB,
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}
