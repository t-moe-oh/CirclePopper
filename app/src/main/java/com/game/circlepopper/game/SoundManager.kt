package com.game.circlepopper.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.game.circlepopper.R

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val wallPlinkId: Int
    private val circlePlinkId: Int
    private val boopId: Int
    private val boomId: Int

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        wallPlinkId = soundPool.load(context, R.raw.plink_wall, 1)
        circlePlinkId = soundPool.load(context, R.raw.plink_circle, 1)
        boopId = soundPool.load(context, R.raw.boop, 1)
        boomId = soundPool.load(context, R.raw.boom, 1)
    }

    fun playWallBump() {
        soundPool.play(wallPlinkId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playCircleHit() {
        soundPool.play(circlePlinkId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playBoop() {
        soundPool.play(boopId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playBoom() {
        soundPool.play(boomId, 0.7f, 0.7f, 1, 0, 1f)
    }
}
