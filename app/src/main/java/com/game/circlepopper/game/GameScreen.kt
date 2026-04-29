package com.game.circlepopper.game

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun CirclePopperApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    BoxWithConstraints {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        when {
            !state.isPlaying && !state.isGameOver -> {
                MenuScreen(
                    onStart = { viewModel.startGame(widthPx, heightPx) },
                    onQuit = { (context as? Activity)?.finish() }
                )
            }

            state.isGameOver -> {
                GameOverScreen(
                    score = state.score,
                    onPlayAgain = { viewModel.startGame(widthPx, heightPx) },
                    onMenu = { viewModel.resetGame() }
                )
            }

            else -> {
                GameScreen(
                    state = state,
                    onTap = viewModel::onTap
                )
            }
        }
    }
}

@Composable
private fun MenuScreen(onStart: () -> Unit, onQuit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Circle Popper",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE94560)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap the circles before they vanish!",
                fontSize = 16.sp,
                color = Color(0xFF8A8AB5),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE94560)
                )
            ) {
                Text(
                    text = "START GAME",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onQuit,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16213E)
                )
            ) {
                Text(
                    text = "QUIT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GameScreen(state: GameState, onTap: (Float, Float) -> Unit) {
    var frameTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                frameTimeMs = System.currentTimeMillis()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F23))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onTap(offset.x, offset.y)
                    }
                }
        ) {
            val now = frameTimeMs
            state.circles.forEach { circle ->
                val age = now - circle.createdAt
                val alpha = if (age > 2500L) {
                    val elapsed = (age - 2500L) / 1000.0
                    val phase = elapsed * 8.0 * PI
                    val normalized = (sin(phase) + 1.0) / 2.0
                    (normalized * 0.80 + 0.20).toFloat()
                } else {
                    1f
                }

                drawCircle(
                    color = circle.color.copy(alpha = alpha),
                    radius = circle.radius,
                    center = Offset(circle.centerX, circle.centerY)
                )

                drawCircle(
                    color = circle.color.copy(alpha = alpha * 0.3f),
                    radius = circle.radius + 4f,
                    center = Offset(circle.centerX, circle.centerY),
                    style = Stroke(width = 3f)
                )
            }
        }

        HUD(score = state.score, misses = state.misses)
        TimerDisplay(
            gameStartTime = state.gameStartTime,
            currentTime = frameTimeMs,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TimerDisplay(gameStartTime: Long, currentTime: Long, modifier: Modifier = Modifier) {
    val elapsed = (currentTime - gameStartTime) / 1000
    val minutes = elapsed / 60
    val seconds = elapsed % 60
    Text(
        text = "%02d:%02d".format(minutes, seconds),
        modifier = modifier.fillMaxWidth().padding(bottom = 24.dp),
        textAlign = TextAlign.Center,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun HUD(score: Int, misses: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Score: $score",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Misses: $misses/5",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (misses >= 4) Color(0xFFFF5733) else Color.White
        )
    }
}

@Composable
private fun GameOverScreen(score: Int, onPlayAgain: () -> Unit, onMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Game Over",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE94560)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Final Score",
                fontSize = 18.sp,
                color = Color(0xFF8A8AB5)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$score",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF33FF57)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE94560)
                )
            ) {
                Text(
                    text = "PLAY AGAIN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onMenu,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16213E)
                )
            ) {
                Text(
                    text = "MAIN MENU",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
