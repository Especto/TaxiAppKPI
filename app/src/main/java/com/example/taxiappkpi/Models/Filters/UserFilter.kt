package com.example.taxiappkpi.Models.Filters

import android.os.Parcel
import android.os.Parcelable

data class UserFilter(
    var gender : Int = -1,
    var minAge : Int = -1,
    var maxAge : Int = -1
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(gender)
        parcel.writeInt(minAge)
        parcel.writeInt(maxAge)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserFilter> {
        override fun createFromParcel(parcel: Parcel): UserFilter {
            return UserFilter(parcel)
        }

        override fun newArray(size: Int): Array<UserFilter?> {
            return arrayOfNulls(size)
        }
    }
}
