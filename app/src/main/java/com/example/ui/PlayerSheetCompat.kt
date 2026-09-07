package com.example.ui

import androidx.compose.runtime.Composable
import com.example.audio.AudioTelemetry
import com.example.model.Track

/** Compatibility overload for existing callers while Sound Catch controls are removed from the UI. */
@Composable
fun PlayerSheet(
    track: Track,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    telemetry: AudioTelemetry,
    isSoundCatchEnabled: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isCachedOffline: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleSoundCatch: () -> Unit,
    onToggleOfflineCache: () -> Unit,
    onOpenInspector: () -> Unit,
    onDismiss: () -> Unit
) {
    PlayerSheet(
        track = track,
        isPlaying = isPlaying,
        playbackPositionMs = playbackPositionMs,
        telemetry = telemetry,
        isShuffle = isShuffle,
        isRepeat = isRepeat,
        isCachedOffline = isCachedOffline,
        onTogglePlayPause = onTogglePlayPause,
        onSeekTo = onSeekTo,
        onSkipNext = onSkipNext,
        onSkipPrevious = onSkipPrevious,
        onToggleShuffle = onToggleShuffle,
        onToggleRepeat = onToggleRepeat,
        onDismiss = onDismiss
    )
}
