package com.example.now_where.ui.theme

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {

    /**
     * Triggers a VERY STRONG vibration when an event is reached.
     * Uses USAGE_ALARM to ensure it fires strongly regardless of user profile settings
     * or system touch vibration settings.
     */
    fun triggerStrongEventVibration(context: Context) {
        try {
            val vibrator = getVibrator(context)?:return
            if (!vibrator.hasVibrator()) return

            val timings = longArrayOf(0, 400, 150, 600, 150, 800)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)

            val attrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, attrs)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Performs touch haptic feedback for user UI interaction using both View haptics
     * and direct Vibrator service invocation.
     * ONLY fires if enabled is true.
     */
    fun performTouchHaptic(context: Context?, enabled: Boolean) {
        if (!enabled || context == null) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            val attrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .build()

            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createOneShot(TOUCH_MS, TOUCH_AMPLITUDE)
            } else {
                VibrationEffect.createOneShot(TOUCH_MS, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, attrs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private const val TOUCH_MS = 30L
    private const val TOUCH_AMPLITUDE = 200 // 1..255


    private fun getVibrator(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }
}
