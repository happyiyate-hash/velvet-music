from pathlib import Path
import re

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# v4 geometry: baseline keeps shuffle/repeat BELOW the primary row; during the
# upward gesture the exact same controls travel into the side positions.
text = text.replace(
    "val baseShuffleY = baseControlsY + 76.dp + 14.dp",
    "val baseShuffleY = baseControlsY + 76.dp + 22.dp"
)
text = text.replace(
    "val expControlsY = expArtY + expArtHeight + 42.dp",
    "val expControlsY = expArtY + expArtHeight + 52.dp"
)
text = text.replace(
    "val expUpNextY = expControlsY + 70.dp",
    "val expUpNextY = expControlsY + 66.dp"
)

# Remove the old stage-alpha control state; controls themselves must never be
# cross-faded or replaced during the physical transition.
text = text.replace(
    "        val controlsAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)\n"
    "        val shuffleAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)\n"
    "        val stage1ControlsAlpha = ((p - 0.52f) / 0.36f).coerceIn(0f, 1f)\n",
    ""
)

# Replace the waveform/progress host. It is still the original waveform/progress
# composable: the whole component travels upward, while its waveform/timestamps
# fade out and its SAME progress line remains visible over the artwork blend.
start = text.index("        // 5. REAL AUDIO WAVEFORM + PROGRESS BAR")
end = text.index("        // YouTube-style physical control transition v3.", start)
new_wave = '''        // 5. ONE REAL WAVEFORM + PROGRESS COMPONENT.\n        // The component physically travels upward with the drag. Its waveform bars\n        // and timestamps fade, but the SAME progress line stays visible and lands\n        // over the lower part of the artwork instead of creating a second slider.\n        val progressHostY = lerp(\n            baseWaveformY,\n            expArtY + (expArtHeight * 0.90f) - 36.dp,\n            p.coerceIn(0f, 1f)\n        )\n        val movingWaveformAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)\n        val movingTimestampAlpha = (1f - (p / 0.58f)).coerceIn(0f, 1f)\n\n        NowPlayingWaveformProgress(\n            positionMs = playbackPositionMs,\n            durationMs = track.durationMs,\n            isPlaying = isPlaying,\n            telemetry = telemetry,\n            activeColor = themeColors.accent,\n            onSeekTo = onSeekTo,\n            waveformAlpha = movingWaveformAlpha,\n            timestampAlpha = movingTimestampAlpha,\n            modifier = Modifier\n                .offset(x = 16.dp, y = progressHostY)\n                .width(totalWidth - 32.dp)\n        )\n\n'''
text = text[:start] + new_wave + text[end:]

# Replace the v3 control block through the Up Next section marker.
start = text.index("        // YouTube-style physical control transition v3.")
end = text.index("        // 8. UP NEXT HANDLE & CONTENT", start)
new_controls = '''        // YouTube-style physical control transition v4.\n        // There is ONE set of five controls. At rest: shuffle/repeat sit below the\n        // previous/play/next row. As the user drags Up Next upward, those same two\n        // controls physically travel to the left/right sides of the primary row.\n        // No duplicate rail and no fade-out/fade-in replacement is used.\n        val controlT = p.coerceIn(0f, 1f)\n        val controlY = lerp(baseControlsY, expControlsY, controlT)\n        val centerX = totalWidth / 2\n\n        val baseThreeWidth = 236.dp\n        val basePrevX = (totalWidth - baseThreeWidth) / 2\n        val basePlayX = basePrevX + 80.dp\n        val baseNextX = basePrevX + 160.dp\n\n        val expandedPrevX = centerX - 116.dp\n        val expandedPlayX = centerX - 36.dp\n        val expandedNextX = centerX + 60.dp\n        val expandedShuffleX = 12.dp\n        val expandedRepeatX = totalWidth - 64.dp\n\n        val prevX = lerp(basePrevX, expandedPrevX, controlT)\n        val playX = lerp(basePlayX, expandedPlayX, controlT)\n        val nextX = lerp(baseNextX, expandedNextX, controlT)\n        val shuffleX = lerp(24.dp, expandedShuffleX, controlT)\n        val repeatX = lerp(totalWidth - 72.dp, expandedRepeatX, controlT)\n\n        // At rest the secondary controls are clearly below the primary row.\n        // During the upward gesture they rise into the same row.\n        val primaryOffsetY = 0.dp\n        val secondaryOffsetY = lerp(76.dp + 22.dp, 8.dp, controlT)\n\n        Box(\n            modifier = Modifier\n                .fillMaxWidth()\n                .height(170.dp)\n                .offset(y = controlY - 8.dp)\n        ) {\n            AnimatedShuffleIcon(\n                isShuffle = isShuffle,\n                activeColor = themeColors.accent,\n                onClick = onToggleShuffle,\n                modifier = Modifier\n                    .offset(x = shuffleX, y = secondaryOffsetY)\n                    .testTag("player_shuffle_button"),\n                touchSize = 52.dp,\n                iconSize = 28.dp\n            )\n\n            IconButton(\n                onClick = onSkipPrevious,\n                modifier = Modifier\n                    .offset(x = prevX, y = primaryOffsetY)\n                    .size(60.dp)\n                    .testTag("player_previous_button")\n            ) {\n                Icon(\n                    imageVector = Icons.Default.SkipPrevious,\n                    contentDescription = "Previous Track",\n                    tint = Color.White.copy(alpha = 0.95f),\n                    modifier = Modifier.size(42.dp)\n                )\n            }\n\n            Box(\n                modifier = Modifier\n                    .offset(x = playX, y = -4.dp)\n                    .size(76.dp)\n                    .shadow(10.dp, CircleShape, spotColor = themeColors.accent.copy(alpha = 0.34f))\n                    .clip(CircleShape)\n                    .background(Brush.verticalGradient(listOf(themeColors.playPauseGradTop, themeColors.playPauseGradBottom)))\n                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)\n                    .clickable(\n                        interactionSource = remember { MutableInteractionSource() },\n                        indication = null,\n                        onClick = onTogglePlayPause\n                    )\n                    .testTag("player_play_pause_button"),\n                contentAlignment = Alignment.Center\n            ) {\n                Icon(\n                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,\n                    contentDescription = if (isPlaying) "Pause" else "Play",\n                    tint = Color.White,\n                    modifier = Modifier.size(38.dp)\n                )\n            }\n\n            IconButton(\n                onClick = onSkipNext,\n                modifier = Modifier\n                    .offset(x = nextX, y = primaryOffsetY)\n                    .size(60.dp)\n                    .testTag("player_next_button")\n            ) {\n                Icon(\n                    imageVector = Icons.Default.SkipNext,\n                    contentDescription = "Next Track",\n                    tint = Color.White.copy(alpha = 0.95f),\n                    modifier = Modifier.size(42.dp)\n                )\n            }\n\n            AnimatedRepeatIcon(\n                isRepeat = isRepeat,\n                activeColor = themeColors.accent,\n                onClick = onToggleRepeat,\n                modifier = Modifier\n                    .offset(x = repeatX, y = secondaryOffsetY)\n                    .testTag("player_repeat_button"),\n                touchSize = 52.dp,\n                iconSize = 28.dp\n            )\n        }\n\n'''
text = text[:start] + new_controls + text[end:]

# Strengthen the bottom artwork smoke/blend so the progress line sits over the\n# blended area, not below the image on a separate surface.
old = '''                            0.72f to themeColors.darkBackground.copy(alpha = 0.10f),\n                            0.88f to themeColors.darkBackground.copy(alpha = 0.62f),\n                            1.00f to themeColors.darkBackground.copy(alpha = 0.98f)'''
new = '''                            0.70f to themeColors.darkBackground.copy(alpha = 0.06f),\n                            0.82f to themeColors.darkBackground.copy(alpha = 0.30f),\n                            0.92f to themeColors.darkBackground.copy(alpha = 0.68f),\n                            1.00f to themeColors.darkBackground.copy(alpha = 0.98f)'''
text = text.replace(old, new)

# Extend the waveform/progress composable with independent fade controls.
text = text.replace(
    "    onSeekTo: (Long) -> Unit,\n    modifier: Modifier = Modifier\n)",
    "    onSeekTo: (Long) -> Unit,\n    waveformAlpha: Float = 1f,\n    timestampAlpha: Float = 1f,\n    modifier: Modifier = Modifier\n)",
    1
)
text = text.replace(
    "                .height(24.dp)\n        ) {",
    "                .height(24.dp)\n                .graphicsLayer { alpha = waveformAlpha.coerceIn(0f, 1f) }\n        ) {",
    1
)
text = text.replace(
    "        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.SpaceBetween\n        ) {",
    "        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .graphicsLayer { alpha = timestampAlpha.coerceIn(0f, 1f) },\n            horizontalArrangement = Arrangement.SpaceBetween\n        ) {",
    1
)

# Queue surface must be opaque and identical to the player background so controls\n# underneath cannot remain visible or clickable once the sheet is dragged up.
text = text.replace(
    ".background(themeColors.darkBackground)\n                .pointerInput(Unit) {",
    ".background(themeColors.darkBackground)\n                .zIndex(10f)\n                .pointerInput(Unit) {",
    1
)

if "YouTube-style physical control transition v4." not in text:
    raise SystemExit("v4 control block was not applied")

path.write_text(text, encoding="utf-8")
print("PlayerSheet v4 patch applied")
