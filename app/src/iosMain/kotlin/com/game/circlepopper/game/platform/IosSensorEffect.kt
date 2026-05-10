package com.game.circlepopper.game.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.game.circlepopper.game.GameViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SensorEffect(viewModel: GameViewModel) {
    val motionManager = CMMotionManager()

    DisposableEffect(Unit) {
        motionManager.deviceMotionUpdateInterval = 0.1
        if (motionManager.isDeviceMotionAvailable()) {
            motionManager.startDeviceMotionUpdatesUsingReferenceFrame(
                referenceFrame = 0UL,
                toQueue = NSOperationQueue.mainQueue,
                withHandler = { motion, _ ->
                    if (motion != null) {
                        motion.gravity.useContents {
                            viewModel.setTilt(x.toFloat(), y.toFloat())
                        }
                    }
                }
            )
        }
        onDispose {
            motionManager.stopDeviceMotionUpdates()
        }
    }
}
