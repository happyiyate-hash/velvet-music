from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

old_gradient = '''Brush.verticalGradient(
                    colors = listOf(
                        themeColors.bgTop,
                        themeColors.bgMidUpper,
                        themeColors.bgMidLower,
                        themeColors.bgBottom
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )'''
new_gradient = '''Brush.verticalGradient(
                    colors = listOf(
                        themeColors.darkBackground,
                        themeColors.darkBackground,
                        themeColors.darkBackground,
                        themeColors.darkBackground
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )'''
if old_gradient in s:
    s = s.replace(old_gradient, new_gradient, 1)

s = s.replace('themeColors.atmosphericBloom.copy(alpha = 0.35f)', 'Color.Transparent', 1)
s = s.replace('themeColors.atmosphericBloom.copy(alpha = 0.12f)', 'Color.Transparent', 1)
s = s.replace('themeColors.bgBottom', 'themeColors.darkBackground')

# Add the mirrored top artwork brush only once.
if '0.86f to themeColors.darkBackground.copy(alpha = 0.86f)' not in s:
    anchor = '''        if (artworkFadeAlpha > 0f) {'''
    if anchor not in s:
        raise SystemExit('Expected bottom artwork brush block was not found.')
    top_brush = '''        if (artworkFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = expArtX, y = expArtY)
                    .width(expArtWidth)
                    .height(expArtHeight * 0.24f)
                    .graphicsLayer { alpha = artworkFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to themeColors.darkBackground,
                                0.16f to themeColors.darkBackground.copy(alpha = 0.86f),
                                0.34f to themeColors.darkBackground.copy(alpha = 0.62f),
                                0.52f to themeColors.darkBackground.copy(alpha = 0.34f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.12f),
                                1.00f to Color.Transparent
                            )
                        )
                    )
                    .zIndex(1f)
            )
        }

'''
    s = s.replace(anchor, top_brush + anchor, 1)

p.write_text(s, encoding='utf-8')
print('Applied uniform artwork-derived background and mirrored top brush; workflow trigger for queue patch.')
