package com.nathan.geoword

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_LOGIN = "login"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AddGroupFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AddGroupFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AddGroupFragment : androidx.fragment.app.Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnGroupInteractionListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit  var db: FirebaseFirestore
    private lateinit var radio_group: RadioGroup
    private lateinit var radioPublic: RadioButton
    private lateinit var radioFriend: RadioButton
    private lateinit var group_name: EditText
    private lateinit var listViewGroupFriendsList: ListView
    private lateinit var fab: FloatingActionButton

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
        var view = inflater.inflate(R.layout.fragment_add_group, container, false)
        radio_group = view.findViewById(R.id.group_radio)
        listViewGroupFriendsList = view.findViewById(R.id.listViewGroupFriendsList)
        radioFriend = radio_group.findViewById(R.id.radioFriend)

        radioPublic = radio_group.findViewById(R.id.radioPublic)
        group_name = view.findViewById(R.id.group_etName)
        fab = view.findViewById(R.id.fab)
        fab.setOnClickListener(fabNewGroupListener())
        radio_group.check(R.id.radioPublic)
        radioPublic.setOnClickListener { event-> listViewGroupFriendsList.visibility = View.GONE
        }
        radioFriend.setOnClickListener { event-> listViewGroupFriendsList.visibility = View.VISIBLE
        }
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

    fun fabNewGroupListener(): View.OnClickListener = View.OnClickListener { view->
        // requires group name
        if (TextUtils.isEmpty(group_name.text.toString())) {
            Toast.makeText(context, getString(R.string.group_required), Toast.LENGTH_SHORT).show()
        } else {
            val group = HashMap<String, Any>()
            group["name"] = group_name.text.toString()
            var visibility = "public"
            when (radio_group.checkedRadioButtonId) {
                R.id.radioPublic -> {
                    visibility = "public"
                }
                R.id.radioFriend -> {
                    visibility = "friends"
                    //must get list of friends to add to group, complete with userIds
                }
            }
            group["visibility"] = visibility
            group["user"] = auth.currentUser!!.uid

            db.collection("groups")
                .add(group)
                .addOnSuccessListener(addGroupSuccessListener())
                .addOnFailureListener(addGroupFailureListener())


        }

    }

    fun addGroupSuccessListener(): OnSuccessListener<DocumentReference> = OnSuccessListener { documentReference ->
        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.id)
        startActivity(Intent(context,
            MapsActivity::class.java))

    }

    fun addGroupFailureListener(): OnFailureListener = OnFailureListener { err->

        Log.w(TAG, "error in group:", err)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onGroupInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnGroupInteractionListener) {
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
    interface OnGroupInteractionListener {
        // TODO: Update argument type and name
        fun onGroupInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddGroupFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
