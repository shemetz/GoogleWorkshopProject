package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.user_event.view.*
import org.team2.ridetogather.*
import kotlin.reflect.jvm.internal.impl.incremental.UtilsKt

class MyRidesAdapter(val items : ArrayList<Ride>, val context: Context?,var itemClickListener: ItemClickListener?) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.user_event, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       Database.getEvent(items.get(position).eventId,{event:Event->
           holder?.tvTitle?.text = event.name
       });
        Database.getDriver(items.get(position).driverId,{driver:Driver->
            holder?.tvLocation?.text = driver.name
        });


        holder?.tvDateTime?.text = items.get(position).departureTime.toString()
        holder?.card_view.setOnClickListener(View.OnClickListener {

            itemClickListener?.onItemClicked(holder,items.get(position),position)
        })

    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }


}



