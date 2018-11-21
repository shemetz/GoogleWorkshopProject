package il.ac.tau.team2.googleworkshopproject

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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
        lateinit var timeOfDay: TimeOfDay

        mPickTimeBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                departureTime.text = SimpleDateFormat("HH:mm").format(cal.time)
            }
            TimePickerDialog(this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            timeOfDay = TimeOfDay(cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE))
        }

        submitBtn.setOnClickListener{
            val eventId = intent.getIntExtra(IntentExtraKeys.EVENT_ID.name, -1)
            val event = Database.getEvent(eventId)!!
            val ride = Database.createNewRide(driver, event,MockData.location3,MockData.location2, timeOfDay,
                carModel.text.toString(),carColor.text.toString(), extraDetails.text.toString())
            val intent = Intent(applicationContext, RidePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(IntentExtraKeys.RIDE_ID.name, ride.id)
            startActivity(intent)

        }

    }
}


