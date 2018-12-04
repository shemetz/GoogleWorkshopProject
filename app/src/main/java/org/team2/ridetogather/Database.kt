package org.team2.ridetogather

import android.util.Log

object Database {
    private val tag = Database::class.java.simpleName
    var idOfCurrentUser: Id = MockData.user1.id // MOCK

    fun getUser(userId: Id): User? {
        // MOCK
        return MockData.users[userId] ?: run {
            Log.e(tag, "No such user with ID = $userId")
            null
        }
    }

    fun getEvent(eventId: Id): Event? {
        // MOCK
        return MockData.events[eventId] ?: run {
            Log.e(tag, "No such event with ID = $eventId")
            null
        }
    }

    fun getRide(rideId: Id): Ride? {
        // MOCK
        return MockData.rides[rideId] ?: run {
            Log.e(tag, "No such ride with ID = $rideId")
            null
        }
    }

    fun getPickup(pickupId: Id): Pickup? {
        // MOCK
        return MockData.pickups[pickupId] ?: run {
            Log.e(tag, "No such pickup with ID = $pickupId")
            null
        }
    }

    fun getDriver(driverId: Id): Driver? {
        // MOCK
        return getUser(driverId)
    }

    /**
     * Will return an empty list if there are no events for user, or
     * if there is no such user.
     */
    fun getEventsForUser(userId: Id): List<Event> {
        // MOCK
        val idsList = MockData.eventsOfUser[userId] ?: mutableListOf()
        return idsList.map { getEvent(it)!! }
    }

    /**
     * Will return an empty list if there are no pickups for the ride, or
     * if there is no such ride.
     */
    fun getPickupsForRide(rideId: Id): List<Pickup> {
        // MOCK
        val idsList = MockData.pickupsOfRide[rideId] ?: mutableListOf()
        return idsList.map { getPickup(it)!! }
    }

    /**
     * Will return an empty list if there are no rides for the event, or
     * if there is no such event.
     */
    fun getRidesForEvent(eventId: Id): List<Ride> {
        // MOCK
        val idsList = MockData.ridesOfEvent[eventId] ?: mutableListOf()
        return idsList.map { getRide(it)!! }
    }

    fun createNewRide(
        driverId: Id,
        eventId: Id,
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
            driverId = driverId,
            eventId = eventId,
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
        userId: Id,
        rideId: Id,
        pickupSpot: Location,
        pickupTime: TimeOfDay
    ): Pickup {
        val newPickupId = MockData.pickups.size + 1
        val newPickup = Pickup(
            newPickupId,
            userId,
            rideId,
            pickupSpot,
            pickupTime
        )
        //MOCK
        MockData.pickups[newPickupId] = newPickup
        MockData.pickupsOfRide.getOrPut(rideId) { mutableListOf() }.add(newPickupId)
        return newPickup
    }

    fun deleteRide(rideId: Id) {
        // MOCK
        // Remove ride from main table
        MockData.rides.remove(rideId)
        // Remove ride from related tables
        for (rides in MockData.ridesOfEvent.values) {
            rides.removeAll { it == rideId }
        }
        // Remove pickups of this ride
        MockData.pickupsOfRide.remove(rideId)

        Log.i(tag, "Deleted ride $rideId")
    }

    fun getThisUserId(): Id {
        return idOfCurrentUser
    }

    fun getThisUser(): User {
        return getUser(getThisUserId())!!
    }
}