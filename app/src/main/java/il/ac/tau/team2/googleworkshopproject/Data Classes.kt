package il.ac.tau.team2.googleworkshopproject

typealias Driver = User
typealias Location = android.location.Location
typealias Datetime = java.util.Date

open class DatabaseObject(val id: Int) {
    override fun equals(other: Any?) =
        (other != null && other is DatabaseObject && other::class.java == this::class.java) && id == other.id

    override fun hashCode() = id.hashCode()
}

class Ride(
    id_: Int,
    var driver: Driver,
    val event: Event,
    var origin: Location,
    var destination: Location,
    var departureTime: TimeOfDay,
    var carModel: String,
    var carColor: String,
    var extraDetails: String = ""
) : DatabaseObject(id_)

class User(
    id_: Int,
    var name: String,
    var facebookProfileId: String
) : DatabaseObject(id_)

class Event(
    id_: Int,
    var name: String,
    var location: Location,
    var datetime: Datetime,
    var facebookEventId: String
) : DatabaseObject(id_)

class Pickup(
    id_: Int,
    val user: User,
    val ride: Ride,
    var pickupSpot: Location,
    var pickupTime: TimeOfDay
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

fun Location.shortenedLocation(): String {
    return "$longitude, $latitude"
}