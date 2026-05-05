package com.game.circlepopper.game

import android.app.Application
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CirclePopper"
    }

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var nextId = 0L
    private var gameStartTime = 0L
    private var gameLoopJob: Job? = null
    private var tiltX = 0f
    private var tiltY = 0f
    private var bonusUntil = 0
    private var pauseStartTime = 0L
    private var countdownJob: Job? = null

    private val realGravityScale = 0.0012f

    fun setTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
    }

    private val highscoreManager = HighscoreManager(application)
    private var vibrator: Vibrator? = null
    private var soundManager: SoundManager? = null

    fun setVibrator(vib: Vibrator) {
        vibrator = vib
    }

    fun setSoundManager(sm: SoundManager) {
        soundManager = sm
    }

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

    private val goldColor = Color(0xFFFFD700)

    fun startGame(widthPx: Float, heightPx: Float) {
        screenWidth = widthPx
        screenHeight = heightPx
        nextId = 0L
        bonusUntil = 0
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
        delay(1500L)
        while (currentCoroutineContext().isActive) {
            if (_state.value.isPaused) { delay(50); continue }
            val interval = spawnInterval()
            delay(interval)
            if (currentCoroutineContext().isActive) spawnCircle()
        }
    }

    private suspend fun cleanupLoop() {
        while (currentCoroutineContext().isActive) {
            if (_state.value.isPaused) { delay(50); continue }
            delay(100)
            cleanupExpiredCircles()
            if (_state.value.isGameOver) break
        }
    }

    private fun spawnCircle() {
        if (bonusUntil <= 0) {
            spawnBonusCircle()
            bonusUntil = Random.nextInt(8, 13)
        } else {
            spawnNormalCircle()
            bonusUntil--
        }
    }

    private fun spawnNormalCircle() {
        val (x, y, radius, vx, vy, ax, ay, color) = commonSpawnParams()
        val circle = GameCircle(
            id = nextId++, centerX = x, centerY = y,
            velocityX = vx, velocityY = vy,
            accelX = ax, accelY = ay,
            radius = radius, color = color,
            createdAt = System.currentTimeMillis()
        )
        _state.update { it.copy(circles = it.circles + circle) }
    }

    private fun spawnBonusCircle() {
        val (x, y, radius, vx, vy, ax, ay, _) = commonSpawnParams()
        val circle = GameCircle(
            id = nextId++, centerX = x, centerY = y,
            velocityX = vx, velocityY = vy,
            accelX = ax, accelY = ay,
            radius = radius * 1.3f, color = goldColor,
            createdAt = System.currentTimeMillis(),
            isBonus = true
        )
        _state.update { it.copy(circles = it.circles + circle) }
    }

    private fun commonSpawnParams(): SpawnParams {
        val minDim = min(screenWidth, screenHeight)
        val minRadius = minDim * 0.10f
        val maxRadius = minDim * 0.25f
        val radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius
        val x = Random.nextFloat() * (screenWidth - 2 * radius) + radius
        val y = Random.nextFloat() * (screenHeight - 2 * radius) + radius
        val score = _state.value.score
        val baseSpeed = Random.nextFloat() * 0.27f + 0.08f
        val speedBoost = 0.35f * (1f - exp(-score / 30f))
        val speed = baseSpeed + speedBoost
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed
        val gravAngle = Random.nextFloat() * 2f * PI.toFloat()
        val gravStrength = Random.nextFloat() * 0.00028f + 0.00012f
        val ax = cos(gravAngle) * gravStrength
        val ay = sin(gravAngle) * gravStrength
        val color = brightColors.random()
        return SpawnParams(x, y, radius, vx, vy, ax, ay, color)
    }

    private data class SpawnParams(
        val x: Float, val y: Float, val radius: Float,
        val vx: Float, val vy: Float,
        val ax: Float, val ay: Float,
        val color: Color
    )

    private fun spawnInterval(): Long {
        val score = _state.value.score
        return (400f + 1600f * exp(-score / 15f)).toLong().coerceAtLeast(400L)
    }

    private fun cleanupExpiredCircles() {
        val now = System.currentTimeMillis()
        val expired = _state.value.circles.filter { now - it.createdAt > 4000L }
        if (expired.isEmpty()) return
        Log.d(TAG, "expired=${expired.size}, misses=${_state.value.misses + expired.size}")
        vibrateStrong()
        soundManager?.playBoop()

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
                showGameOverOverlay = gameOver,
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

    fun clearGameOverOverlay() {
        _state.update { it.copy(showGameOverOverlay = false) }
    }

    fun pauseGame() {
        Log.d(TAG, "pauseGame() called, isPlaying=${_state.value.isPlaying}")
        if (_state.value.isPlaying) {
            pauseStartTime = System.currentTimeMillis()
            _state.update { it.copy(isPaused = true) }
        }
    }

    fun resumeGame() {
        if (!_state.value.isPaused) return
        _state.update { it.copy(resumeCountdown = 3) }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            delay(1000L); _state.update { it.copy(resumeCountdown = 2) }
            delay(1000L); _state.update { it.copy(resumeCountdown = 1) }
            delay(1000L)

            val now = System.currentTimeMillis()
            val pauseDuration = now - pauseStartTime
            _state.update {
                it.copy(
                    resumeCountdown = 0,
                    isPaused = false,
                    circles = it.circles.map { c ->
                        c.copy(createdAt = c.createdAt + pauseDuration + 1000L)
                    }
                )
            }
        }
    }

    fun onTap(x: Float, y: Float) {
        val s = _state.value
        if (!s.isPlaying || s.isGameOver || s.isPaused) return

        val hit = s.circles.find { circle ->
            val dx = x - circle.centerX
            val dy = y - circle.centerY
            dx * dx + dy * dy <= circle.radius * circle.radius
        }

        if (hit != null) {
            _state.update {
                if (hit.isBonus) {
                    val now = System.currentTimeMillis()
                    it.copy(
                        circles = it.circles - hit,
                        score = it.score + 1,
                        slowMotionEndTime = now + 3000L
                    )
                } else {
                    it.copy(
                        circles = it.circles - hit,
                        score = it.score + 1
                    )
                }
            }
            if (hit.isBonus) {
                _state.update { state ->
                    state.copy(
                        circles = state.circles.map { c ->
                            c.copy(
                                velocityX = c.velocityX * 0.1f,
                                velocityY = c.velocityY * 0.1f
                            )
                        }
                    )
                }
            }
        }
    }

    private suspend fun movementLoop() {
        var lastUpdate = System.currentTimeMillis()
        while (currentCoroutineContext().isActive) {
            if (_state.value.isPaused) { delay(50); lastUpdate = System.currentTimeMillis(); continue }
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
        var hitWall = false
        var hitCircle = false

        _state.update { state ->
            if (state.circles.isEmpty()) return@update state
            val dt = dtMs.toFloat()
            val gravMul = gravityMultiplier()
            val realGravX = -tiltX * realGravityScale
            val realGravY = tiltY * realGravityScale
            val slowMo = if (state.slowMotionEndTime > System.currentTimeMillis()) 0.1f else 1f

            var updatedCircles = state.circles.map { circle ->
                var vx = circle.velocityX + (circle.accelX * gravMul + realGravX) * dt * slowMo
                var vy = circle.velocityY + (circle.accelY * gravMul + realGravY) * dt * slowMo
                var newX = circle.centerX + vx * dt
                var newY = circle.centerY + vy * dt

                var bounced = false
                if (newX - circle.radius < 0f) {
                    newX = circle.radius; vx = -vx; bounced = true
                } else if (newX + circle.radius > screenWidth) {
                    newX = screenWidth - circle.radius; vx = -vx; bounced = true
                }

                if (newY - circle.radius < 0f) {
                    newY = circle.radius; vy = -vy; bounced = true
                } else if (newY + circle.radius > screenHeight) {
                    newY = screenHeight - circle.radius; vy = -vy; bounced = true
                }

                if (bounced) hitWall = true

                circle.copy(
                    centerX = newX, centerY = newY,
                    velocityX = vx, velocityY = vy
                )
            }

            val collisionResult = resolveCollisions(updatedCircles)
            if (collisionResult.second) hitCircle = true
            updatedCircles = collisionResult.first

            state.copy(circles = updatedCircles)
        }

        if (hitCircle) {
            vibrateLight()
            soundManager?.playCircleHit()
        } else if (hitWall) {
            vibrateLight()
            soundManager?.playWallBump()
        }
    }

    private fun resolveCollisions(circles: List<GameCircle>): Pair<List<GameCircle>, Boolean> {
        val result = circles.toMutableList()
        var hit = false
        for (i in result.indices) {
            val a = result[i]
            for (j in i + 1 until result.size) {
                val b = result[j]
                val dx = b.centerX - a.centerX
                val dy = b.centerY - a.centerY
                val distSq = dx * dx + dy * dy
                val minDist = a.radius + b.radius
                if (distSq >= minDist * minDist) continue
                if (distSq < 0.0001f) continue

                val dist = sqrt(distSq)
                val nx = dx / dist
                val ny = dy / dist

                val overlap = (minDist - dist) / 2f
                val newAx = a.centerX - overlap * nx
                val newAy = a.centerY - overlap * ny
                val newBx = b.centerX + overlap * nx
                val newBy = b.centerY + overlap * ny

                val dvx = a.velocityX - b.velocityX
                val dvy = a.velocityY - b.velocityY
                val dot = dvx * nx + dvy * ny

                if (dot > 0f) {
                    hit = true
                    result[i] = a.copy(
                        centerX = newAx, centerY = newAy,
                        velocityX = a.velocityX - dot * nx,
                        velocityY = a.velocityY - dot * ny
                    )
                    result[j] = b.copy(
                        centerX = newBx, centerY = newBy,
                        velocityX = b.velocityX + dot * nx,
                        velocityY = b.velocityY + dot * ny
                    )
                } else {
                    result[i] = a.copy(centerX = newAx, centerY = newAy)
                    result[j] = b.copy(centerX = newBx, centerY = newBy)
                }
            }
        }
        return Pair(result, hit)
    }

    private fun vibrateLight() {
        val vib = vibrator ?: return
        Log.d(TAG, "vibrateLight()")
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

    private fun vibrateStrong() {
        val vib = vibrator ?: return
        Log.d(TAG, "vibrateStrong()")
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

    fun resetGame() {
        gameLoopJob?.cancel()
        countdownJob?.cancel()
        _state.value = GameState()
    }
}
