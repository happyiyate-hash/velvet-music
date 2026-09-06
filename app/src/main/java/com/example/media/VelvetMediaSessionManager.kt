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

    // Callbacks to the app's audio engine
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
                override fun onPlay() {
                    onPlayAction?.invoke()
                }

                override fun onPause() {
                    onPauseAction?.invoke()
                }

                override fun onSkipToNext() {
                    onNextAction?.invoke()
                }

                override fun onSkipToPrevious() {
                    onPreviousAction?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    onSeekAction?.invoke(pos)
                }
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

    /**
     * Updates the system media session metadata and lock screen notification.
     * This triggers the native Android 13+ dynamic lock screen player.
     */
    fun updatePlaybackState(
        context: Context,
        track: Track,
        isPlaying: Boolean,
        playbackPositionMs: Long
    ) {
        if (!isInitialized) {
            initialize(context)
        }

        val session = mediaSession ?: return
        val appContext = context.applicationContext

        // 1. Configure PlaybackState with all lock screen actions
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

        // 2. Configure MediaMetadata (Title, Artist, Album, Art Bitmap)
        val albumArtBitmap = loadTrackArtBitmap(appContext, track)
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title.substringBefore(" - "))
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs)

        if (albumArtBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtBitmap)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, albumArtBitmap)
        }
        session.setMetadata(metadataBuilder.build())

        // 3. Build Lock Screen Notification with Notification.MediaStyle
        val notification = buildLockScreenNotification(appContext, track, isPlaying, albumArtBitmap, session)
        try {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission might be pending on Android 13+
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
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val actionPrev = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            prevPendingIntent
        ).build()

        val actionToggle = Notification.Action.Builder(
            playPauseIcon,
            playPauseTitle,
            togglePendingIntent
        ).build()

        val actionNext = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            nextPendingIntent
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
            .setVisibility(Notification.VISIBILITY_PUBLIC) // Dynamic Lock Screen Control
            .setOngoing(isPlaying)
            .addAction(actionPrev)
            .addAction(actionToggle)
            .addAction(actionNext)
            .setStyle(mediaStyle)
            .build()
    }

    private fun loadTrackArtBitmap(context: Context, track: Track): Bitmap? {
        return try {
            if (track.coverResId != 0) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeResource(context.resources, track.coverResId, opts)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun dismissNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
