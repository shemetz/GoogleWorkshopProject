package org.team2.ridetogather

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_join_ride.*
import org.json.JSONException
import org.json.JSONObject


class JoinRideActivity : AppCompatActivity() {
    private val tag = JoinRideActivity::class.java.simpleName
    private var pickedLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_ride)
        pickedLocationTextBox.text = getString(R.string.choose_a_pick_up_location)
        val rideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        submitPickupRequest.isEnabled = false
        pickLocation.setOnClickListener {
            val intent = Intent(applicationContext, MapsActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, eventId)
            intent.putExtra(Keys.RIDE_ID.name, rideId)
            intent.putExtra(Keys.LOCATION.name, pickedLocation?.toLatLng()?.encodeToString())
            intent.putExtra(Keys.REQUEST_CODE.name, MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal)
            startActivityForResult(intent, MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal)
            // Result will return to OnActivityResult()
        }

        submitPickupRequest.setOnClickListener {
            Database.addPickup(rideId, Database.idOfCurrentUser, pickedLocation!!) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    val pickedLocationStr = data!!.getStringExtra(Keys.LOCATION.name)
                    pickedLocation = pickedLocationStr!!.decodeToLatLng().toLocation()
                    val routeJsonStr = data.getStringExtra(Keys.ROUTE_JSON.name)
                    pickedLocationTextBox.text = getString(R.string.updating)
                    geocode(this@JoinRideActivity, pickedLocation!!.toLatLng()) {
                        pickedLocationTextBox.text = it
                        submitPickupRequest.isEnabled = true
                    }
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
                        } catch (e: JSONException) {
                            Log.e(tag, "Error in route json parsing, probably undefined distance", e)
                        }
                    } else Log.i(tag, "Did not get a route JSON in return.")
                } // else Activity.RESULT_CANCELED, so we will just do nothing
            }
            else -> {
                Log.e(tag, "This is not supposed to happen!!! $requestCode $resultCode")
            }
        }
    }

}
