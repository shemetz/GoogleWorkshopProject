package org.team2.ridetogather

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_join_ride.*
import kotlinx.android.synthetic.main.activity_ride_page.*
import kotlinx.android.synthetic.main.activity_ridecreation.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject


class JoinRideActivity : AppCompatActivity() {
    private val tag = JoinRideActivity::class.java.simpleName
    var originLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_ride)
        val rideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
        val ride = Database.getRide(rideId)!!
        val eventId = ride.eventId
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

/*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    val originLocationStr = data!!.getStringExtra(Keys.LOCATION.name)
                    originLocation = originLocationStr!!.decodeToLatLng().toLocation()
                    val routeJsonStr = data.getStringExtra(Keys.ROUTE_JSON.name)
                    btn_origin.text = "(Updating…)"
                    CoroutineScope(Dispatchers.Default).launch {
                        val locationStr = readableLocation(this@RideCreationActivity, originLocation!!)
                        CoroutineScope(Dispatchers.Main).launch {
                            btn_origin.text = locationStr
                        }
                        Log.d(tag, "Back in $tag with location $locationStr")
                        if (routeJsonStr.isNotBlank()) {
                            val routeJson = JSONObject(routeJsonStr)
                            val onlyRoute = routeJson.getJSONArray("routes").getJSONObject(0)
                            val legs = onlyRoute.getJSONArray("legs")
                            val onlyLeg = legs.getJSONObject(0)
                            try {
//                                val distanceInMeters = onlyLeg.getJSONObject("distance").getInt("value")
                                val distanceInText = onlyLeg.getJSONObject("distance").getString("text")
//                                val durationInSeconds = onlyLeg.getJSONObject("duration").getInt("value")
                                val durationInText = onlyLeg.getJSONObject("duration").getString("text")
                                Log.i(
                                    tag,
                                    "Updating UI with route data: distance = $distanceInText, duration = $durationInText"
                                )
                                CoroutineScope(Dispatchers.Main).launch {
                                    ride_time_and_distance.text = "The ride should take about $durationInText ($distanceInText)."
                                    ride_time_and_distance.visibility = View.VISIBLE
                                }
                            } catch (e: JSONException) {
                                Log.e(tag, "Error in route json parsing, probably undefined distance", e)
                                ride_time_and_distance.visibility = View.INVISIBLE
                            }
                        } else Log.i(tag, "Did not get a route JSON in return.")
                    }
                } // else Activity.RESULT_CANCELED, so we will just do nothing
            }
            else -> {
                Log.e(tag, "This is not supposed to happen!!! $requestCode $resultCode")
            }
        }
    }
*/
}
