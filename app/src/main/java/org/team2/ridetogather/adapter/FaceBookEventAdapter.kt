package org.team2.ridetogather.adapter

import android.content.Context
import android.location.Location
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.facebook_event.view.*
import org.team2.ridetogather.Datetime
import org.team2.ridetogather.R
import org.team2.ridetogather.getEventUrl
import java.util.*

class FacebookEventAdapter(val items : ArrayList<FacebookEvent>, val context: Context?,var itemClickListener: ItemClickListener?) : RecyclerView.Adapter<ViewHolderFacebookEvent>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFacebookEvent {
        return ViewHolderFacebookEvent(LayoutInflater.from(context).inflate(R.layout.facebook_event, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderFacebookEvent, position: Int) {
        holder.eventTitle?.text = items[position].name
        holder.eventLocation?.text = items[position].location
        holder.eventDateTime?.text = items[position].datetime

        val facebookId = items[position].id
        getEventUrl(facebookId) { pic_url ->
            Picasso.get()
                .load(pic_url)
                .placeholder(R.drawable.ic_tab_events)
                .error(R.drawable.ic_tab_events)
                .into(holder.eventPicture)
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

class FacebookEvent(var id: String, var name: String, var location: String, var datetime: String,var loc:Location,var dt:String)

class ViewHolderFacebookEvent (view: View) : RecyclerView.ViewHolder(view) {
    val card_view = view.card_view
    val eventTitle = view.eventTitle
    val eventLocation = view.eventLocation
    val eventDateTime = view.eventDateTime
    val eventPicture = view.eventPicture
}


