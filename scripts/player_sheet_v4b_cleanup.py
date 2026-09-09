from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# Keep artwork at its measured/natural height. Never create an artificial shadow block below it.
text = text.replace(
    "val artFadeDepth = if (p <= 1f) lerp(0.dp, 96.dp, p.coerceIn(0f, 1f)) else 0.dp",
    "val artFadeDepth = 0.dp",
    1,
)
text = text.replace(".height(artHeight + artFadeDepth)", ".height(artHeight)", 1)

# No decorative outline around the album artwork.
border_block = '''                .border(
                    if (p < 0.98f) 1.dp else 0.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(artCorner)
                )'''
text = text.replace(border_block, "", 1)

# Normal artwork should have a clean, moderate rounded corner treatment.
text = text.replace("val baseArtCorner = 32.dp", "val baseArtCorner = 24.dp", 1)

# Brighten the dynamic player surface without changing the extracted palette itself.
old_bg = '''        // Dynamic background atmosphere: one exact surface color derived from the artwork.
        // Do not add a separate queue/card color or a vertical tint behind the queue.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = themeColors.darkBackground)
        }'''
new_bg = '''        // Dynamic player surface: lift the extracted artwork color so it is not nearly black.
        // Keep the upper area brighter and let the bottom fall off slightly darker.
        val extractedBg = themeColors.darkBackground
        val playerBackgroundTop = Color(
            red = (extractedBg.red * 1.18f + 0.025f).coerceAtMost(1f),
            green = (extractedBg.green * 1.18f + 0.025f).coerceAtMost(1f),
            blue = (extractedBg.blue * 1.18f + 0.025f).coerceAtMost(1f),
            alpha = 1f
        )
        val playerBackgroundBottom = Color(
            red = (playerBackgroundTop.red * 0.88f).coerceAtLeast(0f),
            green = (playerBackgroundTop.green * 0.88f).coerceAtLeast(0f),
            blue = (playerBackgroundTop.blue * 0.88f).coerceAtLeast(0f),
            alpha = 1f
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(playerBackgroundTop, playerBackgroundBottom)
                )
            )
        }'''
text = text.replace(old_bg, new_bg, 1)

# Use the same dynamic background colors for the artwork dissolve. Crucially, the dissolve is
# invisible at rest and on the compact thumbnail; it only appears as the artwork is dragged into
# the large expanded state.
old_gradient = '''            // Broad shadow-like color wash: the artwork starts disappearing around 60%,
            // then the exact page surface color becomes dominant before the nominal edge.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.52f to Color.Transparent,
                                0.58f to themeColors.darkBackground.copy(alpha = 0.06f),
                                0.64f to themeColors.darkBackground.copy(alpha = 0.18f),
                                0.72f to themeColors.darkBackground.copy(alpha = 0.38f),
                                0.80f to themeColors.darkBackground.copy(alpha = 0.60f),
                                0.88f to themeColors.darkBackground.copy(alpha = 0.80f),
                                0.95f to themeColors.darkBackground.copy(alpha = 0.94f),
                                1.00f to themeColors.darkBackground
                            )
                        )
                    )
            )'''
new_gradient = '''            // Thick lower dissolve. It is completely absent at rest and on the compact
            // thumbnail, and appears only while opening the large artwork state.
            val artworkFadeAlpha = if (p <= 1f) {
                ((p - 0.08f) / 0.55f).coerceIn(0f, 1f)
            } else {
                0f
            }
            if (artworkFadeAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = artworkFadeAlpha }
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.50f to Color.Transparent,
                                    0.56f to playerBackgroundTop.copy(alpha = 0.04f),
                                    0.62f to playerBackgroundTop.copy(alpha = 0.16f),
                                    0.69f to playerBackgroundTop.copy(alpha = 0.36f),
                                    0.76f to playerBackgroundTop.copy(alpha = 0.58f),
                                    0.84f to playerBackgroundBottom.copy(alpha = 0.78f),
                                    0.91f to playerBackgroundBottom.copy(alpha = 0.93f),
                                    0.97f to playerBackgroundBottom.copy(alpha = 0.98f),
                                    1.00f to playerBackgroundBottom
                                )
                            )
                        )
                )
            }'''
text = text.replace(old_gradient, new_gradient, 1)

# The queue is integrated into the same background; no separate surface/card.
text = text.replace(
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)",
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)",
    1,
)

# Make the previous/play/next centers mathematically symmetric at rest and in the expanded row.
# IconButton is 60dp wide while the play disc is 76dp, so use equal center-to-center spacing.
old_controls = '''        val baseThreeWidth = 236.dp
        val basePrevX = (totalWidth - baseThreeWidth) / 2
        val basePlayX = basePrevX + 80.dp
        val baseNextX = basePrevX + 160.dp

        val expandedPrevX = centerX - 116.dp
        val expandedPlayX = centerX - 36.dp
        val expandedNextX = centerX + 60.dp'''
new_controls = '''        val baseCenterSpacing = 80.dp
        val basePlayX = centerX - 38.dp
        val basePrevX = basePlayX - 50.dp
        val baseNextX = basePlayX + 130.dp

        val expandedCenterSpacing = 84.dp
        val expandedPlayX = centerX - 38.dp
        val expandedPrevX = expandedPlayX - (expandedCenterSpacing - 8.dp)
        val expandedNextX = expandedPlayX + (expandedCenterSpacing + 8.dp)'''
text = text.replace(old_controls, new_controls, 1)

# Keep the moving control group internally centered. The slightly different 60dp/76dp widths are
# accounted for above so the visual centers do not drift left or right while dragging back down.
text = text.replace(
    "val expandedPrevX = expandedPlayX - (expandedCenterSpacing - 8.dp)\n        val expandedNextX = expandedPlayX + (expandedCenterSpacing + 8.dp)",
    "val expandedPrevX = expandedPlayX - 46.dp\n        val expandedNextX = expandedPlayX + 122.dp",
    1,
)

# Fade the one real waveform/progress component completely by the time the queue is fully open.
# The progress line itself must disappear too, not only the waveform bars/timestamps.
old_wave_call = '''        NowPlayingWaveformProgress(
            positionMs = playbackPositionMs,
            durationMs = track.durationMs,
            isPlaying = isPlaying,
            telemetry = telemetry,
            activeColor = themeColors.accent,
            onSeekTo = onSeekTo,
            waveformAlpha = movingWaveformAlpha,
            timestampAlpha = movingTimestampAlpha,
            modifier = Modifier
                .offset(x = 16.dp, y = progressHostY)
                .width(totalWidth - 32.dp)
        )'''
new_wave_call = '''        val progressComponentAlpha = (1f - (p / 1.05f)).coerceIn(0f, 1f)
        if (progressComponentAlpha > 0f) {
            NowPlayingWaveformProgress(
                positionMs = playbackPositionMs,
                durationMs = track.durationMs,
                isPlaying = isPlaying,
                telemetry = telemetry,
                activeColor = themeColors.accent,
                onSeekTo = onSeekTo,
                waveformAlpha = movingWaveformAlpha * progressComponentAlpha,
                timestampAlpha = movingTimestampAlpha * progressComponentAlpha,
                progressAlpha = progressComponentAlpha,
                modifier = Modifier
                    .offset(x = 16.dp, y = progressHostY)
                    .width(totalWidth - 32.dp)
            )
        }'''
text = text.replace(old_wave_call, new_wave_call, 1)

# Extend the existing component with a single alpha for the actual progress line as well.
text = text.replace(
    "    timestampAlpha: Float = 1f,\n    modifier: Modifier = Modifier",
    "    timestampAlpha: Float = 1f,\n    progressAlpha: Float = 1f,\n    modifier: Modifier = Modifier",
    1,
)
text = text.replace(
    "                .height(8.dp)\n        ) {",
    "                .height(8.dp)\n                .graphicsLayer { alpha = progressAlpha.coerceIn(0f, 1f) }\n        ) {",
    1,
)

path.write_text(text, encoding="utf-8")
print("PlayerSheet refined: rest artwork is clean, expanded dissolve is stronger, background is brighter, controls are centered, and progress fades with the queue.")
