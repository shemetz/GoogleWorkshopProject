package il.ac.tau.team2.googleworkshopproject

import android.util.Log

object Database {
    private val tag = Database::class.java.simpleName
    fun getUser(userId: Int): User? {
        // MOCK
        return MockData.users[userId] ?: run {
            Log.e(tag, "No such user with ID = $userId")
            null
        }
    }

    fun getEvent(eventId: Int): Event? {
        // MOCK
        return MockData.events[eventId] ?: run {
            Log.e(tag, "No such event with ID = $eventId")
            null
        }
    }

    fun getRide(rideId: Int): Ride? {
        // MOCK
        return MockData.rides[rideId] ?: run {
            Log.e(tag, "No such ride with ID = $rideId")
            null
        }
    }

    fun getPickup(pickupId: Int): Pickup? {
        // MOCK
        return MockData.pickups[pickupId] ?: run {
            Log.e(tag, "No such pickup with ID = $pickupId")
            null
        }
    }

    /**
     * Will return an empty list if there are no events for user, or
     * if there is no such user.
     */
    fun getEventsForUser(userId: Int): List<Event> {
        // MOCK
        val idsList = MockData.eventsOfUser[userId] ?: mutableListOf()
        return idsList.map { getEvent(it)!! }
    }

    /**
     * Will return an empty list if there are no pickups for the ride, or
     * if there is no such ride.
     */
    fun getPickupsForRide(rideId: Int): List<Pickup> {
        // MOCK
        val idsList = MockData.pickupsOfRide[rideId] ?: mutableListOf()
        return idsList.map { getPickup(it)!! }
    }

    /**
     * Will return an empty list if there are no rides for the event, or
     * if there is no such event.
     */
    fun getRidesForEvent(eventId: Int): List<Ride> {
        // MOCK
        val idsList = MockData.ridesOfEvent[eventId] ?: mutableListOf()
        return idsList.map { getRide(it)!! }
    }

    fun createNewRide(
        driver: Driver,
        event: Event,
        origin: Location,
        destination: Location,
        departureTime: TimeOfDay,
        carModel: String,
        carColor: String,
        passengerCount: Int,
        extraDetails: String = ""
    ): Ride {
        //MOCK
        val newRideId = MockData.rides.size + 1
        val newRide = Ride(
            id_ = newRideId,
            driver = driver,
            event = event,
            origin = origin,
            destination = destination,
            departureTime = departureTime,
            carModel = carModel,
            carColor = carColor,
            passengerCount = passengerCount,
            extraDetails = extraDetails
        )
        MockData.rides[newRideId] = newRide
        return newRide
    }

    fun addUserPickup(
        user: User,
        ride: Ride,
        pickupSpot: Location,
        pickupTime: TimeOfDay
    ): Pickup {
        val newPickupId = MockData.pickups.size + 1
        val newPickup = Pickup(
            newPickupId,
            user,
            ride,
            pickupSpot,
            pickupTime
        )
        //MOCK
        MockData.pickups[newPickupId] = newPickup
        MockData.pickupsOfRide.getOrPut(ride.id) { mutableListOf() }.add(newPickupId)
        return newPickup
    }

    fun getThisUser(): User {
        //MOCK
        return MockData.user5
    }
}