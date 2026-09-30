package com.example.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * YouTube Music Style Ultra-Fast Artwork Engine:
 *
 * 1. Frame-0 In-Memory Cache: Keeps decoded 128x128 thumbnails in RAM so returning
 *    to previously visible songs never displays a placeholder or re-decodes.
 * 2. Persistent On-Disk Thumbnail Cache: Extracted embedded album art is saved directly
 *    as a fast JPEG file in cacheDir/velvet_art/art_{track.id}.jpg.
 * 3. Native Android 10+ (Q) MediaStore Hardware Thumbnail Support:
 *    Avoids deprecated "content://media/external/audio/albumart" entirely.
 * 4. Fallback Guard: If a device audio track has no embedded artwork, assigns a rich
 *    photo from FallbackArtworkPool once and remembers it, so MediaStore is never
 *    queried repeatedly for missing art.
 */
object VelvetArtworkCache {
    private const val THUMB_SIZE = 128

    // Memory cache holding up to 300 thumbnail bitmaps (~15MB RAM max)
    private val memoryCache = object : LruCache<String, Bitmap>(300) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    // Tracks confirmed to have no embedded audio artwork to avoid redundant extraction attempts
    private val noEmbeddedArtTracks = ConcurrentHashMap<String, Boolean>()

    fun getFromMemory(trackId: String): Bitmap? {
        return memoryCache.get(trackId)
    }

    fun putInMemory(trackId: String, bitmap: Bitmap) {
        memoryCache.put(trackId, bitmap)
    }

    /**
     * Resolves the artwork Bitmap synchronously from memory if available,
     * or asynchronously from local disk / device storage.
     */
    suspend fun getOrDecodeThumbnail(context: Context, track: Track): Bitmap = withContext(Dispatchers.IO) {
        // 1. In-memory check (0ms)
        val cached = memoryCache.get(track.id)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        // 2. Persistent disk cache check (1-2ms)
        val artDir = File(context.cacheDir, "velvet_art").apply { if (!exists()) mkdirs() }
        val diskFile = File(artDir, "art_${track.id}.jpg")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val diskBmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (diskBmp != null) {
                    memoryCache.put(track.id, diskBmp)
                    return@withContext diskBmp
                }
            } catch (_: Throwable) {}
        }

        // 3. Extract device audio embedded artwork if not already known to be absent
        if (noEmbeddedArtTracks[track.id] != true) {
            val extracted = extractDeviceArtwork(context, track)
            if (extracted != null) {
                // Save to disk cache for instant subsequent app launches
                try {
                    FileOutputStream(diskFile).use { out ->
                        extracted.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                } catch (_: Throwable) {}
                memoryCache.put(track.id, extracted)
                return@withContext extracted
            } else {
                noEmbeddedArtTracks[track.id] = true
            }
        }

        // 4. Guaranteed rich photo fallback from resource pool
        val fallbackRes = if (track.coverResId != 0) {
            track.coverResId
        } else {
            FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
        }
        val fallbackBmp = decodeResourceThumbnail(context, fallbackRes)
        memoryCache.put(track.id, fallbackBmp)
        return@withContext fallbackBmp
    }

    private fun extractDeviceArtwork(context: Context, track: Track): Bitmap? {
        val contentUriStr = track.contentUri ?: return null
        val contentUri = try { Uri.parse(contentUriStr) } catch (_: Throwable) { return null }

        // Method A: Android 10+ (Q) native loadThumbnail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thumb = context.contentResolver.loadThumbnail(
                    contentUri,
                    Size(THUMB_SIZE, THUMB_SIZE),
                    null
                )
                if (thumb != null) {
                    return scaleToThumbnail(thumb)
                }
            } catch (_: Throwable) {}
        }

        // Method B: MediaMetadataRetriever embeddedPicture (universal ID3 reader)
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(picture, 0, picture.size, opts)
                    var sample = 1
                    while (opts.outWidth / (sample * 2) >= THUMB_SIZE && opts.outHeight / (sample * 2) >= THUMB_SIZE) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val decoded = BitmapFactory.decodeByteArray(picture, 0, picture.size, decodeOpts)
                    if (decoded != null) {
                        return scaleToThumbnail(decoded)
                    }
                }
            } finally {
                retriever.release()
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun decodeResourceThumbnail(context: Context, resId: Int): Bitmap {
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        var sample = 1
        while (opts.outWidth / (sample * 2) >= THUMB_SIZE && opts.outHeight / (sample * 2) >= THUMB_SIZE) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bmp = BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
        return bmp ?: Bitmap.createBitmap(THUMB_SIZE, THUMB_SIZE, Bitmap.Config.RGB_565)
    }

    private fun scaleToThumbnail(source: Bitmap): Bitmap {
        if (source.width == THUMB_SIZE && source.height == THUMB_SIZE) return source
        val scaled = Bitmap.createScaledBitmap(source, THUMB_SIZE, THUMB_SIZE, true)
        if (scaled !== source && !source.isRecycled) {
            source.recycle()
        }
        return scaled
    }
}
