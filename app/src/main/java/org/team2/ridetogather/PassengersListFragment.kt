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
import org.team2.ridetogather.adapter.PassengersAdapter


/**
 * A simple [Fragment] subclass.
 */
class PassengersListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var eventId: Id = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        eventId = arguments!!.getInt(Keys.EVENT_ID.name)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_passengers_list, container, false)
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

    var list = arrayListOf<User>()
    private fun updatePassengers() {
        Database.getUsersForEvent(eventId) { users ->
            rides_list_recycler_view ?: return@getUsersForEvent  // dirty patch that doesn't fix the actual issue
            if (users.isNotEmpty()) {
                list.clear()
                if (rides_list_recycler_view.adapter != null) {
                    val adapter = rides_list_recycler_view.adapter as PassengersAdapter
                    val size = adapter.itemCount
                    adapter.items.clear()
                    adapter.notifyItemRangeRemoved(0, size)
                }
                list.addAll(users)
                rides_list_recycler_view.adapter = PassengersAdapter(list, context)
                rides_list_recycler_view.adapter.notifyDataSetChanged()
            } else {
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePassengers()
    }
}