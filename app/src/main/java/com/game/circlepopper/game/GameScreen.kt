package com.game.circlepopper.game

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun CirclePopperApp(viewModel: GameViewModel = viewModel(), vibrator: Vibrator) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(vibrator) {
        viewModel.setVibrator(vibrator)
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (sensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    viewModel.setTilt(event.values[0], event.values[1])
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    BoxWithConstraints {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        when {
            state.showHighscoreList -> {
                HighscoreListScreen(
                    highscores = state.highscores,
                    onBack = viewModel::hideHighscoreList
                )
            }

            !state.isPlaying && !state.isGameOver -> {
                MenuScreen(
                    onStart = { viewModel.startGame(widthPx, heightPx) },
                    onHighscores = viewModel::showHighscoreList,
                    onQuit = { (context as? Activity)?.finish() }
                )
            }

            state.isGameOver -> {
                GameOverScreen(
                    score = state.score,
                    highscores = state.highscores,
                    isQualifying = state.isHighscoreQualifying,
                    highscoreSaved = state.highscoreSaved,
                    onSaveHighscore = viewModel::saveHighscore,
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
private fun MenuScreen(onStart: () -> Unit, onHighscores: () -> Unit, onQuit: () -> Unit) {
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
                onClick = onHighscores,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F3460)
                )
            ) {
                Text(
                    text = "HIGHSCORES",
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
private fun GameOverScreen(
    score: Int,
    highscores: List<Highscore>,
    isQualifying: Boolean,
    highscoreSaved: Boolean,
    onSaveHighscore: (String) -> Unit,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
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

            Spacer(modifier = Modifier.height(32.dp))

            if (isQualifying && !highscoreSaved) {
                HighscoreEntry(onSave = onSaveHighscore)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (highscoreSaved) {
                HighscoreTable(highscores = highscores, highlightScore = score)
                Spacer(modifier = Modifier.height(24.dp))
            }

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

@Composable
private fun HighscoreEntry(onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "New Highscore!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD733)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 12) name = it },
            label = { Text("Enter your name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (name.isNotBlank()) onSave(name.trim()) }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE94560),
                focusedBorderColor = Color(0xFFE94560),
                unfocusedBorderColor = Color(0xFF8A8AB5),
                focusedLabelColor = Color(0xFFE94560),
                unfocusedLabelColor = Color(0xFF8A8AB5),
            ),
            modifier = Modifier.width(260.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { if (name.isNotBlank()) onSave(name.trim()) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD733)
            )
        ) {
            Text(
                text = "SAVE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E)
            )
        }
    }
}

@Composable
private fun HighscoreListScreen(highscores: List<Highscore>, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Highscores",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE94560)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (highscores.isEmpty()) {
                Text(
                    text = "No scores yet!",
                    fontSize = 18.sp,
                    color = Color(0xFF8A8AB5)
                )
            } else {
                HighscoreTable(highscores = highscores, highlightScore = null)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE94560)
                )
            ) {
                Text(
                    text = "BACK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HighscoreTable(highscores: List<Highscore>, highlightScore: Int?) {
    Column(
        modifier = Modifier.width(280.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        highscores.forEachIndexed { index, highscore ->
            val isHighlighted = highscore.score == highlightScore
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHighlighted) Color(0xFF0F3460) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A8AB5)
                )
                Text(
                    text = highscore.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Color(0xFFFFD733) else Color.White,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                Text(
                    text = "${highscore.score}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Color(0xFFFFD733) else Color(0xFF33FF57)
                )
            }
        }
    }
}
