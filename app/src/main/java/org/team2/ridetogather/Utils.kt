package org.team2.ridetogather

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


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

fun parseStandardDatetime(datetimeString: String): Datetime {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(datetimeString)
}

fun formatDatetime(datetime: Datetime): String {
    return SimpleDateFormat("EEEE, dd/M/yy 'at' HH:mm", Locale.getDefault()).format(datetime)
}

@Suppress("IntroduceWhenSubject")
fun durationToString(durationInSeconds: Int): String {
    if (durationInSeconds <= 0)
        return "Unknown~"
    val seconds = durationInSeconds % 60
    val minutes = (durationInSeconds / 60) % 60
    val hours = (durationInSeconds / 60 / 60) % 24
    val days = durationInSeconds / 60 / 60 / 24
    val secondsStr = when {
        minutes > 0 -> ""
        seconds == 0 -> "nothing"
        seconds == 1 -> "1 second"
        else -> "$seconds seconds"
    }
    val minutesStr = when {
        days > 0 -> ""
        minutes == 0 -> ""
        minutes == 1 -> "1 minute"
        else -> "$minutes minutes"
    }
    val hoursStr = when {
        hours == 0 -> ""
        hours == 1 -> "1 hour"
        else -> "$hours hours"
    }
    val daysStr = when {
        days == 0 -> ""
        days == 1 -> "1 day"
        else -> "$days days"
    }

    return "$daysStr $hoursStr $minutesStr $secondsStr".trimStart()
}

fun getBitmapFromURL(src: String): Bitmap? {
    try {
        val url = URL(src)
        val connection = url.openConnection() as HttpURLConnection
        connection.setDoInput(true)
        connection.connect()
        val input = connection.getInputStream()
        return BitmapFactory.decodeStream(input)
    } catch (e: IOException) {
        // Log exception
        return null
    }

}