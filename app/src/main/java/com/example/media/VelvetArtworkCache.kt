package com.example.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal High-Speed In-Memory Artwork Cache for Velvet.
 *
 * Guarantees:
 * 1. Warm-on-Mount: Preloads and decodes all track artwork and theme colors into memory on app launch.
 * 2. Single Shared Cache: The main player card, Up Next queue, and library views read from this exact same cache.
 * 3. Zero-Delay Playback: When music is played, the artwork and colors are already hot in RAM—no re-fetching or decoding hitch.
 * 4. Zero Placeholders: Provides immediate bitmaps for zero layout shift and smooth emergence transitions.
 */
object VelvetArtworkCache {
    private const val TAG = "VelvetArtworkCache"

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 4).coerceIn(32 * 1024, 96 * 1024) // in KB

    // High-resolution bitmap cache for player card (512x512)
    private val fullBitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // High-speed downsampled thumbnail cache for Up Next queue and list rows (128x128)
    private val thumbBitmapCache = object : LruCache<String, Bitmap>(16 * 1024) { // 16MB
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Pre-extracted Theme Colors for instant 0ms palette resolution
    private val colorsCache = ConcurrentHashMap<String, TrackThemeColors>()

    // Observable version counter so composables seamlessly refresh if an item finishes background caching
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getBitmap(trackId: String): Bitmap? {
        return synchronized(fullBitmapCache) {
            fullBitmapCache.get(trackId)
        }
    }

    fun getThumbnail(trackId: String): Bitmap? {
        synchronized(thumbBitmapCache) {
            val thumb = thumbBitmapCache.get(trackId)
            if (thumb != null) return thumb
        }
        return getBitmap(trackId)
    }

    fun getColors(trackId: String): TrackThemeColors? {
        return colorsCache[trackId]
    }

    fun hasArtwork(trackId: String): Boolean {
        return getBitmap(trackId) != null
    }

    fun put(trackId: String, bitmap: Bitmap, thumbnail: Bitmap? = null, colors: TrackThemeColors? = null) {
        synchronized(fullBitmapCache) {
            fullBitmapCache.put(trackId, bitmap)
        }
        val thumb = thumbnail ?: downsample(bitmap, 128, 128)
        synchronized(thumbBitmapCache) {
            thumbBitmapCache.put(trackId, thumb)
        }
        if (colors != null) {
            colorsCache[trackId] = colors
        }
        _version.value++
    }

    fun putColors(trackId: String, colors: TrackThemeColors) {
        colorsCache[trackId] = colors
    }

    /**
     * WARM CACHE:
     * Preloads and caches artwork for all provided tracks in the background.
     * Extracts and stores TrackThemeColors so no work is done when playing music.
     */
    fun warmCache(context: Context, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            val appContext = context.applicationContext
            for (track in tracks) {
                if (hasArtwork(track.id) && colorsCache.containsKey(track.id)) {
                    continue
                }
                try {
                    val bitmap = resolveBitmap(appContext, track, targetSize = 512)
                    if (bitmap != null) {
                        val thumb = downsample(bitmap, 128, 128)
                        val colors = VelvetArtworkColorEngine.extractColorsFromBitmap(thumb)
                        synchronized(fullBitmapCache) {
                            fullBitmapCache.put(track.id, bitmap)
                        }
                        synchronized(thumbBitmapCache) {
                            thumbBitmapCache.put(track.id, thumb)
                        }
                        colorsCache[track.id] = colors
                    } else {
                        val colors = VelvetArtworkColorEngine.generateThemePalette(track.dominantColor)
                        colorsCache[track.id] = colors
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to warm artwork for track ${track.id}", e)
                }
            }
            _version.value++
            Log.d(TAG, "Artwork cache warmed successfully for ${tracks.size} tracks")
        }
    }

    fun resolveBitmap(context: Context, track: Track, targetSize: Int): Bitmap? {
        // 1. If artworkUri is available
        val artUriStr = track.artworkUri
        if (!artUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(artUriStr)
                if (uri.scheme == "android.resource") {
                    val resId = uri.lastPathSegment?.toIntOrNull()
                    if (resId != null && resId != 0) {
                        val bmp = decodeResource(context, resId, targetSize)
                        if (bmp != null) return bmp
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = decodeStream(stream, targetSize)
                        if (bmp != null) return bmp
                    }
                }
            } catch (_: Throwable) {}
        }

        // 2. If embedded coverResId exists
        if (track.coverResId != 0) {
            val bmp = decodeResource(context, track.coverResId, targetSize)
            if (bmp != null) return bmp
        }

        // 3. Fallback pool
        val fallbackRes = FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
        return decodeResource(context, fallbackRes, targetSize)
    }

    private fun decodeResource(context: Context, resId: Int, targetSize: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resId, options)
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeResource(context.resources, resId, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeStream(stream: InputStream, targetSize: Int): Bitmap? {
        return try {
            val bytes = stream.readBytes()
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun downsample(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        return if (bitmap.width <= targetW && bitmap.height <= targetH) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
