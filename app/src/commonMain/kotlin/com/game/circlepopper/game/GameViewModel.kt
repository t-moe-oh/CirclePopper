package com.game.circlepopper.game

import androidx.compose.ui.graphics.Color
import com.game.circlepopper.game.platform.MusicController
import com.game.circlepopper.game.platform.SoundController
import com.game.circlepopper.game.platform.StorageController
import com.game.circlepopper.game.platform.VibrationController
import com.game.circlepopper.game.platform.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

class GameViewModel {

    companion object {
        private const val TAG = "CirclePopper"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    private var storageController: StorageController? = null
    private var highscoreManager: HighscoreManager? = null
    private var vibrationController: VibrationController? = null
    private var soundController: SoundController? = null

    fun setStorageController(storage: StorageController) {
        storageController = storage
        highscoreManager = HighscoreManager(storage)
        _state.update {
            it.copy(
                settingsMenuMusic = storage.getBoolean("menu_music", true),
                settingsHaptics = storage.getBoolean("haptics", true),
                settingsRealGravity = storage.getBoolean("real_gravity", true),
            )
        }
    }

    fun setVibrationController(ctrl: VibrationController) {
        vibrationController = ctrl
    }

    fun setSoundController(ctrl: SoundController) {
        soundController = ctrl
    }

    private var musicController: MusicController? = null

    fun setMusicController(ctrl: MusicController) {
        musicController = ctrl
    }

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

    private val brightColors = listOf(
        Color(0xFFFF5733), Color(0xFF33FF57), Color(0xFF3357FF),
        Color(0xFFFF33A8), Color(0xFFFFD733), Color(0xFF33FFF5),
        Color(0xFFA833FF), Color(0xFFFF8C33), Color(0xFF33FF8C),
        Color(0xFFFF3368),
    )

    private val goldColor = Color(0xFFFFD700)

    fun startGame(widthPx: Float, heightPx: Float) {
        screenWidth = widthPx; screenHeight = heightPx
        nextId = 0L; bonusUntil = 0
        gameStartTime = currentTimeMillis()
        val s = _state.value
        _state.value = GameState(
            isPlaying = true, gameStartTime = gameStartTime,
            settingsMenuMusic = s.settingsMenuMusic,
            settingsHaptics = s.settingsHaptics,
            settingsRealGravity = s.settingsRealGravity,
            showTrails = s.showTrails, trailLength = s.trailLength,
        )
        gameLoopJob?.cancel()
        gameLoopJob = scope.launch {
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
        if (bonusUntil <= 0) { spawnBonusCircle(); bonusUntil = Random.nextInt(8, 13) }
        else if (Random.nextFloat() < 0.15f && bombCount() < 2) { spawnBombCircle(); bonusUntil-- }
        else { spawnNormalCircle(); bonusUntil-- }
    }

    private fun bombCount() = _state.value.circles.count { it.isBomb }

    private fun spawnNormalCircle() {
        val (x, y, radius, vx, vy, ax, ay, color) = commonSpawnParams()
        val circle = GameCircle(id = nextId++, centerX = x, centerY = y,
            velocityX = vx, velocityY = vy, accelX = ax, accelY = ay,
            radius = radius, color = color, createdAt = currentTimeMillis())
        _state.update { it.copy(circles = it.circles + circle) }
    }

    private fun spawnBonusCircle() {
        val (x, y, radius, vx, vy, ax, ay, _) = commonSpawnParams()
        val circle = GameCircle(id = nextId++, centerX = x, centerY = y,
            velocityX = vx, velocityY = vy, accelX = ax, accelY = ay,
            radius = radius * 1.3f, color = goldColor,
            createdAt = currentTimeMillis(), isBonus = true)
        _state.update { it.copy(circles = it.circles + circle) }
    }

    private fun spawnBombCircle() {
        val (x, y, radius, vx, vy, ax, ay, _) = commonSpawnParams()
        val circle = GameCircle(id = nextId++, centerX = x, centerY = y,
            velocityX = vx, velocityY = vy, accelX = ax, accelY = ay,
            radius = radius * 1.2f, color = Color(0xFF1A1A1A),
            createdAt = currentTimeMillis(), isBomb = true)
        _state.update { it.copy(circles = it.circles + circle) }
    }

    private fun commonSpawnParams(): SpawnParams {
        val minDim = min(screenWidth, screenHeight)
        val minRadius = minDim * 0.10f; val maxRadius = minDim * 0.25f
        val radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius
        val x = Random.nextFloat() * (screenWidth - 2 * radius) + radius
        val y = Random.nextFloat() * (screenHeight - 2 * radius) + radius
        val score = _state.value.score
        val baseSpeed = Random.nextFloat() * 0.27f + 0.08f
        val speedBoost = 0.35f * (1f - exp(-score / 30f))
        val speed = baseSpeed + speedBoost
        val angle = Random.nextFloat() * 2f * PI.toFloat()
        val vx = cos(angle) * speed; val vy = sin(angle) * speed
        val gravAngle = Random.nextFloat() * 2f * PI.toFloat()
        val gravStrength = Random.nextFloat() * 0.00028f + 0.00012f
        val ax = cos(gravAngle) * gravStrength; val ay = sin(gravAngle) * gravStrength
        val color = brightColors.random()
        return SpawnParams(x, y, radius, vx, vy, ax, ay, color)
    }

    private data class SpawnParams(
        val x: Float, val y: Float, val radius: Float,
        val vx: Float, val vy: Float, val ax: Float, val ay: Float, val color: Color)

    private fun spawnInterval() = (400f + 1600f * exp(-_state.value.score / 15f)).toLong().coerceAtLeast(400L)

    private fun cleanupExpiredCircles() {
        val now = currentTimeMillis()
        val expired = _state.value.circles.filter { now - it.createdAt > 4000L }
        if (expired.isEmpty()) return
        val expiredNonBomb = expired.filter { !it.isBomb }
        if (expiredNonBomb.isNotEmpty()) {
            println("$TAG expired=${expiredNonBomb.size}, misses=${_state.value.misses + expiredNonBomb.size}")
            if (_state.value.settingsHaptics) vibrationController?.vibrateStrong()
            soundController?.playBoop()
        }
        val expiredBombs = expired.size - expiredNonBomb.size
        _state.update { state ->
            val newMisses = state.misses + expiredNonBomb.size
            val gameOver = newMisses >= 5
            val h = if (gameOver) highscoreManager?.getHighscores() else emptyList()
            state.copy(
                circles = state.circles.filter { now - it.createdAt <= 4000L },
                misses = newMisses, score = state.score + expiredBombs,
                isGameOver = gameOver, isPlaying = if (gameOver) false else state.isPlaying,
                showGameOverOverlay = gameOver,
                bombDodgeEndTime = if (expiredBombs > 0) now + 1000L else state.bombDodgeEndTime,
                highscores = h ?: emptyList(),
                isHighscoreQualifying = if (gameOver) highscoreManager?.isQualifying(state.score) ?: false else false
            )
        }
        if (_state.value.isGameOver) gameLoopJob?.cancel()
    }

    fun saveHighscore(name: String) {
        val score = _state.value.score
        val updated = highscoreManager?.addHighscore(name, score) ?: return
        _state.update { it.copy(highscores = updated, highscoreSaved = true) }
    }

    fun showHighscoreList() {
        val highscores = highscoreManager?.getHighscores() ?: emptyList()
        _state.update { it.copy(showHighscoreList = true, highscores = highscores) }
    }

    fun hideHighscoreList() { _state.update { it.copy(showHighscoreList = false) } }
    fun showSettings() { _state.update { it.copy(showSettings = true) } }
    fun hideSettings() { _state.update { it.copy(showSettings = false) } }
    fun showDebugMenu() { _state.update { it.copy(showDebugMenu = true) } }
    fun hideDebugMenu() { _state.update { it.copy(showDebugMenu = false) } }

    fun toggleTrails(enabled: Boolean) { _state.update { it.copy(showTrails = enabled) } }
    fun setTrailLength(value: Int) { _state.update { it.copy(trailLength = value) } }

    fun toggleMenuMusic(enabled: Boolean) {
        storageController?.putBoolean("menu_music", enabled)
        _state.update { it.copy(settingsMenuMusic = enabled) }
    }

    fun toggleHaptics(enabled: Boolean) {
        storageController?.putBoolean("haptics", enabled)
        _state.update { it.copy(settingsHaptics = enabled) }
    }

    fun toggleRealGravity(enabled: Boolean) {
        storageController?.putBoolean("real_gravity", enabled)
        _state.update { it.copy(settingsRealGravity = enabled) }
    }

    fun clearGameOverOverlay() { _state.update { it.copy(showGameOverOverlay = false) } }

    fun pauseGame() {
        println("$TAG pauseGame() called, isPlaying=${_state.value.isPlaying}")
        if (_state.value.isPlaying) {
            pauseStartTime = currentTimeMillis()
            _state.update { it.copy(isPaused = true) }
        }
    }

    fun resumeGame() {
        if (!_state.value.isPaused) return
        _state.update { it.copy(resumeCountdown = 3) }
        countdownJob?.cancel()
        countdownJob = scope.launch {
            delay(1000L); _state.update { it.copy(resumeCountdown = 2) }
            delay(1000L); _state.update { it.copy(resumeCountdown = 1) }
            delay(1000L)
            val now = currentTimeMillis()
            val pauseDuration = now - pauseStartTime
            _state.update {
                it.copy(resumeCountdown = 0, isPaused = false,
                    circles = it.circles.map { c -> c.copy(createdAt = c.createdAt + pauseDuration + 1000L) })
            }
        }
    }

    fun onTap(x: Float, y: Float) {
        val s = _state.value
        if (!s.isPlaying || s.isGameOver || s.isPaused) return

        val hit = s.circles.find { circle ->
            val dx = x - circle.centerX; val dy = y - circle.centerY
            dx * dx + dy * dy <= circle.radius * circle.radius
        } ?: s.circles.find { circle ->
            circle.trail.any { (tx, ty) ->
                val dx = x - tx; val dy = y - ty
                dx * dx + dy * dy <= circle.radius * circle.radius
            }
        }

        if (hit != null) {
            if (hit.isBomb) {
                _state.update {
                    val newMisses = it.misses + 1; val gameOver = newMisses >= 5
                    val h = if (gameOver) highscoreManager?.getHighscores() else emptyList()
                    it.copy(circles = emptyList(), score = it.score + 1, misses = newMisses,
                        isGameOver = gameOver, isPlaying = if (gameOver) false else it.isPlaying,
                        showGameOverOverlay = gameOver,
                        bombSacrificeEndTime = currentTimeMillis() + 2000L,
                        highscores = h ?: emptyList(),
                        isHighscoreQualifying = if (gameOver) highscoreManager?.isQualifying(it.score) ?: false else false)
                }
                if (_state.value.isGameOver) gameLoopJob?.cancel()
                soundController?.playBoom()
                if (_state.value.settingsHaptics) vibrationController?.vibrateStrong()
            } else if (hit.isBonus) {
                val now = currentTimeMillis()
                _state.update { it.copy(circles = it.circles - hit, score = it.score + 1, slowMotionEndTime = now + 3000L) }
                _state.update { state ->
                    state.copy(circles = state.circles.map { c -> c.copy(velocityX = c.velocityX * 0.1f, velocityY = c.velocityY * 0.1f) })
                }
            } else {
                _state.update { it.copy(circles = it.circles - hit, score = it.score + 1) }
            }
        }
    }

    private suspend fun movementLoop() {
        var lastUpdate = currentTimeMillis()
        while (currentCoroutineContext().isActive) {
            if (_state.value.isPaused) { delay(50); lastUpdate = currentTimeMillis(); continue }
            val now = currentTimeMillis()
            val dt = now - lastUpdate
            if (dt > 0L) updatePositions(dt)
            lastUpdate = now; delay(16L)
        }
    }

    private fun gravityMultiplier(): Float {
        val elapsed = currentTimeMillis() - gameStartTime
        return minOf(5f, 1f + elapsed / 1000f * 0.05f)
    }

    private fun updatePositions(dtMs: Long) {
        var hitWall = false; var hitCircle = false
        _state.update { state ->
            if (state.circles.isEmpty()) return@update state
            val dt = dtMs.toFloat(); val gravMul = gravityMultiplier()
            val realGravX = if (state.settingsRealGravity) -tiltX * realGravityScale else 0f
            val realGravY = if (state.settingsRealGravity) tiltY * realGravityScale else 0f
            val slowMo = if (state.slowMotionEndTime > currentTimeMillis()) 0.1f else 1f
            var updatedCircles = state.circles.map { circle ->
                var vx = circle.velocityX + (circle.accelX * gravMul + realGravX) * dt * slowMo
                var vy = circle.velocityY + (circle.accelY * gravMul + realGravY) * dt * slowMo
                var newX = circle.centerX + vx * dt; var newY = circle.centerY + vy * dt
                var bounced = false
                if (newX - circle.radius < 0f) { newX = circle.radius; vx = -vx; bounced = true }
                else if (newX + circle.radius > screenWidth) { newX = screenWidth - circle.radius; vx = -vx; bounced = true }
                if (newY - circle.radius < 0f) { newY = circle.radius; vy = -vy; bounced = true }
                else if (newY + circle.radius > screenHeight) { newY = screenHeight - circle.radius; vy = -vy; bounced = true }
                if (bounced) hitWall = true
                circle.copy(centerX = newX, centerY = newY, velocityX = vx, velocityY = vy,
                    trail = (circle.trail + Pair(newX, newY)).takeLast(state.trailLength))
            }
            val collisionResult = resolveCollisions(updatedCircles)
            if (collisionResult.second) hitCircle = true
            state.copy(circles = collisionResult.first)
        }
        if (hitCircle) {
            if (_state.value.settingsHaptics) vibrationController?.vibrateLight()
            soundController?.playCircleHit()
        } else if (hitWall) {
            if (_state.value.settingsHaptics) vibrationController?.vibrateLight()
            soundController?.playWallBump()
        }
    }

    private fun resolveCollisions(circles: List<GameCircle>): Pair<List<GameCircle>, Boolean> {
        val result = circles.toMutableList(); var hit = false
        for (i in result.indices) {
            val a = result[i]
            for (j in i + 1 until result.size) {
                val b = result[j]
                val dx = b.centerX - a.centerX; val dy = b.centerY - a.centerY
                val distSq = dx * dx + dy * dy; val minDist = a.radius + b.radius
                if (distSq >= minDist * minDist || distSq < 0.0001f) continue
                val dist = sqrt(distSq); val nx = dx / dist; val ny = dy / dist
                val overlap = (minDist - dist) / 2f
                val newAx = a.centerX - overlap * nx; val newAy = a.centerY - overlap * ny
                val newBx = b.centerX + overlap * nx; val newBy = b.centerY + overlap * ny
                val dvx = a.velocityX - b.velocityX; val dvy = a.velocityY - b.velocityY
                val dot = dvx * nx + dvy * ny
                if (dot > 0f) {
                    hit = true
                    result[i] = a.copy(centerX = newAx, centerY = newAy,
                        velocityX = a.velocityX - dot * nx, velocityY = a.velocityY - dot * ny)
                    result[j] = b.copy(centerX = newBx, centerY = newBy,
                        velocityX = b.velocityX + dot * nx, velocityY = b.velocityY + dot * ny)
                } else {
                    result[i] = a.copy(centerX = newAx, centerY = newAy)
                    result[j] = b.copy(centerX = newBx, centerY = newBy)
                }
            }
        }
        return Pair(result, hit)
    }

    fun resetGame() {
        gameLoopJob?.cancel(); countdownJob?.cancel()
        val s = _state.value
        _state.value = GameState(
            settingsMenuMusic = s.settingsMenuMusic, settingsHaptics = s.settingsHaptics,
            settingsRealGravity = s.settingsRealGravity, showTrails = s.showTrails, trailLength = s.trailLength)
    }
}
