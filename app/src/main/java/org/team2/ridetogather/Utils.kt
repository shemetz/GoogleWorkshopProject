package org.team2.ridetogather

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.math.absoluteValue

enum class Keys {
    RIDE_ID,
    EVENT_ID,
    LOCATION,
    ROUTE_JSON,
    CHANGE_BTN,
}

enum class Preferences {
    CAR_MODEL,
    CAR_COLOR,
    NUMBER_OF_SEATS,
    LAST_ORIGIN_LOCATION__LAT_LNG,
    LAST_ORIGIN_LOCATION__READABLE,
}

enum class PickupStatus {
    PENDING,
    APPROVED,
    DECLINED,
    NOT_EXIST,
}

/**
 * Converts a location into a human-readable string.
 * For example, the location {latitude = 32.055436; longitude = 34.753070} will
 * cause the following readable location string to be returned:
 * "Mifrats Shlomo Promenade 5, Tel Aviv-Yafo, Israel"
 *
 * NOTE: This function is slow because it uses a Geocoder.
 * Try to only use it in asynchronous stuff (e.g. when updating text fields).
 */
fun readableLocation(context: Context, location: Location): String {
    fun coordinatesString(): String {
        val absoluteLatitude = Location.convert(location.latitude.absoluteValue, Location.FORMAT_DEGREES)
        val absoluteLongitude = Location.convert(location.longitude.absoluteValue, Location.FORMAT_DEGREES)
        val latitudeDirection = if (location.latitude >= 0) "N" else "S"
        val longitudeDirection = if (location.longitude >= 0) "N" else "S"
        return "$absoluteLatitude $latitudeDirection, $absoluteLongitude $longitudeDirection"
    }

    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
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
            "Shortened Location", "IO error encountered while reverse-geocoding. Location = $location", ioException
        )
        return coordinatesString()
    } catch (illegalArgumentException: IllegalArgumentException) {
        // Catch invalid latitude or longitude values.
        Log.e(
            "Shortened Location",
            "IO error encountered while reverse-geocoding. Latitude = $location",
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