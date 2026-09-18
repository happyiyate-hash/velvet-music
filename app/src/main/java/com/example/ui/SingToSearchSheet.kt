            NativePlatformItem(
                config = config,
                directUrl = directUrl,
                query = query
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070204))
            .testTag("luxury_matched_card")
    ) {
        // Fixed result content. Only the platform list below is independently scrollable.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)
        ) {
            // 1. HERO ARTWORK CONTAINER WITH GLOWING CURVED WAVE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    // Compact hero so the result details and platform list sit higher.
                    .graphicsLayer {
                        alpha = artworkAlpha.value
                        scaleX = artworkScale.value
                        scaleY = artworkScale.value
                    }
            ) {
                // Large edge-to-edge artwork image with loading skeleton
                if (!resolvedArtworkUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedArtworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        },
                        error = {
                            ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                        }
                    )
                } else {
                    ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                }

                // Glowing Organic Curved Wave Transition into Dark Background
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = brushFadeProgress.value
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Path for the curved wave cutting into the background
                    val wavePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.66f)
                        cubicTo(
                            w * 0.28f, h * 0.78f,
                            w * 0.44f, h * 0.93f,
                            w * 0.66f, h * 0.89f
                        )
                        cubicTo(
                            w * 0.80f, h * 0.86f,
                            w * 0.90f, h * 0.78f,
                            w, h * 0.80f
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    // Fill below the wave with the screen background color
                    drawPath(
                        path = wavePath,
                        color = Color(0xFF070204)
                    )

                    // Stroke along the wave edge for the vibrant crimson neon glow
                    val strokePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.66f)
                        cubicTo(
                            w * 0.28f, h * 0.78f,
                            w * 0.44f, h * 0.93f,
                            w * 0.66f, h * 0.89f
                        )
                        cubicTo(
                            w * 0.80f, h * 0.86f,
                            w * 0.90f, h * 0.78f,
                            w, h * 0.80f
                        )
                    }

                    // Layer 1: Soft diffuse ambient glow
                    drawPath(
                        path = strokePath,
                        color = Color(0x35FF1838),
                        style = Stroke(width = 20f, cap = StrokeCap.Round)
                    )
                    // Layer 2: Vivid mid glow
                    drawPath(
                        path = strokePath,
                        color = Color(0x80FF2448),
                        style = Stroke(width = 7f, cap = StrokeCap.Round)
                    )
                    // Layer 3: Crisp high-luminance core edge
                    drawPath(
                        path = strokePath,
                        color = Color(0xFFFF5270),
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }
            }

            // 2. TRACK HEADER: Left Album Thumbnail + Right Track Metadata & Soundwave
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Keep the thumbnail closer to the left edge like the reference.
                    .graphicsLayer {
                        alpha = trackInfoAlpha.value
                        translationY = trackInfoOffsetY.value
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail (Left Side)
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141418))
                        .border(1.dp, Color(0x33FF2448), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!resolvedArtworkUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(resolvedArtworkUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = result.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                            },
                            success = {
                                SubcomposeAsyncImageContent()
                            },
                            error = {
                                ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                            }
                        )
                    } else {
                        ArtworkLoadingSkeleton(modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Track Metadata Column (Right Side)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 5-bar crimson soundwave indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(3.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                        Box(modifier = Modifier.size(3.dp, 8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2448)))
                    }

                    // Prominent Artist Name
                    Text(
                        text = result.artist,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Song Title
                    Text(
                        text = result.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Album / Subtitle Metadata
                    if (result.album.isNotBlank() && !result.album.equals(result.title, ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = result.album,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. ACTION CONTROLS (Play | Copy Title | Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = actionsAlpha.value
                        translationY = actionsOffsetY.value
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button (Brand gradient pill)
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF2448), Color(0xFFD51035))
                            )
                        )
                        .clickable(onClick = onPlayInVelvet)
                        .testTag("sing_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Copy Title Button (Dark glass pill with subtle border)
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141418))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                        .clickable {
                            val textToCopy = "${result.artist} - ${result.title}"
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "Title copied", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("copy_title_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Title",
                            tint = Color(0xFFE5DEE0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Title",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE5DEE0)
                        )
                    }
                }

                // Share Button (Dark glass circular button)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141418))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val shareUrl = result.spotifyUrl ?: result.youtubeMusicUrl ?: result.appleMusicUrl ?: ""
                                putExtra(Intent.EXTRA_SUBJECT, "${result.artist} - ${result.title}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Found this song via Velvet: ${result.artist} - ${result.title}${if (shareUrl.isNotBlank()) "\n$shareUrl" else ""}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                        .testTag("sing_share_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFFE5DEE0),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. "AVAILABLE ON" HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Available on",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Open in your preferred music app",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. ONLY THE PLATFORM LIST SCROLLS.
            // Artwork, track details, actions, and "Available on" remain fixed.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 8.dp)
            ) {
                platforms.forEachIndexed { index, platform ->
                    NativePlatformRow(
                        platform = platform,
                        alpha = platformAlphas[index].value,
                        offsetY = platformOffsetsY[index].value,
                        onClick = { platform.launch(context) }
                    )
                    // No separator lines; every row shares Velvet's black background.
                }
            }
        }
