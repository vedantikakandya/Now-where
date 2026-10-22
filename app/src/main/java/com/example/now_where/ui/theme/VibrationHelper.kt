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
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                // Very strong crescendo waveform: 3 heavy pulses (400ms, 600ms, 800ms)
                val timings = longArrayOf(0, 400, 150, 600, 150, 800)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttributes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Performs subtle touch haptic feedback for user UI interaction using direct Vibrator service.
     * ONLY fires if enabled is true.
     */
    fun performTouchHaptic(context: Context?, enabled: Boolean) {
        if (!enabled || context == null) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()

                val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttributes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
