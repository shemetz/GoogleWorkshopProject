package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.user_ride.view.*
import org.team2.ridetogather.*
import java.util.*

class MyRidesAdapter(val items: ArrayList<Ride>, val context: Context, var itemClickListener: ItemClickListener?) :
    RecyclerView.Adapter<ViewHolderRide>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderRide {
        return ViewHolderRide(LayoutInflater.from(context).inflate(R.layout.user_ride, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderRide, position: Int) {
        val ride = items[position]
        Database.getEvent(ride.eventId) { event: Event ->
            holder.eventName?.text = event.name
            holder.eventDateTime?.text = formatDatetime(event.datetime)
            geocode(context, event.location.toLatLng()) {
                holder.eventLocation?.text = it
            }
        }
        Database.getDriver(ride.driverId) { driver: Driver ->
            holder.driverName?.text = driver.name
            Database.getPickupsForRide(ride.id) { pickups ->
                val numOfExistingPassengers = pickups.count { it.inRide }
                val passengerCountText = "$numOfExistingPassengers/${ride.passengerCount}"
                holder.card_view.findViewById<TextView>(R.id.passengerCount).text = passengerCountText
            }
            val facebookId = driver.facebookProfileId
            getProfilePicUrl(facebookId) { pic_url ->
                Picasso.get()
                    .load(pic_url)
                    .placeholder(R.drawable.placeholder_profile_circle)
                    .error(R.drawable.placeholder_profile_circle)
                    .resize(256, 256)
                    .transform(CircleTransform())
                    .into(holder.driverPicture)
            }

        }


        holder.card_view.setOnClickListener {
            itemClickListener?.onItemClicked(ride, position)
        }

    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }


}

class ViewHolderRide(view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val card_view = view.card_view
    val driverName = view.driverName
    val eventName = view.eventName
    val eventLocation = view.eventLocation
    val eventDateTime = view.eventDateTime
    val driverPicture = view.img

}





