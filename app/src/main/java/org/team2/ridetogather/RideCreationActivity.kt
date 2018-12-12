package org.team2.ridetogather

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*


class RideCreationActivity : AppCompatActivity() {
    private val tag = RideCreationActivity::class.java.simpleName

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ridecreation)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val event = Database.getEvent(eventId)!!
        Log.d(tag, "Created $tag with Event ID $eventId")

        val timePickButton = findViewById<Button>(R.id.pick_time_button)
        val departureTime = findViewById<TextView>(R.id.departure_time)
        val carModel = findViewById<TextView>(R.id.car_model)
        val carColor = findViewById<TextView>(R.id.car_color)
        val passengerCount = findViewById<TextView>(R.id.num_seats)
        val extraDetails = findViewById<TextView>(R.id.extra_details)
        val submitBtn = findViewById<Button>(R.id.btn_submit)
        val originBtn = findViewById<Button>(R.id.btn_origin)

        val driverId: Id = Database.getThisUser().getIdAsDriver()
        var timeOfDay: TimeOfDay? = null
        val origin = MockData.location3 // MOCK
        val destination = MockData.location2 // MOCK

        timePickButton.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                departureTime.text = SimpleDateFormat("HH:mm").format(cal.time)
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

        originBtn.setOnClickListener {
            val intent = Intent(applicationContext, MapsActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, eventId)
            startActivity(intent)
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
            if (timeOfDay == null) allIsGood = markError(timePickButton)
            if (carModel.text.isBlank()) allIsGood = markError(carModel)
            if (carColor.text.isBlank()) allIsGood = markError(carColor)
            if (passengerCount.text.isBlank()) allIsGood = markError(passengerCount)
            else if (passengerCount.text.toString().toInt() <= 0) allIsGood =
                    markError(passengerCount, getString(R.string.error_nonpositive_passenger_count))
            if (origin == null) allIsGood = TODO()
            if (destination == null) allIsGood = TODO()
            return allIsGood
        }

        submitBtn.setOnClickListener {
            if (!checkThatAllFieldsWereFilledCorrectly()) {
                return@setOnClickListener
            }

            val newRide = Database.createNewRide(
                driverId, eventId, origin, destination, timeOfDay!!,
                carModel.text.toString(), carColor.text.toString(),
                passengerCount.text.toString().toInt(), extraDetails.text.toString()
            )
            val intent = Intent(applicationContext, RidePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(Keys.RIDE_ID.name, newRide.id)
            startActivity(intent)
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


