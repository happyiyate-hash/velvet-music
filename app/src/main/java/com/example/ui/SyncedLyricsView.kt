package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricLine
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetTextPrimary
import com.example.ui.theme.VelvetTextSecondary
import com.example.ui.theme.VelvetTextTertiary

@Composable
fun SyncedLyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showRawLrc by remember { mutableStateOf(false) }

    // Find current active index
    val activeIndex = remember(lyrics, currentPositionMs) {
        val idx = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx == -1) 0 else idx
    }

    // Auto-scroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0),
                scrollOffset = 0
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("synced_lyrics_view")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Subtitles,
                contentDescription = "Synced Lyrics",
                tint = VelvetBrightCrimson,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Whisper Synced Lyrics (.lrc)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VelvetTextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (showRawLrc) "Styled" else "LRC Timestamps",
                fontSize = 11.sp,
                color = VelvetBrightCrimson,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showRawLrc = !showRawLrc }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("toggle_lrc_mode_button")
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }

            itemsIndexed(lyrics) { index, line ->
                val isActive = index == activeIndex
                val isPast = index < activeIndex

                val textColor by animateColorAsState(
                    targetValue = when {
                        isActive -> Color.White
                        isPast -> VelvetTextTertiary
                        else -> VelvetTextSecondary
                    },
                    animationSpec = tween(durationMillis = 250)
                )

                val fontSize = if (isActive) 22.sp else 16.sp
                val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSeekTo(line.timeMs) }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("lyric_line_$index")
                ) {
                    if (showRawLrc) {
                        val minutes = line.timeMs / 60000
                        val seconds = (line.timeMs % 60000) / 1000
                        val millis = (line.timeMs % 1000) / 10
                        Text(
                            text = String.format("[%02d:%02d.%02d]", minutes, seconds, millis),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isActive) VelvetBrightCrimson else VelvetTextTertiary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = line.text,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = textColor,
                        lineHeight = if (isActive) 30.sp else 24.sp,
                        modifier = Modifier.alpha(if (isActive) 1f else if (isPast) 0.5f else 0.8f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
