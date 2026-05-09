package com.game.circlepopper.game.platform

import android.content.Context

class AndroidStorageController(ctx: Context) : StorageController {

    private val prefs = ctx.getSharedPreferences("circle_popper", Context.MODE_PRIVATE)

    override fun getString(key: String, default: String?): String? {
        return prefs.getString(key, default)
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
