package com.example.taxiappkpi.Models.User


data class RiderInfo(
    var userInfo: UserInfo = UserInfo()
) {
    fun toMap(): Map<String, Any> {
        return mapOf("userInfo" to userInfo.toMap())
    }
}
