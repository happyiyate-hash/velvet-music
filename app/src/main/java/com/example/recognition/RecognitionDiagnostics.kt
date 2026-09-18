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
    val requestId: String? = null,
    val requestUrl: String = "https://velvet-recognition-backend-cx6ybckmo-happyiyate-hashs-projects.vercel.app/v1/recognition/batch",
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
    val isNetworkFailure: Boolean = false
) {
    /**
     * Sanitizes all sensitive fields and returns a clean, secure instance.
     */
    fun sanitized(): RecognitionDiagnostics {
        return copy(
            requestUrl = RecognitionSanitizer.sanitize(requestUrl),
            exceptionMessage = exceptionMessage?.let { RecognitionSanitizer.sanitize(it) },
            causeMessage = causeMessage?.let { RecognitionSanitizer.sanitize(it) },
            backendResponseBody = backendResponseBody?.let { RecognitionSanitizer.sanitize(it) },
            backendError = backendError?.let { RecognitionSanitizer.sanitize(it) },
            backendTrace = backendTrace?.map { RecognitionSanitizer.sanitize(it) },
            auddError = auddError?.let { RecognitionSanitizer.sanitize(it) },
            acrcloudError = acrcloudError?.let { RecognitionSanitizer.sanitize(it) },
            timeoutInfo = timeoutInfo?.let { RecognitionSanitizer.sanitize(it) }
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
        sb.appendLine("Request ID: ${s.requestId ?: "None"}")
        sb.appendLine()
        sb.appendLine("Request URL:")
        sb.appendLine(s.requestUrl)
        sb.appendLine()
        sb.appendLine("HTTP Status: ${s.httpStatus?.toString() ?: "None (Backend was not reached / network failure)"}")
        if (!s.httpStatusMessage.isNullOrBlank()) {
            sb.appendLine("HTTP Status Message: ${s.httpStatusMessage}")
        }
        sb.appendLine()
        sb.appendLine("Exception:")
        sb.appendLine(s.exceptionClass ?: "None")
        sb.appendLine()
        sb.appendLine("Exception Message:")
        sb.appendLine(s.exceptionMessage ?: "None")
        sb.appendLine()
        if (!s.causeClass.isNullOrBlank() || !s.causeMessage.isNullOrBlank()) {
            sb.appendLine("Cause:")
            sb.appendLine("${s.causeClass ?: "Unknown"}: ${s.causeMessage ?: ""}".trim())
            sb.appendLine()
        }
        if (!s.timeoutInfo.isNullOrBlank()) {
            sb.appendLine("Timeout Information:")
            sb.appendLine(s.timeoutInfo)
            sb.appendLine()
        }
        sb.appendLine("Backend Response:")
        sb.appendLine(s.backendResponseBody?.takeIf { it.isNotBlank() } ?: "None (Backend was not reached or empty body returned)")
        sb.appendLine()
        if (s.backendSuccess != null || s.backendError != null) {
            sb.appendLine("Backend Top-Level:")
            sb.appendLine("success=${s.backendSuccess}")
            sb.appendLine("error=${s.backendError ?: "None"}")
            sb.appendLine()
        }
        sb.appendLine("AudD:")
        sb.appendLine("success=${s.auddSuccess?.toString() ?: "None"}")
        sb.appendLine("status=${s.auddStatus ?: "None"}")
        sb.appendLine("error=${s.auddError ?: "None"}")
        sb.appendLine()
        sb.appendLine("ACRCloud:")
        sb.appendLine("success=${s.acrcloudSuccess?.toString() ?: "None"}")
        sb.appendLine("status=${s.acrcloudStatus ?: "None"}")
        sb.appendLine("error=${s.acrcloudError ?: "None"}")
        sb.appendLine()
        sb.appendLine("Trace:")
        if (!s.backendTrace.isNullOrEmpty()) {
            s.backendTrace.forEach { step ->
                sb.appendLine("- $step")
            }
        } else {
            sb.appendLine("None")
        }
        return sb.toString().trimEnd()
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
                .putString(KEY_LAST_REPORT, sanitized.toFormattedReport())
                .putString(KEY_TIMESTAMP, sanitized.timestamp)
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
            val report = prefs.getString(KEY_LAST_REPORT, null) ?: return null
            val timestamp = prefs.getString(KEY_TIMESTAMP, "Unknown") ?: "Unknown"
            val requestId = prefs.getString(KEY_REQUEST_ID, null)
            val httpStatusRaw = prefs.getInt(KEY_HTTP_STATUS, -1)
            val httpStatus = if (httpStatusRaw != -1) httpStatusRaw else null
            val requestUrl = prefs.getString(KEY_REQUEST_URL, "https://velvet-recognition-backend-cx6ybckmo-happyiyate-hashs-projects.vercel.app/v1/recognition/batch")
                ?: "https://velvet-recognition-backend-cx6ybckmo-happyiyate-hashs-projects.vercel.app/v1/recognition/batch"
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
                requestId = requestId,
                requestUrl = requestUrl,
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
