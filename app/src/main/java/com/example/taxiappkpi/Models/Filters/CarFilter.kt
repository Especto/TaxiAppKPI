package com.example.taxiappkpi.Models.Filters

import android.os.Parcel
import android.os.Parcelable

data class CarFilter(
    var engType: Int = -1,
    var bodyType: Int = -1
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(engType)
        parcel.writeInt(bodyType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CarFilter> {
        override fun createFromParcel(parcel: Parcel): CarFilter {
            return CarFilter(parcel)
        }

        override fun newArray(size: Int): Array<CarFilter?> {
            return arrayOfNulls(size)
        }
    }
}