package com.game.circlepopper.game.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle

class IosSoundController : SoundController {

    private var wallPlayer: AVAudioPlayer? = null
    private var circlePlayer: AVAudioPlayer? = null
    private var boopPlayer: AVAudioPlayer? = null
    private var boomPlayer: AVAudioPlayer? = null

    @OptIn(ExperimentalForeignApi::class)
    private fun loadSound(name: String, ext: String = "wav"): AVAudioPlayer? {
        val url = NSBundle.mainBundle.URLForResource(name, ext) ?: return null
        return AVAudioPlayer(url, null)
    }

    private fun play(player: AVAudioPlayer?) {
        player?.currentTime = 0.0
        player?.volume = 0.5f
        player?.play()
    }

    override fun playWallBump() {
        if (wallPlayer == null) wallPlayer = loadSound("plink_wall")
        play(wallPlayer)
    }

    override fun playCircleHit() {
        if (circlePlayer == null) circlePlayer = loadSound("plink_circle")
        play(circlePlayer)
    }

    override fun playBoop() {
        if (boopPlayer == null) boopPlayer = loadSound("boop")
        play(boopPlayer)
    }

    override fun playBoom() {
        if (boomPlayer == null) boomPlayer = loadSound("boom")
        boomPlayer?.volume = 0.7f
        play(boomPlayer)
    }
}
