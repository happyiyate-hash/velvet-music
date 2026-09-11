from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Keep the queue almost edge-to-edge: only a tiny 2dp inset remains for touch-safe visual breathing room.
s = s.replace('.padding(start = 4.dp)', '.padding(start = 2.dp)', 1)
s = s.replace('.padding(end = 4.dp)', '.padding(end = 2.dp)', 1)

# Remove row-level rounding. The queue rows themselves are completely sharp/rectangular.
s = s.replace('''                .clip(RoundedCornerShape(9.dp))\n                .background(surfaceColor)''', '''                .background(surfaceColor)''', 1)
s = s.replace('''                .clip(RoundedCornerShape(9.dp))\n                .background(if (isCurrent) accentColor.copy(alpha = .07f) else Color.Transparent)''', '''                .background(if (isCurrent) accentColor.copy(alpha = .07f) else Color.Transparent)''', 1)

# Make the artwork square too: no rounded clipping or rounded border in the Up Next rows.
s = s.replace('''                    .clip(RoundedCornerShape(7.dp))\n                    .border(.7.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(7.dp)),''', '''                    .border(.7.dp, Color.White.copy(alpha = .10f), RectangleShape),''', 1)
s = s.replace('''                    .clip(RoundedCornerShape(7.dp))\n                    .border(0.7.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(7.dp)),''', '''                    .border(0.7.dp, Color.White.copy(alpha = 0.10f), RectangleShape),''', 1)

# Make the two drag lines clearly separated.
s = s.replace('verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)', 'verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)', 1)

# Remove the long-press delay. A normal drag detector starts as soon as the drag is recognized,
# rather than waiting for detectDragGesturesAfterLongPress's long-press timeout.
old = '''detectDragGesturesAfterLongPress(\n                            onDragStart = {\n                                swipeOffset = 0f\n                                onDragStart()\n                            },'''
new = '''detectDragGestures(\n                            onDragStart = {\n                                swipeOffset = 0f\n                                onDragStart()\n                            },'''
if old in s:
    s = s.replace(old, new, 1)
else:
    old2 = '''detectDragGesturesAfterLongPress(\n                            onDragStart = { swipeOffset = 0f; onDragStart() },'''
    new2 = '''detectDragGestures(\n                            onDragStart = { swipeOffset = 0f; onDragStart() },'''
    s = s.replace(old2, new2, 1)

s = s.replace('detectDragGesturesAfterLongPress(', 'detectDragGestures(', 1)

# Bring action icons closer to the screen edge.
s = s.replace('.padding(horizontal = 24.dp)\n                        .size(24.dp)', '.padding(horizontal = 16.dp)\n                        .size(24.dp)', 1)

p.write_text(s, encoding='utf-8')
print('Applied compact sharp queue rows, square artwork, separated drag handle lines, and immediate drag recognition.')
