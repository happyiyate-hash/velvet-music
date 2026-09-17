package com.example

import com.example.recognition.BatchRecognitionResponse
import com.example.recognition.BatchRecognitionResults
import com.example.recognition.RecognitionResponse
import com.example.recognition.RecognitionResult
import com.example.recognition.RecognizedSongDto
import com.example.recognition.RemoteSongRecognitionRepository
import com.example.recognition.SongRecognitionApi
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RemoteSongRecognitionRepositoryTest {

    private fun createFakeApi(response: BatchRecognitionResponse?, shouldThrow: Boolean = false): SongRecognitionApi {
        return object : SongRecognitionApi {
            override suspend fun recognizeBatch(audio: MultipartBody.Part): BatchRecognitionResponse {
                if (shouldThrow) throw IOException("Simulated network failure")
                return response ?: throw IllegalStateException("No response configured")
            }
        }
    }

    @Test
    fun recognizeBatch_dualMatch_mergesMetadataCorrectly() = runBlocking {
        val auddSong = RecognizedSongDto(
            title = "Lurmen",
            artist = "Trendmix",
            album = "Trendmix Hits",
            artworkUrl = "https://cdn.example.com/artwork.jpg",
            durationMs = 0L,
            isrc = "USRC17607839",
            spotifyUrl = "https://open.spotify.com/track/12345",
            appleMusicUrl = null,
            youtubeMusicUrl = null,
            audiomackUrl = null
        )
        val acrSong = RecognizedSongDto(
            title = "Lurmen",
            artist = "Trendmix",
            album = null,
            artworkUrl = null,
            durationMs = 214000L,
            isrc = "USRC17607839",
            spotifyUrl = null,
            appleMusicUrl = "https://music.apple.com/song/987",
            youtubeMusicUrl = "https://music.youtube.com/watch?v=abc",
            audiomackUrl = null
        )

        val batchResponse = BatchRecognitionResponse(
            success = true,
            requestId = "req-test-123",
            results = BatchRecognitionResults(
                audd = RecognitionResponse(
                    status = "matched",
                    success = true,
                    confidence = 88,
                    song = auddSong
                ),
                acrcloud = RecognitionResponse(
                    status = "matched",
                    success = true,
                    confidence = 94,
                    song = acrSong
                )
            )
        )

        val repository = RemoteSongRecognitionRepository(createFakeApi(batchResponse))
        val sampleWav = ByteArray(500) { 1 }
        val result = repository.recognizeBatch(sampleWav)

        assertTrue(result is RecognitionResult.Match)
        val song = (result as RecognitionResult.Match).song
        assertEquals("Lurmen", song.title)
        assertEquals("Trendmix", song.artist)
        assertEquals("Trendmix Hits", song.album)
        assertEquals("https://cdn.example.com/artwork.jpg", song.artworkUrl)
        assertEquals(214000L, song.durationMs)
        assertEquals("USRC17607839", song.isrc)
        assertEquals("https://open.spotify.com/track/12345", song.spotifyUrl)
        assertEquals("https://music.apple.com/song/987", song.appleMusicUrl)
        assertEquals("https://music.youtube.com/watch?v=abc", song.youtubeMusicUrl)
        assertEquals(94, song.confidence)
    }

    @Test
    fun recognizeBatch_onlyAudDMatches_returnsAudDResult() = runBlocking {
        val auddSong = RecognizedSongDto(
            title = "Lurmen",
            artist = "Trendmix",
            album = "Trendmix Hits",
            artworkUrl = "https://cdn.example.com/art.jpg",
            durationMs = 180000L,
            isrc = "USRC17607839",
            spotifyUrl = "https://open.spotify.com/track/12345"
        )

        val batchResponse = BatchRecognitionResponse(
            success = true,
            results = BatchRecognitionResults(
                audd = RecognitionResponse(
                    status = "matched",
                    success = true,
                    confidence = 92,
                    song = auddSong
                ),
                acrcloud = RecognitionResponse(
                    status = "no_match",
                    success = false,
                    confidence = 0
                )
            )
        )

        val repository = RemoteSongRecognitionRepository(createFakeApi(batchResponse))
        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.Match)
        val song = (result as RecognitionResult.Match).song
        assertEquals("Lurmen", song.title)
        assertEquals("Trendmix", song.artist)
        assertEquals(92, song.confidence)
    }

    @Test
    fun recognizeBatch_bothNoMatch_returnsNoMatchState() = runBlocking {
        val batchResponse = BatchRecognitionResponse(
            success = false,
            results = BatchRecognitionResults(
                audd = RecognitionResponse(
                    status = "no_match",
                    success = false,
                    confidence = 0
                ),
                acrcloud = RecognitionResponse(
                    status = "no_match",
                    success = false,
                    confidence = 0
                )
            )
        )

        val repository = RemoteSongRecognitionRepository(createFakeApi(batchResponse))
        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.NoMatch)
        val noMatch = result as RecognitionResult.NoMatch
        assertEquals("Couldn't identify that song", noMatch.title)
    }

    @Test
    fun recognizeBatch_bothProvidersError_returnsProviderErrorState() = runBlocking {
        val batchResponse = BatchRecognitionResponse(
            success = false,
            results = BatchRecognitionResults(
                audd = RecognitionResponse(
                    status = "error",
                    success = false,
                    error = "AudD rate limit reached"
                ),
                acrcloud = RecognitionResponse(
                    status = "error",
                    success = false,
                    error = "ACRCloud timeout"
                )
            )
        )

        val repository = RemoteSongRecognitionRepository(createFakeApi(batchResponse))
        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ProviderError)
        val error = result as RecognitionResult.ProviderError
        assertEquals("Couldn't search right now", error.title)
    }

    @Test
    fun recognizeBatch_networkException_returnsConnectionErrorState() = runBlocking {
        val repository = RemoteSongRecognitionRepository(createFakeApi(null, shouldThrow = true))
        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ConnectionError)
        val connError = result as RecognitionResult.ConnectionError
        assertEquals("Couldn't connect", connError.title)
    }
}
