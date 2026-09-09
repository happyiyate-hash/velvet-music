from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# The background must be driven by the same dynamic artwork accent already used by
# PlayerSheet controls. Do not mix it with Material surface colors or translucent overlays.
import_line = 'import androidx.compose.animation.core.animateColorAsState\n'
anchor = 'import androidx.compose.animation.core.animateFloatAsState\n'
if import_line not in s:
    if anchor not in s:
        raise SystemExit('Animation import anchor not found; refusing to modify.')
    s = s.replace(anchor, anchor + import_line, 1)

old_outer = '''    BoxWithConstraints(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(themeColors.darkBackground)\n            .testTag("full_player_sheet")\n    ) {'''

new_outer = '''    // Pure dynamic artwork color for the entire PlayerSheet surface.\n    // This is a direct 1.0-alpha color assignment: no Material surface, white/gray tint,\n    // darkBackground blend, or translucent layer is placed underneath it.\n    val targetPlayerBackground = themeColors.accent\n    val animatedPlayerBackground by animateColorAsState(\n        targetValue = targetPlayerBackground,\n        animationSpec = tween(650, easing = FastOutSlowInEasing),\n        label = "player_background_color"\n    )\n\n    BoxWithConstraints(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(animatedPlayerBackground)\n            .testTag("full_player_sheet")\n    ) {'''

if old_outer not in s:
    raise SystemExit('Expected PlayerSheet outer background binding not found; refusing to modify.')
s = s.replace(old_outer, new_outer, 1)

old_gradient = '''        // Dynamic PlayerSheet background: derive a deep, rich gradient directly from\n        // the artwork-extracted palette. The color is animated when the track/artwork changes\n        // so the surface never snaps back to a neutral Material background.\n        val extractedBg = themeColors.darkBackground\n        val targetBackgroundTop = Color(\n            red = (extractedBg.red * 0.72f).coerceIn(0f, 1f),\n            green = (extractedBg.green * 0.72f).coerceIn(0f, 1f),\n            blue = (extractedBg.blue * 0.72f).coerceIn(0f, 1f),\n            alpha = 1f\n        )\n        val targetBackgroundBottom = Color(\n            red = (targetBackgroundTop.red * 0.28f).coerceIn(0f, 1f),\n            green = (targetBackgroundTop.green * 0.28f).coerceIn(0f, 1f),\n            blue = (targetBackgroundTop.blue * 0.28f).coerceIn(0f, 1f),\n            alpha = 1f\n        )\n        val animatedBackgroundTop by animateColorAsState(\n            targetValue = targetBackgroundTop,\n            animationSpec = tween(650, easing = FastOutSlowInEasing),\n            label = "player_background_top"\n        )\n        val animatedBackgroundBottom by animateColorAsState(\n            targetValue = targetBackgroundBottom,\n            animationSpec = tween(750, easing = FastOutSlowInEasing),\n            label = "player_background_bottom"\n        )\n        Canvas(modifier = Modifier.fillMaxSize()) {\n            drawRect(\n                brush = Brush.verticalGradient(\n                    colors = listOf(animatedBackgroundTop, animatedBackgroundBottom)\n                )\n            )\n        }\n'''

if old_gradient not in s:
    raise SystemExit('Expected generated gradient overlay block not found; refusing to modify.')
s = s.replace(old_gradient, '', 1)

p.write_text(s, encoding='utf-8')
print('PlayerSheet now uses the animated pure artwork accent as its opaque root background.')
