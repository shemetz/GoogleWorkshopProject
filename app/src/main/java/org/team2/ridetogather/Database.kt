package org.team2.ridetogather

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RequestsQueue constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: RequestsQueue? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RequestsQueue(context).also {
                    INSTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}

object Database {
    private val tag = Database::class.java.simpleName
    var idOfCurrentUser: Id = MockData.user1.id // MOCK

    fun jsonToRide(jsonObject: JSONObject): Ride{
        val rideId = jsonObject.getInt("id")
        val driverId = jsonObject.getInt("driver")
        val eventId = jsonObject.getInt("event")
        val origin = Location("useless paramater")
            .apply { latitude = jsonObject.getDouble("originLat"); longitude = jsonObject.getDouble("originLong") }
        val departureTime = TimeOfDay(jsonObject.getInt("departureHour"),jsonObject.getInt("departureMinute"))
        val carModel = jsonObject.getString("carModel")
        val carColor = jsonObject.getString("carColor")
        val passengerCount = jsonObject.getInt("passengerCount")
        val extraDetails = jsonObject.getString("extraDetails")

        return Ride(rideId,driverId,eventId,origin,departureTime,carModel,carColor,passengerCount,extraDetails)
    }

    fun jsonArrayToPickups(jsonArray: JSONArray): ArrayList<Pickup>{
        val pickups = arrayListOf<Pickup>()
        for (i in 0 until jsonArray.length()) {
            // Get current json object
            val pickupJson = jsonArray.getJSONObject(i)
            val pickupId = pickupJson.getInt("id")
            val userId = pickupJson.getInt("user")
            val rideId = pickupJson.getInt("ride")
            val pickupSpot = Location("useless paramater")
                .apply { latitude = pickupJson.getDouble("pickupLat"); longitude = pickupJson.getDouble("pickupLong") }
            val pickupTime = TimeOfDay(pickupJson.getInt("pickupHour"),pickupJson.getInt("pickupMinute"))
            val inRide = pickupJson.getBoolean("inRide")
            pickups.add(Pickup(pickupId,userId,rideId,pickupSpot,pickupTime,inRide))
        }
        return pickups
    }

    fun jsonToUser(jsonObject: JSONObject):User{
        val userId = jsonObject.getInt("id")
        val name = jsonObject.getString("name")
        val facebookProfileId = jsonObject.getString("facebookProfileId")
        val credits = jsonObject.getInt("credits")
        return User(userId,name,facebookProfileId,credits)
    }

    fun jsonArrayToEvents(jsonArray: JSONArray):ArrayList<Event>{
        val events = arrayListOf<Event>()
        for (i in 0 until jsonArray.length()) {
            // Get current json object
            val eventJson = jsonArray.getJSONObject(i)
            val eventId = eventJson.getInt("id")
            val eventName = eventJson.getString("name")
            val eventLocation = Location("useless paramater")
                .apply { latitude = eventJson.getDouble("locationLat"); longitude = eventJson.getDouble("locationLong") }
            val eventDatetime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY).parse(eventJson.getString("datetime"))
            val facebookEventId = eventJson.getString("facebookEventId")
            events.add(Event(eventId,eventName,eventLocation,eventDatetime,facebookEventId))
        }
        return events
    }

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

    fun addUser(context: Context,name: String,facebookProfileId: String,credits: Int){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("name",name)
        postparams.put("facebookProfileId",facebookProfileId)
        postparams.put("credits",credits)
        val url = "https://ridetogather.herokuapp.com/addUser/"

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun addEvent(context: Context,name: String,location: Location,datetime: Datetime,facebookEventId: String){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("name",name)
        postparams.put("locationLat", location.latitude)
        postparams.put("locationLong", location.longitude)
        postparams.put("datetime", datetime.toString())
        postparams.put("facebookEventId",facebookEventId)
        val url = "https://ridetogather.herokuapp.com/addEvent/"

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }


    fun addRide(context: Context,driverId: Id,eventId: Id,origin: Location,departureTime: TimeOfDay,
                carModel: String,carColor: String,passengerCount: Int,extraDetails: String){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("driver",driverId)
        postparams.put("event",eventId)
        postparams.put("originLat",origin.latitude)
        postparams.put("originLong",origin.longitude)
        postparams.put("departureHour",departureTime.hours)
        postparams.put("departureMinute",departureTime.minutes)
        postparams.put("carModel",carModel)
        postparams.put("carColor",carColor)
        postparams.put("passengerCount",passengerCount)
        postparams.put("extraDetails",extraDetails)
        val url = "https://ridetogather.herokuapp.com/addRide/"

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun addEventToUser(context: Context,userId: Id,eventId: Id){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("user",userId)
        postparams.put("event",eventId)
        val url = "https://ridetogather.herokuapp.com/addAttending/"

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun addPickup(context: Context,rideId: Id,userId: Id,pickupSpot: Location?){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("ride",rideId)
        postparams.put("user",userId)
        postparams.put("pickupLat",pickupSpot!!.latitude)
        postparams.put("pickupLong",pickupSpot.longitude)
        //TODO: update pickup time by driver
        //postparams.put("pickupHour",pickupTime.hours)
        //postparams.put("pickupMinute",pickupTime.minutes)
        val url = "https://ridetogather.herokuapp.com/addPickup/"

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun delUser(context: Context,userId: Id){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val baseUrl = "https://ridetogather.herokuapp.com/deleteUser/"
        val url = baseUrl + userId

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                // TODO: Handle error
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun delRide(context: Context,rideId: Id){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val baseUrl = "https://ridetogather.herokuapp.com/deleteRide/"
        val url = baseUrl + rideId

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                // TODO: Handle error
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun delEvent(context: Context,eventId: Id){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val baseUrl = "https://ridetogather.herokuapp.com/deleteEvent/"
        val url = baseUrl + eventId

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                // TODO: Handle error
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun delPickup(context: Context,pickupId: Id){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val baseUrl = "https://ridetogather.herokuapp.com/deletePickup/"
        val url = baseUrl + pickupId

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                // TODO: Handle error
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun removeUserFromEvent(context: Context,pickupId: Id,eventId: Id){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val baseUrl = "https://ridetogather.herokuapp.com/removeUserFromEvent/"
        val url = baseUrl + pickupId + "/" + eventId

        val jsonObjectRequest = JsonObjectRequest(Request.Method.DELETE, url, null,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                // TODO: Handle error
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun updateUser(context: Context, user: User){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("name",user.name)
        postparams.put("facebookProfileId",user.facebookProfileId)
        postparams.put("credits",user.credits)
        val baseUrl = "https://ridetogather.herokuapp.com/updateUser/"
        val url = baseUrl + user.id.toString()

        val jsonobj = JsonObjectRequest(Request.Method.PUT, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun updateEvent(context: Context,event: Event){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("name",event.name)
        postparams.put("locationLat", event.location.latitude)
        postparams.put("locationLong", event.location.longitude)
        postparams.put("datetime", event.datetime.toString())
        postparams.put("facebookEventId",event.facebookEventId)
        val baseurl = "https://ridetogather.herokuapp.com/addEvent/"
        val url = baseurl + event.id.toString()

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun updateRide(context: Context,ride: Ride){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("driver",ride.driverId)
        postparams.put("event",ride.eventId)
        postparams.put("originLat",ride.origin.latitude)
        postparams.put("originLong",ride.origin.longitude)
        postparams.put("departureHour",ride.departureTime.hours)
        postparams.put("departureMinute",ride.departureTime.minutes)
        postparams.put("carModel",ride.carModel)
        postparams.put("carColor",ride.carColor)
        postparams.put("passengerCount",ride.passengerCount)
        postparams.put("extraDetails",ride.extraDetails)
        val baseurl = "https://ridetogather.herokuapp.com/updateRide/"
        val url = baseurl + ride.id.toString()

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
    }

    fun updatePickup(context: Context,pickup: Pickup){
        val queue = RequestsQueue.getInstance(context.applicationContext).requestQueue
        val postparams = JSONObject()
        postparams.put("ride",pickup.rideId)
        postparams.put("user",pickup.userId)
        postparams.put("pickupLat",pickup.pickupSpot.latitude)
        postparams.put("pickupLong",pickup.pickupSpot.longitude)
        postparams.put("pickupHour",pickup.pickupTime.hours)
        postparams.put("pickupMinute",pickup.pickupTime.minutes)
        val baseurl = "https://ridetogather.herokuapp.com/addPickup/"
        val url = baseurl + pickup.id.toString()

        val jsonobj = JsonObjectRequest(Request.Method.POST, url, postparams,
            Response.Listener { response ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifySuccess(response)
                }*/
            },
            Response.ErrorListener { error ->
                /*if (mResultCallback != null) {
                    mResultCallback.notifyError(error)
                }*/
            }
        )
        queue.add(jsonobj)
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
            pickupTime,
            true
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