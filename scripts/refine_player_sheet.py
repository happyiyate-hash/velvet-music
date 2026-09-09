from pathlib import Path
import re

path = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = path.read_text()

if '// YouTube-style physical control transition v3' in s:
    print('PlayerSheet v3 already applied.')
    raise SystemExit(0)

if '// YouTube-style physical control transition v2' not in s:
    raise SystemExit('Expected v2 physical transition marker not found; refusing to patch unknown layout.')

# Geometry: slightly lower primary controls, but bring Up Next itself upward.
s = s.replace(
    'val expControlsY = expArtY + expArtHeight + 26.dp',
    'val expControlsY = expArtY + expArtHeight + 42.dp'
)
s = s.replace(
    'val expUpNextY = expControlsY + 82.dp',
    'val expUpNextY = expControlsY + 70.dp'
)

physical = r'''        // YouTube-style physical control transition v3.
        // One physical set of playback controls is used throughout phase 0 -> phase 1.
        // They move continuously; there is no duplicate/fade-in replacement row.
        val controlT = p.coerceIn(0f, 1f)
        val controlY = lerp(baseControlsY, expControlsY, controlT)
        val centerX = totalWidth / 2

        // Baseline positions: the original three primary controls remain centered.
        val baseThreeWidth = 236.dp
        val basePrevX = (totalWidth - baseThreeWidth) / 2
        val basePlayX = basePrevX + 80.dp
        val baseNextX = basePrevX + 160.dp

        // Expanded positions: shuffle/repeat travel horizontally into the same control rail.
        val expandedPrevX = centerX - 120.dp
        val expandedPlayX = centerX - 36.dp
        val expandedNextX = centerX + 64.dp
        val expandedShuffleX = 8.dp
        val expandedRepeatX = totalWidth - 60.dp

        val shuffleX = lerp(24.dp, expandedShuffleX, controlT)
        val repeatX = lerp(totalWidth - 72.dp, expandedRepeatX, controlT)
        val prevX = lerp(basePrevX, expandedPrevX, controlT)
        val playX = lerp(basePlayX, expandedPlayX, controlT)
        val nextX = lerp(baseNextX, expandedNextX, controlT)

        // Keep every control on the exact same vertical center line in phase 1.
        // The center play button is larger, so its top offset is slightly smaller.
        val shuffleY = 10.dp
        val repeatY = 10.dp
        val prevY = 8.dp
        val playY = 0.dp
        val nextY = 8.dp

        // During phase 2 the queue surface physically covers these controls. They are NOT
        // interactive through the queue because the queue is drawn later with an opaque surface.
        val primaryControlsVisible = p < 1.02f

        if (primaryControlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .offset(y = controlY - 8.dp)
            ) {
                AnimatedShuffleIcon(
                    isShuffle = isShuffle,
                    activeColor = themeColors.accent,
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .offset(x = shuffleX, y = shuffleY)
                        .testTag("player_shuffle_button"),
                    touchSize = 52.dp,
                    iconSize = 28.dp
                )

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .offset(x = prevX, y = prevY)
                        .size(56.dp)
                        .testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .offset(x = playX, y = playY)
                        .size(72.dp)
                        .shadow(10.dp, CircleShape, spotColor = themeColors.accent.copy(alpha = 0.34f))
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(themeColors.playPauseGradTop, themeColors.playPauseGradBottom)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTogglePlayPause
                        )
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .offset(x = nextX, y = nextY)
                        .size(56.dp)
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                AnimatedRepeatIcon(
                    isRepeat = isRepeat,
                    activeColor = themeColors.accent,
                    onClick = onToggleRepeat,
                    modifier = Modifier
                        .offset(x = repeatX, y = repeatY)
                        .testTag("player_repeat_button"),
                    touchSize = 52.dp,
                    iconSize = 28.dp
                )
            }
        }

'''
pattern = r'        // YouTube-style physical control transition v2\..*?        // 8\. UP NEXT HANDLE & CONTENT'
s, n = re.subn(pattern, physical + '        // 8. UP NEXT HANDLE & CONTENT', s, count=1, flags=re.S)
if n != 1:
    raise SystemExit(f'v2 control block replacement count={n}')

# The queue is an opaque foreground surface in compact phase. This prevents the old primary
# controls from showing through and also prevents taps from reaching them.
s = s.replace(
'''            modifier = Modifier
                .offset(x = 0.dp, y = upNextY)
                .fillMaxWidth()
                .height(upNextHeight.coerceAtLeast(54.dp))''',
'''            modifier = Modifier
                .offset(x = 0.dp, y = upNextY)
                .fillMaxWidth()
                .height(upNextHeight.coerceAtLeast(54.dp))
                .background(themeColors.darkBackground)''',
1
)

path.write_text(s)
print('PlayerSheet v3 physical transition patch applied.')
