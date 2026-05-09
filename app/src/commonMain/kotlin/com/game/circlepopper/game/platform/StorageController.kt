package com.game.circlepopper.game.platform

interface StorageController {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
}
