package il.ac.tau.team2.googleworkshopproject

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.io.IOException
import java.util.*
import kotlin.math.absoluteValue

enum class Keys {
    RIDE_ID,
    EVENT_ID,
}

fun shortenedLocation(context: Context, location: Location): String {
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
            addressString
        } else {
            // No address found for the location coordinates. Returning plain coordinates.
            coordinatesString()
        }
    } catch (ioException: IOException) {
        // Catch network or other I/O problems.
        Log.e(
            "Shortened Location", "IO error encountered while reverse-geocoding. Latitude = $location.latitude , " +
                    "Longitude =  $location.longitude", ioException
        )
        return coordinatesString()
    } catch (illegalArgumentException: IllegalArgumentException) {
        // Catch invalid latitude or longitude values.
        Log.e(
            "Shortened Location", "IO error encountered while reverse-geocoding. Latitude = $location.latitude , " +
                    "Longitude =  $location.longitude", illegalArgumentException
        )
        return coordinatesString()
    }
}