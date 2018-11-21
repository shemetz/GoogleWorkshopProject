package il.ac.tau.team2.googleworkshopproject

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup

import kotlinx.android.synthetic.main.activity_eventrides.*
import kotlinx.android.synthetic.main.card_ride.view.*

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
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        event = Database.getEvent(eventId)!!

        toolbar_layout.title = event.name

        fab.setOnClickListener {
            val intent = Intent(applicationContext, RideCreationActivity::class.java)
            intent.putExtra(Keys.EVENT_ID.name, event.id)
            startActivity(intent)
        }

        // MOCK
        val rides = Database.getRidesForEvent(event.id).toTypedArray()

        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(rides)

        recyclerView = findViewById<RecyclerView>(R.id.rides_list_recycler_view).apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(Keys.EVENT_ID.name, event.id)
        super.onSaveInstanceState(outState)
    }

    class MyAdapter(private val rides: Array<Ride>) :
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
        ): MyAdapter.MyViewHolder {
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

            view.driverName.text = ride.driver.name
            view.originLocationName.text = ride.origin.shortenedLocation()
//            view.driverPicture.drawable = ???
            view.departureTime.text = ride.departureTime.shortenedTime()
            holder.cardView.setOnClickListener {
                val intent = Intent(view.context, RidePageActivity::class.java)
                val rideID = ride.id
                intent.putExtra(Keys.RIDE_ID.name, rideID)
                view.context.startActivity(intent)
            }
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
}