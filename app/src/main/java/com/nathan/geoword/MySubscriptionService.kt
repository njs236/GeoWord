package com.nathan.geoword

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MySubscriptionService : Service() {

    private val binder = LocalBinder()
    private lateinit var auth: FirebaseAuth
    private lateinit  var db: FirebaseFirestore
    private val TAG: String = this.javaClass.simpleName

    inner class LocalBinder: Binder() {
        fun getService(): MySubscriptionService = this@MySubscriptionService
    }

    override fun onBind(intent: Intent): IBinder {
        //TODO("Return the communication channel to the service.")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startCheckingFirebase()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            updateUser(auth.currentUser)

            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()



        } else {
            this.onDestroy()
        }
        return super.onStartCommand(intent, flags, startId)
    }
    lateinit var mHandler: Handler
    var mInterval : Long = 5000
    lateinit var mRunnable :Runnable

    fun checkDB() {

        /* TODO: need to prohibit checking again once the list is received this end.
        ie... the email addresses that are being given permission to go on friends list wait for the user to make decision before deleting from sub list.
        will need doc ids
         */
        db.collection("sub")
            .whereEqualTo("sub", auth.currentUser!!.email).get()
            .addOnSuccessListener {querySnapshot->
                val emailList = ArrayList<String>()
                val docids = ArrayList<String>()
            for (document in querySnapshot) {

                // get the emails of all the users that are asking for friends request.

                val email = document.getString("user")!!
                emailList.add(email)
                val id = document.id
                docids.add(id)
                // send broadcast to app and receive to allow friend into friend list.

            }
            // if not empty
                /*
                if (emailList.count() > 0) {


                    // delete documents after the broadcast is sent
                    for (doc in docids) {
                        db.collection("sub")
                            .document(doc)
                            .delete()
                            .addOnSuccessListener { Log.w(TAG, "successfully deleted sub") }
                            .addOnFailureListener { ex -> Log.w(TAG, "error in deleting sub", ex) }
                    }
                }
                */

        }.addOnFailureListener {ex->
            Log.w(TAG, "error in getting sub", ex)

        }
    }

    fun startCheckingFirebase() {
        mHandler = Handler()
        mRunnable = Runnable { try {
            checkDB()
        } finally {
            mHandler.postDelayed(mRunnable, mInterval)
        }
        }
        mRunnable.run()

    }

    fun stopCheckingFirebase() {
        mHandler.removeCallbacks(mRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckingFirebase()
    }



    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)

    }
}
