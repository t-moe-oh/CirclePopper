package com.game.circlepopper.game.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class AndroidSoundController(
    ctx: Context,
    wallPlinkRes: Int,
    circlePlinkRes: Int,
    boopRes: Int,
    boomRes: Int
) : SoundController {

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

        wallPlinkId = soundPool.load(ctx, wallPlinkRes, 1)
        circlePlinkId = soundPool.load(ctx, circlePlinkRes, 1)
        boopId = soundPool.load(ctx, boopRes, 1)
        boomId = soundPool.load(ctx, boomRes, 1)
    }

    override fun playWallBump() { soundPool.play(wallPlinkId, 0.5f, 0.5f, 1, 0, 1f) }
    override fun playCircleHit() { soundPool.play(circlePlinkId, 0.5f, 0.5f, 1, 0, 1f) }
    override fun playBoop() { soundPool.play(boopId, 0.5f, 0.5f, 1, 0, 1f) }
    override fun playBoom() { soundPool.play(boomId, 0.7f, 0.7f, 1, 0, 1f) }
}
