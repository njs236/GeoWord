package com.nathan.geoword

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile.*

private const val ARG_LOGIN = "login"

class ProfileActivity : AppCompatActivity(),
    ProfileHomeFragment.OnHomeInteractionListener,
    AddGroupFragment.OnGroupInteractionListener,
    AddFriendFragment.OnFriendInteractionListener,
SettingsFragment.OnFragmentInteractionListener{
    override fun onSettingsInteraction(map: HashMap<String, Any>) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        if (map.containsKey("submitAndGoHome")) {
            goHome()
        }
    }

    override fun onFriendInteraction(uri: Uri) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onGroupInteraction(uri: Uri) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onHomeInteraction(uri: Uri) {
        //TODO:("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun goHome() {
        var frag = ProfileHomeFragment()
        var fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_layout, frag)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
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

    private lateinit var storage: FirebaseStorage

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


        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }
    }

    fun displayAvatar(imageName: String?) {
        if (imageName != null) {
            Log.w(TAG, "imageName: $imageName")


            val imageRef = storage.reference.child(imageName)
            GlideApp.with(this).load(imageRef).into(avatar)
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
        //titleText.text = user?.displayName
        //subTitleText.text = user?.email
    }


    fun changeView(item: MenuItem?) {
        var frag: androidx.fragment.app.Fragment? = null
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
            R.id.navigation_settings -> {
                Log.w(TAG, "choosing: SettingsFragment")
                frag = SettingsFragment()
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
