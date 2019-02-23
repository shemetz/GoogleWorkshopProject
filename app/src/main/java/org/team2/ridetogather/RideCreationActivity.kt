package org.team2.ridetogather

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_ridecreation.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class RideCreationActivity : AppCompatActivity() {
    private val tag = RideCreationActivity::class.java.simpleName
    var originLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ridecreation)
        Database.initializeIfNeeded(this)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val existingRideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
        Log.d(tag, "Created $tag with Event ID $eventId")

        val driverId: Id = Database.idOfCurrentUser
        var timeOfDay: TimeOfDay? = null

        pick_time_button.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                pick_time_button.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
                timeOfDay = TimeOfDay(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            }
            TimePickerDialog(
                this,
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        btn_origin.setOnClickListener {
            val intent = Intent(applicationContext, MapsActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, eventId)
            intent.putExtra(Keys.LOCATION.name, originLocation?.toLatLng()?.encodeToString())
            intent.putExtra(Keys.REQUEST_CODE.name, MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN.ordinal)
            startActivityForResult(intent, MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN.ordinal)
            // Result will return to OnActivityResult()
        }

        /**
         * If some required fields are not filled, we mark them as bad and not continue
         */
        fun checkThatAllFieldsWereFilledCorrectly(): Boolean {
            var focusMoved = false
            fun markError(view: Button, overrideErrorText: String? = null): Boolean {
                if (!focusMoved) {
                    focusMoved = view.requestFocus()
                }
                view.error = overrideErrorText ?: getString(R.string.error_field_required)
                return false
            }

            fun markError(view: TextView, overrideErrorText: String? = null): Boolean {
                view.requestFocus()
                view.error = overrideErrorText ?: getString(R.string.error_field_required)
                return false
            }

            var allIsGood = true
            if (timeOfDay == null) allIsGood = markError(pick_time_button)
            if (car_model.text.isBlank()) allIsGood = markError(car_model)
            if (car_color.text.isBlank()) allIsGood = markError(car_color)
            if (num_seats.text.isBlank()) allIsGood = markError(num_seats)
            else if (num_seats.text.toString().toInt() <= 0) allIsGood =
                markError(num_seats, getString(R.string.error_nonpositive_passenger_count))
            if (originLocation == null) allIsGood = markError(btn_origin)
            return allIsGood
        }

        btn_submit.setOnClickListener {
            if (!checkThatAllFieldsWereFilledCorrectly()) {
                return@setOnClickListener
            }

            getSharedPreferences(tag, Context.MODE_PRIVATE).edit().apply {
                putString(Preferences.CAR_COLOR.name, car_color.text.toString())
                putString(Preferences.CAR_MODEL.name, car_model.text.toString())
                putString(Preferences.NUMBER_OF_SEATS.name, num_seats.text.toString())
                putString(Preferences.LAST_ORIGIN_LOCATION__LAT_LNG.name, originLocation!!.toLatLng().encodeToString())
                putString(Preferences.LAST_ORIGIN_LOCATION__READABLE.name, btn_origin.text.toString())
                apply()
            }
            if (existingRideId == -1)
                Database.addRide(
                    driverId, eventId, originLocation!!, timeOfDay!!,
                    car_model.text.toString(), car_color.text.toString(),
                    num_seats.text.toString().toInt(), extra_details.text.toString()
                ) { newRide: Ride ->
                    RidePageActivity.start(this, newRide.id, true, clearPrevActivity = true)
                    finish()
                }
            else
                Database.getRide(existingRideId) { ride ->
                    ride.origin = originLocation!!
                    ride.departureTime = timeOfDay!!
                    ride.carModel = car_model.text.toString()
                    ride.carColor = car_color.text.toString()
                    ride.passengerCount = num_seats.text.toString().toInt()
                    ride.extraDetails = extra_details.text.toString()
                    Database.updateRide(ride)
                    RidePageActivity.start(this, ride.id, true, clearPrevActivity = true)
                    finish()
                }
        }
        new_ride_form.requestFocus()

        val sharedPrefs = getSharedPreferences(tag, Context.MODE_PRIVATE)
        sharedPrefs.getString(Preferences.CAR_MODEL.name, null)?.apply { car_model.setText(this) }
        sharedPrefs.getString(Preferences.CAR_COLOR.name, null)?.apply { car_color.setText(this) }
        sharedPrefs.getString(Preferences.NUMBER_OF_SEATS.name, null)?.apply { num_seats.setText(this) }
        sharedPrefs.getString(Preferences.LAST_ORIGIN_LOCATION__LAT_LNG.name, null)
            ?.apply { originLocation = this.decodeToLatLng().toLocation() }
        sharedPrefs.getString(Preferences.LAST_ORIGIN_LOCATION__READABLE.name, null)?.apply { btn_origin.text = this }

        if (existingRideId != -1) {
            Log.i(tag, "Editing existing ride; loading info...")
            Database.getRide(existingRideId) { ride ->
                car_model.setText(ride.carModel)
                car_color.setText(ride.carColor)
                num_seats.setText(ride.passengerCount.toString())
                originLocation = ride.origin
                timeOfDay = ride.departureTime
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, ride.departureTime.hours)
                cal.set(Calendar.MINUTE, ride.departureTime.minutes)
                pick_time_button.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
                geocode(this@RideCreationActivity, originLocation!!.toLatLng()) {
                    btn_origin.text = it
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    val originLocationStr = data!!.getStringExtra(Keys.LOCATION.name)
                    originLocation = originLocationStr!!.decodeToLatLng().toLocation()
                    val routeJsonStr = data.getStringExtra(Keys.ROUTE_JSON.name)
                    btn_origin.text = getString(R.string.updating)
                    geocode(this@RideCreationActivity, originLocation!!.toLatLng()) {
                        btn_origin.text = it
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
                            ride_time_and_distance.text =
                                "The ride should take about $durationInText ($distanceInText)."
                            ride_time_and_distance.visibility = View.VISIBLE
                        } catch (e: JSONException) {
                            Log.e(tag, "Error in route json parsing, probably undefined distance", e)
                            ride_time_and_distance.visibility = View.INVISIBLE
                        }
                    } else Log.i(tag, "Did not get a route JSON in return.")
                } // else Activity.RESULT_CANCELED, so we will just do nothing
            }
            else -> {
                Log.e(tag, "This is not supposed to happen!!! $requestCode $resultCode")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


