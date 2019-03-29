package com.nathan.geoword

import com.google.firebase.storage.StorageReference

interface ThumbnailCallback {
    fun onThumbnailClick(imageRef: StorageReference)
}