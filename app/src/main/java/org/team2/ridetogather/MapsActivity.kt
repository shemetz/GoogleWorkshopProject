package org.team2.ridetogather

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val eventId = intent.getIntExtra(Keys.EVENT_ID.name, -1)
        val event = Database.getEvent(eventId)!!
        val eventLocation = event.location.toLatLng()
        val defaultOriginLocation = LatLng(eventLocation.latitude - 0.1, eventLocation.longitude - 0.1)
        // Add a marker in Sydney and move the camera
        val eventMarker = MarkerOptions()
            .title(event.name)
            .position(eventLocation)
            .icon(getMarkerIconFromDrawable(getDrawable(R.drawable.ic_pin_drop_black_24dp)))
        map.addMarker(eventMarker)
        val originTitle = "Your ride starts here"  // NOTICE: We're using this as the unique identifier
        val originMarker = MarkerOptions()
            .position(defaultOriginLocation)
            .title(originTitle)
            .anchor(0.5f, 0.5f)
        map.addMarker(originMarker)
//        map.moveCamera(CameraUpdateFactory.newLatLng(eventLocation))
        map.isBuildingsEnabled = true
        map.isIndoorEnabled = true
        map.isTrafficEnabled = true
        map.setOnCameraMoveListener {

        }
    }

    private fun getMarkerIconFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
