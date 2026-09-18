package com.example.recognition

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * High-performance resolver and fetcher for track cover artwork.
 * 
 * 1. Normalizes Apple Music / AudD placeholder dimensions ({w}x{h} -> 800x800).
 * 2. If the recognition service returned no artwork, fetches official high-resolution
 *    cover art asynchronously via the iTunes public catalog.
 * 3. In-memory caching for zero-latency repeats.
 */
object SongArtworkResolver {
    private const val TAG = "SongArtworkResolver"
    private val memoryCache = ConcurrentHashMap<String, String>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun cleanArtworkUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        var cleaned = rawUrl.trim()
        if (cleaned.contains("{w}x{h}")) {
            cleaned = cleaned.replace("{w}x{h}", "800x800")
        }
        if (cleaned.contains("{w}") && cleaned.contains("{h}")) {
            cleaned = cleaned.replace("{w}", "800").replace("{h}", "800")
        }
        if (cleaned.startsWith("http://", ignoreCase = true)) {
            cleaned = "https://" + cleaned.substring(7)
        }
        return cleaned
    }

    suspend fun resolveArtwork(
        artist: String,
        title: String,
        rawArtworkUrl: String?
    ): String? = withContext(Dispatchers.IO) {
        val cleaned = cleanArtworkUrl(rawArtworkUrl)
        if (!cleaned.isNullOrBlank()) {
            return@withContext cleaned
        }

        val cacheKey = "${artist.trim().lowercase()} - ${title.trim().lowercase()}"
        memoryCache[cacheKey]?.let { return@withContext it }

        if (artist.isBlank() && title.isBlank()) return@withContext null

        try {
            val query = "$artist $title".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VelvetMusic/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "iTunes search unsuccessful: HTTP ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null

                val json = JSONObject(body)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val rawArt = first.optString("artworkUrl100").takeIf { it.isNotBlank() }
                        ?: first.optString("artworkUrl60").takeIf { it.isNotBlank() }

                    if (!rawArt.isNullOrBlank()) {
                        // Upgrade 100x100 to 800x800 for pristine display quality
                        val highRes = rawArt
                            .replace("100x100bb", "800x800bb")
                            .replace("60x60bb", "800x800bb")
                            .replace("http://", "https://")
                        memoryCache[cacheKey] = highRes
                        Log.d(TAG, "Successfully fetched high-res artwork for '$query': $highRes")
                        return@withContext highRes
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch artwork from iTunes for '$artist - $title': ${e.message}")
        }

        return@withContext null
    }
}
