package com.nathan.geoword.db

import java.util.*

class User (newUserId: String, newLocation: Boolean, newZoom: Float, newName: String, newEmail: String, newAvatar: String, newDate: Date) {

    var user_id : String
    var location: Boolean
    var defaultZoom: Float
    var name: String
    var email: String
    var avatar: String
    var created_at: Date

    init {
        this.user_id = newUserId
        this.location = newLocation
        this.defaultZoom = newZoom
        this.name = newName
        this.email = newEmail
        this.avatar = newAvatar
        this.created_at = newDate
    }



}