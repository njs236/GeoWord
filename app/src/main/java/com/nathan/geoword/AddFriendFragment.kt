package com.nathan.geoword

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_add_friend.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_LOGIN = "login"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AddFriendFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AddFriendFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AddFriendFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFriendInteractionListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit  var db: FirebaseFirestore
    private lateinit var listViewRecommendedFriendsList: ListView
    private lateinit var editTextEmail: EditText
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_add_friend, container, false)
        listViewRecommendedFriendsList = view.findViewById(R.id.listViewRecommendedFriendsList)
        editTextEmail = view.findViewById(R.id.add_friend_etEmail)
        btnSubmit = view.findViewById(R.id.add_friend_submit)
        btnSubmit.setOnClickListener(onSubmit())
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            updateUser(auth.currentUser)

            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()



        } else {
            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }

        return view
    }

    private val TAG: String = this.javaClass.simpleName

    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)

    }

    fun onSubmit(): View.OnClickListener = View.OnClickListener { event->
        // must include valid email
        var doc = ""
        var email =""
        //search public folder
        db.collection("public").whereEqualTo("email", editTextEmail.text.toString()).get().addOnSuccessListener { querySnapshot->
            var doc = ""
            for (document in querySnapshot) {
                doc = document.id
            }
            db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot->

                // get the friends list
                var found = false
                if (documentSnapshot != null) {
                    val friends = documentSnapshot.data!!.get("friends") as HashMap<String, Boolean>
                    for ((friend, value) in friends) {
                        if (friend == doc ) {
                            found = true
                            break
                        }
                    }
                }
                if (found) {

                } else {
                    db.collection("public").whereEqualTo("email", editTextEmail.text.toString()).get().addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot) {
                            email = document.getString("email")!!
                            doc = document.id
                        }
                        if (TextUtils.isEmpty(doc)) {
                            Toast.makeText(context, "Email address not found", Toast.LENGTH_SHORT).show()
                        } else {
                            // send email to person to register and optionally get program on phone

                            // send request through firebase of user that he wants to invite into friends list
                            val sub = HashMap<String, Any>()
                            sub["user"] = auth.currentUser!!.uid
                            sub["sub"] = doc
                            db.collection("sub").add(sub).addOnSuccessListener {  }.addOnFailureListener {  }


                        }

                        editTextEmail.clearFocus()
                        editTextEmail.setText("")
                    }
                }
            }
        }


        true
    }



    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFriendInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFriendInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFriendInteractionListener {
        // TODO: Update argument type and name
        fun onFriendInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddFriendFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddFriendFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
