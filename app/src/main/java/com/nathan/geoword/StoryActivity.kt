package com.nathan.geoword

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_LAT = "latitude"
private const val ARG_LNG = "longitude"
private const val ARG_TITLE = "title"
private const val ARG_PERSON = "person"
private const val ARG_DESC = "description"
private const val ARG_DOCREF = "document_id"

private var arguments: Bundle? = null

class StoryActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private var state: ActivityState? = null
    private var longitude: Double? = null
    private var latitude: Double? = null
    private var storedTitle : String = ""
    private var storedPerson : String = ""
    private var storedDescription : String = ""
    private var docref :String = ""

    private var titleEditable: EditText? = null
    private var nameEditable: EditText? = null
    private var descEditable: EditText? = null
    private var toolbar: Toolbar? = null
    private var title: TextView? = null
    private var name: TextView? = null
    private var desc: TextView? = null
    private var fab: FloatingActionButton? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    enum class ActivityState (val number: Int){
        new(1),
        display(0),
        edit(2);
    }

    fun findStateFromNumber(number: Int): ActivityState {
        for (state in ActivityState.values()) {
            if (number == state.number) {
                return state
            }
        }
        return ActivityState.display
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // determine what layout to show depending on edit or new



        val param1 = intent.getIntExtra(ARG_PARAM1, 0)
        state = findStateFromNumber(param1)
        latitude = intent.getDoubleExtra(ARG_LAT, -1.0)
        longitude = intent.getDoubleExtra(ARG_LNG, -1.0)
        storedTitle = intent.extras.getString(ARG_TITLE, "")
        storedPerson = intent.extras.getString(ARG_PERSON, "")
        storedDescription = intent.extras.getString(ARG_DESC, "")
        docref = intent.extras.getString(ARG_DOCREF, "")

        if (state == ActivityState.new) {
            setContentView(R.layout.activity_story_new)
            titleEditable = findViewById(R.id.editTextTitle)
            nameEditable = findViewById(R.id.editTextName)
            descEditable = findViewById(R.id.editTextDescription)
            titleEditable?.setText(storedTitle)
            nameEditable?.setText(storedPerson)
            descEditable?.setText(storedDescription)
            fab = findViewById(R.id.fab)
            fab?.setOnClickListener(fabNewClickListener())
        } else if (state == ActivityState.edit) {
            setContentView(R.layout.activity_story_new)
            titleEditable = findViewById(R.id.editTextTitle)
            nameEditable = findViewById(R.id.editTextName)
            descEditable = findViewById(R.id.editTextDescription)
            titleEditable?.setText(storedTitle)
            nameEditable?.setText(storedPerson)
            descEditable?.setText(storedDescription)
            fab = findViewById(R.id.fab)
            fab?.setImageDrawable(getDrawable(R.drawable.ic_edit_black_24dp))
            fab?.setOnClickListener(fabEditClickListener())

        } else {
            setContentView(R.layout.activity_story)
            title = findViewById(R.id.textViewTitle)
            name = findViewById(R.id.textViewName)
            desc = findViewById(R.id.textViewDescription)

            title?.text = storedTitle
            name?.text = storedPerson
            desc?.text = storedDescription
            fab = findViewById(R.id.fab)
            fab?.setOnClickListener(fabDisplayClickListener())
        }

        toolbar = findViewById(R.id.story_toolbar)
        setSupportActionBar(toolbar)
        toolbar?.setNavigationIcon(R.mipmap.ic_launcher)
        toolbar?.setNavigationOnClickListener { click->

            startActivity(Intent(this, MapsActivity::class.java))
        }





        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {

            updateUser(auth.currentUser)
            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()



        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }


    fun fabNewClickListener() : View.OnClickListener = View.OnClickListener { click ->
        // prereqs: check input for valid.
        // send the data to the database and return to mapactivity
        val note = HashMap<String, Any>()
        if (titleEditable?.text.toString() == "") {
            Toast.makeText(this, "please enter a title", Toast.LENGTH_LONG).show()
        } else if (descEditable?.text.toString() == "") {
            Toast.makeText(this, "please enter a story", Toast.LENGTH_LONG).show()
        } else {
            note["cr_date"] = Timestamp.now()
            note["title"] = titleEditable?.text.toString()
            note["person"] = nameEditable?.text.toString()
            note["description"] = descEditable?.text.toString()
            note["latlng"] = GeoPoint(latitude!!, longitude!!)
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .collection("notes")
                .add(note)
                .addOnSuccessListener(addNoteSuccessListener())
                .addOnFailureListener(addNoteFailureListener())

            startActivity(Intent(this, MapsActivity::class.java))
        }

    }
    fun fabEditClickListener() : View.OnClickListener = View.OnClickListener { click ->
        // prereqs: check input for valid.
        // send the data to the database and return to mapactivity
        val note = HashMap<String, Any>()
        if (titleEditable?.text.toString() == "") {
            Toast.makeText(this, "please enter a title", Toast.LENGTH_LONG).show()
        } else if (descEditable?.text.toString() == "") {
            Toast.makeText(this, "please enter a story", Toast.LENGTH_LONG).show()
        } else {
            if (docref != "") {
                val point = GeoPoint(latitude!!, longitude!!)
                note["cr_date"] = Timestamp.now()
                note["title"] = titleEditable?.text.toString()
                note["person"] = nameEditable?.text.toString()
                note["description"] = descEditable?.text.toString()
                note["latlng"] = point
                db.collection("users")
                    .document(auth.currentUser!!.uid)
                    .collection("notes")
                    .document(docref)

                    .update(
                        "title",
                        note["title"],
                        "person",
                        note["person"],
                        "description",
                        note["description"],
                        "latlng",
                        note["latlng"]
                    )
                    .addOnSuccessListener {Log.d(TAG, "DocumentSnapshot updated")}
                    .addOnFailureListener(editNoteFailureListener())

                startActivity(Intent(this, MapsActivity::class.java))
            }
        }

    }

    fun fabDisplayClickListener(): View.OnClickListener = View.OnClickListener { click->

        val intent = Intent(this, StoryActivity::class.java)
        intent.putExtra(ARG_PARAM1, ActivityState.edit.number)
        intent.putExtra(ARG_LAT, latitude)
        intent.putExtra(ARG_LNG, longitude)
        intent.putExtra(ARG_TITLE, storedTitle)
        intent.putExtra(ARG_PERSON, storedPerson)
        intent.putExtra(ARG_DESC, storedDescription)
        intent.putExtra(ARG_DOCREF, docref)
        startActivity(intent)
    }

    fun addNoteSuccessListener(): OnSuccessListener<DocumentReference> = OnSuccessListener { documentReference->
        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)
    }

    fun addNoteFailureListener(): OnFailureListener = OnFailureListener { e->
        Log.w(TAG, "Error adding document", e)
    }


    fun editNoteFailureListener(): OnFailureListener = OnFailureListener { e->
        Log.w(TAG, "Error adding document", e)
    }

    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)
    }


}
