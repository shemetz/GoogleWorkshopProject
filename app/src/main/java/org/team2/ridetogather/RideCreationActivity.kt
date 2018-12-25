package org.team2.ridetogather

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_ridecreation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class RideCreationActivity : AppCompatActivity() {
    private val tag = RideCreationActivity::class.java.simpleName
    var originLocation: Location? = null

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ridecreation)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val event = Database.getEvent(eventId)!!
        Log.d(tag, "Created $tag with Event ID $eventId")

        val driverId: Id = Database.getThisUser().getIdAsDriver()
        var timeOfDay: TimeOfDay? = null
        val destinationLocation = event.location

        pick_time_button.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                pick_time_button.text = SimpleDateFormat("HH:mm").format(cal.time)
            }
            TimePickerDialog(
                this,
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
            timeOfDay = TimeOfDay(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }

        btn_origin.setOnClickListener {
            val intent = Intent(applicationContext, MapsActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, eventId)
            intent.putExtra(Keys.LOCATION.name, originLocation?.toLatLng()?.encodeToString())
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

            val newRide = Database.createNewRide(
                driverId, eventId, originLocation!!, destinationLocation, timeOfDay!!,
                car_model.text.toString(), car_color.text.toString(),
                num_seats.text.toString().toInt(), extra_details.text.toString()
            )
            val intent = Intent(applicationContext, RidePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(Keys.RIDE_ID.name, newRide.id)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN.ordinal -> {
                if (resultCode == Activity.RESULT_OK) {
                    val originLocationStr = data!!.getStringExtra(Keys.LOCATION.name)
                    originLocation = originLocationStr!!.decodeToLatLng().toLocation()
                    btn_origin.setAllCaps(false)
                    btn_origin.text = "(Updating…)"
                    CoroutineScope(Dispatchers.Default).launch {
                        val locationStr = shortenedLocation(this@RideCreationActivity, originLocation!!)
                        CoroutineScope(Dispatchers.Main).launch {
                            btn_origin.text = locationStr
                        }
                        Log.d(tag, "Returned to $tag with location $locationStr")
                    }
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


