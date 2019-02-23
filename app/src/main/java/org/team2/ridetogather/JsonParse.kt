package org.team2.ridetogather

import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

object JsonParse {
    fun ride(rideJson: JSONObject): Ride {
        val rideId = rideJson.getInt("id")
        val driverId = rideJson.getInt("driver")
        val eventId = rideJson.getInt("event")
        val origin = LatLng(
            rideJson.getDouble("originLat"),
            rideJson.getDouble("originLong")
        ).toLocation()
        val departureTime =
            TimeOfDay(rideJson.getInt("departureHour"), rideJson.getInt("departureMinute"))
        val carModel = rideJson.getString("carModel")
        val carColor = rideJson.getString("carColor")
        val passengerCount = rideJson.getInt("passengerCount")
        val extraDetails = rideJson.getString("extraDetails")

        return Ride(
            rideId,
            driverId,
            eventId,
            origin,
            departureTime,
            carModel,
            carColor,
            passengerCount,
            extraDetails
        )
    }

    fun pickup(pickupJson: JSONObject): Pickup {
        val pickupId = pickupJson.getInt("id")
        val userId = pickupJson.getInt("user")
        val rideId = pickupJson.getInt("ride")
        val pickupSpot = LatLng(
            pickupJson.getDouble("pickupLat"),
            pickupJson.getDouble("pickupLong")
        ).toLocation()
        val pickupTime =
            TimeOfDay(pickupJson.optInt("pickupHour"), pickupJson.optInt("pickupMinute"))
        val inRide = pickupJson.getBoolean("inRide")
        val denied = pickupJson.getBoolean("denied")
        return Pickup(pickupId, userId, rideId, pickupSpot, pickupTime, inRide, denied)
    }

    fun user(userJson: JSONObject): User {
        val userId = userJson.getInt("id")
        val name = userJson.getString("name")
        val facebookProfileId = userJson.getString("facebookProfileId")
        val credits = userJson.getInt("credits")
        val firebaseId = userJson.getString("firebaseId")
        return User(userId, name, facebookProfileId, credits, firebaseId)
    }

    fun event(eventJson: JSONObject): Event {
        val eventId = eventJson.getInt("id")
        val eventName = eventJson.getString("name")
        val eventLocation = LatLng(
            eventJson.getDouble("locationLat"),
            eventJson.getDouble("locationLong")
        ).toLocation()
        val eventDatetime = parseStandardDatetime(eventJson.getString("datetime"))
        val facebookEventId = eventJson.getString("facebookEventId")
        return Event(eventId, eventName, eventLocation, eventDatetime, facebookEventId)
    }

    fun attending(eventJson: JSONObject): Attending {
        val attendingId = eventJson.getInt("id")
        val userId = eventJson.getInt("user")
        val eventId = eventJson.getInt("event")
        val isDriver = eventJson.getBoolean("isDriver")
        return Attending(attendingId, userId, eventId, isDriver)
    }

    fun <T> array(jsonArray: JSONArray, specificFunction: (JSONObject) -> T): List<T> {
        val things = mutableListOf<T>()
        for (i in 0 until jsonArray.length()) {
            things.add(specificFunction(jsonArray.getJSONObject(i)))
        }
        return things
    }
}