package com.example.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Universal High-Precision Haptic Engine:
 * Combines Android Hardware Vibrator with View-level tactile feedback
 * to guarantee instantaneous, crisp, physical vibration on all devices.
 */
object VelvetHaptics {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator ?: context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Instant, noticeable vibration burst when holding/activating a queue item to drag.
     */
    fun dragActivated(context: Context, view: View? = null) {
        // Channel 1: View-level haptic feedback
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or 2 // FLAG_IGNORE_GLOBAL_SETTING
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, flags)
        } catch (_: Throwable) {}

        // Channel 2: Direct Hardware Vibrator
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(55)
            }
        } catch (_: Throwable) {
            try {
                @Suppress("DEPRECATION")
                vibrator.vibrate(55)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Tactile punch when crossing the 50% threshold (screen center) into Delete (red) or Play Next (green).
     */
    fun thresholdCrossed(context: Context, view: View? = null) {
        // Channel 1: View-level haptic feedback
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or 2
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM, flags)
        } catch (_: Throwable) {}

        // Channel 2: Direct Hardware Vibrator
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Crisp double-pulse for unmistakable tactile threshold confirmation
                val timings = longArrayOf(0, 28, 40, 35)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 28, 40, 35), -1)
            }
        } catch (_: Throwable) {
            try {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Crisp confirmation pulse when an action is executed (item removed or moved to next).
     */
    fun actionExecuted(context: Context, view: View? = null) {
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or 2
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END, flags)
        } catch (_: Throwable) {}

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Standard UI click tactile feedback.
     */
    fun click(context: Context, view: View? = null) {
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or 2
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, flags)
        } catch (_: Throwable) {}

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, 140))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Throwable) {}
    }
}
