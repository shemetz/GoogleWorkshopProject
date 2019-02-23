package org.team2.ridetogather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import android.widget.Toast
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.math.max

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val tag = MapsActivity::class.java.simpleName

    private lateinit var map: GoogleMap
    private lateinit var event: Event
    private var ride: Ride? = null
    private var pickups: List<Pickup>? = null
    private lateinit var originMarker: Marker
    private lateinit var newPickupMarker: Marker
    private lateinit var eventMarker: Marker
    private lateinit var requestCode: RequestCode
    private var drawnRoute: MutableList<Polyline> = mutableListOf()
    private var routeJson: JSONObject? = null
    private val pickupMarkers: MutableList<PickupMarker> = mutableListOf()
    private var mainMarker: Marker? = null
    private var mainMarkerIsFollowingCamera =
        false  // when not pinned it will be half-transparent and will follow the camera
    private var preexistingOriginLocation: LatLng? = null
    private var somethingChanged = false  // true if something was edited
    private var selectedPickupMarker: PickupMarker? = null
    private val mapLoadingLatch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        initializeAppIfNeeded(this)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val requestCodeInt = intent.getIntExtra(Keys.REQUEST_CODE.name, -1)
        requestCode = MapsActivity.Companion.RequestCode.values()[requestCodeInt]
        Log.d(tag, "Created $tag for Event ID $eventId, Request code: $requestCode")
        preexistingOriginLocation = intent.getStringExtra(Keys.LOCATION.name)?.decodeToLatLng() // null on first time
        setupBeforeSetups()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Log.i(tag, "Loading stuff from server to put on the map…")
        Database.getEvent(eventId) { event ->
            this.event = event
            if (requestCode == RequestCode.PICK_DRIVER_ORIGIN) {
                ride = null
                pickups = null
                onEverythingLoadedFromServer()
            } else {
                val rideId = intent.getIntExtra(Keys.RIDE_ID.name, -1)
                Database.getRide(rideId) { ride ->
                    this.ride = ride
                    Database.getPickupsForRide(rideId) { pickups ->
                        this.pickups = pickups
                        onEverythingLoadedFromServer()
                    }
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.i(tag, "Map is ready.")
        mapLoadingLatch.countDown()
    }

    private fun setupBeforeSetups() {
        fab_confirm_location.visibility = View.GONE
        fab_pin_or_unpin.visibility = View.GONE
        fab_decline.visibility = View.GONE
        fab_plus_or_minus.visibility = View.GONE
        fab_change_time.visibility = View.GONE
        route_total_time.visibility = View.GONE
    }

    private fun onEverythingLoadedFromServer() {
        CoroutineScope(Dispatchers.Default).launch {
            mapLoadingLatch.await()
            Log.i(tag, "Everything is loaded! Beginning setup of everything.")
            CoroutineScope(Dispatchers.Main).launch {
                setupEverything()
            }
        }
    }

    private fun setupEverything() {
        setupMarkers()
        setupMapStartingPosition()
        setupMainMarker()
        setupMapOptions()
        setupButtons()
        setupFabVisibility()
        setupPlaceAutocomplete()
        setupRoute()
    }

    private fun setupMarkers() {
        val eventLocation = event.location.toLatLng()
        val eventMarkerOptions = MarkerOptions()
            .title(event.name)
            .position(eventLocation)
            .icon(createCombinedMarker(R.drawable.ic_check_circle_green_sub_icon, 48))
            .zIndex(1f)
        eventMarker = map.addMarker(eventMarkerOptions)

        // preexistingOriginLocation exists during ride creation only
        val originLocation =
            intent.getStringExtra(Keys.LOCATION.name)?.decodeToLatLng()
                ?: ride?.origin?.toLatLng()
                // default origin location - will only be used for first ride created by the user!
                ?: LatLng(eventLocation.latitude - 0.01, eventLocation.longitude)
        val originTitle = "The ride starts here"
        val originMarkerOptions = MarkerOptions()
            .position(originLocation)
            .title(originTitle)
            .icon(createCombinedMarker(R.drawable.ic_car_white_sub_icon, 36))
            .zIndex(5f)
        originMarker = map.addMarker(originMarkerOptions)
        originMarker.tag = "originMarker"
        if (ride != null)
            originMarker.snippet = ride!!.departureTime.shortenedTime()

        if (pickups != null) {
            pickups!!
                .filter { !it.denied }
                .filter { it.inRide || requestCode == RequestCode.CONFIRM_OR_DENY_PASSENGERS }
                .forEach { pickup ->
                    Database.getUser(pickup.userId) { passenger ->
                        val position = pickup.pickupSpot.toLatLng()
                        val markerOptions = MarkerOptions()
                            .position(position)
                            .title(passenger.name)
                            .snippet(pickup.pickupTime.shortenedTime())
                            .icon(createCombinedMarker(R.drawable.ic_person_white_sub_icon, 36))
                            .zIndex(4f)
                            .alpha(if (pickup.inRide) 1f else 0.5f)
                        val marker = map.addMarker(markerOptions)
                        val state = when {
                            pickup.denied -> PickupMarkerState.DENIED
                            pickup.inRide -> PickupMarkerState.ACCEPTED
                            else -> PickupMarkerState.PENDING
                        }
                        val pickupMarker = PickupMarker(pickup, marker, IconTarget(marker), state, state)
                        marker.tag = pickupMarker
                        pickupMarkers.add(pickupMarker)
                        ApiRequests.getProfilePicUrl(passenger.facebookProfileId) { picUrl ->
                            Log.i(tag, "Picasso is loading profile pic")
                            Picasso.get()
                                .load(picUrl)
                                .placeholder(R.drawable.placeholder_profile_circle)
                                .error(R.drawable.placeholder_profile_circle)
                                .resize(48, 48)
                                .transform(CircleTransform())
                                .into(pickupMarker.iconTarget)
                        }
                    }
                }
        }
    }

    private fun setupMainMarker() {
        when (requestCode) {
            RequestCode.PICK_DRIVER_ORIGIN -> {
                mainMarkerIsFollowingCamera = true
                mainMarker = originMarker
                setPinned(false)
                if (preexistingOriginLocation != null) {
                    onPinButtonClick()
                }
            }
            RequestCode.PICK_PASSENGER_LOCATION -> {
                val defaultNewPickupLocation = map.cameraPosition.target // right in center
                val newPickupTitle = "You will be picked up here"
                val newPickupMarkerOptions = MarkerOptions()
                    .position(defaultNewPickupLocation)
                    .title(newPickupTitle)
                    .icon(createCombinedMarker(R.drawable.ic_person_white_sub_icon, 36)) // TODO use profile picture
                    .zIndex(6f)
                newPickupMarker = map.addMarker(newPickupMarkerOptions)
                mainMarkerIsFollowingCamera = true
                mainMarker = newPickupMarker
                setPinned(false)
            }
            RequestCode.CONFIRM_OR_DENY_PASSENGERS -> {
                mainMarker = null
            }
            RequestCode.JUST_LOOK_AT_MAP -> {
                mainMarker = null
            }
        }
        mainMarker?.tag = "mainMarker"
    }

    private fun setupMapStartingPosition() {
        if (requestCode == RequestCode.PICK_DRIVER_ORIGIN && preexistingOriginLocation == null) {
            val startingZoomLevel = 13.0f
            val defaultOriginMarkerLocation =
                LatLng(event.location.toLatLng().latitude - 0.01, event.location.toLatLng().longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(event.location.toLatLng(), startingZoomLevel / 2))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultOriginMarkerLocation, startingZoomLevel))
        } else {
            val builder = LatLngBounds.Builder()
            builder.include(event.location.toLatLng())
            builder.include(originMarker.position)
            pickupMarkers.forEach {
                builder.include(it.marker.position)
            }
            val bounds = builder.build()
            val padding = 120  // in pixels, between bounds and edge of view
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    private fun setupMapOptions() {
        map.isBuildingsEnabled = true
        map.isIndoorEnabled = false
        map.isTrafficEnabled = false
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.setOnCameraMoveListener {
            if (mainMarkerIsFollowingCamera) {
                moveMarker(mainMarker!!, map.cameraPosition.target)
                mainMarker!!.hideInfoWindow()
                if (requestCode == RequestCode.PICK_DRIVER_ORIGIN) {
                    cancelCurrentRoute()
                }
            }
        }
        map.setOnMarkerClickListener { marker: Marker? ->
            if (marker?.tag == null) {
                unselectPickupMarker()
                return@setOnMarkerClickListener false // move camera to marker, display info
            }
            when (marker.tag!!::class.java) {
                String::class.java -> {
                    unselectPickupMarker()
                    if ((marker.tag as String) == "mainMarker")
                        return@setOnMarkerClickListener onPinButtonClick()
                    else // e.g. if ((marker.tag as String) == "originMarker")
                        return@setOnMarkerClickListener false // move camera to marker, display info
                }
                PickupMarker::class.java -> {
                    return@setOnMarkerClickListener onPickupMarkerClick(marker.tag as PickupMarker)
                }
            }
            unselectPickupMarker()
            return@setOnMarkerClickListener false // move camera to marker, display info
        }

        map.setOnMapClickListener {
            unselectPickupMarker()
        }

        map.setOnPoiClickListener {
            unselectPickupMarker()
        }

        map.setOnGroundOverlayClickListener {
            unselectPickupMarker()
        }

        // If location is enabled, we'll add a location marker and a button to go there
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }

        map.setPadding(0, 50, 0, 0)
    }

    private fun setupButtons() {
        fab_confirm_location.setOnClickListener {
            when (requestCode) {
                RequestCode.PICK_PASSENGER_LOCATION -> {
                    somethingChanged = true
                    finishAndReturn()
                }
                RequestCode.PICK_DRIVER_ORIGIN -> {
                    finishAndReturn()
                }
                RequestCode.CONFIRM_OR_DENY_PASSENGERS -> {
                    hideFab(fab_confirm_location)
                    hideFab(fab_pin_or_unpin)

                    /*
                    What we want to do here:
                    First we ask the user if they want to delete the non-accepted pickups.
                    Then we update the server with the updated and deleted pickups.
                    Finally once all pickups have been handled we finish the activity; if we
                    did it sooner it would have gotten us to an incorrectly-updated ride page.
                     */

                    var counter = pickupMarkers.size
                    fun countDown() {
                        counter--
                        if (counter == 0) {
                            finishAndReturn()
                        }
                    }

                    // do this once, in case it was empty so the for-loop won't do anything
                    counter++
                    countDown()

                    for (pickupMarker in pickupMarkers) {
                        Database.updatePickup(pickupMarker.pickup) {
                            countDown()
                        }

                        // Send notification if needed
                        when (pickupMarker.newState) {
                            PickupMarkerState.PENDING -> Unit
                            pickupMarker.stateOnServer -> Unit  // to prevent irrelevant notification
                            PickupMarkerState.ACCEPTED, PickupMarkerState.DENIED -> {
                                Database.getUser(pickupMarker.pickup.userId) { pickupUser ->
                                    Database.getUser(Database.idOfCurrentUser) { currentUser ->
                                        ApiRequests.getProfilePicUrl(currentUser.facebookProfileId) { picUrl ->
                                            val title: String
                                            val message: String
                                            if (pickupMarker.newState == PickupMarkerState.ACCEPTED) {
                                                title = getString(R.string.notification_title_pickup_accepted)
                                                message = currentUser.name + " has accepted you to their ride."
                                            } else {
                                                title = getString(R.string.notification_title_pickup_rejected)
                                                message = currentUser.name + " has rejected you from their ride."
                                            }
                                            val to = arrayOf(pickupUser.firebaseId)
                                            val keys =
                                                hashMapOf(
                                                    Keys.RIDE_ID.name to ride!!.id,
                                                    Keys.DRIVER_PERSPECTIVE.name to false,
                                                    Keys.EVENT_ID.name to ride!!.eventId
                                                )
                                            Log.d("Firebase", to.toString())
                                            ApiRequests.sendFirebaseNotification(
                                                to, title, message, picUrl,
                                                "com.google.firebase.RIDE_PAGE", keys
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                RequestCode.JUST_LOOK_AT_MAP -> {
                    finishAndReturn()
                }
            }
        }

        fab_pin_or_unpin.setOnClickListener {
            val shouldMoveCamera = onPinButtonClick()
            if (!shouldMoveCamera) {
                map.animateCamera(CameraUpdateFactory.newLatLng(mainMarker!!.position), 500, null)
            }
        }

        fab_plus_or_minus.setOnClickListener {
            val pickupMarker = selectedPickupMarker ?: return@setOnClickListener
            val inRide = !pickupMarker.pickup.inRide
            pickupMarker.pickup.inRide = inRide
            pickupMarker.marker.alpha = if (inRide) 1.0f else 0.5f
            fab_plus_or_minus.setImageDrawable(getDrawable(if (inRide) R.drawable.ic_remove_black_24dp else R.drawable.ic_add_black_24dp))
            if (inRide) {
                if (pickupMarker.pickup.denied) {
                    hideFab(fab_decline)
                    hideFab(fab_change_time)
                    pickupMarker.pickup.denied = false
                }
                pickupMarker.newState = PickupMarkerState.ACCEPTED
                showFab(fab_change_time)
            } else {
                pickupMarker.newState = PickupMarkerState.PENDING
                hideFab(fab_change_time)
            }
            calculateRoute()
        }

        fab_decline.setOnClickListener {
            val pickupMarker = selectedPickupMarker ?: return@setOnClickListener
            if (!pickupMarker.pickup.inRide) return@setOnClickListener
            Database.getUser(pickupMarker.pickup.userId) { pickupUser ->
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.pickup_decline_title)
                    .setMessage(getString(R.string.pickup_decline_description).format(pickupUser.name))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        pickupMarker.pickup.inRide = false
                        pickupMarker.pickup.denied = true
                        pickupMarker.marker.alpha = 0.1f
                        fab_plus_or_minus.setImageDrawable(getDrawable(R.drawable.ic_add_black_24dp))
                        hideFab(fab_decline)
                        hideFab(fab_change_time)
                        pickupMarker.newState = PickupMarkerState.DENIED
                        calculateRoute()
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        //will be dismissed
                    }
                    .show()
            }
        }

        fab_change_time.setOnClickListener {
            val pickupMarker = selectedPickupMarker ?: return@setOnClickListener
            if (!pickupMarker.pickup.inRide) return@setOnClickListener
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, pickupMarker.pickup.pickupTime.hours)
            cal.set(Calendar.MINUTE, pickupMarker.pickup.pickupTime.minutes)
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                val newTime = TimeOfDay(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                if (newTime != pickupMarker.pickup.pickupTime) {
                    somethingChanged = true
                    pickupMarker.pickup.pickupTime = newTime
                    pickupMarker.marker.snippet = pickupMarker.pickup.pickupTime.shortenedTime()
                    refreshMarkerInfoWindow(selectedPickupMarker!!)
                }
                if (!pickupMarker.pickup.inRide) {
                    somethingChanged = true
                }
            }
            TimePickerDialog(
                this,
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun setupFabVisibility() {
        when (requestCode) {
            RequestCode.PICK_DRIVER_ORIGIN, RequestCode.PICK_PASSENGER_LOCATION -> {
                fab_confirm_location.visibility = View.INVISIBLE
                if (preexistingOriginLocation != null)
                    showFab(fab_confirm_location)
                fab_pin_or_unpin.visibility = View.INVISIBLE
                showFab(fab_pin_or_unpin)
                if (mainMarker!!.alpha == 0.5f)
                    hideFab(fab_confirm_location)
                fab_confirm_location.setImageDrawable(getDrawable(R.drawable.ic_done_black_24dp))
            }
            RequestCode.CONFIRM_OR_DENY_PASSENGERS -> {
                fab_confirm_location.visibility = View.INVISIBLE
                showFab(fab_confirm_location)
                fab_pin_or_unpin.visibility = View.GONE
                fab_decline.visibility = View.INVISIBLE
                fab_plus_or_minus.visibility = View.INVISIBLE
                fab_change_time.visibility = View.INVISIBLE
                fab_confirm_location.setImageDrawable(getDrawable(R.drawable.ic_done_all_black_24dp))
            }
            RequestCode.JUST_LOOK_AT_MAP -> {
                fab_confirm_location.visibility = View.INVISIBLE
                showFab(fab_confirm_location)
                fab_pin_or_unpin.visibility = View.GONE
                fab_decline.visibility = View.GONE
                fab_plus_or_minus.visibility = View.GONE
                fab_change_time.visibility = View.GONE
                fab_confirm_location.setImageDrawable(getDrawable(R.drawable.ic_done_black_24dp))

                // this doesn't work for some reason :(
                fab_confirm_location.setBackgroundResource(R.drawable.button_shape)

            }
        }
    }

    private fun setupPlaceAutocomplete() {

        val placeFragment =
            fragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as PlaceAutocompleteFragment

        placeFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place?) {
                place!!
                Log.i(tag, "User selected place from autocomplete place picker: ${place.name} = ${place.address}")
                unselectPickupMarker()
                map.animateCamera(CameraUpdateFactory.newLatLng(place.latLng))
                if (mainMarker != null) {
                    mainMarker!!.position = place.latLng
                    setPinned(false)
                    mainMarkerIsFollowingCamera = true
                    cancelCurrentRoute()
                    onPinButtonClick()
                }
            }

            override fun onError(status: Status?) {
                Log.e(tag, "An error occurred in the place fragment! status=$status")
            }
        })

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        val mapView = mapFragment.view!!
        val locationButton =
            (mapView.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(Integer.parseInt("2"))
        val rlp = locationButton.layoutParams as (RelativeLayout.LayoutParams)
        // position on top right, but with margin due to place autocomplete fragment
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        rlp.setMargins(0, 96, 16, 0)
    }

    private fun setupRoute() {
        if (routeJson == null) {
            when (requestCode) {
                RequestCode.PICK_DRIVER_ORIGIN -> {
                    // No route yet - we're still picking the driver origin
                }
                else -> {
                    // Calculate route
                    // TODO - store routes on server to prevent needless waste of API usage
                    calculateRoute()
                }
            }
        } else {
            drawRoute(routeJson!!)
        }
    }

    private fun finishAndReturn() {
        val resultIntent = Intent()
        if (mainMarker != null) {
            val locationStr = mainMarker!!.position.encodeToString()
            if (intent.getStringExtra(Keys.LOCATION.name) != locationStr)
                somethingChanged = true
            resultIntent.putExtra(Keys.LOCATION.name, locationStr)
        }
        resultIntent.putExtra(Keys.ROUTE_JSON.name, routeJson?.toString() ?: "")
        resultIntent.putExtra(Keys.SOMETHING_CHANGED.name, somethingChanged)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun setPinned(pinned: Boolean) {
        when {
            mainMarker == null -> {
            }
            pinned -> {
                mainMarker!!.alpha = 1.0f
                showFab(fab_confirm_location)
                fab_pin_or_unpin.setImageDrawable(getDrawable(R.drawable.ic_edit_black_24dp))
            }
            else -> {
                mainMarker!!.alpha = 0.5f
                hideFab(fab_confirm_location)
                fab_pin_or_unpin.setImageDrawable(getDrawable(R.drawable.ic_pin_drop_black_24dp))
            }
        }
    }

    private fun onPinButtonClick(): Boolean {
        if (mainMarker == null)
            return false // move camera to marker, display info
        return if (!mainMarkerIsFollowingCamera) {
            Log.d(tag, "Unpinning main marker")
            setPinned(false)
            val handler = Handler()
            /// Let the camera move towards the marker a bit, before making the marker move to the camera
            handler.postDelayed({
                mainMarkerIsFollowingCamera = true
            }, 500)
            false // move camera to marker, display info
        } else {
            Log.d(tag, "Pinning main marker")
            mainMarkerIsFollowingCamera = false
            setPinned(true)
            if (routeJson == null && requestCode == RequestCode.PICK_DRIVER_ORIGIN)
                calculateRoute()
            true
        }
    }

    private fun cancelCurrentRoute() {
        drawnRoute.forEach { it.remove() }
        routeJson = null
        route_total_time.visibility = View.INVISIBLE
    }

    private fun calculateRoute() {
        /*
        Due to some weird problem with the google routes API, only geocoded locations
        work as waypoints - latlng doesn't work no matter which format I try :(
         */

        val origin = originMarker.position
        val destination = eventMarker.position
        val pickupsInRoute = pickupMarkers
            .filter { it.pickup.inRide }
            .take(23) // that's the maximum
        val waypoints = pickupsInRoute
            .map { it.marker.position }
        CoroutineScope(Dispatchers.Default).launch {
            Log.v(tag, "Starting countdown(${waypoints.size})…")
            val countdown = CountDownLatch(waypoints.size)
            waypoints.forEach { latLng ->
                ApiRequests.geocode(this@MapsActivity, latLng) {
                    countdown.countDown()
                    Log.v(tag, "Completed $latLng → $it")
                }
            }
            Log.v(tag, "Awaiting countdown…")
            countdown.await()
            Log.v(tag, "Done with countdown!")

            val waypointsAsParameter = waypoints
                .map {
                    jsonObjOf(
                        "location" to geocodingCache[it],
                        "stopover" to true
                    )
                }

            val params = mapOf(
                "key" to getString(R.string.SECRET_GOOGLE_API_KEY),
                "origin" to "${origin.latitude},${origin.longitude}",
                "destination" to "${destination.latitude},${destination.longitude}",
                "travelMode" to "DRIVING"
            ) + if (waypoints.isNotEmpty()) mapOf(
                "waypoints" to JSONArray(waypointsAsParameter).toString(),
                "optimizeWaypoints" to "true"
            ) else emptyMap()
            Log.i(tag, "Requesting route from Google Maps API…")
            ApiRequests.requestJsonObjectFromGoogleApi(
                "maps/api/directions/json", params
            ) { responseJson ->
                Log.i(tag, "Got a response! \\o/")
                if (responseJson.optJSONArray("routes")?.length() != 0) {
                    routeJson = responseJson
                    drawRoute(routeJson!!)
                    updatePickupTimesAutomatically(routeJson!!, pickupsInRoute)
                } else {
                    runOnUiThread {
                        Log.e(tag, "Failed to find any route..!")
                        Log.e(tag, responseJson.toString(4))
                        Toast.makeText(this@MapsActivity, "No route was found :(", Toast.LENGTH_SHORT).show()
                        // remove existing route if it exists
                        cancelCurrentRoute()
                    }
                }
            }
        }
    }

    /**
     * NOTE: This function doesn't run on the main (UI) thread.
     */
    @SuppressLint("SetTextI18n")
    private fun drawRoute(json: JSONObject) {
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) {
            Log.e(tag, "Failed to find any route..!")
            Log.e(tag, json.toString(4))
            runOnUiThread {
                Toast.makeText(this, "Sorry, Google Maps API call failed :(", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (routes.length() > 1)
            Log.w(tag, "There's more than one route!?")
        val onlyRoute = routes.getJSONObject(0)
        if (onlyRoute.getJSONArray("warnings").length() > 0) {
            Log.w(tag, "WARNINGS")
            Log.w(tag, onlyRoute.getJSONArray("warnings").join("\n"))
        }
        val legs = onlyRoute.getJSONArray("legs")
        val allLegs: List<JSONObject> = (0 until legs.length()).map { i -> legs.getJSONObject(i) }
        val path = mutableListOf<List<LatLng>>()
        allLegs.forEach { leg ->
            val steps = leg.getJSONArray("steps")
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
        }
        val totalDurationSeconds = allLegs.sumBy { it.optJSONObject("duration")?.optInt("value") ?: 0 }
        val totalDurationStr = durationToString(totalDurationSeconds)
        val combinedPath = path.flatten()
        runOnUiThread {
            drawnRoute.forEach { it.remove() }
            val polyLine = map.addPolyline(
                PolylineOptions()
                    .addAll(combinedPath)
                    .color(ContextCompat.getColor(this, R.color.routeColor))
                    .zIndex(-300f)
                    .width(16f)
            )
            drawnRoute.add(polyLine)
            route_total_time.visibility = View.VISIBLE
            route_total_time.text = "Total time: ~$totalDurationStr"
        }
    }

    private fun updatePickupTimesAutomatically(routeJson: JSONObject, pickupMarkersInRoute: List<PickupMarker>) {
        if (pickupMarkersInRoute.isEmpty())
            return
        val routes = routeJson.getJSONArray("routes")
        val onlyRoute = routes.getJSONObject(0)
        val legs = onlyRoute.getJSONArray("legs")
        val allLegs: List<JSONObject> = (0 until legs.length()).map { i -> legs.getJSONObject(i) }
        var totalDurationSeconds = 0
        val waypointOrder = onlyRoute.getJSONArray("waypoint_order")
        allLegs.forEachIndexed { index, leg ->
            totalDurationSeconds += leg.optJSONObject("duration")?.optInt("value") ?: 0
            if (index < waypointOrder.length()) {
                val waypointIndex = waypointOrder.getInt(index)
                val hours = (ride!!.departureTime.hours + (totalDurationSeconds / 60 / 60)) % 24
                val minutes = (ride!!.departureTime.minutes + (totalDurationSeconds / 60)) % 60
                val pickupMarker = pickupMarkersInRoute[waypointIndex]
                pickupMarker.pickup.pickupTime = TimeOfDay(hours, minutes)
                pickupMarker.marker.snippet = pickupMarker.pickup.pickupTime.shortenedTime()
                refreshMarkerInfoWindow(pickupMarker)
            }
        }
    }

    class PickupMarker(
        val pickup: Pickup,
        val marker: Marker,
        val iconTarget: IconTarget,
        val stateOnServer: PickupMarkerState,
        var newState: PickupMarkerState
    )

    private fun selectPickupMarker(pickupMarker: PickupMarker) {
        if (selectedPickupMarker == null) {
            val inRide = pickupMarker.pickup.inRide
            fab_plus_or_minus.setImageDrawable(getDrawable(if (inRide) R.drawable.ic_remove_black_24dp else R.drawable.ic_add_black_24dp))
            showFab(fab_plus_or_minus)
            if (!pickupMarker.pickup.denied) {
                showFab(fab_decline)
                if (inRide)
                    showFab(fab_change_time)
            }
        }
        selectedPickupMarker = pickupMarker
    }

    private fun unselectPickupMarker() {
        if (selectedPickupMarker != null) {
            selectedPickupMarker!!.marker.hideInfoWindow()
            selectedPickupMarker = null
            hideFab(fab_change_time)
            hideFab(fab_decline)
            hideFab(fab_plus_or_minus)
        }
    }

    private fun refreshMarkerInfoWindow(pickupMarker: PickupMarker) {
        if (pickupMarker.marker.isInfoWindowShown) {
            pickupMarker.marker.hideInfoWindow()
            pickupMarker.marker.showInfoWindow()
        }
    }

    private fun onPickupMarkerClick(pickupMarker: PickupMarker): Boolean {
        return when (requestCode) {
            RequestCode.PICK_PASSENGER_LOCATION, RequestCode.JUST_LOOK_AT_MAP -> {
                false  // move camera to marker and display info
            }
            RequestCode.CONFIRM_OR_DENY_PASSENGERS -> {
                selectPickupMarker(pickupMarker)
                false  // move camera to marker and display info
            }
            RequestCode.PICK_DRIVER_ORIGIN -> {
                Log.e(tag, "This thing should never happen")
                false
            }
        }
    }

    private val markerHandlers: MutableMap<Marker, Handler> = mutableMapOf()
    /**
     * From https://stackoverflow.com/a/13912034/1703463
     */
    private fun moveMarker(marker: Marker, toPosition: LatLng) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val startPoint = map.projection.toScreenLocation(marker.position)
        val startLatLng = map.projection.fromScreenLocation(startPoint)
        val duration: Long = 10

        val interpolator = LinearInterpolator()
        markerHandlers[marker] = handler
        handler.post(object : Runnable {
            override fun run() {
                if (markerHandlers[marker] != handler) {
                    // We have multiple move handlers for this marker - let's ignore them if
                    // they're not the most recent one
                    return
                }
                val elapsed = SystemClock.uptimeMillis() - start
                val fractionOfTheWayThrough = max(0f, elapsed.toFloat() / duration)
                val t = interpolator.getInterpolation(fractionOfTheWayThrough)
                val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude
                val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude
                marker.position = LatLng(lat, lng)

                if (t < 1.0) {
                    handler.postDelayed(this, 0)
                }
                if (t <= 0) {
                    markerHandlers.remove(marker)
                }
            }
        })
    }

    /**
     * From https://stackoverflow.com/a/48356646/1703463
     */
    private fun createCombinedMarker(@DrawableRes subIconDrawable: Int, size: Int): BitmapDescriptor {
        val bottomIconDrawable: Int = when (size) {
            36 -> {
                R.drawable.ic_marker_full_blue_36dp
            }
            48 -> {
                R.drawable.ic_marker_full_blue_48dp
            }
            else -> {
                R.drawable.ic_marker_full_blue_48dp
            }
        }
        val background = ContextCompat.getDrawable(this, bottomIconDrawable)
        background!!.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val vectorDrawable = ContextCompat.getDrawable(this, subIconDrawable)
        val leftOffset = background.intrinsicWidth / 2 - size / 2
        val upOffset = background.intrinsicHeight / 2 - size / 2 - size / 4
        vectorDrawable!!.setBounds(
            leftOffset,
            upOffset,
            vectorDrawable.intrinsicWidth + leftOffset,
            vectorDrawable.intrinsicHeight + upOffset
        )
        val bitmap = Bitmap.createBitmap(background.intrinsicWidth, background.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    inner class IconTarget(private val marker: Marker) : com.squareup.picasso.Target {
        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            Log.e(tag, "Failed to load facebook profile icon")
        }

        override fun onBitmapLoaded(profileBitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            if (profileBitmap == null) {
                Log.e(tag, "Facebook profile icon loaded as null")
                return
            }
            val background = ContextCompat.getDrawable(this@MapsActivity, R.drawable.ic_marker_full_blue_36dp)
            background!!.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
            val vectorDrawable = BitmapDrawable(resources, profileBitmap)
            val leftOffset = background.intrinsicWidth / 2 - profileBitmap.width / 2
            val upOffset = background.intrinsicHeight / 2 - profileBitmap.height / 2 - 12
            vectorDrawable.setBounds(
                leftOffset,
                upOffset,
                vectorDrawable.intrinsicWidth + leftOffset,
                vectorDrawable.intrinsicHeight + upOffset
            )
            Log.i(
                tag, "ok so it's intWidth=${background.intrinsicWidth}, intHeight=${background.intrinsicHeight}, " +
                        "leftOffset=$leftOffset, upOffset=$upOffset, profileBitmap=${profileBitmap.width}x${profileBitmap.height}"
            )
            val finalBitmap =
                Bitmap.createBitmap(background.intrinsicWidth, background.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(finalBitmap)
            background.draw(canvas)
            vectorDrawable.draw(canvas)
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(finalBitmap))
        }
    }

    companion object {
        enum class RequestCode {
            PICK_DRIVER_ORIGIN,
            PICK_PASSENGER_LOCATION,
            CONFIRM_OR_DENY_PASSENGERS,
            JUST_LOOK_AT_MAP,
        }

        enum class PickupMarkerState {
            PENDING,
            ACCEPTED,
            DENIED
        }
    }

    /*
    Thanks to these little functions, showing and hiding FABs will work with a slightly increasing
    delay for each FAB, resulting in a cute animation :)
    (this really puts the "fun" in kotlin functions)
     */
    private var fabAnimationDelay: Long = 0
    private var fabAnimationLastMoment: Long = System.currentTimeMillis()
    private fun delayMomentarilyAnd(doThing: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - fabAnimationLastMoment > 100) {
            fabAnimationDelay = 0
        }
        fabAnimationLastMoment = now
        val previousDelay = fabAnimationDelay
        fabAnimationDelay += 75
        val handler = Handler()
        handler.postDelayed({
            doThing()
        }, previousDelay)
    }

    private fun showFab(fab: FloatingActionButton) = delayMomentarilyAnd {
        fab.show()
    }

    private fun hideFab(fab: FloatingActionButton) = delayMomentarilyAnd {
        fab.hide()
    }
}
