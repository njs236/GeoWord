package com.nathan.geoword

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.fragment_settings.*

private const val ARG_DOCREF = "document_id"
private const val ARG_IMAGEREF = "image_reference"

class ImageGalleryActivity : AppCompatActivity(), ThumbnailCallback {
    override fun onThumbnailClick(imageRef: StorageReference) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        Log.w(TAG, "open imageRef: ${imageRef.name}")
        GlideApp.with(context).load(imageRef).into(placeHolderImageView)

    }

    val activity = this@ImageGalleryActivity
    private lateinit var thumbListView: RecyclerView
    private lateinit var placeHolderImageView: ImageView

    private lateinit  var auth: FirebaseAuth
    private val TAG: String = this.javaClass.simpleName

    private val imageNames: ArrayList<String> = ArrayList<String>()


    private lateinit var db: FirebaseFirestore

    private lateinit var storage: FirebaseStorage

    private lateinit var docref: String
    private val context : Context = this@ImageGalleryActivity
    val thumbsManager: ThumbnailsManager = ThumbnailsManager()

    private var imageIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_gallery)
        thumbListView = findViewById(R.id.thumbnails)
        placeHolderImageView = findViewById(R.id.place_holder_imageview)
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        layoutManager.scrollToPosition(0)
        thumbListView.layoutManager = layoutManager
        thumbListView.setHasFixedSize(true)



        try {
            docref = intent.getStringExtra(ARG_DOCREF)
            imageIndex = intent.getIntExtra(ARG_IMAGEREF, 0)
        } catch (ex: IllegalStateException) {
            docref = ""
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            updateUser(auth.currentUser)
            db = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()

            db.collection("public").document(auth.currentUser!!.uid).get().addOnSuccessListener { documentSnapshot->
                if (documentSnapshot != null) {
                    if (documentSnapshot.get("avatar") != null) {
                        //displayAvatar(documentSnapshot.getString("avatar"))
                    }
                    if (documentSnapshot.getString("name")!= null) {
                        //etSetName.setText(documentSnapshot.getString("name"))
                    }
                }
            }

            db.collection("notes").document(docref).collection("images").orderBy("cr_date", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(retrieveImageDataForNoteSuccessListener())
                .addOnFailureListener(retrieveImageDataForNoteFailureListener())
        }





    }
    fun retrieveImageDataForNoteSuccessListener(): OnSuccessListener<QuerySnapshot> = OnSuccessListener { result->
        for (document in result) {
            val imageName = document.get("name") as String
            imageNames.add(imageName)
            //displayImageInGallery(imageName)


        }

        val handler = Handler()
        val runnable = Runnable {
            val thumbs = ArrayList<ThumbnailItem>()
            for (image in imageNames) {
                val thumbnail = ThumbnailItem(image)
                thumbs.add(thumbnail)
            }

            val adapter = ThumbnailsAdapter(thumbs, activity)
            thumbListView.adapter = adapter
            val imageRef = storage.reference.child(thumbs[imageIndex].image)
            GlideApp.with(context).load(imageRef).into(placeHolderImageView)
            adapter.notifyDataSetChanged()
        }
        handler.post(runnable)
    }

    fun retrieveImageDataForNoteFailureListener(): OnFailureListener = OnFailureListener { e->
        Log.w(TAG, "retrieving data for imageGallery failed", e)
        Log.w(TAG, "retrieving data for imageGallery failed", e)
    }

    fun updateUser(user: FirebaseUser?) {
        if (user!= null) {

        }
    }



    inner class ThumbnailItem(newImageName: String) {
        var image: String
        init {
            this.image = newImageName
        }
    }

    inner class ThumbnailsAdapter (newDataSet: List<ThumbnailItem>, newThumbnailCallback: ThumbnailCallback):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        private val TAG: String = this.javaClass.simpleName
        private var lastPosition: Int = -1
        private var thumbNailCallback: ThumbnailCallback
        private var dataSet: List<ThumbnailItem>
        init {
            this.thumbNailCallback = newThumbnailCallback
            this.dataSet = newDataSet


        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
            Log.v(TAG, "On Create View Holder Called")
            val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_thumbnail_item, viewGroup, false)
            return ThumbnailsViewHolder(itemView)
        }

        override fun getItemCount(): Int {
            return dataSet.count()
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
            val thumbnailItem = dataSet.get(i)
            Log.v(TAG, "On Bind View Called")
            val thumbnailsViewHolder : ThumbnailsViewHolder = holder as ThumbnailsViewHolder
            val imageRef = storage.reference.child(thumbnailItem.image)
            GlideApp.with(context).load(imageRef).into(thumbnailsViewHolder.thumbnail)
            thumbnailsViewHolder.thumbnail.setScaleType(ImageView.ScaleType.FIT_START)
            setAnimation(thumbnailsViewHolder.thumbnail, i)
            thumbnailsViewHolder.thumbnail.setOnClickListener { view->
                if (lastPosition != i) {
                    thumbNailCallback.onThumbnailClick(imageRef)
                    lastPosition = i
                }
            }
        }

        fun setAnimation(viewToAnimate: View, position: Int) {
            lastPosition = position
        }

        inner class ThumbnailsViewHolder: RecyclerView.ViewHolder {
            var thumbnail: ImageView

            constructor(v: View): super(v) {
                thumbnail = v.findViewById(R.id.thumbnail)
            }
        }



    }

    inner class ThumbnailsManager {

        private val thumbs = ArrayList<ThumbnailItem>()
        fun addThumb(item: ThumbnailItem) {
            thumbs.add(item)

        }

        fun clearThumbs() {
            thumbs.clear()
        }
    }

}
