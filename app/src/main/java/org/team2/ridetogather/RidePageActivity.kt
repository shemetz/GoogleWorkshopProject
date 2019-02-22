package org.team2.ridetogather

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
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
import org.team2.ridetogather.R.*


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
    private var leaveRideMenuButtonText: String? = null


    private fun showRideDetails() {
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
        carModel.text = getString(string.loading)
        carColor.visibility = View.INVISIBLE
        originLocation.visibility = View.INVISIBLE
        departureTime.visibility = View.INVISIBLE
        details.visibility = View.INVISIBLE

        mainActionButton.isEnabled = false
        mainActionButton.setBackgroundResource(R.drawable.button_disabled)
        mainActionButton.text = getString(string.loading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_ride_page)
//        setSupportActionBar(toolbar)
        Database.initializeIfNeeded(this)
        rideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
        Log.d(tag, "Created $tag with Ride ID $rideId")

        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(this, emptyArray())
        recyclerView = findViewById<RecyclerView>(id.ride_page_recyclerview).apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        showLoadingTextInsteadOfCarDetails()
        Database.getRide(rideId) { ride: Ride ->
            this.ride = ride
            showRideDetails()
            Database.getDriver(ride.driverId) { driver: Driver ->
                driverNamePage.text = driver.name
                val facebookId = driver.facebookProfileId
                getProfilePicUrl(facebookId) { pic_url ->
                    Picasso.get()
                        .load(pic_url)
                        .placeholder(drawable.placeholder_profile_circle)
                        .error(drawable.placeholder_profile_circle)
                        .resize(256, 256)
                        .transform(CircleTransform())
                        .into(DriverProfilePic)
                }
            }
            updatePassengers(ride.id)
            updateButtons()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fun updateStuffAndMaybeShowToast() {
            updatePassengers(ride.id)
            updateButtons()
            val success = resultCode == Activity.RESULT_OK
            val toastTextResourceId = when (MapsActivity.Companion.RequestCode.values()[requestCode]) {
                MapsActivity.Companion.RequestCode.PICK_DRIVER_ORIGIN -> {
                    string.weird_error
                }
                MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION -> {
                    if (success) string.toast_join_ride_success else string.toast_join_ride_cancel
                }
                MapsActivity.Companion.RequestCode.CONFIRM_OR_DENY_PASSENGERS -> {
                    if (success && data?.getBooleanExtra(
                            Keys.SOMETHING_CHANGED.name,
                            false
                        ) == true
                    ) string.toast_ride_map_edit_success else string.null_string
                }
                MapsActivity.Companion.RequestCode.JUST_LOOK_AT_MAP -> {
                    string.null_string
                }
            }
            if (toastTextResourceId != string.null_string) {
                Toast.makeText(this, getString(toastTextResourceId), Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal && resultCode == Activity.RESULT_OK) {
            val pickedLocationStr = data!!.getStringExtra(Keys.LOCATION.name)
            val pickedLocation = pickedLocationStr!!.decodeToLatLng().toLocation()
            Database.addPickup(rideId, Database.idOfCurrentUser, pickedLocation) {
                updateStuffAndMaybeShowToast()
            }
            // Send notification
            Database.getUser(ride.driverId) { driverUser ->
                Database.getUser(Database.idOfCurrentUser) { currentUser ->
                    getProfilePicUrl(currentUser.facebookProfileId) { picUrl ->
                        val title = getString(R.string.notification_title_new_pickup)
                        val message = currentUser.name + " has asked to join your ride."
                        val to = arrayOf(driverUser.firebaseId)
                        val keys = hashMapOf(
                            Keys.RIDE_ID.name to ride.id,
                            Keys.DRIVER_PERSPECTIVE.name to true,
                            Keys.EVENT_ID.name to ride.eventId
                        )
                        Database.sendFirebaseNotification(
                            to, title, message, picUrl,
                            "com.google.firebase.RIDE_PAGE", keys
                        )
                    }
                }
            }
        } else {
            updateStuffAndMaybeShowToast()
        }
    }

    private fun updateButtons() {
        rideMapButton.setOnClickListener {
            val intent = Intent(applicationContext, MapsActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
            intent.putExtra(Keys.RIDE_ID.name, rideId)
            intent.putExtra(
                Keys.REQUEST_CODE.name,
                MapsActivity.Companion.RequestCode.JUST_LOOK_AT_MAP.ordinal
            )
            startActivityForResult(
                intent,
                MapsActivity.Companion.RequestCode.JUST_LOOK_AT_MAP.ordinal
            )
        }

        mainActionButton.isEnabled = false
        mainActionButton.setBackgroundResource(R.drawable.button_disabled)
        mainActionButton.text = getString(string.loading)
        passengersSummary.text = getString(string.loading)
        Database.getPickupsForRide(rideId) { pickups ->
            val numOfExistingPassengers = pickups.count { it.inRide }
            if (driversPerspective) {
                val numOfRequests = pickups.count { !it.inRide && !it.denied }
                passengersSummary.text = if (numOfRequests > 0) getString(
                    if (numOfRequests >= 2) string.passengers_summary_for_driver_plural
                    else string.passengers_summary_for_driver_single,
                    numOfExistingPassengers,
                    ride.passengerCount,
                    numOfRequests
                ) else getString(
                    string.passengers_summary_for_passenger,
                    numOfExistingPassengers,
                    ride.passengerCount
                )

                mainActionButton.isEnabled = true
                mainActionButton.setBackgroundResource(R.drawable.button_shape)
                if (numOfRequests == 0) {
                    mainActionButton.text = getString(string.edit_route)
                    if (numOfExistingPassengers == 0) {
                        mainActionButton.visibility = View.GONE
                    }
                } else {
                    mainActionButton.text = getString(string.view_requests)
                    mainActionButton.visibility = View.VISIBLE
                }
                Database.getRide(rideId) { ride ->
                    mainActionButton.setOnClickListener {
                        val intent = Intent(applicationContext, MapsActivity::class.java)
                        intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
                        intent.putExtra(Keys.RIDE_ID.name, rideId)
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
            } else {
                passengersSummary.text = getString(
                    string.passengers_summary_for_passenger,
                    numOfExistingPassengers,
                    ride.passengerCount
                )
                // Enable join ride button if not already asking for a pickup.
                val pickupOfCurrentUser = pickups.singleOrNull { it.userId == Database.idOfCurrentUser }
                when {
                    pickupOfCurrentUser == null -> {
                        // Pickup does not exist yet - create it
                        leaveRideMenuButtonText = null
                        Database.getRide(rideId) { ride ->
                            mainActionButton.isEnabled = true
                            mainActionButton.setBackgroundResource(R.drawable.button_shape)
                            mainActionButton.text = getString(string.join_ride)
                            mainActionButton.setOnClickListener {
                                val intent = Intent(applicationContext, MapsActivity::class.java)
                                intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
                                intent.putExtra(Keys.RIDE_ID.name, rideId)
                                intent.putExtra(
                                    Keys.REQUEST_CODE.name,
                                    MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal
                                )
                                startActivityForResult(
                                    intent,
                                    MapsActivity.Companion.RequestCode.PICK_PASSENGER_LOCATION.ordinal
                                )
                            }
                        }
                    }
                    pickupOfCurrentUser.denied -> {
                        // Pickup request was declined
                        leaveRideMenuButtonText = null
                        mainActionButton.isEnabled = false
                        mainActionButton.setBackgroundResource(R.drawable.button_disabled)
                        mainActionButton.text = getString(string.request_declined)
                    }
                    pickupOfCurrentUser.inRide -> {
                        // Pickup request was approved
                        leaveRideMenuButtonText = getString(string.leave_ride)
                        mainActionButton.isEnabled = false
                        mainActionButton.setBackgroundResource(R.drawable.button_disabled)
                        mainActionButton.text = getString(string.request_accepted)

                        // the listener is actually a listener for the options menu button, now
                        mainActionButton.setOnClickListener {
                            AlertDialog.Builder(this, style.AlertDialogStyle)
                                .setTitle(string.leave_ride)
                                .setMessage(getString(string.leave_ride_are_you_sure))
                                .setPositiveButton(
                                    string.yes
                                ) { _, _ ->
                                    val pickup = pickups.single { p -> p.userId == Database.idOfCurrentUser }
                                    Database.deletePickup(pickup.id) {
                                        Toast.makeText(
                                            this,
                                            getString(string.toast_leave_ride_success),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        recreate() // to be updated
                                    }
                                    Database.getUser(ride.driverId) { driver ->
                                        val to = arrayOf(driver.firebaseId)
                                        val title = getString(R.string.notification_title_left_ride)
                                        val keys = hashMapOf(
                                            Keys.RIDE_ID.name to ride.id,
                                            Keys.DRIVER_PERSPECTIVE.name to true,
                                            Keys.EVENT_ID.name to ride.eventId
                                        )
                                        Database.getUser(pickup.userId) { pickupUser ->
                                            val message = pickupUser.name + " has left your ride"
                                            getProfilePicUrl(pickupUser.facebookProfileId) { picUrl ->
                                                Database.sendFirebaseNotification(
                                                    to, title, message, picUrl,
                                                    "com.google.firebase.RIDE_PAGE", keys
                                                )
                                            }
                                        }
                                    }
                                    mainActionButton.isEnabled = false
                                    mainActionButton.setBackgroundResource(R.drawable.button_disabled)
                                    mainActionButton.text = getString(string.updating)
                                }
                                .setNegativeButton(string.no, null).show()
                        }
                    }
                    else -> {
                        // Pickup request is still pending
                        leaveRideMenuButtonText = getString(string.cancel_request)
                        mainActionButton.isEnabled = false
                        mainActionButton.setBackgroundResource(R.drawable.button_disabled)
                        mainActionButton.text = getString(string.request_is_pending)

                        // the listener is actually a listener for the options menu button, now
                        mainActionButton.setOnClickListener {
                            AlertDialog.Builder(this, style.AlertDialogStyle)
                                .setTitle(string.cancel_request)
                                .setMessage(getString(string.cancel_request_are_you_sure))
                                .setPositiveButton(string.yes) { _, _ ->
                                    val pickup = pickups.single { p -> p.userId == Database.idOfCurrentUser }
                                    Database.deletePickup(pickup.id) {
                                        Toast.makeText(
                                            this,
                                            getString(string.toast_cancel_request_success),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        recreate() // to be updated
                                    }
                                    mainActionButton.isEnabled = false
                                    mainActionButton.setBackgroundResource(R.drawable.button_disabled)
                                    mainActionButton.text = getString(string.updating)
                                }
                                .setNegativeButton(string.no, null).show()
                        }
                    }
                }
                invalidateOptionsMenu()
            }
        }
    }

    private fun updatePassengers(rideId: Id) {
        Database.getPickupsForRide(rideId) { allPickups: List<Pickup> ->
            val confirmedPickups = allPickups.filter { it.inRide }
            viewAdapter = MyAdapter(this, confirmedPickups.toTypedArray())
            recyclerView = findViewById<RecyclerView>(id.ride_page_recyclerview).apply {
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
            menu.findItem(id.action_edit_ride).isVisible = false
            if (leaveRideMenuButtonText == null)
                menu.findItem(id.action_delete_ride).isVisible = false
            else {
                menu.findItem(id.action_delete_ride).isVisible = true
                menu.findItem(id.action_delete_ride).title = leaveRideMenuButtonText
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            id.action_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            android.R.id.home -> {
                Log.i(tag, "OnBackPressed")
                onBackPressed()
                true
            }
            id.action_edit_ride -> {
//                val intent = Intent(applicationContext, RideCreationActivity::class.java)
//                intent.putExtra(Keys.RIDE_ID.name, rideId)
//                startActivity(intent)
                Toast.makeText(this, "Sorry, editing rides is unsupported right now!", Toast.LENGTH_LONG).show()
                true
            }
            id.action_delete_ride -> {

                // The action_delete_ride button is actually used for deleting a ride, canceling a pending request, and leaving a ride.
                if (!driversPerspective) {
                    // because we're dirty shortcut-takers, the callback is defined somewhere else
                    mainActionButton.callOnClick()
                    return true
                }

                //else - normal button behaviour
                AlertDialog.Builder(this, style.AlertDialogStyle)
                    .setTitle(string.delete_ride_title)
                    .setMessage(getString(string.delete_ride_are_you_sure))
                    .setPositiveButton(
                        string.yes
                    ) { _, whichButton ->
                        if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                            Toast.makeText(
                                this@RidePageActivity,
                                "Your ride was deleted.",
                                Toast.LENGTH_LONG
                            ).show()
                            Database.getUser(ride.driverId) { driver ->
                                val keys = HashMap<String, Any>()
                                keys[Keys.EVENT_ID.name] = ride.eventId
                                val activityName = "com.google.firebase.EVENT_RIDES"
                                val title = getString(R.string.notification_title_ride_canceled)
                                val message = driver.name + " has canceled their ride"
                                //val toList = ArrayList<String>()
                                getProfilePicUrl(driver.facebookProfileId) { picUrl ->
                                    Database.getPickupsForRide(ride.id) { pickups ->
                                        for (pickup in pickups) {
                                            Log.d("firebase", "!!!!" + pickup.userId.toString())
                                            Database.getUser(pickup.userId) { pickupUser ->
                                                val to = arrayOf(pickupUser.firebaseId)
                                                Database.sendFirebaseNotification(
                                                    to, title, message, picUrl,
                                                    activityName, keys
                                                )
                                            }
                                        }
                                        //val to = toList.toTypedArray()
                                        Database.deleteRide(rideId)
                                    }
                                }

                            }
                            val intent = Intent(applicationContext, EventRidesActivity::class.java)
                            intent.putExtra(Keys.EVENT_ID.name, ride.eventId)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton(string.no, null).show()
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
                .inflate(layout.card_ride_page, parent, false) as CardView

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
                        .placeholder(drawable.placeholder_profile_circle)
                        .error(drawable.placeholder_profile_circle)
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

