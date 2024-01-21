package com.example.taxiappkpi.Models.Trip

data class TripRequest(
    val start: List<Double> = listOf(),
    val finish: List<Double> = listOf(),
    var price: Double = 0.0,
    var status: Int = -1
){
    fun toMap(): Map<String, Any> {
        return mapOf(
            "start" to start,
            "finish" to finish,
            "price" to price,
            "status" to status,
        )
    }
}