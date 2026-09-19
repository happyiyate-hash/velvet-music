package com.example.recognition

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Server-side recognition contract.
 * Provider credentials remain entirely on the Vercel backend.
 * Android makes one request only to the Vercel backend.
 *
 * The backend returns ONE canonical song object. Android does not select
 * or merge provider payloads.
 */
interface SongRecognitionApi {
    @Multipart
    @POST("v1/recognition/batch")
    suspend fun recognizeBatch(
        @Part audio: MultipartBody.Part
    ): BatchRecognitionResponse
}

data class BatchRecognitionResponse(
    val success: Boolean = false,
    val requestId: String? = null,
    val song: RecognizedSongDto? = null,
    val error: String? = null,
    val trace: List<String>? = null
)

data class RecognizedSongDto(
    val id: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUrl: String? = null,
    val artworkSource: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val confidence: Int? = null,
    val provider: String? = null,
    val platforms: PlatformLinksDto? = null
)

data class PlatformLinksDto(
    val spotifyUrl: String? = null,
    val appleMusicUrl: String? = null,
    val youtubeMusicUrl: String? = null,
    val audiomackUrl: String? = null
)
