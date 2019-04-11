package com.nathan.geoword.db

import java.util.*

class ImageEntry(newDate: Date, newNoteId: String, newName: String) {

    var created_at: Date
    var note_id: String
    var name : String

    init {
        this.created_at = newDate
        this.note_id = newNoteId
        this.name = newName
    }
}