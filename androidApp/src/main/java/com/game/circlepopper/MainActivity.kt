package com.game.circlepopper

import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.game.circlepopper.game.CirclePopperApp
import com.game.circlepopper.game.GameViewModel
import com.game.circlepopper.game.platform.AndroidMusicController
import com.game.circlepopper.game.platform.AndroidSoundController
import com.game.circlepopper.game.platform.AndroidStorageController
import com.game.circlepopper.game.platform.AndroidVibrationController

class MainActivity : ComponentActivity() {

    private val vibrationController by lazy {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)!!
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)!!
        }
        AndroidVibrationController(vibrator)
    }

    private val soundController by lazy {
        AndroidSoundController(this, R.raw.plink_wall, R.raw.plink_circle, R.raw.boop, R.raw.boom)
    }
    private val musicController by lazy { AndroidMusicController(this, R.raw.menu_music) }
    private val storageController by lazy { AndroidStorageController(this) }
    private val viewModel by lazy { GameViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            CirclePopperApp(
                viewModel = viewModel,
                vibrationController = vibrationController,
                soundController = soundController,
                musicController = musicController,
                storageController = storageController
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CirclePopper", "MainActivity.onResume")
        hideSystemBars()
        viewModel.resumeGame()
        if (!viewModel.state.value.isPlaying && viewModel.state.value.settingsMenuMusic) {
            musicController.resumeMenuMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("CirclePopper", "MainActivity.onPause")
        musicController.pauseMenuMusic()
        viewModel.pauseGame()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
}
