package com.game.circlepopper.game.platform

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

class IosMusicController : MusicController {

    private var mediaPlayer: AVAudioPlayer? = null

    override fun startMenuMusic() {
        if (mediaPlayer?.isPlaying == true) return
        val url = NSBundle.mainBundle.URLForResource("menu_music", "ogg") ?: return
        mediaPlayer = AVAudioPlayer(url, null).apply {
            numberOfLoops = -1
            volume = 0.4f
            prepareToPlay()
            play()
        }
    }

    override fun stopMenuMusic() {
        mediaPlayer?.stop()
        mediaPlayer = null
    }

    override fun pauseMenuMusic() {
        mediaPlayer?.pause()
    }

    override fun resumeMenuMusic() {
        if (mediaPlayer != null) {
            mediaPlayer?.play()
        } else {
            startMenuMusic()
        }
    }
}
