package com.example.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.R
import com.example.model.DeviceVideo
import com.example.model.FallbackArtworkPool
import com.example.model.Track
import com.example.ui.theme.VelvetDarkBurgundy
import com.example.ui.theme.VelvetDeepCrimson
import java.io.File

object DeviceMediaManager {

    val requiredAudioPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    val requiredVideoPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    val allMediaPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasAudioPermission(context: Context): Boolean {
        return requiredAudioPermissions.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                perm
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasVideoPermission(context: Context): Boolean {
        return requiredVideoPermissions.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                perm
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAllMediaPermissions(context: Context): Boolean {
        return allMediaPermissions.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                perm
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun getCachedTracks(context: Context): List<Track> {
        return MediaLibraryCache.loadCachedTracks(context)
    }

    fun loadDeviceTracks(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Track"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Device Audio"
                    val durationMs = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
                    val albumId = if (albumIdColumn != -1) cursor.getLong(albumIdColumn) else -1L
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val cleanArtist = if (artist.contains("<unknown>", ignoreCase = true)) "Device Audio" else artist
                    val trackId = "device_audio_$id"

                    // Strict Artwork Verification:
                    // 1. Never assume albumart belongs to this song. Only assign artworkUri if the track
                    //    actually possesses its own verifiable embedded picture or valid albumart.
                    // 2. If a track has no artwork of its own, artworkUri remains null so it safely
                    //    receives a dedicated distributed fallback image without borrowing another song's art.
                    val resolvedArtUri = resolveArtwork(context, contentUri, albumId, trackId)

                    // Distribute fallback covers across tracks without genuine artwork
                    val cover = if (resolvedArtUri != null) {
                        R.drawable.art_luminous_echoes
                    } else {
                        FallbackArtworkPool.getDistributedPhoto(tracks.size, trackId)
                    }

                    tracks.add(
                        Track(
                            id = trackId,
                            title = title,
                            artist = cleanArtist,
                            album = album,
                            durationMs = if (durationMs > 0) durationMs else 180000L,
                            coverResId = cover,
                            dominantColor = VelvetDarkBurgundy,
                            secondaryColor = VelvetDeepCrimson,
                            catalogSource = "Device Storage",
                            dateAddedMs = dateAdded,
                            contentUri = contentUri.toString(),
                            artworkUri = resolvedArtUri
                        )
                    )
                }
            }
            if (tracks.isNotEmpty()) {
                MediaLibraryCache.saveCachedTracks(context, tracks)
            }
        } catch (e: Exception) {
            Log.e("DeviceMediaManager", "Error querying audio MediaStore", e)
        }
        return tracks
    }

    private fun resolveArtwork(
        context: Context,
        contentUri: Uri,
        albumId: Long,
        trackId: String
    ): String? {
        // 1. Try embedded picture from ID3 tag in this specific audio file (most authentic & exclusive)
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, contentUri)
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    val cacheDir = File(context.cacheDir, "art_cache").apply { mkdirs() }
                    val artFile = File(cacheDir, "art_${trackId}.jpg")
                    if (!artFile.exists() || artFile.length() == 0L) {
                        artFile.outputStream().use { it.write(picture) }
                    }
                    return Uri.fromFile(artFile).toString()
                }
            } finally {
                retriever.release()
            }
        } catch (_: Exception) {}

        // 2. Android 10+ (Q) loadThumbnail from contentResolver for this exact track
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bmp = context.contentResolver.loadThumbnail(contentUri, android.util.Size(512, 512), null)
                val cacheDir = File(context.cacheDir, "art_cache").apply { mkdirs() }
                val artFile = File(cacheDir, "art_${trackId}.jpg")
                if (!artFile.exists() || artFile.length() == 0L) {
                    artFile.outputStream().use { out ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
                    }
                }
                return Uri.fromFile(artFile).toString()
            } catch (_: Exception) {}
        }

        // 3. Try MediaStore album art URI only if albumId is valid and stream genuinely readable
        if (albumId > 0) {
            val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
            try {
                context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                    val b = stream.read()
                    if (b != -1) {
                        return albumArtUri.toString()
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Creates a Track when a user picks an audio file directly from device storage or downloads.
     */
    fun createTrackFromUri(context: Context, uri: Uri): Track? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Device Audio File"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Local Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "Device Storage"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 180000L
            val id = "imported_${System.currentTimeMillis()}_${(0..999).random()}"

            var resolvedArtUri: String? = null
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                val cacheDir = File(context.cacheDir, "art_cache").apply { mkdirs() }
                val artFile = File(cacheDir, "art_${id}.jpg")
                artFile.outputStream().use { it.write(picture) }
                resolvedArtUri = Uri.fromFile(artFile).toString()
            }
            retriever.release()

            val cover = if (resolvedArtUri == null) {
                FallbackArtworkPool.getPhotoForTrack(id, title, artist)
            } else {
                R.drawable.art_luminous_echoes
            }
            val colors = if (resolvedArtUri != null) {
                ArtworkColorExtractor.extractColorsFromUri(context, resolvedArtUri)
            } else {
                ArtworkColorExtractor.getColorsForDrawable(context, cover)
            }

            Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                coverResId = cover,
                dominantColor = colors.dominant,
                secondaryColor = colors.secondary,
                catalogSource = "Device Audio",
                contentUri = uri.toString(),
                artworkUri = resolvedArtUri,
                dateAddedMs = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("DeviceMediaManager", "Error reading audio file from URI: $uri", e)
            null
        }
    }

    fun getCachedVideos(context: Context): List<DeviceVideo> {
        val cached = MediaLibraryCache.loadCachedVideos(context)
        return if (cached.isNotEmpty()) cached else getSampleVideos()
    }

    private fun getSampleVideos(): List<DeviceVideo> {
        return listOf(
            DeviceVideo(
                id = "sample_vid_1",
                title = "Velvet_Concert_Live_2026.mp4",
                durationMs = 248000L,
                sizeBytes = 184500000L,
                resolution = "4K UHD",
                dateAddedMs = System.currentTimeMillis() - 86400000L * 2
            ),
            DeviceVideo(
                id = "sample_vid_2",
                title = "Night_Grooves_AudioVisualizer.mp4",
                durationMs = 192000L,
                sizeBytes = 96200000L,
                resolution = "1080p 60fps",
                dateAddedMs = System.currentTimeMillis() - 86400000L * 4
            ),
            DeviceVideo(
                id = "sample_vid_3",
                title = "Studio_Session_Acoustic_Take_3.mov",
                durationMs = 310000L,
                sizeBytes = 320000000L,
                resolution = "1080p",
                dateAddedMs = System.currentTimeMillis() - 86400000L * 7
            ),
            DeviceVideo(
                id = "sample_vid_4",
                title = "Screen_Recording_SoundCatch_Wave.mp4",
                durationMs = 85000L,
                sizeBytes = 42000000L,
                resolution = "FHD",
                dateAddedMs = System.currentTimeMillis() - 86400000L * 10
            )
        )
    }

    fun loadDeviceVideos(context: Context): List<DeviceVideo> {
        val videos = mutableListOf<DeviceVideo>()
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.RESOLUTION,
                MediaStore.Video.Media.DATE_ADDED
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val resCol = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Video $id"
                    val durationMs = cursor.getLong(durationCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val resolution = if (resCol != -1) cursor.getString(resCol) ?: "1080p" else "1080p"
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    videos.add(
                        DeviceVideo(
                            id = "device_vid_$id",
                            title = title,
                            durationMs = if (durationMs > 0) durationMs else 125000L,
                            sizeBytes = sizeBytes,
                            resolution = resolution,
                            contentUri = uri.toString(),
                            dateAddedMs = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceMediaManager", "Error querying video MediaStore", e)
        }

        if (videos.isNotEmpty()) {
            MediaLibraryCache.saveCachedVideos(context, videos)
        } else {
            videos.addAll(getSampleVideos())
        }
        return videos
    }

    fun shareTrack(context: Context, track: Track) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Listening to ${track.title}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out \"${track.title}\" by ${track.artist} on Velvet Audio!"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
    }
}
