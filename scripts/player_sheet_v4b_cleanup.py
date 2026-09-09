from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# Queue is one continuous surface with the artwork-derived page background.
text = text.replace(
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)",
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)",
    1,
)

# Make the artwork fade into the page instead of stretching the image itself.
# The outer box supplies the fade depth; the actual image keeps its intended
# aspect/crop and ends underneath the transparent-to-background gradient.
if "val artFadeDepth" not in text:
    text = text.replace(
        "        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.\n",
        "        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.\n"
        "        val artFadeDepth = if (p <= 1f) lerp(0.dp, 92.dp, p.coerceIn(0f, 1f)) else 0.dp\n",
        1,
    )

text = text.replace(
    ".height(artHeight + artFadeDepth)\n                .clip(RoundedCornerShape(artCorner))",
    ".height(artHeight + artFadeDepth)\n                .clip(RoundedCornerShape(artCorner))",
    1,
)

old_art_content = '''            TrackArtworkImage(track = track, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.56f to Color.Transparent,
                            0.68f to themeColors.bgMidLower.copy(alpha = 0.08f),
                            0.76f to themeColors.bgMidLower.copy(alpha = 0.22f),
                            0.84f to themeColors.bgBottom.copy(alpha = 0.44f),
                            0.92f to themeColors.bgBottom.copy(alpha = 0.72f),
                            1.00f to themeColors.bgBottom.copy(alpha = 0.96f)
                        )
                    )
                )
            )'''
new_art_content = '''            // Keep the real artwork at its normal height. The remaining fade depth is
            // transparent space over the SAME dynamic background, so there is no hard edge.
            Box(
                modifier = Modifier
                    .width(artWidth)
                    .height(artHeight)
            ) {
                TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.54f to Color.Transparent,
                            0.64f to Color.Transparent,
                            0.72f to themeColors.bgMidLower.copy(alpha = 0.10f),
                            0.80f to themeColors.bgBottom.copy(alpha = 0.30f),
                            0.89f to themeColors.bgBottom.copy(alpha = 0.62f),
                            1.00f to themeColors.bgBottom.copy(alpha = 0.96f)
                        )
                    )
                )
            )'''
if old_art_content in text:
    text = text.replace(old_art_content, new_art_content, 1)

# Keep the SAME waveform/progress component and place its line slightly inside
# the artwork's lower fade area. No second progress bar is introduced.
text = text.replace(
    "expArtY + (expArtHeight * 0.90f) - 48.dp,",
    "expArtY + (expArtHeight * 0.90f) - 48.dp,",
    1,
)

# Resting state: move primary controls slightly down. Expanded state: move the
# physical five-control rail upward so the gesture handle has breathing room.
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
text = text.replace(
    "val secondaryOffsetY = lerp(76.dp + 22.dp, 8.dp, controlT)",
    "val secondaryOffsetY = lerp(76.dp + 28.dp, 6.dp, controlT)",
    1,
)

# Increase the primary previous/next visual size and the central play/pause
# button while keeping the same physical controls and hit areas.
text = text.replace(
    ".size(56.dp)\n                    .testTag(\"player_previous_button\")",
    ".size(60.dp)\n                    .testTag(\"player_previous_button\")",
    1,
)
text = text.replace(
    ".size(72.dp)\n                    .shadow(10.dp, CircleShape",
    ".size(80.dp)\n                    .shadow(10.dp, CircleShape",
    1,
)
text = text.replace(
    "modifier = Modifier.size(36.dp)\n                )",
    "modifier = Modifier.size(40.dp)\n                )",
    1,
)
text = text.replace(
    ".size(56.dp)\n                    .testTag(\"player_next_button\")",
    ".size(60.dp)\n                    .testTag(\"player_next_button\")",
    1,
)

# Remove any stale zIndex workaround from earlier iterations.
text = text.replace("import androidx.compose.ui.zIndex\n", "")
text = text.replace("                .zIndex(10f)\n", "")

path.write_text(text, encoding="utf-8")
print("PlayerSheet v6 artwork fade and control geometry patch applied.")