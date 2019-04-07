package com.nathan.geoword

import com.google.android.gms.maps.model.LatLng
import java.util.*

class MarkerData(title: String, author: String, cr_date: Date, image: String?, latlng: LatLng?) {
    var title: String
    var author: String
    var image: String? = null
    var latlng: LatLng? = null
    var cr_date: Date
    init {
        this.author = author
        this.title = title
        this.image = image
        this.latlng = latlng
        this.cr_date = cr_date
    }
}