package com.example.recognition

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Velvet's server-side recognition contract.
 *
 * The Android app sends captured audio to Velvet's proxy. Provider credentials stay
 * on the server, where the proxy can call AudD/ACRCloud/ShazamKit-backed services
 * without ever shipping a secret inside the APK.
 */
interface SongRecognitionApi {
    @Multipart
    @POST("v1/recognition/hum")
    suspend fun recognizeHum(
        @Part audio: MultipartBody.Part
    ): RecognitionResponse

    @Multipart
    @POST("v1/recognition/audio")
    suspend fun recognizeAudio(
        @Part audio: MultipartBody.Part
    ): RecognitionResponse
}

data class RecognitionResponse(
    val success: Boolean = false,
    val confidence: Int = 0,
    val song: RecognizedSongDto? = null,
    val error: String? = null
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
    val audiomackUrl: String? = null
)
