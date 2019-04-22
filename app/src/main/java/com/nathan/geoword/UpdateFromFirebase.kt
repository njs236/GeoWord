package com.nathan.geoword

import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.nathan.geoword.db.*
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.HashMap


//attempt at making a class that will handle multiple returns at different times to work with getting local DB updated on MapsActivity
class UpdateFromFirebase(context: Activity, handler: Handler, test: Boolean){
    val caller: Activity
    val handler: Handler
    val localDB: FirebaseDBForSqlite
    var shouldTest :Boolean = false
    var resultStatus: Int = 5
    var notesCount: Int = 0
    var hasNotes: Boolean = false
    var imagesCount: Int = 0
    var hasImages: Boolean = false
    var friendsCount: Int = 0
    var hasFriends :Boolean = false
    var userName: String =""
    var hasUser: Boolean = false
    lateinit var latch: CountDownLatch
    val db: FirebaseFirestore

    val auth: FirebaseAuth

    companion object {
        const val RESULT_ERROR = -1
        const val IN_PROGRESS = 1
        const val RESULT_COMPLETED = 0
        const val RESULT_EMPTY_USER = 2
        const val RESULT_EMPTY_FRIEND = 3
        const val RESULT_FINISHED = 4

        const val STATUS_USER = 0
        const val STATUS_NOTE = 1
        const val STATUS_IMAGE = 2
        const val STATUS_FRIEND = 3
        var userfinished= false
        var notefinished = false
        var friendfinished = false
    }
    private var map: HashMap<String, Boolean>? = null

    init {
        this.caller = context
        this.handler = handler
        this.localDB = FirebaseDBForSqlite(this.caller)
        this.auth = FirebaseAuth.getInstance()
        this.db = FirebaseFirestore.getInstance()
        this.shouldTest = if (test) true else false


    }
    fun setUpTest(latch: CountDownLatch) {
        this.latch = latch
    }
    //TODO: progress dialog for update from firebase
    fun onProgressUpdate(vararg values: Int?) {

        val msg = Message()
       /* Log.w(TAG, "userfinished: ${userfinished}")
        Log.w(TAG, "notefinished: ${notefinished}")
        Log.w(TAG, "friendfinished: ${friendfinished}")
        Log.w(TAG, "area: ${values[0]}")
        Log.w(TAG, "result: ${values[1]}")*/

        if (userfinished && notefinished && friendfinished) {
            msg.arg2 = RESULT_FINISHED
            resultStatus = RESULT_FINISHED

            if (shouldTest) {
                latch.countDown()
            }
        } else {
            msg.what = values[0]!!
            msg.arg1 = values[1]!!
        }
        handler.dispatchMessage(msg)
        //do something with: (*values)
    }

    //TODO: return handler dealing with how the update from firebase went
    fun onPostExecute(result: Int?) {
        // 0 if success.
        // do something with (result)
    }

    fun doInBackground(vararg params: Void?) {
       // TODO: Get user data
        updateUserData()
        // TODO: Get Note Data
        // TODO: Get Friend Data
        //return 0
    }

    fun execute(vararg params: Void?) {
        doInBackground(*params)
    }



    private val TAG: String = this.javaClass.simpleName

    fun updateNoteData() {
        if (map != null) {
            db.collection("notes")
                .whereEqualTo("user", auth.currentUser!!.uid).get().addOnSuccessListener {querySnapshot ->
                    notesCount += querySnapshot.count()
                    if (querySnapshot.count() > 0) {
                        hasNotes = true
                    }
                    for (document in querySnapshot) {
                        var created_at = Date()
                        var  description = ""
                        var title = ""
                        var person = ""
                        var point = GeoPoint(0.toDouble(),0.toDouble())
                        var user_id = ""
                        if (document.getTimestamp("created_at") != null) {
                            created_at = document.getTimestamp("created_at")!!.toDate()
                        }
                        if (document.getString("description")!= null) {
                            description = document.getString("description")!!
                        }
                        if (document.getString("title") != null) {
                            title = document.getString("title")!!
                        }
                        if (document.getString("person") != null) {
                            person = document.getString("person")!!
                        }
                        if (document.getGeoPoint("latlng") != null) {
                            point = document.getGeoPoint("latlng")!!
                        }
                        if (document.getString("user") != null) {
                            user_id = document.getString("user")!!
                        }

                        val note_id = document.id
                        val entry = Note(created_at, description, point.latitude, point.longitude, person, title, user_id, note_id)

                        if (localDB.getNote(note_id) == null) {
                            localDB.addNote(entry)
                        } else {
                            localDB.updateNote(entry)
                        }

                        // get images for note
                        db.collection("notes")
                            .document(document.id).collection("images").get().addOnSuccessListener {querySnapshot ->
                                for (document in querySnapshot) {
                                    val stored_note_id = document.getString("note_id")!!
                                    val name = document.getString("name")!!
                                    var created_at = Date()
                                    if (document.getTimestamp("created_at")!= null) {
                                        created_at = document.getTimestamp("created_at")!!.toDate()
                                    }
                                    val image_id = document.id

                                    val entry: Image = Image(created_at, stored_note_id, name, image_id)

                                    if (localDB.getImage(image_id) == null) {
                                        localDB.addImage(entry)
                                    } else {
                                        localDB.updateImage(entry)
                                    }
                                }
                                //TODO: return, finished getting images of note.
                                onProgressUpdate(STATUS_IMAGE, RESULT_COMPLETED)

                            }.addOnFailureListener { e->
                                Log.w(TAG, "Error in retrieving image data", e)
                            onProgressUpdate(STATUS_IMAGE, RESULT_ERROR) }
                    }

                    //TODO: return, finished getting users Note data.
                    //onProgressUpdate(STATUS_NOTE, RESULT_COMPLETED)
                }
            var count = 0
            for ((friend, value) in map!!) {
                db.collection("notes")
                    .whereEqualTo("user",friend).get().addOnSuccessListener { querySnapshot ->
                        count++
                        notesCount += querySnapshot.count()
                        if (querySnapshot.count() > 0) {
                            hasNotes = true
                        }
                        for(document in querySnapshot){

                            var created_at = Date()
                            var  description = ""
                            var title = ""
                            var person = ""
                            var point = GeoPoint(0.toDouble(),0.toDouble())
                            var user_id = ""
                            if (document.getTimestamp("created_at") != null) {
                                created_at = document.getTimestamp("created_at")!!.toDate()
                            }
                            if (document.getString("description")!= null) {
                                description = document.getString("description")!!
                            }
                            if (document.getString("title") != null) {
                                title = document.getString("title")!!
                            }
                            if (document.getString("person") != null) {
                                person = document.getString("person")!!
                            }
                            if (document.getGeoPoint("latlng") != null) {
                                point = document.getGeoPoint("latlng")!!
                            }
                            if (document.getString("user") != null) {
                                user_id = document.getString("user")!!
                            }

                            val note_id = document.id
                            val entry = Note(created_at, description, point.latitude, point.longitude, person, title, user_id, note_id)
                            if (localDB.getNote(note_id) == null) {
                                localDB.addNote(entry)
                            } else {
                                localDB.updateNote(entry)
                            }
                            // get images for note
                            db.collection("notes")
                                .document(document.id).collection("images").get().addOnSuccessListener {querySnapshot ->
                                    for (document in querySnapshot) {

                                        val stored_note_id = document.getString("note_id")!!
                                        val name = document.getString("name")!!
                                        var created_at = Date()
                                        if (document.getTimestamp("created_at") != null) {
                                            created_at = document.getTimestamp("created_at")!!.toDate()
                                        }
                                        val image_id = document.id

                                        val entry = Image(created_at, stored_note_id, name, image_id)

                                        if (localDB.getImage(image_id) == null) {
                                            localDB.addImage(entry)
                                        } else {
                                            localDB.updateImage(entry)
                                        }
                                    }
                                    //TODO: return, finished getting image of note
                                    onProgressUpdate(STATUS_IMAGE, RESULT_COMPLETED)
                                }
                        }

                        //TODO: return, finished getting friends note data.
                        if (count < map!!.count() -1) {

                        } else {
                            notefinished = true
                            onProgressUpdate(STATUS_NOTE, RESULT_COMPLETED)
                        }
                    }.addOnFailureListener {e->
                        onProgressUpdate(STATUS_NOTE, RESULT_ERROR)
                        count++
                        Log.w(TAG, "error in retrieving friends notes data", e)
                    }
            }
        }
    }

    fun updateFriendData() {
        if (map != null) {
            var count = 0
            friendsCount += map!!.count()
            if (map!!.count() > 0) {
                hasFriends = true
            }
            for ((friend, value) in map!!) {
                db.collection("public")
                    .document(friend).get().addOnSuccessListener {documentSnapshot ->
                        count++
                        val entry: Friend = Friend(Date(), "", "", "", "", "")
                        var name = ""
                        var email = ""
                        var avatar = ""
                        var friend_id = ""

                        var created_at = Date()
                        if (documentSnapshot!= null) {
                            friend_id = documentSnapshot.id

                            if (documentSnapshot.getString("name") != null) {
                                name = documentSnapshot.getString("name")!!
                            }
                            if (documentSnapshot.getString("email")!= null) {
                                email = documentSnapshot.getString("email")!!
                            }
                            if (documentSnapshot.getString("avatar")!= null) {
                                avatar = documentSnapshot.getString("avatar")!!
                            }
                            if (documentSnapshot.getTimestamp("created_at")!= null) {
                                created_at = documentSnapshot.getTimestamp("created_at")!!.toDate()
                            }



                        }
                        entry.created_at = created_at
                        entry.friend_id = friend_id
                        entry.name = name
                        entry.email = email
                        entry.avatar = avatar
                        entry.user_id = auth.currentUser!!.uid

                        if (localDB.getFriend(friend) == null) {
                            localDB.addFriend(entry)
                        } else {
                            localDB.updateFriend(entry)
                        }

                        //TODO: return, finished getting friend data.
                        if (count < map!!.count() -1) {
                        } else {
                            friendfinished = true
                            onProgressUpdate(STATUS_FRIEND, RESULT_COMPLETED)
                        }
                    }
            }
        }
    }

    fun updateUserData() {
        db.collection("public")
            .document(auth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot->
                val entry: User = User("", false, 15f, "", "", "", Date())
                var name = ""
                var email = ""
                var avatar = ""
                var user_id = ""
                var created_at = Date()
                if (documentSnapshot!= null) {
                    hasUser = true
                    user_id = documentSnapshot.id

                    if (documentSnapshot.getString("name") != null) {

                        name = documentSnapshot.getString("name")!!

                    }
                    if (documentSnapshot.getString("email")!= null) {
                        email = documentSnapshot.getString("email")!!
                    }
                    if (documentSnapshot.getString("avatar")!= null) {
                        avatar = documentSnapshot.getString("avatar")!!
                    }
                    if (documentSnapshot.getTimestamp("created_at")!= null) {
                        created_at = documentSnapshot.getTimestamp("created_at")!!.toDate()
                    }

                    if (documentSnapshot.get("friends") != null) {
                        map = documentSnapshot.get("friends") as HashMap<String, Boolean>
                        // map size, how many friends have to be cycled through.
                        updateFriendData()
                        updateNoteData()
                    }


                } else {
                    // didn't fetch any data of user
                    onProgressUpdate(STATUS_USER, RESULT_COMPLETED)
                }
                userName = name
                entry.created_at = created_at
                entry.user_id = user_id
                entry.name = name
                entry.email = email
                entry.avatar = avatar

                db.collection("users")
                    .document(auth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot->
                        var location = false
                        var newZoom : Long = -1
                        if (documentSnapshot!= null) {
                            if (documentSnapshot.getBoolean("location") != null) {
                                location = documentSnapshot.getBoolean("location")!!
                            }
                            if (documentSnapshot.data?.get("defaultZoom")!= null) {
                                newZoom = documentSnapshot.data?.get("defaultZoom") as Long
                            }

                        }

                        entry.location = location
                        entry.defaultZoom = newZoom.toFloat()
                        if (localDB.getUser(auth.currentUser!!.uid) == null) {
                            localDB.addUser(entry)
                        } else {
                            localDB.updateUser(entry)
                        }

                        //TODO: return, finished getting user data.
                        userfinished = true
                        onProgressUpdate(STATUS_USER, RESULT_COMPLETED)


                    }
            }
    }
}