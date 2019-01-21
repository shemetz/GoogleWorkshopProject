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
import org.team2.ridetogather.adapter.PassengerssAdapter


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
        //mock - we don't have passenger cards yet!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_passengers_list, container, false)
        return view;

    }

    override fun onStart() {
        super.onStart()

        recyclerView = rides_list_recycler_view.apply {
            // changes in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(EventRidesActivity.MarginItemDecoration(resources.getDimension(R.dimen.ride_card_margin).toInt()))
        }

    }

    var list = arrayListOf<User>();
    private fun updatePassengers() {
        Database.getUsersForEvent(eventId) { users ->
            if (users.size != 0) {
                list.clear()
                if (rides_list_recycler_view.adapter != null) {
                    val adapter = rides_list_recycler_view.adapter as PassengerssAdapter
                    val size = adapter.itemCount
                    adapter.items.clear()
                    adapter.notifyItemRangeRemoved(0, size)
                }
                list.addAll(users)
                rides_list_recycler_view.adapter = PassengerssAdapter(list, context)
                rides_list_recycler_view.adapter.notifyDataSetChanged()
            } else {

                tvEmpty.visibility = View.VISIBLE
            }

        }
    }

    override fun onResume() {
        super.onResume()
        updatePassengers();
    }
}