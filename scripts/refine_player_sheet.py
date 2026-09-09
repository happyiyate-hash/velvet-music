from pathlib import Path
import re

path = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = path.read_text()

# This script is intentionally marker-based so it changes only the transition/layout region.
geometry = r'''        // Stage geometry: the first snap is a true edge-to-edge artwork takeover.
        // 0f = normal player, 1f = expanded artwork, 2f = compact player + queue.
        val topBarY = insetsTop + 4.dp
        val topBarHeight = 44.dp
        val baseUpNextY = totalHeight - insetsBottom - 46.dp
        val availableHeight = (baseUpNextY - (topBarY + topBarHeight)).coerceAtLeast(400.dp)
        val baseArtSize = (totalWidth - 48.dp).coerceAtMost(availableHeight * 0.44f)
        val baseArtX = (totalWidth - baseArtSize) / 2
        val baseArtY = topBarY + topBarHeight + 18.dp
        val baseArtCorner = 32.dp
        val baseTitleX = 24.dp
        val baseTitleY = baseArtY + baseArtSize + 16.dp
        val baseWaveformY = baseTitleY + 66.dp + 14.dp
        val baseControlsY = baseWaveformY + 52.dp + 18.dp
        val baseShuffleY = baseControlsY + 76.dp + 14.dp

        // First snap: artwork reaches the very top, becomes full-width and square-edged.
        val expArtY = 0.dp
        val expArtWidth = totalWidth
        val expArtHeight = (totalHeight * 0.56f).coerceIn(320.dp, 470.dp)
        val expArtX = 0.dp
        val expArtCorner = 0.dp
        val expTitleX = 20.dp
        val expTitleY = expArtY + expArtHeight - 78.dp
        val expTitleWidth = totalWidth - 40.dp
        val expProgressY = expArtY + expArtHeight - 3.dp
        val expControlsY = expArtY + expArtHeight + 24.dp
        val expUpNextY = expControlsY + 86.dp

        val compArtSize = 44.dp
        val compArtX = 16.dp
        val compArtY = insetsTop + 10.dp
        val compArtCorner = 8.dp
        val compTitleX = compArtX + compArtSize + 12.dp
        val compTitleY = compArtY + 2.dp
        val compUpNextY = compArtY + compArtSize + 14.dp

        val artWidth: androidx.compose.ui.unit.Dp
        val artHeight: androidx.compose.ui.unit.Dp
        val artX: androidx.compose.ui.unit.Dp
        val artY: androidx.compose.ui.unit.Dp
        val artCorner: androidx.compose.ui.unit.Dp
        if (p <= 1f) {
            val t = p.coerceIn(0f, 1f)
            artWidth = lerp(baseArtSize, expArtWidth, t)
            artHeight = lerp(baseArtSize, expArtHeight, t)
            artX = lerp(baseArtX, expArtX, t)
            artY = lerp(baseArtY, expArtY, t)
            artCorner = lerp(baseArtCorner, expArtCorner, t)
        } else {
            val t = (p - 1f).coerceIn(0f, 1f)
            artWidth = lerp(expArtWidth, compArtSize, t)
            artHeight = lerp(expArtHeight, compArtSize, t)
            artX = lerp(expArtX, compArtX, t)
            artY = lerp(expArtY, compArtY, t)
            artCorner = lerp(expArtCorner, compArtCorner, t)
        }

        val titleX: androidx.compose.ui.unit.Dp
        val titleY: androidx.compose.ui.unit.Dp
        val titleWidth: androidx.compose.ui.unit.Dp
        val titleSizeSp: Float
        val artistSizeSp: Float
        if (p <= 1f) {
            val t = p.coerceIn(0f, 1f)
            titleX = lerp(baseTitleX, expTitleX, t)
            titleY = lerp(baseTitleY, expTitleY, t)
            titleWidth = lerp(totalWidth - (baseTitleX * 2), expTitleWidth, t)
            titleSizeSp = 21f - (2f * t)
            artistSizeSp = 15f - (1f * t)
        } else {
            val t = (p - 1f).coerceIn(0f, 1f)
            titleX = lerp(expTitleX, compTitleX, t)
            titleY = lerp(expTitleY, compTitleY, t)
            titleWidth = lerp(expTitleWidth, totalWidth - compTitleX - 92.dp, t)
            titleSizeSp = 19f - (5f * t)
            artistSizeSp = 14f - (1f * t)
        }

        val upNextY: androidx.compose.ui.unit.Dp = if (p <= 1f) {
            lerp(baseUpNextY, expUpNextY, p.coerceIn(0f, 1f))
        } else {
            lerp(expUpNextY, compUpNextY, (p - 1f).coerceIn(0f, 1f))
        }

        val topBarAlpha = (1f - (p * 2.2f)).coerceIn(0f, 1f)
        val sourceAlpha = (1f - (p * 2f)).coerceIn(0f, 1f)
        val waveformAlpha = (1f - (p / 0.55f)).coerceIn(0f, 1f)
        val controlsAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)
        val shuffleAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)
        val stage1ControlsAlpha = ((p - 0.52f) / 0.36f).coerceIn(0f, 1f)
        val compactControlsAlpha = ((p - 1.05f) / 0.65f).coerceIn(0f, 1f)

        val stage1DistancePx = with(density) { (baseUpNextY - expUpNextY).toPx().coerceAtLeast(300f) }
        val stage2DistancePx = with(density) { (expUpNextY - compUpNextY).toPx().coerceAtLeast(220f) }

        val onDragDelta: (Float) -> Unit = { dragAmountPx ->
            coroutineScope.launch {
                dragProgress.stop()
                val currentP = dragProgress.value
                val delta = if (currentP < 1f) -dragAmountPx / stage1DistancePx else -dragAmountPx / stage2DistancePx
                dragProgress.snapTo((currentP + delta).coerceIn(0f, 2f))
            }
        }

        val onDragFinish: () -> Unit = {
            coroutineScope.launch {
                val currentP = dragProgress.value
                val target = when {
                    currentP < 0.35f -> 0f
                    currentP < 1.55f -> 1f
                    else -> 2f
                }
                dragProgress.animateTo(target, tween(300, easing = FastOutSlowInEasing))
            }
        }

'''
s, n = re.subn(r'        // 1\. EXACT BOUNDS CALCULATIONS FOR THE 3 STATES:.*?        // Dynamic background atmosphere:', geometry + '        // Dynamic background atmosphere:', s, count=1, flags=re.S)
assert n == 1

artwork = r'''        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.
        // Phase 1 is full-bleed and square-edged; only the bottom is blended into the surface.
        Box(
            modifier = Modifier
                .offset(x = artX, y = artY)
                .width(artWidth)
                .height(artHeight)
                .clip(RoundedCornerShape(artCorner))
                .border(if (p < 0.98f) 1.dp else 0.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(artCorner))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        coroutineScope.launch {
                            dragProgress.animateTo(if (p > 1.2f) 1f else if (p > 0.2f) 0f else 1f, tween(320, easing = FastOutSlowInEasing))
                        }
                    }
                )
                .then(
                    if (p < 1.65f) Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { onDragFinish() },
                            onDragCancel = { onDragFinish() },
                            onVerticalDrag = { change, dragAmount -> change.consume(); onDragDelta(dragAmount) }
                        )
                    } else Modifier
                )
        ) {
            TrackArtworkImage(track = track, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.72f to themeColors.darkBackground.copy(alpha = 0.10f),
                            0.88f to themeColors.darkBackground.copy(alpha = 0.62f),
                            1.00f to themeColors.darkBackground.copy(alpha = 0.98f)
                        )
                    )
                )
            )
        }

'''
s, n = re.subn(r'        // 2\. SINGLE PHYSICAL ARTWORK INSTANCE.*?        // 3\. SONG TITLE & ARTIST', artwork + '        // 3. SONG TITLE & ARTIST', s, count=1, flags=re.S)
assert n == 1

title = r'''        // 3. SONG TITLE & ARTIST. The first phase keeps the long title directly over the artwork.
        Column(
            modifier = Modifier
                .offset(x = titleX, y = titleY)
                .width(titleWidth)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (p > 1.2f) coroutineScope.launch { dragProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) } }
                )
        ) {
            Text(track.title, fontSize = titleSizeSp.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(track.artist, fontSize = artistSizeSp.sp, color = Color.White.copy(alpha = 0.76f), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            if (sourceAlpha > 0f) {
                Spacer(modifier = Modifier.height(2.dp))
                val src = if (track.catalogSource.contains("Device", ignoreCase = true) || track.contentUri != null) "Device Audio" else if (track.catalogSource.isNotBlank()) track.catalogSource else "Device Audio"
                Text(src, fontSize = 13.sp, color = Color.White.copy(alpha = 0.45f * sourceAlpha), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }

'''
s, n = re.subn(r'        // 3\. SONG TITLE & ARTIST.*?        // Sleek progress bar in Stage 1 below the title:', title + '        // Sleek progress bar in Stage 1 below the title:', s, count=1, flags=re.S)
assert n == 1

s = s.replace('.offset(x = 20.dp, y = expProgressY)\n                    .width(totalWidth - 40.dp)', '.offset(x = 0.dp, y = expProgressY)\n                    .width(totalWidth)')

stage1 = r'''
        // 7A. FIRST-PHASE CONTROL RAIL: shuffle / previous / play / next / repeat on one line.
        if (stage1ControlsAlpha > 0f) {
            Row(
                modifier = Modifier.offset(x = 10.dp, y = expControlsY).width(totalWidth - 20.dp).graphicsLayer { alpha = stage1ControlsAlpha },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedShuffleIcon(isShuffle = isShuffle, activeColor = themeColors.accent, onClick = onToggleShuffle, touchSize = 48.dp, iconSize = 24.dp)
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, "Previous Track", tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(36.dp)) }
                Box(
                    modifier = Modifier.size(64.dp).shadow(10.dp, CircleShape, spotColor = themeColors.accent.copy(alpha = 0.34f)).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(themeColors.playPauseGradTop, themeColors.playPauseGradBottom)))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(32.dp)) }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, "Next Track", tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(36.dp)) }
                AnimatedRepeatIcon(isRepeat = isRepeat, activeColor = themeColors.accent, onClick = onToggleRepeat, touchSize = 48.dp, iconSize = 24.dp)
            }
        }

'''
s = s.replace('        // 8. UP NEXT HANDLE & CONTENT', stage1 + '        // 8. UP NEXT HANDLE & CONTENT', 1)

path.write_text(s)
print('PlayerSheet transition patch applied')
