package com.game.circlepopper.game.platform

import android.content.Context
import android.media.MediaPlayer
import com.game.circlepopper.R

class AndroidMusicController(private val ctx: Context) : MusicController {

    private var mediaPlayer: MediaPlayer? = null

    override fun startMenuMusic() {
        if (mediaPlayer?.isPlaying == true) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(ctx, R.raw.menu_music).apply {
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

    override fun pauseMenuMusic() {
        mediaPlayer?.pause()
    }

    override fun resumeMenuMusic() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
        } else {
            startMenuMusic()
        }
    }
}
