package com.nathan.geoword.db

import java.util.*

class FriendEntry(newDate: Date, newFriendID: String, newAvatar:String, newName: String, newEmail: String, newUserID: String) {

    var friend_id: String
    var created_at: Date
    var avatar: String
    var name : String
    var email: String
    var user_id: String
    init {
        this.friend_id = newFriendID
        this.created_at = newDate
        this.avatar = newAvatar
        this.name = newName
        this.email = newEmail
        this.user_id = newUserID
    }

}