package org.team2.ridetogather

typealias Driver = User  // Currently, they are identical. driver ≡ user
typealias Id = Int  // ID in our database
typealias FacebookId = String // ID in Facebook's API
typealias Location = android.location.Location
typealias Datetime = java.util.Date  // Both a date (2018-12-04) and a time (11:04)

open class DatabaseObject(val id: Id) {
    override fun equals(other: Any?) =
        (other != null && other is DatabaseObject && other::class.java == this::class.java) && id == other.id

    override fun hashCode() = id.hashCode()
}

class Ride(
    id_: Id,
    var driverId: Id,
    val eventId: Id,
    var origin: Location,
    var departureTime: TimeOfDay,
    var carModel: String,
    var carColor: String,
    var passengerCount: Int,
    var extraDetails: String = ""
) : DatabaseObject(id_)

class User(
    id_: Id,
    var name: String,
    var facebookProfileId: FacebookId,
    var credits: Int
) : DatabaseObject(id_)

class Event(
    id_: Id,
    var name: String,
    var location: Location,
    var datetime: Datetime,
    var facebookEventId: FacebookId
) : DatabaseObject(id_)

class Attending(
    id_: Id,
    var userId: Id,
    var eventId: Id,
    var isDriver: Boolean
) : DatabaseObject(id_)

class Pickup(
    id_: Id,
    val userId: Id,
    val rideId: Id,
    var pickupSpot: Location,
    var pickupTime: TimeOfDay,
    var inRide: Boolean,
    var denied: Boolean
) : DatabaseObject(id_)

data class TimeOfDay(
    val hours: Int,
    val minutes: Int
) {
    /**
     * Examples:
     * 07:42
     * 23:05
     */
    fun shortenedTime(): String {
        return String.format("%02d:%02d", hours, minutes)
    }
}