package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Custom Application class that configures global app-wide singletons:
 * - High-performance Coil ImageLoader with fine-tuned Memory and Disk caching
 * - Fast hardware bitmap rendering for ultra-smooth 60/120fps Compose scrolling
 */
class VelvetApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Allocate up to 25% of available app memory for instant image retrieval
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024) // 64 MB disk cache
                    .build()
            }
            // Allow hardware bitmaps for zero-copy direct GPU rendering
            .allowHardware(true)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .crossfade(false) // Disable list-level crossfade delays to eliminate scroll micro-stutters
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
            }
            .build()
    }
}
