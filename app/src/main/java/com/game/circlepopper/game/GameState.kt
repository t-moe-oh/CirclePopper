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
    val isBonus: Boolean = false,
    val isBomb: Boolean = false,
    val trail: List<Pair<Float, Float>> = emptyList()
)

data class GameState(
    val circles: List<GameCircle> = emptyList(),
    val score: Int = 0,
    val misses: Int = 0,
    val gameStartTime: Long = 0L,
    val slowMotionEndTime: Long = 0L,
    val bombDodgeEndTime: Long = 0L,
    val bombSacrificeEndTime: Long = 0L,
    val highscores: List<Highscore> = emptyList(),
    val isHighscoreQualifying: Boolean = false,
    val highscoreSaved: Boolean = false,
    val showHighscoreList: Boolean = false,
    val showSettings: Boolean = false,
    val showDebugMenu: Boolean = false,
    val showTrails: Boolean = false,
    val trailLength: Int = 12,
    val showGameOverOverlay: Boolean = false,
    val isPaused: Boolean = false,
    val resumeCountdown: Int = 0,
    val settingsMenuMusic: Boolean = true,
    val settingsHaptics: Boolean = true,
    val settingsRealGravity: Boolean = true,
    val isGameOver: Boolean = false,
    val isPlaying: Boolean = false
)
