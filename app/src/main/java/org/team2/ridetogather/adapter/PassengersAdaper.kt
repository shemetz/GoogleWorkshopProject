package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.card_ride.view.*
import kotlinx.android.synthetic.main.user_event.view.*
import org.team2.ridetogather.*
import kotlin.reflect.jvm.internal.impl.incremental.UtilsKt

public class PassengerssAdapter(val items : ArrayList<User>, val context: Context?) : RecyclerView.Adapter<ViewHolderPes>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPes {
        return ViewHolderPes(LayoutInflater.from(context).inflate(R.layout.item_passengers, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderPes, position: Int) {


        holder?.driverName?.text = items.get(position).name


    }

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return items.size
    }


}

public class ViewHolderPes (view: View) : RecyclerView.ViewHolder(view) {
    // Holds the TextView that will add each animal to
    val driverName = view.driverName

}

