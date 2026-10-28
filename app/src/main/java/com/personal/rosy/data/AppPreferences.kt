package com.personal.rosy.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rosy_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALWAYS_ON_WAKE_WORD = "key_always_on_wake_word"
    }

    var isAlwaysOnWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_ON_WAKE_WORD, false) // Default OFF
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS_ON_WAKE_WORD, value).apply()
}
