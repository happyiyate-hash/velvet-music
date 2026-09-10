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
s = s.replace('val step = with(LocalDensity.current) { 60.dp.toPx() }', 'val step = with(queueDragDensity) { 60.dp.toPx() }')
s = s.replace('with(LocalDensity.current) { 60.dp.toPx() }', 'with(queueDragDensity) { 60.dp.toPx() }')

# Swipe underlay uses fillMaxHeight.
if 'import androidx.compose.foundation.layout.fillMaxHeight\n' not in s:
    s = s.replace(
        'import androidx.compose.foundation.layout.fillMaxWidth\n',
        'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.fillMaxHeight\n',
        1,
    )

# AnimatedPlayingBars: animateFloat is an InfiniteTransition extension. Use the transition
# receiver explicitly so Kotlin resolves it correctly.
s = s.replace('androidx.compose.animation.core.animateFloat(', 'infinite.animateFloat(')
s = s.replace('import androidx.compose.animation.core.animateFloat\n', '')

# The expanded artwork brush must gently disappear during stage 2 so the compact 44dp
# artwork at the top-left is not covered by the fixed expanded-state brush.
brush_anchor = '''        val artworkFadeAlpha = when {\n            p < 0.30f -> 0f\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n'''
brush_new = '''        val artworkFadeAlpha = when {\n            p < 0.30f -> 0f\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            else -> 1f\n        }\n\n        // Stage 2 fade: keep the brush fully visible at the first snap, then dissolve it\n        // smoothly as Up Next approaches maximum height. This follows the finger and also\n        // remains smooth while dragProgress snaps to its final position.\n        val compactBrushFadeAlpha = when {\n            p <= 1.05f -> 1f\n            p >= 1.90f -> 0f\n            else -> {\n                val t = ((p - 1.05f) / 0.85f).coerceIn(0f, 1f)\n                1f - (t * t * (3f - 2f * t))\n            }\n        }\n'''
if 'val compactBrushFadeAlpha = when' not in s:
    if brush_anchor not in s:
        raise SystemExit('Artwork fade anchor not found')
    s = s.replace(brush_anchor, brush_new, 1)
s = s.replace(
    '.graphicsLayer { alpha = artworkFadeAlpha }',
    '.graphicsLayer { alpha = artworkFadeAlpha * compactBrushFadeAlpha }',
)

# Restore the exact end of PlayerSheet if the old queue patch consumed the lyrics-sheet
# closing lines. This is deliberately anchored to the unique LyricsBottomSheet callback.
broken_boundary = '''                onDismiss = { showLyricsSheet = false }\n        @Composable\nprivate fun UpNextTrackRow'''
fixed_boundary = '''                onDismiss = { showLyricsSheet = false }\n            )\n        }\n    }\n}\n\n@Composable\nprivate fun UpNextTrackRow'''
if broken_boundary in s:
    s = s.replace(broken_boundary, fixed_boundary, 1)

# Repair the exact malformed queue-row KDoc boundary produced by the earlier patch chain.
malformed_kdoc = '''/**\n * Up Next list track row component:\n * Clean, modern row displaying track art thumbnail, title, artist & duratio@Composable\nprivate fun UpNextTrackRow('''
fixed_kdoc = '''/**\n * Up Next list track row component:\n * Clean, modern row displaying track art thumbnail, title, artist & duration.\n */\n@Composable\nprivate fun UpNextTrackRow('''
if malformed_kdoc in s:
    s = s.replace(malformed_kdoc, fixed_kdoc, 1)

# Repair other exact malformed remnants from the same patch generation without broad regexes.
s = s.replace('* Cl@Composable\nprivate fun UpNextTrackRow', '*/\n@Composable\nprivate fun UpNextTrackRow', 1)
s = s.replace('* duratio@Composable\nprivate fun UpNextTrackRow', '*/\n@Composable\nprivate fun UpNextTrackRow', 1)

# Remove duplicate copies of the identical expanded-artwork top brush. Keep exactly one
# copy; the distinct lower dissolve block (offset at 76%) is intentionally preserved.
brush_block = '''        if (artworkFadeAlpha > 0f) {\n            Box(\n                modifier = Modifier\n                    .offset(x = expArtX, y = expArtY)\n                    .width(expArtWidth)\n                    .height(expArtHeight * 0.24f)\n                    .graphicsLayer { alpha = artworkFadeAlpha * compactBrushFadeAlpha }\n                    .background(\n                        Brush.verticalGradient(\n                            colorStops = arrayOf(\n                                0.00f to themeColors.darkBackground,\n                                0.16f to themeColors.darkBackground.copy(alpha = 0.86f),\n                                0.34f to themeColors.darkBackground.copy(alpha = 0.62f),\n                                0.52f to themeColors.darkBackground.copy(alpha = 0.34f),\n                                0.72f to themeColors.darkBackground.copy(alpha = 0.12f),\n                                1.00f to Color.Transparent\n                            )\n                        )\n                    )\n                    .zIndex(1f)\n            )\n        }\n'''
while s.count(brush_block) > 1:
    s = s.replace(brush_block, '', 1)

# Repair an exact orphaned fragment before AnimatedPlayingBars if an earlier swipe patch left it.
orphan = '''\nound(Color.White.copy(alpha = .92f)))\n            }\n        }\n    }\n}\n\n@Composable\nprivate fun AnimatedPlayingBars'''
if orphan in s:
    s = s.replace(orphan, '\n@Composable\nprivate fun AnimatedPlayingBars', 1)

p.write_text(s, encoding='utf-8')
print('Stabilized PlayerSheet compile boundaries, queue density usage, duplicate brush blocks, and compact brush fade.')
