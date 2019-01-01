package org.team2.ridetogather

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class MyPagerAdapter(fm: FragmentManager, private val eventId: Id) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val returned = when (position) {
            0 -> {
                RidesListFragment()
            }
            else -> {
                PassengersListFragment()
            }
        }

        returned.arguments = Bundle()
        returned.arguments!!.putInt(Keys.EVENT_ID.name, eventId)
        return returned
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "RIDES"
            else -> {
                return "PASSENGERS"
            }
        }
    }
}