from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Restore threshold-based directional feedback while keeping the action slot full-width.
s = s.replace(
    '    val actionColor = Color.Black\n',
    '''    val actionColor = when {\n        !stage2 -> Color.Black\n        swipingLeft -> Color(0xFFE53935)\n        swipingRight -> Color(0xFF43A047)\n        else -> Color.Black\n    }\n''', 1
)

# If the preceding patch removed stage2, restore it immediately before stageProgress.
if '    val stage2 = abs(swipeOffset) >= thresholdPx\n' not in s:
    s = s.replace(
        '    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)\n',
        '    val stage2 = abs(swipeOffset) >= thresholdPx\n    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)\n',
        1
    )

# Ensure the underlay is rectangular and fills the entire row.
s = s.replace(
    '                    .fillMaxSize()\n                    .background(Color.Black),\n',
    '                    .fillMaxSize()\n                    .background(actionColorAnimated),\n',
    1
)
s = s.replace(
    '                    .fillMaxSize()\n                    .clip(RoundedCornerShape(9.dp))\n                    .background(actionColorAnimated),\n',
    '                    .fillMaxSize()\n                    .background(actionColorAnimated),\n',
    1
)

# Put Delete near the trailing edge and Play Next near the leading edge.
s = s.replace(
    '                        .padding(horizontal = 24.dp)\n                        .size(24.dp)',
    '''                        .padding(\n                            start = if (swipingRight) 20.dp else 0.dp,\n                            end = if (swipingLeft) 20.dp else 0.dp\n                        )\n                        .size(24.dp)''',
    1
)

p.write_text(s, encoding='utf-8')
print('finalized YouTube Music queue swipe visuals')
