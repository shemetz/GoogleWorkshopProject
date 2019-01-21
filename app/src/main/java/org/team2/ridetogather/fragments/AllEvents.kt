package org.team2.ridetogather.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.android.gms.maps.model.LatLng
import org.team2.ridetogather.*
import org.team2.ridetogather.adapter.FacebookEvent
import org.team2.ridetogather.adapter.FacebookEventAdapter
import org.team2.ridetogather.adapter.ItemClickListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AllEvents.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AllEvents.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AllEvents : Fragment() {
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

    fun createEvent(context: Context?,facebookEvent: FacebookEvent){
        val builder = AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
        builder.setMessage("There is no group for this event, do you want to create one?")

        builder.setPositiveButton(R.string.yes) { dialog, which ->
            Database.addEventWithCallback(facebookEvent.name,facebookEvent.loc,facebookEvent.dt,facebookEvent.id)
                {event: Event ->
                    Database.addEventToUser(Database.idOfCurrentUser,event.id)
                    EventRidesActivity.start(context,event.id)
            }
        }
        builder.setNegativeButton(R.string.no) { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    fun joinEvent(context: Context?,event:Event){

        Database.getAttendingByIds(Database.idOfCurrentUser,event.id,{attending: Attending ->
            EventRidesActivity.start(context,event.id)
        },{
            val builder = AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
            builder.setMessage("There is already group for this event, do you want to join?")

            builder.setPositiveButton(R.string.yes) { dialog, which ->
                Database.addEventToUser(Database.idOfCurrentUser,event.id)
                EventRidesActivity.start(context,event.id)
            }
            builder.setNegativeButton(R.string.no) { dialog, which ->
                dialog.dismiss()
            }
            builder.show()
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_all_events, container, false)
        val listView = view.findViewById<ListView>(R.id.events_list)
        val event_request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken()
        ) { Json, response ->
            try {
                val eventsArray = Json.getJSONObject("events").getJSONArray("data")
                val eventsList:ArrayList<FacebookEvent> =  ArrayList()
                for (i in 0..(eventsArray.length() - 1)) {
                    val eventId = eventsArray.optJSONObject(i).getString("id")
                    val eventName = eventsArray.optJSONObject(i).getString("name")
                    val dt = eventsArray.optJSONObject(i).getString("start_time")
                    val datetime = formatDatetime(parseStandardDatetime(eventsArray.optJSONObject(i).getString("start_time")))
                    val placeObject = eventsArray.optJSONObject(i).optJSONObject("place")
                    if (placeObject != null){


                        val locationObject = placeObject.getJSONObject("location")
                        val location = placeObject.getString("name")+", "+locationObject.getString("city")+", "+locationObject.getString("country")
                        val loc = LatLng(
                            locationObject.getDouble("latitude"),
                            locationObject.getDouble("longitude")
                        ).toLocation()
                        eventsList.add(FacebookEvent(eventId,eventName,location,datetime,loc,dt))
                    }
                }
                if(eventsList.size!=0) {


                    val recycle = view.findViewById<RecyclerView>(R.id.recycle)
                    val userEventAdapter = FacebookEventAdapter(eventsList, context, object : ItemClickListener {
                        override fun onItemClicked( item: Any, pos: Int) {
                            val facebookEvent:FacebookEvent = item as FacebookEvent
                            Database.getEventByFacebook(facebookEvent.id,{event: Event ->
                                joinEvent(context,event)
                            }, {createEvent(context,facebookEvent)})
                        }
                    })
                    recycle.layoutManager = LinearLayoutManager(context)
                    recycle.adapter = userEventAdapter
                }
                else
                {
                    val recycle = view.findViewById<TextView>(R.id.tvEmpty)
                    recycle.visibility=View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "events")
        event_request.parameters = parameters
        event_request.executeAsync()
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
         * @return A new instance of fragment AllEvents.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AllEvents().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
