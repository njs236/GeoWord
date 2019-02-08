package com.nathan.geoword

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.EditText
import android.widget.TextView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private var arguments: Bundle? = null

class StoryActivity : AppCompatActivity() {
    private var new: Boolean? = null
    private var titleEditable: EditText? = null
    private var nameEditable: EditText? = null
    private var descEditable: EditText? = null
    private var title: TextView? = null
    private var name: TextView? = null
    private var desc: TextView? = null
    private var fab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // determine what layout to show depending on edit or new
        new = intent.getBooleanExtra(ARG_PARAM1, false)
        if (new!!) {
            setContentView(R.layout.activity_story_new)
            titleEditable = findViewById(R.id.editTextTitle)
            nameEditable = findViewById(R.id.editTextName)
            descEditable = findViewById(R.id.editTextDescription)
            fab = findViewById(R.id.fab)
        } else {
            setContentView(R.layout.activity_story)
            title = findViewById(R.id.textViewTitle)
            name = findViewById(R.id.textViewName)
            desc = findViewById(R.id.textViewDescription)
        }
    }


}
