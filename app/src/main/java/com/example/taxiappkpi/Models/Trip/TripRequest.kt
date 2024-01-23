package com.example.taxiappkpi.Models.Trip

import com.google.android.gms.maps.model.LatLng

data class TripRequest(
    val start: LatLng,
    val finish: LatLng,
    var price: Double = 0.0,
    var status: Int = -1,
    var distance: Double = 0.0,
    var route: MutableIterable<LatLng>
){
    fun toMap(): Map<String, Any> {
        return mapOf(
            "start" to start,
            "finish" to finish,
            "price" to price,
            "status" to status,
            "distance" to distance,
            "route" to route
        )
    }
}