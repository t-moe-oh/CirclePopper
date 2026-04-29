package com.game.circlepopper.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var nextId = 0L
    private var gameStartTime = 0L
    private var gameLoopJob: Job? = null

    private val highscoreManager = HighscoreManager(application)

    private val brightColors = listOf(
        Color(0xFFFF5733),
        Color(0xFF33FF57),
        Color(0xFF3357FF),
        Color(0xFFFF33A8),
        Color(0xFFFFD733),
        Color(0xFF33FFF5),
        Color(0xFFA833FF),
        Color(0xFFFF8C33),
        Color(0xFF33FF8C),
        Color(0xFFFF3368),
    )

    fun startGame(widthPx: Float, heightPx: Float) {
        screenWidth = widthPx
        screenHeight = heightPx
        nextId = 0L
        gameStartTime = System.currentTimeMillis()
        _state.value = GameState(isPlaying = true, gameStartTime = gameStartTime)
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            launch { spawnLoop() }
            launch { movementLoop() }
            launch { cleanupLoop() }
        }
    }

    private suspend fun spawnLoop() {
        while (currentCoroutineContext().isActive) {
            val interval = spawnInterval()
            delay(interval)
            if (currentCoroutineContext().isActive) spawnCircle()
        }
    }

    private suspend fun cleanupLoop() {
        while (currentCoroutineContext().isActive) {
            delay(100)
            cleanupExpiredCircles()
            if (_state.value.isGameOver) break
        }
    }

    private fun spawnCircle() {
        val minDim = min(screenWidth, screenHeight)
        val minRadius = minDim * 0.05f
        val maxRadius = minDim * 0.25f
        val radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius

        val x = Random.nextFloat() * (screenWidth - 2 * radius) + radius
        val y = Random.nextFloat() * (screenHeight - 2 * radius) + radius

        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val speed = Random.nextFloat() * 0.27f + 0.08f
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed

        val gravAngle = Random.nextFloat() * 2f * PI.toFloat()
        val gravStrength = Random.nextFloat() * 0.00028f + 0.00012f
        val ax = cos(gravAngle) * gravStrength
        val ay = sin(gravAngle) * gravStrength

        val color = brightColors.random()

        val circle = GameCircle(
            id = nextId++,
            centerX = x.coerceIn(radius, screenWidth - radius),
            centerY = y.coerceIn(radius, screenHeight - radius),
            velocityX = vx,
            velocityY = vy,
            accelX = ax,
            accelY = ay,
            radius = radius,
            color = color,
            createdAt = System.currentTimeMillis()
        )
        _state.update { it.copy(circles = it.circles + circle) }
    }

    private fun spawnInterval(): Long {
        val score = _state.value.score
        return maxOf(400L, 1500L - score * 30L)
    }

    private fun cleanupExpiredCircles() {
        val now = System.currentTimeMillis()
        val expired = _state.value.circles.filter { now - it.createdAt > 4000L }
        if (expired.isEmpty()) return

        _state.update { state ->
            val newMisses = state.misses + expired.size
            val gameOver = newMisses >= 5
            val highscores = if (gameOver) highscoreManager.getHighscores() else emptyList()
            val isQualifying = if (gameOver) highscoreManager.isQualifying(state.score) else false
            state.copy(
                circles = state.circles.filter { now - it.createdAt <= 4000L },
                misses = newMisses,
                isGameOver = gameOver,
                isPlaying = if (gameOver) false else state.isPlaying,
                highscores = highscores,
                isHighscoreQualifying = isQualifying
            )
        }

        if (_state.value.isGameOver) {
            gameLoopJob?.cancel()
        }
    }

    fun saveHighscore(name: String) {
        val score = _state.value.score
        val updated = highscoreManager.addHighscore(name, score)
        _state.update {
            it.copy(highscores = updated, highscoreSaved = true)
        }
    }

    fun showHighscoreList() {
        val highscores = highscoreManager.getHighscores()
        _state.update { it.copy(showHighscoreList = true, highscores = highscores) }
    }

    fun hideHighscoreList() {
        _state.update { it.copy(showHighscoreList = false) }
    }

    fun onTap(x: Float, y: Float) {
        val s = _state.value
        if (!s.isPlaying || s.isGameOver) return

        val hit = s.circles.find { circle ->
            val dx = x - circle.centerX
            val dy = y - circle.centerY
            dx * dx + dy * dy <= circle.radius * circle.radius
        }

        if (hit != null) {
            _state.update {
                it.copy(
                    circles = it.circles - hit,
                    score = it.score + 1
                )
            }
        }
    }

    private suspend fun movementLoop() {
        var lastUpdate = System.currentTimeMillis()
        while (currentCoroutineContext().isActive) {
            val now = System.currentTimeMillis()
            val dt = now - lastUpdate
            if (dt > 0L) updatePositions(dt)
            lastUpdate = now
            delay(16L)
        }
    }

    private fun gravityMultiplier(): Float {
        val elapsed = System.currentTimeMillis() - gameStartTime
        return minOf(5f, 1f + elapsed / 1000f * 0.05f)
    }

    private fun updatePositions(dtMs: Long) {
        _state.update { state ->
            if (state.circles.isEmpty()) return@update state
            val dt = dtMs.toFloat()
            val gravMul = gravityMultiplier()
            state.copy(
                circles = state.circles.map { circle ->
                    var vx = circle.velocityX + circle.accelX * gravMul * dt
                    var vy = circle.velocityY + circle.accelY * gravMul * dt
                    var newX = circle.centerX + vx * dt
                    var newY = circle.centerY + vy * dt

                    if (newX - circle.radius < 0f) {
                        newX = circle.radius
                        vx = -vx
                    } else if (newX + circle.radius > screenWidth) {
                        newX = screenWidth - circle.radius
                        vx = -vx
                    }

                    if (newY - circle.radius < 0f) {
                        newY = circle.radius
                        vy = -vy
                    } else if (newY + circle.radius > screenHeight) {
                        newY = screenHeight - circle.radius
                        vy = -vy
                    }

                    circle.copy(
                        centerX = newX,
                        centerY = newY,
                        velocityX = vx,
                        velocityY = vy
                    )
                }
            )
        }
    }

    fun resetGame() {
        gameLoopJob?.cancel()
        _state.value = GameState()
    }
}
