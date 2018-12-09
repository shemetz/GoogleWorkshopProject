package org.team2.ridetogather
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    private val tag = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        Log.d(tag, "Created $tag")
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        when (availability) {
            ConnectionResult.SUCCESS -> {
            }
            ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
            ConnectionResult.SERVICE_DISABLED -> {
                Log.e(tag, "Google API is not available! availability = $availability")
                GoogleApiAvailability.getInstance().getErrorDialog(this, availability, 17)  // 17 is not important
            }
            else -> {
                Log.e(tag, "Google API is not available! availability = $availability")
                Log.e(tag, "Exiting app now (google maps won't work)!")
                finish()
            }
        }

        temp_main_activity_text.text = "Hello ${Database.getThisUser().name}!"

        // MOCK
        temp_button_rides_list.setOnClickListener {
            val intent = Intent(applicationContext, EventRidesActivity::class.java)
            val eventID = MockData.event1.id
            intent.putExtra(Keys.EVENT_ID.name, eventID)
            startActivity(intent)
        }

        // MOCK
        temp_button_switch_user.setOnClickListener {
            Database.idOfCurrentUser = if (Database.getThisUserId() == MockData.user1.id)
                MockData.user2.id
            else
                MockData.user1.id
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
