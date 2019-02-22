package org.team2.ridetogather

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * A [SettingsActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 *
 * I used the [PreferenceFragmentCompat version](https://code.luasoftware.com/tutorials/android/android-settings-preference-using-preferencefragmentcompat/).
 */
class SettingsActivity : AppCompatActivity() {
    private val tag = SettingsActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        Log.d(tag, "Created $tag")

        // android.R.id.content is probably for old style activity
        supportFragmentManager.beginTransaction()
            // .replace(android.R.id.content, SettingsFragment())
            .replace(R.id.content, SettingsFragment())
            .commit()
    }

    override fun onPause() {
        super.onPause()
        val prefManager = PrefManager(this)
        val preferenceKeys = listOf(
            R.string.pref_key_notification_when_my_request_is_accepted,
            R.string.pref_key_notification_when_my_request_is_rejected,
            R.string.pref_key_notification_when_someone_cancels_a_ride_i_am_in,
            R.string.pref_key_notification_when_someone_requests_to_join_me,
            R.string.pref_key_notification_when_someone_cancels_their_pickup
        )
        val defaultPrefManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceKeys.forEach {
            val key = getString(it)
            val value = defaultPrefManager.getBoolean(key, false)
            Log.d(tag, "Preference $key is ${value.toString().toUpperCase()}")
            prefManager.setNotificationPreference(key, value)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_settings)
        }
    }
}
