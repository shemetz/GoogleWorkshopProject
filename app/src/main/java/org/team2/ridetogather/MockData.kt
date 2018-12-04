package org.team2.ridetogather

import java.text.SimpleDateFormat
import java.util.*

object MockData {

    val user1 = User(1, "Adam Driver", "fake facebook profile id 1")
    val user2 = User(2, "Billy Bobson", "fake facebook profile id 2")
    val user3 = User(3, "Claire White", "fake facebook profile id 3")
    val user4 = User(4, "Donna Spielstein", "fake facebook profile id 4")
    val user5 = User(5, "Eugene Smith", "fake facebook profile id 5")
    val location1 = Location("useless paramater")
        .apply { latitude = 32.113859; longitude = 34.804167 } // TAU
    val location2 = Location("useless paramater")
        .apply { latitude = 32.071116; longitude = 34.783335 } // Cinema
    val location3 = Location("useless paramater")
        .apply { latitude = 32.055436; longitude = 34.753070 } // Museum
    val event1 = Event(
        1,
        "Summer Party",
        location3,
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY).parse("2019-02-18T15:00:00"),
        "fake facebook event id 1"
    )
    val ride1 = Ride(
        1,
        user1.id,
        event1,
        location1,
        location3,
        TimeOfDay(12, 0),
        "Tesla S",
        "Black",
        4,
        "please no dogs"
    )
    val ride2 = Ride(
        2,
        user4.id,
        event1,
        location2,
        location3,
        TimeOfDay(12, 20),
        "Honda",
        "Red",
        6
    )
    val pickup1 = Pickup(
        1,
        user2.id,
        ride1.id,
        location1,
        TimeOfDay(12, 34)
    )
    val pickup2 = Pickup(
        2,
        user3.id,
        ride1.id,
        location2,
        TimeOfDay(12, 55)
    )

    // main tables
    var users: HashMap<Int, User> = hashMapOf(1 to user1, 2 to user2, 3 to user3, 4 to user4)
    var events: HashMap<Int, Event> = hashMapOf(1 to event1)
    var rides: HashMap<Int, Ride> = hashMapOf(1 to ride1, 2 to ride2)
    var pickups: HashMap<Int, Pickup> = hashMapOf(1 to pickup1, 2 to pickup2)
    // relation/connection/pair tables. for example, pickupsOfRide[ride1.id] == listOf(pickup1.id, pickup2.id)
    var eventsOfUser: HashMap<Int, MutableList<Int>> =
        hashMapOf(1 to mutableListOf(1), 2 to mutableListOf(1), 3 to mutableListOf(1), 4 to mutableListOf())
    var ridesOfEvent: HashMap<Int, MutableList<Int>> = hashMapOf(1 to mutableListOf(1, 2))
    var pickupsOfRide: HashMap<Int, MutableList<Int>> = hashMapOf(1 to mutableListOf(1, 2))
}
