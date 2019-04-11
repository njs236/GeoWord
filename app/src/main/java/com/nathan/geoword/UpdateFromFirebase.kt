package com.nathan.geoword

import android.app.Activity
import android.os.AsyncTask
import android.os.Handler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nathan.geoword.db.FirebaseDBForSqlite
import com.nathan.geoword.db.FriendEntry
import com.nathan.geoword.db.NoteEntry
import com.nathan.geoword.db.UserEntry
import java.util.*
import kotlin.collections.HashMap

class UpdateFromFirebase(context: Activity, handler: Handler) : AsyncTask<Void, Void, Int>() {
    val caller: Activity
    val handler: Handler
    val localDB: FirebaseDBForSqlite
    val db: FirebaseFirestore
    val auth: FirebaseAuth
    private var map: HashMap<String, Boolean>? = null

    init {
        this.caller = context
        this.handler = handler
        this.localDB = FirebaseDBForSqlite(this.caller)
        this.auth = FirebaseAuth.getInstance()
        this.db = FirebaseFirestore.getInstance()

    }
    //TODO: progress dialog for update from firebase
    override fun onProgressUpdate(vararg values: Void?) {
        super.onProgressUpdate(*values)
    }

    //TODO: return handler dealing with how the update from firebase went
    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)
    }

    override fun doInBackground(vararg params: Void?): Int {
       // TODO: Get user data
        updateUserData()
        // TODO: Get Note Data
        // TODO: Get Friend Data
        return 0
    }


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
                        val entry = NoteEntry(created_at!!.toDate(), description!!, point!!.latitude, point!!.longitude, person!!, title!!, user_id!!, note_id)

                        if (localDB.getNote(note_id) == null) {
                            localDB.addNote(entry)
                        } else {
                            localDB.updateNote(entry)
                        }

                        // get images for note
                        db.collection("notes")
                            .document(document.id).collection("images").get().addOnSuccessListener {querySnapshot ->
                                for (document in querySnapshot) {

                                }


                            }
                    }
                }
            for ((friend, value) in map!!) {
                db.collection("notes")
                    .whereEqualTo("user",friend).get().addOnSuccessListener { querySnapshot ->
                        for(document in querySnapshot){
                            val created_at = document.getTimestamp("created_at")
                            val description = document.getString("description")
                            val title = document.getString("title")
                            val person = document.getString("person")
                            val point = document.getGeoPoint("latlng")
                            val note_id = document.id
                            val user_id = document.getString("user")
                            val entry = NoteEntry(created_at!!.toDate(), description!!, point!!.latitude, point!!.longitude, person!!, title!!, user_id!!, note_id)
                            //TODO: getnote with note_id
                            if (localDB.getNote(note_id) == null) {
                                localDB.addNote(entry)
                            } else {
                                //TODO: updateNote with entry
                                localDB.updateNote(entry)
                            }
                            // get images for note
                            db.collection("notes")
                                .document(document.id).collection("images").get().addOnSuccessListener {querySnapshot ->
                                    for (document in querySnapshot) {

                                    }
                                }
                        }
                    }
            }
        }
    }

    fun updateFriendData() {
        if (map != null) {
            for ((friend, value) in map!!) {
                db.collection("public")
                    .document(friend).get().addOnSuccessListener {documentSnapshot ->
                        val entry: FriendEntry = FriendEntry(Date(), "", "", "", "", "")
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
                    }
            }
        }
    }

    fun updateUserData() {
        db.collection("public")
            .document(auth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot->
                val entry: UserEntry = UserEntry("", false, 15f, "", "", "", Date())
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
                        updateFriendData()
                        updateNoteData()
                    }


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


                    }
            }
    }
}