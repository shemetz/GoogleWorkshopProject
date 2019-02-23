package org.team2.ridetogather

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

object InternetRequests {
    private val tag = InternetRequests::class.java.simpleName
    private lateinit var requestQueue: RequestQueue
    const val CACHE_TIME_MS = 1000 * 5  // 5 seconds

    data class CacheEntry(
        val response: Any,
        val birthTime: Datetime
    )

    val cacheOfGETs = mutableMapOf<String, CacheEntry>()

    fun initialize(activityContext: Context) {
        requestQueue = Volley.newRequestQueue(activityContext.applicationContext)
    }

    fun <T : Any> addRequest(request: Request<T>) {
        requestQueue.add(request)
    }

    fun logResponseError(error: VolleyError, url: String) {
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
}

fun initializeAppIfNeeded(activityContext: Context) {
    if (Database.idOfCurrentUser != -1) {
        Log.d("initializeAppIfNeeded", "Database is already initialized, it's OK")
        return
    }
    Log.i("initializeAppIfNeeded", "Initializing app…")
    // applicationContext is key, it keeps you from leaking the
    // Activity or BroadcastReceiver if someone passes one in.
    InternetRequests.initialize(activityContext)
    val prefManager = PrefManager(activityContext)
    Database.idOfCurrentUser = prefManager.thisUserId
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