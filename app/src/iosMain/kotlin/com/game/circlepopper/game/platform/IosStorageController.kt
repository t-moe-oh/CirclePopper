package com.game.circlepopper.game.platform

import platform.Foundation.NSUserDefaults

class IosStorageController : StorageController {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, default: String?): String? {
        return defaults.stringForKey(key) ?: default
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            default
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
        defaults.synchronize()
    }
}
