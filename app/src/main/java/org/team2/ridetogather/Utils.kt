package org.team2.ridetogather

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

enum class Keys {
    RIDE_ID,
    EVENT_ID,
    LOCATION,
    ROUTE_JSON,
    CHANGE_BTN,
    REQUEST_CODE,
    DRIVER_PERSPECTIVE,
    SOMETHING_CHANGED,
}

enum class Preferences {
    CAR_MODEL,
    CAR_COLOR,
    NUMBER_OF_SEATS,
    LAST_ORIGIN_LOCATION__LAT_LNG,
    LAST_ORIGIN_LOCATION__READABLE,
}

val geocodingCache = mutableMapOf<LatLng, String>()

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
    Database.requestJsonObjectFromGoogleApi(
        partialUrl = "maps/api/geocode/json",
        getParams = mapOf(
            "key" to context.getString(R.string.SECRET_GOOGLE_API_KEY),
            "latlng" to "${latLng.latitude},${latLng.longitude}"
        )
    ) { jsonObject ->
        Log.v("Google Geocode", jsonObject.toString(4))
        CoroutineScope(Dispatchers.Main).launch {
            // dirty hack, sorry
            val result = if (jsonObject.getString("status") == "ZERO_RESULTS")
                alternativeGeocode(context, latLng)
            else
                jsonObject.getJSONArray("results").getJSONObject(0).getString("formatted_address")
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
fun alternativeGeocode(context: Context?, latLng: LatLng): String {
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
                (0..address.maxAddressLineIndex).map { i -> address.getAddressLine(i) }.joinToString(separator = ", ")
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


fun jsonObjOf(vararg pairs: Pair<String, Any?>): JSONObject =
    JSONObject(mapOf(*pairs))


fun Location.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun LatLng.toLocation(): Location {
    return Location("useless parameter")
        .apply {
            latitude = this@toLocation.latitude
            longitude = this@toLocation.longitude
        }
}

fun LatLng.encodeToString(): String {
    return "$latitude    $longitude"
}

fun String.decodeToLatLng(): LatLng {
    val (latitude, longitude) = this.split("    ").map { it.toDouble() }
    return LatLng(latitude, longitude)
}

fun getProfilePicUrl(facebookId: String, callback: (String) -> Unit) {
    if (facebookId.equals("fakeprofile")) {
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

fun parseStandardDatetime(datetimeString: String): Datetime {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(datetimeString)
}

fun formatDatetime(datetime: Datetime): String {
    return SimpleDateFormat("EEEE, dd/M/yy 'at' HH:mm", Locale.getDefault()).format(datetime)
}