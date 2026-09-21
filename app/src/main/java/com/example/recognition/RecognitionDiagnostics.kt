package com.example.recognition

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Complete technical diagnostic model capturing end-to-end details from the network/server
 * layer all the way to the UI.
 *
 * Preserved in both DEBUG and RELEASE builds (guaranteed via ProGuard -keep rules).
 * All sensitive API keys and authorization secrets are scrubbed before storage or display.
 */
data class RecognitionDiagnostics(
    val timestamp: String = currentFormattedTimestamp(),
    val mode: String = "Native ACRCloud Android SDK",
    val provider: String = "ACRCloud",
    val failureStage: String? = null,
    val failureCode: String? = null,
    val failureReason: String? = null,
    val requestId: String? = null,
    val requestUrl: String? = null,
    val httpStatus: Int? = null,
    val httpStatusMessage: String? = null,
    val exceptionClass: String? = null,
    val exceptionMessage: String? = null,
    val causeClass: String? = null,
    val causeMessage: String? = null,
    val backendResponseBody: String? = null,
    val backendSuccess: Boolean? = null,
    val backendError: String? = null,
    val backendTrace: List<String>? = null,
    val auddSuccess: Boolean? = null,
    val auddStatus: String? = null,
    val auddError: String? = null,
    val acrcloudSuccess: Boolean? = null,
    val acrcloudStatus: String? = null,
    val acrcloudError: String? = null,
    val timeoutInfo: String? = null,
    val microphonePermissionGranted: Boolean? = null,
    val hostConfigured: Boolean? = null,
    val accessKeyConfigured: Boolean? = null,
    val accessSecretConfigured: Boolean? = null,
    val sdkInitialized: Boolean? = null,
    val recognitionStarted: Boolean? = null,
    val sdkStatusCode: Int? = null,
    val sdkStatusMessage: String? = null,
    val rawProviderResponse: String? = null,
    val backendUsed: Boolean = false,
    val isNetworkFailure: Boolean = false
) {
    /**
     * Sanitizes all sensitive fields and returns a clean, secure instance.
     */
    fun sanitized(): RecognitionDiagnostics {
        return copy(
            requestUrl = requestUrl?.let { RecognitionSanitizer.sanitize(it) },
            failureReason = failureReason?.let { RecognitionSanitizer.sanitize(it) },
            exceptionMessage = exceptionMessage?.let { RecognitionSanitizer.sanitize(it) },
            causeMessage = causeMessage?.let { RecognitionSanitizer.sanitize(it) },
            backendResponseBody = backendResponseBody?.let { RecognitionSanitizer.sanitize(it) },
            backendError = backendError?.let { RecognitionSanitizer.sanitize(it) },
            backendTrace = backendTrace?.map { RecognitionSanitizer.sanitize(it) },
            auddError = auddError?.let { RecognitionSanitizer.sanitize(it) },
            acrcloudError = acrcloudError?.let { RecognitionSanitizer.sanitize(it) },
            timeoutInfo = timeoutInfo?.let { RecognitionSanitizer.sanitize(it) },
            sdkStatusMessage = sdkStatusMessage?.let { RecognitionSanitizer.sanitize(it) },
            rawProviderResponse = rawProviderResponse?.let { RecognitionSanitizer.sanitize(it) }
        )
    }

    /**
     * Generates an exact, human-readable, un-truncated diagnostic report
     * ready to be copied directly into chat or bug trackers.
     */
    fun toFormattedReport(): String {
        val s = sanitized()
        val sb = StringBuilder()
        sb.appendLine("VELVET RECOGNITION DIAGNOSTICS")
        sb.appendLine()
        sb.appendLine("Time: ${s.timestamp}")
        sb.appendLine("Mode: ${s.mode}")
        sb.appendLine("Provider: ${s.provider}")
        sb.appendLine()
        sb.appendLine("FAILURE / RESULT")
        sb.appendLine("Stage: ${s.failureStage ?: "None reported"}")
        sb.appendLine("Code: ${s.failureCode ?: "None"}")
        sb.appendLine("Reason: ${s.failureReason ?: "None reported"}")
        sb.appendLine()
        sb.appendLine("NATIVE ACRCloud STATE")
        sb.appendLine("Microphone permission: ${status(s.microphonePermissionGranted)}")
        sb.appendLine("Host configured: ${status(s.hostConfigured)}")
        sb.appendLine("Access key configured: ${status(s.accessKeyConfigured)}")
        sb.appendLine("Access secret configured: ${status(s.accessSecretConfigured)}")
        sb.appendLine("SDK initialized: ${status(s.sdkInitialized)}")
        sb.appendLine("Recognition started: ${status(s.recognitionStarted)}")
        if (s.sdkStatusCode != null || !s.sdkStatusMessage.isNullOrBlank()) {
            sb.appendLine("SDK status code: ${s.sdkStatusCode ?: "None"}")
            sb.appendLine("SDK status message: ${s.sdkStatusMessage ?: "None"}")
        }
        if (!s.rawProviderResponse.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("RAW ACRCloud RESPONSE")
            sb.appendLine(s.rawProviderResponse)
        }
        if (!s.exceptionClass.isNullOrBlank() || !s.exceptionMessage.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("EXCEPTION")
            sb.appendLine("Type: ${s.exceptionClass ?: "Unknown"}")
            sb.appendLine("Message: ${s.exceptionMessage ?: "None"}")
        }
        if (!s.causeClass.isNullOrBlank() || !s.causeMessage.isNullOrBlank()) {
            sb.appendLine("Cause: ${s.causeClass ?: "Unknown"}: ${s.causeMessage ?: ""}".trim())
        }
        if (!s.timeoutInfo.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("TIMEOUT")
            sb.appendLine(s.timeoutInfo)
        }
        sb.appendLine()
        sb.appendLine("BACKEND")
        if (s.backendUsed) {
            sb.appendLine("Used: Yes")
            sb.appendLine("Request ID: ${s.requestId ?: "None"}")
            sb.appendLine("Request URL: ${s.requestUrl ?: "None"}")
            sb.appendLine("HTTP status: ${s.httpStatus ?: "None"}")
            sb.appendLine("HTTP message: ${s.httpStatusMessage ?: "None"}")
            sb.appendLine("Response: ${s.backendResponseBody?.takeIf { it.isNotBlank() } ?: "None"}")
            sb.appendLine("Backend success: ${s.backendSuccess ?: "None"}")
            sb.appendLine("Backend error: ${s.backendError ?: "None"}")
        } else {
            sb.appendLine("Used: No — this recognition attempt did not use the Velvet backend.")
        }
        return sb.toString().trimEnd()
    }

    private fun status(value: Boolean?): String = when (value) {
        true -> "YES"
        false -> "NO"
        null -> "UNKNOWN"
    }

    companion object {
        fun currentFormattedTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }
    }
}

/**
 * Secure sanitizer ensuring API keys, bearer tokens, and secrets are NEVER exposed in diagnostics.
 */
object RecognitionSanitizer {
    private val BEARER_REGEX = Regex("(?i)bearer\\s+[a-zA-Z0-9_\\-\\.]+")
    private val TOKEN_REGEX = Regex("(?i)(api[_-]?token|access[_-]?key|access[_-]?secret|api[_-]?key|client[_-]?secret|secret|token|authorization)\\s*[:=]\\s*[\"']?([a-zA-Z0-9_\\-\\.]{6,})[\"']?")
    private val URL_PARAM_REGEX = Regex("(?i)([?&](?:token|api_token|access_key|access_secret|api_key|secret)=)[^&]+")

    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        var sanitized = input
        sanitized = BEARER_REGEX.replace(sanitized) { "Bearer [REDACTED]" }
        sanitized = TOKEN_REGEX.replace(sanitized) { matchResult ->
            val key = matchResult.groupValues[1]
            "$key: \"[REDACTED]\""
        }
        sanitized = URL_PARAM_REGEX.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}[REDACTED]"
        }
        return sanitized
    }
}

/**
 * Local storage for the last diagnostic report so it survives across sheet reopens and app restarts.
 */
object RecognitionDiagnosticsStore {
    private const val TAG = "VelvetDiagnosticsStore"
    private const val PREFS_NAME = "velvet_recognition_diagnostics"
    private const val KEY_SCHEMA = "diagnostics_schema_v2"
    private const val KEY_LAST_REPORT = "last_diagnostic_report"
    private const val KEY_TIMESTAMP = "last_timestamp"
    private const val KEY_REQUEST_ID = "last_request_id"
    private const val KEY_HTTP_STATUS = "last_http_status"
    private const val KEY_REQUEST_URL = "last_request_url"
    private const val KEY_EXCEPTION_CLASS = "last_exception_class"
    private const val KEY_EXCEPTION_MESSAGE = "last_exception_message"
    private const val KEY_BACKEND_RESPONSE = "last_backend_response"
    private const val KEY_BACKEND_SUCCESS = "last_backend_success"
    private const val KEY_BACKEND_ERROR = "last_backend_error"
    private const val KEY_AUDD_STATUS = "last_audd_status"
    private const val KEY_AUDD_ERROR = "last_audd_error"
    private const val KEY_ACR_STATUS = "last_acr_status"
    private const val KEY_ACR_ERROR = "last_acr_error"

    @Volatile
    private var inMemoryLastDiagnostics: RecognitionDiagnostics? = null

    fun saveLastDiagnostics(context: Context, diagnostics: RecognitionDiagnostics) {
        val sanitized = diagnostics.sanitized()
        inMemoryLastDiagnostics = sanitized
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SCHEMA, "native-v2")
                .putString(KEY_SCHEMA, "native-v2")
                .putString(KEY_SCHEMA, "native-v2")
                .putString(KEY_LAST_REPORT, sanitized.toFormattedReport())
                .putString(KEY_TIMESTAMP, sanitized.timestamp)
                .putString("last_mode", sanitized.mode)
                .putString("last_provider", sanitized.provider)
                .putString("last_failure_stage", sanitized.failureStage)
                .putString("last_failure_code", sanitized.failureCode)
                .putString("last_failure_reason", sanitized.failureReason)
                .putBoolean("last_mic_permission", sanitized.microphonePermissionGranted ?: false)
                .putBoolean("last_host_configured", sanitized.hostConfigured ?: false)
                .putBoolean("last_key_configured", sanitized.accessKeyConfigured ?: false)
                .putBoolean("last_secret_configured", sanitized.accessSecretConfigured ?: false)
                .putBoolean("last_sdk_initialized", sanitized.sdkInitialized ?: false)
                .putBoolean("last_recognition_started", sanitized.recognitionStarted ?: false)
                .putInt("last_sdk_status_code", sanitized.sdkStatusCode ?: Int.MIN_VALUE)
                .putString("last_sdk_status_message", sanitized.sdkStatusMessage)
                .putString("last_raw_provider_response", sanitized.rawProviderResponse)
                .putBoolean("last_backend_used", sanitized.backendUsed)
                .putString("last_mode", sanitized.mode)
                .putString("last_provider", sanitized.provider)
                .putString("last_failure_stage", sanitized.failureStage)
                .putString("last_failure_code", sanitized.failureCode)
                .putString("last_failure_reason", sanitized.failureReason)
                .putBoolean("last_mic_permission", sanitized.microphonePermissionGranted ?: false)
                .putBoolean("last_host_configured", sanitized.hostConfigured ?: false)
                .putBoolean("last_key_configured", sanitized.accessKeyConfigured ?: false)
                .putBoolean("last_secret_configured", sanitized.accessSecretConfigured ?: false)
                .putBoolean("last_sdk_initialized", sanitized.sdkInitialized ?: false)
                .putBoolean("last_recognition_started", sanitized.recognitionStarted ?: false)
                .putInt("last_sdk_status_code", sanitized.sdkStatusCode ?: Int.MIN_VALUE)
                .putString("last_sdk_status_message", sanitized.sdkStatusMessage)
                .putString("last_raw_provider_response", sanitized.rawProviderResponse)
                .putBoolean("last_backend_used", sanitized.backendUsed)
                .putString("last_mode", sanitized.mode)
                .putString("last_provider", sanitized.provider)
                .putString("last_failure_stage", sanitized.failureStage)
                .putString("last_failure_code", sanitized.failureCode)
                .putString("last_failure_reason", sanitized.failureReason)
                .putBoolean("last_mic_permission", sanitized.microphonePermissionGranted ?: false)
                .putBoolean("last_host_configured", sanitized.hostConfigured ?: false)
                .putBoolean("last_key_configured", sanitized.accessKeyConfigured ?: false)
                .putBoolean("last_secret_configured", sanitized.accessSecretConfigured ?: false)
                .putBoolean("last_sdk_initialized", sanitized.sdkInitialized ?: false)
                .putBoolean("last_recognition_started", sanitized.recognitionStarted ?: false)
                .putInt("last_sdk_status_code", sanitized.sdkStatusCode ?: Int.MIN_VALUE)
                .putString("last_sdk_status_message", sanitized.sdkStatusMessage)
                .putString("last_raw_provider_response", sanitized.rawProviderResponse)
                .putBoolean("last_backend_used", sanitized.backendUsed)
                .putString(KEY_REQUEST_ID, sanitized.requestId)
                .putInt(KEY_HTTP_STATUS, sanitized.httpStatus ?: -1)
                .putString(KEY_REQUEST_URL, sanitized.requestUrl)
                .putString(KEY_EXCEPTION_CLASS, sanitized.exceptionClass)
                .putString(KEY_EXCEPTION_MESSAGE, sanitized.exceptionMessage)
                .putString(KEY_BACKEND_RESPONSE, sanitized.backendResponseBody)
                .putString(KEY_BACKEND_ERROR, sanitized.backendError)
                .putString(KEY_AUDD_STATUS, sanitized.auddStatus)
                .putString(KEY_AUDD_ERROR, sanitized.auddError)
                .putString(KEY_ACR_STATUS, sanitized.acrcloudStatus)
                .putString(KEY_ACR_ERROR, sanitized.acrcloudError)
                .apply()
            Log.d(TAG, "Successfully saved recognition diagnostics locally.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save diagnostics to SharedPreferences", t)
        }
    }

    fun getLastDiagnostics(context: Context): RecognitionDiagnostics? {
        inMemoryLastDiagnostics?.let { return it }
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getString(KEY_SCHEMA, null) != "native-v2") return null
            if (prefs.getString(KEY_SCHEMA, null) != "native-v2") return null
            if (prefs.getString(KEY_SCHEMA, null) != "native-v2") return null
            val report = prefs.getString(KEY_LAST_REPORT, null) ?: return null
            val timestamp = prefs.getString(KEY_TIMESTAMP, "Unknown") ?: "Unknown"
            val requestId = prefs.getString(KEY_REQUEST_ID, null)
            val httpStatusRaw = prefs.getInt(KEY_HTTP_STATUS, -1)
            val httpStatus = if (httpStatusRaw != -1) httpStatusRaw else null
            val requestUrl = prefs.getString(KEY_REQUEST_URL, null)
            val excClass = prefs.getString(KEY_EXCEPTION_CLASS, null)
            val excMsg = prefs.getString(KEY_EXCEPTION_MESSAGE, null)
            val responseBody = prefs.getString(KEY_BACKEND_RESPONSE, null)
            val backendErr = prefs.getString(KEY_BACKEND_ERROR, null)
            val auddStat = prefs.getString(KEY_AUDD_STATUS, null)
            val auddErr = prefs.getString(KEY_AUDD_ERROR, null)
            val acrStat = prefs.getString(KEY_ACR_STATUS, null)
            val acrErr = prefs.getString(KEY_ACR_ERROR, null)

            val loaded = RecognitionDiagnostics(
                timestamp = timestamp,
                mode = prefs.getString("last_mode", "Native ACRCloud Android SDK") ?: "Native ACRCloud Android SDK",
                provider = prefs.getString("last_provider", "ACRCloud") ?: "ACRCloud",
                failureStage = prefs.getString("last_failure_stage", null),
                failureCode = prefs.getString("last_failure_code", null),
                failureReason = prefs.getString("last_failure_reason", null),
                requestId = requestId,
                requestUrl = requestUrl,
                microphonePermissionGranted = prefs.getBoolean("last_mic_permission", false),
                hostConfigured = prefs.getBoolean("last_host_configured", false),
                accessKeyConfigured = prefs.getBoolean("last_key_configured", false),
                accessSecretConfigured = prefs.getBoolean("last_secret_configured", false),
                sdkInitialized = prefs.getBoolean("last_sdk_initialized", false),
                recognitionStarted = prefs.getBoolean("last_recognition_started", false),
                sdkStatusCode = prefs.getInt("last_sdk_status_code", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                sdkStatusMessage = prefs.getString("last_sdk_status_message", null),
                rawProviderResponse = prefs.getString("last_raw_provider_response", null),
                backendUsed = prefs.getBoolean("last_backend_used", false),
                httpStatus = httpStatus,
                exceptionClass = excClass,
                exceptionMessage = excMsg,
                backendResponseBody = responseBody,
                backendError = backendErr,
                auddStatus = auddStat,
                auddError = auddErr,
                acrcloudStatus = acrStat,
                acrcloudError = acrErr
            )
            inMemoryLastDiagnostics = loaded
            loaded
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load diagnostics from SharedPreferences", t)
            null
        }
    }

    fun clear(context: Context) {
        inMemoryLastDiagnostics = null
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Throwable) {}
    }
}
