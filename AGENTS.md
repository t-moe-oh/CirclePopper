# Circle Popper — Agent Guide

## Build

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug        # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease      # release (no minification currently)
```

JDK 25 is installed but incompatible with AGP 8.9.0 / Gradle 8.13 — use JDK 21.

## Project structure

Single-module Android app with Jetpack Compose + Material 3.

```
app/src/main/java/com/game/circlepopper/
  MainActivity.kt          # Entry: enableEdgeToEdge + setContent
  game/
    GameState.kt           # GameCircle + GameState data classes
    GameViewModel.kt       # All game logic (coroutine loops)
    GameScreen.kt          # All Compose UI (Menu, Game, GameOver + HUD)
```

## Architecture

**State management:** MVVM — `GameViewModel` exposes `StateFlow<GameState>`, UI collects via `collectAsState()`.

**GameCircle fields** (immutable data class):
`id, centerX, centerY, velocityX, velocityY, accelX, accelY, radius, color, createdAt`

- `velocityX/Y` in **px/ms** (frame-rate independent)
- `accelX/Y` in **px/ms²** — per-circle gravity, applied as `v += a * dt` each frame
- `createdAt` = `System.currentTimeMillis()` (epoch-based)

**ViewModel runs 3 parallel coroutine loops** inside a parent job:

| Loop | Rate | Purpose |
|---|---|---|
| `spawnLoop` | 400–1500ms (scales with score) | Creates circles at random position with random velocity |
| `movementLoop` | ~16ms (60fps) | Advances positions by `v * dt`, reflects velocity on wall collision |
| `cleanupLoop` | ~100ms | Removes circles aged >4s, increments misses, ends game at 5 misses |

All loops check `currentCoroutineContext().isActive` and are cancelled via `gameLoopJob`.

## Game mechanics

- **Spawn:** Random position (inset by radius), random angle `[0, 2π)`, speed `0.08–0.35` px/ms, radius `5–25%` of smaller screen dimension, random per-circle gravity `0.00012–0.00040` px/ms² in random direction, scaled by `min(5, 1 + elapsed_seconds × 0.05)`
- **Lifetime:** 4s. After 2.5s → blink (alpha oscillation 0.2–1.0 at 4Hz via `sin(phase * 8π)`)
- **Wall bounce:** Reflect velocity component, clamp center to `[radius, dim - radius]`
- **Circle-to-circle:** Equal-mass elastic collision, velocity exchange along collision normal
- **Hit detection:** Euclidean distance from tap to circle center ≤ radius
- **Bonus star:** Golden circle with rotating white star, spawns every 8–12 taps. Hitting it triggers 3-second slow-motion (10% speed) for all circles.
- **Difficulty:** spawn interval = `max(400ms, 1500ms - score × 30)`
- **Misses:** 5 → game over
- **Highscores:** Top 5 persisted via SharedPreferences (JSON-like `name|score;` format). `HighscoreManager` checks qualifying, inserts sorted, trims to 5. UI has dedicated list screen and name entry on game over.

## Canvas + animation

- `GameScreen` has a `LaunchedEffect` + `withFrameNanos` loop that writes `System.currentTimeMillis()` into a `MutableState<Long>` every frame
- The Canvas draw lambda reads this state to compute per-circle blink alpha
- **⚠ `withFrameNanos` returns boot-relative time** — always convert to epoch via `System.currentTimeMillis()` before comparing to `circle.createdAt`

## SDK details

| Setting | Value |
|---|---|
| compileSdk / targetSdk | 36 (API 36: Android 16) |
| minSdk | 26 |
| AGP | 8.9.0 |
| Kotlin | 2.1.0 |
| Compose BOM | 2025.02.00 |
| Build tools | 37.0.0 |

Platform `android-37` is installed as `android-37.0` (source.properties reports ApiLevel=37.0). AGP can't find it, so compile with 36.

## Conventions

- **Screen orientation:** locked to portrait in manifest
- **Immutability:** GameState and GameCircle are immutable data classes; all mutations go through `_state.update {}`
- **No comments** in source code
- **No git commits without explicit instruction** — stage or create commits only when the user says "commit"
