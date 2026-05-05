package com.game.circlepopper.game

import androidx.compose.ui.graphics.Color

data class GameCircle(
    val id: Long,
    val centerX: Float,
    val centerY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val accelX: Float,
    val accelY: Float,
    val radius: Float,
    val color: Color,
    val createdAt: Long,
    val isBonus: Boolean = false
)

data class GameState(
    val circles: List<GameCircle> = emptyList(),
    val score: Int = 0,
    val misses: Int = 0,
    val gameStartTime: Long = 0L,
    val slowMotionEndTime: Long = 0L,
    val highscores: List<Highscore> = emptyList(),
    val isHighscoreQualifying: Boolean = false,
    val highscoreSaved: Boolean = false,
    val showHighscoreList: Boolean = false,
    val showGameOverOverlay: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isPlaying: Boolean = false
)
