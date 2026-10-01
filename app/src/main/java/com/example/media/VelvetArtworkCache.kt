package com.example.media

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
 * YouTube Music Style High-Fidelity Artwork Engine:
 *
 * 1. Full-Resolution Crisp Artwork (for Player Sheet):
 *    - Decodes original embedded ID3 album art in full clarity (up to 1080x1080) with zero blur.
 *    - Cached in `fullMemoryCache` so the player sheet always displays razor-sharp, crystal-clean album art.
 * 2. Ultra-Fast Thumbnail Engine (for Music Lists & Up Next Queue):
 *    - Decodes downscaled 128x128 thumbnails for instant, smooth 60fps scrolling without memory pressure.
 * 3. Dual-Layer Persistence:
 *    - Thumbnail disk cache: `cacheDir/velvet_art/art_{id}.jpg`
 *    - Full-resolution disk cache: `cacheDir/velvet_art/art_full_{id}.jpg`
 */
object VelvetArtworkCache {
    private const val THUMB_SIZE = 128
    private const val FULL_ARTWORK_MAX_SIZE = 1200

    // Memory cache for list/queue thumbnails (up to 300 thumbnails ~15MB RAM)
    private val thumbnailMemoryCache = object : LruCache<String, Bitmap>(300) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    // Memory cache for full-resolution player artwork (holds up to 15 HD album covers)
    private val fullMemoryCache = object : LruCache<String, Bitmap>(15) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    // Tracks confirmed to have no embedded audio artwork to avoid redundant extraction attempts
    private val noEmbeddedArtTracks = ConcurrentHashMap<String, Boolean>()

    fun getFromMemory(trackId: String): Bitmap? = thumbnailMemoryCache.get(trackId)
    fun putInMemory(trackId: String, bitmap: Bitmap) {
        thumbnailMemoryCache.put(trackId, bitmap)
    }

    fun getFullFromMemory(trackId: String): Bitmap? = fullMemoryCache.get(trackId)
    fun putFullInMemory(trackId: String, bitmap: Bitmap) {
        fullMemoryCache.put(trackId, bitmap)
    }

    /**
     * Resolves the FULL-RESOLUTION crisp artwork for the player sheet (zero blur, original quality).
     */
    suspend fun getOrDecodeFullArtwork(context: Context, track: Track): Bitmap = withContext(Dispatchers.IO) {
        // 1. In-memory HD check (0ms)
        val cached = fullMemoryCache.get(track.id)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        // 2. Persistent full-res disk cache check
        val artDir = File(context.cacheDir, "velvet_art").apply { if (!exists()) mkdirs() }
        val fullDiskFile = File(artDir, "art_full_${track.id}.jpg")
        if (fullDiskFile.exists() && fullDiskFile.length() > 0) {
            try {
                val diskBmp = BitmapFactory.decodeFile(fullDiskFile.absolutePath)
                if (diskBmp != null) {
                    fullMemoryCache.put(track.id, diskBmp)
                    return@withContext diskBmp
                }
            } catch (_: Throwable) {}
        }

        // 3. Extract original embedded ID3 artwork from device audio file at full HD clarity
        if (noEmbeddedArtTracks[track.id] != true) {
            val fullExtracted = extractDeviceArtworkFull(context, track)
            if (fullExtracted != null) {
                try {
                    FileOutputStream(fullDiskFile).use { out ->
                        fullExtracted.compress(Bitmap.CompressFormat.JPEG, 94, out)
                    }
                } catch (_: Throwable) {}
                fullMemoryCache.put(track.id, fullExtracted)
                return@withContext fullExtracted
            } else {
                noEmbeddedArtTracks[track.id] = true
            }
        }

        // 4. Guaranteed crisp fallback photo from resource pool at full resolution
        val fallbackRes = if (track.coverResId != 0) {
            track.coverResId
        } else {
            FallbackArtworkPool.getPhotoForTrack(track.id, track.title, track.artist)
        }
        val fullFallbackBmp = decodeResourceFull(context, fallbackRes)
        fullMemoryCache.put(track.id, fullFallbackBmp)
        return@withContext fullFallbackBmp
    }

    /**
     * Resolves downsampled 128x128 thumbnail for lists and queue items.
     */
    suspend fun getOrDecodeThumbnail(context: Context, track: Track): Bitmap = withContext(Dispatchers.IO) {
        // 1. In-memory check (0ms)
        val cached = thumbnailMemoryCache.get(track.id)
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
                    thumbnailMemoryCache.put(track.id, diskBmp)
                    return@withContext diskBmp
                }
            } catch (_: Throwable) {}
        }

        // 3. Extract device audio embedded artwork if not already known to be absent
        if (noEmbeddedArtTracks[track.id] != true) {
            val extracted = extractDeviceArtworkThumb(context, track)
            if (extracted != null) {
                try {
                    FileOutputStream(diskFile).use { out ->
                        extracted.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                } catch (_: Throwable) {}
                thumbnailMemoryCache.put(track.id, extracted)
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
        thumbnailMemoryCache.put(track.id, fallbackBmp)
        return@withContext fallbackBmp
    }

    private fun extractDeviceArtworkFull(context: Context, track: Track): Bitmap? {
        val contentUriStr = track.contentUri ?: return null
        val contentUri = try { Uri.parse(contentUriStr) } catch (_: Throwable) { return null }

        // Method A: Android 10+ (Q) native loadThumbnail at HD resolution (1080x1080)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thumb = context.contentResolver.loadThumbnail(
                    contentUri,
                    Size(FULL_ARTWORK_MAX_SIZE, FULL_ARTWORK_MAX_SIZE),
                    null
                )
                if (thumb != null) {
                    return thumb
                }
            } catch (_: Throwable) {}
        }

        // Method B: MediaMetadataRetriever embeddedPicture (crystal-clear original ID3 image)
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
                    while (opts.outWidth / (sample * 2) >= FULL_ARTWORK_MAX_SIZE && opts.outHeight / (sample * 2) >= FULL_ARTWORK_MAX_SIZE) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    return BitmapFactory.decodeByteArray(picture, 0, picture.size, decodeOpts)
                }
            } finally {
                retriever.release()
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun extractDeviceArtworkThumb(context: Context, track: Track): Bitmap? {
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

        // Method B: MediaMetadataRetriever embeddedPicture
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

    private fun decodeResourceFull(context: Context, resId: Int): Bitmap {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = 1
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
            ?: Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
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
