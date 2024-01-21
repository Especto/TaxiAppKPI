package com.example.taxiappkpi.Models.User

import java.io.Serializable

data class CarInfo(
    var engType: Int = -1, // 0 - ICE, 1 - E
    var brand: String = "",
    var model: String = "",
    var bodyType: Int = 0,
    var color: Int = 0,
    var number: String = "",
    var photo: String = ""
): Serializable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "engType" to engType,
            "brand" to brand,
            "model" to model,
            "body" to bodyType,
            "color" to color,
            "number" to number,
            "photo" to photo
        )
    }
}
