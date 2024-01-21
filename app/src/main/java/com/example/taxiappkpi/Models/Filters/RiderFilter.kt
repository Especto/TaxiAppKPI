package com.example.taxiappkpi.Models.Filters

import android.os.Parcel
import android.os.Parcelable

data class RiderFilter(
    val userFilter: UserFilter = UserFilter()
): Parcelable {
    constructor(parcel: Parcel) : this(parcel.readParcelable(UserFilter::class.java.classLoader)!!) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(userFilter, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RiderFilter> {
        override fun createFromParcel(parcel: Parcel): RiderFilter {
            return RiderFilter(parcel)
        }

        override fun newArray(size: Int): Array<RiderFilter?> {
            return arrayOfNulls(size)
        }
    }
}
