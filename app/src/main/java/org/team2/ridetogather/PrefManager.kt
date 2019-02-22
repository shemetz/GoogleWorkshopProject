package org.team2.ridetogather

import android.content.Context
import android.content.SharedPreferences

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

    fun setNotificationPreference(key: String, value: Boolean) {
        pref.edit().putBoolean(key, value).apply()
    }

    fun getNotificationPreference(key: String): Boolean {
        return pref.getBoolean(key, false)
    }

    fun setDefaultNotificationPreferences(context: Context) {
        // this function is just to keep the next lines of codes a bit shorter
        fun set(key: Int, value: Boolean) = setNotificationPreference(context.getString(key), value)
        set(R.string.pref_key_notification_when_my_request_is_accepted, true)
        set(R.string.pref_key_notification_when_my_request_is_rejected, false)
        set(R.string.pref_key_notification_when_someone_cancels_a_ride_i_am_in, true)
        set(R.string.pref_key_notification_when_someone_requests_to_join_me, true)
        set(R.string.pref_key_notification_when_someone_cancels_their_pickup, true)
    }

    companion object {
        // Shared preferences file name
        private const val PREFERENCES_FILE_NAME = "local_preferences"
    }

}