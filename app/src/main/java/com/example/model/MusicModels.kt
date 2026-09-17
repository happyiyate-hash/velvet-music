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
    val artworkUri: String? = null,
    val isLossless: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "3:30"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

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

    val starterTracks = listOf(
        Track(
            id = "starter_after_hours",
            title = "After Hours",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 361000L,
            coverResId = R.drawable.art_after_hours,
            dominantColor = Color(0xFF880E2F),
            secondaryColor = Color(0xFF2E040E),
            catalogSource = "Velvet Music",
            playCount = 142
        ),
        Track(
            id = "starter_good_news",
            title = "Good News",
            artist = "Mac Miller",
            album = "Circles",
            durationMs = 342000L,
            coverResId = R.drawable.art_good_news,
            dominantColor = Color(0xFF636670),
            secondaryColor = Color(0xFF16171B),
            catalogSource = "Velvet Music",
            playCount = 98
        ),
        Track(
            id = "starter_blinding_lights",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 200000L,
            coverResId = R.drawable.art_blinding_lights,
            dominantColor = Color(0xFFAD1457),
            secondaryColor = Color(0xFF330514),
            catalogSource = "Velvet Music",
            playCount = 85
        ),
        Track(
            id = "starter_chairman",
            title = "Chairman",
            artist = "Premium Emf",
            album = "Afro Sessions",
            durationMs = 195000L,
            coverResId = R.drawable.art_after_hours,
            dominantColor = Color(0xFF7A1B28),
            secondaryColor = Color(0xFF26080D),
            catalogSource = "Velvet Music",
            playCount = 64
        ),
        Track(
            id = "starter_heaven_baby",
            title = "Heaven Baby (feat. ZAYN)",
            artist = "Ayra Starr",
            album = "The Year I Turned 21",
            durationMs = 210000L,
            coverResId = R.drawable.art_night_grooves,
            dominantColor = Color(0xFF4A148C),
            secondaryColor = Color(0xFF1E0738),
            catalogSource = "Velvet Music",
            playCount = 52
        ),
        Track(
            id = "starter_luminous_echoes",
            title = "Luminous Echoes",
            artist = "Tame Impala",
            album = "Currents Deluxe",
            durationMs = 245000L,
            coverResId = R.drawable.art_luminous_echoes,
            dominantColor = Color(0xFF7B1FA2),
            secondaryColor = Color(0xFF240A30),
            catalogSource = "Velvet Music",
            playCount = 47
        ),
        Track(
            id = "starter_sunset_beats",
            title = "Sunset Beats",
            artist = "Kaytranada",
            album = "BUBBA",
            durationMs = 188000L,
            coverResId = R.drawable.art_sunset_beats,
            dominantColor = Color(0xFFC2185B),
            secondaryColor = Color(0xFF3C081B),
            catalogSource = "Velvet Music",
            playCount = 39
        )
    )

    val allMixes = listOf(
        Mix(
            id = "mix_luminous",
            title = "Luminous Echoes",
            curator = "Curated by Tame Impala",
            coverResId = R.drawable.art_luminous_echoes,
            description = "Dreamy psych-pop and ambient synth soundscapes",
            dominantColor = Color(0xFF7B1FA2),
            secondaryColor = Color(0xFF240A30),
            tracks = starterTracks.take(4)
        ),
        Mix(
            id = "mix_acoustic",
            title = "Acoustic Waves",
            curator = "Curated by Bon Iver",
            coverResId = R.drawable.art_acoustic_waves,
            description = "Organic folk, delicate guitars, and warm indie atmospheres",
            dominantColor = Color(0xFF37474F),
            secondaryColor = Color(0xFF151C20),
            tracks = starterTracks.takeLast(4)
        ),
        Mix(
            id = "mix_sunset",
            title = "Sunset Beats",
            curator = "Curated by Kaytranada",
            coverResId = R.drawable.art_sunset_beats,
            description = "Groovy neo-soul, deep house rhythms, and warm dusk baselines",
            dominantColor = Color(0xFFC2185B),
            secondaryColor = Color(0xFF3C081B),
            tracks = starterTracks
        ),
        Mix(
            id = "mix_night",
            title = "Night Grooves",
            curator = "Curated by Khruangbin",
            coverResId = R.drawable.art_night_grooves,
            description = "Late night funk, psychedelic dub, and atmospheric slow jams",
            dominantColor = Color(0xFF4A148C),
            secondaryColor = Color(0xFF1E0738),
            tracks = starterTracks.reversed()
        ),
        Mix(
            id = "mix_azure",
            title = "Azure Drift",
            curator = "Curated by Tycho",
            coverResId = R.drawable.art_azure_drift,
            description = "Downtempo electronic waves and ethereal twilight textures",
            dominantColor = Color(0xFF0D47A1),
            secondaryColor = Color(0xFF041838),
            tracks = starterTracks
        )
    )

    val recentlyPlayedTracks = starterTracks.take(5)
    val newReleases = starterTracks.takeLast(4)
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
