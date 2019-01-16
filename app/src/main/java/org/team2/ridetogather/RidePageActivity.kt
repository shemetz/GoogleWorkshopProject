package org.team2.ridetogather

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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ride_page.*
import kotlinx.android.synthetic.main.card_ride_page.view.*
import org.team2.ridetogather.PickupStatus.*


class RidePageActivity : AppCompatActivity() {
    private val tag = RidePageActivity::class.java.simpleName
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var rideId: Id = -1 // updates in onCreate
    private var userId: Id = -1
    private var pickupStatus: PickupStatus = NOT_EXIST
    private lateinit var ride: Ride
    private var driversPerspective: Boolean = false // updates in onCreate


    fun showRideDetails(ride: Ride) {
        carModel.text = ride.carModel
        carColor.text = ride.carColor
        originLocation.text = readableLocation(this, ride.origin)
        departureTimePage.text = ride.departureTime.shortenedTime()
        details.text = ride.extraDetails


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_page)
//        setSupportActionBar(toolbar)
        rideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
        Log.d(tag, "Created $tag with Ride ID $rideId")
        userId =  Database.idOfCurrentUser
        Database.getRide(rideId) { ride: Ride ->
            this.ride = ride
            showRideDetails(ride)
            Database.getDriver(ride.driverId) { driver: Driver ->
                driverNamePage.text = driver.name
                // load profile picture of driver
                Database.getUser(ride.driverId) { user: User ->
                    val facebookId = user.facebookProfileId
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
            }
            driversPerspective = ride.driverId == Database.idOfCurrentUser
            updatePassengers(ride.id)

        }


        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(this, emptyArray())
        recyclerView = findViewById<RecyclerView>(R.id.ride_page_recyclerview).apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }



        joinRideButton.setOnClickListener {
            val intent = Intent(applicationContext, JoinRideActivity::class.java)
            intent.putExtra(Keys.RIDE_ID.name, rideId)
            // request code doesn't matter - the activity doesn't check it
            startActivityForResult(intent,1)
        }

        // Enable join ride button if no pickup request exist.
        updateJoinRideButton()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        updatePassengers(ride.id)
        updateJoinRideButton()
    }

    private fun updateJoinRideButton()  {
        Database.getPickupsForRide(rideId) { pickups: List<Pickup> ->
           for( pickup in pickups){
                if(pickup.userId == userId){
                    pickupStatus =  if (pickup.inRide) APPROVED else PENDING
                    joinRideButton.isEnabled = false
                }
            }
        }
    }

    fun updatePassengers(rideId: Id) {
        Database.getPickupsForRide(rideId) { pickups: List<Pickup> ->
            viewAdapter = MyAdapter(this, pickups.toTypedArray())
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
                        android.R.string.yes
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
                    .setNegativeButton(android.R.string.no, null).show()
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

            view.pickupSpot.text = readableLocation(context, pickup.pickupSpot)
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

