@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.team2.ridetogather

import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection


object Database {
    private val tag = Database::class.java.simpleName
    private const val SERVER_URL = "https://ridetogather.herokuapp.com"
    var idOfCurrentUser: Id = -1

    private fun requestJsonObject(
        method: Int,
        url: String,
        postParameters: JSONObject? = null,
        successCallback: (JSONObject) -> Unit = {},
        errorListener: Response.ErrorListener? = null
    ) {
        val cacheOfGETs = InternetRequests.cacheOfGETs
        val now = System.currentTimeMillis()
        if (cacheOfGETs.containsKey(url) && method == Request.Method.GET) {
            if (now - cacheOfGETs[url]!!.birthTime.time < InternetRequests.CACHE_TIME_MS) {
                Log.d(tag, "Cached response for $url")
                successCallback(cacheOfGETs[url]!!.response as JSONObject)
                return
            } else {
                Log.v(tag, "Cache is too old for $url")
                cacheOfGETs.remove(url)
            }
        }
        if (method != Request.Method.GET) {
            // Clear entire cache when we send a PUT/POST/UPDATE/DELETE request
            // It's a drastic measure but it's safe and it saves on coding time
            // (A better alternative would be to clear only the specific urls of relevant GETs)
            cacheOfGETs.entries.clear()
        }

        val fullUrl = "$SERVER_URL$url"
        val request = JsonObjectRequestWithNull(method, fullUrl, postParameters,
            Response.Listener { response ->
                Log.d(tag, "Got response for $url")
                Log.v(tag, response.toString(4))
                cacheOfGETs[url] = InternetRequests.CacheEntry(response, Datetime(now))
                successCallback(response)
            },
            errorListener ?: Response.ErrorListener { error ->
                InternetRequests.logResponseError(error, url)
            })
        InternetRequests.addRequest(request)
    }

    private fun requestJsonArray(
        method: Int,
        url: String,
        // No post parameters - this is only used for GET right now, because
        // apparently JsonArrayRequests wants post params to also be an array :/
        successCallback: (JSONArray) -> Unit = {},
        errorListener: Response.ErrorListener? = null
    ) {
        val cacheOfGETs = InternetRequests.cacheOfGETs
        val now = System.currentTimeMillis()
        if (cacheOfGETs.containsKey(url) && method == Request.Method.GET) {
            if (now - cacheOfGETs[url]!!.birthTime.time < InternetRequests.CACHE_TIME_MS) {
                Log.d(tag, "Cached response for $url")
                successCallback(cacheOfGETs[url]!!.response as JSONArray)
                return
            } else {
                Log.v(tag, "Cache is too old for $url")
                cacheOfGETs.remove(url)
            }
        }

        val fullUrl = "$SERVER_URL$url"
        val request = JsonArrayRequest(method, fullUrl, null,
            Response.Listener { response ->
                Log.d(tag, "Got response for $url")
                Log.v(tag, response.toString(4))
                cacheOfGETs[url] = InternetRequests.CacheEntry(response, Datetime(now))
                successCallback(response)
            },
            errorListener ?: Response.ErrorListener { error ->
                InternetRequests.logResponseError(error, url)
            })
        InternetRequests.addRequest(request)
    }

    private fun <T> generifiedGet(
        someId: Id,
        name: String,
        parseFunction: (JSONObject) -> T,
        successCallback: (T) -> Unit
    ) {
        val url = "/get$name/$someId"

        requestJsonObject(Request.Method.GET, url, null,
            { response ->
                successCallback(parseFunction(response))
            }
        )
    }

    fun getUser(userId: Id, successCallback: (User) -> Unit) =
        generifiedGet(userId, "User", JsonParse::user, successCallback)

    fun getEvent(eventId: Id, successCallback: (Event) -> Unit) =
        generifiedGet(eventId, "Event", JsonParse::event, successCallback)

//    fun getEventByFacebook(facebookEventId: String, successCallback: (Event) -> Unit) =
//        generifiedGet(facebookEventId.toInt(), "EventByFacebook", JsonParse::event, successCallback)

    fun getRide(rideId: Id, successCallback: (Ride) -> Unit) =
        generifiedGet(rideId, "Ride", JsonParse::ride, successCallback)

    fun getPickup(pickupId: Id, successCallback: (Pickup) -> Unit) =
        generifiedGet(pickupId, "Pickup", JsonParse::pickup, successCallback)

    fun getDriver(driverId: Id, successCallback: (User) -> Unit) {
        // Currently drivers are identical to users!
        return getUser(driverId, successCallback)
    }

    private fun <T> generifiedGet1sFor2(
        id2: Id,
        name1s: String,
        name2: String,
        parseFunction: (JSONObject) -> T,
        successCallback: (List<T>) -> Unit
    ) {
        val url = "/get${name1s}For$name2/$id2"

        requestJsonArray(Request.Method.GET, url,
            { response ->
                successCallback(JsonParse.array(response, parseFunction))
            }
        )
    }

    fun getEventsForUser(userId: Id, successCallback: (List<Event>) -> Unit) =
        generifiedGet1sFor2(userId, "Events", "User", JsonParse::event, successCallback)

    fun getPickupsForRide(rideId: Id, successCallback: (List<Pickup>) -> Unit) =
        generifiedGet1sFor2(rideId, "Pickups", "Ride", JsonParse::pickup, successCallback)

    fun getRidesForEvent(eventID: Id, successCallback: (List<Ride>) -> Unit) =
        generifiedGet1sFor2(eventID, "Rides", "Event", JsonParse::ride, successCallback)

    fun getRidesForUser(userID: Id, successCallback: (List<Ride>) -> Unit) =
        generifiedGet1sFor2(userID, "Rides", "User", JsonParse::ride, successCallback)

    fun getUsersForEvent(eventID: Id, successCallback: (List<User>) -> Unit) =
        generifiedGet1sFor2(eventID, "Users", "Event", JsonParse::user, successCallback)

    fun getEventByFacebook(facebookEventId: String, successCallback: (Event) -> Unit, failedCallback: () -> Unit) {
        val url = "$SERVER_URL/getEventByFacebook/$facebookEventId"
        val request = JsonObjectRequestWithNull(Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d(tag, "Got response for $url")
                Log.v(tag, response.toString(4))
                successCallback(JsonParse.event(response))
            },
            Response.ErrorListener { error ->
                if (error.networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    failedCallback()
                }
            })
        InternetRequests.addRequest(request)

    }

    fun getAttendingByIds(userId: Id, eventId: Id, successCallback: (Attending) -> Unit, failedCallback: () -> Unit) {
        val url = "$SERVER_URL/getAttendingByIds/$userId/$eventId"
        val request = JsonObjectRequestWithNull(Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d(tag, "Got response for $url")
                Log.v(tag, response.toString(4))
                successCallback(JsonParse.attending(response))
            },
            Response.ErrorListener { error ->
                if (error.networkResponse.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    failedCallback()
                }
            })
        InternetRequests.addRequest(request)

    }

    fun addUser(name: String, facebookProfileId: String, credits: Int, firebaseId: String) {
        val postParams = jsonObjOf(
            "name" to name,
            "facebookProfileId" to facebookProfileId,
            "credits" to credits,
            "firebaseId" to firebaseId
        )
        val url = "/addUser/"

        requestJsonObject(Request.Method.POST, url, postParams)
    }

    fun getOrAddUserByFacebook(name: String, facebookProfileId: String, successCallback: (User) -> Unit) {
        val credits = 0  // users start with 0 credits
        val postParams = jsonObjOf(
            "name" to name,
            "facebookProfileId" to facebookProfileId,
            "credits" to credits
        )
        val url = "/getOrAddUserByFacebook/$facebookProfileId"

        requestJsonObject(Request.Method.POST, url, postParams,
            { response ->
                successCallback(JsonParse.user(response))
            }
        )
    }

    fun addEvent(name: String, location: Location, datetime: Datetime, facebookEventId: String) {
        val postParams = jsonObjOf(
            "name" to name,
            "locationLat" to location.latitude,
            "locationLong" to location.longitude,
            "datetime" to datetime,
            "facebookEventId" to facebookEventId
        )
        val url = "/addEvent/"

        requestJsonObject(Request.Method.POST, url, postParams)
    }

    fun addEventWithCallback(
        name: String,
        location: Location,
        datetime: String,
        facebookEventId: String,
        successCallback: (Event) -> Unit
    ) {
        val postParams = jsonObjOf(
            "name" to name,
            "locationLat" to location.latitude,
            "locationLong" to location.longitude,
            "datetime" to datetime,
            "facebookEventId" to facebookEventId
        )
        val url = "/addEvent/"

        requestJsonObject(Request.Method.POST, url, postParams,
            { response ->
                successCallback(JsonParse.event(response))
            }
        )
    }


    fun addRide(
        driverId: Id, eventId: Id, origin: Location, departureTime: TimeOfDay,
        carModel: String, carColor: String, passengerCount: Int, extraDetails: String,
        successCallback: (Ride) -> Unit
    ) {
        val postParams = jsonObjOf(
            "driver" to driverId,
            "event" to eventId,
            "originLat" to origin.latitude,
            "originLong" to origin.longitude,
            "departureHour" to departureTime.hours,
            "departureMinute" to departureTime.minutes,
            "carModel" to carModel,
            "carColor" to carColor,
            "passengerCount" to passengerCount,
            "extraDetails" to extraDetails
        )
        val url = "/addRide/"

        requestJsonObject(Request.Method.POST, url, postParams,
            { response ->
                successCallback(JsonParse.ride(response))
            }
        )
    }


    fun addEventToUser(userId: Id, eventId: Id) {
        val postParams = jsonObjOf(
            "user" to userId,
            "event" to eventId
        )
        val url = "/addAttending/"

        requestJsonObject(Request.Method.POST, url, postParams)
    }

    fun addPickup(rideId: Id, userId: Id, pickupSpot: Location, successCallback: () -> Unit = {}) {
        val postParams = jsonObjOf(
            "ride" to rideId,
            "user" to userId,
            "pickupLat" to pickupSpot.latitude,
            "pickupLong" to pickupSpot.longitude
            // others will be default (time = 00:00, inRide=false…)
        )
        val url = "/addPickup/"

        requestJsonObject(Request.Method.POST, url, postParams, { successCallback() })
    }

    fun deleteUser(userId: Id) {
        val url = "/deleteUser/$userId"

        requestJsonObject(Request.Method.DELETE, url, null)
    }

    fun deleteRide(rideId: Id) {
        val url = "/deleteRide/$rideId"

        requestJsonObject(Request.Method.DELETE, url, null)
    }

    fun deleteEvent(eventId: Id) {
        val url = "/deleteEvent/$eventId"

        requestJsonObject(Request.Method.DELETE, url, null)
    }

    fun deletePickup(pickupId: Id, successCallback: () -> Unit = {}) {
        val url = "/deletePickup/$pickupId"

        requestJsonObject(Request.Method.DELETE, url, null, { successCallback() })
    }

    fun removeUserFromEvent(userId: Id, eventId: Id) {
        val url = "/removeUserFromEvent/$userId/$eventId"

        requestJsonObject(Request.Method.DELETE, url, null)
    }

    fun updateUser(user: User) {
        val postParams = jsonObjOf(
            "name" to user.name,
            "facebookProfileId" to user.facebookProfileId,
            "credits" to user.credits,
            "firebaseId" to user.firebaseId
        )
        val url = "/updateUser/${user.id}"

        requestJsonObject(Request.Method.PUT, url, postParams)
    }

    fun updateEvent(event: Event) {
        val postParams = jsonObjOf(
            "name" to event.name,
            "locationLat" to event.location.latitude,
            "locationLong" to event.location.longitude,
            "datetime" to event.datetime.toString(),
            "facebookEventId" to event.facebookEventId
        )
        val url = "/addEvent/${event.id}"

        requestJsonObject(Request.Method.PUT, url, postParams)
    }

    fun updateRide(ride: Ride) {
        val postParams = jsonObjOf(
            "driver" to ride.driverId,
            "event" to ride.eventId,
            "originLat" to ride.origin.latitude,
            "originLong" to ride.origin.longitude,
            "departureHour" to ride.departureTime.hours,
            "departureMinute" to ride.departureTime.minutes,
            "carModel" to ride.carModel,
            "carColor" to ride.carColor,
            "passengerCount" to ride.passengerCount,
            "extraDetails" to ride.extraDetails
        )
        val url = "/updateRide/${ride.id}"

        requestJsonObject(Request.Method.PUT, url, postParams)
    }

    fun updatePickup(pickup: Pickup, onCallback: () -> Unit) {
        val postParams = jsonObjOf(
            "ride" to pickup.rideId,
            "user" to pickup.userId,
            "pickupLat" to pickup.pickupSpot.latitude,
            "pickupLong" to pickup.pickupSpot.longitude,
            "pickupHour" to pickup.pickupTime.hours,
            "pickupMinute" to pickup.pickupTime.minutes,
            "inRide" to pickup.inRide,
            "denied" to pickup.denied
        )
        val url = "/updatePickup/${pickup.id}"

        requestJsonObject(
            Request.Method.PUT,
            url,
            postParams,
            { onCallback() },
            Response.ErrorListener { error ->
                InternetRequests.logResponseError(error, url)
                onCallback()
            })
    }
}