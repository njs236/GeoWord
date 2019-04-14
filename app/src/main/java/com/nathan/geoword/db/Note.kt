package com.nathan.geoword.db

import com.google.firebase.firestore.GeoPoint
import java.util.*

class Note(newDate: Date, newDescription: String, newLat: Double, newLng: Double, newPerson: String, newTitle: String, newUserID: String, newNoteId: String) {

    var created_at: Date
    var description: String
    var geopoint: GeoPoint
    var person: String
    var title: String
    var user_id: String
    var note_id: String

    init {
        this.created_at = newDate
        this.description = newDescription
        this.geopoint = GeoPoint(newLat, newLng)
        this.person = newPerson
        this.title = newTitle
        this.user_id = newUserID
        this.note_id = newNoteId
    }
}