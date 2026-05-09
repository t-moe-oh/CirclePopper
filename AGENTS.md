# Circle Popper — Agent Guide

## Build

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/opt/android-sdk
./gradlew :androidApp:assembleRelease   # release APK → androidApp/build/outputs/apk/release/CirclePopper-release.apk (2.6MB, signed)
./gradlew :androidApp:assembleDebug     # debug APK
```

JDK 25 is installed but Gradle 9.5.0 + AGP 9.2.1 have intermittent aapt2 compatibility issues with it.
Use JDK 21 for reliable builds.

## Project structure

Kotlin Multiplatform project with two modules:

```
androidApp/                              # Pure Android entry point
  src/main/java/.../MainActivity.kt      # Entry: creates controllers, calls CirclePopperApp
  build.gradle.kts                       # com.android.application

app/                                     # KMP shared library
  src/
    commonMain/kotlin/.../game/
      GameState.kt                       # GameCircle + GameState data classes
      GameViewModel.kt                   # All game logic (coroutine loops)
      GameScreen.kt                      # All Compose UI (Menu, Game, GameOver + HUD)
      HighscoreManager.kt                # Highscore persistence via StorageController
      platform/
        VibrationController.kt           # expect interface
        SoundController.kt               # expect interface
        MusicController.kt               # expect interface
        StorageController.kt             # expect interface
        SensorEffect.kt                  # expect composable
    androidMain/kotlin/.../game/platform/
      AndroidVibrationController.kt      # actual (Vibrator API)
      AndroidSoundController.kt          # actual (SoundPool)
      AndroidMusicController.kt          # actual (MediaPlayer)
      AndroidStorageController.kt        # actual (SharedPreferences)
      SensorEffect.kt                    # actual (SensorManager)
    iosMain/kotlin/.../game/platform/
      SensorEffect.kt                    # stub (no-op)
  build.gradle.kts                       # com.android.kotlin.multiplatform.library
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
- **Highscores:** Top 5 persisted via `StorageController` (interface). `HighscoreManager` checks qualifying, inserts sorted, trims to 5.

## Canvas + animation

- `GameScreen` has a `LaunchedEffect` + `withFrameNanos` loop that writes `System.currentTimeMillis()` into a `MutableState<Long>` every frame
- The Canvas draw lambda reads this state to compute per-circle blink alpha
- **⚠ `withFrameNanos` returns boot-relative time** — always convert to epoch via `System.currentTimeMillis()` before comparing to `circle.createdAt`

## SDK details

| Setting | Value |
|---|---|
| compileSdk / targetSdk | 37 (API 37: Android 17) |
| minSdk | 23 |
| version | 1.3 |
| AGP | 9.2.1 |
| Kotlin | 2.1.0 |
| Compose Multiplatform | 1.10.3 |
| Gradle | 9.5.0 |
| Build tools | 36.0.0 |

## KMP / iOS

All platform abstractions are in place (VibrationController, SoundController, MusicController,
StorageController, SensorEffect) with Android implementations in `androidMain`. iOS targets
are configured but don't compile on Linux (need macOS + Xcode). iOS stubs are in `iosMain`.

## Conventions

- **Screen orientation:** locked to portrait in manifest
- **Immutability:** GameState and GameCircle are immutable data classes; all mutations go through `_state.update {}`
- **No comments** in source code
- **No git commits without explicit instruction** — stage or create commits only when the user says "commit"
