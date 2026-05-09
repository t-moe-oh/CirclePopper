package com.game.circlepopper.game.platform

import android.content.Context
import android.media.MediaPlayer

class AndroidMusicController(private val ctx: Context, private val menuMusicRes: Int) : MusicController {

    private var mediaPlayer: MediaPlayer? = null

    override fun startMenuMusic() {
        if (mediaPlayer?.isPlaying == true) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(ctx, menuMusicRes).apply {
            isLooping = true
            setVolume(0.4f, 0.4f)
            start()
        }
    }

    override fun stopMenuMusic() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    override fun pauseMenuMusic() { mediaPlayer?.pause() }

    override fun resumeMenuMusic() {
        if (mediaPlayer != null) { mediaPlayer?.start() }
        else { startMenuMusic() }
    }
}
