package com.example.ui

import androidx.compose.runtime.Composable
import com.example.model.Track

/**
 * Public entry point kept stable for VelvetApp.
 * The implementation lives in SingToSearchSheetV2.kt so the visual redesign
 * can evolve without changing the existing navigation call site.
 */
@Composable
fun SingToSearchSheet(
    libraryTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    SingToSearchSheetV2(
        libraryTracks = libraryTracks,
        onPlayTrack = onPlayTrack,
        onDismiss = onDismiss
    )
}
