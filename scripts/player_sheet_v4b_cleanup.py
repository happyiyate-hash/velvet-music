from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# The expanded artwork must keep the exact same height as the normal artwork.
text = text.replace(
    'val expArtHeight = (totalHeight * 0.42f).coerceIn(300.dp, 410.dp)',
    'val expArtHeight = baseArtSize'
)

# Resting controls: lower them slightly. Expanded controls: raise the whole row slightly
# so the gesture handle does not nearly touch the play/pause button.
text = re.sub(
    r'val baseControlsY = baseWaveformY \+ 52\.dp \+ \d+\.dp',
    'val baseControlsY = baseWaveformY + 52.dp + 44.dp',
    text,
    count=1,
)
text = re.sub(
    r'val expControlsY = expArtY \+ expArtHeight \+ \d+\.dp',
    'val expControlsY = expArtY + expArtHeight + 18.dp',
    text,
    count=1,
)
text = re.sub(
    r'val expUpNextY = expControlsY \+ \d+\.dp',
    'val expUpNextY = expControlsY + 72.dp',
    text,
    count=1,
)

# Keep Up Next as a transparent continuation of the same page surface.
text = re.sub(
    r'(\.height\(upNextHeight\.coerceAtLeast\(54\.dp\)\)\s*\.background\()themeColors\.darkBackground(\))',
    r'\1Color.Transparent\2',
    text,
    count=1,
)

# IMPORTANT: make the entire page use ONE extracted background surface color. The artwork
# fade uses this same color, so the fade and queue cannot reveal a different stripe/card tone.
old_gradient = '''drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        themeColors.bgTop,
                        themeColors.bgMidUpper,
                        themeColors.bgMidLower,
                        themeColors.bgBottom
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )
            )'''
if old_gradient in text:
    text = text.replace(
        old_gradient,
        'drawRect(color = themeColors.darkBackground)',
        1,
    )

# The existing waveform/progress component remains the ONLY progress component. Keep its
# expanded position about 10% inside the artwork rather than creating a new slider below it.
text = re.sub(
    r'expArtY \+ \(expArtHeight \* 0\.90f\) - 48\.dp',
    'expArtY + expArtHeight - 34.dp',
    text,
    count=1,
)

# Keep the artwork's broad lower dissolve. The overlay starts around 60% and ends with the
# exact same page color, allowing the background to visually cover the lower artwork.
text = re.sub(
    r'0\.70f to themeColors\.darkBackground\.copy\(alpha = 0\.16f\),\s*'
    r'0\.78f to themeColors\.darkBackground\.copy\(alpha = 0\.42f\),\s*'
    r'0\.86f to themeColors\.darkBackground\.copy\(alpha = 0\.70f\),\s*'
    r'0\.93f to themeColors\.darkBackground\.copy\(alpha = 0\.90f\),\s*'
    r'1\.00f to themeColors\.darkBackground',
    '''0.68f to themeColors.darkBackground.copy(alpha = 0.12f),
                                0.76f to themeColors.darkBackground.copy(alpha = 0.36f),
                                0.84f to themeColors.darkBackground.copy(alpha = 0.64f),
                                0.91f to themeColors.darkBackground.copy(alpha = 0.86f),
                                1.00f to themeColors.darkBackground''',
    text,
    count=1,
)

# Secondary controls start lower at rest and physically rise beside previous/next only as p -> 1.
text = re.sub(
    r'val secondaryOffsetY = lerp\(76\.dp \+ \d+\.dp, 6\.dp, controlT\)',
    'val secondaryOffsetY = lerp(76.dp + 28.dp, 6.dp, controlT)',
    text,
    count=1,
)

# No stale zIndex workaround.
text = text.replace('import androidx.compose.ui.zIndex\n', '')
text = text.replace('                .zIndex(10f)\n', '')

path.write_text(text, encoding="utf-8")
print("PlayerSheet final surface/control patch applied.")
