package com.example.recognition

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Server-side recognition contract.
 * Provider credentials remain entirely on the Vercel backend.
 * Android makes one request only to the Vercel backend.
 */
interface SongRecognitionApi {
    @Multipart
    @POST("v1/recognition/batch")
    suspend fun recognizeBatch(
        @Part audio: MultipartBody.Part
    ): BatchRecognitionResponse

    /** Resolve an already-recognized song into a currently playable remote stream. */
    @POST("v1/playback/resolve")
    suspend fun resolvePlayback(
        @Body request: PlaybackResolveRequest
    ): PlaybackResolveResponse
}

data class RecognitionResponse(
    val success: Boolean = false,
    val status: String? = null,
    val confidence: Int = 0,
    val requestId: String? = null,
    val song: RecognizedSongDto? = null,
    val error: String? = null
)

data class BatchRecognitionResponse(
    val success: Boolean = false,
    val requestId: String? = null,
    val results: BatchRecognitionResults = BatchRecognitionResults(),
    val error: String? = null,
    val trace: List<String>? = null
)

data class BatchRecognitionResults(
    val audd: RecognitionResponse = RecognitionResponse(),
    val acrcloud: RecognitionResponse = RecognitionResponse()
)

data class RecognizedSongDto(
    val id: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val spotifyUrl: String? = null,
    val appleMusicUrl: String? = null,
    val youtubeMusicUrl: String? = null,
    val audiomackUrl: String? = null,
    val soundcloudUrl: String? = null,
    val boomplayUrl: String? = null
)


data class PlaybackResolveRequest(
    val artist: String,
    val title: String,
    val isrc: String? = null,
    val durationMs: Long? = null
)

data class PlaybackResolveResponse(
    val success: Boolean = false,
    val provider: String? = null,
    val streamUrl: String? = null,
    val expiresAt: Long? = null,
    val trackUrl: String? = null,
    val error: String? = null
)
