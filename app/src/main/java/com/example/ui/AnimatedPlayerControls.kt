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

// Solid milk-white: softened off-white with 100% solid opacity (no translucent gray)
private val SolidMilkWhite = Color(0xFFF2F0ED)

/**
 * Animated Repeat Icon:
 * - Rendered with thin, solid off-white vector strokes (1.7.dp) matching navigation icons.
 * - 100% solid opacity with zero translucent gray overlay.
 * - The actual strokes travel along their racetrack loop path when tapped.
 * - Arrowheads dynamically align along the path tangents with rounded caps and joins.
 * - Switches to the solid artwork accent color when active, maintaining uniform stroke width and zero glow.
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
            // One smooth circulation loop forward along racetrack path
            travelOffset.snapTo(0f)
            travelOffset.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
            travelOffset.snapTo(0f)
        } else {
            // Subtle reverse easing when turning off
            travelOffset.animateTo(
                targetValue = -0.18f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            travelOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            )
        }
    }

    val targetColor = if (isRepeat) activeColor.copy(alpha = 1.0f) else SolidMilkWhite
    val iconColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
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
            // Slim, refined stroke matching the visual weight of clean navigation icons
            val strokeWidthPx = 1.7.dp.toPx()

            // Canonical racetrack path in 48x48 coordinate space
            // Height = 18 (from y=15 to y=33), width = 28 (from x=10 to x=38), radius = 9
            val racetrack = Path().apply {
                moveTo(16f * scale, 15f * scale)
                lineTo(32f * scale, 15f * scale)
                arcTo(
                    rect = Rect(23f * scale, 15f * scale, 41f * scale, 33f * scale),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(16f * scale, 33f * scale)
                arcTo(
                    rect = Rect(7f * scale, 15f * scale, 25f * scale, 33f * scale),
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
            // Arrow 1 head on the top straight run pointing right
            // Arrow 2 head on the bottom straight run pointing left
            val baseHead1 = 14f * scale
            val baseHead2 = baseHead1 + totalLength * 0.5f
            val strokeLength = totalLength * 0.38f // elegant gap between the two arrows

            val offsetDist = travelOffset.value * totalLength

            val heads = listOf(
                (baseHead1 + offsetDist).mod(totalLength),
                (baseHead2 + offsetDist).mod(totalLength)
            )

            val strokeStyle = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
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
                    style = strokeStyle
                )

                // Dynamic arrowhead aligned precisely to the local path tangent
                val headPos = pathMeasure.getPosition(headDist)
                val tangent = pathMeasure.getTangent(headDist)
                val angle = atan2(tangent.y, tangent.x)
                val wingAngle = 0.70f // ~40 degrees
                val wingLen = 4.2f * scale

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
 * - Slim, thin vector strokes (1.7.dp) matching navigation icons.
 * - Solid off-white (#F2F0ED) with 100% solid opacity.
 * - Solid opacity ensures the crossing point does NOT compound alpha or create a bright center hotspot.
 * - When tapped, the two crossing paths flex and slide through their crossing point before easing into place.
 * - Completely smooth rounded line caps and joins throughout.
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
            // Smooth natural flex and stroke slide through crossing point
            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing)
            )
            shuffleAnim.snapTo(0f)
        } else {
            // Gentle reverse pulse
            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(
                targetValue = 0.5f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            shuffleAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
            )
        }
    }

    val targetColor = if (isShuffle) activeColor.copy(alpha = 1.0f) else SolidMilkWhite
    val iconColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
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
            // Slim, refined stroke matching the visual weight of clean navigation icons
            val strokeWidthPx = 1.7.dp.toPx()

            val progress = shuffleAnim.value
            val pulse = sin(progress * PI.toFloat())
            val midYOffset = pulse * 2.8f * scale
            val arrowNudge = pulse * 2.6f * scale
            val leftPull = pulse * 1.2f * scale

            // Path A: Top-left (9, 16) -> S-curve to bottom-right (39, 32)
            val pathA = Path().apply {
                moveTo((9f + leftPull) * scale, 16f * scale)
                lineTo((15f + leftPull) * scale, 16f * scale)
                cubicTo(
                    20f * scale, (16f + midYOffset * 0.3f) * scale,
                    22f * scale, (24f - midYOffset) * scale,
                    24f * scale, (24f - midYOffset) * scale
                )
                cubicTo(
                    26f * scale, (24f - midYOffset) * scale,
                    28f * scale, (32f - midYOffset * 0.3f) * scale,
                    33f * scale, 32f * scale
                )
                lineTo((39f + arrowNudge) * scale, 32f * scale)
            }

            // Path B: Bottom-left (9, 32) -> S-curve to top-right (39, 16)
            val pathB = Path().apply {
                moveTo((9f + leftPull) * scale, 32f * scale)
                lineTo((15f + leftPull) * scale, 32f * scale)
                cubicTo(
                    20f * scale, (32f - midYOffset * 0.3f) * scale,
                    22f * scale, (24f + midYOffset) * scale,
                    24f * scale, (24f + midYOffset) * scale
                )
                cubicTo(
                    26f * scale, (24f + midYOffset) * scale,
                    28f * scale, (16f + midYOffset * 0.3f) * scale,
                    33f * scale, 16f * scale
                )
                lineTo((39f + arrowNudge) * scale, 16f * scale)
            }

            val strokeStyle = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // Because iconColor has 100% solid opacity, drawing the crossing paths
            // creates a completely uniform stroke brightness with NO alpha compounding at the intersection
            drawPath(path = pathA, color = iconColor, style = strokeStyle)
            drawPath(path = pathB, color = iconColor, style = strokeStyle)

            // Arrowheads: Slim, refined wings matching stroke width and geometry
            val wingLen = 4.2f * scale
            val wingAngle = 0.68f // ~39 degrees

            // Arrowhead A (bottom-right: tip at (39 + arrowNudge, 32), pointing right)
            val tipA = Offset((39f + arrowNudge) * scale, 32f * scale)
            val wingA1 = tipA - Offset(cos(wingAngle) * wingLen, sin(wingAngle) * wingLen)
            val wingA2 = tipA - Offset(cos(wingAngle) * wingLen, -sin(wingAngle) * wingLen)
            drawLine(color = iconColor, start = wingA1, end = tipA, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)
            drawLine(color = iconColor, start = wingA2, end = tipA, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)

            // Arrowhead B (top-right: tip at (39 + arrowNudge, 16), pointing right)
            val tipB = Offset((39f + arrowNudge) * scale, 16f * scale)
            val wingB1 = tipB - Offset(cos(wingAngle) * wingLen, sin(wingAngle) * wingLen)
            val wingB2 = tipB - Offset(cos(wingAngle) * wingLen, -sin(wingAngle) * wingLen)
            drawLine(color = iconColor, start = wingB1, end = tipB, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)
            drawLine(color = iconColor, start = wingB2, end = tipB, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)
        }
    }
}
