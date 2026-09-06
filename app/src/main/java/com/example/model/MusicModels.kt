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
    @DrawableRes val coverResId: Int = R.drawable.art_after_hours,
    val dominantColor: Color = VelvetDarkBurgundy,
    val secondaryColor: Color = VelvetDeepCrimson,
    val catalogSource: String = "Jamendo / Creative Commons",
    val lyrics: List<LyricLine> = emptyList(),
    val bpm: Int = 84,
    val playCount: Int = 0,
    val dateAddedMs: Long = System.currentTimeMillis(),
    val contentUri: String? = null
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
    val sampleLyricsAfterHours = listOf(
        LyricLine(0L, "[Instrumental Intro - Ambient Synth]", isHeader = true),
        LyricLine(3500L, "Thought I almost died in my dream again"),
        LyricLine(7800L, "Fightin' for my life, I couldn't breathe again"),
        LyricLine(12400L, "Fallin' into too deep without you"),
        LyricLine(17200L, "Baby, in the darkness I just call your name"),
        LyricLine(22100L, "Where are you now when I need you most?"),
        LyricLine(27000L, "I'd give it all just to hold you close"),
        LyricLine(32300L, "Sorry that I broke your heart, your heart"),
        LyricLine(37500L, "Never coming down from this fever dream"),
        LyricLine(42800L, "City lights turn to crimson red in the haze"),
        LyricLine(48000L, "Midnight echoes drifting through the rain")
    )

    val sampleLyricsGoodNews = listOf(
        LyricLine(0L, "[Acoustic Intro - Gentle Drift]", isHeader = true),
        LyricLine(4200L, "I spent the whole day in my head"),
        LyricLine(8900L, "Do a little spring cleanin'"),
        LyricLine(13500L, "I'm always pushin' off the things I said"),
        LyricLine(18200L, "Good news, good news, good news, that's all they wanna hear"),
        LyricLine(23800L, "No, they don't like it when I'm down"),
        LyricLine(29100L, "A lot of things I had to leave behind"),
        LyricLine(34400L, "There's a whole lot more waiting on the other side")
    )

    val sampleLyricsBlindingLights = listOf(
        LyricLine(0L, "[Fast Snap Transient - Kick & Snare]", isHeader = true),
        LyricLine(3800L, "Yeah, I've been tryna call"),
        LyricLine(7600L, "I've been on my own for long enough"),
        LyricLine(12200L, "Maybe you can show me how to love, maybe"),
        LyricLine(17100L, "I'm going through withdrawals"),
        LyricLine(21800L, "You don't even have to do too much"),
        LyricLine(26400L, "I can turn the lights on with a touch"),
        LyricLine(31000L, "Sin City's cold and empty"),
        LyricLine(35500L, "No one's around to judge me"),
        LyricLine(40200L, "I can't see clearly when you're gone")
    )

    val sampleLyricsLuminousEchoes = listOf(
        LyricLine(0L, "[Sound Catch Fluid Drift Mesh Active]", isHeader = true),
        LyricLine(4000L, "Drifting over burgundy waves in silent reverie"),
        LyricLine(9200L, "Crescent light illuminating the foggy dark sky"),
        LyricLine(15000L, "Feel the frequency shift into charcoal embers"),
        LyricLine(21000L, "A low sub bass reverberates in the stillness"),
        LyricLine(28000L, "Hold the frequency, let the audio flow free")
    )

    val trackAfterHours = Track(
        id = "track_1",
        title = "After Hours - The Weeknd",
        artist = "The Weeknd",
        album = "After Hours",
        durationMs = 210000L,
        coverResId = R.drawable.art_after_hours,
        dominantColor = Color(0xFF880E2F),
        secondaryColor = Color(0xFF26050E),
        catalogSource = "Jamendo Licensed Stream",
        lyrics = sampleLyricsAfterHours,
        bpm = 100
    )

    val trackGoodNews = Track(
        id = "track_2",
        title = "Good News - Mac Miller",
        artist = "Mac Miller",
        album = "Circles",
        durationMs = 185000L,
        coverResId = R.drawable.art_good_news,
        dominantColor = Color(0xFF5E2E3E),
        secondaryColor = Color(0xFF1B0F15),
        catalogSource = "Audius Decentralized Catalog",
        lyrics = sampleLyricsGoodNews,
        bpm = 74
    )

    val trackBlindingLights = Track(
        id = "track_3",
        title = "Blinding Lights - The Weeknd",
        artist = "The Weeknd",
        album = "After Hours",
        durationMs = 200000L,
        coverResId = R.drawable.art_blinding_lights,
        dominantColor = Color(0xFFB31238),
        secondaryColor = Color(0xFF380718),
        catalogSource = "Creative Commons Catalog",
        lyrics = sampleLyricsBlindingLights,
        bpm = 120
    )

    val mixLuminousEchoes = Mix(
        id = "mix_1",
        title = "Luminous Echoes",
        curator = "Curated by Tame Impala",
        coverResId = R.drawable.art_luminous_echoes,
        description = "Psychedelic ambient soundscapes, lush synths, and hypnotic subterranean bass.",
        dominantColor = Color(0xFF880E2F),
        secondaryColor = Color(0xFF14050A),
        tracks = listOf(
            Track(
                id = "le_1",
                title = "Luminous Echoes",
                artist = "Tame Impala Selection",
                album = "Velvet Ambient Sessions",
                durationMs = 240000L,
                coverResId = R.drawable.art_luminous_echoes,
                dominantColor = Color(0xFF880E2F),
                secondaryColor = Color(0xFF1A060E),
                catalogSource = "Archive.org High-Res Ambient",
                lyrics = sampleLyricsLuminousEchoes,
                bpm = 80
            ),
            trackAfterHours,
            trackGoodNews
        )
    )

    val mixAcousticWaves = Mix(
        id = "mix_2",
        title = "Acoustic Waves",
        curator = "Curated by Bon Iver",
        coverResId = R.drawable.art_acoustic_waves,
        description = "Stripped-back organic vibrations, acoustic fingerpicking, and tape warmth.",
        dominantColor = Color(0xFF421528),
        secondaryColor = Color(0xFF160810),
        tracks = listOf(
            Track(
                id = "aw_1",
                title = "Acoustic Waves (Intimate)",
                artist = "Bon Iver Selection",
                album = "Velvet Tape Sessions",
                durationMs = 195000L,
                coverResId = R.drawable.art_acoustic_waves,
                dominantColor = Color(0xFF4A1A2D),
                secondaryColor = Color(0xFF180A12),
                catalogSource = "Free Music Archive Curated",
                lyrics = sampleLyricsGoodNews,
                bpm = 68
            ),
            trackGoodNews
        )
    )

    val mixSunsetBeats = Mix(
        id = "mix_3",
        title = "Sunset Beats",
        curator = "Curated by Kaytranada",
        coverResId = R.drawable.art_sunset_beats,
        description = "Heavy off-grid basslines, dusty bounce percussion, and sunset warmth.",
        dominantColor = Color(0xFF9E1B32),
        secondaryColor = Color(0xFF2A0812),
        tracks = listOf(
            Track(
                id = "sb_1",
                title = "Desert Dunes",
                artist = "Kaytranada Selection",
                album = "Velvet Twilight",
                durationMs = 215000L,
                coverResId = R.drawable.art_sunset_beats,
                dominantColor = Color(0xFF9E1B32),
                secondaryColor = Color(0xFF20050E),
                catalogSource = "Audius Portal",
                lyrics = sampleLyricsBlindingLights,
                bpm = 108
            ),
            trackBlindingLights
        )
    )

    val mixNightGrooves = Mix(
        id = "mix_4",
        title = "Night Grooves",
        curator = "Curated by Khruangbin",
        coverResId = R.drawable.art_night_grooves,
        description = "Reverberant guitars, deep twilight pocket grooves, and global psych melodies.",
        dominantColor = Color(0xFF5A1030),
        secondaryColor = Color(0xFF15040B),
        tracks = listOf(
            Track(
                id = "ng_1",
                title = "Night Grooves (Midnight Cut)",
                artist = "Khruangbin Selection",
                album = "Velvet Late Night",
                durationMs = 230000L,
                coverResId = R.drawable.art_night_grooves,
                dominantColor = Color(0xFF5A1030),
                secondaryColor = Color(0xFF120309),
                catalogSource = "Jamendo Pro Stream",
                lyrics = sampleLyricsAfterHours,
                bpm = 86
            ),
            trackAfterHours
        )
    )

    val trackAzureDrift = Track(
        id = "track_blue",
        title = "Azure Drift (Neon Blue)",
        artist = "Cobalt Echoes",
        album = "Electric Ocean",
        durationMs = 210000L,
        coverResId = R.drawable.art_azure_drift,
        dominantColor = Color(0xFF0D58A6),
        secondaryColor = Color(0xFF051D3A),
        catalogSource = "Velvet Ocean Sessions",
        lyrics = listOf(
            LyricLine(0L, "[Vibrant Blue Waves - Atmospheric Synth]", isHeader = true),
            LyricLine(4000L, "Deep electric ocean rolling under midnight skies"),
            LyricLine(9500L, "Cobalt reflections shining in your eyes"),
            LyricLine(15200L, "Lost in azure currents, drifting into space"),
            LyricLine(21000L, "Feel the sound catch pulse at our own pace")
        ),
        bpm = 90
    )

    val allMixes = listOf(mixLuminousEchoes, mixAcousticWaves, mixNightGrooves, mixSunsetBeats)

    val recentlyPlayedTracks = listOf(trackAfterHours, trackAzureDrift, trackGoodNews, trackBlindingLights)

    val newReleases = listOf(
        trackAzureDrift,
        Track(
            id = "rel_1",
            title = "Midnight Velvet Pulse",
            artist = "Aura Noir",
            album = "Obsidian EP",
            durationMs = 178000L,
            coverResId = R.drawable.art_luminous_echoes,
            dominantColor = Color(0xFF880E2F),
            secondaryColor = Color(0xFF16060E),
            catalogSource = "Indie Portal Verified",
            lyrics = sampleLyricsLuminousEchoes,
            bpm = 92
        ),
        Track(
            id = "rel_2",
            title = "Dusk Resonance",
            artist = "Echo District",
            album = "Analog Dreams",
            durationMs = 205000L,
            coverResId = R.drawable.art_night_grooves,
            dominantColor = Color(0xFF6B1238),
            secondaryColor = Color(0xFF1B050E),
            catalogSource = "Jamendo Creative Commons",
            lyrics = sampleLyricsAfterHours,
            bpm = 78
        )
    )
}
