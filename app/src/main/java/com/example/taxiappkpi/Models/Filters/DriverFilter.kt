package com.example.taxiappkpi.Models.Filters

import android.os.Parcel
import android.os.Parcelable

data class DriverFilter(
    var userFilter: UserFilter = UserFilter(),
    var carFilter: CarFilter = CarFilter()
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(UserFilter::class.java.classLoader)!!,
        parcel.readParcelable(CarFilter::class.java.classLoader)!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(userFilter, flags)
        parcel.writeParcelable(carFilter, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DriverFilter> {
        override fun createFromParcel(parcel: Parcel): DriverFilter {
            return DriverFilter(parcel)
        }

        override fun newArray(size: Int): Array<DriverFilter?> {
            return arrayOfNulls(size)
        }
    }
}
