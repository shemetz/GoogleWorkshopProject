package org.team2.ridetogather

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.login.LoginManager
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

        val listView = findViewById<ListView>(R.id.events_list)
        val event_request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { Json, response ->
            try {
                Log.i(tag, "working")
                Log.i(tag, Json.toString(4))
                val eventsArray = Json.getJSONObject("events").getJSONArray("data")
                val eventsNameList = arrayOfNulls<String>(eventsArray.length())
                for (i in 0..(eventsArray.length() - 1)) {
                    val eventName = eventsArray.optJSONObject(i).getString("name")
                    Log.i(tag, "Event name = $eventName")
                    eventsNameList[i] = eventName
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eventsNameList)
                listView.adapter = adapter
                listView.setOnItemClickListener { parent, view, position, id ->
                    val intent = Intent(applicationContext, EventRidesActivity::class.java)
                    // MOCK
                    val eventID = MockData.event1.id
                    intent.putExtra(Keys.EVENT_ID.name, eventID)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "events")
        event_request.parameters = parameters
        event_request.executeAsync()

        val eventID_request = GraphRequest.newGraphPathRequest(
            AccessToken.getCurrentAccessToken(),
            "/{event-id}", GraphRequest.Callback() { response: GraphResponse ->
                try {
                    Log.i(tag, "working")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })

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

            R.id.log_out_facebook -> {
                Log.d("FBLOGIN", "in log out")
                buildDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    fun disconnectFromFacebook() {

        if (AccessToken.getCurrentAccessToken() == null) {
            return  // already logged out
        }

        GraphRequest(AccessToken.getCurrentAccessToken(), "/me/permissions/", null, HttpMethod.DELETE,
            GraphRequest.Callback {
                LoginManager.getInstance().logOut()
                goToFacebookLoginActivity()
            }).executeAsync()
    }

    fun goToFacebookLoginActivity() {
        // Go to MainActivity and start it
        val intent = Intent(applicationContext, FacebookLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun buildDialog() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        //builder.setTitle("Hey there")
        builder.setMessage("Are you sure you want to log out?")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(R.string.leave) { dialog, which ->
            disconnectFromFacebook()
        }
        builder.setNegativeButton(R.string.stay) { dialog, which ->
            dialog.dismiss()
        }
        builder.show()

    }
}
