package com.game.circlepopper.game

import android.content.Context
import android.media.MediaPlayer

class MusicManager(context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val afd = context.assets.openFd("menu_music.wav")

    fun startMenuMusic() {
        if (mediaPlayer?.isPlaying == true) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            isLooping = true
            setVolume(0.4f, 0.4f)
            prepare()
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
}
