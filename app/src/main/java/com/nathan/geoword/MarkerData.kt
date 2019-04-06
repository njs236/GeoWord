package com.nathan.geoword

import com.google.android.gms.maps.model.LatLng
import java.util.*

class MarkerData(title: String, subtitle: String, cr_date: Date, image: String?, latlng: LatLng?) {
    var title: String
    var subtitle: String
    var image: String? = null
    var latlng: LatLng? = null
    var cr_date: Date
    init {
        this.title = title
        this.subtitle = subtitle
        this.image = image
        this.latlng = latlng
        this.cr_date = cr_date
    }
}