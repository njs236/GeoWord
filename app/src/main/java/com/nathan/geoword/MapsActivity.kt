package com.nathan.geoword

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.util.Log
import android.widget.ImageView
import android.widget.TextView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

private const val ARG_PARAM1 = "param1"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener {
    override fun onMarkerDragEnd(p0: Marker?) {
        //TODO: when marker is dragged, resend to firebase API.
    }

    override fun onMarkerDragStart(p0: Marker?) {
        //TODO: Marker is started to drag.
    }

    override fun onMarkerDrag(p0: Marker?) {
        //TODO: Marker is dragged
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        //TODO: when clicking on a marker, load story activity
        val intent = Intent(this, StoryActivity::class.java)
        startActivity(intent)
        return true
    }

    override fun onMapClick(p0: LatLng?) {
        //TODO: when clicking on map, load story activity
        val intent = Intent(this, StoryActivity::class.java)
        intent.putExtra(ARG_PARAM1, true)
        startActivity(intent)
    }

    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private var titleText : TextView? = null
    private var subTitleText: TextView? = null
    private var avatar: ImageView? = null
    private var navView: NavigationView? = null
    private val TAG = this.javaClass.simpleName
    // ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

       navView = findViewById(R.id.nav_view)
        avatar = findViewById(R.id.nav_imageView)
        titleText = navView!!.getHeaderView(0).findViewById(R.id.nav_tvTitle)
        subTitleText = navView!!.getHeaderView(0).findViewById(R.id.nav_tvSubtitle)


// Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            updateUser(auth.currentUser)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)
        titleText?.text = user?.displayName
        subTitleText?.text = user?.email
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
        mMap = googleMap
        mMap.setOnMapClickListener(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMarkerDragListener(this)

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}
