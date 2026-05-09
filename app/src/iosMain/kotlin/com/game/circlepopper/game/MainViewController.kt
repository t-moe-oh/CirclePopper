package com.game.circlepopper.game

import androidx.compose.ui.window.ComposeUIViewController
import com.game.circlepopper.game.platform.IosMusicController
import com.game.circlepopper.game.platform.IosSoundController
import com.game.circlepopper.game.platform.IosStorageController
import com.game.circlepopper.game.platform.IosVibrationController

fun MainViewController() = ComposeUIViewController {
    val viewModel = GameViewModel()
    viewModel.setStorageController(IosStorageController())
    CirclePopperApp(
        viewModel = viewModel,
        vibrationController = IosVibrationController(),
        soundController = IosSoundController(),
        musicController = IosMusicController(),
        storageController = IosStorageController()
    )
}
