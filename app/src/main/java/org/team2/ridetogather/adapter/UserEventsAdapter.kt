package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.facebook_event.view.*
import org.team2.ridetogather.*
import java.util.*

class UserEventsAdapter(private val items: ArrayList<Event>, val context: Context, private var itemClickListener: ItemClickListener?) :
    RecyclerView.Adapter<ViewHolderEvent>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderEvent {
        return ViewHolderEvent(LayoutInflater.from(context).inflate(R.layout.facebook_event, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderEvent, position: Int) {
        holder.eventTitle.text = items[position].name
        geocode(context, items[position].location.toLatLng()) {
            holder.eventLocation.text = it
        }
        holder.eventDateTime?.text = formatDatetime(items[position].datetime)

        val facebookId = items[position].facebookEventId
        getEventUrl(facebookId) { pic_url ->
            Picasso.get()
                .load(pic_url)
                .placeholder(R.drawable.ic_tab_events)
                .error(R.drawable.ic_tab_events)
                .into(holder.eventPicture)
        }

        holder.card_view.setOnClickListener {
            itemClickListener?.onItemClicked(items[position], position)
        }
    }

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
