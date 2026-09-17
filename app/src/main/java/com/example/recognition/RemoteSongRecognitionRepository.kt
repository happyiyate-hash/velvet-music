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
import okhttp3.logging.HttpLoggingInterceptor
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
    data class NoMatch(
        val title: String = "Couldn't identify that song",
        val reason: String = "Try singing a little louder or move closer to the music."
    ) : RecognitionResult()
    data class ProviderError(
        val title: String = "Couldn't search right now",
        val reason: String = "Recognition services encountered an issue. Please try again."
    ) : RecognitionResult()
    data class ConnectionError(
        val title: String = "Couldn't connect",
        val reason: String = "Check your connection and try again."
    ) : RecognitionResult()
}

interface SongRecognitionRepository {
    suspend fun recognizeBatch(wavAudio: ByteArray): RecognitionResult
    suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult = recognizeBatch(wavAudio)
    suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult = recognizeBatch(wavAudio)
}

/**
 * Velvet Music production recognition client.
 * Android makes one request only to the Vercel backend.
 * The backend fans out to AudD and ACRCloud in parallel, returning both results.
 */
class RemoteSongRecognitionRepository(
    private val api: SongRecognitionApi = createApi()
) : SongRecognitionRepository {

    override suspend fun recognizeBatch(wavAudio: ByteArray): RecognitionResult = withContext(Dispatchers.IO) {
        if (wavAudio.isEmpty()) {
            return@withContext RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "No audio was captured. Check microphone and try again."
            )
        }
        if (wavAudio.size > MAX_AUDIO_BYTES) {
            return@withContext RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Captured audio exceeded size limit. Please try a shorter recording."
            )
        }

        val startedAt = System.currentTimeMillis()
        Log.d(TAG, "BATCH_UPLOAD_STARTED bytes=${wavAudio.size}")

        try {
            val requestBody = wavAudio.toRequestBody("audio/wav".toMediaType())
            val audioPart = MultipartBody.Part.createFormData(
                "audio",
                "velvet-recording.wav",
                requestBody
            )

            val response = api.recognizeBatch(audioPart)
            val elapsed = System.currentTimeMillis() - startedAt
            Log.d(TAG, "BATCH_RESPONSE_RECEIVED requestId=${response.requestId ?: "none"} elapsedMs=$elapsed")

            val auddResult = response.results.audd
            val acrResult = response.results.acrcloud

            val auddMatched = isMatched(auddResult)
            val acrMatched = isMatched(acrResult)

            when {
                // Case 1: Both matched -> compare and merge metadata
                auddMatched && acrMatched -> {
                    val auddSong = auddResult.song!!
                    val acrSong = acrResult.song!!

                    val auddIsrc = auddSong.isrc?.trim()?.takeIf { it.isNotBlank() }
                    val acrIsrc = acrSong.isrc?.trim()?.takeIf { it.isNotBlank() }
                    val sameIsrc = auddIsrc != null && acrIsrc != null && auddIsrc.equals(acrIsrc, ignoreCase = true)

                    val auddKey = normalizeKey(auddSong.artist.orEmpty(), auddSong.title.orEmpty())
                    val acrKey = normalizeKey(acrSong.artist.orEmpty(), acrSong.title.orEmpty())
                    val sameTrack = sameIsrc || (auddKey.isNotBlank() && auddKey == acrKey)

                    val finalSong: RecognizedSong = if (sameTrack) {
                        Log.d(TAG, "DUAL_MATCH_AGREED isrcMatch=$sameIsrc keyMatch=${auddKey == acrKey} - merging metadata")
                        mergeMetadata(auddResult, acrResult)
                    } else {
                        Log.d(TAG, "DUAL_MATCH_DIFFERENT audd='${auddSong.title}' acr='${acrSong.title}' - picking higher confidence")
                        if (auddResult.confidence >= acrResult.confidence) {
                            auddSong.toRecognizedSong(auddResult.confidence)
                        } else {
                            acrSong.toRecognizedSong(acrResult.confidence)
                        }
                    }
                    RecognitionResult.Match(finalSong)
                }

                // Case 2: Only AudD matched
                auddMatched -> {
                    Log.d(TAG, "AUDD_ONLY_MATCH title='${auddResult.song?.title}' artist='${auddResult.song?.artist}'")
                    RecognitionResult.Match(auddResult.song!!.toRecognizedSong(auddResult.confidence))
                }

                // Case 3: Only ACRCloud matched
                acrMatched -> {
                    Log.d(TAG, "ACRCLOUD_ONLY_MATCH title='${acrResult.song?.title}' artist='${acrResult.song?.artist}'")
                    RecognitionResult.Match(acrResult.song!!.toRecognizedSong(acrResult.confidence))
                }

                // Case 4: Neither recognized the song
                else -> {
                    val auddIsError = isError(auddResult)
                    val acrIsError = isError(acrResult)

                    if (auddIsError && acrIsError) {
                        Log.w(TAG, "BOTH_PROVIDERS_ERROR auddErr=${auddResult.error} acrErr=${acrResult.error}")
                        RecognitionResult.ProviderError(
                            title = "Couldn't search right now",
                            reason = "Please try again in a moment."
                        )
                    } else {
                        Log.d(TAG, "NO_MATCH_FOUND auddStatus=${auddResult.status} acrStatus=${acrResult.status} auddErr=${auddResult.error} acrErr=${acrResult.error}")
                        RecognitionResult.NoMatch(
                            title = "Couldn't identify that song",
                            reason = "Try singing a little louder or move closer to the music."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "BATCH_CONNECTION_ERROR elapsedMs=${System.currentTimeMillis() - startedAt} message=${e.message}", e)
            RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Check your connection and try again."
            )
        }
    }

    private fun isMatched(result: RecognitionResponse?): Boolean {
        if (result == null) return false
        val s = result.song ?: return false
        val validSong = !s.title.isNullOrBlank() && !s.artist.isNullOrBlank()
        return (result.status == "matched" || (result.success && validSong)) && validSong
    }

    private fun isError(result: RecognitionResponse?): Boolean {
        if (result == null) return true
        val errorMsg = result.error.orEmpty()
        // If the error message is just that fingerprint couldn't be generated (silence / ambient noise / unrecognized sound),
        // it means the audio had no recognizable music, so it's a no-match, NOT a server/provider failure!
        if (errorMsg.contains("fingerprint", ignoreCase = true) || errorMsg.contains("problem with creating", ignoreCase = true)) {
            return false
        }
        return result.status == "error" || (!result.success && result.status != "no_match" && !result.error.isNullOrBlank())
    }

    /** Merge available metadata across providers: first non-null useful value. */
    private fun mergeMetadata(
        audd: RecognitionResponse,
        acrcloud: RecognitionResponse
    ): RecognizedSong {
        val a = audd.song!!
        val b = acrcloud.song!!

        val title = a.title?.trim()?.takeIf { it.isNotBlank() } ?: b.title?.trim().orEmpty()
        val artist = a.artist?.trim()?.takeIf { it.isNotBlank() } ?: b.artist?.trim().orEmpty()
        val album = a.album?.trim()?.takeIf { it.isNotBlank() }
            ?: b.album?.trim()?.takeIf { it.isNotBlank() }
            ?: "Unknown Album"

        // Artwork: AudD usually provides Spotify/Apple CDN artwork; fallback to ACRCloud
        val artworkUrl = a.artworkUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.artworkUrl?.trim()?.takeIf { it.isNotBlank() }

        // Duration: non-zero duration from either provider (ACRCloud often has durationMs)
        val durationMs = when {
            (a.durationMs ?: 0L) > 0L -> a.durationMs!!
            (b.durationMs ?: 0L) > 0L -> b.durationMs!!
            else -> 0L
        }

        val isrc = a.isrc?.trim()?.takeIf { it.isNotBlank() }
            ?: b.isrc?.trim()?.takeIf { it.isNotBlank() }

        val spotifyUrl = a.spotifyUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.spotifyUrl?.trim()?.takeIf { it.isNotBlank() }

        val appleMusicUrl = a.appleMusicUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.appleMusicUrl?.trim()?.takeIf { it.isNotBlank() }

        val youtubeMusicUrl = a.youtubeMusicUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.youtubeMusicUrl?.trim()?.takeIf { it.isNotBlank() }

        val audiomackUrl = a.audiomackUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.audiomackUrl?.trim()?.takeIf { it.isNotBlank() }

        val confidence = maxOf(audd.confidence, acrcloud.confidence).coerceIn(0, 100)
        val id = a.id?.takeIf { it.isNotBlank() } ?: b.id?.takeIf { it.isNotBlank() } ?: "$artist:$title"

        return RecognizedSong(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            confidence = confidence,
            isrc = isrc,
            spotifyUrl = spotifyUrl,
            appleMusicUrl = appleMusicUrl,
            youtubeMusicUrl = youtubeMusicUrl,
            audiomackUrl = audiomackUrl
        )
    }

    private fun RecognizedSongDto.toRecognizedSong(confidence: Int): RecognizedSong {
        val cleanTitle = title?.trim().orEmpty().ifBlank { "Unknown Title" }
        val cleanArtist = artist?.trim().orEmpty().ifBlank { "Unknown Artist" }
        return RecognizedSong(
            id = id?.takeIf { it.isNotBlank() } ?: "$cleanArtist:$cleanTitle",
            title = cleanTitle,
            artist = cleanArtist,
            album = album?.trim().orEmpty().ifBlank { "Unknown Album" },
            artworkUrl = artworkUrl?.trim()?.takeIf { it.isNotBlank() },
            durationMs = durationMs?.coerceAtLeast(0L) ?: 0L,
            confidence = confidence.coerceIn(0, 100),
            isrc = isrc?.trim()?.takeIf { it.isNotBlank() },
            spotifyUrl = spotifyUrl?.trim()?.takeIf { it.isNotBlank() },
            appleMusicUrl = appleMusicUrl?.trim()?.takeIf { it.isNotBlank() },
            youtubeMusicUrl = youtubeMusicUrl?.trim()?.takeIf { it.isNotBlank() },
            audiomackUrl = audiomackUrl?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    private fun normalizeKey(artist: String, title: String): String {
        return "$artist $title".lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    companion object {
        private const val TAG = "VelvetRecognition"
        private const val MAX_AUDIO_BYTES = 5 * 1024 * 1024 // 5 MB
        private const val PRODUCTION_BASE_URL = "https://velvet-recognition-backend-cx6ybckmo-happyiyate-hashs-projects.vercel.app/"

        private fun createApi(): SongRecognitionApi {
            val baseUrl = PRODUCTION_BASE_URL

            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, "[HTTP] $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .callTimeout(50, TimeUnit.SECONDS)
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
