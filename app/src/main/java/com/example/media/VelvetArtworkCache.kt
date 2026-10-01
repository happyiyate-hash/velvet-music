package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.Color
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, centralized device artwork caching and resolution engine for Velvet.
 *
 * Architecture:
 * 1. Dual-Tier Memory Cache:
 *    - HD Artwork Cache (36MB): high-resolution, sharp, unblurred bitmaps for player sheet.
 *    - Thumbnail Cache (16MB): clean 144x144 bitmaps for Up Next & lists.
 *    - Palette Cache: instant synchronous TrackThemeColors for zero-latency player theme styling.
 * 2. Persistent Disk Cache:
 *    - Automatically saves decoded device cover art to app cache directory.
 *    - Instant cold-start loading from disk file without querying MediaMetadataRetriever repeatedly.
 * 3. Asynchronous Prefetching:
 *    - Library tracks & active queue items are prefetched in the background on Dispatchers.IO.
 *    - When the user skips track ("nests the music"), artwork displays INSTANTLY from memory.
 * 4. Decoupled UI Rendering:
 *    - Scrolling never waits for artwork; artwork softly floats/fades in like YouTube Music.
 */
object VelvetArtworkCache {
    private const val TAG = "VelvetArtworkCache"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Memory caches
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val hdCacheSize = (maxMemory / 8).coerceIn(16 * 1024, 48 * 1024) // ~32MB
    private val thumbCacheSize = (maxMemory / 16).coerceIn(8 * 1024, 20 * 1024) // ~16MB

    private val hdBitmapCache = object : LruCache<String, Bitmap>(hdCacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / 1024
    }

    private val thumbBitmapCache = object : LruCache<String, Bitmap>(thumbCacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / 1024
    }

    private val colorsCache = ConcurrentHashMap<String, TrackThemeColors>()

    fun getArtworkBitmap(trackId: String): Bitmap? = hdBitmapCache.get(trackId)
    fun getThumbnailBitmap(trackId: String): Bitmap? = thumbBitmapCache.get(trackId)

    fun getThemeColors(context: Context, track: Track): TrackThemeColors {
        val cached = colorsCache[track.id]
        if (cached != null) return cached

        // Trigger async extraction and return instant default palette
        observeThemeColors(context, track) {}
        return ArtworkColorExtractor.generateThemePalette(track.dominantColor)
    }

    fun observeThemeColors(context: Context, track: Track, onColorsReady: (TrackThemeColors) -> Unit) {
        val cached = colorsCache[track.id]
        if (cached != null) {
            onColorsReady(cached)
            return
        }
        scope.launch {
            val bitmap = getOrResolveTrackBitmap(context, track, isThumbnail = false)
            val extracted = if (bitmap != null) {
                ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
            } else {
                ArtworkColorExtractor.generateThemePalette(track.dominantColor)
            }
            colorsCache[track.id] = extracted
            withContext(Dispatchers.Main) {
                onColorsReady(extracted)
            }
        }
    }

    fun loadArtworkAsync(
        context: Context,
        track: Track,
        isThumbnail: Boolean = false,
        onLoaded: (Bitmap) -> Unit
    ) {
        val mem = if (isThumbnail) thumbBitmapCache.get(track.id) else hdBitmapCache.get(track.id)
        if (mem != null && !mem.isRecycled) {
            onLoaded(mem)
            return
        }

        scope.launch {
            val bitmap = getOrResolveTrackBitmap(context, track, isThumbnail)
            if (bitmap != null && !bitmap.isRecycled) {
                withContext(Dispatchers.Main) {
                    onLoaded(bitmap)
                }
            }
        }
    }

    fun getOrResolveTrackBitmap(context: Context, track: Track, isThumbnail: Boolean): Bitmap? {
        val key = track.id
        val mem = if (isThumbnail) thumbBitmapCache.get(key) else hdBitmapCache.get(key)
        if (mem != null && !mem.isRecycled) return mem

        // Check if HD is available and we only need thumbnail
        if (isThumbnail) {
            val hd = hdBitmapCache.get(key)
            if (hd != null && !hd.isRecycled) {
                val thumb = Bitmap.createScaledBitmap(hd, 144, 144, true)
                thumbBitmapCache.put(key, thumb)
                return thumb
            }
        }

        // Check disk cache
        val diskFile = getDiskCacheFile(context, track.id, isThumbnail)
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bmp != null) {
                    if (isThumbnail) thumbBitmapCache.put(key, bmp) else hdBitmapCache.put(key, bmp)
                    return bmp
                }
            } catch (_: Exception) {}
        }

        // Decode from device sources cleanly (no aggressive blur or downsampling)
        val decoded = decodeDeviceArtwork(context, track, isThumbnail)
        if (decoded != null) {
            val finalBmp = if (isThumbnail) {
                Bitmap.createScaledBitmap(decoded, 144, 144, true)
            } else {
                // If larger than 900x900, scale down cleanly with bilinear filtering to preserve crispness
                if (decoded.width > 900 || decoded.height > 900) {
                    val maxDim = maxOf(decoded.width, decoded.height)
                    val scale = 900f / maxDim
                    Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
                } else {
                    decoded
                }
            }

            if (isThumbnail) {
                thumbBitmapCache.put(key, finalBmp)
            } else {
                hdBitmapCache.put(key, finalBmp)
                // Also create and store thumbnail from this HD bitmap
                val thumb = Bitmap.createScaledBitmap(finalBmp, 144, 144, true)
                thumbBitmapCache.put(key, thumb)
                saveBitmapToDisk(thumb, getDiskCacheFile(context, track.id, isThumbnail = true))
            }

            saveBitmapToDisk(finalBmp, diskFile)
            return finalBmp
        }

        return null
    }

    private fun decodeDeviceArtwork(context: Context, track: Track, isThumbnail: Boolean): Bitmap? {
        // 1. Android Q+ (API 29+) loadThumbnail from contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !track.contentUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(track.contentUri)
                val targetSize = if (isThumbnail) Size(160, 160) else Size(800, 800)
                val bmp = context.contentResolver.loadThumbnail(uri, targetSize, null)
                if (bmp != null) return bmp
            } catch (_: Throwable) {}
        }

        // 2. ID3 embedded picture from audio file
        if (!track.contentUri.isNullOrBlank()) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(track.contentUri))
                val pic = retriever.embeddedPicture
                retriever.release()
                if (pic != null && pic.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeByteArray(pic, 0, pic.size, opts)
                    if (bmp != null) return bmp
                }
            } catch (_: Throwable) {}
        }

        // 3. Track artworkUri if present and openable
        if (!track.artworkUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(track.artworkUri)
                val stream = context.contentResolver.openInputStream(uri)
                if (stream != null) {
                    val bmp = stream.use { BitmapFactory.decodeStream(it) }
                    if (bmp != null) return bmp
                }
            } catch (_: Throwable) {}
        }

        // 4. Fallback pool artwork - unblurred and sharp (inSampleSize = 1)
        val fallbackRes = if (track.coverResId != 0) {
            track.coverResId
        } else {
            FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
        }
        return try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = 1 // UNBLURRED: crisp 1:1 original resolution
            }
            BitmapFactory.decodeResource(context.resources, fallbackRes, opts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun getDiskCacheFile(context: Context, trackId: String, isThumbnail: Boolean): File {
        val dir = File(context.cacheDir, "velvet_art_store").apply { if (!exists()) mkdirs() }
        val prefix = if (isThumbnail) "art_thumb_" else "art_hd_"
        val safeId = trackId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        return File(dir, "$prefix$safeId.png")
    }

    private fun saveBitmapToDisk(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
        } catch (_: Exception) {}
    }

    fun prefetchTracks(context: Context, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            for (track in tracks) {
                try {
                    getOrResolveTrackBitmap(context, track, isThumbnail = true)
                    getOrResolveTrackBitmap(context, track, isThumbnail = false)
                } catch (_: Throwable) {}
            }
        }
    }
}
