package com.nathan.geoword.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.firebase.firestore.GeoPoint

import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class FirebaseDBForSqliteTest {

    lateinit var image: Image
    lateinit var friend: Friend
    lateinit var note: Note
    lateinit var user: User
    var testClass: FirebaseDBForSqlite? = null

    @Before
    fun setUp() {
        testClass = FirebaseDBForSqlite(InstrumentationRegistry.getInstrumentation().targetContext)
        testClass!!.db.deleteTables()
        user = User("steve", false, 15f, "Steve", "steve@test.com", "dummy.jpg", Date())
        friend = Friend(Date(), "nathan", "dummy.png", "Nathan", "nathan@test.com", "steve")
        note = Note(Date(), "a story", -43.50075778603909, 172.59323570877314,"John", "story", "steve", "anote" )
        image = Image(Date(), "anote", "photo.png", "image")
        testClass?.addImage(image)
        testClass?.addNote(note)
        testClass?.addFriend(friend)
        testClass?.addUser(user)

    }

    @After
    fun tearDown() {

        testClass = null
    }

    @Test
    fun addUser() {
        val assertTest = testClass?.getUser("steve")
        assertEquals(assertTest!!.user_id, user.user_id)
    }

    @Test
    fun getFriends() {
        val assertTest = testClass?.getFriends("steve")!!
        assertEquals(assertTest[0].friend_id, friend.friend_id)
    }

    @Test
    fun getMarkerForUser() {

        val assertTest = testClass?.getMarkerForUser(note.geopoint, "steve")!!
        assertEquals(note.geopoint.latitude.toString(), assertTest!![0].geopoint.latitude.toString())
        assertEquals(note.geopoint.longitude.toString(), assertTest!![0].geopoint.longitude.toString())
        assertEquals(assertTest[0].note_id, note.note_id)
    }

    @Test
    fun getImage() {
        val assertTest = testClass?.getImage("image")
        assertEquals(assertTest!!.image_id, image.image_id)
    }

    @Test
    fun getNote() {
        val assertTest = testClass?.getNote("anote")
        assertEquals(note.geopoint.latitude.toString(), assertTest!!.geopoint.latitude.toString())
        assertEquals(note.geopoint.longitude.toString(), assertTest!!.geopoint.longitude.toString())
        assertEquals(assertTest!!.note_id, note.note_id)

    }

    @Test
    fun getNotes() {
        val assertTest = testClass?.getNotes("steve")!!
        assertEquals(assertTest[0].note_id, note.note_id)
    }

    @Test
    fun getFriend() {
        val assertTest = testClass?.getFriend("nathan")
        assertEquals(assertTest!!.friend_id, friend.friend_id)
    }

    @Test
    fun getUser() {
        val assertTest = testClass?.getUser("steve")
        assertEquals(assertTest!!.user_id, user.user_id)
    }

    @Test
    fun updateImage() {
        val imageUpdate = Image(Date(), "anote", "photo2.png", "image")
        testClass?.updateImage(imageUpdate)
        val assertTest = testClass!!.getImage("image")
        assertEquals(imageUpdate.note_id, assertTest!!.note_id)
        assertEquals(imageUpdate.image_id, assertTest!!.image_id)
        assertEquals(imageUpdate.name, assertTest!!.name)
    }

    @Test
    fun updateNote() {
        val noteUpdate = Note(Date(), "a new story", -43.50075778603909, 172.59323570877314,"John", "story", "steve", "anote" )
        testClass?.updateNote(noteUpdate)
        val assertTest = testClass!!.getNote("anote")
        assertEquals(note.geopoint.latitude.toString(), assertTest!!.geopoint.latitude.toString())
        assertEquals(note.geopoint.longitude.toString(), assertTest!!.geopoint.longitude.toString())
        assertEquals(assertTest!!.description, noteUpdate.description)
    }

    @Test
    fun updateUser() {
        val userUpdate = User("steve", false, 15f, "Steve's Work Account", "steve@test.com", "dummy.jpg", Date())
        testClass?.updateUser(userUpdate)
        val assertTest = testClass!!.getUser("steve")
        assertEquals(assertTest!!.name, userUpdate.name)
    }

    @Test
    fun addFriend() {
        val assertTest = testClass?.getFriend("nathan")
        assertEquals(assertTest!!.friend_id, friend.friend_id)
    }

    @Test
    fun updateFriend() {
        val friendUpdate = Friend(Date(), "nathan", "dummy.png", "Nathan's Work Account", "nathan@test.com", "steve")
        testClass?.updateFriend(friendUpdate)
        val assertTest = testClass!!.getFriend("nathan")
        assertEquals(assertTest!!.name, friendUpdate.name)
    }

    @Test
    fun addNote() {
        val assertTest = testClass?.getNote("anote")
        assertEquals(note.geopoint.latitude.toString(), assertTest!!.geopoint.latitude.toString())
        assertEquals(note.geopoint.longitude.toString(), assertTest!!.geopoint.longitude.toString())
        assertEquals(assertTest!!.note_id, note.note_id)
    }

    @Test
    fun addImage() {
        val assertTest = testClass?.getImage("image")
        assertEquals(assertTest!!.image_id, image.image_id)
    }
}