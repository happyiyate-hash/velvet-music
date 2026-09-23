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
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.offset
import com.example.audio.RepeatMode
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Solid softened off-white for controls
private val SolidMilkWhite = Color(0xFFF2F0ED)

/**
 * Animated Repeat Icon:
 * - Rendered with refined, solid vector strokes (1.9.dp) matching navigation icons.
 * - Solid opacity with subtle dimming when inactive.
 * - The actual strokes travel along their racetrack loop path when tapped.
 * - Arrowheads dynamically align along the path tangents with rounded caps and joins.
 * - Displays a bold, clearly visible "1" or "ALL" badge inside the loop.
 */
@Composable
fun AnimatedRepeatIcon(
    repeatMode: RepeatMode,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchSize: Dp = 46.dp,
    iconSize: Dp = 26.dp
) {
    val travelOffset = remember { Animatable(0f) }

    LaunchedEffect(repeatMode) {
        if (repeatMode != RepeatMode.OFF) {
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

    // Solid ash-gray when inactive to prevent double-alpha hotspots / overshining
    val targetColor = if (repeatMode != RepeatMode.OFF) Color.White else Color(0xFF8E9096)
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
            val strokeWidthPx = 1.6.dp.toPx()

            // Canonical racetrack path in 48x48 coordinate space
            // Height = 24 (from y=12 to y=36), width = 44 (from x=2 to x=46), radius = 12
            val racetrack = Path().apply {
                moveTo(14f * scale, 12f * scale)
                lineTo(34f * scale, 12f * scale)
                arcTo(
                    rect = Rect(22f * scale, 12f * scale, 46f * scale, 36f * scale),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(14f * scale, 36f * scale)
                arcTo(
                    rect = Rect(2f * scale, 12f * scale, 26f * scale, 36f * scale),
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
            val baseHead1 = 18f * scale
            val baseHead2 = baseHead1 + totalLength * 0.5f
            val strokeLength = totalLength * 0.38f

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

                val headPos = pathMeasure.getPosition(headDist)
                val tangent = pathMeasure.getTangent(headDist)
                val angle = atan2(tangent.y, tangent.x)
                val wingAngle = 0.70f
                val wingLen = 4.6f * scale

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

        if (repeatMode == RepeatMode.ONE || repeatMode == RepeatMode.ALL) {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center
            ) {
                if (repeatMode == RepeatMode.ONE) {
                    Text(
                        text = "1",
                        color = iconColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        lineHeight = 8.sp
                    )
                } else {
                    Text(
                        text = "ALL",
                        color = iconColor,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.3.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        lineHeight = 6.sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Animated Shuffle Icon:
 * - Refined vector strokes matching navigation icons.
 * - Solid ash-gray when inactive; solid white when active.
 * - Clean crossing without overshining double-alpha hotspot.
 */
@Composable
fun AnimatedShuffleIcon(
    isShuffle: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchSize: Dp = 46.dp,
    iconSize: Dp = 26.dp
) {
    val shuffleAnim = remember { Animatable(0f) }

    LaunchedEffect(isShuffle) {
        if (isShuffle) {
            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing)
            )
            shuffleAnim.snapTo(0f)
        } else {
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

    // Solid ash-gray when inactive to prevent double-alpha hotspots / overshining
    val targetColor = if (isShuffle) Color.White else Color(0xFF8E9096)
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
            val strokeWidthPx = 1.6.dp.toPx()

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

            drawPath(path = pathA, color = iconColor, style = strokeStyle)
            drawPath(path = pathB, color = iconColor, style = strokeStyle)

            val wingLen = 4.6f * scale
            val wingAngle = 0.68f

            val tipA = Offset((39f + arrowNudge) * scale, 32f * scale)
            val wingA1 = tipA - Offset(cos(wingAngle) * wingLen, sin(wingAngle) * wingLen)
            val wingA2 = tipA - Offset(cos(wingAngle) * wingLen, -sin(wingAngle) * wingLen)
            drawLine(color = iconColor, start = wingA1, end = tipA, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)
            drawLine(color = iconColor, start = wingA2, end = tipA, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)

            val tipB = Offset((39f + arrowNudge) * scale, 16f * scale)
            val wingB1 = tipB - Offset(cos(wingAngle) * wingLen, -sin(wingAngle) * wingLen)
            val wingB2 = tipB - Offset(cos(wingAngle) * wingLen, sin(wingAngle) * wingLen)
            drawLine(color = iconColor, start = wingB1, end = tipB, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)
            drawLine(color = iconColor, start = wingB2, end = tipB, strokeWidth = strokeWidthPx, cap = StrokeCap.Round)
        }
    }
}
