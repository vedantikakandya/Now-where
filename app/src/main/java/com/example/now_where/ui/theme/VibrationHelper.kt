package com.example.now_where.ui.theme

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object VibrationHelper {

    /**
     * Triggers a VERY STRONG vibration when an event is reached.
     * Uses USAGE_ALARM to ensure it fires strongly regardless of user profile settings
     * or system touch vibration settings.
     */
    fun triggerStrongEventVibration(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                val timings = longArrayOf(0, 400, 150, 600, 150, 800)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)

                val audioAttr = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val effect = if (vibrator.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackVibrate(context, longArrayOf(0, 400, 150, 600, 150, 800))
        }
    }

    /**
     * Performs touch haptic feedback for user UI interaction using both View haptics
     * and direct Vibrator service invocation.
     * ONLY fires if enabled is true.
     */
    fun performTouchHaptic(context: Context?, enabled: Boolean, view: View? = null) {
        if (!enabled || context == null) return

        // 1. View performHapticFeedback
        try {
            @Suppress("DEPRECATION")
            view?.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Direct Vibrator invocation as guaranteed hardware feedback
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                val audioAttr = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()

                val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllEffectsSupported(VibrationEffect.EFFECT_CLICK) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
                }

                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fallbackVibrate(context: Context, pattern: LongArray) {
        try {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            v?.vibrate(pattern, -1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
