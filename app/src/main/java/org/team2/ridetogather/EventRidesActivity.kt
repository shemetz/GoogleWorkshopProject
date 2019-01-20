package org.team2.ridetogather

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.Log
import android.view.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_eventrides.*
import kotlinx.android.synthetic.main.card_ride.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class EventRidesActivity : AppCompatActivity() {
    private val tag = EventRidesActivity::class.java.simpleName

    companion object {
        fun start(context: Context?,evenid:Int?) {
            val intent = Intent(context,EventRidesActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name,evenid)
            context?.startActivity(intent)

        }
    }

    private var eventId: Id = -1
    private var facebookEventId: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventrides)
        setSupportActionBar(toolbar)

        // The next line gets the event ID either from the intent extras or from the saved activity state.
        eventId = intent.getIntExtra(Keys.EVENT_ID.name, savedInstanceState?.getInt(Keys.EVENT_ID.name) ?: -1)

        if (eventId == -1) {
            Log.e(tag, "No event ID found in intent or in saved state!")
            Log.w(tag, "Due to error, returning to main activity.")
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        val fragmentAdapter = MyPagerAdapter(supportFragmentManager, eventId)
        viewpager_main.adapter = fragmentAdapter

        tabs_main.setupWithViewPager(viewpager_main)
        tabs_main.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab!!.position == 0)
                    fab_create_ride.show()
                else
                    fab_create_ride.hide()
            }

        })
        Database.getEvent(eventId) { event: Event ->
            val eventTime =
                java.text.SimpleDateFormat("EEEE, dd/M/yy 'at' HH:mm", Locale.getDefault()).format(event.datetime)
            facebookEventId = event.facebookEventId
            toolbar_layout.title = event.name
            toolbar_layout.setExpandedTitleColor(ContextCompat.getColor(this, R.color.title_light))
            CoroutineScope(Dispatchers.Default).launch {
                val eventShortLocation = readableLocation(this@EventRidesActivity, event.location)
                CoroutineScope(Dispatchers.Main).launch {
                    tv_description.text = eventShortLocation
                }
            }
            tv_title.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(eventTime, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(eventTime)
            }
            Log.d(tag, "Created $tag with Event ID $eventId")

            toolbar_layout.title = event.name
        }


        viewManager = LinearLayoutManager(this)

        Database.getRidesForEvent(eventId) { rides: List<Ride> ->
            viewAdapter = MyAdapter(this, rides.toTypedArray())

            recyclerView = findViewById<RecyclerView>(R.id.rides_list_recycler_view).apply {
                // changes in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
                addItemDecoration(MarginItemDecoration(resources.getDimension(R.dimen.ride_card_margin).toInt()))
            }
        }
        refreshRecyclerView()
    }

    private fun refreshRecyclerView() {
        fab_create_ride.visibility = View.GONE
        fab_create_ride.hide()
        Database.getRidesForEvent(eventId) { rides: List<Ride> ->
            fab_create_ride.visibility = View.VISIBLE
            fab_create_ride.show()
            viewAdapter = MyAdapter(this, rides.toTypedArray())
            viewAdapter.notifyDataSetChanged()
            recyclerView.adapter = viewAdapter

            val rideCreatedByThisUser = rides.singleOrNull { it.driverId == Database.idOfCurrentUser }
            if (rideCreatedByThisUser == null) {
                fab_create_ride.setImageDrawable(getDrawable(R.drawable.ic_create_ride))
                fab_create_ride.setOnClickListener {
                    // create a new ride
                    val intent = Intent(this, RideCreationActivity::class.java)
                    intent.putExtra(Keys.EVENT_ID.name, eventId)
                    startActivity(intent)
                }
            } else {
                fab_create_ride.setImageDrawable(getDrawable(R.drawable.ic_open_existing_ride))
                fab_create_ride.setOnClickListener {
                    // go to pre-existing ride, instead of creating a new one
                    val intent = Intent(this, RidePageActivity::class.java)
                    intent.putExtra(Keys.RIDE_ID.name, rideCreatedByThisUser.id)
                    intent.putExtra(Keys.DRIVER_PERSPECTIVE.name, true)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRecyclerView()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(Keys.EVENT_ID.name, eventId)
        super.onSaveInstanceState(outState)
    }

    class MyAdapter(private val context: Context, private val rides: Array<Ride>) :
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
                .inflate(R.layout.card_ride, parent, false) as CardView

            return MyViewHolder(cardView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val view = holder.cardView
            val ride = rides[position]

            Database.getDriver(ride.driverId) { driver: Driver ->
                view.driverName.text = driver.name

                val facebookId = driver.facebookProfileId
                getProfilePicUrl(facebookId) { pic_url ->
                    Picasso.get()
                        .load(pic_url)
                        .placeholder(R.drawable.placeholder_profile)
                        .error(R.drawable.placeholder_profile)
                        .resize(256, 256)
                        .transform(CircleTransform())
                        .into(view.driverPicture)
                }
            }
            view.originLocationName.text = readableLocation(context, ride.origin)
//            view.driverPicture.drawable = ???

            view.departureTime.text = ride.departureTime.shortenedTime()
            holder.cardView.setOnClickListener {
                val intent = Intent(view.context, RidePageActivity::class.java)
                val rideID = ride.id
                intent.putExtra(Keys.RIDE_ID.name, rideID)
                intent.putExtra(Keys.DRIVER_PERSPECTIVE.name, ride.driverId == Database.idOfCurrentUser)
                view.context.startActivity(intent)
            }
            /*holder.cardView.joinRidePlusButton.setOnClickListener{
                val intent = Intent(context,JoinRideActivity::class.java)
                intent.putExtra(Keys.RIDE_ID.name, ride.id)
                startActivity(context, intent,null)}*/
        }

        override fun getItemCount() = rides.size
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_eventrides, menu)
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
            R.id.action_open_event_on_facebook -> {
                if (facebookEventId == null) {
                    Log.i(tag, "User pressed \"Open on Facebook\" too fast! ID is still null!")
                    return true
                }
                val url = "https://www.facebook.com/events/$facebookEventId"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class MarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }
                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }
    }