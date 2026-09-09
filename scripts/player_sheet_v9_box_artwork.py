from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# Normal state: the artwork is always a real rectangular image box based on its actual
# aspect ratio, never a forced square. Keep a modest corner radius.
old = '''        val baseArtSize = (totalWidth - 48.dp).coerceAtMost(availableHeight * 0.44f)
        val baseArtX = (totalWidth - baseArtSize) / 2
        val baseArtY = topBarY + topBarHeight + 18.dp
        val baseArtCorner = 24.dp'''
new = '''        val baseArtWidth = (totalWidth - 48.dp).coerceAtLeast(180.dp)
        val naturalBaseArtHeight = baseArtWidth / artworkAspectRatio
        val baseArtHeight = naturalBaseArtHeight.coerceAtMost(availableHeight * 0.52f)
        val baseArtX = (totalWidth - baseArtWidth) / 2
        val baseArtY = topBarY + topBarHeight + 18.dp
        val baseArtCorner = 14.dp'''
if old not in s:
    raise SystemExit('base artwork geometry not found')
s = s.replace(old, new, 1)
s = s.replace('val baseTitleY = baseArtY + baseArtSize + 16.dp', 'val baseTitleY = baseArtY + baseArtHeight + 16.dp', 1)

old = '''        val naturalExpandedHeight = if (isTallArtwork) {
            (totalWidth / artworkAspectRatio).coerceAtLeast(baseArtSize)
        } else {
            baseArtSize
        }
        val expArtHeight = naturalExpandedHeight.coerceAtMost(totalHeight * 0.78f)'''
new = '''        val naturalExpandedHeight = totalWidth / artworkAspectRatio
        val expArtHeight = naturalExpandedHeight.coerceAtMost(totalHeight * 0.78f)'''
if old not in s:
    raise SystemExit('expanded artwork geometry not found')
s = s.replace(old, new, 1)

old = '''            artWidth = lerp(baseArtSize, expArtWidth, t)
            artHeight = lerp(baseArtSize, expArtHeight, t)'''
new = '''            artWidth = lerp(baseArtWidth, expArtWidth, t)
            artHeight = lerp(baseArtHeight, expArtHeight, t)'''
if old not in s:
    raise SystemExit('artwork interpolation not found')
s = s.replace(old, new, 1)

# Move the fade outside the moving artwork container so it stays anchored to the expanded
# screen position while the image itself moves during collapse.
start = s.find('        // 2. SINGLE PHYSICAL ARTWORK INSTANCE.')
if start < 0:
    raise SystemExit('artwork section not found')
fade_marker = s.find('        // 3. SONG TITLE & ARTIST.', start)
if fade_marker < 0:
    raise SystemExit('song title marker not found')
section = s[start:fade_marker]
old_fade_start = section.find('        // Fixed expanded-state dissolve.')
if old_fade_start < 0:
    raise SystemExit('old fade block not found')
art_section = section[:old_fade_start]
art_section = art_section.replace('        val artFadeDepth = 0.dp\n', '')
new_fade = '''        // Fixed expanded-state dissolve. It is deliberately OUTSIDE the moving artwork
        // box, so during collapse the picture leaves this area while the fade remains stable.
        // Resting/mini states have no fade. In expanded mode, the final ~24% dissolves
        // strongly into the player background without changing image height.
        val artworkFadeAlpha = when {
            p < 0.30f -> 0f
            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)
            else -> 1f
        }
        if (artworkFadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.76f))
                    .width(expArtWidth)
                    .height(expArtHeight * 0.24f)
                    .graphicsLayer { alpha = artworkFadeAlpha }
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.16f to playerBackgroundTop.copy(alpha = 0.12f),
                                0.34f to playerBackgroundTop.copy(alpha = 0.34f),
                                0.52f to playerBackgroundBottom.copy(alpha = 0.62f),
                                0.72f to playerBackgroundBottom.copy(alpha = 0.86f),
                                1.00f to playerBackgroundBottom
                            )
                        )
                    )
                    .zIndex(1f)
            )
        }

'''
s = s[:start] + art_section + new_fade + s[fade_marker:]

old = '''                modifier = Modifier
                    .offset(x = 16.dp, y = progressHostY)
                    .width(totalWidth - 32.dp)'''
new = '''                modifier = Modifier
                    .offset(x = 16.dp, y = progressHostY)
                    .width(totalWidth - 32.dp)
                    .zIndex(2f)'''
if old not in s:
    raise SystemExit('progress modifier not found')
s = s.replace(old, new, 1)

old = '''            red = (extractedBg.red * 1.30f + 0.035f).coerceAtMost(1f),
            green = (extractedBg.green * 1.30f + 0.035f).coerceAtMost(1f),
            blue = (extractedBg.blue * 1.30f + 0.035f).coerceAtMost(1f),'''
new = '''            red = (extractedBg.red + (1f - extractedBg.red) * 0.12f).coerceAtMost(1f),
            green = (extractedBg.green + (1f - extractedBg.green) * 0.12f).coerceAtMost(1f),
            blue = (extractedBg.blue + (1f - extractedBg.blue) * 0.12f).coerceAtMost(1f),'''
if old not in s:
    raise SystemExit('background lift block not found')
s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
print('PlayerSheet v9 rectangular artwork/fade layout applied.')
