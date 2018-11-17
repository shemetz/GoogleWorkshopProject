package il.ac.tau.team2.googleworkshopproject

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup

import kotlinx.android.synthetic.main.activity_rideslist.*
import kotlinx.android.synthetic.main.card_ride.view.*

class RidesListActivity : AppCompatActivity() {
    private lateinit var event: Event

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    enum class IntentExtraKeys {
        EVENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rideslist)
        setSupportActionBar(toolbar)

        val eventId = intent.getIntExtra(RidesListActivity.IntentExtraKeys.EVENT.name, -1)
        event = Database.getEvent(eventId)!!

        toolbar_layout.title = event.name

        fab.setOnClickListener { _ ->
            val intent = Intent(applicationContext, NewRiderActivity::class.java)
            intent.putExtra(RidesListActivity.IntentExtraKeys.EVENT.name, event.id)
            startActivity(intent)
        }

        // mock
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
            holder.cardView.setOnClickListener { _ ->
                val intent = Intent(view.context, RidePageActivity::class.java)
                val rideID = ride.id
                intent.putExtra(RidePageActivity.IntentExtraKeys.RIDEID.name, rideID)
                view.context.startActivity(intent)
            }
        }

        override fun getItemCount() = rides.size
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_rideslist, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
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
