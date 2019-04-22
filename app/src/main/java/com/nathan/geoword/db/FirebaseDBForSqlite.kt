package com.nathan.geoword.db

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import android.util.Log
import com.google.firebase.firestore.GeoPoint
import java.util.*

class FirebaseDBForSqlite(context: Context) {
    //major use case: retrieving data from firebase db.
    // 5 minute timer for updating sqlite db with firebase data
    //

    private val TAG = this.javaClass.simpleName
    var context: Context
    val db: UserSqliteOpenHelper
    init {
        this.context = context
        this.db = UserSqliteOpenHelper(this.context)
    }

    val Boolean.int
        get() = if (this) 1 else 0
    val Int.Boolean
        get() = if (this == 1) true else false

    fun addUser(entry: User) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID, entry.user_id)

        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION, entry.location.int)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM, entry.defaultZoom)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL, entry.email)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_AVATAR, entry.avatar)

        db.insert(FirebaseBackupContract.UserEntry.TABLE_NAME, null, values)
    }
    fun getFriends(user_id: String) : ArrayList<Friend> {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(user_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID)

        val sortOrder = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.FriendsEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entries: ArrayList<Friend> = ArrayList()
        with(cursor) {
            while (moveToNext()) {
                val newDate: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val newFriendID: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID))
                val newName: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME))
                val newEmail: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL))
                val newAvatar: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR))
                val newUserId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID))
                val entry = Friend(newDate, newFriendID, newAvatar, newName, newEmail, newUserId)
                entries.add(entry)
            }
        }
        return entries
    }
    fun getMarkerForUser(point: GeoPoint, user_id: String) : ArrayList<Note>{
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(point.latitude.toString(), point.longitude.toString(), user_id)
        val query = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT} = ? AND ${FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG} = ? AND ${FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID} = ?"

        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID)

        val sortOrder = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.NotesEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entries = ArrayList<Note>()

        with(cursor) {
            while (moveToNext()) {
                val created_at: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val description: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION))

                val lat = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT)).toDouble()
                val lng = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG)).toDouble()
                val person: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON))
                val title: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE))
                val user_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID))
                val note_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID))
                val entry: Note  = Note(created_at, description, lat, lng, person, title, user_id, note_id)
                entries.add(entry)
            }
        }
        return entries

    }

    fun getImage(image_id: String): Image? {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(image_id)
        val query = "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.ImageEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.ImageEntry.COLUMN_NAME_NOTE_ID,
            FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID)

        val sortOrder = "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.ImageEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder)

        var entry: Image? = null
        with(cursor){
            while(moveToNext()) {
                val created_at = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val name = getString(getColumnIndexOrThrow(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NAME))
                val note_id = getString(getColumnIndexOrThrow(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NOTE_ID))
                val image_id = getString(getColumnIndexOrThrow(FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID))
                entry = Image(created_at, note_id, name, image_id)
                return entry
            }
        }
        return entry


    }

    fun getNote(note_id: String) : Note? {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(note_id)
        val query = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID)

        val sortOrder = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.NotesEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )

        var entry: Note? = null
        with(cursor) {
            while (moveToNext()) {
                val created_at: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val description: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION))

                val lat = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT)).toDouble()
                val lng = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG)).toDouble()
                val person: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON))
                val title: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE))
                val user_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID))
                val note_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID))
                entry = Note(created_at, description, lat, lng, person, title, user_id, note_id)
                return entry
            }
        }
        return entry

    }

    fun getNotes(user_id: String) : ArrayList<Note>{
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(user_id)
        val query = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID)

        val sortOrder = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.NotesEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entries = ArrayList<Note>()

        with(cursor) {
            while (moveToNext()) {
                val created_at: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val description: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION))

                val lat = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT)).toDouble()
                val lng = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG)).toDouble()
                val person: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON))
                val title: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE))
                val user_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID))
                val note_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID))
                val entry: Note  = Note(created_at, description, lat, lng, person, title, user_id, note_id)
                entries.add(entry)
            }
        }
        return entries
    }


    fun getFriend(friend_id: String): Friend? {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(friend_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID)

        val sortOrder = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.FriendsEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entry: Friend? = null
        with(cursor) {
            while (moveToNext()) {
                val newDate: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val newFriendId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID))
                val newAvatar: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR))
                val newName: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME))
                val newEmail: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL))
                val newUserId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID))

                entry = Friend(newDate, newFriendId,newAvatar, newName, newEmail,newUserId)
                return entry
            }
        }
        return entry

    }

    fun getUser(user_id: String): User? {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(user_id)
        val query = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_AVATAR)

        val sortOrder = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.UserEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
            )
        var entry: User? = null
        with(cursor) {
            while (moveToNext()) {
                val newDate: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val newZoom: Float = getInt(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM)).toFloat()
                //Log.d(TAG, "newZoom: ${newZoom}")
                val newLocation: Boolean = getInt(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION)).Boolean
                val newUserId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID))
                val newEmail: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL))
                val newName: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME))
                val newAvatar: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_AVATAR))

                entry = User(newUserId, newLocation, newZoom, newName, newEmail,newAvatar,newDate)
                return entry
            }
        }
        return entry

    }

    fun updateImage(entry: Image) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NOTE_ID, entry.note_id)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID, entry.image_id)

        val queryColumns = arrayOf(entry.image_id)
        val query = "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID} = ?"

        db.update(FirebaseBackupContract.ImageEntry.TABLE_NAME,
            values,
            query,
            queryColumns)

    }

    fun updateNote(entry: Note) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION, entry.description)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT, entry.geopoint.latitude.toString())
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG, entry.geopoint.longitude.toString())
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON, entry.person)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE, entry.title)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID, entry.user_id)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID, entry.note_id)

        val queryColumns = arrayOf(entry.note_id)
        val query = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID} = ?"

        db.update(FirebaseBackupContract.NotesEntry.TABLE_NAME,
            values,
            query,
            queryColumns)
    }


    fun updateUser(entry: User) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID, entry.user_id)

        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION, entry.location.int)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM, entry.defaultZoom)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL, entry.email)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.UserEntry.COLUMN_NAME_AVATAR, entry.avatar)

        val where =  arrayOf(entry.user_id)
        val query = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID} = ?"

        db.update(FirebaseBackupContract.UserEntry.TABLE_NAME, values, query, where )
    }

    fun addFriend(entry: Friend) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID, entry.friend_id)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL, entry.email)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR, entry.avatar)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID, entry.user_id)

        db.insert(FirebaseBackupContract.FriendsEntry.TABLE_NAME, null, values)
    }

    fun updateFriend(entry: Friend) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID, entry.friend_id)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL, entry.email)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR, entry.avatar)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID, entry.user_id)

        val where = arrayOf(entry.friend_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID} = ?"



        db.update(FirebaseBackupContract.FriendsEntry.TABLE_NAME, values, query, where)
    }

    fun addNote(entry: Note) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION, entry.description)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT, entry.geopoint.latitude.toString())
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG, entry.geopoint.longitude.toString())
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON, entry.person)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE, entry.title)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID, entry.user_id)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID, entry.note_id)

        db.insert(FirebaseBackupContract.NotesEntry.TABLE_NAME, null, values)
    }

    fun addImage(entry: Image) {
        val db = this.db.writableDatabase
        val values = ContentValues()

        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NOTE_ID, entry.note_id)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID, entry.image_id)

        db.insert(FirebaseBackupContract.ImageEntry.TABLE_NAME, null, values)
    }




}