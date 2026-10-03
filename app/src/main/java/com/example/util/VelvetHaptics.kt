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
     * Subtle, pleasant vibration when holding/activating a queue item to drag (fires once).
     */
    fun dragActivated(context: Context, view: View? = null) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Throwable) {}

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Subtle micro-tick when crossing the swipe threshold.
     */
    fun thresholdCrossed(context: Context, view: View? = null) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Throwable) {}

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 80))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crisp subtle confirmation when an action is executed.
     */
    fun actionExecuted(context: Context, view: View? = null) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        } catch (_: Throwable) {}

        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
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
