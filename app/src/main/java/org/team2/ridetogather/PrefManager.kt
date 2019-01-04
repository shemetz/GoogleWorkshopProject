package org.team2.ridetogather

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by Lincoln on 05/05/16.
 */
class PrefManager(_context: Context) {
    private var pref: SharedPreferences

    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean("IsFirstTimeLaunch", true)
        set(value) = pref.edit().putBoolean("IsFirstTimeLaunch", value).apply()

    var thisUserId: Id
        get() = pref.getInt("thisUserId", -1)
        set(value) = pref.edit().putInt("thisUserId", value).apply()

    init {
        pref = _context.getSharedPreferences(PREFERENCES_FILE_NAME, 0) // 0 = PRIVATE MODE
    }

    companion object {
        // Shared preferences file name
        private const val PREFERENCES_FILE_NAME = "local_preferences"
    }

}