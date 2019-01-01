package org.team2.ridetogather

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_eventrides.*
import kotlinx.android.synthetic.main.activity_ride_page.*
import kotlinx.android.synthetic.main.card_ride.*
import kotlinx.android.synthetic.main.card_ride.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class EventRidesActivity : AppCompatActivity() {
    private val tag = EventRidesActivity::class.java.simpleName

    private lateinit var event: Event

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventrides)
        setSupportActionBar(toolbar)

        // The next line gets the event ID either from the intent extras or from the saved activity state.
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, savedInstanceState?.getInt(Keys.EVENT_ID.name) ?: -1)

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
        event = Database.getEvent(eventId)!!
        val eventTime =
            java.text.SimpleDateFormat("EEEE, dd/M/yy 'at' HH:mm", Locale.getDefault()).format(event.datetime)

        toolbar_layout.title = event.name
        toolbar_layout.setExpandedTitleColor(ContextCompat.getColor(this, R.color.title_light))
        CoroutineScope(Dispatchers.Default).launch {
            val eventShortLocation = readableLocation(this@EventRidesActivity, event.location)
            CoroutineScope(Dispatchers.Main).launch {
                tv_description.text = eventShortLocation
            }
        }
        tv_title.text = Html.fromHtml(eventTime)
        Log.d(tag, "Created $tag with Event ID $eventId")

        toolbar_layout.title = event.name
        fab.setOnClickListener {
            val intent = Intent(applicationContext, RideCreationActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, event.id)
            startActivity(intent)
        }

        fab_create_ride.setOnClickListener {
            val intent = Intent(this, RideCreationActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, eventId)
            startActivity(intent)
        }
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(Keys.EVENT_ID.name, event.id)
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
            val driver = Database.getDriver(ride.driverId)!!

            view.driverName.text = driver.name
            view.originLocationName.text = readableLocation(context, ride.origin)
//            view.driverPicture.drawable = ???
            view.departureTime.text = ride.departureTime.shortenedTime()
            val intent = Intent(view.context, RidePageActivity::class.java)
            holder.cardView.setOnClickListener {
                val rideID = ride.id
                intent.putExtra(Keys.RIDE_ID.name, rideID)
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
