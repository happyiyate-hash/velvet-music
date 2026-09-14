package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Track

object VelvetMediaSessionManager {

    const val CHANNEL_ID = "velvet_music_playback"
    const val NOTIFICATION_ID = 4040

    const val ACTION_PLAY = "com.example.ACTION_PLAY"
    const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
    const val ACTION_TOGGLE_PLAY = "com.example.ACTION_TOGGLE_PLAY"
    const val ACTION_NEXT = "com.example.ACTION_NEXT"
    const val ACTION_PREVIOUS = "com.example.ACTION_PREVIOUS"

    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null
    private var isInitialized = false

    var onPlayAction: (() -> Unit)? = null
    var onPauseAction: (() -> Unit)? = null
    var onNextAction: (() -> Unit)? = null
    var onPreviousAction: (() -> Unit)? = null
    var onSeekAction: ((Long) -> Unit)? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext

        notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(appContext)

        mediaSession = MediaSession(appContext, "VelvetMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { onPlayAction?.invoke() }
                override fun onPause() { onPauseAction?.invoke() }
                override fun onSkipToNext() { onNextAction?.invoke() }
                override fun onSkipToPrevious() { onPreviousAction?.invoke() }
                override fun onSeekTo(pos: Long) { onSeekAction?.invoke(pos) }
            })
            isActive = true
        }

        isInitialized = true
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Velvet Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active music playback and dynamic lock screen controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun updatePlaybackState(
        context: Context,
        track: Track,
        isPlaying: Boolean,
        playbackPositionMs: Long
    ) {
        if (!isInitialized) initialize(context)

        val session = mediaSession ?: return
        val appContext = context.applicationContext

        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = (
            PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            )

        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, playbackPositionMs, 1.0f)
            .build()
        session.setPlaybackState(playbackState)

        val fallbackBitmap = if (track.coverResId != 0) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeResource(appContext.resources, track.coverResId, opts)
            }.getOrNull()
        } else {
            null
        }
        val albumArtBitmap = ArtworkColorExtractor.resolveTrackBitmap(appContext, track)
            ?: fallbackBitmap
            ?: ArtworkColorExtractor.getDefaultBitmap(appContext)

        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title.substringBefore(" - "))
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs)

        val fallbackUri = if (track.coverResId != 0) {
            "android.resource://${appContext.packageName}/${track.coverResId}"
        } else null
        val effectiveArtUri = track.artworkUri?.takeIf { !it.startsWith("content://media/external/audio/media") } ?: fallbackUri

        if (effectiveArtUri != null) {
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_ART_URI, effectiveArtUri)
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, effectiveArtUri)
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, effectiveArtUri)
        }

        if (albumArtBitmap != null) {
            metadataBuilder
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtBitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, albumArtBitmap)
                .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, albumArtBitmap)
        }

        session.setMetadata(metadataBuilder.build())

        val notification = buildLockScreenNotification(appContext, track, isPlaying, albumArtBitmap, session)
        try {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun buildLockScreenNotification(
        context: Context,
        track: Track,
        isPlaying: Boolean,
        artBitmap: Bitmap?,
        session: MediaSession
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context, 2, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val actionPrev = Notification.Action.Builder(
            android.R.drawable.ic_media_previous, "Previous", prevPendingIntent
        ).build()
        val actionToggle = Notification.Action.Builder(
            playPauseIcon, playPauseTitle, togglePendingIntent
        ).build()
        val actionNext = Notification.Action.Builder(
            android.R.drawable.ic_media_next, "Next", nextPendingIntent
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title.substringBefore(" - "))
            .setContentText(track.artist)
            .setSubText(track.album)
            .setLargeIcon(artBitmap)
            .setContentIntent(contentPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(actionPrev)
            .addAction(actionToggle)
            .addAction(actionNext)
            .setStyle(mediaStyle)
            .build()
    }

    fun updateMediaSessionMetadata(title: String, artist: String, artworkBitmap: Bitmap) {
        val session = mediaSession ?: return
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artworkBitmap)
            .putBitmap(MediaMetadata.METADATA_KEY_ART, artworkBitmap)
            .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, artworkBitmap)
            .build()
        session.setMetadata(metadata)
    }

    private fun loadTrackArtBitmap(context: Context, track: Track): Bitmap {
        val fallback = if (track.coverResId != 0) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeResource(context.resources, track.coverResId, opts)
            }.getOrNull()
        } else {
            null
        }
        return ArtworkColorExtractor.resolveTrackBitmap(context, track)
            ?: fallback
            ?: ArtworkColorExtractor.getDefaultBitmap(context)
    }

    fun dismissNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
