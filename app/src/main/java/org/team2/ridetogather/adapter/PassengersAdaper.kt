package org.team2.ridetogather.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_passengers.view.*
import org.team2.ridetogather.CircleTransform
import org.team2.ridetogather.R
import org.team2.ridetogather.User
import org.team2.ridetogather.getProfilePicUrl

class PassengersAdapter(val items: ArrayList<User>, val context: Context?) : RecyclerView.Adapter<ViewHolderPes>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPes {
        return ViewHolderPes(LayoutInflater.from(context).inflate(R.layout.item_passengers, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderPes, position: Int) {


        holder.passengerName?.text = items.get(position).name

        val facebookId = items.get(position).facebookProfileId
        getProfilePicUrl(facebookId) { pic_url ->
            Picasso.get()
                .load(pic_url)
                .placeholder(R.drawable.placeholder_profile_circle)
                .error(R.drawable.placeholder_profile_circle)
                .resize(256, 256)
                .transform(CircleTransform())
                .into(holder.passengerPicture)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class ViewHolderPes(view: View) : RecyclerView.ViewHolder(view) {
    val passengerName = view.passengerName
    val passengerPicture = view.passengerPicture
}

