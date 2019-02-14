package com.nathan.geoword

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
class ProfileHomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnHomeInteractionListener? = null
    private lateinit var auth: FirebaseAuth
    private lateinit  var db: FirebaseFirestore
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

            // Access a Cloud Firestore instance from your Activity
            db = FirebaseFirestore.getInstance()



        } else {
            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }
        populateTableList()

        return view
    }
    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser")
        Log.d(TAG, user?.uid)
        Log.d(TAG, user?.displayName)
        Log.d(TAG, user?.email)

        tvName.text = user?.displayName
        Log.d(TAG, "tvName: ${tvName.text}")
    }

    fun populateTableList() {

        //listViewFriendsList.isStretchAllColumns = true
        val header = layoutInflater!!.inflate(R.layout.listview_row_header, listViewFriendsList, false) as ViewGroup
        val title = header.findViewById(R.id.listViewRowHeaderTitle) as TextView
        title.text = getString(R.string.home_friends_list)


        listViewFriendsList.addHeaderView(header)

        val values = ArrayList<String>()
        if (auth.currentUser?.displayName != "Steve Spillane") {
            values.add("Steve Spillane")
            values.add("Sue Sinclair")
            values.add("Rob Sinclair")
            values.add("David Sinclair")
            values.add("Luke Sinclair")
            values.add("Helen Sinclair")
            values.add("Barry Sinclair")

        } else {
            values.add( "Nathan Sinclair")

        }

        val listAdapter = FriendsListAdapter(context!!, R.layout.listview_row_friend, values)
        listViewFriendsList.adapter = listAdapter
    }

    open class FriendsListAdapter(context: Context, resource: Int, list: ArrayList<String>): ArrayAdapter<String>(context, resource,  list) {
        var resource: Int
        var list: ArrayList<String>
        var vi: LayoutInflater

        init {
            this.resource = resource
            this.list = list
            this.vi = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getCount(): Int {
            return list.count()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var holder: ViewHolder
            var retView : View
            if (convertView == null) {
                retView = vi.inflate(resource, null)
                holder = ViewHolder()
                holder.image = retView.findViewById(R.id.listViewRowFriendAvatar)
                holder.name = retView.findViewById(R.id.listViewRowFriendName)


                retView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
                retView = convertView
            }

            holder.name?.text = list[position]

            return retView
        }

        internal  class ViewHolder {
            var image: ImageView? = null
            var name: TextView? = null
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
