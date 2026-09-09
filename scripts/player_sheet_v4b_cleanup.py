from pathlib import Path

path = Path("app/src/main/java/com/example/ui/PlayerSheet.kt")
text = path.read_text(encoding="utf-8")

# v7: keep the artwork at the same height used by the normal player. The expanded
# state may widen the artwork, but must never make it taller just because the sheet expands.
text = text.replace(
    'val expArtHeight = (totalHeight * 0.42f).coerceIn(300.dp, 410.dp)',
    'val expArtHeight = baseArtSize'
)

# Give the expanded controls a little more breathing room from the artwork/gesture handle,
# while keeping the resting controls slightly lower as requested.
text = text.replace(
    'val baseControlsY = baseWaveformY + 52.dp + 28.dp',
    'val baseControlsY = baseWaveformY + 52.dp + 44.dp'
)
text = text.replace(
    'val expControlsY = expArtY + expArtHeight + 42.dp',
    'val expControlsY = expArtY + expArtHeight + 18.dp'
)
text = text.replace(
    'val expUpNextY = expControlsY + 70.dp',
    'val expUpNextY = expControlsY + 72.dp'
)

# The page is one dynamic surface. Do not give Up Next a separate black/card background.
text = text.replace(
    '.height(upNextHeight.coerceAtLeast(54.dp))\n                .background(themeColors.darkBackground)',
    '.height(upNextHeight.coerceAtLeast(54.dp))\n                .background(Color.Transparent)',
    1
)

# Replace the old artwork container with a real bottom dissolve. The image itself ends at
# artHeight, while a separate overlay extends below it and paints the artwork away using the
# exact same page surface color. This creates the requested "background covering the picture"
# effect instead of a hard rectangular image edge.
start = text.find('        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.')
end = text.find('        // 3. SONG TITLE & ARTIST.', start)
if start != -1 and end != -1:
    new_art = '''        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.
        // The artwork keeps its normal height. Only its bottom is dissolved into the same
        // dynamic page surface, beginning around the lower 60% rather than at the hard edge.
        val artFadeDepth = if (p <= 1f) lerp(0.dp, 96.dp, p.coerceIn(0f, 1f)) else 0.dp
        Box(
            modifier = Modifier
                .offset(x = artX, y = artY)
                .width(artWidth)
                .height(artHeight + artFadeDepth)
                .border(
                    if (p < 0.98f) 1.dp else 0.dp,
                    Color.White.copy(alpha = 0.10f),
                    RoundedCornerShape(artCorner)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        coroutineScope.launch {
                            dragProgress.animateTo(
                                if (p > 1.2f) 1f else if (p > 0.2f) 0f else 1f,
                                tween(320, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                )
                .then(
                    if (p < 1.65f) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { onDragFinish() },
                            onDragCancel = { onDragFinish() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount)
                            }
                        )
                    } else Modifier
                )
        ) {
            // The picture is clipped only to its own natural rectangle/corners.
            // The fade is deliberately OUTSIDE that picture so the background can cover it.
            Box(
                modifier = Modifier
                    .width(artWidth)
                    .height(artHeight)
                    .clip(RoundedCornerShape(artCorner))
            ) {
                TrackArtworkImage(
                    track = track,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Broad shadow-like color wash: the artwork starts disappearing around 60%,
            // then the exact page surface color becomes dominant before the nominal edge.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.56f to Color.Transparent,
                                0.62f to Color.Transparent,
                                0.70f to themeColors.darkBackground.copy(alpha = 0.16f),
                                0.78f to themeColors.darkBackground.copy(alpha = 0.42f),
                                0.86f to themeColors.darkBackground.copy(alpha = 0.70f),
                                0.93f to themeColors.darkBackground.copy(alpha = 0.90f),
                                1.00f to themeColors.darkBackground
                            )
                        )
                    )
            )
        }

'''
    text = text[:start] + new_art + text[end:]
else:
    raise SystemExit('Artwork section not found; refusing to patch unknown layout.')

# Put the single existing waveform/progress line about 10% inside the artwork in expanded state.
# Waveform/timestamps still fade away; this does NOT add another progress bar.
text = text.replace(
    'expArtY + (expArtHeight * 0.90f) - 48.dp,',
    'expArtY + expArtHeight - 34.dp,',
    1
)

# Keep the compact queue surface transparent too, so the same artwork-derived page atmosphere
# remains visible behind the queue content.
text = text.replace(
    '.background(Color.Transparent)\n                .pointerInput(Unit)',
    '.background(Color.Transparent)\n                .pointerInput(Unit)',
    1
)

# Remove stale zIndex workarounds if they are still present.
text = text.replace('import androidx.compose.ui.zIndex\n', '')
text = text.replace('                .zIndex(10f)\n', '')

path.write_text(text, encoding="utf-8")
print("PlayerSheet v7 artwork blend and control geometry patch applied.")
