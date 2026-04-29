package com.game.circlepopper.game

import androidx.compose.ui.graphics.Color

data class GameCircle(
    val id: Long,
    val centerX: Float,
    val centerY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val radius: Float,
    val color: Color,
    val createdAt: Long
)

data class GameState(
    val circles: List<GameCircle> = emptyList(),
    val score: Int = 0,
    val misses: Int = 0,
    val isGameOver: Boolean = false,
    val isPlaying: Boolean = false
)
