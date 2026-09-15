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
import retrofit2.converter.moshi.MoshiJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class RecognizedSong(
    val id: String, val title: String, val artist: String, val album: String,
    val artworkUrl: String?, val durationMs: Long, val confidence: Int, val isrc: String?,
    val spotifyUrl: String?, val appleMusicUrl: String?, val youtubeMusicUrl: String?, val audiomackUrl: String?
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

/** One app upload goes to the Worker batch endpoint; the Worker fans it out to AudD and ACRCloud. */
class RemoteSongRecognitionRepository(private val api: SongRecognitionApi? = createApiOrNull()) : SongRecognitionRepository {
    override suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult = recognizeSingle(wavAudio, "HUM") { api!!.recognizeHum(it) }
    override suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult = recognizeSingle(wavAudio, "AUDIO") { api!!.recognizeAudio(it) }

    override suspend fun recognizeBatch(wavAudio: ByteArray): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudio.isEmpty()) return@withContext RecognitionResult.Error("No audio was captured.")
        val remote = api ?: return@withContext RecognitionResult.Error("Velvet recognition is not configured yet. Set VELVET_RECOGNITION_BASE_URL.")
        val startedAt = System.currentTimeMillis()
        Log.d(TAG, "BATCH_UPLOAD_STARTED bytes=${wavAudio.size}")
        try {
            val body = wavAudio.toRequestBody("audio/wav".toMediaType())
            val part = MultipartBody.Part.createFormData("audio", "velvet-capture.wav", body)
            val response = remote.recognizeBatch(part)
            Log.d(TAG, "BATCH_RESPONSE_RECEIVED requestId=${response.requestId ?: "none"} auddSuccess=${response.results.audd.success} acrcloudSuccess=${response.results.acrcloud.success} elapsedMs=${System.currentTimeMillis() - startedAt}")

            val candidates = listOfNotNull(
                response.results.audd.toCandidate("AudD"),
                response.results.acrcloud.toCandidate("ACRCloud")
            )
            if (candidates.isEmpty()) {
                val providerErrors = listOfNotNull(response.results.audd.error, response.results.acrcloud.error).joinToString("; ")
                return@withContext if (response.error.isNullOrBlank()) RecognitionResult.NoMatch(providerErrors.ifBlank { "No confident song match was found." }) else RecognitionResult.Error(response.error)
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
        val dto = song ?: return null
        if (!success || dto.title.isNullOrBlank() || dto.artist.isNullOrBlank()) return null
        return Candidate(provider, dto.toRecognizedSong(confidence.coerceIn(0, 100)))
    }

    /** Compare only objective recognition data; no provider is preferred by default. */
    private fun selectDisplayCandidate(candidates: List<Candidate>): Candidate {
        if (candidates.size == 1) return candidates.first()
        val isrcs = candidates.mapNotNull { it.song.isrc?.trim()?.lowercase()?.takeIf(String::isNotBlank) }.distinct()
        val sameIsrc = isrcs.size == 1
        val keys = candidates.map { normalizeKey(it.song.artist, it.song.title) }.distinct()
        val sameTrack = keys.size == 1
        return candidates.maxWithOrNull(compareBy<Candidate> {
            when {
                sameIsrc -> 3
                sameTrack -> 2
                else -> 0
            }
        }.thenBy { metadataScore(it.song) }.thenBy { it.song.confidence }) ?: candidates.first()
    }

    private fun normalizeKey(artist: String, title: String) = "$artist $title".lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    private fun metadataScore(song: RecognizedSong) = listOf(song.album.isNotBlank(), song.isrc != null, song.artworkUrl != null, song.spotifyUrl != null, song.appleMusicUrl != null, song.youtubeMusicUrl != null, song.audiomackUrl != null).count { it }

    private fun RecognizedSongDto.toRecognizedSong(confidence: Int) = RecognizedSong(
        id = id?.takeIf { it.isNotBlank() } ?: "${artist.orEmpty()}:${title.orEmpty()}",
        title = title!!.trim(), artist = artist!!.trim(), album = album?.trim().orEmpty().ifBlank { "Unknown Album" },
        artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() }, durationMs = durationMs?.coerceAtLeast(0L) ?: 0L,
        confidence = confidence, isrc = isrc?.trim()?.takeIf { it.isNotBlank() }, spotifyUrl = spotifyUrl?.trim()?.takeIf { it.isNotBlank() },
        appleMusicUrl = appleMusicUrl?.trim()?.takeIf { it.isNotBlank() }, youtubeMusicUrl = youtubeMusicUrl?.trim()?.takeIf { it.isNotBlank() }, audiomackUrl = audiomackUrl?.trim()?.takeIf { it.isNotBlank() }
    )

    private suspend fun recognizeSingle(wavAudio: ByteArray, mode: String, call: suspend (MultipartBody.Part) -> RecognitionResponse): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudio.isEmpty()) return@withContext RecognitionResult.Error("No audio was captured.")
        if (api == null) return@withContext RecognitionResult.Error("Velvet recognition is not configured yet. Set VELVET_RECOGNITION_BASE_URL.")
        try {
            val part = MultipartBody.Part.createFormData("audio", "velvet-capture.wav", wavAudio.toRequestBody("audio/wav".toMediaType()))
            val response = call(part)
            val dto = response.song
            if (!response.success || dto == null || dto.title.isNullOrBlank() || dto.artist.isNullOrBlank()) return@withContext RecognitionResult.NoMatch(response.error ?: "No confident song match was found.")
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
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).callTimeout(45, TimeUnit.SECONDS).build()
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(SongRecognitionApi::class.java)
        }
    }
}
