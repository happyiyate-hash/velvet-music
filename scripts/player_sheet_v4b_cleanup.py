from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# Keep artwork at its measured/natural height. Never create an artificial shadow block below it.
text = text.replace("val artFadeDepth = if (p <= 1f) lerp(0.dp, 96.dp, p.coerceIn(0f, 1f)) else 0.dp", "val artFadeDepth = 0.dp", 1)
text = text.replace(".height(artHeight + artFadeDepth)", ".height(artHeight)", 1)

# No decorative outline around the album artwork.
border_block = '''                .border(
                    if (p < 0.98f) 1.dp else 0.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(artCorner)
                )'''
text = text.replace(border_block, "", 1)
text = text.replace("val baseArtCorner = 32.dp", "val baseArtCorner = 24.dp", 1)

# Brighten the dynamic player surface, with a subtly darker lower end.
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

# The artwork has NO dissolve at rest or as the compact thumbnail. The dissolve only appears
# during the 0->1 opening transition and is intentionally thicker than the previous version.
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

# Queue remains transparent/integrated into the same page background.
text = text.replace(
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)",
    ".height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)",
    1,
)

# Make previous/play/next mathematically symmetric. The 60dp side buttons and 76dp play disc
# have different widths, so their LEFT offsets must compensate for that width difference.
old_controls = '''        val baseCenterSpacing = 80.dp
        val basePlayX = centerX - 38.dp
        val basePrevX = basePlayX - 50.dp
        val baseNextX = basePlayX + 130.dp

        val expandedCenterSpacing = 84.dp
        val expandedPlayX = centerX - 38.dp
        val expandedPrevX = expandedPlayX - 46.dp
        val expandedNextX = expandedPlayX + 122.dp'''
new_controls = '''        val basePlayX = centerX - 38.dp
        val basePrevX = basePlayX - 42.dp
        val baseNextX = basePlayX + 122.dp

        val expandedPlayX = centerX - 38.dp
        val expandedPrevX = expandedPlayX - 46.dp
        val expandedNextX = expandedPlayX + 92.dp'''
text = text.replace(old_controls, new_controls, 1)

# Also repair the same block if this script is ever run against the immediately previous patch.
text = text.replace(
    '''        val baseCenterSpacing = 80.dp
        val basePlayX = centerX - 38.dp
        val basePrevX = basePlayX - 50.dp
        val baseNextX = basePlayX + 130.dp

        val expandedCenterSpacing = 84.dp
        val expandedPlayX = centerX - 38.dp
        val expandedPrevX = expandedPlayX - 46.dp
        val expandedNextX = expandedPlayX + 122.dp''',
    new_controls,
    1,
)

# Fade the complete waveform/progress component by the time the queue reaches the compact state.
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

# The actual progress line is inside the existing component, so give that Canvas its own alpha.
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
print("PlayerSheet refinement applied: clean rest artwork, stronger expand-only dissolve, brighter dynamic background, symmetric playback controls, and fading progress.")
