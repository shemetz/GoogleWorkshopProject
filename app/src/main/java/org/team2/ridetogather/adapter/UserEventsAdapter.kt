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

class UserEventsAdapter(val items : ArrayList<Event>, val context: Context?,var itemClickListener: ItemClickListener?) : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.user_event, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder?.tvTitle?.text = items.get(position).name
            holder?.tvLocation?.text =readableLocation(context, items.get(position).location)
            holder?.tvDateTime?.text = java.text.SimpleDateFormat("EEEE, dd/M/yy 'at' HH:mm", Locale.getDefault()).format(items.get(position).datetime)
            holder?.card_view.setOnClickListener(View.OnClickListener {

                itemClickListener?.onItemClicked(holder,items.get(position),position)
            })

        }

        // Gets the number of animals in the list
        override fun getItemCount(): Int {
            return items.size
        }


    }

    class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val card_view = view.card_view
    val tvTitle = view.tvTitle
    val tvLocation = view.tvLocation
    val tvDateTime = view.tvDateTime
}

interface ItemClickListener {

    fun onItemClicked(vh: ViewHolder, item: Any, pos: Int)
}
