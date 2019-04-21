package com.nathan.geoword

import android.content.Context
import android.content.Intent
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_profile_home.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_LOGIN = "login"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ProfileHomeFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ProfileHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ProfileHomeFragment : androidx.fragment.app.Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnHomeInteractionListener? = null
    lateinit var auth: FirebaseAuth
    lateinit  var db: FirebaseFirestore
    var listAdapter : FriendsListAdapter? = null
    var values = ArrayList<FriendListValue>()
    private lateinit var ivAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var listViewFriendsList: ListView
    private val TAG = this.javaClass.simpleName



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }


    }

    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile_home, container, false)
        tvName= view.findViewById(R.id.profileTextViewTitle)
        ivAvatar = view.findViewById(R.id.profileImgViewAvatar)
        listViewFriendsList = view.findViewById(R.id.listViewFriendsList)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            updateUser(auth.currentUser)
            storage = FirebaseStorage.getInstance()
            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()
            db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { document->

                if (document != null) {
                    tvName.text = document.getString("name")
                    if (document.get("avatar") != null) {
                        displayAvatar(document.getString("avatar"))
                    }
                }
            }


        } else {
            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }
        populateTableList()

        return view
    }

    fun checkDB() {
        values.clear()
        // subs.
        db.collection("sub")
            .whereEqualTo("sub", auth.currentUser!!.uid).get()
            .addOnSuccessListener { subSnapshot ->
                val userList = ArrayList<String>()
                val docids = ArrayList<String>()

                for (document in subSnapshot) {
                    // returns the current selection of subs to accept

                    val userID = document.getString("user")!!
                    Log.w(TAG, "user: $userID")
                    userList.add(userID)
                    val id = document.id
                    Log.w(TAG, "docId: $id")
                    docids.add(id)

                }
                if (userList.count() > 0) {
                    for (user in userList) {
                        db.collection("public").document(user).get()
                            .addOnSuccessListener { document ->
                                var count = 0
                                if (document != null) {
                                    val name = document.getString("name")!!
                                    val email = document.getString("email")!!
                                    Log.w(TAG, "name: $name")
                                    Log.w(TAG, "id: ${docids[count]}")
                                    values.add(FriendListValue(name, docids[count], user, FriendType.REGISTRATION))
                                    count++
                                }

                                listAdapter?.notifyDataSetChanged()


                            }

                    }
                }
            }
        //friends

        db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot->
            if (documentSnapshot != null) {
                val friends = documentSnapshot.get("friends") as HashMap<String, Boolean>?
                if (friends != null) {

                    for ((key, value) in friends!!) {
                        db.collection("public").document(key).get().addOnSuccessListener { document ->
                            if (document != null) {
                                values.add(FriendListValue(document.getString("name")!!))
                            }
                            listAdapter?.notifyDataSetChanged()

                        }
                    }
                }


            }
        }
    }
    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        //Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)

        //tvName.text = user?.displayName
        Log.d(TAG, "tvName: ${tvName.text}")
    }
    enum class FriendType(val type: Int) {
        REGISTRATION(1), DEFAULT(0)


    }
    inner class FriendListValue {
        var name: String? = null
        var type: FriendType? = FriendType.DEFAULT
        var userID: String? = null
        var docId: String? = null



        constructor(newName:String, newDoc: String, newUserID: String,newType: FriendType) {
            name = newName
            type = newType
            docId = newDoc
            userID = newUserID
        }

        constructor(newName: String) {
            name = newName
            type = FriendType.DEFAULT
        }
    }

    fun populateTableList() {

        //listViewFriendsList.isStretchAllColumns = true
        val header = layoutInflater!!.inflate(R.layout.listview_row_header, listViewFriendsList, false) as ViewGroup
        val title = header.findViewById(R.id.listViewRowHeaderTitle) as TextView
        title.text = getString(R.string.home_friends_list)
        values.clear()


        listViewFriendsList.addHeaderView(header)
        listAdapter = FriendsListAdapter(context!!, R.layout.listview_row_friend,values)
        listViewFriendsList.adapter = listAdapter

        checkDB()
    }

    inner class FriendsListAdapter(context: Context, resource: Int, list: ArrayList<FriendListValue>): ArrayAdapter<FriendListValue>(context, resource,  list) {
        var resource: Int
        var list: ArrayList<FriendListValue>
        var vi: LayoutInflater
        val TAG = this.javaClass.simpleName


        init {
            this.resource = resource
            this.list = list
            this.vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        }

        override fun getCount(): Int {
            return list.count()
        }

        override fun getViewTypeCount(): Int {
            return 2
        }

        override fun getItemViewType(position: Int): Int {
            return list[position].type!!.type
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var holder: ViewHolder
            var retView: View = View(context)
            if (convertView == null) {

                holder = ViewHolder()
                if (list[position].type == FriendType.REGISTRATION) {
                    retView = vi.inflate(R.layout.listview_row_friend_sub, null)
                    holder.image = retView.findViewById(R.id.listViewRowFriendSubAvatar)
                    holder.name = retView.findViewById(R.id.listViewRowFriendSubName)
                    holder.accept = retView.findViewById(R.id.button_sub_accept)
                    holder.decline = retView.findViewById(R.id.button_sub_decline)
                } else if (list[position].type == FriendType.DEFAULT) {
                    retView = vi.inflate(R.layout.listview_row_friend, null)
                    holder.image = retView.findViewById(R.id.listViewRowFriendAvatar)
                    holder.name = retView.findViewById(R.id.listViewRowFriendName)
                }



                retView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
                retView = convertView
            }

            holder.name?.text = list[position].name
            holder.decline?.setOnClickListener { click ->
                db.collection("sub")
                    .document(list[position].docId!!)
                    .delete()
                    .addOnSuccessListener { Log.d(TAG, "document successfully deleted")
                    checkDB()}
                    .addOnFailureListener { e-> Log.w(TAG, "error deleting document", e) }


            }
            holder.accept?.setOnClickListener { click->



                db.collection("public")
                    .document(auth.currentUser!!.uid)
                    .update("friends.${list[position].userID}",true)
                    .addOnSuccessListener {
                        Log.d(TAG, "friend successfully added")

                        db.collection("public")
                            .document(list[position].userID!!)
                            .update("friends.${auth.currentUser!!.uid}", true)
                            .addOnSuccessListener { Log.d(TAG, "added in other friends list")
                                listAdapter?.notifyDataSetChanged()}}
                    .addOnFailureListener { e-> Log.w(TAG, "error adding friend", e)
                        }

                db.collection("sub")
                    .document(list[position].docId!!)
                    .delete()
                    .addOnSuccessListener { Log.d(TAG, "document successfully deleted")
                        checkDB()
                    }
                    .addOnFailureListener { e-> Log.w(TAG, "error deleting document", e)
                        }
            }



            return retView
        }




    }
    internal  class ViewHolder {
        var image: ImageView? = null
        var name: TextView? = null
        var accept: Button? = null
        var decline: Button? = null
    }

    fun displayAvatar(imageName: String?) {
        if (imageName != null) {
            Log.w(TAG, "imageName: $imageName")


            val imageRef = storage.reference.child(imageName)
            GlideApp.with(this).load(imageRef).into(ivAvatar)
        }
    }



    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onHomeInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnHomeInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnHomeInteractionListener")
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
    interface OnHomeInteractionListener {
        // TODO: Update argument type and name
        fun onHomeInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileHomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileHomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
