package il.ac.tau.team2.googleworkshopproject

import java.text.SimpleDateFormat
import java.util.*

val user1 = User(1, "Adam Driver", "fake facebook profile id 1")
val user2 = User(2, "Billy Bobson", "fake facebook profile id 2")
val user3 = User(3, "Claire White", "fake facebook profile id 3")
val user4 = User(4, "Donna Spielstein", "fake facebook profile id 4")
val location1 = Location("Tel Aviv")
val location2 = Location("Haifa")
val location3 = Location("Eilat")
val pickup1 = Pickup(1, user2, location1, TimeOfDay(12, 34))
val pickup2 = Pickup(2, user3, location2, TimeOfDay(12, 55))
val ride1 = Ride(
        1,
        user1,
        location1,
        location3,
        TimeOfDay(12, 0),
        "Tesla S",
        "Black",
        "please no dogs",
        listOf(pickup1, pickup2))
val ride2 = Ride(
        2,
        user4,
        location1,
        location3,
        TimeOfDay(12, 20),
        "Honda",
        "Red")
val event1 = Event(
        1,
        "Summer Party",
        location3,
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY).parse("2019-02-18T15:00:00"),
        "fake facebook event id 1"
)