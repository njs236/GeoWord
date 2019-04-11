package com.nathan.geoword.db

import android.content.ContentValues
import android.content.Context
import android.media.Image
import android.provider.BaseColumns
import android.provider.ContactsContract
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.auth.User
import com.google.firestore.v1.StructuredQuery
import java.util.*

class FirebaseDBForSqlite(context: Context) {
    //major use case: retrieving data from firebase db.
    // 5 minute timer for updating sqlite db with firebase data
    //

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

    fun addUser(entry: UserEntry) {
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
    fun getFriends(user_id: String) : ArrayList<FriendEntry> {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(user_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID)

        val sortOrder = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.UserEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entries: ArrayList<FriendEntry> = ArrayList()
        with(cursor) {
            while (moveToNext()) {
                val newDate: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val newFriendID: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID))
                val newAvatar: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR))
                val newName: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME))

                val newEmail: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL))


                val newUserId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID))
                val entry = FriendEntry(newDate, newFriendID, newAvatar, newName, newEmail, newUserId)
                entries.add(entry)
            }
        }
        return entries
    }
    fun getMarkerForUser(point: GeoPoint, user_id: String) : ArrayList<NoteEntry>{
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(point.latitude.toInt().toString(), point.longitude.toInt().toString(), user_id)
        val query = "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT} = ? AND ${FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG} = ? AND ${FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID} = ?"

        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID)

        val sortOrder = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.UserEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entries = ArrayList<NoteEntry>()

        with(cursor) {
            while (moveToNext()) {
                val created_at: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val description: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION))

                val lat = getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT)).toDouble()
                val lng = getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG)).toDouble()
                val person: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON))
                val title: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE))
                val user_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID))
                val note_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID))
                val entry: NoteEntry  = NoteEntry(created_at, description, lat, lng, person, title, user_id, note_id)
                entries.add(entry)
            }
        }
        return entries

    }

    fun getNotes(user_id: String) : ArrayList<NoteEntry>{
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(user_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID,
            FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID)

        val sortOrder = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.UserEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entries = ArrayList<NoteEntry>()

        with(cursor) {
            while (moveToNext()) {
                val created_at: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val description: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION))

                val lat = getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT)).toDouble()
                val lng = getInt(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG)).toDouble()
                val person: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON))
                val title: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE))
                val user_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID))
                val note_id: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID))
                val entry: NoteEntry  = NoteEntry(created_at, description, lat, lng, person, title, user_id, note_id)
                entries.add(entry)
            }
        }
        return entries
    }

    //TODO: getnote with note_id

    //TODO: updateNote with entry

    fun getFriend(friend_id: String): FriendEntry? {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(friend_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL,
            FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR)

        val sortOrder = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT} DESC"

        val cursor = db.query(FirebaseBackupContract.UserEntry.TABLE_NAME,
            projection,
            query,
            queryColumn,
            null,
            null,
            sortOrder
        )
        var entry: FriendEntry
        with(cursor) {
            while (moveToNext()) {
                val newDate: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT)).toLong())
                val newFriendId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID))
                val newAvatar: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR))
                val newName: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME))
                val newEmail: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL))
                val newUserId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID))

                entry = FriendEntry(newDate, newFriendId,newAvatar, newName, newEmail,newUserId)
                return entry
            }
        }
        return null

    }

    fun getUser(user_id: String): UserEntry? {
        val db = this.db.readableDatabase
        val queryColumn = arrayOf(user_id)
        val query = "${FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID} = ?"
        val projection = arrayOf(BaseColumns._ID, FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME,
            FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL,
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
        var entry: UserEntry
        with(cursor) {
            while (moveToNext()) {
                val newUserId: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID))
                val newLocation: Boolean = getInt(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION)).Boolean
                val newZoom: Float = getInt(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM)).toFloat()
                val newName: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME))
                val newEmail: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL))
                val newAvatar: String = getString(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_AVATAR))
                val newDate: Date = Date(getInt(getColumnIndexOrThrow(FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT)).toLong())
                entry = UserEntry(newUserId, newLocation, newZoom, newName, newEmail,newAvatar,newDate)
                return entry
            }
        }
        return null

    }


    fun updateUser(entry: UserEntry) {
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

    fun addFriend(entry: FriendEntry) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID, entry.friend_id)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL, entry.email)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR, entry.avatar)

        db.insert(FirebaseBackupContract.FriendsEntry.TABLE_NAME, null, values)
    }

    fun updateFriend(entry: FriendEntry) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID, entry.friend_id)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL, entry.email)
        values.put(FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR, entry.avatar)

        val where = arrayOf(entry.friend_id)
        val query = "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID} = ?"



        db.update(FirebaseBackupContract.FriendsEntry.TABLE_NAME, values, query, where)
    }

    fun addNote(entry: NoteEntry) {
        val db = this.db.writableDatabase
        val values = ContentValues()
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION, entry.description)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT, entry.geopoint.latitude)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG, entry.geopoint.longitude)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID, entry.note_id)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON, entry.person)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE, entry.title)
        values.put(FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID, entry.user_id)

        db.insert(FirebaseBackupContract.NotesEntry.TABLE_NAME, null, values)
    }

    fun addImage(entry: ImageEntry) {
        val db = this.db.writableDatabase
        val values = ContentValues()

        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT, entry.created_at.time)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NAME, entry.name)
        values.put(FirebaseBackupContract.ImageEntry.COLUMN_NAME_NOTE_ID, entry.note_id)

        db.insert(FirebaseBackupContract.ImageEntry.TABLE_NAME, null, values)
    }




}