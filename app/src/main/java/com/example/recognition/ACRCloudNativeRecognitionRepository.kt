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

    fun initialize(): Boolean {
        if (initialized) return true
        if (BuildConfig.ACRCLOUD_HOST.isBlank() ||
            BuildConfig.ACRCLOUD_ACCESS_KEY.isBlank() ||
            BuildConfig.ACRCLOUD_ACCESS_SECRET.isBlank()) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return false

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
            if (initialized) client = newClient else runCatching { newClient.release() }
            initialized
        } catch (t: Throwable) {
            android.util.Log.e("ACRCloudNative", "Failed to initialize native ACRCloud client", t)
            initialized = false
            client = null
            false
        }
    }

    fun startRecognition(onResult: (RecognitionResult) -> Unit): Boolean {
        resultCallback = onResult
        return try {
            if (!initialize()) {
                onResult(RecognitionResult.ProviderError(
                    title = "ACRCloud is not configured",
                    reason = "Configure ACRCloud host, access key and access secret for the Android build."
                ))
                return false
            }
            val started = client?.startRecognize() == true
            if (!started) onResult(RecognitionResult.ProviderError(
                title = "Couldn't start recognition",
                reason = "ACRCloud could not start microphone recognition."
            ))
            started
        } catch (t: Throwable) {
            android.util.Log.e("ACRCloudNative", "Failed to start native recognition", t)
            onResult(RecognitionResult.ProviderError(
                title = "Recognition error",
                reason = t.message ?: "Failed to start native recognizer"
            ))
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
        if (raw.isBlank()) return RecognitionResult.NoMatch()

        return try {
            val root = JSONObject(raw)
            if (root.optJSONObject("status")?.optInt("code", -1) != 0) {
                return RecognitionResult.NoMatch()
            }

            val metadata = root.optJSONObject("metadata") ?: return RecognitionResult.NoMatch()

            // Prefer ACRCloud humming/cover metadata for Sing to Search.
            // Only fall back to normal music metadata when no humming candidate exists.
            val item = metadata.optJSONArray("humming")?.optJSONObject(0)
                ?: metadata.optJSONArray("music")?.optJSONObject(0)
                ?: return RecognitionResult.NoMatch()

            val artist = item.optJSONArray("artists")
                ?.optJSONObject(0)
                ?.optString("name")
                .orEmpty()
            val title = item.optString("title")
            val album = item.optJSONObject("album")?.optString("name").orEmpty()

            if (title.isBlank() || artist.isBlank()) return RecognitionResult.NoMatch()

            val score = item.optDouble("score", 100.0).toInt().coerceIn(0, 100)

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
                )
            )
        } catch (ex: Exception) {
            RecognitionResult.ResponseParsingError(
                reason = "Invalid ACRCloud response: $\{ex.message\}",
                rawException = __EX_MESSAGE__
            )
        }
    }
}
