package com.example.recognition

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Result returned by the real remote recognition service. */
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
 * HTTP implementation used by the Android client.
 *
 * The provider is deliberately hidden behind this repository. A Velvet backend can
 * switch between AudD, ACRCloud, ShazamKit or another provider without changing the UI.
 */
class RemoteSongRecognitionRepository(
    private val api: SongRecognitionApi = createApi()
) : SongRecognitionRepository {

    override suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult =
        recognize { part -> api.recognizeHum(part) }

    override suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult =
        recognize { part -> api.recognizeAudio(part) }

    private suspend fun recognize(
        call: suspend (MultipartBody.Part) -> RecognitionResponse
    ): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudioIsEmpty()) {
            return@withContext RecognitionResult.Error("No audio was captured.")
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
                return@withContext RecognitionResult.NoMatch("The recognition service returned incomplete song metadata.")
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
                    spotifyUrl = dto.spotifyUrl,
                    appleMusicUrl = dto.appleMusicUrl,
                    youtubeMusicUrl = dto.youtubeMusicUrl,
                    audiomackUrl = dto.audiomackUrl
                )
            )
        } catch (e: Exception) {
            RecognitionResult.Error(
                e.message?.takeIf { it.isNotBlank() } ?: "Recognition service is unavailable."
            )
        }
    }

    private fun wavAudioIsEmpty(): Boolean = false

    companion object {
        private fun createApi(): SongRecognitionApi {
            val configuredBaseUrl = BuildConfig.VELVET_RECOGNITION_BASE_URL.trim()
            require(configuredBaseUrl.isNotBlank()) {
                "VELVET_RECOGNITION_BASE_URL is not configured"
            }
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
