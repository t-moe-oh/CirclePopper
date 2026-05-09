package com.game.circlepopper.game.platform

import androidx.compose.runtime.Composable
import com.game.circlepopper.game.GameViewModel

@Composable
expect fun SensorEffect(viewModel: GameViewModel)
