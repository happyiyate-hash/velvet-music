from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# This patch is intentionally limited to the existing PlayerSheet transition.
# It does not create a second control rail or a second progress component.

# 1) Queue must be part of the same dynamic surface. Never paint a separate
# dark/black rectangle behind Up Next.
text = text.replace(
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)",
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)",
    1,
)

# 2) The artwork must dissolve into the exact page palette rather than ending
# as a hard-edged rectangle. Keep the image itself full-bleed; the overlay is
# the only thing that masks its lower edge.
if "val artFadeDepth" not in text:
    text = text.replace(
        "        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.\n",
        "        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.\n"
        "        val artFadeDepth = if (p <= 1f) lerp(0.dp, 92.dp, p.coerceIn(0f, 1f)) else 0.dp\n",
        1,
    )
text = text.replace(
    ".height(artHeight)\n                .clip(RoundedCornerShape(artCorner))",
    ".height(artHeight + artFadeDepth)\n                .clip(RoundedCornerShape(artCorner))",
    1,
)

# Replace the existing lower-artwork fade with a longer, softer fade into the
# same bottom tone used by the full-page atmospheric gradient.
old_fade = '''                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.58f to Color.Transparent,
                            0.70f to themeColors.darkBackground.copy(alpha = 0.05f),
                            0.79f to themeColors.darkBackground.copy(alpha = 0.16f),
                            0.87f to themeColors.darkBackground.copy(alpha = 0.38f),
                            0.94f to themeColors.darkBackground.copy(alpha = 0.68f),
                            1.00f to themeColors.darkBackground.copy(alpha = 0.98f)
                        )
                    )'''
new_fade = '''                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.56f to Color.Transparent,
                            0.68f to themeColors.bgMidLower.copy(alpha = 0.08f),
                            0.76f to themeColors.bgMidLower.copy(alpha = 0.22f),
                            0.84f to themeColors.bgBottom.copy(alpha = 0.44f),
                            0.92f to themeColors.bgBottom.copy(alpha = 0.72f),
                            1.00f to themeColors.bgBottom.copy(alpha = 0.96f)
                        )
                    )'''
if old_fade in text:
    text = text.replace(old_fade, new_fade, 1)

# 3) The existing waveform/progress is the ONLY progress UI. Keep it traveling
# over the artwork during phase 1. No extra LinearProgressIndicator is added.
# Its host is moved slightly upward so the line sits over the lower artwork,
# rather than underneath the picture.
text = text.replace(
    "expArtY + (expArtHeight * 0.90f) - 36.dp,",
    "expArtY + (expArtHeight * 0.90f) - 48.dp,",
    1,
)

# 4) Resting state: primary controls sit a little lower, closer to the secondary
# shuffle/repeat row. Expanded state: the whole five-control rail sits a little
# higher, leaving a clean gap before the Up Next gesture handle.
text = text.replace(
    "val baseControlsY = baseWaveformY + 52.dp + 18.dp",
    "val baseControlsY = baseWaveformY + 52.dp + 28.dp",
    1,
)
text = text.replace(
    "val expControlsY = expArtY + expArtHeight + 34.dp",
    "val expControlsY = expArtY + expArtHeight + 26.dp",
    1,
)

# 5) Keep secondary controls below the primary controls at rest, and move them
# into the same horizontal rail only as the user actually drags toward Up Next.
text = text.replace(
    "val secondaryOffsetY = lerp(76.dp + 22.dp, 8.dp, controlT)",
    "val secondaryOffsetY = lerp(76.dp + 28.dp, 6.dp, controlT)",
    1,
)

# 6) The five controls themselves remain one physical set. Slightly increase the
# play/pause and previous/next visual sizes without changing their hit targets.
text = text.replace(
    ".size(56.dp)\n                        .testTag(\"player_previous_button\")",
    ".size(60.dp)\n                        .testTag(\"player_previous_button\")",
    1,
)
text = text.replace(
    "modifier = Modifier.size(40.dp)",
    "modifier = Modifier.size(44.dp)",
    1,
)
text = text.replace(
    ".size(40.dp)\n                        .testTag(\"player_next_button\")",
    ".size(60.dp)\n                        .testTag(\"player_next_button\")",
    1,
)
text = text.replace(
    ".size(72.dp)\n                        .shadow(10.dp, CircleShape",
    ".size(78.dp)\n                        .shadow(10.dp, CircleShape",
    1,
)
text = text.replace(
    "modifier = Modifier.size(36.dp)\n                ) {",
    "modifier = Modifier.size(40.dp)\n                ) {",
    1,
)
text = text.replace(
    ".size(40.dp)\n                        .testTag(\"player_next_button\")",
    ".size(60.dp)\n                        .testTag(\"player_next_button\")",
    1,
)

# Remove the old zIndex workaround if it is still present; the queue is opaque
# only through its actual content/cards and transparent elsewhere.
text = text.replace("import androidx.compose.ui.zIndex\n", "")
text = text.replace("                .zIndex(10f)\n", "")

path.write_text(text, encoding="utf-8")
print("PlayerSheet v5 visual blend and geometry patch applied.")