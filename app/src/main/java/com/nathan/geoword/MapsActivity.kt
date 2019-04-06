package com.nathan.geoword

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.nathan.geoword.Static.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION

private const val ARG_PARAM1 = "param1"
private const val ARG_LAT = "latitude"
private const val ARG_LNG = "longitude"
private const val ARG_TITLE = "title"
private const val ARG_PERSON = "person"
private const val ARG_DESC = "description"
private const val ARG_DOCREF = "document_id"
private const val ARG_LOGIN = "login"
private const val ARG_EDITABLE = "editable" // ie, your own notes.

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
        p0!!.showInfoWindow()

        return true
    }

    private fun onInfoWindowClick(): GoogleMap.OnInfoWindowClickListener = GoogleMap.OnInfoWindowClickListener { p0 ->

        loadNote(p0)
    }

    /*old marker code
    **/
    private fun loadNote(p0: Marker?) {
        var point = GeoPoint(p0!!.position.latitude, p0!!.position.longitude)
        db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot ->
            var map: HashMap<String, Boolean>? = null
            if (documentSnapshot != null) {
                map = documentSnapshot.get("friends") as HashMap<String, Boolean>
            }
            for ((friend, key) in map!!) {


                db.collection("notes")
                    .whereEqualTo("user", friend)
                    .whereEqualTo("latlng", point)
                    .get()
                    .addOnSuccessListener(retrieveMarkerSuccessListener())
                    .addOnFailureListener(retrieveMarkerFailureListener())
            }
            db.collection("notes")
                .whereEqualTo("user", auth.currentUser!!.uid)
                .whereEqualTo("latlng", point)
                .get()
                .addOnSuccessListener(retrieveMarkerSuccessListener())
                .addOnFailureListener(retrieveMarkerFailureListener())

        }
    }




    override fun onMapClick(p0: LatLng?) {
        //TODO: when clicking on map, load story activity
        val intent = Intent(this, StoryActivity::class.java)
        intent.putExtra(ARG_PARAM1, 1)
        intent.putExtra(ARG_LAT, p0!!.latitude)
        intent.putExtra(ARG_LNG, p0!!.longitude)
        startActivity(intent)

    }

    fun addMarkerSuccessListener() : OnSuccessListener<DocumentReference> = OnSuccessListener { documentReference->
        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)


    }

    fun addMarkerFailureListener() : OnFailureListener = OnFailureListener {e->
        Log.w(TAG, "Error adding document", e)

    }
    var avatars: HashMap<String, String> = HashMap()
    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var titleText : TextView
    private lateinit var subTitleText: TextView
    private lateinit var avatar: ImageView
    private lateinit var navView: NavigationView
    private val TAG = this.javaClass.simpleName
    private var markers = ArrayList<MarkerData>()
    private var zoomProperty: Float = -1f
    private var locationSetting: Boolean = false
    private lateinit var storage: FirebaseStorage

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    // ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setTitle(R.string.title_activity_maps)
        navView = findViewById(R.id.nav_view)
        avatar = navView.getHeaderView(0).findViewById(R.id.nav_imageView)
        avatar.setOnClickListener{ click->

            startActivity(Intent(this,
                ProfileActivity::class.java))
        }
        titleText = navView.getHeaderView(0).findViewById(R.id.nav_tvTitle)
        subTitleText = navView.getHeaderView(0).findViewById(R.id.nav_tvSubtitle)

// Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            updateUser(auth.currentUser)
            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()

            db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { document->

                if (document != null) {
                    titleText?.text = document.getString("name")
                    subTitleText?.text = document.getString("email")
                    if (document.get("avatar") != null) {
                        displayAvatar(document.getString("avatar"))
                    }
                }
            }

            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)



        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }


        navView.menu.getItem(1).setOnMenuItemClickListener { click->
            //logout
            signOut()

            true
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    private var avatarName: String? = null

    fun displayAvatar(imageName: String?) {
        avatarName = imageName
        if (imageName != null) {
            Log.w(TAG, "imageName: $imageName")


            val imageRef = storage.reference.child(imageName)
            GlideApp.with(this).load(imageRef).into(avatar)
        }
    }

    inner class CustomInfoWindow(context: Activity): GoogleMap.InfoWindowAdapter {
        private var context: Activity
        private lateinit var title: TextView
        private lateinit var subtitle: TextView
        private lateinit var avatar: ImageView
        private lateinit var storage: FirebaseStorage
        private lateinit var db: FirebaseFirestore
        private lateinit var auth: FirebaseAuth
        private var avatarName : String? = null
        init {
            this.context = context
        }


        private val TAG: String = this.javaClass.simpleName

        override fun getInfoContents(p0: Marker?): View? {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            val view = this.context.layoutInflater.inflate(R.layout.infowindow_note, null)
            title = view.findViewById(R.id.infowindow_title)
            subtitle = view.findViewById(R.id.infowindow_subtitle)
            avatar = view.findViewById(R.id.infowindow_avatar)
            storage = FirebaseStorage.getInstance()
            auth = FirebaseAuth.getInstance()
            for (marker in markers) {
                if (marker.latlng == p0!!.position) {
                    Log.w(TAG, "loading contents of window")
                    val titleText = marker.title
                    Log.w(TAG, "title: ${titleText}")
                    Log.w(TAG, "avatar: ${marker.image}")
                    title.setText(titleText)
                    subtitle.setText("")
                    if (marker.image != "") {
                        val imageRef = storage.reference.child(marker.image!!)
                        GlideApp.with(context).load(imageRef).into(avatar)
                    }
                }
            }



            return view

        }


        override fun getInfoWindow(p0: Marker?): View? {
            return null
        }
    }

    fun signOut() {
        if (auth.currentUser != null) {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }
    }

    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        //Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)
        //titleText?.text = user?.displayName
        //subTitleText?.text = user?.email
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
        mMap.setInfoWindowAdapter(CustomInfoWindow(this))
        mMap.setOnInfoWindowClickListener(onInfoWindowClick())

        updateLocationUI()

        getDeviceLocation()

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        //mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        markers.clear()
        avatars.clear()

        db.collection("users")
            .document(auth.currentUser!!.uid)
            .addSnapshotListener(zoomProperty())
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .addSnapshotListener(locationProperty())


        // get your markers.
        db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot->
            var map: HashMap<String, Boolean>? = null

            if (documentSnapshot != null) {
                map = documentSnapshot.get("friends") as HashMap<String, Boolean>
                if (documentSnapshot.getString("avatar")!= null) {
                    val avatar = documentSnapshot.getString("avatar")
                    avatars.put(auth.currentUser!!.uid, avatar!!)
                }
            }
            db.collection("notes")
                .whereEqualTo("user", auth.currentUser!!.uid)
                .orderBy("cr_date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(retrieveMarkersSuccessListener())
                .addOnFailureListener(retrieveMarkersFailureListener())
            for ((friend, key) in map!!) {
                db.collection("public").document(friend).get().addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null) {
                        if (documentSnapshot.getString("avatar")!= null) {
                            val avatar = documentSnapshot.getString("avatar")
                            avatars.put(friend, avatar!!)

                        }
                        db.collection("notes")
                            .whereEqualTo("user", friend)
                            .get()
                            .addOnSuccessListener(retrieveMarkersSuccessListener())
                            .addOnFailureListener(retrieveMarkersFailureListener())
                    }
                }

            }

        }


    }

    fun retrieveMarkersSuccessListener(): OnSuccessListener<QuerySnapshot> = OnSuccessListener { result ->


        for (document in result) {
            //Log.d(TAG, document.id + " =>" + document.data)
            val point = document.getGeoPoint("latlng")
            val title = document.getString("title")
            val latlng = LatLng(point!!.latitude, point!!.longitude)
            var avatar = ""
            val cr_date = document.getTimestamp("cr_date")


            if (document.getString("user") != null) {


                    val thisKey = document.getString("user")!!
                    for ((key, value) in avatars) {
                        if (key == thisKey) {
                            avatar = value
                        }
                    }

            }
            val marker = mMap.addMarker(MarkerOptions().position(latlng)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.person)))
            markers.add(MarkerData(title!!, "",cr_date!!.toDate(), avatar , latlng))


        }
        if (markers.count() > 0 && zoomProperty != -1f) {
            val lastRecord = markers[markers.count() - 1].latlng
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastRecord, zoomProperty))
        } else {

        }

    }
    fun retrieveMarkersFailureListener(): OnFailureListener = OnFailureListener { e->

        Log.w(TAG, "error fetching document", e)
    }

    fun retrieveMarkerSuccessListener(): OnSuccessListener<QuerySnapshot> = OnSuccessListener { result ->

        for (document in result) {
            val intent = Intent(this, StoryActivity::class.java)
            Log.d(TAG, document.id + " =>" + document.data)

            if (document.getString("user") == auth.currentUser!!.uid) {
                intent.putExtra(ARG_EDITABLE, true)
            }
            val point = document.getGeoPoint("latlng")
            /*val title = document.getString("title")
            val person = document.getString("person")
            val desc = document.getString("description")*/
            //val latlng = LatLng(point!!.latitude, point!!.longitude)
            //intent.putExtra(ARG_TITLE, title)
            //intent.putExtra(ARG_PERSON, person)
            //intent.putExtra(ARG_DESC, desc)
            intent.putExtra(ARG_LAT, point!!.latitude)
            intent.putExtra(ARG_LNG, point!!.longitude)
            intent.putExtra(ARG_DOCREF, document.id)
            Log.d(TAG, document.id)
            startActivity(intent)
        }
        // move camera to last seen location and update.



    }
    fun retrieveMarkerFailureListener(): OnFailureListener = OnFailureListener { e->

        Log.w(TAG, "error fetching document", e)
    }

    private var mDefaultLocation: LatLng? = null

    fun zoomProperty(): EventListener<DocumentSnapshot> = EventListener { snapshot, e ->

        if (e != null) {
            Log.w(TAG, "Listen failed.", e)
            return@EventListener
        }

        if (snapshot != null && snapshot.exists()) {
            Log.d(TAG, "Current data: " + snapshot.data)
            var newZoom = snapshot.data?.get("defaultZoom") as Long
            if (zoomProperty != newZoom.toFloat()) {
                zoomProperty = newZoom.toFloat()
                if (markers.count() > 0) {
                    val lastRecord = markers[markers.count() - 1].latlng
                    mDefaultLocation = lastRecord
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastRecord, zoomProperty))
                }
            }
        } else {
            Log.d(TAG, "Current data: null")
        }
    }

    fun locationProperty(): EventListener<DocumentSnapshot> = EventListener { snapshot, e ->

        if (e != null) {
            Log.w(TAG, "listen for location failed.", e)
            return@EventListener
        }

        if (snapshot!= null && snapshot.exists()) {
            Log.d(TAG, "Current data:" + snapshot.data)
            if (snapshot.data?.get("location") != null) {
                var newLocation = snapshot.data?.get("location") as Boolean
                locationSetting = newLocation

                getLocationPermission()
                updateLocationUI()
                getDeviceLocation()
            }
        }
    }

    private var mLocationPermissionGranted: Boolean = false

    private fun getLocationPermission() {
        if (locationSetting) {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
                Log.w(TAG, "locationPermissionGranted: ${mLocationPermissionGranted}")
            } else {
                var perms = ArrayList<String>()
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                var array = arrayOfNulls<String>(perms.count())
                ActivityCompat.requestPermissions(this, perms.toArray(array), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mLocationPermissionGranted = false
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.count() > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true
            }
        }
        updateLocationUI()
    }

    private var mLastKnownLocation: Location? = null

    fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            getLocationPermission()
            if (mLocationPermissionGranted && locationSetting) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException ) {
            Log.e(TAG, "Exception:", e)
        }
    }

    fun getDeviceLocation() {
        try {
            if (mLocationPermissionGranted && locationSetting) {
                val locationResult = mFusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener { task->
                    if (task.isSuccessful) {
                        mLastKnownLocation = task.getResult()
                        mMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(LatLng(mLastKnownLocation!!.latitude,
                                mLastKnownLocation!!.longitude),
                                zoomProperty))
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults")
                        Log.e(TAG, "Exception", task.exception)
                        mMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(mDefaultLocation, zoomProperty))
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "exception:", e)
        }
    }
}
