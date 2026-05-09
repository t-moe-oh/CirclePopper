package com.game.circlepopper.game.platform

import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class AndroidVibrationController(private val vibrator: Vibrator?) : VibrationController {

    override fun vibrateLight() {
        val vib = vibrator ?: return
        Log.d("CirclePopper", "vibrateLight()")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION)
                .build()
            vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK), attrs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vib.vibrate(VibrationEffect.createOneShot(30L, 100))
        }
    }

    override fun vibrateStrong() {
        val vib = vibrator ?: return
        Log.d("CirclePopper", "vibrateStrong()")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION)
                .build()
            vib.vibrate(VibrationEffect.createOneShot(500L, 255), attrs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vib.vibrate(VibrationEffect.createOneShot(500L, 255))
        } else {
            vib.vibrate(VibrationEffect.createOneShot(500L, 255))
        }
    }
}
