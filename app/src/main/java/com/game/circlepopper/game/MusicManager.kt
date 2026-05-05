package com.game.circlepopper.game

import android.content.Context
import android.media.MediaPlayer
import com.game.circlepopper.R

class MusicManager(private val appContext: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun startMenuMusic() {
        if (mediaPlayer?.isPlaying == true) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(appContext, R.raw.menu_music).apply {
            isLooping = true
            setVolume(0.4f, 0.4f)
            start()
        }
    }

    fun stopMenuMusic() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    fun pauseMenuMusic() {
        mediaPlayer?.pause()
    }

    fun resumeMenuMusic() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
        } else {
            startMenuMusic()
        }
    }
}
