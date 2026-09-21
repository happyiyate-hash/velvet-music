package com.example.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.acrcloud.rec.ACRCloudClient
import com.acrcloud.rec.ACRCloudConfig
import com.acrcloud.rec.ACRCloudResult
import com.acrcloud.rec.IACRCloudListener
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/** Direct microphone -> ACRCloud SDK. Velvet owns all UI. */
class ACRCloudNativeRecognitionRepository(
    private val context: Context
) : SongRecognitionRepository, IACRCloudListener {
    private val _volume = MutableStateFlow(0.0)
    val volume: StateFlow<Double> = _volume
    private var client: ACRCloudClient? = null
    private var initialized = false
    private var resultCallback: ((RecognitionResult) -> Unit)? = null
    private var lastInitializationDiagnostics: RecognitionDiagnostics? = null

    fun initialize(): Boolean {
        if (initialized) return true

        val microphoneGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hostConfigured = BuildConfig.ACRCLOUD_HOST.isNotBlank()
        val accessKeyConfigured = BuildConfig.ACRCLOUD_ACCESS_KEY.isNotBlank()
        val accessSecretConfigured = BuildConfig.ACRCLOUD_ACCESS_SECRET.isNotBlank()

        if (!microphoneGranted) {
            lastInitializationDiagnostics = RecognitionDiagnostics(
                failureStage = "MICROPHONE_PERMISSION",
                failureCode = "RECORD_AUDIO_PERMISSION_DENIED",
                failureReason = "Android RECORD_AUDIO permission is not granted, so ACRCloud cannot open the microphone.",
                microphonePermissionGranted = false,
                hostConfigured = hostConfigured,
                accessKeyConfigured = accessKeyConfigured,
                accessSecretConfigured = accessSecretConfigured,
                sdkInitialized = false,
                recognitionStarted = false
            )
            return false
        }

        val missing = buildList {
            if (!hostConfigured) add("host")
            if (!accessKeyConfigured) add("access key")
            if (!accessSecretConfigured) add("access secret")
        }
        if (missing.isNotEmpty()) {
            lastInitializationDiagnostics = RecognitionDiagnostics(
                failureStage = "CONFIGURATION",
                failureCode = "ACRCLOUD_CREDENTIALS_MISSING",
                failureReason = "ACRCloud configuration is incomplete. Missing: ${missing.joinToString(", ")}. The actual secret values are never shown.",
                microphonePermissionGranted = true,
                hostConfigured = hostConfigured,
                accessKeyConfigured = accessKeyConfigured,
                accessSecretConfigured = accessSecretConfigured,
                sdkInitialized = false,
                recognitionStarted = false
            )
            return false
        }

        return try {
            val config = ACRCloudConfig().apply {
                this.context = context.applicationContext
                host = BuildConfig.ACRCLOUD_HOST
                accessKey = BuildConfig.ACRCLOUD_ACCESS_KEY
                accessSecret = BuildConfig.ACRCLOUD_ACCESS_SECRET
                acrcloudListener = this@ACRCloudNativeRecognitionRepository
                recorderConfig.isVolumeCallback = true
                recorderConfig.reservedRecordBufferMS = 3000
            }
            val newClient = ACRCloudClient()
            initialized = newClient.initWithConfig(config)
            if (initialized) {
                client = newClient
                lastInitializationDiagnostics = null
            } else {
                runCatching { newClient.release() }
                lastInitializationDiagnostics = RecognitionDiagnostics(
                    failureStage = "SDK_INITIALIZATION",
                    failureCode = "ACRCLOUD_INIT_RETURNED_FALSE",
                    failureReason = "ACRCloudClient.initWithConfig(...) returned false. The SDK rejected or could not initialize the supplied configuration.",
                    microphonePermissionGranted = true,
                    hostConfigured = true,
                    accessKeyConfigured = true,
                    accessSecretConfigured = true,
                    sdkInitialized = false,
                    recognitionStarted = false
                )
            }
            initialized
        } catch (t: Throwable) {
            android.util.Log.e("ACRCloudNative", "Failed to initialize native ACRCloud client", t)
            initialized = false
            client = null
            lastInitializationDiagnostics = RecognitionDiagnostics(
                failureStage = "SDK_INITIALIZATION",
                failureCode = "ACRCLOUD_INIT_EXCEPTION",
                failureReason = "ACRCloud SDK initialization threw an exception.",
                exceptionClass = t.javaClass.name,
                exceptionMessage = t.message ?: t.toString(),
                causeClass = t.cause?.javaClass?.name,
                causeMessage = t.cause?.message,
                microphonePermissionGranted = true,
                hostConfigured = true,
                accessKeyConfigured = true,
                accessSecretConfigured = true,
                sdkInitialized = false,
                recognitionStarted = false
            )
            false
        }
    }

    fun startRecognition(onResult: (RecognitionResult) -> Unit): Boolean {
        resultCallback = onResult
        var callbackDelivered = false
        val deliver: (RecognitionResult) -> Unit = {
            callbackDelivered = true
            onResult(it)
        }

        return try {
            if (!initialize()) {
                deliver(
                    RecognitionResult.ProviderError(
                        title = "ACRCloud could not initialize",
                        reason = lastInitializationDiagnostics?.failureReason ?: "ACRCloud initialization failed.",
                        diagnostics = lastInitializationDiagnostics
                    )
                )
                resultCallback = null
                return false
            }

            val started = try {
                client?.startRecognize() == true
            } catch (t: Throwable) {
                val diag = RecognitionDiagnostics(
                    failureStage = "RECOGNITION_START",
                    failureCode = "ACRCLOUD_START_EXCEPTION",
                    failureReason = "ACRCloudClient.startRecognize() threw an exception.",
                    exceptionClass = t.javaClass.name,
                    exceptionMessage = t.message ?: t.toString(),
                    causeClass = t.cause?.javaClass?.name,
                    causeMessage = t.cause?.message,
                    microphonePermissionGranted = true,
                    hostConfigured = true,
                    accessKeyConfigured = true,
                    accessSecretConfigured = true,
                    sdkInitialized = true,
                    recognitionStarted = false
                )
                deliver(RecognitionResult.ProviderError("ACRCloud start exception", diag.failureReason ?: "ACRCloud start failed.", diagnostics = diag))
                false
            }

            if (!started && !callbackDelivered) {
                val diag = RecognitionDiagnostics(
                    failureStage = "RECOGNITION_START",
                    failureCode = "ACRCLOUD_START_RETURNED_FALSE",
                    failureReason = "ACRCloudClient.startRecognize() returned false. The SDK did not start microphone recognition.",
                    microphonePermissionGranted = true,
                    hostConfigured = true,
                    accessKeyConfigured = true,
                    accessSecretConfigured = true,
                    sdkInitialized = true,
                    recognitionStarted = false
                )
                deliver(RecognitionResult.ProviderError("ACRCloud could not start", diag.failureReason ?: "ACRCloud start failed.", diagnostics = diag))
            }
            if (!started) resultCallback = null
            started
        } catch (t: Throwable) {
            if (!callbackDelivered) {
                val diag = RecognitionDiagnostics(
                    failureStage = "RECOGNITION_START",
                    failureCode = "ACRCLOUD_START_UNEXPECTED_EXCEPTION",
                    failureReason = "Unexpected exception while starting ACRCloud microphone recognition.",
                    exceptionClass = t.javaClass.name,
                    exceptionMessage = t.message ?: t.toString(),
                    causeClass = t.cause?.javaClass?.name,
                    causeMessage = t.cause?.message,
                    microphonePermissionGranted = true,
                    hostConfigured = true,
                    accessKeyConfigured = true,
                    accessSecretConfigured = true,
                    sdkInitialized = initialized,
                    recognitionStarted = false
                )
                deliver(RecognitionResult.ProviderError("ACRCloud start exception", diag.failureReason ?: "ACRCloud start failed.", diagnostics = diag))
            }
            resultCallback = null
            false
        }
    }

    fun stopRecognition() {
        try {
            client?.cancel()
        } catch (t: Throwable) {
            android.util.Log.e("ACRCloudNative", "Error canceling client", t)
        }
        resultCallback = null
        _volume.value = 0.0
    }

    fun release() {
        resultCallback = null
        try {
            client?.release()
        } catch (t: Throwable) {
            android.util.Log.e("ACRCloudNative", "Error releasing client", t)
        }
        client = null
        initialized = false
        _volume.value = 0.0
    }

    override fun onResult(results: ACRCloudResult?) {
        val raw = results?.result.orEmpty()
        resultCallback?.invoke(parseResult(raw))
        resultCallback = null
    }

    override fun onVolumeChanged(curVolume: Double) {
        _volume.value = curVolume
    }

    override suspend fun recognizeBatch(wavAudio: ByteArray): RecognitionResult =
        RecognitionResult.ProviderError(
            title = "Direct recognition mode",
            reason = "Live recognition uses the native ACRCloud microphone SDK."
        )

    override suspend fun recognizeHumming(wavAudio: ByteArray): RecognitionResult =
        RecognitionResult.ProviderError(title = "Direct recognition mode", reason = "Use the native microphone recognizer.")

    override suspend fun recognizeAmbientAudio(wavAudio: ByteArray): RecognitionResult =
        RecognitionResult.ProviderError(title = "Direct recognition mode", reason = "Use the native microphone recognizer.")

    private fun parseResult(raw: String): RecognitionResult {
        if (raw.isBlank()) {
            val diag = RecognitionDiagnostics(
                failureStage = "PROVIDER_RESPONSE",
                failureCode = "ACRCLOUD_EMPTY_RESPONSE",
                failureReason = "ACRCloud SDK delivered an empty result callback.",
                microphonePermissionGranted = true,
                hostConfigured = true,
                accessKeyConfigured = true,
                accessSecretConfigured = true,
                sdkInitialized = initialized,
                recognitionStarted = true
            )
            return RecognitionResult.ProviderError("ACRCloud returned no response", diag.failureReason ?: "Empty response.", diagnostics = diag)
        }

        return try {
            val root = JSONObject(raw)
            val status = root.optJSONObject("status")
            val statusCode = status?.optInt("code", -1) ?: -1
            val statusMessage = status?.optString("msg").orEmpty().ifBlank { "No status message supplied" }

            if (statusCode != 0) {
                val diag = RecognitionDiagnostics(
                    failureStage = "PROVIDER_RESPONSE",
                    failureCode = "ACRCLOUD_STATUS_$statusCode",
                    failureReason = "ACRCloud returned status code $statusCode: $statusMessage",
                    sdkInitialized = initialized,
                    recognitionStarted = true,
                    sdkStatusCode = statusCode,
                    sdkStatusMessage = statusMessage,
                    rawProviderResponse = raw,
                    microphonePermissionGranted = true,
                    hostConfigured = true,
                    accessKeyConfigured = true,
                    accessSecretConfigured = true
                )
                return RecognitionResult.ProviderError(
                    title = "ACRCloud rejected the recognition request",
                    reason = diag.failureReason ?: statusMessage,
                    diagnostics = diag
                )
            }

            val metadata = root.optJSONObject("metadata")
            if (metadata == null) {
                val diag = RecognitionDiagnostics(
                    failureStage = "PROVIDER_RESPONSE",
                    failureCode = "ACRCLOUD_METADATA_MISSING",
                    failureReason = "ACRCloud returned status 0 but the response contains no metadata object.",
                    sdkInitialized = initialized,
                    recognitionStarted = true,
                    sdkStatusCode = statusCode,
                    sdkStatusMessage = statusMessage,
                    rawProviderResponse = raw
                )
                return RecognitionResult.ResponseParsingError("ACRCloud response missing metadata", diag.failureReason ?: "Metadata missing.", diagnostics = diag)
            }

            val item = metadata.optJSONArray("humming")?.optJSONObject(0)
                ?: metadata.optJSONArray("music")?.optJSONObject(0)

            if (item == null) {
                val diag = RecognitionDiagnostics(
                    failureStage = "PROVIDER_RESPONSE",
                    failureCode = "ACRCLOUD_NO_MATCH",
                    failureReason = "ACRCloud accepted the audio but returned no humming or music match.",
                    sdkInitialized = initialized,
                    recognitionStarted = true,
                    sdkStatusCode = statusCode,
                    sdkStatusMessage = statusMessage,
                    rawProviderResponse = raw
                )
                return RecognitionResult.NoMatch(
                    title = "Couldn't identify that song",
                    reason = diag.failureReason ?: "ACRCloud returned no match.",
                    diagnostics = diag
                )
            }

            val artist = item.optJSONArray("artists")?.optJSONObject(0)?.optString("name").orEmpty()
            val title = item.optString("title")
            val album = item.optJSONObject("album")?.optString("name").orEmpty()
            if (title.isBlank() || artist.isBlank()) {
                val diag = RecognitionDiagnostics(
                    failureStage = "PROVIDER_RESPONSE",
                    failureCode = "ACRCLOUD_RESULT_INCOMPLETE",
                    failureReason = "ACRCloud returned a candidate, but title or artist was missing.",
                    sdkInitialized = initialized,
                    recognitionStarted = true,
                    sdkStatusCode = statusCode,
                    sdkStatusMessage = statusMessage,
                    rawProviderResponse = raw
                )
                return RecognitionResult.ResponseParsingError("Incomplete ACRCloud result", diag.failureReason ?: "Title or artist missing.", diagnostics = diag)
            }

            val score = item.optDouble("score", 100.0).toInt().coerceIn(0, 100)
            val diag = RecognitionDiagnostics(
                failureStage = "RESULT",
                failureReason = "ACRCloud returned a match.",
                sdkInitialized = initialized,
                recognitionStarted = true,
                sdkStatusCode = statusCode,
                sdkStatusMessage = statusMessage,
                rawProviderResponse = raw
            )
            RecognitionResult.Match(
                RecognizedSong(
                    id = item.optString("acrid", "$title:$artist"),
                    title = title,
                    artist = artist,
                    album = album,
                    artworkUrl = null,
                    durationMs = item.optLong("duration_ms", 0L),
                    confidence = score,
                    isrc = null,
                    spotifyUrl = null,
                    appleMusicUrl = null,
                    youtubeMusicUrl = null,
                    audiomackUrl = null,
                    soundcloudUrl = null,
                    boomplayUrl = null
                ),
                diagnostics = diag
            )
        } catch (ex: Exception) {
            val diag = RecognitionDiagnostics(
                failureStage = "RESPONSE_PARSING",
                failureCode = "ACRCLOUD_INVALID_JSON",
                failureReason = "Could not parse the exact JSON returned by the ACRCloud SDK: ${ex.message}",
                exceptionClass = ex.javaClass.name,
                exceptionMessage = ex.message ?: ex.toString(),
                sdkInitialized = initialized,
                recognitionStarted = true,
                rawProviderResponse = raw
            )
            RecognitionResult.ResponseParsingError(
                reason = diag.failureReason ?: "Invalid ACRCloud response.",
                rawException = ex.message,
                diagnostics = diag
            )
        }
    }

}
