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
    r'val baseControlsY = baseWaveformY \\+ 52\\.dp \\+ \\d+\\.dp',
    'val baseControlsY = baseWaveformY + 52.dp + 44.dp',
    text,
    count=1,
)
text = re.sub(
    r'val expControlsY = expArtY \\+ expArtHeight \\+ \\d+\\.dp',
    'val expControlsY = expArtY + expArtHeight + 18.dp',
    text,
    count=1,
)
text = re.sub(
    r'val expUpNextY = expControlsY \\+ \\d+\\.dp',
    'val expUpNextY = expControlsY + 72.dp',
    text,
    count=1,
)

# Keep Up Next as a transparent continuation of the same page surface.
text = re.sub(
    r'(\\.height\\(upNextHeight\\.coerceAtLeast\\(54\\.dp\\)\\)\\s*\\.background\\()themeColors\\.darkBackground(\\))',
    r'\\1Color.Transparent\\2',
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
    r'expArtY \\+ \\(expArtHeight \\* 0\\.90f\\) - 48\\.dp',
    'expArtY + expArtHeight - 34.dp',
    text,
    count=1,
)

# Keep the artwork's broad lower dissolve. The overlay starts around 60% and ends with the
# exact same page color, allowing the background to visually cover the lower artwork.
text = re.sub(
    r'0\\.70f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.16f\\),\\s*'
    r'0\\.78f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.42f\\),\\s*'
    r'0\\.86f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.70f\\),\\s*'
    r'0\\.93f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.90f\\),\\s*'
    r'1\\.00f to themeColors\\.darkBackground',
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
    r'val secondaryOffsetY = lerp\\(76\\.dp \\+ \\d+\\.dp, 6\\.dp, controlT\\)',
    'val secondaryOffsetY = lerp(76.dp + 28.dp, 6.dp, controlT)',
    text,
    count=1,
)

# No stale zIndex workaround.
text = text.replace('import androidx.compose.ui.zIndex\\n', '')
text = text.replace('                .zIndex(10f)\\n', '')

# Final artwork correction requested from the reference images:
# - do NOT add a border around the artwork;
# - do NOT increase the artwork's measured height just to create a fade;
# - keep the picture itself at its normal height;
# - let the same page background color paint over the lower part of the picture.
text = re.sub(
    r'        val artFadeDepth = if \(p <= 1f\) lerp\(0\.dp, 96\.dp, p\.coerceIn\(0f, 1f\)\) else 0\.dp\\n',
    '        val artFadeDepth = 0.dp\\n',
    text,
    count=1,
)
text = text.replace(
    '.height(artHeight + artFadeDepth)',
    '.height(artHeight)',
    1,
)

# Remove the decorative white border entirely. The normal player keeps its rounded image
# corners through the clip below; expanded artwork remains edge-to-edge without a border.
text = re.sub(
    r'\\n                \\.border\\(\\n                    if \(p < 0\\.98f\) 1\\.dp else 0\\.dp,\\n                    Color\\.White\\.copy\\(alpha = 0\\.10f\\),\\n                    RoundedCornerShape\(artCorner\)\\n                \\)',
    '',
    text,
    count=1,
)

# Make the lower dissolve noticeably thicker while keeping the image at its original height.
# The first ~58% of the artwork stays readable; the background then progressively covers the
# bottom section, with a full match at the final edge so there is no straight visual line.
text = re.sub(
    r'0\\.00f to Color\\.Transparent,\\n                                0\\.54f to Color\\.Transparent,\\n                                0\\.60f to Color\\.Transparent,\\n                                0\\.66f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.06f\\),\\n                                0\\.74f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.18f\\),\\n                                0\\.82f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.38f\\),\\n                                0\\.90f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.68f\\),\\n                                0\\.96f to themeColors\\.darkBackground\\.copy\\(alpha = 0\\.90f\\),\\n                                1\\.00f to themeColors\\.darkBackground',
    '''0.00f to Color.Transparent,
                                0.52f to Color.Transparent,
                                0.58f to themeColors.darkBackground.copy(alpha = 0.06f),
                                0.64f to themeColors.darkBackground.copy(alpha = 0.18f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.38f),
                                0.80f to themeColors.darkBackground.copy(alpha = 0.60f),
                                0.88f to themeColors.darkBackground.copy(alpha = 0.80f),
                                0.95f to themeColors.darkBackground.copy(alpha = 0.94f),
                                1.00f to themeColors.darkBackground''',
    text,
    count=1,
)

# Slightly reduce the normal-state corner radius so it is rounded but not excessively circular.
text = text.replace('val baseArtCorner = 32.dp', 'val baseArtCorner = 24.dp', 1)

path.write_text(text, encoding="utf-8")
print("PlayerSheet final artwork dissolve/border patch applied.")
