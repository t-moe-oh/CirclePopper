package com.game.circlepopper.game.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.game.circlepopper.game.GameViewModel

@Composable
actual fun SensorEffect(viewModel: GameViewModel) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (sensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    viewModel.setTilt(event.values[0], event.values[1])
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }
}
