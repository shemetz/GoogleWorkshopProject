package org.team2.ridetogather

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by Lincoln on 05/05/16.
 */
class PrefManager(_context: Context) {
    private var pref: SharedPreferences

    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean(IS_FIRST_TIME_LAUNCH, true)
        set(value) = pref.edit().putBoolean(IS_FIRST_TIME_LAUNCH, value).apply()

    init {
        pref = _context.getSharedPreferences(PREFERENCES_FILE_NAME, 0) // 0 = PRIVATE MODE
    }

    companion object {
        // Shared preferences file name
        private const val PREFERENCES_FILE_NAME = "local_preferences"

        private const val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"
    }

}