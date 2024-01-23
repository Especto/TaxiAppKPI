package com.example.taxiappkpi.Models.User

import android.util.Log

fun Map<*, *>.toUserInfo(): UserInfo {
    return UserInfo(
        firstName = this["firstName"].toString(),
        phoneNumber = this["phoneNumber"].toString(),
        gender = (this["gender"] as? Long)?.toInt() ?: 0,
        avatar = this["avatar"].toString(),
        rating = (this["rating"] as? Long)?.toDouble() ?: (this["rating"] as Double),
        birthdate = (this["birthdate"] as? Map<String, Any>)?.toBirthdate() ?: Birthdate(),
        numTrips = (this["numTrips"] as? Long)?.toInt() ?: 0
    )
}

fun Map<*, *>.toBirthdate(): Birthdate {
    return Birthdate(
        year = (this["year"] as? Long)?.toInt() ?: 0,
        month = (this["month"] as? Long)?.toInt() ?: 0,
        day = (this["day"] as? Long)?.toInt() ?: 0
    )
}

fun Map<*, *>.toCarInfo(): CarInfo {
    return CarInfo(
        engType = (this["engType"] as? Long)?.toInt() ?: 0,
        brand = this["brand"].toString(),
        model = this["model"].toString(),
        bodyType = (this["body"] as? Long)?.toInt() ?: 0,
        color = (this["color"] as? Long)?.toInt() ?: 0,
        number = this["number"].toString(),
        photo = this["photo"].toString()
    )
}

fun Map<*, *>.toDriverInfo(): DriverInfo {
    val userInfoMap = this["userInfo"] as MutableMap<String, Any>
    val driverLicenseValue = userInfoMap["driverLicense"] as? String ?: ""
    userInfoMap.remove("driverLicense")
    return DriverInfo(
        userInfo = userInfoMap.toUserInfo(),
        carInfo = this["carInfo"]?.let { (it as Map<String, Any>).toCarInfo() } ?: CarInfo(),
        driverLicense = driverLicenseValue
    )
}

fun Map<*, *>.toRiderInfo(): RiderInfo {
    Log.d("log_check", "${this["userInfo"]}")
    return RiderInfo(
        userInfo = this["userInfo"]?.let { (it as Map<String, Any>).toUserInfo() } ?: UserInfo()
    )
}
