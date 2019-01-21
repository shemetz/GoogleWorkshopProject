package org.team2.ridetogather.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.team2.ridetogather.Database
import org.team2.ridetogather.R
import org.team2.ridetogather.Ride
import org.team2.ridetogather.RidePageActivity
import org.team2.ridetogather.adapter.ItemClickListener
import org.team2.ridetogather.adapter.MyRidesAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MyRides.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MyRides.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MyRides : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    var list = arrayListOf<Ride>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_rides, container, false)
        Database.getRidesForUser(Database.idOfCurrentUser) { rides: List<Ride> ->
            if (rides.isNotEmpty()) {
                list.addAll(rides)
                val recycle = view.findViewById<RecyclerView>(R.id.recycle)
                val myRidesAdapter = MyRidesAdapter(list, context!!, object : ItemClickListener {
                    override fun onItemClicked(item: Any, pos: Int) {
                        val ride: Ride = item as Ride
                        RidePageActivity.start(context, ride.id, ride.driverId == Database.idOfCurrentUser)
                    }
                })
                recycle.layoutManager = LinearLayoutManager(context)
                recycle.adapter = myRidesAdapter
            } else {
                val recycle = view.findViewById<TextView>(R.id.tvEmpty)
                recycle.visibility = View.VISIBLE
            }

        }
        return view
    }

    internal var context: Context? = null
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.context = context as Activity?

    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        this.context = activity

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MyRides.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyRides().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
