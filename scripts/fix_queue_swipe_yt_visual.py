from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Full-width, sharp action surface with threshold-based directional feedback.
if 'val swipingLeft = swipeOffset < 0f' in s:
    if 'val stage2 = abs(swipeOffset) >= thresholdPx' not in s:
        s = s.replace(
            '    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)\n',
            '    val stage2 = abs(swipeOffset) >= thresholdPx\n    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)\n',
            1
        )
    s = s.replace(
        '    val actionColor = Color.Black\n',
        '''    val actionColor = when {\n        !stage2 -> Color.Black\n        swipingLeft -> Color(0xFFE53935)\n        swipingRight -> Color(0xFF43A047)\n        else -> Color.Black\n    }\n''',
        1
    )

# Remove any rounded clipping or card-like action container.
s = s.replace(
    '                    .fillMaxSize()\n                    .clip(RoundedCornerShape(9.dp))\n                    .background(actionColorAnimated),\n',
    '                    .fillMaxSize()\n                    .background(actionColorAnimated),\n',
    1
)

# Keep the action surface edge-to-edge and pin the icon close to its screen edge.
s = s.replace(
    '                        .padding(horizontal = 24.dp)\n                        .size(24.dp)',
    '''                        .padding(\n                            start = if (swipingRight) 20.dp else 0.dp,\n                            end = if (swipingLeft) 20.dp else 0.dp\n                        )\n                        .size(24.dp)''',
    1
)

p.write_text(s, encoding='utf-8')
print('finalized YouTube Music queue swipe visuals')
