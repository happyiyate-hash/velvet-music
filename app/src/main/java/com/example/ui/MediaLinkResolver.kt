package com.example.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

/**
 * Resolves metadata only from a publicly reachable URL. It deliberately does not
 * bypass authentication, DRM, private-content checks, or platform protections.
 */
data class ResolvedMedia(
    val platform: String,
    val title: String,
    val creator: String,
    val thumbnailUrl: String?,
    val mediaUrl: String?,
    val audioUrl: String?,
    val duration: String?
)

class MediaLinkResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    suspend fun resolve(rawUrl: String): ResolvedMedia = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        require(url.startsWith("https://") || url.startsWith("http://")) { "Invalid URL" }
        require(!url.contains("youtube.com", true) && !url.contains("youtu.be", true)) {
            "YouTube downloading is not supported"
        }

        val host = URL(url).host.lowercase()
        val platform = when {
            host.contains("tiktok.com") -> "TikTok"
            host.contains("instagram.com") -> "Instagram"
            host.contains("facebook.com") || host.contains("fb.watch") -> "Facebook"
            else -> "Web"
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) throw IllegalStateException("Empty response")

            fun meta(vararg names: String): String? {
                for (name in names) {
                    val escaped = Regex.escape(name)
                    val patterns = listOf(
                        Regex("<meta[^>]+property=[\\\"']$escaped[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]*>", RegexOption.IGNORE_CASE),
                        Regex("<meta[^>]+name=[\\\"']$escaped[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]*>", RegexOption.IGNORE_CASE),
                        Regex("<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+property=[\\\"']$escaped[\\\"'][^>]*>", RegexOption.IGNORE_CASE)
                    )
                    patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.getOrNull(1) }?.let { return decode(it) }
                }
                return null
            }

            val title = meta("og:title", "twitter:title", "title") ?: "Media from $platform"
            val creator = meta("og:site_name", "twitter:creator", "author") ?: platform
            val image = meta("og:image", "twitter:image")
            val video = meta("og:video", "og:video:url", "twitter:player:stream")
            val audio = meta("og:audio")
            val duration = meta("video:duration", "music:duration")

            ResolvedMedia(platform, title, creator, image, video, audio, duration)
        }
    }

    private fun decode(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}
