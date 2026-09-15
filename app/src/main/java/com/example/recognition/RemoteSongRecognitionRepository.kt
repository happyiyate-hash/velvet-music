package com.example.recognition

import android.util.Log
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
    suspend fun recognizeBatch(wavAudio: ByteArray): RecognitionResult
}

/** Remote recognition client. One app upload is sent to the Worker, which fans out to both providers. */
class RemoteSongRecognitionRepository(
    private val api: SongRecognitionApi? = createApiOrNull()
) : SongRecognitionRepository {

    override suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult =
        recognizeSingle(wavAudio, "HUM") { part -> api!!.recognizeHum(part) }

    override suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult =
        recognizeSingle(wavAudio, "AUDIO") { part -> api!!.recognizeAudio(part) }

    override suspend fun recognizeBatch(wavAudio: ByteArray): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudio.isEmpty()) return@withContext RecognitionResult.Error("No audio was captured.")
        if (api == null) return@withContext RecognitionResult.Error("Velvet recognition is not configured yet. Set VELVET_RECOGNITION_BASE_URL.")

        val startedAt = System.currentTimeMillis()
        Log.d(TAG, "BATCH_UPLOAD_STARTED bytes=${wavAudio.size}")
        try {
            val body = wavAudio.toRequestBody("audio/wav".toMediaType())
            val part = MultipartBody.Part.createFormData("audio", "velvet-capture.wav", body)
            val response = api.recognizeBatch(part)
            Log.d(TAG, "BATCH_RESPONSE_RECEIVED requestId=${response.requestId ?: "none"} auddSuccess=${response.results.audd.success} acrcloudSuccess=${response.results.acrcloud.success} elapsedMs=${System.currentTimeMillis() - startedAt}")

            val candidates = listOf(
                response.results.audd.toCandidate("AudD"),
                response.results.acrcloud.toCandidate("ACRCloud")
            ).filterNotNull()

            if (candidates.isEmpty()) {
                return@withContext if (response.error.isNullOrBlank()) {
                    RecognitionResult.NoMatch(
                        listOfNotNull(
                            response.results.audd.error,
                            response.results.acrcloud.error
                        ).joinToString("; ").ifBlank { "No confident song match was found." }
                    )
                } else {
                    RecognitionResult.Error(response.error)
                }
            }

            val selected = selectDisplayCandidate(candidates)
            Log.d(TAG, "BATCH_DISPLAY_RESULT requestId=${response.requestId ?: "none"} provider=${selected.provider} title=${selected.song.title} artist=${selected.song.artist} confidence=${selected.song.confidence}")
            RecognitionResult.Match(selected.song)
        } catch (e: Exception) {
            Log.e(TAG, "BATCH_RECOGNITION_FAILED elapsedMs=${System.currentTimeMillis() - startedAt} message=${e.message}", e)
            RecognitionResult.Error(e.message?.takeIf { it.isNotBlank() } ?: "Recognition service is unavailable.")
        }
    }

    private data class Candidate(val provider: String, val song: RecognizedSong)

    private fun RecognitionResponse.toCandidate(provider: String): Candidate? {
        if (!success || song == null) return null
        val title = song.title?.trim().orEmpty()
        val artist = song.artist?.trim().orEmpty()
        if (title.isBlank() || artist.isBlank()) return null
        return Candidate(provider, song.toRecognizedSong())
    }

    /** Objective local comparison: shared ISRC first, then title/artist agreement, then confidence and metadata. */
    private fun selectDisplayCandidate(candidates: List<Candidate>): Candidate {
        if (candidates.size == 1) return candidates.first()
        val normalized = candidates.map { it to normalizeKey(it.song.artist, it.song.title) }
        val sharedIsrc = candidates.mapNotNull { it.song.isrc?.trim()?.lowercase()?.takeIf(String::isNotBlank) }.distinct()
        if (sharedIsrc.size == 1) return candidates.maxBy { metadataScore(it.song) * 100 + it.song.confidence }
        if (normalized.map { it.second }.distinct().size == 1) return candidates.maxBy { metadataScore(it.song) * 100 + it.song.confidence }
        return candidates.maxWithOrNull(compareBy<Candidate> { it.song.confidence }.thenBy { metadataScore(it.song) }) ?: candidates.first()
    }

    private fun normalizeKey(artist: String, title: String): String =
        "$artist $title".lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun metadataScore(song: RecognizedSong): Int = listOf(
        song.album.isNotBlank(),
        song.isrc != null,
        song.artworkUrl != null,
        song.spotifyUrl != null,
        song.appleMusicUrl != null,
        song.youtubeMusicUrl != null,
        song.audiomackUrl != null
    ).count { it }

    private fun RecognizedSongDto.toRecognizedSong(confidence: Int = 0): RecognizedSong = RecognizedSong(
        id = id?.takeIf { it.isNotBlank() } ?: "${artist.orEmpty()}:${title.orEmpty()}",
        title = title!!.trim(),
        artist = artist!!.trim(),
        album = album?.trim().orEmpty().ifBlank { "Unknown Album" },
        artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() },
        durationMs = durationMs?.coerceAtLeast(0L) ?: 0L,
        confidence = confidence,
        isrc = isrc?.trim()?.takeIf { it.isNotBlank() },
        spotifyUrl = spotifyUrl?.trim()?.takeIf { it.isNotBlank() },
        appleMusicUrl = appleMusicUrl?.trim()?.takeIf { it.isNotBlank() },
        youtubeMusicUrl = youtubeMusicUrl?.trim()?.takeIf { it.isNotBlank() },
        audiomackUrl = audiomackUrl?.trim()?.takeIf { it.isNotBlank() }
    )

    private fun RecognitionResponse.toCandidate(provider: String, ignored: Int = 0): Candidate? =
        if (!success || song == null || song.title.isNullOrBlank() || song.artist.isNullOrBlank()) null
        else Candidate(provider, song.toRecognizedSong(confidence.coerceIn(0, 100)))

    private suspend fun recognizeSingle(
        wavAudio: ByteArray,
        mode: String,
        call: suspend (MultipartBody.Part) -> RecognitionResponse
    ): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudio.isEmpty()) return@withContext RecognitionResult.Error("No audio was captured.")
        if (api == null) return@withContext RecognitionResult.Error("Velvet recognition is not configured yet. Set VELVET_RECOGNITION_BASE_URL.")
        try {
            val body = wavAudio.toRequestBody("audio/wav".toMediaType())
            val part = MultipartBody.Part.createFormData("audio", "velvet-capture.wav", body)
            val response = call(part)
            if (!response.success || response.song == null) return@withContext RecognitionResult.NoMatch(response.error ?: "No confident song match was found.")
            val dto = response.song
            if (dto.title.isNullOrBlank() || dto.artist.isNullOrBlank()) return@withContext RecognitionResult.NoMatch("The recognition service returned incomplete song metadata.")
            RecognitionResult.Match(dto.toRecognizedSong(response.confidence.coerceIn(0, 100)))
        } catch (e: Exception) {
            Log.e(TAG, "RECOGNITION_FAILED mode=$mode message=${e.message}", e)
            RecognitionResult.Error(e.message ?: "Recognition service is unavailable.")
        }
    }

    companion object {
        private const val TAG = "VelvetRecognition"

        private fun createApiOrNull(): SongRecognitionApi? {
            val configuredBaseUrl = BuildConfig.VELVET_RECOGNITION_BASE_URL.trim()
            if (configuredBaseUrl.isBlank()) return null
            val baseUrl = if (configuredBaseUrl.endsWith('/')) configuredBaseUrl else "$configuredBaseUrl/"
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(SongRecognitionApi::class.java)
        }
    }
}
