from pathlib import Path

# Home Feed: extend crimson gradient, raise secondary contrast, and keep active chromatic background off Home.
path = Path("app/src/main/java/com/example/ui/HomeFeedScreen.kt")
text = path.read_text()

replacements = [
    (
        "import com.example.ui.theme.VelvetCardBorder\n",
        "import com.example.ui.theme.VelvetCardBorder\nimport com.example.ui.theme.BrightAshGray\n",
    ),
    (
        "modifier = modifier\n            .fillMaxSize()\n            .testTag(\"home_feed_screen\"),",
        "modifier = modifier\n            .fillMaxSize()\n            .background(\n                Brush.verticalGradient(\n                    colorStops = arrayOf(\n                        0.0f to Color(0xFF621017),\n                        0.55f to Color(0xFF38090C),\n                        1.0f to Color(0xFF0F0B0C)\n                    )\n                )\n            )\n            .testTag(\"home_feed_screen\"),",
    ),
    (
        "color = VelvetTextSecondary.copy(alpha = 0.90f)",
        "color = BrightAshGray.copy(alpha = 0.95f)",
    ),
    (
        "color = VelvetTextTertiary",
        "color = BrightAshGray.copy(alpha = 0.82f)",
    ),
    (
        "color = VelvetTextSecondary.copy(alpha = 0.85f)",
        "color = BrightAshGray.copy(alpha = 0.92f)",
    ),
    (
        "color = VelvetTextSecondary,",
        "color = BrightAshGray.copy(alpha = 0.92f),",
    ),
    (
        "color = VelvetTextSecondary.copy(alpha = 0.6f),",
        "color = BrightAshGray.copy(alpha = 0.72f),",
    ),
    (
        "color = Color.White.copy(alpha = 0.03f))\n                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))",
        "color = Color(0x33FFFFFF))\n                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))",
    ),
    (
        "Brush.verticalGradient(\n                            listOf(\n                                Color.White.copy(alpha = 0.06f),\n                                Color(0xFFE50914).copy(alpha = 0.04f),\n                                Color.White.copy(alpha = 0.03f)\n                            )\n                        )",
        "Color(0x33FFFFFF)",
    ),
    (
        ".border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))",
        ".border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(22.dp))",
    ),
]

for old, new in replacements:
    if old in text:
        text = text.replace(old, new, 1)

path.write_text(text)
print("Home Feed visual polish applied (idempotent)")
