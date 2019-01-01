package org.team2.ridetogather

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_join_ride.*
import kotlinx.android.synthetic.main.activity_ride_page.*
import kotlinx.android.synthetic.main.activity_ridecreation.*
import java.util.*;

class JoinRideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_ride)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val event = Database.getEvent(eventId)!!
        val driverId: Id = Database.getThisUser().getIdAsDriver()
        var timeOfDay: TimeOfDay? = null
        val destinationLocation = event.location

        pickLocation.setOnClickListener {
            val intent = Intent(applicationContext, MapsActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, eventId)
            intent.putExtra(Keys.LOCATION.name, originLocation?.toLatLng()?.encodeToString())
            startActivityForResult(intent, MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN.ordinal)
            // Result will return to OnActivityResult()
        }



    }
}
