package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.VelvetBrightCrimson
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetDeepCrimson
import com.example.ui.theme.VelvetMutedPurple
import com.example.ui.theme.VelvetWine

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val isHeader: Boolean = false
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    @DrawableRes val coverResId: Int = R.drawable.art_luminous_echoes,
    val dominantColor: Color = VelvetDarkBurgundy,
    val secondaryColor: Color = VelvetDeepCrimson,
    val catalogSource: String = "Device Audio",
    val lyrics: List<LyricLine> = emptyList(),
    val bpm: Int = 84,
    val playCount: Int = 0,
    val dateAddedMs: Long = System.currentTimeMillis(),
    val contentUri: String? = null,
    val artworkUri: String? = null
)

data class DeviceVideo(
    val id: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long = 0L,
    val resolution: String = "1080p",
    val contentUri: String? = null,
    val dateAddedMs: Long = System.currentTimeMillis()
)

data class Mix(
    val id: String,
    val title: String,
    val curator: String,
    @DrawableRes val coverResId: Int,
    val description: String,
    val dominantColor: Color,
    val secondaryColor: Color,
    val tracks: List<Track>
)

object SampleData {
    val defaultIdleTrack = Track(
        id = "device_music_idle",
        title = "No Track Selected",
        artist = "Select audio from device",
        album = "Device Storage",
        durationMs = 0L,
        coverResId = R.drawable.art_luminous_echoes,
        dominantColor = Color(0xFF1C1A20),
        secondaryColor = Color(0xFF100E14),
        catalogSource = "Device Audio"
    )

    val starterTracks = emptyList<Track>()
    val allMixes = emptyList<Mix>()
    val recentlyPlayedTracks = emptyList<Track>()
    val newReleases = emptyList<Track>()
}

/**
 * Fallback pool using the app's photos to automatically assign distinct photos
 * to device music that does not have an embedded photo.
 */
object FallbackArtworkPool {
    val covers = listOf(
        R.drawable.art_after_hours,
        R.drawable.art_night_grooves,
        R.drawable.art_luminous_echoes,
        R.drawable.art_good_news,
        R.drawable.art_sunset_beats,
        R.drawable.art_blinding_lights,
        R.drawable.art_acoustic_waves
    )

    fun getPhotoForTrack(id: String, title: String = "", artist: String = ""): Int {
        val rawHash = id.hashCode() xor (title.hashCode() * 31) xor (artist.hashCode() * 17)
        val index = kotlin.math.abs(rawHash) % covers.size
        return covers[index]
    }
}
