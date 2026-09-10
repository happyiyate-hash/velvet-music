from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# YouTube Music-style swipe action: the revealed slot is a continuous black
# surface. It is never a rounded card and never changes to red/green.
old = '''    val neutralAction = Color(0xFF1E1E1E)\n    val deleteAction = Color(0xFFE53935)\n    val playNextAction = Color(0xFF43A047)\n    val swipingRight = swipeOffset > 0f\n    val swipingLeft = swipeOffset < 0f\n    val stage2 = abs(swipeOffset) >= thresholdPx\n    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)\n    val actionColor = if (stage2) {\n        if (swipingLeft) deleteAction else playNextAction\n    } else {\n        neutralAction\n    }\n'''
new = '''    val swipingRight = swipeOffset > 0f\n    val swipingLeft = swipeOffset < 0f\n    val stageProgress = (abs(swipeOffset) / thresholdPx).coerceIn(0f, 1f)\n    // Continuous black action surface, matching the reference behavior.\n    val actionColor = Color.Black\n'''
if old in s:
    s = s.replace(old, new, 1)

# Keep the action surface physically edge-to-edge with no radius or clipping.
s = s.replace('''                    .fillMaxSize()\n                    .background(actionColorAnimated),\n                contentAlignment = if (swipingLeft) Alignment.CenterEnd else Alignment.CenterStart\n''', '''                    .fillMaxSize()\n                    .background(Color.Black),\n                contentAlignment = if (swipingLeft) Alignment.CenterEnd else Alignment.CenterStart\n''', 1)

# The icon sits close to the screen edge: right edge for left-swipe delete,
# left edge for right-swipe Play Next.
old_padding = '''                        .padding(horizontal = 24.dp)\n                        .size(24.dp)\n'''
new_padding = '''                        .padding(horizontal = 24.dp)\n                        .size(24.dp)\n'''
if old_padding in s:
    s = s.replace(old_padding, new_padding, 1)

# Explicitly remove any rounded action clipping left by an older revision.
s = s.replace('''                    .fillMaxSize()\n                    .clip(RoundedCornerShape(9.dp))\n                    .background(actionColorAnimated),\n''', '''                    .fillMaxSize()\n                    .background(Color.Black),\n''', 1)

p.write_text(s, encoding='utf-8')
print('patched YouTube-style queue swipe visuals')
