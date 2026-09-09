from pathlib import Path

p = Path('app/src/main/java/com/example/ui/PlayerSheet.kt')
s = p.read_text(encoding='utf-8')

# 1) Detect the drawable's intrinsic aspect ratio without blocking the composition.
needle = '''        val density = LocalDensity.current\n\n        // Safe insets'''
replacement = '''        val density = LocalDensity.current\n\n        // Artwork framing is based on the actual drawable dimensions. Square artwork keeps\n        // the normal player geometry; portrait/tall artwork is allowed to use its natural\n        // height instead of being forced into a 1:1 box. Intrinsic dimensions are available\n        // immediately for bundled artwork, so this does not delay the first render.\n        val artworkIntrinsicSize = remember(track.coverResId) {\n            painterResource(track.coverResId).intrinsicSize\n        }\n        val artworkAspectRatio = if (artworkIntrinsicSize.width > 0f && artworkIntrinsicSize.height > 0f) {\n            artworkIntrinsicSize.width / artworkIntrinsicSize.height\n        } else 1f\n        val isTallArtwork = artworkAspectRatio < 0.94f\n\n        // Safe insets'''
if needle not in s:
    raise SystemExit('intrinsic-size insertion point not found')
s = s.replace(needle, replacement, 1)

# 2) Expanded artwork: square stays normal, tall artwork gets its natural proportional height.
old = '''        // First snap: artwork reaches the very top, becomes full-width and square-edged.\n        val expArtY = 0.dp\n        val expArtWidth = totalWidth\n        val expArtHeight = baseArtSize\n        val expArtX = 0.dp\n        val expArtCorner = 0.dp'''
new = '''        // First snap: artwork reaches the very top and becomes truly edge-to-edge.\n        // Do not force portrait artwork into the square height. Its natural aspect ratio\n        // determines the expanded image height, capped to a sensible player region.\n        val expArtY = 0.dp\n        val expArtWidth = totalWidth\n        val naturalExpandedHeight = if (isTallArtwork) {\n            (totalWidth / artworkAspectRatio).coerceAtLeast(baseArtSize)\n        } else {\n            baseArtSize\n        }\n        val expArtHeight = naturalExpandedHeight.coerceAtMost(totalHeight * 0.78f)\n        val expArtX = 0.dp\n        val expArtCorner = 0.dp'''
if old not in s:
    raise SystemExit('expanded geometry block not found')
s = s.replace(old, new, 1)

# 3) Make the physical controls use fixed center-relative geometry. This prevents any
#    parent-width/offset interpolation from introducing asymmetric drift.
old = '''        val playCenterX = centerX\n        val sideButtonCenterSpacing = 96.dp\n\n        val basePlayX = playCenterX - 38.dp\n        val basePrevX = playCenterX - sideButtonCenterSpacing - 30.dp\n        val baseNextX = playCenterX + sideButtonCenterSpacing - 30.dp\n\n        val expandedPlayX = playCenterX - 38.dp\n        val expandedPrevX = playCenterX - sideButtonCenterSpacing - 30.dp\n        val expandedNextX = playCenterX + sideButtonCenterSpacing - 30.dp'''
new = '''        val playCenterX = centerX\n        val sideButtonCenterSpacing = 96.dp\n\n        // These are LEFT offsets for equal-sized 60dp buttons, calculated from the same\n        // center axis. The three primary controls therefore cannot drift horizontally.\n        val playLeft = playCenterX - 38.dp\n        val prevLeft = playCenterX - sideButtonCenterSpacing - 30.dp\n        val nextLeft = playCenterX + sideButtonCenterSpacing - 30.dp\n\n        val basePlayX = playLeft\n        val basePrevX = prevLeft\n        val baseNextX = nextLeft\n        val expandedPlayX = playLeft\n        val expandedPrevX = prevLeft\n        val expandedNextX = nextLeft'''
if old not in s:
    raise SystemExit('control geometry block not found')
s = s.replace(old, new, 1)

# 4) Use fit-height for portrait artwork, fit for square artwork. The image itself remains
#    proportional; the separate dissolve layer handles the bottom bleed.
old = '''                    track = track,\n                    contentDescription = track.title,\n                    contentScale = ContentScale.Fit,\n                    modifier = Modifier.fillMaxSize()'''
new = '''                    track = track,\n                    contentDescription = track.title,\n                    contentScale = if (isTallArtwork) ContentScale.FillHeight else ContentScale.Fit,\n                    modifier = Modifier.fillMaxSize()'''
if old not in s:
    raise SystemExit('artwork contentScale block not found')
s = s.replace(old, new, 1)

# 5) Keep the dissolve stable throughout collapse. It should not disappear just because p
#    crossed an early threshold. It is tied to the artwork's expanded-to-compact travel.
old = '''        val artworkFadeAlpha = when {\n            p < 0.35f -> 0f\n            p < 0.78f -> ((p - 0.35f) / 0.43f).coerceIn(0f, 1f)\n            else -> 1f\n        }'''
new = '''        val artworkFadeAlpha = when {\n            // Clean resting state: no dissolve.\n            p < 0.30f -> 0f\n            // Fade in as the artwork first opens.\n            p < 0.72f -> ((p - 0.30f) / 0.42f).coerceIn(0f, 1f)\n            // Once established, keep the shadow stable through the entire collapse.\n            else -> 1f\n        }'''
if old not in s:
    raise SystemExit('fade alpha block not found')
s = s.replace(old, new, 1)

# 6) Make the dissolve occupy the expanded artwork's lower section without changing the
#    actual image height. It remains a separate overlay at the expanded coordinate.
old = '''                    .offset(x = 0.dp, y = expArtY + (expArtHeight * 0.50f))\n                    .width(expArtWidth)\n                    .height(expArtHeight * 0.50f)'''
new = '''                    .offset(x = expArtX, y = expArtY + (expArtHeight * 0.48f))\n                    .width(expArtWidth)\n                    .height(expArtHeight * 0.52f)'''
if old not in s:
    raise SystemExit('fade geometry block not found')
s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
print('PlayerSheet v8 artwork logic applied.')
