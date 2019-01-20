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
import kotlin.reflect.jvm.internal.impl.incremental.UtilsKt

class FaceBookEventAdapter(val items : ArrayList<FaceBookEvent>, val context: Context?) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.user_event, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder?.tvTitle?.text = items.get(position).name
        holder?.tvLocation?.text =items.get(position).location
        holder?.tvDateTime?.text = items.get(position).datetime.toString()


    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }


}

class FaceBookEvent
{
    var name:String="";
    var location:String="";
    var datetime="";

    constructor(name: String, location: String, datetime: String) {
        this.name = name
        this.location = location
        this.datetime = datetime
    }
}


