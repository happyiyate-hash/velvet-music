package com.example.recognition

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Thread-safe capture of raw network payloads and HTTP status,
 * ensuring complete diagnostic observation even when Moshi fails during deserialization.
 */
object NetworkResponseCapture {
    @Volatile
    var lastResponseBody: String? = null
    @Volatile
    var lastStatusCode: Int? = null
    @Volatile
    var lastStatusMessage: String? = null
    @Volatile
    var lastRequestUrl: String? = null
    @Volatile
    var lastRequestId: String? = null

    fun reset() {
        lastResponseBody = null
        lastStatusCode = null
        lastStatusMessage = null
        lastRequestUrl = null
        lastRequestId = null
    }
}

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
    val audiomackUrl: String?,
    val soundcloudUrl: String?,
    val boomplayUrl: String?
)

sealed class RecognitionResult {
    data class Match(
        val song: RecognizedSong,
        val diagnostics: RecognitionDiagnostics? = null
    ) : RecognitionResult()
    data class NoMatch(
        val title: String = "Couldn't identify that song",
        val reason: String = "Try singing a little louder or move closer to the music.",
        val diagnostics: RecognitionDiagnostics? = null
    ) : RecognitionResult()
    data class ProviderError(
        val title: String = "Couldn't search right now",
        val reason: String = "Recognition services encountered an issue. Please try again.",
        val httpStatus: Int? = null,
        val responseBody: String? = null,
        val diagnostics: RecognitionDiagnostics? = null
    ) : RecognitionResult()
    data class ResponseParsingError(
        val title: String = "Couldn't parse response",
        val reason: String = "Failed to parse recognition response from server.",
        val rawException: String? = null,
        val diagnostics: RecognitionDiagnostics? = null
    ) : RecognitionResult()
    data class ConnectionError(
        val title: String = "Couldn't connect",
        val reason: String = "Check your connection and try again.",
        val isTimeout: Boolean = false,
        val exceptionType: String? = null,
        val diagnostics: RecognitionDiagnostics? = null
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
        val startedAt = System.currentTimeMillis()
        val fullEndpointUrl = "${PRODUCTION_BASE_URL}v1/recognition/batch"
        NetworkResponseCapture.reset()
        NetworkResponseCapture.lastRequestUrl = fullEndpointUrl

        if (wavAudio.isEmpty()) {
            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = "IllegalArgumentException",
                exceptionMessage = "No audio was captured from microphone (0 bytes).",
                causeMessage = "AudioRecord buffer is empty",
                isNetworkFailure = false
            )
            return@withContext RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "No audio was captured. Check microphone and try again.",
                isTimeout = false,
                exceptionType = "EmptyAudioException",
                diagnostics = diag
            )
        }
        if (wavAudio.size > MAX_AUDIO_BYTES) {
            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = "IllegalArgumentException",
                exceptionMessage = "Captured audio byte count (${wavAudio.size} bytes) exceeded maximum allowed limit ($MAX_AUDIO_BYTES bytes).",
                causeMessage = "Audio recording too large",
                isNetworkFailure = false
            )
            return@withContext RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Captured audio exceeded size limit. Please try a shorter recording.",
                isTimeout = false,
                exceptionType = "AudioSizeExceededException",
                diagnostics = diag
            )
        }

        Log.d(TAG, "BATCH_UPLOAD_STARTED bytes=${wavAudio.size}")
        Log.d(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

        try {
            val requestBody = wavAudio.toRequestBody("audio/wav".toMediaType())
            val audioPart = MultipartBody.Part.createFormData(
                "audio",
                "velvet-recording.wav",
                requestBody
            )

            val response = api.recognizeBatch(audioPart)
            val elapsed = System.currentTimeMillis() - startedAt
            val rawBody = NetworkResponseCapture.lastResponseBody
            val reqId = response.requestId ?: NetworkResponseCapture.lastRequestId

            Log.d(TAG, "BATCH_RESPONSE_HTTP=200")
            Log.d(TAG, "BATCH_RESPONSE_RECEIVED requestId=${reqId ?: "none"} elapsedMs=$elapsed")

            val auddResult = response.results.audd
            val acrResult = response.results.acrcloud

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestId = reqId,
                requestUrl = NetworkResponseCapture.lastRequestUrl ?: fullEndpointUrl,
                httpStatus = 200,
                httpStatusMessage = "OK",
                backendResponseBody = rawBody,
                backendSuccess = response.success,
                backendError = response.error,
                backendTrace = response.trace,
                auddSuccess = auddResult.success,
                auddStatus = auddResult.status,
                auddError = auddResult.error,
                acrcloudSuccess = acrResult.success,
                acrcloudStatus = acrResult.status,
                acrcloudError = acrResult.error,
                isNetworkFailure = false
            )

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
                    RecognitionResult.Match(finalSong, diagnostics = diag)
                }

                // Case 2: Only AudD matched
                auddMatched -> {
                    Log.d(TAG, "AUDD_ONLY_MATCH title='${auddResult.song?.title}' artist='${auddResult.song?.artist}'")
                    RecognitionResult.Match(auddResult.song!!.toRecognizedSong(auddResult.confidence), diagnostics = diag)
                }

                // Case 3: Only ACRCloud matched
                acrMatched -> {
                    Log.d(TAG, "ACRCLOUD_ONLY_MATCH title='${acrResult.song?.title}' artist='${acrResult.song?.artist}'")
                    RecognitionResult.Match(acrResult.song!!.toRecognizedSong(acrResult.confidence), diagnostics = diag)
                }

                // Case 4: Neither recognized the song
                else -> {
                    val auddIsError = isError(auddResult)
                    val acrIsError = isError(acrResult)

                    if (!response.success && !response.error.isNullOrBlank()) {
                        Log.w(TAG, "TOP_LEVEL_BACKEND_ERROR error=${response.error}")
                        RecognitionResult.ProviderError(
                            title = "Recognition service error",
                            reason = response.error,
                            httpStatus = 200,
                            responseBody = rawBody,
                            diagnostics = diag
                        )
                    } else if (auddIsError && acrIsError) {
                        Log.w(TAG, "BOTH_PROVIDERS_ERROR auddErr=${auddResult.error} acrErr=${acrResult.error}")
                        RecognitionResult.ProviderError(
                            title = "Couldn't search right now",
                            reason = "AudD: ${auddResult.error ?: "Error"}; ACRCloud: ${acrResult.error ?: "Error"}",
                            httpStatus = 200,
                            responseBody = rawBody,
                            diagnostics = diag
                        )
                    } else if (auddIsError || acrIsError) {
                        val providerMsg = if (auddIsError) "AudD: ${auddResult.error}" else "ACRCloud: ${acrResult.error}"
                        Log.w(TAG, "SINGLE_PROVIDER_ERROR $providerMsg")
                        RecognitionResult.ProviderError(
                            title = "Provider error",
                            reason = providerMsg,
                            httpStatus = 200,
                            responseBody = rawBody,
                            diagnostics = diag
                        )
                    } else {
                        Log.d(TAG, "NO_MATCH_FOUND auddStatus=${auddResult.status} acrStatus=${acrResult.status} auddErr=${auddResult.error} acrErr=${acrResult.error}")
                        RecognitionResult.NoMatch(
                            title = "Couldn't identify that song",
                            reason = "Try singing a little louder or move closer to the music.",
                            diagnostics = diag
                        )
                    }
                }
            }
        } catch (e: HttpException) {
            val status = e.code()
            val statusMsg = e.message()
            val errorBody = try {
                e.response()?.errorBody()?.string().orEmpty()
            } catch (_: Exception) {
                ""
            }.ifBlank { NetworkResponseCapture.lastResponseBody.orEmpty() }

            val reqUrl = e.response()?.raw()?.request?.url?.toString()
                ?: NetworkResponseCapture.lastRequestUrl
                ?: fullEndpointUrl
            val reqId = e.response()?.headers()?.get("x-request-id")
                ?: e.response()?.headers()?.get("x-vercel-id")
                ?: NetworkResponseCapture.lastRequestId
                ?: "none"

            Log.e(TAG, "BATCH_HTTP_ERROR status=$status")
            Log.e(TAG, "body=$errorBody")
            Log.e(TAG, "BATCH_REQUEST_URL=$reqUrl")
            Log.e(TAG, "requestId=$reqId")
            Log.e(TAG, "exception type=${e.javaClass.name}")
            Log.e(TAG, "exception message=${e.message}")

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestId = reqId,
                requestUrl = reqUrl,
                httpStatus = status,
                httpStatusMessage = statusMsg,
                exceptionClass = e.javaClass.name,
                exceptionMessage = "HTTP $status: $statusMsg",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                backendResponseBody = errorBody,
                isNetworkFailure = false
            )

            RecognitionResult.ProviderError(
                title = "Server error ($status)",
                reason = if (errorBody.isNotBlank()) "Server returned HTTP $status: $errorBody" else "Recognition server returned HTTP $status. Please try again.",
                httpStatus = status,
                responseBody = errorBody,
                diagnostics = diag
            )
        } catch (e: JsonDataException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_RESPONSE_PARSING_ERROR elapsedMs=$elapsed")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")
            Log.e(TAG, "exception type=${e.javaClass.name}")
            Log.e(TAG, "exception message=${e.message}", e)

            val rawBody = NetworkResponseCapture.lastResponseBody
            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestId = NetworkResponseCapture.lastRequestId,
                requestUrl = NetworkResponseCapture.lastRequestUrl ?: fullEndpointUrl,
                httpStatus = NetworkResponseCapture.lastStatusCode ?: 200,
                httpStatusMessage = NetworkResponseCapture.lastStatusMessage ?: "OK",
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message,
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                backendResponseBody = rawBody,
                isNetworkFailure = false
            )

            RecognitionResult.ResponseParsingError(
                title = "Couldn't parse response",
                reason = "JSON schema mismatch: ${e.message}",
                rawException = e.message,
                diagnostics = diag
            )
        } catch (e: JsonEncodingException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_RESPONSE_PARSING_ERROR elapsedMs=$elapsed")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")
            Log.e(TAG, "exception type=${e.javaClass.name}")
            Log.e(TAG, "exception message=${e.message}", e)

            val rawBody = NetworkResponseCapture.lastResponseBody
            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestId = NetworkResponseCapture.lastRequestId,
                requestUrl = NetworkResponseCapture.lastRequestUrl ?: fullEndpointUrl,
                httpStatus = NetworkResponseCapture.lastStatusCode ?: 200,
                httpStatusMessage = NetworkResponseCapture.lastStatusMessage ?: "OK",
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message,
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                backendResponseBody = rawBody,
                isNetworkFailure = false
            )

            RecognitionResult.ResponseParsingError(
                title = "Couldn't parse response",
                reason = "Malformed JSON from server: ${e.message}",
                rawException = e.message,
                diagnostics = diag
            )
        } catch (e: SocketTimeoutException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_TIMEOUT_ERROR exception type=${e.javaClass.name} message=${e.message}")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

            val timeoutInfo = "ConnectTimeout: 15s, WriteTimeout: 20s, ReadTimeout: 45s, CallTimeout: 50s. Elapsed: ${elapsed}ms"
            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message ?: "Socket timeout during recognition request",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                timeoutInfo = timeoutInfo,
                isNetworkFailure = true
            )

            RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Connection timed out. Check your connection and try again.",
                isTimeout = true,
                exceptionType = e.javaClass.simpleName,
                diagnostics = diag
            )
        } catch (e: UnknownHostException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_UNKNOWN_HOST_ERROR exception type=${e.javaClass.name} message=${e.message}")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = e.javaClass.name,
                exceptionMessage = "Unable to resolve server address: ${e.message}",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                isNetworkFailure = true
            )

            RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Unable to resolve server address (${e.message}). Check your connection and try again.",
                isTimeout = false,
                exceptionType = e.javaClass.simpleName,
                diagnostics = diag
            )
        } catch (e: ConnectException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_CONNECT_EXCEPTION exception type=${e.javaClass.name} message=${e.message}")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = e.javaClass.name,
                exceptionMessage = "Connection refused or unreachable: ${e.message}",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                isNetworkFailure = true
            )

            RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Connection failed: ${e.message}. Check your connection and try again.",
                isTimeout = false,
                exceptionType = e.javaClass.simpleName,
                diagnostics = diag
            )
        } catch (e: SSLException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_SSL_EXCEPTION exception type=${e.javaClass.name} message=${e.message}")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = e.javaClass.name,
                exceptionMessage = "TLS/SSL handshake failure: ${e.message}",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                isNetworkFailure = true
            )

            RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = "Secure connection failed: ${e.message}. Check your connection and try again.",
                isTimeout = false,
                exceptionType = e.javaClass.simpleName,
                diagnostics = diag
            )
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_IO_ERROR ${e.javaClass.name}: ${e.message}")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message ?: "I/O network transmission error",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                isNetworkFailure = true
            )

            RecognitionResult.ConnectionError(
                title = "Couldn't connect",
                reason = e.message ?: "Check your connection and try again.",
                isTimeout = false,
                exceptionType = e.javaClass.simpleName,
                diagnostics = diag
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startedAt
            Log.e(TAG, "BATCH_UNEXPECTED_EXCEPTION ${e.javaClass.name}: ${e.message}")
            Log.e(TAG, "BATCH_REQUEST_URL=$fullEndpointUrl")

            val diag = RecognitionDiagnostics(
                timestamp = RecognitionDiagnostics.currentFormattedTimestamp(),
                requestUrl = fullEndpointUrl,
                exceptionClass = e.javaClass.name,
                exceptionMessage = e.message ?: "Exception in ${e.javaClass.name}",
                causeClass = e.cause?.javaClass?.name,
                causeMessage = e.cause?.message,
                isNetworkFailure = false
            )

            RecognitionResult.ConnectionError(
                title = "System error (${e.javaClass.simpleName})",
                reason = e.message ?: "Error in recognition engine: ${e.javaClass.name}",
                isTimeout = false,
                exceptionType = e.javaClass.simpleName,
                diagnostics = diag
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
        val soundcloudUrl = a.soundcloudUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.soundcloudUrl?.trim()?.takeIf { it.isNotBlank() }
        val boomplayUrl = a.boomplayUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: b.boomplayUrl?.trim()?.takeIf { it.isNotBlank() }

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
            audiomackUrl = audiomackUrl,
            soundcloudUrl = soundcloudUrl,
            boomplayUrl = boomplayUrl
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
            audiomackUrl = audiomackUrl?.trim()?.takeIf { it.isNotBlank() },
            soundcloudUrl = soundcloudUrl?.trim()?.takeIf { it.isNotBlank() },
            boomplayUrl = boomplayUrl?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    private fun normalizeKey(artist: String, title: String): String {
        return "$artist $title".lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    companion object {
        private const val TAG = "VelvetRecognition"
        private const val MAX_AUDIO_BYTES = 5 * 1024 * 1024 // 5 MB
        const val PRODUCTION_BASE_URL = "https://velvet-recognition-backend-cx6ybckmo-happyiyate-hashs-projects.vercel.app/"

        private fun createApi(): SongRecognitionApi {
            val baseUrl = PRODUCTION_BASE_URL

            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, "[HTTP] $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val captureInterceptor = Interceptor { chain ->
                val request = chain.request()
                NetworkResponseCapture.lastRequestUrl = request.url.toString()
                val response = chain.proceed(request)
                NetworkResponseCapture.lastStatusCode = response.code
                NetworkResponseCapture.lastStatusMessage = response.message
                val reqId = response.header("x-request-id")
                    ?: response.header("x-vercel-id")
                    ?: response.header("request-id")
                NetworkResponseCapture.lastRequestId = reqId
                try {
                    val peek = response.peekBody(1024 * 1024).string()
                    NetworkResponseCapture.lastResponseBody = peek
                } catch (_: Throwable) {
                    // Safe fallback
                }
                Log.d(TAG, "BATCH_RESPONSE_HTTP=${response.code} reqId=$reqId")
                response
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(captureInterceptor)
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
