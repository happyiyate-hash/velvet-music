package com.example.recognition

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class RecognizedSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val confidence: Int,
    val isrc: String?,
    val spotifyUrl: String?,
    val appleMusicUrl: String?,
    val youtubeMusicUrl: String?,
    val audiomackUrl: String?
)

sealed class RecognitionResult {
    data class Match(val song: RecognizedSong) : RecognitionResult()
    data class NoMatch(val reason: String) : RecognitionResult()
    data class Error(val reason: String) : RecognitionResult()
}

interface SongRecognitionRepository {
    suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult
    suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult
}

/**
 * Remote recognition client. Provider credentials are never stored in the APK.
 * The Velvet server can route these requests to AudD, ACRCloud, ShazamKit, etc.
 */
class RemoteSongRecognitionRepository(
    private val api: SongRecognitionApi? = createApiOrNull()
) : SongRecognitionRepository {

    override suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult =
        recognize(wavAudio) { part -> api!!.recognizeHum(part) }

    override suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult =
        recognize(wavAudio) { part -> api!!.recognizeAudio(part) }

    private suspend fun recognize(
        wavAudio: ByteArray,
        call: suspend (MultipartBody.Part) -> RecognitionResponse
    ): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudio.isEmpty()) {
            return@withContext RecognitionResult.Error("No audio was captured.")
        }
        if (api == null) {
            return@withContext RecognitionResult.Error(
                "Velvet recognition is not configured yet. Set VELVET_RECOGNITION_BASE_URL."
            )
        }

        try {
            val body = wavAudio.toRequestBody("audio/wav".toMediaType())
            val part = MultipartBody.Part.createFormData("audio", "velvet-capture.wav", body)
            val response = call(part)

            if (!response.success || response.song == null) {
                return@withContext RecognitionResult.NoMatch(
                    response.error?.takeIf { it.isNotBlank() }
                        ?: "No confident song match was found."
                )
            }

            val dto = response.song
            val title = dto.title?.trim().orEmpty()
            val artist = dto.artist?.trim().orEmpty()
            if (title.isBlank() || artist.isBlank()) {
                return@withContext RecognitionResult.NoMatch(
                    "The recognition service returned incomplete song metadata."
                )
            }

            RecognitionResult.Match(
                RecognizedSong(
                    id = dto.id?.takeIf { it.isNotBlank() } ?: "$artist:$title",
                    title = title,
                    artist = artist,
                    album = dto.album?.trim().orEmpty().ifBlank { "Unknown Album" },
                    artworkUrl = dto.artworkUrl?.trim()?.takeIf { it.isNotBlank() },
                    durationMs = dto.durationMs?.coerceAtLeast(0L) ?: 0L,
                    confidence = response.confidence.coerceIn(0, 100),
                    isrc = dto.isrc?.trim()?.takeIf { it.isNotBlank() },
                    spotifyUrl = dto.spotifyUrl?.trim()?.takeIf { it.isNotBlank() },
                    appleMusicUrl = dto.appleMusicUrl?.trim()?.takeIf { it.isNotBlank() },
                    youtubeMusicUrl = dto.youtubeMusicUrl?.trim()?.takeIf { it.isNotBlank() },
                    audiomackUrl = dto.audiomackUrl?.trim()?.takeIf { it.isNotBlank() }
                )
            )
        } catch (e: Exception) {
            RecognitionResult.Error(
                e.message?.takeIf { it.isNotBlank() } ?: "Recognition service is unavailable."
            )
        }
    }

    companion object {
        private fun createApiOrNull(): SongRecognitionApi? {
            val configuredBaseUrl = BuildConfig.VELVET_RECOGNITION_BASE_URL.trim()
            if (configuredBaseUrl.isBlank()) return null

            val baseUrl = if (configuredBaseUrl.endsWith('/')) configuredBaseUrl else "$configuredBaseUrl/"
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SongRecognitionApi::class.java)
        }
    }
}
