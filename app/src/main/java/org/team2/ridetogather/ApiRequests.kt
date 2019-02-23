package org.team2.ridetogather

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.math.absoluteValue


object ApiRequests {
    fun sendFirebaseNotification(
        to: Array<String>,
        title: String?,
        message: String?,
        picUrl: String?,
        intentName: String,
        keys: HashMap<String, Any>
    ) {
        val tag = "sendFirebaseNotific"
        val url = "https://fcm.googleapis.com/fcm/send"
        val toJson = JSONArray()
        val keysJson = JSONObject(keys)
        for (i in to) {
            toJson.put(i)
        }
        val data = jsonObjOf(
            "title" to title,
            "body" to message,
            "click_action" to intentName,
            "keys" to keysJson,
            "pic_url" to picUrl
        )

        val postParams = jsonObjOf(
            "registration_ids" to toJson,
            "data" to data
        )
        val request: JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST, url, postParams,
            Response.Listener<JSONObject?> { response ->
                Log.d(tag, "Got response for $url")
                Log.v(tag, response!!.toString(4))
            }, Response.ErrorListener { error ->
                InternetRequests.logResponseError(error, url)
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =
                    "key=AAAA2UXUXYM:APA91bG2bbXcooQiIRwjntn_mdCEq_yViJtEZQkGwl-LH6NyU9_DaHXmRy9wo7uJKP5O6xvk5F7P0KBvR5x4eLhQaTQ9um4RkfxpQhqVHDJ5AmYm3YA_NmyxOBoAidXJokkHAcZoTq0g"
                return headers
            }
        }
        InternetRequests.addRequest(request)
    }

    fun requestJsonObjectFromGoogleApi(
        partialUrl: String,
        getParams: Map<String, String>,
        successCallback: (JSONObject) -> Unit = {}
    ) {
        val tag = "requestJsonFromGoogle"
        val httpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("maps.googleapis.com")
            .addPathSegments(partialUrl)
        getParams.entries.forEach {
            val (key, value) = it
            httpUrl.addQueryParameter(key, value)
        }
        val request = JsonObjectRequestWithNull(
            Request.Method.GET,
            httpUrl.build().toString(),
            null,
            Response.Listener { response ->
                Log.d(tag, "Got response for ${httpUrl.build()}")
                Log.v(tag, response.toString(4))
                successCallback(response)
            },
            Response.ErrorListener { error ->
                InternetRequests.logResponseError(error, partialUrl)
            })
        InternetRequests.addRequest(request)
    }

    /**
     * Converts a location into a human-readable string.
     * For example, the location {latitude = 32.055436; longitude = 34.753070} will
     * cause the following readable location string to be returned:
     * "Mifrats Shlomo Promenade 5, Tel Aviv-Yafo, Israel"
     *
     * Works with Google Reverse-Geocoding API.
     */
    fun geocode(context: Context, latLng: LatLng, successCallback: (String) -> Unit) {
        if (geocodingCache.containsKey(latLng)) {
            successCallback(geocodingCache[latLng]!!)
            return
        }
        requestJsonObjectFromGoogleApi(
            partialUrl = "maps/api/geocode/json",
            getParams = mapOf(
                "key" to context.getString(R.string.SECRET_GOOGLE_API_KEY),
                "latlng" to "${latLng.latitude},${latLng.longitude}"
            )
        ) { jsonObject ->
            Log.v("Google Geocode", jsonObject.toString(4))
            CoroutineScope(Dispatchers.Main).launch {
                val result = if (jsonObject.getJSONArray("results").length() > 0)
                    jsonObject.getJSONArray("results").getJSONObject(0).getString("formatted_address")
                else {
                    if (jsonObject.getString("status") != "ZERO_RESULTS")
                        Log.w(
                            "Google Geocode",
                            "Got zero results but status is ${jsonObject.getString("status")}"
                        )
                    alternativeGeocode(context, latLng)
                }
                Log.v("Google Geocode", "Caching result: $latLng → $result")
                geocodingCache[latLng] = result
                successCallback(result)
            }
        }
    }

    /**
     * Converts a location into a human-readable string.
     * For example, the location {latitude = 32.055436; longitude = 34.753070} will
     * cause the following readable location string to be returned:
     * "Mifrats Shlomo Promenade 5, Tel Aviv-Yafo, Israel"
     *
     * Works with the default android geocoder, so it's slower and worse. And free.
     */
    private fun alternativeGeocode(context: Context?, latLng: LatLng): String {
        fun coordinatesString(): String {
            val absoluteLatitude = Location.convert(latLng.latitude.absoluteValue, Location.FORMAT_DEGREES)
            val absoluteLongitude = Location.convert(latLng.longitude.absoluteValue, Location.FORMAT_DEGREES)
            val latitudeDirection = if (latLng.latitude >= 0) "N" else "S"
            val longitudeDirection = if (latLng.longitude >= 0) "N" else "S"
            return "$absoluteLatitude $latitudeDirection, $absoluteLongitude $longitudeDirection"
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            return if (addresses.isNotEmpty()) {
                val address = addresses[0]
                // combine address lines into one comma-separated line, it is usually the best address format!
                val addressString =
                    (0..address.maxAddressLineIndex).joinToString(separator = ", ") { i -> address.getAddressLine(i) }
//            val city = address.locality
//            val state = address.adminArea
//            val country = address.countryName
//            val postalCode = address.postalCode // may be null
//            val knownName = address.featureName // may be null, may be irrelevant
                if (addressString.startsWith("Unnamed")) {
                    Log.w("Shortened Location", "Address starts with 'Unnamed'; this is sad.")
                    Log.i("Shortened Location", addresses.joinToString("\n"))
                }
                addressString
            } else {
                // No address found for the location coordinates. Returning plain coordinates.
                coordinatesString()
            }
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            Log.e(
                "Alternative Geocode", "IO error encountered while reverse-geocoding. latLng = $latLng", ioException
            )
            return coordinatesString()
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(
                "Alternative Geocode",
                "IO error encountered while reverse-geocoding $latLng",
                illegalArgumentException
            )
            return coordinatesString()
        }
    }

    fun getProfilePicUrl(facebookId: String, callback: (String) -> Unit) {
        if (facebookId == "fakeprofile") {
            callback("http://pluspng.com/img-png/png-hd-of-puppies-puppy-other-400.png")
        } else {
            val request = GraphRequest.newGraphPathRequest(
                AccessToken.getCurrentAccessToken(),
                "/$facebookId"
            ) { response ->
                val picture = response.jsonObject.getJSONObject("picture")
                val picUrl = picture.getJSONObject("data").getString("url")
                callback(picUrl)
            }
            request.parameters.putString("fields", "picture.type(large)")
            request.executeAsync()
        }
    }

    fun getEventUrl(eventId: String, callback: (String) -> Unit) {
        if (eventId == "fakefacebookevent") {
            callback("https://www.cesarsway.com/sites/newcesarsway/files/d6/images/features/2012/sept/How-to-Care-for-Newborn-Puppies.jpg")
        } else {
            val request = GraphRequest.newGraphPathRequest(
                AccessToken.getCurrentAccessToken(),
                "/$eventId"
            ) { response ->
                val picture = response.jsonObject.getJSONObject("cover")
                val picUrl = picture.getString("source")
                callback(picUrl)
            }
            request.parameters.putString("fields", "cover")
            request.executeAsync()
        }
    }
}