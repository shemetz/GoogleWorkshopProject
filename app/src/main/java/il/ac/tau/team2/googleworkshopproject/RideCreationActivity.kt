package il.ac.tau.team2.googleworkshopproject

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*


class RideCreationActivity : AppCompatActivity() {


    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ridecreation)
        val mPickTimeBtn = findViewById<Button>(R.id.pick_time_button)
        val departureTime = findViewById<TextView>(R.id.departure_time)
        val carModel = findViewById<TextView>(R.id.car_model)
        val carColor = findViewById<TextView>(R.id.car_color)
        val submitBtn = findViewById<Button>(R.id.btn_submit)
        val extraDetails = findViewById<TextView>(R.id.extra_details)
        val driver: Driver = Database.getThisUser()
        var timeOfDay: TimeOfDay? = null
        val origin = MockData.location3 // MOCK
        val destination = MockData.location2 // MOCK

        mPickTimeBtn.setOnClickListener {
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

        fun checkThatAllFieldsWereFilledCorrectly(): Boolean {
            //If some required fields are not filled, we mark them as bad and not continue
            fun markError(view: Button): Boolean {
                view.requestFocus()
                view.error = "This field is required."
                return false
            }

            fun markError(view: TextView): Boolean {
                view.requestFocus()
                view.error = "This field is required."
                return false
            }
            return when {
                timeOfDay == null -> markError(mPickTimeBtn)
                carModel.text.isBlank() -> markError(carModel)
                carColor.text.isBlank() -> markError(carModel)
                origin == null -> TODO()
                destination == null -> TODO()
                else -> true
            }
        }

        submitBtn.setOnClickListener {
            if (!checkThatAllFieldsWereFilledCorrectly()) {
                return@setOnClickListener
            }

            val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
            val event = Database.getEvent(eventId)!!

            val ride = Database.createNewRide(
                driver, event, origin, destination, timeOfDay!!,
                carModel.text.toString(), carColor.text.toString(), extraDetails.text.toString()
            )
            val intent = Intent(applicationContext, RidePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(Keys.RIDE_ID.name, ride.id)
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


