package com.vizualx.app.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.vizualx.app.context.EventPriority

class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun triggerFeedback(priority: EventPriority) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (priority) {
                EventPriority.CRITICAL -> {
                    // Urgent double pulse: vibrate 200ms, pause 100ms, vibrate 300ms
                    val timings = longArrayOf(0, 200, 100, 300)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
                EventPriority.HIGH -> {
                    // Single crisp alert: 150ms
                    vib.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                EventPriority.NORMAL -> {
                    // Subtle tick: 50ms
                    vib.vibrate(VibrationEffect.createOneShot(50, 80))
                }
                EventPriority.LOW, EventPriority.IGNORE -> {
                    // Silent
                }
            }
        } else {
            @Suppress("DEPRECATION")
            when (priority) {
                EventPriority.CRITICAL -> vib.vibrate(500)
                EventPriority.HIGH -> vib.vibrate(200)
                EventPriority.NORMAL -> vib.vibrate(50)
                EventPriority.LOW, EventPriority.IGNORE -> {}
            }
        }
    }
}
