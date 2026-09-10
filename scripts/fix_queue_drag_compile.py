from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')
if 'import androidx.compose.runtime.mutableIntStateOf' not in s:
    s = s.replace('import androidx.compose.runtime.mutableFloatStateOf\n', 'import androidx.compose.runtime.mutableFloatStateOf\nimport androidx.compose.runtime.mutableIntStateOf\n', 1)
s = s.replace('animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow', 'animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow', 1)
p.write_text(s, encoding='utf-8')
