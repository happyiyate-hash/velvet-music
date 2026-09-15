package com.example.recognition

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** Server-side recognition contract. Provider credentials stay on the server. */
interface SongRecognitionApi {
    @Multipart
    @POST("v1/recognition/batch")
    suspend fun recognizeBatch(
        @Part audio: MultipartBody.Part
    ): BatchRecognitionResponse

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
    val requestId: String? = null,
    val song: RecognizedSongDto? = null,
    val error: String? = null
)

data class BatchRecognitionResponse(
    val success: Boolean = false,
    val requestId: String? = null,
    val results: BatchRecognitionResults = BatchRecognitionResults(),
    val error: String? = null
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
    val audiomackUrl: String? = null
)
