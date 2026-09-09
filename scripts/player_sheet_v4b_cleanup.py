from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# Remove the obsolete second Stage-1 progress bar. The moving
# NowPlayingWaveformProgress instance remains the only expanded-state progress UI.
start_marker = "        // Sleek progress bar in Stage 1 below the title:\n"
end_marker = "        // 4. COMPACT CONTROLS IN STAGE 2"
if start_marker in text:
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    text = text[:start] + text[end:]

# Keep the existing physical-control implementation. These changes only refine
# its geometry; they do not introduce another row or cross-fade between controls.
text = text.replace(
    "val expControlsY = expArtY + expArtHeight + 52.dp",
    "val expControlsY = expArtY + expArtHeight + 34.dp",
)
text = text.replace(
    "val expUpNextY = expControlsY + 66.dp",
    "val expUpNextY = expControlsY + 84.dp",
)

# The artwork needs a real visual fade beyond its nominal bottom edge. The image
# continues underneath the fade area and disappears into the SAME dynamic page
# background instead of ending with a hard rectangular boundary.
if "val artFadeDepth" not in text:
    text = text.replace(
        "        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.\n",
        "        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.\n"
        "        // Extend the artwork below its nominal edge so its lower portion can dissolve\n"
        "        // naturally into the dynamic background, matching the YouTube Music treatment.\n"
        "        val artFadeDepth = if (p <= 1f) lerp(0.dp, 76.dp, p.coerceIn(0f, 1f)) else 0.dp\n",
        1,
    )

text = text.replace(
    ".height(artHeight)\n                .clip(RoundedCornerShape(artCorner))",
    ".height(artHeight + artFadeDepth)\n                .clip(RoundedCornerShape(artCorner))",
    1,
)

old_gradient = '''                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.70f to themeColors.darkBackground.copy(alpha = 0.06f),
                            0.82f to themeColors.darkBackground.copy(alpha = 0.30f),
                            0.92f to themeColors.darkBackground.copy(alpha = 0.68f),
                            1.00f to themeColors.darkBackground.copy(alpha = 0.98f)
                        )
                    )'''
new_gradient = '''                    Brush.verticalGradient(
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
if old_gradient in text:
    text = text.replace(old_gradient, new_gradient, 1)

# The Up Next area must not become a different black/brown rectangle. Leave its
# surface transparent so the same artwork-derived background underneath continues
# through the queue, with only queue cards providing their own contrast.
old_queue_background = ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)"
new_queue_background = ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)"
if old_queue_background in text:
    text = text.replace(old_queue_background, new_queue_background, 1)

# Defensive cleanup: no zIndex dependency is needed for this layered layout.
text = text.replace("import androidx.compose.ui.zIndex\n", "")
text = text.replace("                .zIndex(10f)\n", "")

path.write_text(text, encoding="utf-8")
print("PlayerSheet color blend, artwork fade, and control spacing refined.")
