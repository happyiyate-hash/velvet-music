from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Compose imports required by the two-stage swipe implementation.
if 'import androidx.compose.animation.core.animateFloat\n' not in s:
    s = s.replace('import androidx.compose.animation.core.Animatable\n', 'import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.animateFloat\n', 1)
if 'import androidx.compose.foundation.layout.fillMaxHeight\n' not in s:
    s = s.replace('import androidx.compose.foundation.layout.fillMaxWidth\n', 'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.fillMaxHeight\n', 1)

# updateQueueDrag is a non-composable local function, so capture density in the composable scope.
if 'val queueDragDensity = LocalDensity.current' not in s:
    anchor = '    val haptic = LocalHapticFeedback.current\n'
    s = s.replace(anchor, anchor + '    val queueDragDensity = LocalDensity.current\n', 1)
s = s.replace('val step = with(LocalDensity.current) { 60.dp.toPx() }', 'val step = with(queueDragDensity) { 60.dp.toPx() }', 1)

p.write_text(s, encoding='utf-8')
print('Fixed queue swipe/drag Compose compile errors.')
