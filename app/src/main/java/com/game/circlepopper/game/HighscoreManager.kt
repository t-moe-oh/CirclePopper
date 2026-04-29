package com.game.circlepopper.game

import android.content.Context

data class Highscore(val name: String, val score: Int)

class HighscoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("highscores", Context.MODE_PRIVATE)
    private val maxEntries = 5

    fun getHighscores(): List<Highscore> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            raw.split(SEPARATOR).map { entry ->
                val parts = entry.split(FIELD_SEPARATOR)
                Highscore(parts[0], parts[1].toInt())
            }.sortedByDescending { it.score }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isQualifying(score: Int): Boolean {
        val highscores = getHighscores()
        if (highscores.size < maxEntries) return true
        return score > highscores.last().score
    }

    fun addHighscore(name: String, score: Int): List<Highscore> {
        val highscores = (getHighscores() + Highscore(name, score))
            .sortedByDescending { it.score }
            .take(maxEntries)

        val raw = highscores.joinToString(SEPARATOR) { "${it.name}$FIELD_SEPARATOR${it.score}" }
        prefs.edit().putString(KEY, raw).apply()
        return highscores
    }

    companion object {
        private const val KEY = "highscores_list"
        private const val SEPARATOR = ";"
        private const val FIELD_SEPARATOR = "|"
    }
}
