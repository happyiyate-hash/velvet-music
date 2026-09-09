from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# The artwork itself must stay at its normal measured height. The dissolve is painted over
# the existing image; it must never extend the image with an artificial extra block.
text = text.replace(
    "val artFadeDepth = if (p <= 1f) lerp(0.dp, 96.dp, p.coerceIn(0f, 1f)) else 0.dp",
    "val artFadeDepth = 0.dp",
    1,
)
text = text.replace(
    ".height(artHeight + artFadeDepth)",
    ".height(artHeight)",
    1,
)

# Remove the decorative border completely. The image itself supplies the rounded corners in
# the normal state through the clip below; expanded artwork has no border/card outline.
border_block = '''                .border(
                    if (p < 0.98f) 1.dp else 0.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(artCorner)
                )'''
text = text.replace(border_block, "", 1)

# Use a moderate normal-state radius rather than an overly circular picture.
text = text.replace("val baseArtCorner = 32.dp", "val baseArtCorner = 24.dp", 1)

# Paint a thicker, smooth shadow/dissolve over the lower part of the SAME image. It begins
# around the lower 58-60%, becomes strong through the bottom section, and finishes at the exact
# page background color. Nothing below the image is added to its height.
old_gradient = '''0.00f to Color.Transparent,
                                0.54f to Color.Transparent,
                                0.60f to Color.Transparent,
                                0.66f to themeColors.darkBackground.copy(alpha = 0.06f),
                                0.74f to themeColors.darkBackground.copy(alpha = 0.18f),
                                0.82f to themeColors.darkBackground.copy(alpha = 0.38f),
                                0.90f to themeColors.darkBackground.copy(alpha = 0.68f),
                                0.96f to themeColors.darkBackground.copy(alpha = 0.90f),
                                1.00f to themeColors.darkBackground'''
new_gradient = '''0.00f to Color.Transparent,
                                0.52f to Color.Transparent,
                                0.58f to themeColors.darkBackground.copy(alpha = 0.06f),
                                0.64f to themeColors.darkBackground.copy(alpha = 0.18f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.38f),
                                0.80f to themeColors.darkBackground.copy(alpha = 0.60f),
                                0.88f to themeColors.darkBackground.copy(alpha = 0.80f),
                                0.95f to themeColors.darkBackground.copy(alpha = 0.94f),
                                1.00f to themeColors.darkBackground'''
text = text.replace(old_gradient, new_gradient, 1)

# The page and queue must remain one continuous dynamic background; do not restore a separate
# opaque queue surface.
text = text.replace(
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)",
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)",
    1,
)

path.write_text(text, encoding="utf-8")
print("PlayerSheet artwork dissolve correction applied.")
