package com.nathan.geoword

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_LAT = "latitude"
private const val ARG_LNG = "longitude"
private const val ARG_TITLE = "title"
private const val ARG_PERSON = "person"
private const val ARG_DESC = "description"
private const val ARG_DOCREF = "document_id"
private const val ARG_LOGIN = "login"

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
    private var newImage: ImageView? = null
    private var ll_imageGallery: LinearLayout? = null
    private var fab: FloatingActionButton? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

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
            newImage = findViewById(R.id.story_ivNewImage)
            ll_imageGallery = findViewById(R.id.ll_imageGallery)
            titleEditable?.setText(storedTitle)
            nameEditable?.setText(storedPerson)
            descEditable?.setText(storedDescription)

            fab = findViewById(R.id.fab)
            fab?.setOnClickListener(fabNewClickListener())
            newImage?.setOnClickListener(newImageClickListener())
        } else if (state == ActivityState.edit) {
            setContentView(R.layout.activity_story_new)
            titleEditable = findViewById(R.id.editTextTitle)
            nameEditable = findViewById(R.id.editTextName)
            descEditable = findViewById(R.id.editTextDescription)
            newImage = findViewById(R.id.story_ivNewImage)
            ll_imageGallery = findViewById(R.id.ll_imageGallery)
            titleEditable?.setText(storedTitle)
            nameEditable?.setText(storedPerson)
            descEditable?.setText(storedDescription)

            fab = findViewById(R.id.fab)
            fab?.setImageDrawable(getDrawable(R.drawable.ic_edit_black_24dp))
            fab?.setOnClickListener(fabEditClickListener())
            newImage?.setOnClickListener(newImageClickListener())

        } else {
            setContentView(R.layout.activity_story)
            title = findViewById(R.id.textViewTitle)
            name = findViewById(R.id.textViewName)
            desc = findViewById(R.id.textViewDescription)
            ll_imageGallery = findViewById(R.id.ll_imageGallery)

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
            storage = FirebaseStorage.getInstance()
            if (state == ActivityState.edit || state == ActivityState.display)
                db.collection("notes")
                    .document(docref)
                    .collection("images").orderBy("cr_date", Query.Direction.ASCENDING).get()
                    .addOnSuccessListener(retrieveImageDataForNoteSuccessListener())
                    .addOnFailureListener(retrieveImageDataForNoteFailureListener())



        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }
    }

    fun newImageClickListener(): View.OnClickListener = View.OnClickListener { click->
        // intent for camera gallery.
        createAlertForAddingImage()

    }

    fun retrieveImageDataForNoteSuccessListener(): OnSuccessListener<QuerySnapshot> = OnSuccessListener { result->
        for (document in result) {
            val imageName = document.get("name") as String
            displayImageInGallery(imageName)

        }
    }

    fun retrieveImageDataForNoteFailureListener(): OnFailureListener = OnFailureListener { e->
        Log.w(TAG, "retrieving data for imageGallery failed", e)
    }

    val REQUEST_IMAGE_CAPTURE = 1001
    var IMAGE_NAME= ""
    val LIBRARY_REQUEST = 1002
    val REQUEST_STORAGE_CAMERA = 1003
    val REQUEST_STORAGE_SELECT = 1004

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
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
                        this,
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
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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

    fun displayImageInGallery(imageName: String?) {
        if (imageName != null) {
            Log.w(TAG, "imageName: $imageName")
            var imageView = ImageView(this@StoryActivity)
            imageView.setBackground(getDrawable(R.drawable.dotted))
            var params = LinearLayout.LayoutParams(150, 150)
            params.setMargins(0,0,15,0)
            imageView.layoutParams = params

            val id = View.generateViewId()
            Log.w(TAG, "id: $id")
            imageView.setId(id)
            ll_imageGallery?.addView(imageView, 0)
            val imageRef = storage.reference.child(imageName)
            val findView = findViewById<ImageView>(id)
            GlideApp.with(this).load(imageRef).into(findView)







            // Set the Image in ImageView after decoding the String


        }
    }

    private fun createAlertForAddingImage() {
        var perms = ArrayList<String>()
        perms.add(WRITE_EXTERNAL_STORAGE)
        perms.add(CAMERA)
        var array = arrayOfNulls<String>(perms.count())

        val items = arrayOf<CharSequence>(
            getString(R.string.take_photo),
            getString(R.string.choose_from_library),
            getString(R.string.cancel)
        )
        val builder = AlertDialog.Builder(this@StoryActivity)
        builder.setTitle(getString(R.string.select_image))
        builder.setItems(items) { dialog, item ->
            if (items[item] == getString(R.string.take_photo)) {
                val storagePerm = ContextCompat.checkSelfPermission(this@StoryActivity, WRITE_EXTERNAL_STORAGE)
                val cameraPerm = ContextCompat.checkSelfPermission(this@StoryActivity, CAMERA)
                if ( storagePerm != PERMISSION_GRANTED || cameraPerm != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@StoryActivity, perms.toArray(array), REQUEST_STORAGE_CAMERA)
                } else {
                    dispatchTakePictureIntent()
                }
            } else if (items[item] == getString(R.string.choose_from_library)) {
                if (ContextCompat.checkSelfPermission(this@StoryActivity, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@StoryActivity, perms.toArray(array), REQUEST_STORAGE_SELECT)
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
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                val imageView = ImageView(this@StoryActivity)

                // Set the Image in ImageView after decoding the String
                imageView.setImageBitmap(BitmapFactory
                    .decodeFile(mCurrentPhotoPath))
                imageView.layoutParams = ViewGroup.LayoutParams(240, 240)
                ll_imageGallery?.addView(imageView, 0)
                if (state == ActivityState.new) {
                    imagePhotoPathArray.add(mCurrentPhotoPath)
                } else {
                    storeImageOnFirebaseStorage(mCurrentPhotoPath)
                }


            }
			// When an Image is picked
			else if (requestCode == LIBRARY_REQUEST && resultCode == RESULT_OK
					&& null != data) {
				// Get the Image from data

				var selectedImage = data.getData()
                Log.w(TAG, "selectedImage : $selectedImage")
				var filePathColumn = ArrayList<String>()
                filePathColumn.add(MediaStore.Images.Media.DATA )
                var array = arrayOfNulls<String>(filePathColumn.size)

				// Get the cursor
				var cursor = getContentResolver().query(selectedImage,
						filePathColumn.toArray(array), null, null, null);
				// Move to first row
				cursor.moveToFirst();

                var columnIndex = cursor.getColumnIndex(filePathColumn[0])
				imgDecodableString = cursor.getString(columnIndex)
                Log.w(TAG, imgDecodableString)
				cursor.close()
                val imageView = ImageView(this@StoryActivity)

                // Set the Image in ImageView after decoding the String
                imageView.setImageBitmap(BitmapFactory
                    .decodeFile(imgDecodableString))
                imageView.layoutParams = ViewGroup.LayoutParams(240, 240)
                ll_imageGallery?.addView(imageView, 0)
                if (state==ActivityState.new) {
                    imagePhotoPathArray.add(imgDecodableString!!)
                } else {
                    storeImageOnFirebaseStorage(imgDecodableString)
                }

			} else {
				Toast.makeText(this, "You haven't picked Image",
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
            val image = HashMap<String, Any>()
            image["name"] = name
            image["cr_date"] = Timestamp.now()

            db.collection("notes")
                .document(docref)
                .collection("images").add(image).addOnSuccessListener { ref ->
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
            note["user"] = auth.currentUser!!.uid
            db.collection("notes")
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
                db.collection("notes")
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
        docref = documentReference.id
        for (document in imagePhotoPathArray) {
            storeImageOnFirebaseStorage(document)
        }
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
