@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.team2.ridetogather

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
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
        val pickupTime = TimeOfDay(pickupJson.getInt("pickupHour"), pickupJson.getInt("pickupMinute"))
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
    var idOfCurrentUser: Id = MockData.user1.id // MOCK

    fun initializeRequestQueue(context: Context) {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        requestQueue = Volley.newRequestQueue(context.applicationContext)
    }

    fun getUser(userId: Id, successCallback: (User) -> Unit) {
        val url = "$SERVER_URL/getUser/$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                successCallback(JsonParse.user(response))
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun getEvent(eventId: Id): Event? {
        // MOCK
        return MockData.events[eventId] ?: run {
            Log.e(tag, "No such event with ID = $eventId")
            null
        }
    }

    fun getRide(rideId: Id, successCallback: (Ride) -> Unit) {
        val url = "$SERVER_URL/getRide/$rideId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                successCallback(JsonParse.ride(response))
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun getPickup(pickupId: Id): Pickup? {
        // MOCK
        return MockData.pickups[pickupId] ?: run {
            Log.e(tag, "No such pickup with ID = $pickupId")
            null
        }
    }

    fun getDriver(driverId: Id, successCallback: (Driver) -> Unit) {
        // Currently drivers are identical to users!
        return getUser(driverId, successCallback)
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
     * Will return an empty list if there are no pickups for the ride
     */
    fun getPickupsForRide(rideId: Id, successCallback: (List<Pickup>) -> Unit) {
        val url = "$SERVER_URL/getPickupsForRide/$rideId"

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                successCallback(JsonParse.array(response, JsonParse::pickup))
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    /**
     * Will return an empty list if there are no rides for the event, or
     * if there is no such event.
     */
    fun getRidesForEvent(eventId: Id, successCallback: (List<Ride>) -> Unit) {
        val url = "$SERVER_URL/getRidesForEvent/$eventId"

        val request = JsonArrayRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                successCallback(JsonParse.array(response, JsonParse::ride))
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun addUser(name: String, facebookProfileId: String, credits: Int) {
        val postParams = jsonObjOf(
            "name" to name,
            "facebookProfileId" to facebookProfileId,
            "credits" to credits
        )
        val url = "$SERVER_URL/addUser/"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun addEvent(name: String, location: Location, datetime: Datetime, facebookEventId: String) {
        val postParams = jsonObjOf(
            "name" to name,
            "locationLat" to location.latitude,
            "locationLong" to location.longitude,
            "datetime" to datetime.toString(),
            "facebookEventId" to facebookEventId
        )
        val url = "$SERVER_URL/addEvent/"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
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
        val url = "$SERVER_URL/addRide/"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                Log.v(tag, "Response success for $url")
                successCallback(JsonParse.ride(response))
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun addEventToUser(userId: Id, eventId: Id) {
        val postParams = jsonObjOf(
            "user" to userId,
            "event" to eventId
        )
        val url = "$SERVER_URL/addAttending/"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun addPickup(rideId: Id, userId: Id, pickupSpot: Location, pickupTime: TimeOfDay) {
        val postParams = jsonObjOf(
            "ride" to rideId,
            "user" to userId,
            "pickupLat" to pickupSpot.latitude,
            "pickupLong" to pickupSpot.longitude,
            "pickupHour" to pickupTime.hours,
            "pickupMinute" to pickupTime.minutes
        )
        val url = "$SERVER_URL/addPickup/"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun deleteUser(userId: Id) {
        val url = "$SERVER_URL/deleteUser/$userId"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    fun deleteRide(rideId: Id) {
        val url = "$SERVER_URL/deleteRide/$rideId"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    fun deleteEvent(eventId: Id) {
        val url = "$SERVER_URL/deleteEvent/$eventId"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    fun deletePickup(pickupId: Id) {
        val url = "$SERVER_URL/deletePickup/$pickupId"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    fun removeUserFromEvent(pickupId: Id, eventId: Id) {
        val url = "$SERVER_URL/removeUserFromEvent/$pickupId/$eventId"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    fun updateUser(user: User) {
        val postParams = jsonObjOf(
            "name" to user.name,
            "facebookProfileId" to user.facebookProfileId,
            "credits" to user.credits
        )
        val url = "$SERVER_URL/updateUser/${user.id}"

        val request = JsonObjectRequest(Request.Method.PUT, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun updateEvent(event: Event) {
        val postParams = jsonObjOf(
            "name" to event.name,
            "locationLat" to event.location.latitude,
            "locationLong" to event.location.longitude,
            "datetime" to event.datetime.toString(),
            "facebookEventId" to event.facebookEventId
        )
        val url = "$SERVER_URL/addEvent/${event.id}"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
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
        val url = "$SERVER_URL/updateRide/${ride.id}"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    fun updatePickup(pickup: Pickup) {
        val postParams = jsonObjOf(
            "ride" to pickup.rideId,
            "user" to pickup.userId,
            "pickupLat" to pickup.pickupSpot.latitude,
            "pickupLong" to pickup.pickupSpot.longitude,
            "pickupHour" to pickup.pickupTime.hours,
            "pickupMinute" to pickup.pickupTime.minutes
        )
        val url = "$SERVER_URL/addPickup/${pickup.id}"

        val request = JsonObjectRequest(Request.Method.POST, url, postParams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                Log.e(tag, "Response error for $url", error)
            }
        )
        requestQueue.add(request)
    }

    lateinit var thisUser: User

    fun getThisUserId(): Id {
        return idOfCurrentUser
    }
}