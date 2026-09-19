package com.example

import com.example.recognition.BatchRecognitionResponse
import com.example.recognition.PlatformLinksDto
import com.example.recognition.RecognitionResult
import com.example.recognition.RecognizedSongDto
import com.example.recognition.RemoteSongRecognitionRepository
import com.example.recognition.SongRecognitionApi
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RemoteSongRecognitionRepositoryTest {

    private fun createFakeApi(
        response: BatchRecognitionResponse?,
        throwException: Throwable? = null
    ): SongRecognitionApi {
        return object : SongRecognitionApi {
            override suspend fun recognizeBatch(audio: MultipartBody.Part): BatchRecognitionResponse {
                if (throwException != null) throw throwException
                return response ?: throw IllegalStateException("No response configured")
            }
        }
    }

    @Test
    fun recognizeBatch_unifiedMatch_readsCanonicalSongAndPlatformLinks() = runBlocking {
        val song = RecognizedSongDto(
            id = "track-123",
            title = "Red Potion",
            artist = "Rema",
            album = "RAVAGE",
            artworkUrl = "https://cdn.example.com/red-potion.jpg",
            artworkSource = "itunes",
            durationMs = 174000L,
            isrc = "NGA3B2314022",
            confidence = 25,
            provider = "acrcloud",
            platforms = PlatformLinksDto(
                spotifyUrl = "https://open.spotify.com/track/123",
                appleMusicUrl = null,
                youtubeMusicUrl = "https://music.youtube.com/watch?v=123",
                audiomackUrl = null
            )
        )

        val repository = RemoteSongRecognitionRepository(
            createFakeApi(
                BatchRecognitionResponse(
                    success = true,
                    requestId = "req-test-123",
                    song = song
                )
            )
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.Match)
        val matched = (result as RecognitionResult.Match).song
        assertEquals("Red Potion", matched.title)
        assertEquals("Rema", matched.artist)
        assertEquals("RAVAGE", matched.album)
        assertEquals("https://cdn.example.com/red-potion.jpg", matched.artworkUrl)
        assertEquals(174000L, matched.durationMs)
        assertEquals("NGA3B2314022", matched.isrc)
        assertEquals(25, matched.confidence)
        assertEquals("https://open.spotify.com/track/123", matched.spotifyUrl)
        assertEquals("https://music.youtube.com/watch?v=123", matched.youtubeMusicUrl)
        assertEquals(null, matched.soundcloudUrl)
    }

    @Test
    fun recognizeBatch_unifiedNoMatch_returnsNoMatchState() = runBlocking {
        val repository = RemoteSongRecognitionRepository(
            createFakeApi(
                BatchRecognitionResponse(
                    success = false,
                    requestId = "req-no-match",
                    song = null
                )
            )
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.NoMatch)
    }

    @Test
    fun recognizeBatch_backendError_returnsProviderErrorState() = runBlocking {
        val repository = RemoteSongRecognitionRepository(
            createFakeApi(
                BatchRecognitionResponse(
                    success = false,
                    requestId = "req-error",
                    song = null,
                    error = "Recognition backend unavailable"
                )
            )
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ProviderError)
        assertEquals("Recognition service error", (result as RecognitionResult.ProviderError).title)
    }

    @Test
    fun recognizeBatch_networkException_returnsConnectionError() = runBlocking {
        val repository = RemoteSongRecognitionRepository(
            createFakeApi(null, throwException = IOException("Simulated network failure"))
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ConnectionError)
        assertEquals("Couldn't connect", (result as RecognitionResult.ConnectionError).title)
    }

    @Test
    fun recognizeBatch_httpException404_returnsProviderErrorState() = runBlocking {
        val errorResponse = Response.error<BatchRecognitionResponse>(
            404,
            "{\"error\":\"Not Found\"}".toResponseBody("application/json".toMediaType())
        )
        val repository = RemoteSongRecognitionRepository(
            createFakeApi(null, throwException = HttpException(errorResponse))
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ProviderError)
        val providerError = result as RecognitionResult.ProviderError
        assertEquals("Server error (404)", providerError.title)
        assertEquals(404, providerError.httpStatus)
    }

    @Test
    fun recognizeBatch_socketTimeout_returnsConnectionError() = runBlocking {
        val repository = RemoteSongRecognitionRepository(
            createFakeApi(null, throwException = SocketTimeoutException("Read timed out"))
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ConnectionError)
        assertTrue((result as RecognitionResult.ConnectionError).isTimeout)
    }

    @Test
    fun recognizeBatch_jsonDataException_returnsParsingError() = runBlocking {
        val repository = RemoteSongRecognitionRepository(
            createFakeApi(null, throwException = JsonDataException("Required field missing"))
        )

        val result = repository.recognizeBatch(ByteArray(500) { 1 })

        assertTrue(result is RecognitionResult.ResponseParsingError)
        assertTrue((result as RecognitionResult.ResponseParsingError).reason.contains("JSON schema mismatch"))
    }
}
