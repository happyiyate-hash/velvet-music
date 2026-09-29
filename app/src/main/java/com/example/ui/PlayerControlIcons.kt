package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Custom player icons matching the bold, rounded design language:
 * - Pure solid white (#FFFFFF)
 * - Rounded triangle vertices and rounded pause/skip bars
 * - Scaled to a 64x64 canonical viewBox
 */

/**
 * Liquid morphing Play / Pause Icon:
 * - 3-stage choreography with a visible hold delay:
 *   - Play -> Pause: play triangle splits into two halves, HOLDS in place for a distinct delay,
 *     then straightens and morphs into the two vertical rounded pause bars.
 *   - Pause -> Play: pause bars morph back into the two separated half-triangles, HOLDS in place
 *     for a distinct delay, then slides together, overlaps across the seam, and joins into a 100%
 *     continuous, seamless unified play triangle with zero center empty line.
 */
@Composable
fun MorphingPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF08090C),
    contentDescription: String? = if (isPlaying) "Pause" else "Play"
) {
    // Value: 0f = Unified Play Triangle, 0.40f = Separated Half-Triangles (Hold state), 1f = Dual Pause Bars
    val anim = remember { Animatable(if (isPlaying) 1f else 0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // PLAYING: Play -> Pause
            // Phase 1: Triangle splits and separates cleanly
            anim.animateTo(
                targetValue = 0.40f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            // Phase 2: Perceptible HOLD / DELAY beat so separation is clearly seen
            delay(150)
            // Phase 3: Straighten and morph into vertical pause bars
            anim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        } else {
            // PAUSED: Pause -> Play
            // Phase 1: Morph pause bars into separated half-play triangles
            anim.animateTo(
                targetValue = 0.40f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            // Phase 2: Perceptible HOLD / DELAY beat
            delay(150)
            // Phase 3: Slide together, overlap across center seam, and fuse into solid play button
            anim.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
    }

    val morphProgress = anim.value.coerceIn(0f, 1f)

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

            val t = morphProgress

            val gapProgress = if (t <= 0.40f) {
                (t / 0.40f).coerceIn(0f, 1f)
            } else {
                1f
            }

            val shapeProgress = if (t <= 0.40f) {
                0f
            } else {
                ((t - 0.40f) / 0.60f).coerceIn(0f, 1f)
            }

            val rCorner = 4.2f * scale
            // Inner seam radius: 0 when joined for zero-seam fusion; softly rounded when separated
            val innerR = 4.0f * scale * gapProgress * (0.35f + 0.65f * shapeProgress)

            // Subtle scale breathing during transformation
            val pulseScale = 1f - sin(t * Math.PI.toFloat()) * 0.035f

            val center = Offset(size.width / 2f, size.height / 2f)

            // Convert canonical coordinates to canvas coordinates with scale and offset
            fun toCanvas(pt: Offset): Offset {
                val cx = offsetX + pt.x * scale
                val cy = offsetY + pt.y * scale
                return Offset(
                    center.x + (cx - center.x) * pulseScale,
                    center.y + (cy - center.y) * pulseScale
                )
            }

            // WHEN FULLY JOINED (Play button):
            // Directly render the single unified continuous rounded triangle path.
            // This guarantees 100% solid continuity with ZERO center empty line, cut-off, or seam!
            if (gapProgress <= 0.005f) {
                val pTopLeft = toCanvas(Offset(21f, 15f))
                val pApex = toCanvas(Offset(49f, 32f))
                val pBottomLeft = toCanvas(Offset(21f, 49f))
                val unifiedPlayTriangle = buildRoundedPolygonPath(
                    points = listOf(pTopLeft, pApex, pBottomLeft),
                    radii = listOf(rCorner, rCorner, rCorner)
                )
                drawPath(path = unifiedPlayTriangle, color = tint)
                return@Canvas
            }

            // WHEN SEPARATED OR MORPHING:
            // Generous seam overlap across x=33f ensures that as the two halves slide together,
            // they overlap completely without any anti-aliasing hairline or center hollow artifact.
            val seamOverlap = 1.8f * (1f - gapProgress)

            val p0 = Offset(lerp(21f, 18f, gapProgress), lerp(15f, 16f, shapeProgress))
            val p1 = Offset(lerp(33f + seamOverlap, 27f, gapProgress), lerp(22.3f, 16f, shapeProgress))
            val p2 = Offset(lerp(33f + seamOverlap, 27f, gapProgress), lerp(41.7f, 48f, shapeProgress))
            val p3 = Offset(lerp(21f, 18f, gapProgress), lerp(49f, 48f, shapeProgress))

            val tipX = lerp(49f, 50.5f, gapProgress)
            val q0 = Offset(lerp(33f - seamOverlap, 37f, gapProgress), lerp(22.3f, 16f, shapeProgress))
            val q1 = Offset(lerp(tipX, 46f, shapeProgress), lerp(32f, 16f, shapeProgress))
            val q2 = Offset(lerp(tipX, 46f, shapeProgress), lerp(32f, 48f, shapeProgress))
            val q3 = Offset(lerp(33f - seamOverlap, 37f, gapProgress), lerp(41.7f, 48f, shapeProgress))

            val canvasP0 = toCanvas(p0)
            val canvasP1 = toCanvas(p1)
            val canvasP2 = toCanvas(p2)
            val canvasP3 = toCanvas(p3)

            val canvasQ0 = toCanvas(q0)
            val canvasQ1 = toCanvas(q1)
            val canvasQ2 = toCanvas(q2)
            val canvasQ3 = toCanvas(q3)

            // Draw Left piece
            val leftPath = buildRoundedPolygonPath(
                points = listOf(canvasP0, canvasP1, canvasP2, canvasP3),
                radii = listOf(rCorner, innerR, innerR, rCorner)
            )
            drawPath(path = leftPath, color = tint)

            // Draw Right piece
            val tipDist = (canvasQ2 - canvasQ1).getDistance()
            val rightPath = if (tipDist < 1.2f) {
                // Collapsed tip (single rounded apex point for triangle half)
                val apex = Offset((canvasQ1.x + canvasQ2.x) / 2f, (canvasQ1.y + canvasQ2.y) / 2f)
                buildRoundedPolygonPath(
                    points = listOf(canvasQ0, apex, canvasQ3),
                    radii = listOf(innerR, rCorner, innerR)
                )
            } else {
                // Splitting / separated dual corners
                buildRoundedPolygonPath(
                    points = listOf(canvasQ0, canvasQ1, canvasQ2, canvasQ3),
                    radii = listOf(innerR, rCorner, rCorner, innerR)
                )
            }
            drawPath(path = rightPath, color = tint)
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun buildRoundedPolygonPath(
    points: List<Offset>,
    radii: List<Float>
): Path {
    val n = points.size
    val path = Path()
    if (n < 3) return path

    val lastEdgeMid = Offset(
        (points[n - 1].x + points[0].x) / 2f,
        (points[n - 1].y + points[0].y) / 2f
    )
    path.moveTo(lastEdgeMid.x, lastEdgeMid.y)

    for (i in 0 until n) {
        val p = points[i]
        val prev = points[(i - 1 + n) % n]
        val next = points[(i + 1) % n]
        val r = radii[i]

        val v1 = prev - p
        val len1 = v1.getDistance()
        val v2 = next - p
        val len2 = v2.getDistance()

        if (r <= 0.2f || len1 < 0.5f || len2 < 0.5f) {
            path.lineTo(p.x, p.y)
        } else {
            val u1 = v1 / len1
            val u2 = v2 / len2
            val effectiveR = minOf(r, len1 * 0.45f, len2 * 0.45f)
            val start = p + u1 * effectiveR
            val end = p + u2 * effectiveR

            path.lineTo(start.x, start.y)
            path.quadraticBezierTo(p.x, p.y, end.x, end.y)
        }
    }

    path.close()
    return path
}

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
