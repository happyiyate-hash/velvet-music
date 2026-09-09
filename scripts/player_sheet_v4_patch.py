from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# The build failure came from the v4 patch adding zIndex without importing it.
# The queue is now drawn as the same page surface, so no zIndex is needed.
text = text.replace("import androidx.compose.ui.zIndex\n", "")
text = re.sub(r"\n\s*\.zIndex\(10f\)", "", text)

# Keep the expanded artwork at the exact same natural height as the normal artwork.
text = re.sub(
    r"val expArtHeight = \(totalHeight \* 0\.42f\)\.coerceIn\(300\.dp, 410\.dp\)",
    "val expArtHeight = baseArtSize",
    text,
    count=1,
)

# Expanded controls move UP a little, while the queue handle stays safely below them.
text = re.sub(
    r"val expControlsY = expArtY \+ expArtHeight \+ \d+\.dp",
    "val expControlsY = expArtY + expArtHeight + 42.dp",
    text,
    count=1,
)
text = re.sub(
    r"val expUpNextY = expControlsY \+ \d+\.dp",
    "val expUpNextY = expControlsY + 78.dp",
    text,
    count=1,
)

# The page background must be ONE exact extracted artwork color. Remove the radial bloom
# because it made the queue look like a second colored card instead of one continuous surface.
canvas_pattern = re.compile(
    r"        // Dynamic background atmosphere:.*?\n        Canvas\(modifier = Modifier\.fillMaxSize\(\)\) \{.*?\n        \}\n\n        // 1\. TOP BAR",
    re.S,
)
canvas_replacement = '''        // Dynamic background atmosphere: one exact surface color derived from the artwork.
        // Do not add a separate queue/card color or a vertical tint behind the queue.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = themeColors.darkBackground)
        }

        // 1. TOP BAR'''
text, canvas_count = canvas_pattern.subn(canvas_replacement, text, count=1)
if canvas_count != 1:
    raise SystemExit(f"background block replacement count={canvas_count}")

# Make the artwork dissolve smoothly into that exact same surface. This is a soft mask,
# not a separate card/shadow: the photo remains normal-sized and the lower part is gradually
# covered by the page color with no hard rectangular edge.
old_fade = re.compile(
    r"0\.56f to Color\.Transparent,\s*"
    r"0\.62f to Color\.Transparent,\s*"
    r"0\.68f to themeColors\.darkBackground\.copy\(alpha = 0\.12f\),\s*"
    r"0\.76f to themeColors\.darkBackground\.copy\(alpha = 0\.36f\),\s*"
    r"0\.84f to themeColors\.darkBackground\.copy\(alpha = 0\.64f\),\s*"
    r"0\.91f to themeColors\.darkBackground\.copy\(alpha = 0\.86f\),\s*"
    r"1\.00f to themeColors\.darkBackground",
)
new_fade = '''0.54f to Color.Transparent,
                                0.60f to Color.Transparent,
                                0.66f to themeColors.darkBackground.copy(alpha = 0.06f),
                                0.74f to themeColors.darkBackground.copy(alpha = 0.18f),
                                0.82f to themeColors.darkBackground.copy(alpha = 0.38f),
                                0.90f to themeColors.darkBackground.copy(alpha = 0.68f),
                                0.96f to themeColors.darkBackground.copy(alpha = 0.90f),
                                1.00f to themeColors.darkBackground'''
text, fade_count = old_fade.subn(new_fade, text, count=1)
if fade_count != 1:
    raise SystemExit(f"artwork fade replacement count={fade_count}")

# The queue surface is transparent because the whole page underneath is already the exact
# extracted background color. This keeps it visually continuous rather than a card.
text = text.replace(
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)",
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)",
    1,
)

# At stage 2, the SAME five physical controls fade away as the queue takes over. They are not
# replaced by another control row and cannot remain visible behind the queue.
marker = "        val secondaryOffsetY = lerp(76.dp + 22.dp, 8.dp, controlT)"
if marker not in text:
    marker = "        val secondaryOffsetY = lerp(76.dp + 28.dp, 6.dp, controlT)"
if marker not in text:
    raise SystemExit("secondaryOffsetY marker not found")
text = text.replace(
    marker,
    marker + "\n        val expandedControlsFade = (1f - ((p - 1f) / 0.45f)).coerceIn(0f, 1f)",
    1,
)
text = text.replace(
    ".height(170.dp)\n                .offset(y = controlY - 8.dp)",
    ".height(170.dp)\n                .offset(y = controlY - 8.dp)\n                .graphicsLayer { alpha = expandedControlsFade }",
    1,
)

# The waveform/timestamps fade away progressively, but the existing progress line remains the
# only progress line and travels with the artwork.
text = re.sub(
    r"expArtY \+ \(expArtHeight \* 0\.90f\) - 36\.dp",
    "expArtY + expArtHeight - 34.dp",
    text,
    count=1,
)

# Never reintroduce the broken zIndex workaround.
text = re.sub(r"\n\s*\.zIndex\([^\n]+\)", "", text)

path.write_text(text, encoding="utf-8")
print("PlayerSheet v4b visual cleanup applied.")
