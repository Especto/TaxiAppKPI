package com.example.taxiappkpi.Models.Trip

data class RouteData(
    val distance: Double,
    val routeJson: MutableIterable<com.google.android.gms.maps.model.LatLng>
)


