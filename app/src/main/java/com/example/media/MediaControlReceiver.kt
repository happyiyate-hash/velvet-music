package com.example.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            VelvetMediaSessionManager.ACTION_TOGGLE_PLAY -> {
                VelvetMediaSessionManager.onPlayAction?.let {
                    // Toggle play/pause
                    if (VelvetMediaSessionManager.onPauseAction != null) {
                        VelvetMediaSessionManager.onPlayAction?.invoke()
                    }
                }
            }
            VelvetMediaSessionManager.ACTION_PLAY -> {
                VelvetMediaSessionManager.onPlayAction?.invoke()
            }
            VelvetMediaSessionManager.ACTION_PAUSE -> {
                VelvetMediaSessionManager.onPauseAction?.invoke()
            }
            VelvetMediaSessionManager.ACTION_NEXT -> {
                VelvetMediaSessionManager.onNextAction?.invoke()
            }
            VelvetMediaSessionManager.ACTION_PREVIOUS -> {
                VelvetMediaSessionManager.onPreviousAction?.invoke()
            }
        }
    }
}
