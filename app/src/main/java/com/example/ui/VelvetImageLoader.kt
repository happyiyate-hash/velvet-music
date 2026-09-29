package com.example.ui

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers

/**
 * High-Performance ImageLoader for Velvet:
 * - 25% Memory Cache of available heap for storing decoded downsampled thumbnails.
 * - 50MB Disk Cache for offline and persistent cover art caching.
 * - Hardware bitmaps enabled for zero-copy GPU texturing.
 * - Asynchronous decoding strictly on Dispatchers.IO.
 */
object VelvetImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: buildLoader(context.applicationContext).also {
                instance = it
                Coil.setImageLoader(it)
            }
        }
    }

    private fun buildLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("velvet_art_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .dispatcher(Dispatchers.IO)
            .interceptorDispatcher(Dispatchers.IO)
            .build()
    }

    fun isInMemory(context: Context, key: String): Boolean {
        val loader = get(context)
        val memoryCache = loader.memoryCache ?: return false
        val memoryKey = MemoryCache.Key(key)
        return memoryCache[memoryKey] != null
    }
}
