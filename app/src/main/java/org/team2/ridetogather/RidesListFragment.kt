package org.team2.ridetogather

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.content_eventrides.*


/**
 * A simple [Fragment] subclass.
 */
class RidesListFragment : Fragment() {

    private var eventId: Id = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        eventId = arguments!!.getInt(Keys.EVENT_ID.name)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rides_list, container, false)
    }

    override fun onStart() {
        super.onStart()
        rides_list_recycler_view.apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = EventRidesActivity.MyAdapter(context, emptyArray())
            if (itemDecorationCount == 0) // this fixes a dumb bug
                addItemDecoration(EventRidesActivity.MarginItemDecoration(resources.getDimension(R.dimen.ride_card_margin).toInt()))
        }
    }

    private fun updateRides() {
        Database.getRidesForEvent(eventId) { rides ->
            rides_list_recycler_view.adapter = EventRidesActivity.MyAdapter(context!!, rides.toTypedArray())
            rides_list_recycler_view.adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        updateRides()
    }
}