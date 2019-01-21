package org.team2.ridetogather

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.Toast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ride_page.*
import kotlinx.android.synthetic.main.card_ride_page.view.*
import org.team2.ridetogather.PickupStatus.*


class RidePageActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context?, rideId: Int?, driverPerspective: Boolean, clearPrevActivity: Boolean = false) {
            val intent = Intent(context, RidePageActivity::class.java)
            intent.putExtra(Keys.RIDE_ID.name, rideId)
            intent.putExtra(Keys.DRIVER_PERSPECTIVE.name, driverPerspective)
            if (clearPrevActivity)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context?.startActivity(intent)
        }
    }

    private val tag = RidePageActivity::class.java.simpleName
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var rideId: Id = -1 // updates in onCreate
    private lateinit var ride: Ride
    private val driversPerspective: Boolean by lazy { intent.getBooleanExtra(Keys.DRIVER_PERSPECTIVE.name, false) }


    private fun showRideDetails(ride: Ride) {
        carColor.visibility = View.VISIBLE
        originLocation.visibility = View.VISIBLE
        departureTime.visibility = View.VISIBLE
        details.visibility = View.VISIBLE

        carModel.text = ride.carModel
        carColor.text = ride.carColor
        geocode(this, ride.origin.toLatLng()) {
            originLocation.text = it
        }
        departureTime.text = ride.departureTime.shortenedTime()
        details.text = ride.extraDetails

        detailsLayout.visibility = if (ride.extraDetails.isBlank()) View.GONE else View.VISIBLE
    }

    private fun showLoadingTextInsteadOfCarDetails() {
        carModel.text = getString(R.string.loading)
        carColor.visibility = View.INVISIBLE
        originLocation.visibility = View.INVISIBLE
        departureTime.visibility = View.INVISIBLE
        details.visibility = View.INVISIBLE

        mainButton.isEnabled = false
        mainButton.text = getString(R.string.loading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_page)
//        setSupportActionBar(toolbar)
        rideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
        Log.d(tag, "Created $tag with Ride ID $rideId")

        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(this, emptyArray())
        recyclerView = findViewById<RecyclerView>(R.id.ride_page_recyclerview).apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        showLoadingTextInsteadOfCarDetails()
        Database.getRide(rideId) { ride: Ride ->
            this.ride = ride
            showRideDetails(ride)
            Database.getDriver(ride.driverId) { driver: Driver ->
                driverNamePage.text = driver.name
                val facebookId = driver.facebookProfileId
                getProfilePicUrl(facebookId) { pic_url ->
                    Picasso.get()
                        .load(pic_url)
                        .placeholder(R.drawable.placeholder_profile)
                        .error(R.drawable.placeholder_profile)
                        .resize(256, 256)
                        .transform(CircleTransform())
                        .into(DriverProfilePic)
                }
            }
            updatePassengers(ride.id)
        }

        updateMainButton()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        updatePassengers(ride.id)
        updateMainButton()
        val success = resultCode == Activity.RESULT_OK
        val toastTextResourceId = when (MapsActivity.Companion.RequestCode.values()[requestCode]) {
            MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN -> {
                R.string.weird_error
            }
            MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION -> {
                if (success) R.string.toast_join_ride_success else R.string.toast_join_ride_cancel
            }
            MapsActivity.Companion.RequestCode.CONFIRM_OR_DENY_PASSENGERS -> {
                if (success) R.string.toast_ride_map_edit_success else R.string.null_string
            }
        }
        if (toastTextResourceId != R.string.null_string) {
            Toast.makeText(this, getString(toastTextResourceId), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMainButton() {
        mainButton.isEnabled = false
        mainButton.text = getString(R.string.loading)
        if (driversPerspective) {
            // "Confirm pickups"
            Database.getPickupsForRide(rideId) { pickups: List<Pickup> ->
                var somePickupsAreNotInRide = false
                for (pickup in pickups) {
                    if (!pickup.inRide) {
                        somePickupsAreNotInRide = true
                    }
                }
                if (somePickupsAreNotInRide) {
                    mainButton.isEnabled = true
                    mainButton.text = getString(R.string.ride_map_with_pending_request)
                } else {
                    mainButton.isEnabled = true
                    mainButton.text = getString(R.string.ride_map)
                }

                Database.getRide(rideId) { ride ->
                    mainButton.setOnClickListener {
                        val intent = Intent(applicationContext, MapsActivity::class.java)
                        intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
                        intent.putExtra(Keys.RIDE_ID.name, rideId)
                        intent.putExtra(Keys.LOCATION.name, ride.origin.toLatLng().encodeToString())
                        intent.putExtra(
                            Keys.REQUEST_CODE.name,
                            MapsActivity.Companion.RequestCode.CONFIRM_OR_DENY_PASSENGERS.ordinal
                        )
                        startActivityForResult(
                            intent,
                            MapsActivity.Companion.RequestCode.CONFIRM_OR_DENY_PASSENGERS.ordinal
                        )
                    }
                }
            }
        } else {
            // Enable join ride button if not already asking for a pickup.
            Database.getPickupsForRide(rideId) { pickups: List<Pickup> ->
                var pickupStatus: PickupStatus = NOT_EXIST
                for (pickup in pickups) {
                    if (pickup.userId == Database.idOfCurrentUser) {
                        pickupStatus = if (pickup.inRide) APPROVED else PENDING
                        break
                    }
                }
                when (pickupStatus) {
                    APPROVED -> {
                        mainButton.isEnabled = true
                        mainButton.text = getString(R.string.leave_ride)
                        mainButton.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed))
                        mainButton.setTextColor(ContextCompat.getColor(this, R.color.title_light))
                        mainButton.setOnClickListener {
                            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                                .setTitle(R.string.leave_ride)
                                .setMessage(getString(R.string.leave_ride_are_you_sure))
                                .setPositiveButton(
                                    R.string.yes
                                ) { _, _ ->
                                    val pickup = pickups.single { p -> p.userId == Database.idOfCurrentUser }
                                    Database.deletePickup(pickup.id) {
                                        Toast.makeText(this, getString(R.string.toast_leave_ride_success), Toast.LENGTH_SHORT).show()
                                        recreate() // to be updated
                                    }
                                    mainButton.isEnabled = false
                                    mainButton.text = getString(R.string.updating)
                                }
                                .setNegativeButton(R.string.no, null).show()
                        }
                    }
                    PENDING -> {
                        mainButton.isEnabled = true
                        mainButton.text = getString(R.string.request_is_pending)
                        mainButton.setOnClickListener {
                            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                                .setTitle(R.string.cancel_request)
                                .setMessage(getString(R.string.cancel_request_are_you_sure))
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    val pickup = pickups.single { p -> p.userId == Database.idOfCurrentUser }
                                    Database.deletePickup(pickup.id) {
                                        Toast.makeText(this, getString(R.string.toast_cancel_request_success), Toast.LENGTH_SHORT).show()
                                        recreate() // to be updated
                                    }
                                    mainButton.isEnabled = false
                                    mainButton.text = getString(R.string.updating)
                                }
                                .setNegativeButton(R.string.no, null).show()
                        }
                    }
                    NOT_EXIST -> {
                        Database.getRide(rideId) { ride ->
                            mainButton.isEnabled = true
                            mainButton.text = getString(R.string.join_ride)
                            mainButton.setOnClickListener {
                                val intent = Intent(applicationContext, JoinRideActivity::class.java)
                                intent.putExtra(Keys.RIDE_ID.name, rideId)
                                intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
                                // request code doesn't matter - the activity doesn't check it
                                startActivityForResult(intent, 1)
                            }
                        }
                    }
                    DECLINED -> {
                        mainButton.isEnabled = false
                        mainButton.text = getString(R.string.request_declined)
                    }
                }
            }
        }
    }

    private fun updatePassengers(rideId: Id) {
        Database.getPickupsForRide(rideId) { allPickups: List<Pickup> ->
            val confirmedPickups = allPickups.filter { it.inRide }
            viewAdapter = MyAdapter(this, confirmedPickups.toTypedArray())
            recyclerView = findViewById<RecyclerView>(R.id.ride_page_recyclerview).apply {
                // changes in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_ride_page, menu)

        if (!driversPerspective) {
            menu.findItem(R.id.action_edit_ride).isVisible = false
            menu.findItem(R.id.action_delete_ride).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            android.R.id.home -> {
                Log.i(tag, "OnBackPressed")
                onBackPressed()
                true
            }
            R.id.action_edit_ride -> {
//                val intent = Intent(applicationContext, RideCreationActivity::class.java)
//                intent.putExtra(Keys.RIDE_ID.name, rideId)
//                startActivity(intent)
                true
            }
            R.id.action_delete_ride -> {
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.delete_ride_title)
                    .setMessage(getString(R.string.delete_ride_are_you_sure))
                    .setPositiveButton(
                        R.string.yes
                    ) { _, whichButton ->
                        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                            Database.deleteRide(rideId)
                            Toast.makeText(
                                this@RidePageActivity,
                                "Your ride was deleted.",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(applicationContext, EventRidesActivity::class.java)
                            intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton(R.string.no, null).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class MyAdapter(private val context: Context, private val pickups: Array<Pickup>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {


        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a CardView.
        class MyViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView)

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyViewHolder {
            // create a new view
            val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_ride_page, parent, false) as CardView

            return MyViewHolder(cardView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val view = holder.cardView
            val pickup = pickups[position]

            Database.getUser(pickup.userId) { user: User ->
                view.passengerName.text = user.name
            }

            geocode(context, pickup.pickupSpot.toLatLng()) {
                view.pickupSpot.text = it
            }
            // load profile picture of passenger
            Database.getUser(pickup.userId) { user: User ->
                val facebookId = user.facebookProfileId
                getProfilePicUrl(facebookId) { pic_url ->
                    Picasso.get()
                        .load(pic_url)
                        .placeholder(R.drawable.placeholder_profile)
                        .error(R.drawable.placeholder_profile)
                        .resize(256, 256)
                        .transform(CircleTransform())
                        .into(view.PassengerProfilePic)
                }
            }
            view.pickupTime.text = pickup.pickupTime.shortenedTime()

        }

        override fun getItemCount() = pickups.size
    }
}

