package com.nathan.geoword

import com.google.android.gms.maps.model.LatLng

class MarkerData(title: String, subtitle: String, image: String?, latlng: LatLng?) {
    var title: String
    var subtitle: String
    var image: String? = null
    var latlng: LatLng? = null
    init {
        this.title = title
        this.subtitle = subtitle
        this.image = image
        this.latlng = latlng
    }
}