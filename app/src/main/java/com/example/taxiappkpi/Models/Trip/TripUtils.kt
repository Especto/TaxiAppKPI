package com.example.taxiappkpi.Models.Trip

import com.google.android.gms.maps.model.LatLng

fun Map<*, *>.toTripRequest(): TripRequest {
    return TripRequest(
        start = parseLatLng(this["start"]),
        finish = parseLatLng(this["finish"]),
        price = this["price"] as? Double ?: 0.0,
        status = this["status"] as? Int ?: -1,
        distance = this["distance"] as? Double?: 0.0,
        route = parseRoute(this["route"]),
    )
}

fun parseLatLng(data: Any?): LatLng {
    if (data is Map<*, *>) {
        val latitude = data["latitude"] as? Double ?: 0.0
        val longitude = data["longitude"] as? Double ?: 0.0
        return LatLng(latitude, longitude)
    }
    throw IllegalArgumentException("Invalid LatLng data")
}
fun parseRoute(value: Any?): MutableIterable<LatLng> {
    return (value as? Iterable<*>)?.mapNotNull { parseLatLng(it) }?.toMutableList()
        ?: mutableListOf()
}
