package com.nathan.geoword

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.storage.FirebaseStorage
import com.nathan.geoword.Static.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.nathan.geoword.db.FirebaseDBForSqlite
import com.nathan.geoword.db.Note
import com.nathan.geoword.db.User
import java.util.*

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
        mLastShownInfoWindowMarker = p0!!
        p0!!.showInfoWindow()

        return false
    }

    private fun onInfoWindowClick(): GoogleMap.OnInfoWindowClickListener = GoogleMap.OnInfoWindowClickListener { p0 ->

        loadNote(p0)
    }

    /*old marker code
    **/
    private fun loadNote(p0: Marker?) {


        var point = GeoPoint(p0!!.position.latitude, p0!!.position.longitude)
        // search sqlitedb for friends
        if (getBoolean(this, LOW_BANDWIDTH)) {
            localUser = localDB.getUser(auth.currentUser!!.uid)
            // hold friends_id in map
            val friends = localDB.getFriends(auth.currentUser!!.uid)
            if (friends.count() > 0) {

                // search sqlitedb in notes for friend at lat lng combination
                for (friend in friends) {
                    val friendMarkers = localDB.getMarkerForUser(point,friend.friend_id)
                    //if found
                    if (friendMarkers.count() > 0) {
                        retrieveMarkerLocalDB(friendMarkers[0])
                    }
                }
                // search sqlitedb in notes for user at lat lng combination
                val userMarkers = localDB.getMarkerForUser(point, auth.currentUser!!.uid)
                //if found
                if (userMarkers.count() > 0) {
                    retrieveMarkerLocalDB(userMarkers[0])
                }
            }

        }


        // search Firebase DB for friends list in public db.
        if (!getBoolean(this, LOW_BANDWIDTH)) {
            db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot ->
                // hold friends_id in map
                var map: HashMap<String, Boolean>? = null
                if (documentSnapshot != null) {
                    map = documentSnapshot.get("friends") as HashMap<String, Boolean>
                }
                for ((friend, key) in map!!) {

                    //search Firebase DB for note of friend with lat lng combination
                    db.collection("notes")
                        .whereEqualTo("user", friend)
                        .whereEqualTo("latlng", point)
                        .get()
                        //if found
                        .addOnSuccessListener(retrieveMarkerSuccessListener())
                        .addOnFailureListener(retrieveMarkerFailureListener())
                }
                // search firebase DB for note of user with lat lng combination
                db.collection("notes")
                    .whereEqualTo("user", auth.currentUser!!.uid)
                    .whereEqualTo("latlng", point)
                    .get()
                    //if found
                    .addOnSuccessListener(retrieveMarkerSuccessListener())
                    .addOnFailureListener(retrieveMarkerFailureListener())

            }
        }
    }
    var mLastShownInfoWindowMarker: Marker? = null





    override fun onMapClick(p0: LatLng?) {
        if (settingNote) {
            createAlertForCreatingNote(p0)

        } else {
            if (mLastShownInfoWindowMarker != null && mLastShownInfoWindowMarker!!.isInfoWindowShown) {
                mLastShownInfoWindowMarker?.hideInfoWindow()
                mLastShownInfoWindowMarker = null
            }

        }

    }

    override fun onBackPressed() {
        if (mLastShownInfoWindowMarker != null && mLastShownInfoWindowMarker!!.isInfoWindowShown) {
            mLastShownInfoWindowMarker!!.hideInfoWindow()
            mLastShownInfoWindowMarker = null
        } else {
            super.onBackPressed()
        }
    }

    fun addMarkerSuccessListener() : OnSuccessListener<DocumentReference> = OnSuccessListener { documentReference->
        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)


    }

    fun addMarkerFailureListener() : OnFailureListener = OnFailureListener {e->
        Log.w(TAG, "Error adding document", e)

    }
    var public: HashMap<String, ArrayList<String>> = HashMap()
    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var titleText : TextView
    private lateinit var subTitleText: TextView
    private lateinit var avatar: ImageView
    private lateinit var navView: NavigationView
    private lateinit var flAddNote: FrameLayout
    private var localUser: User? = null
    private val TAG = this.javaClass.simpleName
    private var markers = ArrayList<MarkerData>()
    private var zoomProperty: Float = -1f
    private var locationSetting: Boolean = false
    private var settingNote: Boolean = false
    private lateinit var storage: FirebaseStorage
    private lateinit var localDB: FirebaseDBForSqlite

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    // ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setTitle(R.string.title_activity_maps)
        navView = findViewById(R.id.nav_view)
        flAddNote = findViewById(R.id.flAddNote)
        flAddNote.setOnClickListener { view->

            Toast.makeText(this@MapsActivity, "Click on map to create note", Toast.LENGTH_SHORT).show()
            settingNote = true

        }
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
            localDB = FirebaseDBForSqlite(this)
            storage = FirebaseStorage.getInstance()
            mHandler.post(runnable)
            // search sqlite for user data
            if (getBoolean(this, LOW_BANDWIDTH)) {
               localUser = localDB.getUser(auth.currentUser!!.uid)
                if (localUser != null) {
                    // store name of user in menu bar
                    titleText.text = localUser!!.name

                    // store email of user in menu bar
                    subTitleText.text = localUser!!.email
                    //if avatar is not empty
                    if (localUser!!.avatar != "") {
                        // store avatar of user in menu bar
                        displayAvatar(localUser!!.avatar)
                    }

                }


            }
            // search firebase db for users public folder
            if (!getBoolean(this, LOW_BANDWIDTH)) {
                db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { document ->

                    if (document != null) {
                        // store name of user in menu bar
                        titleText?.text = document.getString("name")
                        // store email of user in menu bar
                        subTitleText?.text = document.getString("email")
                        // if user has an avatar
                        if (document.get("avatar") != null) {
                            // store avatar in menu bar
                            displayAvatar(document.getString("avatar"))
                        }
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
    // store avatar in menu bar
    // pre-condition: empty string means not loading image into menu bar
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
            // copy information from markers array into infowindow.
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
                    title.setText(marker.author)
                    subtitle.setText(marker.title)
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
        // for tracking location of user
        updateLocationUI()

        getDeviceLocation()


        // prepare map for new data
        markers.clear()
        public.clear()
        if (getBoolean(this, LOW_BANDWIDTH)) {
            localUser = localDB.getUser(auth.currentUser!!.uid)
            // search sqlite db for zoom
            localZoomProperty()
            // search sqlite db for location
            localLocationProperty()
        }


        // search firebase db for zoom property
        if (!getBoolean(this, LOW_BANDWIDTH)) {
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .addSnapshotListener(zoomProperty())
            // search firebase db for location property
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .addSnapshotListener(locationProperty())
        }


        // get your markers.
        if (getBoolean(this, LOW_BANDWIDTH)) {
            // put friends list into map
            val map = localDB.getFriends(auth.currentUser!!.uid)
            localUser = localDB.getUser(auth.currentUser!!.uid)

            if (map.count() > 0 && localUser != null) {
                var avatar = ""
                var name = ""
                // if avatar property
                if (localUser!!.avatar != "") {
                    avatar = localUser!!.avatar
                }
                // if name property
                if (localUser!!.name != "") {
                    name = localUser!!.name

                }
                // place information of public into public array
                val item = ArrayList<String>()
                item.add(avatar)
                item.add(name)
                public.put(auth.currentUser!!.uid, item)

                // search sqlite for notes of authenticated user
                val notes = localDB.getNotes(auth.currentUser!!.uid)
                // if success
                if (notes.count() > 0) {

                }
                // search sqlite for notes of friends
                for (friend in map) {
                    val friendsNotes = localDB.getNotes(friend.friend_id)
                    // if success
                    if (friendsNotes.count() > 0) {
                        var friendAvatar = ""
                        var friendName = ""
                        // if avatar
                        if (friend.avatar != "") {
                            friendAvatar = friend.avatar
                        }
                        // if name
                        if (friend.name != "") {
                            friendName = friend.name
                        }
                        // put public information in public array
                        val item = ArrayList<String>()
                        item.add(avatar)
                        item.add(name)
                        public.put(auth.currentUser!!.uid, item)
                        // put marker on map
                        retrieveMarkersLocalDB(friendsNotes)

                    }
                }

            }




        }
        // search firebase db for friends list of authenticated user
        db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot->
            // put friends list into map
            var map: HashMap<String, Boolean>? = null

            if (documentSnapshot != null) {
                map = documentSnapshot.get("friends") as HashMap<String, Boolean>
                var avatar = ""
                var name = ""
                // if avatar property
                if (documentSnapshot.getString("avatar")!= null) {
                    avatar = documentSnapshot.getString("avatar")!!
                }
                // if name property
                if (documentSnapshot.getString("name")!= null) {

                    name = documentSnapshot.getString("name")!!
                }
                // place information of public into public array
                    val item = ArrayList<String>()
                    item.add(avatar)
                    item.add(name)
                    public.put(auth.currentUser!!.uid, item)

            }
            // search firebase db for notes of current user sorted by creation date
            if (!getBoolean(this, LOW_BANDWIDTH)) {
                db.collection("notes")
                    .whereEqualTo("user", auth.currentUser!!.uid)
                    .orderBy("cr_date", Query.Direction.ASCENDING)
                    .get()
                    // if success
                    .addOnSuccessListener(retrieveMarkersSuccessListener())
                    .addOnFailureListener(retrieveMarkersFailureListener())
                for ((friend, key) in map!!) {
                    // search firebase db for public friends information
                    db.collection("public").document(friend).get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot != null) {
                            var avatar = ""
                            var name = ""
                            // if avatar
                            if (documentSnapshot.getString("avatar") != null) {
                                avatar = documentSnapshot.getString("avatar")!!
                            }
                            // if name
                            if (documentSnapshot.getString("name") != null) {

                                name = documentSnapshot.getString("name")!!
                            }
                            // place public information of friends into public array
                            val item = ArrayList<String>()
                            item.add(avatar)
                            item.add(name)
                            public.put(friend, item)
                            // search firebase db for friends of markers
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


    }

    fun retrieveMarkersLocalDB(friendsNotes: ArrayList<Note>) {
        for (note in friendsNotes) {
            var avatar = ""
            var author = ""
            val latlng = LatLng(note.geopoint.latitude, note.geopoint.longitude)
            for ((key, item) in public) {
                if (key == note.user_id) {
                    avatar = item[0]
                    author = item[1]
                }
            }
            val marker = mMap.addMarker(MarkerOptions().position(latlng)
                .title(note.title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.person)))
            markers.add(MarkerData(note.title, author,note.created_at, avatar , latlng))

        }
    }

    fun retrieveMarkersSuccessListener(): OnSuccessListener<QuerySnapshot> = OnSuccessListener { result ->


        for (document in result) {
            //Log.d(TAG, document.id + " =>" + document.data)
            val point = document.getGeoPoint("latlng")
            val title = document.getString("title")
            val latlng = LatLng(point!!.latitude, point!!.longitude)
            var avatar = ""
            var author = ""
            val cr_date = document.getTimestamp("cr_date")


            if (document.getString("user") != null) {


                    val thisKey = document.getString("user")!!
                    for ((key, item) in public) {
                        if (key == thisKey) {
                            avatar = item[0]
                            author = item[1]
                        }
                    }

            }
            val marker = mMap.addMarker(MarkerOptions().position(latlng)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.person)))
            markers.add(MarkerData(title!!, author,cr_date!!.toDate(), avatar , latlng))


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


    fun retrieveMarkerLocalDB(marker: Note) {
        val intent = Intent(this, StoryActivity::class.java)
        Log.d(TAG, marker.note_id + " =>" + marker)

        if (marker.user_id == auth.currentUser!!.uid) {
            intent.putExtra(ARG_EDITABLE, true)
        }
        /*val title = document.getString("title")
        val person = document.getString("person")
        val desc = document.getString("description")*/
        //val latlng = LatLng(point!!.latitude, point!!.longitude)
        //intent.putExtra(ARG_TITLE, title)
        //intent.putExtra(ARG_PERSON, person)
        //intent.putExtra(ARG_DESC, desc)
        intent.putExtra(ARG_LAT,marker.geopoint.latitude)
        intent.putExtra(ARG_LNG, marker.geopoint.longitude)
        intent.putExtra(ARG_DOCREF, marker.note_id)
        Log.d(TAG, marker.note_id)
        startActivity(intent)
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

    fun localZoomProperty() {
        if (localUser != null) {
            if (markers.count() > 0) {
                val lastRecord = markers[markers.count() - 1].latlng
                mDefaultLocation = lastRecord
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastRecord, localUser!!.defaultZoom))
            }
        }
    }


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
    fun localLocationProperty() {
        if (localUser != null) {
            locationSetting = localUser!!.location
            getLocationPermission()
            updateLocationUI()
            getDeviceLocation()
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

    private fun createAlertForCreatingNote(p0: LatLng?) {
        val items = arrayOf<CharSequence>(getString(R.string.create_note), getString(R.string.cancel))
        val builder = AlertDialog.Builder(this@MapsActivity)
        builder.setTitle("Are you sure you want to create note?")
        builder.setItems(items) { dialog, item ->
            if (items[item] == getString(R.string.create_note)) {
                createNote(p0)
                settingNote = false
            } else if (items[item] == getString(R.string.cancel)){
                settingNote = false
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun createNote(p0: LatLng?) {
        val intent = Intent(this, StoryActivity::class.java)
        intent.putExtra(ARG_PARAM1, 1)
        intent.putExtra(ARG_LAT, p0!!.latitude)
        intent.putExtra(ARG_LNG, p0.longitude)
        startActivity(intent)
    }

    val mHandler = Handler()
    val runnable: Runnable = Runnable {
        // DO SOMETHING
        val job = UpdateFromFirebase(this,handlerUpdateFirebase )
        job.execute()
        doJob()

    }

    val handlerUpdateFirebase = Handler(Handler.Callback {message->
        if (message.what == RESULT_COMPLETED) {
            when (message.arg1) {
                STATUS_USER ->{
            }
                STATUS_FRIEND ->{}


                STATUS_NOTE ->{}

                STATUS_IMAGE ->{}
            }

        } else if (message.what == IN_PROGRESS) {

        } else if (message.what == RESULT_ERROR) {

        } else if (message.what == RESULT_EMPTY_USER) {

        }
            false

    })



    fun doJob() {
        mHandler.postDelayed(runnable, INTERVAL)
    }


    companion object {

        const val PREF_FILE = "prefs"
        const val LOW_BANDWIDTH = "low_bandwidth"
        const val INTERVAL: Long = 1000 * 60 * 5 // 5 Minutes
        fun getBoolean(context: Context, key: String) : Boolean{
            val sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE )
            return sharedPreferences.getBoolean(key, false)

        }
        const val RESULT_ERROR = -1
        const val IN_PROGRESS = 1
        const val RESULT_COMPLETED = 0
        const val RESULT_EMPTY_USER = 2
        const val RESULT_EMPTY_FRIEND = 3


        const val STATUS_USER = 0
        const val STATUS_NOTE = 1
        const val STATUS_IMAGE = 2
        const val STATUS_FRIEND = 3
    }




}
