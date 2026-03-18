package com.twentyfourduel.sound

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import com.twentyfourduel.R

class SoundManager(private val context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(6).build()
    private val sounds = mutableMapOf<String, Int>()
    private var bgmPlayer: MediaPlayer? = null

    init {
        val map = mapOf(
            "correct" to R.raw.correct,
            "wrong" to R.raw.wrong,
            "tap" to R.raw.tap,
            "timeout" to R.raw.timeout,
            "tick" to R.raw.tick,
            "gameover" to R.raw.gameover
        )
        for ((name, resId) in map) {
            sounds[name] = soundPool.load(context, resId, 1)
        }
    }

    fun play(name: String) {
        sounds[name]?.let { soundPool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun startBGM() {
        if (bgmPlayer == null) {
            bgmPlayer = MediaPlayer.create(context, R.raw.bgm).apply {
                isLooping = true
                setVolume(0.35f, 0.35f)
            }
        }
        bgmPlayer?.start()
    }

    fun release() {
        soundPool.release()
        bgmPlayer?.release()
        bgmPlayer = null
    }
}
