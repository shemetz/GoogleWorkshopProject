package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.user_event.view.*
import org.team2.ridetogather.*
import java.util.*

class UserEventsAdapter(val items: ArrayList<Event>, val context: Context, var itemClickListener: ItemClickListener?) :
    RecyclerView.Adapter<ViewHolderEvent>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderEvent {
        return ViewHolderEvent(LayoutInflater.from(context).inflate(R.layout.user_event, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderEvent, position: Int) {
        holder?.eventTitle?.text = items.get(position).name
        geocode(context, items.get(position).location.toLatLng()) {
            holder?.eventLocation?.text = it
        }
        holder?.eventDateTime?.text = formatDatetime(items.get(position).datetime)

        val facebookId = items.get(position).facebookEventId
        getEventUrl(facebookId) { pic_url ->
            Picasso.get()
                .load(pic_url)
                .placeholder(R.drawable.placeholder_profile)
                .error(R.drawable.placeholder_profile)
                .resize(256, 256)
                .into(holder?.eventPicture)
        }

        holder?.card_view.setOnClickListener(View.OnClickListener {

            itemClickListener?.onItemClicked(items.get(position), position)
        })

    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }


}

class ViewHolderEvent(view: View) : RecyclerView.ViewHolder(view) {
    val card_view = view.card_view
    val eventTitle = view.eventTitle
    val eventLocation = view.eventLocation
    val eventDateTime = view.eventDateTime
    val eventPicture = view.eventPicture
}

interface ItemClickListener {

    fun onItemClicked(item: Any, pos: Int)
}
