@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.team2.ridetogather

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


object JsonParse {

    fun ride(rideJson: JSONObject): Ride {
        val rideId = rideJson.getInt("id")
        val driverId = rideJson.getInt("driver")
        val eventId = rideJson.getInt("event")
        val origin = LatLng(
            rideJson.getDouble("originLat"),
            rideJson.getDouble("originLong")
        ).toLocation()
        val departureTime = TimeOfDay(rideJson.getInt("departureHour"), rideJson.getInt("departureMinute"))
        val carModel = rideJson.getString("carModel")
        val carColor = rideJson.getString("carColor")
        val passengerCount = rideJson.getInt("passengerCount")
        val extraDetails = rideJson.getString("extraDetails")

        return Ride(rideId, driverId, eventId, origin, departureTime, carModel, carColor, passengerCount, extraDetails)
    }

    fun pickup(pickupJson: JSONObject): Pickup {
        val pickupId = pickupJson.getInt("id")
        val userId = pickupJson.getInt("user")
        val rideId = pickupJson.getInt("ride")
        val pickupSpot = LatLng(
            pickupJson.getDouble("pickupLat"),
            pickupJson.getDouble("pickupLong")
        ).toLocation()
        val pickupTime = TimeOfDay(pickupJson.optInt("pickupHour"), pickupJson.optInt("pickupMinute"))
        val inRide = pickupJson.getBoolean("inRide")
        return Pickup(pickupId, userId, rideId, pickupSpot, pickupTime, inRide)
    }

    fun user(userJson: JSONObject): User {
        val userId = userJson.getInt("id")
        val name = userJson.getString("name")
        val facebookProfileId = userJson.getString("facebookProfileId")
        val credits = userJson.getInt("credits")
        return User(userId, name, facebookProfileId, credits)
    }

    fun event(eventJson: JSONObject): Event {
        val eventId = eventJson.getInt("id")
        val eventName = eventJson.getString("name")
        val eventLocation = LatLng(
            eventJson.getDouble("locationLat"),
            eventJson.getDouble("locationLong")
        ).toLocation()
        val eventDatetime =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY)
                .parse(eventJson.getString("datetime"))
        val facebookEventId = eventJson.getString("facebookEventId")
        return Event(eventId, eventName, eventLocation, eventDatetime, facebookEventId)
    }

    fun <T> array(jsonArray: JSONArray, specificFunction: (JSONObject) -> T): List<T> {
        val things = mutableListOf<T>()
        for (i in 0 until jsonArray.length()) {
            things.add(specificFunction(jsonArray.getJSONObject(i)))
        }
        return things
    }
}

object Database {
    private lateinit var requestQueue: RequestQueue
    private val tag = Database::class.java.simpleName
    private const val SERVER_URL = "https://ridetogather.herokuapp.com"
    private const val CACHE_TIME_MS = 1000 * 5  // 5 seconds
    var idOfCurrentUser: Id = -1

    data class CacheEntry(
        val response: Any,
        val birthTime: Datetime
    )

    val cacheOfGETs = mutableMapOf<String, CacheEntry>()

    fun initialize(activityContext: Context) {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        requestQueue = Volley.newRequestQueue(activityContext.applicationContext)
        val prefManager = PrefManager(activityContext)
        idOfCurrentUser = prefManager.thisUserId
    }

    private fun logResponseError(error: VolleyError, url: String) {
        if (error.networkResponse == null) {
            Log.e(tag, "Response error: ${error.message} (for $url)")
            return
        }
        val errorData = String(error.networkResponse.data, Charset.forName("utf-8"))
        try {
            val errors = JSONObject(errorData).getJSONArray("errors")
            val errorMessage = errors.getJSONObject(0).getString("message")
            Log.e(tag, "Response error: $errorMessage (for $url)")
        } catch (e: JSONException) {
            Log.e(tag, "Response error: $errorData (for $url)")
        }
    }

    private fun requestJsonObject(
        method: Int,
        url: String,
        postParameters: JSONObject? = null,
        successCallback: (JSONObject) -> Unit = {},
        errorListener: Response.ErrorListener? = null
    ) {
        val now = System.currentTimeMillis()
        if (cacheOfGETs.containsKey(url) && method == Request.Method.GET) {
            if (now - cacheOfGETs[url]!!.birthTime.time < Database.CACHE_TIME_MS) {
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
                cacheOfGETs[url] = CacheEntry(response, Datetime(now))
                successCallback(response)
            },
            errorListener ?: Response.ErrorListener { error ->
                logResponseError(error, url)
            })
        requestQueue.add(request)
    }

    private fun requestJsonArray(
        method: Int,
        url: String,
        // No post parameters - this is only used for GET right now, because
        // apparently JsonArrayRequests wants post params to also be an array :/
        successCallback: (JSONArray) -> Unit = {},
        errorListener: Response.ErrorListener? = null
    ) {
        val now = System.currentTimeMillis()
        if (cacheOfGETs.containsKey(url) && method == Request.Method.GET) {
            if (now - cacheOfGETs[url]!!.birthTime.time < Database.CACHE_TIME_MS) {
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
                cacheOfGETs[url] = CacheEntry(response, Datetime(now))
                successCallback(response)
            },
            errorListener ?: Response.ErrorListener { error ->
                logResponseError(error, url)
            })
        requestQueue.add(request)
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

    fun getRide(rideId: Id, successCallback: (Ride) -> Unit) =
        generifiedGet(rideId, "Ride", JsonParse::ride, successCallback)

    fun getPickup(pickupId: Id, successCallback: (Pickup) -> Unit) =
        generifiedGet(pickupId, "Pickup", JsonParse::pickup, successCallback)

    fun getDriver(driverId: Id, successCallback: (Driver) -> Unit) {
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

    fun addUser(name: String, facebookProfileId: String, credits: Int) {
        val postParams = jsonObjOf(
            "name" to name,
            "facebookProfileId" to facebookProfileId,
            "credits" to credits
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
            "datetime" to datetime.toString(),
            "facebookEventId" to facebookEventId
        )
        val url = "/addEvent/"

        requestJsonObject(Request.Method.POST, url, postParams)
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
            //TODO: update pickup time by driver
//            "pickupHour" to pickupTime.hours,
//            "pickupMinute" to pickupTime.minutes
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
            "credits" to user.credits
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

        requestJsonObject(Request.Method.POST, url, postParams)
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

        requestJsonObject(Request.Method.POST, url, postParams)
    }

    fun updatePickup(pickup: Pickup, onCallback: () -> Unit) {
        val postParams = jsonObjOf(
            "ride" to pickup.rideId,
            "user" to pickup.userId,
            "pickupLat" to pickup.pickupSpot.latitude,
            "pickupLong" to pickup.pickupSpot.longitude,
            "pickupHour" to pickup.pickupTime.hours,
            "pickupMinute" to pickup.pickupTime.minutes
        )
        val url = "/addPickup/${pickup.id}"

        requestJsonObject(
            Request.Method.POST,
            url,
            postParams,
            { onCallback() },
            Response.ErrorListener { error ->
                logResponseError(error, url)
                onCallback()
            })
    }

    /**
     * See: https://stackoverflow.com/a/29407122/1703463
     * Solution from: https://stackoverflow.com/a/24566878/1703463
     */
    class JsonObjectRequestWithNull(
        method: Int, url: String, jsonRequest: JSONObject?,
        listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener
    ) : JsonObjectRequest(
        method,
        url,
        jsonRequest,
        listener,
        errorListener
    ) {
        override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
            val responseMaybeNull =
                try {
                    if (response.data.isEmpty()) {
                        val responseData = "{}".toByteArray(charset("UTF8"))
                        NetworkResponse(
                            response.statusCode,
                            responseData,
                            response.notModified,
                            response.networkTimeMs,
                            response.allHeaders
                        )
                    } else response
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                    response
                }

            return super.parseNetworkResponse(responseMaybeNull)
        }
    }
}