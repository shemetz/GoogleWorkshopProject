package org.team2.ridetogather

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.content_eventrides.*


/**
 * A simple [Fragment] subclass.
 */
class PassengersListFragment : Fragment() {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var recyclerView: RecyclerView
    private var eventId: Id = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        eventId = arguments!!.getInt(Keys.EVENT_ID.name)
        //mock
        val rides = Database.getRidesForEvent(eventId).toTypedArray()
        viewManager = LinearLayoutManager(context)
        viewAdapter = EventRidesActivity.MyAdapter(context, rides)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_passengers_list, container, false)

    }

    override fun onStart() {
        super.onStart()

        recyclerView = rides_list_recycler_view.apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(EventRidesActivity.MarginItemDecoration(resources.getDimension(R.dimen.ride_card_margin).toInt()))
        }

    }

    override fun onResume() {
        super.onResume()
        val rides = Database.getRidesForEvent(eventId).toTypedArray()
        viewAdapter = EventRidesActivity.MyAdapter(context!!, rides)
        viewAdapter.notifyDataSetChanged()
        recyclerView.adapter = viewAdapter
    }
}