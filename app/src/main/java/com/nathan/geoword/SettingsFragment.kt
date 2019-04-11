package com.nathan.geoword

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SettingsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var TAG = this.javaClass.simpleName
    private var listener: OnFragmentInteractionListener? = null
    private lateinit var ivAvatar: ImageView
    private lateinit var ivNewAvatar: ImageView
    private lateinit var etOldPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etSetName: EditText
    private lateinit var btnSubmitSettings: Button
    private lateinit var checkBoxLocation: CheckBox
    private lateinit var checkBoxLowBandwidth: CheckBox
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity

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
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        ivAvatar = view.findViewById(R.id.ivAvatar)
        ivNewAvatar = view.findViewById(R.id.ivNewAvatar)
        ivNewAvatar.setOnClickListener(changeAvatar())
        etOldPassword = view.findViewById(R.id.etOldPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        etSetName = view.findViewById(R.id.etSetName)
        btnSubmitSettings = view.findViewById(R.id.btnSubmitSettings)
        btnSubmitSettings.setOnClickListener(submitSettings())
        checkBoxLocation = view.findViewById(R.id.checkBoxLocation)
        checkBoxLowBandwidth = view.findViewById(R.id.checkBoxLowBandwidth)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            updateUser(auth.currentUser)
            db = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()

            db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot->
                if (documentSnapshot != null) {
                    if (documentSnapshot.get("avatar") != null) {
                        displayAvatar(documentSnapshot.getString("avatar"))
                    }

                    if (documentSnapshot.getString("name")!= null) {
                        etSetName.setText(documentSnapshot.getString("name"))
                    }
                }
            }

            db.collection("users").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    if (documentSnapshot.getBoolean("location")!= null) {
                        checkBoxLocation.isChecked = documentSnapshot.getBoolean("location")!!
                    }
                }
            }

            checkBoxLowBandwidth.isChecked = getBoolean(context!!, LOW_BANDWIDTH)


        }
        return view
    }

    fun finishPasswordAndRespond(map: HashMap<String, Any>?) {
        etOldPassword.setText("")
        etConfirmPassword.setText("")
        etNewPassword.setText("")
        if (map != null) {
            onButtonPressed(map)
        }
    }

    fun submitSettings(): View.OnClickListener = View.OnClickListener { click->

        // get data from activity

        val name = etSetName.text.toString()
        var changePassword = false
        val oldPassword = etOldPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val email = auth.currentUser!!.email!!
        val user = auth.currentUser!!
        val location = checkBoxLocation.isChecked
        val low_bandwidth = checkBoxLowBandwidth.isChecked


        if (newPassword != "") {
            changePassword = true
        }
        if (changePassword) {
            if (confirmPassword == "") {
                Toast.makeText(context!!, "Please confirm Password", Toast.LENGTH_SHORT).show()
            } else {
                val credential = EmailAuthProvider.getCredential(email, oldPassword)
                if (newPassword != confirmPassword) {
                    Toast.makeText(context!!, "confirm password must be the same as your new password", Toast.LENGTH_SHORT).show()
                } else {
                    user.reauthenticate(credential).addOnCompleteListener { task->
                        if (task.isSuccessful) {
                            user.updatePassword(newPassword).addOnCompleteListener { passwordUpdateTask->
                                if (!passwordUpdateTask.isSuccessful) {
                                    Toast.makeText(context!!, "Something went wrong. Enter password information again.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.w(TAG, "Password updated successfully")
                                    Toast.makeText(context!!, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                    val map = HashMap<String, Any>()
                                    map.put("submitAndGoToHome", true)
                                    finishPasswordAndRespond(map)
                                    db.collection("public").document(user.uid).update("name", name)
                                        .addOnSuccessListener { Log.w(TAG, "name updated successfully") }
                                        .addOnFailureListener { e-> Log.d(TAG, "name update fail.", e) }
                                    db.collection("users").document(user.uid).update("location", location)
                                        .addOnSuccessListener { Log.w(TAG, "location setting updated successfully") }
                                        .addOnFailureListener { e-> Log.d(TAG, "location setting update fail.", e) }
                                    setBoolean(context!!, low_bandwidth)
                                }
                            }

                        } else {
                            Toast.makeText(context!!, "Authentication Failed", Toast.LENGTH_SHORT).show()
                            finishPasswordAndRespond(null)
                        }
                    }
                }

            }
        }else {
            db.collection("public").document(user.uid).update("name", name)
                .addOnSuccessListener { Log.w(TAG, "name updated successfully") }
                .addOnFailureListener { e -> Log.d(TAG, "name update fail.", e) }
            db.collection("users").document(user.uid).update("location", location)
                .addOnSuccessListener { Log.w(TAG, "location setting updated successfully") }
                .addOnFailureListener { e-> Log.d(TAG, "location setting update fail.", e) }
            setBoolean(context!!, low_bandwidth)
        }
    }


    fun displayAvatar(imageName: String?) {
        if (imageName != null) {
            Log.w(TAG, "imageName: $imageName")


            val imageRef = storage.reference.child(imageName)
            GlideApp.with(this).load(imageRef).into(ivAvatar)
        }
    }
    val REQUEST_IMAGE_CAPTURE = 1001
    var IMAGE_NAME= ""
    val LIBRARY_REQUEST = 1002
    val REQUEST_STORAGE_CAMERA = 1003
    val REQUEST_STORAGE_SELECT = 1004

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(activity.packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File

                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        context!!,
                        "com.nathan.geoword.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }


    private fun dispatchSelectImageIntent() {
        IMAGE_NAME = System.currentTimeMillis().toString() + ".jpg"

        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        intent.type = "image/*"
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.select_image)),
            LIBRARY_REQUEST
        )
    }

    var mCurrentPhotoPath: String = ""
    var redundantImageName: String = ""
    var imagePhotoPathArray: ArrayList<String> = ArrayList()

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        redundantImageName = "JPEG_${timeStamp}_.jpg"
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
    }

    private fun generateImageName() : String {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return "JPEG_${timeStamp}_.jpg"
    }

    private fun createAlertForAddingImage() {
        var perms = ArrayList<String>()
        perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        perms.add(Manifest.permission.CAMERA)
        var array = arrayOfNulls<String>(perms.count())

        val items = arrayOf<CharSequence>(
            getString(R.string.take_photo),
            getString(R.string.choose_from_library),
            getString(R.string.cancel)
        )
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.select_image))
        builder.setItems(items) { dialog, item ->
            if (items[item] == getString(R.string.take_photo)) {
                val storagePerm = ContextCompat.checkSelfPermission(context!!,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                val cameraPerm = ContextCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
                if ( storagePerm != PackageManager.PERMISSION_GRANTED || cameraPerm != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, perms.toArray(array), REQUEST_STORAGE_CAMERA)
                } else {
                    dispatchTakePictureIntent()
                }
            } else if (items[item] == getString(R.string.choose_from_library)) {
                if (ContextCompat.checkSelfPermission(context!!,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(activity, perms.toArray(array), REQUEST_STORAGE_SELECT)
                } else {
                    dispatchSelectImageIntent()
                }
            } else if (items[item] == getString(R.string.cancel)) {
                dialog.dismiss()
            }
        }
        builder.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_STORAGE_CAMERA) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            }
        } else if (requestCode == REQUEST_STORAGE_SELECT) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchSelectImageIntent()
            }
        }
    }

    private var imgDecodableString: String? = ""

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {


                // Set the Image in ImageView after decoding the String
                ivAvatar.setImageBitmap(
                    BitmapFactory
                        .decodeFile(mCurrentPhotoPath))

                    storeImageOnFirebaseStorage(mCurrentPhotoPath)


            }
            // When an Image is picked
            else if (requestCode == LIBRARY_REQUEST && resultCode == AppCompatActivity.RESULT_OK
                && null != data) {
                // Get the Image from data

                var selectedImage = data.getData()
                Log.w(TAG, "selectedImage : $selectedImage")
                var filePathColumn = ArrayList<String>()
                filePathColumn.add(MediaStore.Images.Media.DATA )
                var array = arrayOfNulls<String>(filePathColumn.size)

                // Get the cursor
                var cursor = context!!.getContentResolver().query(selectedImage,
                    filePathColumn.toArray(array), null, null, null);
                // Move to first row
                cursor.moveToFirst();

                var columnIndex = cursor.getColumnIndex(filePathColumn[0])
                imgDecodableString = cursor.getString(columnIndex)
                Log.w(TAG, imgDecodableString)
                cursor.close()
                val imageView = ImageView(context)

                // Set the Image in ImageView after decoding the String
                ivAvatar.setImageBitmap(
                    BitmapFactory
                        .decodeFile(imgDecodableString))
                storeImageOnFirebaseStorage(imgDecodableString)


            } else {
                Toast.makeText(context!!, "You haven't picked Image",
                    Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Log.w(TAG, "found an error in getting data", e)
        }

    }

    private fun storeImageOnFirebaseStorage(imagePath: String?) {
        if (imagePath != null) {
            val storageRef = storage.reference
            val name = generateImageName()
            val imageRef = storageRef.child(name)

            db.collection("public")
                .document(auth.currentUser!!.uid).update("avatar", name)
                .addOnSuccessListener { ref ->
                    Log.d(TAG, "successfully added image data to note")
                }.addOnFailureListener { e->
                    Log.w(TAG, "error uploading image data", e)
                }


            var uploadTask = imageRef.putFile(Uri.fromFile(File(imagePath)))
            uploadTask.addOnFailureListener { e ->
                // Handle unsuccessful uploads
                Log.w(TAG, "error in uploading image", e)
            }.addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                // ...
                Log.d(TAG, "successfully uploaded file: ${taskSnapshot.metadata!!.name}")
            }
        }
    }


    fun changeAvatar(): View.OnClickListener = View.OnClickListener { click->
        createAlertForAddingImage()
    }

        fun updateUser(user: FirebaseUser?) {
            Log.d(TAG, "updateUser")
            Log.d(TAG, user?.uid)
            //Log.d(TAG, user?.displayName)
            Log.d(TAG, user?.email)

            //tvName.text = user?.displayName
            //Log.d(TAG, "tvName: ${tvName.text}")
        }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(map: HashMap<String, Any>) {
        listener?.onSettingsInteraction(map)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ProfileActivity) {
            this.activity = context
        }
        if (context is OnFragmentInteractionListener) {
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
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onSettingsInteraction(map: HashMap<String, Any>)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        const val PREF_FILE = "prefs"
        const val LOW_BANDWIDTH = "low_bandwidth"

        fun setBoolean(context: Context, boolean: Boolean) {
            val sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(LOW_BANDWIDTH, boolean)
            sharedPreferences.edit().apply()
        }

        fun getBoolean(context: Context, key: String) : Boolean{
            val sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE )
            return sharedPreferences.getBoolean(key, false)

        }
    }
}
