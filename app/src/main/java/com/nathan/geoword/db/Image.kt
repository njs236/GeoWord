package com.nathan.geoword.db

import java.util.*

class Image(newDate: Date, newNoteId: String, newName: String, newId: String) {

    var created_at: Date
    var note_id: String
    var name : String
    var image_id: String

    init {
        this.created_at = newDate
        this.note_id = newNoteId
        this.name = newName
        this.image_id = newId
    }
}