from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Queue drag: updateQueueDrag is a normal local function, so density must be captured
# from the composable scope before that function is declared.
if 'val queueDragDensity = LocalDensity.current' not in s:
    anchor = '    val haptic = LocalHapticFeedback.current\n'
    if anchor not in s:
        raise SystemExit('Queue drag haptic anchor not found')
    s = s.replace(anchor, anchor + '    val queueDragDensity = LocalDensity.current\n', 1)
s = s.replace('val step = with(LocalDensity.current) { 60.dp.toPx() }', 'val step = with(queueDragDensity) { 60.dp.toPx() }', 1)

# Swipe underlay uses fillMaxHeight.
if 'import androidx.compose.foundation.layout.fillMaxHeight\n' not in s:
    s = s.replace(
        'import androidx.compose.foundation.layout.fillMaxWidth\n',
        'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.fillMaxHeight\n',
        1,
    )

# AnimatedPlayingBars: animateFloat is an InfiniteTransition extension. Use the transition
# receiver explicitly so Kotlin resolves it correctly.
s = s.replace(
    'androidx.compose.animation.core.animateFloat(',
    'infinite.animateFloat(',
)
s = s.replace('import androidx.compose.animation.core.animateFloat\n', '')

# The expanded artwork brush currently remains fixed over the compact 44dp artwork.
# Fade that brush progressively during stage 2 so the small top-left artwork is unobstructed.
brush_anchor = '''        val artworkFadeAlpha = when {\n            p < 0.30f -> 0f\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n'''
brush_new = '''        val artworkFadeAlpha = when {\n            p < 0.30f -> 0f\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n\n        // During the second snap, the expanded artwork brush must gently disappear as\n        // the Up Next queue approaches its maximum height. This keeps the compact 44dp\n        // artwork at the top-left completely clear instead of covering it with the brush.\n        // The fade follows the drag continuously and remains smooth during snap animation.\n        val compactBrushFadeAlpha = when {\n            p <= 1.05f -> 1f\n            p >= 1.90f -> 0f\n            else -> {\n                val t = ((p - 1.05f) / 0.85f).coerceIn(0f, 1f)\n                1f - (t * t * (3f - 2f * t))\n            }\n        }\n'''
if 'val compactBrushFadeAlpha = when' not in s:
    if brush_anchor not in s:
        raise SystemExit('Artwork fade anchor not found')
    s = s.replace(brush_anchor, brush_new, 1)

s = s.replace(
    '.graphicsLayer { alpha = artworkFadeAlpha }',
    '.graphicsLayer { alpha = artworkFadeAlpha * compactBrushFadeAlpha }',
)

# Repair only the known malformed queue-row comment/annotation boundary. This is deliberately
# exact rather than regex-based so unrelated source cannot be rewritten.
for bad in ('* Cl@Composable', '* duratio@Composable'):
    s = s.replace(
        bad + '\nprivate fun UpNextTrackRow',
        '*/\n@Composable\nprivate fun UpNextTrackRow',
        1,
    )

# Repair the exact orphaned fragment before AnimatedPlayingBars if an earlier patch left it.
orphan = '''\nound(Color.White.copy(alpha = .92f)))\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun AnimatedPlayingBars'''
if orphan in s:
    s = s.replace(orphan, '\n@Composable\nprivate fun AnimatedPlayingBars', 1)

p.write_text(s, encoding='utf-8')
print('Fixed queue swipe/drag compile errors, repaired queue-row annotation/tail, and added compact-state brush fade.')
