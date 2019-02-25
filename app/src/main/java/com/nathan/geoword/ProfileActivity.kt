package com.nathan.geoword

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_profile.*

private const val ARG_LOGIN = "login"

class ProfileActivity : AppCompatActivity(), ProfileHomeFragment.OnHomeInteractionListener {
    override fun onHomeInteraction(uri: Uri) {
        //TODO:("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var mSelectedItem: Int = -1
    private val SELECTED_ITEM = "arg_selected_item"
    private lateinit var navigation : BottomNavigationView
    private lateinit var navView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var titleText : TextView
    private lateinit var subTitleText: TextView
    private lateinit var avatar: ImageView
    private val TAG = this.javaClass.simpleName

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

            changeView(item)

        false
    }

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        navigation = findViewById(R.id.navigation)
        navView = findViewById(R.id.nav_view)
        avatar = navView.getHeaderView(0).findViewById(R.id.nav_imageView)

        titleText = navView.getHeaderView(0).findViewById(R.id.nav_tvTitle)
        subTitleText = navView.getHeaderView(0).findViewById(R.id.nav_tvSubtitle)

        navView.menu.getItem(0).setOnMenuItemClickListener { click->
            startActivity(Intent(this,
                MapsActivity::class.java))
            true
        }

        navView.menu.getItem(1).setOnMenuItemClickListener { click->
            //logout
            signOut()

            true
        }

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val selectedItem: MenuItem = navigation.menu.getItem(0)


        changeView(selectedItem)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {

            updateUser(auth.currentUser)
            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()



        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
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
        Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)
        titleText.text = user?.displayName
        subTitleText.text = user?.email
    }


    fun changeView(item: MenuItem?) {
        var frag: Fragment? = null
        when (item!!.itemId) {
            R.id.navigation_home -> {
                Log.w(TAG, "choosing: ProfileHome")
                frag = ProfileHomeFragment()
            }
            R.id.navigation_friend -> {
                Log.w(TAG, "choosing: FriendFragment")
                frag = AddFriendFragment()
            }
            R.id.navigation_group -> {
                Log.w(TAG, "choosing: GroupFragment")
                frag = AddGroupFragment()
            }
        }


        if (frag!= null) {

            var fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_layout, frag)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }

    }
}
