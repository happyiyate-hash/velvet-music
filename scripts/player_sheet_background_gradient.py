from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

old = '''        // Dynamic player surface: lift the extracted artwork color so it is not nearly black.
        // Keep the upper area brighter and let the bottom fall off slightly darker.
        val extractedBg = themeColors.darkBackground
        val playerBackgroundTop = Color(
            red = (extractedBg.red + (1f - extractedBg.red) * 0.12f).coerceAtMost(1f),
            green = (extractedBg.green + (1f - extractedBg.green) * 0.12f).coerceAtMost(1f),
            blue = (extractedBg.blue + (1f - extractedBg.blue) * 0.12f).coerceAtMost(1f),
            alpha = 1f
        )
        val playerBackgroundBottom = Color(
            red = (playerBackgroundTop.red * 0.90f).coerceAtLeast(0f),
            green = (playerBackgroundTop.green * 0.90f).coerceAtLeast(0f),
            blue = (playerBackgroundTop.blue * 0.90f).coerceAtLeast(0f),
            alpha = 1f
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(playerBackgroundTop, playerBackgroundBottom)
                )
            )
        }
'''

new = '''        // Dynamic PlayerSheet background: derive a deep, rich gradient directly from
        // the artwork-extracted palette. The color is animated when the track/artwork changes
        // so the surface never snaps back to a neutral Material background.
        val extractedBg = themeColors.darkBackground
        val targetBackgroundTop = Color(
            red = (extractedBg.red * 0.72f).coerceIn(0f, 1f),
            green = (extractedBg.green * 0.72f).coerceIn(0f, 1f),
            blue = (extractedBg.blue * 0.72f).coerceIn(0f, 1f),
            alpha = 1f
        )
        val targetBackgroundBottom = Color(
            red = (targetBackgroundTop.red * 0.28f).coerceIn(0f, 1f),
            green = (targetBackgroundTop.green * 0.28f).coerceIn(0f, 1f),
            blue = (targetBackgroundTop.blue * 0.28f).coerceIn(0f, 1f),
            alpha = 1f
        )
        val animatedBackgroundTop by animateColorAsState(
            targetValue = targetBackgroundTop,
            animationSpec = tween(650, easing = FastOutSlowInEasing),
            label = "player_background_top"
        )
        val animatedBackgroundBottom by animateColorAsState(
            targetValue = targetBackgroundBottom,
            animationSpec = tween(750, easing = FastOutSlowInEasing),
            label = "player_background_bottom"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(animatedBackgroundTop, animatedBackgroundBottom)
                )
            )
        }
'''

if old not in s:
    raise SystemExit('Expected PlayerSheet background block not found; refusing to modify.')
s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
print('PlayerSheet dynamic animated deep gradient applied.')
