package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.user_event.view.*
import org.team2.ridetogather.Event
import org.team2.ridetogather.R
import org.team2.ridetogather.readableLocation
import java.util.*
import kotlin.reflect.jvm.internal.impl.incremental.UtilsKt

class UserEventsAdapter(val items : ArrayList<Event>, val context: Context?,var itemClickListener: ItemClickListener?) : RecyclerView.Adapter<ViewHolderEvent>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderEvent {
            return ViewHolderEvent(LayoutInflater.from(context).inflate(R.layout.user_event, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolderEvent, position: Int) {
            holder?.eventTitle?.text = items.get(position).name
            holder?.eventLocation?.text =readableLocation(context, items.get(position).location)
            holder?.eventDateTime?.text = java.text.SimpleDateFormat("EEEE, dd/M/yy 'at' HH:mm", Locale.getDefault()).format(items.get(position).datetime)
            holder?.card_view.setOnClickListener(View.OnClickListener {

                itemClickListener?.onItemClicked(items.get(position),position)
            })

        }

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return items.size
        }


    }

    class ViewHolderEvent (view: View) : RecyclerView.ViewHolder(view) {
        val card_view = view.card_view
        val eventTitle = view.eventTitle
        val eventLocation = view.eventLocation
        val eventDateTime = view.eventDateTime
        val eventPicture = view.eventPicture
}

interface ItemClickListener {

    fun onItemClicked( item: Any, pos: Int)
}
