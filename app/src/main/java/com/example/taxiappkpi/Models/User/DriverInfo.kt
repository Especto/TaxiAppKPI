package com.example.taxiappkpi.Models.User

data class DriverInfo(
    val userInfo: UserInfo = UserInfo(),
    val carInfo: CarInfo = CarInfo(),
    var driverLicense: String = ""
) {

    fun toMap(): Map<String, Any> {
        val userInfoMap = userInfo.toMap().toMutableMap()
        userInfoMap["driverLicense"] = driverLicense
        return mapOf(
            "userInfo" to userInfoMap,
            "carInfo" to carInfo.toMap()
        )
    }
}
