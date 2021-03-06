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
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.Glide
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.nathan.geoword.Static.Companion.LIBRARY_REQUEST
import com.nathan.geoword.Static.Companion.REQUEST_IMAGE_CAPTURE
import com.nathan.geoword.Static.Companion.REQUEST_STORAGE_CAMERA
import com.nathan.geoword.Static.Companion.REQUEST_STORAGE_SELECT
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
private const val ARG_EDITABLE = "editable"
private const val ARG_IMAGEREF = "image_reference"

private var arguments: Bundle? = null

class StoryActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private var state: ActivityState? = null
    private var longitude: Double? = null
    private var latitude: Double? = null
    private var storedTitle : String = ""
    private var storedPerson : String = ""
    private var storedDescription : String = ""
    private var storedEditable: Boolean = false
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
    private var imageNames: ArrayList<String> = ArrayList()

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

    private lateinit var fabDelete: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // determine what layout to show depending on edit or new



        val param1 = intent.getIntExtra(ARG_PARAM1, 0)
        state = findStateFromNumber(param1)
        latitude = intent.getDoubleExtra(ARG_LAT, -1.0)
        longitude = intent.getDoubleExtra(ARG_LNG, -1.0)
        storedEditable = intent.getBooleanExtra(ARG_EDITABLE, false)
        try {
            //storedTitle = intent.getStringExtra(ARG_TITLE)
            //storedPerson = intent.getStringExtra(ARG_PERSON)
            //storedDescription = intent.getStringExtra(ARG_DESC)

            docref = intent.getStringExtra(ARG_DOCREF)
        } catch (ex: IllegalStateException) {
            //storedTitle = ""
            //storedPerson = ""
            //storedDescription = ""
            docref = ""
        }



        if (state == ActivityState.new) {
            setContentView(R.layout.activity_story_new)
            titleEditable = findViewById(R.id.editTextTitle)
            nameEditable = findViewById(R.id.editTextName)
            descEditable = findViewById(R.id.editTextDescription)
            newImage = findViewById(R.id.story_ivNewImage)
            ll_imageGallery = findViewById(R.id.ll_imageGallery)


            fab = findViewById(R.id.fab)
            fab?.setOnClickListener(fabNewClickListener())
            newImage?.setOnClickListener(newImageClickListener())
            fabDelete = findViewById(R.id.fabDelete)
            fabDelete.hide()
        } else if (state == ActivityState.edit) {
            setContentView(R.layout.activity_story_new)
            titleEditable = findViewById(R.id.editTextTitle)
            nameEditable = findViewById(R.id.editTextName)
            descEditable = findViewById(R.id.editTextDescription)
            newImage = findViewById(R.id.story_ivNewImage)
            ll_imageGallery = findViewById(R.id.ll_imageGallery)
            //titleEditable?.setText(storedTitle)
            //nameEditable?.setText(storedPerson)
            //descEditable?.setText(storedDescription)

            fab = findViewById(R.id.fab)
            fab?.setImageDrawable(getDrawable(R.drawable.ic_edit_black_24dp))
            fab?.setOnClickListener(fabEditClickListener())

            newImage?.setOnClickListener(newImageClickListener())
            fabDelete = findViewById(R.id.fabDelete)
            fabDelete.setOnClickListener(fabDeleteClickListener())

        } else {
            setContentView(R.layout.activity_story)
            title = findViewById(R.id.textViewTitle)
            name = findViewById(R.id.textViewName)
            desc = findViewById(R.id.textViewDescription)
            ll_imageGallery = findViewById(R.id.ll_imageGallery)


            fab = findViewById(R.id.fab)
            fab?.setOnClickListener(fabDisplayClickListener())
            if (storedEditable) {
                fab?.show()
            } else {
                fab?.hide()
            }
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
                if (docref != "") {
                    db.collection("notes")
                        .document(docref).get().addOnSuccessListener { documentSnapshot ->

                            if (documentSnapshot != null) {
                                storedTitle = documentSnapshot.getString("title")!!
                                storedPerson = documentSnapshot.getString("person")!!
                                storedDescription = documentSnapshot.getString("description")!!
                                if (state == StoryActivity.ActivityState.edit) {
                                    titleEditable?.setText(storedTitle)
                                    nameEditable?.setText(storedPerson)
                                    descEditable?.setText(storedDescription)
                                } else if (state == StoryActivity.ActivityState.display) {
                                    title?.text = storedTitle
                                    name?.text = storedPerson
                                    desc?.text = storedDescription
                                }
                            }
                            db.collection("notes").document(docref).collection("images").orderBy("cr_date", Query.Direction.ASCENDING).get()
                                .addOnSuccessListener(retrieveImageDataForNoteSuccessListener())
                                .addOnFailureListener(retrieveImageDataForNoteFailureListener())
                        }

                }



        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(ARG_LOGIN, 0)
            startActivity(intent)
        }
    }

    private fun deleteImageFromStorage(imageName: String?) {
        if (imageName != null) {
            db.collection("notes").document(docref).collection("images")
                .whereEqualTo("name", imageName).get().addOnSuccessListener { querySnapshot->
                    for (document in querySnapshot) {
                        val ref = storage.reference
                        val delete = ref.child(imageName)
                        delete.delete().addOnSuccessListener {
                            Log.w(TAG, "deleted image: ${imageName}")
                            db.collection("notes").document(docref).collection("images")
                                .document(document.id).delete().addOnSuccessListener { Log.w(TAG, "deleted image in note") }
                                .addOnFailureListener {e->Log.d(TAG, "error deleting image", e)}
                        }
                            .addOnFailureListener { e ->
                                Log.d(TAG, "error deleting image.", e)
                                db.collection("notes").document(docref).collection("images")
                                    .document(document.id).delete().addOnSuccessListener { Log.w(TAG, "deleted image in note") }
                                    .addOnFailureListener {e->Log.d(TAG, "error deleting image", e)}
                            }

                    }
                }

        }
    }

    private fun deleteNoteFromDatabase() {
        db
            .collection("notes")
            .document(docref)
            .delete()
            .addOnSuccessListener {
                Log.w(TAG, "successfully deleted note from database")
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)}
            .addOnFailureListener { e->
                Log.d(TAG, "had an error deleting document from database:", e)
                val intent = Intent(this, MapsActivity::class.java)
                startActivity(intent)}
    }

    private fun fabDeleteClickListener(): View.OnClickListener?  = View.OnClickListener{view->
        // call delete function of document and then go back to mapactivity
        createAlertForDeletingNote()

    }

    private fun confirmYesDeleteNote() {
        db.collection("notes").document(docref).collection("images").get()
            .addOnSuccessListener { querySnapshot ->
                var count = 0
                var size = querySnapshot.documents.size
                if (querySnapshot.documents.count() > 0) {
                    for (document in querySnapshot) {
                        val imageName = document.getString("name")
                        val ref = storage.reference
                        val delete = ref.child(imageName!!)
                        delete.delete().addOnSuccessListener {
                            Log.w(TAG, "deleted image: ${imageName}")
                            count++
                            if (count == size) {
                                deleteNoteFromDatabase()
                            }
                        }
                            .addOnFailureListener { e ->
                                if (e is StorageException) {
                                    count++
                                }
                                if (count == size) {
                                    deleteNoteFromDatabase()
                                }

                                Log.d(TAG, "error deleting image.", e)

                            }
                    }
                } else {
                    deleteNoteFromDatabase()
                }
        }

    }

    fun newImageClickListener(): View.OnClickListener = View.OnClickListener { click->
        // intent for camera gallery.
        createAlertForAddingImage()

    }

    fun retrieveImageDataForNoteSuccessListener(): OnSuccessListener<QuerySnapshot> = OnSuccessListener { result->
        for (document in result) {
            val imageName = document.get("name") as String
            imageNames.add(imageName)
            displayImageInGallery(imageName)

        }
    }

    fun retrieveImageDataForNoteFailureListener(): OnFailureListener = OnFailureListener { e->
        Log.w(TAG, "retrieving data for imageGallery failed", e)
        Log.w(TAG, "retrieving data for imageGallery failed", e)
    }


    var IMAGE_NAME= ""

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
                    Log.w(TAG, "photoURI String: ${photoURI.path}")
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
    var mCurrentPhotoName: String = ""
    var redundantImageName: String = ""
    var imagePhotoPathArray: ArrayList<String> = ArrayList()
    var imagePhotoNameArray: ArrayList<String> = ArrayList()

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val userId = auth.currentUser!!.uid
        redundantImageName = "JPEG_${userId}_${timeStamp}_.jpg"
        val file = File.createTempFile(
            "JPEG_${userId}_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
        mCurrentPhotoName = file.name
        Log.w(TAG, "File: absolutePath: ${file.absolutePath}")
        return file
    }

    private fun generateImageName() : String {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val userId = auth.currentUser!!.uid
        return "JPEG_${userId}_${timeStamp}_.jpg"
    }

    fun displayImageInGallery(imageName: String?) {
        if (imageName != null) {

            val actualImage = ImageView(this@StoryActivity)
            val deleteImage = ImageView(this@StoryActivity)
            var params = LinearLayout.LayoutParams(240, 240)
            params.setMargins(0,0,15,0)
            actualImage.layoutParams = params

            val id = View.generateViewId()
            Log.w(TAG, "id: $id")
            actualImage.setId(id)

            if (state != ActivityState.display) {
                val frameLayout = FrameLayout(this@StoryActivity)

                frameLayout.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
                actualImage.setBackground(getDrawable(R.drawable.dotted))
                val deleteDrawable = getDrawable(R.drawable.ic_delete_black_24dp)


                deleteImage.setImageDrawable(deleteDrawable)
                DrawableCompat.setTint(deleteImage.drawable, ContextCompat.getColor(this, R.color.zomato_red))
                deleteImage.setOnClickListener(deleteImageClickListener())
                val deleteLayoutParams = FrameLayout.LayoutParams(50, 50)
                deleteLayoutParams.gravity = Gravity.END
                deleteLayoutParams.bottomMargin = 0
                deleteLayoutParams.topMargin = 0
                deleteLayoutParams.marginStart = 0
                deleteLayoutParams.marginEnd = 0
                deleteImage.layoutParams = deleteLayoutParams
                frameLayout.addView(actualImage, 0)
                frameLayout.addView(deleteImage, 1)
                ll_imageGallery?.addView(frameLayout, 0)

            }

            if (state == ActivityState.display) {
                ll_imageGallery?.addView(actualImage, 0)
            }



            Log.w(TAG, "imageName: $imageName")
            val imageRef = storage.reference.child(imageName)
            val findView = findViewById<ImageView>(id)
            findView.setOnClickListener(displayLargeImage())
            GlideApp.with(this).load(imageRef).into(findView)







            // Set the Image in ImageView after decoding the String


        }
    }

    private fun displayLargeImage(): View.OnClickListener = View.OnClickListener {view->
        if (state == ActivityState.display) {
            val ll: LinearLayout = view.parent as LinearLayout
            val index = ll.indexOfChild(view)
            Log.w(TAG, "index: ${index}")
            val intent = Intent(this, ImageGalleryActivity::class.java)
            intent.putExtra(ARG_DOCREF, docref)
            intent.putExtra(ARG_IMAGEREF, index)
            startActivity(intent)
        }
    }

    private fun deleteImageClickListener(): View.OnClickListener = View.OnClickListener { view->
        createAlertForDeletingImage(view)
    }

    private fun confirmYesDeleteImage(view: View) {
        val ll: LinearLayout = view.parent.parent as LinearLayout
        val index = ll.indexOfChild(view.parent as View)
        ll.removeViewAt(index)
        if (state == ActivityState.new) {

            imagePhotoNameArray.removeAt(index)
            imagePhotoPathArray.removeAt(index)

            //deleteImageFromStorage(imageNames[index])

        } else if (state == ActivityState.edit) {
            Log.d(TAG, "imageName: ${imageNames.get(index)}")
            deleteImageFromStorage(imageNames.get(index))
        }
    }
    private fun createAlertForDeletingImage(view : View) {
        val items = arrayOf<CharSequence>(getString(R.string.yes), getString(R.string.no))
        val builder = AlertDialog.Builder(this@StoryActivity)
        builder.setTitle("Are you sure you want to delete image?")
        builder.setItems(items) { dialog, item ->
            if (items[item] == getString(R.string.yes)) {
                confirmYesDeleteImage(view)
            } else if (items[item] == getString(R.string.no)){
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun createAlertForDeletingNote() {
        val items = arrayOf<CharSequence>(getString(R.string.yes), getString(R.string.no))
        val builder = AlertDialog.Builder(this@StoryActivity)
        builder.setTitle("Are you sure you want to delete note?")
        builder.setItems(items) { dialog, item ->
            if (items[item] == getString(R.string.yes)) {
                    confirmYesDeleteNote()
                } else if (items[item] == getString(R.string.no)){
                    dialog.dismiss()
            }
        }
        builder.show()
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

    fun placeImageInContainerAfterCapture(file: String) {

        val frameLayout = FrameLayout(this@StoryActivity)
        frameLayout.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT)

        val actualImage = ImageView(this@StoryActivity)
        val deleteImage = ImageView(this@StoryActivity)

        actualImage.setImageBitmap(BitmapFactory.decodeFile(file))
        actualImage.layoutParams = ViewGroup.LayoutParams(240,240)
        actualImage.setBackground(getDrawable(R.drawable.dotted))
        val deleteDrawable = getDrawable(R.drawable.ic_delete_black_24dp)

        deleteImage.setOnClickListener(deleteImageClickListener())
        deleteImage.setImageDrawable(deleteDrawable)
        DrawableCompat.setTint(deleteImage.drawable, ContextCompat.getColor(this, R.color.zomato_red))
        val deleteLayoutParams = FrameLayout.LayoutParams(50, 50)
        deleteLayoutParams.gravity = Gravity.END
        deleteLayoutParams.bottomMargin = 0
        deleteLayoutParams.topMargin = 0
        deleteLayoutParams.marginStart = 0
        deleteLayoutParams.marginEnd = 0
        deleteImage.layoutParams = deleteLayoutParams
        frameLayout.addView(actualImage, 0)
        frameLayout.addView(deleteImage, 1)

        ll_imageGallery?.addView(frameLayout, 0)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		try {
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                placeImageInContainerAfterCapture(mCurrentPhotoPath)
                if (state == ActivityState.new) {
                    imagePhotoPathArray.add(mCurrentPhotoPath)
                    imagePhotoNameArray.add(mCurrentPhotoName)
                    Log.w(TAG, "takePicture: path: ${imagePhotoPathArray[imagePhotoPathArray.size -1]}")
                } else {
                    val imageName = generateImageName()
                    imageNames.add(imageName)
                    storeImageOnFirebaseStorage(mCurrentPhotoPath, imageName)
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
                placeImageInContainerAfterCapture(imgDecodableString!!)
                if (state==ActivityState.new) {
                    imagePhotoPathArray.add(imgDecodableString!!)
                } else {
                    val imageName = generateImageName()
                    imageNames.add(imageName)
                    storeImageOnFirebaseStorage(imgDecodableString, imageName)
                }

			} else {
				Toast.makeText(this, "You haven't picked Image",
						Toast.LENGTH_LONG).show()
			}

		} catch (e: Exception) {
			Log.w(TAG, "found an error in getting data", e)
		}

	}

    private fun storeImageOnFirebaseStorage(imagePath: String?, fileName: String?) {
        if (imagePath != null) {
            val storageRef = storage.reference

            var name = ""
            if (fileName != null) {
                name = fileName
            } else {
                name = generateImageName()
            }
            Log.w(TAG, "importing to Firestore: ${name}")
            val imageRef = storageRef.child(name)
            val image = HashMap<String, Any>()
            image["name"] = name
            image["cr_date"] = Timestamp.now()
            image["note_id"] = docref

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
        var count = 0
        for (document in imagePhotoPathArray) {
            Log.w(TAG, "addNoteSuccess: document: ${document}")
            storeImageOnFirebaseStorage(document, imagePhotoNameArray[count])
            count++
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
