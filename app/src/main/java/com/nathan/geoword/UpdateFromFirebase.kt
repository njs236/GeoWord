package com.nathan.geoword

import android.app.Activity
import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nathan.geoword.db.*
import java.util.*
import kotlin.collections.HashMap


//attempt at making a class that will handle multiple returns at different times to work with getting local DB updated on MapsActivity
class UpdateFromFirebase(context: Activity, handler: Handler){
    val caller: Activity
    val handler: Handler
    val localDB: FirebaseDBForSqlite
    val db: FirebaseFirestore
    val auth: FirebaseAuth

    companion object {
        const val RESULT_ERROR = -1
        const val IN_PROGRESS = 1
        const val RESULT_COMPLETED = 0
        const val RESULT_EMPTY_USER = 2
        const val RESULT_EMPTY_FRIEND = 3

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

    }
    //TODO: progress dialog for update from firebase
    fun onProgressUpdate(vararg values: Void?) {

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
                    for (document in querySnapshot) {
                        val created_at = document.getTimestamp("created_at")
                        val description = document.getString("description")
                        val title = document.getString("title")
                        val person = document.getString("person")
                        val point = document.getGeoPoint("latlng")
                        val note_id = document.id
                        val user_id = document.getString("user")
                        val entry = Note(created_at!!.toDate(), description!!, point!!.latitude, point!!.longitude, person!!, title!!, user_id!!, note_id)

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
                                    val created_at = document.getTimestamp("created_at")!!
                                    val image_id = document.id

                                    val entry: Image = Image(created_at.toDate(), stored_note_id, name, image_id)

                                    if (localDB.getImage(image_id) == null) {
                                        localDB.addImage(entry)
                                    } else {
                                        localDB.updateImage(entry)
                                    }
                                }
                                //TODO: return, finished getting images of note.
                                val msg = Message()
                                msg.what = STATUS_IMAGE
                                msg.arg1 = RESULT_COMPLETED
                                handler.dispatchMessage(msg)


                            }.addOnFailureListener { e->
                                Log.w(TAG, "Error in retrieving image data", e)
                            val msg = Message()
                            msg.what = STATUS_IMAGE
                            msg.arg1 = RESULT_ERROR
                            handler.dispatchMessage(msg)}
                    }

                    //TODO: return, finished getting users Note data.
                    val msg = Message()
                    msg.what = STATUS_NOTE
                    msg.arg1 = RESULT_COMPLETED
                    handler.dispatchMessage(msg)
                }
            var count = 0
            for ((friend, value) in map!!) {
                db.collection("notes")
                    .whereEqualTo("user",friend).get().addOnSuccessListener { querySnapshot ->
                        count++
                        for(document in querySnapshot){

                            val created_at = document.getTimestamp("created_at")
                            val description = document.getString("description")
                            val title = document.getString("title")
                            val person = document.getString("person")
                            val point = document.getGeoPoint("latlng")
                            val note_id = document.id
                            val user_id = document.getString("user")
                            val entry = Note(created_at!!.toDate(), description!!, point!!.latitude, point!!.longitude, person!!, title!!, user_id!!, note_id)
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
                                        val created_at = document.getTimestamp("created_at")!!
                                        val image_id = document.id

                                        val entry = Image(created_at.toDate(), stored_note_id, name, image_id)

                                        if (localDB.getImage(image_id) == null) {
                                            localDB.addImage(entry)
                                        } else {
                                            localDB.updateImage(entry)
                                        }
                                    }
                                    //TODO: return, finished getting image of note
                                    val msg = Message()
                                    msg.what = STATUS_IMAGE
                                    msg.arg1 = RESULT_COMPLETED

                                    handler.dispatchMessage(msg)
                                }
                        }

                        //TODO: return, finished getting friends note data.
                        if (count < map!!.count() -1) {
                            val msg= Message()
                            msg.what = STATUS_NOTE
                            msg.arg1 = IN_PROGRESS
                        } else {
                            val msg = Message()
                            msg.what = STATUS_NOTE
                            msg.arg1 = RESULT_COMPLETED
                            handler.dispatchMessage(msg)
                        }
                    }.addOnFailureListener {e->
                        val msg = Message()
                        msg.what = STATUS_FRIEND
                        msg.arg1 = RESULT_ERROR
                        count++
                    handler.dispatchMessage(msg)
                        Log.w(TAG, "error in retrieving friends notes data", e)
                    }
            }
        }
    }

    fun updateFriendData() {
        if (map != null) {
            var count = 0
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
                            created_at = documentSnapshot.getTimestamp("created_at")!!.toDate()



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
                            val msg = Message()
                            msg.what = STATUS_FRIEND
                            msg.arg1 = IN_PROGRESS
                            handler.dispatchMessage(msg)
                        } else {
                            val msg = Message()
                            msg.what = STATUS_FRIEND
                            msg.arg1 = RESULT_COMPLETED
                            friendfinished = true
                            handler.dispatchMessage(msg)
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
                    created_at = documentSnapshot.getTimestamp("created_at")!!.toDate()
                    if (documentSnapshot.get("friends") != null) {
                        map = documentSnapshot.get("friends") as HashMap<String, Boolean>
                        // map size, how many friends have to be cycled through.
                        updateFriendData()
                        updateNoteData()
                    }


                } else {
                    // didn't fetch any data of user
                    val msg = Message()
                    msg.what = STATUS_USER
                    msg.arg1 = RESULT_COMPLETED
                    handler.dispatchMessage(msg)
                }
                entry.created_at = created_at
                entry.user_id = user_id
                entry.name = name
                entry.email = email
                entry.avatar = avatar

                db.collection("users")
                    .document(auth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot->
                        var location = false
                        var newZoom = -1f
                        if (documentSnapshot!= null) {
                            if (documentSnapshot.getBoolean("location") != null) {
                                location = documentSnapshot.getBoolean("location")!!
                            }
                            if (documentSnapshot.data?.get("defaultZoom")!= null) {
                                newZoom = documentSnapshot.data?.get("defaultZoom") as Float
                            }

                        }

                        entry.location = location
                        entry.defaultZoom = newZoom
                        if (localDB.getUser(auth.currentUser!!.uid) == null) {
                            localDB.addUser(entry)
                        } else {
                            localDB.updateUser(entry)
                        }

                        //TODO: return, finished getting user data.
                        val msg = Message()
                        msg.what = STATUS_USER
                        msg.arg1 = RESULT_COMPLETED
                        userfinished = true
                        handler.dispatchMessage(msg)


                    }
            }
    }
}