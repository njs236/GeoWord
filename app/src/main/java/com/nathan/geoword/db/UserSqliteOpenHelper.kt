package com.nathan.geoword.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object FirebaseBackupContract{
    object UserEntry: BaseColumns {
        const val TABLE_NAME = "tbl_user"
        const val COLUMN_NAME_CREATED_AT = "created_at"
        const val COLUMN_NAME_DEFAULTZOOM = "default_zoom"
        const val COLUMN_NAME_LOCATION = "location"
        const val COLUMN_NAME_USER_ID = "user_id"
        const val COLUMN_NAME_EMAIL = "email"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_AVATAR = "avatar"
    }

    object FriendsEntry: BaseColumns {
        const val TABLE_NAME = "tbl_friends"
        const val COLUMN_NAME_CREATED_AT = "created_at"
        const val COLUMN_NAME_FRIEND_ID = "friend_id"
        const val COLUMN_NAME_USER_ID = "user_id"
        const val COLUMN_NAME_EMAIL = "email"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_AVATAR = "avatar"
    }

    object NotesEntry : BaseColumns {
        const val TABLE_NAME = "tbl_notes"
        const val COLUMN_NAME_CREATED_AT = "created_at"
        const val COLUMN_NAME_DESCRIPTION = "description"
        const val COLUMN_NAME_LAT = "lat"
        const val COLUMN_NAME_LNG = "lng"
        const val COLUMN_NAME_PERSON = "person"
        const val COLUMN_NAME_TITLE = "title"
        const val COLUMN_NAME_USER_ID = "user_id"
        const val COLUMN_NAME_NOTE_ID = "note_id"
    }

    object ImageEntry: BaseColumns {
        const val TABLE_NAME = "tbl_images"
        const val COLUMN_NAME_NOTE_ID = "note_id"
        const val COLUMN_NAME_CREATED_AT = "created_at"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_IMAGE_ID = "image_id"
    }

}

private const val SQL_CREATE_USER_ENTRIES = "CREATE TABLE ${FirebaseBackupContract.UserEntry.TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FirebaseBackupContract.UserEntry.COLUMN_NAME_CREATED_AT} INTEGER," +
        "${FirebaseBackupContract.UserEntry.COLUMN_NAME_DEFAULTZOOM} INTEGER," +
        "${FirebaseBackupContract.UserEntry.COLUMN_NAME_LOCATION} INTEGER," +
        "${FirebaseBackupContract.UserEntry.COLUMN_NAME_USER_ID} TEXT," +
        "${FirebaseBackupContract.UserEntry.COLUMN_NAME_EMAIL} TEXT," +
        "${FirebaseBackupContract.UserEntry.COLUMN_NAME_NAME} TEXT," +
"${FirebaseBackupContract.UserEntry.COLUMN_NAME_AVATAR} TEXT)"

private const val SQL_CREATE_FRIENDS_ENTRIES = "CREATE TABLE ${FirebaseBackupContract.FriendsEntry.TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_CREATED_AT} INTEGER,"+
        "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_FRIEND_ID} TEXT," +
        "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_NAME} TEXT," +
        "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_EMAIL} TEXT," +
        "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_AVATAR} TEXT," +
        "${FirebaseBackupContract.FriendsEntry.COLUMN_NAME_USER_ID} TEXT)"


private const val SQL_CREATE_NOTES_ENTRIES = "CREATE TABLE ${FirebaseBackupContract.NotesEntry.TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_CREATED_AT} INTEGER," +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_DESCRIPTION} TEXT," +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_LAT} INTEGER, " +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_LNG} INTEGER," +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_PERSON} TEXT," +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_TITLE} TEXT," +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_USER_ID} TEXT," +
        "${FirebaseBackupContract.NotesEntry.COLUMN_NAME_NOTE_ID} TEXT)"

private const val SQL_CREATE_IMAGES_ENTRIES = "CREATE TABLE ${FirebaseBackupContract.ImageEntry.TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY, " +
        "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_CREATED_AT} INTEGER," +
        "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_NAME} TEXT," +
        "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_NOTE_ID} TEXT," +
        "${FirebaseBackupContract.ImageEntry.COLUMN_NAME_IMAGE_ID} TEXT)"

private const val SQL_DELETE_USER_ENTRIES = "DROP TABLE IF EXISTS ${FirebaseBackupContract.UserEntry.TABLE_NAME}"
private const val SQL_DELETE_FRIENDS_ENTRIES = "DROP TABLE IF EXISTS ${FirebaseBackupContract.FriendsEntry.TABLE_NAME}"
private const val SQL_DELETE_NOTES_ENTRIES = "DROP TABLE IF EXISTS ${FirebaseBackupContract.NotesEntry.TABLE_NAME}"
private const val SQL_DELETE_IMAGES_ENTRIES = "DROP TABLE IF EXISTS ${FirebaseBackupContract.ImageEntry.TABLE_NAME}"

class UserSqliteOpenHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_USER_ENTRIES)
        db.execSQL(SQL_CREATE_FRIENDS_ENTRIES)
        db.execSQL(SQL_CREATE_NOTES_ENTRIES)
        db.execSQL(SQL_CREATE_IMAGES_ENTRIES)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_USER_ENTRIES)
        db.execSQL(SQL_DELETE_FRIENDS_ENTRIES)
        db.execSQL(SQL_DELETE_NOTES_ENTRIES)
        db.execSQL(SQL_DELETE_IMAGES_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }


    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FIREBASE_DB"
    }
}