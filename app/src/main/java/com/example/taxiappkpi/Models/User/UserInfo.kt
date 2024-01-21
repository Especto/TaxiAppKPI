package com.example.taxiappkpi.Models.User

data class UserInfo(
    var firstName: String = "",
    var phoneNumber: String = "",
    var gender: Int = -1, // 0 male, 1 female
    var avatar: String = "",
    var rating: Double = 0.0,
    var birthdate: Birthdate = Birthdate(),
    var numTrips: Int = 0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "firstName" to firstName,
            "phoneNumber" to phoneNumber,
            "gender" to gender,
            "avatar" to avatar,
            "rating" to rating,
            "birthdate" to birthdate,
            "numTrips" to numTrips
        )
    }
}
