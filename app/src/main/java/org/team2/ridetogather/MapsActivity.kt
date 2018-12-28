package org.team2.ridetogather

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.*
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.support.v4.content.ContextCompat
import android.support.annotation.DrawableRes
import android.view.View
import com.google.android.gms.maps.model.BitmapDescriptor
import kotlinx.android.synthetic.main.activity_maps.*
import kotlin.math.max
import android.content.pm.PackageManager


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val tag = MapsActivity::class.java.simpleName

    private lateinit var map: GoogleMap
    private lateinit var originMarker: Marker
    private lateinit var eventMarker: Marker
    private var savedInstanceState: Bundle? = null // in case phone rotates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        Log.d(tag, "Created $tag for Event ID $eventId")
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
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        Log.d(tag, "Map is ready for Event ID $eventId")

        setupMarkers()
        setupMapStartingPosition()
        setupMapOptions()
        setupButtons()
        if (savedInstanceState != null)
            setupWithSavedInstanceBundle()
    }

    private fun setupMarkers() {
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val preexistingLocation = intent.getStringExtra(Keys.LOCATION.name)?.decodeToLatLng() // null on first time
        val event = Database.getEvent(eventId)!!
        val eventLocation = event.location.toLatLng()
        val eventMarkerOptions = MarkerOptions()
            .title(event.name)
            .position(eventLocation)
            .icon(createCombinedMarker(R.drawable.ic_check_circle_green_sub_icon, 48))
            .zIndex(1f)
        eventMarker = map.addMarker(eventMarkerOptions)
        eventMarker.tag = "The event marker - probably not gonna get used"

        val defaultOriginLocation = LatLng(eventLocation.latitude - 0.01, eventLocation.longitude)
        val originTitle = "Your ride starts here"  // NOTICE: We're using this as the unique identifier
        val originMarkerOptions = MarkerOptions()
            .position(preexistingLocation ?: defaultOriginLocation)
            .title(originTitle)
            .icon(createCombinedMarker(R.drawable.ic_car_white_sub_icon, 36))
            .zIndex(2f)
        originMarker = map.addMarker(originMarkerOptions)
        originMarker.tag = TheOriginMarker
    }

    private fun setupMapStartingPosition() {
        val preexistingLocation = intent.getStringExtra(Keys.LOCATION.name)?.decodeToLatLng() // null on first time
        val startingZoomLevel = 13.0f
        if (preexistingLocation == null) {
            setPinned(false)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(eventMarker.position, startingZoomLevel / 2))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(originMarker.position, startingZoomLevel))
        } else {
            setPinned(true)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(preexistingLocation, startingZoomLevel))
        }
    }

    private fun setupMapOptions() {
        map.isBuildingsEnabled = true
        map.isIndoorEnabled = true
        map.isTrafficEnabled = true
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.setOnCameraMoveListener {
            if (TheOriginMarker.isFollowingCamera) {
                moveMarker(originMarker, map.cameraPosition.target)
                originMarker.hideInfoWindow()
            }
        }
        map.setOnMarkerClickListener { marker: Marker? ->
            if (marker!!.tag == TheOriginMarker) {
                return@setOnMarkerClickListener onPinButtonClick()
            }
            return@setOnMarkerClickListener false // move camera to marker, display info
        }

        // The marker dragging might not be working, but I'm leaving it here just in case it is
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker?) {
                Log.i(tag, "Starting to drag a marker!")
                if (p0 == null || p0.tag != TheOriginMarker) {
                    return
                }
                Log.i(tag, "the origin marker!")
                TheOriginMarker.isBeingDragged = true
                setPinned(false)
            }

            override fun onMarkerDrag(p0: Marker?) {
                if (p0 == null || p0.tag != TheOriginMarker) {
                    return
                }
            }

            override fun onMarkerDragEnd(p0: Marker?) {
                if (p0 == null || p0.tag != TheOriginMarker) {
                    return
                }
                TheOriginMarker.isBeingDragged = false
                setPinned(true)
            }
        })

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
            val resultIntent = Intent()
            val locationStr = map.cameraPosition.target.encodeToString()
            resultIntent.putExtra(Keys.LOCATION.name, locationStr)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        fab_pin_or_unpin.setOnClickListener {
            val shouldMoveCamera = onPinButtonClick()
            if (!shouldMoveCamera) {
                map.animateCamera(CameraUpdateFactory.newLatLng(originMarker.position), 500, null)
            }
        }
    }

    private fun setupWithSavedInstanceBundle() {
        val savedInstanceState = this.savedInstanceState!!
        val cameraPosition = savedInstanceState.getParcelable<CameraPosition>(StoredInstanceKeys.CAMERA_POSITION.name)
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        val originLocation = savedInstanceState.getParcelable<LatLng>(StoredInstanceKeys.ORIGIN_LOCATION.name)
        originMarker.position = originLocation
        val pinned = savedInstanceState.getBoolean(StoredInstanceKeys.ORIGIN_IS_PINNED.name)
        if (pinned && TheOriginMarker.isFollowingCamera) {
            onPinButtonClick()
        }
    }

    object TheOriginMarker {
        var isFollowingCamera = true  // when not pinned it will be half-transparent and will follow the camera
        var isBeingDragged = false  // can also be dragged while not following camera, temporarily unpinning it
    }

    private fun setPinned(pinned: Boolean) {
        if (pinned) {
            originMarker.alpha = 1.0f
            fab_confirm_location.visibility = View.VISIBLE
            fab_confirm_location.isClickable = true
            fab_pin_or_unpin.setImageDrawable(getDrawable(R.drawable.ic_edit_black_24dp))
        } else {
            originMarker.alpha = 0.5f
            fab_confirm_location.visibility = View.GONE
            fab_confirm_location.isClickable = false
            fab_pin_or_unpin.setImageDrawable(getDrawable(R.drawable.ic_pin_drop_black_24dp))
        }
    }

    private fun onPinButtonClick(): Boolean {
        return if (!TheOriginMarker.isFollowingCamera) {
            originMarker.isDraggable = true
            setPinned(false)
            val handler = Handler()
            /// Let the camera move towards the marker a bit, before making the marker move to the camera
            handler.postDelayed({
                TheOriginMarker.isFollowingCamera = true
            }, 500)
            false // move camera to marker, display info
        } else {
            TheOriginMarker.isFollowingCamera = false
            originMarker.isDraggable = false
            setPinned(true)
            true
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
        val bottomIconDrawable: Int
        val leftOffset: Int
        val upOffset: Int
        when (size) {
            36 -> {
                bottomIconDrawable = R.drawable.ic_marker_full_blue_36dp
                leftOffset = 26
                upOffset = 13
            }
            48 -> {
                bottomIconDrawable = R.drawable.ic_marker_full_blue_48dp
                leftOffset = 42
                upOffset = 23
            }
            else -> {
                bottomIconDrawable = R.drawable.ic_marker_full_blue_48dp
                leftOffset = 42
                upOffset = 23
            }
        }
        val background = ContextCompat.getDrawable(this, bottomIconDrawable)
        background!!.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val vectorDrawable = ContextCompat.getDrawable(this, subIconDrawable)
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(StoredInstanceKeys.CAMERA_POSITION.name, map.cameraPosition)
        outState.putParcelable(StoredInstanceKeys.ORIGIN_LOCATION.name, originMarker.position)
        outState.putBoolean(StoredInstanceKeys.ORIGIN_IS_PINNED.name, !TheOriginMarker.isFollowingCamera)
        super.onSaveInstanceState(outState)
    }

    companion object {
        enum class RequestCode {
            PICK_DRIVER_ORIGIN
        }

        enum class StoredInstanceKeys {
            CAMERA_POSITION,
            ORIGIN_LOCATION,
            ORIGIN_IS_PINNED,
        }
    }
}
